package com.aidentx.taskmanagement.service;

import java.time.Instant;
import java.util.UUID;

import org.quartz.SchedulerException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.dto.*;
import com.aidentx.taskmanagement.mapper.TaskMapper;
import com.aidentx.taskmanagement.model.entity.Task;
import com.aidentx.taskmanagement.model.entity.TaskRun;
import com.aidentx.taskmanagement.model.enums.ExecutionStatus;
import com.aidentx.taskmanagement.model.enums.ScheduleType;
import com.aidentx.taskmanagement.repository.TaskRepository;
import com.aidentx.taskmanagement.repository.TaskRunRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of the TaskService interface. Handles business logic for task creation, lifecycle
 * management, and scheduling.
 */
@Service
public class TaskServiceImpl implements TaskService {

  private final TaskRepository taskRepository;
  private final TaskRunRepository taskRunRepository;
  private final QuartzSchedulerService schedulerService;
  private final ApplicationConfigService applicationConfigService;
  private final UserIdentityService userIdentityService;
  private final TaskMapper taskMapper;
  private final ObjectMapper objectMapper; // Keep for partial updates
  private final TaskExecutionStateRegistry stateRegistry;
  private final IUnifiedLogger log;

  public TaskServiceImpl(
      TaskRepository taskRepository,
      TaskRunRepository taskRunRepository,
      QuartzSchedulerService schedulerService,
      ApplicationConfigService applicationConfigService,
      UserIdentityService userIdentityService,
      TaskMapper taskMapper,
      ObjectMapper objectMapper,
      TaskExecutionStateRegistry stateRegistry,
      UnifiedLoggerFactory unifiedLoggerFactory) {
    this.taskRepository = taskRepository;
    this.taskRunRepository = taskRunRepository;
    this.schedulerService = schedulerService;
    this.applicationConfigService = applicationConfigService;
    this.userIdentityService = userIdentityService;
    this.taskMapper = taskMapper;
    this.objectMapper = objectMapper;
    this.stateRegistry = stateRegistry;
    this.log = unifiedLoggerFactory.getLogger(TaskServiceImpl.class);
  }

  @Override
  @Transactional
  public TaskCreationResponse createTask(
      TaskCreationRequest request, UUID applicationId, String tenantId, UUID userId) {
    log.info()
        .message(
            String.format(
                TaskManagementConstants.MSG_CREATING_TASK, request.getName(), applicationId))
        .log();

    // Convert DTO to entity using the mapper
    Task task = taskMapper.toTaskEntity(request, applicationId, tenantId, userId);

    // Save to the database
    Task savedTask = taskRepository.save(task);
    log.info()
        .message(String.format(TaskManagementConstants.MSG_TASK_SAVED, savedTask.getId()))
        .log();

    // Schedule the task with Quartz, unless it's a manual task
    if (savedTask.getScheduleType() != ScheduleType.MANUAL) {
      try {
        if (savedTask.getScheduleType() == ScheduleType.ONCE) {
          schedulerService.scheduleOneTimeTask(savedTask);
        } else {
          schedulerService.scheduleRecurringTask(savedTask);
        }
        log.info()
            .message(
                String.format(
                    TaskManagementConstants.MSG_TASK_SCHEDULED_SUCCESS, savedTask.getId()))
            .log();
      } catch (SchedulerException e) {
        log.error(e).message(TaskManagementConstants.MSG_FAILED_SCHEDULE_QUARTZ).log();
        // If scheduling fails, we must roll back the transaction.
        throw new RuntimeException(TaskManagementConstants.ERR_FAILED_SCHEDULE_TASK, e);
      }
    }

    return TaskCreationResponse.success(savedTask.getId(), request.getExecutionDateTime());
  }

  @Override
  @Transactional(readOnly = true)
  public TaskResponse getTaskByIdAndApplicationId(
      UUID taskId, UUID applicationId, String tenantId) {
    Task task = findTaskById(taskId, tenantId);
    if (!task.getApplicationId().equals(applicationId)) {
      throw new IllegalArgumentException(
          String.format(TaskManagementConstants.ERR_TASK_NOT_BELONG_TO_APP, taskId, applicationId));
    }
    return mapToResponse(task);
  }

