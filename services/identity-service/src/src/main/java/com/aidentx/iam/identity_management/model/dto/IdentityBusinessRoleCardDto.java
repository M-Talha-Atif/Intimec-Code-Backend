package com.aidentx.iam.identity_management.model.dto;

import java.util.UUID;

/**
 * Lightweight card DTO representing a business/access role assigned to an identity.
 * <p>
 * This DTO is intentionally compact — it carries only the fields a role-card
 * widget in the identity detail screen needs for display and navigation.
 *
 * @param roleId           UUID of the access role
 * @param roleName         display-friendly name of the role
 * @param roleType         classification type (business, technical, functional, application)
 * @param roleStatus       lifecycle status (draft, active, deprecated, etc.)
 * @param riskLevel        governance risk level (low, medium, high, critical)
 * @param businessUnit     business unit the role belongs to
 * @param description      short description of the role
 * @param assignmentSource how the identity received this role (RULE, MANUAL, API, etc.)
 * @param assignedAt       epoch millis when the role was assigned to this identity
 */
public record IdentityBusinessRoleCardDto(
        UUID roleId,
        String roleName,
        String roleType,
        String roleStatus,
        String riskLevel,
        String businessUnit,
        String description,
        String assignmentSource,
        Long assignedAt
) {
}

