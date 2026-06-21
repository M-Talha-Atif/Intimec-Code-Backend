package com.aidentx.iam.access_roles_management.audit;

/**
 * Constants specific to access role audit logging — DB actions,
 * descriptions, trigger sources, log messages, detail keys,
 * and UI-friendly activity labels.
 *
 * <p>All access role audit infrastructure code should reference
 * these constants instead of hardcoded strings.</p>
 *
 * <h3>Activity Label Strategy</h3>
 * <p>Activity labels are <strong>dynamically built</strong> using the role's type.
 * For example, a "business" role creation produces "Business Role Created",
 * while a "technical" role creation produces "Technical Role Created".
 * This is achieved via {@link String#format} templates and the
 * {@link #toActivityLabel(String, String)} resolver.</p>
 */
public final class AccessRoleAuditConstants {

    private AccessRoleAuditConstants() {
    }

    // ===================================================================
    //  Entity Type — Identifies the kind of object in the audit trail.
    //  Used as the "objectType" field in the Kafka AuditLogKafkaEvent.
    // ===================================================================

    public static final String ENTITY_TYPE = "Access Role";

    // ===================================================================
    //  DB Actions — Machine-readable action identifiers.
    //  These are stored in the audit database and used as internal keys.
    //  They are NOT displayed to users — see Activity Labels below.
    // ===================================================================

    /**
     * A new access role was created (with entitlements and children).
     */
    public static final String ACTION_ROLE_CREATED = "ROLE_CREATED";

    /**
     * An existing access role's governance metadata was updated.
     */
    public static final String ACTION_ROLE_UPDATED = "ROLE_UPDATED";

    /**
     * An access role was permanently deleted along with all its links.
     */
    public static final String ACTION_ROLE_DELETED = "ROLE_DELETED";

    /**
     * A role's entitlement assignments were updated (added and/or removed).
     */
    public static final String ACTION_ENTITLEMENTS_UPDATED = "ENTITLEMENTS_UPDATED";

    /**
     * A role's child role composition was updated (added and/or removed).
     */
    public static final String ACTION_CHILDREN_UPDATED = "CHILDREN_UPDATED";

    // ===================================================================
    //  UI-Friendly Activity Label Templates
    //
    //  These templates use Java's String.format() placeholder (%s) for
    //  the role type. At runtime, the placeholder is replaced with the
    //  capitalized role type (e.g. "Business", "Technical").
    //
    //  Example:
    //    String.format(ACTIVITY_ROLE_CREATED_TEMPLATE, "Business")
    //    → "Business Role Created"
    // ===================================================================

    /**
     * Template for role creation. Example: "Business Role Created".
     */
    public static final String ACTIVITY_ROLE_CREATED_TEMPLATE = "%s Role Created";

    /**
     * Template for role update. Example: "Technical Role Updated".
     */
    public static final String ACTIVITY_ROLE_UPDATED_TEMPLATE = "%s Role Updated";

    /**
     * Template for role deletion. Example: "Application Role Deleted".
     */
    public static final String ACTIVITY_ROLE_DELETED_TEMPLATE = "%s Role Deleted";

    /**
     * Template for entitlement sync. Example: "Business Role Entitlements Updated".
     */
    public static final String ACTIVITY_ENTITLEMENTS_UPDATED_TEMPLATE = "%s Role Entitlements Updated";

    /**
     * Template for children sync. Example: "Functional Role Children Updated".
     */
    public static final String ACTIVITY_CHILDREN_UPDATED_TEMPLATE = "%s Role Children Updated";

    // ===================================================================
    //  Descriptions — Human-readable explanations for audit records.
    //  Used in the "description" field of database audit log entries.
    // ===================================================================

    public static final String DESC_ROLE_CREATED = "A new access role was created";
    public static final String DESC_ROLE_CREATED_THROUGH_IMPORT = "Role was created through import";
    public static final String DESC_ROLE_UPDATED = "Access role governance metadata was updated";
    public static final String DESC_ROLE_DELETED = "An access role was deleted";
    public static final String DESC_ENTITLEMENTS_UPDATED = "Role entitlement assignments were updated";
    public static final String DESC_CHILDREN_UPDATED = "Role child composition was updated";

    // ===================================================================
    //  Trigger Sources — Identifies what initiated the audit event.
    // ===================================================================

