package com.splitwise.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Custom annotation to inject the currently authenticated user into controller methods.
 *
 * Usage: public ResponseEntity<?> getProfile(@CurrentUser UserPrincipal user) { ... }
 *
 * WHY a custom annotation instead of using @AuthenticationPrincipal directly?
 * - @AuthenticationPrincipal is verbose and Spring-specific
 * - @CurrentUser is shorter, self-documenting, and hides the framework detail
 * - If we ever change from Spring Security to something else, we only change
 *   this annotation's implementation, not every controller method
 * - This is the Adapter Pattern applied to annotations
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
