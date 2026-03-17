package com.splitwise.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Generic wrapper for simple API responses (success/failure messages).
 * Used for operations that don't return a specific resource (e.g., "User deleted").
 */
@Data
@AllArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
}
