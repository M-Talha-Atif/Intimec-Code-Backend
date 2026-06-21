package com.aidentx.iam.policy.service;

import java.util.List;
import java.util.UUID;

import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingGroupDTO;
import com.aidentx.iam.contracts.dto.policy.CorrelationRuleDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityMappingPolicyDTO;
import com.aidentx.iam.contracts.dto.policy.SynchronizationReactionDTO;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.policy.model.entity.IdentityGovernancePolicy;

/**
 * Service interface for resource-scoped governance policy management.
 * Handles governance data operations including correlation rules and attribute
 * mappings.
 */
public interface ResourceGovernancePolicyService {

  /**
   * Retrieves the complete identity mapping policy for a tenant, type and
   * resourceId.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type of the schema
   * @return The complete identity mapping policy
   */
  IdentityMappingPolicyDTO getPolicy(String tenantId, IdentityType identityType, UUID resourceId);

  List<IdentityMappingPolicyDTO> getAllPoliciesForIdentityType(String tenantId,
      IdentityType identityType);

  /**
   * Updates the complete identity mapping policy for a tenant and schema.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type of the schema
   * @param resourceId   The resource UUID
   * @param schemaId     The schema UUID (nullable)
   * @param policyDto    The updated policy data
   * @return The updated identity mapping policy
   */
  IdentityMappingPolicyDTO updatePolicy(String tenantId, IdentityType identityType, UUID resourceId,
      UUID schemaId, IdentityMappingPolicyDTO policyDto);

  /**
   * Deletes all policies associated with a resource (application).
   *
   * @param tenantId   The tenant identifier
   * @param resourceId The resource UUID
   * @return The number of policies deleted
   */
  int deletePoliciesByResource(String tenantId, UUID resourceId);

  /**
   * Retrieves all attribute mappings for a tenant and identityType.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type
   * @return List of attribute mappings
   */
  List<AttributeMappingDTO> getAttributeMappings(String tenantId, IdentityType identityType);

  /**
   * Retrieves all correlation rules for a tenant and schema.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type of the schema
   * @return List of correlation rules
   */
  List<CorrelationRuleDTO> getCorrelationRules(String tenantId, IdentityType identityType);

  /**
   * Updates all correlation rules for a tenant and schema.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type of the schema
   * @param ruleDTOs     The updated correlation rules
   * @return The updated list of correlation rules
   */
  List<CorrelationRuleDTO> updateCorrelationRules(String tenantId, IdentityType identityType, UUID resourceId,
      List<CorrelationRuleDTO> ruleDTOs);

  /**
   * Deletes all governance policies for a tenant and schema.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type of the schema
   */
  void deletePolicies(String tenantId, IdentityType identityType);

  /**
   * Deletes all correlation rules for a tenant and schema.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type of the schema
   */
  void deleteCorrelationRules(String tenantId, IdentityType identityType);

  /**
   * Deletes all attribute mappings for a tenant and schema.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type of the schema
   */
  void deleteAttributeMappings(String tenantId, IdentityType identityType);

  /**
   * Deletes a specific correlation rule.
   */
  void deleteCorrelationRule(String tenantId, IdentityType identityType, UUID resourceId, UUID ruleId);

  /**
   * Deletes a specific attribute mapping.
   */
  void deleteAttributeMapping(String tenantId, IdentityType identityType, UUID resourceId, UUID mappingId);

  /**
   * Validates a policy DTO.
   */
  void validatePolicy(String tenantId, IdentityType identityType, IdentityMappingPolicyDTO policyDto);

  /**
   * Validates attribute mappings.
   */
  void validateAttributeMappings(String tenantId, IdentityType identityType,
      List<AttributeMappingDTO> mappingDTOs);

  /**
   * Validates correlation rules.
   */
  void validateCorrelationRules(String tenantId, IdentityType identityType,
      List<CorrelationRuleDTO> ruleDTOs);

  /**
   * Updates a specific correlation rule.
   */
  CorrelationRuleDTO updateCorrelationRule(String tenantId, IdentityType identityType, UUID resourceId,
      UUID ruleId, CorrelationRuleDTO ruleDto);

  /**
   * Updates a specific attribute mapping.
   */
  AttributeMappingDTO updateAttributeMapping(String tenantId, IdentityType identityType, UUID mappingId,
      AttributeMappingDTO mappingDto);

  /**
   * Validates a single correlation rule.
   */
  void validateCorrelationRule(String tenantId, IdentityType identityType, CorrelationRuleDTO ruleDto);

  /**
   * Validates a single attribute mapping.
   */
  void validateAttributeMapping(String tenantId, IdentityType identityType, AttributeMappingDTO mappingDto);

  /**
   * Retrieves grouped attribute mappings.
   */
  List<AttributeMappingGroupDTO> getAttributeMappingGroups(String tenantId, IdentityType identityType);

  /**
   * Gets or creates a policy entity.
   */
  IdentityGovernancePolicy getOrCreatePolicy(String tenantId, IdentityType identityType, UUID resourceId);

  /**
   * Retrieves synchronization reactions for a specific resource.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type
   * @param resourceId   The resource UUID
   * @return List of configured synchronization reactions, or empty list if none
   */
  List<SynchronizationReactionDTO> getSynchronizationReactions(String tenantId, IdentityType identityType,
      UUID resourceId);

  /**
   * Updates the synchronization reactions for a specific resource.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type
   * @param resourceId   The resource UUID
   * @param reactions    The new synchronization reactions
   * @return The updated list of reactions
   */
  List<SynchronizationReactionDTO> updateSynchronizationReactions(String tenantId, IdentityType identityType,
      UUID resourceId, List<SynchronizationReactionDTO> reactions);
}
