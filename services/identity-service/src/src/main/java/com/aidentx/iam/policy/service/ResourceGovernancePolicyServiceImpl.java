package com.aidentx.iam.policy.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingGroupDTO;
import com.aidentx.iam.contracts.dto.policy.AttributeMappingRuleDTO;
import com.aidentx.iam.contracts.dto.policy.CorrelationRuleDTO;
import com.aidentx.iam.contracts.dto.policy.IdentityMappingPolicyDTO;
import com.aidentx.iam.contracts.dto.policy.SynchronizationReactionDTO;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.policy.model.entity.IdentityGovernancePolicy;
import com.aidentx.iam.policy.repository.IdentityGovernancePolicyRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service implementation for governance policy management.
 * Handles all governance data operations including correlation rules, attribute
 * mappings, and source priorities.
 * Moved from IAM service to Core service as part of architectural refactoring.
 */
@Service
public class ResourceGovernancePolicyServiceImpl implements ResourceGovernancePolicyService {

        private final IdentityGovernancePolicyRepository policyRepository;
        private final ObjectMapper objectMapper;
        private final IUnifiedLogger logger;
        private final GovernancePolicyHelper helper;
        private final GovernancePolicyValidator validator;

        public ResourceGovernancePolicyServiceImpl(IdentityGovernancePolicyRepository policyRepository,
                        ObjectMapper objectMapper,
                        UnifiedLoggerFactory loggerFactory,
                        GovernancePolicyHelper helper,
                        GovernancePolicyValidator validator) {
                this.policyRepository = policyRepository;
                this.objectMapper = objectMapper;
                this.logger = loggerFactory.getLogger(ResourceGovernancePolicyServiceImpl.class);
                this.helper = helper;
                this.validator = validator;
        }

        @Override
        @Transactional
        public IdentityMappingPolicyDTO getPolicy(String tenantId, IdentityType identityType, UUID resourceId) {
                logger.info().message("Retrieving governance policy")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("resourceId", resourceId)
                                .log();

                validator.validateSchemaExists(tenantId, identityType);
                Optional<IdentityGovernancePolicy> policyOpt = policyRepository
                                .findByTenantIdAndResourceIdAndIdentityType(
                                                tenantId, resourceId,
                                                identityType);

                if (policyOpt.isEmpty()) {
                        logger.info().message("No governance policy found, returning empty policy")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .detail("resourceId", resourceId)
                                        .log();
                        return new IdentityMappingPolicyDTO(resourceId, List.of(), List.of());
                }

                IdentityGovernancePolicy policy = policyOpt.get();
                logger.info().message("Found governance policy")
                                .detail("policyId", policy.getId())
                                .detail("hasAttributeMappings", policy.getAttributeMappings() != null)
                                .detail("hasSyncReactions", policy.getSynchronizationReactions() != null)
                                .log();
                return helper.convertEntityToDto(policy, policyRepository);
        }

        @Override
        @Transactional
        public List<IdentityMappingPolicyDTO> getAllPoliciesForIdentityType(String tenantId,
                        IdentityType identityType) {
                logger.info().message("Retrieving all governance policies")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                validator.validateSchemaExists(tenantId, identityType);
                List<IdentityGovernancePolicy> policies = policyRepository.findByTenantIdAndIdentityType(tenantId,
                                identityType);

                return policies.stream()
                                .map(p -> helper.convertEntityToDto(p, policyRepository))
                                .collect(Collectors.toList());
        }

        @Transactional
        @Override
        public IdentityMappingPolicyDTO updatePolicy(String tenantId, IdentityType identityType, UUID resourceId,
                        UUID schemaId, IdentityMappingPolicyDTO policyDto) {
                logger.info().message("Updating governance policy")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("schemaId", schemaId)
                                .log();

                IdentityGovernancePolicy policy = policyRepository
                                .findByTenantIdAndResourceIdAndIdentityType(tenantId, resourceId,
                                                identityType)
                                .orElseGet(() -> {
                                        IdentityGovernancePolicy newPolicy = new IdentityGovernancePolicy(tenantId, identityType, resourceId, schemaId);
                                        return newPolicy;
                                });

                // Update schemaId if provided and policy already exists
                if (schemaId != null && policy.getId() != null) {
                        policy.setSchemaId(schemaId);
                }

                // Update the policy data
                helper.updateEntityFromDto(policy, policyDto);

                IdentityGovernancePolicy savedPolicy = policyRepository.save(policy);

                logger.info().message("Successfully updated governance policy")
                                .detail("policyId", savedPolicy.getId())
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                return helper.convertEntityToDto(savedPolicy, policyRepository);
        }

