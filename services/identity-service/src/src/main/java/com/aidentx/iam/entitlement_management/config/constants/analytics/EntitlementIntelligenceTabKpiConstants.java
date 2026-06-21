package com.aidentx.iam.entitlement_management.config.constants.analytics;

/**
 * Constants for Entitlement Intelligence Tab KPI Analytics
 * Contains all hardcoded values, log messages, field names and headers
 * to ensure no magic strings in service code
 * 
 * This is specifically for Intelligence Tab KPI analytics.
 */
public class EntitlementIntelligenceTabKpiConstants {

    // ===================================================================
    // Lifecycle Constants
    // ===================================================================
    public static final String LIFECYCLE_ACTIVE = "Active";
    public static final String LIFECYCLE_DEPRECATED = "Deprecated";
    public static final String LIFECYCLE_ORPHANED = "Orphaned";
    public static final String LIFECYCLE_INACTIVE = "Inactive";

    // ===================================================================
    // Risk Level Constants
    // ===================================================================
    public static final String RISK_LEVEL_HIGH = "High";
    public static final String RISK_LEVEL_MEDIUM = "Medium";
    public static final String RISK_LEVEL_LOW = "Low";

    // ===================================================================
    // Numeric Constants
    // ===================================================================
    public static final int ZERO_COUNT = 0;
    public static final double ZERO_DOUBLE = 0.0;
    public static final int DECIMAL_PRECISION = 2;

    // ===================================================================
    // Header Constants
    // ===================================================================
    public static final String HEADER_TENANT_ID = "X-Tenant-ID";

    // ===================================================================
    // Logger Constants
    // ===================================================================
    public static final String LOGGER_MODULE_NAME = "INTELLIGENCE_KPI";

    // ===================================================================
    // Log Messages - INFO Level
    // ===================================================================
    public static final String LOG_FETCHING_KPI = "Fetching entitlement KPIs for tenantId: {}";
    public static final String LOG_KPI_CALCULATION_SUCCESS = "Successfully calculated entitlement KPIs: totalEntitlements={}, activeEntitlements={}, orphanedEntitlements={}, unclassifiedEntitlements={}, highRiskEntitlements={}, averageUsersPerEntitlement={}";
    public static final String LOG_KPI_RETURNED_SUCCESS = "Successfully returned entitlement KPIs";

    // ===================================================================
    // Log Messages - DEBUG Level
    // ===================================================================
    public static final String LOG_DEBUG_NO_ENTITLEMENTS = "No entitlements found for tenantId: {}. Returning 0.0 for average.";

    // ===================================================================
    // Log Messages - ERROR Level
    // ===================================================================
    public static final String LOG_ERROR_INVALID_REQUEST = "Invalid KPI request parameters: {}";
    public static final String LOG_ERROR_CALCULATING_KPI = "Error calculating entitlement KPIs: {}";

    // ===================================================================
    // Validation Error Messages
    // ===================================================================
    public static final String ERROR_TENANT_ID_BLANK = "Tenant ID cannot be null or blank";

    // ===================================================================
    // Logging Detail Keys
    // ===================================================================
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_TOTAL_ENTITLEMENTS = "totalEntitlements";
    public static final String DETAIL_ACTIVE_ENTITLEMENTS = "activeEntitlements";
    public static final String DETAIL_ORPHANED_ENTITLEMENTS = "orphanedEntitlements";
    public static final String DETAIL_UNCLASSIFIED_ENTITLEMENTS = "unclassifiedEntitlements";
    public static final String DETAIL_HIGH_RISK_ENTITLEMENTS = "highRiskEntitlements";
    public static final String DETAIL_AVERAGE_USERS_PER_ENTITLEMENT = "averageUsersPerEntitlement";
    public static final String DETAIL_ERROR = "error";

    // ===================================================================
    // Response Field Names
    // ===================================================================
    public static final String FIELD_TOTAL_ENTITLEMENTS = "totalEntitlements";
    public static final String FIELD_ACTIVE_ENTITLEMENTS = "activeEntitlements";
    public static final String FIELD_ORPHANED_ENTITLEMENTS = "orphanedEntitlements";
    public static final String FIELD_UNCLASSIFIED_ENTITLEMENTS = "unclassifiedEntitlements";
    public static final String FIELD_HIGH_RISK_ENTITLEMENTS = "highRiskEntitlements";
    public static final String FIELD_AVERAGE_USERS_PER_ENTITLEMENT = "averageUsersPerEntitlement";
    public static final String FIELD_TIMESTAMP = "timestamp";
    public static final String FIELD_TENANT_ID = "tenantId";

    // ===================================================================
    // API Documentation Messages
    // ===================================================================
    public static final String API_KPI_DESCRIPTION = "Retrieves comprehensive KPI metrics for entitlement governance";
    public static final String API_KPI_SUMMARY = "Get Entitlement Intelligence Tab KPI Analytics";

    private EntitlementIntelligenceTabKpiConstants() {
        // Utility class - private constructor
    }
}



