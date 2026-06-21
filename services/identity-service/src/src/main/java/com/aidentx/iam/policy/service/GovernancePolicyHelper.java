package com.aidentx.iam.policy.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.CorrelationRuleDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityMappingPolicyDTO;
import com.aidentx.iam.contracts.dto.policy.SynchronizationReactionDTO;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.model.enums.SchemaLifecycleState;
import com.aidentx.iam.policy.model.entity.IdentityGovernancePolicy;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Helper class for governance policy operations.
 * Provides utility methods for schema operations, JSON deserialization, ID
 * backfilling, and policy operations.
 */
@Component
public class GovernancePolicyHelper {

  private final com.aidentx.iam.identity_management.service.IdentitySchemaService identitySchemaService;
  private final ApplicationSchemaService applicationSchemaService;
  private final ObjectMapper objectMapper;
  private final IUnifiedLogger logger;

  public GovernancePolicyHelper(
      com.aidentx.iam.identity_management.service.IdentitySchemaService identitySchemaService,
      ApplicationSchemaService applicationSchemaService,
      ObjectMapper objectMapper,
      UnifiedLoggerFactory loggerFactory) {
    this.identitySchemaService = identitySchemaService;
    this.applicationSchemaService = applicationSchemaService;
    this.objectMapper = objectMapper;
    this.logger = loggerFactory.getLogger(GovernancePolicyHelper.class);
  }

  // ============================================================================
  // Schema Operations
  // ============================================================================

  /**
   * Retrieves the schema for the given identity type.
   */
  public com.aidentx.iam.identity_management.model.dto.IdentitySchemaDtos.IdentitySchemaResponseDto getSchemaForIdentityType(
      String tenantId, IdentityType identityType) {
    try {
      return switch (identityType) {
        case EMPLOYEE -> identitySchemaService.getEmployeeIdentitySchema(tenantId);
        case NON_EMPLOYEE -> identitySchemaService.getNonEmployeeIdentitySchema(tenantId);
        case NON_HUMAN -> identitySchemaService.getNonHumanIdentitySchema(tenantId);
        default -> throw new IllegalArgumentException("Unknown identity type: " + identityType);
      };
    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Failed to retrieve schema for identity type '" + identityType + "': " + e.getMessage(), e);
    }
  }

  /**
   * Creates a set of field names from the schema for validation.
   */
  public Set<String> getSchemaFieldNames(String tenantId, IdentityType identityType) {
    var schemaResponse = getSchemaForIdentityType(tenantId, identityType);
    Set<String> fieldNames = new HashSet<>();
    schemaResponse.coreAttributes().forEach(attr -> fieldNames.add(attr.systemKey()));
    schemaResponse.extendedAttributes().forEach(attr -> {
      SchemaLifecycleState lifecycleState = attr.lifecycleState() != null
          ? attr.lifecycleState()
          : SchemaLifecycleState.ACTIVE;

      if (lifecycleState == SchemaLifecycleState.ACTIVE
          || lifecycleState == SchemaLifecycleState.DEPRECATED) {
        fieldNames.add(attr.systemKey());
      }
    });
    return fieldNames;
  }

  // ============================================================================
  // JSON Deserialization
  // ============================================================================

  /**
   * Safely deserializes a JSONB field to a list of DTOs.
   */
  public <T> List<T> deserializeJsonList(JsonNode jsonNode, TypeReference<List<T>> typeRef, String fieldName) {
    if (jsonNode == null) {
      logger.info().message("JSONB field is null during deserialization")
          .detail("fieldName", fieldName)
          .log();
      return List.of();
    }
    try {
      List<T> result = objectMapper.convertValue(jsonNode, typeRef);
      logger.info().message("Successfully deserialized JSONB field")
          .detail("fieldName", fieldName)
          .detail("resultSize", result != null ? result.size() : 0)
          .log();
      return result;
    } catch (Exception e) {
      logger.error(e).message("Failed to deserialize " + fieldName)
          .detail("error", e.getMessage())
          .detail("jsonNodeType", jsonNode.getNodeType().name())
          .detail("jsonNodeContent", jsonNode.toString().substring(0, Math.min(500, jsonNode.toString().length())))
          .log();
      return List.of();
    }
  }

