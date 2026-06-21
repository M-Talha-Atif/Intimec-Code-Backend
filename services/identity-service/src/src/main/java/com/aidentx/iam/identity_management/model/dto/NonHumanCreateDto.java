package com.aidentx.iam.identity_management.model.dto;

import java.util.Map;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for creating a new Non-Human Identity.
 */
public record NonHumanCreateDto(
        @NotBlank String serviceName,
        String description,
        String ownerTeam,
        IdentityStatus status,
        Map<String, Object> extendedAttributes) {
}