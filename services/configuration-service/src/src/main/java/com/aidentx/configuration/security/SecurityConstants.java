package com.aidentx.configuration.security;

/**
 * Security-related constants for the configuration service.
 */
public final class SecurityConstants {

    private SecurityConstants() {
        // Utility class
    }

    // Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String USER_HEADER = "X-User";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String HEADER_USER_ID = "x-user-id";
    public static final String HEADER_USERNAME = "x-username";
    public static final String HEADER_IP_ADDRESS = "x-ip-address";
    public static final String HEADER_TENANT_ID = "X-Tenant-ID"; // Bean names

    public static final String JWT_FILTER_BEAN_NAME = "configurationJwtAuthenticationFilter";

    // Endpoints
    public static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/**",
            "/health/**",
            "/info/**",
            "/metrics/**",
            "/api/v1/logs/level",         // Internal log level read -- core service polling
            "/api/v1/logs/level/**"        // Internal log level write -- core service updates
    };

    public static final String API_PATH_PATTERN = "/api/**";

    // Specific public endpoints for configuration service
    public static final String[] CONFIGURATION_PUBLIC_ENDPOINTS = {
            "/api/v1/configurations/ui_customization/theme",
            "/api/v1/artifacts/ui_customization/theme/artifacts/logoDark/file",
            "/api/v1/artifacts/ui_customization/theme/artifacts/logoLight/file",
            "/api/v1/artifacts/ui_customization/theme/artifacts/favicon/file",
            "/api/v1/artifacts/ui_customization/theme/artifacts/loginBackground/file",
            "/api/v1/artifacts/ui_customization/theme/artifacts/loginBackgroundDark/file"
    };
}
