package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing Dynamic Entitlement Assignment Rules.
 *
 * <p>One rule per entitlement is enforced. Conditions are organized into
 * groups with per-group AND/OR operator and optional group-level negation.
 * Groups chain left-to-right.
 *
 * <p>Condition format (canonical grouped):
 * <pre>
 * conditionGroups = [
 *   {
 *     groupOperator: "AND",           // combines conditions inside + connects to next group
 *     negated: false,                 // if true, entire group result is NOTed
 *     conditions: [
 *       {field: "department", operator: "EQUALS", value: "Engineering"},
 *       {field: "jobTitle", operator: "STARTS_WITH", value: "Senior"}
 *     ]
 *   },
 *   {
 *     groupOperator: "OR",
 *     negated: true,
 *     conditions: [
 *       {field: "location", operator: "IN", values: ["NYC", "London"]}
 *     ]
 *   }
 * ]
 *
 * Evaluation:
 *   Group 1 = (department=Engineering AND jobTitle STARTS_WITH Senior)
 *   Group 2 = NOT(location IN [NYC, London])
 *   Result  = Group1 AND Group2
 *           = (dept=Eng AND title=Senior) AND NOT(loc IN [NYC, London])
 * </pre>
 *
 * <p>Legacy flat format (without groups) is still accepted and automatically
 * wrapped into a single group with {@code groupOperator=AND} and {@code negated=false}.
 *
 * <p>Schedule types:
 * <ul>
 *   <li>{@code IMMEDIATE} — auto-evaluated after every create/update via
 *       Spring application event (AFTER_COMMIT)</li>
 *   <li>{@code CRON} — evaluated on a schedule by the cron poller using
 *       {@code next_execution_at}</li>
 *   <li>{@code MANUAL} — never auto-evaluated. Only evaluated when an admin
 *       explicitly triggers it via the evaluate API endpoint</li>
 * </ul>
 */
public interface EntitlementAssignmentRuleService {

    /**
     * Creates a new assignment rule for an entitlement.
     *
     * <p>Rejects if a rule already exists for the entitlement (one rule per entitlement).
     *
     * <p>Accepts both grouped ({@code conditionGroups}) and legacy flat
     * ({@code conditions}) formats. If the legacy format is provided,
     * it is wrapped into a single group with {@code groupOperator=AND}
     * and {@code negated=false}.
     *
     * <p>After creation:
     * <ul>
     *   <li>IMMEDIATE rules: auto-evaluated via application event</li>
     *   <li>CRON rules: {@code next_execution_at} calculated</li>
     *   <li>MANUAL rules: saved only, no evaluation triggered</li>
     * </ul>
     *
     * @param tenantId the tenant identifier
     * @param request  the creation request with conditions, schedule, and identity type
     * @return the created rule response with both grouped and flattened conditions
     * @throws IllegalArgumentException if entitlement not found, rule already exists,
     *         conditions are empty, or validation fails
     */
    RuleResponse createRule(String tenantId, RuleCreateRequest request, String createdBy);

    /**
     * Updates an existing rule's conditions, schedule, enabled state, or identity type.
     *
     * <p>Only non-null fields in the request are applied. Null fields are left unchanged.
     *
     * <p>If conditions are provided (grouped or legacy format), they replace
     * all existing conditions. Legacy conditions are auto-migrated to grouped format.
     *
     * <p>After update:
     * <ul>
     *   <li>IMMEDIATE rules: auto-evaluated via application event</li>
     *   <li>CRON rules: {@code next_execution_at} recalculated</li>
     *   <li>MANUAL rules: saved only, no evaluation triggered</li>
     * </ul>
     *
     * @param tenantId the tenant identifier
     * @param ruleId   the rule UUID to update
     * @param request  the update request with fields to change
     * @return the updated rule response
     * @throws IllegalArgumentException if rule not found or validation fails
     */
    RuleResponse updateRule(String tenantId, UUID ruleId, RuleUpdateRequest request, String updatedBy);

