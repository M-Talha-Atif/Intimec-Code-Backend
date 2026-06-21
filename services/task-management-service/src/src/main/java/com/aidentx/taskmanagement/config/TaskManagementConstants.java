package com.aidentx.taskmanagement.config;

/**
 * Application constants for the Task Management Service. Contains all hardcoded strings, log
 * levels, status values, and configuration constants.
 */
public final class TaskManagementConstants {

  // ========== CORE CONSTANTS (Status, Types, Defaults) ==========

  // Log Levels
  public static final String LOG_LEVEL_INFO = "INFO";
  public static final String LOG_LEVEL_ERROR = "ERROR";
  public static final String LOG_LEVEL_WARN = "WARN";
  public static final String LOG_LEVEL_DEBUG = "DEBUG";

  // Task Execution Status
  public static final String STATUS_STARTED = "STARTED";
  public static final String STATUS_RUNNING = "RUNNING";
  public static final String STATUS_COMPLETED = "COMPLETED";
  public static final String STATUS_FAILED = "FAILED";
  public static final String STATUS_CANCELLED = "CANCELLED";
  public static final String STATUS_RETRYING = "RETRYING";

  // Task Cancellation Messages
  public static final String MSG_TASK_CANCELLED = "Task cancelled by operator request";
  public static final String MSG_TASK_CANCELLED_LOG = "Task execution cancelled by operator: executionId=%s";

  // SSE Event Constants
  public static final String SSE_EVENT_LOG = "log";
  public static final String SSE_EVENT_COMPLETE = "complete";
  /** Sent when the stream polling gives up waiting after 5 minutes of silence.
   *  The task is still RUNNING — the frontend must NOT treat this as task completion. */
  public static final String SSE_EVENT_TIMEOUT = "stream_timeout";
  /** Sent when a completed execution has no matching logs in OpenSearch (index expired).
   *  Data payload is a compact JSON summary built from the {@code task_runs} row so the
   *  frontend can render a meaningful fallback card without an extra API round-trip. */
  public static final String SSE_EVENT_LOGS_UNAVAILABLE = "logs_unavailable";
  public static final String SSE_DATA_STREAM_COMPLETED = "Stream completed";

  // Connector Constants
  public static final String OBJECT_CLASS_ACCOUNT = "__ACCOUNT__";
  public static final String OBJECT_CLASS_GROUP = "__GROUP__";
  public static final int DEFAULT_PAGE_SIZE = 100;

  /**
   * Number of accounts to collect before sending a batch gRPC call
   * to identity-service. Reduces RPC overhead from O(n) to O(n/500).
   */
  public static final int SYNC_BATCH_SIZE = 500;

  /**
   * Maximum time (in minutes) a single task execution is allowed to run.
   * gRPC stubs created in TaskExecutionJob are given this deadline, preventing
   * the Quartz thread from blocking indefinitely on a stalled streaming iterator.
   * If the connector-runtime-service does not complete within this window, the
   * task is marked FAILED with a DEADLINE_EXCEEDED error.
   */
  public static final int MAX_TASK_DURATION_MINUTES = 60;

  // Default Values
  public static final String UNKNOWN_NODE_ID = "unknown-node";

  // Configuration Keys
  public static final String CONFIG_CONNECTOR_ID = "connectorId";

  // ========== VALIDATION ERROR MESSAGES ==========

  // TaskMapper Validation Errors
  public static final String ERR_EXECUTION_DATETIME_REQUIRED =
      "Execution date/time is required for one-time tasks.";
  public static final String ERR_EXECUTION_DATETIME_FUTURE =
      "Execution date/time for one-time tasks must be in the future.";
  public static final String ERR_CRON_EXPRESSION_REQUIRED =
      "A cron expression is required for CRON tasks.";
  public static final String ERR_INTERVAL_SECONDS_POSITIVE =
      "A positive interval in seconds is required for INTERVAL tasks.";
  public static final String ERR_INVALID_SCHEDULE_TYPE =
      "Invalid schedule type: '%s'. Valid values are: ONCE, CRON, INTERVAL, MANUAL.";
  public static final String ERR_INVALID_TASK_CONFIG_FORMAT = "Invalid task configuration format.";
  public static final String ERR_CRON_REQUIRES_LOOSE_BINDING =
      "CRON scheduled tasks must use LOOSE binding";
  public static final String ERR_TIGHT_BINDING_REQUIRES_INTERVAL =
      "TIGHT binding requires intervalSeconds to be set";
  public static final String ERR_FAILED_CREATE_OPENSEARCH_CLIENT =
      "Failed to create OpenSearch client";
  public static final String ERR_TASK_NOT_FOUND_JOB = "Task not found: %s";

