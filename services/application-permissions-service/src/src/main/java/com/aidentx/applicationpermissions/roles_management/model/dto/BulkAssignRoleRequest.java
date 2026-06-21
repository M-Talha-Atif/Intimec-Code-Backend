package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for assigning a role to multiple users in bulk.
 * 
 * <p>Contains the necessary information to create multiple user-role assignments
 * within the tenant context. The tenant ID is automatically extracted
 * from the JWT token for security and multi-tenancy compliance.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Request to assign a role to multiple users")
public record BulkAssignRoleRequest(

    @Schema(description = "The IDs of the users to assign the role to", example = "[\"123e4567-e89b-12d3-a456-426614174000\", \"234e5678-e89b-12d3-a456-426614174001\"]")
    @NotNull(message = AppConstants.VALIDATION_USER_IDS_LIST_REQUIRED)
    @NotEmpty(message = AppConstants.VALIDATION_USER_IDS_LIST_CANNOT_BE_EMPTY)
    @Size(max = 100, message = AppConstants.VALIDATION_MAXIMUM_100_USERS_CAN_BE_ASSIGNED_AT_ONCE)
    List<UUID> userIds,

    @Schema(description = "The ID of the role to assign to the users", example = "456e7890-e89b-12d3-a456-426614174001")
    @NotNull(message = AppConstants.VALIDATION_ROLE_ID_REQUIRED)
    UUID roleId
) {
    
    /**
     * Creates a new BulkAssignRoleRequest.
     * 
     * @param userIds the IDs of the users to assign the role to
     * @param roleId the ID of the role to assign to the users
     */
    public BulkAssignRoleRequest {
        // Record validation is handled by annotations
        // Additional validation: ensure no duplicate user IDs
        if (userIds != null && userIds.size() != userIds.stream().distinct().count()) {
            throw new IllegalArgumentException(AppConstants.ERROR_DUPLICATE_USER_IDS_NOT_ALLOWED);
        }
    }
}