    /**
     * Retrieves a rule by its ID.
     *
     * <p>The response includes both {@code conditionGroups} (canonical grouped format)
     * and {@code conditions} (flattened legacy format) for backward compatibility.
     *
     * @param tenantId the tenant identifier
     * @param ruleId   the rule UUID
     * @return the rule response
     * @throws IllegalArgumentException if rule not found
     */
    RuleResponse getRule(String tenantId, UUID ruleId);

    /**
     * Retrieves a rule by entitlement ID.
     *
     * <p>Since one rule per entitlement is enforced, this returns at most one result.
     *
     * @param tenantId      the tenant identifier
     * @param entitlementId the entitlement UUID
     * @return optional containing the rule response, or empty if no rule exists
     */
    Optional<RuleResponse> getRuleByEntitlement(String tenantId, UUID entitlementId);

        /**
     * Lists all rules for a tenant with pagination.
     *
     * @param tenantId the tenant identifier
     * @param page     zero-based page number
     * @param size     number of results per page
     * @param sort     optional sort field (e.g., "createdAt", "name", "priority")
     * @return page of rule responses
     */
    Page<RuleResponse> listRules(String tenantId, int page, int size, String sort);

    /**
     * Deletes a rule and all DYNAMIC assignments it created.
     *
     * @param tenantId the tenant identifier
     * @param ruleId   the rule UUID to delete
     * @throws IllegalArgumentException if rule not found
     */
    void deleteRule(String tenantId, UUID ruleId);

    /**
     * Manually triggers evaluation of a single rule.
     *
     * <p>Works for all schedule types (IMMEDIATE, CRON, MANUAL).
     * For MANUAL rules, this is the only way to trigger evaluation.
     *
     * <p>The evaluation pipeline:
     * <ol>
     *   <li>Queries identities matching the rule's grouped conditions</li>
     *   <li>Resolves shadow/account records for matched identities</li>
     *   <li>Creates new assignments for shadows that don't have them</li>
     *   <li>Revokes stale assignments for identities that no longer match</li>
     *   <li>Records execution history with per-group evidence</li>
     * </ol>
     *
     * @param tenantId the tenant identifier
     * @param ruleId   the rule UUID to evaluate
     * @return evaluation result with match/assign/revoke counts and duration
     * @throws IllegalArgumentException if rule not found
     */
    RuleEvaluationResult evaluateRule(String tenantId, UUID ruleId, String userName);

    /**
     * Manually triggers evaluation of all enabled rules for a tenant.
     *
     * <p>Evaluates all enabled rules regardless of schedule type.
     * Each rule is evaluated in its own transaction so that one failure
     * does not roll back other evaluations.
     *
     * @param tenantId the tenant identifier
     * @return list of evaluation results, one per rule (including failures)
     */
    List<RuleEvaluationResult> evaluateAllRules(String tenantId, String userName);

    /**
     * Updates the schedule type and cron configuration of all enabled rules
     * for a tenant in one operation.
     *
     * <p>Used to globally switch between schedule modes:
     * <ul>
     *   <li><b>IMMEDIATE:</b> clears cron fields, clears {@code next_execution_at},
     *       rules auto-evaluate on change</li>
     *   <li><b>CRON:</b> applies cron configuration to all enabled rules,
     *       recalculates {@code next_execution_at} for each</li>
     *   <li><b>MANUAL:</b> clears cron fields, clears {@code next_execution_at},
     *       rules only evaluate via explicit API call</li>
     * </ul>
     *
     * @param tenantId the tenant identifier
     * @param request  the global schedule update request with schedule type and optional cron config
     * @return response with the new schedule type and count of rules updated
     */
    GlobalScheduleUpdateResponse updateGlobalScheduleType(String tenantId, GlobalScheduleUpdateRequest request);

        /**
     * Returns the current global schedule config for a tenant.
     *
     * @param tenantId the tenant identifier
     * @return the global schedule config response
     */
    GlobalScheduleConfigResponse getGlobalScheduleConfig(String tenantId);
}