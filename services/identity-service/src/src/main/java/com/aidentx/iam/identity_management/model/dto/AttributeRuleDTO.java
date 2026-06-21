package com.aidentx.iam.identity_management.model.dto;

import com.aidentx.iam.contracts.dto.policy.SourcePriorityDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AttributeRuleDTO(
    String attribute,
    List<SourcePriorityDTO> sources
) {}