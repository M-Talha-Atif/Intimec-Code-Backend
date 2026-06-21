package com.aidentx.iam.access_roles_management.config.constants;

import com.aidentx.iam.entitlement_management.config.constants.EntitlementAssignmentSourceConstants;

/**
 * Constants used by access-role assignment-rule CRUD APIs, services, scans,
 * execution history, and role-derived entitlement propagation.
 *
 * <p>An access-role assignment rule defines identity-attribute criteria under
 * which an access role should be assigned. The rule targets an access role via
 * {@code roleId}; entitlement and child-role entitlement expansion are handled
 * by the role-assignment propagation flow.</p>
 *
 * <p>This constants class centralizes:
 * <ul>
 *   <li>API paths and table names</li>
 *   <li>header names and default values</li>
 *   <li>error and response messages</li>
 *   <li>structured logging messages and detail keys</li>
 * </ul>
 * so that controllers and services can produce consistent log output and
 * operational traces.</p>
 */
public final class AccessRoleAssignmentRuleConstants {

    /**
     * Logger category for access-role assignment-rule related operations.
     */
    public static final String LOG_CATEGORY = "access-role-assignment-rules";

    /** Dynamic assignment source marker for entitlement-rule-created grants. */
    public static final String ENTITLEMENT_ASSIGNMENT_SOURCE_DYNAMIC = EntitlementAssignmentSourceConstants.SOURCE_DYNAMIC;

    /** Role-derived assignment source marker for access-role-created entitlement grants. */
    public static final String ENTITLEMENT_ASSIGNMENT_SOURCE_ROLE_DERIVED = EntitlementAssignmentSourceConstants.SOURCE_ROLE_DERIVED;

    /**
     * Direct assignment source marker for non-rule entitlement grants, including
     * manual/UI and file/import flows.
     */
    public static final String ENTITLEMENT_ASSIGNMENT_SOURCE_DIRECT = EntitlementAssignmentSourceConstants.SOURCE_DIRECT;

    /**
     * Legacy manual assignment source marker kept for backward-compatible reads.
     */
    public static final String ENTITLEMENT_ASSIGNMENT_SOURCE_MANUAL = EntitlementAssignmentSourceConstants.LEGACY_SOURCE_MANUAL;

    /**
     * Legacy import assignment source marker kept for backward-compatible reads.
     */
    public static final String ENTITLEMENT_ASSIGNMENT_SOURCE_IMPORT = EntitlementAssignmentSourceConstants.LEGACY_SOURCE_IMPORT;

    /**
     * Shadow kind used when syncing entitlements only to eligible account shadows.
     */
    public static final String SHADOW_KIND_ACCOUNT = "account";

    /**
     * Separator used when building composite assignment source keys.
     */
    public static final String ASSIGNMENT_KEY_SEPARATOR = "|";

    private AccessRoleAssignmentRuleConstants() {
    }

    // -------------------------------------------------------------------------
    // Persistence / API
    // -------------------------------------------------------------------------

    /**
     * Database table name for access-role assignment rules.
     */
    public static final String TABLE_ACCESS_ROLE_ASSIGNMENT_RULES = "access_role_assignment_rules";

    /**
     * Base REST path for CRUD and trigger endpoints.
     */
    public static final String API_BASE_PATH = "/api/v1/access-roles/assignment-rules";

    /**
     * REST path for assignment-rule execution history APIs.
     */
    public static final String API_EXECUTION_HISTORY_PATH = "/api/v1/access-roles/assignment-rules/executions";

    /**
     * Relative REST path for assignment-rule audit-history retrieval.
     */
    public static final String PATH_AUDIT_HISTORY = "/{ruleId}/audit-history";

    /**
     * Relative REST path for tenant-level global schedule operations.
     */
    public static final String PATH_GLOBAL_SCHEDULE = "/global-schedule";

    /**
     * Tenant header expected on all tenant-scoped requests.
     */
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";

    // -------------------------------------------------------------------------
    // Defaults / system values
    // -------------------------------------------------------------------------

    /**
     * Fallback actor name used when no authenticated principal is available.
     */
    public static final String SYSTEM_TRIGGER = "System";

    /**
     * Default timezone used for CRON schedules when none is supplied.
     */
    public static final String DEFAULT_TIMEZONE = "UTC";

    /**
     * Default identity type for access-role rule creation.
     */
    public static final String DEFAULT_IDENTITY_TYPE = "EMPLOYEE";

    /**
     * Default logical operator within a condition group.
     */
    public static final String DEFAULT_GROUP_OPERATOR = "AND";

    /**
     * Default logical operator connecting sibling groups.
     */
    public static final String DEFAULT_INTER_GROUP_OPERATOR = "AND";

