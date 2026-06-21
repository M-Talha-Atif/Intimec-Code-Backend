package com.aidentx.iam.identity_management.model.dto;

import java.util.Map;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;

/**
 * DTO for updating an existing Non-Human Identity.
 * All fields are optional since updates may only modify specific attributes.
 */
public record NonHumanUpdateDto(
        String serviceName,
        String description,
        String ownerTeam,
        IdentityStatus status,
        Map<String, Object> extendedAttributes) {
}