  // ============================================================================
  // Policy Operations
  // ============================================================================

  /**
   * Retrieves and deserializes attribute mappings from a policy, with error
   * handling.
   */
  public List<AttributeMappingDTO> getAttributeMappingsFromPolicy(IdentityGovernancePolicy policy,
      String tenantId, IdentityType identityType) {
    if (policy.getAttributeMappings() == null) {
      return List.of();
    }

    try {
      return objectMapper.convertValue(
          policy.getAttributeMappings(),
          new TypeReference<List<AttributeMappingDTO>>() {
          });
    } catch (Exception e) {
      logger.error(e).message("Failed to deserialize attribute mappings")
          .detail("policyId", policy.getId())
          .detail("tenantId", tenantId)
          .detail("identityType", identityType)
          .log();
      throw new IllegalArgumentException("Failed to retrieve attribute mappings: " + e.getMessage());
    }
  }

  /**
   * Retrieves and deserializes correlation rules from a policy, with error
   * handling.
   */
  public List<CorrelationRuleDTO> getCorrelationRulesFromPolicy(IdentityGovernancePolicy policy,
      String tenantId, IdentityType identityType) {
    if (policy.getCorrelationRules() == null) {
      return List.of();
    }

    try {
      List<CorrelationRuleDTO> rules = objectMapper.convertValue(
          policy.getCorrelationRules(),
          new TypeReference<List<CorrelationRuleDTO>>() {
          });
      // Backfill priority/enabled on legacy rows that pre-date the fields so the
      // frontend always receives a stable, deterministic ordering.
      List<CorrelationRuleDTO> normalized = new java.util.ArrayList<>(rules.size());
      int seq = 1;
      for (CorrelationRuleDTO rule : rules) {
        Integer priority = rule.priority() != null ? rule.priority() : seq;
        Boolean enabled = rule.enabled() != null ? rule.enabled() : Boolean.TRUE;
        normalized.add(new CorrelationRuleDTO(
            rule.id(), rule.resourceId(), rule.mode(), rule.ruleGroup(), rule.script(),
            priority, enabled));
        seq++;
      }
      return normalized;
    } catch (Exception e) {
      logger.error(e).message("Failed to deserialize correlation rules")
          .detail("policyId", policy.getId())
          .detail("tenantId", tenantId)
          .detail("identityType", identityType)
          .log();
      throw new IllegalArgumentException("Failed to retrieve correlation rules: " + e.getMessage());
    }
  }

  // ============================================================================
  // Entity/DTO Conversion Methods
  // ============================================================================

  /**
   * Converts an entity to a DTO with ID backfilling and persistence if needed.
   */
  public IdentityMappingPolicyDTO convertEntityToDto(IdentityGovernancePolicy policy,
      com.aidentx.iam.policy.repository.IdentityGovernancePolicyRepository policyRepository) {
    try {
      // Deserialize and backfill IDs
      List<CorrelationRuleDTO> correlationRules = deserializeJsonList(
          policy.getCorrelationRules(),
          new TypeReference<List<CorrelationRuleDTO>>() {
          },
          "correlation rules");

      List<AttributeMappingDTO> attributeMappings = deserializeJsonList(
          policy.getAttributeMappings(),
          new TypeReference<List<AttributeMappingDTO>>() {
          },
          "attribute mappings");

      // Convert source priorities and lifecycle policies

      List<SynchronizationReactionDTO> syncReactions = deserializeJsonList(
          policy.getSynchronizationReactions(),
          new TypeReference<List<SynchronizationReactionDTO>>() {
          },
          "synchronization reactions");

      return new IdentityMappingPolicyDTO(policy.getResourceId(),
          correlationRules, attributeMappings, syncReactions);
    } catch (Exception e) {
      logger.error(e).message("Failed to convert entity to DTO")
          .detail("policyId", policy.getId())
          .log();
      return new IdentityMappingPolicyDTO(policy.getResourceId(), List.of(), List.of(), List.of());
    }
  }