    /**
     * The operation was triggered via a REST API call.
     */
    public static final String TRIGGER_API = "API";

    /**
     * The operation was triggered by an access-role import job.
     */
    public static final String TRIGGER_IMPORT = "IMPORT";

    /**
     * State marker written to created-role audit snapshots for import-created roles.
     */
    public static final String STATE_FIELD_CREATED_THROUGH = "createdThrough";

    // ===================================================================
    //  Log Messages — Structured messages for the unified logger.
    //  Used internally for observability; not displayed to end users.
    // ===================================================================

    /**
     * Logged when the aspect fails to publish an audit event.
     */
    public static final String LOG_AUDIT_PUBLISH_FAILED =
            "Failed to publish access role audit event from aspect";

    /**
     * Logged when the aspect cannot read pre-operation state from DB.
     */
    public static final String LOG_STATE_CAPTURE_FAILED =
            "Could not capture pre-operation state for audit";

    /**
     * Logged when a Kafka publish is about to start.
     */
    public static final String LOG_PUBLISHING_EVENT =
            "Publishing access role audit event to Kafka";

    /**
     * Logged after successful Kafka publish.
     */
    public static final String LOG_EVENT_PUBLISHED =
            "Access role audit event published to Kafka successfully";

    /**
     * Logged when Kafka publish fails.
     */
    public static final String LOG_EVENT_PUBLISH_FAILED =
            "Failed to publish access role audit event to Kafka";

    // ===================================================================
    //  Detail Keys — Field names used in the Kafka event's "details"
    //  JSON object. Consumers of the audit stream use these keys to
    //  extract specific information from the event payload.
    // ===================================================================

    public static final String DETAIL_ROLE_ID = "roleId";
    public static final String DETAIL_ROLE_NAME = "roleName";
    public static final String DETAIL_ROLE_TYPE = "roleType";
    public static final String DETAIL_STATUS = "status";
    public static final String DETAIL_BUSINESS_UNIT = "businessUnit";
    public static final String DETAIL_RISK_LEVEL = "riskLevel";
    public static final String DETAIL_OWNER_ID = "ownerId";
    public static final String DETAIL_REQUESTABLE = "requestable";
    public static final String DETAIL_REQUIRE_APPROVAL = "requireApproval";
    public static final String DETAIL_CERTIFICATION_SCHEDULE = "certificationSchedule";

    /**
     * Array of child roles that were successfully added.
     */
    public static final String DETAIL_ADDED_CHILDREN = "addedChildren";

    /**
     * Array of child roles that were removed.
     */
    public static final String DETAIL_REMOVED_CHILDREN = "removedChildren";

    /**
     * Array of child roles that failed validation (circular dep, etc.).
     */
    public static final String DETAIL_FAILED_CHILDREN = "failedChildren";

    /**
     * Array of entitlements that were successfully linked.
     */
    public static final String DETAIL_ADDED_ENTITLEMENTS = "addedEntitlements";

    /**
     * Array of entitlements that were unlinked.
     */
    public static final String DETAIL_REMOVED_ENTITLEMENTS = "removedEntitlements";

    /**
     * The highest risk level among all children involved in the operation.
     */
    public static final String DETAIL_HIGHEST_CHILD_RISK = "highestChildRiskLevel";

    /**
     * The highest risk level among all entitlements involved in the operation.
     */
    public static final String DETAIL_HIGHEST_ENTITLEMENT_RISK = "highestEntitlementRiskLevel";

    /**
     * Count of entities successfully added in the operation.
     */
    public static final String DETAIL_ADDED_COUNT = "addedCount";

    /**
     * Count of entities removed in the operation.
     */
    public static final String DETAIL_REMOVED_COUNT = "removedCount";

    /**
     * Count of entities that failed validation in the operation.
     */
    public static final String DETAIL_FAILED_COUNT = "failedCount";

    /**
     * The action identifier key.
     */
    public static final String DETAIL_ACTION = "action";

    /**
     * The exception class name for error logging.
     */
    public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";

    // ===================================================================
    //  Metadata Keys — Field names used in the Kafka event's "metadata"
    //  JSON object. Metadata provides context about the event source
    //  and is used for routing, filtering, and correlation.
    // ===================================================================

