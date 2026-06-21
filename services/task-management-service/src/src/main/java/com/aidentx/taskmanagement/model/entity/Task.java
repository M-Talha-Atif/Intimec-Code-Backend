package com.aidentx.taskmanagement.model.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.Check;
import org.hibernate.type.SqlTypes;

import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.model.enums.*;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA Entity representing a task in the task management system.
 *
 * <p>This entity supports:
 *
 * <ul>
 *   <li>One-time and recurring task scheduling
 *   <li>Multi-tenant isolation via tenantId
 *   <li>Audit trail with createdBy/updatedBy tracking
 *   <li>Optimistic locking via @Version annotation
 *   <li>JSONB storage for flexible task configuration
 * </ul>
 *
 * <p><b>Version Management:</b><br>
 * The version field uses JPA's @Version annotation for optimistic locking. It automatically
 * increments on each update, preventing concurrent modification issues. Initial version is set to 1
 * on creation.
 *
 * <p><b>User Context Tracking:</b><br>
 * - createdBy: Set on creation to the user ID from X-User-ID header<br>
 * - updatedBy: Initially same as createdBy, updated on each modification<br>
 * - ownerId: The user who owns and has full control of the task<br>
 * - assignedTo: JSONB array of user UUIDs who can execute/manage the task (supports groups)<br>
 * - tenantId: For multi-tenant isolation from X-Tenant-ID header<br>
 * - applicationId: The application this task belongs to (required)
 *
 * <p><b>Binding Type:</b><br>
 * Controls how strictly the task adheres to its schedule:<br>
 * - LOOSE: Task can execute independently of schedule (e.g., CRON tasks can catch up if missed)<br>
 * - TIGHT: Task must execute exactly on schedule or fail (no catching up missed executions)
 *
 * <p><b>Timestamps:</b><br>
 * All timestamps use Instant (UTC) for timezone independence.<br>
 * - createdAt and updatedAt are automatically managed by Hibernate
 */
@Entity
@Table(name = "tasks")
@Check(constraints = "status IN ('PENDING','RUNNABLE','RUNNING','WAITING','SUSPENDED','CLOSED','COMPLETED','SKIPPED','FAILED','CANCELLED')")
@Data
@Getter
@Setter
public class Task {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private UUID id;

  @Column(name = "name", nullable = false)
  @NotBlank(message = "Task name is required")
  @Size(min = 1, max = 255, message = "Task name must be between 1 and 255 characters")
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  private String description;

  @Column(name = "category")
  @NotBlank(message = "Category is required")
  private String category;

  @Column(name = "priority")
  @NotNull(message = "Priority is required")
  @Min(value = 1, message = "Priority must be between 1 and 9")
  @Max(value = 9, message = "Priority must be between 1 and 9")
  private Integer priority;

  // Ownership and permissions
  @Column(name = "owner_id")
  private UUID ownerId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "assigned_to", columnDefinition = "jsonb")
  private List<UUID> assignedTo = new ArrayList<>(); // JSONB array of user/identity UUIDs

  @Column(name = "tenant_id")
  private String tenantId;

  @Column(name = "application_id", nullable = false)
  @NotNull(message = "Application ID is required")
  private UUID applicationId;

  // Status
  @Enumerated(EnumType.STRING)
  @Column(name = "persistence_status", nullable = false)
  private PersistenceStatus persistenceStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ExecutionStatus status;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "business_configuration", columnDefinition = "jsonb")
  private String businessConfiguration;

  // Configuration
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "task_config", columnDefinition = "jsonb")
  private String taskConfig;

  // Scheduling (MidPoint-inspired)
  @Enumerated(EnumType.STRING)
  @Column(name = "schedule_type")
  private ScheduleType scheduleType;

  @Column(name = "cron_expression")
  private String cronExpression;

  @Column(name = "interval_seconds")
  private Integer intervalSeconds;

  @Enumerated(EnumType.STRING)
  @Column(name = "binding_type", nullable = false)
  private BindingType bindingType;

  @Column(name = "earliest_start_time")
  private Instant earliestStartTime;

  @Column(name = "latest_start_time")
  private Instant latestStartTime;

  @Column(name = "latest_finish_time")
  private Instant latestFinishTime;

  @Column(name = "next_execution")
  private Instant nextExecution;

  @Column(name = "last_execution")
  private Instant lastExecution;

  // Execution tracking
  @Column(name = "last_run_start_timestamp")
  private Instant lastRunStartTimestamp;

  @Column(name = "last_run_finish_timestamp")
  private Instant lastRunFinishTimestamp;

  @Column(name = "execution_node_id")
  private String executionNodeId;

  // Metadata
  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @Version
  @Column(name = "version")
  private Integer version;

  // JPA Lifecycle callbacks for setting defaults and validation
  @PrePersist
  protected void onCreate() {
    if (persistenceStatus == null) {
      persistenceStatus = PersistenceStatus.PERSISTENT;
    }
    if (status == null) {
      status = ExecutionStatus.RUNNABLE;
    }
    if (bindingType == null) {
      bindingType = BindingType.LOOSE;
    }
    validateConstraints();
  }

  @PreUpdate
  protected void onUpdate() {
    validateConstraints();
  }

  private void validateConstraints() {
    // Cron requires loose binding
    if (cronExpression != null && bindingType != BindingType.LOOSE) {
      throw new IllegalStateException(TaskManagementConstants.ERR_CRON_REQUIRES_LOOSE_BINDING);
    }

    // Tight binding requires interval
    if (bindingType == BindingType.TIGHT && intervalSeconds == null) {
      throw new IllegalStateException(TaskManagementConstants.ERR_TIGHT_BINDING_REQUIRES_INTERVAL);
    }
  }
}
