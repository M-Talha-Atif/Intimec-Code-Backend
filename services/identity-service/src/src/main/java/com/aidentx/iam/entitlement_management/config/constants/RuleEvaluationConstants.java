package com.aidentx.iam.entitlement_management.config.constants;

/**
 * Constants for Dynamic Entitlement Assignment Rule Evaluation.
 *
 * <p>Centralizes all log messages, detail keys, configuration values,
 * and error messages used by the rule evaluation engine and related services.
 */
public final class RuleEvaluationConstants {

    private RuleEvaluationConstants() {
    }

    // System
    public static final String SYSTEM = "System";
    // Events for rule updated and rule created
    public static final String EVENT_RULE_CREATED = "IMMEDIATE_CREATE";
    public static final String EVENT_RULE_UPDATED = "IMMEDIATE_UPDATE";


    // ===================================================================
    //  Logger Module
    // ===================================================================

    public static final String LOGGER_MODULE_NAME = "RuleEvaluationEngine";
    public static final String LOGGER_MODULE_SERVICE = "AssignmentRuleService";
    public static final String LOGGER_MODULE_HISTORY = "ExecutionHistoryWriter";

    // ===================================================================
    //  Entitlement Audit Log  DB Persistence from Engine
    // ===================================================================

    public static final String LOG_AUDIT_DB_PERSISTED = "Entitlement audit log persisted for rule evaluation";
    public static final String LOG_AUDIT_DB_PERSIST_FAILED = "Failed to persist entitlement audit log for rule evaluation";

    public static final String AUDIT_DESC_RULE_EVALUATED_FORMAT =
            "Rule \"%s\" evaluated for entitlement \"%s\"  matched %d, created %d, revoked %d";

    public static final String DETAIL_TRIGGER_SOURCE = "triggerSource";


    // ===================================================================
    //  Configuration Defaults
    // ===================================================================

    public static final int IDENTITY_CHUNK_SIZE = 500;
    public static final int BATCH_INSERT_SIZE = 100;
    public static final String SOURCE_DYNAMIC = "DYNAMIC";
    public static final String SHADOW_KIND_ACCOUNT = "account";
    public static final String DEFAULT_LOGICAL_OPERATOR = "AND";
    public static final String DEFAULT_TIMEZONE = "UTC";
    public static final String SYSTEM_TRIGGER = "SYSTEM";
    public static final String DEFAULT_IDENTITY_TYPE = "EMPLOYEE";

    // ===================================================================
    //  Detail Keys
    // ===================================================================

    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_RULE_ID = "ruleId";
    public static final String DETAIL_RULE_NAME = "ruleName";
    public static final String DETAIL_RULE_COUNT = "ruleCount";
    public static final String DETAIL_SCHEDULE_TYPE = "scheduleType";
    public static final String DETAIL_CRON_EXPRESSION = "cronExpression";
    public static final String DETAIL_SCHEDULE_TIMEZONE = "scheduleTimezone";
    public static final String DETAIL_IDENTITY_TYPE = "identityType";
    public static final String DETAIL_IDENTITY_TYPES = "identityTypes";
    public static final String DETAIL_IDENTITY_CLASS = "identityClass";
    public static final String DETAIL_MATCHED_COUNT = "matchedCount";
    public static final String DETAIL_CHUNK_INDEX = "chunkIndex";
    public static final String DETAIL_CHUNK_SIZE = "chunkSize";
    public static final String DETAIL_SHADOW_COUNT = "shadowCount";
    public static final String DETAIL_EXISTING_ASSIGNMENT_COUNT = "existingAssignmentCount";
    public static final String DETAIL_CREATED_COUNT = "createdCount";
    public static final String DETAIL_REVOKED_COUNT = "revokedCount";
    public static final String DETAIL_SKIPPED_NO_SHADOW = "skippedNoShadow";
    public static final String DETAIL_ENTITLEMENT_ID = "entitlementId";
    public static final String DETAIL_RESOURCE_ID = "resourceId";
    public static final String DETAIL_IDENTITY_ID = "identityId";
    public static final String DETAIL_SHADOW_ID = "shadowId";
    public static final String DETAIL_FIELD = "field";
    public static final String DETAIL_ALLOWED_FIELDS = "allowedFields";
    public static final String DETAIL_OPERATOR = "operator";
    public static final String DETAIL_ERROR = "error";
    public static final String DETAIL_DURATION_MS = "durationMs";
    public static final String DETAIL_PAGE = "page";
    public static final String DETAIL_TOTAL_PAGES = "totalPages";
    public static final String DETAIL_EXECUTION_ID = "executionId";
    public static final String DETAIL_TRIGGER_TYPE = "triggerType";

