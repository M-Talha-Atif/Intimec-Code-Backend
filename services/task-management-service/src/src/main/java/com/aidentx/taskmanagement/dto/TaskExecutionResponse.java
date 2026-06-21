package com.aidentx.taskmanagement.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

/** DTO for task execution response. Returned when a task execution is triggered. */
@Data
@Builder
public class TaskExecutionResponse {

  private UUID taskId;
  private String executionId;
  private String message;

  private Instant triggeredAt;
}