        @Override
        @Transactional
        public int deletePoliciesByResource(String tenantId, UUID resourceId) {
                logger.info().message("Deleting policies by resource")
                                .detail("tenantId", tenantId)
                                .detail("resourceId", resourceId)
                                .log();

                // 1. Delete the policy row(s) owned by this resource.
                int deletedCount = policyRepository.deleteByTenantIdAndResourceId(tenantId, resourceId);

                // 2. Cascade: purge any attribute-mapping rules in OTHER policy rows that
                //    reference this resource as their sourceResourceId. These are created when
                //    a user configures cross-app mappings and would remain as orphans after
                //    the app is deleted without this step.
                purgeOrphanedSourceMappings(tenantId, resourceId);

                logger.info().message("Successfully deleted policies and purged orphaned source-mapping references")
                                .detail("tenantId", tenantId)
                                .detail("resourceId", resourceId)
                                .detail("deletedPolicyRows", deletedCount)
                                .log();

                return deletedCount;
        }

        /**
         * Scans every remaining policy for the tenant and removes any
         * {@link AttributeMappingDTO} entry whose {@code sourceResourceId} matches the
         * given (just-deleted) resource. This prevents stale/orphaned source-mapping
         * rows from appearing in the UI after an application is deleted.
         */
        private void purgeOrphanedSourceMappings(String tenantId, UUID deletedResourceId) {
                List<IdentityGovernancePolicy> allPolicies = policyRepository.findByTenantId(tenantId);
                int purgedRules = 0;

                for (IdentityGovernancePolicy policy : allPolicies) {
                        if (policy.getAttributeMappings() == null) {
                                continue;
                        }

                        List<AttributeMappingDTO> current = helper.deserializeJsonList(
                                        policy.getAttributeMappings(),
                                        new TypeReference<List<AttributeMappingDTO>>() {
                                        },
                                        "attribute mappings for orphan purge");

                        long before = current.size();
                        List<AttributeMappingDTO> cleaned = current.stream()
                                        .filter(m -> !deletedResourceId.equals(m.sourceResourceId()))
                                        .collect(Collectors.toList());

                        if (cleaned.size() < before) {
                                purgedRules += (before - cleaned.size());
                                policy.setAttributeMappings(objectMapper.valueToTree(cleaned));
                                policyRepository.save(policy);
                        }
                }

                if (purgedRules > 0) {
                        logger.info().message("Purged orphaned source-mapping rules")
                                        .detail("deletedResourceId", deletedResourceId)
                                        .detail("purgedRuleCount", purgedRules)
                                        .log();
                }
        }

        @Override
        public IdentityGovernancePolicy getOrCreatePolicy(String tenantId, IdentityType identityType, UUID resourceId) {
                return policyRepository.findByTenantIdAndResourceIdAndIdentityType(tenantId, resourceId, identityType)
                                .orElse(new IdentityGovernancePolicy(tenantId, identityType, resourceId, null));
        }