    // ===================================================================
    //  Log Messages
    // ===================================================================

    public static final String LOG_EVALUATE_ALL_START = "Starting evaluation of all enabled rules";
    public static final String LOG_EVALUATE_ALL_COMPLETE = "Completed evaluation of all enabled rules";
    public static final String LOG_EVALUATE_IMMEDIATE_START = "Starting evaluation of IMMEDIATE rules";
    public static final String LOG_EVALUATE_CRON_START = "Starting evaluation of CRON rules";
    public static final String LOG_EVALUATE_RULE_START = "Starting evaluation of rule";
    public static final String LOG_EVALUATE_RULE_COMPLETE = "Completed evaluation of rule";
    public static final String LOG_EVALUATE_RULE_FAILED = "Rule evaluation failed";
    public static final String LOG_IDENTITY_QUERY_START = "Querying identities matching rule conditions";
    public static final String LOG_IDENTITY_QUERY_COMPLETE = "Identity query complete";
    public static final String LOG_IDENTITY_CHUNK_PROCESSING = "Processing identity chunk";
    public static final String LOG_SHADOW_BULK_LOOKUP = "Bulk loading shadows for identity chunk";
    public static final String LOG_SHADOW_BULK_LOOKUP_COMPLETE = "Shadow bulk lookup complete";
    public static final String LOG_ASSIGNMENT_CHECK_EXISTING = "Checking existing assignments for chunk";
    public static final String LOG_ASSIGNMENT_BULK_INSERT = "Bulk inserting new dynamic assignments";
    public static final String LOG_ASSIGNMENT_BULK_INSERT_COMPLETE = "Bulk insert complete";
    public static final String LOG_REVOCATION_START = "Starting revocation of stale dynamic assignments";
    public static final String LOG_REVOCATION_COMPLETE = "Revocation complete";
    public static final String LOG_FIELD_VALIDATION_FAILED = "Field validation failed for rule condition";
    public static final String LOG_EXECUTION_HISTORY_PERSISTED = "Execution history persisted";
    public static final String LOG_EXECUTION_HISTORY_FAILED = "Failed to persist execution history - evaluation result is NOT affected";

    // ===================================================================
    //  Error Messages
    // ===================================================================

