package com.aidentx.iam.identity_management.repository;

import com.aidentx.iam.identity_management.model.entity.ExtAttrNumberValue;
import com.aidentx.iam.identity_management.model.entity.ExtendedAttributeValueId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ExtAttrNumberValueRepository extends JpaRepository<ExtAttrNumberValue, ExtendedAttributeValueId> {

    List<ExtAttrNumberValue> findByIdentityId(UUID identityId);

    List<ExtAttrNumberValue> findByIdentityIdAndDefinitionIdIn(UUID identityId, Iterable<UUID> definitionIds);

    void deleteByIdentityId(UUID identityId);

    long countByDefinitionId(UUID definitionId);

    boolean existsByIdentityIdAndDefinitionId(UUID identityId, UUID definitionId);
}