    /**
     * Prefix applied to generated rule names when the name is derived from role.
     */
    public static final String RULE_NAME_PREFIX = "Dynamic Role Rule - ";

    // -------------------------------------------------------------------------
    // Errors / validation messages
    // -------------------------------------------------------------------------

    public static final String ERROR_TENANT_REQUIRED = "Tenant ID is required";
    public static final String ERROR_IDENTITY_ID_REQUIRED = "Identity ID is required";
    public static final String ERROR_ROLE_ID_REQUIRED = "Role ID is required";
    public static final String ERROR_ROLE_NOT_FOUND = "Access role not found";
    public static final String ERROR_RULE_NOT_FOUND = "Access role assignment rule not found";
    public static final String ERROR_RULE_ALREADY_EXISTS = "Assignment rule already exists for role";
    public static final String ERROR_CONDITION_GROUPS_REQUIRED = "At least one condition group is required";
    public static final String ERROR_CONDITION_GROUP_EMPTY = "Condition group must contain at least one condition or nested condition group";
    public static final String ERROR_CONDITION_FIELD_REQUIRED = "Condition field is required";
    public static final String ERROR_CONDITION_OPERATOR_REQUIRED = "Condition operator is required";
    public static final String ERROR_INVALID_GROUP_OPERATOR = "Group operator must be AND or OR";
    public static final String ERROR_INVALID_SCHEDULE_TYPE = "Invalid schedule type";
    public static final String ERROR_CRON_SCHEDULE_REQUIRED = "CRON schedule requires cronExpression or scheduleTime";
    public static final String ERROR_INVALID_CRON_EXPRESSION = "Invalid cron expression";
    public static final String ERROR_INVALID_SCHEDULE_TIME = "Invalid schedule time. Expected HH:mm";
    public static final String ERROR_INVALID_SCHEDULE_DATE = "Invalid schedule date. Expected yyyy-MM-dd";
    public static final String ERROR_INVALID_TIMEZONE = "Invalid schedule timezone";
    public static final String ERROR_INVALID_IDENTITY_TYPE = "Invalid identity type";
    public static final String ERROR_IDENTITY_TYPES_REQUIRED = "At least one identity type is required";
    public static final String ERROR_EXPRESSION_REQUIRED = "Rule expression is required";
    public static final String PREVIEW_VALIDATION_SUCCESSFUL = "Rule Validation Successful";

    // -------------------------------------------------------------------------
    // Controller request / response logs
    // -------------------------------------------------------------------------

    /**
     * Logged when create-rule API is invoked.
     */
    public static final String LOG_CREATE_RULE_REQUEST = "Create access role assignment rule endpoint hit";

    /**
     * Logged when create-rule API completes successfully.
     */
    public static final String LOG_CREATE_RULE_RESPONSE = "Create access role assignment rule endpoint completed";

    /**
     * Logged when update-rule API is invoked.
     */
    public static final String LOG_UPDATE_RULE_REQUEST = "Update access role assignment rule endpoint hit";

    /**
     * Logged when update-rule API completes successfully.
     */
    public static final String LOG_UPDATE_RULE_RESPONSE = "Update access role assignment rule endpoint completed";

    /**
     * Logged when get-rule-by-id API is invoked.
     */
    public static final String LOG_GET_RULE_REQUEST = "Get access role assignment rule endpoint hit";

    /**
     * Logged when get-rule-by-id API completes successfully.
     */
    public static final String LOG_GET_RULE_RESPONSE = "Get access role assignment rule endpoint completed";

    /**
     * Logged when get-rule-by-role API is invoked.
     */
    public static final String LOG_GET_RULE_BY_ROLE_REQUEST = "Get access role assignment rule by role endpoint hit";

    /**
     * Logged when get-rule-by-role API returns a matching rule.
     */
    public static final String LOG_GET_RULE_BY_ROLE_RESPONSE = "Get access role assignment rule by role endpoint completed";

    /**
     * Logged when get-rule-by-role API returns no rule.
     */
    public static final String LOG_GET_RULE_BY_ROLE_NOT_FOUND = "No access role assignment rule found for role";

    /**
     * Logged when list-rules API is invoked.
     */
    public static final String LOG_LIST_RULES_REQUEST = "List access role assignment rules endpoint hit";

    /**
     * Logged when list-rules API completes successfully.
     */
    public static final String LOG_LIST_RULES_RESPONSE = "List access role assignment rules endpoint completed";

    /**
     * Logged when preview-rule API is invoked.
     */
    public static final String LOG_PREVIEW_RULE_REQUEST = "Preview access role assignment rule endpoint hit";

    /**
     * Logged when preview-rule API completes successfully.
     */
    public static final String LOG_PREVIEW_RULE_RESPONSE = "Preview access role assignment rule endpoint completed";

