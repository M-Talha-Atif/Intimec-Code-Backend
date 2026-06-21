package com.aidentx.iam.access_roles_management.config.constants;

/**
 * Constants for Access Role Intelligence analytics endpoints, messages, labels,
 * and logging keys.
 */
public final class AccessRoleIntelligenceConstants {

    private AccessRoleIntelligenceConstants() {
    }

    // ===================================================================
    //  Endpoint Paths
    // ===================================================================

    /**
     * Base path segment for access-role intelligence analytics.
     */
    public static final String INTELLIGENCE_BASE_PATH = "/intelligence";

    /**
     * Endpoint path for intelligence KPIs.
     */
    public static final String INTELLIGENCE_KPIS_PATH = "/kpis";

    /**
     * Endpoint path for role distribution by type chart.
     */
    public static final String ROLE_DISTRIBUTION_BY_TYPE_PATH = "/role-distribution-by-type";

    /**
     * Endpoint path for roles and users by business unit chart.
     */
    public static final String ROLES_USERS_BY_BUSINESS_UNIT_PATH = "/roles-users-by-business-unit";

    // ===================================================================
    //  Response Messages
    // ===================================================================

    /**
     * Success message for intelligence KPI response.
     */
    public static final String MESSAGE_INTELLIGENCE_KPIS_FETCHED =
            "Access role intelligence KPIs fetched successfully";

    /**
     * Success message for role distribution by type response.
     */
    public static final String MESSAGE_ROLE_DISTRIBUTION_BY_TYPE_FETCHED =
            "Access role distribution by type fetched successfully";

    /**
     * Success message for roles and users by business unit response.
     */
    public static final String MESSAGE_ROLES_USERS_BY_BUSINESS_UNIT_FETCHED =
            "Access role business unit analytics fetched successfully";

    // ===================================================================
    //  Business Labels / Defaults
    // ===================================================================

    /**
     * Role status value representing active roles.
     */
    public static final String ROLE_STATUS_ACTIVE = "active";

    /**
     * Role status value representing disabled roles.
     */
    public static final String ROLE_STATUS_DISABLED = "disabled";

    /**
     * Default business unit label for roles without a business unit.
     */
    public static final String BUSINESS_UNIT_UNSPECIFIED = "Unspecified";

    /**
     * Mock orphaned-role count until orphan detection is implemented.
     */
    public static final long MOCK_ORPHANED_ROLES = 0L;

    /**
     * Mock user count for business-unit chart until role-user assignment analytics are implemented.
     */
    public static final long MOCK_USERS_COUNT = 0L;

    /**
     * Default average when no roles exist.
     */
    public static final double DEFAULT_AVERAGE_ENTITLEMENTS_PER_ROLE = 0.0d;

    // ===================================================================
    //  Logging Messages
    // ===================================================================

    /**
     * Controller log for KPI request start.
     */
    public static final String LOG_CONTROLLER_KPIS_REQUEST =
            "Received access role intelligence KPI request";

    /**
     * Controller log for role-distribution request start.
     */
    public static final String LOG_CONTROLLER_ROLE_DISTRIBUTION_REQUEST =
            "Received access role distribution by type request";

    /**
     * Controller log for business-unit analytics request start.
     */
    public static final String LOG_CONTROLLER_BUSINESS_UNIT_REQUEST =
            "Received access role business unit analytics request";

    /**
     * Controller log for KPI request completion.
     */
    public static final String LOG_CONTROLLER_KPIS_RESPONSE =
            "Completed access role intelligence KPI request";

    /**
     * Controller log for role-distribution request completion.
     */
    public static final String LOG_CONTROLLER_ROLE_DISTRIBUTION_RESPONSE =
            "Completed access role distribution by type request";

    /**
     * Controller log for business-unit analytics request completion.
     */
    public static final String LOG_CONTROLLER_BUSINESS_UNIT_RESPONSE =
            "Completed access role business unit analytics request";

    /**
     * Service log for KPI analytics start.
     */
    public static final String LOG_SERVICE_KPIS_START =
            "Calculating access role intelligence KPIs";

    /**
     * Service log for KPI analytics completion.
     */
    public static final String LOG_SERVICE_KPIS_COMPLETE =
            "Calculated access role intelligence KPIs";

    /**
     * Service log for role-distribution analytics start.
     */
    public static final String LOG_SERVICE_ROLE_DISTRIBUTION_START =
            "Calculating role distribution by type";

    /**
     * Service log for role-distribution analytics completion.
     */
    public static final String LOG_SERVICE_ROLE_DISTRIBUTION_COMPLETE =
            "Calculated role distribution by type";

    /**
     * Service log for business-unit analytics start.
     */
    public static final String LOG_SERVICE_BUSINESS_UNIT_START =
            "Calculating roles and users by business unit";

    /**
     * Service log for business-unit analytics completion.
     */
    public static final String LOG_SERVICE_BUSINESS_UNIT_COMPLETE =
            "Calculated roles and users by business unit";

    // ===================================================================
    //  Log Detail Keys
    // ===================================================================

    /**
     * Log detail key for tenant identifier.
     */
    public static final String DETAIL_TENANT_ID = "tenantId";

    /**
     * Log detail key for total role count.
     */
    public static final String DETAIL_TOTAL_ROLES = "totalRoles";

    /**
     * Log detail key for active role count.
     */
    public static final String DETAIL_ACTIVE_ROLES = "activeRoles";

    /**
     * Log detail key for disabled role count.
     */
    public static final String DETAIL_DISABLED_ROLES = "disabledRoles";

    /**
     * Log detail key for orphaned role count.
     */
    public static final String DETAIL_ORPHANED_ROLES = "orphanedRoles";

    /**
     * Log detail key for average entitlements per role.
     */
    public static final String DETAIL_AVG_ENTITLEMENTS_PER_ROLE = "averageEntitlementsPerRole";

    /**
     * Log detail key for high-risk role count.
     */
    public static final String DETAIL_HIGH_RISK_ROLES = "highRiskRoles";

    /**
     * Log detail key for chart row count.
     */
    public static final String DETAIL_ROW_COUNT = "rowCount";
}