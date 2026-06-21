package com.aidentx.taskmanagement.dto;

import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.*;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * Request DTO for previewing a task execution. This creates a temporary manual task, executes it,
 * streams logs via SSE, and then removes the task.
 */
@Data
@Schema(
    description =
        "Request body for previewing a task execution. The task will be created temporarily, executed, and then deleted.",
    example =
        "{\n"
            + "  \"category\": \"MAINTENANCE\",\n"
            + "  \"applicationId\": \"123e4567-e89b-12d3-a456-426614174000\",\n"
            + "  \"taskConfig\": {\n"
            + "    \"syncType\": \"FULL\",\n"
            + "    \"batchSize\": 100\n"
            + "  }\n"
            + "}")
public class TaskPreviewRequest {

  @Schema(
      description = "Task category",
      example = "MAINTENANCE",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Category is required")
  private String category;

  @Schema(
      description = "Application ID this task belongs to",
      example = "123e4567-e89b-12d3-a456-426614174000",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Application ID is required")
  private UUID applicationId;

  @Schema(
      description =
          "Optional task configuration stored as JSON. This allows flexible configuration storage for different task types.",
      example = "{ \"syncType\": \"FULL\", \"batchSize\": 100 }")
  private Map<String, Object> taskConfig;
}