  /**
   * Updates an entity from a DTO.
   */
  public void updateEntityFromDto(IdentityGovernancePolicy policy, IdentityMappingPolicyDTO policyDto) {
    try {
      if (policyDto.correlationRules() != null) {
        policy.setCorrelationRules(objectMapper.valueToTree(policyDto.correlationRules()));
      }
      if (policyDto.attributeMappings() != null) {
        // Assign stable UUIDs to any attribute mapping that lacks one so that
        // individual DELETE calls on source-mapping rows can reliably find them by ID.
        java.util.List<AttributeMappingDTO> normalized = policyDto.attributeMappings().stream()
            .map(m -> m.id() != null ? m
                : new AttributeMappingDTO(java.util.UUID.randomUUID(), m.sourceResourceId(),
                    m.sourceAttributeName(), m.targetAttributeName(), m.mode(), m.script()))
            .collect(java.util.stream.Collectors.toList());
        policy.setAttributeMappings(objectMapper.valueToTree(normalized));
      }
    } catch (Exception e) {
      logger.error(e).message("Failed to update entity from DTO")
          .detail("policyId", policy.getId())
          .log();
    }
  }

  // ============================================================================
  // Validation Methods
  // ============================================================================

  /**
   * Validates that the identity type is valid and not null.
   */
  public void validateIdentityType(IdentityType identityType) {
    if (identityType == null) {
      throw new IllegalArgumentException("Identity type must be specified");
    }
  }

  /**
   * Validates schema fields for attribute mappings.
   * Validates both source fields (against application schema) and target fields
   * (against internal schema).
   */
  public void validateSchemaFields(String tenantId, IdentityType identityType,
      List<AttributeMappingDTO> mappings) {
    // Cache for source schemas to avoid repeated gRPC calls
    Map<UUID, Set<String>> sourceSchemaCache = new HashMap<>();

    // Get internal schema field names
    Set<String> internalFieldNames = getSchemaFieldNames(tenantId, identityType);

    // Validate each mapping
    for (AttributeMappingDTO mapping : mappings) {
      // Validate sourceResourceId is not null
      if (mapping.sourceResourceId() == null) {
        throw new IllegalArgumentException(
            "Attribute mapping with ID '" + mapping.id() + "' has null sourceResourceId");
      }

      // Validate sourceAttributeName is not null
      if (mapping.sourceAttributeName() == null || mapping.sourceAttributeName().isBlank()) {
        throw new IllegalArgumentException(
            "Attribute mapping with ID '" + mapping.id() + "' has null or blank sourceAttributeName");
      }

      // Validate targetAttributeName is not null
      if (mapping.targetAttributeName() == null || mapping.targetAttributeName().isBlank()) {
        throw new IllegalArgumentException(
            "Attribute mapping with ID '" + mapping.id() + "' has null or blank targetAttributeName");
      }

      // Validate source field against application schema
      Set<String> sourceFieldNames = sourceSchemaCache.computeIfAbsent(
          mapping.sourceResourceId(),
          resourceId -> {
            try {
              return applicationSchemaService.getApplicationSchemaFieldNames(tenantId, resourceId);
            } catch (Exception e) {
              logger.error(e)
                  .message("Failed to fetch application schema for resource")
                  .detail("tenantId", tenantId)
                  .detail("resourceId", resourceId)
                  .detail("error", e.getMessage())
                  .log();
              throw new IllegalArgumentException(
                  "Failed to validate source fields for resource '" + resourceId +
                      "': " + e.getMessage(),
                  e);
            }
          });

      if (!sourceFieldNames.contains(mapping.sourceAttributeName())) {
        throw new IllegalArgumentException(
            "Source attribute '" + mapping.sourceAttributeName() +
                "' in mapping with ID '" + mapping.id() +
                "' does not exist in application schema for resource '" +
                mapping.sourceResourceId() + "'. Available fields: " + sourceFieldNames);
      }

      // Validate target field against internal schema
      if (!internalFieldNames.contains(mapping.targetAttributeName())) {
        throw new IllegalArgumentException(
            "Target attribute '" + mapping.targetAttributeName() +
                "' in mapping with ID '" + mapping.id() +
                "' does not exist in internal schema for identity type '" +
                identityType + "'. Available fields: " + internalFieldNames);
      }
    }
  }

