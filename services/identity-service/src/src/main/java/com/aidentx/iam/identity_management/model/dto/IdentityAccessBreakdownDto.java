package com.aidentx.iam.identity_management.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * Grouped UI response showing how an identity has every entitlement assignment.
 */
public record IdentityAccessBreakdownDto(
        UUID identityId,
        String search,
        long totalAssignments,
        long totalRoleEntitlements,
        long totalStandaloneEntitlements,
        List<IdentityAccessRoleGroupDto> businessRoles,
        List<IdentityAccessEntitlementDto> standaloneEntitlements
) {
}