        @Override
        @Transactional
        public List<AttributeMappingDTO> getAttributeMappings(String tenantId, IdentityType identityType) {
                logger.info().message("Retrieving attribute mappings")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                List<IdentityGovernancePolicy> policies = policyRepository.findByTenantIdAndIdentityType(tenantId,
                                identityType);

                if (policies.isEmpty()) {
                        logger.info().message("No attribute mappings found")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .log();
                        return List.of();
                }

                List<AttributeMappingDTO> allMappings = new ArrayList<>();

                for (IdentityGovernancePolicy policy : policies) {
                        if (policy.getAttributeMappings() == null) {
                                continue;
                        }

                        List<AttributeMappingDTO> mappings = helper.deserializeJsonList(
                                        policy.getAttributeMappings(),
                                        new TypeReference<List<AttributeMappingDTO>>() {
                                        },
                                        "attribute mappings");

                        // Backfill sourceResourceId and id from the policy's resourceId when they were not
                        // persisted (legacy records stored before these fields were enforced).
                        UUID policyResourceId = policy.getResourceId();
                        List<AttributeMappingDTO> backfilled = mappings.stream()
                                        .map(m -> {
                                                UUID id = m.id() != null ? m.id() : UUID.randomUUID();
                                                UUID srcRes = m.sourceResourceId() != null ? m.sourceResourceId() : policyResourceId;
                                                return (id == m.id() && srcRes == m.sourceResourceId()) ? m
                                                        : new AttributeMappingDTO(id, srcRes,
                                                                        m.sourceAttributeName(),
                                                                        m.targetAttributeName(), m.mode(), m.script());
                                        })
                                        // Drop entries that still have no sourceResourceId (policies
                                        // created without a resource, e.g. old global policies).
                                        .filter(m -> m.sourceResourceId() != null)
                                        .collect(Collectors.toList());

                        allMappings.addAll(backfilled);
                }

                logger.info().message("Retrieved all attribute mappings")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("totalMappings", allMappings.size())
                                .detail("fromPolicies", policies.size())
                                .log();

                return allMappings;
        }

        @Override
        @Transactional
        public List<CorrelationRuleDTO> getCorrelationRules(String tenantId, IdentityType identityType) {
                logger.info().message("Retrieving correlation rules")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                List<IdentityGovernancePolicy> policies = policyRepository.findByTenantIdAndIdentityType(tenantId,
                                identityType);

                if (policies.isEmpty()) {
                        logger.info().message("No correlation rules found")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .log();
                        return List.of();
                }

                List<CorrelationRuleDTO> allRules = new ArrayList<>();

                for (IdentityGovernancePolicy policy : policies) {
                        List<CorrelationRuleDTO> rules = helper.getCorrelationRulesFromPolicy(policy, tenantId,
                                        identityType);
                        allRules.addAll(rules);
                }

                return allRules;
        }

        @Override
        @Transactional
        public List<CorrelationRuleDTO> updateCorrelationRules(String tenantId, IdentityType identityType,
                        UUID resourceId,
                        List<CorrelationRuleDTO> ruleDTOs) {
                logger.info().message("Updating correlation rules")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("rulesCount", ruleDTOs.size())
                                .log();

                // Normalize incoming payload (generate IDs for new items)
                List<CorrelationRuleDTO> normalizedIncoming = ruleDTOs.stream()
                                .map(rule -> rule.id() == null
                                                ? new CorrelationRuleDTO(UUID.randomUUID(), rule.resourceId(),
                                                                rule.mode(), rule.ruleGroup(),
                                                                rule.script(), rule.priority(), rule.enabled())
                                                : rule)
                                .toList();

                // Load existing rules and merge (add new or update by ID), instead of replacing
                // all
                IdentityGovernancePolicy policy = policyRepository
                                .findByTenantIdAndResourceIdAndIdentityType(tenantId, resourceId, identityType)
                                .orElse(new IdentityGovernancePolicy(tenantId, identityType, resourceId, null));

                List<CorrelationRuleDTO> existing = new java.util.ArrayList<>(
                                helper.getCorrelationRulesFromPolicy(policy, tenantId, identityType));

                for (CorrelationRuleDTO incoming : normalizedIncoming) {
                        boolean updated = false;
                        for (int i = 0; i < existing.size(); i++) {
                                CorrelationRuleDTO current = existing.get(i);
                                if (current.id() != null && current.id().equals(incoming.id())) {
                                        existing.set(i, incoming);
                                        updated = true;
                                        break;
                                }
                        }
                        if (!updated) {
                                existing.add(incoming);
                        }
                }

                policy.setCorrelationRules(objectMapper.valueToTree(existing));
                policyRepository.save(policy);

                logger.info().message("Successfully updated correlation rules (merged)")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("totalRules", existing.size())
                                .log();

                // Return only the processed items from this request (with IDs resolved)
                return normalizedIncoming;
        }

