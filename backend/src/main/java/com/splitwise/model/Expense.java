package com.splitwise.model;

import com.splitwise.model.enums.ExpenseCategory;
import com.splitwise.model.enums.SplitType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Expense entity — the central business object.
 *
 * KEY DESIGN DECISIONS:
 *
 * 1. WHY BigDecimal for amount instead of Double?
 *    Double has floating-point precision issues: 0.1 + 0.2 = 0.30000000000000004
 *    In a money app, this is unacceptable. BigDecimal provides exact decimal arithmetic.
 *    This is the #1 rule of financial software: NEVER use float/double for money.
 *
 * 2. WHY separate Expense and ExpenseSplit?
 *    An expense has ONE total amount but MANY individual shares (splits).
 *    Example: $100 dinner, split 3 ways = 3 ExpenseSplit records of $33.33 each.
 *    Alternative: Store splits as a JSON column — loses queryability and referential integrity.
 *    Alternative: Store only the total and calculate splits on the fly — loses audit trail
 *    of what each person owed at the time the expense was created.
 *
 * 3. WHY paidBy is a single User?
 *    In most cases, one person pays. Multi-payer expenses (2 people split the bill)
 *    are rare. We handle multi-payer by creating multiple expenses or adding a
 *    "payers" list later if needed. Keeping it simple for 95% of use cases.
 *
 * 4. WHY a "group" field that's nullable?
 *    Expenses can be group expenses OR 1-on-1 expenses (no group).
 *    A null group means it's a direct expense between two users.
 *
 * 5. WHY isRecurring + recurringInterval?
 *    Recurring expenses (rent, subscriptions) are created once and auto-duplicated
 *    by a scheduled task. The interval is stored as a string like "MONTHLY", "WEEKLY".
 */
@Entity
@Table(name = "expenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String description;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SplitType splitType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExpenseCategory category = ExpenseCategory.OTHER;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @Builder.Default
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExpenseSplit> splits = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "expense", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Comment> comments = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    private Boolean isRecurring = false;

    private String recurringInterval;

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    private String receiptUrl;
}
