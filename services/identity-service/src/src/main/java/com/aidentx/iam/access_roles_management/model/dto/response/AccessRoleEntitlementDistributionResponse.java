package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for role entitlement distribution analytics.
 *
 * <p>Counts are unique by entitlement ID by default. If the same entitlement is
 * inherited through multiple child roles, it is counted once in effective totals,
 * while each child/source role still shows its own contribution in the child-role
 * breakdown.</p>
 */
public record AccessRoleEntitlementDistributionResponse(
        UUID roleId,
        String roleName,
        String roleDisplayName,
        String tenantId,
        Summary summary,
        DistributionSection direct,
        DistributionSection inheritedFromChildRoles,
        DistributionSection effectiveTotal,
        List<ChildRoleBreakdown> childRoleBreakdown
) {

    public record Summary(
            int directEntitlementCount,
            int childRoleEntitlementCount,
            int effectiveEntitlementCount,
            int directApplicationCount,
            int childRoleApplicationCount,
            int effectiveApplicationCount,
            int childRoleCount,
            long assignedUserCount
    ) {}

    public record DistributionSection(
            int entitlementCount,
            int applicationCount,
            List<PrivilegeLevelDistribution> privilegeLevelDistribution,
            List<ApplicationDistribution> applicationDistribution
    ) {}

    public record PrivilegeLevelDistribution(
            String privilegeLevel,
            long count,
            double percentage
    ) {}

    public record ApplicationDistribution(
            UUID applicationId,
            String applicationName,
            long entitlementCount
    ) {}

    public record ChildRoleBreakdown(
            UUID childRoleId,
            String childRoleName,
            String childRoleDisplayName,
            int entitlementCount,
            int applicationCount,
            List<PrivilegeLevelDistribution> privilegeLevelDistribution
    ) {}
}
