package com.aidentx.iam.config;

/**
 * Application constants for Identity Schema service.
 * Contains all constant values to avoid hardcoding throughout the application.
 *
 * @author aidentx
 * @since 1.0
 */
public class AppConstants {

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - DGRAPH SCHEMA SERVICE
    // ===================================================================
    public static final String LOG_DGRAPH_BASIC_SCHEMA_INIT_STARTED = "Basic Dgraph schema initialization started";
    public static final String LOG_DGRAPH_BASIC_SCHEMA_INIT_SUCCESS = "Basic Dgraph schema initialized successfully";
    public static final String LOG_DGRAPH_BASIC_SCHEMA_INIT_FAILED = "Failed to initialize basic Dgraph schema";
    public static final String LOG_DGRAPH_SCHEMA_SYNC_STARTED = "Schema synchronization to Dgraph started";
    public static final String LOG_DGRAPH_SCHEMA_SYNC_SUCCESS = "Schema synced to Dgraph successfully";
    public static final String LOG_DGRAPH_SCHEMA_SYNC_FAILED = "Failed to sync schema to Dgraph";
    public static final String LOG_DGRAPH_SCHEMA_REMOVAL_NOTED = "Schema removal from Dgraph noted (manual cleanup may be required)";
    public static final String LOG_DGRAPH_SCHEMA_REMOVAL_FAILED = "Failed to remove schema from Dgraph";
    public static final String LOG_DGRAPH_FIELD_SYNC_STARTED = "Field synchronization to Dgraph started";
    public static final String LOG_DGRAPH_FIELD_SYNC_SUCCESS = "Field synced to Dgraph successfully";
    public static final String LOG_DGRAPH_FIELD_SYNC_FAILED = "Failed to sync field to Dgraph";
    public static final String LOG_DGRAPH_SCHEMA_UPDATE_STARTED = "Schema update in Dgraph started";
    public static final String LOG_DGRAPH_SCHEMA_UPDATE_SUCCESS = "Schema updated in Dgraph successfully";
    public static final String LOG_DGRAPH_SCHEMA_UPDATE_FAILED = "Failed to update schema in Dgraph";
    public static final String LOG_DGRAPH_OPERATION_BUILDING = "Building Dgraph operation";
    public static final String LOG_DGRAPH_OPERATION_EXECUTING = "Executing Dgraph operation";
    public static final String LOG_DGRAPH_SCHEMA_DEFINITION_BUILDING = "Building schema definition";
    public static final String LOG_DGRAPH_FIELD_DEFINITION_BUILDING = "Building field definition";

    // ===================================================================
    // EXCEPTION MESSAGES - DGRAPH SCHEMA SERVICE
    // ===================================================================
    public static final String EXCEPTION_DGRAPH_SCHEMA_SYNC_FAILED = "Failed to sync schema to Dgraph";
    public static final String EXCEPTION_DGRAPH_FIELD_SYNC_FAILED = "Failed to sync field to Dgraph";
    public static final String EXCEPTION_DGRAPH_SCHEMA_UPDATE_FAILED = "Failed to update schema in Dgraph";

    // ===================================================================
    // LOGGER CATEGORIES
    // ===================================================================
    public static final String LOG_CATEGORY_DGRAPH_SCHEMA = "Dgraph-Schema";


    // Logger Names
    public static final String IDENTITIES_SERVICE_LOGGER_NAME = "IdentitiesService";
    public static final String IDENTITIES_CONTROLLER_LOGGER_NAME = "IdentitiesController";

    // Logger Categories
    public static final String LOG_CATEGORY_IDENTITIES = "identities";
    public static final String LOG_CATEGORY_IDENTITIES_SERVICE = "identities-service";
    public static final String LOG_CATEGORY_IDENTITIES_CONTROLLER = "identities-controller";
    public static final String LOG_CATEGORY_DGRAPH_INITIALIZER = "dgraph-initializer";
    public static final String LOG_CATEGORY_FIELD_VALIDATION = "field-validation";
    public static final String LOG_CATEGORY_IDENTITY_AUDIT_ASPECT = "identity-audit-aspect";

    // ===================================================================
    // API PATHS
    // ===================================================================

    public static final String API_IDENTITIES_PATH = "/api/v1/identity";
    public static final String API_PATH_BY_TENANT_AND_SCHEMA = "/by-schema/{primarySchemaId}";
    public static final String API_PATH_BY_USER = "/user/{userId}";
    public static final String API_PATH_BY_EXTERNAL_ID = "/external/{externalId}";
    public static final String API_PATH_SCHEMA_DRIVEN_CREATE = "/schema-driven";
    public static final String API_PATH_SCHEMA_DRIVEN_UPDATE = "/{id}/schema-driven";
    public static final String API_INACTIVE_PATH = "/inactive";
    public static final String API_SEARCH_PATH = "/search";

    // ===================================================================
    // JSON CONVERSION ERROR MESSAGES
    // ===================================================================

    public static final String JSONNODE_TO_STRING_ERROR = "Failed to convert JsonNode to String";
    public static final String STRING_TO_JSONNODE_ERROR = "Failed to convert String to JsonNode";

    // ===================================================================
    // SUCCESS MESSAGES
    // ===================================================================

    public static final String IDENTITY_CREATED_SUCCESS = "Identity created successfully";
    public static final String IDENTITY_UPDATED_SUCCESS = "Identity updated successfully";
    public static final String IDENTITY_DELETED_SUCCESS = "Identity deleted successfully";
    public static final String IDENTITY_RETRIEVED_SUCCESS = "Identity retrieved successfully";
    public static final String IDENTITIES_RETRIEVED_SUCCESS = "Identities retrieved successfully";

    // ===================================================================
    // ERROR MESSAGES
    // ===================================================================

    public static final String IDENTITY_NOT_FOUND = "Identity not found";
    public static final String IDENTITY_ALREADY_EXISTS = "Identity already exists";
    public static final String INVALID_IDENTITY_DATA = "Invalid identity data";
    public static final String IDENTITY_CREATION_FAILED = "Failed to create identity";
    public static final String IDENTITY_UPDATE_FAILED = "Failed to update identity";
    public static final String IDENTITY_DELETION_FAILED = "Failed to delete identity";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - IDENTITY MANAGEMENT
    // ===================================================================
    public static final String LOG_IDENTITY_CREATION_STARTED = "Starting identity creation process";
    public static final String LOG_IDENTITY_CREATED_SUCCESSFULLY = "Identity created successfully";
    public static final String LOG_IDENTITY_CREATION_FAILED = "Failed to create identity";
    public static final String LOG_IDENTITY_UPDATE_STARTED = "Starting identity update process";
    public static final String LOG_IDENTITY_UPDATED_SUCCESSFULLY = "Identity updated successfully";
    public static final String LOG_IDENTITY_UPDATE_FAILED = "Failed to update identity";
    public static final String LOG_IDENTITY_DELETION_STARTED = "Starting identity deletion process";
    public static final String LOG_IDENTITY_DELETED_SUCCESSFULLY = "Identity deleted successfully";
    public static final String LOG_IDENTITY_DELETION_FAILED = "Failed to delete identity";
    public static final String LOG_IDENTITY_SEARCH_STARTED = "Starting identity search";
    public static final String LOG_IDENTITY_SEARCH_COMPLETED = "Identity search completed successfully";
    public static final String LOG_IDENTITY_SEARCH_FAILED = "Failed to execute identity search";
    public static final String LOG_IDENTITY_NOT_FOUND = "Identity not found";
    public static final String LOG_IDENTITY_VALIDATION_FAILED = "Identity validation failed";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - DGRAPH SYNCHRONIZATION
    // ===================================================================
    public static final String LOG_DGRAPH_SYNC_STARTED = "Starting Dgraph synchronization";
    public static final String LOG_DGRAPH_SYNC_COMPLETED = "Dgraph synchronization completed successfully";
    public static final String LOG_DGRAPH_SYNC_FAILED = "Failed to synchronize with Dgraph";
    public static final String LOG_DGRAPH_SCHEMA_INIT_STARTED = "Starting Dgraph schema initialization";
    public static final String LOG_DGRAPH_SCHEMA_INIT_COMPLETED = "Dgraph schema initialization completed";
    public static final String LOG_DGRAPH_SCHEMA_INIT_FAILED = "Failed to initialize Dgraph schema";
    public static final String LOG_DGRAPH_CONNECTION_FAILED = "Failed to connect to Dgraph";
    public static final String LOG_DGRAPH_UNAVAILABLE = "Dgraph service unavailable, continuing with PostgreSQL only";
    public static final String LOG_DGRAPH_NODE_CREATED = "Dgraph node created successfully";
    public static final String LOG_DGRAPH_NODE_UPDATED = "Dgraph node updated successfully";
    public static final String LOG_DGRAPH_NODE_DELETED = "Dgraph node deleted successfully";
    public static final String LOG_DGRAPH_RELATIONSHIP_CREATED = "Dgraph relationship created successfully";
    public static final String LOG_DGRAPH_RELATIONSHIP_UPDATED = "Dgraph relationship updated successfully";
    public static final String LOG_DGRAPH_RELATIONSHIP_DELETED = "Dgraph relationship deleted successfully";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - DATABASE OPERATIONS
    // ===================================================================
    public static final String LOG_DATABASE_SAVE_STARTED = "Starting database save operation";
    public static final String LOG_DATABASE_SAVE_COMPLETED = "Database save operation completed";
    public static final String LOG_DATABASE_SAVE_FAILED = "Failed to save to database";
    public static final String LOG_DATABASE_QUERY_STARTED = "Starting database query";
    public static final String LOG_DATABASE_QUERY_COMPLETED = "Database query completed successfully";
    public static final String LOG_DATABASE_QUERY_FAILED = "Failed to execute database query";
    public static final String LOG_DATABASE_CONNECTION_FAILED = "Failed to connect to database";
    public static final String LOG_TRANSACTION_STARTED = "Database transaction started";
    public static final String LOG_TRANSACTION_COMMITTED = "Database transaction committed";
    public static final String LOG_TRANSACTION_ROLLED_BACK = "Database transaction rolled back";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - API OPERATIONS
    // ===================================================================
    public static final String LOG_API_REQUEST_RECEIVED = "API request received";
    public static final String LOG_API_REQUEST_PROCESSING = "Processing API request";
    public static final String LOG_API_REQUEST_COMPLETED = "API request completed successfully";
    public static final String LOG_API_REQUEST_FAILED = "API request failed";
    public static final String LOG_INVALID_REQUEST_PARAMETERS = "Invalid request parameters";
    public static final String LOG_REQUEST_VALIDATION_FAILED = "Request validation failed";
    public static final String LOG_RESPONSE_GENERATION_STARTED = "Response generation started";
    public static final String LOG_RESPONSE_GENERATION_COMPLETED = "Response generation completed";
    public static final String LOG_RESPONSE_GENERATION_FAILED = "Response generation failed";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - SCHEMA FIELD CLASSIFICATION
    // ===================================================================
    public static final String LOG_SCHEMA_CLASSIFICATION_STARTED = "Schema field classification started";
    public static final String LOG_SCHEMA_CLASSIFICATION_COMPLETED = "Schema field classification completed";
    public static final String LOG_SCHEMA_CLASSIFICATION_FAILED = "Schema field classification failed";
    public static final String LOG_SCHEMA_FETCH_STARTED = "Schema definition fetch started";
    public static final String LOG_SCHEMA_FETCH_COMPLETED = "Schema definition fetch completed";
    public static final String LOG_SCHEMA_FETCH_FAILED = "Schema definition fetch failed";
    public static final String LOG_SCHEMA_CACHE_HIT = "Schema definition found in cache";
    public static final String LOG_SCHEMA_CACHE_MISS = "Schema definition not found in cache";
    public static final String LOG_SCHEMA_CACHE_STORED = "Schema definition stored in cache";
    public static final String LOG_SCHEMA_CACHE_CLEARED = "Schema cache cleared";
    public static final String LOG_SCHEMA_URL_CONSTRUCTED = "Schema API URL constructed";
    public static final String LOG_SCHEMA_HTTP_REQUEST_PREPARED = "Schema HTTP request prepared";
    public static final String LOG_SCHEMA_HTTP_RESPONSE_RECEIVED = "Schema HTTP response received";
    public static final String LOG_SCHEMA_PARSING_STARTED = "Schema response parsing started";
    public static final String LOG_SCHEMA_PARSING_COMPLETED = "Schema response parsing completed";
    public static final String LOG_SCHEMA_PARSING_FAILED = "Schema response parsing failed";
    public static final String LOG_FIELD_CORE_CLASSIFIED = "Field classified as core";
    public static final String LOG_FIELD_EXTENDED_CLASSIFIED = "Field classified as extended";
    public static final String LOG_FIELD_CLASSIFICATION_LOOP_STARTED = "Field classification loop started";
    public static final String LOG_FIELD_CLASSIFICATION_LOOP_COMPLETED = "Field classification loop completed";
    public static final String LOG_SCHEMA_FIELD_LOOKUP_STARTED = "Schema field lookup started";
    public static final String LOG_SCHEMA_FIELD_LOOKUP_COMPLETED = "Schema field lookup completed";
    public static final String LOG_SCHEMA_FIELD_NOT_FOUND = "Schema field not found";
    public static final String LOG_CORE_SERVICE_AUTHENTICATION_CONFIGURED = "Core-service authentication configured";
    public static final String LOG_CORE_SERVICE_UNAUTHENTICATED_REQUEST = "Attempting unauthenticated core-service request";
    public static final String LOG_SCHEMA_ARRAY_RESPONSE_DETECTED = "Schema array response detected";
    public static final String LOG_SCHEMA_OBJECT_RESPONSE_DETECTED = "Schema object response detected";
    public static final String LOG_SCHEMA_BACKWARD_COMPATIBILITY_MODE = "Using backward compatibility mode for schema parsing";

    // ===================================================================
    // EXCEPTION MESSAGES - SCHEMA FIELD CLASSIFICATION
    // ===================================================================
    public static final String EXCEPTION_SCHEMA_CLASSIFICATION_FAILED = "Failed to classify fields for schema";
    public static final String EXCEPTION_SCHEMA_FETCH_FAILED = "Failed to fetch schema definition";
    public static final String EXCEPTION_SCHEMA_PARSING_FAILED = "Failed to parse schema response";
    public static final String EXCEPTION_CORE_SERVICE_UNAVAILABLE = "Core-service is unavailable";
    public static final String EXCEPTION_CORE_SERVICE_HTTP_ERROR = "Core-service HTTP error";
    public static final String EXCEPTION_SCHEMA_CACHE_ERROR = "Schema cache operation failed";
    public static final String EXCEPTION_FIELD_LOOKUP_FAILED = "Field lookup in schema failed";
    public static final String EXCEPTION_AUTHENTICATION_TOKEN_INVALID = "Authentication token invalid or expired";
    public static final String EXCEPTION_REQUIRED_FIELDS_MISSING = "Required fields are missing";
    public static final String EXCEPTION_FIELD_VALIDATION_FAILED = "Field validation failed";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - FIELD CLASSIFICATION SERVICE
    // ===================================================================
    public static final String LOG_FIELD_CLASSIFICATION_STARTED = "Field classification process started";
    public static final String LOG_FIELD_CLASSIFICATION_COMPLETED = "Field classification completed";
    public static final String LOG_FIELD_CLASSIFICATION_FAILED = "Field classification failed";
    public static final String LOG_CORE_FIELDS_CLASSIFIED = "Core fields classified successfully";
    public static final String LOG_EXTENDED_FIELDS_CLASSIFIED = "Extended fields classified successfully";
    public static final String LOG_POSTGRESQL_DELETION_STARTED = "Starting PostgreSQL deletion";
    public static final String LOG_POSTGRESQL_DELETION_COMPLETED = "PostgreSQL deletion completed";
    public static final String LOG_IDENTITY_MAPPING_STARTED = "Identity field mapping started";
    public static final String LOG_IDENTITY_MAPPING_COMPLETED = "Identity field mapping completed";
    public static final String LOG_ATTRIBUTES_SERIALIZATION_STARTED = "Attributes serialization started";
    public static final String LOG_ATTRIBUTES_SERIALIZATION_COMPLETED = "Attributes serialization completed";
    public static final String LOG_ATTRIBUTES_SERIALIZATION_FAILED = "Attributes serialization failed";

