package com.aidentx.iam.entitlement_management.policy.service;

import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.EntitlementMappingPolicyDTO;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for managing entitlement governance policies.
 *
 * <p>Entitlement governance policies store the inbound attribute mapping rules
 * for each application (resource), controlling how connector attributes are
 * projected onto canonical {@code Entitlement} governance fields at sync time.</p>
 */
public interface EntitlementGovernancePolicyService {

    /**
     * Returns the governance policy (attribute mappings) for the given resource.
     * Returns an empty policy if none is configured.
     *
     * @param tenantId   tenant context
     * @param resourceId application/resource UUID
     * @return the policy DTO, never {@code null}
     */
    EntitlementMappingPolicyDTO getPolicy(String tenantId, UUID resourceId);

    /**
     * Creates or fully replaces the governance policy for the given resource.
     *
     * @param tenantId   tenant context
     * @param resourceId application/resource UUID
     * @param policyDto  the desired policy state
     * @return the persisted policy DTO
     */
    EntitlementMappingPolicyDTO updatePolicy(String tenantId, UUID resourceId,
            EntitlementMappingPolicyDTO policyDto);

    /**
     * Returns the raw attribute mapping rules for in-engine consumption.
     *
     * @param tenantId   tenant context
     * @param resourceId application/resource UUID
     * @return list of mapping rules, never {@code null}
     */
    List<AttributeMappingDTO> getAttributeMappings(String tenantId, UUID resourceId);

    /**
     * Deletes all entitlement governance policies associated with the resource.
     * Called as part of application deletion cascade.
     *
     * @param tenantId   tenant context
     * @param resourceId application/resource UUID
     * @return number of rows deleted
     */
    int deletePoliciesByResource(String tenantId, UUID resourceId);
}
