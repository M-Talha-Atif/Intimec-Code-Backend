package com.aidentx.iam.entitlement_management.config.constants.analytics;

/**
 * Constants for Entitlement Sprawl KPI Analytics
 * Contains all hardcoded values, log messages, field names and headers
 * to ensure no magic strings in service code
 * 
 * This is specifically for Sprawl KPI analytics - tracks entitlement sprawl metrics.
 */
public class EntitlementSprawlKpiConstants {

    // ===================================================================
    // Header Constants
    // ===================================================================
    public static final String HEADER_TENANT_ID = "X-Tenant-ID";

    // ===================================================================
    // Logger Constants
    // ===================================================================
    public static final String LOGGER_MODULE_NAME = "SPRAWL_KPI";

    // ===================================================================
    // Numeric Constants
    // ===================================================================
    public static final int ZERO_COUNT = 0;
    public static final double ZERO_DOUBLE = 0.0;
    public static final int DECIMAL_PRECISION = 2;
    public static final int OVER_PROVISIONED_THRESHOLD = 100;

    // ===================================================================
    // Log Messages - INFO Level
    // ===================================================================
    public static final String LOG_FETCHING_SPRAWL_KPI = "Fetching entitlement sprawl KPIs for tenantId: {}";
    public static final String LOG_SPRAWL_KPI_CALCULATION_SUCCESS = "Successfully calculated sprawl KPIs: sprawlIndex={}, staleEntitlements={}, overProvisionedCount={}, directAssignmentCount={}";
    public static final String LOG_SPRAWL_KPI_RETURNED_SUCCESS = "Successfully returned sprawl KPIs";

    // ===================================================================
    // Log Messages - DEBUG Level
    // ===================================================================
    public static final String LOG_DEBUG_NO_ENTITLEMENTS = "No entitlements found for sprawl calculation, returning defaults.";
    public static final String LOG_DEBUG_CALCULATING_OVER_PROVISIONED = "Calculating over-provisioned entitlements (threshold: {}) for tenantId: {}";
    public static final String LOG_DEBUG_CALCULATING_DIRECT_ASSIGNMENTS = "Calculating direct assignment entitlements for tenantId: {}";

    // ===================================================================
    // Log Messages - ERROR Level
    // ===================================================================
    public static final String LOG_ERROR_INVALID_REQUEST = "Invalid sprawl KPI request parameters: {}";
    public static final String LOG_ERROR_CALCULATING_SPRAWL_KPI = "Error calculating sprawl KPIs: {}";

    // ===================================================================
    // Validation Error Messages
    // ===================================================================
    public static final String ERROR_TENANT_ID_BLANK = "Tenant ID cannot be null or blank";

    // ===================================================================
    // Logging Detail Keys
    // ===================================================================
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_SPRAWL_INDEX = "sprawlIndex";
    public static final String DETAIL_STALE_ENTITLEMENTS = "staleEntitlements";
    public static final String DETAIL_OVER_PROVISIONED_COUNT = "overProvisionedCount";
    public static final String DETAIL_DIRECT_ASSIGNMENT_COUNT = "directAssignmentCount";
    public static final String DETAIL_ERROR = "error";
    public static final String DETAIL_THRESHOLD = "threshold";

    // ===================================================================
    // Response Field Names
    // ===================================================================
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_SPRAWL_INDEX = "sprawlIndex";
    public static final String FIELD_STALE_ENTITLEMENTS = "staleEntitlements";
    public static final String FIELD_OVER_PROVISIONED_COUNT = "overProvisionedCount";
    public static final String FIELD_DIRECT_ASSIGNMENT_COUNT = "directAssignmentCount";
    public static final String FIELD_TIMESTAMP = "timestamp";

    // ===================================================================
    // API Documentation Messages
    // ===================================================================
    public static final String API_DESCRIPTION = "Retrieves entitlement sprawl KPI metrics";
    public static final String API_SUMMARY = "Get Entitlement Sprawl KPI Analytics";

    // ===================================================================
    // KPI Descriptions
    // ===================================================================
    public static final String KPI_SPRAWL_INDEX_DESC = "Average entitlements per user (currently mock - returns 0)";
    public static final String KPI_STALE_ENTITLEMENTS_DESC = "Entitlements deprecated or unused since start of year (currently mock - returns 0)";
    public static final String KPI_OVER_PROVISIONED_DESC = "Entitlements assigned to 100+ users";
    public static final String KPI_DIRECT_ASSIGNMENT_DESC = "Entitlements with direct manual assignments";

    private EntitlementSprawlKpiConstants() {
        // Utility class - private constructor
    }
}

