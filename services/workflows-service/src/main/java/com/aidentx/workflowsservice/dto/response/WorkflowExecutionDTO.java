package com.aidentx.workflowsservice.dto.response;

import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.util.ResponseDataSanitizer;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for Workflow Execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowExecutionDTO {

    private UUID id;
    private UUID workflowId;
    private String workflowVersion;
    private String triggerType;
    private UUID triggeredBy;
    private String triggeredByUsername; // Username of the user who triggered the workflow

    private Map<String, Object> executionContext;

    private String status; // RUNNING, COMPLETED, FAILED, etc.
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Long durationMs;

    // Summary fields (for monitoring list view)
    private Integer totalSteps;
    private Integer stepsCompleted;
    private Integer failedSteps;

    private String currentNodeId;
    private List<Map<String, Object>> pathTrace;

    private String errorMessage;
    private Integer riskScore;

    private List<Map<String, Object>> sodViolations;
    private List<Map<String, Object>> provisioningPlan;
    private Map<String, Object> metrics;
    
    private List<WorkflowNodeExecutionDTO> nodeExecutions; // Detailed node execution status

    /**
     * Factory method to convert Entity to DTO
     */
    public static WorkflowExecutionDTO from(WorkflowExecution execution) {
        if (execution == null) return null;

        Map<String, Object> sanitizedContext = ResponseDataSanitizer.sanitizeMap(execution.getExecutionContext());

        String triggeredByUsername = null;
        if (sanitizedContext != null) {
            Object v = sanitizedContext.get("triggeredByUsername");
            if (v == null) {
                v = sanitizedContext.get("username");
            }
            if (v != null) {
                triggeredByUsername = String.valueOf(v);
            }
        }

        return WorkflowExecutionDTO.builder()
                .id(execution.getId())
                .workflowId(execution.getWorkflowId())
                .workflowVersion(execution.getWorkflowVersion())
                .triggerType(execution.getTriggerType())
                .triggeredBy(execution.getTriggeredBy())
                .triggeredByUsername(triggeredByUsername)
                .executionContext(sanitizedContext)
                .status(execution.getStatus() != null ? execution.getStatus().name() : null)
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .durationMs(execution.getDurationMs() != null ? execution.getDurationMs().longValue() : null)
                .currentNodeId(execution.getCurrentNodeId())
                .pathTrace(execution.getPathTrace())
                .errorMessage(execution.getErrorMessage())
                .riskScore(execution.getRiskScore())
                .sodViolations(execution.getSodViolations())
                .provisioningPlan(execution.getProvisioningPlan())
                .metrics(execution.getMetrics())
                .build();
    }
}
