package com.aidentx.core.log_management.config;

/**
 * Application constants for logPinning service.
 * Contains all constant values to avoid hardcoding throughout the application.
 */
public class AppConstants {

    // API Endpoints

    // Logger Categories
    public static final String LOG_CATEGORY_PINNED_LOGS = "Pinned-logs";

    // Success & Error Messages for API Responses/Exceptions
    public static final String MSG_INVALID_REQUEST_DATA = "Invalid request data";
    public static final String MSG_FAILED_TO_CREATE_PINNED_LOG = "Failed to create pinned log";
    public static final String MSG_FAILED_TO_DELETE_PINNED_LOG = "Failed to delete pinned log";
    public static final String MSG_FAILED_TO_RETRIEVE_PINNED_LOGS = "Failed to retrieve pinned logs";
    public static final String MSG_FAILED_TO_UPDATE_PINNED_LOG = "Failed to update pinned log";
    public static final String MSG_PINNED_LOG_NOT_FOUND_BY_ID_FORMAT = "Pinned log not found with ID: %s";
    public static final String MSG_DUPLICATE_PINNED_LOG_ID_FORMAT = "Log with document ID %s is already pinned";
    public static final String MSG_LOG_NOT_FOUND_IN_OPENSEARCH_FORMAT = "Log with document ID %s not found in OpenSearch";
    public static final String MSG_NO_UPDATES_PROVIDED = "No updates provided in request";

    // Log Messages
    public static final String LOG_PIN_ATTEMPT = "Entering pinLog endpoint";
    public static final String LOG_DELETE_ATTEMPT = "Entering deletePinnedLog endpoint";
    public static final String LOG_GET_ALL_ATTEMPT = "Entering getPinnedLogs endpoint";
    public static final String LOG_UPDATE_ATTEMPT = "Entering updatePinnedLog endpoint";
    public static final String LOG_ATTEMPTED_SAVE_NULL_LOGGER = "Attempted to save null pinned logger";
    public static final String LOG_SUCCESSFULLY_CREATED_PINNED_LOG = "Successfully created pinned log";
    public static final String LOG_SUCCESSFULLY_UPDATED_PINNED_LOG = "Successfully updated pinned log";
    public static final String LOG_INVALID_DATA_PINNED_LOG_CREATION = "Invalid data provided for pinned log creation";
    public static final String LOG_INVALID_DATA_PINNED_LOG_UPDATE = "Invalid data provided for pinned log update";
    public static final String LOG_FAILED_UPDATE_PINNED_LOG = "Failed to update pinned log";
    public static final String LOG_ATTEMPTED_DELETE_NULL_ID = "Attempted to delete pinned log with null ID";
    public static final String LOG_SUCCESSFULLY_DELETED_PINNED_LOG = "Successfully deleted pinned log";
    public static final String LOG_ATTEMPTED_DELETE_NON_EXISTENT = "Attempted to delete non-existent pinned log";
    public static final String LOG_INVALID_ID_PINNED_LOG_DELETION = "Invalid ID provided for pinned log deletion";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_PINNED_LOGS = "Successfully retrieved pinned logs";
    public static final String LOG_FAILED_CREATE_PINNED_LOG = "Failed to create pinned log";
    public static final String LOG_FAILED_DELETE_PINNED_LOG = "Failed to delete pinned log";
    public static final String LOG_FAILED_RETRIEVE_PINNED_LOGS = "Failed to retrieve pinned logs";
    public static final String LOG_DUPLICATE_DATA_REJECTED = "Duplicate pinned log data rejected";
    public static final String LOG_SKIPPED_RESOLVED_LOG = "Skipped resolved log during retrieval";


    // Validation Messages
    public static final String VALIDATION_LOG_ID_REQUIRED = "Log ID is required";
    public static final String VALIDATION_NAME_REQUIRED = "Name is required and cannot be empty";

    // Failure reasons
    public static final String FAILURE_REASON_LOG_ID_NULL = "Log ID is null";
    public static final String FAILURE_REASON_NAME_NULL_OR_EMPTY = "Name field is null or empty";
    public static final String FAILURE_REASON_DATA_NULL = "Data field is null";

