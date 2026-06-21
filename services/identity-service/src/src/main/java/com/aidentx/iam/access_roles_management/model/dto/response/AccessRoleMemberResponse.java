package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Enriched member view for an identity holding an access role.
 *
 * <p>Rows are emitted per active identity-role assignment and effective role
 * entitlement. If a role currently has no effective entitlements, the endpoint
 * still returns the member assignment with entitlement/application/path fields
 * set to {@code null} or {@code NO_ENTITLEMENT}.</p>
 */
public record AccessRoleMemberResponse(
        UUID assignmentId,
        UUID identityId,
        String identityName,
        String identityType,
        String identityRiskLevel,
        UUID roleId,
        String roleName,
        Long roleAssignedAt,
        UUID entitlementId,
        String entitlementName,
        UUID applicationId,
        String applicationName,
        String accessPathType,
        UUID sourceRoleId,
        String sourceRoleName,
        UUID childRoleId,
        String childRoleName,
        Boolean roleEntitlementDirect,
        Integer roleEntitlementDepth,
        List<RolePathNode> rolePath,
        String assignmentSourceType
) {

    /** One role node in the direct/inherited role-entitlement path. */
    public record RolePathNode(
            UUID roleId,
            String roleName,
            Integer depth
    ) {
    }
}