        @Override
        @Transactional
        public void deletePolicies(String tenantId, IdentityType identityType) {
                logger.info().message("Deleting all governance policies")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                List<IdentityGovernancePolicy> policies = policyRepository.findByTenantIdAndIdentityType(tenantId,
                                identityType);

                if (!policies.isEmpty()) {
                        policyRepository.deleteAll(policies);
                        logger.info().message("Successfully deleted governance policies")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .detail("deletedCount", policies.size())
                                        .log();
                } else {
                        logger.info().message("No governance policies found to delete")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .log();
                }
        }

        @Override
        @Transactional
        public void deleteCorrelationRules(String tenantId, IdentityType identityType) {
                logger.info().message("Deleting correlation rules")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                List<IdentityGovernancePolicy> policies = policyRepository.findByTenantIdAndIdentityType(tenantId,
                                identityType);

                if (!policies.isEmpty()) {
                        for (IdentityGovernancePolicy policy : policies) {
                                policy.setCorrelationRules(null);
                        }
                        policyRepository.saveAll(policies);
                        logger.info().message("Successfully deleted correlation rules")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .detail("affectedPolicies", policies.size())
                                        .log();
                } else {
                        logger.info().message("No governance policy found for correlation rules deletion")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .log();
                }
        }

        @Override
        @Transactional
        public void deleteAttributeMappings(String tenantId, IdentityType identityType) {
                logger.info().message("Deleting attribute mappings")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                List<IdentityGovernancePolicy> policies = policyRepository.findByTenantIdAndIdentityType(tenantId,
                                identityType);

                if (!policies.isEmpty()) {
                        for (IdentityGovernancePolicy policy : policies) {
                                policy.setAttributeMappings(null);
                        }
                        policyRepository.saveAll(policies);
                        logger.info().message("Successfully deleted attribute mappings")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .detail("affectedPolicies", policies.size())
                                        .log();
                } else {
                        logger.info().message("No governance policy found for attribute mappings deletion")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .log();
                }
        }

        @Override
        @Transactional
        public void deleteCorrelationRule(String tenantId, IdentityType identityType, UUID resourceId, UUID ruleId) {
                logger.info().message("Deleting correlation rule by ID")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("ruleId", ruleId)
                                .log();

                IdentityGovernancePolicy policy = getOrCreatePolicy(tenantId, identityType, resourceId);
                List<CorrelationRuleDTO> rules = new java.util.ArrayList<>(
                                helper.getCorrelationRulesFromPolicy(policy, tenantId, identityType));
                boolean removed = rules.removeIf(r -> r.id() != null && r.id().equals(ruleId));

                if (removed) {
                        policy.setCorrelationRules(objectMapper.valueToTree(rules));
                        policyRepository.save(policy);
                        logger.info().message("Successfully deleted correlation rule")
                                        .detail("ruleId", ruleId)
                                        .log();
                } else {
                        logger.info().message("No correlation rule found to delete")
                                        .detail("ruleId", ruleId)
                                        .log();
                }
        }

        @Override
        @Transactional
        public void deleteAttributeMapping(String tenantId, IdentityType identityType, UUID resourceId,
                        UUID mappingId) {
                logger.info().message("Deleting attribute mapping by ID")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("mappingId", mappingId)
                                .log();

                IdentityGovernancePolicy policy = getOrCreatePolicy(tenantId, identityType, resourceId);
                List<AttributeMappingDTO> mappings = new java.util.ArrayList<>(
                                helper.getAttributeMappingsFromPolicy(policy, tenantId, identityType));
                boolean removed = mappings.removeIf(m -> m.id() != null && m.id().equals(mappingId));

                if (removed) {
                        policy.setAttributeMappings(objectMapper.valueToTree(mappings));
                        policyRepository.save(policy);
                        logger.info().message("Successfully deleted attribute mapping")
                                        .detail("mappingId", mappingId)
                                        .log();
                } else {
                        logger.info().message("No attribute mapping found to delete")
                                        .detail("mappingId", mappingId)
                                        .log();
                }
        }

