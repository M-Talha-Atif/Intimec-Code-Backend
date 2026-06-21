package com.aidentx.iam.access_roles_management.config.constants;

/**
 * Centralized constants for the AccessRole controller, service, and audit layers.
 *
 * <p>This class organizes constants by functional area to improve discoverability
 * and maintainability. All endpoint paths, log messages, error messages, and
 * response messages are grouped together by their usage context.</p>
 */
public final class AccessRoleConstants {

    private AccessRoleConstants() {
    }

    // ===================================================================
    //  HTTP API Endpoints
    // ===================================================================

    /**
     * Base REST path for access-role APIs.
     */
    public static final String BASE_PATH = "/api/v1/access-roles";

    /**
     * Path for a specific role by role ID.
     */
    public static final String PATH_ROLE_ID = "/{roleId}";

    /**
     * Path for role-entitlement operations.
     */
    public static final String PATH_ENTITLEMENTS = "/{roleId}/entitlements";

    /**
     * Path for comparing overlapping effective entitlements between two roles.
     */
    public static final String PATH_COMPARE_ENTITLEMENTS = "/compare/entitlements";

    /**
     * Path for role-child composition operations.
     */
    public static final String PATH_COMPOSITIONS = "/{roleId}/children";

    /**
     * Path for role-parent composition retrieval.
     */
    public static final String PATH_PARENTS = "/{roleId}/parents";

    /**
     * Path for unified direct/hierarchy/conflict role relationship retrieval.
     */
    public static final String PATH_RELATIONSHIPS = "/{roleId}/relationships";

    /**
     * Path for role audit history retrieval.
     */
    public static final String PATH_AUDIT_HISTORY = "/{roleId}/audit-history";

    /**
     * Path for role member retrieval.
     */
    public static final String PATH_MEMBERS = "/{roleId}/members";

    /**
     * Path for role member count and assignment-mode summary retrieval.
     */
    public static final String PATH_MEMBERS_SUMMARY = "/{roleId}/members/summary";

    /**
     * Path for grouped role member retrieval.
     */
    public static final String PATH_MEMBERS_GROUPED = "/{roleId}/members/grouped";

    /**
     * Path for one grouped member's effective entitlements.
     */
    public static final String PATH_MEMBER_ENTITLEMENTS = "/{roleId}/members/{assignmentId}/entitlements";

    /**
     * Path for bulk manual member grants to a role.
     */
    public static final String PATH_MEMBERS_MANUAL = "/{roleId}/members/manual";

    /**
     * Path for role entitlement distribution analytics.
     */
    public static final String PATH_ENTITLEMENT_DISTRIBUTION = "/{roleId}/entitlement-distribution";

    /**
     * Path for counts-only role delete impact preview.
     */
    public static final String PATH_DELETE_IMPACT = "/{roleId}/delete-impact";

    /**
     * Path for simulating the impact of saving role edits.
     */
    public static final String PATH_IMPACT_SIMULATION = "/{roleId}/impact-simulation";

    /**
     * Path for lightweight directory summary used by the role navigator.
     */
    public static final String PATH_DIRECTORY_SUMMARY = "/directory-summary";

    // ===================================================================
    //  HTTP Headers
    // ===================================================================

    /**
     * Header containing tenant identifier.
     */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    /** Header containing authenticated actor/user ID forwarded by the gateway. */
    public static final String HEADER_USER_ID = "X-User-ID";

    /** Header containing authenticated actor username/name forwarded by the gateway. */
    public static final String HEADER_USERNAME = "X-Username";

    /** Legacy/pre-authenticated gateway user header used as a username fallback. */
    public static final String HEADER_USER = "X-User";

    // ===================================================================
    //  Default Configuration Values
    // ===================================================================

    /**
     * Default status value assigned to newly created roles when not otherwise specified.
     */
    public static final String DEFAULT_STATUS = "Enabled";

    /**
     * Default role type assigned when create/import omits type.
     */
    public static final String DEFAULT_ROLE_TYPE = "No type";

