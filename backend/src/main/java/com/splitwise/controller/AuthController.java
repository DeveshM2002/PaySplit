package com.splitwise.controller;

import com.splitwise.dto.request.LoginRequest;
import com.splitwise.dto.request.SignupRequest;
import com.splitwise.dto.response.AuthResponse;
import com.splitwise.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Authentication endpoints — these are PUBLIC (no JWT required).
 *
 * WHY /api/auth/ prefix?
 * - /api/ distinguishes API routes from static files
 * - /auth/ groups all authentication endpoints
 * - This makes SecurityConfig rules cleaner: permitAll on "/api/auth/**"
 *
 * WHY @Valid on request body?
 * Triggers bean validation (the @NotBlank, @Email annotations on DTOs).
 * Without @Valid, those annotations are ignored and invalid data gets through.
 * This is a common Spring Boot gotcha!
 *
 * WHY ResponseEntity instead of returning the object directly?
 * ResponseEntity gives us control over HTTP status codes:
 * - 201 CREATED for signup (resource was created)
 * - 200 OK for login (no new resource)
 * Returning the object directly always returns 200. Not RESTful.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration and login")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        AuthResponse response = authService.signup(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }
}
