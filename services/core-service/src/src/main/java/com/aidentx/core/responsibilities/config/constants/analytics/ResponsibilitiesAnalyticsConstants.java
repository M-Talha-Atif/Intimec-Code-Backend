package com.aidentx.core.responsibilities.config.constants.analytics;

/**
 * Constants for the Responsibilities Analytics module.
 *
 * <p>Covers API paths, log message templates, log detail keys,
 * owned roles response wrapper keys, and the userCount field name
 * used to compute total members across owned roles.
 *
 * <p>All string literals that appear in
 * {@link com.aidentx.core.responsibilities.service.impl.ResponsibilitiesAnalyticsServiceImpl}
 * and
 * {@link com.aidentx.core.responsibilities.controller.ResponsibilitiesAnalyticsController}
 * are defined here so they can be changed in one place.
 */
public final class ResponsibilitiesAnalyticsConstants {

    private ResponsibilitiesAnalyticsConstants() {}

    // -------------------------------------------------------------------------
    // API paths
    // -------------------------------------------------------------------------

    /**
     * Base path for all responsibilities analytics endpoints.
     * Full path: /api/v1/responsibilities/analytics
     */
    public static final String BASE_PATH = "/api/v1/responsibilities/analytics";

    /**
     * KPIs sub-path.
     * Full path: /api/v1/responsibilities/analytics/kpis
     */
    public static final String PATH_KPIS = "/kpis";

    // -------------------------------------------------------------------------
    // HTTP header names
    // -------------------------------------------------------------------------

    public static final String HEADER_AUTHORIZATION = "Authorization";

    // -------------------------------------------------------------------------
    // Owned roles response wrapper keys
    // Tried in order when the response is a Map rather than a direct List.
    // The first key whose value is a List wins.
    // -------------------------------------------------------------------------

    public static final String ROLES_KEY_ROLES   = "roles";
    public static final String ROLES_KEY_DATA    = "data";
    public static final String ROLES_KEY_CONTENT = "content";
    public static final String ROLES_KEY_ITEMS   = "items";

    /**
     * Ordered array of wrapper keys tried when extracting the roles list
     * from a Map response. Avoids repeating the array literal in service code.
     */
    public static final String[] ROLES_WRAPPER_KEYS = {
        ROLES_KEY_ROLES,
        ROLES_KEY_DATA,
        ROLES_KEY_CONTENT,
        ROLES_KEY_ITEMS,
    };

    // -------------------------------------------------------------------------
    // Field names within each role map
    // -------------------------------------------------------------------------

    /**
     * Field name for the member count on each owned role entry.
     * Matches the frontend formula:
     * {@code internalRoles.reduce((s, r) => s + (r.userCount ?? 0), 0)}
     */
    public static final String FIELD_USER_COUNT = "userCount";

    // -------------------------------------------------------------------------
    // Log detail keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_USER_ID               = "userId";
    public static final String LOG_KEY_TENANT_ID             = "tenantId";
    public static final String LOG_KEY_COUNT                 = "count";
    public static final String LOG_KEY_ROLE_COUNT            = "roleCount";
    public static final String LOG_KEY_TOTAL_MEMBERS         = "totalMembers";
    public static final String LOG_KEY_TOTAL_RESPONSIBILITIES = "totalResponsibilities";
    public static final String LOG_KEY_PENDING_TASKS         = "pendingTasks";
    public static final String LOG_KEY_OWNED_ROLES           = "ownedRoles";
    public static final String LOG_KEY_OWNED_ROLES_MEMBERS   = "ownedRolesTotalMembers";
    public static final String LOG_KEY_RESPONSE_TYPE         = "responseType";

    // -------------------------------------------------------------------------
    // Log message templates -- service
    // -------------------------------------------------------------------------

    public static final String LOG_COMPUTING_KPIS =
            "Computing responsibilities KPIs";

    public static final String LOG_KPIS_COMPUTED =
            "Responsibilities KPIs computed";

    public static final String LOG_PENDING_FETCHED =
            "Pending approvals fetched for KPI";

    public static final String LOG_PENDING_FAILED =
            "Failed to fetch pending approvals for KPI, defaulting to 0";

    public static final String LOG_PROCESSED_FETCHED =
            "Processed approvals fetched for KPI";

    public static final String LOG_PROCESSED_FAILED =
            "Failed to fetch processed approvals for KPI, defaulting to 0";

    public static final String LOG_OWNED_ROLES_FETCHED =
            "Owned roles fetched for KPI";

    public static final String LOG_OWNED_ROLES_FAILED =
            "Failed to fetch owned roles for KPI, defaulting to 0";

    public static final String LOG_UNRECOGNISED_SHAPE =
            "Owned roles response has unrecognised shape, returning empty list";

    // -------------------------------------------------------------------------
    // Log message templates -- controller
    // -------------------------------------------------------------------------

    public static final String LOG_KPI_REQUEST_RECEIVED =
            "Responsibilities analytics KPI request received";
}