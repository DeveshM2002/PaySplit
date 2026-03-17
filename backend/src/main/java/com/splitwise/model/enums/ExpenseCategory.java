package com.splitwise.model.enums;

/**
 * Predefined expense categories.
 *
 * WHY predefined enum vs. user-created categories?
 * - Simpler to implement and query
 * - Enables consistent analytics/charts across all users
 * - Splitwise itself uses predefined categories
 * - Alternative: A separate Category entity with user-created categories
 *   (more flexible but adds complexity for minimal benefit)
 */
public enum ExpenseCategory {
    FOOD,
    TRANSPORT,
    RENT,
    UTILITIES,
    ENTERTAINMENT,
    SHOPPING,
    HEALTHCARE,
    EDUCATION,
    TRAVEL,
    GROCERIES,
    SUBSCRIPTIONS,
    OTHER
}