        @Override
        public void validatePolicy(String tenantId, IdentityType identityType, IdentityMappingPolicyDTO policyDto) {
                validator.validatePolicy(tenantId, identityType, policyDto);
        }

        @Override
        public void validateAttributeMappings(String tenantId, IdentityType identityType,
                        List<AttributeMappingDTO> mappingDTOs) {
                validator.validateAttributeMappings(tenantId, identityType, mappingDTOs);
        }

        @Override
        public void validateCorrelationRules(String tenantId, IdentityType identityType,
                        List<CorrelationRuleDTO> ruleDTOs) {
                validator.validateCorrelationRules(tenantId, identityType, ruleDTOs);
        }

        @Override
        public CorrelationRuleDTO updateCorrelationRule(String tenantId, IdentityType identityType, UUID resourceId,
                        UUID ruleId,
                        CorrelationRuleDTO ruleDto) {
                logger.info().message("Updating correlation rule by ID")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("ruleId", ruleId)
                                .log();

                // Retrieve existing policy
                IdentityGovernancePolicy policy = getOrCreatePolicy(tenantId, identityType, resourceId);

                // Find and update the specific rule
                List<CorrelationRuleDTO> rules = new java.util.ArrayList<>(
                                helper.getCorrelationRulesFromPolicy(policy, tenantId, identityType));
                for (int i = 0; i < rules.size(); i++) {
                        if (rules.get(i).id() != null && rules.get(i).id().equals(ruleId)) {
                                // Ensure the DTO has the correct ID
                                CorrelationRuleDTO updatedRule = new CorrelationRuleDTO(ruleId, ruleDto.resourceId(),
                                                ruleDto.mode(),
                                                ruleDto.ruleGroup(), ruleDto.script(),
                                                ruleDto.priority(), ruleDto.enabled());
                                rules.set(i, updatedRule);
                                break;
                        }
                }

                // Save the updated policy
                policy.setCorrelationRules(objectMapper.valueToTree(rules));
                policyRepository.save(policy);

                logger.info().message("Successfully updated correlation rule")
                                .detail("ruleId", ruleId)
                                .log();

                return ruleDto;
        }

        @Override
        @Transactional
        public AttributeMappingDTO updateAttributeMapping(String tenantId, IdentityType identityType, UUID mappingId,
                        AttributeMappingDTO mappingDto) {
                logger.info().message("Updating attribute mapping by ID")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("mappingId", mappingId)
                                .log();

                // Validate the mapping
                validateAttributeMapping(tenantId, identityType, mappingDto);

                // Get the resource ID from the mapping DTO
                UUID resourceId = mappingDto.sourceResourceId();
                if (resourceId == null) {
                        throw new IllegalArgumentException("Source resource ID must be provided in the mapping");
                }

                // Retrieve existing policy
                IdentityGovernancePolicy policy = getOrCreatePolicy(tenantId, identityType, resourceId);

                // Get current mappings (ensure modifiable)
                List<AttributeMappingDTO> mappings = new java.util.ArrayList<>(
                                helper.getAttributeMappingsFromPolicy(policy, tenantId, identityType));

                // Find and update the specific mapping
                boolean found = false;
                for (int i = 0; i < mappings.size(); i++) {
                        if (mappings.get(i).id() != null && mappings.get(i).id().equals(mappingId)) {
                                // Ensure the DTO has the correct ID
                                AttributeMappingDTO updatedMapping = new AttributeMappingDTO(
                                                mappingId,
                                                mappingDto.sourceResourceId(),
                                                mappingDto.sourceAttributeName(),
                                                mappingDto.targetAttributeName(),
                                                mappingDto.mode(),
                                                mappingDto.script());
                                mappings.set(i, updatedMapping);
                                found = true;
                                break;
                        }
                }

                if (!found) {
                        throw new IllegalArgumentException("Attribute mapping with ID " + mappingId + " not found");
                }

                // Store the updated mappings
                policy.setAttributeMappings(objectMapper.valueToTree(mappings));
                policyRepository.save(policy);

                logger.info().message("Successfully updated attribute mapping")
                                .detail("mappingId", mappingId)
                                .log();

                return mappingDto;
        }

