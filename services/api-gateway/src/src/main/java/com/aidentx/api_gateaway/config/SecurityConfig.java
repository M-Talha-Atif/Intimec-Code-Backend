package com.aidentx.api_gateaway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Security configuration for the API Gateway.
 * 
 * Configures which routes require authentication and which are public.
 * This configuration allows public access to authentication endpoints while
 * delegating protection of sensitive resources to gateway filters.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Configures the security filter chain for the API Gateway.
     * 
     * Defines which endpoints are publicly accessible and which require authentication.
     * The actual authentication for protected resources is handled by custom gateway filters.
     * 
     * @param http the ServerHttpSecurity to configure
     * @return the configured SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        // Allow authentication endpoints
                        .pathMatchers("/api/v1/auth/**").permitAll()
                        // Allow logs endpoints
                        .pathMatchers("/api/v1/logs/**").permitAll()
                        // Allow configuration endpoints
                        .pathMatchers("/api/v1/configurations/**").permitAll()
                        // Allow actuator endpoints for health checks
                        .pathMatchers("/actuator/**").permitAll()
                        // All other requests need authentication (handled by filters)
                        .anyExchange().permitAll()
                )
                .build();
    }
}
