package com.aidentx.iam.access_roles_management.audit;

/**
 * Constants for access-role assignment rule audit events.
 *
 * <p>This class centralizes raw action keys, UI-friendly activity label templates,
 * structured log messages, JSON detail keys, state snapshot field names, and
 * metadata helpers used by the access-role assignment rule audit pipeline.</p>
 */
public final class AccessRoleAssignmentRuleAuditConstants {

    private AccessRoleAssignmentRuleAuditConstants() {
    }

    // ===================================================================
    //  Entity Types
    // ===================================================================

    public static final String ENTITY_TYPE = "Access Role Assignment Rule";

    // ===================================================================
    //  Actions
    // ===================================================================

    public static final String ACTION_RULE_CREATED = "ACCESS_ROLE_ASSIGNMENT_RULE_CREATED";
    public static final String ACTION_RULE_UPDATED = "ACCESS_ROLE_ASSIGNMENT_RULE_UPDATED";
    public static final String ACTION_RULE_DELETED = "ACCESS_ROLE_ASSIGNMENT_RULE_DELETED";

    // ===================================================================
    //  Activity Labels
    // ===================================================================

    public static final String ACTIVITY_RULE_CREATED_TEMPLATE = "%s Role Assignment Rule Created";
    public static final String ACTIVITY_RULE_UPDATED_TEMPLATE = "%s Role Assignment Rule Updated";
    public static final String ACTIVITY_RULE_DELETED_TEMPLATE = "%s Role Assignment Rule Deleted";

    // ===================================================================
    //  Descriptions
    // ===================================================================

    public static final String DESC_RULE_CREATED_TEMPLATE =
            "A %s role assignment rule \"%s\" was created for role \"%s\" by %s";
    public static final String DESC_RULE_UPDATED_TEMPLATE =
            "A %s role assignment rule \"%s\" was updated for role \"%s\" by %s";
    public static final String DESC_RULE_DELETED_TEMPLATE =
            "A %s role assignment rule \"%s\" was deleted for role \"%s\" by %s";

    // ===================================================================
    //  Trigger Source
    // ===================================================================

    public static final String TRIGGER_API = "API";

    // ===================================================================
    //  Logging
    // ===================================================================

    public static final String LOG_CATEGORY = "access-role-assignment-rule-audit";
    public static final String LOG_STATE_CAPTURE_FAILED = "Could not capture access-role assignment rule audit state";
    public static final String LOG_PUBLISHING_EVENT = "Publishing access-role assignment rule audit event to Kafka";
    public static final String LOG_EVENT_PUBLISHED = "Access-role assignment rule audit event published to Kafka successfully";
    public static final String LOG_EVENT_PUBLISH_FAILED = "Failed to publish access-role assignment rule audit event to Kafka";
    public static final String LOG_CREATE_RULE_AUDIT_FAILED = "Failed to publish access-role assignment rule create audit event";
    public static final String LOG_UPDATE_RULE_AUDIT_FAILED = "Failed to publish access-role assignment rule update audit event";
    public static final String LOG_DELETE_RULE_AUDIT_FAILED = "Failed to publish access-role assignment rule delete audit event";
    public static final String LOG_RULE_CONTEXT_RESOLVED = "Access-role assignment rule context resolved for audit";
    public static final String LOG_RULE_NOT_FOUND_FOR_AUDIT = "Access-role assignment rule not found for audit context";

    // ===================================================================
    //  Detail / Metadata Keys
    // ===================================================================

    public static final String DETAIL_ACTION = "action";
    public static final String DETAIL_RULE_ID = "ruleId";
    public static final String DETAIL_RULE_NAME = "ruleName";
    public static final String DETAIL_ROLE_ID = "roleId";
    public static final String DETAIL_ROLE_NAME = "roleName";
    public static final String DETAIL_ROLE_TYPE = "roleType";
    public static final String DETAIL_ROLE_STATUS = "roleStatus";
    public static final String DETAIL_RISK_LEVEL = "riskLevel";
    public static final String DETAIL_ENABLED = "enabled";
    public static final String DETAIL_SCHEDULE_TYPE = "scheduleType";
    public static final String DETAIL_PRIORITY = "priority";
    public static final String DETAIL_CONDITION_GROUP_COUNT = "conditionGroupCount";
    public static final String DETAIL_IDENTITY_TYPES = "identityTypes";
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";
    public static final String DETAIL_EXCEPTION_MESSAGE = "exceptionMessage";
    public static final String DETAIL_MESSAGE = "message";