    // ===================================================================
    // DEBUG LOGGING MESSAGES
    // ===================================================================
    public static final String LOG_DEBUG_METHOD_ENTRY = "Entering method";
    public static final String LOG_DEBUG_METHOD_EXIT = "Exiting method";
    public static final String LOG_DEBUG_PARAMETER_VALUES = "Method parameter values";
    public static final String LOG_DEBUG_PROCESSING_STEP = "Processing step";
    public static final String LOG_DEBUG_VALIDATION_STEP = "Validation step";
    public static final String LOG_DEBUG_STATE_CHANGE = "State change detected";
    public static final String LOG_DEBUG_CONDITION_CHECK = "Condition check";
    public static final String LOG_DEBUG_LOOP_ITERATION = "Loop iteration";
    public static final String LOG_DEBUG_OBJECT_STATE = "Object state";
    public static final String LOG_DEBUG_CONFIGURATION_VALUE = "Configuration value";
    public static final String LOG_DEBUG_TEMPORARY_DATA = "Temporary data state";

    // ===================================================================
    // EXCEPTION MESSAGES
    // ===================================================================
    public static final String EXCEPTION_IDENTITY_SAVE_FAILED = "Failed to save identity to database";
    public static final String EXCEPTION_IDENTITY_UPDATE_FAILED = "Failed to update identity in database";
    public static final String EXCEPTION_IDENTITY_DELETE_FAILED = "Failed to delete identity from database";
    public static final String EXCEPTION_IDENTITY_FETCH_FAILED = "Failed to fetch identity from database";
    public static final String EXCEPTION_FIELD_CLASSIFICATION_FAILED = "Field classification process failed";
    public static final String EXCEPTION_DGRAPH_SYNC_FAILED = "Dgraph synchronization failed";
    public static final String EXCEPTION_ATTRIBUTES_SERIALIZATION_FAILED = "Failed to serialize attributes to JSON";
    public static final String EXCEPTION_INVALID_SCHEMA_ID = "Invalid or missing schema ID";
    public static final String EXCEPTION_INVALID_TENANT_ID = "Invalid or missing tenant ID";
    public static final String EXCEPTION_INVALID_REQUEST_DATA = "Invalid request data provided";
    public static final String EXCEPTION_AUTHENTICATION_FAILED = "Authentication failed";
    public static final String EXCEPTION_AUTHORIZATION_FAILED = "Authorization failed";
    public static final String EXCEPTION_SERVICE_UNAVAILABLE = "Required service is currently unavailable";
    public static final String EXCEPTION_DGRAPH_SCHEMA_INIT_FAILED = "Failed to initialize Dgraph schema";
    public static final String EXCEPTION_DGRAPH_NODE_CREATE_FAILED = "Failed to create Dgraph node";
    public static final String EXCEPTION_DGRAPH_NODE_UPDATE_FAILED = "Failed to update Dgraph node";
    public static final String EXCEPTION_DGRAPH_NODE_DELETE_FAILED = "Failed to delete Dgraph node";
    public static final String EXCEPTION_DGRAPH_HEALTH_CHECK_FAILED = "Dgraph health check failed";
    public static final String EXCEPTION_JSON_SERIALIZATION_FAILED = "Failed to serialize object to JSON";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - DGRAPH OPERATIONS
    // ===================================================================
    public static final String LOG_DGRAPH_TRANSACTION_STARTED = "Dgraph transaction started";
    public static final String LOG_DGRAPH_TRANSACTION_COMMITTED = "Dgraph transaction committed successfully";
    public static final String LOG_DGRAPH_TRANSACTION_DISCARDED = "Dgraph transaction discarded";
    public static final String LOG_DGRAPH_MUTATION_PREPARED = "Dgraph mutation prepared";
    public static final String LOG_DGRAPH_MUTATION_EXECUTED = "Dgraph mutation executed successfully";
    public static final String LOG_DGRAPH_QUERY_EXECUTED = "Dgraph query executed successfully";
    public static final String LOG_DGRAPH_NODE_ID_RETRIEVED = "Dgraph node ID retrieved successfully";
    public static final String LOG_DGRAPH_NODE_ID_MISSING = "Dgraph node ID missing from response";
    public static final String LOG_DGRAPH_NODE_CREATION_FALLBACK = "Creating new Dgraph node as fallback";
    public static final String LOG_DGRAPH_NODE_REMOVAL_SKIPPED = "Dgraph node removal skipped - no node ID";
    public static final String LOG_DGRAPH_HEALTH_CHECK_STARTED = "Dgraph health check started";
    public static final String LOG_DGRAPH_HEALTH_CHECK_COMPLETED = "Dgraph health check completed";
    public static final String LOG_DGRAPH_HEALTH_CHECK_FAILED = "Dgraph health check failed";
    public static final String LOG_DGRAPH_ATTRIBUTES_EXTRACTION_STARTED = "Dgraph attributes extraction started";
    public static final String LOG_DGRAPH_ATTRIBUTES_EXTRACTION_COMPLETED = "Dgraph attributes extraction completed";
    public static final String LOG_DGRAPH_ATTRIBUTES_EXTRACTION_FAILED = "Failed to extract attributes for Dgraph";
    public static final String LOG_DGRAPH_RELATIONSHIP_SYNC_PLACEHOLDER = "Dgraph relationship synchronization placeholder";
    public static final String LOG_DGRAPH_SCHEMA_BUILD_STARTED = "Dgraph schema build started";
    public static final String LOG_DGRAPH_SCHEMA_BUILD_COMPLETED = "Dgraph schema build completed";
    public static final String LOG_DGRAPH_JSON_CONVERSION_STARTED = "JSON conversion for Dgraph started";
    public static final String LOG_DGRAPH_JSON_CONVERSION_COMPLETED = "JSON conversion for Dgraph completed";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - JWT SECURITY
    // ===================================================================
    public static final String LOG_JWT_TOKEN_VALIDATION_STARTED = "JWT token validation started";
    public static final String LOG_JWT_TOKEN_VALIDATION_SUCCESSFUL = "JWT token validation successful";
    public static final String LOG_JWT_TOKEN_VALIDATION_FAILED = "JWT token validation failed";
    public static final String LOG_JWT_TOKEN_NULL_OR_EMPTY = "JWT token is null or empty";
    public static final String LOG_JWT_TOKEN_EXPIRED = "JWT token is expired";
    public static final String LOG_JWT_USERNAME_EXTRACTION_STARTED = "JWT username extraction started";
    public static final String LOG_JWT_USERNAME_EXTRACTION_SUCCESSFUL = "JWT username extraction successful";
    public static final String LOG_JWT_USERNAME_EXTRACTION_FAILED = "JWT username extraction failed";
    public static final String LOG_JWT_USERID_EXTRACTION_STARTED = "JWT user ID extraction started";
    public static final String LOG_JWT_USERID_EXTRACTION_SUCCESSFUL = "JWT user ID extraction successful";
    public static final String LOG_JWT_USERID_EXTRACTION_FAILED = "JWT user ID extraction failed";
    public static final String LOG_JWT_USERID_FALLBACK_TO_SUBJECT = "JWT user ID extraction falling back to subject claim";
    public static final String LOG_JWT_EXPIRATION_EXTRACTION_STARTED = "JWT expiration extraction started";
    public static final String LOG_JWT_EXPIRATION_EXTRACTION_SUCCESSFUL = "JWT expiration extraction successful";
    public static final String LOG_JWT_EXPIRATION_EXTRACTION_FAILED = "JWT expiration extraction failed";
    public static final String LOG_JWT_CLAIMS_PARSING_STARTED = "JWT claims parsing started";
    public static final String LOG_JWT_CLAIMS_PARSING_SUCCESSFUL = "JWT claims parsing successful";
    public static final String LOG_JWT_CLAIMS_PARSING_FAILED = "JWT claims parsing failed";
    public static final String LOG_JWT_EXPIRATION_CHECK_STARTED = "JWT expiration check started";
    public static final String LOG_JWT_EXPIRATION_CHECK_SUCCESSFUL = "JWT expiration check successful";
    public static final String LOG_JWT_EXPIRATION_CHECK_FAILED = "JWT expiration check failed";
    public static final String LOG_JWT_TENANT_EXTRACTION_STARTED = "JWT tenant ID extraction started";
    public static final String LOG_JWT_TENANT_EXTRACTION_SUCCESSFUL = "JWT tenant ID extraction successful";
    public static final String LOG_JWT_TENANT_EXTRACTION_FAILED = "JWT tenant ID extraction failed";
    public static final String LOG_JWT_ROLES_EXTRACTION_STARTED = "JWT roles extraction started";
    public static final String LOG_JWT_ROLES_EXTRACTION_SUCCESSFUL = "JWT roles extraction successful";
    public static final String LOG_JWT_ROLES_EXTRACTION_FAILED = "JWT roles extraction failed";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - JWT AUTHENTICATION FILTER
    // ===================================================================
    public static final String LOG_JWT_FILTER_PROCESSING_STARTED = "JWT filter processing started";
    public static final String LOG_JWT_FILTER_PROCESSING_COMPLETED = "JWT filter processing completed";
    public static final String LOG_JWT_FILTER_PROCESSING_FAILED = "JWT filter processing failed";
    public static final String LOG_JWT_FILTER_USER_AUTHENTICATED = "User authenticated via JWT filter";
    public static final String LOG_JWT_FILTER_USER_AUTHENTICATION_FAILED = "User authentication failed in JWT filter";
    public static final String LOG_JWT_FILTER_USER_FROM_GATEWAY = "User authenticated via API Gateway header";
    public static final String LOG_JWT_FILTER_TOKEN_FROM_HEADER = "JWT token extracted from Authorization header";
    public static final String LOG_JWT_FILTER_TOKEN_VALIDATION_STARTED = "JWT token validation started in filter";
    public static final String LOG_JWT_FILTER_TOKEN_VALIDATION_SUCCESSFUL = "JWT token validation successful in filter";
    public static final String LOG_JWT_FILTER_TOKEN_VALIDATION_FAILED = "JWT token validation failed in filter";
    public static final String LOG_JWT_FILTER_SECURITY_CONTEXT_SET = "Security context set for authenticated user";
    public static final String LOG_JWT_FILTER_SECURITY_CONTEXT_FAILED = "Failed to set security context";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - JWT AUTHENTICATION ENTRY POINT
    // ===================================================================
    public static final String LOG_JWT_UNAUTHORIZED_ACCESS_ATTEMPT = "Unauthorized access attempt detected";
    public static final String LOG_JWT_UNAUTHORIZED_RESPONSE_GENERATED = "Unauthorized response generated";
    public static final String LOG_JWT_CLIENT_IP_EXTRACTED = "Client IP address extracted";
    public static final String LOG_JWT_CLIENT_IP_EXTRACTION_FAILED = "Client IP address extraction failed";
    public static final String LOG_JWT_ERROR_RESPONSE_CREATED = "JWT error response created";
    public static final String LOG_JWT_ERROR_RESPONSE_WRITTEN = "JWT error response written to output";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - DGRAPH INITIALIZER
    // ===================================================================
    public static final String LOG_DGRAPH_INITIALIZER_STARTUP = "Dgraph initializer starting up";
    public static final String LOG_DGRAPH_INITIALIZER_SCHEMA_INIT_STARTED = "Initializing Dgraph schema for identities service";
    public static final String LOG_DGRAPH_INITIALIZER_SCHEMA_INIT_COMPLETED = "Dgraph schema initialization completed successfully";
    public static final String LOG_DGRAPH_INITIALIZER_SCHEMA_INIT_FAILED = "Failed to initialize Dgraph schema";
    public static final String LOG_DGRAPH_INITIALIZER_FALLBACK_MODE = "Application will continue to run, but Dgraph functionality may be limited";
    public static final String LOG_DGRAPH_INITIALIZER_SERVICE_CONFIGURED = "Dgraph synchronization service configured";
    public static final String LOG_DGRAPH_INITIALIZER_PROFILE_ACTIVE = "Dgraph initializer running in non-test profile";
    public static final String LOG_DGRAPH_INITIALIZER_STARTUP_COMPLETED = "Dgraph initializer startup completed";

    // ===================================================================
    // EXCEPTION MESSAGES - DGRAPH INITIALIZER
    // ===================================================================
    public static final String EXCEPTION_DGRAPH_INITIALIZER_STARTUP_FAILED = "Dgraph initializer startup failed";
    public static final String EXCEPTION_DGRAPH_INITIALIZER_SERVICE_UNAVAILABLE = "Dgraph synchronization service is unavailable";

    // ===================================================================
    // EXCEPTION MESSAGES - JWT SECURITY
    // ===================================================================
    public static final String EXCEPTION_JWT_TOKEN_INVALID = "Invalid JWT token provided";
    public static final String EXCEPTION_JWT_TOKEN_EXPIRED = "JWT token has expired";
    public static final String EXCEPTION_JWT_TOKEN_PARSING_FAILED = "Failed to parse JWT token";
    public static final String EXCEPTION_JWT_USERNAME_EXTRACTION_FAILED = "Failed to extract username from JWT token";
    public static final String EXCEPTION_JWT_USERID_EXTRACTION_FAILED = "Failed to extract user ID from JWT token";
    public static final String EXCEPTION_JWT_EXPIRATION_EXTRACTION_FAILED = "Failed to extract expiration from JWT token";
    public static final String EXCEPTION_JWT_CLAIMS_EXTRACTION_FAILED = "Failed to extract claims from JWT token";
    public static final String EXCEPTION_JWT_SIGNATURE_VERIFICATION_FAILED = "JWT signature verification failed";
    public static final String EXCEPTION_JWT_MALFORMED_TOKEN = "Malformed JWT token structure";
    public static final String EXCEPTION_JWT_AUTHENTICATION_PROCESSING_FAILED = "JWT authentication processing failed";
    public static final String EXCEPTION_JWT_SECURITY_CONTEXT_CREATION_FAILED = "Failed to create security context for JWT authentication";

    // ===================================================================
    // UNIFIED LOGGING MESSAGES - REST CLIENT CONFIGURATION
    // ===================================================================
    public static final String LOG_REST_CLIENT_CONFIG_INITIALIZED = "REST client configuration initialized";
    public static final String LOG_REST_CLIENT_FACTORY_CREATED = "HTTP request factory created";
    public static final String LOG_REST_CLIENT_TIMEOUTS_CONFIGURED = "REST client timeouts configured";
    public static final String LOG_REST_CLIENT_RESTTEMPLATE_CREATED = "RestTemplate bean created";
    public static final String LOG_REST_CLIENT_CONFIG_DETAILS = "REST client configuration details";

    // ===================================================================
    // LOGGER CATEGORIES - REST CLIENT
    // ===================================================================
    public static final String LOG_CATEGORY_REST_CLIENT = "REST-Client";

    // ===================================================================
    // INNER CLASSES FOR DOMAIN-SPECIFIC CONSTANTS
    // ===================================================================

    // ===================================================================
    // ERROR CONSTANTS
    // ===================================================================

    // Error Codes
    public static final String ERROR_CODE_FIELD_VALIDATION_ERROR = "FIELD_VALIDATION_ERROR";
    public static final String ERROR_CODE_REQUIRED_FIELD_MISSING = "REQUIRED_FIELD_MISSING";
    public static final String ERROR_CODE_UNIQUE_FIELD_VIOLATION = "UNIQUE_FIELD_VIOLATION";
    public static final String ERROR_CODE_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";

    // Error Messages
    public static final String ERROR_MESSAGE_FIELD_REQUIRED_SUFFIX = " is required";
    public static final String ERROR_MESSAGE_UNEXPECTED_ERROR = "An unexpected error occurred while processing your request";
    public static final String ERROR_MESSAGE_UNIQUE_FIELD_VIOLATION_FORMAT = "Field '%s' with value '%s' already exists";
    public static final String ERROR_MESSAGE_SUPPORT_CONTACT = "An unexpected error occurred. Please contact support if the problem persists.";
    public static final String ERROR_MESSAGE_UNIQUE_FIELD_SINGLE_FORMAT = "Field '%s' with value '%s' already exists for this schema and tenant. This field must be unique.";
    public static final String ERROR_MESSAGE_UNIQUE_FIELDS_MULTIPLE_PREFIX = "The following unique fields already exist for this schema and tenant: ";
    public static final String ERROR_MESSAGE_UNIQUE_FIELD_VALUE_FORMAT = "'%s' (value: '%s')";

    // Exception Messages for IllegalArgumentException
    public static final String EXCEPTION_MESSAGE_NULL_PARAMETERS = "fieldKey, fieldValue, and tenantId cannot be null";
    public static final String EXCEPTION_MESSAGE_ALL_PARAMETERS_NULL = "All parameters cannot be null";
    public static final String EXCEPTION_MESSAGE_EMPTY_VIOLATING_FIELDS = "violatingFields cannot be empty";
    public static final String EXCEPTION_MESSAGE_MISSING_DISPLAY_NAME = "Missing display name for field: ";
    public static final String EXCEPTION_MESSAGE_MISSING_VALUE = "Missing value for field: ";

