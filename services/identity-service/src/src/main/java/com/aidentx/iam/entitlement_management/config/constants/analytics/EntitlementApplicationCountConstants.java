package com.aidentx.iam.entitlement_management.config.constants.analytics;

/**
 * Constants for Entitlement Application Count Analytics
 * Contains all hardcoded values, log messages, field names and headers
 * to ensure no magic strings in service code
 */
public class EntitlementApplicationCountConstants {

    // ===================================================================
    // Header Constants
    // ===================================================================
    public static final String HEADER_TENANT_ID = "X-Tenant-ID";

    // ===================================================================
    // Logger Constants
    // ===================================================================
    public static final String LOGGER_MODULE_NAME = "APPLICATION_COUNT";

    // ===================================================================
    // Numeric Constants
    // ===================================================================
    public static final int ZERO_COUNT = 0;

    // ===================================================================
    // Log Messages - INFO Level
    // ===================================================================
    public static final String LOG_FETCHING_APPLICATION_COUNTS = "Fetching entitlement counts per application";
    public static final String LOG_APPLICATION_COUNT_SUCCESS = "Successfully retrieved entitlement counts for applications";
    public static final String LOG_APPLICATION_COUNT_RETURNED = "Successfully returned application entitlement counts";

    // ===================================================================
    // Log Messages - DEBUG Level
    // ===================================================================
    public static final String LOG_DEBUG_NO_APPLICATIONS = "No applications found for tenantId: {}";
    public static final String LOG_DEBUG_PROCESSING_APPLICATION = "Processing application count for applicationId: {}";

    // ===================================================================
    // Log Messages - ERROR Level
    // ===================================================================
    public static final String LOG_ERROR_INVALID_REQUEST = "Invalid application count request parameters: {}";
    public static final String LOG_ERROR_FETCHING_APPLICATION_COUNTS = "Error fetching entitlement application counts: {}";
    public static final String LOG_ERROR_APPLICATION_CLIENT_FAILED = "Failed to fetch application details from client";

    // ===================================================================
    // Validation Error Messages
    // ===================================================================
    public static final String ERROR_REQUEST_NULL = "EntitlementApplicationCountRequest cannot be null";
    public static final String ERROR_TENANT_ID_BLANK = "Tenant ID in EntitlementApplicationCountRequest cannot be null or blank";

    // ===================================================================
    // Logging Detail Keys
    // ===================================================================
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_APPLICATION_ID = "applicationId";
    public static final String DETAIL_APPLICATION_NAME = "applicationName";
    public static final String DETAIL_ENTITLEMENT_COUNT = "entitlementCount";
    public static final String DETAIL_TOTAL_APPLICATIONS = "totalApplications";
    public static final String DETAIL_ERROR = "error";

    // ===================================================================
    // Response Field Names
    // ===================================================================
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_APPLICATIONS = "applications";
    public static final String FIELD_APPLICATION_ID = "applicationId";
    public static final String FIELD_APPLICATION_NAME = "applicationName";
    public static final String FIELD_ENTITLEMENT_COUNT = "entitlementCount";
    public static final String FIELD_TIMESTAMP = "timestamp";

    // ===================================================================
    // API Documentation Messages
    // ===================================================================
    public static final String API_DESCRIPTION = "Retrieves entitlement count for each application";
    public static final String API_SUMMARY = "Get Entitlement Count Per Application";

    private EntitlementApplicationCountConstants() {
        // Utility class - private constructor
    }
}