    // Controller Response Field Names
    public static final String RESPONSE_FIELD_EXISTS = "exists";
    public static final String RESPONSE_FIELD_TIMESTAMP = "timestamp";
    public static final String RESPONSE_FIELD_MESSAGE = "message";
    public static final String RESPONSE_FIELD_CREATED = "created";
    public static final String RESPONSE_FIELD_RESOLVED = "resolved";
    public static final String RESPONSE_FIELD_RESOLVER_ID = "resolverId";
    public static final String RESPONSE_FIELD_RESOLVED_AT = "resolvedAt";
    public static final String RESPONSE_FIELD_UNRESOLVED_BY = "unresolvedBy";
    public static final String RESPONSE_FIELD_UNRESOLVED_AT = "unresolvedAt";
    public static final String RESPONSE_FIELD_ERROR = "error";

    // OpenSearch Document Field Names
    public static final String OPENSEARCH_FIELD_IS_RESOLVED = "isResolved";
    public static final String OPENSEARCH_FIELD_RESOLVER_ID = "resolverId";
    public static final String OPENSEARCH_FIELD_RESOLVED_AT = "resolvedAt";

    // OpenSearch document structure field names
    public static final String OPENSEARCH_FIELD_HITS = "hits";
    public static final String OPENSEARCH_FIELD_SOURCE = "_source";
    public static final String OPENSEARCH_FIELD_ID = "id";
    public static final String OPENSEARCH_FIELD_PINNED_NAME = "pinned.name";
    public static final String OPENSEARCH_FIELD_PINNED_DESCRIPTION = "pinned.description";
    public static final String OPENSEARCH_FIELD_PINNED_AT = "pinnedAt";
    public static final String OPENSEARCH_FIELD_PINNED_UPDATED_AT = "pinned.updatedAt";
    public static final String OPENSEARCH_FIELD_PINNED_UPDATED_BY = "pinned.updatedBy";

    // Response Messages
    public static final String RESPONSE_ERROR_PREFIX = "Error: ";
    public static final String EXCEPTION_NOT_FOUND_TEXT = "not found";

    // Default Values
    public static final String DEBUG_SEARCH_DEFAULT_SIZE = "50";

    // ================== RESOLVER CONSTANTS ==================

    // Resolver Log Messages
    public static final String RESOLVER_ATTEMPTING_TO_RESOLVE_LOG = "Attempting to resolve log";
    public static final String RESOLVER_NO_VALID_USER_ID_FOR_RESOLVE = "No valid user ID found for resolve operation";
    public static final String RESOLVER_ATTEMPTED_TO_RESOLVE_NON_EXISTENT_LOG = "Attempted to resolve non-existent log";
    public static final String RESOLVER_ATTEMPTED_TO_RESOLVE_ALREADY_RESOLVED_LOG = "Attempted to resolve already resolved log";
    public static final String RESOLVER_LOG_RESOLVED_SUCCESSFULLY = "Log resolved successfully";
    public static final String RESOLVER_FAILED_TO_RESOLVE_LOG_IN_OPENSEARCH = "Failed to resolve log in OpenSearch";
    public static final String RESOLVER_ATTEMPTING_TO_UNRESOLVE_LOG = "Attempting to unresolve log";
    public static final String RESOLVER_NO_VALID_USER_ID_FOR_UNRESOLVE = "No valid user ID found for unresolve operation";
    public static final String RESOLVER_ATTEMPTED_TO_UNRESOLVE_NON_EXISTENT_LOG = "Attempted to unresolve non-existent log";
    public static final String RESOLVER_LOG_UNRESOLVED_SUCCESSFULLY = "Log unresolved successfully";
    public static final String RESOLVER_FAILED_TO_UNRESOLVE_LOG_IN_OPENSEARCH = "Failed to unresolve log in OpenSearch";
    public static final String RESOLVER_ERROR_RESOLVING_LOG = "Error occurred while resolving log";
    public static final String RESOLVER_ERROR_UNRESOLVING_LOG = "Error occurred while unresolving log";
    public static final String RESOLVER_FAILED_TO_EXTRACT_USER_ID = "Failed to extract user ID from current request";

    // Resolver Error Messages
    public static final String RESOLVER_AUTHENTICATION_REQUIRED = "Authentication required";
    public static final String RESOLVER_LOG_NOT_FOUND = "Log not found";
    public static final String RESOLVER_LOG_ALREADY_RESOLVED = "Log is already resolved";
    public static final String RESOLVER_FAILED_TO_RESOLVE_LOG = "Failed to resolve log";
    public static final String RESOLVER_FAILED_TO_UNRESOLVE_LOG = "Failed to unresolve log";
    public static final String RESOLVER_INTERNAL_SERVER_ERROR_PREFIX = "Internal server error: ";

