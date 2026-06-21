package com.aidentx.iam.policy.repository;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.policy.model.entity.IdentityGovernancePolicy;

/**
 * Repository for IdentityGovernancePolicy entities.
 * Provides CRUD operations for governance policies stored at the schema level.
 */
@Repository
public interface IdentityGovernancePolicyRepository extends JpaRepository<IdentityGovernancePolicy, UUID> {

    /**
     * Find governance policy by tenant and schema.
     *
     * @param tenantId The tenant identifier
     * @param schemaId The schema identifier
     * @return Optional containing the governance policy if found
     */
    Optional<IdentityGovernancePolicy> findByTenantIdAndResourceId(String tenantId, UUID resourceId);

    Optional<IdentityGovernancePolicy> findByTenantIdAndResourceIdAndIdentityType(String tenantId, UUID resourceId,
            IdentityType identityType);

    List<IdentityGovernancePolicy> findByTenantIdAndIdentityType(String tenantId, IdentityType identityType);

    /**
     * Find all governance policies for a tenant.
     *
     * @param tenantId The tenant identifier
     * @return List of governance policies for the tenant
     */
    java.util.List<IdentityGovernancePolicy> findByTenantId(String tenantId);

    /**
     * Check if a governance policy exists for the given tenant and schema.
     *
     * @param tenantId The tenant identifier
     * @param schemaId The schema identifier
     * @return true if policy exists, false otherwise
     */
    boolean existsByTenantIdAndIdentityType(String tenantId, IdentityType identityType);

    /**
     * Delete all policies associated with a resource.
     *
     * @param tenantId   The tenant identifier
     * @param resourceId The resource UUID
     * @return The number of deleted policies
     */
    int deleteByTenantIdAndResourceId(String tenantId, UUID resourceId);
}
