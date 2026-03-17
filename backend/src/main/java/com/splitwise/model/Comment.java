package com.splitwise.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Comment — a note/comment on an expense.
 *
 * Example: "This includes the tip" on a restaurant expense.
 *
 * WHY tie comments to Expenses only (not Groups or Settlements)?
 * - Comments on expenses are the most useful (context about what was bought)
 * - Group-level comments would be more like a chat system (different feature)
 * - Settlement comments can use the Settlement.notes field instead
 * - Keeps the model simple — we can always expand later
 */
@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment extends BaseEntity {

    @NotBlank
    @Column(nullable = false, length = 1000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;
}