    public static final String ERROR_TENANT_ID_BLANK = "Tenant ID must not be null or blank";
    public static final String ERROR_RULE_NOT_FOUND = "Assignment rule not found";
    public static final String ERROR_ENTITLEMENT_NOT_FOUND = "Entitlement not found";
    public static final String ERROR_ENTITLEMENT_TENANT_MISMATCH = "Entitlement does not belong to this tenant";
    public static final String ERROR_CONDITIONS_EMPTY = "At least one condition is required";
    public static final String ERROR_RULE_ALREADY_EXISTS = "A rule already exists for this entitlement. Each entitlement can have only one rule";
    public static final String ERROR_FIELD_NOT_ALLOWED = "Field is not allowed for this identity type";
    public static final String ERROR_FIELD_UNRESOLVED = "Field could not be resolved to a known identity attribute";
    public static final String ERROR_OPERATOR_BLANK = "Operator cannot be null or blank";
    public static final String ERROR_INVALID_LOGICAL_OPERATOR = "logicalOperator must be AND or OR";
    public static final String ERROR_IDENTITY_TYPES_REQUIRED = "At least one identity type is required";
    public static final String ERROR_INVALID_IDENTITY_TYPE = "Invalid identity type. Allowed values: EMPLOYEE, NON_EMPLOYEE, NON_HUMAN";
    /**
     * Error message when schedule type is invalid.
     */
    public static final String ERROR_INVALID_SCHEDULE_TYPE =
            "Invalid schedule type. Allowed values: IMMEDIATE, CRON, MANUAL";
    public static final String ERROR_CRON_SCHEDULE_REQUIRED = "cronExpression or scheduleTime is required when scheduleType is CRON";
    public static final String ERROR_INVALID_CRON_EXPRESSION = "Invalid cron expression";
    public static final String ERROR_INVALID_SCHEDULE_TIME = "scheduleTime must be in HH:mm format (e.g., 02:00, 14:30)";
    public static final String ERROR_INVALID_SCHEDULE_DATE = "scheduleDate must be in yyyy-MM-dd format (e.g., 2026-05-01)";
    public static final String ERROR_INVALID_TIMEZONE = "Invalid timezone. Use IANA format (e.g., America/New_York, Asia/Karachi, UTC)";
    public static final String ERROR_EXECUTION_NOT_FOUND = "Execution history record not found";

    public static final String SCHEDULE_TIME_PATTERN = "^([01]\\d|2[0-3]):[0-5]\\d$";
    public static final String SCHEDULE_DATE_PATTERN = "^\\d{4}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])$";
    public static final char LIKE_ESCAPE_CHAR = '\\';

    // ===================================================================
    //  Condition Group Constants
    // ===================================================================

    /**
     * Default group operator when not specified.
     */
    public static final String DEFAULT_GROUP_OPERATOR = "AND";

    /**
     * Maximum number of condition groups per rule.
     */
    public static final int MAX_CONDITION_GROUPS = 10;

    /**
     * Maximum number of conditions per group.
     */
    public static final int MAX_CONDITIONS_PER_GROUP = 10;

    // ===================================================================
    //  Condition Group Error Messages
    // ===================================================================

    /**
     * Error when condition groups are empty.
     */
    public static final String ERROR_CONDITION_GROUPS_EMPTY =
            "At least one condition group with at least one condition is required";

    /**
     * Error when a group has no conditions.
     */
    public static final String ERROR_GROUP_CONDITIONS_EMPTY =
            "Each condition group must contain at least one condition";

    /**
     * Error when group operator is invalid.
     */
    public static final String ERROR_INVALID_GROUP_OPERATOR =
            "Invalid group operator. Allowed values: AND, OR";

    /**
     * Error when too many groups are provided.
     */
    public static final String ERROR_TOO_MANY_GROUPS =
            "Maximum " + MAX_CONDITION_GROUPS + " condition groups allowed per rule";

    /**
     * Alias for ERROR_TENANT_ID_BLANK used by the service layer.
     */
    public static final String ERROR_TENANT_ID_REQUIRED = ERROR_TENANT_ID_BLANK;

    /**
     * Error when too many conditions in a group.
     */
    public static final String ERROR_TOO_MANY_CONDITIONS_IN_GROUP =
            "Maximum " + MAX_CONDITIONS_PER_GROUP + " conditions allowed per group";


    /**
     * Detail key for the number of rules updated in a global schedule change.
     */
    public static final String DETAIL_RULES_UPDATED = "rulesUpdated";

    // ===================================================================
    //  Condition Group Log Details
    // ===================================================================

    public static final String DETAIL_GROUP_COUNT = "groupCount";
    public static final String DETAIL_GROUP_INDEX = "groupIndex";
    public static final String DETAIL_GROUP_OPERATOR = "groupOperator";
    public static final String DETAIL_GROUP_NEGATED = "groupNegated";

    /**
     * Maximum allowed nesting depth for condition groups.
     */
    public static final int MAX_GROUP_DEPTH = 5;

