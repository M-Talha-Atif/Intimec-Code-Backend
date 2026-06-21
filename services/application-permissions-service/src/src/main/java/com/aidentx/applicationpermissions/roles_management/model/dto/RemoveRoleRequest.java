package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for removing a role from a user.
 * 
 * <p>Contains the necessary information to remove a user-role assignment
 * within the tenant context. The tenant ID is automatically extracted
 * from the JWT token for security and multi-tenancy compliance.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Request to remove a role from a user")
public record RemoveRoleRequest(

    @Schema(description = "The ID of the user to remove the role from", example = "123e4567-e89b-12d3-a456-426614174000")
    @NotNull(message = AppConstants.VALIDATION_USER_ID_REQUIRED)
    UUID userId,

    @Schema(description = "The ID of the role to remove from the user", example = "456e7890-e89b-12d3-a456-426614174001")
    @NotNull(message = AppConstants.VALIDATION_ROLE_ID_REQUIRED)
    UUID roleId
) {
    
    /**
     * Creates a new RemoveRoleRequest.
     * 
     * @param userId the ID of the user to remove the role from
     * @param roleId the ID of the role to remove from the user
     */
    public RemoveRoleRequest {
        // Record validation is handled by annotations
    }
}