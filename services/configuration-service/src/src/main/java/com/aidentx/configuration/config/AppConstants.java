package com.aidentx.configuration.config;

public final class AppConstants {

    private AppConstants() {
    }

    // ===================================================================
    // GENERAL & SHARED
    // ===================================================================
    public static final String CONFIG_SUBTYPE_CONFIG = "config";

    // --- DTO Validation Messages ---
    public static final String CONFIG_DATA_REQUIRED_MSG = "Configuration data is required";
    public static final String CONFIG_TYPE_REQUIRED_MSG = "Configuration type is required";

    // --- JsonNodeConverter ---
    public static final String JSONNODE_TO_STRING_ERROR = "Failed to convert JsonNode to String";
    public static final String STRING_TO_JSONNODE_ERROR = "Failed to convert String to JsonNode";

    // ===================================================================
    // SERVICE & CONTROLLER CONSTANTS
    // ===================================================================

    // --- Logger Categories ---
    public static final String LOG_CATEGORY = "config";

    // --- Log Detail Keys ---
    public static final String DETAIL_CONFIG_TYPE = "configType";
    public static final String DETAIL_CONFIG_KEY = "configKey";
    public static final String DETAIL_ARTIFACT_TYPE = "artifactType";
    public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";
    public static final String DETAIL_EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String DETAIL_STACK_TRACE = "stackTrace";
    public static final String DETAIL_COUNT = "count";
    public static final String DETAIL_EXISTS = "exists";
    public static final String DETAIL_NEW_VERSION = "newVersion";
    public static final String LOG_DETAIL_FILE_PATH = "filePath";
    public static final String LOG_DETAIL_ENCRYPTED = "encrypted";
    public static final String LOG_DETAIL_ARTIFACT = "artifact";

    // --- Method Names (for consistent logging) ---
    public static final String METHOD_GET_CONFIGURATION = "getConfiguration";
    public static final String METHOD_GET_CONFIGS_BY_TYPE = "getConfigurationsByType";
    public static final String METHOD_SAVE_CONFIGURATION = "saveConfiguration";
    public static final String METHOD_DELETE_CONFIGURATION = "deleteConfiguration";
    public static final String METHOD_CONFIGURATION_EXISTS = "configurationExists";
    public static final String METHOD_GET_SUPPORTED_TYPES = "getSupportedConfigurationTypes";
    public static final String METHOD_GET_RAW_DATA = "getRawConfigurationData";
    public static final String METHOD_PARTIAL_UPDATE = "partialUpdateConfiguration";

    // --- General Log Message Formats ---
    public static final String LOG_ENTERING_METHOD_FORMAT = "Entering %s";
    public static final String LOG_BAD_REQUEST_IN_METHOD_FORMAT = "Bad request in %s";
    public static final String LOG_ERROR_IN_METHOD_FORMAT = "Error in %s";

    // --- Specific Log & Error Messages ---
    public static final String LOG_SUCCESS_GET_CONFIG = "Successfully retrieved configuration";
    public static final String LOG_CONFIG_NOT_FOUND = "Configuration not found";
    public static final String LOG_SUCCESS_GET_BY_TYPE = "Successfully retrieved configurations by type";
    public static final String LOG_SUCCESS_GET_ALL = "Successfully retrieved all configurations";
    public static final String LOG_SUCCESS_SAVE_FORMAT = "Successfully saved configuration %s/%s";
    public static final String LOG_ERROR_SAVING_FORMAT = "Error saving configuration for %s/%s";
    public static final String LOG_SUCCESS_DELETE_FORMAT = "Successfully deleted configuration %s/%s";
    public static final String LOG_WARN_DELETE_NONEXISTENT_FORMAT = "Attempt to delete non-existent configuration: %s/%s";
    public static final String LOG_CONFIG_EXISTS_COMPLETE = "Configuration existence check complete";
    public static final String LOG_SUCCESS_GET_RAW = "Successfully retrieved raw configuration data";
    public static final String LOG_RAW_CONFIG_NOT_FOUND = "Raw configuration not found";
    public static final String LOG_ERROR_GET_RAW_FORMAT = "Error retrieving raw configuration data for %s/%s";
    public static final String LOG_SUCCESS_PATCH_FORMAT = "Successfully patched configuration %s/%s";
    public static final String LOG_ERROR_PATCHING_FORMAT = "Error patching configuration for %s/%s";
    public static final String CONFIG_ARTIFACT_FILE_NOT_FOUND = "Artifact file not found";

