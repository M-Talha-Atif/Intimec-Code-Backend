package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.roles_management.enums.ScheduleType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * Request DTO for updating a dynamic rule.
 * 
 * <p>This request uses the roleId to identify which dynamic rule to update,
 * since each role can have only one dynamic rule.</p>
 * 
 * @param roleId the ID of the role whose dynamic rule should be updated
 * @param ruleIsActive whether the rule is active
 * @param ruleConditionScript the SpEL script for evaluating user conditions
 * @param sendNotificationOnAssignment whether to send notifications on assignment
 * @param requireApprovalBeforeAssignment whether approval is required before assignment
 * @param scheduleType the schedule type (IMMEDIATE or DAILY)
 * @param time the execution time for DAILY schedules (HH:mm format)
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Request for updating a dynamic rule")
public record UpdateDynamicRuleRequest(
    
    @NotNull(message = AppConstants.VALIDATION_ROLE_ID_REQUIRED)
    @Schema(description = "The ID of the role whose dynamic rule should be updated", example = "64aedcba-f440-4b4c-b549-1083ed12070b", required = true)
    UUID roleId,

    @NotNull(message = AppConstants.VALIDATION_RULE_ACTIVE_STATUS_REQUIRED)
    @Schema(description = "Whether the dynamic rule is active", example = "true", required = true)
    Boolean ruleIsActive,

    @NotNull(message = AppConstants.VALIDATION_RULE_CONDITION_SCRIPT_REQUIRED)
    @Size(min = 1, max = 1000, message = AppConstants.VALIDATION_RULE_CONDITION_SCRIPT_LENGTH)
    @Schema(description = "SpEL expression for evaluating user conditions", example = "user.status == 'ACTIVE'", required = true)
    String ruleConditionScript,

    @NotNull(message = AppConstants.VALIDATION_SEND_NOTIFICATION_FLAG_REQUIRED)
    @Schema(description = "Whether to send notifications when roles are assigned", example = "false", required = true)
    Boolean sendNotificationOnAssignment,

    @NotNull(message = AppConstants.VALIDATION_REQUIRE_APPROVAL_FLAG_REQUIRED)
    @Schema(description = "Whether approval is required before role assignment", example = "false", required = true)
    Boolean requireApprovalBeforeAssignment,

    @NotNull(message = AppConstants.VALIDATION_SCHEDULE_TYPE_REQUIRED)
    @Schema(description = "The execution schedule type", example = "DAILY", required = true)
    ScheduleType scheduleType,

    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = AppConstants.VALIDATION_TIME_MUST_BE_IN_HH_MM_FORMAT)
    @Schema(description = "Execution time for DAILY schedules (HH:mm format)", example = "14:28")
    String time
) {
    
    /**
     * Validates the request data.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public UpdateDynamicRuleRequest {
        // Additional validation: time is required for DAILY schedules
        if (scheduleType == ScheduleType.DAILY && (time == null || time.trim().isEmpty())) {
            throw new IllegalArgumentException(AppConstants.ERROR_TIME_REQUIRED_FOR_DAILY_SCHEDULE);
        }
        
        // Additional validation: time should be null for IMMEDIATE schedules
        if (scheduleType == ScheduleType.IMMEDIATE && time != null && !time.trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.ERROR_TIME_NOT_ALLOWED_FOR_IMMEDIATE_SCHEDULE);
        }
    }
}