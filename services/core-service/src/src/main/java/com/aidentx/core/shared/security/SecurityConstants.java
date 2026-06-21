package com.aidentx.core.shared.security;

/**
 * Security constants for the shared security module. Contains all constant
 * values to avoid hardcoding throughout the security components.
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
        "/test/**"
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
    public static final String LOG_CLEARING_USER_CONTEXT = "Clearing user context after request completion";
    public static final String USER_CONTEXT_SET = "User context set for gRPC calls";
    public static final String USER_CONTEXT_VERIFIED = "User context verification successful";
    public static final String USER_CONTEXT_FAILED = "User context verification failed - context is null";
    public static final String USER_CONTEXT_CLEARING = "Clearing user context after request completion";
    public static final String USER_CONTEXT_SET_FAILED = "Failed to set user context: ";
    // Security Configuration
    public static final String JWT_FILTER_BEAN_NAME = "sharedJwtAuthenticationFilter";

    // ===== gRPC Client Interceptor =====
    public static final String GRPC_INTERCEPTOR_INSTANTIATED = "GrpcClientContextInterceptor instantiated!";
    public static final String GRPC_INTERCEPT_CALL = "GrpcClientContextInterceptor.interceptCall() called for method: {}";
    public static final String GRPC_START_CALL = "GrpcClientContextInterceptor.start() called for method: {}";
    public static final String GRPC_USER_CONTEXT = "GrpcClientContextInterceptor: UserContext = {}";
    public static final String GRPC_USER_ID_MISSING = "GrpcClientContextInterceptor: userId missing or blank; header not added";
    public static final String GRPC_USERNAME_MISSING = "GrpcClientContextInterceptor: username missing or blank; header not added";
    public static final String GRPC_IP_MISSING = "GrpcClientContextInterceptor: ipAddress missing or blank; header not added";
    public static final String GRPC_HEADERS_ADDED = "GrpcClientContextInterceptor: Added headers - userId: {}, username: {}, ip: {}";
    public static final String GRPC_NO_USER_CONTEXT = "GrpcClientContextInterceptor: No user context found!";

    private SecurityConstants() {
        // Private constructor to prevent instantiation
    }
}
