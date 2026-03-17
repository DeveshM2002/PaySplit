package com.splitwise.repository;

import com.splitwise.model.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByGroupIdOrderByDateDesc(Long groupId);

    @Query("SELECT s FROM Settlement s WHERE s.paidBy.id = :userId OR s.paidTo.id = :userId ORDER BY s.date DESC")
    List<Settlement> findSettlementsInvolvingUser(@Param("userId") Long userId);

    @Query("SELECT s FROM Settlement s WHERE s.group.id = :groupId AND " +
            "(s.paidBy.id = :userId OR s.paidTo.id = :userId) ORDER BY s.date DESC")
    List<Settlement> findByGroupIdAndUserId(
            @Param("groupId") Long groupId,
            @Param("userId") Long userId);

    @Query("SELECT s FROM Settlement s WHERE " +
            "((s.paidBy.id = :userId1 AND s.paidTo.id = :userId2) OR " +
            "(s.paidBy.id = :userId2 AND s.paidTo.id = :userId1)) " +
            "ORDER BY s.date DESC")
    List<Settlement> findSettlementsBetweenUsers(
            @Param("userId1") Long userId1,
            @Param("userId2") Long userId2);
}