    // ================== DEBUG CONSTANTS ==================

    // Debug Log Messages
    public static final String DEBUG_ERROR_SEARCHING_ALL_LOGS = "Error occurred while searching all logs";

    // Debug Error Messages
    public static final String DEBUG_FAILED_TO_SEARCH_LOGS = "Failed to search logs";
    public static final String DEBUG_INTERNAL_SERVER_ERROR_PREFIX = "Internal server error: ";

    // ================== ADDITIONAL LOG MESSAGES ==================

    // Additional log messages that were hardcoded
    public static final String LOG_FAILED_TO_PIN_LOG_OPENSEARCH = "Failed to pin log in OpenSearch - log may not exist";
    public static final String LOG_PINNING_LOG_NOT_FOUND = "Log not found for pinning";
    public static final String LOG_FAILED_TO_UNPIN_LOG_OPENSEARCH = "Failed to unpin log in OpenSearch";
    public static final String LOG_INVALID_UUID_FORMAT = "Invalid UUID format for log ID";
    public static final String LOG_FAILED_TO_CONVERT_DOCUMENT = "Failed to convert OpenSearch document to PinnedLog";
    public static final String LOG_FAILED_TO_EXTRACT_USER_ID_SECURITY = "Failed to get current user from security context";
    public static final String LOG_FAILED_TO_EXTRACT_USER_ID_JWT = "Failed to extract user ID from JWT token";
    public static final String LOG_CHECKING_DOCUMENT_EXISTS = "Checking if document exists in OpenSearch";
    public static final String LOG_GETTING_DEBUG_CONFIG_FOR_ALL_MICROSERVICES = "Getting debug configuration for all microservices";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_DEBUG_CONFIG = "Successfully retrieved debug configuration. Total components: %s";
    public static final String LOG_ERROR_GETTING_DEBUG_CONFIG = "Error getting debug configuration";
    public static final String LOG_UPDATING_COMPONENT_LOG_LEVELS_WITH_REQUEST = "Updating component log levels with request: %s";
    public static final String LOG_COMPONENT_LOG_LEVELS_UPDATED_SUCCESSFULLY = "Component log levels updated successfully";
    public static final String LOG_FAILED_TO_UPDATE_COMPONENT_LOG_LEVELS = "Failed to update component log levels";
    public static final String LOG_NO_VALID_PARAMETERS_FOR_LOG_LEVEL_UPDATE = "No valid parameters provided for log level update";
    public static final String LOG_MICROSERVICE_LOG_MGMT_INITIALIZED = "MicroserviceLogManagementService initialized";
    public static final String LOG_AUTH_SERVICE_URL = "Auth service URL: %s";
    public static final String LOG_CORE_SERVICE_URL = "Core service URL: %s";
    public static final String LOG_CONFIGURED_COMPONENTS = "Configured components: %s";
    public static final String LOG_COMPONENT_TYPE_LOGGER = "Component: %s - type: %s, logger: %s";
    public static final String LOG_GETTING_DEBUG_CONFIG_FOR_ALL_COMPONENTS = "Getting debug configuration for all components";
    public static final String LOG_PROCESSING_COMPONENT_TYPE = "Processing component: %s (type: %s)";
    public static final String LOG_COMPONENT_LEVEL_ACTIVE = "Component %s - level: %s, active: %s";
    public static final String LOG_DEBUG_CONFIG_COMPLETE = "Debug configuration complete - %d/%d components active";
    public static final String LOG_SERVICE_URL_NOT_CONFIGURED = "No service URL configured for component: %s";
    public static final String LOG_CALLING_SERVICE_FOR_COMPONENT = "Calling service at URL: %s for component: %s";
    public static final String LOG_SERVICE_RESPONSE_STATUS_BODY = "Service response - status: %s, hasBody: %s";
    public static final String LOG_SERVICE_RETURNED_LOG_LEVELS = "Service returned %d log levels";
    public static final String LOG_SERVICE_COMPONENT_LEVEL_FROM_LOGGER = "Service component %s level: %s (from logger: %s)";
    public static final String LOG_SERVICE_CALL_FAILED_OR_NO_BODY = "Service call failed or returned no body for component: %s";
    public static final String LOG_COMPONENT_FINAL_RESULT = "Component %s final result - level: %s, active: %s";
    public static final String LOG_ERROR_GETTING_DEBUG_INFO_FOR_COMPONENT = "Error getting debug info for component %s: %s";
    public static final String LOG_UPDATING_LOG_LEVEL_FOR_COMPONENT = "Updating log level for component: %s to level: %s";
    public static final String LOG_COMPONENT_CONFIG_NOT_FOUND = "Component configuration not found for: %s";
    public static final String LOG_UPDATING_LOCAL_CORE_COMPONENT_TO_LEVEL = "Updating local core component: %s logger: %s to level: %s";
    public static final String LOG_SUCCESSFULLY_UPDATED_LOCAL_COMPONENT = "Successfully updated local component: %s";
    public static final String LOG_CALLING_SERVICE_TO_UPDATE_COMPONENT_LOGGER = "Calling service at URL: %s to update component: %s logger: %s to level: %s";
    public static final String LOG_SERVICE_UPDATE_RESPONSE_STATUS_BODY = " service update response - status: %s, body: %s";
    public static final String LOG_SUCCESSFULLY_UPDATED_COMPONENT = "Successfully updated component: %s";
    public static final String LOG_FAILED_TO_UPDATE_COMPONENT_STATUS = "Failed to update component: %s - status: %s";
    public static final String LOG_ERROR_UPDATING_COMPONENT = "Error updating component %s: %s";

