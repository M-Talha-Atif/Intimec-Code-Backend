package com.aidentx.iam.access_roles_management.controller;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecution;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentExecutionItem;
import com.aidentx.iam.access_roles_management.service.AccessRoleAssignmentExecutionService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for access-role assignment-rule execution history.
 *
 * <p>Exposes read-only history endpoints for executions and per-identity
 * execution items. CRUD and trigger operations are handled by
 * {@link AccessRoleAssignmentRuleController}.</p>
 *
 * <p>Base path: {@code /api/v1/access-roles/assignment-rules/executions}</p>
 */
@RestController
@RequestMapping(AccessRoleAssignmentRuleConstants.API_EXECUTION_HISTORY_PATH)
public class AccessRoleAssignmentRuleExecutionHistoryController {

    private final AccessRoleAssignmentExecutionService executionService;

    public AccessRoleAssignmentRuleExecutionHistoryController(AccessRoleAssignmentExecutionService executionService) {
        this.executionService = executionService;
    }

    // -------------------------------------------------------------------------
    // Execution listing
    // -------------------------------------------------------------------------

    /**
     * Lists all execution records for a tenant, newest first.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param page     zero-based page number (default {@code 0})
     * @param size     page size (default {@code 20})
     * @return paged execution summaries
     */
    @GetMapping
    public ResponseEntity<Page<AccessRoleAssignmentExecution>> listExecutions(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(executionService.listExecutions(tenantId, page, size));
    }

    /**
     * Lists execution records for a specific rule, newest first.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param ruleId   rule identifier
     * @param page     zero-based page number (default {@code 0})
     * @param size     page size (default {@code 20})
     * @return paged execution summaries for the rule
     */
    @GetMapping("/by-rule/{ruleId}")
    public ResponseEntity<Page<AccessRoleAssignmentExecution>> listExecutionsByRule(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID ruleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(executionService.listExecutionsByRule(tenantId, ruleId, page, size));
    }

    /**
     * Lists execution records for a specific role, newest first.
     *
     * @param tenantId tenant identifier from {@code X-Tenant-Id}
     * @param roleId   role identifier
     * @param page     zero-based page number (default {@code 0})
     * @param size     page size (default {@code 20})
     * @return paged execution summaries for the role
     */
    @GetMapping("/by-role/{roleId}")
    public ResponseEntity<Page<AccessRoleAssignmentExecution>> listExecutionsByRole(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID roleId,
            @RequestParam(required = false) AccessRoleAssignmentExecution.TriggerType triggerType,
            @RequestParam(required = false) AccessRoleAssignmentExecution.ExecutionStatus status,
            @RequestParam(required = false) Long startedFrom,
            @RequestParam(required = false) Long startedTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(executionService.listExecutionsByRole(
                tenantId,
                roleId,
                triggerType,
                status,
                startedFrom,
                startedTo,
                page,
                size));
    }

    // -------------------------------------------------------------------------
    // Execution item listing
    // -------------------------------------------------------------------------

    /**
     * Lists identity-level execution items for a specific execution run, newest first.
     *
     * @param tenantId    tenant identifier from {@code X-Tenant-Id}
     * @param executionId execution identifier
     * @param page        zero-based page number (default {@code 0})
     * @param size        page size (default {@code 20})
     * @return paged execution items
     */
    @GetMapping("/{executionId}/items")
    public ResponseEntity<Page<AccessRoleAssignmentExecutionItem>> listExecutionItems(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID executionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(executionService.listExecutionItems(tenantId, executionId, page, size));
    }

    /**
     * Lists identity-level execution items for a specific execution run filtered by outcome.
     *
     * @param tenantId    tenant identifier from {@code X-Tenant-Id}
     * @param executionId execution identifier
     * @param outcome     outcome filter (e.g. {@code ASSIGNED}, {@code REVOKED}, {@code ERROR})
     * @param page        zero-based page number (default {@code 0})
     * @param size        page size (default {@code 20})
     * @return paged execution items matching the outcome
     */
    @GetMapping("/{executionId}/items/by-outcome/{outcome}")
    public ResponseEntity<Page<AccessRoleAssignmentExecutionItem>> listExecutionItemsByOutcome(
            @RequestHeader(AccessRoleAssignmentRuleConstants.HEADER_TENANT_ID) String tenantId,
            @PathVariable UUID executionId,
            @PathVariable AccessRoleAssignmentExecutionItem.Outcome outcome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                executionService.listExecutionItemsByOutcome(tenantId, executionId, outcome, page, size));
    }
}

