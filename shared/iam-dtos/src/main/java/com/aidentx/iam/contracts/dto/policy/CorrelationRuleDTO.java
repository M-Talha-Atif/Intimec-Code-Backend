package com.aidentx.iam.contracts.dto.policy;

import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CorrelationRuleDTO(
        UUID id, // Unique identifier for the correlation rule
        UUID resourceId, // The resource (application) this correlation rule belongs to
        String mode, // The correlation mode (e.g., "attribute", "rule")
        OnboardingRequestDTO.RuleGroupDTO ruleGroup, // The actual correlation rule structure
        OnboardingRequestDTO.ScriptDTO script, // The script for script-based correlation
        Integer priority, // 1-based evaluation order within the same resource (lower = earlier). Null falls back to array position.
        Boolean enabled // When false, the rule is skipped during correlation. Null is treated as true (backward-compatible).
) {
}