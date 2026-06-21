package com.aidentx.iam.access_roles_management.model.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UI response for previewing the impact of saving access-role edits.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccessRoleSaveSimulationResponse(
        UUID roleId,
        String roleName,
        String simulationType,
        int impactScore,
        String impactSeverity,
        ImpactScoreDetails impactScoreDetails,
        Summary summary,
        List<IdentityImpact> affectedIdentities,
        List<EntitlementImpact> affectedEntitlements
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Summary(
            long totalAffectedUsers,
            long usersAddedToRole,
            long usersRemovedFromRole,
            long addedEntitlements,
            long removedEntitlements,
            Long unchangedEntitlements,
            long directEntitlementChanges,
            long childRoleChanges
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IdentityImpact(
            UUID identityId,
            String identityName,
            String identityType,
            String riskLevel,
            String overallImpactType,
            List<String> impactTypes,
            RoleImpact roleImpact,
            List<ChildRoleImpact> childRoleImpacts,
            List<EntitlementImpact> entitlementImpacts
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RoleImpact(
            UUID roleId,
            String roleName,
            String impactType,
            String status,
            boolean currentlyAssigned,
            boolean willBeAssigned
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChildRoleImpact(
            UUID childRoleId,
            String childRoleName,
            String impactType,
            String status,
            boolean currentlyInherited,
            boolean willBeInherited
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record EntitlementImpact(
            UUID identityId,
            String identityName,
            UUID entitlementId,
            String entitlementName,
            UUID applicationId,
            String applicationName,
            String impactType,
            String status,
            String source,
            String assignmentStatus,
            boolean currentlyAssigned,
            boolean willBeAssigned,
            String riskLevel,
            UUID viaRoleId,
            String viaRoleName,
            UUID viaChildRoleId,
            String viaChildRoleName,
            List<RolePathNode> rolePath
    ) {
    }

    public record RolePathNode(
            UUID roleId,
            String roleName,
            Integer depth
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ImpactScoreDetails(
            String formula,
            Integer cappedAt,
            Map<String, Integer> weights,
            Map<String, Long> inputs,
            Map<String, Long> contributions,
            String riskLevelUsed,
            long riskBoost,
            long rawScore
    ) {
    }
}

