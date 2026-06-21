package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing the result of a dynamic rule execution.
 * 
 * @param dynamicRuleId the ID of the dynamic rule that was executed
 * @param roleId the ID of the role that was assigned
 * @param totalUsersEvaluated total number of users evaluated against the condition
 * @param usersMatchingCondition number of users who matched the condition
 * @param successfulAssignments number of users who were successfully assigned the role
 * @param failedAssignments number of assignments that failed
 * @param skippedAssignments number of assignments that were skipped (users already had the role)
 * @param assignedUserIds list of user IDs who were successfully assigned the role
 * @param executionTimeMs execution time in milliseconds
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Result of dynamic rule execution containing assignment statistics")
public record DynamicRuleExecutionResult(
    
    @Schema(description = "ID of the dynamic rule that was executed", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("dynamicRuleId")
    UUID dynamicRuleId,
    
    @Schema(description = "ID of the role that was assigned", example = "123e4567-e89b-12d3-a456-426614174000")
    @JsonProperty("roleId")
    UUID roleId,
    
    @Schema(description = "Total number of users evaluated against the condition", example = "50")
    @JsonProperty("totalUsersEvaluated")
    int totalUsersEvaluated,
    
    @Schema(description = "Number of users who matched the condition", example = "25")
    @JsonProperty("usersMatchingCondition")
    int usersMatchingCondition,
    
    @Schema(description = "Number of users who were successfully assigned the role", example = "20")
    @JsonProperty("successfulAssignments")
    int successfulAssignments,
    
    @Schema(description = "Number of assignments that failed", example = "0")
    @JsonProperty("failedAssignments")
    int failedAssignments,
    
    @Schema(description = "Number of assignments that were skipped (users already had the role)", example = "5")
    @JsonProperty("skippedAssignments")
    int skippedAssignments,
    
    @Schema(description = "List of user IDs who were successfully assigned the role")
    @JsonProperty("assignedUserIds")
    List<UUID> assignedUserIds,
    
    @Schema(description = "Execution time in milliseconds", example = "1250")
    @JsonProperty("executionTimeMs")
    long executionTimeMs
) {
    
    @Override
    public String toString() {
        return "DynamicRuleExecutionResult{" +
                "dynamicRuleId=" + dynamicRuleId +
                ", roleId=" + roleId +
                ", totalUsersEvaluated=" + totalUsersEvaluated +
                ", usersMatchingCondition=" + usersMatchingCondition +
                ", successfulAssignments=" + successfulAssignments +
                ", failedAssignments=" + failedAssignments +
                ", skippedAssignments=" + skippedAssignments +
                ", assignedUserIds=" + assignedUserIds.size() + " users" +
                ", executionTimeMs=" + executionTimeMs +
                '}';
    }
}