    /**
     * Error message when group nesting exceeds maximum depth.
     */
    public static final String ERROR_GROUP_DEPTH_EXCEEDED =
            "Condition group nesting depth exceeds maximum allowed depth of " + MAX_GROUP_DEPTH;

    /**
     * Error message when a group has neither conditions nor sub-groups.
     */
    public static final String ERROR_GROUP_EMPTY =
            "A condition group must have at least one condition or one sub-group.";

    /**
     * Default inter-group operator when not specified.
     */
    public static final String DEFAULT_INTER_GROUP_OPERATOR = "AND";

    // Execution ID fallback
    public static final String EXECUTION_ID_NOT_PERSISTED = "not-persisted";

    // Audit publish failure
    public static final String LOG_AUDIT_PUBLISH_FAILED = "Failed to publish evaluation audit event  evaluation result is NOT affected";

    // Entitlement name fallbacks
    public static final String ENTITLEMENT_UNKNOWN = "UNKNOWN";
    public static final String ENTITLEMENT_UNKNOWN_PREFIX = "Unknown (ID: ";
    public static final String ENTITLEMENT_UNRESOLVABLE_PREFIX = "Unresolvable (ID: ";

    // ===================================================================
    //  Native Queries
    // ===================================================================

    public static final String NATIVE_UPSERT_ASSIGNMENT =
            "INSERT INTO entitlement_assignments (id, tenant_id, shadow_id, identity_id, entitlement_id, resource_id, source, status, rule_id, assigned_at, updated_at) " +
                    "VALUES (:id, :tenantId, :shadowId, :identityId, :entitlementId, :resourceId, :source, 'ASSIGNED', :ruleId, :assignedAt, :updatedAt) " +
                    "ON CONFLICT (shadow_id, entitlement_id, tenant_id, source) WHERE shadow_id IS NOT NULL DO NOTHING";

    public static final String NATIVE_UPSERT_PENDING_ASSIGNMENT =
            "INSERT INTO entitlement_assignments (id, tenant_id, shadow_id, identity_id, entitlement_id, resource_id, source, status, rule_id, assigned_at, updated_at) " +
                    "VALUES (:id, :tenantId, NULL, :identityId, :entitlementId, :resourceId, :source, 'PENDING', :ruleId, :assignedAt, :updatedAt) " +
                    "ON CONFLICT (identity_id, entitlement_id, tenant_id, source) WHERE shadow_id IS NULL AND identity_id IS NOT NULL DO NOTHING";

    public static final String NATIVE_DELETE_PENDING_ASSIGNMENT =
            "DELETE FROM entitlement_assignments ea " +
                    "WHERE ea.tenant_id = :tenantId " +
                    "AND ea.rule_id = :ruleId " +
                    "AND ea.source = 'DYNAMIC' " +
                    "AND ea.shadow_id IS NULL " +
                    "AND ea.identity_id = :identityId " +
                    "AND ea.entitlement_id = :entitlementId";

    public static final String NATIVE_BULK_REVOKE =
            "DELETE FROM entitlement_assignments ea " +
                    "WHERE ea.tenant_id = :tenantId " +
                    "AND ea.entitlement_id = :entitlementId " +
                    "AND ea.source = 'DYNAMIC' " +
                    "AND NOT (" +
                    "  ea.identity_id IN (:matchedIdentityIds) " +
                    "  OR EXISTS (" +
                    "    SELECT 1 FROM shadows s " +
                    "    WHERE s.id = ea.shadow_id " +
                    "    AND s.tenant_id = :tenantId " +
                    "    AND s.linked_identity_id IN (:matchedIdentityIds)" +
                    "  )" +
                    ")";

    public static final String NATIVE_REVOKE_ALL_FOR_ENTITLEMENT =
            "DELETE FROM entitlement_assignments ea " +
                    "WHERE ea.tenant_id = :tenantId " +
                    "AND ea.entitlement_id = :entitlementId " +
                    "AND ea.source = 'DYNAMIC'";
}