    /**
     * Logged when delete-rule API is invoked.
     */
    public static final String LOG_DELETE_RULE_REQUEST = "Delete access role assignment rule endpoint hit";

    /**
     * Logged when delete-rule API completes successfully.
     */
    public static final String LOG_DELETE_RULE_RESPONSE = "Delete access role assignment rule endpoint completed";

    /**
     * Logged when rule audit-history API is invoked.
     */
    public static final String LOG_GET_RULE_AUDIT_HISTORY_REQUEST = "Get access role assignment rule audit history endpoint hit";

    /**
     * Logged when rule audit-history API completes successfully.
     */
    public static final String LOG_GET_RULE_AUDIT_HISTORY_RESPONSE = "Get access role assignment rule audit history endpoint completed";

    /**
     * Logged when execute-single-rule API is invoked.
     */
    public static final String LOG_EXECUTE_RULE_REQUEST = "Execute access role assignment rule endpoint hit";

    /**
     * Logged when execute-single-rule API completes successfully.
     */
    public static final String LOG_EXECUTE_RULE_RESPONSE = "Execute access role assignment rule endpoint completed";

    /**
     * Logged when execute-all-rules API is invoked.
     */
    public static final String LOG_EXECUTE_ALL_RULES_REQUEST = "Execute all access role assignment rules endpoint hit";

    /**
     * Logged when execute-all-rules API completes successfully.
     */
    public static final String LOG_EXECUTE_ALL_RULES_RESPONSE = "Execute all access role assignment rules endpoint completed";

    /** Logged when get-global-schedule API is invoked. */
    public static final String LOG_GET_GLOBAL_SCHEDULE_REQUEST = "Get access role assignment rule global schedule endpoint hit";

    /** Logged when get-global-schedule API completes successfully. */
    public static final String LOG_GET_GLOBAL_SCHEDULE_RESPONSE = "Get access role assignment rule global schedule endpoint completed";

    /** Logged when update-global-schedule API is invoked. */
    public static final String LOG_UPDATE_GLOBAL_SCHEDULE_REQUEST = "Update access role assignment rule global schedule endpoint hit";

    /** Logged when update-global-schedule API completes successfully. */
    public static final String LOG_UPDATE_GLOBAL_SCHEDULE_RESPONSE = "Update access role assignment rule global schedule endpoint completed";

    // -------------------------------------------------------------------------
    // Service / domain logs
    // -------------------------------------------------------------------------

    public static final String LOG_RULE_CREATED = "Access role assignment rule created";
    public static final String LOG_RULE_UPDATED = "Access role assignment rule updated";
    public static final String LOG_RULE_DELETED = "Access role assignment rule deleted";
    public static final String LOG_RULE_FETCHED = "Access role assignment rule fetched";
    public static final String LOG_RULES_LISTED = "Access role assignment rules listed";
    public static final String LOG_SCAN_RULE_STARTED = "Access role assignment rule scan started";
    public static final String LOG_SCAN_RULE_COMPLETED = "Access role assignment rule scan completed";
    public static final String LOG_SCAN_ALL_STARTED = "Access role assignment full scan started";
    public static final String LOG_SCAN_ALL_COMPLETED = "Access role assignment full scan completed";
    public static final String LOG_SCAN_IMMEDIATE_STARTED = "Access role assignment immediate scan started";
    public static final String LOG_SCAN_IMMEDIATE_COMPLETED = "Access role assignment immediate scan completed";
    public static final String LOG_SCAN_CRON_STARTED = "Access role assignment cron scan started";
    public static final String LOG_SCAN_CRON_COMPLETED = "Access role assignment cron scan completed";
    public static final String LOG_ROLE_ASSIGNMENT_ACTIVE_REUSED = "Existing access role assignment reused";
    public static final String LOG_ROLE_ASSIGNMENT_REACTIVATED = "Access role assignment reactivated";
    public static final String LOG_ROLE_ASSIGNMENT_CREATED = "Access role assignment created";
    public static final String LOG_ROLE_ASSIGNMENT_REVOKED = "Access role assignment revoked";
    public static final String LOG_ROLE_ASSIGNMENT_REVOKE_SKIPPED = "Access role assignment revoke skipped because no matching assignment exists";
    public static final String LOG_ROLE_ENTITLEMENT_REVOKE_SKIPPED_ACTIVE_ASSIGNMENT = "Role-derived entitlement revoke skipped because another active assignment still grants the same role";
    public static final String LOG_ROLE_ENTITLEMENT_SYNC_STARTED = "Role-derived entitlement sync started";
    public static final String LOG_ROLE_ENTITLEMENT_SYNC_COMPLETED = "Role-derived entitlement sync completed";
    public static final String LOG_ROLE_ENTITLEMENT_SYNC_SKIPPED_NO_ENTITLEMENTS = "Role-derived entitlement sync skipped because the role has no effective entitlements";
    public static final String LOG_ROLE_ENTITLEMENT_SYNC_SKIPPED_NO_SHADOWS = "Role-derived entitlement sync skipped because the identity has no eligible linked account shadows";
    public static final String LOG_ROLE_ENTITLEMENT_REVOKE_STARTED = "Role-derived entitlement revoke started";
    public static final String LOG_ROLE_ENTITLEMENT_REVOKE_COMPLETED = "Role-derived entitlement revoke completed";
    public static final String LOG_ROLE_ENTITLEMENT_REVOKE_SKIPPED = "Role-derived entitlement revoke skipped because no matching role-derived assignments were found";
    public static final String LOG_RULE_PREVIEW_COMPLETED = "Access role assignment rule preview completed";
    public static final String LOG_GLOBAL_SCHEDULE_PERSISTED = "Access role assignment rule global schedule config persisted";
    public static final String LOG_GLOBAL_SCHEDULE_UPDATED = "Access role assignment rule global schedule applied to tenant rules";
    public static final String LOG_RULE_INHERITED_GLOBAL_SCHEDULE = "New access role assignment rule inherited tenant global schedule config";
    public static final String LOG_RULE_EVALUATION_QUEUED = "Access role assignment rule evaluation queued after commit";
    public static final String LOG_RULE_EVALUATION_TRIGGERED = "Access role assignment rule post-commit evaluation triggered";
    public static final String LOG_RULE_EVALUATION_FAILED = "Access role assignment rule post-commit evaluation failed";

