package com.splitwise.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Settlement — records when one user pays back another to settle a debt.
 *
 * Example: Alice owes Bob $50 overall. Alice pays Bob $50. A Settlement is created.
 *
 * WHY a separate Settlement entity instead of just another Expense?
 * - Semantically different: an Expense is "we spent money together",
 *   a Settlement is "I'm paying you back"
 * - Settlements reduce balances; Expenses create balances
 * - Mixing them would make balance calculations confusing
 * - Splitwise also keeps them separate in their data model
 *
 * WHY paidBy and paidTo (not just two user_ids)?
 * - Named fields make the code self-documenting
 * - "paidBy sends money TO paidTo" is immediately clear
 * - Alternative: Generic "fromUser" / "toUser" — works but less descriptive
 */
@Entity
@Table(name = "settlements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paid_to", nullable = false)
    private User paidTo;

    @NotNull
    @Positive
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private Group group;

    @NotNull
    @Column(nullable = false)
    private LocalDate date;

    private String notes;
}
