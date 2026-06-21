package com.aidentx.iam.entitlement_management.controller;

import java.util.List;
import java.util.UUID;

import com.aidentx.iam.entitlement_management.config.constants.RuleEvaluationConstants;
import com.aidentx.iam.entitlement_management.model.dto.GlobalScheduleConfigResponse;
import com.aidentx.iam.entitlement_management.model.dto.GlobalScheduleUpdateRequest;
import com.aidentx.iam.entitlement_management.model.dto.GlobalScheduleUpdateResponse;
import com.aidentx.iam.entitlement_management.model.dto.RuleCreateRequest;
import com.aidentx.iam.entitlement_management.model.dto.RuleEvaluationResult;
import com.aidentx.iam.entitlement_management.model.dto.RuleResponse;
import com.aidentx.iam.entitlement_management.model.dto.RuleUpdateRequest;
import com.aidentx.iam.entitlement_management.service.EntitlementAssignmentRuleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/entitlements/assignment-rules")
public class DynamicAssignmentController {

    private final EntitlementAssignmentRuleService ruleService;

    public DynamicAssignmentController(EntitlementAssignmentRuleService ruleService) {
        this.ruleService = ruleService;
    }

    /**
     * Helper to extract the authenticated username from SecurityContext.
     * Falls back to "System" if no authentication is present (e.g., internal calls).
     */
    private String getAuthenticatedUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return RuleEvaluationConstants.SYSTEM_TRIGGER;
    }

    @PostMapping
    public ResponseEntity<RuleResponse> createRule(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody RuleCreateRequest request) {
        RuleResponse response = ruleService.createRule(tenantId, request, getAuthenticatedUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{ruleId}")
    public ResponseEntity<RuleResponse> updateRule(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID ruleId,
            @Valid @RequestBody RuleUpdateRequest request) {
        RuleResponse response = ruleService.updateRule(tenantId, ruleId, request, getAuthenticatedUsername());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{ruleId}")
    public ResponseEntity<RuleResponse> getRule(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID ruleId) {
        RuleResponse response = ruleService.getRule(tenantId, ruleId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<RuleResponse>> listRules(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort) {
        Page<RuleResponse> response = ruleService.listRules(tenantId, page, size, sort);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID ruleId) {
        ruleService.deleteRule(tenantId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{ruleId}/evaluate")
    public ResponseEntity<RuleEvaluationResult> evaluateRule(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @PathVariable UUID ruleId) {
        RuleEvaluationResult result = ruleService.evaluateRule(tenantId, ruleId, getAuthenticatedUsername());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/evaluate-all")
    public ResponseEntity<List<RuleEvaluationResult>> evaluateAllRules(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        List<RuleEvaluationResult> results = ruleService.evaluateAllRules(tenantId, getAuthenticatedUsername());
        return ResponseEntity.ok(results);
    }

        /**
     * GET /api/v1/entitlements/assignment-rules/global-schedule
     *
     * Returns the current global schedule configuration for the tenant.
     * Used by the frontend to pre-populate the schedule config dialog.
     */
    @GetMapping("/global-schedule")
    public ResponseEntity<GlobalScheduleConfigResponse> getGlobalScheduleConfig(
            @RequestHeader("X-Tenant-Id") String tenantId) {
        return ResponseEntity.ok(
                ruleService.getGlobalScheduleConfig(tenantId));
    }

    /**
     * PUT /api/v1/entitlements/assignment-rules/global-schedule
     *
     * Updates the global schedule type of all enabled rules at once.
     * Also persists the config so new rules inherit it.
     * Body: { "scheduleType": "CRON", "cronExpression": "...", "scheduleTimezone": "..." }
     *
     * Returns the new schedule type and number of rules updated.
     */
    @PutMapping("/global-schedule")
    public ResponseEntity<GlobalScheduleUpdateResponse> updateGlobalSchedule(
            @RequestHeader("X-Tenant-Id") String tenantId,
            @Valid @RequestBody GlobalScheduleUpdateRequest request) {
        return ResponseEntity.ok(
                ruleService.updateGlobalScheduleType(tenantId, request));
    }
}