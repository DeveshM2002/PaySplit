package com.splitwise.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * ExpenseSplit — represents one person's share in an expense.
 *
 * For a $300 expense split 3 ways:
 *   ExpenseSplit { user: Alice, amount: 100.00 }
 *   ExpenseSplit { user: Bob,   amount: 100.00 }
 *   ExpenseSplit { user: Carol, amount: 100.00 }
 *
 * WHY a separate entity and not just a Map<User, BigDecimal> on Expense?
 * - Maps in JPA use @ElementCollection which creates unindexed tables
 * - We can't easily query "show all splits for User X" with a Map
 * - A proper entity gives us full JPA capabilities (joins, queries, indexes)
 *
 * The "amount" here is what this user OWES, not what they paid.
 * The payer info is on the Expense entity itself.
 */
@Entity
@Table(name = "expense_splits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSplit extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 5, scale = 2)
    private BigDecimal percentage;
}
