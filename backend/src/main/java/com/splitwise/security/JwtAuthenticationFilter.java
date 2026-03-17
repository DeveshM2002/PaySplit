package com.splitwise.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — runs on EVERY incoming HTTP request.
 *
 * HOW IT WORKS:
 * 1. Extract the "Authorization: Bearer <token>" header
 * 2. Validate the token (signature, expiration)
 * 3. Extract the userId from the token
 * 4. Load the user from the database
 * 5. Set the user in Spring Security's SecurityContext
 * 6. Continue the filter chain — the controller now has access to the authenticated user
 *
 * WHY OncePerRequestFilter (not GenericFilterBean)?
 * - OncePerRequestFilter guarantees it runs exactly ONCE per request
 * - In some setups (like forwards/includes), filters can run multiple times
 * - OncePerRequestFilter prevents that — it's a safety net
 *
 * WHY we don't throw an exception for invalid tokens:
 * - Some endpoints are public (login, register, swagger)
 * - If the token is missing/invalid, we just don't set the SecurityContext
 * - Spring Security will then check if the endpoint requires authentication
 * - If it does and there's no SecurityContext, it returns 401 automatically
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Long userId = tokenProvider.getUserIdFromToken(jwt);
                UserDetails userDetails = customUserDetailsService.loadUserById(userId);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());

                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
