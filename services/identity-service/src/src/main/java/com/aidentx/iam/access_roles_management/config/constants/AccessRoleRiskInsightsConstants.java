package com.aidentx.iam.access_roles_management.config.constants;

/**
 * Constants for the role risk insights feature.
 *
 * <p>The configurable max depth caps how deep the closure traversal contributes
 * to counts. Default is {@link #MAX_DEPTH_DEFAULT}; override via the
 * {@link #CONFIG_KEY_MAX_DEPTH} property.</p>
 */
public final class AccessRoleRiskInsightsConstants {

    private AccessRoleRiskInsightsConstants() {
    }

    /** Sub-path appended to {@link AccessRoleConstants#BASE_PATH}. */
    public static final String PATH_RISK_INSIGHTS = "/{roleId}/risk-insights";

    /** Property key for the configurable closure-depth cap. */
    public static final String CONFIG_KEY_MAX_DEPTH = "access-roles.risk-insights.max-depth";

    /** Default value of {@link #CONFIG_KEY_MAX_DEPTH}. */
    public static final int MAX_DEPTH_DEFAULT = 15;

    /** Lower-case risk level token treated as "high risk". */
    public static final String RISK_LEVEL_HIGH = "high";

    /** Lower-case privilege level token treated as "admin privilege". */
    public static final String PRIVILEGE_LEVEL_ADMIN = "admin";

    /** Log category for the risk-insights service. */
    public static final String SERVICE_LOG_CATEGORY = "access-role-risk-insights-service";

    /** Log message for controller entry. */
    public static final String LOG_CONTROLLER_GET_INSIGHTS = "Fetching role risk insights";

    /** Log message for service-level computation entry. */
    public static final String LOG_SERVICE_COMPUTE = "Computing role risk insights";

    /** Success message wrapped in {@code ApiResponse}. */
    public static final String RESPONSE_INSIGHTS_FETCHED = "Role risk insights fetched";

    /** Error template when the target role is missing. */
    public static final String ERROR_ROLE_NOT_FOUND = "Access role not found: %s";
}

