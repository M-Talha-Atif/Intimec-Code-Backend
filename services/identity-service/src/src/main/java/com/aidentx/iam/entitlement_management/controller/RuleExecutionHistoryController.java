package com.aidentx.iam.entitlement_management.controller;

import com.aidentx.iam.entitlement_management.model.dto.ExecutionDetailResponse;
import com.aidentx.iam.entitlement_management.model.dto.ExecutionHistoryResponse;
import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionDetail;
import com.aidentx.iam.entitlement_management.model.entity.RuleExecutionHistory;
import com.aidentx.iam.entitlement_management.repository.RuleExecutionDetailRepository;
import com.aidentx.iam.entitlement_management.repository.RuleExecutionHistoryRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for querying rule execution history.
 *
 * <p>Provides read-only endpoints for:
 * <ul>
 *   <li>Run-level execution summaries (per rule, per entitlement, per tenant)</li>
 *   <li>Identity-level execution details (per execution, per identity)</li>
 *   <li>Failure reporting</li>
 *   <li>Latest execution status per rule</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/entitlements/assignment-rules/history")
public class RuleExecutionHistoryController {

    private final RuleExecutionHistoryRepository historyRepository;
    private final RuleExecutionDetailRepository detailRepository;

    @Autowired
    public RuleExecutionHistoryController(RuleExecutionHistoryRepository historyRepository,
                                           RuleExecutionDetailRepository detailRepository) {
        this.historyRepository = historyRepository;
        this.detailRepository = detailRepository;
    }

    /**
     * List all execution history for a tenant.
     */
    @GetMapping
    public ResponseEntity<Page<ExecutionHistoryResponse>> listAll(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(
                historyRepository.findByTenantIdOrderByStartedAtDesc(tenantId, PageRequest.of(page, size))
                        .map(this::toHistoryResponse));
    }

    /**
     * List execution history for a specific rule.
     */
    @GetMapping("/by-rule/{ruleId}")
    public ResponseEntity<Page<ExecutionHistoryResponse>> listByRule(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID ruleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                historyRepository.findByTenantIdAndRuleIdOrderByStartedAtDesc(tenantId, ruleId, PageRequest.of(page, size))
                        .map(this::toHistoryResponse));
    }

    /**
     * List execution history for a specific entitlement.
     */
    @GetMapping("/by-entitlement/{entitlementId}")
    public ResponseEntity<Page<ExecutionHistoryResponse>> listByEntitlement(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID entitlementId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                historyRepository.findByTenantIdAndEntitlementIdOrderByStartedAtDesc(tenantId, entitlementId, PageRequest.of(page, size))
                        .map(this::toHistoryResponse));
    }

    /**
     * Latest execution status per rule (dashboard view).
     */
    @GetMapping("/latest")
    public ResponseEntity<List<ExecutionHistoryResponse>> latestPerRule(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return ResponseEntity.ok(
                historyRepository.findLatestPerRule(tenantId).stream()
                        .map(this::toHistoryResponse).toList());
    }

    /**
     * List identity-level details for a specific execution run.
     */
    @GetMapping("/{executionId}/details")
    public ResponseEntity<Page<ExecutionDetailResponse>> listDetails(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID executionId,
            @RequestParam(required = false) String outcome,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (outcome != null && !outcome.isBlank()) {
            RuleExecutionDetail.Outcome outcomeEnum = RuleExecutionDetail.Outcome.valueOf(outcome.toUpperCase());
            return ResponseEntity.ok(
                    detailRepository.findByTenantIdAndExecutionIdAndOutcomeOrderByProcessedAtDesc(
                                    tenantId, executionId, outcomeEnum, PageRequest.of(page, size))
                            .map(this::toDetailResponse));
        }
        return ResponseEntity.ok(
                detailRepository.findByTenantIdAndExecutionIdOrderByProcessedAtDesc(
                                tenantId, executionId, PageRequest.of(page, size))
                        .map(this::toDetailResponse));
    }

    /**
     * History for a specific identity across all executions.
     * Answers: "What happened to this person over time?"
     */
    @GetMapping("/by-identity/{identityId}")
    public ResponseEntity<Page<ExecutionDetailResponse>> listByIdentity(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID identityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                detailRepository.findByTenantIdAndIdentityIdOrderByProcessedAtDesc(
                                tenantId, identityId, PageRequest.of(page, size))
                        .map(this::toDetailResponse));
    }

    /**
     * History for a specific identity + rule combination.
     * Answers: "Why does this person have/not have this entitlement?"
     */
    @GetMapping("/by-rule/{ruleId}/identity/{identityId}")
    public ResponseEntity<List<ExecutionDetailResponse>> listByRuleAndIdentity(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID ruleId,
            @PathVariable UUID identityId) {
        return ResponseEntity.ok(
                detailRepository.findByTenantIdAndRuleIdAndIdentityIdOrderByProcessedAtDesc(
                                tenantId, ruleId, identityId).stream()
                        .map(this::toDetailResponse).toList());
    }

    // ===================================================================
    //  Response Mapping
    // ===================================================================

    private ExecutionHistoryResponse toHistoryResponse(RuleExecutionHistory h) {
        ExecutionHistoryResponse r = new ExecutionHistoryResponse();
        r.setId(h.getId());
        r.setTenantId(h.getTenantId());
        r.setRuleId(h.getRuleId());
        r.setRuleName(h.getRuleName());
        r.setEntitlementId(h.getEntitlementId());
        r.setEntitlementDisplayName(h.getEntitlementDisplayName());
        r.setResourceId(h.getResourceId());
        r.setTriggerType(h.getTriggerType().name());
        r.setTriggeredBy(h.getTriggeredBy());
        r.setConditionsSnapshot(h.getConditionsSnapshot());
        r.setIdentityTypes(h.getIdentityTypes());
        r.setStatus(h.getStatus().name());
        r.setIdentitiesMatched(h.getIdentitiesMatched());
        r.setAssignmentsCreated(h.getAssignmentsCreated());
        r.setAssignmentsRevoked(h.getAssignmentsRevoked());
        r.setSkippedNoShadow(h.getSkippedNoShadow());
        r.setSkippedAlreadyAssigned(h.getSkippedAlreadyAssigned());
        r.setErrorCount(h.getErrorCount());
        r.setErrorMessage(h.getErrorMessage());
        r.setStartedAt(h.getStartedAt());
        r.setCompletedAt(h.getCompletedAt());
        r.setDurationMs(h.getDurationMs());
        return r;
    }

    private ExecutionDetailResponse toDetailResponse(RuleExecutionDetail d) {
        ExecutionDetailResponse r = new ExecutionDetailResponse();
        r.setId(d.getId());
        r.setTenantId(d.getTenantId());
        r.setExecutionId(d.getExecutionId());
        r.setRuleId(d.getRuleId());
        r.setIdentityId(d.getIdentityId());
        r.setIdentityDisplayName(d.getIdentityDisplayName());
        r.setOutcome(d.getOutcome().name());
        r.setShadowId(d.getShadowId());
        r.setMatchEvidence(d.getMatchEvidence());
        r.setErrorMessage(d.getErrorMessage());
        r.setProcessedAt(d.getProcessedAt());
        return r;
    }
}