    // Schema Field Classification Service Messages
    public static final String LOG_MESSAGE_FIELD_VALIDATION_FAILED_CLASSIFICATION = "Field validation failed during field classification";
    public static final String LOG_MESSAGE_UNIQUE_VALIDATION_FAILED_CLASSIFICATION = "Unique field validation failed during field classification";
    public static final String LOG_MESSAGE_FIELD_VALIDATION_FAILED_UPDATE_CLASSIFICATION = "Field validation failed during update field classification";
    public static final String LOG_MESSAGE_UNIQUE_VALIDATION_FAILED_UPDATE_CLASSIFICATION = "Unique field validation failed during update field classification";

    // Schema Field Classification Log Detail Keys
    public static final String LOG_DETAIL_METHOD_VALIDATE_REQUIRED_FIELDS = "validateRequiredFields";
    public static final String LOG_DETAIL_METHOD_VALIDATE_UNIQUE_FIELDS = "validateUniqueFields";
    public static final String LOG_DETAIL_METHOD_VALIDATE_PROVIDED_REQUIRED_FIELDS = "validateProvidedRequiredFields";
    public static final String LOG_DETAIL_METHOD_GET_SCHEMA_DEFINITION = "getSchemaDefinition";
    public static final String LOG_DETAIL_METHOD_PARSE_SCHEMA_RESPONSE = "parseSchemaResponse";
    public static final String LOG_DETAIL_METHOD_CLASSIFY_FIELDS = "classifyFields";
    public static final String LOG_DETAIL_METHOD_CLASSIFY_FIELDS_FOR_UPDATE = "classifyFieldsForUpdate";

    // Schema Field Classification Status Messages
    public static final String LOG_DETAIL_STATUS_MISSING_REQUIRED = "missing_required";
    public static final String LOG_DETAIL_STATUS_DUPLICATE_UNIQUE_VALUE = "duplicate_unique_value";
    public static final String LOG_DETAIL_STATUS_INVALID_REQUIRED_FIELD_UPDATE = "invalid_required_field_update";
    public static final String LOG_DETAIL_ACTION_SETTING_DEFAULT_VALUE = "setting_default_value";
    public static final String LOG_DETAIL_ACTION_CLASSIFIED_AS_EXTENDED = "classified as extended";
    public static final String LOG_DETAIL_ACTION_VALIDATION_FAILED = "validation_failed";
    public static final String LOG_DETAIL_ACTION_VALIDATION_SUCCESS = "validation_success";
    public static final String LOG_DETAIL_ACTION_UNIQUE_VALIDATION_FAILED = "unique_validation_failed";
    public static final String LOG_DETAIL_ACTION_UNIQUE_VALIDATION_SUCCESS = "unique_validation_success";
    public static final String LOG_DETAIL_ACTION_VALIDATION_FAILED_UPDATE = "validation_failed_update";
    public static final String LOG_DETAIL_ACTION_VALIDATION_SUCCESS_UPDATE = "validation_success_update";
    public static final String LOG_DETAIL_ACTION_FIELD_CLASSIFICATION_DETAILS = "field_classification_details";
    public static final String LOG_DETAIL_ACTION_FIELD_CLASSIFICATION_DETAILS_UPDATE = "field_classification_details_update";

    // Global Exception Handler Action Constants
    public static final String LOG_DETAIL_ACTION_VALIDATION_EXCEPTION_HANDLED = "validation_exception_handled";
    public static final String LOG_DETAIL_ACTION_UNIQUE_VIOLATION_EXCEPTION_HANDLED = "unique_violation_exception_handled";
    public static final String LOG_DETAIL_ACTION_GENERAL_EXCEPTION_HANDLED = "general_exception_handled";

    // Global Exception Handler Detail Keys
    public static final String LOG_DETAIL_KEY_MISSING_FIELDS = "missingFields";
    public static final String LOG_DETAIL_KEY_VIOLATED_FIELDS = "violatedFields";
    public static final String LOG_DETAIL_KEY_ERROR_MESSAGE = "errorMessage";
    public static final String LOG_DETAIL_KEY_REQUEST_PATH = "requestPath";
    public static final String LOG_DETAIL_KEY_ERROR_TYPE = "errorType";
    public static final String LOG_DETAIL_KEY_ACTION = "action";

    // URI Processing Constants
    public static final String URI_PREFIX_TO_REMOVE = "uri=";
    public static final String URI_REPLACEMENT = "";

    // Identity Controller Operation Names
    public static final String LOG_DETAIL_OPERATION_CREATE_IDENTITY_WITH_FIELD_CLASSIFICATION = "createIdentityWithFieldClassification";
    public static final String LOG_DETAIL_OPERATION_GET_IDENTITY_BY_ID = "getIdentityById";
    public static final String LOG_DETAIL_OPERATION_UPDATE_IDENTITY = "updateIdentity";
    public static final String LOG_DETAIL_OPERATION_UPDATE_IDENTITY_WITH_FIELD_CLASSIFICATION = "updateIdentityWithFieldClassification";
    public static final String LOG_DETAIL_OPERATION_DELETE_IDENTITY = "deleteIdentity";
    public static final String LOG_DETAIL_OPERATION_GET_IDENTITIES_BY_TENANT_AND_SCHEMA = "getIdentitiesByTenantAndSchema";

    // Identity Controller Detail Keys
    public static final String LOG_DETAIL_KEY_OPERATION = "operation";
    public static final String LOG_DETAIL_KEY_TENANT_ID = "tenantId";
    public static final String LOG_DETAIL_KEY_SCHEMA_ID = "schemaId";
    public static final String LOG_DETAIL_KEY_FIELD_COUNT = "fieldCount";
    public static final String LOG_DETAIL_KEY_IDENTITY_ID = "identityId";
    public static final String LOG_DETAIL_KEY_STATUS_CODE = "statusCode";
    public static final String LOG_DETAIL_KEY_ERROR = "error";
    public static final String LOG_DETAIL_KEY_AUTH_HEADER = "authHeader";
    public static final String LOG_DETAIL_KEY_RESULT_COUNT = "resultCount";
    public static final String LOG_DETAIL_KEY_PRIMARY_SCHEMA_ID = "primarySchemaId";

    // Identity Controller JWT Messages
    public static final String LOG_JWT_TOKEN_FOUND_IN_REQUEST = "JWT token found in request";
    public static final String LOG_JWT_TOKEN_NOT_PROVIDED_TESTING = "No JWT token provided - using null for testing";

    // Identity Controller JWT Detail Values
    public static final String LOG_DETAIL_VALUE_AUTH_HEADER_PRESENT_INVALID = "Present but invalid format";
    public static final String LOG_DETAIL_VALUE_AUTH_HEADER_MISSING = "Missing";

    // Identity Controller HTTP Header Constants
    public static final String HTTP_HEADER_AUTHORIZATION = "Authorization";
    public static final String HTTP_BEARER_PREFIX = "Bearer ";
    public static final int HTTP_BEARER_PREFIX_LENGTH = 7;

    // ===================================================================
    // SCHEMA FIELD CLASSIFICATION SERVICE CONSTANTS
    // ===================================================================

    // Schema Field Classification Service - Additional Detail Keys (Missing Ones)
    public static final String LOG_DETAIL_KEY_SCHEMA_FIELD_COUNT = "schemaFieldCount";
    public static final String LOG_DETAIL_KEY_INPUT_FIELD_COUNT = "inputFieldCount";
    public static final String LOG_DETAIL_KEY_TOTAL_MISSING = "totalMissing";
    public static final String LOG_DETAIL_KEY_REQUIRED_FIELDS_CHECKED = "requiredFieldsChecked";
    public static final String LOG_DETAIL_KEY_TOTAL_VIOLATIONS = "totalViolations";
    public static final String LOG_DETAIL_KEY_UNIQUE_FIELDS_CHECKED = "uniqueFieldsChecked";
    public static final String LOG_DETAIL_KEY_VALUE = "value";
    public static final String LOG_DETAIL_KEY_IS_CORE = "isCore";
    public static final String LOG_DETAIL_KEY_OBJECT = "object";
    public static final String LOG_DETAIL_KEY_CORE_FIELDS = "coreFields";
    public static final String LOG_DETAIL_KEY_EXTENDED_FIELDS = "extendedFields";
    public static final String LOG_DETAIL_KEY_TOTAL_FIELDS = "totalFields";
    public static final String LOG_DETAIL_KEY_PROVIDED_REQUIRED_FIELDS = "providedRequiredFields";
    public static final String LOG_DETAIL_KEY_CORE_SERVICE_URL = "coreServiceUrl";
    public static final String LOG_DETAIL_KEY_QUERY_PARAMS = "queryParams";
    public static final String LOG_DETAIL_KEY_REQUEST_URL = "requestUrl";
    public static final String LOG_DETAIL_KEY_RESPONSE_SIZE = "responseSize";
    public static final String LOG_DETAIL_KEY_HTTP_STATUS = "httpStatus";
    public static final String LOG_DETAIL_KEY_ERROR_DETAILS = "errorDetails";
    public static final String LOG_DETAIL_KEY_REQUEST_HEADERS = "requestHeaders";
    public static final String LOG_DETAIL_KEY_RESPONSE_HEADERS = "responseHeaders";
    public static final String LOG_DETAIL_KEY_IS_REQUIRED = "isRequired";
    public static final String LOG_DETAIL_KEY_DEFAULT_VALUE = "defaultValue";
    public static final String LOG_DETAIL_KEY_IS_MULTIVALUED = "isMultivalued";
    public static final String LOG_DETAIL_KEY_CLASSIFICATION_RESULT = "classificationResult";
    public static final String LOG_DETAIL_KEY_CORE_FIELD_COUNT = "coreFieldCount";
    public static final String LOG_DETAIL_KEY_EXTENDED_FIELD_COUNT = "extendedFieldCount";
    public static final String LOG_DETAIL_KEY_CORE_FIELD_NAMES = "coreFieldNames";
    public static final String LOG_DETAIL_KEY_EXTENDED_FIELD_NAMES = "extendedFieldNames";
    public static final String LOG_DETAIL_KEY_VALIDATION_MESSAGE = "validationMessage";
    public static final String LOG_DETAIL_KEY_FIELD_NAME = "fieldName";
    public static final String LOG_DETAIL_KEY_DISPLAY_NAME = "displayName";
    public static final String LOG_DETAIL_KEY_INVALID_FIELDS = "invalidFields";
    public static final String LOG_DETAIL_KEY_TOTAL_INVALID = "totalInvalid";
    public static final String LOG_DETAIL_KEY_PROVIDED_FIELDS_CHECKED = "providedFieldsChecked";
    public static final String LOG_DETAIL_KEY_FIELD_COUNT_TOTAL = "fieldCount";
    public static final String LOG_DETAIL_KEY_SOURCE = "source";
    public static final String LOG_DETAIL_KEY_AUTH_TYPE = "authType";
    public static final String LOG_DETAIL_KEY_REASON = "reason";
    public static final String LOG_DETAIL_KEY_PARSE_MODE = "parseMode";
    public static final String LOG_DETAIL_KEY_RESPONSE_TYPE = "responseType";
    public static final String LOG_DETAIL_KEY_RESPONSE_BODY = "responseBody";
    public static final String LOG_DETAIL_KEY_COMPONENT = "component";
    public static final String LOG_DETAIL_KEY_STATUS = "status";
    public static final String LOG_DETAIL_KEY_METHOD = "method";

    // Schema Field Classification Service - Object Names
    public static final String LOG_DETAIL_OBJECT_SCHEMA_DEFINITION = "SchemaDefinition";

    // Schema Field Classification Service - Component and Status Values
    public static final String LOG_DETAIL_COMPONENT_SCHEMA_FIELD_CLASSIFICATION_SERVICE = "SchemaFieldClassificationService";
    public static final String LOG_DETAIL_STATUS_INITIALIZED = "initialized";
    public static final String LOG_DETAIL_OPERATION_UPDATE = "update";

    // Schema Field Classification Service - Messages
    public static final String LOG_MESSAGE_FIELD_VALIDATION_FAILED_DURING_CLASSIFICATION = "Field validation failed during field classification";
    public static final String LOG_MESSAGE_UNIQUE_FIELD_VALIDATION_FAILED_DURING_CLASSIFICATION = "Unique field validation failed during field classification";
    public static final String LOG_MESSAGE_FIELD_VALIDATION_FAILED_DURING_UPDATE_CLASSIFICATION = "Field validation failed during update field classification";
    public static final String LOG_MESSAGE_UNIQUE_FIELD_VALIDATION_FAILED_DURING_UPDATE_CLASSIFICATION = "Unique field validation failed during update field classification";

    // Schema Field Classification Service - Authentication Constants
    public static final String LOG_DETAIL_AUTH_TYPE_BEARER_JWT = "Bearer JWT";
    public static final String LOG_DETAIL_REASON_NO_JWT_TOKEN = "no JWT token provided";
    public static final String LOG_DETAIL_SOURCE_CORE_SERVICE_API = "core-service API";

    // Schema Field Classification Service - Parsing Constants
    public static final String LOG_DETAIL_PARSE_MODE_ARRAY_RESPONSE = "array response";
    public static final String LOG_DETAIL_PARSE_MODE_OBJECT_RESPONSE = "object response";

