package com.splitwise.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (User, Group, Expense) doesn't exist.
 *
 * @ResponseStatus(NOT_FOUND) tells Spring to return HTTP 404 automatically.
 *
 * WHY a custom exception instead of returning null?
 * - Null returns force every caller to check for null (fragile, easy to forget)
 * - Exceptions provide a clear, consistent error response to the client
 * - The GlobalExceptionHandler catches this and formats a proper JSON error response
 * - This follows the "fail fast" principle — errors are caught immediately
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
