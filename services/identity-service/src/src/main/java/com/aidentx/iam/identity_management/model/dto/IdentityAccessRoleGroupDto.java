package com.aidentx.iam.identity_management.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * Business/access role group in an identity access breakdown.
 */
public record IdentityAccessRoleGroupDto(
		UUID roleId,
		String roleName,
		String roleType,
		String roleStatus,
		String riskLevel,
		String businessUnit,
		String description,
		String assignmentSource,
		Long assignedAt,
		String assignedByUserId,
		String assignedByUserName,
		long entitlementCount,
		List<IdentityAccessEntitlementDto> entitlements
) {
}

