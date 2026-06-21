package com.aidentx.iam.policy.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.CorrelationRuleDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityMappingPolicyDTO;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

@Component
public class GovernancePolicyValidator {

  private final GovernancePolicyHelper helper;
  private final ApplicationSchemaService applicationSchemaService;
  private final IUnifiedLogger logger;

  public GovernancePolicyValidator(GovernancePolicyHelper helper,
      ApplicationSchemaService applicationSchemaService,
      UnifiedLoggerFactory loggerFactory) {
    this.helper = helper;
    this.applicationSchemaService = applicationSchemaService;
    this.logger = loggerFactory.getLogger(GovernancePolicyValidator.class);
  }

  public void validatePolicy(String tenantId, IdentityType identityType, IdentityMappingPolicyDTO policyDto) {
    // Validate input parameters
    if (tenantId == null || tenantId.trim().isEmpty()) {
      throw new IllegalArgumentException("Tenant ID cannot be null or empty");
    }
    if (identityType == null) {
      throw new IllegalArgumentException("Schema ID cannot be null");
    }
    if (policyDto == null) {
      throw new IllegalArgumentException("Policy DTO cannot be null");
    }

    // Validate that the schema is associated with at least one application
    validateSchemaExists(tenantId, identityType);

    logger.debug().message("Validating identity mapping policy")
        .detail("tenantId", tenantId)
        .detail("identityType", identityType)
        .log();

    // Validate all components of the policy
    validateAttributeMappings(tenantId, identityType, policyDto.attributeMappings());
    validateCorrelationRules(tenantId, identityType, policyDto.correlationRules());
  }

  public void validateSchemaExists(String tenantId, IdentityType identityType) {
    logger.debug().message("Validating schema exists")
        .detail("tenantId", tenantId)
        .detail("identityType", identityType)
        .log();

    // This will throw an exception if the schema doesn't exist
    helper.getSchemaForIdentityType(tenantId, identityType);

    logger.debug().message("Schema validation successful")
        .detail("tenantId", tenantId)
        .detail("identityType", identityType)
        .log();
  }

  public void validateAttributeMappings(String tenantId, IdentityType identityType,
      List<AttributeMappingDTO> mappingDTOs) {
    if (mappingDTOs == null || mappingDTOs.isEmpty()) {
      return; // Empty list is valid
    }

    // Validate identityType
    if (identityType == null) {
      throw new IllegalArgumentException("Identity type cannot be null for attribute mappings");
    }

    logger.debug().message("Validating attribute mappings")
        .detail("tenantId", tenantId)
        .detail("identityType", identityType)
        .detail("mappingCount", mappingDTOs.size())
        .log();

    // Perform basic structural validation
    for (int i = 0; i < mappingDTOs.size(); i++) {
      AttributeMappingDTO mapping = mappingDTOs.get(i);

      if (mapping.sourceResourceId() == null) {
        throw new IllegalArgumentException(
            "Source resource ID cannot be null in attribute mapping at index " + i);
      }

      if (mapping.sourceAttributeName() == null || mapping.sourceAttributeName().trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Source attribute name cannot be null or empty in attribute mapping at index " + i);
      }

      if (mapping.mode() == null || mapping.mode().trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Mode cannot be null or empty in attribute mapping at index " + i);
      }

      if (mapping.targetAttributeName() == null || mapping.targetAttributeName().trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Target attribute name cannot be null or empty in attribute mapping at index " + i);
      }
    }

    // Validate against schema definition for field IDs and names
    helper.validateSchemaFields(tenantId, identityType, mappingDTOs);
  }

