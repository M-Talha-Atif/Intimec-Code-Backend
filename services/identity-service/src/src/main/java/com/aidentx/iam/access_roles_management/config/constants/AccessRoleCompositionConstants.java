package com.aidentx.iam.access_roles_management.config.constants;

/**
 * Constants specific to role composition (parent-child) operations.
 */
public final class AccessRoleCompositionConstants {

    private AccessRoleCompositionConstants() {}

    // --- Log Category ---
    public static final String LOG_CATEGORY = "access-role-compositions";

    // --- Log Messages ---
    public static final String LOG_CHILDREN_ADDED = "Child roles added to parent role";
    public static final String LOG_CHILDREN_REMOVED = "Child roles removed from parent role";
    public static final String LOG_CHILD_LINK_SKIPPED = "Child role already linked, skipping";
    public static final String LOG_CHILDREN_FETCHED = "Child roles fetched for parent role";
    public static final String LOG_PARENTS_FETCHED = "Parent roles fetched for child role";
    public static final String LOG_RELATIONSHIPS_FETCHED = "Unified access-role relationships fetched";
    public static final String LOG_ADD_FAILED = "Failed to add child roles";
    public static final String LOG_REMOVE_FAILED = "Failed to remove child roles";
    public static final String LOG_CYCLE_DETECTED = "Circular dependency detected in role composition";
    public static final String LOG_SELF_REFERENCE = "Self-reference attempted in role composition";

    // --- Log Detail Keys ---
    public static final String DETAIL_PARENT_ROLE_ID = "parentRoleId";
    public static final String DETAIL_CHILD_ROLE_ID = "childRoleId";
    public static final String DETAIL_ADDED_COUNT = "addedCount";
    public static final String DETAIL_REMOVED_COUNT = "removedCount";
    public static final String DETAIL_CHILDREN_COUNT = "childrenCount";
    public static final String DETAIL_PARENTS_COUNT = "parentsCount";
    public static final String DETAIL_ROLE_ID = "roleId";
    public static final String DETAIL_MODE = "mode";
    public static final String DETAIL_DIRECTION = "direction";
    public static final String DETAIL_RELATED_ROLE_COUNT = "relatedRoleCount";

    // --- API Response Messages ---
    public static final String RESPONSE_CHILDREN_ADDED = "Child roles added successfully";
    public static final String RESPONSE_CHILDREN_REMOVED = "Child roles removed successfully";
    public static final String RESPONSE_CHILDREN_FETCHED = "Child roles retrieved successfully";
    public static final String RESPONSE_PARENTS_FETCHED = "Parent roles retrieved successfully";
    public static final String RESPONSE_RELATIONSHIPS_FETCHED = "Role relationships retrieved successfully";
    public static final String RESPONSE_CHILDREN_PARTIAL = "Some child roles could not be added";

    // --- Conflict Reasons ---
    public static final String CONFLICT_REASON_ENTITLEMENT_OVERLAP =
            "The same entitlement is directly assigned to the current role and one or more related parent/child roles.";
    public static final String CONFLICT_REASON_ENTITLEMENT_OVERLAP_WITH_ROLES =
            "Entitlement '%s' is assigned to multiple roles in this relationship: %s. Review these role assignments to resolve the overlap.";

}
