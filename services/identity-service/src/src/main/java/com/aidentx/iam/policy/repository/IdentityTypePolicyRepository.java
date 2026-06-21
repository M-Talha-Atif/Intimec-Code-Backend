package com.aidentx.iam.policy.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.policy.model.entity.IdentityTypePolicy;

/**
 * Repository for IdentityTypePolicy entities.
 * Provides CRUD operations for global governance policies stored at the
 * identity type level.
 */
@Repository
public interface IdentityTypePolicyRepository extends JpaRepository<IdentityTypePolicy, UUID> {

  /**
   * Finds a policy by tenant ID and identity type.
   *
   * @param tenantId     The tenant identifier
   * @param identityType The identity type
   * @return Optional containing the policy if found
   */
  Optional<IdentityTypePolicy> findByTenantIdAndIdentityType(String tenantId, IdentityType identityType);
}
