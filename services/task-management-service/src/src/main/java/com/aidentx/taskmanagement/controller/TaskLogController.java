package com.aidentx.taskmanagement.controller;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aidentx.taskmanagement.dto.TaskLogStreamResponse;
import com.aidentx.taskmanagement.service.TaskExecutionLogService;
import com.aidentx.taskmanagement.service.TaskRunService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Task Logs", description = "APIs for streaming and viewing task execution logs")
@RestController
@RequestMapping("/api/v1/tasks")
public class TaskLogController {

  private final TaskExecutionLogService logService;
  private final TaskRunService taskRunService;

  public TaskLogController(
      TaskExecutionLogService logService,
      TaskRunService taskRunService) {
    this.logService = logService;
    this.taskRunService = taskRunService;
  }

  // --- Task-Level Log Endpoints ---

  @Operation(
      summary = "Stream logs for a task (all runs)",
      description =
          "Streams task execution logs for all runs of a task in real-time using Server-Sent Events.")
  @GetMapping(value = "/{taskId}/logs/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamTaskLogs(
      @Parameter(description = "Task UUID", required = true) @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID", required = true) @RequestHeader("X-Tenant-ID")
          String tenantId,
      @Parameter(
              description =
                  "Comma-separated list of log levels to filter by (e.g., 'ERROR,INFO,WARN')")
          @RequestParam(required = false)
          String levels) {
    List<String> logLevels =
        levels != null && !levels.trim().isEmpty()
            ? Arrays.stream(levels.split(",")).map(String::trim).collect(Collectors.toList())
            : null;
    return logService.streamLogsForTask(taskId, tenantId, logLevels);
  }

  @Operation(
      summary = "Get historical logs for a task (all runs)",
      description = "Retrieves historical logs for all runs of a task.")
  @GetMapping("/{taskId}/logs")
  public ResponseEntity<List<TaskLogStreamResponse>> getTaskLogs(
      @Parameter(description = "Task UUID", required = true) @PathVariable UUID taskId,
      @Parameter(description = "Tenant UUID", required = true) @RequestHeader("X-Tenant-ID")
          String tenantId,
      @Parameter(description = "Maximum number of logs to retrieve")
          @RequestParam(defaultValue = "1000")
          int limit,
      @Parameter(
              description =
                  "If true, get the last N logs (tail mode). If false, get the first N logs.")
          @RequestParam(defaultValue = "false")
          boolean tail,
      @Parameter(
              description =
                  "Comma-separated list of log levels to filter by (e.g., 'ERROR,INFO,WARN')")
          @RequestParam(required = false)
          String levels) {
    List<String> logLevels =
        levels != null && !levels.trim().isEmpty()
            ? Arrays.stream(levels.split(",")).map(String::trim).collect(Collectors.toList())
            : null;
    List<TaskLogStreamResponse> logs =
        logService.getTaskLogs(taskId, tenantId, limit, tail, logLevels);
    return ResponseEntity.ok(logs);
  }

  // --- Task Run-Level Log Endpoints ---

  @Operation(
      summary = "Get historical logs for a specific task run",
      description = "Retrieves all execution logs for a single, specific task run.",
      tags = {"Task Runs"})
  @GetMapping("/{taskId}/runs/{executionId}/logs")
  public ResponseEntity<List<TaskLogStreamResponse>> getRunLogs(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Execution ID") @PathVariable String executionId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(description = "Maximum number of logs to retrieve")
          @RequestParam(defaultValue = "1000")
          int limit,
      @Parameter(
              description =
                  "If true, get the last N logs (tail mode). If false, get the first N logs.")
          @RequestParam(defaultValue = "false")
          boolean tail,
      @Parameter(
              description =
                  "Comma-separated list of log levels to filter by (e.g., 'ERROR,INFO,WARN')")
          @RequestParam(required = false)
          String levels) {
    // First, verify the run exists and belongs to the task and tenant.
    // This throws IllegalArgumentException if not found, which our global handler
    // catches.
    taskRunService.getTaskRun(executionId, taskId, tenantId);

    List<String> logLevels =
        levels != null && !levels.trim().isEmpty()
            ? Arrays.stream(levels.split(",")).map(String::trim).collect(Collectors.toList())
            : null;
    List<TaskLogStreamResponse> logs =
        logService.getExecutionLogs(taskId, tenantId, executionId, limit, tail, logLevels);
    return ResponseEntity.ok(logs);
  }

  @Operation(
      summary = "Stream logs for a specific task run",
      description = "Streams execution logs for a single, specific run using Server-Sent Events.",
      tags = {"Task Runs"})
  @GetMapping(
      value = "/{taskId}/runs/{executionId}/logs/stream",
      produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter streamRunLogs(
      @Parameter(description = "Task UUID") @PathVariable UUID taskId,
      @Parameter(description = "Execution ID") @PathVariable String executionId,
      @Parameter(description = "Tenant UUID") @RequestHeader("X-Tenant-ID") String tenantId,
      @Parameter(
              description =
                  "Comma-separated list of log levels to filter by (e.g., 'ERROR,INFO,WARN')")
          @RequestParam(required = false)
          String levels) {
    // Verify the run exists before attempting to stream logs.
    taskRunService.getTaskRun(executionId, taskId, tenantId);

    List<String> logLevels =
        levels != null && !levels.trim().isEmpty()
            ? Arrays.stream(levels.split(",")).map(String::trim).collect(Collectors.toList())
            : null;
    return logService.streamLogsForExecution(taskId, executionId, tenantId, logLevels);
  }
}
