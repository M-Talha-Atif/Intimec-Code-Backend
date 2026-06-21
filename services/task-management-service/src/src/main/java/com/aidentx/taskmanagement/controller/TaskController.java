package com.aidentx.taskmanagement.controller;

import java.time.Instant;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.audit.IgaTaskAuditEvent;
import com.aidentx.taskmanagement.audit.IgaTaskAuditPublisher;
import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.aidentx.taskmanagement.service.TaskExecutionLogService;
import com.aidentx.taskmanagement.service.TaskPreviewService;
import com.aidentx.taskmanagement.service.TaskRunService;
import com.aidentx.taskmanagement.service.TaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for task management operations. Provides CRUD, control, and history operations
 * for tasks.
 */
@Tag(name = "Task Management", description = "APIs for managing and controlling tasks")
@RestController
@RequestMapping("/api/v1")
public class TaskController {

  private final TaskService taskService;
  private final TaskRunService taskRunService;
  private final TaskExecutionLogService logService;
  private final TaskPreviewService taskPreviewService;
  private final IgaTaskAuditPublisher auditPublisher;
  private final ObjectMapper objectMapper;
  private final IUnifiedLogger log;

  public TaskController(
      TaskService taskService,
      TaskRunService taskRunService,
      TaskExecutionLogService logService,
      TaskPreviewService taskPreviewService,
      IgaTaskAuditPublisher auditPublisher,
      ObjectMapper objectMapper,
      UnifiedLoggerFactory unifiedLoggerFactory) {
    this.taskService = taskService;
    this.taskRunService = taskRunService;
    this.logService = logService;
    this.taskPreviewService = taskPreviewService;
    this.auditPublisher = auditPublisher;
    this.objectMapper = objectMapper;
    this.log = unifiedLoggerFactory.getLogger(TaskController.class);
  }