    // --- ConfigurationServiceImpl log and error messages ---
    public static final String LOG_PATCH_STARTED_FORMAT = "Patch operation started for config: %s/%s. Encrypt entire config: %b";
    public static final String LOG_DELETED_CONFIG_FORMAT = "Deleted configuration: %s/%s";
    public static final String LOG_SAVING_INLINE_CONFIG = "Saving inline configuration";
    public static final String LOG_SAVING_ARTIFACT_CONFIG = "Saving artifact configuration";
    public static final String ERROR_CONFIG_NOT_FOUND_FORMAT = "Configuration not found for type: %s and key: %s";
    public static final String ERROR_PATCH_DATA_PROCESSING_FAILED_FORMAT = "Failed to process patch data for config: %s/%s";
    public static final String ERROR_PATCH_UPDATE_FAILED_FORMAT = "Failed to process patch update for config: %s/%s";
    public static final String ERROR_DECRYPTING_DATA_FAILED_FORMAT = "Error getting and decrypting configuration data for %s/%s";
    public static final String ERROR_SAVING_INLINE_CONFIG_FAILED_FORMAT = "Failed to save inline configuration for %s/%s";
    public static final String ERROR_SAVING_ARTIFACT_CONFIG_FAILED_FORMAT = "Failed to save artifact configuration for %s/%s";
    public static final String AUDIT_PATCH_DETAILS_FORMAT = "Patched config %s/%s. Version changed from %d to %d. Is Encrypted: %b";
    public static final String JSON_KEY_ACTIVE = "active";

    // --- ARTIFACT SERVICE CONSTANTS ---
    public static final String ARTIFACT_SERVICE_LOGGER_NAME = "ConfigurationArtifactService";
    public static final String ARTIFACT_SERVICE_LOGGER_CATEGORY = "config-artifact";
    public static final String ARTIFACT_BASE_PATH_TEMPLATE = "./storage/configurations/%s/%s";
    public static final String ARTIFACT_ENCRYPTED_EXTENSION = ".enc";
    public static final String ARTIFACT_MAP_KEY_PREFIX = "artifact_";
    public static final String ARTIFACT_NULL_VALUE = "null";
    public static final String ARTIFACT_MAP_TYPE = "artifactType";
    public static final String ARTIFACT_MAP_FILE_PATH = "filePath";
    public static final String ARTIFACT_MAP_FILE_NAME = "fileName";
    public static final String ARTIFACT_MAP_ENCRYPTED = "encrypted";
    public static final String ARTIFACT_MAP_CONTENT = "content";
    public static final String ARTIFACT_MAP_ERROR = "error";
    public static final String CONFIG_ARTIFACT_READ_OR_DECRYPT_FAILED = "Failed to read or decrypt artifact content.";
    public static final String LOG_ARTIFACT_SAVED = "Artifact saved to disk";
    public static final String LOG_ARTIFACT_ENCRYPTED_SAVED = "Saved encrypted file for %s to %s";
    public static final String LOG_ARTIFACT_SAVED_PLAIN = "Saved file for %s to %s";
    public static final String LOG_ARTIFACT_NOT_RETRIEVED = "Artifact content not retrieved; artifact is null, inactive, or has no file path";
    public static final String LOG_ARTIFACT_DECRYPTED = "Successfully decrypted artifact content";
    public static final String LOG_ARTIFACT_READ = "Successfully read artifact content";
    public static final String LOG_ARTIFACT_READ_FAILED = "Failed to read artifact file: %s";
    public static final String LOG_ARTIFACT_DECRYPT_FAILED = "Failed to decrypt artifact file: %s";
    public static final String LOG_ARTIFACT_METADATA_PROCESSED = "Processed artifact metadata";
    public static final String LOG_ARTIFACT_DELETED = "Deleted artifact file: %s";
    public static final String LOG_ARTIFACT_PHYSICAL_DELETED = "Physical artifact file deleted";
    public static final String LOG_ARTIFACT_DELETE_FAILED = "Failed to delete physical artifact file: %s %s";
    public static final String LOG_ARTIFACT_BULK_DELETE = "Deleting physical artifact as part of bulk delete";
    public static final String JSON_KEY_IS_ACTIVE = "isActive";

