package com.splitwise.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * WHY a BaseEntity?
 * Every table needs an ID and timestamps. Instead of repeating these 3 fields
 * in every entity (DRY violation), we extract them into a superclass.
 *
 * @MappedSuperclass tells JPA: "Don't create a table for this class, but include
 * its fields in every child entity's table."
 *
 * Alternative: Use an @Embeddable for audit fields — works but is less clean.
 * Alternative: Use Spring Data Auditing (@CreatedDate, @LastModifiedDate) — more
 * powerful (can track who created/modified) but requires extra config.
 * We keep it simple with Hibernate's @CreationTimestamp / @UpdateTimestamp.
 *
 * WHY Long for ID instead of UUID?
 * - Long is faster for indexing and joins (8 bytes vs 16 bytes)
 * - Auto-increment is simpler to debug (you can see order of creation)
 * - Alternative: UUID — better for distributed systems where multiple servers
 *   generate IDs independently. We're not distributed, so Long wins.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
