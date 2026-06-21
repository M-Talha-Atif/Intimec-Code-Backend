package com.aidentx.taskmanagement.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a task execution log entry. These logs are stored in OpenSearch for real-time
 * streaming and querying.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionLog {

  /** Unique identifier for this log entry */
  private String id;

  /** ID of the task being executed */
  private UUID taskId;

  /** Tenant ID for multi-tenant isolation */
  private String tenantId;

  /** Application ID the task belongs to */
  private UUID applicationId;

  /** Task execution ID (unique per execution attempt) */
  private String executionId;

  /** Timestamp of the log entry */
  @Builder.Default private Long timestamp = Instant.now().toEpochMilli();

  /** Log level (INFO, WARN, ERROR, DEBUG) */
  private String level;

  /** Log message */
  private String message;

  /** Task execution status (STARTED, RUNNING, COMPLETED, FAILED, RETRYING) */
  private String status;

  /** Exception message if error occurred */
  private String exception;

  /** Exception stack trace if error occurred */
  private String stackTrace;

  /** Metadata or additional context */
  private Object metadata;

  /** Microservice name */
  @Builder.Default private String microservice = "task-management-service";

  /** Log category */
  @Builder.Default private String category = "TASK_EXECUTION";
}
