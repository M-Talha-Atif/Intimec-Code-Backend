package com.aidentx.iam.access_roles_management.model.dto.response;

import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipDirection;
import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipMode;

import java.util.List;
import java.util.UUID;

/** Unified response for direct, hierarchy, and conflict role relationships. */
public record AccessRoleRelationshipResponse(
        UUID roleId,
        String tenantId,
        RoleRelationshipMode mode,
        RoleRelationshipDirection direction,
        int maxDepth,
        RoleSummary rootRole,
        List<RelationshipEntitlement> rootEntitlements,
        List<RoleRelationshipNode> childRoles,
        List<RoleRelationshipNode> parentRoles,
        List<ConflictEntitlement> conflicts,
        Summary summary
) {
    public record RoleSummary(
            UUID id,
            String name,
            String displayName,
            String type,
            String status,
            String riskLevel,
            long entitlementCount
    ) {
    }

    public record RoleRelationshipNode(
            RoleSummary role,
            int depth,
            List<UUID> pathRoleIds,
            List<RelationshipEntitlement> entitlements,
            List<RoleRelationshipNode> childRoles,
            List<RoleRelationshipNode> parentRoles
    ) {
    }

    public record RelationshipEntitlement(
            UUID entitlementId,
            String displayName,
            String description,
            UUID applicationId,
            String sourceUid,
            String objectClass,
            String kind,
            String lifecycle,
            String riskLevel,
            String privilegeLevel,
            boolean conflict,
            String conflictReason,
            List<UUID> conflictRoleIds
    ) {
    }

    public record ConflictEntitlement(
            RelationshipEntitlement entitlement,
            List<RoleSummary> roles,
            int occurrenceCount,
            String conflictReason
    ) {
    }

    public record Summary(
            int directChildCount,
            int directParentCount,
            int totalChildRoleCount,
            int totalParentRoleCount,
            int totalRoleCount,
            int totalEntitlementCount,
            int conflictEntitlementCount
    ) {
    }
}

