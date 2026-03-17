package com.splitwise.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Group entity — represents a shared expense group (e.g., "Trip to Goa", "Apartment").
 *
 * KEY DESIGN DECISIONS:
 *
 * 1. WHY @ManyToMany with User?
 *    A user can be in many groups, and a group can have many users.
 *    This is the textbook many-to-many relationship.
 *    The "user_groups" join table is owned by User (mappedBy = "groups").
 *
 * 2. WHY a separate "createdBy" field?
 *    The group creator has special privileges (can delete group, manage members).
 *    We store this as a @ManyToOne to User rather than a simple userId Long because
 *    JPA gives us type safety and can eagerly/lazily load the creator.
 *
 * 3. WHY List for expenses and not Set?
 *    Expenses have a natural ordering (by date/creation time).
 *    List preserves insertion order; Set does not.
 *    Also, Hibernate has a known bug with Set + @OneToMany that causes extra queries.
 *
 * 4. WHY FetchType.LAZY everywhere?
 *    EAGER loading fetches ALL related data when you load the entity.
 *    For a group with 100 expenses, EAGER would load all 100 on every group query.
 *    LAZY means "only load when explicitly accessed" — much better for performance.
 *    Rule of thumb: ALWAYS use LAZY, fetch explicitly when needed via JPQL JOIN FETCH.
 */
@Entity
@Table(name = "expense_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Group extends BaseEntity {

    @NotBlank
    @Size(min = 1, max = 100)
    @Column(nullable = false)
    private String name;

    @Size(max = 500)
    private String description;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Builder.Default
    @ManyToMany(mappedBy = "groups", fetch = FetchType.LAZY)
    private Set<User> members = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Expense> expenses = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Settlement> settlements = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean simplifyDebts = true;
}