    public static final String METADATA_SOURCE = "source";
    public static final String METADATA_SOURCE_VALUE = "access-role-assignment-rule-service";
    public static final String METADATA_TENANT_ID = "tenantId";
    public static final String METADATA_ENTITY_TYPE = "entityType";
    public static final String METADATA_RULE_ID = "ruleId";
    public static final String METADATA_RULE_NAME = "ruleName";
    public static final String METADATA_ROLE_ID = "roleId";
    public static final String METADATA_ROLE_NAME = "roleName";
    public static final String METADATA_ROLE_TYPE = "roleType";
    public static final String METADATA_RISK_LEVEL = "riskLevel";

    public static final String TARGET_TYPE = "type";
    public static final String TARGET_OBJECT_ID = "objectId";
    public static final String TARGET_OBJECT_NAME = "objectName";

    // ===================================================================
    //  State Snapshot Fields
    // ===================================================================

    public static final String STATE_RULE_ID = "ruleId";
    public static final String STATE_RULE_NAME = "ruleName";
    public static final String STATE_ROLE_ID = "roleId";
    public static final String STATE_ROLE_NAME = "roleName";
    public static final String STATE_ROLE_TYPE = "roleType";
    public static final String STATE_ROLE_STATUS = "roleStatus";
    public static final String STATE_ROLE_RISK_LEVEL = "roleRiskLevel";
    public static final String STATE_ENABLED = "enabled";
    public static final String STATE_SCHEDULE_TYPE = "scheduleType";
    public static final String STATE_CRON_EXPRESSION = "cronExpression";
    public static final String STATE_SCHEDULE_TIME = "scheduleTime";
    public static final String STATE_SCHEDULE_DATE = "scheduleDate";
    public static final String STATE_SCHEDULE_TIMEZONE = "scheduleTimezone";
    public static final String STATE_LAST_EVALUATED_AT = "lastEvaluatedAt";
    public static final String STATE_NEXT_EXECUTION_AT = "nextExecutionAt";
    public static final String STATE_PRIORITY = "priority";
    public static final String STATE_IDENTITY_TYPES = "identityTypes";
    public static final String STATE_CONDITION_GROUPS = "conditionGroups";
    public static final String STATE_CREATED_BY = "createdBy";
    public static final String STATE_CREATED_AT = "createdAt";
    public static final String STATE_UPDATED_AT = "updatedAt";

    public static final String NOT_AVAILABLE = "N/A";
    public static final String UNKNOWN = "Unknown";

    /**
     * Capitalizes a raw role type for user-facing labels.
     *
     * @param rawType raw role type from persistence
     * @return capitalized role type, or {@code Access} when absent
     */
    public static String capitalizeRoleType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "Access";
        }
        return rawType.substring(0, 1).toUpperCase() + rawType.substring(1).toLowerCase();
    }

    /**
     * Converts the raw role type to the audit entity type shown in Kafka/UI.
     *
     * @param roleType raw role type
     * @return display entity type such as {@code Business Role Assignment Rule}
     */
    public static String toEntityType(String roleType) {
        String displayType = capitalizeRoleType(roleType);
        if ("Access".equals(displayType)) {
            return ENTITY_TYPE;
        }
        return displayType + " Role Assignment Rule";
    }

    /**
     * Resolves the UI-friendly activity label from the raw action and role type.
     *
     * @param action raw action constant
     * @param roleType raw role type
     * @return user-friendly activity label
     */
    public static String toActivityLabel(String action, String roleType) {
        if (action == null) {
            return UNKNOWN;
        }
        String displayType = capitalizeRoleType(roleType);
        return switch (action) {
            case ACTION_RULE_CREATED -> String.format(ACTIVITY_RULE_CREATED_TEMPLATE, displayType);
            case ACTION_RULE_UPDATED -> String.format(ACTIVITY_RULE_UPDATED_TEMPLATE, displayType);
            case ACTION_RULE_DELETED -> String.format(ACTIVITY_RULE_DELETED_TEMPLATE, displayType);
            default -> action;
        };
    }
}

