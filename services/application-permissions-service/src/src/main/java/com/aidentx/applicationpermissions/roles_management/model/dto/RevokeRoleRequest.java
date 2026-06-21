package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for revoking roles from multiple users.
 * 
 * @param userIds List of user IDs to revoke the role from (must not be null or empty)
 * @param roleId Role ID to revoke from the users (must not be null)
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public record RevokeRoleRequest(
        @NotNull(message = AppConstants.VALIDATION_USER_IDS_CANNOT_BE_NULL)
    @NotEmpty(message = AppConstants.VALIDATION_USER_IDS_LIST_CANNOT_BE_EMPTY)
    List<UUID> userIds,

    @NotNull(message = AppConstants.VALIDATION_ROLE_ID_CANNOT_BE_NULL)
    UUID roleId
) {
    /**
     * Checks if this is a bulk revocation request (multiple users).
     * 
     * @return true if more than one user ID is provided
     */
    public boolean isBulkRevocation() {
        return userIds != null && userIds.size() > 1;
    }
    
    /**
     * Gets the count of users in this revocation request.
     * 
     * @return number of users to revoke the role from
     */
    public int getUserCount() {
        return userIds != null ? userIds.size() : 0;
    }
}