    // Log Categories for Exception Handling
    public static final String LOG_CATEGORY_GENERAL_EXCEPTION = "general-exception";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - METHODS
    // ===================================================================
    public static final String LOG_DETAIL_METHOD_CREATE_IDENTITY_WITH_FIELD_CLASSIFICATION = "createIdentityWithFieldClassification";
    public static final String LOG_DETAIL_METHOD_UPDATE_IDENTITY = "updateIdentity";
    public static final String LOG_DETAIL_METHOD_FIND_BY_ID = "findById";
    public static final String LOG_DETAIL_METHOD_FIND_BY_TENANT_ID = "findByTenantId";
    public static final String LOG_DETAIL_METHOD_FIND_BY_TENANT_AND_SCHEMA = "findByTenantIdAndPrimarySchemaId";
    public static final String LOG_DETAIL_METHOD_FIND_BY_STATUS_AND_TENANT = "findByStatusAndTenantId";
    public static final String LOG_DETAIL_METHOD_DELETE_IDENTITY = "deleteIdentity";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - PROCESSING STEPS
    // ===================================================================
    public static final String LOG_DETAIL_STEP_REQUEST_VALIDATION = "request validation";
    public static final String LOG_DETAIL_STEP_FIELD_CLASSIFICATION = "field classification";
    public static final String LOG_DETAIL_STEP_IDENTITY_OBJECT_CONSTRUCTION = "identity object construction";
    public static final String LOG_DETAIL_STEP_ATTRIBUTES_SERIALIZATION = "attributes serialization";
    public static final String LOG_DETAIL_STEP_POSTGRESQL_PERSISTENCE = "PostgreSQL persistence";
    public static final String LOG_DETAIL_STEP_POSTGRESQL_UPDATE_OPERATION = "PostgreSQL update operation";
    public static final String LOG_DETAIL_STEP_POSTGRESQL_DELETION_OPERATION = "PostgreSQL deletion operation";
    public static final String LOG_DETAIL_STEP_DATABASE_QUERY_EXECUTION = "database query execution";
    public static final String LOG_DETAIL_STEP_PAGINATED_DATABASE_QUERY = "paginated database query";
    public static final String LOG_DETAIL_STEP_FILTERED_DATABASE_QUERY = "filtered database query";
    public static final String LOG_DETAIL_STEP_FILTERED_PAGINATED_DATABASE_QUERY = "filtered paginated database query";
    public static final String LOG_DETAIL_STEP_IDENTITY_EXISTENCE_CHECK = "identity existence check";
    public static final String LOG_DETAIL_STEP_IDENTITY_VALIDATION = "identity validation";
    public static final String LOG_DETAIL_STEP_DGRAPH_SYNCHRONIZATION = "Dgraph synchronization";
    public static final String LOG_DETAIL_STEP_DGRAPH_UPDATE_OPERATION = "Dgraph update operation";
    public static final String LOG_DETAIL_STEP_DGRAPH_DELETION_OPERATION = "Dgraph deletion operation";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - PHASES
    // ===================================================================
    public static final String LOG_DETAIL_PHASE_ATTRIBUTE_MAPPING = "attribute mapping";
    public static final String LOG_DETAIL_PHASE_JSON_CONVERSION = "JSON conversion";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - SERVICES
    // ===================================================================
    public static final String LOG_DETAIL_SERVICE_SCHEMA_FIELD_CLASSIFICATION = "SchemaFieldClassificationService";
    public static final String LOG_DETAIL_SERVICE_IDENTITY_REPOSITORY = "IdentityRepository";
    public static final String LOG_DETAIL_SERVICE_DGRAPH_SYNC = "DgraphSyncService";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - OBJECTS
    // ===================================================================
    public static final String LOG_DETAIL_OBJECT_FIELD_CLASSIFICATION = "FieldClassification";
    public static final String LOG_DETAIL_OBJECT_IDENTITY = "Identity";
    public static final String LOG_DETAIL_OBJECT_PAGE_RESULT = "PageResult";
    public static final String LOG_DETAIL_OBJECT_LIST_RESULT = "ListResult";
    public static final String LOG_DETAIL_OBJECT_FILTERED_PAGE_RESULT = "FilteredPageResult";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - OPERATIONS
    // ===================================================================
    public static final String LOG_DETAIL_OPERATION_FIND_BY_ID = "findById";
    public static final String LOG_DETAIL_OPERATION_FIND_BY_TENANT_ID = "findByTenantId";
    public static final String LOG_DETAIL_OPERATION_FIND_BY_TENANT_AND_SCHEMA = "findByTenantIdAndPrimarySchemaId";
    public static final String LOG_DETAIL_OPERATION_FIND_BY_STATUS_AND_TENANT = "findByStatusAndTenantId";
    public static final String LOG_DETAIL_OPERATION_REMOVE_IDENTITY_FROM_GRAPH = "removeIdentityFromGraph";
    public static final String LOG_DETAIL_OPERATION_UPDATE_IDENTITY_IN_GRAPH = "updateIdentityInGraph";
    public static final String LOG_DETAIL_OPERATION_DELETE = "delete";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - QUERY TYPES
    // ===================================================================
    public static final String LOG_DETAIL_QUERY_TYPE_FIND_BY_ID = "findById";
    public static final String LOG_DETAIL_QUERY_TYPE_FIND_BY_TENANT_ID = "findByTenantId";
    public static final String LOG_DETAIL_QUERY_TYPE_FIND_BY_TENANT_AND_SCHEMA = "findByTenantIdAndPrimarySchemaId";
    public static final String LOG_DETAIL_QUERY_TYPE_FIND_BY_STATUS_AND_TENANT = "findByStatusAndTenantId";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - FILTER CRITERIA
    // ===================================================================
    public static final String LOG_DETAIL_FILTER_CRITERIA_TENANT_AND_SCHEMA = "tenantId + primarySchemaId";
    public static final String LOG_DETAIL_FILTER_CRITERIA_STATUS_AND_TENANT = "status + tenantId";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - CONDITIONS
    // ===================================================================
    public static final String LOG_DETAIL_CONDITION_CORE_FIELDS_PRESENT = "core fields present";
    public static final String LOG_DETAIL_CONDITION_EXTENDED_FIELDS_PRESENT = "extended fields present";
    public static final String LOG_DETAIL_CONDITION_IDENTITY_FOUND = "identity found";
    public static final String LOG_DETAIL_CONDITION_IDENTITY_NOT_FOUND = "identity not found";
    public static final String LOG_DETAIL_CONDITION_IDENTITY_FOUND_FOR_DELETION = "identity found for deletion";
    public static final String LOG_DETAIL_CONDITION_IDENTITY_NOT_FOUND_FOR_DELETION = "identity not found for deletion";
    public static final String LOG_DETAIL_CONDITION_DGRAPH_SYNC_FAILED_CONTINUING = "Dgraph sync failed - continuing with PostgreSQL only";
    public static final String LOG_DETAIL_CONDITION_DGRAPH_UPDATE_FAILED_SUCCESS = "Dgraph update failed - PostgreSQL update successful";
    public static final String LOG_DETAIL_CONDITION_DGRAPH_DELETION_FAILED_CONTINUING = "Dgraph deletion failed - continuing with PostgreSQL deletion";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - RESULTS
    // ===================================================================
    public static final String LOG_DETAIL_RESULT_SUCCESS = "success";
    public static final String LOG_DETAIL_RESULT_FAILURE = "failure";
    public static final String LOG_DETAIL_RESULT_FOUND = "found";
    public static final String LOG_DETAIL_RESULT_NOT_FOUND = "not found";
    public static final String LOG_DETAIL_RESULT_VALIDATION_FAILURE = "validation failure";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - STATES
    // ===================================================================
    public static final String LOG_DETAIL_STATE_TRANSIENT = "transient";
    public static final String LOG_DETAIL_STATE_PERSISTENT = "persistent";
    public static final String LOG_DETAIL_STATE_DELETED = "deleted";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - FIELDS
    // ===================================================================
    public static final String LOG_DETAIL_FIELD_DGRAPH_NODE_ID = "dgraphNodeId";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - RESILIENCE
    // ===================================================================
    public static final String LOG_DETAIL_RESILIENCE_ENABLED = "enabled";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - EXCEPTIONS
    // ===================================================================
    public static final String LOG_DETAIL_EXCEPTION_FIELD_VALIDATION = "FieldValidationException";

    // ===================================================================
    // VALIDATION ERROR MESSAGES
    // ===================================================================
    public static final String LOG_FIELD_VALIDATION_FAILED_DURING_CREATION = "Field validation failed during identity creation";
    public static final String LOG_UNIQUE_FIELD_VALIDATION_FAILED_DURING_CREATION = "Unique field validation failed during identity creation";
    public static final String LOG_UNIQUE_FIELD_VALIDATION_FAILED_DURING_UPDATE = "Unique field validation failed during identity update";

    // ===================================================================
    // DETAILED LOGGING CONSTANTS - FIELD VALUES
    // ===================================================================
    public static final String LOG_DETAIL_CORE_FIELDS_CONTENT = "coreFieldsContent";
    public static final String LOG_DETAIL_EXTENDED_FIELDS_CONTENT = "extendedFieldsContent";

    // ===================================================================

    /**
     * Constants specific to Identity entity.
     */
    public static final class Identity {

        // Field length limits
        public static final int TENANT_ID_MAX_LENGTH = 100;
        public static final int DEPARTMENT_MAX_LENGTH = 255;
        public static final int JOB_TITLE_MAX_LENGTH = 255;
        public static final int DGRAPH_NODE_ID_MAX_LENGTH = 100;
        public static final int COST_CENTER_MAX_LENGTH = 50;
        public static final int LOCATION_MAX_LENGTH = 255;
        public static final int IDENTITY_TYPE_MAX_LENGTH = 50;
        public static final int SOURCE_SYSTEM_MAX_LENGTH = 100;
        public static final int EXTERNAL_ID_MAX_LENGTH = 255;

        // Validation messages
        public static final String PRIMARY_SCHEMA_ID_REQUIRED_MESSAGE = "Primary schema ID is required";
        public static final String IDENTITY_STATUS_REQUIRED_MESSAGE = "Identity status is required";
        public static final String TENANT_ID_REQUIRED_MESSAGE = "Tenant ID is required";
        public static final String TENANT_ID_SIZE_MESSAGE = "Tenant ID cannot exceed {max} characters";
        public static final String DEPARTMENT_SIZE_MESSAGE = "Department cannot exceed {max} characters";
        public static final String JOB_TITLE_SIZE_MESSAGE = "Job title cannot exceed {max} characters";
        public static final String DGRAPH_NODE_ID_SIZE_MESSAGE = "Dgraph node ID cannot exceed {max} characters";
        public static final String COST_CENTER_SIZE_MESSAGE = "Cost center cannot exceed {max} characters";
        public static final String LOCATION_SIZE_MESSAGE = "Location cannot exceed {max} characters";
        public static final String IDENTITY_TYPE_SIZE_MESSAGE = "Identity type cannot exceed {max} characters";
        public static final String SOURCE_SYSTEM_SIZE_MESSAGE = "Source system cannot exceed {max} characters";
        public static final String EXTERNAL_ID_SIZE_MESSAGE = "External ID cannot exceed {max} characters";

        private Identity() {
            // Utility class - prevent instantiation
        }
    }

    // ===================================================================
    // AUDIT CONSTANTS
    // ===================================================================

    // Audit Event Types
    public static final String AUDIT_EVENT_CREATE = "create";
    public static final String AUDIT_EVENT_UPDATE = "update";
    public static final String AUDIT_EVENT_DELETE = "delete";

    // Audit Entity Types
    public static final String AUDIT_ENTITY_TYPE_IDENTITY = "IDENTITY";
    public static final String AUDIT_ENTITY_TYPE_EXTENDED_ATTRIBUTE_DEFINITION = "EXTENDED_ATTRIBUTE_DEFINITION";

    // Audit Status
    public static final String AUDIT_STATUS_SUCCESS = "success";
    public static final String AUDIT_STATUS_FAILURE = "failure";

    // Audit Risk Levels
    public static final String AUDIT_RISK_LOW = "low";
    public static final String AUDIT_RISK_MEDIUM = "medium";
    public static final String AUDIT_RISK_HIGH = "high";

    // Audit Actor Types
    public static final String AUDIT_ACTOR_TYPE_USER = "user";
    public static final String AUDIT_ACTOR_TYPE_SYSTEM = "system";

    // ===================================================================
    // AUDIT LOG MESSAGES
    // ===================================================================

    // Audit Aspect Messages
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_IDENTITY_CREATION = "Audit aspect triggered for identity creation";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_IDENTITY_UPDATE = "Audit aspect triggered for identity update";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_IDENTITY_DELETION = "Audit aspect triggered for identity deletion";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_SCHEMA_DRIVEN_CREATION = "Audit aspect triggered for schema-driven identity creation";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_SCHEMA_DRIVEN_UPDATE = "Audit aspect triggered for schema-driven identity update";

    // Audit Operation Messages
    public static final String AUDIT_LOG_AUDITING_IDENTITY_CREATION = "Auditing identity creation";
    public static final String AUDIT_LOG_AUDITING_IDENTITY_UPDATE = "Auditing identity update";
    public static final String AUDIT_LOG_AUDITING_IDENTITY_DELETION = "Auditing identity deletion";
    public static final String AUDIT_LOG_AUDITING_SCHEMA_DRIVEN_CREATION = "Auditing schema-driven identity creation";
    public static final String AUDIT_LOG_AUDITING_SCHEMA_DRIVEN_UPDATE = "Auditing schema-driven identity update";

    // Audit State Retrieval Messages
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_IDENTITY_STATE_BEFORE_UPDATE = "Could not retrieve identity state before update";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_IDENTITY_STATE_BEFORE_DELETION = "Could not retrieve identity state before deletion";

    // Audit Success Messages
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_IDENTITY_CREATION = "Successfully published audit event for identity creation";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_IDENTITY_UPDATE = "Successfully published audit event for identity update";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_IDENTITY_DELETION = "Successfully published audit event for identity deletion";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_SCHEMA_DRIVEN_CREATION = "Successfully published audit event for schema-driven identity creation";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_SCHEMA_DRIVEN_UPDATE = "Successfully published audit event for schema-driven identity update";

    // Audit Failure Messages
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_IDENTITY_CREATION = "Failed to publish audit event for identity creation";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_IDENTITY_UPDATE = "Failed to publish audit event for identity update";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_IDENTITY_DELETION = "Failed to publish audit event for identity deletion";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_SCHEMA_DRIVEN_CREATION = "Failed to publish audit event for schema-driven identity creation";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_SCHEMA_DRIVEN_UPDATE = "Failed to publish audit event for schema-driven identity update";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_IDENTITY_AUDIT_EVENT = "Failed to create or publish identity audit event";

    // Audit Publisher Messages
    public static final String AUDIT_LOG_CONVERTING_EVENT = "Converting identity audit event to standardized format";
    public static final String AUDIT_LOG_PUBLISHING_EVENT = "Publishing standardized audit event to Kafka";
    public static final String AUDIT_LOG_SUCCESS_SENT = "Successfully sent audit event to Kafka";
    public static final String AUDIT_LOG_FAILED_PUBLISH = "Failed to publish identity audit event to Kafka";

    // Audit Metadata
    public static final String AUDIT_SOURCE_IDENTITY_SERVICE = "identity-service";
    public static final String AUDIT_TOPIC_NAME = "audit-log";
    public static final String AUDIT_LOGGER_NAME = "AuditEventPublisher";
    public static final String AUDIT_NOT_AVAILABLE = "N/A";

    // Audit Metadata Field Names
    public static final String AUDIT_METADATA_IDENTITY_NAME = "identityName";
    public static final String AUDIT_METADATA_TENANT_ID = "tenantId";
    public static final String AUDIT_METADATA_ENTITY_TYPE = "entityType";
    public static final String AUDIT_METADATA_USER_IP = "userIp";
    public static final String AUDIT_METADATA_SOURCE = "source";

    // Audit Target Object Field Names
    public static final String AUDIT_TARGET_TYPE = "type";
    public static final String AUDIT_TARGET_OBJECT_ID = "objectId";
    public static final String AUDIT_TARGET_OBJECT_NAME = "objectName";

    // Audit Log Detail Field Names
    public static final String AUDIT_DETAIL_ACTION = "action";
    public static final String AUDIT_DETAIL_ENTITY_TYPE = "entityType";
    public static final String AUDIT_DETAIL_ENTITY_ID = "entityId";
    public static final String AUDIT_DETAIL_KAFKA_TOPIC = "kafkaTopic";
    public static final String AUDIT_DETAIL_EVENT_TYPE = "eventType";
    public static final String AUDIT_DETAIL_OBJECT_ID = "objectId";
    public static final String AUDIT_DETAIL_EXCEPTION_TYPE = "exceptionType";

    // ===================================================================
    // AUDIT CONSTANTS ENTITLEMENT MANAGEMENT
    // ===================================================================

    // ===================================================================
    //  Entitlement Rule Audit Action Names (Human-Readable)
    // ===================================================================
    public static final String AUDIT_EVENT_RULE_CREATED = "Entitlement Rule Created";
    public static final String AUDIT_EVENT_RULE_UPDATED = "Entitlement Rule Updated";
    public static final String AUDIT_EVENT_RULE_DELETED = "Entitlement Rule Deleted";
    public static final String AUDIT_EVENT_RULE_ENABLED = "Entitlement Rule Enabled";
    public static final String AUDIT_EVENT_RULE_DISABLED = "Entitlement Rule Disabled";
    public static final String AUDIT_EVENT_GLOBAL_SCHEDULE_UPDATED = "Entitlement Global Schedule Updated";
    public static final String AUDIT_EVENT_RULE_EVALUATED = "Entitlement Rule Evaluated";
    public static final String AUDIT_EVENT_ENTITLEMENT_ASSIGNED = "Entitlement Assigned";
    public static final String AUDIT_EVENT_ENTITLEMENT_REVOKED = "Entitlement Revoked";

    // ===================================================================
    //  Entitlement Rule Audit Entity Types
    // ===================================================================
    public static final String AUDIT_ENTITY_TYPE_ASSIGNMENT_RULE = "Entitlement assignment rule";
    public static final String AUDIT_ENTITY_TYPE_ENTITLEMENT_ASSIGNMENT = "Entitlement assignment";

    // ===================================================================
    //  Entitlement Rule Audit Trigger Sources
    // ===================================================================
    public static final String AUDIT_TRIGGER_SOURCE_API = "API";
    public static final String AUDIT_TRIGGER_SOURCE_IMMEDIATE = "IMMEDIATE";
    public static final String AUDIT_TRIGGER_SOURCE_CRON = "CRON";
    public static final String AUDIT_TRIGGER_SOURCE_EVALUATE_ALL = "Evaluate all entitlement rules";

    // ===================================================================
    //  Entitlement Rule Audit Global Schedule Entity
    // ===================================================================
    public static final String AUDIT_GLOBAL_SCHEDULE_NAME = "Global Schedule";

