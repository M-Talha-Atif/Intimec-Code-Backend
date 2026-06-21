package com.aidentx.applicationpermissions.shared;

/**
 * Shared application constants for the Application Permissions Service.
 * 
 * <p>Contains commonly used constants, error messages, and configuration values
 * that are shared across different components of the application.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public final class SharedAppConstants {

    // Private constructor to prevent instantiation
    private SharedAppConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    // JSON Conversion Error Messages
    public static final String JSONNODE_TO_STRING_ERROR = "Failed to convert JsonNode to String";
    public static final String STRING_TO_JSONNODE_ERROR = "Failed to convert String to JsonNode";

    // General Error Messages
    public static final String ENTITY_NOT_FOUND = "Entity not found";
    public static final String INVALID_REQUEST = "Invalid request";
    public static final String ACCESS_DENIED = "Access denied";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";

    // Validation Error Messages
    public static final String REQUIRED_FIELD_MISSING = "Required field is missing";
    public static final String INVALID_FORMAT = "Invalid format";
    public static final String DUPLICATE_ENTRY = "Duplicate entry";

    // Success Messages
    public static final String OPERATION_SUCCESSFUL = "Operation completed successfully";
    public static final String ENTITY_CREATED = "Entity created successfully";
    public static final String ENTITY_UPDATED = "Entity updated successfully";
    public static final String ENTITY_DELETED = "Entity deleted successfully";

    // Request Context Messages
    public static final String MSG_NO_REQUEST_CONTEXT = "No request context available";
    public static final String MSG_NO_JWT_TOKEN = "No JWT token found in Authorization header";
    public static final String MSG_TENANT_ACCESS_DENIED = "Access denied. User tenant '%s' does not match required tenant '%s'";

    // Status Messages
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PARTIAL = "PARTIAL";

    // ===================================================================
    // GRPC SERVICE LOG MESSAGES
    // ===================================================================
    public static final String LOG_GRPC_PERMISSION_CHECK_RECEIVED = "gRPC permission check request received";
    public static final String LOG_GRPC_PERMISSION_CHECK_COMPLETED = "gRPC permission check completed";
    public static final String LOG_GRPC_PERMISSION_CHECK_ERROR = "Error processing gRPC permission check";
    public static final String LOG_GRPC_ENDPOINT_CHECK_RECEIVED = "gRPC endpoint existence check request received";
    public static final String LOG_GRPC_ENDPOINT_CHECK_COMPLETED = "gRPC endpoint existence check completed";
    public static final String LOG_GRPC_ENDPOINT_CHECK_ERROR = "Error processing gRPC endpoint existence check";

    // ===================================================================
    // GRPC SERVICE DETAIL KEYS
    // ===================================================================
    public static final String DETAIL_USER_ID = "userId";
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_API_ENDPOINT = "apiEndpoint";
    public static final String DETAIL_HTTP_METHOD = "httpMethod";
    public static final String DETAIL_ENDPOINT = "endpoint";
    public static final String DETAIL_HAS_PERMISSION = "hasPermission";
    public static final String DETAIL_IS_MANAGED = "isManaged";
    public static final String CALLING_PERMISSION_SERVICE = "Calling UserPermissionService.hasPermissionForEndpoint";
    public static final String BUILDING_PERMISSION_RESPONSE = "Building gRPC permission check response";
    public static final String CALLING_ENDPOINT_SERVICE = "Calling UserPermissionService.isEndpointManaged";
    public static final String BUILDING_ENDPOINT_RESPONSE = "Building gRPC endpoint check response";
    // ===================================================================
    // GRPC SERVICE OBJECT TYPES
    // ===================================================================
    public static final String OBJECT_TYPE_PERMISSION_GRPC_SERVICE = "PermissionGrpcService";
}