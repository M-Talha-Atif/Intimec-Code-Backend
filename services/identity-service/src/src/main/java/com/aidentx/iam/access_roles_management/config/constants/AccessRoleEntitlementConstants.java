package com.aidentx.iam.access_roles_management.config.constants;

/**
 * Constants specific to role-entitlement link operations.
 */
public final class AccessRoleEntitlementConstants {

    private AccessRoleEntitlementConstants() {
    }

    // --- Log Category ---
    public static final String LOG_CATEGORY = "access-role-entitlements";

    // --- Log Messages ---
    public static final String LOG_ENTITLEMENTS_ADDED = "Entitlements added to access role";
    public static final String LOG_ENTITLEMENTS_REMOVED = "Entitlements removed from access role";
    public static final String LOG_ENTITLEMENT_LINK_SKIPPED = "Entitlement already linked, skipping";
    public static final String LOG_ENTITLEMENTS_FETCHED = "Entitlement IDs fetched for access role";
    public static final String LOG_ADD_FAILED = "Failed to add entitlements to access role";
    public static final String LOG_REMOVE_FAILED = "Failed to remove entitlements from access role";
    public static final String LOG_ENTITLEMENT_DETAILS_FETCHED = "Entitlement details fetched for role";
    public static final String RESPONSE_GROUPED_ENTITLEMENTS_FETCHED = "Grouped entitlements fetched successfully";
    public static final String LOG_GROUPED_ENTITLEMENTS_FETCHED = "Grouped entitlement details fetched for role";
    public static final int DEFAULT_APP_PAGE_SIZE = 5;
    public static final int DEFAULT_ENT_PAGE_SIZE = 10;

    // --- Log Detail Keys ---
    public static final String DETAIL_ROLE_ID = "roleId";
    public static final String DETAIL_ENTITLEMENT_ID = "entitlementId";
    public static final String DETAIL_ADDED_COUNT = "addedCount";
    public static final String DETAIL_REMOVED_COUNT = "removedCount";
    public static final String DETAIL_TOTAL_COUNT = "totalCount";
    public static final String DETAIL_REQUESTED_COUNT = "requestedCount";
    public static final String DETAIL_RESOLVED_COUNT = "resolvedCount";


    // --- API Response Messages ---
    public static final String RESPONSE_ENTITLEMENTS_ADDED = "Entitlements added to role successfully";
    public static final String RESPONSE_ENTITLEMENTS_REMOVED = "Entitlements removed from role successfully";
    public static final String RESPONSE_ENTITLEMENTS_FETCHED = "Role entitlements retrieved successfully";
}