    // --- CONFIGURATION SERVICE CONSTANTS ---
    public static final String CONFIG_ENCRYPTION_SERVICE_LOGGER_NAME = "ConfigurationEncryptionService";
    public static final String CONFIG_ENCRYPTION_SERVICE_LOGGER_CATEGORY = "encryption-config";
    public static final String ENCRYPTION_CATEGORY = "config-encryption";
    public static final String DECRYPTION_CATEGORY = "config-decryption";
    public static final String ENCRYPTION_FIELD_ENCRYPTED = "encrypted";
    public static final String ENCRYPTION_FIELD_DATA = "data";
    public static final String LOG_FULL_ENCRYPTION_APPLIED = "Applying full encryption based on request flag";
    public static final String LOG_FIELD_ENCRYPTION_APPLIED = "Applying field-level encryption based on rules";
    public static final String LOG_NO_ENCRYPTION_RULE_MATCHED = "No encryption rule matched, checking for sensitive fields anyway";
    public static final String LOG_FULL_DECRYPTION_APPLIED = "Applying full decryption based on envelope";
    public static final String LOG_FIELD_DECRYPTION_APPLIED = "Applying field-level decryption";
    public static final String ENCRYPTION_ALGORITHM_AES_256_GCM = "AES-256-GCM";

    // Encryption Log Detail Keys
    public static final String LOG_DETAIL_CONFIG_TYPE = "configType";
    public static final String LOG_DETAIL_CONFIG_KEY = "configKey";

    public static final String ARTIFACT_CONFIG_SUBTYPE = "config";

    public static final String SECURITY_LOG_NO_HTTP_REQUEST_FOUND = "No HTTP request found in context. Assuming a system-level operation.";

    // --- Logger Configuration ---
    public static final String LOGGER_NAME_USER_CONTEXT_INTERCEPTOR = "UserContextInterceptor";
    public static final String LOG_CATEGORY_GRPC_USER_CONTEXT = "grpc-user-context";

    // --- Logging Messages ---
    public static final String LOG_MSG_SET_USER_CONTEXT_FROM_METADATA = "Set user context from gRPC metadata";
    public static final String LOG_MSG_NO_USER_METADATA_USING_SYSTEM = "No user metadata found, using system user";
    
    // --- System & Default Values (shared) ---
    public static final String UNKNOWN_IP_ADDRESS = "unknown";

    // --- Logging Detail Keys (shared) ---
    public static final String LOG_DETAIL_USER_ID = "userId";
    public static final String LOG_DETAIL_USERNAME = "username";

    // =========================================================================
    // ConfigurationController Constants
    // =========================================================================

    // --- API Configuration ---
    public static final String API_V1_CONFIGURATIONS_BASE_PATH = "/api/v1/configurations";
    public static final String API_CONFIG_BY_TYPE = "/{configType}";
    public static final String API_CONFIG_BY_TYPE_AND_KEY = "/{configType}/{configKey}";

    // --- Logger Configuration ---
    public static final String LOGGER_NAME_CONFIGURATION_CONTROLLER = "ConfigurationController";

    // --- Logging Messages ---
    public static final String LOG_MSG_ENTERING_GET_ALL_CONFIGS = "Entering REST GET /api/v1/configurations";
    public static final String LOG_MSG_GET_ALL_CONFIGS_FAILED = "Failed to retrieve all configurations via gRPC.";
    public static final String LOG_MSG_ENTERING_GET_CONFIGS_BY_TYPE = "Entering REST GET /api/v1/configurations/{configType}";
    public static final String LOG_MSG_GET_CONFIGS_BY_TYPE_FAILED = "Failed to get configurations by type via gRPC.";
    public static final String LOG_MSG_ENTERING_GET_CONFIG = "Entering REST GET /{configType}/{configKey}";
    public static final String LOG_MSG_GET_CONFIG_FAILED = "Failed to get configuration via gRPC.";
    public static final String LOG_MSG_ENTERING_PATCH_CONFIG = "Entering REST PATCH /{configType}/{configKey}";
    public static final String LOG_MSG_PATCH_CONFIG_FAILED = "Failed to patch configuration via gRPC.";
    public static final String LOG_MSG_ENTERING_PUT_CONFIG = "Entering REST PUT /{configType}/{configKey}";
    public static final String LOG_MSG_SAVE_CONFIG_FAILED = "Failed to save configuration via gRPC.";
    public static final String LOG_MSG_ENTERING_DELETE_CONFIG = "Entering REST DELETE /{configType}/{configKey}";

