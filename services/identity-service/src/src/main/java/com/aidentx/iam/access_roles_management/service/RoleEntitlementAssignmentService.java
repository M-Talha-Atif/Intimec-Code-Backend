package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;

import java.util.UUID;

/**
 * Applies and revokes effective entitlements implied by an identity's access-role assignment.
 */
public interface RoleEntitlementAssignmentService {

    /**
     * Ensures all effective entitlements of the role are assigned to the identity's
     * linked account shadows. Role-derived entitlement rows are stamped with
     * {@code entitlement_assignments.source = "ROLE_DERIVED"} so they can coexist
     * with direct/manual/import rows ({@code "DIRECT"}) and entitlement-rule rows
     * ({@code "DYNAMIC"}) for the same identity and entitlement.
     */
    void applyRoleEntitlements(String tenantId,
                               UUID identityId,
                               UUID roleId,
                               UUID assignmentRuleId,
                               IdentityRoleAssignment.SourceType sourceType,
                               String assignedByUserId,
                               String assignedByUserName);

    default void applyRoleEntitlements(String tenantId,
                                       UUID identityId,
                                       UUID roleId,
                                       UUID assignmentRuleId,
                                       IdentityRoleAssignment.SourceType sourceType) {
        applyRoleEntitlements(tenantId, identityId, roleId, assignmentRuleId, sourceType, null, null);
    }

    /** Revokes role-derived entitlements that are no longer required after a role assignment is removed. */
    void revokeRoleEntitlements(String tenantId, UUID identityId, UUID roleId, UUID assignmentRuleId);
}
