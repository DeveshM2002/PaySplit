package com.splitwise.service;

import com.splitwise.dto.response.ActivityResponse;
import com.splitwise.dto.response.BalanceResponse;
import com.splitwise.dto.response.DashboardResponse;
import com.splitwise.model.Expense;
import com.splitwise.model.ExpenseSplit;
import com.splitwise.model.Group;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aggregates data from multiple services for the dashboard view.
 *
 * WHY a separate DashboardService instead of doing this in the controller?
 * - Dashboard needs data from BalanceService, ActivityService, ExpenseService
 * - Aggregating in the controller would make it bloated
 * - This service can optimize queries (e.g., single DB call for multiple metrics)
 * - Follows the Facade Pattern — one simple interface over complex subsystems
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BalanceService balanceService;
    private final ActivityService activityService;
    private final ExpenseRepository expenseRepository;
    private final GroupRepository groupRepository;

    public DashboardResponse getDashboard(Long userId) {
        BalanceResponse overallBalance = balanceService.getUserOverallBalance(userId);

        List<DashboardResponse.FriendBalanceSummary> friendBalances = new ArrayList<>();
        if (overallBalance.getBalances() != null) {
            for (BalanceResponse.UserBalance ub : overallBalance.getBalances()) {
                if (ub.getUser() != null) {
                    friendBalances.add(DashboardResponse.FriendBalanceSummary.builder()
                            .friendId(ub.getUser().getId())
                            .friendName(ub.getUser().getName())
                            .friendEmail(ub.getUser().getEmail())
                            .amount(ub.getAmount())
                            .build());
                }
            }
        }

        Page<ActivityResponse> recentActivityPage = activityService.getUserActivities(userId, 0, 10);

        Map<String, BigDecimal> categorySpending = calculateCategorySpending(userId);
        Map<String, BigDecimal> monthlySpending = calculateMonthlySpending(userId);

        List<DashboardResponse.GroupBalanceSummary> groupBalances = calculateGroupBalances(userId);

        return DashboardResponse.builder()
                .totalOwed(overallBalance.getTotalOwed())
                .totalOwing(overallBalance.getTotalOwing())
                .netBalance(overallBalance.getNetBalance())
                .friendBalances(friendBalances)
                .groupBalances(groupBalances)
                .recentActivity(recentActivityPage.getContent())
                .categorySpending(categorySpending)
                .monthlySpending(monthlySpending)
                .build();
    }

    private Map<String, BigDecimal> calculateCategorySpending(Long userId) {
        List<Expense> expenses = expenseRepository.findExpensesInvolvingUser(userId);
        Map<String, BigDecimal> categorySpending = new TreeMap<>();

        for (Expense expense : expenses) {
            String category = expense.getCategory().name();
            for (ExpenseSplit split : expense.getSplits()) {
                if (split.getUser().getId().equals(userId)) {
                    categorySpending.merge(category, split.getAmount(), BigDecimal::add);
                }
            }
        }

        return categorySpending;
    }

    private Map<String, BigDecimal> calculateMonthlySpending(Long userId) {
        List<Expense> expenses = expenseRepository.findExpensesInvolvingUser(userId);
        Map<String, BigDecimal> monthlySpending = new TreeMap<>();

        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);

        for (Expense expense : expenses) {
            if (expense.getDate().isAfter(sixMonthsAgo)) {
                String monthKey = expense.getDate().getYear() + "-" +
                        String.format("%02d", expense.getDate().getMonthValue());
                for (ExpenseSplit split : expense.getSplits()) {
                    if (split.getUser().getId().equals(userId)) {
                        monthlySpending.merge(monthKey, split.getAmount(), BigDecimal::add);
                    }
                }
            }
        }

        return monthlySpending;
    }

    private List<DashboardResponse.GroupBalanceSummary> calculateGroupBalances(Long userId) {
        List<Group> groups = groupRepository.findGroupsByUserId(userId);
        List<DashboardResponse.GroupBalanceSummary> summaries = new ArrayList<>();

        for (Group group : groups) {
            BalanceResponse groupBalance = balanceService.getGroupBalance(group.getId(), userId);
            summaries.add(DashboardResponse.GroupBalanceSummary.builder()
                    .groupId(group.getId())
                    .groupName(group.getName())
                    .balance(groupBalance.getNetBalance())
                    .build());
        }

        return summaries;
    }
}
