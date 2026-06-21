package com.aidentx.iam.contracts.dto.policy;

import com.aidentx.iam.contracts.dto.OnboardingRequestDTO.ScriptDTO;
import java.util.UUID;

public record AttributeMappingRuleDTO(
    UUID id,
    UUID sourceResourceId,
    String sourceAttributeName,
    String mode,
    ScriptDTO script) {
}
