package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.enums.ScheduleType;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO for Dynamic Rule operations.
 * 
 * @param id the UUID of the dynamic rule
 * @param roleId the UUID of the role this rule applies to
 * @param roleName the name of the role this rule applies to
 * @param ruleIsActive whether the rule is active
 * @param ruleConditionScript the condition script for the rule
 * @param sendNotificationOnAssignment whether to send notification on assignment
 * @param requireApprovalBeforeAssignment whether to require approval before assignment
 * @param scheduleType the schedule type for rule execution
 * @param time the time for scheduled execution
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Schema(description = "Response containing dynamic rule information")
public record DynamicRuleResponse(
    
    @Schema(description = "The unique identifier of the dynamic rule", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID id,
    
    @Schema(description = "The ID of the role this rule applies to", example = "456e7890-e89b-12d3-a456-426614174001")
    UUID roleId,
    
    @Schema(description = "The name of the role this rule applies to", example = "System Administrator")
    String roleName,
    
    @Schema(description = "Whether the rule is active", example = "true")
    Boolean ruleIsActive,
    
    @Schema(description = "The condition script for the rule", example = "user.department == 'Finance' && user.level >= 3")
    String ruleConditionScript,
    
    @Schema(description = "Whether to send notification on assignment", example = "false")
    Boolean sendNotificationOnAssignment,
    
    @Schema(description = "Whether to require approval before assignment", example = "false")
    Boolean requireApprovalBeforeAssignment,
    
    @Schema(description = "The schedule type for rule execution", example = "IMMEDIATE")
    ScheduleType scheduleType,
    
    @Schema(description = "The time for scheduled execution", example = "14:30", format = "time")
    @JsonFormat(pattern = "HH:mm")
    LocalTime time,
    
    @Schema(description = "Creation timestamp (epoch milliseconds)", example = "1697372400000")
    Long createdAt,
    
    @Schema(description = "Last update timestamp (epoch milliseconds)", example = "1697372400000")
    Long updatedAt,
    
    @Schema(description = "ID of user who created this rule", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID createdById,
    
    @Schema(description = "Name of user who created this rule", example = "john.doe")
    String createdByName,
    
    @Schema(description = "ID of user who last updated this rule", example = "123e4567-e89b-12d3-a456-426614174000")
    UUID updatedById,
    
    @Schema(description = "Name of user who last updated this rule", example = "jane.smith")
    String updatedByName
) {}