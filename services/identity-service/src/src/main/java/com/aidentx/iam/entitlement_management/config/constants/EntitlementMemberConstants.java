package com.aidentx.iam.entitlement_management.config.constants;

/**
 * Constants for the entitlement member (assignment view) domain.
 *
 * <p>Centralises log messages, detail keys, and assignment-status values
 * used when building {@code EntitlementMemberDto} responses.</p>
 */
public final class EntitlementMemberConstants {

    private EntitlementMemberConstants() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    // ============================================================
    //  Log Category
    // ============================================================

    public static final String LOG_CATEGORY = "EntitlementMembers";

    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_USER_ID = "X-User-ID";
    public static final String HEADER_USERNAME = "X-Username";
    public static final String HEADER_USER = "X-User";
    public static final String PATH_ENTITLEMENT_MEMBERS = "/{entitlementId}/members";
    public static final String PATH_ENTITLEMENT_MEMBERS_MANUAL = "/{entitlementId}/members/manual";

    // ============================================================
    //  Log Messages
    // ============================================================

    public static final String LOG_MEMBERS_FETCHED = "Entitlement members fetched";
    public static final String LOG_ROLE_RESOLVED = "Resolved access role for entitlement member";
    public static final String LOG_ROLE_NOT_FOUND = "Access role referenced by assignment not found";
    public static final String LOG_MANUAL_ASSIGNMENT_REQUESTED = "Manual entitlement member assignment requested";
    public static final String LOG_MANUAL_ASSIGNMENT_COMPLETED = "Manual entitlement member assignment completed";
    public static final String LOG_MANUAL_ASSIGNMENT_CREATED = "Manual entitlement assignment created";
    public static final String LOG_MANUAL_ASSIGNMENT_UPDATED = "Manual entitlement assignment updated";
    public static final String LOG_MANUAL_ASSIGNMENT_ALREADY_EXISTS = "Manual entitlement assignment already exists";
    public static final String LOG_MANUAL_ASSIGNMENT_PENDING = "Manual entitlement assignment pending because identity has no target account";
    public static final String LOG_MANUAL_SYNC_STARTED = "Manual entitlement member sync started";
    public static final String LOG_MANUAL_SYNC_COMPLETED = "Manual entitlement member sync completed";
    public static final String LOG_MANUAL_SYNC_GRANT = "Manual entitlement member sync grant";
    public static final String LOG_MANUAL_SYNC_REVOKE = "Manual entitlement member sync revoke";
    public static final String LOG_MANUAL_SYNC_SKIPPED = "Manual entitlement member sync skipped existing member";
    public static final String LOG_MANUAL_SYNC_FAILED = "Manual entitlement member sync failed";

    // ============================================================
    //  Log Detail Keys
    // ============================================================

    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_ENTITLEMENT_ID = "entitlementId";
    public static final String DETAIL_ASSIGNMENT_ID = "assignmentId";
    public static final String DETAIL_ACCESS_ROLE_ID = "accessRoleId";
    public static final String DETAIL_IDENTITY_ID = "identityId";
    public static final String DETAIL_REQUESTED_BY = "requestedBy";
    public static final String DETAIL_REQUESTED_COUNT = "requestedCount";
    public static final String DETAIL_CREATED_COUNT = "createdCount";
    public static final String DETAIL_UPDATED_COUNT = "updatedCount";
    public static final String DETAIL_PENDING_COUNT = "pendingCount";
    public static final String DETAIL_ALREADY_ASSIGNED_COUNT = "alreadyAssignedCount";
    public static final String DETAIL_SHADOW_ID = "shadowId";
    public static final String DETAIL_MEMBER_COUNT = "memberCount";
    public static final String DETAIL_DESIRED_COUNT = "desiredCount";
    public static final String DETAIL_CURRENT_COUNT = "currentCount";
    public static final String DETAIL_TO_ADD_COUNT = "toAddCount";
    public static final String DETAIL_TO_REVOKE_COUNT = "toRevokeCount";
    public static final String DETAIL_REVOKED_COUNT = "revokedCount";
    public static final String DETAIL_SKIPPED_COUNT = "skippedCount";
    public static final String DETAIL_FAILED_COUNT = "failedCount";
    public static final String DETAIL_TOTAL_DIRECT_COUNT = "totalDirectCount";
    public static final String DETAIL_ERROR = "error";

    // ============================================================
    //  Assignment Status Values
    // ============================================================

    /** Entitlement row exists in entitlement_assignments — fully provisioned. */
    public static final String STATUS_ASSIGNED = "ASSIGNED";

    /** Identity has no account (shadow) on the target application yet. */
    public static final String STATUS_PENDING = "PENDING";

    /** Account exists but entitlement is not yet provisioned to it. */
    public static final String STATUS_NOT_ASSIGNED = "NOT_ASSIGNED";

    // ============================================================
    //  Assignment Result Actions
    // ============================================================

    public static final String ACTION_CREATED = "CREATED";
    public static final String ACTION_UPDATED = "UPDATED";
    public static final String ACTION_ALREADY_ASSIGNED = "ALREADY_ASSIGNED";
    public static final String ACTION_REVOKED = "REVOKED";

    // ============================================================
    //  Assignment Status Reason Messages
    // ============================================================

    public static final String REASON_NO_ACCOUNT = "Identity has no account on the target application";
    public static final String REASON_NOT_PROVISIONED = "Entitlement not yet provisioned to identity's account";
}

