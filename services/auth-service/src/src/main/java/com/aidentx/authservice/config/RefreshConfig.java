package com.aidentx.authservice.config;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to ensure refresh scope is properly configured.
 * This enables dynamic configuration updates via the /actuator/refresh endpoint.
 */
@Configuration
@RefreshScope
public class RefreshConfig {
    // This class ensures that the refresh scope is properly configured
    // and that the application can handle configuration updates at runtime
} 