package com.aidentx.core.audit_management.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * Configuration class for setting up Cross-Origin Resource Sharing (CORS).
 * <p>
 * This allows the frontend (typically running on a different origin) to communicate
 * with the backend APIs by explicitly allowing cross-origin requests.
 */
@Configuration
public class CorsConfig {

    /**
     * The base URL of the frontend application allowed to access this backend.
     * <p>
     * This is injected from the {@code application.properties} or {@code application.yml}
     * file using the {@code front.url} property.
     */
    @Value("${front.url}")
    private String frontUrl;

    /**
     * Defines and registers a CORS filter bean to handle cross-origin requests.
     * <p>
     * This configuration allows requests from the specified frontend URL with support
     * for credentials (e.g., cookies, authorization headers), and permits all headers and
     * common HTTP methods.
     *
     * @return a configured {@link CorsFilter} that applies the CORS rules
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);  
        config.setAllowedOrigins(List.of(frontUrl)); 
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")); 

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); 

        return new CorsFilter(source);
    }
}
