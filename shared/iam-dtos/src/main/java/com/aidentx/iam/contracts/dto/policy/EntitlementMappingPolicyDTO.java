package com.aidentx.iam.contracts.dto.policy;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/**
 * Policy DTO for entitlement attribute mappings.
 *
 * <p>Carries the per-resource mapping rules that control how connector-provided
 * attributes are mapped to canonical {@code Entitlement} governance fields
 * during inbound synchronization. Entitlement-equivalent of
 * {@link IdentityMappingPolicyDTO}.</p>
 *
 * <p>Target attribute names recognised by the {@code EntitlementMappingEngine}:</p>
 * <ul>
 *   <li>{@code displayName} – human-readable label</li>
 *   <li>{@code description} – free-text description</li>
 *   <li>{@code riskLevel} – Low / Medium / High / Critical</li>
 *   <li>{@code riskReason} – text explaining the rating</li>
 *   <li>{@code lifecycle} – Active / Deprecated / Orphaned / Inactive</li>
 *   <li>{@code classification} – multi-value: SOX, PII, GDPR, …</li>
 *   <li>{@code identityType} – Employee / NonEmployee / NonHuman</li>
 *   <li>{@code privilegeLevel} – Read / Write / Admin / Privileged</li>
 *   <li>{@code requestable} – true/false</li>
 *   <li>{@code requireApproval} – true/false</li>
 *   <li>{@code allowedAutomatedGrant} – true/false</li>
 *   <li>{@code ownerId} – UUID of the owning identity</li>
 *   <li>{@code maxDurationMs} – max assignment duration in ms</li>
 *   <li>{@code autoExpireDurationMs} – auto-expire duration in ms</li>
 * </ul>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntitlementMappingPolicyDTO(
        UUID resourceId,
        List<AttributeMappingDTO> attributeMappings) {

    /** Convenience constructor for empty policy. */
    public static EntitlementMappingPolicyDTO empty(UUID resourceId) {
        return new EntitlementMappingPolicyDTO(resourceId, List.of());
    }
}
