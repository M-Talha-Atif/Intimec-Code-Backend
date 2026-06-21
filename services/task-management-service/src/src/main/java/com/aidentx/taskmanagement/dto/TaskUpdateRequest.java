package com.aidentx.taskmanagement.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import com.aidentx.taskmanagement.model.enums.ScheduleType;

import lombok.Data;

/** Request DTO for updating an existing task. */
@Data
public class TaskUpdateRequest {

  @Size(min = 1, max = 255, message = "Task name must be between 1 and 255 characters")
  private String name;

  @Size(max = 1000, message = "Description must not exceed 1000 characters")
  private String description;

  @Min(value = 1, message = "Priority must be at least 1")
  @Max(value = 9, message = "Priority must not exceed 9")
  private Integer priority;

  /** Updated execution date/time (optional). If provided, must be in the future. */
  @Future(message = "Execution date/time must be in the future")
  private Instant executionDateTime; // Changed from LocalDateTime

  /** Schedule type for the task (ONCE, CRON, INTERVAL, MANUAL) */
  private ScheduleType scheduleType;

  /** Cron expression in Quartz format - required if scheduleType is CRON */
  private String cronExpression;

  /** Interval in seconds - required if scheduleType is INTERVAL */
  @Min(value = 1, message = "Interval must be at least 1 second")
  private Integer intervalSeconds;

  private Map<String, Object> taskConfig;

  private List<UUID> assignedTo;

  private String businessStatus;
}
