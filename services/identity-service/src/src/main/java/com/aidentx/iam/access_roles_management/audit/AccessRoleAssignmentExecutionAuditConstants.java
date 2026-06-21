package com.aidentx.iam.access_roles_management.audit;

/**
 * Constants for access-role assignment execution audit events.
 */
public final class AccessRoleAssignmentExecutionAuditConstants {

    private AccessRoleAssignmentExecutionAuditConstants() {
    }

    public static final String ACTION_RULE_EXECUTED = "ACCESS_ROLE_ASSIGNMENT_RULE_EXECUTED";
    public static final String ACTION_RULES_EXECUTED = "ACCESS_ROLE_ASSIGNMENT_RULES_EXECUTED";

    public static final String ENTITY_TYPE_SINGLE = "Access Role Assignment Rule";
    public static final String ENTITY_TYPE_ALL = "Access Role Assignment Rules";

    public static final String ACTIVITY_RULE_EXECUTED_TEMPLATE = "%s Role Assignment Rule Evaluated";
    public static final String ACTIVITY_RULES_EXECUTED = "Access Role Assignment Rules Evaluated";

    public static final String DESC_RULE_EXECUTED_TEMPLATE =
            "A %s role assignment rule \"%s\" was evaluated for role \"%s\" by %s";
    public static final String DESC_RULES_EXECUTED_TEMPLATE =
            "All access role assignment rules were evaluated by %s";

    public static final String LOG_CATEGORY = "access-role-assignment-execution-audit";
    public static final String LOG_PUBLISHING_EVENT = "Publishing access-role assignment execution audit event to Kafka";
    public static final String LOG_EVENT_PUBLISHED = "Access-role assignment execution audit event published to Kafka successfully";
    public static final String LOG_EVENT_PUBLISH_FAILED = "Failed to publish access-role assignment execution audit event to Kafka";
    public static final String LOG_EXECUTE_RULE_AUDIT_FAILED = "Failed to publish access-role assignment rule execution audit event";
    public static final String LOG_EXECUTE_ALL_AUDIT_FAILED = "Failed to publish access-role assignment rule execute-all audit event";

    public static final String DETAIL_ACTION = "action";
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_RULE_ID = "ruleId";
    public static final String DETAIL_RULE_NAME = "ruleName";
    public static final String DETAIL_ROLE_ID = "roleId";
    public static final String DETAIL_ROLE_NAME = "roleName";
    public static final String DETAIL_ROLE_TYPE = "roleType";
    public static final String DETAIL_ROLE_STATUS = "roleStatus";
    public static final String DETAIL_RISK_LEVEL = "riskLevel";
    public static final String DETAIL_TRIGGER_TYPE = "triggerType";
    public static final String DETAIL_TRIGGERED_BY = "triggeredBy";
    public static final String DETAIL_EXECUTION_ID = "executionId";
    public static final String DETAIL_EXECUTION_STATUS = "executionStatus";
    public static final String DETAIL_EXECUTION_COUNT = "executionCount";
    public static final String DETAIL_IDENTITIES_MATCHED = "identitiesMatched";
    public static final String DETAIL_ROLES_ASSIGNED = "rolesAssigned";
    public static final String DETAIL_ROLES_REACTIVATED = "rolesReactivated";
    public static final String DETAIL_ROLES_REVOKED = "rolesRevoked";
    public static final String DETAIL_SKIPPED_ALREADY_ASSIGNED = "skippedAlreadyAssigned";
    public static final String DETAIL_SKIPPED_NOT_MATCHED = "skippedNotMatched";
    public static final String DETAIL_ERROR_COUNT = "errorCount";
    public static final String DETAIL_HIGHEST_RISK_LEVEL = "highestRiskLevel";
    public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";

    public static final String METADATA_SOURCE = "source";
    public static final String METADATA_SOURCE_VALUE = "access-role-assignment-execution-service";
    public static final String METADATA_ENTITY_TYPE = "entityType";
    public static final String METADATA_RISK_LEVEL = "riskLevel";

    public static final String TARGET_TYPE = "type";
    public static final String TARGET_OBJECT_ID = "objectId";
    public static final String TARGET_OBJECT_NAME = "objectName";

    public static final String TRIGGER_EXECUTION = "EXECUTION";
    public static final String UNKNOWN = "Unknown";
    public static final String NOT_AVAILABLE = "N/A";

    public static String capitalizeRoleType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "Access";
        }
        return rawType.substring(0, 1).toUpperCase() + rawType.substring(1).toLowerCase();
    }

    public static String toSingleActivityLabel(String roleType) {
        return String.format(ACTIVITY_RULE_EXECUTED_TEMPLATE, capitalizeRoleType(roleType));
    }
}

