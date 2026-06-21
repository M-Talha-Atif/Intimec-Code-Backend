package com.aidentx.iam.organization_management.service;

import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgAssignmentCreateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgAssignmentDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgDelegateCreateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgDelegateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgHistoryDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitCreateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.OrgUnitUpdateDto;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.BulkImportItem;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.BulkImportRequest;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.BulkImportResponse;
import com.aidentx.iam.organization_management.model.dto.OrgDtos.BulkImportRowResult;
import com.aidentx.iam.organization_management.model.entity.IdentityOrgAssignment;
import com.aidentx.iam.organization_management.model.entity.OrgUnit;
import com.aidentx.iam.organization_management.model.entity.OrgUnitDelegate;
import com.aidentx.iam.organization_management.model.entity.OrgUnitHistory;
import com.aidentx.iam.organization_management.model.enums.AssignmentType;
import com.aidentx.iam.organization_management.model.enums.HierarchyType;
import com.aidentx.iam.organization_management.model.enums.OrgEventType;
import com.aidentx.iam.organization_management.model.enums.OrgUnitStatus;
import com.aidentx.iam.organization_management.model.enums.OrgUnitType;
import com.aidentx.iam.organization_management.repository.IdentityOrgAssignmentRepository;
import com.aidentx.iam.organization_management.repository.OrgUnitDelegateRepository;
import com.aidentx.iam.organization_management.repository.OrgUnitHistoryRepository;
import com.aidentx.iam.organization_management.repository.OrgUnitRepository;
import com.aidentx.iam.identity_management.repository.EmployeeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class OrgUnitServiceImpl implements OrgUnitService {

    private final OrgUnitRepository orgUnitRepository;
    private final IdentityOrgAssignmentRepository assignmentRepository;
    private final OrgUnitDelegateRepository delegateRepository;
    private final OrgUnitHistoryRepository historyRepository;
    private final OrgAggregateService aggregateService;
    private final OrgUnitMapper mapper;
    private final ObjectMapper objectMapper;
    private final OrgHistoryService orgHistoryService;
    private final EmployeeRepository employeeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public OrgUnitServiceImpl(OrgUnitRepository orgUnitRepository,
                              IdentityOrgAssignmentRepository assignmentRepository,
                              OrgUnitDelegateRepository delegateRepository,
                              OrgUnitHistoryRepository historyRepository,
                              OrgAggregateService aggregateService,
                              OrgUnitMapper mapper,
                              ObjectMapper objectMapper,
                              OrgHistoryService orgHistoryService,
                              EmployeeRepository employeeRepository) {
        this.orgUnitRepository = orgUnitRepository;
        this.assignmentRepository = assignmentRepository;
        this.delegateRepository = delegateRepository;
        this.historyRepository = historyRepository;
        this.aggregateService = aggregateService;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.orgHistoryService = orgHistoryService;
        this.employeeRepository = employeeRepository;
    }

    // ====================================================================
    // CRUD
    // ====================================================================

    @Override
    @Transactional
    public OrgUnitDto create(String tenantId, OrgUnitCreateDto dto) {
        if (dto == null || dto.name() == null || dto.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }
        if (dto.type() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type is required");
        }

        HierarchyType hierarchyType = dto.hierarchyType() != null
                ? dto.hierarchyType() : HierarchyType.FUNCTIONAL;

        OrgUnit parent = null;
        if (dto.parentId() != null) {
            parent = orgUnitRepository.findByIdAndTenantId(dto.parentId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Parent org unit not found: " + dto.parentId()));
            if (parent.getHierarchyType() != hierarchyType) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Parent belongs to a different hierarchy");
            }
        }

        UUID parentId = parent != null ? parent.getId() : null;
        if (orgUnitRepository.existsByTenantIdAndParentIdAndNameIgnoreCase(tenantId, parentId, dto.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An org unit with this name already exists under the same parent");
        }

        OrgUnit u = new OrgUnit();
        u.setTenantId(tenantId);
        u.setName(dto.name());
        u.setType(dto.type());
        u.setHierarchyType(hierarchyType);
        u.setCode(dto.code());
        u.setDescription(dto.description());
        u.setParentId(parentId);
        u.setHeadIdentityId(dto.headIdentityId());
        u.setSourceSystem(dto.sourceSystem() != null ? dto.sourceSystem() : "MANUAL");
        u.setExternalId(dto.externalId());

        u.setCostCenter(dto.costCenter());
        u.setLegalEntity(dto.legalEntity());
        u.setRegion(dto.region());
        u.setCountry(dto.country());
        u.setTimeZone(dto.timeZone());
        u.setCurrency(dto.currency());
        u.setLocation(dto.location());
        u.setBuildingCode(dto.buildingCode());
        u.setFloor(dto.floor());
        u.setDataClassification(dto.dataClassification());
        u.setComplianceStatus(dto.complianceStatus());
        u.setCertificationStatus(dto.certificationStatus());
        u.setLastAuditDate(dto.lastAuditDate());
        u.setNextReviewDate(dto.nextReviewDate());

        u.setBudget(dto.budget());
        u.setActualSpend(dto.actualSpend());
        u.setLicenseCost(dto.licenseCost());

        if (dto.tags() != null) u.setTags(new ArrayList<>(dto.tags()));
        if (dto.metadata() != null) u.setMetadata(new HashMap<>(dto.metadata()));

        u.setCreatedBy(currentActor());
        u.setUpdatedBy(currentActor());

        // Set a temporary placeholder so entity-level @NotBlank on path is satisfied
        // during the first flush; path is recomputed from the assigned id afterwards.
        u.setPath("/");
        u.setDepth(0);

        // Persist once to obtain id, then set real path/depth and update.
        u = orgUnitRepository.saveAndFlush(u);
        String path = parent != null
                ? OrgPathService.childPath(parent.getPath(), u.getId())
                : OrgPathService.rootPath(u.getId());
        u.setPath(path);
        u.setDepth(parent != null ? parent.getDepth() + 1 : 0);
        // Use saveAndFlush so the UPDATE is committed before recordHistory triggers its own flush.
        u = orgUnitRepository.saveAndFlush(u);

        // Compute the DTO BEFORE recordHistory so the aggregate SELECT runs in a clean session
        // before the REQUIRES_NEW inner transaction in OrgHistoryService can affect session state.
        OrgUnitDto result = toDtoFlat(u);
        recordHistory(u, OrgEventType.CREATED, "Created", null, null);
        return result;
    }

    @Override
    @Transactional
    public OrgUnitDto update(String tenantId, UUID id, OrgUnitUpdateDto dto) {
        OrgUnit u = loadOrThrow(tenantId, id);
        JsonNode before = snapshot(u);
        boolean renamed = false;

        if (dto.name() != null && !dto.name().isBlank() && !dto.name().equals(u.getName())) {
            if (orgUnitRepository.existsByTenantIdAndParentIdAndNameIgnoreCase(tenantId, u.getParentId(), dto.name())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An org unit with this name already exists under the same parent");
            }
            u.setName(dto.name());
            renamed = true;
        }
        if (dto.type() != null) u.setType(dto.type());
        if (dto.code() != null) u.setCode(dto.code());
        if (dto.description() != null) u.setDescription(dto.description());
        if (dto.headIdentityId() != null) u.setHeadIdentityId(dto.headIdentityId());
        if (dto.status() != null) u.setStatus(dto.status());

        if (dto.costCenter() != null) u.setCostCenter(dto.costCenter());
        if (dto.legalEntity() != null) u.setLegalEntity(dto.legalEntity());
        if (dto.region() != null) u.setRegion(dto.region());
        if (dto.country() != null) u.setCountry(dto.country());
        if (dto.timeZone() != null) u.setTimeZone(dto.timeZone());
        if (dto.currency() != null) u.setCurrency(dto.currency());
        if (dto.location() != null) u.setLocation(dto.location());
        if (dto.buildingCode() != null) u.setBuildingCode(dto.buildingCode());
        if (dto.floor() != null) u.setFloor(dto.floor());
        if (dto.dataClassification() != null) u.setDataClassification(dto.dataClassification());
        if (dto.complianceStatus() != null) u.setComplianceStatus(dto.complianceStatus());
        if (dto.certificationStatus() != null) u.setCertificationStatus(dto.certificationStatus());
        if (dto.lastAuditDate() != null) u.setLastAuditDate(dto.lastAuditDate());
        if (dto.nextReviewDate() != null) u.setNextReviewDate(dto.nextReviewDate());

        if (dto.budget() != null) u.setBudget(dto.budget());
        if (dto.actualSpend() != null) u.setActualSpend(dto.actualSpend());
        if (dto.licenseCost() != null) u.setLicenseCost(dto.licenseCost());

        if (dto.tags() != null) u.setTags(new ArrayList<>(dto.tags()));
        if (dto.metadata() != null) u.setMetadata(new HashMap<>(dto.metadata()));

        u.setUpdatedBy(currentActor());
        u = orgUnitRepository.save(u);

        recordHistory(u, renamed ? OrgEventType.RENAMED : OrgEventType.UPDATED,
                renamed ? "Renamed" : "Updated", before, snapshot(u));

        return toDtoFlat(u);
    }

    @Override
    @Transactional(readOnly = true)
    public OrgUnitDto findById(String tenantId, UUID id) {
        OrgUnit u = loadOrThrow(tenantId, id);
        return toDtoWithDelegates(u);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrgUnitDto> search(String tenantId,
                                   HierarchyType hierarchyType,
                                   OrgUnitType type,
                                   OrgUnitStatus status,
                                   String search,
                                   Pageable pageable) {
        Page<OrgUnit> page = orgUnitRepository.search(tenantId, hierarchyType, type, status, search, pageable);
        return page.map(this::toDtoFlat);
    }

    @Override
    @Transactional
    public void softDelete(String tenantId, UUID id) {
        OrgUnit u = loadOrThrow(tenantId, id);
        if (orgUnitRepository.countActiveByTenantIdAndParentId(tenantId, id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete an org unit that has children. Move or delete children first.");
        }
        if (assignmentRepository.countByTenantIdAndOrgUnitId(tenantId, id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cannot delete an org unit that still has identity assignments.");
        }
        u.setStatus(OrgUnitStatus.INACTIVE);
        u.setValidTo(Instant.now());
        u.setUpdatedBy(currentActor());
        orgUnitRepository.save(u);
        recordHistory(u, OrgEventType.DEACTIVATED, "Deactivated", null, snapshot(u));
    }

    // ====================================================================
    // Tree
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrgUnitDto> getTree(String tenantId, HierarchyType hierarchyType, UUID rootId) {
        HierarchyType ht = hierarchyType != null ? hierarchyType : HierarchyType.FUNCTIONAL;
        List<OrgUnit> all = orgUnitRepository.findAllForHierarchy(tenantId, ht);
        return assembleTree(all, rootId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrgUnitDto> getSubtree(String tenantId, UUID id, Integer maxDepth) {
        OrgUnit root = loadOrThrow(tenantId, id);
        List<OrgUnit> subtree = orgUnitRepository.findSubtree(tenantId, root.getPath());
        if (maxDepth != null) {
            int cutoff = root.getDepth() + maxDepth;
            subtree.removeIf(u -> u.getDepth() > cutoff);
        }
        return assembleTree(subtree, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrgUnitDto> getAncestors(String tenantId, UUID id) {
        OrgUnit self = loadOrThrow(tenantId, id);
        List<UUID> ancestorIds = parsePathIds(self.getPath());
        ancestorIds.remove(self.getId()); // exclude self
        if (ancestorIds.isEmpty()) return List.of();
        Map<UUID, OrgUnit> byId = new HashMap<>();
        orgUnitRepository.findAllById(ancestorIds).forEach(u -> byId.put(u.getId(), u));
        List<OrgUnitDto> result = new ArrayList<>();
        for (UUID aid : ancestorIds) {
            OrgUnit u = byId.get(aid);
            if (u != null && tenantId.equals(u.getTenantId())) {
                result.add(toDtoFlat(u));
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrgUnitDto> getChildren(String tenantId, UUID id) {
        loadOrThrow(tenantId, id); // existence check
        return orgUnitRepository.findByTenantIdAndParentIdOrderByNameAsc(tenantId, id).stream()
                .map(this::toDtoFlat).toList();
    }

    @Override
    @Transactional
    public OrgUnitDto move(String tenantId, UUID id, UUID newParentId) {
        OrgUnit u = loadOrThrow(tenantId, id);
        OrgUnit newParent = null;
        if (newParentId != null) {
            newParent = orgUnitRepository.findByIdAndTenantId(newParentId, tenantId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "New parent not found: " + newParentId));
            if (newParent.getHierarchyType() != u.getHierarchyType()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot move into a different hierarchy");
            }
            // Cycle prevention: new parent must not be inside self's subtree.
            if (newParent.getPath() != null && newParent.getPath().startsWith(u.getPath())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cannot move a unit into its own subtree");
            }
        }

        JsonNode before = snapshot(u);
        String oldPath = u.getPath();
        String newSelfPath = newParent != null
                ? OrgPathService.childPath(newParent.getPath(), u.getId())
                : OrgPathService.rootPath(u.getId());

        int oldDepth = u.getDepth();
        int newDepth = newParent != null ? newParent.getDepth() + 1 : 0;
        int depthDelta = newDepth - oldDepth;

        // Atomically rewrite paths/depths of self + every descendant.
        orgUnitRepository.rewriteSubtreePaths(tenantId, oldPath, newSelfPath, depthDelta, oldPath + "%");

        u.setParentId(newParent != null ? newParent.getId() : null);
        u.setUpdatedBy(currentActor());
        // Reload to pick up the bulk-updated path/depth
        OrgUnit reloaded = loadOrThrow(tenantId, id);
        recordHistory(reloaded, OrgEventType.MOVED, "Moved",
                before, snapshot(reloaded));
        return toDtoFlat(reloaded);
    }

    // ====================================================================
    // History
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<OrgHistoryDto> getHistory(String tenantId, UUID id, Pageable pageable) {
        loadOrThrow(tenantId, id);
        return historyRepository
                .findByTenantIdAndOrgUnitIdOrderByOccurredAtDesc(tenantId, id, pageable)
                .map(h -> new OrgHistoryDto(
                        h.getId(),
                        h.getOrgUnitId(),
                        h.getEventType(),
                        h.getActionLabel(),
                        h.getDetails(),
                        h.getActorIdentityId(),
                        h.getActorUsername(),
                        h.getOccurredAt(),
                        h.getBeforeState(),
                        h.getAfterState()
                ));
    }

    // ====================================================================
    // Delegates
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrgDelegateDto> listDelegates(String tenantId, UUID orgUnitId) {
        loadOrThrow(tenantId, orgUnitId);
        return delegateRepository
                .findByTenantIdAndOrgUnitIdAndRevokedAtIsNull(tenantId, orgUnitId)
                .stream().map(mapper::toDelegateDto).toList();
    }

    @Override
    @Transactional
    public OrgDelegateDto addDelegate(String tenantId, UUID orgUnitId, OrgDelegateCreateDto dto) {
        OrgUnit u = loadOrThrow(tenantId, orgUnitId);
        OrgUnitDelegate d = new OrgUnitDelegate();
        d.setTenantId(tenantId);
        d.setOrgUnitId(orgUnitId);
        d.setIdentityId(dto.identityId());
        d.setIdentityDisplayName(dto.name());
        d.setDelegateRole(dto.role());
        d.setExpiresAt(dto.expiresAt());
        d = delegateRepository.save(d);
        recordHistory(u, OrgEventType.DELEGATE_ADDED,
                "Delegate added", null, objectMapper.valueToTree(mapper.toDelegateDto(d)));
        return mapper.toDelegateDto(d);
    }

    @Override
    @Transactional
    public void revokeDelegate(String tenantId, UUID orgUnitId, UUID delegateId) {
        OrgUnit u = loadOrThrow(tenantId, orgUnitId);
        OrgUnitDelegate d = delegateRepository.findById(delegateId)
                .filter(x -> tenantId.equals(x.getTenantId()) && orgUnitId.equals(x.getOrgUnitId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delegate not found"));
        d.setRevokedAt(Instant.now());
        delegateRepository.save(d);
        recordHistory(u, OrgEventType.DELEGATE_REVOKED, "Delegate revoked",
                objectMapper.valueToTree(mapper.toDelegateDto(d)), null);
    }

    // ====================================================================
    // Identity assignments
    // ====================================================================

    @Override
    @Transactional(readOnly = true)
    public List<OrgAssignmentDto> listAssignments(String tenantId, UUID orgUnitId) {
        OrgUnit u = loadOrThrow(tenantId, orgUnitId);
        List<IdentityOrgAssignment> rows = assignmentRepository.findByTenantIdAndOrgUnitId(tenantId, orgUnitId);
        // Batch-resolve identity display names
        List<UUID> identityIds = rows.stream().map(IdentityOrgAssignment::getIdentityId).distinct().toList();
        Map<UUID, String> displayNameById = employeeRepository.findAllById(identityIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        com.aidentx.iam.identity_management.model.entity.HumanIdentity::getId,
                        emp -> {
                            String dn = emp.getDisplayName();
                            if (dn != null && !dn.isBlank()) return dn;
                            String full = (emp.getFirstName() + " " + emp.getLastName()).trim();
                            return full.isBlank() ? emp.getId().toString() : full;
                        }));
        return rows.stream()
                .map(a -> toAssignmentDto(a, u.getName(), displayNameById.get(a.getIdentityId())))
                .toList();
    }

    @Override
    @Transactional
    public OrgAssignmentDto addAssignment(String tenantId, UUID orgUnitId, OrgAssignmentCreateDto dto) {
        OrgUnit u = loadOrThrow(tenantId, orgUnitId);
        HierarchyType ht = dto.hierarchyType() != null ? dto.hierarchyType() : HierarchyType.FUNCTIONAL;
        AssignmentType at = dto.assignmentType() != null ? dto.assignmentType() : AssignmentType.HR_OF_RECORD;
        boolean primary = Boolean.TRUE.equals(dto.primary());

        // If marking primary, unset any existing primary for the (identity, hierarchy)
        if (primary) {
            assignmentRepository
                    .findByTenantIdAndIdentityIdAndHierarchyTypeAndPrimaryTrue(tenantId, dto.identityId(), ht)
                    .ifPresent(existing -> {
                        existing.setPrimary(false);
                        assignmentRepository.save(existing);
                    });
        }

        IdentityOrgAssignment a = new IdentityOrgAssignment();
        a.setTenantId(tenantId);
        a.setIdentityId(dto.identityId());
        a.setOrgUnitId(orgUnitId);
        a.setHierarchyType(ht);
        a.setAssignmentType(at);
        a.setPrimary(primary);
        a.setValidFrom(dto.validFrom() != null ? dto.validFrom() : Instant.now());
        a.setValidTo(dto.validTo());
        a.setCreatedBy(currentActor());
        a.setUpdatedBy(currentActor());
        a = assignmentRepository.save(a);

        recordHistory(u, OrgEventType.ASSIGNMENT_ADDED, "Identity assigned",
                null, objectMapper.valueToTree(toAssignmentDto(a, u.getName())));
        return toAssignmentDto(a, u.getName());
    }

    @Override
    @Transactional
    public void removeAssignment(String tenantId, UUID orgUnitId, UUID identityId, HierarchyType hierarchyType) {
        OrgUnit u = loadOrThrow(tenantId, orgUnitId);
        HierarchyType ht = hierarchyType != null ? hierarchyType : HierarchyType.FUNCTIONAL;
        assignmentRepository.deleteByTenantIdAndOrgUnitIdAndIdentityIdAndHierarchyType(
                tenantId, orgUnitId, identityId, ht);
        recordHistory(u, OrgEventType.ASSIGNMENT_REMOVED, "Identity unassigned", null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrgAssignmentDto> listAssignmentsForIdentity(String tenantId, UUID identityId) {
        List<IdentityOrgAssignment> rows = assignmentRepository.findByTenantIdAndIdentityId(tenantId, identityId);
        Map<UUID, String> nameByOrg = new HashMap<>();
        rows.forEach(a -> nameByOrg.computeIfAbsent(a.getOrgUnitId(),
                oid -> orgUnitRepository.findById(oid).map(OrgUnit::getName).orElse("")));
        return rows.stream().map(a -> toAssignmentDto(a, nameByOrg.get(a.getOrgUnitId()))).toList();
    }

    // ====================================================================
    // Bulk import
    // ====================================================================

    @Override
    @Transactional
    public BulkImportResponse bulkImport(String tenantId, BulkImportRequest request, boolean dryRun) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Import payload is empty");
        }

        List<BulkImportItem> items = request.items();
        HierarchyType defaultHierarchy = request.defaultHierarchyType() != null
                ? request.defaultHierarchyType() : HierarchyType.FUNCTIONAL;

        // Optional anchor unit: imported roots become children of this unit.
        OrgUnit anchor = null;
        if (request.rootParentId() != null) {
            anchor = orgUnitRepository.findByIdAndTenantId(request.rootParentId(), tenantId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "rootParentId not found: " + request.rootParentId()));
        }

        // ---- Phase 1: pre-validate (no DB writes) ----
        List<BulkImportRowResult> errors = new ArrayList<>();
        Map<String, BulkImportItem> byExternalId = new LinkedHashMap<>();
        Set<String> codesSeen = new HashSet<>();

        for (int i = 0; i < items.size(); i++) {
            BulkImportItem it = items.get(i);
            String ext = it.externalId();
            String tag = ext != null ? ext : ("row#" + (i + 1));
            if (ext == null || ext.isBlank()) {
                errors.add(new BulkImportRowResult(tag, "failed", null, "externalId is required"));
                continue;
            }
            if (byExternalId.containsKey(ext)) {
                errors.add(new BulkImportRowResult(tag, "failed", null, "Duplicate externalId in file"));
                continue;
            }
            if (it.name() == null || it.name().isBlank()) {
                errors.add(new BulkImportRowResult(tag, "failed", null, "name is required"));
                continue;
            }
            if (it.type() == null) {
                errors.add(new BulkImportRowResult(tag, "failed", null, "type is required"));
                continue;
            }
            if (it.code() != null && !it.code().isBlank()) {
                if (!codesSeen.add(it.code())) {
                    errors.add(new BulkImportRowResult(tag, "failed", null,
                            "Duplicate code in file: " + it.code()));
                    continue;
                }
            }
            byExternalId.put(ext, it);
        }

        // Parent-reference resolution (must point inside the file).
        for (BulkImportItem it : byExternalId.values()) {
            String pe = it.parentExternalId();
            if (pe == null || pe.isBlank()) continue;
            if (!byExternalId.containsKey(pe)) {
                errors.add(new BulkImportRowResult(it.externalId(), "failed", null,
                        "parentExternalId '" + pe + "' not found in file"));
            }
            if (pe.equals(it.externalId())) {
                errors.add(new BulkImportRowResult(it.externalId(), "failed", null,
                        "parentExternalId cannot equal externalId"));
            }
        }

        // Topological sort (Kahn's). Detect cycles.
        List<BulkImportItem> sorted = new ArrayList<>();
        if (errors.isEmpty()) {
            Map<String, Integer> inDegree = new HashMap<>();
            Map<String, List<String>> children = new HashMap<>();
            for (BulkImportItem it : byExternalId.values()) {
                inDegree.put(it.externalId(), 0);
                children.put(it.externalId(), new ArrayList<>());
            }
            for (BulkImportItem it : byExternalId.values()) {
                String pe = it.parentExternalId();
                if (pe != null && !pe.isBlank() && byExternalId.containsKey(pe)) {
                    children.get(pe).add(it.externalId());
                    inDegree.merge(it.externalId(), 1, Integer::sum);
                }
            }
            Deque<String> queue = new ArrayDeque<>();
            inDegree.forEach((k, v) -> { if (v == 0) queue.add(k); });
            while (!queue.isEmpty()) {
                String n = queue.poll();
                sorted.add(byExternalId.get(n));
                for (String c : children.get(n)) {
                    if (inDegree.merge(c, -1, Integer::sum) == 0) queue.add(c);
                }
            }
            if (sorted.size() < byExternalId.size()) {
                Set<String> sortedIds = new HashSet<>();
                sorted.forEach(s -> sortedIds.add(s.externalId()));
                for (String ext : byExternalId.keySet()) {
                    if (!sortedIds.contains(ext)) {
                        errors.add(new BulkImportRowResult(ext, "failed", null,
                                "Cycle detected involving this row"));
                    }
                }
            }
        }

        // ---- Phase 2: short-circuit on validation failure or dry-run ----
        if (!errors.isEmpty()) {
            return new BulkImportResponse(dryRun, items.size(), 0, 0, errors.size(), errors);
        }
        if (dryRun) {
            List<BulkImportRowResult> ok = new ArrayList<>(sorted.size());
            for (BulkImportItem it : sorted) {
                ok.add(new BulkImportRowResult(it.externalId(), "validated", null, null));
            }
            return new BulkImportResponse(true, items.size(), 0, ok.size(), 0, ok);
        }

        // ---- Phase 3: insert in topological order ----
        Map<String, UUID> idByExternal = new HashMap<>();
        List<BulkImportRowResult> results = new ArrayList<>(sorted.size());

        for (BulkImportItem it : sorted) {
            HierarchyType ht = it.hierarchyType() != null ? it.hierarchyType() : defaultHierarchy;
            UUID effectiveParentId;
            if (it.parentExternalId() != null && !it.parentExternalId().isBlank()) {
                effectiveParentId = idByExternal.get(it.parentExternalId());
            } else {
                effectiveParentId = anchor != null ? anchor.getId() : null;
            }

            // Resolve head employee-id → identity UUID (best-effort; import proceeds if not found)
            UUID headId = null;
            String headDisplayName = null;
            if (it.headExternalId() != null && !it.headExternalId().isBlank()) {
                var headOpt = employeeRepository.findByTenantIdAndEmployeeId(tenantId, it.headExternalId());
                if (headOpt.isPresent()) {
                    headId = headOpt.get().getId();
                    headDisplayName = headOpt.get().getDisplayName();
                    if (headDisplayName == null || headDisplayName.isBlank()) {
                        headDisplayName = (headOpt.get().getFirstName() + " " + headOpt.get().getLastName()).trim();
                    }
                }
            }
            final UUID finalHeadId = headId;
            OrgUnitCreateDto createDto = new OrgUnitCreateDto(
                    it.name(), it.type(), effectiveParentId, ht,
                    it.code(), it.description(), finalHeadId,
                    it.costCenter(), null, it.region(), it.country(), null, null,
                    it.location(), null, null, null, null, null, null, null,
                    null, null, null,
                    null, null,
                    it.externalId(), "IMPORT"
            );
            try {
                OrgUnitDto created = create(tenantId, createDto);
                // If we resolved a head, backfill the cached display name (create() doesn't populate it)
                if (finalHeadId != null && headDisplayName != null) {
                    final String finalHeadDisplayName = headDisplayName;
                    orgUnitRepository.findByIdAndTenantId(created.id(), tenantId).ifPresent(orgUnit -> {
                        orgUnit.setHeadDisplayName(finalHeadDisplayName);
                        orgUnitRepository.save(orgUnit);
                    });
                }
                if (it.status() != null && it.status() != OrgUnitStatus.ACTIVE) {
                    OrgUnit u = loadOrThrow(tenantId, created.id());
                    u.setStatus(it.status());
                    orgUnitRepository.save(u);
                }
                idByExternal.put(it.externalId(), created.id());
                results.add(new BulkImportRowResult(it.externalId(), "created", created.id(), null));
            } catch (ResponseStatusException ex) {
                String msg = ex.getReason() != null ? ex.getReason() : ex.getMessage();
                results.add(new BulkImportRowResult(it.externalId(), "failed", null, msg));
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Bulk import failed at row '" + it.externalId() + "': " + msg, ex);
            }
        }

        return new BulkImportResponse(false, items.size(), results.size(), 0, 0, results);
    }

    // ====================================================================
    // Internals
    // ====================================================================

    private OrgUnit loadOrThrow(String tenantId, UUID id) {
        return orgUnitRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Org unit not found: " + id));
    }

    private OrgUnitDto toDtoFlat(OrgUnit u) {
        return mapper.toDto(u, aggregateService.computeAggregates(u), null, null);
    }

    private OrgUnitDto toDtoWithDelegates(OrgUnit u) {
        List<OrgDelegateDto> dgs = delegateRepository
                .findByTenantIdAndOrgUnitIdAndRevokedAtIsNull(u.getTenantId(), u.getId())
                .stream().map(mapper::toDelegateDto).toList();
        return mapper.toDto(u, aggregateService.computeAggregates(u), dgs, null);
    }

    /**
     * Build a tree from a flat list. If {@code rootId} is non-null only the
     * subtree rooted at that id is returned; otherwise every top-level node
     * (within the supplied list) is returned.
     */
    private List<OrgUnitDto> assembleTree(List<OrgUnit> units, UUID rootId) {
        if (units == null || units.isEmpty()) return List.of();

        // Sort by depth/path so parents are processed before children.
        units.sort(Comparator.comparingInt(OrgUnit::getDepth)
                .thenComparing(OrgUnit::getPath, Comparator.nullsLast(Comparator.naturalOrder())));

        Map<UUID, OrgUnitDtoBuilder> nodes = new HashMap<>();
        List<OrgUnitDtoBuilder> roots = new ArrayList<>();

        for (OrgUnit u : units) {
            OrgAggregateService.OrgAggregates agg = aggregateService.computeAggregates(u);
            OrgUnitDtoBuilder b = new OrgUnitDtoBuilder(u, agg);
            nodes.put(u.getId(), b);
            if (rootId != null) {
                if (u.getId().equals(rootId)) {
                    roots.add(b);
                } else {
                    OrgUnitDtoBuilder p = nodes.get(u.getParentId());
                    if (p != null) p.children.add(b);
                }
            } else {
                OrgUnitDtoBuilder p = u.getParentId() != null ? nodes.get(u.getParentId()) : null;
                if (p != null) p.children.add(b);
                else roots.add(b);
            }
        }
        return roots.stream().map(this::buildDto).toList();
    }

    private OrgUnitDto buildDto(OrgUnitDtoBuilder b) {
        List<OrgUnitDto> children = b.children.stream().map(this::buildDto).toList();
        return mapper.toDto(b.unit, b.agg, null,
                children.isEmpty() ? null : children);
    }

    private static class OrgUnitDtoBuilder {
        final OrgUnit unit;
        final OrgAggregateService.OrgAggregates agg;
        final List<OrgUnitDtoBuilder> children = new ArrayList<>();
        OrgUnitDtoBuilder(OrgUnit u, OrgAggregateService.OrgAggregates agg) {
            this.unit = u;
            this.agg = agg;
        }
    }

    private OrgAssignmentDto toAssignmentDto(IdentityOrgAssignment a, String orgName) {
        return toAssignmentDto(a, orgName, null);
    }

    private OrgAssignmentDto toAssignmentDto(IdentityOrgAssignment a, String orgName, String identityDisplayName) {
        return new OrgAssignmentDto(
                a.getId(),
                a.getIdentityId(),
                identityDisplayName,
                a.getOrgUnitId(),
                orgName,
                a.getHierarchyType(),
                a.getAssignmentType(),
                a.isPrimary(),
                a.getValidFrom(),
                a.getValidTo()
        );
    }

    private void recordHistory(OrgUnit u, OrgEventType type, String label,
                               JsonNode before, JsonNode after) {
        try {
            orgHistoryService.record(u.getTenantId(), u.getId(), type, label, currentActor(), before, after);
        } catch (Exception ignored) {
            // History is best-effort; the REQUIRES_NEW transaction in OrgHistoryService
            // isolates any failure from the caller's transaction.
        }
    }

    private JsonNode snapshot(OrgUnit u) {
        try {
            return objectMapper.valueToTree(toDtoFlat(u));
        } catch (Exception e) {
            return null;
        }
    }

    private List<UUID> parsePathIds(String path) {
        List<UUID> ids = new ArrayList<>();
        if (path == null) return ids;
        for (String part : path.split("/")) {
            if (part.isBlank()) continue;
            try { ids.add(UUID.fromString(part)); } catch (IllegalArgumentException ignore) {}
        }
        return ids;
    }

    private String currentActor() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                String name = auth.getName();
                if (name != null && !"anonymousUser".equals(name)) return name;
            }
        } catch (Exception ignored) {}
        return "system";
    }
}
