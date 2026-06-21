package com.aidentx.iam.entitlement_management.config.constants.analytics;

/**
 * Constants for Entitlement Classification KPI Analytics
 * Contains all hardcoded values, log messages, field names and headers
 * to ensure no magic strings in service code
 * 
 * This is specifically for Classification KPI analytics - tracks entitlement classification metrics.
 */
public class EntitlementClassificationKpiConstants {

    // ===================================================================
    // Header Constants
    // ===================================================================
    public static final String HEADER_TENANT_ID = "X-Tenant-ID";

    // ===================================================================
    // Logger Constants
    // ===================================================================
    public static final String LOGGER_MODULE_NAME = "CLASSIFICATION_KPI";

    // ===================================================================
    // Numeric Constants
    // ===================================================================
    public static final int ZERO_COUNT = 0;
    public static final double ZERO_DOUBLE = 0.0;
    public static final int DECIMAL_PRECISION = 2;

    // ===================================================================
    // Log Messages - INFO Level
    // ===================================================================
    public static final String LOG_FETCHING_CLASSIFICATION_KPI = "Fetching entitlement classification KPIs for tenantId: {}";
    public static final String LOG_CLASSIFICATION_KPI_CALCULATION_SUCCESS = "Successfully calculated classification KPIs: ownershipGaps={}, unclassified={}, requestabilityRate={}, requestableEntitlements={}";
    public static final String LOG_CLASSIFICATION_KPI_RETURNED_SUCCESS = "Successfully returned classification KPIs";

    // ===================================================================
    // Log Messages - DEBUG Level
    // ===================================================================
    public static final String LOG_DEBUG_NO_ENTITLEMENTS = "No entitlements found for classification calculation, returning defaults.";
    public static final String LOG_DEBUG_CALCULATING_OWNERSHIP_GAPS = "Calculating entitlements without owners for tenantId: {}";
    public static final String LOG_DEBUG_CALCULATING_REQUESTABLE_ENTITLEMENTS = "Calculating requestable entitlements count for tenantId: {}";

    // ===================================================================
    // Log Messages - ERROR Level
    // ===================================================================
    public static final String LOG_ERROR_INVALID_REQUEST = "Invalid classification KPI request parameters: {}";
    public static final String LOG_ERROR_CALCULATING_CLASSIFICATION_KPI = "Error calculating classification KPIs: {}";

    // ===================================================================
    // Validation Error Messages
    // ===================================================================
    public static final String ERROR_TENANT_ID_BLANK = "Tenant ID cannot be null or blank";

    // ===================================================================
    // Logging Detail Keys
    // ===================================================================
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_OWNERSHIP_GAPS = "ownershipGaps";
    public static final String DETAIL_UNCLASSIFIED = "unclassified";
    public static final String DETAIL_REQUESTABILITY_RATE = "requestabilityRate";
    public static final String DETAIL_REQUESTABLE_ENTITLEMENTS = "requestableEntitlements";
    public static final String DETAIL_ERROR = "error";

    // ===================================================================
    // Response Field Names
    // ===================================================================
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_OWNERSHIP_GAPS = "ownershipGaps";
    public static final String FIELD_UNCLASSIFIED = "unclassified";
    public static final String FIELD_REQUESTABILITY_RATE = "requestabilityRate";
    public static final String FIELD_REQUESTABLE_ENTITLEMENTS = "requestableEntitlements";
    public static final String FIELD_TIMESTAMP = "timestamp";

    // ===================================================================
    // API Documentation Messages
    // ===================================================================
    public static final String API_DESCRIPTION = "Retrieves entitlement classification KPI metrics";
    public static final String API_SUMMARY = "Get Entitlement Classification KPI Analytics";

    // ===================================================================
    // KPI Descriptions
    // ===================================================================
    public static final String KPI_OWNERSHIP_GAPS_DESC = "Count of entitlements without owners (null ownerId)";
    public static final String KPI_UNCLASSIFIED_DESC = "Entitlements without classification labels (currently mock - returns 0)";
    public static final String KPI_REQUESTABILITY_RATE_DESC = "Percentage of requestable entitlements (currently mock - returns 0%)";
    public static final String KPI_REQUESTABLE_ENTITLEMENTS_DESC = "Count of entitlements marked as requestable";

    private EntitlementClassificationKpiConstants() {
        // Utility class - private constructor
    }
}

