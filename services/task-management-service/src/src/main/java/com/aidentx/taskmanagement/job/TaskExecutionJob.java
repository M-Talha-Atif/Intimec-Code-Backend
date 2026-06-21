package com.aidentx.taskmanagement.job;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import javax.sql.DataSource;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aidentx.dal.protos.application.ApplicationDTO;
import com.aidentx.dal.protos.runtime.*;
import com.aidentx.dal.protos.shadow.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.dto.TaskExecutionLog;
import com.aidentx.taskmanagement.dto.TaskPreviewResponse;
import com.aidentx.taskmanagement.dto.TaskPreviewResponse.AccountPreview;
import com.aidentx.taskmanagement.dto.TaskPreviewResponse.AttributeChangeEntry;
import com.aidentx.taskmanagement.model.entity.Task;
import com.aidentx.taskmanagement.model.entity.TaskRun;
import com.aidentx.taskmanagement.model.enums.ExecutionStatus;
import com.aidentx.taskmanagement.model.enums.TaskAuditEventType;
import com.aidentx.taskmanagement.repository.TaskRepository;
import com.aidentx.taskmanagement.repository.TaskRunRepository;
import com.aidentx.taskmanagement.service.ApplicationConfigService;
import com.aidentx.taskmanagement.service.TaskExecutionStateRegistry;
import com.aidentx.taskmanagement.service.TaskExecutionLogService;
import com.aidentx.taskmanagement.service.TaskRunAuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.stream.Collectors;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

/**
 * Quartz Job for executing scheduled tasks. This job orchestrates the task execution process,
 * including logging, status updates, and invocation of the actual task logic.
 *
 * <p><b>Concurrency Model:</b> Multiple tasks CAN run concurrently across different applications.
 * A PostgreSQL advisory lock prevents two imports from running against the same application
 * simultaneously (which would cause shadow conflicts), while allowing different applications to
 * import in parallel. Advisory locks work across multiple JVM instances (Quartz clustering)
 * because the lock is held at the database level, not in JVM memory.
 */
@Component
public class TaskExecutionJob implements Job {

  @Autowired private TaskRepository taskRepository;
  @Autowired private TaskRunRepository taskRunRepository;
  @Autowired private TaskExecutionLogService logService;
  @Autowired private UnifiedLoggerFactory unifiedLoggerFactory;
  @Autowired private TaskExecutionStateRegistry stateRegistry;
  @Autowired private ApplicationConfigService applicationConfigService;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private DataSource dataSource;
  @Autowired private TaskRunAuditService auditService;

  /**
   * Maximum number of per-account detail records to store in execution results.
   * Summary counters always cover ALL processed accounts.
   */
  private static final int MAX_STORED_ACCOUNT_DETAILS = 500;

  @Autowired
  private ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub connectorRuntimeStub;

  @Autowired
  private ShadowSyncServiceGrpc.ShadowSyncServiceBlockingStub shadowSyncStub;

  private IUnifiedLogger log;

  @Override
  public void execute(JobExecutionContext context) throws JobExecutionException {
    if (log == null) {
      log = unifiedLoggerFactory.getLogger(TaskExecutionJob.class);
    }

    JobDataMap dataMap = context.getMergedJobDataMap();
    UUID taskId = UUID.fromString(dataMap.getString("taskId"));
    String tenantId = dataMap.getString("tenantId");
    String executionId =
        Optional.ofNullable(dataMap.getString("executionId")).orElse(UUID.randomUUID().toString());
    boolean dryRun = "true".equalsIgnoreCase(dataMap.getString("dryRun"));

    log.info()
        .message(
            String.format(TaskManagementConstants.MSG_STARTING_TASK_EXECUTION, taskId, executionId))
        .log();

    Task task =
        taskRepository
            .findByIdAndTenantId(taskId, tenantId)
            .orElseThrow(
                () -> {
                  log.error()
                      .message(String.format(TaskManagementConstants.MSG_TASK_NOT_FOUND, taskId))
                      .log();
                  return new JobExecutionException(
                      String.format(TaskManagementConstants.ERR_TASK_NOT_FOUND_JOB, taskId), false);
                });

    // Per-application locking using PostgreSQL advisory lock.
    // This works across multiple JVM instances (Quartz clustering) because the lock
    // is held at the DB connection level, not in JVM memory.
    String appLockKey = task.getApplicationId().toString();
    long advisoryLockKey = appLockKey.hashCode(); // deterministic int for advisory lock

    Connection lockConnection = null;
    boolean lockAcquired = false;
    try {
      try {
        lockConnection = dataSource.getConnection();
        lockConnection.setAutoCommit(true); // advisory locks are session-level, not transactional

        try (PreparedStatement ps = lockConnection.prepareStatement(
                "SELECT pg_try_advisory_lock(?)")) {
          ps.setLong(1, advisoryLockKey);
          try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
              lockAcquired = rs.getBoolean(1);
            }
          }
        }
      } catch (SQLException e) {
        throw new JobExecutionException("Failed to acquire advisory lock: " + e.getMessage(), e, false);
      }

      if (!lockAcquired) {
        log.warn()
            .message(String.format(
                "Import already running for application %s — skipping task %s (executionId=%s)",
                appLockKey, taskId, executionId))
            .log();
        TaskRun taskRun = getOrCreateTaskRun(task, executionId);
        taskRun.setStatus(ExecutionStatus.SKIPPED);
        taskRun.setStartTime(Instant.now());
        taskRun.setEndTime(Instant.now());
        taskRun.setErrorMessage("Skipped: another import is already running for this application");
        taskRunRepository.save(taskRun);
        auditService.warn(executionId, taskId, tenantId, TaskAuditEventType.TASK_SKIPPED,
            "Skipped: another import is already running for application " + appLockKey);
        return;
      }

