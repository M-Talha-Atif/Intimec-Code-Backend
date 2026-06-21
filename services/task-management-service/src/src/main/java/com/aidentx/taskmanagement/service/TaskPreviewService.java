package com.aidentx.taskmanagement.service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Service;

import com.aidentx.dal.protos.application.ApplicationDTO;
import com.aidentx.dal.protos.runtime.*;
import com.aidentx.dal.protos.shadow.*;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.taskmanagement.config.TaskManagementConstants;
import com.aidentx.taskmanagement.dto.TaskPreviewResponse;
import com.aidentx.taskmanagement.dto.TaskPreviewResponse.AccountPreview;
import com.aidentx.taskmanagement.dto.TaskPreviewResponse.AttributeChangeEntry;
import com.aidentx.taskmanagement.dto.TaskPreviewResponse.DeletionPreviewEntry;
import com.aidentx.taskmanagement.model.entity.Task;
import com.aidentx.taskmanagement.model.entity.TaskRun;
import com.aidentx.taskmanagement.model.enums.ExecutionStatus;
import com.aidentx.taskmanagement.repository.TaskRepository;
import com.aidentx.taskmanagement.repository.TaskRunRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Struct;

/**
 * Service for executing dry-run/preview of sync tasks.
 * Processes ALL accounts with dry_run=true — exactly like a real execution —
 * so operators see accurate statistics for the full dataset.
 *
 * <p>Memory safety is achieved the same way as the real import pipeline:
 * <ul>
 *   <li>Accounts stream from the connector via gRPC (never all in memory)</li>
 *   <li>Dry-run processing uses batch gRPC calls ({@value #PREVIEW_BATCH_SIZE} per batch)</li>
 *   <li>Only summary counters are kept for all accounts</li>
 *   <li>Per-account detail records (attribute changes) are capped at
 *       {@value #MAX_STORED_ACCOUNT_DETAILS} to keep JSON/DB storage bounded</li>
 * </ul></p>
 */
@Service
public class TaskPreviewService {

    /**
     * Maximum number of per-account detail records to store in the DB/response.
     * Summary statistics always cover ALL processed accounts.
     */
    private static final int MAX_STORED_ACCOUNT_DETAILS = 500;

    /**
     * Batch size for dry-run gRPC calls to the identity-service.
     * Matches the real import pipeline's batch size.
     */
    private static final int PREVIEW_BATCH_SIZE = TaskManagementConstants.SYNC_BATCH_SIZE;

    private final TaskRepository taskRepository;
    private final TaskRunRepository taskRunRepository;
    private final ApplicationConfigService applicationConfigService;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger log;

    private final ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub connectorRuntimeStub;
    private final ShadowSyncServiceGrpc.ShadowSyncServiceBlockingStub shadowSyncStub;

    public TaskPreviewService(
            TaskRepository taskRepository,
            TaskRunRepository taskRunRepository,
            ApplicationConfigService applicationConfigService,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory unifiedLoggerFactory,
            ConnectorRuntimeServiceGrpc.ConnectorRuntimeServiceBlockingStub connectorRuntimeStub,
            ShadowSyncServiceGrpc.ShadowSyncServiceBlockingStub shadowSyncStub) {
        this.taskRepository = taskRepository;
        this.taskRunRepository = taskRunRepository;
        this.applicationConfigService = applicationConfigService;
        this.objectMapper = objectMapper;
        this.log = unifiedLoggerFactory.getLogger(TaskPreviewService.class);
        this.connectorRuntimeStub = connectorRuntimeStub;
        this.shadowSyncStub = shadowSyncStub;
    }

    /**
     * Executes a bounded dry-run preview for the given task.
     * Samples up to {@value #MAX_PREVIEW_ACCOUNTS} accounts from the connector, processes
     * them in batches via the identity-service with dry_run=true, and returns summary
     * statistics plus limited per-account details.
     */
    public TaskPreviewResponse executePreview(UUID taskId, String tenantId) {
        long startTime = System.currentTimeMillis();

        log.info().message(String.format("Starting dry-run preview for task %s", taskId)).log();

        // 1. Load the task
        Task task = taskRepository.findByIdAndTenantId(taskId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(TaskManagementConstants.ERR_TASK_NOT_FOUND, taskId)));

