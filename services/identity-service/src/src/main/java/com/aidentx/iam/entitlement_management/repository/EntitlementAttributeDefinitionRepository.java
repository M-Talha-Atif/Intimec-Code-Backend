package com.aidentx.iam.entitlement_management.repository;

import com.aidentx.iam.entitlement_management.model.entity.EntitlementAttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntitlementAttributeDefinitionRepository extends JpaRepository<EntitlementAttributeDefinition, UUID> {
    List<EntitlementAttributeDefinition> findByTenantIdOrderByDisplayOrderAscDisplayNameAsc(String tenantId);

    Optional<EntitlementAttributeDefinition> findByTenantIdAndSystemKey(String tenantId, String systemKey);

    boolean existsByTenantIdAndSystemKey(String tenantId, String systemKey);
}
