package com.splitwise.service;

import com.splitwise.dto.request.LoginRequest;
import com.splitwise.dto.request.SignupRequest;
import com.splitwise.dto.response.AuthResponse;
import com.splitwise.dto.response.UserResponse;
import com.splitwise.exception.BadRequestException;
import com.splitwise.mapper.EntityMapper;
import com.splitwise.model.User;
import com.splitwise.repository.UserRepository;
import com.splitwise.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WHY Service layer between Controller and Repository?
 *
 * Controllers handle HTTP concerns (request parsing, status codes).
 * Repositories handle data access.
 * Services handle BUSINESS LOGIC — the actual rules of your application.
 *
 * Example: "A user can't register with an already-used email" is a business rule.
 * It doesn't belong in the Controller (mixing HTTP with logic) or the Repository
 * (repositories should only do CRUD, not enforce rules).
 *
 * WHY @Transactional?
 * Signup involves: check if email exists → save user. These must be atomic.
 * Without @Transactional, if the save fails after the check, we'd have an inconsistent state.
 * @Transactional ensures either EVERYTHING succeeds or EVERYTHING rolls back.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final EntityMapper entityMapper;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        user = userRepository.save(user);

        String accessToken = tokenProvider.generateTokenFromUserId(user.getId());
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());
        UserResponse userResponse = entityMapper.toUserResponse(user);

        return new AuthResponse(accessToken, refreshToken, userResponse);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmail(request.getEmail().toLowerCase())
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        String accessToken = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(user.getId());
        UserResponse userResponse = entityMapper.toUserResponse(user);

        return new AuthResponse(accessToken, refreshToken, userResponse);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        Long userId = tokenProvider.getUserIdFromToken(refreshToken);
        String newAccessToken = tokenProvider.generateTokenFromUserId(userId);
        String newRefreshToken = tokenProvider.generateRefreshToken(userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User not found"));

        return new AuthResponse(newAccessToken, newRefreshToken, entityMapper.toUserResponse(user));
    }
}