        // 2. Load application config and connector config
        ApplicationDTO application = applicationConfigService.getApplication(tenantId, task.getApplicationId());
        Struct connectorConfig = application.getConnectorConfiguration();

        if (connectorConfig == null || connectorConfig.getFieldsCount() == 0) {
            throw new IllegalStateException(TaskManagementConstants.ERR_MISSING_CONNECTOR_CONFIG);
        }

        String connectorId = connectorConfig
                .getFieldsOrThrow(TaskManagementConstants.CONFIG_CONNECTOR_ID)
                .getStringValue();

        // 3. Test connection (fail fast if connector is unreachable)
        TestConnectionRequest testRequest = TestConnectionRequest.newBuilder()
                .setId(connectorId)
                .setConfiguration(connectorConfig)
                .build();
        TestConnectionResponse testResponse = connectorRuntimeStub.testConnection(testRequest);

        if (!testResponse.getSuccess()) {
            throw new RuntimeException(
                    "Connection test failed during preview: " + testResponse.getMessage());
        }

        // 4. Resolve task type and reactions
        String identityType = application.getIdentityType();
        SyncTaskType syncTaskType = resolveSyncTaskType(task.getCategory());
        List<SyncReaction> taskReactions = parseTaskReactions(task.getTaskConfig());

        // 5. Stream ALL accounts from the connector (same as real execution)
        SearchObjectsRequest searchRequest = SearchObjectsRequest.newBuilder()
                .setId(connectorId)
                .setConfiguration(connectorConfig)
                .setObjectClassName(TaskManagementConstants.OBJECT_CLASS_ACCOUNT)
                .setPageSize(TaskManagementConstants.DEFAULT_PAGE_SIZE)
                .build();

        Iterator<ConnectorObject> accountIterator = connectorRuntimeStub.searchObjects(searchRequest);

        // Use a unique preview run ID (not persisted)
        String previewRunId = "preview-" + UUID.randomUUID();

        // Counters — tracked for ALL accounts, not just a sample
        int totalScanned = 0;
        int wouldCreate = 0;
        int wouldUpdate = 0;
        int wouldLink = 0;
        int unchanged = 0;
        int disputed = 0;
        int errors = 0;

        List<AccountPreview> accounts = new ArrayList<>();

        // Tracks every account UID seen during this preview scan.
        // Passed to finalizeSyncRun so the absent-shadow query correctly excludes
        // accounts that WERE present in this run — preventing the false-positive deletion
        // previews that occur because processDryRun never writes lastSyncRunId to shadows.
        List<String> seenResourceUids = new ArrayList<>();

        // 6. Process ALL accounts in batches with dry_run=true
        //    Same streaming + batching pattern as TaskExecutionJob — memory-safe for 100K+
        List<ConnectorObject> batch = new ArrayList<>(PREVIEW_BATCH_SIZE);