    public static final String METADATA_SOURCE = "source";
    public static final String METADATA_SOURCE_VALUE = "access-role-service";
    public static final String METADATA_TENANT_ID = "tenantId";
    public static final String METADATA_ENTITY_TYPE = "entityType";
    public static final String METADATA_ROLE_ID = "roleId";
    public static final String METADATA_ROLE_NAME = "roleName";
    public static final String METADATA_RISK_LEVEL = "riskLevel";

    // ===================================================================
    //  Target Object Keys — Identifies the audited entity in the Kafka
    //  event's "targetObject" JSON. Used by the audit UI to link events
    //  to the affected resource.
    // ===================================================================

    public static final String TARGET_TYPE = "type";
    public static final String TARGET_OBJECT_ID = "objectId";
    public static final String TARGET_OBJECT_NAME = "objectName";

    // ===================================================================
    //  Logger Category — Used by the UnifiedLogger to categorize
    //  all audit-related log entries under one namespace.
    // ===================================================================

    public static final String LOG_CATEGORY = "access-role-audit";

    // ===================================================================
    //  Placeholder for missing values in the audit payload.
    // ===================================================================

    public static final String NOT_AVAILABLE = "N/A";

    // ===================================================================
    //  Role Type Display Helpers
    // ===================================================================

    /**
     * Capitalizes a raw role type string from the database for display.
     *
     * <p>The database stores role types in lowercase (e.g. "business",
     * "technical"). This method produces a capitalized version suitable
     * for user-facing activity labels.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>"business" → "Business"</li>
     *   <li>"technical" → "Technical"</li>
     *   <li>"application" → "Application"</li>
     *   <li>null → "Access" (fallback)</li>
     * </ul>
     *
     * @param rawType the role type as stored in the database (lowercase)
     * @return capitalized display label, or "Access" as fallback
     */
    public static String capitalizeRoleType(String rawType) {
        if (rawType == null || rawType.isBlank()) {
            return "Access";
        }
        return rawType.substring(0, 1).toUpperCase() + rawType.substring(1).toLowerCase();
    }

    /**
     * Resolves the audit entity type from the raw role type.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>"business" → "Business Role"</li>
     *   <li>"technical" → "Technical Role"</li>
     *   <li>"functional" → "Functional Role"</li>
     *   <li>"application" → "Application Role"</li>
     *   <li>null/blank → "Access Role"</li>
     * </ul>
     *
     * @param roleType the raw role subtype stored in the database
     * @return the human-readable audit entity type
     */
    public static String toEntityType(String roleType) {
        String displayType = capitalizeRoleType(roleType);
        if ("Access".equals(displayType)) {
            return ENTITY_TYPE;
        }
        return displayType + " Role";
    }

    /**
     * Maps a DB action string and role type to a user-friendly activity label.
     *
     * <p>This is the <strong>single source of truth</strong> for how audit
     * actions appear in the UI. Every audit consumer (dashboard, reports,
     * certification reviews) should use this method to resolve labels.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code toActivityLabel("ROLE_CREATED", "business")} → "Business Role Created"</li>
     *   <li>{@code toActivityLabel("ENTITLEMENTS_UPDATED", "technical")} → "Technical Role Entitlements Updated"</li>
     *   <li>{@code toActivityLabel("ROLE_DELETED", "application")} → "Application Role Deleted"</li>
     * </ul>
     *
     * @param action   the DB action constant (e.g. {@link #ACTION_ROLE_CREATED})
     * @param roleType the raw role type from the database (e.g. "business", "technical")
     * @return the UI-friendly activity label
     */
    public static String toActivityLabel(String action, String roleType) {
        if (action == null) return "Unknown";
        String displayType = capitalizeRoleType(roleType);
        return switch (action) {
            case ACTION_ROLE_CREATED -> String.format(ACTIVITY_ROLE_CREATED_TEMPLATE, displayType);
            case ACTION_ROLE_UPDATED -> String.format(ACTIVITY_ROLE_UPDATED_TEMPLATE, displayType);
            case ACTION_ROLE_DELETED -> String.format(ACTIVITY_ROLE_DELETED_TEMPLATE, displayType);
            case ACTION_ENTITLEMENTS_UPDATED -> String.format(ACTIVITY_ENTITLEMENTS_UPDATED_TEMPLATE, displayType);
            case ACTION_CHILDREN_UPDATED -> String.format(ACTIVITY_CHILDREN_UPDATED_TEMPLATE, displayType);
            default -> action;
        };
    }
}