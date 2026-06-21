package com.aidentx.iam.entitlement_management.config.constants.analytics;

/**
 * Constants for Entitlement Risk Distribution Analytics.
 * Contains all hardcoded values, log messages, field names and headers
 * to ensure no magic strings in service code.
 *
 * This is specifically for Risk Distribution analytics - tracks entitlement counts by risk level.
 */
public class EntitlementRiskDistributionConstants {

    // ===================================================================
    // Header Constants
    // ===================================================================
    public static final String HEADER_TENANT_ID = "X-Tenant-ID";

    // ===================================================================
    // Logger Constants
    // ===================================================================
    public static final String LOGGER_MODULE_NAME = "RISK_DISTRIBUTION";

    // ===================================================================
    // Numeric Constants
    // ===================================================================
    public static final int ZERO_COUNT = 0;

    // ===================================================================
    // Risk Level Constants
    // ===================================================================
    public static final String RISK_LEVEL_HIGH = "HIGH";
    public static final String RISK_LEVEL_MEDIUM = "MEDIUM";
    public static final String RISK_LEVEL_LOW = "LOW";

    // ===================================================================
    // Log Messages - INFO Level
    // ===================================================================
    public static final String LOG_FETCHING_RISK_DISTRIBUTION =
            "Fetching entitlement risk distribution for tenantId: {}";
    public static final String LOG_RISK_DISTRIBUTION_CALCULATION_SUCCESS =
            "Successfully calculated risk distribution: highRiskEntitlements={}, mediumRiskEntitlements={}, lowRiskEntitlements={}";
    public static final String LOG_RISK_DISTRIBUTION_RETURNED_SUCCESS =
            "Successfully returned risk distribution";

    // ===================================================================
    // Log Messages - DEBUG Level
    // ===================================================================
    public static final String LOG_DEBUG_CALCULATING_HIGH_RISK =
            "Calculating HIGH risk entitlement count for tenantId: {}";
    public static final String LOG_DEBUG_CALCULATING_MEDIUM_RISK =
            "Calculating MEDIUM risk entitlement count for tenantId: {}";
    public static final String LOG_DEBUG_CALCULATING_LOW_RISK =
            "Calculating LOW risk entitlement count for tenantId: {}";

    // ===================================================================
    // Log Messages - ERROR Level
    // ===================================================================
    public static final String LOG_ERROR_INVALID_REQUEST =
            "Invalid risk distribution request parameters: {}";
    public static final String LOG_ERROR_CALCULATING_RISK_DISTRIBUTION =
            "Error calculating risk distribution: {}";

    // ===================================================================
    // Validation Error Messages
    // ===================================================================
    public static final String ERROR_TENANT_ID_BLANK = "Tenant ID cannot be null or blank";

    // ===================================================================
    // Logging Detail Keys
    // ===================================================================
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_HIGH_RISK_ENTITLEMENTS = "highRiskEntitlements";
    public static final String DETAIL_MEDIUM_RISK_ENTITLEMENTS = "mediumRiskEntitlements";
    public static final String DETAIL_LOW_RISK_ENTITLEMENTS = "lowRiskEntitlements";
    public static final String DETAIL_ERROR = "error";

    // ===================================================================
    // Response Field Names
    // ===================================================================
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_HIGH_RISK_ENTITLEMENTS = "highRiskEntitlements";
    public static final String FIELD_MEDIUM_RISK_ENTITLEMENTS = "mediumRiskEntitlements";
    public static final String FIELD_LOW_RISK_ENTITLEMENTS = "lowRiskEntitlements";
    public static final String FIELD_TIMESTAMP = "timestamp";

    // ===================================================================
    // API Documentation Messages
    // ===================================================================
    public static final String API_DESCRIPTION = "Retrieves entitlement risk distribution metrics";
    public static final String API_SUMMARY = "Get Entitlement Risk Distribution Analytics";

    // ===================================================================
    // KPI Descriptions
    // ===================================================================
    public static final String KPI_HIGH_RISK_DESC = "Count of entitlements with HIGH risk level";
    public static final String KPI_MEDIUM_RISK_DESC = "Count of entitlements with MEDIUM risk level";
    public static final String KPI_LOW_RISK_DESC = "Count of entitlements with LOW risk level";

    private EntitlementRiskDistributionConstants() {
        // Utility class - private constructor
    }
}