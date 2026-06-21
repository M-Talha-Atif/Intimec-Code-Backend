package com.aidentx.applicationpermissions.roles_management.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for assigning multiple roles to multiple users.
 *
 * <p>This operation creates M×N assignments where M = number of roles and N = number of users.
 * For large batches (>10,000 assignments), processing is asynchronous.</p>
 *
 * <p><b>Example:</b> Assigning 3 roles to 100 users creates 300 role assignments.</p>
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public record BulkMultiRoleAssignmentRequest(

        /**
         * List of role IDs to assign (max 1000 roles per batch).
         */
        @NotEmpty(message = "At least one role ID must be provided")
        @Size(max = 1000, message = "Maximum 1000 roles can be assigned at once")
        List<UUID> roleIds,

        /**
         * List of user IDs to receive the roles (max 10,000 users per batch).
         */
        @NotEmpty(message = "At least one user ID must be provided")
        @Size(max = 10000, message = "Maximum 10,000 users can be assigned at once")
        List<UUID> userIds,

        /**
         * Business justification for these assignments.
         */
        @NotNull(message = "Justification is required")
        String justification,

        /**
         * Number of days the assignment is valid (null = permanent).
         */
        Integer validityDays,

        /**
         * Whether to process asynchronously (recommended for >1000 assignments).
         * If null, system decides based on total assignment count.
         */
        Boolean processAsync,

        /**
         * Whether to send notifications to users.
         */
        Boolean sendNotification
) {

    /**
     * Calculates total number of assignments (M × N).
     *
     * @return roleIds.size() × userIds.size()
     */
    public int getTotalAssignments() {
        return roleIds.size() * userIds.size();
    }

    /**
     * Determines if this should be processed asynchronously.
     * Auto-decides based on total assignments if not specified.
     *
     * @return true if async processing is needed
     */
    public boolean shouldProcessAsync() {
        // If explicitly set, use that value
        if (processAsync != null) {
            return processAsync;
        }
        // Auto-decide: >1000 assignments → async
        return getTotalAssignments() > 1000;
    }

    /**
     * Determines if notifications should be sent.
     * Defaults to false for bulk operations to avoid spam.
     *
     * @return true if notifications should be sent
     */
    public boolean shouldSendNotification() {
        return sendNotification != null && sendNotification;
    }
}