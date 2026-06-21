package com.aidentx.iam.role_management.model.dto;

import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;
import java.util.UUID;

public final class RoleDtos {

    /** DTO for creating a new business role. */
    public record RoleCreateDto(
        @NotBlank String tenantId,
        @NotBlank String displayName,
        String description
    ) {}

    /** DTO for updating a role's metadata or entitlement composition. */
    public record RoleUpdateDto(
        String displayName,
        String description,
        Set<UUID> entitlementIds // A list of Entitlement UUIDs to associate with this role
    ) {}
    
    /** DTO for returning full role details via the API. */
    public record RoleResponseDto(
        UUID id,
        String tenantId,
        String displayName,
        String description,
        Set<EntitlementDtos.EntitlementResponseDto> includedEntitlements,
        Long createdAt,
        Long updatedAt
    ) {}
}