        while (accountIterator.hasNext()) {
            batch.add(accountIterator.next());
            totalScanned++;

            if (batch.size() >= PREVIEW_BATCH_SIZE || !accountIterator.hasNext()) {
                // Build batch request
                ProcessImportedObjectBatchRequest.Builder batchReq = ProcessImportedObjectBatchRequest.newBuilder()
                        .setTenantId(tenantId)
                        .setResourceId(task.getApplicationId().toString())
                        .setObjectClass(TaskManagementConstants.OBJECT_CLASS_ACCOUNT)
                        .setKind("account")
                        .setIdentityType(identityType != null ? identityType : "")
                        .setSyncRunId(previewRunId)
                        .setTaskType(syncTaskType)
                        .setDryRun(true);

                if (!taskReactions.isEmpty()) {
                    batchReq.addAllTaskReactions(taskReactions);
                }

                for (ConnectorObject account : batch) {
                    String uid = account.getUid();
                    batchReq.addObjects(BatchObjectEntry.newBuilder()
                            .setResourceUid(uid)
                            .setAttributes(account.getAttributes())
                            .build());
                    // Track every UID seen so finalizeSyncRun can exclude them
                    // from absent-shadow detection (dry-run correctness fix).
                    seenResourceUids.add(uid);
                }

                try {
                    ProcessImportedObjectBatchResponse batchResponse =
                            shadowSyncStub.processImportedObjectBatch(batchReq.build());

                    for (ProcessImportedObjectResponse response : batchResponse.getResultsList()) {
                        // Track summary counts
                        if (!response.getSuccess()) {
                            errors++;
                            log.warn().message(String.format(
                                    "Preview: account uid=%s failed — %s",
                                    response.getResourceUid().isEmpty()
                                            ? "(uid-missing)"
                                            : response.getResourceUid(),
                                    response.getMessage() != null && !response.getMessage().isBlank()
                                            ? response.getMessage()
                                            : "no error details returned by identity-service")).log();
                        } else {
                            switch (response.getAction()) {
                                case IDENTITY_CREATED: wouldCreate++; break;
                                case IDENTITY_UPDATED: wouldUpdate++; break;
                                case IDENTITY_LINKED: wouldLink++; break;
                                default:
                                    if (response.getSituation() == SynchronizationSituation.DISPUTED) {
                                        disputed++;
                                    } else {
                                        unchanged++;
                                    }
                                    break;
                            }
                        }

                        // Keep per-account details only up to the storage limit
                        if (accounts.size() < MAX_STORED_ACCOUNT_DETAILS) {
                            List<AttributeChangeEntry> changes = response.getAttributeChangesList().stream()
                                    .map(ac -> AttributeChangeEntry.builder()
                                            .fieldName(ac.getFieldName())
                                            .oldValue(ac.getOldValue())
                                            .newValue(ac.getNewValue())
                                            .build())
                                    .collect(Collectors.toList());

                            accounts.add(AccountPreview.builder()
                                    .resourceUid(response.getResourceUid())
                                    .shadowId(response.getShadowId())
                                    .situation(response.getSituation().name())
                                    .shadowExists(response.getSituation() == SynchronizationSituation.LINKED)
                                    .resolvedReaction(response.getResolvedReaction())
                                    .action(response.getAction().name())
                                    .linkedIdentityId(response.getLinkedIdentityId())
                                    .attributeChanges(changes.size())
                                    .changes(changes)
                                    .message(response.getMessage())
                                    .build());
                        }
                    }
                } catch (Exception e) {
                    errors += batch.size();
                    log.warn().message(String.format(
                            "Preview: batch processing failed for %d accounts: %s",
                            batch.size(), e.getMessage())).log();
                }

                batch.clear();
            }
        }

        // 7. Entitlement Import (dry-run) — mirrors TaskExecutionJob Phase 2
        boolean importEntitlements = isImportScopeEnabled(task.getTaskConfig(), "entitlement");
        int entitlementCount = 0;
        int entitlementCreated = 0;
        int entitlementUpdated = 0;
        int entitlementErrors = 0;
        boolean groupSearchSucceeded = false;
        List<TaskPreviewResponse.EntitlementPreview> entitlementDetails = new ArrayList<>();

