package com.aidentx.taskmanagement.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.*;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.dto.TaskExecutionLog;
import com.aidentx.taskmanagement.dto.TaskLogStreamResponse;
import com.aidentx.taskmanagement.model.entity.TaskRun;
import com.aidentx.taskmanagement.model.enums.ExecutionStatus;
import com.aidentx.taskmanagement.repository.TaskRunRepository;

/** Service for writing and reading task execution logs from OpenSearch. */
@Service
public class TaskExecutionLogService {

  private static final String INDEX_PREFIX = "task-execution-logs-";
  private static final DateTimeFormatter INDEX_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd");

  private final OpenSearchClient openSearchClient;
  private final IUnifiedLogger log;
  private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
  private final TaskRunRepository taskRunRepository;

  public TaskExecutionLogService(
      OpenSearchClient openSearchClient,
      UnifiedLoggerFactory unifiedLoggerFactory,
      TaskRunRepository taskRunRepository) {
    this.openSearchClient = openSearchClient;
    this.log = unifiedLoggerFactory.getLogger(TaskExecutionLogService.class);
    this.taskRunRepository = taskRunRepository;
  }

  /**
   * Writes a task execution log entry to OpenSearch.
   *
   * @param logEntry The log entry to write.
   */
  public void writeLog(TaskExecutionLog logEntry) {
    try {
      String indexName = INDEX_PREFIX + LocalDate.now().format(INDEX_DATE_FORMATTER);
      openSearchClient.index(i -> i.index(indexName).id(logEntry.getId()).document(logEntry));
    } catch (IOException e) {
      log.error(e).message(TaskManagementConstants.ERR_FAILED_WRITE_LOG_OPENSEARCH).log();
    }
  }

  /**
   * Retrieves historical logs for a specific task.
   *
   * @param taskId    The task ID.
   * @param tenantId  The tenant ID.
   * @param limit     The maximum number of logs to retrieve.
   * @param tail      If true, get the last N logs (tail mode). If false, get the
   *                  first N logs.
   * @param logLevels List of log levels to filter by, or null for all levels.
   * @return A list of log entries.
   */
  public List<TaskLogStreamResponse> getTaskLogs(
      UUID taskId, String tenantId, int limit, boolean tail, List<String> logLevels) {
    Query query = buildQuery(tenantId, "taskId.keyword", taskId.toString(), null, null, logLevels);
    return searchLogs(query, limit, tail);
  }

  /**
   * Retrieves historical logs for a specific task execution run.
   *
   * @param taskId      The task ID.
   * @param tenantId    The tenant ID.
   * @param executionId The execution ID.
   * @param limit       The maximum number of logs to retrieve.
   * @param tail        If true, get the last N logs (tail mode). If false, get
   *                    the first N logs.
   * @param logLevels   List of log levels to filter by, or null for all levels.
   * @return A list of log entries.
   */
  public List<TaskLogStreamResponse> getExecutionLogs(
      UUID taskId,
      String tenantId,
      String executionId,
      int limit,
      boolean tail,
      List<String> logLevels) {
    Query query = buildQuery(tenantId, "executionId.keyword", executionId, null, null, logLevels);
    return searchLogs(query, limit, tail);
  }

  /**
   * Streams logs in real-time for a specific task. This will stream logs from all
   * executions of the
   * task.
   */
  public SseEmitter streamLogsForTask(UUID taskId, String tenantId, List<String> logLevels) {
    // For task-level streaming (all runs), we can't easily determine if task is
    // "running"
    // So we keep the stream open and rely on isLastMessage flag from logs
    return createSseEmitter(
        null, // No specific execution ID to check
        tenantId,
        (lastTimestamp) -> {
          Query query = buildQuery(
              tenantId, "taskId.keyword", taskId.toString(), lastTimestamp, null, logLevels);
          return searchLogs(query, 100, false);
        });
  }

  /** Streams logs in real-time for a specific task execution run. */
  public SseEmitter streamLogsForExecution(
      UUID taskId, String executionId, String tenantId, List<String> logLevels) {
    return createSseEmitter(
        executionId, // Pass execution ID so we can check if it's still running
        tenantId,
        (lastTimestamp) -> {
          Query query = buildQuery(
              tenantId, "executionId.keyword", executionId, lastTimestamp, null, logLevels);
          return searchLogs(query, 100, false);
        });
  }

