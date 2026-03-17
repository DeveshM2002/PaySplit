package com.splitwise.repository;

import com.splitwise.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * WHY @Query with JPQL for getGroupsByUserId?
 * This query joins across the many-to-many relationship (user_groups table).
 * The derived query method name would be unwieldy: "findByMembersId" might work
 * but is less clear. JPQL gives us explicit control.
 *
 * JPQL vs Native SQL:
 * - JPQL: Uses entity class names (Group, User) — database-agnostic
 * - Native SQL: Uses table names (expense_groups, users) — DB-specific
 * - We prefer JPQL because it works with H2 (dev) and PostgreSQL (prod) without changes
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    @Query("SELECT g FROM Group g JOIN g.members m WHERE m.id = :userId")
    List<Group> findGroupsByUserId(@Param("userId") Long userId);

    @Query("SELECT g FROM Group g WHERE g.createdBy.id = :userId")
    List<Group> findGroupsCreatedByUser(@Param("userId") Long userId);
}
