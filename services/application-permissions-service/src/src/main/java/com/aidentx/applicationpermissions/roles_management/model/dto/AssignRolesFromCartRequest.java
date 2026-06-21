package com.aidentx.applicationpermissions.roles_management.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Request DTO for assigning multiple roles to multiple users from access portal cart.
 *
 * <p>This represents a cart-based assignment where each role can have different users,
 * different justifications, and different validity periods. The structure mirrors the
 * access portal cart where users can request multiple roles for different users.</p>
 *
 * <p><b>Example Request:</b></p>
 * <pre>
 * {
 *   "roleUserMap": {
 *     "role-uuid-1": {
 *       "userIds": ["user-1", "user-2", "user-3"],
 *       "justification": "Team needs admin access",
 *       "validityDays": 30
 *     },
 *     "role-uuid-2": {
 *       "userIds": ["user-1", "user-4"],
 *       "justification": "Required for project",
 *       "validityDays": 60
 *     }
 *   },
 *   "overallJustification": "Q1 team setup",
 *   "overallValidityDays": 90,
 *   "shouldSendNotification": true
 * }
 * </pre>
 *
 * @param roleUserMap map of role IDs to assignment details (users, justification, validity)
 * @param overallJustification general justification for the entire cart (optional, used as fallback)
// * @param overallValidityDays default validity days for roles without specific validity (optional)
 * @param shouldSendNotification whether to send notification emails to assigned users
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public record AssignRolesFromCartRequest(
        @NotEmpty(message = "Role-user map cannot be empty")
        Map<@NotNull UUID, @Valid @NotNull RoleAssignmentDetail> roleUserMap,

        @Size(max = 1000, message = "Overall justification must not exceed 1000 characters")
        String overallJustification,

//        Integer overallValidityDays,

        boolean shouldSendNotification
) {
    /**
     * Calculates the total number of role assignments in this request.
     * This is the sum of all users across all roles (may include duplicates if same user gets multiple roles).
     *
     * @return total number of role-user assignments to be created
     */
    public int getTotalAssignments() {
        if (roleUserMap == null) {
            return 0;
        }
        return roleUserMap.values().stream()
                .mapToInt(RoleAssignmentDetail::getUserCount)
                .sum();
    }

    /**
     * Gets the number of unique roles in this cart.
     *
     * @return count of roles
     */
    public int getRoleCount() {
        return roleUserMap != null ? roleUserMap.size() : 0;
    }

    /**
     * Gets all unique user IDs across all roles in the cart.
     *
     * @return set of unique user IDs
     */
    public Set<UUID> getAllUniqueUserIds() {
        if (roleUserMap == null) {
            return Set.of();
        }
        Set<UUID> allUsers = new HashSet<>();
        roleUserMap.values().forEach(detail -> allUsers.addAll(detail.userIds()));
        return allUsers;
    }

    /**
     * Gets the total number of unique users in this cart.
     *
     * @return count of unique users
     */
    public int getUniqueUserCount() {
        return getAllUniqueUserIds().size();
    }

    /**
     * Gets the justification for a specific role, falling back to overall justification if not specified.
     *
     * @param roleId the role ID to get justification for
     * @return role-specific justification or overall justification
     */
    public String getJustificationForRole(UUID roleId) {
        RoleAssignmentDetail detail = roleUserMap.get(roleId);
        if (detail != null && detail.hasJustification()) {
            return detail.justification();
        }
        return overallJustification;
    }

    /**
     * Gets the validity days for a specific role, falling back to overall validity if not specified.
     *
     * @param roleId the role ID to get validity for
     * @return role-specific validity days or overall validity days
     */
    public Integer getValidityDaysForRole(UUID roleId) {
        RoleAssignmentDetail detail = roleUserMap.get(roleId);
        if (detail != null && detail.hasValidityDays()) {
            return detail.validityDays();
        }
        return null; // permanent
    }

    /**
     * Determines if this request should be processed asynchronously.
     * Large batches (≥1000 assignments) should be processed async to avoid blocking.
     *
     * @return true if total assignments >= 1000
     */
    public boolean shouldProcessAsync() {
        return getTotalAssignments() >= 1000;
    }
}