    // -------------------------------------------------------------------------
    // Structured log detail keys
    // -------------------------------------------------------------------------

    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_RULE_ID = "ruleId";
    public static final String DETAIL_ROLE_ID = "roleId";
    public static final String DETAIL_IDENTITY_ID = "identityId";
    public static final String DETAIL_RULE_NAME = "ruleName";
    public static final String DETAIL_SCHEDULE_TYPE = "scheduleType";
    public static final String DETAIL_CRON_EXPRESSION = "cronExpression";
    public static final String DETAIL_SCHEDULE_TIME = "scheduleTime";
    public static final String DETAIL_SCHEDULE_DATE = "scheduleDate";
    public static final String DETAIL_SCHEDULE_TIMEZONE = "scheduleTimezone";
    public static final String DETAIL_GROUP_COUNT = "conditionGroupCount";
    public static final String DETAIL_TRIGGERED_BY = "triggeredBy";
    public static final String DETAIL_TRIGGER_TYPE = "triggerType";
    public static final String DETAIL_EXECUTION_ID = "executionId";
    public static final String DETAIL_EXECUTION_COUNT = "executionCount";
    public static final String DETAIL_EXECUTION_STATUS = "executionStatus";
    public static final String DETAIL_IDENTITIES_MATCHED = "identitiesMatched";
    public static final String DETAIL_ROLES_ASSIGNED = "rolesAssigned";
    public static final String DETAIL_ROLES_REACTIVATED = "rolesReactivated";
    public static final String DETAIL_ROLES_REVOKED = "rolesRevoked";
    public static final String DETAIL_SKIPPED_ALREADY_ASSIGNED = "skippedAlreadyAssigned";
    public static final String DETAIL_ERROR_COUNT = "errorCount";
    public static final String DETAIL_SOURCE_TYPE = "sourceType";
    public static final String DETAIL_SOURCE_KEY = "sourceKey";
    public static final String DETAIL_ENTITLEMENT_COUNT = "entitlementCount";
    public static final String DETAIL_SHADOW_COUNT = "shadowCount";
    public static final String DETAIL_CREATED_COUNT = "createdCount";
    public static final String DETAIL_EXISTING_COUNT = "existingCount";
    public static final String DETAIL_REMOVED_COUNT = "removedCount";
    public static final String DETAIL_ACTIVE_ROLE_COUNT = "activeRoleCount";
    public static final String DETAIL_PAGE = "page";
    public static final String DETAIL_SIZE = "size";
    public static final String DETAIL_SORT = "sort";
    public static final String DETAIL_ACTION = "action";
    public static final String DETAIL_SEARCH = "search";
    public static final String DETAIL_ENDPOINT = "endpoint";
    public static final String DETAIL_HTTP_METHOD = "httpMethod";
    public static final String DETAIL_STATUS_CODE = "statusCode";
    public static final String DETAIL_ROLE_FOUND = "roleFound";
    public static final String DETAIL_RESULT_COUNT = "resultCount";
    public static final String DETAIL_RULES_UPDATED = "rulesUpdated";
    public static final String DETAIL_NEXT_EXECUTION_PREVIEW = "nextExecutionPreview";
    public static final String DETAIL_RULE_COUNT = "ruleCount";
    public static final String DETAIL_ERROR = "error";
}