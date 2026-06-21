package com.aidentx.iam.access_roles_management.audit;

/**
 * Constants for persistent access role audit log storage and history mapping.
 *
 * <p>This class centralizes table-related constants, JSON field names,
 * changed-field labels, log messages, and fallback values used by the
 * role audit log persistence layer.</p>
 */
public final class AccessRoleAuditLogConstants {

    private AccessRoleAuditLogConstants() {
    }

    // ===================================================================
    //  Table / Index Names
    // ===================================================================

    public static final String TABLE_ACCESS_ROLE_AUDIT_LOG = "access_role_audit_log";

    public static final String INDEX_TENANT_ROLE_CREATED =
            "idx_aral_tenant_role_created";

    public static final String INDEX_TENANT_RULE_CREATED =
            "idx_aral_tenant_rule_created";

    public static final String INDEX_TENANT_ACTION_CREATED =
            "idx_aral_tenant_action_created";

    public static final String INDEX_TENANT_ACTOR_CREATED =
            "idx_aral_tenant_actor_created";

    public static final String INDEX_TENANT_ROLETYPE_CREATED =
            "idx_aral_tenant_roletype_created";

    // ===================================================================
    //  JSON / Changed Field Names
    // ===================================================================

    public static final String FIELD_ROLE = "role";
    public static final String FIELD_ENTITLEMENTS = "entitlements";
    public static final String FIELD_CHILD_ROLES = "childRoles";

    public static final String JSON_ADDED_CHILDREN = "addedChildren";
    public static final String JSON_REMOVED_CHILDREN = "removedChildren";
    public static final String JSON_FAILED_CHILDREN = "failedChildren";
    public static final String JSON_ADDED_ENTITLEMENTS = "addedEntitlements";
    public static final String JSON_REMOVED_ENTITLEMENTS = "removedEntitlements";

    public static final String JSON_ID = "id";
    public static final String JSON_NAME = "name";
    public static final String JSON_TYPE = "type";
    public static final String JSON_RISK_LEVEL = "riskLevel";
    public static final String JSON_REASON = "reason";

    // ===================================================================
    //  Log Category / Messages
    // ===================================================================

    public static final String LOG_CATEGORY = "access-role-audit-log";

    public static final String LOG_PERSISTING_AUDIT_LOG =
            "Persisting access role audit log entry";

    public static final String LOG_AUDIT_LOG_PERSISTED =
            "Access role audit log entry persisted successfully";

    public static final String LOG_AUDIT_LOG_PERSIST_FAILED =
            "Failed to persist access role audit log entry";

    public static final String LOG_QUERY_AUDIT_HISTORY =
            "Querying access role audit history";

    public static final String LOG_QUERY_AUDIT_HISTORY_DONE =
            "Access role audit history query completed";

    public static final String LOG_QUERY_AUDIT_HISTORY_FAILED =
            "Failed to query access role audit history";

    public static final String LOG_COMPUTED_CHANGED_FIELDS =
            "Computed changed fields for access role audit log";

    // ===================================================================
    //  Fallback Values
    // ===================================================================

    public static final String UNKNOWN_ROLE_NAME = "Unknown";
    public static final String UNKNOWN_ROLE_TYPE = "unknown";
    public static final String SYSTEM = "SYSTEM";
}