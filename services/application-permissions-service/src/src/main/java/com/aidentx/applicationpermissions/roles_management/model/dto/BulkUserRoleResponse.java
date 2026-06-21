package com.aidentx.applicationpermissions.roles_management.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for bulk user-role assignment operations.
 * 
 * <p>Contains detailed information about the results of a bulk role assignment,
 * including successful assignments, failed assignments with reasons, and
 * summary statistics for monitoring and auditing purposes.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Bulk user role assignment response with success and failure details")
public record BulkUserRoleResponse(

    @Schema(description = "List of successful user-role assignments")
    List<UserRoleResponse> successfulAssignments,

    @Schema(description = "List of failed assignments with error details")
    List<FailedAssignment> failedAssignments,

    @Schema(description = "Summary statistics of the bulk operation")
    BulkOperationSummary summary
) {

    /**
     * Represents a failed role assignment with error details.
     */
    @Schema(description = "Details of a failed role assignment")
    public record FailedAssignment(
        
        @Schema(description = "The user ID that failed to be assigned", example = "123e4567-e89b-12d3-a456-426614174000")
        UUID userId,
        
        @Schema(description = "The reason for the failure", example = "User not found or does not belong to tenant")
        String reason,
        
        @Schema(description = "Error code for programmatic handling", example = "USER_NOT_FOUND")
        String errorCode
    ) {}

    /**
     * Summary statistics for the bulk operation.
     */
    @Schema(description = "Summary statistics of the bulk role assignment operation")
    public record BulkOperationSummary(
        
        @Schema(description = "Total number of users processed", example = "10")
        int totalUsers,
        
        @Schema(description = "Number of successful assignments", example = "8")
        int successCount,
        
        @Schema(description = "Number of failed assignments", example = "2")
        int failureCount,
        
        @Schema(description = "The role ID that was assigned", example = "456e7890-e89b-12d3-a456-426614174001")
        UUID roleId,
        
        @Schema(description = "The name of the role that was assigned", example = "Administrator")
        String roleName
    ) {}

    /**
     * Creates a new BulkUserRoleResponse.
     * 
     * @param successfulAssignments list of successful assignments
     * @param failedAssignments list of failed assignments with reasons
     * @param summary summary statistics of the operation
     */
    public BulkUserRoleResponse {
        // Record validation is handled by constructor
    }
}