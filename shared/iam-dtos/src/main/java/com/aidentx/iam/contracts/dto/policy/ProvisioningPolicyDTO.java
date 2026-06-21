package com.aidentx.iam.contracts.dto.policy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents the complete PROVISIONING_POLICY artifact for a resource.
 * It contains separate configurations for each distinct lifecycle action,
 * mirroring the options available in the UI.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProvisioningPolicyDTO(
    LifecycleActionPolicy create,
    LifecycleActionPolicy update,
    LifecycleActionPolicy delete,
    LifecycleActionPolicy suspend,
    LifecycleActionPolicy unlock
) {}