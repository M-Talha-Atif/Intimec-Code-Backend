package com.aidentx.iam.contracts.dto.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Represents the configuration for a single provisioning action (e.g., create,
 * update).
 * It includes a flag to enable/disable the action and a list of attribute
 * mappings.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LifecycleActionPolicy(
                boolean enabled,
                List<MappingRuleDTO> attributeMappings) {
}