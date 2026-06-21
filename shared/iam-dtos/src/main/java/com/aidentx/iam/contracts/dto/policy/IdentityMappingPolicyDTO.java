package com.aidentx.iam.contracts.dto.policy;

import java.util.List;
import java.util.UUID;

/**
 * IAM Policy DTO containing governance data for identity reconciliation.
 * This DTO is used by the IAM service to fetch governance data from the
 * core-service
 * for identity reconciliation and correlation operations.
 */
public record IdentityMappingPolicyDTO(
    UUID resourceId,
    List<CorrelationRuleDTO> correlationRules,
    List<AttributeMappingDTO> attributeMappings,
    List<SynchronizationReactionDTO> synchronizationReactions) {

    /**
     * Backward-compatible constructor without reactions (defaults to null).
     */
    public IdentityMappingPolicyDTO(UUID resourceId, List<CorrelationRuleDTO> correlationRules,
                                     List<AttributeMappingDTO> attributeMappings) {
        this(resourceId, correlationRules, attributeMappings, null);
    }
}