  // ========== LOG MESSAGES ==========

  // Task Execution Messages
  public static final String MSG_TASK_EXECUTION_STARTED = "Task execution started";
  public static final String MSG_TASK_RUNNING = "Task is now running";
  public static final String MSG_TASK_COMPLETED_SUCCESS = "Task execution completed successfully";
  public static final String MSG_TASK_EXECUTION_FAILED = "Task execution failed";
  public static final String MSG_STARTING_TASK_EXECUTION =
      "Starting task execution: taskId=%s, executionId=%s";
  public static final String MSG_TASK_EXECUTION_SUCCEEDED =
      "Task execution succeeded: executionId=%s";
  public static final String MSG_TASK_EXECUTION_FAILED_LOG =
      "Task execution failed: executionId=%s";
  public static final String MSG_RETRY_ATTEMPT = "Retrying task execution (attempt %d of %d)";

  // Connector and Application Messages
  public static final String MSG_FETCHING_CONNECTOR_CONFIG =
      "Fetching application connector configuration";
  public static final String MSG_USING_CONNECTOR = "Using connector: ";
  public static final String MSG_TESTING_CONNECTION = "Testing connection to external system...";
  public static final String MSG_CONNECTION_SUCCESS = "Connection test successful: ";
  public static final String MSG_STARTING_SYNC_TASK = "Starting SYNC task - fetching accounts...";
  public static final String MSG_STARTING_ENTITLEMENT_IMPORT = "Starting entitlement import - fetching groups/entitlements...";
  public static final String MSG_ENTITLEMENT_IMPORT_COMPLETED = "Entitlement import completed. Fetched %d entitlements (%d created, %d updated, %d errors).";
  public static final String MSG_SYNC_COMPLETED = "SYNC completed. Fetched %d accounts.";
  public static final String MSG_TASK_NOT_FOUND = "Task not found: %s. Aborting execution.";

  // User and Application Resolution Messages
  public static final String MSG_GRPC_ERROR_FETCHING_USERS =
      "gRPC error fetching users; will use IDs as fallback names.";
  public static final String MSG_UNEXPECTED_ERROR_FETCHING_USERS =
      "Unexpected error fetching users; will use IDs as fallback names.";
  public static final String MSG_FETCHING_APPLICATION_DETAILS =
      "Fetching application details from core-service";
  public static final String MSG_APPLICATION_NOT_FOUND_CORE =
      "Application not found in core-service";
  public static final String MSG_GRPC_ERROR_FETCHING_APPLICATION =
      "gRPC error fetching application from core-service";
  public static final String MSG_APPLICATION_NOT_FOUND_FALLBACK =
      "Application not found, using ID as fallback name for reference.";
  public static final String MSG_FAILED_RESOLVE_APP_NAME_FALLBACK =
      "Failed to resolve application name due to an error, using ID as fallback.";

  // Task Service Log Messages
  public static final String MSG_CREATING_TASK = "Creating new task '%s' for application %s";
  public static final String MSG_TASK_SAVED = "Task saved with ID: %s";
  public static final String MSG_UPDATING_TASK = "Updating task %s by user %s";
  public static final String MSG_TASK_UPDATED_SUCCESS =
      "Task %s updated successfully. New version: %d";
  public static final String MSG_DELETING_TASK = "Deleting task %s from application %s";
  public static final String MSG_TASK_DELETED_SUCCESS =
      "Task %s deleted successfully from database";
  public static final String MSG_TRIGGERING_EXECUTION = "Triggering manual execution for task %s";
  public static final String MSG_TASK_TRIGGERED = "Task %s triggered with executionId: %s";
  public static final String MSG_EXECUTION_TRIGGERED_SUCCESS =
      "Task execution triggered successfully.";
  public static final String MSG_FAILED_TRIGGER_TASK = "Failed to trigger task %s";
  public static final String MSG_PAUSING_TASK = "Pausing task %s";
  public static final String MSG_RESUMING_TASK = "Resuming task %s";
  public static final String MSG_TERMINATING_TASK = "Terminating task %s";