  private List<TaskLogStreamResponse> searchLogs(Query query, int limit, boolean tail) {
    try {
      SearchResponse<Map> response = openSearchClient.search(
          s -> s.index(INDEX_PREFIX + "*")
              .query(query)
              .sort(
                  sort -> sort.field(
                      f -> f.field("timestamp")
                          .order(
                              tail
                                  ? org.opensearch.client.opensearch._types.SortOrder.Desc
                                  : org.opensearch.client.opensearch._types.SortOrder.Asc)))
              .size(limit),
          Map.class);

      return response.hits().hits().stream()
          .map(this::hitToLogStreamResponse)
          .collect(Collectors.toList());
    } catch (IOException e) {
      log.error(e).message(TaskManagementConstants.ERR_FAILED_RETRIEVE_LOGS_OPENSEARCH).log();
      return new ArrayList<>();
    }
  }

  private SseEmitter createSseEmitter(String executionId, String tenantId, LogFetcher fetcher) {
    SseEmitter emitter = new SseEmitter(300_000L); // 5-minute timeout

    sseExecutor.execute(
        () -> {
          try {
            // Send initial connected event to confirm stream is active
            emitter.send(
                SseEmitter.event()
                    .name("connected")
                    .data("Stream connected and monitoring for logs"));

            long lastTimestamp = 0L;
            boolean taskCompleted = false;
            int emptyPollCount = 0;

            // Check if this is a completed execution upfront and retain the run
            // summary for the logs_unavailable fallback payload.
            boolean isCompletedExecution = false;
            TaskRun completedTaskRun = null;
            if (executionId != null) {
              Optional<TaskRun> taskRun = taskRunRepository.findByExecutionIdAndTenantId(executionId, tenantId);
              if (taskRun.isPresent()) {
                ExecutionStatus status = taskRun.get().getStatus();
                isCompletedExecution = ExecutionStatus.COMPLETED.equals(status)
                    || ExecutionStatus.FAILED.equals(status)
                    || ExecutionStatus.CLOSED.equals(status);
                if (isCompletedExecution) {
                  completedTaskRun = taskRun.get();
                }
              }
            }

            while (!Thread.currentThread().isInterrupted() && !taskCompleted) {
              List<TaskLogStreamResponse> logs = fetcher.fetch(lastTimestamp);

              if (logs.isEmpty()) {
                emptyPollCount++;

                // If this is a completed execution and we have no matching logs after first
                // poll
                // End the stream immediately (like when you query a completed task without
                // filters)
                if (isCompletedExecution && emptyPollCount >= 2) {
                  // Emit a structured logs_unavailable event — the OpenSearch index for
                  // this execution no longer exists. Include the PostgreSQL summary so
                  // the frontend can display a meaningful fallback card.
                  String summaryJson = buildRunSummaryJson(completedTaskRun);
                  emitter.send(
                      SseEmitter.event()
                          .name(TaskManagementConstants.SSE_EVENT_LOGS_UNAVAILABLE)
                          .data(summaryJson));
                  emitter.complete();
                  break;
                }

                // For live tasks: only timeout after extended period of no logs
                if (!isCompletedExecution && emptyPollCount >= 300) { // 5 minutes
                  emitter.send(
                      SseEmitter.event()
                          .name(TaskManagementConstants.SSE_EVENT_TIMEOUT)
                          .data("Stream timeout - no new logs for 5 minutes. Task may still be running — check History for final status."));
                  emitter.complete();
                  break;
                }
              } else {
                emptyPollCount = 0; // Reset counter when logs are found

                for (TaskLogStreamResponse logEntry : logs) {
                  emitter.send(
                      SseEmitter.event()
                          .name(TaskManagementConstants.SSE_EVENT_LOG)
                          .data(logEntry));
                  lastTimestamp = Math.max(lastTimestamp, logEntry.getTimestamp());
                  if (logEntry.getIsLastMessage()) {
                    taskCompleted = true;
                  }
                }

                if (taskCompleted) {
                  emitter.send(
                      SseEmitter.event()
                          .name(TaskManagementConstants.SSE_EVENT_COMPLETE)
                          .data(TaskManagementConstants.SSE_DATA_STREAM_COMPLETED));
                  emitter.complete();
                }
              }

              if (!taskCompleted) {
                Thread.sleep(1000); // Poll every second
              }
            }
          } catch (Exception e) {
            emitter.completeWithError(e);
          }
        });

    emitter.onCompletion(
        () -> log.info().message(TaskManagementConstants.MSG_SSE_STREAM_COMPLETED).log());
    emitter.onTimeout(
        () -> log.warn().message(TaskManagementConstants.MSG_SSE_STREAM_TIMED_OUT).log());

    return emitter;
  }

