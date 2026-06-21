package com.aidentx.core.config;

import org.springframework.context.annotation.Configuration;

/**
 * Security configuration for the core service.
 * 
 * <p>Note: Permission checking has been moved to the API Gateway level.
 * All requests to this service are expected to come through the API Gateway,
 * which handles authentication and authorization centrally.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class SecurityConfig {
    // Permission filtering is now handled at API Gateway level
    // No local filters needed
}
