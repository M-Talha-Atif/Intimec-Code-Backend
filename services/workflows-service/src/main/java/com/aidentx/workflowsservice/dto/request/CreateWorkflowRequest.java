package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.validation.ValidCronExpression;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Request DTO for creating a new workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequest {

    @NotBlank(message = VALIDATION_WORKFLOW_NAME_REQUIRED)
    @Size(min = WORKFLOW_NAME_MIN_SIZE, max = WORKFLOW_NAME_MAX_SIZE, message = VALIDATION_WORKFLOW_NAME_SIZE)
    private String name;

    @Size(max = DESCRIPTION_MAX_SIZE, message = VALIDATION_DESCRIPTION_MAX_SIZE)
    private String description;

    @NotBlank(message = VALIDATION_CATEGORY_REQUIRED)
    private String category; // JOINER_MOVER_LEAVER, ACCESS_REQUEST, etc.

    private String triggerType; // EVENT_BASED, TIME_BASED, MANUAL

    @JsonProperty("eventConfig")
    private Map<String, Object> eventConfig; // Event configuration for EVENT_BASED triggers

    @ValidCronExpression(message = "Invalid cron expression format")
    @JsonProperty("cronSchedule")
    private String cronSchedule; // Cron expression for TIME_BASED triggers

    @NotNull(message = VALIDATION_NODES_REQUIRED)
    @Size(min = 1, message = VALIDATION_NODES_MIN_SIZE)
    private List<NodeDTO> nodes;

    @NotNull(message = VALIDATION_EDGES_REQUIRED)
    private List<EdgeDTO> edges;

    private List<String> applicationScope;

    private List<String> tags;

    private List<String> complianceFrameworks;

    private String riskLevel; // "low", "medium", "high" from frontend

    private Integer riskScore; // Will be calculated from riskLevel

    private Boolean hasTimeLogic;

    private Boolean hasSodViolation;

    private String roleSensitivity; // LOW, MEDIUM, HIGH

    private String policyCompliance; // COMPLIANT, WARNING, VIOLATION

    private String owner; // Frontend sends this instead of createdBy

    @JsonProperty("sourceTemplateId")
    private String sourceTemplateId; // UUID string

    private Map<String, Boolean> permissions;

    /**
     * Node DTO for workflow graph
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NodeDTO {
        @NotBlank(message = VALIDATION_NODE_ID_REQUIRED)
        private String id;

        @NotBlank(message = VALIDATION_NODE_TYPE_REQUIRED)
        private String type; // trigger.start, approval.single, condition.if, etc.

        private Map<String, Object> data; // Node-specific configuration

        private PositionDTO position; // For visual designer

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class PositionDTO {
            private Integer x;
            private Integer y;
        }
    }

    /**
     * Edge DTO for workflow graph connections
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EdgeDTO {
        @NotBlank(message = VALIDATION_EDGE_ID_REQUIRED)
        private String id;

        @NotBlank(message = VALIDATION_SOURCE_NODE_REQUIRED)
        private String source;

        @NotBlank(message = VALIDATION_TARGET_NODE_REQUIRED)
        private String target;

        private String sourceHandle;
        private String targetHandle;

        private String label;
        private String type; // default, conditional
        private Map<String, Object> condition; // Condition object for conditional routing
        private Map<String, Object> data;
    }
}