    /**
     * Default page size for paginated role APIs.
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * Fallback business-unit label used when business unit is null or blank.
     */
    public static final String DEFAULT_BUSINESS_UNIT = "No business unit";

    /**
     * Placeholder value used in logs when an optional filter is not supplied.
     */
    public static final String DEFAULT_FILTER_ALL = "ALL";

    /**
     * Default lookback window for active member classification, in days.
     */
    public static final int DEFAULT_ACTIVITY_WINDOW_DAYS = 90;

    /**
     * String form of {@link #DEFAULT_ACTIVITY_WINDOW_DAYS} for annotation defaults.
     */
    public static final String DEFAULT_ACTIVITY_WINDOW_DAYS_PARAM = "90";

    // ===================================================================
    //  Log Categories
    // ===================================================================

    /**
     * Log category for service-layer role operations.
     */
    public static final String LOG_CATEGORY = "access-roles";

    /**
     * Log category for controller-layer role operations.
     */
    public static final String CONTROLLER_LOG_CATEGORY = "access-role-controller";

    // ===================================================================
    //  Service Layer Log Messages
    // ===================================================================

    // --- Role CRUD Operations ---

    /**
     * Service log message for successful role creation.
     */
    public static final String LOG_ROLE_CREATED = "Access role created successfully";

    /**
     * Service log message for successful role update.
     */
    public static final String LOG_ROLE_UPDATED = "Access role updated successfully";

    /**
     * Service log message for successful role deletion.
     */
    public static final String LOG_ROLE_DELETED = "Access role deleted successfully";

    /**
     * Service log message for successful role-detail retrieval.
     */
    public static final String LOG_ROLE_FETCHED = "Access role detail fetched";

    /**
     * Service log message for successful role list retrieval.
     */
    public static final String LOG_ROLES_LISTED = "Access roles listed";

    /**
     * Service log message for role-directory summary calculation start.
     */
    public static final String LOG_ROLE_DIRECTORY_SUMMARY_START =
            "Calculating access role directory summary";

    /**
     * Service log message for role-directory summary calculation completion.
     */
    public static final String LOG_ROLE_DIRECTORY_SUMMARY_COMPLETE =
            "Calculated access role directory summary";

    // --- Failure Scenarios ---

    /**
     * Service log message for role creation failure.
     */
    public static final String LOG_ROLE_CREATE_FAILED = "Failed to create access role";

    /**
     * Service log message for role update failure.
     */
    public static final String LOG_ROLE_UPDATE_FAILED = "Failed to update access role";

    /**
     * Service log message for role deletion failure.
     */
    public static final String LOG_ROLE_DELETE_FAILED = "Failed to delete access role";

    /**
     * Service log message when requested role does not exist.
     */
    public static final String LOG_ROLE_NOT_FOUND = "Access role not found";

    /**
     * Service log message when a duplicate role name is detected for a tenant.
     */
    public static final String LOG_ROLE_NAME_DUPLICATE = "Duplicate access role name for tenant";

    // ===================================================================
    //  Log Detail Keys (for structured logging)
    // ===================================================================

    /**
     * Structured log detail key for role ID.
     */
    public static final String DETAIL_ROLE_ID = "roleId";

    /**
     * Structured log detail key for assignment ID.
     */
    public static final String DETAIL_ASSIGNMENT_ID = "assignmentId";

    /**
     * Structured log detail key for role name.
     */
    public static final String DETAIL_ROLE_NAME = "roleName";

    /**
     * Structured log detail key for tenant ID.
     */
    public static final String DETAIL_TENANT_ID = "tenantId";

    /**
     * Structured log detail key for role type.
     */
    public static final String DETAIL_ROLE_TYPE = "roleType";

    /**
     * Structured log detail key for generic count.
     */
    public static final String DETAIL_COUNT = "count";

    /**
     * Structured log detail key for exception type.
     */
    public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";

    /**
     * Structured log detail key for error text.
     */
    public static final String DETAIL_ERROR = "error";

