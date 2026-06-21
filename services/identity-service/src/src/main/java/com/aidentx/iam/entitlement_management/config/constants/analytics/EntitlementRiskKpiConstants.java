package com.aidentx.iam.entitlement_management.config.constants.analytics;

/**
 * Constants for Entitlement Risk KPI Analytics.
 * Contains all hardcoded values, log messages, field names and headers
 * to ensure no magic strings in service code.
 *
 * This is specifically for Risk KPI analytics - tracks entitlement risk posture metrics.
 */
public class EntitlementRiskKpiConstants {

    // ===================================================================
    // Header Constants
    // ===================================================================
    public static final String HEADER_TENANT_ID = "X-Tenant-ID";

    // ===================================================================
    // Logger Constants
    // ===================================================================
    public static final String LOGGER_MODULE_NAME = "RISK_KPI";

    // ===================================================================
    // Numeric Constants
    // ===================================================================
    public static final int ZERO_COUNT = 0;

    // ===================================================================
    // Risk Level Constants
    // ===================================================================
    public static final String RISK_LEVEL_HIGH = "HIGH";

    // ===================================================================
    // Log Messages - INFO Level
    // ===================================================================
    public static final String LOG_FETCHING_RISK_KPI = "Fetching entitlement risk KPIs for tenantId: {}";
    public static final String LOG_RISK_KPI_CALCULATION_SUCCESS =
            "Successfully calculated risk KPIs: highRiskEntitlements={}, sodExposure={}, complianceTagged={}, dataSensitive={}";
    public static final String LOG_RISK_KPI_RETURNED_SUCCESS = "Successfully returned risk KPIs";

    // ===================================================================
    // Log Messages - DEBUG Level
    // ===================================================================
    public static final String LOG_DEBUG_CALCULATING_HIGH_RISK_ENTITLEMENTS =
            "Calculating high-risk entitlements count for tenantId: {}";

    // ===================================================================
    // Log Messages - ERROR Level
    // ===================================================================
    public static final String LOG_ERROR_INVALID_REQUEST = "Invalid risk KPI request parameters: {}";
    public static final String LOG_ERROR_CALCULATING_RISK_KPI = "Error calculating risk KPIs: {}";

    // ===================================================================
    // Validation Error Messages
    // ===================================================================
    public static final String ERROR_TENANT_ID_BLANK = "Tenant ID cannot be null or blank";

    // ===================================================================
    // Logging Detail Keys
    // ===================================================================
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_HIGH_RISK_ENTITLEMENTS = "highRiskEntitlements";
    public static final String DETAIL_SOD_EXPOSURE = "sodExposure";
    public static final String DETAIL_COMPLIANCE_TAGGED = "complianceTagged";
    public static final String DETAIL_DATA_SENSITIVE = "dataSensitive";
    public static final String DETAIL_ERROR = "error";

    // ===================================================================
    // Response Field Names
    // ===================================================================
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_HIGH_RISK_ENTITLEMENTS = "highRiskEntitlements";
    public static final String FIELD_SOD_EXPOSURE = "sodExposure";
    public static final String FIELD_COMPLIANCE_TAGGED = "complianceTagged";
    public static final String FIELD_DATA_SENSITIVE = "dataSensitive";
    public static final String FIELD_TIMESTAMP = "timestamp";

    // ===================================================================
    // API Documentation Messages
    // ===================================================================
    public static final String API_DESCRIPTION = "Retrieves entitlement risk KPI metrics";
    public static final String API_SUMMARY = "Get Entitlement Risk KPI Analytics";

    // ===================================================================
    // KPI Descriptions
    // ===================================================================
    public static final String KPI_HIGH_RISK_ENTITLEMENTS_DESC =
            "Count of entitlements with HIGH risk level";
    public static final String KPI_SOD_EXPOSURE_DESC =
            "Count of SoD exposure entitlements (currently mock - returns 0)";
    public static final String KPI_COMPLIANCE_TAGGED_DESC =
            "Count of compliance tagged entitlements (currently mock - returns 0)";
    public static final String KPI_DATA_SENSITIVE_DESC =
            "Count of data sensitive entitlements (currently mock - returns 0)";

    private EntitlementRiskKpiConstants() {
        // Utility class - private constructor
    }
}