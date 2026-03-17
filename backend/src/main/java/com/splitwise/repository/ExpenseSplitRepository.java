package com.splitwise.repository;

import com.splitwise.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseSplitRepository extends JpaRepository<ExpenseSplit, Long> {

    List<ExpenseSplit> findByUserId(Long userId);

    List<ExpenseSplit> findByExpenseId(Long expenseId);

    @Query("SELECT es FROM ExpenseSplit es WHERE es.user.id = :userId AND es.expense.group.id = :groupId")
    List<ExpenseSplit> findByUserIdAndGroupId(
            @Param("userId") Long userId,
            @Param("groupId") Long groupId);
}
