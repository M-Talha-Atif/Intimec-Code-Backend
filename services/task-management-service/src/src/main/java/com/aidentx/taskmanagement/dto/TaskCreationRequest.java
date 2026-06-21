package com.aidentx.taskmanagement.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.*;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Data;

/**
 * Request DTO for creating a new task. This class contains all required and optional fields for
 * task creation.
 *
 * <p>User context is provided via HTTP headers (X-Tenant-ID, X-User-ID) and the owner ID is
 * provided in the request body.
 *
 * @see TaskCreationResponse
 */
@Data
@Schema(
    description = "Request body for creating a new task. Supports one-time and recurring tasks.",
    example =
        "{\n"
            + "  \"name\": \"Daily Backup Task\",\n"
            + "  \"description\": \"Backup user data daily at 2 AM\",\n"
            + "  \"category\": \"MAINTENANCE\",\n"
            + "  \"priority\": 5,\n"
            + "  \"ownerId\": \"789e4567-e89b-12d3-a456-426614174111\",\n"
            + "  \"executionDateTime\": \"2025-10-03T02:00:00Z\",\n"
            + "  \"scheduleType\": \"CRON\",\n"
            + "  \"cronExpression\": \"0 0 2 * * ?\",\n"
            + "  \"taskConfig\": {\n"
            + "    \"backupType\": \"INCREMENTAL\",\n"
            + "    \"retentionDays\": 30,\n"
            + "    \"compressionEnabled\": true\n"
            + "  }\n"
            + "}")
public class TaskCreationRequest {

  @Schema(
      description = "Task name",
      example = "Daily Backup Task",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "Task name is required")
  @Size(min = 1, max = 255, message = "Task name must be between 1 and 255 characters")
  private String name;

  @Schema(description = "Task description", example = "Backup user data daily at 2 AM")
  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  private String description;

  @Schema(
      description = "Task category",
      example = "MAINTENANCE",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Category is required")
  private String category;

  @Schema(
      description =
          "Scheduled execution date and time (ISO 8601 format). For one-time tasks, this is required and must be in the future. For recurring tasks, this is the first execution time (optional, defaults to now). For manual tasks, this is ignored. Accepts both timezone-aware formats (e.g., '2025-10-03T02:00:00Z') and local time formats (e.g., '2025-10-03T02:00:00' assumed UTC).",
      example = "2025-10-03T02:00:00Z")
  private Instant executionDateTime;

  @Schema(
      description = "Task priority (1=lowest, 9=highest)",
      example = "5",
      minimum = "1",
      maximum = "9",
      requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "Priority is required")
  @Min(value = 1, message = "Priority must be at least 1")
  @Max(value = 9, message = "Priority must not exceed 9")
  private Integer priority;

  @Schema(
      description =
          "Schedule type: ONCE (one-time execution), CRON (cron-based recurring), INTERVAL (fixed interval recurring), MANUAL (manual execution only)",
      example = "ONCE",
      allowableValues = {"ONCE", "CRON", "INTERVAL", "MANUAL"},
      defaultValue = "ONCE")
  private String scheduleType = "ONCE";

  @Schema(
      description =
          "Cron expression for CRON schedule type in Quartz format (6-7 fields). Examples: '0 0 12 * * ?' for daily at noon, '0 */15 * * * ?' for every 15 minutes.",
      example = "0 0 12 * * ?")
  private String cronExpression;

  @Schema(
      description =
          "Interval in seconds for INTERVAL schedule type. Must be greater than 0. Example: 300 (every 5 minutes), 3600 (every hour)",
      example = "300",
      minimum = "1")
  @Min(value = 1, message = "Interval must be at least 1 second")
  private Integer intervalSeconds;

  /**
   * The UUID of the user who owns this task. This is provided in the request body and should match
   * a valid user ID. The owner has full control over the task.
   */
  @NotNull(message = "Owner ID is required")
  private UUID ownerId;

  /**
   * Optional task configuration stored as JSONB in PostgreSQL. This allows flexible configuration
   * storage for different task types.
   */
  private Map<String, Object> taskConfig;
}