    /**
     * Structured log detail value representing missing/unavailable data.
     */
    public static final String DETAIL_NOT_AVAILABLE = "N/A";

    // Additional detail keys for controller logging

    /**
     * Structured controller log detail key for tenant ID.
     */
    public static final String LOG_DETAIL_TENANT_ID = "tenantId";

    /**
     * Structured controller log detail key for role ID.
     */
    public static final String LOG_DETAIL_ROLE_ID = "roleId";

    /**
     * Structured controller log detail key for role type filter.
     */
    public static final String LOG_DETAIL_TYPE = "type";

    /**
     * Structured controller log detail key for role status filter.
     */
    public static final String LOG_DETAIL_STATUS = "status";

    /**
     * Structured controller log detail key for page number.
     */
    public static final String LOG_DETAIL_PAGE = "page";

    /**
     * Structured controller log detail key for page size.
     */
    public static final String LOG_DETAIL_SIZE = "size";

    /**
     * Structured controller log detail key for action.
     */
    public static final String LOG_DETAIL_ACTION = "action";

    /**
     * Structured controller log detail key for business-unit filter.
     */
    public static final String LOG_DETAIL_BUSINESS_UNIT = "businessUnit";

    /**
     * Structured controller log detail key for search filter.
     */
    public static final String LOG_DETAIL_SEARCH = "search";

    /**
     * Structured controller log detail key for risk-level filter.
     */
    public static final String LOG_DETAIL_RISK_LEVEL = "riskLevel";

    // ===================================================================
    //  API Response Messages
    // ===================================================================

    // --- Role Operations ---

    /**
     * Success response message for role creation.
     */
    public static final String RESPONSE_ROLE_CREATED = "Access role created successfully";

    /**
     * Success response message for role update.
     */
    public static final String RESPONSE_ROLE_UPDATED = "Access role updated successfully";

    /**
     * Success response message for role deletion.
     */
    public static final String RESPONSE_ROLE_DELETED = "Access role deleted successfully";

    /**
     * Success response message for role-detail retrieval.
     */
    public static final String RESPONSE_ROLE_FETCHED = "Access role detail retrieved successfully";

    /**
     * Success response message for role-list retrieval.
     */
    public static final String RESPONSE_ROLES_LISTED = "Access roles retrieved successfully";

    /**
     * Success response message for role-directory summary retrieval.
     */
    public static final String RESPONSE_ROLE_DIRECTORY_SUMMARY_FETCHED =
            "Access role directory summary fetched successfully";

    // --- Entitlement Sync Operations ---

    /**
     * Success response message for entitlement synchronization.
     */
    public static final String RESPONSE_ENTITLEMENTS_SYNCED =
            "Role entitlements synchronized successfully";

    // --- Child Role Composition Operations ---

    /**
     * Success response message for full child-role synchronization.
     */
    public static final String RESPONSE_CHILDREN_SYNCED =
            "Role child composition synchronized successfully";

    /**
     * Success response message for partial child-role synchronization.
     */
    public static final String RESPONSE_CHILDREN_PARTIAL =
            "Role child composition synchronized with some failures";

    // --- Audit History Operations ---

    /**
     * Success response message for audit-history retrieval.
     */
    public static final String RESPONSE_AUDIT_HISTORY_FETCHED =
            "Role audit history fetched successfully";

    /**
     * Success response message for role member retrieval.
     */
    public static final String RESPONSE_ROLE_MEMBERS_FETCHED =
            "Role members fetched successfully";

    /**
     * Success response message for role member summary retrieval.
     */
    public static final String RESPONSE_ROLE_MEMBERS_SUMMARY_FETCHED =
            "Role member summary fetched successfully";

    /**
     * Success response message for grouped role member retrieval.
     */
    public static final String RESPONSE_ROLE_MEMBERS_GROUPED_FETCHED =
            "Grouped role members fetched successfully";

    /**
     * Success response message for grouped member entitlement drilldown retrieval.
     */
    public static final String RESPONSE_ROLE_MEMBER_ENTITLEMENTS_FETCHED =
            "Role member entitlements fetched successfully";