    // ===================================================================
    //  Entitlement Rule Audit Details Field Keys
    // ===================================================================
    public static final String AUDIT_DETAIL_RULE_NAME = "ruleName";
    public static final String AUDIT_DETAIL_ENTITLEMENT_NAME = "entitlementName";
    public static final String AUDIT_DETAIL_ENTITLEMENT_ID = "entitlementId";
    public static final String AUDIT_DETAIL_RESOURCE_ID = "resourceId";
    public static final String AUDIT_DETAIL_TRIGGER_SOURCE = "triggerSource";
    public static final String AUDIT_DETAIL_EXECUTION_ID = "executionId";
    public static final String AUDIT_DETAIL_IDENTITIES_MATCHED = "identitiesMatched";
    public static final String AUDIT_DETAIL_ASSIGNMENTS_CREATED = "assignmentsCreated";
    public static final String AUDIT_DETAIL_ASSIGNMENTS_REVOKED = "assignmentsRevoked";
    public static final String AUDIT_DETAIL_SKIPPED_NO_SHADOW = "skippedNoShadow";
    public static final String AUDIT_DETAIL_DURATION_MS = "durationMs";
    public static final String AUDIT_DETAIL_RULE_ID = "ruleId";
    public static final String AUDIT_DETAIL_IDENTITY_ID = "identityId";
    public static final String AUDIT_DETAIL_IDENTITY_NAME = "identityName";
    public static final String AUDIT_DETAIL_SHADOW_ID = "shadowId";
    public static final String AUDIT_DETAIL_IDENTITY_TYPE = "identityType";
    public static final String AUDIT_DETAIL_IDENTITY_TYPES = "identityTypes";
    // Audit Trigger Sources
    public static final String AUDIT_TRIGGER_API = "API";
    public static final String AUDIT_TRIGGER_IMMEDIATE = "IMMEDIATE";
    public static final String AUDIT_TRIGGER_CRON = "CRON";
    public static final String AUDIT_TRIGGER_EVALUATE_ALL = "EVALUATE_ALL";

    // ===================================================================
    //  Entitlement Rule Audit Snapshot Field Names
    // ===================================================================
    public static final String AUDIT_FIELD_NAME = "name";
    public static final String AUDIT_FIELD_ENABLED = "enabled";
    public static final String AUDIT_FIELD_SCHEDULE_TYPE = "scheduleType";
    public static final String AUDIT_FIELD_CRON_EXPRESSION = "cronExpression";
    public static final String AUDIT_FIELD_SCHEDULE_TIME = "scheduleTime";
    public static final String AUDIT_FIELD_SCHEDULE_TIMEZONE = "scheduleTimezone";
    public static final String AUDIT_FIELD_SCHEDULE_DATE = "scheduleDate";
    public static final String AUDIT_FIELD_PRIORITY = "priority";
    public static final String AUDIT_FIELD_IDENTITY_TYPE = "identityType";
    public static final String AUDIT_FIELD_IDENTITY_TYPES = "identityTypes";
    public static final String AUDIT_FIELD_CONDITION_GROUPS = "conditionGroups";
    public static final String AUDIT_FIELD_TOTAL_ENABLED_RULES = "totalEnabledRules";

    // ===================================================================
    //  Entitlement Rule Audit Operation Names (for logging)
    // ===================================================================
    public static final String AUDIT_OPERATION_UPDATE_RULE = "updateRule";
    public static final String AUDIT_OPERATION_DELETE_RULE = "deleteRule";
    public static final String AUDIT_OPERATION_GLOBAL_SCHEDULE = "globalScheduleUpdate";

    // ===================================================================
    //  Entitlement Rule Audit Misc
    // ===================================================================
    public static final String AUDIT_DETAIL_OPERATION = "operation";
    public static final String AUDIT_DETAIL_ERROR = "error";
    public static final String AUDIT_UNKNOWN = "Unknown";
    public static final String AUDIT_RULE_NAME_PREFIX = "Dynamic Rule - ";
    public static final String AUDIT_TRIGGER_SOURCE_ENTITLEMENT_RULE_CREATED = "Entitlement rule created";
    public static final String AUDIT_TRIGGER_SOURCE_ENTITLEMENT_RULE_UPDATED = "Entitlement rule updated";

    // Audit operations
    public static final String AUDIT_OPERATION_EVALUATE_RULE = "evaluateRule";
    public static final String AUDIT_OPERATION_EVALUATE_ALL = "evaluateAllRules";

    // Audit log messages
    public static final String AUDIT_LOG_EVALUATE_RULE_AUDIT_FAILED = "Failed to publish evaluate-rule audit event";
    public static final String AUDIT_LOG_EVALUATE_ALL_AUDIT_FAILED = "Failed to publish evaluate-all audit event";

    // Audit Source Entitlement
    public static final String AUDIT_SOURCE_ENTITLEMENT_SERVICE = "entitlement-management-service";

    // Audit Actor System
    public static final String AUDIT_ACTOR_SYSTEM_ID = "SYSTEM";
    public static final String AUDIT_ACTOR_SYSTEM_USERNAME = "SYSTEM";

    // ===================================================================
//  Audit Event Descriptions Human-readable, user-friendly
// ===================================================================

    // Rule CRUD
    public static final String AUDIT_DESC_RULE_CREATED = "A new entitlement assignment rule was created";
    public static final String AUDIT_DESC_RULE_UPDATED = "An entitlement assignment rule was updated";
    public static final String AUDIT_DESC_RULE_DELETED = "An entitlement assignment rule was deleted";
    public static final String AUDIT_DESC_RULE_ENABLED = "An entitlement assignment rule was enabled";
    public static final String AUDIT_DESC_RULE_DISABLED = "An entitlement assignment rule was disabled";

    // Rule Evaluation
    public static final String AUDIT_DESC_RULE_EVALUATED_PREFIX = "Entitlement assignment rule was evaluated";
    public static final String AUDIT_DESC_RULE_EVALUATED_VIA_CREATE = "Entitlement assignment rule was automatically evaluated after creation";
    public static final String AUDIT_DESC_RULE_EVALUATED_VIA_UPDATE = "Entitlement assignment rule was automatically evaluated after update";
    public static final String AUDIT_DESC_RULE_EVALUATED_VIA_API = "Entitlement assignment rule was manually evaluated via API";
    public static final String AUDIT_DESC_RULE_EVALUATED_VIA_CRON = "Entitlement assignment rule was evaluated by the scheduled cron job";
    public static final String AUDIT_DESC_RULE_EVALUATED_VIA_EVALUATE_ALL = "Entitlement assignment rule was evaluated as part of a bulk evaluate-all operation";

    // Assignments
    public static final String AUDIT_DESC_ENTITLEMENT_ASSIGNED = "An entitlement was assigned to an identity based on a matching rule";
    public static final String AUDIT_DESC_ENTITLEMENT_REVOKED = "An entitlement was revoked from an identity because the rule no longer matches";

    // Global Schedule
    public static final String AUDIT_DESC_GLOBAL_SCHEDULE_UPDATED = "The global evaluation schedule was updated for all enabled rules";

    // Entitlement Governance
    public static final String AUDIT_DESC_ENTITLEMENT_GOVERNANCE_UPDATED = "Entitlement governance metadata was updated";

    // Detail key for description
    public static final String AUDIT_DETAIL_MESSAGE = "message";

    // Audit Metadata Field Names Entitlement
    public static final String AUDIT_METADATA_TRIGGER_SOURCE = "triggerSource";
    public static final String AUDIT_METADATA_ENTITLEMENT_ID = "entitlementId";
    public static final String AUDIT_METADATA_ENTITLEMENT_NAME = "entitlementName";
    public static final String AUDIT_METADATA_RESOURCE_ID = "resourceId";
    public static final String AUDIT_METADATA_IDENTITY_TYPE = "identityType";
    public static final String AUDIT_METADATA_IDENTITY_TYPES = "identityTypes";
    public static final String AUDIT_METADATA_RULE_ID = "ruleId";
    public static final String AUDIT_METADATA_RULE_NAME = "ruleName";
    public static final String AUDIT_METADATA_IDENTITY_ID = "identityId";
    public static final String AUDIT_METADATA_SHADOW_ID = "shadowId";
    public static final String AUDIT_METADATA_EXECUTION_ID = "executionId";
    public static final String AUDIT_METADATA_IDENTITIES_MATCHED = "identitiesMatched";
    public static final String AUDIT_METADATA_ASSIGNMENTS_CREATED = "assignmentsCreated";
    public static final String AUDIT_METADATA_ASSIGNMENTS_REVOKED = "assignmentsRevoked";
    public static final String AUDIT_METADATA_SKIPPED_NO_SHADOW = "skippedNoShadow";
    public static final String AUDIT_METADATA_DURATION_MS = "durationMs";

    // Audit Log Messages Entitlement
    public static final String AUDIT_LOG_PUBLISHING_ENTITLEMENT_EVENT = "Publishing entitlement audit event";
    public static final String AUDIT_LOG_ENTITLEMENT_EVENT_PUBLISHED = "Entitlement audit event published successfully";
    public static final String AUDIT_LOG_ENTITLEMENT_EVENT_FAILED = "Failed to publish entitlement audit event operation is NOT affected";
    public static final String AUDIT_LOG_EVALUATION_AUDIT_FAILED = "Failed to publish evaluation audit event evaluation result is NOT affected";
    public static final String AUDIT_LOG_CREATE_RULE_AUDIT_FAILED = "Failed to publish create rule audit event";
    public static final String AUDIT_LOG_UPDATE_RULE_AUDIT_FAILED = "Failed to publish update rule audit event";
    public static final String AUDIT_LOG_DELETE_RULE_AUDIT_FAILED = "Failed to publish delete rule audit event";
    public static final String AUDIT_LOG_GLOBAL_SCHEDULE_AUDIT_FAILED = "Failed to publish global schedule audit event";
    public static final String AUDIT_LOG_BEFORE_STATE_CAPTURE_FAILED = "Could not capture before state for audit";

    // Audit Global Entity
    public static final String AUDIT_GLOBAL_ENTITY_ID = "GLOBAL";
    public static final String AUDIT_GLOBAL_ENTITY_NAME = "Global Schedule";


    // ===================================================================
//  Entitlement Governance Audit Constants
// ===================================================================

    // ===================================================================
    //  Audit Aspect — Log Messages for Rule Evaluation
    // ===================================================================

    public static final String AUDIT_LOG_SKIPPING_ASPECT_AUDIT = "Skipping aspect audit — handled by engine/evaluate-all aspect";
    public static final String AUDIT_LOG_STARTING_RULE_EVAL_AUDIT = "Starting audit for rule evaluation via engine";
    public static final String AUDIT_LOG_RULE_CONTEXT_RESOLVED = "Rule context resolved for audit";
    public static final String AUDIT_LOG_RULE_NOT_FOUND_FOR_AUDIT = "Rule not found for audit context resolution";
    public static final String AUDIT_LOG_RULE_EVAL_COMPLETED = "Rule evaluation completed, publishing audit events";
    public static final String AUDIT_LOG_KAFKA_PUBLISHED_RULE_EVAL = "Kafka audit event published for rule evaluation";

    // ===================================================================
    //  Audit Aspect — Detail Keys
    // ===================================================================
    public static final String AUDIT_DETAIL_TENANT_ID = "tenantId";
    public static final String AUDIT_DETAIL_TRIGGER_TYPE = "triggerType";
    public static final String AUDIT_DETAIL_TRIGGERED_BY = "triggeredBy";
    public static final String AUDIT_DETAIL_RESOLVED_ACTOR = "resolvedActor";
    public static final String AUDIT_DETAIL_NULL = "null";

    // Action
    public static final String AUDIT_EVENT_ENTITLEMENT_GOVERNANCE_UPDATED = "Entitlement updated";

    // Entity type
    public static final String AUDIT_ENTITY_TYPE_ENTITLEMENT = "Entitlement";

    // Log messages
    public static final String AUDIT_LOG_PUBLISHING_ENTITLEMENT_GOVERNANCE_EVENT = "Publishing entitlement governance audit event";
    public static final String AUDIT_LOG_ENTITLEMENT_GOVERNANCE_EVENT_PUBLISHED = "Entitlement governance audit event published";
    public static final String AUDIT_LOG_ENTITLEMENT_GOVERNANCE_EVENT_FAILED = "Failed to publish entitlement governance audit event";

    // Detail keys governance-specific
    public static final String AUDIT_DETAIL_RESOURCE_NAME = "resourceName";
    public static final String AUDIT_DETAIL_LIFECYCLE = "lifecycle";
    public static final String AUDIT_DETAIL_RISK_LEVEL = "riskLevel";
    public static final String AUDIT_DETAIL_KIND = "kind";
    public static final String AUDIT_DETAIL_RISK_REASON = "riskReason";
    public static final String AUDIT_DETAIL_REQUESTABLE = "requestable";
    public static final String AUDIT_DETAIL_PRIVILEGE_LEVEL = "privilegeLevel";
    public static final String AUDIT_DETAIL_OWNER_ID = "ownerId";
    public static final String AUDIT_DETAIL_REQUIRE_APPROVAL = "requireApproval";
    public static final String AUDIT_DETAIL_ALLOWED_AUTOMATED_GRANT = "allowedAutomatedGrant";
    public static final String AUDIT_DETAIL_MAX_DURATION_MS = "maxDurationMs";
    public static final String AUDIT_DETAIL_AUTO_EXPIRE_DURATION_MS = "autoExpireDurationMs";

    // Metadata keys governance-specific
    public static final String AUDIT_METADATA_RESOURCE_NAME = "resourceName";
    public static final String AUDIT_METADATA_RISK_LEVEL = "riskLevel";
    public static final String AUDIT_METADATA_PRIVILEGE_LEVEL = "privilegeLevel";
    public static final String AUDIT_METADATA_OWNER_ID = "ownerId";

    // ===================================================================
    // DGRAPH INITIALIZER CONSTANTS
    // ===================================================================

    // DgraphInitializer Method Names
    public static final String DGRAPH_INITIALIZER_CONSTRUCTOR = "DgraphInitializer";
    public static final String DGRAPH_INITIALIZER_RUN_METHOD = "run";

    // DgraphInitializer Detail Field Names
    public static final String DGRAPH_DETAIL_METHOD = "method";
    public static final String DGRAPH_DETAIL_SYNC_SERVICE_CLASS = "syncServiceClass";
    public static final String DGRAPH_DETAIL_PROFILE = "profile";
    public static final String DGRAPH_DETAIL_ARGS_COUNT = "argsCount";
    public static final String DGRAPH_DETAIL_STEP = "step";
    public static final String DGRAPH_DETAIL_ERROR_MESSAGE = "errorMessage";
    public static final String DGRAPH_DETAIL_ERROR_TYPE = "errorType";
    public static final String DGRAPH_DETAIL_EXCEPTION = "exception";

    // DgraphInitializer Step Names
    public static final String DGRAPH_STEP_STARTING_SCHEMA_INIT = "Starting schema initialization";
    public static final String DGRAPH_STEP_SCHEMA_INIT_SUCCESSFUL = "Schema initialization successful";
    public static final String DGRAPH_STEP_DGRAPH_INIT_FAILED = "Dgraph initialization failed, continuing in PostgreSQL-only mode";

    // DgraphInitializer Profile Values
    public static final String DGRAPH_PROFILE_NOT_TEST = "!test";

    // ===================================================================
    // SCHEMA SYNCHRONIZATION CONSTANTS
    // ===================================================================

    // Schema Sync Logger Names

    // Schema Sync Log Categories
    public static final String SCHEMA_SYNC_LOG_CATEGORY = "schema-field-sync";

    // Schema Sync Operations
    public static final String SCHEMA_SYNC_OPERATION_ADD_FIELD = "addField";
    public static final String SCHEMA_SYNC_OPERATION_REMOVE_FIELD = "removeField";

    // Schema Sync Log Messages - Field Operations
    public static final String SCHEMA_SYNC_LOG_ADDING_FIELD_TO_IDENTITIES = "Adding field to all identities using schema";
    public static final String SCHEMA_SYNC_LOG_REMOVING_FIELD_FROM_IDENTITIES = "Removing field from all identities using schema";
    public static final String SCHEMA_SYNC_LOG_FOUND_IDENTITIES_TO_UPDATE = "Found identities to update";
    public static final String SCHEMA_SYNC_LOG_NO_IDENTITIES_TO_UPDATE = "No identities found to update";
    public static final String SCHEMA_SYNC_LOG_COMPLETED_FIELD_ADDITION = "Completed field addition to identities";
    public static final String SCHEMA_SYNC_LOG_COMPLETED_FIELD_REMOVAL = "Completed field removal from identities";

    // Schema Sync Log Messages - Individual Identity Updates
    public static final String SCHEMA_SYNC_LOG_ADDED_CORE_FIELD = "Added core field to identity";
    public static final String SCHEMA_SYNC_LOG_ADDED_EXTENDED_FIELD = "Added extended field to identity";
    public static final String SCHEMA_SYNC_LOG_REMOVED_CORE_FIELD = "Removed core field from identity";
    public static final String SCHEMA_SYNC_LOG_REMOVED_EXTENDED_FIELD = "Removed extended field from identity";

