package com.splitwise.service;

import com.splitwise.dto.request.CreateExpenseRequest;
import com.splitwise.dto.response.ApiResponse;
import com.splitwise.dto.response.ExpenseResponse;
import com.splitwise.exception.BadRequestException;
import com.splitwise.exception.ResourceNotFoundException;
import com.splitwise.mapper.EntityMapper;
import com.splitwise.model.*;
import com.splitwise.model.enums.ActivityType;
import com.splitwise.model.enums.SplitType;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The core business logic for expense management.
 *
 * SPLIT CALCULATION EXPLAINED:
 *
 * EQUAL: amount / numberOfPeople. The remainder (if any) goes to the first person.
 *   Example: $100 / 3 = $33.33, $33.33, $33.34 (last person gets the extra cent)
 *
 * EXACT: The client specifies exact amounts per person. We just validate they sum to the total.
 *
 * PERCENTAGE: Client specifies percentages per person. We validate they sum to 100%.
 *   Then calculate: amount * (percentage / 100) for each person.
 *
 * WHY RoundingMode.HALF_UP?
 * - HALF_UP is the most common rounding mode for financial calculations (banker's rounding)
 * - Alternative: HALF_EVEN (round to nearest even number) — used in some financial systems
 *   to avoid statistical bias, but HALF_UP is simpler and expected by users
 */
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final GroupService groupService;
    private final ActivityService activityService;
    private final EntityMapper entityMapper;

    @Transactional
    public ExpenseResponse createExpense(Long paidByUserId, CreateExpenseRequest request) {
        User paidBy = userRepository.findById(paidByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", paidByUserId));

        Group group = null;
        if (request.getGroupId() != null) {
            group = groupService.findGroupOrThrow(request.getGroupId());
        }

        Expense expense = Expense.builder()
                .description(request.getDescription())
                .amount(request.getAmount())
                .splitType(request.getSplitType())
                .category(request.getCategory())
                .date(request.getDate() != null ? request.getDate() : LocalDate.now())
                .paidBy(paidBy)
                .group(group)
                .isRecurring(request.getIsRecurring() != null ? request.getIsRecurring() : false)
                .recurringInterval(request.getRecurringInterval())
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .build();

        List<ExpenseSplit> splits = calculateSplits(expense, request);
        expense.setSplits(splits);

        expense = expenseRepository.save(expense);

        activityService.logActivity(
                ActivityType.EXPENSE_ADDED,
                paidBy.getName() + " added \"" + expense.getDescription() + "\" (" + expense.getCurrency() + " " + expense.getAmount() + ")",
                paidBy, group, expense.getId(), "EXPENSE"
        );

        return entityMapper.toExpenseResponse(expense);
    }

    @Transactional
    public ExpenseResponse updateExpense(Long expenseId, Long userId, CreateExpenseRequest request) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));

        if (!expense.getPaidBy().getId().equals(userId)) {
            throw new BadRequestException("Only the payer can update this expense");
        }

        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setSplitType(request.getSplitType());
        expense.setCategory(request.getCategory());
        if (request.getDate() != null) {
            expense.setDate(request.getDate());
        }

        expense.getSplits().clear();
        List<ExpenseSplit> newSplits = calculateSplits(expense, request);
        expense.getSplits().addAll(newSplits);

        expense = expenseRepository.save(expense);

        User user = userRepository.findById(userId).orElseThrow();
        activityService.logActivity(
                ActivityType.EXPENSE_UPDATED,
                user.getName() + " updated \"" + expense.getDescription() + "\"",
                user, expense.getGroup(), expense.getId(), "EXPENSE"
        );

        return entityMapper.toExpenseResponse(expense);
    }

    @Transactional
    public ApiResponse deleteExpense(Long expenseId, Long userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));

        if (!expense.getPaidBy().getId().equals(userId)) {
            throw new BadRequestException("Only the payer can delete this expense");
        }

        User user = userRepository.findById(userId).orElseThrow();
        activityService.logActivity(
                ActivityType.EXPENSE_DELETED,
                user.getName() + " deleted \"" + expense.getDescription() + "\"",
                user, expense.getGroup(), expenseId, "EXPENSE"
        );

        expenseRepository.delete(expense);
        return new ApiResponse(true, "Expense deleted successfully");
    }

    public ExpenseResponse getExpenseById(Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", "id", expenseId));
        return entityMapper.toExpenseResponse(expense);
    }

    public List<ExpenseResponse> getGroupExpenses(Long groupId) {
        return expenseRepository.findByGroupIdOrderByDateDesc(groupId).stream()
                .map(entityMapper::toExpenseResponse)
                .collect(Collectors.toList());
    }

    public Page<ExpenseResponse> getUserExpenses(Long userId, int page, int size) {
        Page<Expense> expenses = expenseRepository.findExpensesInvolvingUser(
                userId, PageRequest.of(page, size, Sort.by("date").descending()));
        return expenses.map(entityMapper::toExpenseResponse);
    }

    public List<ExpenseResponse> getExpensesBetweenUsers(Long userId1, Long userId2) {
        return expenseRepository.findNonGroupExpensesBetweenUsers(userId1, userId2).stream()
                .map(entityMapper::toExpenseResponse)
                .collect(Collectors.toList());
    }

    /**
     * Calculates how much each participant owes based on the split type.
     *
     * This is the HEART of Splitwise — the split calculation algorithm.
     */
    private List<ExpenseSplit> calculateSplits(Expense expense, CreateExpenseRequest request) {
        List<ExpenseSplit> splits = new ArrayList<>();

        if (request.getSplits() == null || request.getSplits().isEmpty()) {
            throw new BadRequestException("At least one split is required");
        }

        switch (request.getSplitType()) {
            case EQUAL -> splits = calculateEqualSplits(expense, request.getSplits());
            case EXACT -> splits = calculateExactSplits(expense, request);
            case PERCENTAGE -> splits = calculatePercentageSplits(expense, request);
        }

        return splits;
    }

    private List<ExpenseSplit> calculateEqualSplits(Expense expense,
                                                    List<CreateExpenseRequest.SplitDetail> splitDetails) {
        int count = splitDetails.size();
        BigDecimal equalShare = expense.getAmount()
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        BigDecimal totalDistributed = equalShare.multiply(BigDecimal.valueOf(count));
        BigDecimal remainder = expense.getAmount().subtract(totalDistributed);

        List<ExpenseSplit> splits = new ArrayList<>();
        for (int i = 0; i < splitDetails.size(); i++) {
            final Long splitUserId = splitDetails.get(i).getUserId();
            User user = userRepository.findById(splitUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", splitUserId));

            BigDecimal amount = equalShare;
            if (i == 0) {
                amount = amount.add(remainder);
            }

            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(user)
                    .amount(amount)
                    .build());
        }
        return splits;
    }

    private List<ExpenseSplit> calculateExactSplits(Expense expense, CreateExpenseRequest request) {
        BigDecimal totalSplit = request.getSplits().stream()
                .map(CreateExpenseRequest.SplitDetail::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalSplit.compareTo(expense.getAmount()) != 0) {
            throw new BadRequestException(
                    "Split amounts (" + totalSplit + ") don't add up to total (" + expense.getAmount() + ")");
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        for (CreateExpenseRequest.SplitDetail detail : request.getSplits()) {
            User user = userRepository.findById(detail.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", detail.getUserId()));

            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(user)
                    .amount(detail.getAmount())
                    .build());
        }
        return splits;
    }

    private List<ExpenseSplit> calculatePercentageSplits(Expense expense, CreateExpenseRequest request) {
        BigDecimal totalPercentage = request.getSplits().stream()
                .map(CreateExpenseRequest.SplitDetail::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPercentage.compareTo(new BigDecimal("100")) != 0) {
            throw new BadRequestException("Percentages must add up to 100%, got " + totalPercentage + "%");
        }

        List<ExpenseSplit> splits = new ArrayList<>();
        for (CreateExpenseRequest.SplitDetail detail : request.getSplits()) {
            User user = userRepository.findById(detail.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", detail.getUserId()));

            BigDecimal amount = expense.getAmount()
                    .multiply(detail.getPercentage())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            splits.add(ExpenseSplit.builder()
                    .expense(expense)
                    .user(user)
                    .amount(amount)
                    .percentage(detail.getPercentage())
                    .build());
        }
        return splits;
    }
}
