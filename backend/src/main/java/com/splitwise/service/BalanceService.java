package com.splitwise.service;

import com.splitwise.dto.response.BalanceResponse;
import com.splitwise.mapper.EntityMapper;
import com.splitwise.model.*;
import com.splitwise.repository.ExpenseRepository;
import com.splitwise.repository.SettlementRepository;
import com.splitwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * THE DEBT SIMPLIFICATION ALGORITHM — the crown jewel of any Splitwise clone.
 *
 * PROBLEM: In a group of 5 people with 20 expenses, there could be 20 separate debts.
 * GOAL: Minimize the number of transactions needed to settle all debts.
 *
 * ALGORITHM (Greedy approach):
 * 1. Calculate net balance for each person (total owed TO them minus total they OWE)
 * 2. Separate people into creditors (positive balance) and debtors (negative balance)
 * 3. Match the largest debtor with the largest creditor
 * 4. Transfer the minimum of their amounts
 * 5. One of them becomes zero — remove from the list
 * 6. Repeat until all balances are zero
 *
 * EXAMPLE:
 *   Alice: +$50 (people owe her $50)
 *   Bob:   -$30 (he owes $30)
 *   Carol: -$20 (she owes $20)
 *
 *   Step 1: Bob pays Alice $30 → Alice: +$20, Bob: 0, Carol: -$20
 *   Step 2: Carol pays Alice $20 → Alice: 0, Bob: 0, Carol: 0
 *   Result: 2 transactions instead of potentially many more.
 *
 * WHY GREEDY and not OPTIMAL?
 * - Finding the truly optimal (minimum transactions) is NP-hard (subset-sum problem)
 * - The greedy approach gives at most N-1 transactions for N people
 * - Splitwise also uses the greedy approach — it's good enough
 * - Alternative: Brute force all subsets (2^N complexity, impractical for large groups)
 *
 * TIME COMPLEXITY: O(N log N) where N = number of people (due to sorting)
 */
@Service
@RequiredArgsConstructor
public class BalanceService {

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final UserRepository userRepository;
    private final EntityMapper entityMapper;

    /**
     * Calculate balances for a user across all groups and direct expenses.
     */
    public BalanceResponse getUserOverallBalance(Long userId) {
        List<Expense> expenses = expenseRepository.findExpensesInvolvingUser(userId);
        List<Settlement> settlements = settlementRepository.findSettlementsInvolvingUser(userId);

        Map<Long, BigDecimal> balanceMap = calculateNetBalances(userId, expenses, settlements);

        return buildBalanceResponse(balanceMap);
    }

    /**
     * Calculate balances within a specific group.
     */
    public BalanceResponse getGroupBalance(Long groupId, Long userId) {
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByDateDesc(groupId);
        List<Settlement> settlements = settlementRepository.findByGroupIdOrderByDateDesc(groupId);

        Map<Long, BigDecimal> balanceMap = calculateNetBalances(userId, expenses, settlements);

        return buildBalanceResponse(balanceMap);
    }

    /**
     * Get simplified debts — the minimum set of transactions to settle all debts in a group.
     */
    public List<BalanceResponse.DebtEntry> getSimplifiedDebts(Long groupId) {
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByDateDesc(groupId);
        List<Settlement> settlements = settlementRepository.findByGroupIdOrderByDateDesc(groupId);

        Map<Long, BigDecimal> netBalances = calculateGroupNetBalances(expenses, settlements);

        return simplifyDebts(netBalances);
    }

