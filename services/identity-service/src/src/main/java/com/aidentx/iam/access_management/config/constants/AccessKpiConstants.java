package com.aidentx.iam.access_management.config.constants;

/**
 * Constants for cross-domain access KPI analytics.
 *
 * <p>The KPI endpoint combines entitlement-assignment and access-role data, so
 * path segments, labels, source tokens, units, and logging keys are centralized
 * here to avoid hardcoded literals across controllers and services.</p>
 */
public final class AccessKpiConstants {

    private AccessKpiConstants() {
    }

    /** Base path for access analytics endpoints exposed through the identity service. */
    public static final String ACCESS_ANALYTICS_BASE_PATH = "/api/v1/access/analytics";

    /** Endpoint path for access KPI cards. */
    public static final String ACCESS_KPIS_PATH = "/access-kpis";

    /** Tenant context header propagated by the gateway. */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /** Success message returned by the access KPI endpoint. */
    public static final String MESSAGE_ACCESS_KPIS_FETCHED = "Access KPIs fetched successfully";

    /** Period label used while KPIs are computed live without historical snapshots. */
    public static final String PERIOD_LABEL_CURRENT = "current period";

    /** Delta direction used while no previous-period snapshot is available. */
    public static final String DELTA_DIRECTION_NONE = "none";

    /** Percent unit label for ratio KPIs. */
    public static final String UNIT_PERCENT = "percent";

    /** Count unit label for count KPIs. */
    public static final String UNIT_COUNT = "count";

    /** Average unit label for average KPIs. */
    public static final String UNIT_AVERAGE = "average";

    /** KPI response key for role coverage ratio. */
    public static final String KPI_ROLE_COVERAGE_RATIO = "roleCoverageRatio";

    /** KPI response key for direct access exposure. */
    public static final String KPI_DIRECT_ACCESS_EXPOSURE = "directAccessExposure";

    /** KPI response key for role complexity index. */
    public static final String KPI_ROLE_COMPLEXITY_INDEX = "roleComplexityIndex";

    /** KPI response key for orphaned access objects. */
    public static final String KPI_ORPHANED_ACCESS_OBJECTS = "orphanedAccessObjects";

    /** KPI response key for access reuse score. */
    public static final String KPI_ACCESS_REUSE_SCORE = "accessReuseScore";

    /** Active access-role status used by role-complexity denominator. */
    public static final String ROLE_STATUS_ACTIVE = "active";

    /** Entitlement assignment source for access granted through roles. */
    public static final String SOURCE_ROLE_DERIVED = "ROLE_DERIVED";

    /** Canonical direct entitlement assignment source. */
    public static final String SOURCE_DIRECT = "DIRECT";

    /** Legacy manual entitlement assignment source kept for backward-compatible analytics. */
    public static final String SOURCE_MANUAL = "MANUAL";

    /** Legacy import entitlement assignment source treated as direct exposure. */
    public static final String SOURCE_IMPORT = "IMPORT";

    /** Numeric threshold for reused roles. */
    public static final long REUSED_ROLE_MIN_MEMBER_COUNT = 2L;

    /** Scale used for percentage and average KPI values. */
    public static final int KPI_VALUE_SCALE = 2;

    /** Controller log for KPI request start. */
    public static final String LOG_CONTROLLER_ACCESS_KPIS_REQUEST = "Received access KPI analytics request";

    /** Controller log for KPI request completion. */
    public static final String LOG_CONTROLLER_ACCESS_KPIS_RESPONSE = "Completed access KPI analytics request";

    /** Service log for KPI calculation start. */
    public static final String LOG_SERVICE_ACCESS_KPIS_START = "Calculating access KPIs";

    /** Service log for KPI calculation completion. */
    public static final String LOG_SERVICE_ACCESS_KPIS_COMPLETE = "Calculated access KPIs";

    /** Service debug log after raw aggregate retrieval. */
    public static final String LOG_SERVICE_ACCESS_KPIS_AGGREGATES = "Fetched raw access KPI aggregates";

    /** Log detail key for tenant identifier. */
    public static final String DETAIL_TENANT_ID = "tenantId";

    /** Log detail key for total entitlement assignments. */
    public static final String DETAIL_TOTAL_ENTITLEMENT_ASSIGNMENTS = "totalEntitlementAssignments";

    /** Log detail key for role-derived entitlement assignments. */
    public static final String DETAIL_ROLE_DERIVED_ASSIGNMENTS = "roleDerivedAssignments";

    /** Log detail key for direct entitlement assignments. */
    public static final String DETAIL_DIRECT_ASSIGNMENTS = "directAssignments";

    /** Log detail key for total roles. */
    public static final String DETAIL_TOTAL_ROLES = "totalRoles";

    /** Log detail key for active roles. */
    public static final String DETAIL_ACTIVE_ROLES = "activeRoles";

    /** Log detail key for total role-entitlement memberships. */
    public static final String DETAIL_ROLE_ENTITLEMENT_MEMBERSHIPS = "roleEntitlementMemberships";

    /** Log detail key for orphaned entitlements. */
    public static final String DETAIL_ORPHANED_ENTITLEMENTS = "orphanedEntitlements";

    /** Log detail key for orphaned roles. */
    public static final String DETAIL_ORPHANED_ROLES = "orphanedRoles";

    /** Log detail key for reused roles. */
    public static final String DETAIL_REUSED_ROLES = "reusedRoles";
}

