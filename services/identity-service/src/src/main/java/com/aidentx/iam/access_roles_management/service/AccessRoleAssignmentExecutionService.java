package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecutionItem;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for evaluating dynamic access-role assignment rules.
 *
 * <p>Executions are tenant-scoped, source-aware, and idempotent. A run creates
 * execution history, assigns or reactivates matching identity-role assignments,
 * and revokes stale role assignments previously produced by the same rule.</p>
 */
public interface AccessRoleAssignmentExecutionService {

    /**
     * Executes one assignment rule for a tenant.
     *
     * @param tenantId tenant identifier
     * @param ruleId rule identifier
     * @param triggerType execution trigger type
     * @param triggeredBy actor or system identifier that triggered the run
     * @return persisted execution summary
     */
    AccessRoleAssignmentExecution executeRule(String tenantId,
                                              UUID ruleId,
                                              AccessRoleAssignmentExecution.TriggerType triggerType,
                                              String triggeredBy);

    /** Executes all enabled rules for a tenant in priority order. */
    List<AccessRoleAssignmentExecution> executeAllRules(String tenantId, String triggeredBy);

    /** Executes enabled rules configured with the IMMEDIATE schedule type. */
    List<AccessRoleAssignmentExecution> executeImmediateRules(String tenantId, String triggeredBy);

    /** Executes currently due CRON rules for a tenant. */
    List<AccessRoleAssignmentExecution> executeCronRules(String tenantId, String triggeredBy);

    /** Lists execution history for a tenant, newest first. */
    Page<AccessRoleAssignmentExecution> listExecutions(String tenantId, int page, int size);

    /** Lists execution history for one rule, newest first. */
    Page<AccessRoleAssignmentExecution> listExecutionsByRule(String tenantId, UUID ruleId, int page, int size);

    /** Lists execution history for one role, newest first. */
    Page<AccessRoleAssignmentExecution> listExecutionsByRole(String tenantId,
                                                             UUID roleId,
                                                             AccessRoleAssignmentExecution.TriggerType triggerType,
                                                             AccessRoleAssignmentExecution.ExecutionStatus status,
                                                             Long startedFrom,
                                                             Long startedTo,
                                                             int page,
                                                             int size);

    /** Lists identity-level details for one execution run. */
    Page<AccessRoleAssignmentExecutionItem> listExecutionItems(String tenantId, UUID executionId, int page, int size);

    /** Lists identity-level details for one execution run filtered by outcome. */
    Page<AccessRoleAssignmentExecutionItem> listExecutionItemsByOutcome(String tenantId,
                                                                        UUID executionId,
                                                                        AccessRoleAssignmentExecutionItem.Outcome outcome,
                                                                        int page,
                                                                        int size);
}

