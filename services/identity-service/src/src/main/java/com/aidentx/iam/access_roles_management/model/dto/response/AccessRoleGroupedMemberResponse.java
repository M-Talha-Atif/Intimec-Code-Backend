package com.aidentx.iam.access_roles_management.model.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * Lightweight role-member summary for UI list views.
 *
 * <p>Each row represents one active identity-role assignment and returns only
 * member-level data relevant to the role screen. Effective entitlements are not
 * expanded here; the UI can fetch them on demand from the dedicated member
 * entitlement drilldown endpoint.</p>
 */
public record AccessRoleGroupedMemberResponse(
        UUID assignmentId,
        UUID identityId,
        String identityName,
        String identityType,
        String identityRiskLevel,
        UUID roleId,
        String roleName,
        Long roleAssignedAt,
        String assignmentSourceType,
        int entitlementCount,
        int pendingEntitlementCount,
        List<String> childRoleNames,
        String assignmentPathSummary
) {
}