    // Schema Sync Log Messages - Errors
    public static final String SCHEMA_SYNC_LOG_FAILED_UPDATE_IDENTITY = "Failed to update individual identity";
    public static final String SCHEMA_SYNC_LOG_BULK_UPDATE_FAILED = "Bulk update operation failed";

    // Schema Sync Detail Field Names
    public static final String SCHEMA_SYNC_DETAIL_IDENTITY_COUNT = "identityCount";
    public static final String SCHEMA_SYNC_DETAIL_IDENTITY_ID = "identityId";
    public static final String SCHEMA_SYNC_DETAIL_TOTAL_IDENTITIES = "totalIdentities";
    public static final String SCHEMA_SYNC_DETAIL_UPDATED_COUNT = "updatedCount";
    public static final String SCHEMA_SYNC_DETAIL_OPERATION = "operation";

    // Schema Sync Error Messages
    public static final String SCHEMA_SYNC_ERROR_BULK_UPDATE_FAILED = "Bulk update operation failed";
    public static final String SCHEMA_SYNC_ERROR_EVENT_PROCESSING_FAILED = "Schema field change event processing failed";

    // Schema Sync - Additional Log Messages
    public static final String SCHEMA_SYNC_LOG_RECEIVED_EVENT = "Received schema field change event from core-service";
    public static final String SCHEMA_SYNC_LOG_PROCESSING_FIELD_ADDITION = "Processing field addition for schema";
    public static final String SCHEMA_SYNC_LOG_PROCESSING_FIELD_DELETION = "Processing field deletion for schema";
    public static final String SCHEMA_SYNC_LOG_PROCESSING_FIELD_UPDATE = "Processing field update for schema";
    public static final String SCHEMA_SYNC_LOG_FIELD_UPDATE_NO_ACTION = "Field update requires no data modification";
    public static final String SCHEMA_SYNC_LOG_UNKNOWN_CHANGE_TYPE = "Unknown change type in schema field change event";
    public static final String SCHEMA_SYNC_LOG_EVENT_PROCESSED_SUCCESSFULLY = "Schema field change event processed successfully";
    public static final String SCHEMA_SYNC_LOG_EVENT_PROCESSING_FAILED = "Failed to process schema field change event";

    // Schema Sync - Additional Detail Fields
    public static final String SCHEMA_SYNC_DETAIL_CHANGE_TYPE = "changeType";
    public static final String SCHEMA_SYNC_DETAIL_KAFKA_PARTITION = "kafkaPartition";
    public static final String SCHEMA_SYNC_DETAIL_KAFKA_OFFSET = "kafkaOffset";
    public static final String SCHEMA_SYNC_DETAIL_REASON = "reason";
    public static final String SCHEMA_SYNC_DETAIL_ERROR_TYPE = "errorType";

    // Schema Sync - Reason Codes
    public static final String SCHEMA_SYNC_REASON_FIELD_UPDATE_NO_DATA_CHANGE = "Field update does not require data modification";

    // ===================================================================
    // IDENTITY AUDIT ASPECT CONSTANTS
    // ===================================================================

    // Identity Audit Aspect Default Values
    public static final String IDENTITY_AUDIT_UNKNOWN_VALUE = "UNKNOWN";
    public static final String IDENTITY_AUDIT_PREFIX = "Identity-";

    // Identity Audit JSON Field Names
    public static final String IDENTITY_JSON_FIELD_EMAIL = "email";
    public static final String IDENTITY_JSON_FIELD_USERNAME = "username";
    public static final String IDENTITY_JSON_FIELD_FIRST_NAME = "firstName";
    public static final String IDENTITY_JSON_FIELD_LAST_NAME = "lastName";

    // Identity Audit Messages
    public static final String IDENTITY_AUDIT_FAILED_EXTRACT_DISPLAY_NAME = "Failed to extract display name from identity";

    // ===================================================================
    // DATABASE TRIGGER INITIALIZER CONSTANTS
    // ===================================================================

    // Database Trigger Initializer Component Names
    public static final String LOG_DETAIL_COMPONENT_DATABASE_TRIGGER_INITIALIZER = "DatabaseTriggerInitializer";
    public static final String LOG_DETAIL_OBJECT_DATABASE_TRIGGER_INITIALIZER = "DatabaseTriggerInitializer";

    // Database Trigger Initializer Method Names
    public static final String LOG_DETAIL_METHOD_DATABASE_TRIGGER_INITIALIZER_RUN = "run";
    public static final String LOG_DETAIL_METHOD_CREATE_STATISTICS_TABLE = "createStatisticsTableIfNotExists";
    public static final String LOG_DETAIL_METHOD_CREATE_INCREMENT_FUNCTION = "createIncrementFunction";
    public static final String LOG_DETAIL_METHOD_CREATE_INSERT_TRIGGER_FUNCTION = "createInsertTriggerFunction";
    public static final String LOG_DETAIL_METHOD_CREATE_DELETE_TRIGGER_FUNCTION = "createDeleteTriggerFunction";
    public static final String LOG_DETAIL_METHOD_CREATE_UPDATE_TRIGGER_FUNCTION = "createUpdateTriggerFunction";
    public static final String LOG_DETAIL_METHOD_CREATE_TRIGGERS = "createTriggers";

    // Database Trigger Initializer Step Names
    public static final String LOG_DETAIL_STEP_DATABASE_TRIGGER_INITIALIZATION = "database trigger initialization";
    public static final String LOG_DETAIL_STEP_STATISTICS_TABLE_CREATION = "statistics table creation";
    public static final String LOG_DETAIL_STEP_FUNCTION_CREATION = "function creation";
    public static final String LOG_DETAIL_STEP_TRIGGER_CREATION = "trigger creation";
    public static final String LOG_DETAIL_STEP_INDEX_CREATION = "index creation";

    // Database Trigger Initializer Operation Names
    public static final String LOG_DETAIL_OPERATION_CREATE_STATISTICS_TABLE = "create statistics table";
    public static final String LOG_DETAIL_OPERATION_CREATE_INDEXES = "create indexes";
    public static final String LOG_DETAIL_OPERATION_CREATE_INCREMENT_FUNCTION = "create increment function";
    public static final String LOG_DETAIL_OPERATION_CREATE_INSERT_TRIGGER_FUNCTION = "create insert trigger function";
    public static final String LOG_DETAIL_OPERATION_CREATE_DELETE_TRIGGER_FUNCTION = "create delete trigger function";
    public static final String LOG_DETAIL_OPERATION_CREATE_UPDATE_TRIGGER_FUNCTION = "create update trigger function";
    public static final String LOG_DETAIL_OPERATION_DROP_EXISTING_TRIGGERS = "drop existing triggers";
    public static final String LOG_DETAIL_OPERATION_CREATE_NEW_TRIGGERS = "create new triggers";

    // Database Trigger Initializer Messages
    public static final String LOG_DATABASE_TRIGGER_INITIALIZATION_STARTED = "Database trigger initialization started";
    public static final String LOG_DATABASE_TRIGGER_INITIALIZATION_COMPLETED = "Database trigger initialization completed successfully";
    public static final String LOG_DATABASE_TRIGGER_INITIALIZATION_FAILED = "Database trigger initialization failed";
    public static final String LOG_DATABASE_TRIGGER_INITIALIZATION_FAILED_CONTINUING = "Database trigger initialization failed, application will continue without field usage statistics";
    public static final String LOG_STATISTICS_TABLE_CREATION_STARTED = "Statistics table creation started";
    public static final String LOG_STATISTICS_TABLE_CREATION_COMPLETED = "Statistics table and indexes created successfully";
    public static final String LOG_FUNCTION_CREATION_COMPLETED = "Database function created successfully";
    public static final String LOG_TRIGGER_CREATION_COMPLETED = "Database triggers created successfully";
    public static final String LOG_INDEX_CREATION_COMPLETED = "Database indexes created successfully";

    // Database Trigger Initializer Detail Keys
    public static final String LOG_DETAIL_TABLE_NAME = "tableName";
    public static final String LOG_DETAIL_FUNCTION_NAME = "functionName";
    public static final String LOG_DETAIL_TRIGGER_NAME = "triggerName";
    public static final String LOG_DETAIL_INDEX_NAME = "indexName";
    public static final String LOG_DETAIL_INDEX_COUNT = "indexCount";
    public static final String LOG_DETAIL_FUNCTION_COUNT = "functionCount";
    public static final String LOG_DETAIL_TRIGGER_COUNT = "triggerCount";

    // Database Trigger Initializer Exception Messages
    public static final String EXCEPTION_DATABASE_TRIGGER_INITIALIZATION_FAILED = "Failed to initialize database triggers";
    public static final String EXCEPTION_STATISTICS_TABLE_CREATION_FAILED = "Failed to create statistics table";
    public static final String EXCEPTION_DATABASE_FUNCTION_CREATION_FAILED = "Failed to create database function";
    public static final String EXCEPTION_DATABASE_TRIGGER_CREATION_FAILED = "Failed to create database triggers";

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

    // ===================================================================
    // GENERAL CONSTANTS
    // ===================================================================

    private AppConstants() {
        // Private constructor to prevent instantiation
    }

    // ===================================================================
    // LOGGER CONSTANTS
    // ===================================================================

    // Logger Names
    public static final String IDENTITY_SCHEMA_SERVICE_LOGGER_NAME = "IdentitySchemaService";
    public static final String IDENTITY_SCHEMA_CONTROLLER_LOGGER_NAME = "IdentitySchemaController";

    // Logger Categories
    public static final String LOG_CATEGORY_IDENTITY_SCHEMA = "identity-schema";
    public static final String LOG_CATEGORY_IDENTITY_SCHEMA_SERVICE = "identity-schema-service";
    public static final String LOG_CATEGORY_IDENTITY_SCHEMA_CONTROLLER = "identity-schema-controller";

    // ===================================================================
    // API PATHS
    // ===================================================================

    public static final String API_BASE_PATH = "/api/v1";
    public static final String API_IDENTITY_SCHEMAS_PATH = "/v1/identity-schemas";
    public static final String API_PATH_BY_ID = "/{id}";
    public static final String API_PATH_BY_TENANT = "/tenant/{tenantId}";
    public static final String API_PATH_BY_TENANT_AND_CODE = "/tenant/{tenantId}/code/{schemaCode}";
    public static final String API_PATH_DEFAULT_SCHEMA = "/tenant/{tenantId}/default";
    public static final String API_PATH_ACTIVE_SCHEMAS = "/tenant/{tenantId}/active";
    public static final String API_ACTIVE_PATH = "/active";
    public static final String API_DEFAULT_PATH = "/default";
    public static final String API_SET_DEFAULT_PATH = "/set-default";
    public static final String API_ACTIVATE_PATH = "/activate";
    public static final String API_DEACTIVATE_PATH = "/deactivate";
    public static final String API_CHECK_AVAILABILITY_PATH = "/check-availability";
    public static final String API_BY_PARENT_PATH = "/by-parent";

    // Schema Fields API Paths
    public static final String API_FIELDS_PATH = "/fields";
    public static final String API_FIELD_BY_ID_PATH = "/fields/{fieldId}";
    public static final String API_FIELD_STATISTICS_PATH = "/fields/statistics";
    public static final String API_CORE_FIELDS_PATH = "/core-fields";
    public static final String API_STATISTICS_PATH = "/statistics";

    // ===================================================================
    // HTTP HEADERS AND PARAMETERS
    // ===================================================================

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String PARAM_TENANT_ID = "tenantId";
    public static final String PARAM_SCHEMA_CODE = "schemaCode";
    public static final String PARAM_FIELD_ID = "fieldId";

    // ===================================================================
    // HTTP RESPONSE MESSAGES
    // ===================================================================

    // Success Messages
    public static final String MSG_IDENTITY_SCHEMA_CREATED = "Identity schema created successfully";
    public static final String MSG_IDENTITY_SCHEMA_UPDATED = "Identity schema updated successfully";
    public static final String MSG_IDENTITY_SCHEMA_DELETED = "Identity schema deleted successfully";
    public static final String MSG_IDENTITY_SCHEMA_RETRIEVED = "Identity schema retrieved successfully";
    public static final String MSG_IDENTITY_SCHEMAS_RETRIEVED = "Identity schemas retrieved successfully";
    public static final String MSG_DEFAULT_SCHEMA_SET = "Default schema set successfully";
    public static final String FIELD_DELETE_SUCCESS = "Field deleted successfully";

    // Schema Fields Success Messages
    public static final String MSG_SCHEMA_FIELD_ADDED = "Schema field added successfully";
    public static final String MSG_SCHEMA_FIELDS_RETRIEVED = "Schema fields retrieved successfully";
    public static final String MSG_SCHEMA_FIELD_RETRIEVED = "Schema field retrieved successfully";
    public static final String MSG_SCHEMA_FIELD_STATISTICS_RETRIEVED = "Schema field statistics retrieved successfully";
    public static final String MSG_CORE_FIELD_ADDED = "Core field added successfully";
    public static final String MSG_FIELD_DELETED = "Field deleted successfully";
    public static final String MSG_SCHEMA_NOT_ACTIVE = "Schema with ID %s is not active";

    // Error Messages
    public static final String MSG_IDENTITY_SCHEMA_NOT_FOUND = "Identity schema not found";
    public static final String MSG_IDENTITY_SCHEMA_NOT_FOUND_WITH_ID = "Identity schema not found with ID: %s";
    public static final String MSG_IDENTITY_SCHEMA_NOT_FOUND_WITH_CODE = "Identity schema not found with code: %s for tenant: %s";
    public static final String MSG_IDENTITY_SCHEMA_CODE_EXISTS = "Identity schema with code '%s' already exists for tenant '%s'";
    public static final String MSG_IDENTITY_SCHEMA_DEFAULT_EXISTS = "A default identity schema already exists for tenant '%s'";
    public static final String MSG_INVALID_TENANT_ID = "Invalid tenant ID provided";
    public static final String MSG_INVALID_SCHEMA_CODE = "Invalid schema code provided";
    public static final String MSG_INVALID_SCHEMA_DATA = "Invalid schema data provided";
    public static final String MSG_SCHEMA_IN_USE = "Schema cannot be deleted as it is currently in use";

    // Schema Fields Error Messages
    public static final String MSG_SCHEMA_FIELD_NOT_FOUND = "Schema field not found";
    public static final String MSG_DUPLICATE_FIELD_KEY = "Field with key '%s' already exists in schema '%s'";
    public static final String MSG_INACTIVE_SCHEMA = "Cannot add fields to inactive schema with ID: %s";
    public static final String MSG_INVALID_FIELD_DATA = "Invalid field data provided";

    // Field Management Validation Messages
    public static final String MSG_SCHEMA_ID_NULL = "Schema ID cannot be null";
    public static final String MSG_FIELD_CREATE_DATA_NULL = "Field creation data cannot be null";
    public static final String MSG_FIELD_UPDATE_DATA_NULL = "Field update data cannot be null";
    public static final String MSG_TENANT_ID_NULL_OR_EMPTY = "Tenant ID cannot be null or empty";
    public static final String MSG_SCHEMA_NOT_FOUND_WITH_ID = "Schema not found with ID: %s";
    public static final String MSG_SCHEMA_NOT_FOUND_FOR_TENANT = "Schema not found for tenant: %s";
    public static final String MSG_CANNOT_ADD_FIELDS_TO_INACTIVE_SCHEMA = "Cannot add fields to inactive schema: %s";
    public static final String MSG_FIELD_KEY_ALREADY_EXISTS = "Field with system key '%s' already exists in schema: %s";
    // Core Field and Deletion Messages
    public static final String MSG_FIELD_ID_NULL = "Field ID cannot be null";
    public static final String MSG_FIELD_NOT_FOUND_WITH_ID = "Field not found with ID: %s";
    public static final String MSG_FIELD_NOT_FOUND_IN_SCHEMA = "Field with ID '%s' not found in schema '%s'";
    public static final String MSG_CANNOT_DELETE_CORE_FIELD = "Cannot delete core field with ID: %s";
    public static final String MSG_SYSTEM_KEY_NOT_EDITABLE = "System key cannot be modified after field creation for security reasons";
    public static final String MSG_ONLY_ALLOWED_FIELDS_EDITABLE = "Only displayName, isRequired, isSensitive, isWritable, and description fields can be updated";
    public static final String MSG_CORE_FIELD_ADDED_SUCCESSFULLY = "Core field added successfully";
    public static final String MSG_FIELD_DELETED_SUCCESSFULLY = "Field deleted successfully";
    public static final String MSG_FIELD_UPDATED_SUCCESSFULLY = "Field updated successfully";