  public void validateCorrelationRules(String tenantId, IdentityType identityType,
      List<CorrelationRuleDTO> ruleDTOs) {
    if (ruleDTOs == null || ruleDTOs.isEmpty()) {
      return; // Empty list is valid
    }

    // Validate identityType
    if (identityType == null) {
      throw new IllegalArgumentException("Identity type cannot be null for correlation rules");
    }

    logger.debug().message("Validating correlation rules")
        .detail("tenantId", tenantId)
        .detail("identityType", identityType)
        .detail("ruleCount", ruleDTOs.size())
        .log();

    // Get internal schema field names for validation (target/value side)
    Set<String> internalFieldNames = helper.getSchemaFieldNames(tenantId, identityType);

    // Cache for source schema field names (per resourceId)
    Map<UUID, Set<String>> sourceSchemaCache = new HashMap<>();

    // Validate rule structure
    for (int i = 0; i < ruleDTOs.size(); i++) {
      CorrelationRuleDTO rule = ruleDTOs.get(i);
      if (rule.mode() == null || rule.mode().trim().isEmpty()) {
        throw new IllegalArgumentException("Correlation mode cannot be null or empty in rule at index " + i);
      }
      if (rule.resourceId() == null) {
        throw new IllegalArgumentException("Resource ID cannot be null in correlation rule at index " + i);
      }
      if (rule.ruleGroup() == null) {
        throw new IllegalArgumentException("Rule group cannot be null in correlation rule at index " + i);
      }
      if (rule.ruleGroup().combinator() == null || rule.ruleGroup().combinator().trim().isEmpty()) {
        throw new IllegalArgumentException(
            "Rule group combinator cannot be null or empty in correlation rule at index " + i);
      }
      if (rule.ruleGroup().rules() == null || rule.ruleGroup().rules().isEmpty()) {
        throw new IllegalArgumentException(
            "Rule group must contain at least one rule in correlation rule at index " + i);
      }

      // Validate resource exists
      UUID resourceId = rule.resourceId();
      try {
        applicationSchemaService.validateResourceExists(tenantId, resourceId);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Resource ID '" + resourceId + "' does not exist at correlation rule index " + i
                + ": " + e.getMessage());
      }

      // Get source schema for this rule (cached)
      Set<String> sourceFieldNames = sourceSchemaCache.computeIfAbsent(resourceId,
          resId -> {
            try {
              return applicationSchemaService.getApplicationSchemaFieldNames(tenantId, resId);
            } catch (Exception e) {
              throw new IllegalArgumentException(
                  "Failed to fetch source schema for resource '" + resId
                      + "': " + e.getMessage(),
                  e);
            }
          });

      // Validate individual rules in the rule group
      for (int j = 0; j < rule.ruleGroup().rules().size(); j++) {
        var subRule = rule.ruleGroup().rules().get(j);

        // Check if this is a leaf rule (has field/operator/value) or a group rule (has
        // combinator/rules)
        boolean isLeafRule = subRule.field() != null && !subRule.field().trim().isEmpty();
        boolean isGroupRule = subRule.combinator() != null && !subRule.combinator().trim().isEmpty();

        if (isLeafRule) {
          // Validate leaf rule fields
          if (subRule.operator() == null || subRule.operator().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Rule operator cannot be null or empty in correlation rule at index " + i
                    + ", sub-rule index " + j);
          }
          if (subRule.value() == null || subRule.value().trim().isEmpty()) {
            throw new IllegalArgumentException(
                "Rule value cannot be null or empty in correlation rule at index " + i
                    + ", sub-rule index " + j);
          }

          // Validate the 'field' attribute exists in SOURCE schema
          if (subRule.field() != null && !subRule.field().trim().isEmpty()) {
            if (!sourceFieldNames.contains(subRule.field())) {
              throw new IllegalArgumentException("Field '" + subRule.field()
                  + "' does not exist in source application schema for resource '" + resourceId
                  + "' in correlation rule at index " + i + ", sub-rule index " + j);
            }
          }

          // If valueSource is "field", validate that the value references a valid field
          // in the internal schema
          if ("field".equals(subRule.valueSource())) {
            if (!internalFieldNames.contains(subRule.value())) {
              throw new IllegalArgumentException("Field '" + subRule.value()
                  + "' does not exist in internal schema for identity type '" + identityType
                  + "' in correlation rule at index " + i + ", sub-rule index " + j);
            }
          }
        } else if (isGroupRule) {
          // Validate group rule fields
          if (subRule.rules() == null || subRule.rules().isEmpty()) {
            throw new IllegalArgumentException(
                "Group rule must contain at least one sub-rule in correlation rule at index " + i
                    + ", sub-rule index " + j);
          }
          // Recursively validate nested rules
          helper.validateCorrelationRuleFields(subRule.rules(), sourceFieldNames, internalFieldNames,
              identityType, resourceId, i, j + "-nested");
        } else {
          throw new IllegalArgumentException(
              "Rule must be either a leaf rule (with field/operator/value) or a group rule (with combinator/rules) in correlation rule at index "
                  + i + ", sub-rule index " + j);
        }
      }
    }
  }

  public void validateCorrelationRule(String tenantId, IdentityType identityType, CorrelationRuleDTO ruleDto) {
    validateCorrelationRules(tenantId, identityType, List.of(ruleDto));
  }

  public void validateAttributeMapping(String tenantId, IdentityType identityType, AttributeMappingDTO mappingDto) {
    validateAttributeMappings(tenantId, identityType, List.of(mappingDto));
  }
}
