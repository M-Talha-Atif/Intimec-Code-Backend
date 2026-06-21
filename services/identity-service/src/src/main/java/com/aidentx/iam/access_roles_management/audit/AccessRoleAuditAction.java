package com.aidentx.iam.access_roles_management.audit;

/**
 * Enumeration of all auditable access role operations.
 *
 * <p>Each value maps to a DB action constant in {@link AccessRoleAuditConstants}.
 * Used as the {@code action} parameter in the {@link AuditAccessRoleChange}
 * annotation to declare which operation a service method performs.</p>
 *
 * <h3>Design Note</h3>
 * <p>There are only <strong>5 audit actions</strong>, not 7. Individual
 * add/remove operations for entitlements and children do NOT have their
 * own audit actions. Instead:</p>
 * <ul>
 *   <li>{@link #ENTITLEMENTS_UPDATED} covers both adds and removes in a single event</li>
 *   <li>{@link #CHILDREN_UPDATED} covers both adds and removes in a single event</li>
 *   <li>During role creation, entitlement/children linking is captured inside the
 *       {@link #ROLE_CREATED} event's stateAfter</li>
 * </ul>
 * <p>This ensures <strong>one user action = one audit event</strong>.</p>
 */
public enum AccessRoleAuditAction {

    /**
     * A new access role was created.
     * <p>The stateAfter includes linked entitlements and child roles
     * with their names and risk levels.</p>
     */
    ROLE_CREATED(AccessRoleAuditConstants.ACTION_ROLE_CREATED),

    /**
     * An existing access role's governance metadata was updated.
     * <p>Only fires if the role's fields actually changed.</p>
     */
    ROLE_UPDATED(AccessRoleAuditConstants.ACTION_ROLE_UPDATED),

    /**
     * An access role was permanently deleted.
     * <p>All related links (entitlements, children, parent references)
     * are removed as part of the deletion.</p>
     */
    ROLE_DELETED(AccessRoleAuditConstants.ACTION_ROLE_DELETED),

    /**
     * A role's entitlement assignments were updated via the sync endpoint.
     * <p>The event includes both added and removed entitlements with
     * their display names and risk levels. The backend computes the diff
     * from the desired state sent by the frontend.</p>
     */
    ENTITLEMENTS_UPDATED(AccessRoleAuditConstants.ACTION_ENTITLEMENTS_UPDATED),

    /**
     * A role's child role composition was updated via the sync endpoint.
     * <p>The event includes added, removed, and failed child roles with
     * their names, types, risk levels, and failure reasons.</p>
     */
    CHILDREN_UPDATED(AccessRoleAuditConstants.ACTION_CHILDREN_UPDATED);

    private final String dbAction;

    AccessRoleAuditAction(String dbAction) {
        this.dbAction = dbAction;
    }

    /**
     * Returns the DB action constant string for persistence and Kafka events.
     *
     * @return the action string (e.g. "ROLE_CREATED", "ENTITLEMENTS_UPDATED")
     */
    public String getDbAction() {
        return dbAction;
    }
}