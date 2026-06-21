package com.aidentx.workflowsservice.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * WorkflowNodeExecution Entity - Individual node execution details
 * Tracks input/output, retry attempts, and approvals
 */
@Entity
@Table(name = "workflow_node_executions", indexes = {
    @Index(name = "idx_node_executions_execution", columnList = "execution_id"),
    @Index(name = "idx_node_executions_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowNodeExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "step_type", nullable = false)
    private String stepType;

    @Column(name = "step_number")
    private Integer stepNumber;

    // Execution details
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NodeExecutionStatus status;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "duration_ms")
    private Integer durationMs;

    // Input/Output
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "input_data")
    private Map<String, Object> inputData;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "output_data")
    private Map<String, Object> outputData;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "error_details")
    private Map<String, Object> errorDetails;

    // Approvals (for approval nodes)
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<Map<String, Object>> approvals = List.of();

    // Retry tracking
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 0;

    public enum NodeExecutionStatus {
        PENDING, RUNNING, COMPLETED, FAILED, SKIPPED, TIMEOUT, PAUSED
    }
}
