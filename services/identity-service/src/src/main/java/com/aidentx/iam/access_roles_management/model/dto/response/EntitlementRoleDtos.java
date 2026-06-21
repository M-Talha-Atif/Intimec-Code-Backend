package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * DTOs for entitlement-centric role assignment APIs.
 */
public final class EntitlementRoleDtos {

    private EntitlementRoleDtos() {}

    /** Role row returned by GET /api/v1/entitlements/{entitlementId}/roles. */
    public record EntitlementRoleResponse(
            UUID linkId,
            UUID entitlementId,
            UUID roleId,
            String roleName,
            String roleDisplayName,
            String roleType,
            String roleStatus,
            String roleRiskLevel,
            String businessUnit,
            Long assignedAt
    ) {}

    /** Mutation summary returned by POST/DELETE/PUT entitlement roles endpoints. */
    public record EntitlementRoleAssignmentResponse(
            UUID entitlementId,
            int requestedCount,
            int uniqueRequestedCount,
            int addedCount,
            int removedCount,
            int unchangedCount,
            int skippedAlreadyAssignedCount,
            int skippedNotAssignedCount,
            List<UUID> addedRoleIds,
            List<UUID> removedRoleIds,
            List<UUID> unchangedRoleIds,
            List<UUID> skippedRoleIds,
            List<UUID> roleIds
    ) {}
}
