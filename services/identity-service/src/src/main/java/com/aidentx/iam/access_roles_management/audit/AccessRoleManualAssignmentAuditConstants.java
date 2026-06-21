package com.aidentx.iam.access_roles_management.audit;

/**
 * Constants for manual access-role member grant auditing.
 */
public final class AccessRoleManualAssignmentAuditConstants {

    private AccessRoleManualAssignmentAuditConstants() {
    }

    public static final String ACTION_MEMBERS_MANUALLY_GRANTED = "ROLE_MEMBERS_MANUALLY_GRANTED";
    public static final String ACTION_MEMBERS_MANUALLY_SYNCED = "ROLE_MEMBERS_MANUALLY_SYNCED";

    public static final String ACTIVITY_MEMBERS_MANUALLY_GRANTED_TEMPLATE = "%s Role Members Manually Granted";
    public static final String ACTIVITY_MEMBERS_MANUALLY_SYNCED_TEMPLATE = "%s Role Members Synced";
    public static final String DESC_MEMBERS_MANUALLY_GRANTED_TEMPLATE =
            "Manual role grants were processed for role \"%s\" by %s (%d added, %d skipped, %d failed)";
    public static final String DESC_MEMBERS_MANUALLY_SYNCED_TEMPLATE =
            "Manual member sync was processed for role \"%s\" by %s (%d added, %d revoked, %d skipped, %d failed)";

    public static final String LOG_MANUAL_SYNC_AUDIT_FAILED = "Failed to publish manual access-role member sync audit event";
    public static final String LOG_PUBLISHING_SYNC_EVENT = "Publishing manual access-role member sync audit event to Kafka";
    public static final String LOG_SYNC_EVENT_PUBLISHED = "Manual access-role member sync audit event published to Kafka successfully";
    public static final String LOG_SYNC_EVENT_PUBLISH_FAILED = "Failed to publish manual access-role member sync audit event to Kafka";

    // Sync-specific detail / state keys
    public static final String DETAIL_REVOKED_COUNT = "revokedCount";
    public static final String DETAIL_TOTAL_ACTIVE_COUNT = "totalActiveCount";
    public static final String DETAIL_REVOKED_IDENTITIES = "revokedIdentities";
    public static final String STATE_REVOKED_COUNT = "revokedCount";
    public static final String STATE_TOTAL_ACTIVE_COUNT = "totalActiveCount";
    public static final String STATE_REVOKED_IDENTITIES = "revokedIdentities";

    // Per-member entitlement breakdown keys
    public static final String MEMBER_ENTITLEMENTS_PROVISIONED = "entitlementsProvisioned";
    public static final String MEMBER_ENTITLEMENTS_PENDING = "entitlementsPending";
    public static final String MEMBER_ENTITLEMENTS_DEPROVISIONED = "entitlementsDeprovisioned";
    public static final String MEMBER_ENTITLEMENT_ID = "entitlementId";
    public static final String MEMBER_ENTITLEMENT_NAME = "entitlementName";
    public static final String MEMBER_ENTITLEMENT_APPLICATION_ID = "applicationId";
    public static final String MEMBER_ENTITLEMENT_APPLICATION_NAME = "applicationName";
    public static final String MEMBER_ENTITLEMENT_ACTION = "action";
    public static final String ENTITLEMENT_ACTION_PROVISIONED = "PROVISIONED";
    public static final String ENTITLEMENT_ACTION_PENDING = "PENDING";
    public static final String ENTITLEMENT_ACTION_DEPROVISIONED = "DEPROVISIONED";

    public static final String TRIGGER_API = "API";

    public static final String LOG_CATEGORY = "access-role-manual-assignment-audit";
    public static final String LOG_PUBLISHING_EVENT = "Publishing manual access-role member grant audit event to Kafka";
    public static final String LOG_EVENT_PUBLISHED = "Manual access-role member grant audit event published to Kafka successfully";
    public static final String LOG_EVENT_PUBLISH_FAILED = "Failed to publish manual access-role member grant audit event to Kafka";
    public static final String LOG_MANUAL_GRANT_AUDIT_FAILED = "Failed to publish manual access-role member grant audit event";

    public static final String DETAIL_ACTION = "action";
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_ROLE_ID = "roleId";
    public static final String DETAIL_ROLE_NAME = "roleName";
    public static final String DETAIL_ROLE_TYPE = "roleType";
    public static final String DETAIL_RISK_LEVEL = "riskLevel";
    public static final String DETAIL_REQUESTED_COUNT = "requestedCount";
    public static final String DETAIL_ADDED_COUNT = "addedCount";
    public static final String DETAIL_SKIPPED_COUNT = "skippedCount";
    public static final String DETAIL_FAILED_COUNT = "failedCount";
    public static final String DETAIL_ADDED_IDENTITIES = "addedIdentities";
    public static final String DETAIL_SKIPPED_IDENTITIES = "skippedIdentities";
    public static final String DETAIL_FAILED_IDENTITIES = "failedIdentities";
    public static final String DETAIL_MESSAGE = "message";
    public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";

    public static final String METADATA_SOURCE = "source";
    public static final String METADATA_SOURCE_VALUE = "access-role-manual-assignment-service";
    public static final String METADATA_ENTITY_TYPE = "entityType";
    public static final String METADATA_RISK_LEVEL = "riskLevel";

    public static final String TARGET_TYPE = "type";
    public static final String TARGET_OBJECT_ID = "objectId";
    public static final String TARGET_OBJECT_NAME = "objectName";

    public static final String STATE_ROLE_ID = "roleId";
    public static final String STATE_ROLE_NAME = "roleName";
    public static final String STATE_ROLE_TYPE = "roleType";
    public static final String STATE_RISK_LEVEL = "riskLevel";
    public static final String STATE_REQUESTED_COUNT = "requestedCount";
    public static final String STATE_ADDED_COUNT = "addedCount";
    public static final String STATE_SKIPPED_COUNT = "skippedCount";
    public static final String STATE_FAILED_COUNT = "failedCount";
    public static final String STATE_ADDED_IDENTITIES = "addedIdentities";
    public static final String STATE_SKIPPED_IDENTITIES = "skippedIdentities";
    public static final String STATE_FAILED_IDENTITIES = "failedIdentities";

    public static final String NOT_AVAILABLE = "N/A";

    public static String capitalizeRoleType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "Access";
        }
        return rawType.substring(0, 1).toUpperCase() + rawType.substring(1).toLowerCase();
    }

    public static String toEntityType(String roleType) {
        return AccessRoleAuditConstants.toEntityType(roleType);
    }

    public static String toActivityLabel(String roleType) {
        return String.format(ACTIVITY_MEMBERS_MANUALLY_GRANTED_TEMPLATE, capitalizeRoleType(roleType));
    }

    public static String toSyncActivityLabel(String roleType) {
        return String.format(ACTIVITY_MEMBERS_MANUALLY_SYNCED_TEMPLATE, capitalizeRoleType(roleType));
    }
}

