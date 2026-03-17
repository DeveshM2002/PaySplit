package com.splitwise.model.enums;

/**
 * Types of activities logged in the activity feed.
 * Each action a user takes generates an activity record for the feed.
 */
public enum ActivityType {
    EXPENSE_ADDED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,
    SETTLEMENT_ADDED,
    GROUP_CREATED,
    GROUP_UPDATED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    COMMENT_ADDED
}
