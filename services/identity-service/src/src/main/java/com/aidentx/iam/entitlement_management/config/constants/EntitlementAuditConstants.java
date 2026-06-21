package com.aidentx.iam.entitlement_management.config.constants;

/**
 * Constants specific to entitlement audit logging  DB actions,
 * descriptions, trigger sources, log messages, and detail keys.
 *
 * <p>All entitlement audit infrastructure code should reference
 * these constants instead of hardcoded strings.
 */
public final class EntitlementAuditConstants {

    private EntitlementAuditConstants() {
    }

    // ===================================================================
    //  DB Actions
    // ===================================================================

    public static final String ACTION_GOVERNANCE_UPDATED = "ENTITLEMENT_UPDATED";
    public static final String ACTION_RULE_CREATED = "RULE_CREATED";
    public static final String ACTION_RULE_UPDATED = "RULE_UPDATED";
    public static final String ACTION_RULE_DELETED = "RULE_DELETED";
    public static final String ACTION_RULE_EVALUATED = "RULE_EVALUATED";
    public static final String ACTION_MEMBERS_MANUALLY_SYNCED = "ENTITLEMENT_MEMBERS_MANUALLY_SYNCED";

    // ===================================================================
    //  Trigger Sources
    // ===================================================================

    public static final String TRIGGER_API = "API";
    public static final String TRIGGER_ENTITLEMENT_RULE_CREATED = "ENTITLEMENT_RULE_CREATED";
    public static final String TRIGGER_ENTITLEMENT_RULE_UPDATED = "ENTITLEMENT_RULE_UPDATED";
    public static final String TRIGGER_CRON = "CRON";
    public static final String TRIGGER_EVALUATE_ALL = "EVALUATE_ALL";

    // ===================================================================
    //  Descriptions
    // ===================================================================

    public static final String DESC_GOVERNANCE_UPDATED = "Entitlement governance metadata was updated";
    public static final String DESC_RULE_CREATED = "A new entitlement assignment rule was created";
    public static final String DESC_RULE_UPDATED = "An entitlement assignment rule was updated";
    public static final String DESC_RULE_DELETED = "An entitlement assignment rule was deleted";
    public static final String DESC_RULE_EVALUATED = "Entitlement assignment rule was evaluated";
    public static final String DESC_MEMBERS_MANUALLY_SYNCED_TEMPLATE =
            "Manual member sync was processed for entitlement \"%s\" by %s (%d added, %d removed, %d skipped, %d failed)";

    // ===================================================================
    //  Log Messages
    // ===================================================================

    public static final String LOG_DB_PERSIST_FAILED = "Failed to persist entitlement audit record to database";
    public static final String LOG_GOVERNANCE_AUDIT_FAILED = "Failed to publish governance audit event from aspect";

    // ===================================================================
    //  Detail Keys
    // ===================================================================

    public static final String DETAIL_ACTION = "action";
    public static final String DETAIL_ENTITLEMENT_ID = "entitlementId";
    public static final String DETAIL_ENTITLEMENT_NAME = "entitlementName";
    public static final String DETAIL_RISK_LEVEL = "riskLevel";
    public static final String DETAIL_ADDED_COUNT = "addedCount";
    public static final String DETAIL_REMOVED_COUNT = "removedCount";
    public static final String DETAIL_SKIPPED_COUNT = "skippedCount";
    public static final String DETAIL_FAILED_COUNT = "failedCount";
    public static final String DETAIL_TOTAL_DIRECT_COUNT = "totalDirectCount";
    public static final String DETAIL_ADDED_MEMBERS = "addedMembers";
    public static final String DETAIL_REMOVED_MEMBERS = "removedMembers";
    public static final String DETAIL_SKIPPED_MEMBERS = "skippedMembers";
    public static final String DETAIL_FAILED_MEMBERS = "failedMembers";
    public static final String DETAIL_MESSAGE = "message";
    public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";
    public static final String DETAIL_ERROR = "error";
    public static final String DETAIL_NOT_AVAILABLE = "N/A";

    // ===================================================================
    //  Logger Category
    // ===================================================================

    public static final String LOG_CATEGORY = "entitlement-audit";

        // ===================================================================
    //  UI-Friendly Activity Labels
    // ===================================================================

    public static final String ACTIVITY_GOVERNANCE_UPDATED = "Entitlement Updated";
    public static final String ACTIVITY_RULE_CREATED = "Rule Created";
    public static final String ACTIVITY_RULE_UPDATED = "Rule Updated";
    public static final String ACTIVITY_RULE_DELETED = "Rule Deleted";
    public static final String ACTIVITY_RULE_EVALUATED = "Rule Evaluated";
    public static final String ACTIVITY_MEMBERS_MANUALLY_SYNCED = "Entitlement Members Synced";

    // Excluding fields
    public static final String PRIORITY_SKIP_FIELD = "priority";

    /**
     * Maps a DB action string to a user-friendly activity label.
     *
     * @param action the DB action constant
     * @return the UI-friendly label
     */
    public static String toActivityLabel(String action) {
        if (action == null) return "Unknown";
        return switch (action) {
            case ACTION_GOVERNANCE_UPDATED -> ACTIVITY_GOVERNANCE_UPDATED;
            case ACTION_RULE_CREATED -> ACTIVITY_RULE_CREATED;
            case ACTION_RULE_UPDATED -> ACTIVITY_RULE_UPDATED;
            case ACTION_RULE_DELETED -> ACTIVITY_RULE_DELETED;
            case ACTION_RULE_EVALUATED -> ACTIVITY_RULE_EVALUATED;
            case ACTION_MEMBERS_MANUALLY_SYNCED -> ACTIVITY_MEMBERS_MANUALLY_SYNCED;
            default -> action;
        };
    }
}