    // SsoConfigController Constants
    // =========================================================================

    // --- API Configuration ---
    public static final String API_V1_ADMIN_SSO_BASE_PATH = "/api/v1/admin/sso";
    public static final String API_SSO_ALL_CONFIGS = "/configurations";
    public static final String API_SSO_BY_PROVIDER = "/{provider}";

    // --- SSO Configuration Types and Keys ---
    public static final String CONFIG_TYPE_SSO = "sso";
    public static final String PROVIDER_LDAP = "ldap";
    public static final String PROVIDER_SAML = "saml";
    public static final String PROVIDER_OAUTH2 = "oauth2";
    public static final String CONFIG_KEY_LDAP = "ldap_config";
    public static final String CONFIG_KEY_SAML = "saml_config";
    public static final String CONFIG_KEY_OAUTH2 = "oauth2_config";
    public static final String CONFIG_SUB_TYPE_DEFAULT = "config";

    // --- JSON/Map Keys and Statuses ---
    public static final String JSON_KEY_PROVIDER = "provider";
    public static final String JSON_KEY_STATUS = "status";
    public static final String JSON_KEY_MESSAGE = "message";
    public static final String JSON_KEY_ENCRYPT_ENTIRE_CONFIG = "encryptEntireConfig";
    public static final String STATUS_ERROR = "error";
    public static final String STATUS_SUCCESS = "success";

    // --- Validation and Error Messages ---
    public static final String ERROR_MSG_CONFIG_DATA_REQUIRED = "Configuration data is required in the request body.";
    public static final String ERROR_MSG_UNKNOWN_PROVIDER = "Unknown provider specified.";

    // --- Response Message Formats ---
    public static final String MSG_FORMAT_CONFIG_UPDATED = "%s configuration updated.";
    public static final String MSG_FORMAT_CONFIG_DELETED = "%s configuration deleted.";

    // --- Logger Configuration ---
    public static final String LOGGER_NAME_SSO_CONFIG_CONTROLLER = "SsoConfigController";
    public static final String LOG_CATEGORY_SSO_CONFIG_CONTROLLER = "sso-config-controller";

    // --- Logging Messages ---
    public static final String LOG_MSG_GET_ALL_SSO_CONFIGS = "Entering REST GET /configurations to fetch all SSO provider settings.";
    public static final String LOG_MSG_GET_ALL_SSO_CONFIGS_SUCCESS = "Successfully retrieved all SSO configurations.";
    public static final String LOG_MSG_GET_ALL_SSO_CONFIGS_ERROR = "An internal error occurred while retrieving all SSO configurations.";
    public static final String LOG_MSG_GET_SSO_CONFIG = "Entering REST GET /{provider}";
    public static final String LOG_MSG_GET_UNKNOWN_PROVIDER = "Attempted to get configuration for an unknown provider.";
    public static final String LOG_MSG_SAVE_SSO_CONFIG = "Entering REST PUT /{provider}";
    public static final String LOG_MSG_SAVE_SSO_INVALID_BODY = "Save attempt failed due to empty or invalid request body.";
    public static final String LOG_MSG_SAVE_SSO_SUCCESS = "Successfully processed save request for provider via gRPC.";
    public static final String LOG_MSG_SAVE_SSO_FAILED = "Failed to save SSO config for provider due to gRPC call failure.";
    public static final String LOG_MSG_GET_PROVIDER_DATA_GRPC_FAILED = "gRPC call failed while getting config key.";
    public static final String LOG_MSG_GET_PROVIDER_DATA_PARSE_FAILED = "Failed to parse gRPC response for config key.";
    public static final String LOG_MSG_PATCH_SSO_CONFIG = "Entering REST PATCH /{provider}";
    public static final String LOG_MSG_PATCH_SSO_CONFIG_FAILED = "Failed to patch SSO config for provider via gRPC.";
    public static final String LOG_MSG_DELETE_SSO_CONFIG = "Entering REST DELETE /{provider}";
    public static final String LOG_MSG_DELETE_SSO_SUCCESS = "Successfully deleted configuration for provider via gRPC.";
    public static final String LOG_MSG_DELETE_SSO_NOT_FOUND = "Attempted to delete a non-existent provider configuration.";

