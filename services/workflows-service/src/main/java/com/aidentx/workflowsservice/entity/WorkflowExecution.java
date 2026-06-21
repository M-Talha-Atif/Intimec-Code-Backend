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
 * WorkflowExecution Entity - Execution history and state tracking
 * Stores execution trace, metrics, and provisioning results
 */
@Entity
@Table(name = "workflow_executions", indexes = {
    @Index(name = "idx_executions_workflow", columnList = "workflow_id"),
    @Index(name = "idx_executions_status", columnList = "status"),
    @Index(name = "idx_executions_started", columnList = "started_at"),
    @Index(name = "idx_executions_triggered_by", columnList = "triggered_by")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "workflow_id")
    private UUID workflowId;

    @Column(name = "workflow_version")
    private String workflowVersion;

    // Execution context
    @Column(name = "trigger_type", nullable = false)
    private String triggerType;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "triggered_by_name")
    private String triggeredByName;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "cancelled_by_name")
    private String cancelledByName;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "execution_context")
    private Map<String, Object> executionContext; // Identity data, request details, etc.

    // Status & results
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ExecutionStatus status;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "duration_ms")
    private Integer durationMs;

    // Execution trace
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "path_trace")
    @Builder.Default
    private List<Map<String, Object>> pathTrace = List.of();

    @Column(name = "current_node_id")
    private String currentNodeId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Risk & compliance
    @Column(name = "risk_score")
    private Integer riskScore;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "sod_violations")
    @Builder.Default
    private List<Map<String, Object>> sodViolations = List.of();

    // Provisioning results
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "provisioning_plan")
    @Builder.Default
    private List<Map<String, Object>> provisioningPlan = List.of();

    // Metrics
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> metrics = Map.of();

    // Idempotency & timeout support (for production)
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "timeout_at")
    private OffsetDateTime timeoutAt;

    public enum ExecutionStatus {
        RUNNING, COMPLETED, FAILED, PARTIAL, CANCELLED, TIMEOUT, PAUSED
    }
}
