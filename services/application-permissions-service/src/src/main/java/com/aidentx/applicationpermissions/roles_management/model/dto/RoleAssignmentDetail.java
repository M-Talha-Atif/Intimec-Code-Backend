package com.aidentx.applicationpermissions.roles_management.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO representing role assignment details for cart-based assignments.
 * Contains the list of users to assign the role to, along with role-specific justification and validity.
 *
 * <p>This is used as the value in the roleUserMap where the key is the roleId.</p>
 *
 * @param userIds list of user IDs to assign this role to (must not be empty)
 * @param justification role-specific justification for this assignment (optional, max 500 chars)
 * @param validityDays number of days this role assignment should remain valid (optional, overrides overall validity)
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public record RoleAssignmentDetail(
        @NotEmpty(message = "User IDs list cannot be empty for a role")
        List<UUID> userIds,

        @Size(max = 500, message = "Justification must not exceed 500 characters")
        String justification,

        Integer validityDays
) {
    /**
     * Gets the number of users in this role assignment.
     *
     * @return count of users
     */
    public int getUserCount() {
        return userIds != null ? userIds.size() : 0;
    }

    /**
     * Checks if role-specific justification is provided.
     *
     * @return true if justification is not null and not empty
     */
    public boolean hasJustification() {
        return justification != null && !justification.trim().isEmpty();
    }

    /**
     * Checks if role-specific validity days is provided.
     *
     * @return true if validityDays is not null
     */
    public boolean hasValidityDays() {
        return validityDays != null;
    }
}