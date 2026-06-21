package com.aidentx.iam.contracts.dto.policy;

import java.util.List;

import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;

/**
 * DTO for Identity Type level policy configuration.
 * Contains configuration that applies to the entire identity type, not specific
 * resources.
 */
public record IdentityTypePolicyDTO(
    List<OnboardingRequestDTO.AttributeSourcePriorityDTO> sourcePriorities) {
}