  @Override
  @Transactional(readOnly = true)
  public TaskResponse getTaskById(UUID taskId, String tenantId) {
    Task task = findTaskById(taskId, tenantId);
    return mapToResponse(task);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TaskResponse> getAllTasks(String tenantId, Pageable pageable) {
    Page<Task> tasks = taskRepository.findByTenantId(tenantId, pageable);
    return tasks.map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<TaskResponse> getTasksByApplicationId(
      UUID applicationId, String tenantId, Pageable pageable) {
    Page<Task> tasks =
        taskRepository.findByApplicationIdAndTenantId(applicationId, tenantId, pageable);
    return tasks.map(this::mapToResponse);
  }

  @Override
  @Transactional
  public TaskResponse updateTask(
      UUID taskId, UUID applicationId, TaskUpdateRequest request, String tenantId, UUID userId) {
    log.info()
        .message(String.format(TaskManagementConstants.MSG_UPDATING_TASK, taskId, userId))
        .log();

    Task task = findTaskById(taskId, tenantId);
    if (!task.getApplicationId().equals(applicationId)) {
      throw new IllegalArgumentException(
          String.format(TaskManagementConstants.ERR_TASK_NOT_BELONG_TO_APP, taskId, applicationId));
    }

    // Track if we need to reschedule
    boolean needsRescheduling = false;
    Instant oldExecutionTime = task.getNextExecution();
    ScheduleType oldScheduleType = task.getScheduleType();
    String oldCronExpression = task.getCronExpression();
    Integer oldIntervalSeconds = task.getIntervalSeconds();

    // Apply partial updates
    if (request.getName() != null) task.setName(request.getName());
    if (request.getDescription() != null) task.setDescription(request.getDescription());
    if (request.getPriority() != null) task.setPriority(request.getPriority());
    if (request.getAssignedTo() != null) task.setAssignedTo(request.getAssignedTo());

    // Handle schedule type changes
    if (request.getScheduleType() != null && request.getScheduleType() != oldScheduleType) {
      task.setScheduleType(request.getScheduleType());
      needsRescheduling = true;

      // Clear incompatible fields when changing schedule type
      if (request.getScheduleType() == ScheduleType.CRON) {
        // Switching to CRON - clear interval
        task.setIntervalSeconds(null);
      } else if (request.getScheduleType() == ScheduleType.INTERVAL) {
        // Switching to INTERVAL - clear cron expression
        task.setCronExpression(null);
      } else if (request.getScheduleType() == ScheduleType.ONCE) {
        // Switching to ONCE - clear both cron and interval
        task.setCronExpression(null);
        task.setIntervalSeconds(null);
      } else if (request.getScheduleType() == ScheduleType.MANUAL) {
        // Switching to MANUAL - clear all scheduling fields
        task.setCronExpression(null);
        task.setIntervalSeconds(null);
        task.setNextExecution(null);
      }

      log.info()
          .message(
              String.format(
                  "Task %s scheduleType changed from %s to %s, will reschedule",
                  task.getId(), oldScheduleType, request.getScheduleType()))
          .log();
    }

    // Handle cron expression changes - only if we're keeping/using CRON schedule
    if (request.getCronExpression() != null
        && !request.getCronExpression().equals(oldCronExpression)
        && task.getScheduleType() == ScheduleType.CRON) {
      String convertedCron =
          taskMapper.convertAndValidateCronExpression(request.getCronExpression());
      task.setCronExpression(convertedCron);
      needsRescheduling = true;
      log.info()
          .message(
              String.format(
                  "Task %s cronExpression changed from '%s' to '%s', will reschedule",
                  task.getId(), oldCronExpression, convertedCron))
          .log();
    }

    // Handle interval changes - only if we're keeping/using INTERVAL schedule
    if (request.getIntervalSeconds() != null
        && !request.getIntervalSeconds().equals(oldIntervalSeconds)
        && task.getScheduleType() == ScheduleType.INTERVAL) {
      task.setIntervalSeconds(request.getIntervalSeconds());
      needsRescheduling = true;
      log.info()
          .message(
              String.format(
                  "Task %s intervalSeconds changed from %d to %d, will reschedule",
                  task.getId(), oldIntervalSeconds, request.getIntervalSeconds()))
          .log();
    }

    // Check if execution time is being changed
    if (request.getExecutionDateTime() != null
        && !request.getExecutionDateTime().equals(oldExecutionTime)) {
      if (task.getScheduleType() == ScheduleType.MANUAL) {
        log.warn()
            .message(
                String.format("Ignoring executionDateTime update for MANUAL task %s", task.getId()))
            .log();
      } else {
        task.setNextExecution(request.getExecutionDateTime());
        needsRescheduling = true;
        log.info()
            .message(
                String.format(
                    "Task %s executionDateTime changed from %s to %s, will reschedule",
                    task.getId(), oldExecutionTime, request.getExecutionDateTime()))
            .log();
      }
    }

    if (request.getTaskConfig() != null && !request.getTaskConfig().isEmpty()) {
      try {
        task.setTaskConfig(objectMapper.writeValueAsString(request.getTaskConfig()));
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException(TaskManagementConstants.ERR_INVALID_TASK_CONFIG, e);
      }
    }

    task.setUpdatedBy(userId);
    Task updatedTask = taskRepository.save(task);

    // Reschedule if needed
    if (needsRescheduling) {
      try {
        log.info()
            .message(
                String.format(
                    "Rescheduling task %s with new execution time: %s",
                    updatedTask.getId(), updatedTask.getNextExecution()))
            .log();

        // Reschedule based on task type
        if (updatedTask.getScheduleType() == ScheduleType.ONCE) {
          schedulerService.scheduleOneTimeTask(updatedTask);
        } else if (updatedTask.getScheduleType() == ScheduleType.CRON
            || updatedTask.getScheduleType() == ScheduleType.INTERVAL) {
          schedulerService.scheduleRecurringTask(updatedTask);
        }

        log.info()
            .message(
                String.format("Task %s rescheduled successfully with Quartz", updatedTask.getId()))
            .log();
      } catch (SchedulerException e) {
        log.error(e)
            .message(
                String.format(
                    "Failed to reschedule task %s: %s", updatedTask.getId(), e.getMessage()))
            .log();
        // Don't throw - the task update succeeded, just the rescheduling failed
        // The task will still have the updated execution time in the database
      }
    }

    log.info()
        .message(
            String.format(
                TaskManagementConstants.MSG_TASK_UPDATED_SUCCESS,
                updatedTask.getId(),
                updatedTask.getVersion()))
        .log();
    return mapToResponse(updatedTask);
  }

  @Override
  @Transactional
  public void deleteTask(UUID taskId, UUID applicationId, String tenantId) {
    log.info()
        .message(String.format(TaskManagementConstants.MSG_DELETING_TASK, taskId, applicationId))
        .log();

    Task task = findTaskById(taskId, tenantId);
    if (!task.getApplicationId().equals(applicationId)) {
      throw new IllegalArgumentException(
          String.format(TaskManagementConstants.ERR_TASK_NOT_BELONG_TO_APP, taskId, applicationId));
    }

    // First, cancel the scheduled job in Quartz
    try {
      schedulerService.cancelTask(taskId);
      log.info().message(String.format(TaskManagementConstants.MSG_UNSCHEDULED_TASK, taskId)).log();
    } catch (SchedulerException e) {
      // Log the error but proceed with deletion. The job might not exist if it was
      // MANUAL.
      log.warn()
          .message(String.format(TaskManagementConstants.MSG_COULD_NOT_UNSCHEDULE, taskId))
          .log();
    }

    taskRepository.delete(task);
    log.info()
        .message(String.format(TaskManagementConstants.MSG_TASK_DELETED_SUCCESS, taskId))
        .log();
  }

  @Override
  @Transactional
  public void deleteAllTasksByApplicationId(UUID applicationId, String tenantId) {
    log.info()
        .message(String.format("Deleting all tasks for application %s (tenant %s)", applicationId, tenantId))
        .log();

    java.util.List<Task> tasks =
        taskRepository.findAllByApplicationIdAndTenantId(applicationId, tenantId);

    for (Task task : tasks) {
      try {
        schedulerService.cancelTask(task.getId());
      } catch (Exception e) {
        log.warn()
            .message(String.format("Could not unschedule task %s during cascade delete: %s",
                task.getId(), e.getMessage()))
            .log();
      }
    }

    taskRunRepository.deleteAllByApplicationIdAndTenantId(applicationId, tenantId);
    taskRepository.deleteAllByApplicationIdAndTenantId(applicationId, tenantId);
    log.info()
        .message(String.format("Deleted %d tasks for application %s", tasks.size(), applicationId))
        .log();
  }

  @Override
  @Transactional
  public TaskExecutionResponse executeTask(UUID taskId, String tenantId) {
    Task task = findTaskById(taskId, tenantId);

    // Validate that only MANUAL tasks can be executed via this endpoint
    if (task.getScheduleType() != ScheduleType.MANUAL) {
      String errorMsg =
          String.format(
              "Cannot manually execute task %s. Only MANUAL tasks can be executed via this endpoint. This task has schedule type: %s",
              taskId, task.getScheduleType());
      log.warn().message(errorMsg).log();
      throw new IllegalArgumentException(errorMsg);
    }

    String executionId = UUID.randomUUID().toString();
    Instant triggeredAt = Instant.now();

    // Create the TaskRun immediately with PENDING status so it's visible right away
    TaskRun taskRun = new TaskRun();
    taskRun.setExecutionId(executionId);
    taskRun.setTaskId(task.getId());
    taskRun.setTenantId(task.getTenantId());
    taskRun.setApplicationId(task.getApplicationId());
    taskRun.setStatus(ExecutionStatus.PENDING);
    taskRun.setExecutionNodeId(getExecutionNodeId());
    taskRun.setStartTime(triggeredAt);

    // Estimate total from last successful run
    taskRunRepository.findTopByTaskIdAndTenantIdAndStatusAndPreviewFalseOrderByEndTimeDesc(
        task.getId(), task.getTenantId(), ExecutionStatus.COMPLETED)
        .ifPresent(prev -> taskRun.setEstimatedTotalCount(prev.getProcessedCount()));

    taskRunRepository.save(taskRun);

    log.info()
        .message(
            String.format("Created TaskRun with executionId: %s, status: PENDING", executionId))
        .log();

    try {
      // For MANUAL tasks, ensure a durable job exists in Quartz before triggering
      if (task.getScheduleType() == ScheduleType.MANUAL
          && !schedulerService.isTaskScheduled(taskId)) {
        schedulerService.scheduleManualTask(task);
      }
      schedulerService.triggerTask(taskId, executionId);
      log.info()
          .message(String.format(TaskManagementConstants.MSG_TASK_TRIGGERED, taskId, executionId))
          .log();

      return TaskExecutionResponse.builder()
          .taskId(taskId)
          .executionId(executionId)
          .message(TaskManagementConstants.MSG_EXECUTION_TRIGGERED_SUCCESS)
          .triggeredAt(triggeredAt)
          .build();
    } catch (SchedulerException e) {
      log.error(e)
          .message(String.format(TaskManagementConstants.MSG_FAILED_TRIGGER_TASK, taskId))
          .log();
      throw new RuntimeException(TaskManagementConstants.ERR_FAILED_EXECUTE_TASK, e);
    }
  }

  @Override
  @Transactional
  public TaskExecutionResponse executePreview(UUID taskId, String tenantId) {
    Task task = findTaskById(taskId, tenantId);

    String executionId = UUID.randomUUID().toString();
    Instant triggeredAt = Instant.now();

    // Create the TaskRun immediately with PENDING status and isPreview=true
    TaskRun taskRun = new TaskRun();
    taskRun.setExecutionId(executionId);
    taskRun.setTaskId(task.getId());
    taskRun.setTenantId(task.getTenantId());
    taskRun.setApplicationId(task.getApplicationId());
    taskRun.setStatus(ExecutionStatus.PENDING);
    taskRun.setPreview(true);
    taskRun.setExecutionNodeId(getExecutionNodeId());
    taskRun.setStartTime(triggeredAt);

    // Estimate total from last successful run
    taskRunRepository.findTopByTaskIdAndTenantIdAndStatusAndPreviewFalseOrderByEndTimeDesc(
        task.getId(), task.getTenantId(), ExecutionStatus.COMPLETED)
        .ifPresent(prev -> taskRun.setEstimatedTotalCount(prev.getProcessedCount()));

    taskRunRepository.save(taskRun);

    log.info()
        .message(
            String.format("Created preview TaskRun with executionId: %s, status: PENDING", executionId))
        .log();

    try {
      // Ensure a durable job exists in Quartz before triggering
      if (!schedulerService.isTaskScheduled(taskId)) {
        schedulerService.scheduleManualTask(task);
      }
      schedulerService.triggerTask(taskId, executionId, true);
      log.info()
          .message(String.format("Preview task triggered: taskId=%s, executionId=%s", taskId, executionId))
          .log();

      return TaskExecutionResponse.builder()
          .taskId(taskId)
          .executionId(executionId)
          .message("Preview execution triggered successfully")
          .triggeredAt(triggeredAt)
          .build();
    } catch (SchedulerException e) {
      log.error(e)
          .message(String.format("Failed to trigger preview for task %s", taskId))
          .log();
      throw new RuntimeException("Failed to trigger preview execution", e);
    }
  }

  private String getExecutionNodeId() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      return "unknown-node";
    }
  }

