package com.aidentx.iam.identity_management.model.dto;

import java.util.List;
import java.util.UUID;

/**
 * UI-friendly entitlement assignment row in an identity access breakdown.
 * Duplicate entitlement rows are intentionally preserved by assignmentId.
 */
public record IdentityAccessEntitlementDto(
        UUID assignmentId,
        UUID shadowId,
        String accountName,
        UUID resourceId,
        String applicationName,
        UUID entitlementId,
        String entitlementName,
        String description,
        String kind,
        String objectClass,
        String riskLevel,
        List<String> classification,
        String source,
        String accessPathType,
        UUID accessRoleId,
        String accessRoleName,
        UUID sourceRoleId,
        String sourceRoleName,
        Boolean roleEntitlementDirect,
        Integer roleEntitlementDepth,
        List<IdentityEntitlementRolePathNodeDto> rolePath,
        String assignmentStatus,
        String assignmentStatusReason,
        Long assignedAt,
        String assignedByUserId,
        String assignedByUserName,
        Long updatedAt
) {
}

