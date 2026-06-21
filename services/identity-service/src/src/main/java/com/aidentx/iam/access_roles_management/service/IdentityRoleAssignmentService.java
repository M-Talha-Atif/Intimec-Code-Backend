package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Defines safe lifecycle operations for identity-to-access-role assignments.
 *
 * <p>Implementations must be idempotent: assigning an already-active role from
 * the same source returns the existing assignment; assigning a previously
 * revoked row reactivates it; revocation is source-aware and only touches the
 * matching source row.</p>
 */
public interface IdentityRoleAssignmentService {

    /**
     * Assigns a role to an identity from the given source.
     *
     * @param tenantId tenant identifier
     * @param identityId identity receiving the role
     * @param roleId access role to assign
     * @param sourceType source of assignment
     * @param assignmentRuleId optional dynamic access-role assignment rule ID
     * @return active assignment row
     */
    IdentityRoleAssignment assignRole(String tenantId,
                                      UUID identityId,
                                      UUID roleId,
                                      IdentityRoleAssignment.SourceType sourceType,
                                      UUID assignmentRuleId,
                                      String assignedByUserId,
                                      String assignedByUserName);

    default IdentityRoleAssignment assignRole(String tenantId,
                                             UUID identityId,
                                             UUID roleId,
                                             IdentityRoleAssignment.SourceType sourceType,
                                             UUID assignmentRuleId) {
        return assignRole(tenantId, identityId, roleId, sourceType, assignmentRuleId, null, null);
    }

    /**
     * Assigns a role to an identity using an explicit source key.
     *
     * @param tenantId tenant identifier
     * @param identityId identity receiving the role
     * @param roleId access role to assign
     * @param sourceType source of assignment
     * @param assignmentRuleId optional source rule ID
     * @param sourceKey stable source discriminator for uniqueness
     * @return active assignment row
     */
    IdentityRoleAssignment assignRole(String tenantId,
                                      UUID identityId,
                                      UUID roleId,
                                      IdentityRoleAssignment.SourceType sourceType,
                                      UUID assignmentRuleId,
                                      String sourceKey,
                                      String assignedByUserId,
                                      String assignedByUserName);

    default IdentityRoleAssignment assignRole(String tenantId,
                                             UUID identityId,
                                             UUID roleId,
                                             IdentityRoleAssignment.SourceType sourceType,
                                             UUID assignmentRuleId,
                                             String sourceKey) {
        return assignRole(tenantId, identityId, roleId, sourceType, assignmentRuleId, sourceKey, null, null);
    }

    /**
     * Source-aware revoke for a role assignment.
     *
     * @param tenantId tenant identifier
     * @param identityId identity losing the role
     * @param roleId access role to revoke
     * @param sourceType source of assignment to revoke
     * @param assignmentRuleId optional source rule ID
     * @return revoked assignment if a matching active row existed
     */
    Optional<IdentityRoleAssignment> revokeRole(String tenantId,
                                                UUID identityId,
                                                UUID roleId,
                                                IdentityRoleAssignment.SourceType sourceType,
                                                UUID assignmentRuleId);

    /** Revoke using an explicit source key. */
    Optional<IdentityRoleAssignment> revokeRole(String tenantId,
                                                UUID identityId,
                                                UUID roleId,
                                                IdentityRoleAssignment.SourceType sourceType,
                                                UUID assignmentRuleId,
                                                String sourceKey);

    /** Lists active assignments for an identity. */
    List<IdentityRoleAssignment> findActiveAssignmentsForIdentity(String tenantId, UUID identityId);

    /** Lists active assignments for a role. */
    List<IdentityRoleAssignment> findActiveAssignmentsForRole(String tenantId, UUID roleId);

    /** Lists active assignments created by a dynamic access-role assignment rule. */
    List<IdentityRoleAssignment> findActiveAssignmentsByRule(String tenantId, UUID assignmentRuleId);
}

