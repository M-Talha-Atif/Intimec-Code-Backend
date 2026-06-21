package com.aidentx.iam.entitlement_management.config.constants;

/**
 * Constants for the Application Client module.
 *
 * <p>This class contains all constant values used by the ApplicationClientImpl
 * for making HTTP requests to the application service through the API gateway.
 * Centralizing these constants ensures consistency and ease of maintenance.</p>
 *
 * @author aidentx Development Team
 * @since 1.0
 */
public final class ApplicationClientConstants {

    private ApplicationClientConstants() {
        // Private constructor to prevent instantiation
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ============================================================
    // API Endpoints
    // ============================================================

    /**
     * Base path for application service API endpoints.
     * <p>Example: /api/v1/applications</p>
     */
    public static final String APPLICATION_BASE_PATH = "/api/v1/applications";

    /**
     * Bulk fetch endpoint for retrieving multiple applications at once.
     * <p>Example: /bulk</p>
     */
    public static final String BULK_ENDPOINT = "/bulk";

    // ============================================================
    // HTTP Headers
    // ============================================================

    /**
     * HTTP header name for tenant identification.
     * <p>Used for multi-tenancy isolation across the application.</p>
     * <p>Value: X-Tenant-ID</p>
     */
    public static final String TENANT_HEADER = "X-Tenant-ID";

    /**
     * HTTP header name for authentication token.
     * <p>Used to propagate authentication credentials to downstream services.</p>
     * <p>Value: Authorization</p>
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    // ============================================================
    // Request Body Fields
    // ============================================================

    /**
     * JSON field name for the list of application IDs in the request body.
     * <p>Example: {"ids": ["uuid1", "uuid2"]}</p>
     */
    public static final String REQUEST_IDS_FIELD = "ids";

    /**
     * JSON field name for the applications list in the response body.
     * <p>Example: {"applications": [{"id": "...", "name": "..."}]}</p>
     */
    public static final String RESPONSE_APPLICATIONS_FIELD = "applications";

    /**
     * JSON field name for application ID in response objects.
     */
    public static final String APPLICATION_ID_FIELD = "id";

    /**
     * JSON field name for application name in response objects.
     */
    public static final String APPLICATION_NAME_FIELD = "name";

    // ============================================================
    // Logging Messages
    // ============================================================

    /**
     * Log message for empty or null resource IDs.
     */
    public static final String LOG_EMPTY_RESOURCE_IDS = "Empty or null resource IDs provided, returning empty map";

    /**
     * Log message for fetching application names.
     */
    public static final String LOG_FETCHING_APPLICATION_NAMES = "Fetching application names via bulk API";

    /**
     * Log message for added authorization token.
     */
    public static final String LOG_AUTH_TOKEN_ADDED = "Added authorization token to request";

    /**
     * Log message for missing authorization token.
     */
    public static final String LOG_AUTH_TOKEN_MISSING = "No authorization token found in current request";

    /**
     * Log message for successful resolution of application names.
     */
    public static final String LOG_APPLICATION_NAMES_RESOLVED = "Application names resolved successfully";

    /**
     * Log message for failure to fetch application names.
     */
    public static final String LOG_FAILED_TO_FETCH = "Failed to fetch application names from application service";

    /**
     * Log message for successful API call.
     */
    public static final String LOG_API_CALL_SUCCESS = "Successfully called application service bulk API";

    /**
     * Log message for API call failure.
     */
    public static final String LOG_API_CALL_FAILURE = "Application service bulk API call failed";

    // ============================================================
    // Logging Detail Keys
    // ============================================================

    /**
     * Log detail key for resource count.
     */
    public static final String LOG_DETAIL_COUNT = "count";

    /**
     * Log detail key for resolved count.
     */
    public static final String LOG_DETAIL_RESOLVED_COUNT = "resolvedCount";

    /**
     * Log detail key for requested count.
     */
    public static final String LOG_DETAIL_REQUESTED_COUNT = "requestedCount";

    /**
     * Log detail key for tenant ID.
     */
    public static final String LOG_DETAIL_TENANT_ID = "tenantId";

    /**
     * Log detail key for resource count in error logs.
     */
    public static final String LOG_DETAIL_RESOURCE_COUNT = "resourceCount";

    // ============================================================
    // HTTP Methods
    // ============================================================

    /**
     * HTTP POST method.
     */
    public static final String HTTP_METHOD_POST = "POST";

    // ============================================================
    // Fallback Values
    // ============================================================

    /**
     * Fallback application name when the actual name cannot be retrieved.
     */
    public static final String UNKNOWN_APP = "Unknown";

    /**
     * Default base URL for the API gateway when not configured.
     */
    public static final String DEFAULT_GATEWAY_URL = "http://localhost:8081";

    // ============================================================
    // Configuration Properties
    // ============================================================

    /**
     * Configuration property key for the API gateway URL.
     */
    public static final String CONFIG_API_GATEWAY_URL = "api.gateway.url";

    // ============================================================
    // Response Status
    // ============================================================

    /**
     * HTTP status code for successful response.
     */
    public static final int HTTP_STATUS_OK = 200;

    /**
     * HTTP status code for unauthorized access.
     */
    public static final int HTTP_STATUS_UNAUTHORIZED = 401;

    /**
     * HTTP status code for forbidden access.
     */
    public static final int HTTP_STATUS_FORBIDDEN = 403;

    /**
     * HTTP status code for not found.
     */
    public static final int HTTP_STATUS_NOT_FOUND = 404;

    /**
     * HTTP status code for internal server error.
     */
    public static final int HTTP_STATUS_INTERNAL_ERROR = 500;
}