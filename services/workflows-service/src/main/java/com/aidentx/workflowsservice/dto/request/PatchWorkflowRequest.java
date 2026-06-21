package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.validation.ValidCronExpression;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Request DTO for partial workflow updates (PATCH)
 * All fields are optional - only provided fields will be updated
 * Used for auto-save functionality in the frontend
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatchWorkflowRequest {

    private String name;
    
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
    
    @JsonProperty("sourceTemplateId")
    private String sourceTemplateId;
    
    private Map<String, Boolean> permissions;
    
    private Integer riskScore;
    
    private Boolean hasTimeLogic;
    
    private Boolean hasSodViolation;
    
    private String roleSensitivity; // LOW, MEDIUM, HIGH
    
    private String policyCompliance; // COMPLIANT, WARNING, VIOLATION
    
    // Additional fields from frontend
    private String riskLevel; // low, medium, high
    
    private List<String> complianceFrameworks;
    
    private Boolean sensitiveData;
    
    private Boolean externalIntegration;
    
    private String businessJustification;
    
    private String triggerConditions;
}
