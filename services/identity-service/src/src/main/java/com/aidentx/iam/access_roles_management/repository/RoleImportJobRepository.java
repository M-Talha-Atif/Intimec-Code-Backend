package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.RoleImportJob;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleImportJobRepository extends JpaRepository<RoleImportJob, UUID> {

    Optional<RoleImportJob> findByIdAndTenantId(UUID id, String tenantId);

    Page<RoleImportJob> findByTenantId(String tenantId, Pageable pageable);

    long countByTenantIdAndStatus(String tenantId, RoleImportJobStatus status);
}

