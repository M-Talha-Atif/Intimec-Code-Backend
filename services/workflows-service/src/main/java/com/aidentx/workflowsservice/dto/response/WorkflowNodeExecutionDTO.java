package com.aidentx.workflowsservice.dto.response;

import com.aidentx.workflowsservice.entity.WorkflowNodeExecution;
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
 * Response DTO for Workflow Node Execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowNodeExecutionDTO {

    private UUID id;
    private UUID executionId;
    private String nodeId;
    private String stepType;
    private Integer stepNumber;
    
    private String status; // PENDING, RUNNING, COMPLETED, FAILED, SKIPPED, TIMEOUT, PAUSED
    private OffsetDateTime startedAt;
    private OffsetDateTime completedAt;
    private Long durationMs;
    
    private Map<String, Object> inputData;
    private Map<String, Object> outputData;
    private Map<String, Object> errorDetails;
    
    private List<Map<String, Object>> approvals;
    private Integer retryCount;
    private Integer maxRetries;

    /**
     * Factory method to convert Entity to DTO
     */
    public static WorkflowNodeExecutionDTO from(WorkflowNodeExecution nodeExecution) {
        if (nodeExecution == null) return null;

        return WorkflowNodeExecutionDTO.builder()
                .id(nodeExecution.getId())
                .executionId(nodeExecution.getExecutionId())
                .nodeId(nodeExecution.getNodeId())
                .stepType(nodeExecution.getStepType())
                .stepNumber(nodeExecution.getStepNumber())
                .status(nodeExecution.getStatus() != null ? nodeExecution.getStatus().name() : null)
                .startedAt(nodeExecution.getStartedAt())
                .completedAt(nodeExecution.getCompletedAt())
                .durationMs(nodeExecution.getDurationMs() != null ? nodeExecution.getDurationMs().longValue() : null)
                .inputData(ResponseDataSanitizer.sanitizeMap(nodeExecution.getInputData()))
                .outputData(ResponseDataSanitizer.sanitizeMap(nodeExecution.getOutputData()))
                .errorDetails(ResponseDataSanitizer.sanitizeMap(nodeExecution.getErrorDetails()))
                .approvals((List<Map<String, Object>>) ResponseDataSanitizer.sanitizeObject(nodeExecution.getApprovals()))
                .retryCount(nodeExecution.getRetryCount())
                .maxRetries(nodeExecution.getMaxRetries())
                .build();
    }
}
