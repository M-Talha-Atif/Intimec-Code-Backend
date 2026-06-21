package com.aidentx.applicationpermissions.roles_management.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user-role assignment information.
 * 
 * <p>Contains the complete information about a user-role assignment
 * including identifiers and role metadata. Used in API responses to
 * provide detailed information about role assignments.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "User role assignment information")
public record UserRoleResponse(

    @Schema(description = "The unique identifier of the user-role assignment", example = "789e1234-e89b-12d3-a456-426614174002")
    UUID id,

    @Schema(description = "The ID of the user", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID userId,

    @Schema(description = "The ID of the role", example = "456e7890-e89b-12d3-a456-426614174001")
    UUID roleId,

    @Schema(description = "The tenant identifier", example = "tenant123")
    String tenantId,

    @Schema(description = "The name of the role", example = "Administrator")
    String roleName,

    @Schema(description = "The description of the role", example = "Full system administrator access")
    String roleDescription,

    @Schema(description = "Whether the role is active", example = "true")
    Boolean roleActive,

    @Schema(description = "When the role assignment was created", example = "2024-03-15T10:30:00")
    LocalDateTime assignedAt
) {
    
    /**
     * Creates a new UserRoleResponse.
     * 
     * @param id the unique identifier of the user-role assignment
     * @param userId the ID of the user
     * @param roleId the ID of the role
     * @param tenantId the tenant identifier
     * @param roleName the name of the role
     * @param roleDescription the description of the role
     * @param roleActive whether the role is active
     * @param assignedAt when the role assignment was created
     */
    public UserRoleResponse {
        // Record validation is handled by caller
    }
}