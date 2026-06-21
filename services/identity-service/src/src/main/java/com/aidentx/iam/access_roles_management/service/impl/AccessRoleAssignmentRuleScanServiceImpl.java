package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentExecutionService;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentRuleScanService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Default scan orchestrator for dynamic access-role assignment rules.
 *
 * <p>This implementation intentionally delegates the heavy lifting to
 * {@link AccessRoleAssignmentExecutionService}. That keeps scan orchestration
 * separate from condition evaluation, assignment persistence, stale revocation,
 * execution summary writing, and execution-item outcome logging.</p>
 */
@Service
public class AccessRoleAssignmentRuleScanServiceImpl implements AccessRoleAssignmentRuleScanService {

    private final AccessRoleAssignmentExecutionService executionService;
    private final IUnifiedLogger logger;

    public AccessRoleAssignmentRuleScanServiceImpl(AccessRoleAssignmentExecutionService executionService,
                                                   UnifiedLoggerFactory loggerFactory) {
        this.executionService = executionService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentRuleConstants.API_BASE_PATH);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public AccessRoleAssignmentExecution scanRule(String tenantId, UUID ruleId, String triggeredBy) {
        validateTenantId(tenantId);
        if (ruleId == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_RULE_NOT_FOUND + ": null");
        }
        String actor = resolveTriggeredBy(triggeredBy);

        logger.info()
                .category(AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .message(AccessRoleAssignmentRuleConstants.LOG_SCAN_RULE_STARTED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, ruleId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, actor)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGER_TYPE, AccessRoleAssignmentExecution.TriggerType.MANUAL.name())
                .log();

        AccessRoleAssignmentExecution execution = executionService.executeRule(
                tenantId,
                ruleId,
                AccessRoleAssignmentExecution.TriggerType.MANUAL,
                actor);

        logExecutionSummary(AccessRoleAssignmentRuleConstants.LOG_SCAN_RULE_COMPLETED, execution);
        return execution;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<AccessRoleAssignmentExecution> scanAllRules(String tenantId, String triggeredBy) {
        validateTenantId(tenantId);
        String actor = resolveTriggeredBy(triggeredBy);

        logger.info()
                .category(AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .message(AccessRoleAssignmentRuleConstants.LOG_SCAN_ALL_STARTED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, actor)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGER_TYPE, AccessRoleAssignmentExecution.TriggerType.EVALUATE_ALL.name())
                .log();

        List<AccessRoleAssignmentExecution> executions = executionService.executeAllRules(tenantId, actor);
        logExecutionCollectionSummary(AccessRoleAssignmentRuleConstants.LOG_SCAN_ALL_COMPLETED, tenantId, actor, executions);
        return executions;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<AccessRoleAssignmentExecution> scanImmediateRules(String tenantId, String triggeredBy) {
        validateTenantId(tenantId);
        String actor = resolveTriggeredBy(triggeredBy);

        logger.debug()
                .category(AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .message(AccessRoleAssignmentRuleConstants.LOG_SCAN_IMMEDIATE_STARTED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, actor)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGER_TYPE, AccessRoleAssignmentExecution.TriggerType.ACCESS_ROLE_RULE_CREATED.name())
                .log();

        List<AccessRoleAssignmentExecution> executions = executionService.executeImmediateRules(tenantId, actor);
        logExecutionCollectionSummary(AccessRoleAssignmentRuleConstants.LOG_SCAN_IMMEDIATE_COMPLETED, tenantId, actor, executions);
        return executions;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public List<AccessRoleAssignmentExecution> scanDueCronRules(String tenantId, String triggeredBy) {
        validateTenantId(tenantId);
        String actor = resolveTriggeredBy(triggeredBy);

        logger.debug()
                .category(AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .message(AccessRoleAssignmentRuleConstants.LOG_SCAN_CRON_STARTED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, actor)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGER_TYPE, AccessRoleAssignmentExecution.TriggerType.CRON.name())
                .log();

        List<AccessRoleAssignmentExecution> executions = executionService.executeCronRules(tenantId, actor);
        logExecutionCollectionSummary(AccessRoleAssignmentRuleConstants.LOG_SCAN_CRON_COMPLETED, tenantId, actor, executions);
        return executions;
    }

    private void logExecutionSummary(String message, AccessRoleAssignmentExecution execution) {
        logger.info()
                .category(AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .message(message)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, execution.getTenantId())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, execution.getRuleId().toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, execution.getRoleId().toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_EXECUTION_ID, execution.getId() != null ? execution.getId().toString() : null)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_EXECUTION_STATUS, execution.getStatus().name())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITIES_MATCHED, String.valueOf(execution.getIdentitiesMatched()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLES_ASSIGNED, String.valueOf(execution.getRolesAssigned()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLES_REACTIVATED, String.valueOf(execution.getRolesReactivated()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLES_REVOKED, String.valueOf(execution.getRolesRevoked()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SKIPPED_ALREADY_ASSIGNED, String.valueOf(execution.getSkippedAlreadyAssigned()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ERROR_COUNT, String.valueOf(execution.getErrorCount()))
                .log();
    }

    private void logExecutionCollectionSummary(String message,
                                               String tenantId,
                                               String triggeredBy,
                                               List<AccessRoleAssignmentExecution> executions) {
        int errorCount = executions.stream()
                .mapToInt(AccessRoleAssignmentExecution::getErrorCount)
                .sum();
        int assignedCount = executions.stream()
                .mapToInt(AccessRoleAssignmentExecution::getRolesAssigned)
                .sum();
        int skippedAlreadyAssignedCount = executions.stream()
                .mapToInt(AccessRoleAssignmentExecution::getSkippedAlreadyAssigned)
                .sum();

        logger.info()
                .category(AccessRoleAssignmentRuleConstants.API_BASE_PATH)
                .message(message)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TRIGGERED_BY, triggeredBy)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_EXECUTION_COUNT, String.valueOf(executions.size()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLES_ASSIGNED, String.valueOf(assignedCount))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SKIPPED_ALREADY_ASSIGNED, String.valueOf(skippedAlreadyAssignedCount))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ERROR_COUNT, String.valueOf(errorCount))
                .log();
    }

    private String resolveTriggeredBy(String triggeredBy) {
        return triggeredBy != null && !triggeredBy.isBlank()
                ? triggeredBy.trim()
                : AccessRoleAssignmentRuleConstants.SYSTEM_TRIGGER;
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
        }
    }
}

