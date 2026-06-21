package com.aidentx.taskmanagement.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import org.hibernate.annotations.Check;
import org.hibernate.annotations.CreationTimestamp;

import com.aidentx.taskmanagement.model.enums.ExecutionStatus;

import lombok.Data;

/**
 * JPA Entity representing a single execution run of a task. All timestamps are stored as Instant to
 * ensure timezone consistency (UTC).
 */
@Entity
@Check(constraints = "status IN ('PENDING','RUNNABLE','RUNNING','WAITING','SUSPENDED','CLOSED','COMPLETED','FAILED','CANCELLED','SKIPPED')")
@Table(
    name = "task_runs",
    indexes = {
      @Index(name = "idx_task_runs_task_id", columnList = "taskId"),
      @Index(name = "idx_task_runs_tenant_id", columnList = "tenantId")
    })
@Data
public class TaskRun {

  @Id
  @Column(name = "execution_id", updatable = false, nullable = false, length = 36)
  private String executionId;

  @Column(name = "task_id", nullable = false)
  private UUID taskId;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "application_id", nullable = false)
  private UUID applicationId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ExecutionStatus status;

  @Column(name = "execution_node_id")
  private String executionNodeId;

  @Column(name = "start_time")
  private Instant startTime; // Changed from LocalDateTime

  @Column(name = "end_time")
  private Instant endTime; // Changed from LocalDateTime

  @Column(name = "duration_ms")
  private Long durationMs;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "stack_trace", columnDefinition = "TEXT")
  private String stackTrace;

  // --- Synchronization Statistics ---

  @Column(name = "processed_count")
  private Integer processedCount;

  @Column(name = "created_count")
  private Integer createdCount;

  @Column(name = "updated_count")
  private Integer updatedCount;

  @Column(name = "deleted_count")
  private Integer deletedCount;

  @Column(name = "linked_count")
  private Integer linkedCount;

  @Column(name = "failed_count")
  private Integer failedCount;

  @Column(name = "skipped_count")
  private Integer skippedCount;

  @Column(name = "entitlements_imported")
  private Integer entitlementsImported;

  /** Estimated total object count — derived from the last successful run of the same task. */
  @Column(name = "estimated_total_count")
  private Integer estimatedTotalCount;

  /** Whether this run was a dry-run preview (no data was actually modified). */
  @Column(name = "is_preview", nullable = false)
  private boolean preview = false;

  /** JSON blob storing detailed preview results (accounts, deletions, attribute changes). */
  @Column(name = "preview_details", columnDefinition = "TEXT")
  private String previewDetails;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt; // Changed from LocalDateTime

  /** Calculates and sets the duration in milliseconds. */
  public void calculateDuration() {
    if (startTime != null && endTime != null) {
      this.durationMs = java.time.Duration.between(startTime, endTime).toMillis();
    }
  }
}
