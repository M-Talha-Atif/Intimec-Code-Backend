package com.aidentx.iam.organization_management.repository;

import com.aidentx.iam.organization_management.model.entity.OrgUnitHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrgUnitHistoryRepository extends JpaRepository<OrgUnitHistory, Long> {

    Page<OrgUnitHistory> findByTenantIdAndOrgUnitIdOrderByOccurredAtDesc(
            String tenantId, UUID orgUnitId, Pageable pageable);
}