        if (importEntitlements) {
            try {
                List<ApplicationConfigService.EntitlementObjectClassMeta> entitlementObjectClasses =
                        applicationConfigService.getEntitlementObjectClasses(tenantId, task.getApplicationId());

                log.info().message(String.format("Preview: entitlement object classes to import: %s",
                        entitlementObjectClasses.stream()
                                .map(ApplicationConfigService.EntitlementObjectClassMeta::objectClass)
                                .collect(Collectors.toList()))).log();

                for (ApplicationConfigService.EntitlementObjectClassMeta entitlementMeta : entitlementObjectClasses) {
                    String entitlementObjectClass = entitlementMeta.objectClass();

                    SearchObjectsRequest groupSearchRequest = SearchObjectsRequest.newBuilder()
                            .setId(connectorId)
                            .setConfiguration(connectorConfig)
                            .setObjectClassName(entitlementObjectClass)
                            .setPageSize(TaskManagementConstants.DEFAULT_PAGE_SIZE)
                            .build();

                    Iterator<ConnectorObject> groupIterator;
                    try {
                        groupIterator = connectorRuntimeStub.searchObjects(groupSearchRequest);
                        groupSearchSucceeded = true;
                    } catch (StatusRuntimeException e) {
                        if (e.getStatus().getCode() != Status.Code.UNIMPLEMENTED) {
                            throw e;
                        }
                        log.warn().message("Preview: connector does not support " + entitlementObjectClass + ": " + e.getMessage()).log();
                        continue;
                    } catch (Exception e) {
                        log.warn().message("Preview: failed to search " + entitlementObjectClass + ": " + e.getMessage()).log();
                        continue;
                    }

                    List<ConnectorObject> entitlementBatch = new ArrayList<>(PREVIEW_BATCH_SIZE);

                    while (groupIterator.hasNext()) {
                        ConnectorObject group = groupIterator.next();
                        entitlementBatch.add(group);
                        entitlementCount++;

                        if (entitlementBatch.size() >= PREVIEW_BATCH_SIZE || !groupIterator.hasNext()) {
                            try {
                                ProcessImportedObjectBatchRequest.Builder batchReq =
                                        ProcessImportedObjectBatchRequest.newBuilder()
                                                .setTenantId(tenantId)
                                                .setResourceId(task.getApplicationId().toString())
                                                .setObjectClass(entitlementObjectClass)
                                                .setKind("entitlement")
                                                .setIdentityType(identityType != null ? identityType : "")
                                                .setSyncRunId(previewRunId)
                                                .setTaskType(syncTaskType)
                                                .setDryRun(true);

                                if (entitlementMeta.displayAttribute() != null) {
                                    batchReq.setDisplayAttribute(entitlementMeta.displayAttribute());
                                }
                                if (entitlementMeta.descriptionAttribute() != null) {
                                    batchReq.setDescriptionAttribute(entitlementMeta.descriptionAttribute());
                                }
                                if (entitlementMeta.schemaAttributeNames() != null && !entitlementMeta.schemaAttributeNames().isEmpty()) {
                                    batchReq.addAllSchemaAttributeNames(entitlementMeta.schemaAttributeNames());
                                }

                                for (ConnectorObject batchGroup : entitlementBatch) {
                                    batchReq.addObjects(BatchObjectEntry.newBuilder()
                                            .setResourceUid(batchGroup.getUid())
                                            .setAttributes(batchGroup.getAttributes())
                                            .build());
                                }

                                ProcessImportedObjectBatchResponse batchResponse =
                                        shadowSyncStub.processImportedObjectBatch(batchReq.build());

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
                            } catch (Exception e) {
                                entitlementErrors += entitlementBatch.size();
                                log.warn().message(String.format(
                                        "Preview: entitlement batch processing failed for %d items: %s",
                                        entitlementBatch.size(), e.getMessage())).log();
                            }

                            entitlementBatch.clear();
                        }
                    }
                } // end for (entitlementObjectClass)

                log.info().message(String.format(
                        "Preview entitlement import: %d total, %d created, %d updated, %d errors",
                        entitlementCount, entitlementCreated, entitlementUpdated, entitlementErrors)).log();
            } catch (StatusRuntimeException e) {
                log.warn().message("Preview: entitlement import phase failed: " + e.getMessage()).log();
            } catch (Exception e) {
                log.warn().message("Preview: entitlement import phase failed: " + e.getMessage()).log();
            }
        }

        // 8. Finalize (deletion detection + entitlement cleanup preview).
        // Account deletion detection: RECONCILIATION only.
        // Entitlement cleanup preview: runs whenever entitlements are configured.
        List<DeletionPreviewEntry> deletions = new ArrayList<>();
        List<TaskPreviewResponse.EntitlementDeletionPreviewEntry> entitlementDeletions = new ArrayList<>();
        int wouldDelete = 0;
        int wouldDeleteEntitlements = 0;
        int wouldRevokeAssignments = 0;

