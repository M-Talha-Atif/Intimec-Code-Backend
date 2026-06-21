package com.aidentx.iam.identity_management.repository;

import com.aidentx.iam.identity_management.model.entity.ExtendedAttributeDefinition;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExtendedAttributeDefinitionRepository extends JpaRepository<ExtendedAttributeDefinition, UUID>,
                JpaSpecificationExecutor<ExtendedAttributeDefinition> {

        List<ExtendedAttributeDefinition> findByTenantIdAndIdentityType(String tenantId, IdentityType identityType);

        List<ExtendedAttributeDefinition> findByTenantIdAndIdentityTypeAndIsRequired(String tenantId,
                        IdentityType identityType, Boolean isRequired);

        Optional<ExtendedAttributeDefinition> findByTenantIdAndSystemKey(String tenantId, String systemKey);

        boolean existsByTenantIdAndIdentityTypeAndSystemKey(String tenantId, IdentityType identityType,
                        String systemKey);

        Optional<ExtendedAttributeDefinition> findByTenantIdAndIdentityTypeAndSystemKey(String tenantId,
                        IdentityType identityType, String systemKey);

        Optional<ExtendedAttributeDefinition> findByIdAndTenantId(UUID id, String tenantId);
}