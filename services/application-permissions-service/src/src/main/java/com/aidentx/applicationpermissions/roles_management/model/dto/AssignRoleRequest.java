package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for assigning a role to one or more users.
 * 
 * <p>Contains the necessary information to create user-role assignments
 * within the tenant context. Supports both single user assignment (userId)
 * and bulk assignment (userIds). The tenant ID is automatically extracted
 * from the JWT token for security and multi-tenancy compliance.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Request to assign a role to one or more users")
public record AssignRoleRequest(

    @Schema(description = "The ID of the user to assign the role to (for single assignment)", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID userId,

    @Schema(description = "The IDs of the users to assign the role to (for bulk assignment)", example = "[\"123e4567-e89b-12d3-a456-426614174000\", \"234e5678-e89b-12d3-a456-426614174001\"]")
    @Size(max = 100, message = AppConstants.VALIDATION_MAXIMUM_100_USERS_CAN_BE_ASSIGNED_AT_ONCE)
    List<UUID> userIds,

    @Schema(description = "The ID of the role to assign to the user(s)", example = "456e7890-e89b-12d3-a456-426614174001")
    @NotNull(message = AppConstants.VALIDATION_ROLE_ID_REQUIRED)
    UUID roleId,

    @Schema(description = "Whether to send notification to users upon successful role assignment", example = "true", defaultValue = "true")
    Boolean sendNotificationOnAssignment,

    @Schema(description = "Whether manager approval is required before role assignment (not implemented yet)", example = "false", defaultValue = "false")
    Boolean requireApprovalBeforeAssignment
) {
    
    /**
     * Creates a new AssignRoleRequest.
     * 
     * @param userId the ID of the user to assign the role to (for single assignment)
     * @param userIds the IDs of the users to assign the role to (for bulk assignment)
     * @param roleId the ID of the role to assign to the user(s)
     * @param sendNotificationOnAssignment whether to send notification to users upon successful role assignment (defaults to true if null)
     * @param requireApprovalBeforeAssignment whether manager approval is required before role assignment (not implemented yet, ignored)
     */
    public AssignRoleRequest {
        // Validation: exactly one of userId or userIds must be provided
        if ((userId == null && (userIds == null || userIds.isEmpty())) ||
            (userId != null && userIds != null && !userIds.isEmpty())) {
            throw new IllegalArgumentException(AppConstants.ERROR_EITHER_USER_ID_OR_USER_IDS_MUST_BE_PROVIDED);
        }
        
        // Additional validation for userIds: ensure no duplicates
        if (userIds != null && !userIds.isEmpty() && userIds.size() != userIds.stream().distinct().count()) {
            throw new IllegalArgumentException(AppConstants.ERROR_DUPLICATE_USER_IDS_NOT_ALLOWED);
        }
    }
    
    /**
     * Checks if this is a bulk assignment request.
     * 
     * @return true if userIds is provided, false if userId is provided
     */
    public boolean isBulkAssignment() {
        return userIds != null && !userIds.isEmpty();
    }
    
    /**
     * Gets all user IDs as a list (whether single or bulk).
     * 
     * @return list containing either the single userId or all userIds
     */
    public List<UUID> getAllUserIds() {
        return isBulkAssignment() ? userIds : List.of(userId);
    }
    
    /**
     * Checks if notifications should be sent upon successful role assignment.
     * Defaults to true if sendNotificationOnAssignment is null.
     * 
     * @return true if notifications should be sent, false otherwise
     */
    public boolean shouldSendNotification() {
        return sendNotificationOnAssignment == null || sendNotificationOnAssignment;
    }
}