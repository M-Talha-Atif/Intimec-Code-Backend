package com.aidentx.iam.access_roles_management.config.constants;

/**
 * Constants for asynchronous access-role import jobs.
 */
public final class AccessRoleImportConstants {

    private AccessRoleImportConstants() {
    }

    public static final String PATH_IMPORT = "/import";
    public static final String PATH_IMPORT_JOB = "/import/{jobId}";
    public static final String PATH_IMPORT_JOB_ITEMS = "/import/{jobId}/items";
    public static final String PATH_IMPORT_JOB_ERRORS_CSV = "/import/{jobId}/errors.csv";

    public static final String PARAM_FORMAT = "format";
    public static final String PARAM_DRY_RUN = "dryRun";
    public static final String PARAM_STATUS = "status";
    public static final String PART_FILE = "file";
    public static final String HEADER_USER_ID = "X-User-ID";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_LEGACY_USER = "X-User";

    public static final String FORMAT_JSON = "json";
    public static final String FORMAT_CSV = "csv";

    public static final String TOPIC_DEFAULT = "role-import-events";
    public static final String CONSUMER_GROUP_DEFAULT = "identity-service-role-import";
    public static final String CONFIG_TOPIC = "access-roles.import.topic";
    public static final String CONFIG_MAX_ROWS = "access-roles.import.max-rows";
    public static final String CONFIG_BATCH_SIZE = "access-roles.import.batch-size";

    public static final int MAX_ROWS_DEFAULT = 100_000;
    public static final int BATCH_SIZE_DEFAULT = 100;

    public static final String LOG_CATEGORY = "access-role-import";
    public static final String LOG_SUBMIT = "Submitting access-role import job";
    public static final String LOG_SUBMIT_ACCEPTED = "Access-role import job accepted";
    public static final String LOG_JOB_FETCH = "Fetching access-role import job";
    public static final String LOG_ITEMS_FETCH = "Fetching access-role import job items";
    public static final String LOG_ERRORS_DOWNLOAD = "Downloading access-role import errors CSV";
    public static final String LOG_PARSE_COMPLETE = "Access-role import file parsed";
    public static final String LOG_VALIDATE_COMPLETE = "Access-role import rows validated";
    public static final String LOG_ITEMS_PERSISTED = "Access-role import job items persisted";
    public static final String LOG_EVENT_AFTER_COMMIT = "Scheduling access-role import event after transaction commit";
    public static final String LOG_PROCESS = "Processing access-role import job";
    public static final String LOG_PUBLISH = "Publishing access-role import job event";
    public static final String LOG_CONSUME = "Consumed access-role import job event";
    public static final String LOG_ACK = "Acknowledged access-role import job event";
    public static final String LOG_CREATE_PHASE_START = "Starting access-role import create phase";
    public static final String LOG_CREATE_BATCH = "Processing access-role import create batch";
    public static final String LOG_CREATE_ITEM_FAILED = "Access-role import row failed during create phase";
    public static final String LOG_WIRE_PHASE_START = "Starting access-role import child-reference wiring phase";
    public static final String LOG_WIRE_REF_MAP_BUILT = "Built access-role import external reference map";
    public static final String LOG_WIRE_BATCH = "Processing access-role import wire batch";
    public static final String LOG_WIRE_ITEM_FAILED = "Access-role import row failed during wire phase";
    public static final String LOG_ASSIGN_PHASE_START = "Starting access-role import identity assignment phase";
    public static final String LOG_ASSIGN_BATCH = "Processing access-role import identity assignment batch";
    public static final String LOG_ASSIGN_ITEM_FAILED = "Access-role import row failed during identity assignment phase";
    public static final String LOG_JOB_RUNNING = "Access-role import job marked running";
    public static final String LOG_JOB_FINISHED = "Access-role import job finished";
    public static final String LOG_JOB_FAILED = "Access-role import job failed";

    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_ACTOR_ID = "actorId";
    public static final String DETAIL_ACTOR_USERNAME = "actorUsername";
    public static final String DETAIL_JOB_ID = "jobId";
    public static final String DETAIL_ITEM_ID = "itemId";
    public static final String DETAIL_ROW_NUMBER = "rowNumber";
    public static final String DETAIL_FORMAT = "format";
    public static final String DETAIL_STATUS = "status";
    public static final String DETAIL_PHASE = "phase";
    public static final String DETAIL_ROW_COUNT = "rowCount";
    public static final String DETAIL_BATCH_SIZE = "batchSize";
    public static final String DETAIL_BATCH_COUNT = "batchCount";
    public static final String DETAIL_FILE_NAME = "fileName";
    public static final String DETAIL_FILE_SIZE = "fileSize";
    public static final String DETAIL_ERROR_CODE = "errorCode";
    public static final String DETAIL_ERROR_MESSAGE = "errorMessage";
    public static final String DETAIL_REF_COUNT = "refCount";
    public static final String DETAIL_SUCCEEDED_COUNT = "succeededCount";
    public static final String DETAIL_FAILED_COUNT = "failedCount";
    public static final String DETAIL_SKIPPED_COUNT = "skippedCount";

