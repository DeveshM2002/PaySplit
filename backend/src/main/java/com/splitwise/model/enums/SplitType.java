package com.splitwise.model.enums;

/**
 * Defines how an expense is split among participants.
 *
 * EQUAL: Total divided equally (most common — ~80% of real Splitwise usage)
 * EXACT: Each person's share specified manually
 * PERCENTAGE: Each person pays a percentage of the total
 *
 * WHY an enum and not a String?
 * - Type safety: compiler catches typos ("EQUL" would be a compile error, not a runtime bug)
 * - JPA stores it as a string in DB via @Enumerated(EnumType.STRING)
 * - Easy to add new types later without breaking existing code
 */
public enum SplitType {
    EQUAL,
    EXACT,
    PERCENTAGE
}
