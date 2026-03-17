package com.splitwise.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 *
 * WHY DO WE NEED THIS?
 * Our React frontend runs on localhost:5173, backend on localhost:8080.
 * Browsers block requests between different origins (port = different origin) by default.
 * CORS headers tell the browser: "Yes, localhost:5173 is allowed to call me."
 *
 * Without this, every API call from React would fail with a CORS error.
 *
 * WHY configured in Java instead of application.yml?
 * - More control (can add dynamic origins, environment-based logic)
 * - application.yml only supports basic CORS config
 * - Alternative: Configure CORS per-controller with @CrossOrigin — doesn't scale
 *   if you have 20 controllers
 *
 * SECURITY NOTE: In production, replace "http://localhost:5173" with your actual domain.
 * Never use "*" (all origins) in production — it's a security risk.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setExposedHeaders(List.of("Authorization"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
