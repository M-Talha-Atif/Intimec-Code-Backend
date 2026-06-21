package com.aidentx.taskmanagement.dto;

import java.time.Instant;
import java.util.UUID;

import com.aidentx.taskmanagement.model.entity.TaskRun;
import com.aidentx.taskmanagement.model.enums.ExecutionStatus;

import lombok.Builder;
import lombok.Data;

/** DTO for task run information. Timestamps are returned as epoch milliseconds. */
@Data
@Builder
public class TaskRunResponse {

  private String executionId;
  private UUID taskId;
  private ExecutionStatus status;
  private Instant startTime; // Changed from LocalDateTime
  private Instant endTime; // Changed from LocalDateTime
  private Long durationMs;
  private String errorMessage;
  private String stackTrace;
  private Instant createdAt; // Changed from LocalDateTime

  // Synchronization statistics
  private Integer processedCount;
  private Integer createdCount;
  private Integer updatedCount;
  private Integer deletedCount;
  private Integer linkedCount;
  private Integer failedCount;
  private Integer skippedCount;
  private Integer entitlementsImported;

  // Progress estimation
  private Integer estimatedTotalCount;

  // Preview fields
  private boolean preview;
  private String previewDetails;

  /** Category of the parent task (e.g. IMPORT, RECONCILIATION, DISCOVERY). */
  private String taskCategory;

  /**
   * Creates a TaskRunResponse DTO from a TaskRun entity.
   *
   * @param run The TaskRun entity from the database.
   * @return A new TaskRunResponse DTO.
   */
  public static TaskRunResponse fromEntity(TaskRun run) {
    return fromEntity(run, null);
  }

  /**
   * Creates a TaskRunResponse DTO from a TaskRun entity, including the
   * parent task's category so consumers can render category-specific UI.
   */
  public static TaskRunResponse fromEntity(TaskRun run, String taskCategory) {
    return TaskRunResponse.builder()
        .executionId(run.getExecutionId())
        .taskId(run.getTaskId())
        .status(run.getStatus())
        .startTime(run.getStartTime())
        .endTime(run.getEndTime())
        .durationMs(run.getDurationMs())
        .errorMessage(run.getErrorMessage())
        .stackTrace(run.getStackTrace())
        .createdAt(run.getCreatedAt())
        .processedCount(run.getProcessedCount())
        .createdCount(run.getCreatedCount())
        .updatedCount(run.getUpdatedCount())
        .deletedCount(run.getDeletedCount())
        .linkedCount(run.getLinkedCount())
        .failedCount(run.getFailedCount())
        .skippedCount(run.getSkippedCount())
        .entitlementsImported(run.getEntitlementsImported())
        .estimatedTotalCount(run.getEstimatedTotalCount())
        .preview(run.isPreview())
        .previewDetails(run.getPreviewDetails())
        .taskCategory(taskCategory)
        .build();
  }
}
