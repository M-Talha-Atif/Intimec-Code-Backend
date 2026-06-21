package com.aidentx.core.shared.security;

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
    // PROVISIONING SERVICE LOG MESSAGES
    // ===================================================================
    public static final String LOG_PROVISIONING_EVENT_RECEIVED = "Provisioning event received.";
    public static final String LOG_FAILED_PROCESS_LIFECYCLE_EVENT = "Failed to process lifecycle event.";
    public static final String LOG_FAILED_PROVISION_ACCOUNT_JOINER = "Failed to provision account during Joiner process.";
    public static final String LOG_FAILED_PROCESS_UPDATE_MOVER = "Failed to process update during Mover process.";
    public static final String LOG_FAILED_DEPROVISION_ACCOUNT_LEAVER = "Failed to deprovision account during Leaver process.";
    public static final String LOG_NO_ATTRIBUTE_CHANGES_SKIP_UPDATE = "No attribute changes based on policy. Skipping update action.";
    public static final String LOG_FAILED_FETCH_PROVISIONING_POLICY = "Failed to fetch provisioning policy from iam-service.";
    public static final String LOG_BIRTHRIGHT_PLACEHOLDER_WARNING = "getBirthrightResourcesForIdentity is using placeholder logic and needs to be refactored.";

    // ===================================================================
    // PROVISIONING SERVICE ERROR MESSAGES
    // ===================================================================
    public static final String ERROR_PROVISIONING_FAILED = "Provisioning failed for identity ";
    public static final String ERROR_DELETION_FAILED = "Deletion failed for account ";
    public static final String ERROR_APPLICATION_NOT_FOUND = "Application not found for resourceId: ";
    public static final String ERROR_CONNECTOR_ID_NOT_FOUND = "Connector ID not found in configuration for application: ";
    public static final String ERROR_FAILED_FETCH_PROVISIONING_POLICY = "Failed to fetch provisioning policy";
    public static final String ERROR_PROTO_TO_JSON_CONVERSION = "Error during proto message to JsonNode conversion";
    public static final String ERROR_JSON_TO_STRUCT_CONVERSION = "Error during JsonNode to Struct conversion";
    public static final String ERROR_MAP_TO_STRUCT_CONVERSION = "Error during Map to Struct conversion";

    // ===================================================================
    // PROVISIONING SERVICE DETAIL KEYS
    // ===================================================================
    public static final String DETAIL_IDENTITY_ID = "identityId";
    public static final String DETAIL_EVENT_TYPE = "eventType";
    public static final String DETAIL_EVENT_ID = "eventId";
    public static final String DETAIL_RESOURCE_ID = "resourceId";
    public static final String DETAIL_ACCOUNT_ID = "accountId";

    // ===================================================================
    // PROVISIONING SERVICE CONSTANTS
    // ===================================================================
    public static final String LOGGER_NAME_PROVISIONING_SERVICE = "ProvisioningService";
    public static final String OBJECT_CLASS_ACCOUNT = "__ACCOUNT__";
    public static final String LIFECYCLE_ACTION_CREATE = "CREATE";
    public static final String LIFECYCLE_ACTION_UPDATE = "UPDATE";
    public static final String LIFECYCLE_ACTION_DELETE = "DELETE";
    public static final String LIFECYCLE_ACTION_DISABLE = "DISABLE";
    public static final String CONFIG_CONNECTOR_ID = "connectorId";

    // ===================================================================
    // PERMISSION FILTER LOG MESSAGES
    // ===================================================================
    public static final String LOG_PERMISSION_CHECK_STARTED = "Starting permission check";
    public static final String LOG_PERMISSION_CHECK_SKIPPED = "Skipping permission check for excluded path";
    public static final String LOG_ENDPOINT_NOT_MANAGED = "Endpoint not managed by permission system, allowing access";
    public static final String LOG_CHECKING_MANAGED_ENDPOINT = "Checking permission for managed endpoint";
    public static final String LOG_PERMISSION_DENIED = "Permission denied";
    public static final String LOG_PERMISSION_GRANTED = "Permission granted";
    public static final String LOG_PERMISSION_CHECK_ERROR = "Error during permission check";
    public static final String LOG_ENDPOINT_MANAGEMENT_CHECK = "Endpoint management check result";
    public static final String LOG_GRPC_ENDPOINT_CHECK_FAILED = "Failed to check if endpoint is managed, allowing access";
    public static final String LOG_GRPC_PERMISSION_CALL = "Calling permission service via gRPC";
    public static final String LOG_GRPC_PERMISSION_RESPONSE = "Permission service response";
    public static final String LOG_GRPC_PERMISSION_ERROR = "Error calling permission service via gRPC";
    public static final String LOG_JWT_EXTRACTION_START = "Extracting JWT token from Authorization header";
    public static final String LOG_JWT_USER_INFO_EXTRACTED = "User information extracted from JWT";
    public static final String LOG_GRPC_REQUEST_BUILDING = "Building gRPC request for endpoint check";
    public static final String LOG_GRPC_PERMISSION_REQUEST_BUILDING = "Building gRPC request for permission check";
    
    // ===================================================================
    // PERMISSION FILTER ERROR MESSAGES
    // ===================================================================
    public static final String ERROR_MISSING_AUTH_HEADER = "Authorization header is missing or invalid";
    public static final String ERROR_ACCESS_DENIED = "Access denied: insufficient permissions for this action";
    public static final String ERROR_INTERNAL_PERMISSION_CHECK = "Internal server error during permission check";
    
    // ===================================================================
    // PERMISSION FILTER DETAIL KEYS
    // ===================================================================
    public static final String DETAIL_METHOD = "method";
    public static final String DETAIL_REQUEST_URI = "requestURI";
    public static final String DETAIL_USER_ID = "userId";
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_API_ENDPOINT = "apiEndpoint";
    public static final String DETAIL_HTTP_METHOD = "httpMethod";
    public static final String DETAIL_IS_MANAGED = "isManaged";
    public static final String DETAIL_HAS_PERMISSION = "hasPermission";
    public static final String DETAIL_ERROR = "error";
    public static final String DETAIL_AUTH_HEADER = "authHeader";
    public static final String DETAIL_JWT_TOKEN_LENGTH = "jwtTokenLength";
    
    // ===================================================================
    // PERMISSION FILTER OBJECT TYPES
    // ===================================================================
    public static final String OBJECT_TYPE_PERMISSION_FILTER = "PermissionFilter";
    public static final String OBJECT_TYPE_ENDPOINT_CHECK = "EndpointCheck";
    public static final String OBJECT_TYPE_GRPC_CALL = "GrpcCall";
    public static final String OBJECT_TYPE_JWT_EXTRACTION = "JwtExtraction";
    public static final String USER_INFO_EXTRACTED = "User information successfully extracted from JWT";
    public static final String CHECKING_ENDPOINT_MANAGED = "Checking if endpoint is managed by permission system";
    public static final String INIT_PERMISSION_CHECK = "Initiating permission check via gRPC";
    public static final String CALLING_GRPC_CHECK_ENDPOINT = "Calling gRPC checkEndpointExists";

    private AppConstants() {
        throw new IllegalStateException("Utility class");
    }
}