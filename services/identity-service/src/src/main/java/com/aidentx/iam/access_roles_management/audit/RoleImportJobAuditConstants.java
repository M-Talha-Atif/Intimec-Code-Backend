package com.aidentx.iam.access_roles_management.audit;

/** Constants for access-role import job completion audit events. */
public final class RoleImportJobAuditConstants {

    private RoleImportJobAuditConstants() {
    }

    public static final String ENTITY_TYPE = "Access Role Import Job";
    public static final String ACTION_ROLE_IMPORT_JOB_FINISHED = "ROLE_IMPORT_JOB_FINISHED";
    public static final String ACTIVITY_ROLE_IMPORT_JOB_FINISHED = "Access Role Import Finished";

    public static final String LOG_CATEGORY = "access-role-import-audit";
    public static final String LOG_PUBLISHING_EVENT = "Publishing access role import completion audit event to Kafka";
    public static final String LOG_EVENT_PUBLISHED = "Access role import completion audit event published to Kafka successfully";
    public static final String LOG_EVENT_PUBLISH_FAILED = "Failed to publish access role import completion audit event to Kafka";

    public static final String METADATA_SOURCE = "source";
    public static final String METADATA_SOURCE_VALUE = "access-role-import-service";
    public static final String METADATA_TENANT_ID = "tenantId";
    public static final String METADATA_ENTITY_TYPE = "entityType";

    public static final String DETAIL_ACTION = "action";
    public static final String DETAIL_JOB_ID = "jobId";
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_STATUS = "status";
    public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";

    public static final String FIELD_JOB_ID = "jobId";
    public static final String FIELD_TENANT_ID = "tenantId";
    public static final String FIELD_ACTOR_ID = "actorId";
    public static final String FIELD_ACTOR_USERNAME = "actorUsername";
    public static final String FIELD_FORMAT = "format";
    public static final String FIELD_STATUS = "status";
    public static final String FIELD_RISK_LEVEL = "riskLevel";
    public static final String FIELD_ERROR_MESSAGE = "errorMessage";
    public static final String FIELD_COUNTS = "counts";
    public static final String FIELD_TOTAL = "total";
    public static final String FIELD_SUCCEEDED = "succeeded";
    public static final String FIELD_FAILED = "failed";
    public static final String FIELD_SKIPPED = "skipped";
    public static final String FIELD_ASSIGNMENTS = "assignments";
    public static final String FIELD_ATTEMPTED = "attempted";
    public static final String FIELD_CREATED_ROLES = "createdRoles";
    public static final String FIELD_FAILED_ROWS = "failedRows";
    public static final String FIELD_SUCCEEDED_ASSIGNMENTS = "succeededAssignments";
    public static final String FIELD_FAILED_ASSIGNMENTS = "failedAssignments";
    public static final String FIELD_ROW_NUMBER = "rowNumber";
    public static final String FIELD_EXTERNAL_REF = "externalRef";
    public static final String FIELD_ROLE_ID = "roleId";
    public static final String FIELD_ROLE_NAME = "roleName";
    public static final String FIELD_ROLE_TYPE = "roleType";
    public static final String FIELD_IDENTITY_ID = "identityId";
    public static final String FIELD_ERROR_CODE = "errorCode";
    public static final String FIELD_REASON = "reason";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_STARTED_AT = "startedAt";
    public static final String FIELD_COMPLETED_AT = "completedAt";
    public static final String FIELD_DURATION_MS = "durationMs";

    public static final String TARGET_TYPE = "type";
    public static final String TARGET_OBJECT_ID = "objectId";
    public static final String TARGET_OBJECT_NAME = "objectName";
}
