package com.aidentx.taskmanagement.mapper;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.dto.ApplicationReference;
import com.aidentx.taskmanagement.dto.TaskCreationRequest;
import com.aidentx.taskmanagement.dto.TaskResponse;
import com.aidentx.taskmanagement.model.dto.UserReference;
import com.aidentx.taskmanagement.model.entity.Task;
import com.aidentx.taskmanagement.model.enums.BindingType;
import com.aidentx.taskmanagement.model.enums.PersistenceStatus;
import com.aidentx.taskmanagement.model.enums.ScheduleType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mapper class for converting between Task entities and DTOs. This class centralizes the
 * transformation logic, promoting code reuse and separating concerns from the service layer.
 */
@Component
public class TaskMapper {

  private final ObjectMapper objectMapper;
  private final IUnifiedLogger log;

  public TaskMapper(ObjectMapper objectMapper, UnifiedLoggerFactory loggerFactory) {
    this.objectMapper = objectMapper;
    this.log = loggerFactory.getLogger(TaskMapper.class);
  }

  /**
   * Converts a Task entity to a TaskResponse DTO.
   *
   * @param task The Task entity.
   * @param resolveUserRef A function to resolve a user ID to a UserReference.
   * @param resolveUsersRef A function to resolve a list of user IDs to a list of UserReferences.
   * @param resolveAppRef A function to resolve an application ID to an ApplicationReference.
   * @param getNextFireTime A function to get the next scheduled execution time from Quartz.
   * @return A TaskResponse DTO.
   */
  public TaskResponse toTaskResponse(
      Task task,
      Function<UUID, UserReference> resolveUserRef,
      Function<List<UUID>, List<UserReference>> resolveUsersRef,
      Function<UUID, ApplicationReference> resolveAppRef,
      Function<UUID, java.util.Date> getNextFireTime) {
    if (task == null) {
      return null;
    }

    TaskResponse response = new TaskResponse();
    response.setId(task.getId());
    response.setName(task.getName());
    response.setDescription(task.getDescription());
    response.setCategory(task.getCategory());
    response.setPriority(task.getPriority());

    // Resolve references
    response.setOwner(resolveUserRef.apply(task.getOwnerId()));
    response.setAssignedTo(resolveUsersRef.apply(task.getAssignedTo()));
    response.setApplication(resolveAppRef.apply(task.getApplicationId()));
    response.setCreatedBy(resolveUserRef.apply(task.getCreatedBy()));
    response.setUpdatedBy(resolveUserRef.apply(task.getUpdatedBy()));

    response.setTenantId(task.getTenantId());
    response.setPersistenceStatus(task.getPersistenceStatus());
    response.setExecutionStatus(task.getStatus());

    // Scheduling info
    response.setScheduleType(task.getScheduleType());
    response.setCronExpression(task.getCronExpression());
    response.setIntervalSeconds(task.getIntervalSeconds());
    response.setBindingType(task.getBindingType());

    // For CRON and INTERVAL tasks, get next execution from Quartz
    // For ONCE tasks, use the database value
    if (task.getScheduleType() == com.aidentx.taskmanagement.model.enums.ScheduleType.CRON
        || task.getScheduleType() == com.aidentx.taskmanagement.model.enums.ScheduleType.INTERVAL) {
      java.util.Date nextFireTime = getNextFireTime.apply(task.getId());
      response.setNextExecution(
          nextFireTime != null
              ? nextFireTime.toInstant().atZone(ZoneId.systemDefault()).toInstant()
              : null);
    } else {
      response.setNextExecution(task.getNextExecution());
    }

    response.setLastExecution(task.getLastExecution());

    // Execution tracking
    response.setLastRunStartTimestamp(task.getLastRunStartTimestamp());
    response.setLastRunFinishTimestamp(task.getLastRunFinishTimestamp());
    response.setExecutionNodeId(task.getExecutionNodeId());

    // Metadata
    response.setCreatedAt(task.getCreatedAt());
    response.setUpdatedAt(task.getUpdatedAt());
    response.setVersion(task.getVersion());

    // Deserialize taskConfig JSON
    response.setTaskConfig(stringToMap(task.getTaskConfig()));

    return response;
  }

  /**
   * Converts a TaskCreationRequest DTO to a Task entity.
   *
   * @param request The TaskCreationRequest DTO.
   * @param applicationId The ID of the application this task belongs to.
   * @param tenantId The tenant ID.
   * @param userId The ID of the user creating the task.
   * @return A Task entity.
   */
  public Task toTaskEntity(
      TaskCreationRequest request, UUID applicationId, String tenantId, UUID userId) {
    Task task = new Task();

    // Basic fields
    task.setName(request.getName());
    task.setDescription(request.getDescription());
    task.setCategory(request.getCategory());
    task.setPriority(request.getPriority());

    // User and tenant context
    task.setApplicationId(applicationId);
    task.setOwnerId(request.getOwnerId());
    task.setTenantId(tenantId);
    task.setCreatedBy(userId);
    task.setUpdatedBy(userId);

    // Convert taskConfig map to JSON string
    task.setTaskConfig(mapToString(request.getTaskConfig()));

    // Scheduling
    ScheduleType scheduleType = parseScheduleType(request.getScheduleType());
    task.setScheduleType(scheduleType);
    validateAndSetScheduleFields(task, request, scheduleType);

    // Set default values for new tasks
    task.setPersistenceStatus(PersistenceStatus.PERSISTENT);
    task.setBindingType(BindingType.LOOSE);
    task.setVersion(1); // Initial version

    return task;
  }