    /**
     * Get per-person net balance summary for a group.
     * Positive = is owed money, Negative = owes money.
     */
    public List<BalanceResponse.UserBalance> getGroupMemberBalances(Long groupId) {
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByDateDesc(groupId);
        List<Settlement> settlements = settlementRepository.findByGroupIdOrderByDateDesc(groupId);

        Map<Long, BigDecimal> netBalances = calculateGroupNetBalances(expenses, settlements);

        List<BalanceResponse.UserBalance> result = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            User user = userRepository.findById(entry.getKey()).orElse(null);
            if (user == null) continue;
            result.add(BalanceResponse.UserBalance.builder()
                    .user(entityMapper.toUserResponse(user))
                    .amount(entry.getValue())
                    .build());
        }
        result.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return result;
    }

    /**
     * Get all pairwise debts within a group as from/to pairs.
     */
    public List<BalanceResponse.DebtEntry> getGroupDebts(Long groupId) {
        List<Expense> expenses = expenseRepository.findByGroupIdOrderByDateDesc(groupId);
        List<Settlement> settlements = settlementRepository.findByGroupIdOrderByDateDesc(groupId);

        Map<String, BigDecimal> pairwiseDebts = new HashMap<>();

        for (Expense expense : expenses) {
            Long payerId = expense.getPaidBy().getId();
            for (ExpenseSplit split : expense.getSplits()) {
                Long splitUserId = split.getUser().getId();
                if (!splitUserId.equals(payerId)) {
                    String key = splitUserId + "->" + payerId;
                    pairwiseDebts.merge(key, split.getAmount(), BigDecimal::add);
                }
            }
        }

        for (Settlement settlement : settlements) {
            Long fromId = settlement.getPaidBy().getId();
            Long toId = settlement.getPaidTo().getId();
            String key = fromId + "->" + toId;
            pairwiseDebts.merge(key, settlement.getAmount().negate(), BigDecimal::add);
        }

        Map<String, BigDecimal> netDebts = new HashMap<>();
        Set<String> processed = new HashSet<>();
        for (Map.Entry<String, BigDecimal> entry : pairwiseDebts.entrySet()) {
            String key = entry.getKey();
            if (processed.contains(key)) continue;

            String[] parts = key.split("->");
            String reverseKey = parts[1] + "->" + parts[0];
            BigDecimal forward = entry.getValue();
            BigDecimal reverse = pairwiseDebts.getOrDefault(reverseKey, BigDecimal.ZERO);
            BigDecimal net = forward.subtract(reverse);

            processed.add(key);
            processed.add(reverseKey);

            if (net.compareTo(new BigDecimal("0.01")) > 0) {
                netDebts.put(key, net);
            } else if (net.compareTo(new BigDecimal("-0.01")) < 0) {
                netDebts.put(reverseKey, net.abs());
            }
        }

        List<BalanceResponse.DebtEntry> debts = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : netDebts.entrySet()) {
            String[] parts = entry.getKey().split("->");
            Long fromId = Long.parseLong(parts[0]);
            Long toId = Long.parseLong(parts[1]);

            User fromUser = userRepository.findById(fromId).orElse(null);
            User toUser = userRepository.findById(toId).orElse(null);
            if (fromUser == null || toUser == null) continue;

            debts.add(BalanceResponse.DebtEntry.builder()
                    .from(entityMapper.toUserResponse(fromUser))
                    .to(entityMapper.toUserResponse(toUser))
                    .amount(entry.getValue())
                    .build());
        }

        debts.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return debts;
    }

    /**
     * Calculate balance between the current user and every other user they've transacted with.
     *
     * For each expense:
     *   - If current user PAID: everyone in the splits owes them their split amount
     *   - If current user is in a SPLIT: they owe the payer their split amount
     *
     * For each settlement:
     *   - If current user PAID: the recipient's debt to us decreases
     *   - If current user RECEIVED: our debt to the payer decreases
     */
    private Map<Long, BigDecimal> calculateNetBalances(Long userId,
                                                       List<Expense> expenses,
                                                       List<Settlement> settlements) {
        Map<Long, BigDecimal> balanceMap = new HashMap<>();

        for (Expense expense : expenses) {
            Long payerId = expense.getPaidBy().getId();

            for (ExpenseSplit split : expense.getSplits()) {
                Long splitUserId = split.getUser().getId();
                BigDecimal splitAmount = split.getAmount();

                if (payerId.equals(userId) && !splitUserId.equals(userId)) {
                    balanceMap.merge(splitUserId, splitAmount, BigDecimal::add);
                } else if (splitUserId.equals(userId) && !payerId.equals(userId)) {
                    balanceMap.merge(payerId, splitAmount.negate(), BigDecimal::add);
                }
            }
        }

        for (Settlement settlement : settlements) {
            Long fromId = settlement.getPaidBy().getId();
            Long toId = settlement.getPaidTo().getId();
            BigDecimal amount = settlement.getAmount();

            if (fromId.equals(userId)) {
                balanceMap.merge(toId, amount.negate(), BigDecimal::add);
            } else if (toId.equals(userId)) {
                balanceMap.merge(fromId, amount, BigDecimal::add);
            }
        }

        balanceMap.entrySet().removeIf(e -> e.getValue().compareTo(BigDecimal.ZERO) == 0);

        return balanceMap;
    }

    private Map<Long, BigDecimal> calculateGroupNetBalances(List<Expense> expenses,
                                                            List<Settlement> settlements) {
        Map<Long, BigDecimal> netBalances = new HashMap<>();

        for (Expense expense : expenses) {
            Long payerId = expense.getPaidBy().getId();
            netBalances.merge(payerId, expense.getAmount(), BigDecimal::add);

            for (ExpenseSplit split : expense.getSplits()) {
                netBalances.merge(split.getUser().getId(), split.getAmount().negate(), BigDecimal::add);
            }
        }

        for (Settlement settlement : settlements) {
            netBalances.merge(settlement.getPaidBy().getId(), settlement.getAmount().negate(), BigDecimal::add);
            netBalances.merge(settlement.getPaidTo().getId(), settlement.getAmount(), BigDecimal::add);
        }

        netBalances.entrySet().removeIf(e -> e.getValue().abs().compareTo(new BigDecimal("0.01")) < 0);

        return netBalances;
    }

    /**
     * THE GREEDY DEBT SIMPLIFICATION ALGORITHM.
     * Returns a list of "User X owes User Y $Z" pairs — the minimum transactions needed.
     */
    private List<BalanceResponse.DebtEntry> simplifyDebts(Map<Long, BigDecimal> netBalances) {
        List<BalanceResponse.DebtEntry> simplifiedDebts = new ArrayList<>();

        PriorityQueue<Map.Entry<Long, BigDecimal>> creditors = new PriorityQueue<>(
                (a, b) -> b.getValue().compareTo(a.getValue()));
        PriorityQueue<Map.Entry<Long, BigDecimal>> debtors = new PriorityQueue<>(
                Comparator.comparing(Map.Entry::getValue));

        for (Map.Entry<Long, BigDecimal> entry : netBalances.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(entry);
            } else if (entry.getValue().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(entry);
            }
        }

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            Map.Entry<Long, BigDecimal> creditor = creditors.poll();
            Map.Entry<Long, BigDecimal> debtor = debtors.poll();

            BigDecimal creditAmount = creditor.getValue();
            BigDecimal debtAmount = debtor.getValue().abs();
            BigDecimal settleAmount = creditAmount.min(debtAmount);

            User debtorUser = userRepository.findById(debtor.getKey()).orElse(null);
            User creditorUser = userRepository.findById(creditor.getKey()).orElse(null);

            if (debtorUser != null && creditorUser != null) {
                simplifiedDebts.add(BalanceResponse.DebtEntry.builder()
                        .from(entityMapper.toUserResponse(debtorUser))
                        .to(entityMapper.toUserResponse(creditorUser))
                        .amount(settleAmount)
                        .build());
            }

            BigDecimal remainingCredit = creditAmount.subtract(settleAmount);
            BigDecimal remainingDebt = debtAmount.subtract(settleAmount);

            if (remainingCredit.compareTo(new BigDecimal("0.01")) > 0) {
                creditor.setValue(remainingCredit);
                creditors.add(creditor);
            }
            if (remainingDebt.compareTo(new BigDecimal("0.01")) > 0) {
                debtor.setValue(remainingDebt.negate());
                debtors.add(debtor);
            }
        }

        return simplifiedDebts;
    }

    private BalanceResponse buildBalanceResponse(Map<Long, BigDecimal> balanceMap) {
        BigDecimal totalOwed = BigDecimal.ZERO;
        BigDecimal totalOwing = BigDecimal.ZERO;
        List<BalanceResponse.UserBalance> userBalances = new ArrayList<>();

        for (Map.Entry<Long, BigDecimal> entry : balanceMap.entrySet()) {
            User user = userRepository.findById(entry.getKey()).orElse(null);
            if (user == null) continue;

            BigDecimal amount = entry.getValue();
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                totalOwed = totalOwed.add(amount);
            } else {
                totalOwing = totalOwing.add(amount.abs());
            }

            userBalances.add(BalanceResponse.UserBalance.builder()
                    .user(entityMapper.toUserResponse(user))
                    .amount(amount)
                    .build());
        }

        userBalances.sort((a, b) -> b.getAmount().abs().compareTo(a.getAmount().abs()));

        return BalanceResponse.builder()
                .totalOwed(totalOwed)
                .totalOwing(totalOwing)
                .netBalance(totalOwed.subtract(totalOwing))
                .balances(userBalances)
                .build();
    }
}