        try {
            FinalizeSyncRunRequest.Builder finalizeBuilder = FinalizeSyncRunRequest.newBuilder()
                    .setTenantId(tenantId)
                    .setResourceId(task.getApplicationId().toString())
                    .setSyncRunId(previewRunId)
                    .setTaskType(syncTaskType)
                    .setIdentityType(identityType != null ? identityType : "")
                    .setDryRun(true)
                    .setSkipEntitlementCleanup(!importEntitlements || !groupSearchSucceeded)
                    .setSkipAssignmentCleanup(false);

            if (!taskReactions.isEmpty()) {
                finalizeBuilder.addAllTaskReactions(taskReactions);
            }

            // Pass the full list of UIDs seen during this dry-run scan.
            // Without this, finalizeSyncRun would find ALL shadows absent
            // (since processDryRun never writes lastSyncRunId to any row)
            // and falsely report every account as a deletion candidate.
            if (!seenResourceUids.isEmpty()) {
                finalizeBuilder.addAllSeenResourceUids(seenResourceUids);
            }

            FinalizeSyncRunResponse finalizeResponse =
                    shadowSyncStub.finalizeSyncRun(finalizeBuilder.build());

            wouldDelete = finalizeResponse.getDeletionPreviewsCount();

            deletions = finalizeResponse.getDeletionPreviewsList().stream()
                    .map(dp -> DeletionPreviewEntry.builder()
                            .shadowId(dp.getShadowId())
                            .resourceUid(dp.getResourceUid())
                            .linkedIdentityId(dp.getLinkedIdentityId())
                            .resolvedReaction(dp.getResolvedReaction())
                            .build())
                    .collect(Collectors.toList());

            // Entitlement deletion previews
            wouldDeleteEntitlements = finalizeResponse.getDeletedEntitlements();
            wouldRevokeAssignments = finalizeResponse.getRevokedAssignments();

            entitlementDeletions = finalizeResponse.getEntitlementDeletionPreviewsList().stream()
                    .map(ep -> TaskPreviewResponse.EntitlementDeletionPreviewEntry.builder()
                            .entitlementId(ep.getEntitlementId())
                            .sourceUid(ep.getSourceUid())
                            .displayName(ep.getDisplayName())
                            .objectClass(ep.getObjectClass())
                            .assignmentCount(ep.getAssignmentCount())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn().message("Preview: failed to finalize (deletion/entitlement detection): " + e.getMessage()).log();
        }

        long durationMs = System.currentTimeMillis() - startTime;

        // 9. Build final response
        boolean detailsTruncated = totalScanned > MAX_STORED_ACCOUNT_DETAILS;
        String summary = String.format(
                "Preview complete: %d accounts processed | %d would create | %d would update | %d would link | %d would delete | %d unchanged | %d disputed | %d errors | %d entitlements imported (%d created, %d updated, %d ent. errors) | %d entitlements would be removed | %d assignments would be revoked",
                totalScanned, wouldCreate, wouldUpdate, wouldLink, wouldDelete, unchanged, disputed, errors,
                entitlementCount, entitlementCreated, entitlementUpdated, entitlementErrors,
                wouldDeleteEntitlements, wouldRevokeAssignments);

        log.info().message(summary).log();

        TaskPreviewResponse previewResponse = TaskPreviewResponse.builder()
                .totalAccountsScanned(totalScanned)
                .totalInDataSource(totalScanned)
                .wasTruncated(detailsTruncated)
                .wouldCreate(wouldCreate)
                .wouldUpdate(wouldUpdate)
                .wouldLink(wouldLink)
                .wouldDelete(wouldDelete)
                .unchanged(unchanged)
                .disputed(disputed)
                .errors(errors)
                .accounts(accounts)
                .deletions(deletions)
                .entitlementDeletions(entitlementDeletions)
                .previewedAt(Instant.now())
                .durationMs(durationMs)
                .summary(summary)
                .build();

        // 9. Persist as a TaskRun in history with isPreview=true
        String executionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant previewStartTime = Instant.ofEpochMilli(startTime);

        TaskRun taskRun = new TaskRun();
        taskRun.setExecutionId(executionId);
        taskRun.setTaskId(taskId);
        taskRun.setTenantId(tenantId);
        taskRun.setApplicationId(task.getApplicationId());
        taskRun.setStatus(errors > 0 ? ExecutionStatus.FAILED : ExecutionStatus.COMPLETED);
        taskRun.setStartTime(previewStartTime);
        taskRun.setEndTime(now);
        taskRun.setDurationMs(durationMs);
        taskRun.setPreview(true);

        // Map preview counts to regular run stats
        taskRun.setProcessedCount(totalScanned);
        taskRun.setCreatedCount(wouldCreate);
        taskRun.setUpdatedCount(wouldUpdate);
        taskRun.setDeletedCount(wouldDelete);
        taskRun.setLinkedCount(wouldLink);
        taskRun.setFailedCount(errors);
        taskRun.setSkippedCount(disputed);
        taskRun.setEntitlementsImported(entitlementCount);

        // Serialize preview details (accounts + deletions + entitlements + entitlement deletions) as JSON
        try {
            Map<String, Object> previewDetailsMap = new LinkedHashMap<>();
            previewDetailsMap.put("accounts", accounts);
            previewDetailsMap.put("deletions", deletions);
            previewDetailsMap.put("entitlements", entitlementDetails);
            previewDetailsMap.put("summary", summary);
            previewDetailsMap.put("unchanged", unchanged);
            previewDetailsMap.put("entitlementDeletions", entitlementDeletions);
            previewDetailsMap.put("entitlementStats", Map.of(
                "deletedEntitlements", wouldDeleteEntitlements,
                "revokedAssignments", wouldRevokeAssignments,
                "entitlementsImported", entitlementCount
            ));
            taskRun.setPreviewDetails(objectMapper.writeValueAsString(previewDetailsMap));
        } catch (Exception e) {
            log.warn().message("Failed to serialize preview details: " + e.getMessage()).log();
        }

        taskRunRepository.save(taskRun);
        log.info().message(String.format("Preview saved to history with executionId=%s", executionId)).log();

        previewResponse.setExecutionId(executionId);
        return previewResponse;
    }