    /**
     * Success response message for bulk manual member grants.
     */
    public static final String RESPONSE_ROLE_MEMBERS_MANUAL_GRANTED =
            "Role members manually granted successfully";

    /**
     * Success response message for bulk manual member grants with partial failures.
     */
    public static final String RESPONSE_ROLE_MEMBERS_MANUAL_GRANTED_PARTIAL =
            "Role members manually granted with some failures";

    /**
     * Success response message for manual member sync.
     */
    public static final String RESPONSE_ROLE_MEMBERS_MANUAL_SYNCED =
            "Role manual members synchronized successfully";

    /**
     * Success response message for manual member sync with partial failures.
     */
    public static final String RESPONSE_ROLE_MEMBERS_MANUAL_SYNCED_PARTIAL =
            "Role manual members synchronized with some failures";

    /**
     * Success response message for role entitlement distribution retrieval.
     */
    public static final String RESPONSE_ROLE_ENTITLEMENT_DISTRIBUTION_FETCHED =
            "Role entitlement distribution fetched successfully";

    /**
     * Success response message for role entitlement comparison retrieval.
     */
    public static final String RESPONSE_ROLE_ENTITLEMENT_COMPARISON_FETCHED =
            "Role entitlement comparison fetched successfully";

    /**
     * Success response message for role delete impact preview retrieval.
     */
    public static final String RESPONSE_ROLE_DELETE_IMPACT_FETCHED =
            "Access role delete impact fetched successfully";

    /**
     * Success response message for role save impact simulation.
     */
    public static final String RESPONSE_ROLE_IMPACT_SIMULATED =
            "Access role save impact simulated successfully";

    // ===================================================================
    //  Error Messages
    // ===================================================================

    /**
     * Error message template when role is not found.
     */
    public static final String ERROR_ROLE_NOT_FOUND = "Access role not found with id: %s";

    /**
     * Error message template when role name already exists for a tenant.
     */
    public static final String ERROR_ROLE_NAME_EXISTS =
            "Access role with name '%s' already exists for this tenant";

    /** Error message when the mandatory organization reference is missing. */
    public static final String ERROR_ORGANIZATION_REQUIRED = "Organization is required";

    /** Error message when the mandatory business unit is missing. */
    public static final String ERROR_BUSINESS_UNIT_REQUIRED = "Business unit is required";

    /** Error message template when the organization does not belong to the tenant. */
    public static final String ERROR_ORGANIZATION_NOT_FOUND =
            "Organization not found for tenant: %s";

    /**
     * Error message when circular role composition is detected.
     */
    public static final String ERROR_CIRCULAR_COMPOSITION =
            "Cannot add child role: circular dependency detected";

    /**
     * Error message when a role is assigned as its own child.
     */
    public static final String ERROR_SELF_COMPOSITION =
            "A role cannot be a child of itself";

    // ===================================================================
    //  Controller Method Log Messages
    // ===================================================================

    // --- Role Management ---

    /**
     * Controller log message for role creation endpoint invocation.
     */
    public static final String LOG_CONTROLLER_CREATE_ROLE =
            "AccessRoleController.createRole invoked";

    /**
     * Controller log message for role-detail endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_ROLE_DETAIL =
            "AccessRoleController.getRoleDetail invoked";

    /**
     * Controller log message for role-list endpoint invocation.
     */
    public static final String LOG_CONTROLLER_LIST_ROLES =
            "AccessRoleController.listRoles invoked";

    /**
     * Controller log message for role-directory summary endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_ROLE_DIRECTORY_SUMMARY =
            "AccessRoleController.getRoleDirectorySummary invoked";

    /**
     * Controller log message for role update endpoint invocation.
     */
    public static final String LOG_CONTROLLER_UPDATE_ROLE =
            "AccessRoleController.updateRole invoked";

    /**
     * Controller log message for role deletion endpoint invocation.
     */
    public static final String LOG_CONTROLLER_DELETE_ROLE =
            "AccessRoleController.deleteRole invoked";