        @Override
        public void validateCorrelationRule(String tenantId, IdentityType identityType, CorrelationRuleDTO ruleDto) {
                validator.validateCorrelationRule(tenantId, identityType, ruleDto);
        }

        @Override
        public void validateAttributeMapping(String tenantId, IdentityType identityType,
                        AttributeMappingDTO mappingDto) {
                validator.validateAttributeMapping(tenantId, identityType, mappingDto);
        }

        @Override
        @Transactional(readOnly = true)
        public List<AttributeMappingGroupDTO> getAttributeMappingGroups(String tenantId, IdentityType identityType) {
                logger.info().message("Retrieving grouped attribute mappings")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                List<AttributeMappingDTO> flatMappings = getAttributeMappings(tenantId, identityType);
                // log to console
                logger.info().message("Flat mappings").detail("flatMappings", flatMappings).log();

                if (flatMappings.isEmpty()) {
                        return List.of();
                }

                // grouped by target attribute name — filter out entries without a valid
                // targetAttributeName to avoid NullPointerException in groupingBy
                Map<String, List<AttributeMappingDTO>> groupedMappings = flatMappings.stream()
                                .filter(mapping -> mapping.targetAttributeName() != null
                                                && !mapping.targetAttributeName().isBlank())
                                .collect(Collectors.groupingBy(AttributeMappingDTO::targetAttributeName));

                List<AttributeMappingGroupDTO> result = new ArrayList<>();
                for (Map.Entry<String, List<AttributeMappingDTO>> entry : groupedMappings.entrySet()) {
                        String targetAttributeName = entry.getKey();
                        List<AttributeMappingDTO> mappings = entry.getValue();

                        List<AttributeMappingRuleDTO> rules = mappings.stream()
                                        .map(mapping -> new AttributeMappingRuleDTO(
                                                        mapping.id(),
                                                        mapping.sourceResourceId(),
                                                        mapping.sourceAttributeName(),
                                                        mapping.mode(),
                                                        mapping.script()))
                                        .collect(Collectors.toList());

                        result.add(
                                        new AttributeMappingGroupDTO(targetAttributeName,
                                                        identityType.toString(),
                                                        rules));
                }

                logger.info().message("Retrieved grouped attribute mappings")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("groupCount", result.size())
                                .log();

                return result;
        }

        @Override
        @Transactional(readOnly = true)
        public List<SynchronizationReactionDTO> getSynchronizationReactions(String tenantId,
                        IdentityType identityType, UUID resourceId) {
                logger.info().message("Retrieving synchronization reactions")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("resourceId", resourceId)
                                .log();

                Optional<IdentityGovernancePolicy> policyOpt = policyRepository
                                .findByTenantIdAndResourceIdAndIdentityType(tenantId, resourceId, identityType);

                if (policyOpt.isEmpty() || policyOpt.get().getSynchronizationReactions() == null) {
                        return List.of();
                }

                return helper.deserializeJsonList(
                                policyOpt.get().getSynchronizationReactions(),
                                new TypeReference<List<SynchronizationReactionDTO>>() {},
                                "synchronization reactions");
        }

        @Override
        @Transactional
        public List<SynchronizationReactionDTO> updateSynchronizationReactions(String tenantId,
                        IdentityType identityType, UUID resourceId,
                        List<SynchronizationReactionDTO> reactions) {
                logger.info().message("Updating synchronization reactions")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("resourceId", resourceId)
                                .detail("reactionsCount", reactions.size())
                                .log();

                IdentityGovernancePolicy policy = getOrCreatePolicy(tenantId, identityType, resourceId);
                policy.setSynchronizationReactions(objectMapper.valueToTree(reactions));
                policyRepository.save(policy);

                logger.info().message("Successfully updated synchronization reactions")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("resourceId", resourceId)
                                .detail("reactionsCount", reactions.size())
                                .log();

                return reactions;
        }

}