  // Quartz Scheduler Service Log Messages
  public static final String MSG_SCHEDULING_ONE_TIME_TASK = "Scheduling one-time task: %s at %s";
  public static final String MSG_SCHEDULING_RECURRING_TASK = "Scheduling recurring task: %s";
  public static final String MSG_SCHEDULING_MANUAL_TASK = "Scheduling durable manual task: %s";
  public static final String MSG_TASK_SCHEDULED_SUCCESS =
      "Task %s scheduled successfully with Quartz";
  public static final String MSG_FAILED_SCHEDULE_QUARTZ =
      "Failed to schedule task with Quartz. Rolling back task creation.";
  public static final String MSG_UNSCHEDULED_TASK = "Unscheduled task %s from Quartz";
  public static final String MSG_COULD_NOT_UNSCHEDULE =
      "Could not unschedule task %s. It may not have been scheduled.";

  // Controller Log Messages
  public static final String MSG_RECEIVED_CREATE_TASK_REQUEST =
      "Received request to create task: %s for application: %s";
  public static final String MSG_LOG_STREAM_REQUESTED = "Log stream requested for task: %s";
  public static final String MSG_RETRIEVING_LOGS = "Retrieving logs for task: %s with limit: %d";

  // SSE Stream Messages
  public static final String MSG_SSE_STREAM_COMPLETED = "SSE stream completed.";
  public static final String MSG_SSE_STREAM_TIMED_OUT = "SSE stream timed out.";

  // ========== ERROR MESSAGES ==========

  // Task Service Error Messages
  public static final String ERR_FAILED_SCHEDULE_TASK =
      "Failed to schedule task due to a scheduler error";
  public static final String ERR_INVALID_TASK_CONFIG = "Invalid task configuration format";
  public static final String ERR_FAILED_EXECUTE_TASK =
      "Failed to execute task due to a scheduler error";
  public static final String ERR_FAILED_PAUSE_TASK = "Failed to pause task";
  public static final String ERR_FAILED_RESUME_TASK = "Failed to resume task";
  public static final String ERR_FAILED_TERMINATE_TASK = "Failed to terminate task";
  public static final String ERR_TASK_NOT_BELONG_TO_APP =
      "Task with ID %s does not belong to application %s";
  public static final String ERR_TASK_NOT_FOUND = "Task not found with ID: %s";
  public static final String ERR_TASK_RUN_NOT_FOUND = "Task run not found with execution ID: %s";
  public static final String ERR_FAILED_WRITE_LOG_OPENSEARCH =
      "Failed to write task execution log to OpenSearch";
  public static final String ERR_FAILED_RETRIEVE_LOGS_OPENSEARCH =
      "Failed to retrieve task logs from OpenSearch";
  public static final String ERR_APPLICATION_NOT_FOUND = "Application not found with ID: %s";
  public static final String ERR_FETCHING_APPLICATION_CONFIG =
      "Error fetching application configuration: %s";

  // Connection and Connector Errors
  public static final String ERR_CONNECTION_TEST_FAILED = "Connection test failed: ";
  public static final String ERR_MISSING_CONNECTOR_CONFIG =
      "Application connector configuration is missing or empty.";

  // Controller Error Messages
  public static final String MSG_RESOURCE_NOT_FOUND = "Resource not found: ";
  public static final String MSG_VALIDATION_FAILED = "Validation failed: ";
  public static final String MSG_VALIDATION_ERROR = "Validation error: ";
  public static final String MSG_UNEXPECTED_ERROR_OCCURRED = "An unexpected error occurred";
  public static final String MSG_UNEXPECTED_INTERNAL_ERROR =
      "An unexpected internal error occurred. Please contact support.";

  private TaskManagementConstants() {
    // Private constructor to prevent instantiation
  }
}
