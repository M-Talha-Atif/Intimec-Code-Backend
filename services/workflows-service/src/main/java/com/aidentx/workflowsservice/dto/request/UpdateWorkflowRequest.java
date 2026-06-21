package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.validation.ValidCronExpression;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Request DTO for updating an existing workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWorkflowRequest {

    @Size(min = WORKFLOW_NAME_MIN_SIZE, max = WORKFLOW_NAME_MAX_SIZE, message = VALIDATION_WORKFLOW_NAME_SIZE)
    private String name;

    @Size(max = DESCRIPTION_MAX_SIZE, message = VALIDATION_DESCRIPTION_MAX_SIZE)
    private String description;

    private String status; // DRAFT, ACTIVE, DEPRECATED

    private String category;

    private String triggerType;

    @JsonProperty("eventConfig")
    private Map<String, Object> eventConfig;

    @ValidCronExpression(message = "Invalid cron expression format")
    @JsonProperty("cronSchedule")
    private String cronSchedule;

    private List<CreateWorkflowRequest.NodeDTO> nodes;

    private List<CreateWorkflowRequest.EdgeDTO> edges;

    private List<String> applicationScope;

    private List<String> tags;

    private List<String> complianceFrameworks;

    @JsonProperty("sourceTemplateId")
    private String sourceTemplateId;

    private Map<String, Boolean> permissions;

    private Integer riskScore;

    private Boolean hasTimeLogic;

    private Boolean hasSodViolation;

    private String roleSensitivity; // LOW, MEDIUM, HIGH

    private String policyCompliance; // COMPLIANT, WARNING, VIOLATION
}