    // ===================================================================
    // LOG MESSAGE FORMATS
    // ===================================================================

    // Method Entry/Exit Logging
    public static final String LOG_ENTERING_METHOD = "Entering %s";
    public static final String LOG_EXITING_METHOD = "Exiting %s";
    public static final String LOG_METHOD_START = "Starting method %s";
    public static final String LOG_METHOD_SUCCESS = "Method %s completed successfully";
    public static final String LOG_METHOD_COMPLETED = "Method %s completed successfully";
    public static final String LOG_METHOD_ERROR = "Error in method %s: %s";

    // Service Layer Logging
    public static final String LOG_CREATING_IDENTITY_SCHEMA = "Creating identity schema with code: %s for tenant: %s";
    public static final String LOG_IDENTITY_SCHEMA_CREATED = "Identity schema created successfully with ID: %s";
    public static final String LOG_UPDATING_IDENTITY_SCHEMA = "Updating identity schema with ID: %s";
    public static final String LOG_IDENTITY_SCHEMA_UPDATED = "Identity schema updated successfully with ID: %s";
    public static final String LOG_DELETING_IDENTITY_SCHEMA = "Deleting identity schema with ID: %s";
    public static final String LOG_IDENTITY_SCHEMA_DELETED = "Identity schema deleted successfully with ID: %s";
    public static final String LOG_RETRIEVING_IDENTITY_SCHEMA = "Retrieving identity schema with ID: %s";
    public static final String LOG_RETRIEVING_SCHEMAS_BY_TENANT = "Retrieving identity schemas for tenant: %s";
    public static final String LOG_RETRIEVING_ACTIVE_SCHEMAS = "Retrieving active identity schemas for tenant: %s";
    public static final String LOG_SETTING_DEFAULT_SCHEMA = "Setting schema with ID: %s as default for tenant: %s";
    public static final String LOG_VALIDATING_SCHEMA_CODE = "Validating schema code: %s for tenant: %s";

    // Dgraph Synchronization Logging
    public static final String LOG_SCHEMA_SYNCED_TO_DGRAPH_SUCCESS = "Schema synced to Dgraph successfully";
    public static final String LOG_SCHEMA_SYNC_TO_DGRAPH_FAILED = "Failed to sync schema to Dgraph, continuing with PostgreSQL operation";
    public static final String LOG_SCHEMA_CODE_VALIDATION_RESULT = "Schema code validation result: %s";

    // Controller Layer Logging
    public static final String LOG_REQUEST_RECEIVED = "Request received: %s %s";
    public static final String LOG_REQUEST_PROCESSED = "Request processed successfully: %s %s";
    public static final String LOG_REQUEST_FAILED = "Request failed: %s %s - %s";
    public static final String LOG_INVALID_REQUEST_DATA = "Invalid request data received for %s";
    public static final String LOG_VALIDATION_ERROR = "Validation error in %s: %s";

    // Data Validation Logging
    public static final String LOG_CHECKING_SCHEMA_EXISTS = "Checking if schema exists with code: %s for tenant: %s";
    public static final String LOG_SCHEMA_EXISTS_RESULT = "Schema exists check result: %s";
    public static final String LOG_CHECKING_DEFAULT_SCHEMA = "Checking for existing default schema for tenant: %s";
    public static final String LOG_DEFAULT_SCHEMA_CHECK_RESULT = "Default schema check result: %s";

    // ===================================================================
    // LOG DETAIL KEYS
    // ===================================================================

    public static final String DETAIL_SCHEMA_ID = "schemaId";
    public static final String DETAIL_SCHEMA_CODE = "schemaCode";
    public static final String DETAIL_SCHEMA_NAME = "schemaName";
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_VERSION = "version";
    public static final String DETAIL_IS_ACTIVE = "isActive";
    public static final String DETAIL_IS_DEFAULT = "isDefault";
    public static final String DETAIL_PARENT_SCHEMA_ID = "parentSchemaId";
    public static final String DETAIL_CREATED_BY = "createdBy";
    public static final String DETAIL_PAGE_NUMBER = "pageNumber";
    public static final String DETAIL_PAGE_SIZE = "pageSize";
    public static final String DETAIL_TOTAL_ELEMENTS = "totalElements";
    public static final String DETAIL_TOTAL_PAGES = "totalPages";
    public static final String DETAIL_REQUEST_METHOD = "requestMethod";
    public static final String DETAIL_REQUEST_PATH = "requestPath";
    public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";
    public static final String DETAIL_EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String DETAIL_PARENT_SCHEMA_ID_KEY = "parentSchemaId";

    // Usage Statistics Detail Keys
    public static final String DETAIL_STATS_COUNT = "statsCount";
    public static final String DETAIL_ERROR = "error";

    // Field Management Detail Keys
    public static final String DETAIL_FIELD_ID = "fieldId";
    public static final String DETAIL_SYSTEM_KEY = "systemKey";
    public static final String DETAIL_IS_CORE_FIELD = "isCoreField";
    public static final String DETAIL_KAFKA_ERROR = "kafkaError";
    public static final String DETAIL_DATA_TYPE = "dataType";
    public static final String DETAIL_FIELD_COUNT = "fieldCount";

    // ===================================================================
    // METHOD NAMES (for consistent logging)
    // ===================================================================

    public static final String METHOD_CREATE_IDENTITY_SCHEMA = "createIdentitySchema";
    public static final String METHOD_UPDATE_IDENTITY_SCHEMA = "updateIdentitySchema";
    public static final String METHOD_DELETE_IDENTITY_SCHEMA = "deleteIdentitySchema";
    public static final String METHOD_GET_IDENTITY_SCHEMA_BY_ID = "getIdentitySchemaById";
    public static final String METHOD_GET_SCHEMAS_BY_TENANT = "getSchemasByTenant";
    public static final String METHOD_GET_SCHEMA_BY_TENANT_AND_CODE = "getSchemaByTenantAndCode";
    public static final String METHOD_GET_ACTIVE_SCHEMAS = "getActiveSchemas";
    public static final String METHOD_GET_DEFAULT_SCHEMA = "getDefaultSchema";
    public static final String METHOD_SET_DEFAULT_SCHEMA = "setDefaultSchema";
    public static final String METHOD_VALIDATE_SCHEMA_CODE = "validateSchemaCode";
    public static final String METHOD_SET_AS_DEFAULT = "setAsDefault";
    public static final String METHOD_ACTIVATE_SCHEMA = "activateSchema";
    public static final String METHOD_DEACTIVATE_SCHEMA = "deactivateSchema";
    public static final String METHOD_CREATE_SCHEMA_VERSION = "createSchemaVersion";
    public static final String METHOD_GET_SCHEMA_VERSIONS = "getSchemaVersions";
    public static final String METHOD_GET_SCHEMAS_BY_PARENT = "getSchemasByParent";
    public static final String METHOD_IS_SCHEMA_CODE_AVAILABLE = "isSchemaCodeAvailable";
    public static final String METHOD_ADD_FIELD_TO_SCHEMA = "addFieldToSchema";
    public static final String METHOD_ADD_CORE_FIELD_TO_SCHEMA = "addCoreFieldToSchema";
    public static final String METHOD_DELETE_FIELD = "deleteField";
    public static final String METHOD_UPDATE_FIELD = "updateField";

    // ===================================================================
    // VALIDATION CONSTANTS
    // ===================================================================

    // Field Validation Messages
    public static final String VALIDATION_SCHEMA_NAME_REQUIRED = "Schema name is required";
    public static final String VALIDATION_SCHEMA_CODE_REQUIRED = "Schema code is required";
    public static final String VALIDATION_TENANT_ID_REQUIRED = "Tenant ID is required";
    public static final String VALIDATION_IDENTITY_TYPE_REQUIRED = "Identity type is required";
    public static final String VALIDATION_CREATED_BY_REQUIRED = "Created by is required";
    public static final String VALIDATION_SCHEMA_NAME_LENGTH = "Schema name must be between 1 and 255 characters";
    public static final String VALIDATION_SCHEMA_CODE_LENGTH = "Schema code must be between 1 and 100 characters";
    public static final String VALIDATION_TENANT_ID_LENGTH = "Tenant ID must be between 1 and 100 characters";
    public static final String VALIDATION_DESCRIPTION_LENGTH = "Description cannot exceed 5000 characters";
    public static final String VALIDATION_MIGRATION_STRATEGY_LENGTH = "Migration strategy must be between 1 and 50 characters";
    public static final String VALIDATION_VERSION_POSITIVE = "Version must be a positive number";

    // Schema Field Validation Messages
    public static final String VALIDATION_FIELD_DISPLAY_NAME_REQUIRED = "Display name is required";
    public static final String VALIDATION_FIELD_DISPLAY_NAME_LENGTH = "Display name must not exceed 255 characters";
    public static final String VALIDATION_FIELD_SYSTEM_KEY_REQUIRED = "System key is required";
    public static final String VALIDATION_FIELD_SYSTEM_KEY_LENGTH = "System key must not exceed 100 characters";
    public static final String VALIDATION_FIELD_DATA_TYPE_REQUIRED = "Data type is required";
    public static final String VALIDATION_FIELD_DATA_TYPE_LENGTH = "Data type must not exceed 50 characters";
    public static final String VALIDATION_FIELD_DESCRIPTION_LENGTH = "Description must not exceed 1000 characters";
    public static final String VALIDATION_FIELD_RISK_LEVEL_LENGTH = "Risk level must not exceed 20 characters";
    public static final String VALIDATION_FIELD_DEFAULT_VALUE_LENGTH = "Default value must not exceed 1000 characters";
    public static final String VALIDATION_FIELD_USAGE_CONTEXT_LENGTH = "Usage context must not exceed 100 characters";
    public static final String VALIDATION_FIELD_SOURCE_SYSTEM_LENGTH = "Source system must not exceed 100 characters";
    public static final String VALIDATION_FIELD_LABEL_EN_LENGTH = "English label must not exceed 255 characters";
    public static final String VALIDATION_FIELD_LABEL_FR_LENGTH = "French label must not exceed 255 characters";
    public static final String VALIDATION_FIELD_LABEL_IT_LENGTH = "Italian label must not exceed 255 characters";
    public static final String VALIDATION_FIELD_LABEL_JA_LENGTH = "Japanese label must not exceed 255 characters";
    public static final String VALIDATION_FIELD_LABEL_ES_LENGTH = "Spanish label must not exceed 255 characters";
    public static final String VALIDATION_FIELD_LABEL_DE_LENGTH = "German label must not exceed 255 characters";
    public static final String VALIDATION_FIELD_LABEL_PT_LENGTH = "Portuguese label must not exceed 255 characters";
    public static final String VALIDATION_FIELD_LABEL_ZH_LENGTH = "Chinese label must not exceed 255 characters";

    // Schema Field Update Validation Messages
    public static final String VALIDATION_FIELD_NAME_UPDATE_LENGTH = "Field name must be between 1 and 255 characters";
    public static final String VALIDATION_FIELD_TYPE_UPDATE_LENGTH = "Field type must be between 1 and 50 characters";
    public static final String VALIDATION_FIELD_VALIDATION_RULES_LENGTH = "Validation rules cannot exceed 2000 characters";
    public static final String VALIDATION_FIELD_GROUP_NAME_LENGTH = "Group name cannot exceed 100 characters";
    public static final String VALIDATION_FIELD_DISPLAY_LABEL_LENGTH = "Display label cannot exceed 255 characters";
    public static final String VALIDATION_FIELD_PLACEHOLDER_TEXT_LENGTH = "Placeholder text cannot exceed 255 characters";

    // Schema Code Pattern
    public static final String SCHEMA_CODE_PATTERN = "^[a-zA-Z0-9_-]+$";
    public static final String VALIDATION_SCHEMA_CODE_PATTERN = "Schema code can only contain letters, numbers, underscores, and hyphens";

    // ===================================================================
    // FIELD UPDATE VALIDATION CONSTANTS
    // ===================================================================

    // Allowed Field Update Error Messages
    public static final String FIELD_UPDATE_ERROR_DATA_TYPE = "dataType cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_FIELD_TYPE = "fieldType cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_IS_MULTIVALUED = "isMultivalued cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_SYNC_FROM_SOURCE = "syncFromSource cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_RISK_LEVEL = "riskLevel cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_DEFAULT_VALUE = "defaultValue cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_USAGE_CONTEXT = "usageContext cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_SOURCE_SYSTEM = "sourceSystem cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_COMPLIANCE_TAGS = "complianceTags cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_LABEL_EN = "labelEn cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_LABEL_FR = "labelFr cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_LABEL_IT = "labelIt cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_LABEL_JA = "labelJa cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_LABEL_ES = "labelEs cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_LABEL_DE = "labelDe cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_LABEL_PT = "labelPt cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_LABEL_ZH = "labelZh cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_VALIDATION_RULES = "validationRules cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_IS_VISIBLE = "isVisible cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_IS_EDITABLE = "isEditable cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_GROUP_NAME = "groupName cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_DISPLAY_LABEL = "displayLabel cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_PLACEHOLDER_TEXT = "placeholderText cannot be updated. ";
    public static final String FIELD_UPDATE_ERROR_FIELD_NAME = "fieldName cannot be updated (use displayName instead). ";

    // Composite Error Message
    public static final String FIELD_UPDATE_ERROR_PREFIX = "Only displayName, isRequired, isSensitive, isWritable, and description fields can be updated. ";

    // ===================================================================
    // PAGINATION CONSTANTS
    // ===================================================================

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final String DEFAULT_SORT_FIELD = "schemaName";
    public static final String DEFAULT_SORT_DIRECTION = "ASC";

    // ===================================================================
    // BUSINESS LOGIC CONSTANTS
    // ===================================================================

    // Default Values
    public static final int DEFAULT_SCHEMA_VERSION = 1;
    public static final boolean DEFAULT_IS_ACTIVE = true;
    public static final boolean DEFAULT_IS_DEFAULT = false;

    // Migration Strategies
    public static final String MIGRATION_STRATEGY_MANUAL = "MANUAL";
    public static final String MIGRATION_STRATEGY_AUTOMATIC = "AUTOMATIC";
    public static final String MIGRATION_STRATEGY_VERSIONED = "VERSIONED";

    // Schema Types
    public static final String SCHEMA_TYPE_USER = "USER";
    public static final String SCHEMA_TYPE_EMPLOYEE = "EMPLOYEE";
    public static final String SCHEMA_TYPE_CONTRACTOR = "CONTRACTOR";
    public static final String SCHEMA_TYPE_SERVICE_ACCOUNT = "SERVICE_ACCOUNT";
    public static final String SCHEMA_TYPE_CUSTOM = "CUSTOM";

    // ===================================================================
    // AUDIT LOG CONSTANTS
    // ===================================================================

    // Audit Actions
    public static final String AUDIT_ACTION_CREATE = "CREATE_IDENTITY_SCHEMA";
    public static final String AUDIT_ACTION_UPDATE = "UPDATE_IDENTITY_SCHEMA";
    public static final String AUDIT_ACTION_DELETE = "DELETE_IDENTITY_SCHEMA";
    public static final String AUDIT_ACTION_VIEW = "VIEW_IDENTITY_SCHEMA";
    public static final String AUDIT_ACTION_LIST = "LIST_IDENTITY_SCHEMAS";
    public static final String AUDIT_ACTION_SET_DEFAULT = "SET_DEFAULT_SCHEMA";

    // Audit Object Types
    public static final String AUDIT_OBJECT_TYPE = "IDENTITY_SCHEMA";

    // Audit Messages
    public static final String AUDIT_SCHEMA_CREATED = "Identity schema created: %s";
    public static final String AUDIT_SCHEMA_UPDATED = "Identity schema updated: %s";
    public static final String AUDIT_SCHEMA_DELETED = "Identity schema deleted: %s";
    public static final String AUDIT_SCHEMA_VIEWED = "Identity schema viewed: %s";
    public static final String AUDIT_SCHEMAS_LISTED = "Identity schemas listed for tenant: %s";
    public static final String AUDIT_DEFAULT_SCHEMA_SET = "Default schema set for tenant: %s";

    // Audit Event Constants (Lowercase for standardized audit format)

    public static final String AUDIT_EVENT_CREATE_VERSION = "create_version";
    public static final String AUDIT_EVENT_SET_DEFAULT = "set_default";
    public static final String AUDIT_EVENT_ACTIVATE = "activate";
    public static final String AUDIT_EVENT_DEACTIVATE = "deactivate";

    // ===================================================================
    // AUDIT ASPECT LOG MESSAGES
    // ===================================================================

