package com.aidentx.iam.access_roles_management.model.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Paginated comparison of overlapping effective entitlements between two access roles.
 */
public record AccessRoleEntitlementComparisonResponse(
        ComparedRole roleA,
        ComparedRole roleB,
        String sourceFilter,
        long totalOverlappingEntitlements,
        Page<ComparedEntitlement> entitlements
) {

    /**
     * Lightweight role metadata for the compared roles.
     */
    public record ComparedRole(
            UUID roleId,
            String name,
            String displayName,
            String type,
            String riskLevel
    ) {
    }

    /**
     * One entitlement that exists effectively in both compared roles.
     */
    public record ComparedEntitlement(
            UUID entitlementId,
            String displayName,
            String description,
            UUID applicationId,
            String applicationName,
            String sourceUid,
            String objectClass,
            String kind,
            String lifecycle,
            String riskLevel,
            String riskReason,
            String identityType,
            String privilegeLevel,
            RoleEntitlementPresence roleA,
            RoleEntitlementPresence roleB
    ) {
    }

    /**
     * How one role receives the overlapping entitlement.
     */
    public record RoleEntitlementPresence(
            boolean direct,
            boolean inherited,
            List<String> accessTypes,
            long sourceRoleCount
    ) {
    }
}

