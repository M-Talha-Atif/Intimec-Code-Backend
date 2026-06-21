package com.aidentx.iam.entitlement_management.policy.service;

import com.aidentx.iam.contracts.dto.policy.AttributeMappingDTO;
import com.aidentx.iam.contracts.dto.policy.EntitlementMappingPolicyDTO;
import com.aidentx.iam.entitlement_management.policy.entity.EntitlementGovernancePolicy;
import com.aidentx.iam.entitlement_management.policy.repository.EntitlementGovernancePolicyRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for entitlement governance policy management.
 *
 * <p>Handles CRUD operations on {@link EntitlementGovernancePolicy} entities,
 * serialising/deserialising the {@code attribute_mappings} JSONB column to/from
 * {@code List<AttributeMappingDTO>} using Jackson.</p>
 */
@Service
public class EntitlementGovernancePolicyServiceImpl implements EntitlementGovernancePolicyService {

    private final EntitlementGovernancePolicyRepository repository;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public EntitlementGovernancePolicyServiceImpl(
            EntitlementGovernancePolicyRepository repository,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(EntitlementGovernancePolicyServiceImpl.class);
    }

    // -----------------------------------------------------------------------
    // Reads
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public EntitlementMappingPolicyDTO getPolicy(String tenantId, UUID resourceId) {
        logger.info().message("Retrieving entitlement governance policy")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId)
                .log();

        Optional<EntitlementGovernancePolicy> opt =
                repository.findByTenantIdAndResourceId(tenantId, resourceId);

        if (opt.isEmpty()) {
            logger.debug().message("No entitlement governance policy found — returning empty policy")
                    .detail("tenantId", tenantId)
                    .detail("resourceId", resourceId)
                    .log();
            return EntitlementMappingPolicyDTO.empty(resourceId);
        }

        return toDto(opt.get());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttributeMappingDTO> getAttributeMappings(String tenantId, UUID resourceId) {
        Optional<EntitlementGovernancePolicy> opt =
                repository.findByTenantIdAndResourceId(tenantId, resourceId);

        if (opt.isEmpty() || opt.get().getAttributeMappings() == null) {
            return List.of();
        }

        List<AttributeMappingDTO> raw = deserializeMappings(opt.get());

        // Back-fill sourceResourceId and id from the policy's resourceId where absent
        // (same pattern as ResourceGovernancePolicyServiceImpl for backward-compat)
        UUID policyResourceId = opt.get().getResourceId();
        return raw.stream()
                .map(m -> {
                    if (m.id() == null || m.sourceResourceId() == null) {
                        return new AttributeMappingDTO(
                                m.id() != null ? m.id() : UUID.randomUUID(),
                                m.sourceResourceId() != null ? m.sourceResourceId() : policyResourceId,
                                m.sourceAttributeName(),
                                m.targetAttributeName(),
                                m.mode(),
                                m.script()
                        );
                    }
                    return m;
                })
                .collect(Collectors.toList());
    }

    // -----------------------------------------------------------------------
    // Writes
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public EntitlementMappingPolicyDTO updatePolicy(String tenantId, UUID resourceId,
            EntitlementMappingPolicyDTO policyDto) {
        logger.info().message("Updating entitlement governance policy")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId)
                .detail("mappingCount",
                        policyDto.attributeMappings() != null ? policyDto.attributeMappings().size() : 0)
                .log();

        EntitlementGovernancePolicy policy = repository
                .findByTenantIdAndResourceId(tenantId, resourceId)
                .orElseGet(() -> new EntitlementGovernancePolicy(tenantId, resourceId));

        // Ensure every mapping rule has a stable id and sourceResourceId
        List<AttributeMappingDTO> mappings = policyDto.attributeMappings() == null
                ? List.of()
                : policyDto.attributeMappings().stream()
                    .map(m -> new AttributeMappingDTO(
                            m.id() != null ? m.id() : UUID.randomUUID(),
                            m.sourceResourceId() != null ? m.sourceResourceId() : resourceId,
                            m.sourceAttributeName(),
                            m.targetAttributeName(),
                            m.mode(),
                            m.script()))
                    .collect(Collectors.toList());

        policy.setAttributeMappings(objectMapper.valueToTree(mappings));
        EntitlementGovernancePolicy saved = repository.save(policy);

        logger.info().message("Entitlement governance policy saved")
                .detail("policyId", saved.getId())
                .detail("mappingCount", mappings.size())
                .log();

        return toDto(saved);
    }

    @Override
    @Transactional
    public int deletePoliciesByResource(String tenantId, UUID resourceId) {
        logger.info().message("Deleting entitlement governance policy")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId)
                .log();

        int deleted = repository.deleteByTenantIdAndResourceId(tenantId, resourceId);

        // Cascade: purge any cross-resource source-mapping references pointing to this
        // (deleted) resource from all other entitlement policies in the tenant.
        purgeOrphanedSourceMappings(tenantId, resourceId);

        logger.info().message("Deleted entitlement governance policy rows and purged orphaned source mappings")
                .detail("tenantId", tenantId)
                .detail("resourceId", resourceId)
                .detail("deletedRows", deleted)
                .log();

        return deleted;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * After an application is deleted, scans remaining entitlement policies for
     * the same tenant and removes any {@link AttributeMappingDTO} whose
     * {@code sourceResourceId} matches the deleted resource.
     */
    private void purgeOrphanedSourceMappings(String tenantId, UUID deletedResourceId) {
        List<EntitlementGovernancePolicy> allPolicies = repository.findByTenantId(tenantId);
        int purgedRules = 0;

        for (EntitlementGovernancePolicy policy : allPolicies) {
            if (policy.getAttributeMappings() == null) {
                continue;
            }
            List<AttributeMappingDTO> current = deserializeMappings(policy);
            long before = current.size();
            List<AttributeMappingDTO> cleaned = current.stream()
                    .filter(m -> !deletedResourceId.equals(m.sourceResourceId()))
                    .collect(Collectors.toList());

            if (cleaned.size() < before) {
                purgedRules += (before - cleaned.size());
                policy.setAttributeMappings(objectMapper.valueToTree(cleaned));
                repository.save(policy);
            }
        }

        if (purgedRules > 0) {
            logger.info().message("Purged orphaned entitlement source-mapping rules")
                    .detail("deletedResourceId", deletedResourceId)
                    .detail("purgedRuleCount", purgedRules)
                    .log();
        }
    }

    private EntitlementMappingPolicyDTO toDto(EntitlementGovernancePolicy policy) {
        return new EntitlementMappingPolicyDTO(
                policy.getResourceId(),
                deserializeMappings(policy));
    }

    private List<AttributeMappingDTO> deserializeMappings(EntitlementGovernancePolicy policy) {
        if (policy.getAttributeMappings() == null) {
            return new ArrayList<>();
        }
        try {
            List<AttributeMappingDTO> result = objectMapper.convertValue(
                    policy.getAttributeMappings(),
                    new TypeReference<List<AttributeMappingDTO>>() {});
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            logger.error(e).message("Failed to deserialise entitlement attribute_mappings JSONB")
                    .detail("policyId", policy.getId())
                    .detail("error", e.getMessage())
                    .log();
            return new ArrayList<>();
        }
    }
}