    public static final String RESPONSE_JOB_ACCEPTED = "Access role import job accepted";
    public static final String RESPONSE_JOB_FETCHED = "Access role import job fetched";
    public static final String RESPONSE_JOB_ITEMS_FETCHED = "Access role import job items fetched";

    public static final String ERROR_UNSUPPORTED_FORMAT = "Unsupported import format: %s";
    public static final String ERROR_EMPTY_FILE = "Import file is empty";
    public static final String ERROR_TOO_MANY_ROWS = "Import contains %d rows; maximum allowed is %d";
    public static final String ERROR_JOB_NOT_FOUND = "Role import job not found: %s";
    public static final String ERROR_UNKNOWN_REF = "UNKNOWN_REF";
    public static final String ERROR_VALIDATION = "VALIDATION_ERROR";
    public static final String ERROR_REFERENCE_VALIDATION = "REFERENCE_VALIDATION_ERROR";
    public static final String ERROR_PROCESSING = "PROCESSING_ERROR";
    public static final String ERROR_PARSE = "PARSE_ERROR";

    public static final String USER_ERROR_CHILD_ROLE_ID_NOT_FOUND =
            "One of the role IDs in childRoleIds does not exist for this tenant/environment. "
                    + "Verify the role ID exists in the deployed database, or use childRoleRefs for roles in the same import file.";
    public static final String USER_ERROR_UNKNOWN_CHILD_ROLE_REF =
            "childRoleRefs contains '%s', but no successfully imported row has that externalRef. "
                    + "Check the spelling/case or include the child role in the same import file.";
    public static final String USER_ERROR_DUPLICATE_ROLE_NAME =
            "A role with this name already exists for this tenant. Use a unique role name before importing again.";
    public static final String USER_ERROR_CHILD_ROLE_LINK_FAILED =
            "One or more childRoleRefs could not be linked to this role. Check the referenced rows for failures and retry after fixing them.";
    public static final String USER_ERROR_CHILD_ROLE_REF_DEPENDENCY_FAILED =
            "Role '%s' was not created because child role reference '%s' did not import successfully. "
                    + "Fix row %d first, or use childRoleIds if the child role already exists in the system.";
    public static final String USER_ERROR_CHILD_ROLE_REF_DEPENDENCY_MISSING =
            "Role '%s' was not created because child role reference '%s' is not present in this import job. "
                    + "Add a row with that externalRef, or use childRoleIds if the child role already exists in the system.";
    public static final String USER_ERROR_PAYLOAD_UNREADABLE =
            "This import row could not be read by the server. Verify the JSON/CSV format for this row and upload again.";
    public static final String USER_ERROR_IDENTITY_ASSIGNMENT_FAILED =
            "One of the identityIds could not be assigned to the imported role. Verify the identity exists for this tenant.";
    public static final String USER_ERROR_REFERENCE_VALIDATION_PREFIX =
            "%s was not created because referenced records are missing from this tenant/environment: %s. ";
    public static final String USER_ERROR_REFERENCE_VALIDATION_SUFFIX =
            "Fix these IDs or create the records first, then import again.";

    public static final String DISPLAY_STATUS_CREATED = "CREATED";
    public static final String DISPLAY_STATUS_CREATED_WITH_ERRORS = "CREATED_WITH_ERRORS";
    public static final String DISPLAY_STATUS_FAILED = "FAILED";
    public static final String DISPLAY_STATUS_QUEUED = "QUEUED";
    public static final String DISPLAY_STATUS_SKIPPED = "SKIPPED";

    public static final String USER_ERROR_REQUIRED_FIELD_TEMPLATE =
            "%s is required. Add the '%s' field with a valid value and upload again.";
    public static final String USER_ERROR_INVALID_FIELD_TEMPLATE =
            "%s is invalid. Correct the '%s' field and upload again.";
    public static final String USER_ERROR_CONSTRAINT_VIOLATION =
            "Some required role fields are missing or invalid. Fix the highlighted fields in this row and upload again.";
    public static final String USER_ERROR_DATA_INTEGRITY =
            "The role could not be saved because a database constraint was violated. Check required fields and duplicate values, then upload again.";
    public static final String USER_ERROR_UNEXPECTED_PROCESSING =
            "This row could not be processed. Review the reason below, fix the row, and upload again.";

    public static final String STATUS_URL_TEMPLATE = "/api/v1/access-roles/import/%s";
    public static final String ITEMS_URL_TEMPLATE = "/api/v1/access-roles/import/%s/items";
    public static final String ERRORS_URL_TEMPLATE = "/api/v1/access-roles/import/%s/errors.csv";
}

