package com.aidentx.iam.access_roles_management.audit;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Immutable audit event record for access role operations.
 *
 * <p>This is a Java {@code record} — an immutable data carrier introduced
 * in Java 16. The compiler automatically generates the constructor, getters,
 * {@code equals()}, {@code hashCode()}, and {@code toString()} from the
 * component list. Records are ideal for audit events because audit data
 * should never be modified after creation.</p>
 *
 * <h3>Event Structure</h3>
 * <p>Every audit event contains:</p>
 * <ul>
 *   <li><strong>Actor</strong> — who performed the action (from Spring Security)</li>
 *   <li><strong>Action</strong> — what was done (DB action constant)</li>
 *   <li><strong>Entity</strong> — the role being audited (ID, name, type)</li>
 *   <li><strong>State</strong> — before/after JSON snapshots for diff comparison</li>
 *   <li><strong>Details</strong> — rich information about affected sub-entities
 *       (child roles with names/types, entitlements with names/risk levels)</li>
 * </ul>
 *
 * <h3>Factory Methods</h3>
 * <p>Instead of calling the constructor directly (which has 20+ parameters),
 * use the static factory methods: {@link #roleCreated}, {@link #roleUpdated},
 * {@link #roleDeleted}, {@link #entitlementsUpdated}, {@link #childrenUpdated}.
 * Each method populates only the fields relevant to that action and fills
 * the rest with empty defaults.</p>
 *
 * @param actorId              the identity ID of the user who performed the action
 * @param actorUsername         the username of the actor
 * @param tenantId             the tenant identifier
 * @param action               the DB action string (e.g. "ROLE_CREATED")
 * @param entityId             the role ID (null for pre-create state)
 * @param entityName           the role name
 * @param entityType           always "ACCESS_ROLE"
 * @param roleType             the role type (business, technical, etc.)
 * @param status               the role status (enabled, disabled, draft)
 * @param riskLevel            the risk level for this audit event
 * @param businessUnit         the business unit of the role
 * @param ownerId              the role owner identity ID
 * @param addedChildren        child roles successfully added
 * @param removedChildren      child roles removed
 * @param failedChildren       child roles that failed validation
 * @param addedEntitlements    entitlements successfully linked
 * @param removedEntitlements  entitlements unlinked
 * @param stateBefore          JSON snapshot before the operation
 * @param stateAfter           JSON snapshot after the operation
 * @param timestamp            when the event occurred
 */
public record AccessRoleAuditEvent(
        String actorId,
        String actorUsername,
        String tenantId,
        String action,
        String entityId,
        String entityName,
        String entityType,
        String roleType,
        String status,
        String riskLevel,
        String businessUnit,
        String ownerId,
        List<ChildRoleAuditDetail> addedChildren,
        List<ChildRoleAuditDetail> removedChildren,
        List<FailedChildRoleAuditDetail> failedChildren,
        List<EntitlementAuditDetail> addedEntitlements,
        List<EntitlementAuditDetail> removedEntitlements,
        JsonNode stateBefore,
        JsonNode stateAfter,
        Instant timestamp
) {

    // ===================================================================
    //  Inner Records — Rich detail carriers for sub-entities.
    //
    //  These are defined as inner records so they are tightly coupled
    //  to the audit event. They exist solely to make audit logs
    //  human-readable by including names, not just UUIDs.
    // ===================================================================

    /**
     * Audit detail for a child role that was successfully added or removed.
     *
     * @param id        the child role UUID as string
     * @param name      the child role display name
     * @param type      the child role type (business, technical, etc.)
     * @param riskLevel the child role's risk level (high, medium, low, or null)
     */
    public record ChildRoleAuditDetail(
            String id,
            String name,
            String type,
            String riskLevel
    ) {
    }

    /**
     * Audit detail for a child role that failed validation during an add operation.
     *
     * <p>Extends the child detail with a failure reason so auditors can see
     * <em>why</em> a specific child role was rejected (e.g. circular dependency,
     * self-reference, role not found).</p>
     *
     * @param id        the child role UUID as string
     * @param name      the child role display name
     * @param type      the child role type
     * @param riskLevel the child role's risk level
     * @param reason    the human-readable failure reason
     */
    public record FailedChildRoleAuditDetail(
            String id,
            String name,
            String type,
            String riskLevel,
            String reason
    ) {
    }

    /**
     * Audit detail for an entitlement that was added or removed.
     *
     * @param id        the entitlement UUID as string
     * @param name      the entitlement display name (e.g. "SAP_FI_POSTING")
     * @param riskLevel the entitlement's risk level (high, medium, low, or null)
     */
    public record EntitlementAuditDetail(
            String id,
            String name,
            String riskLevel
    ) {
    }

    // ===================================================================
    //  Factory Methods
    // ===================================================================

    /**
     * Creates an audit event for a newly created role.
     *
     * <p>The {@code stateAfter} JSON should be enriched by the aspect to
     * include linked entitlements and child roles with their names,
     * giving a complete picture of the role at creation time.</p>
     *
     * @param actorId       the actor's identity ID
     * @param actorUsername  the actor's username
     * @param tenantId      the tenant identifier
     * @param roleId        the newly created role's ID
     * @param roleName      the role name
     * @param roleType      the role type (e.g. "business")
     * @param roleStatus    the role status (e.g. "enabled")
     * @param riskLevel     the role's risk level
     * @param businessUnit  the business unit
     * @param ownerId       the owner identity ID
     * @param stateAfter    enriched JSON snapshot of the created role
     * @return the immutable audit event
     */
    public static AccessRoleAuditEvent roleCreated(
            String actorId, String actorUsername, String tenantId,
            String roleId, String roleName, String roleType,
            String roleStatus, String riskLevel, String businessUnit,
            String ownerId, JsonNode stateAfter) {
        return new AccessRoleAuditEvent(
                actorId, actorUsername, tenantId,
                AccessRoleAuditConstants.ACTION_ROLE_CREATED,
                roleId, roleName, AccessRoleAuditConstants.ENTITY_TYPE,
                roleType, roleStatus, riskLevel, businessUnit, ownerId,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                null, stateAfter, Instant.now()
        );
    }

    /**
     * Creates an audit event for an updated role.
     *
     * @param actorId       the actor's identity ID
     * @param actorUsername  the actor's username
     * @param tenantId      the tenant identifier
     * @param roleId        the role ID
     * @param roleName      the role name (after update)
     * @param roleType      the role type (after update)
     * @param roleStatus    the role status (after update)
     * @param riskLevel     the role's risk level (after update)
     * @param businessUnit  the business unit (after update)
     * @param ownerId       the owner identity ID (after update)
     * @param stateBefore   JSON snapshot before the update
     * @param stateAfter    JSON snapshot after the update
     * @return the immutable audit event
     */
    public static AccessRoleAuditEvent roleUpdated(
            String actorId, String actorUsername, String tenantId,
            String roleId, String roleName, String roleType,
            String roleStatus, String riskLevel, String businessUnit,
            String ownerId, JsonNode stateBefore, JsonNode stateAfter) {
        return new AccessRoleAuditEvent(
                actorId, actorUsername, tenantId,
                AccessRoleAuditConstants.ACTION_ROLE_UPDATED,
                roleId, roleName, AccessRoleAuditConstants.ENTITY_TYPE,
                roleType, roleStatus, riskLevel, businessUnit, ownerId,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                stateBefore, stateAfter, Instant.now()
        );
    }

    /**
     * Creates an audit event for a deleted role.
     *
     * @param actorId       the actor's identity ID
     * @param actorUsername  the actor's username
     * @param tenantId      the tenant identifier
     * @param roleId        the deleted role's ID
     * @param roleName      the deleted role's name (captured before deletion)
     * @param roleType      the deleted role's type
     * @param riskLevel     the deleted role's risk level
     * @param businessUnit  the business unit
     * @param ownerId       the owner identity ID
     * @param stateBefore   JSON snapshot of the role before deletion
     * @return the immutable audit event (stateAfter is null since role no longer exists)
     */
    public static AccessRoleAuditEvent roleDeleted(
            String actorId, String actorUsername, String tenantId,
            String roleId, String roleName, String roleType,
            String riskLevel, String businessUnit, String ownerId,
            JsonNode stateBefore) {
        return new AccessRoleAuditEvent(
                actorId, actorUsername, tenantId,
                AccessRoleAuditConstants.ACTION_ROLE_DELETED,
                roleId, roleName, AccessRoleAuditConstants.ENTITY_TYPE,
                roleType, null, riskLevel, businessUnit, ownerId,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(),
                stateBefore, null, Instant.now()
        );
    }

    /**
     * Creates an audit event for a role's entitlement assignments being updated.
     *
     * <p>This represents the <strong>business-level</strong> audit event.
     * The frontend sends the desired final state of entitlements; the backend
     * computes what to add and remove. This single event captures both
     * additions and removals with full entity details.</p>
     *
     * @param actorId              the actor's identity ID
     * @param actorUsername         the actor's username
     * @param tenantId             the tenant identifier
     * @param roleId               the role ID
     * @param roleName             the role name
     * @param roleType             the role type
     * @param highestRisk          the highest risk level among all involved entitlements
     * @param addedEntitlements    entitlements that were newly linked
     * @param removedEntitlements  entitlements that were unlinked
     * @param stateBefore          full entitlement list (with names) before the sync
     * @param stateAfter           full entitlement list (with names) after the sync
     * @return the immutable audit event
     */
    public static AccessRoleAuditEvent entitlementsUpdated(
            String actorId, String actorUsername, String tenantId,
            String roleId, String roleName, String roleType,
            String highestRisk,
            List<EntitlementAuditDetail> addedEntitlements,
            List<EntitlementAuditDetail> removedEntitlements,
            JsonNode stateBefore, JsonNode stateAfter) {
        return new AccessRoleAuditEvent(
                actorId, actorUsername, tenantId,
                AccessRoleAuditConstants.ACTION_ENTITLEMENTS_UPDATED,
                roleId, roleName, AccessRoleAuditConstants.ENTITY_TYPE,
                roleType, null, highestRisk, null, null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                addedEntitlements, removedEntitlements,
                stateBefore, stateAfter, Instant.now()
        );
    }

    /**
     * Creates an audit event for a role's child composition being updated.
     *
     * <p>Captures added, removed, and failed child roles in one event.
     * Failed children include the validation reason (e.g. circular dependency).</p>
     *
     * @param actorId          the actor's identity ID
     * @param actorUsername     the actor's username
     * @param tenantId         the tenant identifier
     * @param roleId           the parent role ID
     * @param roleName         the parent role name
     * @param roleType         the parent role type
     * @param highestRisk      the highest risk level among all involved child roles
     * @param addedChildren    child roles that were successfully added
     * @param removedChildren  child roles that were removed
     * @param failedChildren   child roles that failed validation
     * @param stateBefore      full child role list (with names) before the sync
     * @param stateAfter       full child role list (with names) after the sync
     * @return the immutable audit event
     */
    public static AccessRoleAuditEvent childrenUpdated(
            String actorId, String actorUsername, String tenantId,
            String roleId, String roleName, String roleType,
            String highestRisk,
            List<ChildRoleAuditDetail> addedChildren,
            List<ChildRoleAuditDetail> removedChildren,
            List<FailedChildRoleAuditDetail> failedChildren,
            JsonNode stateBefore, JsonNode stateAfter) {
        return new AccessRoleAuditEvent(
                actorId, actorUsername, tenantId,
                AccessRoleAuditConstants.ACTION_CHILDREN_UPDATED,
                roleId, roleName, AccessRoleAuditConstants.ENTITY_TYPE,
                roleType, null, highestRisk, null, null,
                addedChildren, removedChildren, failedChildren,
                Collections.emptyList(), Collections.emptyList(),
                stateBefore, stateAfter, Instant.now()
        );
    }
}