package com.aidentx.iam.organization_management.repository;

import com.aidentx.iam.organization_management.model.entity.OrgUnitDelegate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrgUnitDelegateRepository extends JpaRepository<OrgUnitDelegate, UUID> {

    List<OrgUnitDelegate> findByTenantIdAndOrgUnitIdAndRevokedAtIsNull(String tenantId, UUID orgUnitId);

    List<OrgUnitDelegate> findByTenantIdAndOrgUnitId(String tenantId, UUID orgUnitId);
}
