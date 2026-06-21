package com.aidentx.iam.entitlement_management.service.impl;

import static com.aidentx.iam.entitlement_management.config.constants.EntitlementAuditConstants.PRIORITY_SKIP_FIELD;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.aidentx.iam.entitlement_management.config.constants.EntitlementAuditConstants;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementAuditHistoryDto;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAuditLog;
import com.aidentx.iam.entitlement_management.repository.EntitlementAuditLogRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementAuditLogService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link EntitlementAuditLogService}.
 *
 * <p>All write methods are executed asynchronously and wrapped in
 * try-catch persistence failures are logged but never propagate
 * to the caller. Business operations are never blocked by audit
 * infrastructure failures.
 */
@Service
@Transactional
public class EntitlementAuditLogServiceImpl implements EntitlementAuditLogService {

    private final EntitlementAuditLogRepository repository;
     private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    /**
     * Constructs the service with persistence and logging dependencies.
     *
     * @param repository    the audit log JPA repository
     * @param loggerFactory the unified logger factory
     */
    public EntitlementAuditLogServiceImpl(EntitlementAuditLogRepository repository,
                                          ObjectMapper objectMapper,
                                          UnifiedLoggerFactory loggerFactory) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementAuditConstants.LOG_CATEGORY);
    }

    // ===================================================================
    //  Write Methods
    // ===================================================================

       /** Governance fields that are meaningful for audit history display. */
    private static final Set<String> GOVERNANCE_AUDIT_FIELDS = Set.of(
            "displayName",
            "description",
            "lifecycle",
            "riskLevel",
            "kind",
            "riskReason",
            "requestable",
            "classification",
            "identityType",
            "privilegeLevel",
            "ownerId",
            "requireApproval"
    );

    @Override
    @Async
    public void logGovernanceUpdate(String tenantId, UUID entitlementId, String entitlementName,
                                    UUID resourceId, String resourceName,
                                    String actorId, String actorUsername,
                                    String description,
                                    JsonNode stateBefore, JsonNode stateAfter) {
        persistSafely(() -> {
            // Filter snapshots to only governance fields
            JsonNode filteredBefore = filterToGovernanceFields(stateBefore);
            JsonNode filteredAfter = filterToGovernanceFields(stateAfter);

            EntitlementAuditLog log = new EntitlementAuditLog();
            log.setTenantId(tenantId);
            log.setEntitlementId(entitlementId);
            log.setEntitlementName(entitlementName);
            log.setResourceId(resourceId);
            log.setResourceName(resourceName);
            log.setAction(EntitlementAuditConstants.ACTION_GOVERNANCE_UPDATED);
            log.setTriggerSource(EntitlementAuditConstants.TRIGGER_API);
            log.setDescription(description);
            log.setActorId(actorId);
            log.setActorUsername(actorUsername);
            log.setStateBefore(filteredBefore);
            log.setStateAfter(filteredAfter);
            log.setChangedFields(extractChangedFields(filteredBefore, filteredAfter));

            repository.save(log);
        }, EntitlementAuditConstants.ACTION_GOVERNANCE_UPDATED, entitlementId);
    }

    /**
     * Filters a JSON snapshot to only include governance-relevant fields.
     * Non-governance fields (updatedAt, createdAt, sourceUid, etc.) are excluded.
     *
     * @param fullSnapshot the full JSON snapshot
     * @return a filtered snapshot with only governance fields, or null if input is null
     */
    private JsonNode filterToGovernanceFields(JsonNode fullSnapshot) {
        if (fullSnapshot == null || fullSnapshot.isNull()) {
            return null;
        }

        ObjectNode filtered = objectMapper.createObjectNode();
        for (String field : GOVERNANCE_AUDIT_FIELDS) {
            JsonNode value = fullSnapshot.get(field);
            if (value != null) {
                filtered.set(field, value);
            }
        }

        return filtered.isEmpty() ? null : filtered;
    }

    @Override
    @Async
    public void logRuleCreated(String tenantId, UUID entitlementId, String entitlementName,
                               UUID resourceId,
                               String actorId, String actorUsername,
                               String description,
                               UUID ruleId, String ruleName,
                               JsonNode stateAfter) {
        persistSafely(() -> {
            EntitlementAuditLog log = new EntitlementAuditLog();
            log.setTenantId(tenantId);
            log.setEntitlementId(entitlementId);
            log.setEntitlementName(entitlementName);
            log.setResourceId(resourceId);
            log.setAction(EntitlementAuditConstants.ACTION_RULE_CREATED);
            log.setTriggerSource(EntitlementAuditConstants.TRIGGER_API);
            log.setDescription(description);
            log.setActorId(actorId);
            log.setActorUsername(actorUsername);
            log.setStateAfter(excludeField(stateAfter,PRIORITY_SKIP_FIELD ));
            log.setRuleId(ruleId);
            log.setRuleName(ruleName);

            repository.save(log);
        }, EntitlementAuditConstants.ACTION_RULE_CREATED, entitlementId);
    }

     /**
     * Removes a field from a JSON snapshot.
     *
     * @param node      the JSON node to filter
     * @param fieldName the field name to remove
     * @return a copy without the specified field, or null if input is null
     */
    private JsonNode excludeField(JsonNode node, String fieldName) {
        if (node == null || node.isNull() || !node.isObject()) {
            return node;
        }
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove(fieldName);
        return node;
    }

    @Override
    @Async
    public void logRuleUpdated(String tenantId, UUID entitlementId, String entitlementName,
                               UUID resourceId,
                               String actorId, String actorUsername,
                               String description,
                               UUID ruleId, String ruleName,
                               JsonNode stateBefore, JsonNode stateAfter) {
      persistSafely(() -> {
            EntitlementAuditLog log = new EntitlementAuditLog();
            log.setTenantId(tenantId);
            log.setEntitlementId(entitlementId);
            log.setEntitlementName(entitlementName);
            log.setResourceId(resourceId);
            log.setAction(EntitlementAuditConstants.ACTION_RULE_UPDATED);
            log.setTriggerSource(EntitlementAuditConstants.TRIGGER_API);
            log.setDescription(description);
            log.setActorId(actorId);
            log.setActorUsername(actorUsername);
            log.setStateBefore(stateBefore);
            log.setStateAfter(stateAfter);
            log.setChangedFields(extractChangedFields(stateBefore, stateAfter));
            log.setRuleId(ruleId);
            log.setRuleName(ruleName);

            repository.save(log);
        }, EntitlementAuditConstants.ACTION_RULE_UPDATED, entitlementId);
    }


    @Override
    @Async
    public void logRuleDeleted(String tenantId, UUID entitlementId, String entitlementName,
                               UUID resourceId,
                               String actorId, String actorUsername,
                               String description,
                               UUID ruleId, String ruleName,
                               JsonNode stateBefore) {
        persistSafely(() -> {
            EntitlementAuditLog log = new EntitlementAuditLog();
            log.setTenantId(tenantId);
            log.setEntitlementId(entitlementId);
            log.setEntitlementName(entitlementName);
            log.setResourceId(resourceId);
            log.setAction(EntitlementAuditConstants.ACTION_RULE_DELETED);
            log.setTriggerSource(EntitlementAuditConstants.TRIGGER_API);
            log.setDescription(description);
            log.setActorId(actorId);
            log.setActorUsername(actorUsername);
            log.setStateBefore(stateBefore);
            log.setRuleId(ruleId);
            log.setRuleName(ruleName);

            repository.save(log);
        }, EntitlementAuditConstants.ACTION_RULE_DELETED, entitlementId);
    }

    @Override
    @Async
    public void logRuleEvaluated(String tenantId, UUID entitlementId, String entitlementName,
                                 UUID resourceId,
                                 String actorId, String actorUsername,
                                 String description, String triggerSource,
                                 UUID ruleId, String ruleName, UUID executionId,
                                 int identitiesMatched, int assignmentsCreated,
                                 int assignmentsRevoked, int skippedNoShadow, long durationMs) {
        persistSafely(() -> {
            EntitlementAuditLog log = new EntitlementAuditLog();
            log.setTenantId(tenantId);
            log.setEntitlementId(entitlementId);
            log.setEntitlementName(entitlementName);
            log.setResourceId(resourceId);
            log.setAction(EntitlementAuditConstants.ACTION_RULE_EVALUATED);
            log.setTriggerSource(triggerSource);
            log.setDescription(description);
            log.setActorId(actorId);
            log.setActorUsername(actorUsername);
            log.setRuleId(ruleId);
            log.setRuleName(ruleName);
            log.setExecutionId(executionId);
            log.setIdentitiesMatched(identitiesMatched);
            log.setAssignmentsCreated(assignmentsCreated);
            log.setAssignmentsRevoked(assignmentsRevoked);
            log.setSkippedNoShadow(skippedNoShadow);
            log.setDurationMs(durationMs);

            repository.save(log);
        }, EntitlementAuditConstants.ACTION_RULE_EVALUATED, entitlementId);
    }

    // ===================================================================
    //  Read Methods
    // ===================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementAuditLog> findByEntitlementId(String tenantId, UUID entitlementId, Pageable pageable) {
        return repository.findByTenantIdAndEntitlementIdOrderByCreatedAtDesc(tenantId, entitlementId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementAuditLog> findByAction(String tenantId, String action, Pageable pageable) {
        return repository.findByTenantIdAndActionOrderByCreatedAtDesc(tenantId, action, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementAuditLog> findByActor(String tenantId, String actorUsername, Pageable pageable) {
        return repository.findByTenantIdAndActorUsernameOrderByCreatedAtDesc(tenantId, actorUsername, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementAuditLog> findByRuleId(String tenantId, UUID ruleId, Pageable pageable) {
        return repository.findByTenantIdAndRuleIdOrderByCreatedAtDesc(tenantId, ruleId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementAuditLog> findByDateRange(String tenantId, Long from, Long to, Pageable pageable) {
        return repository.findByTenantIdAndCreatedAtBetweenOrderByCreatedAtDesc(tenantId, from, to, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementAuditLog> findByEntitlementIdAndAction(String tenantId, UUID entitlementId, String action, Pageable pageable) {
        return repository.findByTenantIdAndEntitlementIdAndActionOrderByCreatedAtDesc(tenantId, entitlementId, action, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementAuditHistoryDto> findAuditHistory(String tenantId, UUID entitlementId, String action, Pageable pageable) {
        Page<EntitlementAuditLog> page;

        if (action != null && !action.isBlank()) {
            page = repository.findByTenantIdAndEntitlementIdAndActionOrderByCreatedAtDesc(
                    tenantId, entitlementId, action, pageable);
        } else {
            page = repository.findByTenantIdAndEntitlementIdOrderByCreatedAtDesc(
                    tenantId, entitlementId, pageable);
        }

        return page.map(this::toHistoryDto);
    }

    /**
     * Maps an audit log entity to a UI-friendly DTO.
     *
     * @param log the audit log entity
     * @return the mapped DTO with user-friendly activity label
     */
    private EntitlementAuditHistoryDto toHistoryDto(EntitlementAuditLog log) {
        return new EntitlementAuditHistoryDto(
                log.getId() != null ? log.getId().toString() : null,
                log.getActorUsername(),
                EntitlementAuditConstants.toActivityLabel(log.getAction()),
                log.getDescription(),
                log.getTriggerSource(),
                log.getEntitlementName(),
                log.getRuleName(),
                log.getStateBefore(),
                log.getStateAfter(),
                log.getChangedFields(),
                log.getCreatedAt()
        );
    }

    // ===================================================================
    //  Changed Fields Extraction
    // ===================================================================

    /**
     * Extracts the list of field names that differ between two JSON snapshots.
     *
     * @param before the state before the change
     * @param after  the state after the change
     * @return list of changed field names, or empty list if identical/null
     */
    private List<String> extractChangedFields(JsonNode before, JsonNode after) {
        if (before == null || after == null) {
            return Collections.emptyList();
        }

        List<String> changed = new ArrayList<>();
        Set<String> allFields = new HashSet<>();

        before.fieldNames().forEachRemaining(allFields::add);
        after.fieldNames().forEachRemaining(allFields::add);

        for (String field : allFields) {
            JsonNode beforeVal = before.get(field);
            JsonNode afterVal = after.get(field);

            if (beforeVal == null && afterVal == null) {
                continue;
            }
            if (beforeVal == null || afterVal == null || !beforeVal.equals(afterVal)) {
                changed.add(field);
            }
        }

        Collections.sort(changed);
        return changed;
    }

    // ===================================================================
    //  Safe Persistence Wrapper
    // ===================================================================

    /**
     * Executes a persistence operation within a try-catch boundary.
     * Audit failures are logged but never propagate.
     *
     * @param operation     the persistence operation to execute
     * @param action        the action type (for error logging)
     * @param entitlementId the entitlement ID (for error logging)
     */
    private void persistSafely(Runnable operation, String action, UUID entitlementId) {
        try {
            operation.run();
        } catch (Exception e) {
            logger.error()
                    .category(EntitlementAuditConstants.LOG_CATEGORY)
                    .message(EntitlementAuditConstants.LOG_DB_PERSIST_FAILED)
                    .detail(EntitlementAuditConstants.DETAIL_ACTION, action)
                    .detail(EntitlementAuditConstants.DETAIL_ENTITLEMENT_ID,
                            entitlementId != null ? entitlementId.toString() : EntitlementAuditConstants.DETAIL_NOT_AVAILABLE)
                    .detail(EntitlementAuditConstants.DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName())
                    .detail(EntitlementAuditConstants.DETAIL_ERROR, e.getMessage())
                    .log();
        }
    }
}