  @Operation(summary = "Create a new task for an application")
  @PostMapping("/applications/{applicationId}/tasks")
  public ResponseEntity<TaskCreationResponse> createTask(
      @NotNull @Parameter(description = "Application UUID") @PathVariable UUID applicationId,
      @Valid @RequestBody TaskCreationRequest request,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "User UUID") @RequestHeader("X-User-ID") UUID userId) {

    log.info()
        .message(
            String.format(
                TaskManagementConstants.MSG_RECEIVED_CREATE_TASK_REQUEST,
                request.getName(),
                applicationId))
        .log();
    TaskCreationResponse response =
        taskService.createTask(request, applicationId, tenantId, userId);

    ObjectNode stateAfter = objectMapper.createObjectNode();
    stateAfter.put("id", response.getTaskId().toString());
    stateAfter.put("name", request.getName());
    if (request.getCategory() != null) stateAfter.put("category", request.getCategory());
    stateAfter.put("applicationId", applicationId.toString());
    stateAfter.put("tenantId", tenantId);
    auditPublisher.publish(new IgaTaskAuditEvent(
        userId.toString(), resolveClientIp(), "task_created",
        response.getTaskId().toString(), request.getName(),
        applicationId.toString(), "low", tenantId,
        null, stateAfter, Instant.now()));

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Operation(summary = "Get a task by ID within an application")
  @GetMapping("/applications/{applicationId}/tasks/{taskId}")
  public ResponseEntity<TaskResponse> getTask(
      @Parameter(description = "Application UUID") @PathVariable UUID applicationId,
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId) {

    TaskResponse response =
        taskService.getTaskByIdAndApplicationId(taskId, applicationId, tenantId);
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "List all tasks for an application with pagination")
  @GetMapping("/applications/{applicationId}/tasks")
  public ResponseEntity<PagedTaskResponse> getAllTasksByApplication(
      @Parameter(description = "Application UUID") @PathVariable UUID applicationId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "Pagination parameters") Pageable pageable) {

    Page<TaskResponse> page =
        taskService.getTasksByApplicationId(applicationId, tenantId, pageable);
    PagedTaskResponse response =
        new PagedTaskResponse(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.isFirst(),
            page.isLast(),
            page.getNumberOfElements(),
            page.isEmpty());
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "List all tasks for a tenant with pagination")
  @GetMapping("/tasks")
  public ResponseEntity<PagedTaskResponse> getAllTasks(
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "Pagination parameters") Pageable pageable) {

    Page<TaskResponse> page = taskService.getAllTasks(tenantId, pageable);
    PagedTaskResponse response =
        new PagedTaskResponse(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalPages(),
            page.getTotalElements(),
            page.isFirst(),
            page.isLast(),
            page.getNumberOfElements(),
            page.isEmpty());
    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Update an existing task")
  @PutMapping("/applications/{applicationId}/tasks/{taskId}")
  public ResponseEntity<TaskResponse> updateTask(
      @Parameter(description = "Application UUID") @PathVariable UUID applicationId,
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Valid @RequestBody TaskUpdateRequest request,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "User UUID") @RequestHeader("X-User-ID") UUID userId) {

    JsonNode stateBefore = null;
    try {
      TaskResponse before = taskService.getTaskByIdAndApplicationId(taskId, applicationId, tenantId);
      stateBefore = buildTaskState(before);
    } catch (Exception e) {
      log.warn().message("Could not capture stateBefore for task audit log").detail("taskId", taskId.toString()).log();
    }

    TaskResponse response =
        taskService.updateTask(taskId, applicationId, request, tenantId, userId);

    auditPublisher.publish(new IgaTaskAuditEvent(
        userId.toString(), resolveClientIp(), "task_updated",
        taskId.toString(), response.getName(),
        applicationId.toString(), "low", tenantId,
        stateBefore, buildTaskState(response), Instant.now()));

    return ResponseEntity.ok(response);
  }

  @Operation(summary = "Delete a task")
  @DeleteMapping("/applications/{applicationId}/tasks/{taskId}")
  public ResponseEntity<Void> deleteTask(
      @Parameter(description = "Application UUID") @PathVariable UUID applicationId,
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "User UUID") @RequestHeader(value = "X-User-ID", required = false) UUID userId) {

    JsonNode stateBefore = null;
    String taskName = taskId.toString();
    try {
      TaskResponse before = taskService.getTaskByIdAndApplicationId(taskId, applicationId, tenantId);
      stateBefore = buildTaskState(before);
      taskName = before.getName();
    } catch (Exception ignored) {}

    taskService.deleteTask(taskId, applicationId, tenantId);

    auditPublisher.publish(new IgaTaskAuditEvent(
        userId != null ? userId.toString() : "00000000-0000-0000-0000-000000000000",
        resolveClientIp(), "task_deleted",
        taskId.toString(), taskName,
        applicationId.toString(), "medium", tenantId,
        stateBefore, null, Instant.now()));

    return ResponseEntity.noContent().build();
  }

  @Operation(summary = "Delete all tasks for an application (cascade delete)")
  @DeleteMapping("/applications/{applicationId}/tasks")
  public ResponseEntity<Void> deleteAllTasksForApplication(
      @Parameter(description = "Application UUID") @PathVariable UUID applicationId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "User UUID") @RequestHeader(value = "X-User-ID", required = false) UUID userId) {

    taskService.deleteAllTasksByApplicationId(applicationId, tenantId);
    return ResponseEntity.noContent().build();
  }

  @Operation(
      summary = "Trigger immediate execution of a task",
      tags = {"Task Control"})
  @PostMapping("/tasks/{taskId}/execute")
  public ResponseEntity<TaskExecutionResponse> executeTask(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "User UUID") @RequestHeader(value = "X-User-ID", required = false) UUID userId) {

    String taskName = taskId.toString();
    try {
      taskName = taskService.getTaskById(taskId, tenantId).getName();
    } catch (Exception ignored) {}

    TaskExecutionResponse response = taskService.executeTask(taskId, tenantId);

    auditPublisher.publish(new IgaTaskAuditEvent(
        userId != null ? userId.toString() : "00000000-0000-0000-0000-000000000000",
        resolveClientIp(), "task_executed",
        taskId.toString(), taskName,
        null, "medium", tenantId,
        null, null, Instant.now()));

    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Dry-run preview of a task execution",
      description = "Triggers the full sync pipeline asynchronously with dry_run=true via the Quartz job. "
          + "Returns immediately with an executionId — the preview runs in the background and is "
          + "cancellable, pausable, and resumable just like a real execution.",
      tags = {"Task Control"})
  @PostMapping("/tasks/{taskId}/preview")
  public ResponseEntity<TaskExecutionResponse> previewTask(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId) {

    log.info()
        .message(String.format("Received preview request for task %s", taskId))
        .log();
    TaskExecutionResponse response = taskService.executePreview(taskId, tenantId);
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Pause a scheduled task",
      tags = {"Task Control"})
  @PostMapping("/tasks/{taskId}/pause")
  public ResponseEntity<Void> pauseTask(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId) {

    taskService.pauseTask(taskId, tenantId);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Resume a paused task",
      tags = {"Task Control"})
  @PostMapping("/tasks/{taskId}/resume")
  public ResponseEntity<Void> resumeTask(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId) {

    taskService.resumeTask(taskId, tenantId);
    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Terminate a running/scheduled task",
      tags = {"Task Control"})
  @PostMapping("/tasks/{taskId}/terminate")
  public ResponseEntity<Void> terminateTask(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "User UUID") @RequestHeader(value = "X-User-ID", required = false) UUID userId) {

    JsonNode stateBefore = null;
    String taskName = taskId.toString();
    try {
      TaskResponse task = taskService.getTaskById(taskId, tenantId);
      stateBefore = buildTaskState(task);
      taskName = task.getName();
    } catch (Exception ignored) {}

    taskService.terminateTask(taskId, tenantId);

    auditPublisher.publish(new IgaTaskAuditEvent(
        userId != null ? userId.toString() : "00000000-0000-0000-0000-000000000000",
        resolveClientIp(), "task_terminated",
        taskId.toString(), taskName,
        null, "high", tenantId,
        stateBefore, null, Instant.now()));

    return ResponseEntity.ok().build();
  }

  @Operation(
      summary = "Get task execution history with pagination",
      description = "Retrieves a paginated list of all execution runs for a specific task.",
      tags = {"Task Runs"})
  @GetMapping("/tasks/{taskId}/runs")
  public ResponseEntity<PagedTaskRunResponse> getTaskRunHistory(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "Pagination parameters (e.g., ?page=0&size=20&sort=createdAt,desc)")
          Pageable pageable) {

    // 1. Call the service, which now returns a Page object containing DTOs and
    // pagination info.
    Page<TaskRunResponse> runsPage = taskRunService.getTaskRunHistory(taskId, tenantId, pageable);

    // 2. Construct the final API response DTO using the data from the Page object.
    PagedTaskRunResponse response =
        new PagedTaskRunResponse(
            runsPage.getContent(),
            runsPage.getNumber(),
            runsPage.getSize(),
            runsPage.getTotalPages(),
            runsPage.getTotalElements(),
            runsPage.isFirst(),
            runsPage.isLast(),
            runsPage.getNumberOfElements(),
            runsPage.isEmpty());

    // 3. Return the structured paginated response.
    return ResponseEntity.ok(response);
  }

  @Operation(
      summary = "Get details of a specific task run",
      tags = {"Task Runs"})
  @GetMapping("/tasks/{taskId}/runs/{executionId}")
  public ResponseEntity<TaskRunResponse> getTaskRun(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Execution ID") @PathVariable String executionId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId) {

    TaskRunResponse run = taskRunService.getTaskRun(executionId, taskId, tenantId);
    return ResponseEntity.ok(run);
  }

  // -------------------------------------------------------------------------
  // Audit helpers
  // -------------------------------------------------------------------------

  private JsonNode buildTaskState(TaskResponse task) {
    ObjectNode node = objectMapper.createObjectNode();
    if (task.getId() != null) node.put("id", task.getId().toString());
    if (task.getName() != null) node.put("name", task.getName());
    if (task.getDescription() != null) node.put("description", task.getDescription());
    if (task.getTaskType() != null) node.put("taskType", task.getTaskType());
    if (task.getCategory() != null) node.put("category", task.getCategory());
    if (task.getPriority() != null) node.put("priority", task.getPriority());
    if (task.getScheduleType() != null) node.put("scheduleType", task.getScheduleType().name());
    if (task.getCronExpression() != null) node.put("cronExpression", task.getCronExpression());
    if (task.getIntervalSeconds() != null) node.put("intervalSeconds", task.getIntervalSeconds());
    if (task.getBindingType() != null) node.put("bindingType", task.getBindingType().name());
    if (task.getExecutionConfig() != null) node.put("executionConfig", task.getExecutionConfig());
    if (task.getMisfireAction() != null) node.put("misfireAction", task.getMisfireAction().name());
    if (task.getIsResilient() != null) node.put("isResilient", task.getIsResilient());
    if (task.getTaskConfig() != null) node.set("taskConfig", objectMapper.valueToTree(task.getTaskConfig()));
    if (task.getApplication() != null && task.getApplication().getId() != null)
      node.put("applicationId", task.getApplication().getId().toString());
    if (task.getTenantId() != null) node.put("tenantId", task.getTenantId());
    return node;
  }

  private String resolveClientIp() {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs != null) {
        HttpServletRequest req = attrs.getRequest();
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
          return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
      }
    } catch (Exception ignored) {
    }
    return "unknown";
  }
}