    // === Log Filtering & Pinning ===
    public static final String LOG_ADVANCED_LOG_SEARCH_REQUEST_RECEIVED = "Advanced log search request received";
    public static final String LOG_ADVANCED_LOG_SEARCH_COMPLETED = "Advanced log search completed successfully";
    public static final String LOG_INVALID_SEARCH_REQUEST_PARAMETERS = "Invalid search request parameters";
    public static final String LOG_FAILED_TO_EXECUTE_LOG_SEARCH = "Failed to execute log search";
    public static final String LOG_FILTER_AGGREGATIONS_REQUEST_RECEIVED = "Filter aggregations request received";
    public static final String LOG_FILTER_AGGREGATIONS_COMPLETED = "Filter aggregations completed successfully";
    public static final String LOG_FAILED_TO_GET_FILTER_AGGREGATIONS = "Failed to get filter aggregations";
    public static final String LOG_LOG_STATISTICS_REQUEST_RECEIVED = "Log statistics request received";
    public static final String LOG_LOG_STATISTICS_COMPLETED = "Log statistics completed successfully";
    public static final String LOG_INVALID_STATISTICS_REQUEST_PARAMETERS = "Invalid statistics request parameters";
    public static final String LOG_FAILED_TO_GET_LOG_STATISTICS = "Failed to get log statistics";
    public static final String LOG_SEARCH_SUGGESTIONS_REQUEST_RECEIVED = "Search suggestions request received";
    public static final String LOG_SEARCH_SUGGESTIONS_COMPLETED = "Search suggestions completed successfully";
    public static final String LOG_FAILED_TO_GET_SEARCH_SUGGESTIONS = "Failed to get search suggestions";