  /**
   * Builds a compact JSON string containing the key summary statistics of a completed
   * {@link TaskRun}. This is used as the payload of the {@code logs_unavailable} SSE event
   * so the frontend can render an informative fallback card without needing an extra API call.
   */
  private String buildRunSummaryJson(TaskRun run) {
    if (run == null) return "{}";
    return "{"
        + "\"processedCount\":"      + nullableInt(run.getProcessedCount())      + ","
        + "\"createdCount\":"        + nullableInt(run.getCreatedCount())        + ","
        + "\"updatedCount\":"        + nullableInt(run.getUpdatedCount())        + ","
        + "\"deletedCount\":"        + nullableInt(run.getDeletedCount())        + ","
        + "\"linkedCount\":"         + nullableInt(run.getLinkedCount())         + ","
        + "\"failedCount\":"         + nullableInt(run.getFailedCount())         + ","
        + "\"skippedCount\":"        + nullableInt(run.getSkippedCount())        + ","
        + "\"entitlementsImported\":" + nullableInt(run.getEntitlementsImported()) + ","
        + "\"durationMs\":"  + (run.getDurationMs()  != null ? run.getDurationMs()             : "null") + ","
        + "\"status\":\""   + (run.getStatus()       != null ? run.getStatus().name()          : "UNKNOWN") + "\","
        + "\"endTime\":"    + (run.getEndTime()       != null ? run.getEndTime().toEpochMilli() : "null")
        + "}";
  }

  private static int nullableInt(Integer val) {
    return val != null ? val : 0;
  }

  private Query buildQuery(
      String tenantId,
      String field,
      String value,
      Long afterTimestamp,
      String executionId,
      List<String> logLevels) {
    BoolQuery.Builder boolQuery = new BoolQuery.Builder();

    boolQuery.must(
        TermQuery.of(t -> t.field("tenantId.keyword").value(v -> v.stringValue(tenantId)))
            .toQuery());
    boolQuery.must(TermQuery.of(t -> t.field(field).value(v -> v.stringValue(value))).toQuery());

    if (executionId != null) {
      boolQuery.must(
          TermQuery.of(t -> t.field("executionId.keyword").value(v -> v.stringValue(executionId)))
              .toQuery());
    }

    if (afterTimestamp != null && afterTimestamp > 0) {
      boolQuery.must(
          RangeQuery.of(
              r -> r.field("timestamp")
                  .gt(org.opensearch.client.json.JsonData.of(afterTimestamp)))
              .toQuery());
    }

    if (logLevels != null && !logLevels.isEmpty()) {
      BoolQuery.Builder levelsQuery = new BoolQuery.Builder();
      for (String level : logLevels) {
        levelsQuery.should(
            TermQuery.of(t -> t.field("level.keyword").value(v -> v.stringValue(level))).toQuery());
      }
      boolQuery.must(levelsQuery.build().toQuery());
    }

    return boolQuery.build().toQuery();
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private TaskLogStreamResponse hitToLogStreamResponse(Hit<Map> hit) {
    Map<String, Object> source = (Map<String, Object>) hit.source();
    if (source == null)
      return null;

    String status = (String) source.get("status");
    boolean isLastMessage = TaskManagementConstants.STATUS_COMPLETED.equals(status)
        || TaskManagementConstants.STATUS_FAILED.equals(status);

    return TaskLogStreamResponse.builder()
        .timestamp(getLongValue(source, "timestamp"))
        .level((String) source.get("level"))
        .message((String) source.get("message"))
        .status(status)
        .exception((String) source.get("exception"))
        .isLastMessage(isLastMessage)
        .build();
  }

  private Long getLongValue(Map<String, Object> source, String key) {
    Object value = source.get(key);
    return (value instanceof Number) ? ((Number) value).longValue() : null;
  }

  @FunctionalInterface
  private interface LogFetcher {
    List<TaskLogStreamResponse> fetch(long lastTimestamp) throws IOException;
  }
}
