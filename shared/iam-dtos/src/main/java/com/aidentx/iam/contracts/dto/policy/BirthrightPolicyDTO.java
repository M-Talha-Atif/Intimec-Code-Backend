package com.aidentx.iam.contracts.dto.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO representing the BIRTHRIGHT_POLICY artifact for a tenant.
 * It contains a list of rules that grant access to resources based on identity attributes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BirthrightPolicyDTO(
    List<BirthrightRuleDTO> rules
) {}