      executeWithLock(context, task, taskId, executionId, dryRun);
    } finally {
      // Release advisory lock and close the dedicated connection
      if (lockConnection != null) {
        try {
          if (!lockConnection.isClosed()) {
            if (lockAcquired) {
              try (PreparedStatement ps = lockConnection.prepareStatement(
                      "SELECT pg_advisory_unlock(?)")) {
                ps.setLong(1, advisoryLockKey);
                ps.executeQuery();
              }
            }
            lockConnection.close();
          }
        } catch (SQLException e) {
          log.warn().message("Failed to release advisory lock: " + e.getMessage()).log();
        }
      }
    }
  }

  /**
   * Executes the task while holding the per-application lock.
   */
  private void executeWithLock(JobExecutionContext context, Task task, UUID taskId, String executionId, boolean dryRun)
      throws JobExecutionException {

    // Register with the cooperative pause/cancel registry so that pauseTask() and
    // terminateTask() signals reach this execution thread.
    stateRegistry.register(taskId);
    // Get or create the TaskRun - it might already exist if created by the trigger
    // endpoint
    TaskRun taskRun = getOrCreateTaskRun(task, executionId);
    writeLog(
        task,
        executionId,
        TaskManagementConstants.LOG_LEVEL_INFO,
        TaskManagementConstants.MSG_TASK_EXECUTION_STARTED,
        TaskManagementConstants.STATUS_STARTED,
        null);
    auditService.info(executionId, task.getId(), task.getTenantId(),
        TaskAuditEventType.TASK_STARTED, TaskManagementConstants.MSG_TASK_EXECUTION_STARTED);

    try {
      // Update the TaskRun to RUNNING status and set actual start time
      taskRun.setStatus(ExecutionStatus.RUNNING);
      taskRun.setStartTime(Instant.now());
      taskRunRepository.save(taskRun);

      updateTaskStatus(task.getId(), ExecutionStatus.RUNNING, Instant.now(), null);
      writeLog(
          task,
          executionId,
          TaskManagementConstants.LOG_LEVEL_INFO,
          TaskManagementConstants.MSG_TASK_RUNNING,
          TaskManagementConstants.STATUS_RUNNING,
          null);

      executeTaskLogic(task, executionId, taskRun, dryRun);
      handleSuccess(task.getId(), taskRun, executionId);

    } catch (TaskCancelledException e) {
      handleCancelled(task.getId(), taskRun, executionId);
      // Do NOT call handleRetry — cancellation is intentional, not an error.
    } catch (Exception e) {
      handleFailure(task.getId(), taskRun, executionId, e);
      handleRetry(context, e);
    } finally {
      stateRegistry.deregister(taskId);
    }
  }

  private TaskRun getOrCreateTaskRun(Task task, String executionId) {
    // Try to find existing TaskRun (created by trigger endpoint)
    return taskRunRepository
        .findByExecutionIdAndTaskIdAndTenantId(executionId, task.getId(), task.getTenantId())
        .orElseGet(
            () -> {
              // Create new TaskRun if it doesn't exist (for scheduled executions)
              TaskRun taskRun = new TaskRun();
              taskRun.setExecutionId(executionId);
              taskRun.setTaskId(task.getId());
              taskRun.setTenantId(task.getTenantId());
              taskRun.setApplicationId(task.getApplicationId());
              taskRun.setStatus(ExecutionStatus.RUNNING);
              taskRun.setExecutionNodeId(getExecutionNodeId());
              taskRun.setStartTime(Instant.now());
              return taskRunRepository.save(taskRun);
            });
  }

  private void executeTaskLogic(Task task, String executionId, TaskRun taskRun, boolean dryRun) throws Exception {
    writeLog(
        task,
        executionId,
        TaskManagementConstants.LOG_LEVEL_INFO,
        TaskManagementConstants.MSG_FETCHING_CONNECTOR_CONFIG,
        TaskManagementConstants.STATUS_RUNNING,
        null);
    ApplicationDTO application =
        applicationConfigService.getApplication(task.getTenantId(), task.getApplicationId());
    Struct connectorConfig = application.getConnectorConfiguration();

    if (connectorConfig == null || connectorConfig.getFieldsCount() == 0) {
      throw new IllegalStateException(TaskManagementConstants.ERR_MISSING_CONNECTOR_CONFIG);
    }
    String connectorId =
        connectorConfig
            .getFieldsOrThrow(TaskManagementConstants.CONFIG_CONNECTOR_ID)
            .getStringValue();

    writeLog(
        task,
        executionId,
        TaskManagementConstants.LOG_LEVEL_INFO,
        TaskManagementConstants.MSG_USING_CONNECTOR + connectorId,
        TaskManagementConstants.STATUS_RUNNING,
        null);
    writeLog(
        task,
        executionId,
        TaskManagementConstants.LOG_LEVEL_INFO,
        TaskManagementConstants.MSG_TESTING_CONNECTION,
        TaskManagementConstants.STATUS_RUNNING,
        null);

    TestConnectionRequest testRequest =
        TestConnectionRequest.newBuilder()
            .setId(connectorId)
            .setConfiguration(connectorConfig)
            .build();
    TestConnectionResponse testResponse = connectorRuntimeStub
        .withDeadlineAfter(2, TimeUnit.MINUTES)
        .testConnection(testRequest);

    if (!testResponse.getSuccess()) {
      auditService.error(executionId, task.getId(), task.getTenantId(),
          TaskAuditEventType.CONNECTION_TESTED,
          TaskManagementConstants.ERR_CONNECTION_TEST_FAILED + testResponse.getMessage());
      throw new RuntimeException(
          TaskManagementConstants.ERR_CONNECTION_TEST_FAILED + testResponse.getMessage());
    }
    writeLog(
        task,
        executionId,
        TaskManagementConstants.LOG_LEVEL_INFO,
        TaskManagementConstants.MSG_CONNECTION_SUCCESS + testResponse.getMessage(),
        TaskManagementConstants.STATUS_RUNNING,
        null);
    auditService.info(executionId, task.getId(), task.getTenantId(),
        TaskAuditEventType.CONNECTION_TESTED,
        TaskManagementConstants.MSG_CONNECTION_SUCCESS + testResponse.getMessage());

    executeSyncTask(task, executionId, connectorId, connectorConfig, application, taskRun, dryRun);
  }

  private void executeSyncTask(
      Task task, String executionId, String connectorId, Struct connectorConfig,
      ApplicationDTO application, TaskRun taskRun, boolean dryRun) throws Exception {
    writeLog(
        task,
        executionId,
        TaskManagementConstants.LOG_LEVEL_INFO,
        TaskManagementConstants.MSG_STARTING_SYNC_TASK,
        TaskManagementConstants.STATUS_RUNNING,
        null);

    // -----------------------------------------------------------------------
    // Deadline-scoped stubs — CRITICAL for preventing the 100-hour hang.
    //
    // gRPC server-streaming calls (searchObjects) return an Iterator.  If the
    // connector-runtime-service stalls or the network partitions, a call to
    // Iterator.hasNext() will BLOCK THE QUARTZ THREAD INDEFINITELY unless a
    // deadline is set.  withDeadlineAfter() creates a lightweight stub wrapper
    // that propagates an absolute deadline to every RPC call made through it,
    // including in-progress streaming iterators.  When the deadline expires the
    // iterator throws StatusRuntimeException(DEADLINE_EXCEEDED) which propagates
    // to handleFailure() and marks the run as FAILED rather than hanging forever.
    // -----------------------------------------------------------------------
    ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub timedConnectorStub =
        connectorRuntimeStub.withDeadlineAfter(
            TaskManagementConstants.MAX_TASK_DURATION_MINUTES, TimeUnit.MINUTES);
    ShadowSyncServiceGrpc.ShadowSyncServiceBlockingStub timedShadowStub =
        shadowSyncStub.withDeadlineAfter(
            TaskManagementConstants.MAX_TASK_DURATION_MINUTES, TimeUnit.MINUTES);

    // Use identity type from application configuration
    String identityType = application.getIdentityType();

    // Resolve SyncTaskType from the task's category
    SyncTaskType syncTaskType = resolveSyncTaskType(task.getCategory());
    log.info().message(String.format("Task category=%s → SyncTaskType=%s", task.getCategory(), syncTaskType)).log();

    // Parse task-level scenario reactions from task config
    List<SyncReaction> taskReactions = parseTaskReactions(task.getTaskConfig());
    if (!taskReactions.isEmpty()) {
      log.info().message(String.format("Task has %d configured reactions", taskReactions.size())).log();
    }

    // Parse import scope toggles (default: both enabled for backward compatibility)
    boolean importAccounts = isImportScopeEnabled(task.getTaskConfig(), "account");
    boolean importEntitlements = isImportScopeEnabled(task.getTaskConfig(), "entitlement");
    log.info().message(String.format("Import scope — accounts: %s, entitlements: %s",
        importAccounts, importEntitlements)).log();

    SearchObjectsRequest searchRequest =
        SearchObjectsRequest.newBuilder()
            .setId(connectorId)
            .setConfiguration(connectorConfig)
            .setObjectClassName(TaskManagementConstants.OBJECT_CLASS_ACCOUNT)
            .setPageSize(TaskManagementConstants.DEFAULT_PAGE_SIZE)
            .build();

    int accountCount = 0;
    int linkedCount = 0;
    int unmatchedCount = 0;
    int disputedCount = 0;
    int createdCount = 0;
    int updatedCount = 0;
    int errorCount = 0;
    String syncRunId = dryRun ? ("preview-" + executionId) : executionId;

    // For dry-run mode: track every account UID seen so finalizeSyncRun can
    // correctly exclude them from absent-shadow detection (since processDryRun
    // never writes lastSyncRunId to any shadow row).
    List<String> seenResourceUids = dryRun ? new java.util.ArrayList<>() : null;

    // Per-account execution details (capped for memory/storage safety)
    List<AccountPreview> accountDetails = new java.util.ArrayList<>();

    // ===================================================================
    //  PHASE 1: Entitlement Import
    //  Entitlements are imported BEFORE accounts so that the entitlement
    //  catalog is populated when account membership attributes (memberOf)
    //  are resolved during account import. This prevents unresolved
    //  references on the first import cycle.
    //  The object classes to import are read from the application's schema
    //  configuration — each class marked as "entitlement" type. Falls back
    //  to __GROUP__ when no entitlement schemas are configured.
    //  Skipped when entitlement import is disabled in task config.
    // ===================================================================
    int entitlementCount = 0;
    int entitlementCreated = 0;
    int entitlementUpdated = 0;
    int entitlementErrors = 0;
    boolean groupSearchSucceeded = false; // true only when connector search returned results (even 0)
    List<TaskPreviewResponse.EntitlementPreview> entitlementDetails = new java.util.ArrayList<>();

    if (importEntitlements) {
    try {
      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
          TaskManagementConstants.MSG_STARTING_ENTITLEMENT_IMPORT,
          TaskManagementConstants.STATUS_RUNNING, null);
      auditService.info(executionId, task.getId(), task.getTenantId(),
          TaskAuditEventType.ENTITLEMENT_IMPORT_STARTED,
          TaskManagementConstants.MSG_STARTING_ENTITLEMENT_IMPORT);

      // Resolve configured entitlement object classes from the application schema.
      // Falls back to ["__GROUP__"] if no entitlement schema definitions exist.
      List<ApplicationConfigService.EntitlementObjectClassMeta> entitlementObjectClasses =
          applicationConfigService.getEntitlementObjectClasses(task.getTenantId(), task.getApplicationId());

      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
          String.format("Entitlement object classes to import: %s",
              entitlementObjectClasses.stream()
                  .map(ApplicationConfigService.EntitlementObjectClassMeta::objectClass)
                  .collect(java.util.stream.Collectors.toList())),
          TaskManagementConstants.STATUS_RUNNING, null);

      for (ApplicationConfigService.EntitlementObjectClassMeta entitlementMeta : entitlementObjectClasses) {
      String entitlementObjectClass = entitlementMeta.objectClass();

      SearchObjectsRequest groupSearchRequest =
          SearchObjectsRequest.newBuilder()
              .setId(connectorId)
              .setConfiguration(connectorConfig)
              .setObjectClassName(entitlementObjectClass)
              .setPageSize(TaskManagementConstants.DEFAULT_PAGE_SIZE)
              .build();

      Iterator<ConnectorObject> groupIterator;
      try {
        groupIterator = timedConnectorStub.searchObjects(groupSearchRequest);
        groupSearchSucceeded = true;
      } catch (StatusRuntimeException e) {
        if (e.getStatus().getCode() != Status.Code.UNIMPLEMENTED) {
          // DEADLINE_EXCEEDED, UNAVAILABLE, etc. — propagate so the task is marked FAILED
          throw e;
        }
        // UNIMPLEMENTED: connector does not expose this object class — skip it
        log.warn().message("Connector does not support " + entitlementObjectClass + " object class: " + e.getMessage()).log();
        writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_WARN,
            "Entitlement import skipped for " + entitlementObjectClass + " — connector does not support it: " + e.getMessage(),
            TaskManagementConstants.STATUS_RUNNING, null);
        auditService.warn(executionId, task.getId(), task.getTenantId(),
            TaskAuditEventType.ENTITLEMENT_IMPORT_SKIPPED,
            "Connector does not support " + entitlementObjectClass + ": " + e.getMessage());
        continue; // Try next entitlement class
      } catch (Exception e) {
        log.warn().message("Failed to search " + entitlementObjectClass + ": " + e.getMessage()).log();
        writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_WARN,
            "Entitlement import skipped for " + entitlementObjectClass + ": " + e.getMessage(),
            TaskManagementConstants.STATUS_RUNNING, null);
        auditService.warn(executionId, task.getId(), task.getTenantId(),
            TaskAuditEventType.ENTITLEMENT_IMPORT_SKIPPED,
            "Connector does not support " + entitlementObjectClass + ": " + e.getMessage());
        continue; // Try next entitlement class
      }

      List<ConnectorObject> entitlementBatch = new java.util.ArrayList<>(TaskManagementConstants.SYNC_BATCH_SIZE);

      while (groupIterator.hasNext()) {
        ConnectorObject group = groupIterator.next();
        entitlementBatch.add(group);
        entitlementCount++;

        if (entitlementBatch.size() >= TaskManagementConstants.SYNC_BATCH_SIZE || !groupIterator.hasNext()) {
          try {
            ProcessImportedObjectBatchRequest.Builder batchRequestBuilder =
                ProcessImportedObjectBatchRequest.newBuilder()
                    .setTenantId(task.getTenantId())
                    .setResourceId(task.getApplicationId().toString())
                    .setObjectClass(entitlementObjectClass)
                    .setKind("entitlement")
                    .setIdentityType(identityType != null ? identityType : "")
                    .setSyncRunId(syncRunId)
                    .setTaskType(syncTaskType)
                    .setDryRun(dryRun);

            // Pass schema-configured attribute names so identity-service uses them
            if (entitlementMeta.displayAttribute() != null) {
              batchRequestBuilder.setDisplayAttribute(entitlementMeta.displayAttribute());
            }
            if (entitlementMeta.descriptionAttribute() != null) {
              batchRequestBuilder.setDescriptionAttribute(entitlementMeta.descriptionAttribute());
            }
            // Pass schema attribute names for filtering — only these get stored
            if (entitlementMeta.schemaAttributeNames() != null && !entitlementMeta.schemaAttributeNames().isEmpty()) {
              batchRequestBuilder.addAllSchemaAttributeNames(entitlementMeta.schemaAttributeNames());
            }

            for (ConnectorObject batchGroup : entitlementBatch) {
              batchRequestBuilder.addObjects(
                  BatchObjectEntry.newBuilder()
                      .setResourceUid(batchGroup.getUid())
                      .setAttributes(batchGroup.getAttributes())
                      .build());
            }

            ProcessImportedObjectBatchResponse batchResponse =
                timedShadowStub.processImportedObjectBatch(batchRequestBuilder.build());

            for (int i = 0; i < batchResponse.getResultsCount(); i++) {
              ProcessImportedObjectResponse syncResponse = batchResponse.getResults(i);
              if (!syncResponse.getSuccess()) {
                entitlementErrors++;
                if (entitlementDetails.size() < MAX_STORED_ACCOUNT_DETAILS) {
                  entitlementDetails.add(TaskPreviewResponse.EntitlementPreview.builder()
                      .resourceUid(syncResponse.getResourceUid())
                      .objectClass(entitlementObjectClass)
                      .action("ERROR")
                      .message(syncResponse.getMessage())
                      .build());
                }
                continue;
              }
              SynchronizationAction respAction = syncResponse.getAction();
              if (respAction == SynchronizationAction.SHADOW_CREATED) {
                entitlementCreated++;
              } else if (respAction == SynchronizationAction.SHADOW_UPDATED) {
                entitlementUpdated++;
              }
              if (entitlementDetails.size() < MAX_STORED_ACCOUNT_DETAILS) {
                entitlementDetails.add(TaskPreviewResponse.EntitlementPreview.builder()
                    .resourceUid(syncResponse.getResourceUid())
                    .objectClass(entitlementObjectClass)
                    .action(respAction.name())
                    .message(syncResponse.getMessage())
                    .build());
              }
            }

            writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
                String.format("Processed batch of %d entitlements (%d total so far)",
                    entitlementBatch.size(), entitlementCount),
                TaskManagementConstants.STATUS_RUNNING, null);

            // Persist incremental KPI stats for live progress
            taskRun.setProcessedCount(accountCount + entitlementCount);
            taskRun.setEntitlementsImported(entitlementCount);
            taskRunRepository.save(taskRun);
          } catch (Exception e) {
            entitlementErrors += entitlementBatch.size();
            log.error(e).message(String.format("Failed to process entitlement batch of %d: %s",
                entitlementBatch.size(), e.getMessage())).log();
          }

          entitlementBatch.clear();

          // Cooperative checkpoint: pause if requested, abort if cancelled.
          jobCheckpoint(task.getId(), task, executionId);
        }
      }

      } // end for (entitlementObjectClass)

      String entitlementSummary = String.format(
          TaskManagementConstants.MSG_ENTITLEMENT_IMPORT_COMPLETED,
          entitlementCount, entitlementCreated, entitlementUpdated, entitlementErrors);
      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
          entitlementSummary, TaskManagementConstants.STATUS_RUNNING, null);
      log.info().message(entitlementSummary).log();
      auditService.info(executionId, task.getId(), task.getTenantId(),
          TaskAuditEventType.ENTITLEMENT_IMPORT_COMPLETED, entitlementSummary,
          String.format("{\"total\":%d,\"created\":%d,\"updated\":%d,\"errors\":%d}",
              entitlementCount, entitlementCreated, entitlementUpdated, entitlementErrors));
    } catch (StatusRuntimeException e) {
      if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
        // Deadline expired mid-stream — propagate so the task is marked FAILED
        auditService.error(executionId, task.getId(), task.getTenantId(),
            TaskAuditEventType.ENTITLEMENT_IMPORT_TIMEOUT,
            String.format("Entitlement import timed out after %d minutes",
                TaskManagementConstants.MAX_TASK_DURATION_MINUTES));
        throw new RuntimeException(
            String.format("Task timed out during entitlement import after %d minutes — connector did not complete streaming",
                TaskManagementConstants.MAX_TASK_DURATION_MINUTES), e);
      }
      log.warn().message("Entitlement import phase failed: " + e.getMessage()).log();
      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_WARN,
          "Entitlement import phase failed (non-fatal): " + e.getMessage(),
          TaskManagementConstants.STATUS_RUNNING, null);
    } catch (Exception e) {
      log.warn().message("Entitlement import phase failed: " + e.getMessage()).log();
      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_WARN,
          "Entitlement import phase failed (non-fatal): " + e.getMessage(),
          TaskManagementConstants.STATUS_RUNNING, null);
    }
    } else {
      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
          "Entitlement import skipped — disabled in task configuration",
          TaskManagementConstants.STATUS_RUNNING, null);
      log.info().message("Entitlement import skipped — disabled in task configuration").log();
      auditService.info(executionId, task.getId(), task.getTenantId(),
          TaskAuditEventType.ENTITLEMENT_IMPORT_SKIPPED,
          "Entitlement import skipped — disabled in task configuration");
    } // end if (importEntitlements)

    // ===================================================================
    //  PHASE 2: Account Import
    //  Runs after entitlement import so that account membership attributes
    //  (memberOf/groups) can be resolved against the populated entitlement
    //  catalog. Each account goes through: shadow → correlation → reaction.
    //  Skipped when account import is disabled in task config.
    // ===================================================================
    Iterator<ConnectorObject> accountIterator;
    if (importAccounts) {
    auditService.info(executionId, task.getId(), task.getTenantId(),
        TaskAuditEventType.ACCOUNT_IMPORT_STARTED, "Account import phase started");
    accountIterator = timedConnectorStub.searchObjects(searchRequest);

    // Load account schema attribute names for filtering
    List<String> accountSchemaAttrNames = applicationConfigService.getAccountSchemaAttributeNames(
        task.getTenantId(), task.getApplicationId());

    // Batch processing: collect accounts into batches and send via batch gRPC.
    // Reduces RPC overhead from O(n) individual calls to O(n / batchSize) batch calls.
    List<ConnectorObject> batch = new java.util.ArrayList<>(TaskManagementConstants.SYNC_BATCH_SIZE);

    while (accountIterator.hasNext()) {
      ConnectorObject account = accountIterator.next();
      batch.add(account);
      accountCount++;

      // When batch is full or no more accounts, flush the batch
      if (batch.size() >= TaskManagementConstants.SYNC_BATCH_SIZE || !accountIterator.hasNext()) {
        try {
          // Build batch request with shared fields + per-object entries
          ProcessImportedObjectBatchRequest.Builder batchRequestBuilder =
              ProcessImportedObjectBatchRequest.newBuilder()
                  .setTenantId(task.getTenantId())
                  .setResourceId(task.getApplicationId().toString())
                  .setObjectClass(TaskManagementConstants.OBJECT_CLASS_ACCOUNT)
                  .setKind("account")
                  .setIdentityType(identityType != null ? identityType : "")
                  .setSyncRunId(syncRunId)
                  .setTaskType(syncTaskType)
                  .setDryRun(dryRun);

          if (!taskReactions.isEmpty()) {
            batchRequestBuilder.addAllTaskReactions(taskReactions);
          }
          // Pass schema attribute names for filtering — only these get stored
          if (!accountSchemaAttrNames.isEmpty()) {
            batchRequestBuilder.addAllSchemaAttributeNames(accountSchemaAttrNames);
          }

          for (ConnectorObject batchAccount : batch) {
            batchRequestBuilder.addObjects(
                BatchObjectEntry.newBuilder()
                    .setResourceUid(batchAccount.getUid())
                    .setAttributes(batchAccount.getAttributes())
                    .build());
            // Track seen UIDs for dry-run finalization correctness
            if (dryRun) {
              seenResourceUids.add(batchAccount.getUid());
            }
          }

          // Send batch to identity-service
          ProcessImportedObjectBatchResponse batchResponse =
              timedShadowStub.processImportedObjectBatch(batchRequestBuilder.build());

          // Process batch results — each response carries its own resourceUid
          for (int i = 0; i < batchResponse.getResultsCount(); i++) {
            ProcessImportedObjectResponse syncResponse = batchResponse.getResults(i);
            String uid = syncResponse.getResourceUid().isEmpty()
                ? (i < batch.size() ? batch.get(i).getUid() : "unknown")
                : syncResponse.getResourceUid();

            if (!syncResponse.getSuccess()) {
              errorCount++;
              log.warn()
                  .message(String.format(
                      "Sync operation FAILED for uid=%s: %s", uid, syncResponse.getMessage()))
                  .log();

              // Still capture failed accounts in execution details
              if (accountDetails.size() < MAX_STORED_ACCOUNT_DETAILS) {
                accountDetails.add(AccountPreview.builder()
                    .resourceUid(uid)
                    .shadowId(syncResponse.getShadowId())
                    .situation("ERROR")
                    .shadowExists(false)
                    .resolvedReaction("")
                    .action("ERROR")
                    .linkedIdentityId("")
                    .attributeChanges(0)
                    .changes(Collections.emptyList())
                    .message(syncResponse.getMessage())
                    .build());
              }
              continue;
            }

            // Track statistics based on action
            SynchronizationAction respAction = syncResponse.getAction();
            if (respAction == SynchronizationAction.IDENTITY_CREATED) {
              createdCount++;
            } else if (respAction == SynchronizationAction.IDENTITY_UPDATED) {
              updatedCount++;
            } else if (respAction == SynchronizationAction.IDENTITY_LINKED) {
              linkedCount++;
            }
            if (syncResponse.getSituation() == SynchronizationSituation.UNMATCHED) {
              unmatchedCount++;
            } else if (syncResponse.getSituation() == SynchronizationSituation.DISPUTED) {
              disputedCount++;
            }

            // Collect per-account details (capped at MAX_STORED_ACCOUNT_DETAILS)
            if (accountDetails.size() < MAX_STORED_ACCOUNT_DETAILS) {
              List<AttributeChangeEntry> changes = syncResponse.getAttributeChangesList().stream()
                  .map(ac -> AttributeChangeEntry.builder()
                      .fieldName(ac.getFieldName())
                      .oldValue(ac.getOldValue())
                      .newValue(ac.getNewValue())
                      .build())
                  .collect(Collectors.toList());

              accountDetails.add(AccountPreview.builder()
                  .resourceUid(uid)
                  .shadowId(syncResponse.getShadowId())
                  .situation(syncResponse.getSituation().name())
                  .shadowExists(syncResponse.getSituation() == SynchronizationSituation.LINKED)
                  .resolvedReaction(syncResponse.getResolvedReaction())
                  .action(syncResponse.getAction().name())
                  .linkedIdentityId(syncResponse.getLinkedIdentityId())
                  .attributeChanges(changes.size())
                  .changes(changes)
                  .message(syncResponse.getMessage())
                  .build());
            }

            log.debug()
                .message(String.format(
                    "Processed account: uid=%s, shadow=%s, situation=%s, action=%s",
                    uid, syncResponse.getShadowId(),
                    syncResponse.getSituation(), syncResponse.getAction()))
                .log();
          }

          writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
              String.format("Processed batch of %d accounts (%d total so far)", batch.size(), accountCount),
              TaskManagementConstants.STATUS_RUNNING, null);
          auditService.info(executionId, task.getId(), task.getTenantId(),
              TaskAuditEventType.ACCOUNT_BATCH_PROCESSED,
              String.format("Batch of %d accounts processed (%d total so far)", batch.size(), accountCount),
              String.format("{\"batchSize\":%d,\"totalSoFar\":%d,\"created\":%d,\"updated\":%d," +
                  "\"linked\":%d,\"unmatched\":%d,\"disputed\":%d,\"errors\":%d}",
                  batch.size(), accountCount, createdCount, updatedCount,
                  linkedCount, unmatchedCount, disputedCount, errorCount));

          // Persist incremental KPI stats so the running page can show live progress
          taskRun.setProcessedCount(accountCount + entitlementCount);
          taskRun.setCreatedCount(createdCount);
          taskRun.setUpdatedCount(updatedCount);
          taskRun.setLinkedCount(linkedCount);
          taskRun.setFailedCount(errorCount);
          taskRun.setSkippedCount(disputedCount);
          taskRun.setEntitlementsImported(entitlementCount);
          taskRunRepository.save(taskRun);

        } catch (Exception e) {
          errorCount += batch.size();
          log.error(e)
              .message(String.format("Failed to process batch of %d accounts: %s",
                  batch.size(), e.getMessage()))
              .log();
          writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_ERROR,
              String.format("Batch processing failed for %d accounts: %s",
                  batch.size(), e.getMessage()),
              TaskManagementConstants.STATUS_RUNNING, e);
        }

        batch.clear();

        // Cooperative checkpoint: pause if requested, abort if cancelled.
        jobCheckpoint(task.getId(), task, executionId);
      }
    }
    } else {
      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
          "Account import skipped — disabled in task configuration",
          TaskManagementConstants.STATUS_RUNNING, null);
      log.info().message("Account import skipped — disabled in task configuration").log();
      auditService.info(executionId, task.getId(), task.getTenantId(),
          TaskAuditEventType.ACCOUNT_IMPORT_SKIPPED,
          "Account import skipped — disabled in task configuration");
    } // end if (importAccounts)

    // ===================================================================
    //  PHASE 3: Finalization
    // ===================================================================

    // Finalize sync run — called for ALL task types.
    // For RECONCILIATION: detects deleted accounts + entitlement cleanup + manager resolution.
    // For IMPORT: entitlement cleanup + manager resolution only (no deletion detection).
    int finalDeletedCount = 0;
    int finalDeletedEntitlements = 0;
    int finalRevokedAssignments = 0;
    try {
      FinalizeSyncRunRequest.Builder finalizeBuilder = FinalizeSyncRunRequest.newBuilder()
              .setTenantId(task.getTenantId())
              .setResourceId(task.getApplicationId().toString())
              .setSyncRunId(syncRunId)
              .setTaskType(syncTaskType)
              .setIdentityType(identityType != null ? identityType : "")
              .setDryRun(dryRun)
              // Skip entitlement cleanup when the connector search did not succeed.
              // groupSearchSucceeded is true ONLY when the connector responded to __GROUP__
              // search (even with 0 results — meaning the source genuinely has no groups).
              // It stays false when the connector returned UNIMPLEMENTED, threw an error,
              // or the outer catch fired. This prevents wiping entitlements whose
              // lastSyncRunId belongs to a prior run when the connector is unreachable.
              .setSkipEntitlementCleanup(!importEntitlements || !groupSearchSucceeded)
              // Skip assignment cleanup when accounts were not imported in this run.
              // Assignments are stamped with syncRunId only during account import — if accounts
              // were not processed this run, all existing assignments appear stale and get deleted.
              .setSkipAssignmentCleanup(!importAccounts);

      // Safety audit: log when the connector-failure guard activates
      if (importEntitlements && !groupSearchSucceeded) {
        String safetyMsg = "Entitlement cleanup SKIPPED — connector __GROUP__ search did not succeed (UNIMPLEMENTED, error, or outer catch fired)";
        writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_WARN,
            safetyMsg, TaskManagementConstants.STATUS_RUNNING, null);
        auditService.warn(executionId, task.getId(), task.getTenantId(),
            TaskAuditEventType.ENTITLEMENT_IMPORT_SKIPPED, safetyMsg);
      }

      // For dry-run: pass seen UIDs so finalize can correctly exclude them
      // from absent-shadow detection (dry-run never writes lastSyncRunId)
      if (dryRun && seenResourceUids != null && !seenResourceUids.isEmpty()) {
        finalizeBuilder.addAllSeenResourceUids(seenResourceUids);
      }

      // Add task-level reactions for deletion handling
      if (!taskReactions.isEmpty()) {
        finalizeBuilder.addAllTaskReactions(taskReactions);
      }

      FinalizeSyncRunResponse fr = timedShadowStub.finalizeSyncRun(finalizeBuilder.build());
      finalDeletedCount = fr.getDeletedCount();
      finalDeletedEntitlements = fr.getDeletedEntitlements();
      finalRevokedAssignments = fr.getRevokedAssignments();

      String finalizeMsg = String.format(
          "Sync finalized: %d shadows marked DELETED, %d identities disabled, %d entitlements removed, %d assignments revoked — %s",
          fr.getDeletedCount(), fr.getIdentitiesDisabled(),
          fr.getDeletedEntitlements(), fr.getRevokedAssignments(), fr.getMessage());
      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
          finalizeMsg, TaskManagementConstants.STATUS_RUNNING, null);
      auditService.info(executionId, task.getId(), task.getTenantId(),
          TaskAuditEventType.SYNC_FINALIZED, finalizeMsg,
          String.format("{\"shadowsDeleted\":%d,\"identitiesDisabled\":%d,\"entitlementsDeleted\":%d,\"assignmentsRevoked\":%d}",
              fr.getDeletedCount(), fr.getIdentitiesDisabled(),
              fr.getDeletedEntitlements(), fr.getRevokedAssignments()));
    } catch (Exception e) {
      log.warn().message("Failed to finalize sync run: " + e.getMessage()).log();
    }

    String summary = String.format(
        "%s: %d accounts processed | %d created | %d updated | %d linked | %d unmatched | %d disputed | %d errors | %d entitlements imported",
        dryRun ? "Preview completed" : "Import completed",
        accountCount, createdCount, updatedCount, linkedCount, unmatchedCount, disputedCount, errorCount, entitlementCount);

    writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
        summary, TaskManagementConstants.STATUS_RUNNING, null);
    log.info().message(summary).log();

    // Persist sync statistics directly to the TaskRun entity that was passed in
    // processedCount = total objects touched across all phases (accounts + entitlements)
    taskRun.setProcessedCount(accountCount + entitlementCount);
    taskRun.setCreatedCount(createdCount);
    taskRun.setUpdatedCount(updatedCount);
    taskRun.setDeletedCount(finalDeletedCount);
    taskRun.setLinkedCount(linkedCount);
    taskRun.setFailedCount(errorCount);
    taskRun.setSkippedCount(disputedCount);
    taskRun.setEntitlementsImported(entitlementCount);

    // Serialize per-account execution details (same format as preview runs)
    try {
      Map<String, Object> executionDetailsMap = new LinkedHashMap<>();
      executionDetailsMap.put("accounts", accountDetails);
      executionDetailsMap.put("entitlements", entitlementDetails);
      executionDetailsMap.put("deletions", Collections.emptyList());
      executionDetailsMap.put("summary", summary);
      executionDetailsMap.put("unchanged", 0);
      executionDetailsMap.put("entitlementStats", Map.of(
          "deletedEntitlements", finalDeletedEntitlements,
          "revokedAssignments", finalRevokedAssignments,
          "entitlementsImported", entitlementCount
      ));
      taskRun.setPreviewDetails(objectMapper.writeValueAsString(executionDetailsMap));
    } catch (Exception e) {
      log.warn().message("Failed to serialize execution details: " + e.getMessage()).log();
    }
    // Note: taskRun will be saved by handleSuccess() after this method returns
  }

  private void handleSuccess(UUID taskId, TaskRun taskRun, String executionId) {
    updateTaskStatus(taskId, ExecutionStatus.RUNNABLE, null, Instant.now());
    updateTaskRunStatus(taskRun, ExecutionStatus.COMPLETED, null, null);

    taskRepository
        .findById(taskId)
        .ifPresent(
            task -> {
              writeLog(
                  task,
                  executionId,
                  TaskManagementConstants.LOG_LEVEL_INFO,
                  TaskManagementConstants.MSG_TASK_COMPLETED_SUCCESS,
                  TaskManagementConstants.STATUS_COMPLETED,
                  null);
              auditService.info(executionId, taskId, task.getTenantId(),
                  TaskAuditEventType.TASK_COMPLETED,
                  TaskManagementConstants.MSG_TASK_COMPLETED_SUCCESS);
            });
    log.info()
        .message(String.format(TaskManagementConstants.MSG_TASK_EXECUTION_SUCCEEDED, executionId))
        .log();
  }

  private void handleCancelled(UUID taskId, TaskRun taskRun, String executionId) {
    // Save TaskRun FIRST — this must always succeed regardless of what happens
    // to the Task entity. If TaskRun is persisted, the frontend will see CANCELLED
    // and the run will never be stuck as RUNNING.
    updateTaskRunStatus(taskRun, ExecutionStatus.CANCELLED, TaskManagementConstants.MSG_TASK_CANCELLED, null);

    // Task entity update is best-effort — a DB check constraint violation (e.g.
    // missing CANCELLED value during a partial migration) must not prevent TaskRun
    // from being marked correctly.
    try {
      updateTaskStatus(taskId, ExecutionStatus.CANCELLED, null, Instant.now());
    } catch (Exception e) {
      log.warn()
          .message(String.format(
              "Could not update Task entity status to CANCELLED for taskId=%s — TaskRun already persisted. Error: %s",
              taskId, e.getMessage()))
          .log();
    }

    taskRepository
        .findById(taskId)
        .ifPresent(
            task -> {
              writeLog(
                  task,
                  executionId,
                  TaskManagementConstants.LOG_LEVEL_WARN,
                  TaskManagementConstants.MSG_TASK_CANCELLED,
                  TaskManagementConstants.STATUS_CANCELLED,
                  null);
              auditService.warn(executionId, taskId, task.getTenantId(),
                  TaskAuditEventType.TASK_CANCELLED,
                  TaskManagementConstants.MSG_TASK_CANCELLED);
            });
    log.warn()
        .message(String.format(TaskManagementConstants.MSG_TASK_CANCELLED_LOG, executionId))
        .log();
  }

  private void handleFailure(UUID taskId, TaskRun taskRun, String executionId, Exception e) {
    updateTaskStatus(taskId, ExecutionStatus.FAILED, null, Instant.now());
    updateTaskRunStatus(taskRun, ExecutionStatus.FAILED, e.getMessage(), getStackTrace(e));

    taskRepository
        .findById(taskId)
        .ifPresent(
            task -> {
              writeLog(
                  task,
                  executionId,
                  TaskManagementConstants.LOG_LEVEL_ERROR,
                  TaskManagementConstants.MSG_TASK_EXECUTION_FAILED,
                  TaskManagementConstants.STATUS_FAILED,
                  e);
              auditService.error(executionId, taskId, task.getTenantId(),
                  TaskAuditEventType.TASK_FAILED,
                  TaskManagementConstants.MSG_TASK_EXECUTION_FAILED + ": " + e.getMessage());
            });
    log.error(e)
        .message(String.format(TaskManagementConstants.MSG_TASK_EXECUTION_FAILED_LOG, executionId))
        .log();
  }

  /**
   * Updates and persists the TaskRun status immediately to the database.
   *
   * @param taskRun The TaskRun entity to update
   * @param status The new execution status
   * @param errorMessage Optional error message for failed executions
   * @param stackTrace Optional stack trace for failed executions
   */
  private void updateTaskRunStatus(
      TaskRun taskRun, ExecutionStatus status, String errorMessage, String stackTrace) {
    taskRun.setStatus(status);
    taskRun.setEndTime(Instant.now());
    taskRun.calculateDuration();

    if (errorMessage != null) {
      taskRun.setErrorMessage(errorMessage);
    }
    if (stackTrace != null) {
      taskRun.setStackTrace(stackTrace);
    }

    taskRunRepository.save(taskRun);
  }

  private void handleRetry(JobExecutionContext context, Exception e) throws JobExecutionException {
    JobDataMap dataMap = context.getJobDetail().getJobDataMap();
    int maxRetries = dataMap.containsKey("maxRetries") ? dataMap.getIntFromString("maxRetries") : 0;
    int refireCount = context.getRefireCount();

    if (refireCount < maxRetries) {
      taskRepository
          .findById(UUID.fromString(dataMap.getString("taskId")))
          .ifPresent(
              task ->
                  writeLog(
                      task,
                      context.getFireInstanceId(),
                      TaskManagementConstants.LOG_LEVEL_WARN,
                      String.format(
                          TaskManagementConstants.MSG_RETRY_ATTEMPT, refireCount + 1, maxRetries),
                      TaskManagementConstants.STATUS_RETRYING,
                      null));
      throw new JobExecutionException(e, true);
    } else {
      throw new JobExecutionException(e, false);
    }
  }

  private void updateTaskStatus(
      UUID taskId, ExecutionStatus status, Instant startTime, Instant finishTime) {
    taskRepository
        .findById(taskId)
        .ifPresent(
            task -> {
              task.setStatus(status);
              if (startTime != null) task.setLastRunStartTimestamp(startTime);
              if (finishTime != null) {
                task.setLastRunFinishTimestamp(finishTime);
                task.setLastExecution(finishTime);
              }
              taskRepository.save(task);
            });
  }

  private void writeLog(
      Task task, String executionId, String level, String message, String status, Exception e) {
    TaskExecutionLog.TaskExecutionLogBuilder logBuilder =
        TaskExecutionLog.builder()
            .id(UUID.randomUUID().toString())
            .taskId(task.getId())
            .tenantId(task.getTenantId())
            .applicationId(task.getApplicationId())
            .executionId(executionId)
            .level(level)
            .message(message)
            .status(status);

    if (e != null) {
      logBuilder.exception(e.getMessage()).stackTrace(getStackTrace(e));
    }
    logService.writeLog(logBuilder.build());
  }

  private String getExecutionNodeId() {
    try {
      return java.net.InetAddress.getLocalHost().getHostName();
    } catch (Exception e) {
      return TaskManagementConstants.UNKNOWN_NODE_ID;
    }
  }

  private String getStackTrace(Exception e) {
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }

  private Map<String, Object> structToMap(Struct struct) {
    Map<String, Object> result = new LinkedHashMap<>();
    struct.getFieldsMap().forEach((key, value) -> result.put(key, valueToObject(value)));
    return result;
  }

  private Object valueToObject(Value value) {
    switch (value.getKindCase()) {
      case NULL_VALUE:
        return null;
      case NUMBER_VALUE:
        return value.getNumberValue();
      case STRING_VALUE:
        return value.getStringValue();
      case BOOL_VALUE:
        return value.getBoolValue();
      case STRUCT_VALUE:
        return structToMap(value.getStructValue());
      case LIST_VALUE:
        List<Object> list = new ArrayList<>();
        value.getListValue().getValuesList().forEach(v -> list.add(valueToObject(v)));
        return list.size() == 1 ? list.get(0) : list;
      default:
        return null;
    }
  }

  /**
   * Parses task configuration JSON and extracts sync reaction overrides.
   * Schema expected: { "account": { "scenarios": [ { "scenario": "...", "action": "..." } ] } }
   *
   * @param taskConfigJson The task configuration as JSON string
   * @return List of SyncReaction proto objects, empty if none configured
   */
  @SuppressWarnings("unchecked")
  private List<SyncReaction> parseTaskReactions(String taskConfigJson) {
    List<SyncReaction> reactions = new ArrayList<>();
    if (taskConfigJson == null || taskConfigJson.isBlank()) {
      return reactions;
    }

    try {
      Map<String, Object> config = objectMapper.readValue(taskConfigJson,
          new TypeReference<Map<String, Object>>() {});

      // Navigate to account.scenarios
      Object accountObj = config.get("account");
      if (!(accountObj instanceof Map)) {
        return reactions;
      }

      Map<String, Object> account = (Map<String, Object>) accountObj;
      Object scenariosObj = account.get("scenarios");
      if (!(scenariosObj instanceof List)) {
        return reactions;
      }

      List<Map<String, Object>> scenarios = (List<Map<String, Object>>) scenariosObj;
      for (Map<String, Object> sc : scenarios) {
        String scenario = (String) sc.get("scenario");
        String action = (String) sc.get("action");
        if (scenario != null && action != null) {
          // Map frontend scenario names to backend situation names
          String situation = mapScenarioToSituation(scenario);
          reactions.add(SyncReaction.newBuilder()
              .setSituation(situation)
              .setAction(action)
              .build());
        }
      }

      log.debug().message(String.format("Parsed %d task reactions from config", reactions.size())).log();
    } catch (Exception e) {
      log.warn().message("Failed to parse task config reactions: " + e.getMessage()).log();
    }

    return reactions;
  }

  /**
   * Checks whether a specific import scope (account or entitlement) is enabled in task config.
   * Defaults to true for backward compatibility — if the field is missing or config is null,
   * the scope is considered enabled.
   *
   * @param taskConfigJson The task configuration JSON string
   * @param scope          "account" or "entitlement"
   * @return true if the scope is enabled (or config is missing)
   */
  @SuppressWarnings("unchecked")
  private boolean isImportScopeEnabled(String taskConfigJson, String scope) {
    if (taskConfigJson == null || taskConfigJson.isBlank()) {
      return true; // No config → both enabled (backward compatible)
    }
    try {
      Map<String, Object> config = objectMapper.readValue(taskConfigJson,
          new TypeReference<Map<String, Object>>() {});
      Object scopeObj = config.get(scope);
      if (!(scopeObj instanceof Map)) {
        return true; // Scope section missing → enabled
      }
      Map<String, Object> scopeConfig = (Map<String, Object>) scopeObj;
      Object enabledObj = scopeConfig.get("enabled");
      if (enabledObj instanceof Boolean) {
        return (Boolean) enabledObj;
      }
      return true; // Field missing → enabled
    } catch (Exception e) {
      log.warn().message("Failed to parse import scope from task config: " + e.getMessage()).log();
      return true; // Parse error → default enabled
    }
  }

  /**
   * Maps frontend scenario names to backend synchronization situation names.
   */
  private String mapScenarioToSituation(String scenario) {
    switch (scenario.toLowerCase()) {
      case "deleted":
        return "DELETED";
      case "updated":
      case "linked":
        return "LINKED";
      case "unlinked":
        return "UNLINKED";
      case "unmatched":
        return "UNMATCHED";
      default:
        return scenario.toUpperCase();
    }
  }

  /**
   * Maps the task's category string to the corresponding proto SyncTaskType.
   * Falls back to IMPORT for unrecognized categories (backward compatibility).
   */
  private SyncTaskType resolveSyncTaskType(String category) {
    if (category == null) {
      return SyncTaskType.IMPORT;
    }
    switch (category.toUpperCase()) {
      case "IMPORT":
        return SyncTaskType.IMPORT;
      case "RECONCILIATION":
        return SyncTaskType.RECONCILIATION;
      case "DISCOVERY":
        return SyncTaskType.DISCOVERY;
      default:
        log.warn().message(String.format("Unknown task category '%s', defaulting to IMPORT", category)).log();
        return SyncTaskType.IMPORT;
    }
  }

  /**
   * Cooperative checkpoint called at each batch boundary.
   *
   * <ul>
   *   <li>If a <b>pause</b> is pending: logs "Task paused", blocks until resumed or cancelled,
   *       then logs "Task resumed".</li>
   *   <li>If a <b>cancel</b> is pending: throws {@link TaskCancelledException}, causing
   *       {@code handleCancelled} to mark the run as CANCELLED (not FAILED).</li>
   * </ul>
   */
  private void jobCheckpoint(UUID taskId, Task task, String executionId) throws Exception {
    boolean wasPaused = stateRegistry.isPauseRequested(taskId);
    if (wasPaused) {
      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
          "Task paused — waiting for resume signal", TaskManagementConstants.STATUS_RUNNING, null);
      log.info().message(String.format("Task %s paused at batch boundary", taskId)).log();
    }
    try {
      boolean cancelled = stateRegistry.checkpointAndMaybePause(taskId);
      if (cancelled) {
        // handleCancelled() in the catch block will write the audit log entry.
        throw new TaskCancelledException();
      }
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Task thread interrupted during pause wait", ie);
    }
    if (wasPaused) {
      writeLog(task, executionId, TaskManagementConstants.LOG_LEVEL_INFO,
          "Task resumed — continuing processing", TaskManagementConstants.STATUS_RUNNING, null);
      log.info().message(String.format("Task %s resumed", taskId)).log();
    }
  }
}
