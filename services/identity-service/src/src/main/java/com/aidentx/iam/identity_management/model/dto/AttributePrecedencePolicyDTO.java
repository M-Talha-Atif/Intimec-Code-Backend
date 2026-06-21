package com.aidentx.iam.identity_management.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AttributePrecedencePolicyDTO(
        int defaultPriority,
        List<AttributeRuleDTO> attributeOverrides) {
}