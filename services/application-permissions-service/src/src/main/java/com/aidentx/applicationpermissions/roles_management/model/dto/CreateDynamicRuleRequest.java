package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.ScheduleType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Request DTO for creating a Dynamic Rule.
 * 
 * @param roleId the UUID of the role this rule applies to
 * @param ruleIsActive whether the rule is active (defaults to true)
 * @param ruleConditionScript the condition script for the rule (can be null)
 * @param sendNotificationOnAssignment whether to send notification on assignment (defaults to false)
 * @param requireApprovalBeforeAssignment whether to require approval before assignment (defaults to false)
 * @param scheduleType the schedule type for rule execution (defaults to IMMEDIATE)
 * @param time the time for scheduled execution (can be null, especially for IMMEDIATE)
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Request for creating a dynamic rule")
public record CreateDynamicRuleRequest(
    
    @Schema(description = "The ID of the role this rule applies to", example = "123e4567-e89b-12d3-a456-426614174000", required = true)
    @NotNull(message = AppConstants.VALIDATION_ROLE_ID_REQUIRED)
    UUID roleId,
    
    @Schema(description = "Whether the rule is active", example = "true", defaultValue = "true")
    Boolean ruleIsActive,
    
    @Schema(description = "The condition script for the rule", example = "user.department == 'Finance' && user.level >= 3")
    String ruleConditionScript,
    
    @Schema(description = "Whether to send notification on assignment", example = "false", defaultValue = "false")
    Boolean sendNotificationOnAssignment,
    
    @Schema(description = "Whether to require approval before assignment", example = "false", defaultValue = "false")
    Boolean requireApprovalBeforeAssignment,
    
    @Schema(description = "The schedule type for rule execution", example = "IMMEDIATE", defaultValue = "IMMEDIATE")
    ScheduleType scheduleType,
    
    @Schema(description = "The time for scheduled execution (null for IMMEDIATE)", example = "14:30", format = "time")
    @JsonFormat(pattern = "HH:mm")
    LocalTime time
) {
    
    /**
     * Default constructor with sensible defaults.
     */
    public CreateDynamicRuleRequest {
        if (ruleIsActive == null) {
            ruleIsActive = true;
        }
        if (sendNotificationOnAssignment == null) {
            sendNotificationOnAssignment = false;
        }
        if (requireApprovalBeforeAssignment == null) {
            requireApprovalBeforeAssignment = false;
        }
        if (scheduleType == null) {
            scheduleType = ScheduleType.IMMEDIATE;
        }
    }
}