    // ─── Helpers (same logic as TaskExecutionJob) ───────────────────────────────

    private SyncTaskType resolveSyncTaskType(String category) {
        if (category == null) return SyncTaskType.IMPORT;
        switch (category.toUpperCase()) {
            case "IMPORT": return SyncTaskType.IMPORT;
            case "RECONCILIATION": return SyncTaskType.RECONCILIATION;
            case "DISCOVERY": return SyncTaskType.DISCOVERY;
            default:
                log.warn().message(String.format("Unknown task category '%s', defaulting to IMPORT", category)).log();
                return SyncTaskType.IMPORT;
        }
    }

    @SuppressWarnings("unchecked")
    private List<SyncReaction> parseTaskReactions(String taskConfigJson) {
        List<SyncReaction> reactions = new ArrayList<>();
        if (taskConfigJson == null || taskConfigJson.isBlank()) return reactions;

        try {
            Map<String, Object> config = objectMapper.readValue(taskConfigJson,
                    new TypeReference<Map<String, Object>>() {});

            Object accountObj = config.get("account");
            if (!(accountObj instanceof Map)) return reactions;

            Map<String, Object> account = (Map<String, Object>) accountObj;
            Object scenariosObj = account.get("scenarios");
            if (!(scenariosObj instanceof List)) return reactions;

            List<Map<String, Object>> scenarios = (List<Map<String, Object>>) scenariosObj;
            for (Map<String, Object> sc : scenarios) {
                String scenario = (String) sc.get("scenario");
                String action = (String) sc.get("action");
                if (scenario != null && action != null) {
                    reactions.add(SyncReaction.newBuilder()
                            .setSituation(mapScenarioToSituation(scenario))
                            .setAction(action)
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn().message("Failed to parse task config reactions: " + e.getMessage()).log();
        }

        return reactions;
    }

    private String mapScenarioToSituation(String scenario) {
        switch (scenario.toLowerCase()) {
            case "deleted": return "DELETED";
            case "updated":
            case "linked": return "LINKED";
            case "unlinked": return "UNLINKED";
            case "unmatched": return "UNMATCHED";
            default: return scenario.toUpperCase();
        }
    }

    @SuppressWarnings("unchecked")
    private boolean isImportScopeEnabled(String taskConfigJson, String scope) {
        if (taskConfigJson == null || taskConfigJson.isBlank()) {
            return true;
        }
        try {
            Map<String, Object> config = objectMapper.readValue(taskConfigJson,
                    new TypeReference<Map<String, Object>>() {});
            Object scopeObj = config.get(scope);
            if (!(scopeObj instanceof Map)) {
                return true;
            }
            Map<String, Object> scopeConfig = (Map<String, Object>) scopeObj;
            Object enabledObj = scopeConfig.get("enabled");
            if (enabledObj instanceof Boolean) {
                return (Boolean) enabledObj;
            }
            return true;
        } catch (Exception e) {
            log.warn().message("Failed to parse import scope from task config: " + e.getMessage()).log();
            return true;
        }
    }
}
