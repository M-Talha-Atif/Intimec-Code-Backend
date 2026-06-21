package com.aidentx.iam.contracts.dto.policy;

import com.aidentx.iam.contracts.dto.OnboardingRequestDTO.ScriptDTO;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AttributeMappingDTO(
        UUID id,
        UUID sourceResourceId,
        String sourceAttributeName,
        String targetAttributeName,
        String mode,
        ScriptDTO script
) {
}
