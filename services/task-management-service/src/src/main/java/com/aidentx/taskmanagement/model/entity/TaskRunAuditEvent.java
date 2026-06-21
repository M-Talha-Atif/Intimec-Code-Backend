package com.aidentx.taskmanagement.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.aidentx.taskmanagement.model.enums.TaskAuditEventType;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Immutable, append-only audit record for a single structural milestone in a task run.
 * <p>
 * Written to PostgreSQL so that the audit trail is durable regardless of OpenSearch
 * availability. OpenSearch holds verbose per-account logs; this table holds the
 * headline events that regulators and auditors require (SOX, ISO 27001, ISO 27701).
 * <p>
 * Only ~10–15 rows are written per task run. Per-account details (100K rows per import)
 * are never written here — they stay in OpenSearch where they belong.
 * <p>
 * This table must NEVER have rows updated or deleted after insert.
 * The {@code updatable = false} annotations on all columns enforce this at the ORM level.
 */
@Entity
@Table(
    name = "task_run_audit_events",
    indexes = {
        @Index(name = "idx_task_run_audit_execution", columnList = "execution_id, event_time"),
        @Index(name = "idx_task_run_audit_tenant",    columnList = "tenant_id, event_time"),
        @Index(name = "idx_task_run_audit_task_id",   columnList = "task_id, event_time")
    }
)
public class TaskRunAuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * The execution ID of the task run this event belongs to.
     * Matches {@link TaskRun#executionId}.
     */
    @NotBlank
    @Column(name = "execution_id", nullable = false, updatable = false, length = 36)
    private String executionId;

    @NotNull
    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, updatable = false, length = 100)
    private String tenantId;

    /** UTC timestamp of when this event occurred. */
    @NotNull
    @Column(name = "event_time", nullable = false, updatable = false)
    private Instant eventTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 64)
    private TaskAuditEventType eventType;

    /** INFO, WARN, or ERROR. */
    @NotBlank
    @Column(name = "level", nullable = false, updatable = false, length = 10)
    private String level;

    /** Human-readable description of this event — same text written to OpenSearch. */
    @NotBlank
    @Column(name = "message", nullable = false, updatable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Optional structured metadata as a JSONB column.
     * e.g. {@code {"total": 1200, "created": 1198, "errors": 2}} for
     * {@link TaskAuditEventType#ENTITLEMENT_IMPORT_COMPLETED}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb", updatable = false)
    private String metadata;

    protected TaskRunAuditEvent() {}

    private TaskRunAuditEvent(Builder builder) {
        this.executionId = builder.executionId;
        this.taskId      = builder.taskId;
        this.tenantId    = builder.tenantId;
        this.eventTime   = Instant.now();
        this.eventType   = builder.eventType;
        this.level       = builder.level;
        this.message     = builder.message;
        this.metadata    = builder.metadata;
    }

    // ── Getters (no setters — immutable after construction) ──────────────────

    public UUID getId()                    { return id; }
    public String getExecutionId()         { return executionId; }
    public UUID getTaskId()                { return taskId; }
    public String getTenantId()            { return tenantId; }
    public Instant getEventTime()          { return eventTime; }
    public TaskAuditEventType getEventType() { return eventType; }
    public String getLevel()               { return level; }
    public String getMessage()             { return message; }
    public String getMetadata()            { return metadata; }

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String executionId;
        private UUID taskId;
        private String tenantId;
        private TaskAuditEventType eventType;
        private String level = "INFO";
        private String message;
        private String metadata;

        public Builder executionId(String v)         { this.executionId = v; return this; }
        public Builder taskId(UUID v)                { this.taskId = v;      return this; }
        public Builder tenantId(String v)            { this.tenantId = v;    return this; }
        public Builder eventType(TaskAuditEventType v) { this.eventType = v; return this; }
        public Builder level(String v)               { this.level = v;       return this; }
        public Builder message(String v)             { this.message = v;     return this; }
        public Builder metadata(String v)            { this.metadata = v;    return this; }

        public TaskRunAuditEvent build() {
            if (executionId == null || taskId == null || tenantId == null
                    || eventType == null || message == null) {
                throw new IllegalStateException(
                    "executionId, taskId, tenantId, eventType and message are required");
            }
            return new TaskRunAuditEvent(this);
        }
    }
}
