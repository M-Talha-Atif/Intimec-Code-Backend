package com.aidentx.iam.organization_management.service;

import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgAssignmentCreateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgAssignmentDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgDelegateCreateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgDelegateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgHistoryDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitCreateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitUpdateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.BulkImportRequest;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.BulkImportResponse;
import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import com.aidentx.iam.organization_management.model.enums.OrgUnitStatus;
import com.aidentx.iam.organization_management.model.enums.OrgUnitType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Public surface of the Organization Management module.
 */
public interface OrgUnitService {

    // --- CRUD ---

    OrgUnitDto create(String tenantId, OrgUnitCreateDto dto);

    OrgUnitDto update(String tenantId, UUID id, OrgUnitUpdateDto dto);

    OrgUnitDto findById(String tenantId, UUID id);

    Page<OrgUnitDto> search(String tenantId,
                            HierarchyType hierarchyType,
                            OrgUnitType type,
                            OrgUnitStatus status,
                            String search,
                            Pageable pageable);

    void softDelete(String tenantId, UUID id);

    /**
     * Bulk import a list of org units from a CSV/JSON feed. The list is
     * topologically sorted by {@code parentExternalId} so parents are inserted
     * before their children. When {@code dryRun} is true no rows are written;
     * the response contains validation results only.
     */
    BulkImportResponse bulkImport(String tenantId, BulkImportRequest request, boolean dryRun);

    // --- Tree ---

    List<OrgUnitDto> getTree(String tenantId, HierarchyType hierarchyType, UUID rootId);

    List<OrgUnitDto> getSubtree(String tenantId, UUID id, Integer maxDepth);

    List<OrgUnitDto> getAncestors(String tenantId, UUID id);

    List<OrgUnitDto> getChildren(String tenantId, UUID id);

    OrgUnitDto move(String tenantId, UUID id, UUID newParentId);

    // --- History ---

    Page<OrgHistoryDto> getHistory(String tenantId, UUID id, Pageable pageable);

    // --- Delegates ---

    List<OrgDelegateDto> listDelegates(String tenantId, UUID orgUnitId);

    OrgDelegateDto addDelegate(String tenantId, UUID orgUnitId, OrgDelegateCreateDto dto);

    void revokeDelegate(String tenantId, UUID orgUnitId, UUID delegateId);

    // --- Assignments ---

    List<OrgAssignmentDto> listAssignments(String tenantId, UUID orgUnitId);

    OrgAssignmentDto addAssignment(String tenantId, UUID orgUnitId, OrgAssignmentCreateDto dto);

    void removeAssignment(String tenantId, UUID orgUnitId, UUID identityId, HierarchyType hierarchyType);

    List<OrgAssignmentDto> listAssignmentsForIdentity(String tenantId, UUID identityId);
}
