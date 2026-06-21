package com.aidentx.iam.contracts.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MappingRuleDTO(
        UUID id,
        UUID sourceResourceId, // Where to get the data from (e.g., Active Directory)
        UUID sourceAttributeId, // Which attribute to get (e.g., the 'cn' attribute)
        String sourceAttributeName,
        String transform, // How to transform it (e.g., 'direct')
        String constantValue,
        String scriptLanguage,
        String scriptContent) {
}