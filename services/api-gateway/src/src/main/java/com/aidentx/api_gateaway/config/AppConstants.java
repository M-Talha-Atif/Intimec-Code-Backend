package com.aidentx.api_gateaway.config;

import org.springframework.http.MediaType;

/**
 * Application-wide constants for the API Gateway.
 *
 * <p>
 * This class contains all constant values used throughout the API Gateway
 * application,
 * including error messages, HTTP headers, logger categories, and configuration
 * values.
 */
public final class AppConstants {

    private AppConstants() {
        // Private constructor to prevent instantiation of this utility class.
    }

    public static final String LOG_LEVEL_UPDATE_SUCCESS = "Log level updated for %s";
    // Logger Categories
    public static final String LOG_CATEGORY_AUTH_FILTER = "Gateway-AuthFilter";
    public static final String LOG_CATEGORY_JWT_UTIL = "Gateway-JwtUtil";
    public static final String LOG_CATEGORY_CORRELATION_FILTER = "Gateway-CorrelationFilter";
    public static final String LOG_CATEGORY_GATEWAY_FILTER = "Gateway-Filter";

    // HTTP Header Constants
    public static final String HEADER_X_USER = "X-User";
    // conventional headers the API Gateway would add after validating
    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String BEARER_TOKEN_PREFIX = "Bearer ";
    public static final int BEARER_TOKEN_PREFIX_LENGTH = BEARER_TOKEN_PREFIX.length();
    public static final String CONTENT_TYPE_JSON = MediaType.APPLICATION_JSON_VALUE;

    // Error Messages for API Responses & Exceptions
    public static final String MSG_MISSING_AUTHORIZATION_HEADER = "Missing Authorization header";
    public static final String MSG_INVALID_AUTHORIZATION_HEADER_FORMAT = "Invalid Authorization header format. Expected 'Bearer <token>'";
    public static final String MSG_INVALID_OR_EXPIRED_TOKEN = "Invalid or expired JWT token";
    public static final String MSG_TOKEN_VALIDATION_FAILED = "Token validation failed";
    public static final String MSG_PREFIX_VALIDATION_FAILED = "Prefix for correlation ID cannot be null or empty.";

    // Log Messages
    public static final String LOG_SECURED_ROUTE_ACCESS = "Accessing secured route";
    public static final String LOG_TOKEN_VALIDATION_SUCCESS = "JWT token validated successfully";
    public static final String LOG_TOKEN_VALIDATION_FAILURE = "JWT token validation failed";
    public static final String LOG_CLAIMS_EXTRACTION_FAILURE = "Failed to extract claims from JWT token";
    public static final String LOG_CORRELATION_ID_GENERATED = "Generated new correlation ID";
    public static final String LOG_CORRELATION_ID_RECEIVED = "Received correlation ID from request header";
    public static final String LOG_CORRELATION_ID_SET = "Set correlation ID in response header and context";

    // Permission Filter Constants
    public static final String LOG_PERMISSION_CHECK_STARTED = "Starting permission check for request";
    public static final String LOG_PERMISSION_CHECK_SKIPPED = "Permission check skipped for public endpoint";
    public static final String LOG_ENDPOINT_NOT_MANAGED = "Endpoint not managed by permission system, allowing access";
    public static final String LOG_CHECKING_MANAGED_ENDPOINT = "Checking permission for managed endpoint";
    public static final String LOG_PERMISSION_DENIED = "Access denied: User does not have the required role or permissions to perform this action";
    public static final String LOG_PERMISSION_GRANTED = "Permission granted for user";
    public static final String LOG_PERMISSION_CHECK_ERROR = "Error during permission check";
    public static final String LOG_JWT_EXTRACTION_START = "Starting JWT extraction";
    public static final String LOG_JWT_USER_INFO_EXTRACTED = "JWT user information extracted";
    public static final String LOG_JWT_TOKEN_PROCESSING_ERROR = "JWT token processing error during permission check";
    public static final String LOG_TOKEN_EXTRACTION_PUBLIC_ENDPOINT = "Token extraction error for public endpoint, allowing access";
    public static final String LOG_AUTHENTICATION_FAILED = "Authentication failed: Missing or invalid token";
    public static final String LOG_NO_AUTH_HEADER = "No Authorization header found for permission check";
    public static final String USER_INFO_EXTRACTED = "User info extracted from JWT token";
    public static final String CHECKING_ENDPOINT_MANAGED = "Checking if endpoint is managed by permission system";
    public static final String INIT_PERMISSION_CHECK = "Initiating permission check via gRPC";
    public static final String LOG_GRPC_REQUEST_BUILDING = "Building gRPC request for endpoint check";
    public static final String CALLING_GRPC_CHECK_ENDPOINT = "Calling gRPC checkEndpointExists";
    public static final String LOG_ENDPOINT_MANAGEMENT_CHECK = "Endpoint management check result";
    public static final String LOG_GRPC_ENDPOINT_CHECK_FAILED = "gRPC endpoint check failed, assuming endpoint is not managed";
    public static final String LOG_GRPC_PERMISSION_CHECK_FAILED = "gRPC permission check failed - denying access for safety";
    public static final String LOG_GRPC_PERMISSION_REQUEST_BUILDING = "Building gRPC permission check request";
    public static final String LOG_GRPC_PERMISSION_CALL = "Calling gRPC checkPermission";
    public static final String LOG_GRPC_PERMISSION_RESPONSE = "Received gRPC permission check response";
    public static final String LOG_GRPC_PERMISSION_ERROR = "Error during gRPC permission check";
    
    // Error Messages for Permission Filter
    public static final String ERROR_MISSING_AUTH_HEADER = "Authentication required: Please provide valid credentials";
    public static final String ERROR_ACCESS_DENIED = "Access denied: Your current role does not have permission to perform this action. Please contact your administrator to request access.";
    public static final String ERROR_INTERNAL_PERMISSION_CHECK = "Internal error during permission check";
    
    // Object Types for Logging
    public static final String OBJECT_TYPE_PERMISSION_FILTER = "PermissionFilter";
    public static final String OBJECT_TYPE_JWT_EXTRACTION = "JwtExtraction";
    public static final String OBJECT_TYPE_ENDPOINT_CHECK = "EndpointCheck";
    public static final String OBJECT_TYPE_GRPC_CALL = "GrpcCall";
    
    // Detail Keys for Logging
    public static final String DETAIL_METHOD = "method";
    public static final String DETAIL_REQUEST_URI = "requestUri";
    public static final String DETAIL_AUTH_HEADER = "authHeader";
    public static final String DETAIL_JWT_TOKEN_LENGTH = "tokenLength";
    public static final String DETAIL_USER_ID = "userId";
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_IS_MANAGED = "isManaged";
    public static final String DETAIL_API_ENDPOINT = "apiEndpoint";
    public static final String DETAIL_HTTP_METHOD = "httpMethod";
    public static final String DETAIL_HAS_PERMISSION = "hasPermission";
    public static final String DETAIL_ERROR = "error";

    // Response Format Templates
    public static final String ERROR_RESPONSE_TEMPLATE = "{\"status\": %d, \"error\": \"%s\", \"message\": \"%s\"}";
}