  /**
   * Helper method to recursively validate correlation rule fields against both
   * source and internal schemas.
   */
  public void validateCorrelationRuleFields(List<OnboardingRequestDTO.RuleDTO> rules,
      Set<String> sourceFieldNames, Set<String> internalFieldNames,
      IdentityType identityType, UUID resourceId, int ruleIndex, String subRulePath) {
    for (int k = 0; k < rules.size(); k++) {
      var nestedRule = rules.get(k);
      String currentPath = subRulePath + "-" + k;

      // Check if this is a leaf rule (has field/operator/value) or a group rule (has
      // combinator/rules)
      boolean isLeafRule = nestedRule.field() != null && !nestedRule.field().trim().isEmpty();
      boolean isGroupRule = nestedRule.combinator() != null && !nestedRule.combinator().trim().isEmpty();

      if (isLeafRule) {
        // Validate leaf rule fields
        if (nestedRule.operator() == null || nestedRule.operator().trim().isEmpty()) {
          throw new IllegalArgumentException(
              "Rule operator cannot be null or empty in correlation rule at index " + ruleIndex
                  + ", sub-rule path " + currentPath);
        }
        if (nestedRule.value() == null || nestedRule.value().trim().isEmpty()) {
          throw new IllegalArgumentException(
              "Rule value cannot be null or empty in correlation rule at index " + ruleIndex
                  + ", sub-rule path " + currentPath);
        }

        // Validate the 'field' attribute exists in SOURCE schema
        if (nestedRule.field() != null && !nestedRule.field().trim().isEmpty()) {
          if (!sourceFieldNames.contains(nestedRule.field())) {
            throw new IllegalArgumentException("Field '" + nestedRule.field()
                + "' does not exist in source application schema for resource '" + resourceId
                + "' in correlation rule at index " + ruleIndex + ", sub-rule path " + currentPath);
          }
        }

        // If valueSource is "field", validate that the value references a valid field
        // in the internal schema
        if ("field".equals(nestedRule.valueSource())) {
          if (!internalFieldNames.contains(nestedRule.value())) {
            throw new IllegalArgumentException("Field '" + nestedRule.value()
                + "' does not exist in internal schema for identity type '" + identityType
                + "' in correlation rule at index " + ruleIndex
                + ", sub-rule path " + currentPath);
          }
        }
      } else if (isGroupRule) {
        // Validate group rule fields
        if (nestedRule.rules() == null || nestedRule.rules().isEmpty()) {
          throw new IllegalArgumentException(
              "Group rule must contain at least one sub-rule in correlation rule at index " + ruleIndex
                  + ", sub-rule path " + currentPath);
        }
        // Recursively validate nested rules
        validateCorrelationRuleFields(nestedRule.rules(), sourceFieldNames, internalFieldNames,
            identityType, resourceId, ruleIndex, currentPath);
      } else {
        throw new IllegalArgumentException(
            "Rule must be either a leaf rule (with field/operator/value) or a group rule (with combinator/rules) in correlation rule at index "
                + ruleIndex + ", sub-rule path " + currentPath);
      }
    }
  }
}