  private void validateAndSetScheduleFields(
      Task task, TaskCreationRequest request, ScheduleType scheduleType) {
    switch (scheduleType) {
      case ONCE:
        if (request.getExecutionDateTime() == null) {
          throw new IllegalArgumentException(
              TaskManagementConstants.ERR_EXECUTION_DATETIME_REQUIRED);
        }
        if (request.getExecutionDateTime().isBefore(Instant.now())) {
          throw new IllegalArgumentException(TaskManagementConstants.ERR_EXECUTION_DATETIME_FUTURE);
        }
        log.info()
            .message(
                String.format(
                    "Setting nextExecution for ONCE task: received=%s (%d), currentTime=%s (%d)",
                    request.getExecutionDateTime(),
                    request.getExecutionDateTime().toEpochMilli(),
                    Instant.now(),
                    Instant.now().toEpochMilli()))
            .log();
        task.setNextExecution(request.getExecutionDateTime());
        break;
      case CRON:
        if (request.getCronExpression() == null || request.getCronExpression().trim().isEmpty()) {
          throw new IllegalArgumentException(TaskManagementConstants.ERR_CRON_EXPRESSION_REQUIRED);
        }
        String cronExpr = request.getCronExpression().trim();
        // Convert Unix cron (5 fields) to Quartz cron (6 fields) if needed, and
        // validate
        String quartzCronExpr = convertAndValidateCronExpression(cronExpr);
        task.setCronExpression(quartzCronExpr);
        task.setNextExecution(request.getExecutionDateTime()); // Optional start time
        break;
      case INTERVAL:
        if (request.getIntervalSeconds() == null || request.getIntervalSeconds() <= 0) {
          throw new IllegalArgumentException(TaskManagementConstants.ERR_INTERVAL_SECONDS_POSITIVE);
        }
        task.setIntervalSeconds(request.getIntervalSeconds());
        task.setNextExecution(request.getExecutionDateTime()); // Optional start time
        break;
      case MANUAL:
        task.setNextExecution(null); // Manual tasks have no scheduled execution
        break;
    }
  }

  private ScheduleType parseScheduleType(String scheduleTypeStr) {
    if (scheduleTypeStr == null || scheduleTypeStr.isBlank()) {
      return ScheduleType.ONCE; // Default to ONCE if not provided
    }
    try {
      return ScheduleType.valueOf(scheduleTypeStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          String.format(TaskManagementConstants.ERR_INVALID_SCHEDULE_TYPE, scheduleTypeStr));
    }
  }

  private String mapToString(Map<String, Object> map) {
    if (map == null || map.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(map);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException(TaskManagementConstants.ERR_INVALID_TASK_CONFIG_FORMAT, e);
    }
  }

  private Map<String, Object> stringToMap(String json) {
    if (json == null || json.trim().isEmpty()) {
      return Collections.emptyMap();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      // Log this warning but don't fail the entire request.
      // A corrupted JSON config shouldn't prevent retrieval of the task itself.
      log.warn()
          .message("Failed to deserialize task config JSON: " + e.getMessage())
          .failureReason(e.getLocalizedMessage())
          .log();
      return Collections.emptyMap();
    }
  }

  /** Validates a Quartz cron expression (6-7 fields). Examples: "0 0 12 * * ?" = Daily at noon */
  public String convertAndValidateCronExpression(String cronExpression) {
    if (cronExpression == null || cronExpression.trim().isEmpty()) {
      throw new IllegalArgumentException("Cron expression cannot be empty");
    }

    String trimmed = cronExpression.trim();
    String[] parts = trimmed.split("\\s+");

    // Quartz requires 6 or 7 fields (second minute hour day month weekday [year])
    if (parts.length < 6 || parts.length > 7) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid cron expression '%s'. Quartz requires 6-7 fields (second minute hour day-of-month month day-of-week [year]), but found %d fields. "
                  + "Examples: '0 0 12 * * ?' = Daily at noon, '0 */15 * * * ?' = Every 15 minutes",
              cronExpression, parts.length));
    }

    // Validate the Quartz cron expression by attempting to parse it
    try {
      new org.quartz.CronExpression(trimmed);
    } catch (Exception e) {
      throw new IllegalArgumentException(
          String.format(
              "Invalid cron expression '%s': %s. Quartz format: 'seconds minutes hours day-of-month month day-of-week [year]'. "
                  + "Example: '0 0 12 * * ?' = Daily at noon, '0 */15 * * * ?' = Every 15 minutes.",
              cronExpression, e.getMessage()),
          e);
    }

    return trimmed;
  }
}
