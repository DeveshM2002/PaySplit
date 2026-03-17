package com.splitwise.repository;

import com.splitwise.model.Expense;
import com.splitwise.model.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByGroupIdOrderByDateDesc(Long groupId);

    Page<Expense> findByGroupId(Long groupId, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.paidBy.id = :userId OR " +
            "EXISTS (SELECT s FROM ExpenseSplit s WHERE s.expense = e AND s.user.id = :userId) " +
            "ORDER BY e.date DESC")
    List<Expense> findExpensesInvolvingUser(@Param("userId") Long userId);

    @Query("SELECT e FROM Expense e WHERE e.paidBy.id = :userId OR " +
            "EXISTS (SELECT s FROM ExpenseSplit s WHERE s.expense = e AND s.user.id = :userId) " +
            "ORDER BY e.date DESC")
    Page<Expense> findExpensesInvolvingUser(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId AND e.date BETWEEN :startDate AND :endDate " +
            "ORDER BY e.date DESC")
    List<Expense> findByGroupIdAndDateRange(
            @Param("groupId") Long groupId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT e FROM Expense e WHERE e.group.id = :groupId AND e.category = :category " +
            "ORDER BY e.date DESC")
    List<Expense> findByGroupIdAndCategory(
            @Param("groupId") Long groupId,
            @Param("category") ExpenseCategory category);

    List<Expense> findByIsRecurringTrueAndRecurringIntervalIsNotNull();

    @Query("SELECT e FROM Expense e WHERE e.group IS NULL AND " +
            "((e.paidBy.id = :userId1 AND EXISTS (SELECT s FROM ExpenseSplit s WHERE s.expense = e AND s.user.id = :userId2)) OR " +
            "(e.paidBy.id = :userId2 AND EXISTS (SELECT s FROM ExpenseSplit s WHERE s.expense = e AND s.user.id = :userId1))) " +
            "ORDER BY e.date DESC")
    List<Expense> findNonGroupExpensesBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2);
}