    /**
     * Controller log message for role delete impact preview endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_DELETE_IMPACT =
            "AccessRoleController.getDeleteImpact invoked";

    /**
     * Controller log message for role save impact simulation endpoint invocation.
     */
    public static final String LOG_CONTROLLER_SIMULATE_ROLE_IMPACT =
            "AccessRoleController.simulateRoleSaveImpact invoked";

    // --- Entitlement Management ---

    /**
     * Controller log message for add-entitlements endpoint invocation.
     */
    public static final String LOG_CONTROLLER_ADD_ENTITLEMENTS =
            "AccessRoleController.addEntitlements invoked";

    /**
     * Controller log message for remove-entitlements endpoint invocation.
     */
    public static final String LOG_CONTROLLER_REMOVE_ENTITLEMENTS =
            "AccessRoleController.removeEntitlements invoked";

    /**
     * Controller log message for get-entitlements endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_ENTITLEMENTS =
            "AccessRoleController.getEntitlementDetails invoked";

    /**
     * Controller log message for grouped-entitlements endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_GROUPED_ENTITLEMENTS =
            "AccessRoleController.getGroupedEntitlementDetails invoked";

    /**
     * Controller log message for role entitlement comparison endpoint invocation.
     */
    public static final String LOG_CONTROLLER_COMPARE_ENTITLEMENTS =
            "AccessRoleController.compareRoleEntitlements invoked";

    /**
     * Controller log message for entitlement-sync endpoint invocation.
     */
    public static final String LOG_CONTROLLER_SYNC_ENTITLEMENTS =
            "AccessRoleController.syncEntitlements invoked";

    // --- Child Role Management ---

    /**
     * Controller log message for add-child-roles endpoint invocation.
     */
    public static final String LOG_CONTROLLER_ADD_CHILDREN =
            "AccessRoleController.addChildRoles invoked";

    /**
     * Controller log message for remove-child-roles endpoint invocation.
     */
    public static final String LOG_CONTROLLER_REMOVE_CHILDREN =
            "AccessRoleController.removeChildRoles invoked";

    /**
     * Controller log message for get-child-roles endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_CHILDREN =
            "AccessRoleController.getChildRoles invoked";

    /**
     * Controller log message for get-parent-roles endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_PARENTS =
            "AccessRoleController.getParentRoles invoked";

    /**
     * Controller log message for child-role-sync endpoint invocation.
     */
    public static final String LOG_CONTROLLER_SYNC_CHILDREN =
            "AccessRoleController.syncChildRoles invoked";

    // --- Audit History ---

    /**
     * Controller log message for audit-history endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_AUDIT_HISTORY =
            "AccessRoleController.getAuditHistory invoked";

    /**
     * Controller log message for role members endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_ROLE_MEMBERS =
            "AccessRoleController.getRoleMembers invoked";

    /**
     * Controller log message for role member summary endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_ROLE_MEMBERS_SUMMARY =
            "AccessRoleController.getRoleMembersSummary invoked";

    /**
     * Controller log message for grouped role members endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_ROLE_MEMBERS_GROUPED =
            "AccessRoleController.getRoleMembersGrouped invoked";

    /**
     * Controller log message for grouped member entitlement drilldown endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_ROLE_MEMBER_ENTITLEMENTS =
            "AccessRoleController.getRoleMemberEntitlements invoked";

    /**
     * Controller log message for bulk manual member grant endpoint invocation.
     */
    public static final String LOG_CONTROLLER_MANUAL_GRANT_ROLE_MEMBERS =
            "AccessRoleController.manualGrantRoleMembers invoked";

    /**
     * Controller log message for manual member sync endpoint invocation.
     */
    public static final String LOG_CONTROLLER_SYNC_MANUAL_MEMBERS =
            "AccessRoleController.syncManualMembers invoked";

    /**
     * Controller log message for role entitlement distribution endpoint invocation.
     */
    public static final String LOG_CONTROLLER_GET_ENTITLEMENT_DISTRIBUTION =
            "AccessRoleController.getEntitlementDistribution invoked";
}