package com.aidentx.iam.contracts.dto.policy;

import java.util.*;

public record AttributeMappingGroupDTO(
    String targetAttributeName, // in our system
    String identityType,
    List<AttributeMappingRuleDTO> mappings) {
}