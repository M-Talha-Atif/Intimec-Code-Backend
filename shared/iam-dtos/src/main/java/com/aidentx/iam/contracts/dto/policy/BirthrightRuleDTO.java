package com.aidentx.iam.contracts.dto.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/**
 * DTO representing a single birthright rule.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BirthrightRuleDTO(
    String name,
    int priority,
    Map<String, Object> conditions,
    List<String> grants
) {}