    // LogPinningHelper
    public static final String LOG_CHECKING_IF_LOG_ALREADY_PINNED = "Checking if log is already pinned";
    public static final String LOG_FAILED_TO_CHECK_IF_LOG_IS_PINNED = "Failed to check if log is pinned";
    public static final String LOG_ATTEMPTING_TO_PIN_LOG_USING_DOCUMENT_ID = "Attempting to pin log using document ID";
    public static final String LOG_SUCCESSFULLY_PINNED_LOG_USING_DOCUMENT_ID = "Successfully pinned log using document ID";
    public static final String LOG_FAILED_TO_PIN_LOG_USING_DOCUMENT_ID = "Failed to pin log using document ID";
    public static final String LOG_EXCEPTION_WHILE_PINNING_LOG_USING_DOCUMENT_ID = "Exception while pinning log using document ID";
    public static final String LOG_ATTEMPTING_TO_UNPIN_LOG_USING_DOCUMENT_ID = "Attempting to unpin log using document ID";
    public static final String LOG_SUCCESSFULLY_UNPINNED_LOG_USING_DOCUMENT_ID = "Successfully unpinned log using document ID";
    public static final String LOG_EXCEPTION_WHILE_UNPINNING_LOG_USING_DOCUMENT_ID = "Exception while unpinning log using document ID";
    public static final String LOG_CHECKING_IF_DOCUMENT_IS_PINNED_USING_DOCUMENT_ID = "Checking if document is pinned using document ID";
    public static final String LOG_DOCUMENT_PINNED_STATUS_CHECKED = "Document pinned status checked";
    public static final String LOG_DOCUMENT_NOT_FOUND = "Document not found";
    public static final String LOG_EXCEPTION_WHILE_CHECKING_IF_DOCUMENT_IS_PINNED = "Exception while checking if document is pinned";
    public static final String LOG_SEARCHING_FOR_PINNED_LOGS_WITH_DOCUMENT_IDS = "Searching for pinned logs with document IDs";
    public static final String LOG_SUCCESSFULLY_RETRIEVED_PINNED_LOGS_WITH_DOCUMENT_IDS = "Successfully retrieved pinned logs with document IDs";
    public static final String LOG_FAILED_TO_SEARCH_PINNED_LOGS_WITH_DOCUMENT_IDS = "Failed to search pinned logs with document IDs";
    public static final String LOG_EXCEPTION_WHILE_SEARCHING_PINNED_LOGS_WITH_DOCUMENT_IDS = "Exception while searching pinned logs with document IDs";

    // LogFilteringServiceImpl
    public static final String LOG_STARTING_LOG_SEARCH = "Starting log search";
    public static final String LOG_OPENSEARCH_RETURNED_NULL_RESULTS = "OpenSearch returned null results";
    public static final String LOG_LOG_SEARCH_COMPLETED_SUCCESSFULLY = "Log search completed successfully";
    public static final String LOG_FAILED_TO_EXECUTE_LOG_SEARCH_IMPL = "Failed to execute log search";
    public static final String LOG_GETTING_FILTER_AGGREGATIONS = "Getting filter aggregations";
    public static final String LOG_OPENSEARCH_RETURNED_NULL_AGGREGATION_RESULTS = "OpenSearch returned null aggregation results";
    public static final String LOG_FAILED_TO_GET_FILTER_AGGREGATIONS_IMPL = "Failed to get filter aggregations";
    public static final String LOG_GETTING_LOG_STATISTICS = "Getting log statistics";
    public static final String LOG_FAILED_TO_GET_LOG_STATISTICS_IMPL = "Failed to get log statistics";
    public static final String LOG_GETTING_SEARCH_SUGGESTIONS = "Getting search suggestions";
    public static final String LOG_FAILED_TO_GET_SEARCH_SUGGESTIONS_IMPL = "Failed to get search suggestions";

    // OpenSearchService
    public static final String LOG_GETTING_DOCUMENT_BY_ID = "Getting document by ID";
    public static final String LOG_DOCUMENT_RETRIEVED_SUCCESSFULLY = "Document retrieved successfully";
    public static final String LOG_DOCUMENT_NOT_FOUND_OPENSEARCH = "Document not found";
    public static final String LOG_FAILED_TO_GET_DOCUMENT = "Failed to get document";
    public static final String LOG_EXCEPTION_WHILE_GETTING_DOCUMENT = "Exception while getting document";
    public static final String LOG_UPDATING_DOCUMENT_BY_ID = "Updating document by ID";
    public static final String LOG_DOCUMENT_UPDATED_SUCCESSFULLY = "Document updated successfully";
    public static final String LOG_FAILED_TO_UPDATE_DOCUMENT = "Failed to update document";
    public static final String LOG_EXCEPTION_WHILE_UPDATING_DOCUMENT = "Exception while updating document";
    public static final String LOG_ATTEMPTING_TO_UPDATE_LOG_DOCUMENT = "Attempting to update log document";
    public static final String LOG_FAILED_TO_UPDATE_LOG_DOCUMENT = "Failed to update log document";
    public static final String LOG_CHECKING_IF_DOCUMENT_EXISTS_BY_ID = "Checking if document exists by ID";
    public static final String LOG_DOCUMENT_EXISTENCE_CHECK_COMPLETED = "Document existence check completed";
    public static final String LOG_EXCEPTION_WHILE_CHECKING_DOCUMENT_EXISTENCE_BY_ID = "Exception while checking document existence by ID";
    public static final String LOG_SEARCHING_DOCUMENTS = "Searching documents";
    public static final String LOG_FAILED_TO_SEARCH_DOCUMENTS = "Failed to search documents";
    public static final String LOG_EXCEPTION_WHILE_SEARCHING_DOCUMENTS = "Exception while searching documents";
    public static final String SECURITY_ANONYMOUS_USER = "anonymousUser";
    public static final String SECURITY_BEARER_PREFIX = "Bearer ";
    // Numeric values used in the application
    public static final int BEARER_PREFIX_LENGTH = 7; // Length of "Bearer " prefix