    public static final String LOG_DETAIL_GRPC_STATUS = "grpcStatus";

    public static final String LOG_CATEGORY_SERVICE = "Configuration";
    // Log Categories
    public static final String LOG_CATEGORY_JWT_UTIL = "jwt-util";
    public static final String LOG_CATEGORY_SECURITY_CONTEXT = "security-context";

    // JWT Utility Log Messages
    public static final String LOG_CLAIMS_EXTRACTION_FAILURE = "Failed to extract claims from JWT token";
    public static final String LOG_TOKEN_VALIDATION_SUCCESS = "JWT token validation successful";
    public static final String LOG_TOKEN_VALIDATION_FAILURE = "JWT token validation failed: ";

    // JWT Tenant ID extraction log messages
    public static final String LOG_TENANT_ID_EXTRACTED = "Tenant ID extracted from JWT token";
    public static final String LOG_TENANT_ID_NOT_FOUND = "No tenant ID found in JWT token";
    public static final String LOG_TENANT_ID_EXTRACTION_FAILED = "Failed to extract tenant ID from JWT token";

    // JWT Username extraction log messages
    public static final String LOG_USERNAME_EXTRACTED = "Username extracted from JWT token";
    public static final String LOG_USERNAME_FALLBACK_TO_SUBJECT = "Username not found, falling back to subject claim";

    // JWT User ID extraction log messages
    public static final String LOG_USER_ID_EXTRACTED = "User ID extracted from JWT token";
    public static final String LOG_USER_ID_FALLBACK_TO_SUBJECT = "User ID not found, falling back to subject claim";

    // Security Context Service Log Messages
    public static final String LOG_USER_FROM_GATEWAY_HEADERS = "Retrieved user from HTTP headers (Gateway Auth)";
    public static final String LOG_USER_FROM_JWT_TOKEN = "Retrieved user from JWT token";
    public static final String LOG_USER_FROM_THREAD_LOCAL = "Retrieved user from thread-local context (gRPC)";
    public static final String LOG_JWT_CLAIMS_MISSING = "JWT token found but could not extract required claims (sub, username)";
    public static final String LOG_AUDIT_USER_NOT_FOUND = "CRITICAL: Audit user context not found. Unable to proceed with audited operation";
    public static final String LOG_HTTP_REQUEST_CONTEXT_ERROR = "Could not retrieve HTTP request for audit context (not in a request scope)";

    // Tenant Resolution Log Messages
    public static final String LOG_TENANT_RESOLVED_FROM_HEADER = "Tenant ID resolved from HTTP header";
    public static final String LOG_TENANT_RESOLVED_FROM_JWT = "Tenant ID resolved from JWT token";
    public static final String LOG_TENANT_RESOLVED_FROM_THREAD_LOCAL = "Tenant ID resolved from thread-local context";
    public static final String LOG_TENANT_FALLBACK_TO_DEFAULT = "Tenant ID not found, falling back to default tenant";

    // IP Address Resolution Log Messages
    public static final String LOG_IP_RESOLVED_FROM_X_FORWARDED_FOR = "Client IP resolved from X-Forwarded-For header";
    public static final String LOG_IP_RESOLVED_FROM_X_REAL_IP = "Client IP resolved from X-Real-IP header";
    public static final String LOG_IP_RESOLVED_FROM_REMOTE_ADDR = "Client IP resolved from remote address";

    // Error Messages
    public static final String ERROR_AUDIT_USER_CONTEXT_NOT_FOUND = "Audit user context could not be determined. This is a critical security failure.";
    public static final String ERROR_JWT_TOKEN_PARSING_FAILED = "Failed to parse JWT token";
    public static final String ERROR_JWT_TOKEN_EXPIRED = "JWT token has expired";
    public static final String ERROR_JWT_TOKEN_INVALID = "JWT token is invalid";

    // Default Values
    public static final String DEFAULT_TENANT_ID = "tenant_001";
}