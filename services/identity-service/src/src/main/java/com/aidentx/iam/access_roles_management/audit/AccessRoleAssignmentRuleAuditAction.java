package com.aidentx.iam.access_roles_management.audit;

/**
 * Enumerates auditable access-role assignment rule CRUD operations.
 *
 * <p>Each enum value maps to one persisted raw action constant and one
 * UI-friendly activity label resolved through
 * {@link AccessRoleAssignmentRuleAuditConstants#toActivityLabel(String, String)}.</p>
 */
public enum AccessRoleAssignmentRuleAuditAction {

    /** A new access-role assignment rule was created. */
    RULE_CREATED(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_CREATED),

    /** An existing access-role assignment rule was updated. */
    RULE_UPDATED(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_UPDATED),

    /** An existing access-role assignment rule was deleted. */
    RULE_DELETED(AccessRoleAssignmentRuleAuditConstants.ACTION_RULE_DELETED);

    private final String dbAction;

    AccessRoleAssignmentRuleAuditAction(String dbAction) {
        this.dbAction = dbAction;
    }

    /**
     * Returns the raw DB action constant.
     *
     * @return persisted action string
     */
    public String getDbAction() {
        return dbAction;
    }
}
