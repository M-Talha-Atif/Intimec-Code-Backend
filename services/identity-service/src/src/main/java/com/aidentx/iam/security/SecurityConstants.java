package com.aidentx.iam.security;

/**
 * Security constants for the identities service.
 * Contains all constant values to avoid hardcoding throughout the security
 * components.
 */
public class SecurityConstants {

    // Logger Categories
    public static final String LOG_CATEGORY_SECURITY_AUTH = "Security-Auth";
    public static final String LOG_CATEGORY_SECURITY_JWT = "Security-JWT";

    // HTTP Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String USER_HEADER = "X-User";

    // Public Endpoints (no authentication required)
    public static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/**",
            "/health",
            "/health/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/test/**",
            "/api/v1/logs/level",         // Internal log level read -- core service polling
            "/api/v1/logs/level/**",       // Internal log level write -- core service updates
            "/api/v1/internal/**"          // Internal cross-service calls (e.g. cascade application purge)
    };

    // API Endpoints (authentication required)
    public static final String API_PATH_PATTERN = "/api/**";

    // Error Messages for API Responses/Exceptions
    public static final String MSG_AUTHENTICATION_REQUIRED = "Authentication required to access this resource";
    public static final String MSG_AUTHENTICATION_FAILED = "Authentication failed";
    public static final String MSG_INVALID_TOKEN = "Invalid or expired token";
    public static final String LOG_JWT_ATTEMPT_VALIDATION = "Attempting to validate JWT token";
    public static final String LOG_JWT_VALIDATION_SUCCESS = "Successfully extracted user ID from token";
    public static final String LOG_JWT_VALIDATION_FAILED = "JWT token validation failed";

    // Log Messages
    public static final String LOG_UNAUTHORIZED_ACCESS = "Unauthorized access attempt";
    public static final String LOG_AUTHENTICATION_FILTER_ERROR = "Authentication failed during JWT filter processing";
    public static final String LOG_USER_AUTHENTICATED_VIA_GATEWAY = "User authenticated via API Gateway";
    public static final String LOG_USER_AUTHENTICATED_VIA_TOKEN = "User authenticated via JWT token";
    public static final String LOG_TOKEN_VALIDATION_FAILED = "JWT token validation failed";
    public static final String LOG_AUTHENTICATION_SET = "Authentication set for user";

    // Security Configuration
    public static final String JWT_FILTER_BEAN_NAME = "identitiesJwtAuthenticationFilter";

    private SecurityConstants() {
        // Private constructor to prevent instantiation
    }
}
