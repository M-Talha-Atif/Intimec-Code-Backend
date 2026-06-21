package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.identity_management.model.entity.Identity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Evaluates access-role assignment rules against identity records.
 *
 * <p>This service is intentionally focused on rule matching only. Assignment
 * persistence, revocation, and execution-history writing are handled by the
 * access-role assignment execution and identity-role assignment services.</p>
 */
public interface AccessRoleAssignmentRuleEvaluationService {

    /**
     * Evaluates a persisted access-role assignment rule against one identity.
     *
     * @param tenantId tenant boundary for both the rule and identity
     * @param rule access-role assignment rule to evaluate
     * @param identity identity candidate
     * @return match result with structured evidence
     */
    EvaluationResult evaluateIdentity(String tenantId, AccessRoleAssignmentRule rule, Identity identity);

    /**
     * Returns {@code true} when the identity satisfies the rule and is included
     * in the rule's configured identity types.
     */
    boolean matches(String tenantId, AccessRoleAssignmentRule rule, Identity identity);

    /**
     * Evaluates canonical grouped condition JSON against an identity.
     *
     * @param conditionGroups canonical access-role rule condition groups
     * @param identity identity candidate
     * @return condition-group evaluation result with evidence
     */
    ConditionEvaluationResult evaluateConditionGroups(List<Map<String, Object>> conditionGroups, Identity identity);

    /**
     * Builds structured match evidence without changing assignment state.
     */
    Map<String, Object> buildEvidence(AccessRoleAssignmentRule rule, Identity identity, boolean matched, String errorMessage);

    /** Rule-vs-identity evaluation result. */
    record EvaluationResult(UUID ruleId,
                            UUID roleId,
                            UUID identityId,
                            String identityType,
                            boolean matched,
                            Map<String, Object> evidence,
                            String errorMessage) {
    }

    /** Condition-group evaluation result. */
    record ConditionEvaluationResult(boolean matched, List<Map<String, Object>> groupEvidence) {
    }
}

