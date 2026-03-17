package com.splitwise.repository;

import com.splitwise.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByGroupIdOrderByCreatedAtDesc(Long groupId);

    Page<Activity> findByGroupId(Long groupId, Pageable pageable);

    @Query("SELECT a FROM Activity a WHERE a.group.id IN " +
            "(SELECT g.id FROM Group g JOIN g.members m WHERE m.id = :userId) " +
            "OR (a.group IS NULL AND a.performedBy.id = :userId) " +
            "ORDER BY a.createdAt DESC")
    Page<Activity> findActivitiesForUser(@Param("userId") Long userId, Pageable pageable);
}
