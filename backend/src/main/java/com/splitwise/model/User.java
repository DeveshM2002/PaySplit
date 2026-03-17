package com.splitwise.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * User entity — represents a registered user in the system.
 *
 * KEY DESIGN DECISIONS:
 *
 * 1. WHY @Table(name = "users") and not "user"?
 *    "user" is a reserved keyword in PostgreSQL/H2. Using it causes SQL errors.
 *    Always avoid reserved words for table names.
 *
 * 2. WHY unique constraint on email?
 *    Email is the login identifier. Duplicates would break authentication.
 *    We enforce this at the DB level (not just application level) because
 *    race conditions could bypass app-level checks.
 *
 * 3. WHY we don't store the user's groups directly here (no @ManyToMany)?
 *    We use a separate UserGroup join entity instead of JPA's @ManyToMany.
 *    Reason: We need extra fields on the relationship (role, joinedAt).
 *    @ManyToMany only creates a bare join table with two foreign keys.
 *
 * 4. WHY HashSet for collections?
 *    - Prevents duplicates automatically
 *    - O(1) contains/remove operations
 *    - Alternative: ArrayList — allows duplicates, O(n) contains
 */
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity {

    @NotBlank
    @Size(min = 1, max = 100)
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Email
    @Column(nullable = false)
    private String email;

    @NotBlank
    @Size(min = 6)
    @Column(nullable = false)
    private String password;

    private String avatarUrl;

    @Column(length = 20)
    private String phone;

    @Builder.Default
    @Column(nullable = false)
    private Boolean emailVerified = false;

    private String passwordResetToken;

    private String emailVerificationToken;

    @Builder.Default
    private Boolean registered = true;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_groups",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<Group> groups = new HashSet<>();
}
