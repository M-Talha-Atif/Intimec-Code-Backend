package com.aidentx.applicationpermissions.shared.security;

/**
 * Application constants for JWT Security operations. Contains all constant
 * values to avoid hardcoding throughout the JWT security components.
 */
public class AppConstants {

    // ===================================================================
    // JWT ERROR MESSAGES
    // ===================================================================
    public static final String MSG_INVALID_TOKEN_USERNAME = "Invalid token: unable to extract username";
    public static final String MSG_INVALID_TOKEN_EXPIRATION = "Invalid token: unable to extract expiration";
    public static final String MSG_INVALID_TOKEN_PARSING = "Invalid token: parsing failed";
    public static final String LOG_JWT_SECRET_USED = "Using JWT secret for validation";
    public static final String LOG_JWT_CLAIMS_EXTRACTED = "Token claims extracted successfully";

    // ===================================================================
    // JWT LOG MESSAGES
    // ===================================================================
    public static final String LOG_FAILED_EXTRACT_USERNAME = "Failed to extract username from token";
    public static final String LOG_FAILED_EXTRACT_EXPIRATION = "Failed to extract expiration from token";
    public static final String LOG_FAILED_PARSE_TOKEN = "Failed to parse JWT token";
    public static final String LOG_TOKEN_EXPIRATION_CHECK_FAILED = "Could not determine token expiration, treating as expired";
    public static final String LOG_TOKEN_VALIDATION_NULL_EMPTY = "Token validation failed: token is null or empty";
    public static final String LOG_TOKEN_VALIDATION_EXPIRED = "Token validation failed: token is expired";
    public static final String LOG_TOKEN_VALIDATION_SUCCESS = "Token validation successful";
    public static final String LOG_TOKEN_VALIDATION_FAILED = "Token validation failed";
    public static final String LOG_TOKEN_USERNAME_MISMATCH = "Token validation failed: username mismatch";
    public static final String LOG_TOKEN_VALIDATION_USER_FAILED = "Token validation failed for user";
    public static final String INVALID_TOKEN_CANNOT_EXTRACT_USER_ID = "Invalid token - cannot extract user ID";
    public static final String FAILED_TO_EXTRACT_USER_ID = "Failed to extract user ID from JWT token";
    public static final String TENANT_ID_MISSING = "JWT token does not contain tenantId claim";
    public static final String TENANT_EXTRACTION_FAILED = "Failed to extract tenantId from JWT token";

    // ===================================================================
    // APPLICATION PERMISSIONS SERVICE LOG MESSAGES
    // ===================================================================
    public static final String LOG_PERMISSION_CHECK_STARTED = "Permission check started for user";
    public static final String LOG_PERMISSION_CHECK_COMPLETED = "Permission check completed for user";
    public static final String LOG_ROLE_ASSIGNMENT_STARTED = "Role assignment started for user";
    public static final String LOG_ROLE_ASSIGNMENT_COMPLETED = "Role assignment completed for user";
    public static final String LOG_ORGANIZATION_ACCESS_CHECK = "Organization access check for user";

    // ===================================================================
    // APPLICATION PERMISSIONS SERVICE ERROR MESSAGES
    // ===================================================================
    public static final String ERROR_PERMISSION_CHECK_FAILED = "Permission check failed for user ";
    public static final String ERROR_ROLE_NOT_FOUND = "Role not found for roleId: ";
    public static final String ERROR_USER_NOT_FOUND = "User not found for userId: ";
    public static final String ERROR_ORGANIZATION_ACCESS_DENIED = "Organization access denied for user ";
    public static final String ERROR_INVALID_PERMISSION_REQUEST = "Invalid permission request";
    public static final String ERROR_ROLE_ASSIGNMENT_FAILED = "Role assignment failed for user ";

    // ===================================================================
    // APPLICATION PERMISSIONS SERVICE CONSTANTS
    // ===================================================================
    public static final String LOGGER_NAME_PERMISSIONS_SERVICE = "ApplicationPermissionsService";
    public static final String PERMISSION_ACTION_CREATE = "CREATE";
    public static final String PERMISSION_ACTION_READ = "READ";
    public static final String PERMISSION_ACTION_UPDATE = "UPDATE";
    public static final String PERMISSION_ACTION_DELETE = "DELETE";
    public static final String PERMISSION_ACTION_APPROVE = "APPROVE";
    public static final String PERMISSION_ACTION_REJECT = "REJECT";

    private AppConstants() {
        throw new IllegalStateException("Utility class");
    }
}