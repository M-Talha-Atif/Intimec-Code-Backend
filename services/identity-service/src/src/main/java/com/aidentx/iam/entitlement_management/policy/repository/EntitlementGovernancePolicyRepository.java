package com.aidentx.iam.entitlement_management.policy.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aidentx.iam.entitlement_management.policy.entity.EntitlementGovernancePolicy;

/**
 * JPA repository for {@link EntitlementGovernancePolicy}.
 */
@Repository
public interface EntitlementGovernancePolicyRepository
        extends JpaRepository<EntitlementGovernancePolicy, UUID> {

    Optional<EntitlementGovernancePolicy> findByTenantIdAndResourceId(
            String tenantId, UUID resourceId);

    List<EntitlementGovernancePolicy> findByTenantId(String tenantId);

    int deleteByTenantIdAndResourceId(String tenantId, UUID resourceId);

    boolean existsByTenantIdAndResourceId(String tenantId, UUID resourceId);
}
