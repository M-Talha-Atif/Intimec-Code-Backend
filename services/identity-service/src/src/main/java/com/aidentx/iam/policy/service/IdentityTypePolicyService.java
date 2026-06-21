package com.aidentx.iam.policy.service;

import com.aidentx.iam.contracts.dto.policy.IdentityTypePolicyDTO;
import com.aidentx.iam.identity_management.model.enums.IdentityType;

/**
 * Service interface for identity type level policy management.
 * Handles global policies such as source priorities.
 */
public interface IdentityTypePolicyService {

  /**
   * Retrieves the identity type level policy (e.g. source priorities).
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type
   * @return The identity type policy
   */
  IdentityTypePolicyDTO getIdentityTypePolicy(String tenantId, IdentityType identityType);

  /**
   * Updates the identity type level policy.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type
   * @param policyDto    The updated policy data
   * @return The updated identity type policy
   */
  IdentityTypePolicyDTO updateIdentityTypePolicy(String tenantId, IdentityType identityType,
      IdentityTypePolicyDTO policyDto);
}