    // Audit Aspect Trigger Messages
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_SCHEMA_CREATION = "Audit aspect triggered for schema creation";
    public static final String AUDIT_LOG_AUDITING_SCHEMA_CREATION = "Auditing schema creation";
    public static final String AUDIT_LOG_AUDITING_SCHEMA_UPDATE = "Auditing schema update";
    public static final String AUDIT_LOG_AUDITING_SCHEMA_DELETION = "Auditing schema deletion";
    public static final String AUDIT_LOG_AUDITING_SCHEMA_VERSION_CREATION = "Auditing schema version creation";
    public static final String AUDIT_LOG_AUDITING_SCHEMA_SET_DEFAULT = "Auditing schema set as default";
    public static final String AUDIT_LOG_AUDITING_SCHEMA_ACTIVATION = "Auditing schema activation";
    public static final String AUDIT_LOG_AUDITING_SCHEMA_DEACTIVATION = "Auditing schema deactivation";
    public static final String AUDIT_LOG_AUDITING_FIELD_CREATION = "Auditing field creation";
    public static final String AUDIT_LOG_AUDITING_FIELD_UPDATE = "Auditing field update";
    public static final String AUDIT_LOG_AUDITING_FIELD_DELETION = "Auditing field deletion";

    // State Retrieval Messages
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_SCHEMA_STATE_BEFORE_UPDATE = "Could not retrieve schema state before update";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_SCHEMA_STATE_BEFORE_DELETION = "Could not retrieve schema state before deletion";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_SCHEMA_STATE_BEFORE_SET_DEFAULT = "Could not retrieve schema state before setting as default";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_SCHEMA_STATE_BEFORE_ACTIVATION = "Could not retrieve schema state before activation";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_SCHEMA_STATE_BEFORE_DEACTIVATION = "Could not retrieve schema state before deactivation";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_FIELD_STATE_BEFORE_UPDATE = "Could not retrieve field state before update";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_FIELD_STATE_BEFORE_DELETION = "Could not retrieve field state before deletion";

    // Audit Event Creation and Publishing Messages
    public static final String AUDIT_LOG_CREATING_AUDIT_EVENT = "Creating audit event";
    public static final String AUDIT_LOG_PUBLISHING_AUDIT_EVENT_TO_KAFKA = "Publishing audit event to Kafka";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_SCHEMA_CREATION = "Successfully published audit event for schema creation";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_SCHEMA_SET_DEFAULT = "Successfully published audit event for schema set as default";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_SCHEMA_ACTIVATION = "Successfully published audit event for schema activation";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_SCHEMA_DEACTIVATION = "Successfully published audit event for schema deactivation";

    // Audit Event Publishing Error Messages
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_SCHEMA_CREATION = "Failed to publish audit event for schema creation";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_SCHEMA_SET_DEFAULT = "Failed to publish audit event for schema set as default";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_SCHEMA_ACTIVATION = "Failed to publish audit event for schema activation";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_SCHEMA_DEACTIVATION = "Failed to publish audit event for schema deactivation";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_SCHEMA_AUDIT_EVENT = "Failed to create or publish schema audit event";

    // ===================================================================
    // SECURITY CONTEXT SERVICE LOG MESSAGES
    // ===================================================================

    // Security Context Warning Messages
    public static final String SECURITY_LOG_NO_HTTP_REQUEST_FOUND = "No HTTP request found in context. Assuming a system-level operation.";
    public static final String SECURITY_LOG_COULD_NOT_RETRIEVE_HTTP_REQUEST = "Could not retrieve HTTP request for audit context.";

    // User Extraction Success Messages
    public static final String SECURITY_LOG_USING_USER_INFO_FROM_GATEWAY_HEADERS = "Using user info from gateway headers";
    public static final String SECURITY_LOG_NO_USER_CONTEXT_FOUND = "No user context found, using SYSTEM user";
    public static final String SECURITY_LOG_SUCCESSFULLY_EXTRACTED_USER_FROM_JWT = "Successfully extracted user info from JWT token";
    public static final String SECURITY_LOG_USING_USER_INFO_FROM_SPRING_SECURITY = "Using user info from Spring Security context";

    // User Extraction Error Messages
    public static final String SECURITY_LOG_FAILED_TO_EXTRACT_USER_FROM_JWT = "Failed to extract user info from JWT token";
    public static final String SECURITY_LOG_FAILED_TO_EXTRACT_USER_FROM_SPRING_SECURITY = "Failed to extract user info from Spring Security context";

    // ===================================================================
    // IDENTITY SCHEMA SERVICE EXCEPTION MESSAGES
    // ===================================================================

    // Schema State Exception Messages
    public static final String EXCEPTION_SCHEMA_ALREADY_ACTIVE = "Schema with ID %s is already active";
    public static final String EXCEPTION_SCHEMA_ALREADY_INACTIVE = "Schema with ID %s is already inactive";
    public static final String EXCEPTION_SCHEMA_ALREADY_DEFAULT = "Schema with ID %s is already set as default for tenant %s";
    public static final String EXCEPTION_INVALID_DATA_TYPE = "Invalid data type: %s";

    // ===================================================================
    // ERROR CODES
    // ===================================================================

    public static final String ERROR_CODE_SCHEMA_NOT_FOUND = "IDENTITY_SCHEMA_NOT_FOUND";
    public static final String ERROR_CODE_SCHEMA_CODE_EXISTS = "IDENTITY_SCHEMA_CODE_EXISTS";
    public static final String ERROR_CODE_DEFAULT_SCHEMA_EXISTS = "DEFAULT_IDENTITY_SCHEMA_EXISTS";
    public static final String ERROR_CODE_INVALID_TENANT = "INVALID_TENANT_ID";
    public static final String ERROR_CODE_INVALID_SCHEMA_CODE = "INVALID_SCHEMA_CODE";
    public static final String ERROR_CODE_SCHEMA_IN_USE = "IDENTITY_SCHEMA_IN_USE";
    public static final String ERROR_CODE_VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String ERROR_CODE_INTERNAL_ERROR = "INTERNAL_SERVER_ERROR";

    // ===================================================================
    // ADDITIONAL LOG MESSAGES
    // ===================================================================

    // Service Operation Messages
    public static final String LOG_SETTING_SCHEMA_AS_DEFAULT = "Setting schema as default: %s";
    public static final String LOG_RETRIEVING_SCHEMAS_BY_PARENT = "Retrieving schemas by parent ID: %s";
    public static final String LOG_ENTITY_UPDATED_SUCCESSFULLY = "Entity updated successfully with ID: %s";

    // Error Messages
    public static final String ERROR_CANNOT_DEACTIVATE_DEFAULT = "Cannot deactivate default schema. Set another schema as default first.";

    // Conversion Error Messages
    public static final String ERROR_JSONNODE_TO_STRING = "Failed to convert JsonNode to String: %s";
    public static final String ERROR_STRING_TO_JSONNODE = "Failed to convert String to JsonNode: %s";
    public static final String ERROR_INVALID_CREATED_BY_UUID = "Invalid createdBy UUID format: %s";
    public static final String ERROR_INVALID_PARENT_SCHEMA_ID = "Invalid parent schema ID format: %s";

    // ===================================================================
    // SCHEMA SYNCHRONIZATION CONSTANTS
    // ===================================================================

    // Schema Sync Logger Names
    public static final String SCHEMA_SYNC_LOGGER_NAME = "SchemaFieldChangeEventPublisher";

    // Schema Sync Log Categories
    public static final String AUDIT_LOG_CATEGORY = "schema-field-sync";

    // Schema Sync Log Messages - Field Addition
    public static final String SCHEMA_SYNC_LOG_PUBLISHING_FIELD_ADDED = "Publishing field added event";
    public static final String SCHEMA_SYNC_LOG_FAILED_PUBLISH_FIELD_ADDED = "Failed to publish field added event";

    // Schema Sync Log Messages - Field Deletion
    public static final String SCHEMA_SYNC_LOG_PUBLISHING_FIELD_DELETED = "Publishing field deleted event";
    public static final String SCHEMA_SYNC_LOG_FAILED_PUBLISH_FIELD_DELETED = "Failed to publish field deleted event";

    // Schema Sync Log Messages - Field Update
    public static final String SCHEMA_SYNC_LOG_PUBLISHING_FIELD_UPDATED = "Publishing field updated event";
    public static final String SCHEMA_SYNC_LOG_FAILED_PUBLISH_FIELD_UPDATED = "Failed to publish field updated event";

    // Schema Sync Log Messages - Kafka Publishing
    public static final String SCHEMA_SYNC_LOG_SENDING_TO_KAFKA = "Sending schema field change event to Kafka";
    public static final String SCHEMA_SYNC_LOG_SUCCESS_SENT_TO_KAFKA = "Successfully sent schema field change event to Kafka";

    // Schema Sync Detail Field Names
    public static final String SCHEMA_SYNC_DETAIL_SCHEMA_ID = "schemaId";
    public static final String SCHEMA_SYNC_DETAIL_FIELD_ID = "fieldId";
    public static final String SCHEMA_SYNC_DETAIL_FIELD_KEY = "fieldSystemKey";
    public static final String SCHEMA_SYNC_DETAIL_IS_CORE_FIELD = "isCoreField";
    public static final String SCHEMA_SYNC_DETAIL_TENANT_ID = "tenantId";
    public static final String SCHEMA_SYNC_DETAIL_ERROR = "error";
    public static final String SCHEMA_SYNC_DETAIL_KAFKA_TOPIC = "kafkaTopic";
    public static final String SCHEMA_SYNC_DETAIL_EVENT_TYPE = "eventType";
    public static final String SCHEMA_SYNC_DETAIL_EVENT_ID = "eventId";
    public static final String SCHEMA_SYNC_DETAIL_KAFKA_KEY = "kafkaKey";
    public static final String SCHEMA_SYNC_DETAIL_KAFKA_ERROR = "kafkaError";

    // Schema Sync Event Source Constants
    public static final String SCHEMA_SYNC_EVENT_SOURCE_SYSTEM = "SYSTEM";
    public static final String SCHEMA_SYNC_EVENT_SOURCE_SERVICE = "IdentitySchemaService";

    // Schema Sync Log Messages - Specific Field Added Event
    public static final String SCHEMA_SYNC_LOG_FAILED_PUBLISH_FIELD_ADDED_EVENT = "Failed to publish field added event, continuing with operation";

    // Schema Sync Log Messages - Field Event Publishing
    public static final String SCHEMA_SYNC_LOG_PUBLISHED_FIELD_DELETED_EVENT = "Published field deleted event for schema synchronization";
    public static final String SCHEMA_SYNC_LOG_FAILED_PUBLISH_FIELD_DELETED_EVENT = "Failed to publish field deleted event, continuing with operation";
    public static final String SCHEMA_SYNC_LOG_PUBLISHED_CORE_FIELD_ADDED_EVENT = "Published core field added event for schema synchronization";
    public static final String SCHEMA_SYNC_LOG_FAILED_PUBLISH_CORE_FIELD_ADDED_EVENT = "Failed to publish core field added event, continuing with operation";

    // Usage Statistics Log Messages
    public static final String USAGE_STATS_LOG_RETRIEVED_STATISTICS = "Retrieved usage statistics";
    public static final String USAGE_STATS_LOG_FAILED_RETRIEVE_STATISTICS = "Failed to retrieve usage statistics, using default values";

    // ===================================================================
    // ENTITY VALIDATION CONSTANTS
    // ===================================================================

    /**
     * Validation constants for SchemaField entity.
     */
    public static final class SchemaField {

        // Field length limits
        public static final int DISPLAY_NAME_MAX_LENGTH = 255;
        public static final int SYSTEM_KEY_MAX_LENGTH = 100;
        public static final int SOURCE_SYSTEM_MAX_LENGTH = 100;
        public static final int RISK_LEVEL_MAX_LENGTH = 20;
        public static final int USAGE_CONTEXT_MAX_LENGTH = 100;
        public static final int SYNC_STATUS_MAX_LENGTH = 50;
        public static final int LABEL_MAX_LENGTH = 255;

        // Validation messages
        public static final String DISPLAY_NAME_REQUIRED_MESSAGE = "Display name is required";
        public static final String DISPLAY_NAME_SIZE_MESSAGE = "Display name cannot exceed {max} characters";
        public static final String SYSTEM_KEY_REQUIRED_MESSAGE = "System key is required";
        public static final String SYSTEM_KEY_SIZE_MESSAGE = "System key cannot exceed {max} characters";
        public static final String SOURCE_SYSTEM_SIZE_MESSAGE = "Source system cannot exceed {max} characters";
        public static final String RISK_LEVEL_SIZE_MESSAGE = "Risk level cannot exceed {max} characters";
        public static final String USAGE_CONTEXT_SIZE_MESSAGE = "Usage context cannot exceed {max} characters";
        public static final String SYNC_STATUS_SIZE_MESSAGE = "Sync status cannot exceed {max} characters";
        public static final String LABEL_EN_SIZE_MESSAGE = "English label cannot exceed {max} characters";
        public static final String LABEL_FR_SIZE_MESSAGE = "French label cannot exceed {max} characters";
        public static final String LABEL_IT_SIZE_MESSAGE = "Italian label cannot exceed {max} characters";
        public static final String LABEL_JA_SIZE_MESSAGE = "Japanese label cannot exceed {max} characters";
        public static final String LABEL_ES_SIZE_MESSAGE = "Spanish label cannot exceed {max} characters";
        public static final String LABEL_DE_SIZE_MESSAGE = "German label cannot exceed {max} characters";
        public static final String LABEL_PT_SIZE_MESSAGE = "Portuguese label cannot exceed {max} characters";
        public static final String LABEL_ZH_SIZE_MESSAGE = "Chinese label cannot exceed {max} characters";

        private SchemaField() {
            // Utility class - prevent instantiation
        }
    }

    // ===================================================================
    // JWT TENANTID EXTRACTION CONSTANTS
    // ===================================================================

    // JWT TenantId Service Constants
    public static final String JWT_TENANTID_SERVICE_LOGGER_NAME = "JwtTenantIdService";
    public static final String LOG_CATEGORY_JWT_TENANTID = "jwt-tenantid";

    // JWT TenantId Log Messages
    public static final String LOG_JWT_TENANTID_EXTRACTION_STARTED = "Starting JWT tenant ID extraction";
    public static final String LOG_JWT_TENANTID_EXTRACTION_SUCCESS = "Successfully extracted tenant ID from JWT";
    public static final String LOG_JWT_TENANTID_EXTRACTION_FAILED = "Failed to extract tenant ID from JWT";
    public static final String LOG_JWT_TENANTID_NOT_FOUND = "Tenant ID not found in JWT token";
    public static final String LOG_JWT_TENANTID_FALLBACK_HEADER = "Using tenant ID from request header as fallback";
    public static final String LOG_JWT_TENANTID_NO_FALLBACK = "No tenant ID found in JWT or headers";

    // JWT TenantId Detail Keys
    public static final String DETAIL_JWT_EXTRACTED_TENANTID = "extractedTenantId";
    public static final String DETAIL_JWT_HEADER_TENANTID = "headerTenantId";
    public static final String DETAIL_JWT_TOKEN_AVAILABLE = "jwtTokenAvailable";

    // JWT TenantId Exception Messages
    public static final String EXCEPTION_JWT_TENANTID_EXTRACTION_FAILED = "Failed to extract tenant ID from JWT token";
    public static final String EXCEPTION_JWT_TENANTID_NOT_AVAILABLE = "Tenant ID not available in JWT token or request headers";

    // ===================================================================
    // AUDIT TENANTID CONSTANTS  
    // ===================================================================

    // Audit TenantId Log Messages
    public static final String LOG_AUDIT_TENANTID_ADDED = "Tenant ID added to audit event";
    public static final String LOG_AUDIT_TENANTID_MISSING = "Tenant ID missing from audit event";
    public static final String LOG_AUDIT_TENANTID_EXTRACTED_FROM_JWT = "Tenant ID extracted from JWT for audit";
    public static final String LOG_AUDIT_TENANTID_USING_HEADER_FALLBACK = "Using header tenant ID for audit as JWT extraction failed";

    // Audit TenantId Detail Keys
    public static final String DETAIL_AUDIT_TENANTID = "auditTenantId";
    public static final String DETAIL_AUDIT_TENANTID_SOURCE = "tenantIdSource";

    // TenantId Source Values
    public static final String TENANTID_SOURCE_JWT = "jwt";
    public static final String TENANTID_SOURCE_HEADER = "header";
    public static final String TENANTID_SOURCE_FALLBACK = "fallback";

}