  @Override
  public void pauseTask(UUID taskId, String tenantId) {
    log.info().message(String.format(TaskManagementConstants.MSG_PAUSING_TASK, taskId)).log();
    findTaskById(taskId, tenantId); // Validate task exists
    // Signal cooperative pause: the job will block at its next batch boundary.
    stateRegistry.requestPause(taskId);
    // Mark the latest run as SUSPENDED so the UI reflects the paused state.
    taskRunRepository.findTopByTaskIdAndTenantIdOrderByCreatedAtDesc(taskId, tenantId)
        .ifPresent(run -> {
          run.setStatus(ExecutionStatus.SUSPENDED);
          taskRunRepository.save(run);
        });
    // Also update the Task entity status to SUSPENDED so the polling task endpoint
    // returns executionStatus=SUSPENDED, allowing the UI to switch from Pause→Resume.
    taskRepository.findById(taskId).ifPresent(task -> {
      task.setStatus(ExecutionStatus.SUSPENDED);
      taskRepository.save(task);
    });
    try {
      // Also pause the Quartz trigger so no new fires start while paused.
      schedulerService.pauseTask(taskId);
    } catch (SchedulerException e) {
      throw new RuntimeException(TaskManagementConstants.ERR_FAILED_PAUSE_TASK, e);
    }
  }