    // ================== HTTP STATUS CONSTANTS ==================

    // HTTP status codes used in responses
    public static final String HTTP_STATUS_FIELD = "status";
    public static final String HTTP_ERROR_FIELD = "error";

    // ================== RESPONSE FIELD CONSTANTS ==================

    // Additional Response Field Names for resolver operations
    public static final String RESPONSE_FIELD_IS_RESOLVED = "isResolved";

    // ================== OBJECT TYPE CONSTANTS ==================

    // Object type names for logging
    public static final String OBJECT_TYPE_PINNED_LOG = "PinnedLog";
    public static final String OBJECT_ID_NULL = "null";

    public static final String OPENSEARCH_FIELD_IS_PINNED = "isPinned";

    public static final String OPENSEARCH_FIELD_PINNER_ID = "pinnerId";

    public static final String OPENSEARCH_FIELD_UPDATED_AT = "updatedAt";

    public static final String OPENSEARCH_FIELD_UPDATED_BY = "updatedBy";

    private AppConstants() {
        // Private constructor to prevent instantiation
    }

    public static final String LOG_CHECKING_IF_DOCUMENT_ALREADY_PINNED = "Checking if document is already pinned";
    public static final String LOG_PINNING_LOG_USING_DOCUMENT_ID = "Pinning log using document ID";
    public static final String LOG_ATTEMPTED_DELETE_NULL_OR_EMPTY_DOCUMENT_ID = "Attempted delete with null or empty document ID";
    public static final String LOG_ATTEMPTED_DELETE_NON_EXISTENT_OR_NON_PINNED_DOCUMENT = "Attempted delete of non-existent or non-pinned document";
    public static final String LOG_SUCCESSFULLY_DELETED_PINNED_LOG_USING_DOCUMENT_ID = "Successfully deleted pinned log using document ID";
    public static final String LOG_FAILED_TO_UNPIN_LOG_USING_DOCUMENT_ID = "Failed to unpin log using document ID";
    public static final String LOG_INVALID_DOCUMENT_ID_FOR_PINNED_LOG_DELETION = "Invalid document ID for pinned log deletion";
    public static final String LOG_FAILED_TO_DELETE_PINNED_LOG_USING_DOCUMENT_ID = "Failed to delete pinned log using document ID";
    public static final String LOG_UPDATE_ATTEMPT_USING_DOCUMENT_ID = "Update attempt using document ID";
    public static final String LOG_ATTEMPTED_UPDATE_NULL_OR_EMPTY_DOCUMENT_ID = "Attempted update with null or empty document ID";
    public static final String LOG_ATTEMPTED_UPDATE_NULL_UPDATE_REQUEST = "Attempted update with null update request";
    public static final String LOG_ATTEMPTED_UPDATE_NON_EXISTENT_OR_NON_PINNED_DOCUMENT = "Attempted update of non-existent or non-pinned document";
    public static final String LOG_SUCCESSFULLY_UPDATED_PINNED_LOG_USING_DOCUMENT_ID = "Successfully updated pinned log using document ID";
    public static final String LOG_FAILED_TO_UPDATE_PINNED_LOG_USING_DOCUMENT_ID = "Failed to update pinned log using document ID";
    public static final String LOG_INVALID_REQUEST_FOR_PINNED_LOG_UPDATE = "Invalid request for pinned log update";
    public static final String LOG_FAILED_TO_ADD_RESOLVER_INFORMATION_TO_PINNED_LOG = "Failed to add resolver information to pinned log";
    public static final String LOG_DEBUG_INFO_FOR_COMPONENT = "Getting debug info for component: %s (type: %s, logger: %s)";
    public static final String LOG_GETTING_LOCAL_LOG_LEVELS_FOR_CORE_COMPONENT = "Getting local log levels for core component: %s";
    public static final String LOG_LOCAL_COMPONENT_LEVEL = "Local component %s level: %s";
}