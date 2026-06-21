package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for cart-based role assignments.
 *
 * <p>Contains detailed information about successful and failed assignments,
 * organized by role for easy tracking and reporting.</p>
 *
 * @param batchId unique identifier for tracking this batch (if async)
 * @param status processing status: "COMPLETED", "PENDING", "PROCESSING", "FAILED"
 * @param totalAssignments total number of role-user assignments requested
 * @param successfulAssignments number of assignments successfully created
 * @param skippedAssignments number of assignments skipped (already existed)
 * @param failedAssignments number of assignments that failed
 * @param successByRole breakdown of successful assignments per role
 * @param failuresByRole list of failures grouped by role
 * @param processedAt timestamp when processing completed (null if still processing)
 * @param estimatedCompletionTime estimated completion time for async processing (null if sync or completed)
 * @param trackingUrl URL to check batch status (null if sync processing)
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public record CartBasedAssignmentResponse(
        UUID batchId,
        String status,
        int totalAssignments,
        int successfulAssignments,
        int skippedAssignments,
        int failedAssignments,
        Map<UUID, Integer> successByRole,
        Map<UUID, List<RoleAssignmentFailure>> failuresByRole,
        LocalDateTime processedAt,
        String estimatedCompletionTime,
        String trackingUrl
) {
    /**
     * Represents a failed role assignment with details.
     *
     * @param roleId the role ID that failed
     * @param userId the user ID that failed
     * @param roleName the role name (for display)
     * @param reason failure reason/error message
     * @param errorCode error code for programmatic handling
     */
    public record RoleAssignmentFailure(
            UUID roleId,
            UUID userId,
            String roleName,
            String reason,
            String errorCode
    ) {}

    /**
     * Checks if all assignments were successful.
     *
     * @return true if no failures or skipped assignments
     */
    public boolean isFullSuccess() {
        return failedAssignments == 0 && skippedAssignments == 0;
    }

    /**
     * Checks if processing is still in progress.
     *
     * @return true if status is PENDING or PROCESSING
     */
    public boolean isProcessing() {
        return "PENDING".equals(status) || "PROCESSING".equals(status);
    }
}