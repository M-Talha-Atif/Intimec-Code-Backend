package com.aidentx.core.shared.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Shared Security Configuration for the core-service module.
 * This configuration provides JWT-based authentication for all microservices
 * within the core-service module.
 * 
 * Production-ready with proper security settings and CSRF protection disabled
 * for stateless JWT authentication.
 */
@Configuration
@EnableWebSecurity
public class SharedSecurityConfig {

    @Autowired
    @Qualifier(SecurityConstants.JWT_FILTER_BEAN_NAME)
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Configures the security filter chain for JWT authentication.
     * 
     * @param http the HttpSecurity to configure
     * @return the configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for stateless JWT authentication
            .csrf(csrf -> csrf.disable())
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Allow public access to health and monitoring endpoints
                .requestMatchers(SecurityConstants.PUBLIC_ENDPOINTS).permitAll()
                
                // Require authentication for all API endpoints
                .requestMatchers(SecurityConstants.API_PATH_PATTERN).authenticated()
                
                // Any other request should be authenticated
                .anyRequest().authenticated()
            )
            
            // Configure session management for stateless JWT
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configure exception handling
            .exceptionHandling(exceptions -> 
                exceptions.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            
            // Add JWT authentication filter before UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}