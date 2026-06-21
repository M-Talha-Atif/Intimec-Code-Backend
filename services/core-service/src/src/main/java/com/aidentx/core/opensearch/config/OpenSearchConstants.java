package com.aidentx.core.opensearch.config;

public class OpenSearchConstants {
    
    // Logger Messages
    public static final String ATTEMPTING_TO_PIN_LOG = "Attempting to pin log document";
    public static final String FAILED_TO_PIN_LOG = "Failed to pin log document";
    public static final String ATTEMPTING_TO_UNPIN_LOG = "Attempting to unpin log document";
    public static final String FAILED_TO_UNPIN_LOG = "Failed to unpin log document";
    public static final String SEARCHING_PINNED_LOGS = "Searching for pinned logs in OpenSearch";
    public static final String SUCCESSFULLY_RETRIEVED_PINNED_LOGS = "Successfully retrieved pinned logs from OpenSearch";
    public static final String FAILED_TO_SEARCH_PINNED_LOGS = "Failed to search pinned logs";
    public static final String FAILED_TO_SEARCH_PINNED_LOGS_EXCEPTION = "Failed to search pinned logs in OpenSearch";
    public static final String CHECKING_IF_LOG_ALREADY_PINNED = "Checking if log is already pinned";
    public static final String FAILED_TO_CHECK_IF_LOG_PINNED = "Failed to check if log is pinned";
    public static final String DOCUMENT_EXISTS_CHECK_VIA_ID = "Document exists check via _id";
    public static final String DOCUMENT_EXISTS_CHECK_VIA_SEARCH = "Document exists check via search";
    public static final String FAILED_TO_CHECK_DOCUMENT_EXISTENCE = "Failed to check document existence";
    public static final String SENDING_UPDATE_BY_QUERY = "Sending update_by_query to OpenSearch";
    public static final String OPENSEARCH_UPDATE_BY_QUERY_RESPONSE = "OpenSearch update_by_query response";
    public static final String SUCCESSFULLY_UPDATED_LOG_DOCUMENT = "Successfully updated log document";
    public static final String NO_DOCUMENTS_UPDATED = "No documents updated - log may not exist";
    public static final String FAILED_TO_UPDATE_LOG_DOCUMENT = "Failed to update log document";
    public static final String EXCEPTION_DURING_DOCUMENT_UPDATE = "Exception during document update";
    
    // Resolver Operations
    public static final String ATTEMPTING_TO_RESOLVE_LOG = "Attempting to resolve log";
    public static final String ATTEMPTING_TO_UNRESOLVE_LOG = "Attempting to unresolve log";
    public static final String NO_VALID_USER_ID_FOR_RESOLVE = "No valid user ID found for resolve operation";
    public static final String NO_VALID_USER_ID_FOR_UNRESOLVE = "No valid user ID found for unresolve operation";
    public static final String ATTEMPTED_TO_RESOLVE_NON_EXISTENT_LOG = "Attempted to resolve non-existent log";
    public static final String ATTEMPTED_TO_UNRESOLVE_NON_EXISTENT_LOG = "Attempted to unresolve non-existent log";
    public static final String LOG_RESOLVED_SUCCESSFULLY = "Log resolved successfully";
    public static final String LOG_UNRESOLVED_SUCCESSFULLY = "Log unresolved successfully";
    public static final String FAILED_TO_RESOLVE_LOG_IN_OPENSEARCH = "Failed to resolve log in OpenSearch";
    public static final String FAILED_TO_UNRESOLVE_LOG_IN_OPENSEARCH = "Failed to unresolve log in OpenSearch";
    public static final String ERROR_RESOLVING_LOG = "Error resolving log";
    public static final String ERROR_UNRESOLVING_LOG = "Error unresolving log";
    public static final String FAILED_TO_EXTRACT_USER_ID_FROM_REQUEST = "Failed to extract user ID from current request";
    
    // Debug Operations
    public static final String ERROR_CHECKING_DOCUMENT_EXISTENCE = "Error checking document existence";
    public static final String ERROR_SEARCHING_ALL_LOGS = "Error searching all logs";
    public static final String ERROR_CREATING_TEST_LOG = "Error creating test log";
    
    // Error Messages
    public static final String AUTHENTICATION_REQUIRED = "Authentication required";
    public static final String FAILED_TO_RESOLVE_LOG = "Failed to resolve log";
    public static final String FAILED_TO_UNRESOLVE_LOG = "Failed to unresolve log";
    public static final String FAILED_TO_RETRIEVE_LOGS = "Failed to retrieve logs";
    public static final String FAILED_TO_CREATE_TEST_LOG = "Failed to create test log";
    public static final String INTERNAL_SERVER_ERROR_PREFIX = "Internal server error: ";
    
}
