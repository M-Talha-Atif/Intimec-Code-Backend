package com.aidentx.iam.access_roles_management.repository;

import com.aidentx.iam.access_roles_management.model.entity.RoleImportJobItem;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemPhase;
import com.aidentx.iam.access_roles_management.model.enums.RoleImportItemStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RoleImportJobItemRepository extends JpaRepository<RoleImportJobItem, UUID> {

    Page<RoleImportJobItem> findByJobId(UUID jobId, Pageable pageable);

    Page<RoleImportJobItem> findByJobIdAndStatus(UUID jobId, RoleImportItemStatus status, Pageable pageable);

    List<RoleImportJobItem> findByJobIdAndPhaseAndStatusOrderByRowNumberAsc(
            UUID jobId,
            RoleImportItemPhase phase,
            RoleImportItemStatus status,
            Pageable pageable);

    List<RoleImportJobItem> findByJobIdAndPhaseAndStatusAndRowNumberGreaterThanOrderByRowNumberAsc(
            UUID jobId,
            RoleImportItemPhase phase,
            RoleImportItemStatus status,
            int rowNumber,
            Pageable pageable);

    List<RoleImportJobItem> findByJobIdAndStatusOrderByRowNumberAsc(UUID jobId, RoleImportItemStatus status);

    List<RoleImportJobItem> findByJobIdAndExternalRefOrderByRowNumberAsc(UUID jobId, String externalRef);

    long countByJobIdAndStatus(UUID jobId, RoleImportItemStatus status);

    long countByJobIdAndRoleIdIsNotNull(UUID jobId);

    long countByJobIdAndStatusAndRoleIdIsNotNull(UUID jobId, RoleImportItemStatus status);
}