  @Override
  public void resumeTask(UUID taskId, String tenantId) {
    log.info().message(String.format(TaskManagementConstants.MSG_RESUMING_TASK, taskId)).log();
    findTaskById(taskId, tenantId); // Validate task exists
    // Wake the blocked execution thread.
    stateRegistry.requestResume(taskId);
    // Restore run status to RUNNING.
    taskRunRepository.findTopByTaskIdAndTenantIdOrderByCreatedAtDesc(taskId, tenantId)
        .ifPresent(run -> {
          run.setStatus(ExecutionStatus.RUNNING);
          taskRunRepository.save(run);
        });
    // Restore the Task entity status to RUNNING so the UI switches back from Resume→Pause.
    taskRepository.findById(taskId).ifPresent(task -> {
      task.setStatus(ExecutionStatus.RUNNING);
      taskRepository.save(task);
    });
    try {
      // Resume the Quartz trigger so future scheduled fires are allowed again.
      schedulerService.resumeTask(taskId);
    } catch (SchedulerException e) {
      throw new RuntimeException(TaskManagementConstants.ERR_FAILED_RESUME_TASK, e);
    }
  }

  @Override
  public void terminateTask(UUID taskId, String tenantId) {
    log.info().message(String.format(TaskManagementConstants.MSG_TERMINATING_TASK, taskId)).log();
    Task task = findTaskById(taskId, tenantId);
    // Signal cooperative cancellation — the job stops at the next batch boundary
    // and also wakes any thread that is currently blocking on a pause.
    stateRegistry.requestCancel(taskId);
    try {
      // Remove the Quartz job entry so no future scheduled fires occur.
      schedulerService.cancelTask(taskId);
    } catch (SchedulerException e) {
      throw new RuntimeException(TaskManagementConstants.ERR_FAILED_TERMINATE_TASK, e);
    }

    // If no job thread is actively running (Handle not registered), the cooperative
    // signal has nobody to reach. Directly update DB status so the task doesn't
    // stay stuck in RUNNING/SUSPENDED forever.
    if (!stateRegistry.isRegistered(taskId)) {
      ExecutionStatus current = task.getStatus();
      if (current == ExecutionStatus.RUNNING || current == ExecutionStatus.SUSPENDED) {
        task.setStatus(ExecutionStatus.CANCELLED);
        task.setLastRunFinishTimestamp(Instant.now());
        task.setLastExecution(Instant.now());
        taskRepository.save(task);

        // Also mark the latest TaskRun as CANCELLED
        taskRunRepository.findTopByTaskIdAndTenantIdOrderByCreatedAtDesc(taskId, tenantId)
            .ifPresent(run -> {
              if (run.getStatus() == ExecutionStatus.RUNNING || run.getStatus() == ExecutionStatus.SUSPENDED) {
                run.setStatus(ExecutionStatus.CANCELLED);
                run.setEndTime(Instant.now());
                run.setErrorMessage("Cancelled by user (no active job thread)");
                taskRunRepository.save(run);
              }
            });

        log.info().message(String.format("Task %s directly marked CANCELLED (no active job thread)", taskId)).log();
      }
    }
  }

  // Helper method to find a task or throw a standard exception
  private Task findTaskById(UUID taskId, String tenantId) {
    return taskRepository
        .findByIdAndTenantId(taskId, tenantId)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    String.format(TaskManagementConstants.ERR_TASK_NOT_FOUND, taskId)));
  }

  // Helper method to centralize mapping and reference resolution
  private TaskResponse mapToResponse(Task task) {
    return taskMapper.toTaskResponse(
        task,
        userId -> userIdentityService.getUserById(task.getTenantId(), userId),
        userIds -> userIdentityService.getUsersByIds(task.getTenantId(), userIds),
        appId -> applicationConfigService.getApplicationReference(task.getTenantId(), appId),
        taskId -> schedulerService.getNextFireTime(taskId));
  }
}
