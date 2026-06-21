package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.dto.response.ManualRoleMemberGrantResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncManualMembersResultDto;

import java.util.List;
import java.util.UUID;

/**
 * Coordinates bulk manual member grants and revocations for access roles.
 */
public interface AccessRoleManualAssignmentService {

    /**
     * Grants a role manually to the supplied identities.
     *
     * @param tenantId tenant identifier
     * @param roleId target access-role ID
     * @param identityIds identities to grant manually
     * @param assignedByUserId actor/user ID that performed the grant, when known
     * @param assignedByUserName actor username/name that performed the grant, when known
     * @return bulk grant outcome with added, skipped, and failed members
     */
    ManualRoleMemberGrantResultDto grantRoleToMembers(String tenantId,
                                                      UUID roleId,
                                                      List<UUID> identityIds,
                                                      String assignedByUserId,
                                                      String assignedByUserName);

    default ManualRoleMemberGrantResultDto grantRoleToMembers(String tenantId, UUID roleId, List<UUID> identityIds) {
        return grantRoleToMembers(tenantId, roleId, identityIds, null, null);
    }

    /**
     * Synchronizes the manual member assignments for a role to the desired state.
     *
     * <p>The caller sends the complete desired list of identity IDs that should hold
     * a MANUAL assignment for this role. The service computes the diff against
     * current MANUAL assignments and applies grants/revocations accordingly.</p>
     *
     * <p>For each identity added or revoked, the service also logs which entitlements
     * were provisioned or deprovisioned and whether those entitlements already existed
     * in the {@code entitlement_assignments} table.</p>
     *
     * @param tenantId           tenant identifier
     * @param roleId             target access-role ID
     * @param desiredIdentityIds desired set of identity IDs that should hold the role
     * @param assignedByUserId actor/user ID that performed added grants, when known
     * @param assignedByUserName actor username/name that performed added grants, when known
     * @return sync outcome with added, revoked, skipped, failed members and entitlement impact
     */
    SyncManualMembersResultDto syncManualMembers(String tenantId,
                                                 UUID roleId,
                                                 List<UUID> desiredIdentityIds,
                                                 String assignedByUserId,
                                                 String assignedByUserName);

    default SyncManualMembersResultDto syncManualMembers(String tenantId, UUID roleId, List<UUID> desiredIdentityIds) {
        return syncManualMembers(tenantId, roleId, desiredIdentityIds, null, null);
    }
}

