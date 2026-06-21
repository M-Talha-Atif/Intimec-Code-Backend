package com.aidentx.taskmanagement.dto;

import java.time.Instant;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

@Data
@Schema(description = "Response after creating a task")
public class TaskCreationResponse {

  @Schema(
      description = "UUID of the created task",
      example = "abc12345-e29b-41d4-a716-446655440001")
  private UUID taskId;

  @Schema(description = "Status of the operation", example = "CREATED")
  private String status;

  @Schema(description = "Human-readable message", example = "Task created successfully")
  private String message;

  @Schema(
      description = "Timestamp when task was created (epoch milliseconds)",
      example = "1696281000000")
  private Instant createdAt;

  @Schema(description = "Scheduled execution time (epoch milliseconds)", example = "1696297200000")
  private Instant scheduledFor;

  public static TaskCreationResponse success(UUID taskId, Instant scheduledFor) {
    TaskCreationResponse response = new TaskCreationResponse();
    response.setTaskId(taskId);
    response.setStatus("CREATED");
    response.setMessage("Task created successfully");
    response.setCreatedAt(Instant.now());
    response.setScheduledFor(scheduledFor);
    return response;
  }
}
