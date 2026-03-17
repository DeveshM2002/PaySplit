package com.splitwise.config;

import com.splitwise.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Central Spring Security configuration.
 *
 * KEY DECISIONS EXPLAINED:
 *
 * 1. WHY SessionCreationPolicy.STATELESS?
 *    We're using JWT, which is stateless. The server doesn't need to store session data.
 *    Without this, Spring Security creates an HttpSession for every request (wasted memory).
 *
 * 2. WHY disable CSRF?
 *    CSRF protection is for cookie-based auth (session-based apps).
 *    Since we use JWT in Authorization headers (not cookies), CSRF attacks are impossible.
 *    The browser doesn't automatically send Authorization headers like it does cookies.
 *
 * 3. WHY BCrypt for password hashing?
 *    - BCrypt is intentionally slow (configurable work factor), making brute-force hard
 *    - It includes a built-in salt (prevents rainbow table attacks)
 *    - Alternative: Argon2 (newer, more resistant to GPU attacks) — Spring supports it
 *      but BCrypt is battle-tested and sufficient for our needs
 *    - Alternative: PBKDF2 — also good but slower to verify than BCrypt
 *    - NEVER use MD5 or SHA-256 for passwords (they're fast = easy to brute-force)
 *
 * 4. WHY @EnableMethodSecurity?
 *    Allows us to use @PreAuthorize("hasRole('ADMIN')") on individual methods
 *    for fine-grained access control. We might need it later.
 *
 * 5. WHY the filter order matters?
 *    Our JwtAuthenticationFilter must run BEFORE UsernamePasswordAuthenticationFilter.
 *    If it ran after, the default filter would reject the request before we check the JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
