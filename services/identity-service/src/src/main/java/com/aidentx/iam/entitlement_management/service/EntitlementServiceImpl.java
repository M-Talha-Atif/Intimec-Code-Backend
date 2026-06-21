package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.entitlement_management.audit.AuditEntitlementGovernance;
import com.aidentx.iam.entitlement_management.audit.EntitlementManualSyncAuditService;
import com.aidentx.iam.entitlement_management.client.ApplicationClient;
import com.aidentx.iam.entitlement_management.config.constants.ApplicationClientConstants;
import com.aidentx.iam.entitlement_management.config.constants.EntitlementAssignmentSourceConstants;
import com.aidentx.iam.entitlement_management.config.constants.EntitlementMemberConstants;
import com.aidentx.iam.entitlement_management.mapper.EntitlementMapper;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementMemberDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementResponseDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementSyncDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementUpdateDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.FilterOptionDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.FilterValueDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ManualEntitlementAssignmentRequest;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ManualEntitlementAssignmentResponse;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ManualEntitlementAssignmentResult;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.ShadowEntitlementDto;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncEntitlementMemberFailure;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncEntitlementMemberResult;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncManualEntitlementMembersRequest;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.SyncManualEntitlementMembersResponse;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignment;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.identity_management.model.dto.IdentityEntitlementRolePathNodeDto;
import com.aidentx.iam.identity_management.model.dto.IdentityAccessBreakdownDto;
import com.aidentx.iam.identity_management.model.dto.IdentityAccessEntitlementDto;
import com.aidentx.iam.identity_management.model.dto.IdentityAccessRoleGroupDto;
import com.aidentx.iam.identity_management.model.dto.IdentityEntitlementViewDto;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.iam.shadow_management.sync.EntitlementMappingEngine;
import com.aidentx.iam.shadow_management.sync.EntitlementMappingEngine.EntitlementMappingResult;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Production implementation of {@link EntitlementService}.
 *
 * <p>Handles the full entitlement lifecycle:
 * <ul>
 *   <li><b>Sync pipeline</b> ├óÔé¼ÔÇØ createOrUpdateFromSync, membership processing,
 *       deletion detection via finalizeEntitlementSyncRun</li>
 *   <li><b>Governance projection</b> ├óÔé¼ÔÇØ invokes {@link EntitlementMappingEngine} to
 *       derive governance metadata (riskLevel, lifecycle, classification, ...) from
 *       raw connector attributes using admin-configured mapping rules</li>
 *   <li><b>Catalog API</b> ├óÔé¼ÔÇØ search, filter, pagination, member enumeration,
 *       assignmentSource / assignedUsers range filters</li>
 *   <li><b>Performance</b> ├óÔé¼ÔÇØ G1-G4 optimisations: pre-loaded catalog maps,
 *       assignment-key sets, batch upserts, bulk deletes</li>
 * </ul>
 */
@Service
public class EntitlementServiceImpl implements EntitlementService {

    private final EntitlementRepository entitlementRepository;
    private final EntitlementAssignmentRepository assignmentRepository;
    private final EntitlementMapper entitlementMapper;
    private final EntitlementMappingEngine mappingEngine;
    private final ShadowRepository shadowRepository;
    private final IdentityRepository identityRepository;
    private final ApplicationClient applicationClient;
    private final EntitlementSchemaService entitlementSchemaService;
    private final AccessRoleRepository accessRoleRepository;
    private final IdentityRoleAssignmentRepository identityRoleAssignmentRepository;
    private final AccessRoleAssignmentRuleRepository accessRoleAssignmentRuleRepository;
    private final RoleEntitlementClosureRepository roleEntitlementClosureRepository;
    private final EntitlementManualSyncAuditService manualSyncAuditService;
    private final IUnifiedLogger logger;

    public EntitlementServiceImpl(EntitlementRepository entitlementRepository,
                                  EntitlementAssignmentRepository assignmentRepository,
                                  EntitlementMapper entitlementMapper,
                                  EntitlementMappingEngine mappingEngine,
                                  ShadowRepository shadowRepository,
                                   IdentityRepository identityRepository,
                                  ApplicationClient applicationClient,
                                  EntitlementSchemaService entitlementSchemaService,
                                  AccessRoleRepository accessRoleRepository,
                                   IdentityRoleAssignmentRepository identityRoleAssignmentRepository,
                                  AccessRoleAssignmentRuleRepository accessRoleAssignmentRuleRepository,
                                  RoleEntitlementClosureRepository roleEntitlementClosureRepository,
                                   EntitlementManualSyncAuditService manualSyncAuditService,
                                  UnifiedLoggerFactory loggerFactory) {
        this.entitlementRepository = entitlementRepository;
        this.assignmentRepository = assignmentRepository;
        this.entitlementMapper = entitlementMapper;
        this.mappingEngine = mappingEngine;
        this.shadowRepository = shadowRepository;
        this.identityRepository = identityRepository;
        this.applicationClient = applicationClient;
        this.entitlementSchemaService = entitlementSchemaService;
        this.accessRoleRepository = accessRoleRepository;
        this.identityRoleAssignmentRepository = identityRoleAssignmentRepository;
        this.accessRoleAssignmentRuleRepository = accessRoleAssignmentRuleRepository;
        this.roleEntitlementClosureRepository = roleEntitlementClosureRepository;
        this.manualSyncAuditService = manualSyncAuditService;
        this.logger = loggerFactory.getLogger(EntitlementServiceImpl.class);
    }

    // ===================================================================
    //  Sync Pipeline
    // ===================================================================

    @Override
    @Transactional
    public EntitlementResponseDto createOrUpdateFromSync(EntitlementSyncDto syncDto) {
        Optional<Entitlement> existing = entitlementRepository
                .findByResourceIdAndSourceUidAndTenantId(
                        syncDto.resourceId(), syncDto.sourceUid(), syncDto.tenantId());

        Entitlement entitlement;
        boolean isNew;

        if (existing.isPresent()) {
            entitlement = existing.get();
            isNew = false;

            // Update technical/sync fields only -- governance fields are NOT overwritten
            entitlementMapper.updateEntityFromSyncDto(syncDto, entitlement);
            entitlement.setLastSyncRunId(syncDto.lastSyncRunId());

            // Re-apply governance mapping on every sync so rules stay current
            applyGovernanceMappings(entitlement, syncDto);
        } else {
            entitlement = entitlementMapper.syncDtoToEntity(syncDto);
            isNew = true;

            // Apply governance mappings to populate governance fields from connector attrs
            applyGovernanceMappings(entitlement, syncDto);
        }

        entitlement = entitlementRepository.save(entitlement);

        logger.debug().message(isNew ? "Created entitlement from sync" : "Updated entitlement from sync")
                .detail("entitlementId", entitlement.getId())
                .detail("sourceUid", syncDto.sourceUid())
                .detail("resourceId", syncDto.resourceId())
                .log();

        return toResponseDto(entitlement, 0L);
    }

    @Override
    @Transactional
    public void assignEntitlementToShadow(UUID shadowId, UUID entitlementId, UUID resourceId,
                                          String tenantId, String syncRunId) {
        Optional<EntitlementAssignment> existing =
                assignmentRepository.findByShadowIdAndEntitlementIdAndTenantIdAndSourceIgnoreCase(
                        shadowId, entitlementId, tenantId, EntitlementAssignmentSourceConstants.SOURCE_DIRECT);

        if (existing.isPresent()) {
            EntitlementAssignment assignment = existing.get();
            assignment.setLastSyncRunId(syncRunId);
            assignmentRepository.save(assignment);
        } else {
            EntitlementAssignment assignment = new EntitlementAssignment();
            assignment.setTenantId(tenantId);
            assignment.setShadowId(shadowId);
            assignment.setEntitlementId(entitlementId);
            assignment.setResourceId(resourceId);
            assignment.setSource(EntitlementAssignmentSourceConstants.SOURCE_DIRECT);
            assignment.setLastSyncRunId(syncRunId);
            assignmentRepository.save(assignment);

            logger.debug().message("Created entitlement assignment.")
                    .detail("shadowId", shadowId)
                    .detail("entitlementId", entitlementId)
                    .log();
        }
    }

    @Override
    @Transactional
    public int processAccountMemberships(UUID shadowId, UUID resourceId, String tenantId,
                                         String syncRunId, Map<String, Object> attributes) {
        // Delegate to cached variant with a freshly-loaded catalog (avoids per-ref DB hits)
        Map<String, UUID> catalog = loadEntitlementCatalogMap(resourceId, tenantId);
        return processAccountMembershipsWithCache(shadowId, resourceId, tenantId, syncRunId, attributes, catalog);
    }

    @Override
    @Transactional
    public int processAccountMembershipsWithCache(UUID shadowId, UUID resourceId, String tenantId,
                                                  String syncRunId, Map<String, Object> attributes,
                                                  Map<String, UUID> entitlementCatalog) {
        // G1 optimized: O(1) map lookup instead of DB query per reference
        List<String> memberRefs = extractMembershipReferences(attributes);
        if (memberRefs.isEmpty()) {
            return 0;
        }

        int resolved = 0;
        long now = Instant.now().toEpochMilli();

        for (String ref : memberRefs) {
            UUID entitlementId = entitlementCatalog.get(ref);
            if (entitlementId == null) {
                logger.debug().message("Unresolved membership reference (group not yet cataloged)")
                        .detail("shadowId", shadowId)
                        .detail("memberRef", ref)
                        .detail("resourceId", resourceId)
                        .log();
                continue;
            }

            assignmentRepository.upsertAssignment(
                    UUID.randomUUID(), tenantId, shadowId, entitlementId,
                    resourceId, EntitlementAssignmentSourceConstants.SOURCE_DIRECT, syncRunId, now);
            resolved++;
        }

        if (resolved > 0) {
            logger.debug().message("Processed account memberships (cache-optimized)")
                    .detail("shadowId", shadowId)
                    .detail("resolved", resolved)
                    .detail("total", memberRefs.size())
                    .log();
        }

        return resolved;
    }

    @Override
    @Transactional
    public int[] finalizeEntitlementSyncRun(UUID resourceId, String tenantId, String syncRunId,
                                            boolean skipAssignmentCleanup) {
        logger.debug().message("Starting entitlement reconciliation")
                .detail("resourceId", resourceId)
                .detail("syncRunId", syncRunId)
                .detail("skipAssignmentCleanup", skipAssignmentCleanup)
                .log();

        // Phase 1: Delete assignments tied to absent entitlements (cascade-style, before deleting entitlements)
        int revokedAssignments = 0;
        if (!skipAssignmentCleanup) {
            revokedAssignments += assignmentRepository.deleteAssignmentsForAbsentEntitlements(
                    resourceId, tenantId, syncRunId);

            // Also delete stale assignments (assignments whose lastSyncRunId doesn't match this run)
            // MUST be skipped when importAccounts=false to avoid wiping all prior assignments
            revokedAssignments += assignmentRepository.deleteStaleAssignmentsBulk(
                    resourceId, tenantId, syncRunId);
        }

        // Phase 2: Delete absent entitlements (not seen in this sync run)
        int deletedEntitlements = entitlementRepository.deleteAbsentEntitlements(
                resourceId, tenantId, syncRunId);

        logger.info().message("Entitlement reconciliation completed (bulk optimized)")
                .detail("resourceId", resourceId)
                .detail("deletedEntitlements", deletedEntitlements)
                .detail("revokedAssignments", revokedAssignments)
                .log();

        return new int[]{ deletedEntitlements, revokedAssignments };
    }

    // ===================================================================
    //  B3b: Dry-run preview -- list entitlements that WOULD be deleted
    //  Two overloads: simple (from your branch) + seenUids-aware (from dev branch)
    // ===================================================================

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> previewEntitlementCleanup(UUID resourceId, String tenantId, String syncRunId) {
        return previewEntitlementCleanup(resourceId, tenantId, syncRunId, Collections.emptySet());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> previewEntitlementCleanup(UUID resourceId, String tenantId, String syncRunId,
                                                    Set<String> seenEntitlementUids) {
        // Fetch all entitlements NOT seen in this sync run (capped at 500 for preview safety)
        Page<Entitlement> absentPage = entitlementRepository.findAbsentEntitlements(
                resourceId, tenantId, syncRunId,
                org.springframework.data.domain.PageRequest.of(0, 500));

        List<Object[]> previews = new ArrayList<>();
        for (Entitlement e : absentPage.getContent()) {
            // In dry-run mode, the syncRunId is not stamped. Exclude entitlements
            // that were actually seen (tracked in the dedup cache).
            if (seenEntitlementUids.contains(e.getSourceUid())) {
                continue;
            }
            long assignmentCount = assignmentRepository.countByEntitlementIdAndTenantId(e.getId(), tenantId);
            previews.add(new Object[]{
                    e.getId(), e.getSourceUid(), e.getDisplayName(), e.getObjectClass(), assignmentCount
            });
        }
        return previews;
    }

    // ===================================================================
    //  Performance Optimizations (G1-G5)
    // ===================================================================

    @Override
    @Transactional(readOnly = true)
    public Map<String, UUID> loadEntitlementCatalogMap(UUID resourceId, String tenantId) {
        logger.debug().message("Loading entitlement catalog into memory")
                .detail("resourceId", resourceId)
                .detail("tenantId", tenantId)
                .log();

        List<Object[]> rows = entitlementRepository.findAllIdsAndSourceUids(resourceId, tenantId);
        Map<String, UUID> catalog = new HashMap<>(rows.size());
        for (Object[] row : rows) {
            UUID id = (UUID) row[0];
            String sourceUid = (String) row[1];
            catalog.put(sourceUid, id);
        }

        logger.info().message("Entitlement catalog loaded")
                .detail("resourceId", resourceId)
                .detail("entriesLoaded", catalog.size())
                .log();

        return catalog;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> loadAssignmentKeysSet(UUID resourceId, String tenantId) {
        logger.debug().message("Loading assignment keys into memory")
                .detail("resourceId", resourceId)
                .detail("tenantId", tenantId)
                .log();

        List<Object[]> rows = assignmentRepository.findAllAssignmentKeys(resourceId, tenantId);
        Set<String> keys = new HashSet<>(rows.size());
        for (Object[] row : rows) {
            UUID shadowId = (UUID) row[0];
            UUID entitlementId = (UUID) row[1];
            keys.add(shadowId + "|" + entitlementId);
        }

        logger.info().message("Assignment keys loaded")
                .detail("resourceId", resourceId)
                .detail("keysLoaded", keys.size())
                .log();

        return keys;
    }

    @Override
    @Transactional
    public void batchUpsertAssignments(List<UUID[]> shadowEntitlementPairs, UUID resourceId,
                                       String tenantId, String syncRunId) {
        if (shadowEntitlementPairs == null || shadowEntitlementPairs.isEmpty()) {
            return;
        }

        logger.debug().message("Batch upserting assignments")
                .detail("batchSize", shadowEntitlementPairs.size())
                .log();

        long now = Instant.now().toEpochMilli();
        for (UUID[] pair : shadowEntitlementPairs) {
            assignmentRepository.upsertAssignment(
                    UUID.randomUUID(), tenantId, pair[0], pair[1],
                    resourceId, EntitlementAssignmentSourceConstants.SOURCE_DIRECT, syncRunId, now);
        }

        logger.debug().message("Batch upsert completed")
                .detail("upsertedCount", shadowEntitlementPairs.size())
                .log();
    }

    // ===================================================================
    //  API / Catalog Operations
    // ===================================================================

    @Override
    @Transactional(readOnly = true)
    public Optional<EntitlementResponseDto> findById(UUID id) {
        return entitlementRepository.findById(id)
                .map(e -> {
                    long count = assignmentRepository.countByEntitlementIdAndTenantId(e.getId(), e.getTenantId());
                    Set<String> customKeys = loadCustomAttributeKeys(e.getTenantId());
                    // Resolve resource name for single-entity fetch
                    Map<UUID, String> appMap = applicationClient.getApplicationNames(
                            e.getTenantId(), List.of(e.getResourceId()));
                    return toResponseDto(e, count, customKeys, appMap.getOrDefault(e.getResourceId(), null));
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementResponseDto> findAll(String tenantId, UUID resourceId, String search,
                                                String riskLevel, String lifecycle, String kind,
                                                Boolean classified, Boolean requestable,
                                                Boolean hasOwner, UUID ownerId,
                                                String assignmentSource, Long minAssignedUsers,
                                                Long maxAssignedUsers, Pageable pageable) {
        Specification<Entitlement> spec = buildSpecification(tenantId, resourceId, search,
                riskLevel, lifecycle, kind, classified, requestable, hasOwner, ownerId,
                assignmentSource, minAssignedUsers, maxAssignedUsers);

        Page<Entitlement> page = entitlementRepository.findAll(spec, pageable);
        return enrichPage(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementResponseDto> findByResourceId(UUID resourceId, String tenantId, Pageable pageable) {
        Page<Entitlement> page = entitlementRepository.findByResourceIdAndTenantId(resourceId, tenantId, pageable);
        return enrichPage(page);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementResponseDto> searchByResource(UUID resourceId, String tenantId,
                                                         String search, String riskLevel,
                                                         String lifecycle, String kind,
                                                         Pageable pageable) {
        Specification<Entitlement> spec = buildSpecification(tenantId, resourceId, search,
                riskLevel, lifecycle, kind, null, null, null, null, null, null, null);
        Page<Entitlement> page = entitlementRepository.findAll(spec, pageable);
        return enrichPage(page);
    }

    @Override
    @Transactional
    @AuditEntitlementGovernance(idParamIndex = 0, tenantIdParamIndex = 1, updateDtoParamIndex = 2)
    public EntitlementResponseDto updateGovernance(UUID id, String tenantId, EntitlementUpdateDto updateDto) {
        Entitlement entitlement = entitlementRepository.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> new EntityNotFoundException("Entitlement not found: " + id));

        entitlementMapper.updateEntityFromGovernanceDto(updateDto, entitlement);
        entitlement = entitlementRepository.save(entitlement);

        long count = assignmentRepository.countByEntitlementIdAndTenantId(id, tenantId);

        logger.info().message("Updated entitlement governance metadata")
                .detail("entitlementId", id)
                .detail("tenantId", tenantId)
                .detail("kind", updateDto.kind())
                .log();

        Set<String> customKeys = loadCustomAttributeKeys(tenantId);
        return toResponseDto(entitlement, count, customKeys);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementMemberDto> getEntitlementMembers(UUID entitlementId, String tenantId, String search, Pageable pageable) {
        Page<EntitlementAssignment> assignments = search != null && !search.isBlank()
                ? assignmentRepository.findByEntitlementIdAndTenantIdAndMemberSearch(
                entitlementId, tenantId, "%" + search.trim().toLowerCase(Locale.ROOT) + "%", pageable)
                : assignmentRepository.findByEntitlementIdAndTenantId(entitlementId, tenantId, pageable);

        Map<UUID, Shadow> shadowsById = loadAssignmentShadows(assignments.getContent());
        Map<UUID, Identity> identitiesById = loadAssignmentIdentities(assignments.getContent(), shadowsById, tenantId);
        Map<UUID, AccessRole> rolesById = loadAssignmentRoles(assignments.getContent());

        Page<EntitlementMemberDto> result = assignments.map(ea -> toEntitlementMemberDto(ea, shadowsById, identitiesById, rolesById));

        logger.info()
                .message(EntitlementMemberConstants.LOG_MEMBERS_FETCHED)
                .detail(EntitlementMemberConstants.DETAIL_TENANT_ID, tenantId)
                .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlementId.toString())
                .detail(EntitlementMemberConstants.DETAIL_MEMBER_COUNT, String.valueOf(result.getTotalElements()))
                .log();

        return result;
    }

    private EntitlementMemberDto toEntitlementMemberDto(EntitlementAssignment assignment,
                                                        Map<UUID, Shadow> shadowsById,
                                                        Map<UUID, Identity> identitiesById,
                                                        Map<UUID, AccessRole> rolesById) {
        Shadow shadow = assignment.getShadowId() != null ? shadowsById.get(assignment.getShadowId()) : null;
        String accountName = shadow != null ? shadow.getResourceUid() : null;
        UUID identityId = assignment.getIdentityId();
        if (identityId == null && shadow != null) {
            identityId = shadow.getLinkedIdentityId();
        }
        Identity identity = identityId != null ? identitiesById.get(identityId) : null;
        String accessRoleName = null;
        String assignmentPath = null;
        if (assignment.getAccessRoleId() != null) {
            AccessRole role = rolesById.get(assignment.getAccessRoleId());
            if (role != null) {
                accessRoleName = role.getDisplayName() != null ? role.getDisplayName() : role.getName();
                assignmentPath = accessRoleName;
            }
        }

        return new EntitlementMemberDto(
                assignment.getId(),
                assignment.getShadowId(),
                accountName,
                identityId,
                identityDisplayName(identity),
                identityRiskLevel(identity),
                normalizeAssignmentSource(assignment.getSource()),
                assignment.getAssignedAt(),
                assignment.getUpdatedAt(),
                assignment.getAccessRoleId(),
                accessRoleName,
                assignmentPath,
                assignmentStatus(assignmentStatusEnum(assignment)),
                assignmentStatusReason(assignment));
    }

    private Map<UUID, Shadow> loadAssignmentShadows(List<EntitlementAssignment> assignments) {
        Set<UUID> shadowIds = assignments.stream()
                .map(EntitlementAssignment::getShadowId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        if (shadowIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Shadow> shadowsById = new HashMap<>();
        shadowRepository.findAllById(shadowIds).forEach(shadow -> shadowsById.put(shadow.getId(), shadow));
        return shadowsById;
    }

    private Map<UUID, Identity> loadAssignmentIdentities(List<EntitlementAssignment> assignments,
                                                         Map<UUID, Shadow> shadowsById,
                                                         String tenantId) {
        Set<UUID> identityIds = new LinkedHashSet<>();
        for (EntitlementAssignment assignment : assignments) {
            if (assignment.getIdentityId() != null) {
                identityIds.add(assignment.getIdentityId());
            }
            Shadow shadow = assignment.getShadowId() != null ? shadowsById.get(assignment.getShadowId()) : null;
            if (shadow != null && shadow.getLinkedIdentityId() != null) {
                identityIds.add(shadow.getLinkedIdentityId());
            }
        }
        if (identityIds.isEmpty()) {
            return Map.of();
        }
        return identityRepository.findByTenantIdAndIdIn(tenantId, identityIds).stream()
                .collect(Collectors.toMap(Identity::getId, identity -> identity, (left, right) -> left));
    }

    private Map<UUID, AccessRole> loadAssignmentRoles(List<EntitlementAssignment> assignments) {
        Set<UUID> roleIds = assignments.stream()
                .map(EntitlementAssignment::getAccessRoleId)
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return accessRoleRepository.findByIdIn(roleIds).stream()
                .collect(Collectors.toMap(AccessRole::getId, role -> role, (left, right) -> left));
    }

    private String identityDisplayName(Identity identity) {
        if (identity instanceof HumanIdentity human) {
            return Stream.of(human.getDisplayName(), human.getUsername(), human.getEmail())
                    .filter(value -> value != null && !value.isBlank())
                    .findFirst()
                    .orElse(null);
        }
        if (identity instanceof NonHumanIdentity nonHuman) {
            return nonHuman.getServiceName();
        }
        return null;
    }

    private String identityRiskLevel(Identity identity) {
        if (identity instanceof HumanIdentity human) {
            return human.getRiskLevel();
        }
        if (identity instanceof NonHumanIdentity nonHuman) {
            return nonHuman.getRiskLevel();
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ShadowEntitlementDto> getShadowEntitlements(UUID shadowId, String tenantId, Pageable pageable) {
        Page<EntitlementAssignment> assignments =
                assignmentRepository.findByShadowIdAndTenantId(shadowId, tenantId, pageable);

        return assignments.map(ea -> {
            String entitlementName = null;
            String entitlementType = null;
            String objectClass = null;
            String riskLevel = null;

            Optional<Entitlement> entOpt = entitlementRepository.findById(ea.getEntitlementId());
            if (entOpt.isPresent()) {
                Entitlement ent = entOpt.get();
                entitlementName = ent.getDisplayName();
                entitlementType = ent.getKind();
                objectClass = ent.getObjectClass();
                riskLevel = ent.getRiskLevel();
            }

            return new ShadowEntitlementDto(
                    ea.getId(),
                    ea.getEntitlementId(),
                    entitlementName,
                    entitlementType,
                    objectClass,
                    riskLevel,
                    normalizeAssignmentSource(ea.getSource()),
                    ea.getAssignedAt()
            );
        });
    }

    @Override
    @Transactional
    public ManualEntitlementAssignmentResponse assignEntitlementMembers(UUID entitlementId,
                                                                        String tenantId,
                                                                        ManualEntitlementAssignmentRequest request,
                                                                        String requestedBy,
                                                                        String requestedByUserName) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (request == null || request.identityIds() == null || request.identityIds().isEmpty()) {
            throw new IllegalArgumentException("At least one identity ID is required");
        }

        Entitlement entitlement = entitlementRepository.findById(entitlementId)
                .filter(e -> tenantId.equals(e.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Entitlement not found: " + entitlementId));

        List<UUID> identityIds = request.identityIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        logger.info().message(EntitlementMemberConstants.LOG_MANUAL_ASSIGNMENT_REQUESTED)
                .detail(EntitlementMemberConstants.DETAIL_TENANT_ID, tenantId)
                .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlementId.toString())
                .detail(EntitlementMemberConstants.DETAIL_REQUESTED_BY, requestedBy)
                .detail(EntitlementMemberConstants.DETAIL_REQUESTED_COUNT, String.valueOf(identityIds.size()))
                .log();

        List<ManualEntitlementAssignmentResult> results = new ArrayList<>();
        int created = 0;
        int updated = 0;
        int pending = 0;
        int alreadyAssigned = 0;

        for (UUID identityId : identityIds) {
            List<AssignmentOutcome> outcomes = assignDirectEntitlementToIdentity(
                    tenantId, identityId, entitlement, requestedBy, requestedByUserName);
            for (AssignmentOutcome outcome : outcomes) {
                results.add(toManualAssignmentResult(identityId, outcome));
                if (EntitlementMemberConstants.ACTION_CREATED.equals(outcome.action())) {
                    created++;
                } else if (EntitlementMemberConstants.ACTION_UPDATED.equals(outcome.action())) {
                    updated++;
                } else if (EntitlementMemberConstants.ACTION_ALREADY_ASSIGNED.equals(outcome.action())) {
                    alreadyAssigned++;
                }
                if (outcome.assignment().getStatus() == EntitlementAssignment.AssignmentStatus.PENDING) {
                    pending++;
                }
            }
        }

        logger.info().message(EntitlementMemberConstants.LOG_MANUAL_ASSIGNMENT_COMPLETED)
                .detail(EntitlementMemberConstants.DETAIL_TENANT_ID, tenantId)
                .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlementId.toString())
                .detail(EntitlementMemberConstants.DETAIL_CREATED_COUNT, String.valueOf(created))
                .detail(EntitlementMemberConstants.DETAIL_UPDATED_COUNT, String.valueOf(updated))
                .detail(EntitlementMemberConstants.DETAIL_PENDING_COUNT, String.valueOf(pending))
                .detail(EntitlementMemberConstants.DETAIL_ALREADY_ASSIGNED_COUNT, String.valueOf(alreadyAssigned))
                .log();

        ManualEntitlementAssignmentResponse response = new ManualEntitlementAssignmentResponse(
                entitlementId,
                identityIds.size(),
                created,
                updated,
                pending,
                alreadyAssigned,
                results);
        manualSyncAuditService.auditManualSync(
                tenantId,
                entitlement,
                toManualAssignmentAuditResponse(entitlementId, identityIds.size(), results),
                requestedBy);
        return response;
    }

    @Override
    @Transactional
    public SyncManualEntitlementMembersResponse syncManualEntitlementMembers(UUID entitlementId,
                                                                             String tenantId,
                                                                             SyncManualEntitlementMembersRequest request,
                                                                             String requestedBy,
                                                                             String requestedByUserName) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (request == null || request.identityIds() == null) {
            throw new IllegalArgumentException("identityIds is required");
        }

        Entitlement entitlement = entitlementRepository.findById(entitlementId)
                .filter(e -> tenantId.equals(e.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException("Entitlement not found: " + entitlementId));

        LinkedHashSet<UUID> desiredIdentityIds = request.identityIds().stream()
                .filter(Objects::nonNull)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);

        List<EntitlementAssignment> currentAssignments = assignmentRepository
                .findStandaloneDirectAssignmentsByTenantIdAndEntitlementId(tenantId, entitlementId);
        Map<UUID, List<EntitlementAssignment>> currentByIdentity = groupAssignmentsByIdentity(tenantId, currentAssignments);

        List<UUID> toAdd = desiredIdentityIds.stream()
                .filter(identityId -> !currentByIdentity.containsKey(identityId))
                .toList();
        List<UUID> toRevoke = currentByIdentity.keySet().stream()
                .filter(identityId -> !desiredIdentityIds.contains(identityId))
                .toList();
        List<UUID> toSkip = desiredIdentityIds.stream()
                .filter(currentByIdentity::containsKey)
                .toList();

        logger.info().message(EntitlementMemberConstants.LOG_MANUAL_SYNC_STARTED)
                .detail(EntitlementMemberConstants.DETAIL_TENANT_ID, tenantId)
                .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlementId.toString())
                .detail(EntitlementMemberConstants.DETAIL_REQUESTED_BY, requestedBy)
                .detail(EntitlementMemberConstants.DETAIL_DESIRED_COUNT, String.valueOf(desiredIdentityIds.size()))
                .detail(EntitlementMemberConstants.DETAIL_CURRENT_COUNT, String.valueOf(currentByIdentity.size()))
                .detail(EntitlementMemberConstants.DETAIL_TO_ADD_COUNT, String.valueOf(toAdd.size()))
                .detail(EntitlementMemberConstants.DETAIL_TO_REVOKE_COUNT, String.valueOf(toRevoke.size()))
                .log();

        List<SyncEntitlementMemberResult> added = new ArrayList<>();
        List<SyncEntitlementMemberResult> revoked = new ArrayList<>();
        List<SyncEntitlementMemberResult> skipped = new ArrayList<>();
        List<SyncEntitlementMemberFailure> failed = new ArrayList<>();

        for (UUID identityId : toAdd) {
            try {
                List<ManualEntitlementAssignmentResult> assignmentResults = assignDirectEntitlementToIdentity(
                                tenantId, identityId, entitlement, requestedBy, requestedByUserName).stream()
                        .map(outcome -> toManualAssignmentResult(identityId, outcome))
                        .toList();
                added.add(new SyncEntitlementMemberResult(identityId, assignmentResults));
                logger.debug().message(EntitlementMemberConstants.LOG_MANUAL_SYNC_GRANT)
                        .detail(EntitlementMemberConstants.DETAIL_IDENTITY_ID, identityId.toString())
                        .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlementId.toString())
                        .log();
            } catch (RuntimeException ex) {
                failed.add(new SyncEntitlementMemberFailure(identityId, ex.getMessage()));
                logger.warn().message(EntitlementMemberConstants.LOG_MANUAL_SYNC_FAILED)
                        .detail(EntitlementMemberConstants.DETAIL_IDENTITY_ID, identityId.toString())
                        .detail(EntitlementMemberConstants.DETAIL_ERROR, ex.getMessage())
                        .log();
            }
        }

        List<EntitlementAssignment> assignmentsToDelete = new ArrayList<>();
        for (UUID identityId : toRevoke) {
            List<EntitlementAssignment> assignments = currentByIdentity.getOrDefault(identityId, List.of());
            assignmentsToDelete.addAll(assignments);
            revoked.add(new SyncEntitlementMemberResult(
                    identityId,
                    assignments.stream()
                            .map(assignment -> new ManualEntitlementAssignmentResult(
                                    identityId,
                                    assignment.getShadowId(),
                                    assignment.getId(),
                                    assignmentStatus(assignmentStatusEnum(assignment)),
                                    assignmentStatusReason(assignment),
                                    EntitlementMemberConstants.ACTION_REVOKED))
                            .toList()));
            logger.debug().message(EntitlementMemberConstants.LOG_MANUAL_SYNC_REVOKE)
                    .detail(EntitlementMemberConstants.DETAIL_IDENTITY_ID, identityId.toString())
                    .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlementId.toString())
                    .log();
        }
        if (!assignmentsToDelete.isEmpty()) {
            assignmentRepository.deleteAll(assignmentsToDelete);
        }

        for (UUID identityId : toSkip) {
            List<EntitlementAssignment> assignments = currentByIdentity.getOrDefault(identityId, List.of());
            skipped.add(new SyncEntitlementMemberResult(
                    identityId,
                    assignments.stream()
                            .map(assignment -> new ManualEntitlementAssignmentResult(
                                    identityId,
                                    assignment.getShadowId(),
                                    assignment.getId(),
                                    assignmentStatus(assignmentStatusEnum(assignment)),
                                    assignmentStatusReason(assignment),
                                    EntitlementMemberConstants.ACTION_ALREADY_ASSIGNED))
                            .toList()));
            logger.debug().message(EntitlementMemberConstants.LOG_MANUAL_SYNC_SKIPPED)
                    .detail(EntitlementMemberConstants.DETAIL_IDENTITY_ID, identityId.toString())
                    .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlementId.toString())
                    .log();
        }

        int totalDirectCount = Math.max(0, currentByIdentity.size() - revoked.size()) + added.size();
        logger.info().message(EntitlementMemberConstants.LOG_MANUAL_SYNC_COMPLETED)
                .detail(EntitlementMemberConstants.DETAIL_TENANT_ID, tenantId)
                .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlementId.toString())
                .detail(EntitlementMemberConstants.DETAIL_CREATED_COUNT, String.valueOf(added.size()))
                .detail(EntitlementMemberConstants.DETAIL_REVOKED_COUNT, String.valueOf(revoked.size()))
                .detail(EntitlementMemberConstants.DETAIL_SKIPPED_COUNT, String.valueOf(skipped.size()))
                .detail(EntitlementMemberConstants.DETAIL_FAILED_COUNT, String.valueOf(failed.size()))
                .detail(EntitlementMemberConstants.DETAIL_TOTAL_DIRECT_COUNT, String.valueOf(totalDirectCount))
                .log();

        SyncManualEntitlementMembersResponse response = new SyncManualEntitlementMembersResponse(
                entitlementId,
                desiredIdentityIds.size(),
                added.size(),
                revoked.size(),
                skipped.size(),
                failed.size(),
                totalDirectCount,
                added,
                revoked,
                skipped,
                failed);
        manualSyncAuditService.auditManualSync(tenantId, entitlement, response, requestedBy);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilterOptionDto> getFilterOptions(UUID resourceId, String tenantId) {
        List<FilterOptionDto> filters = new ArrayList<>();

        List<String> riskLevels = entitlementRepository.findDistinctRiskLevels(resourceId, tenantId);
        if (!riskLevels.isEmpty()) {
            filters.add(new FilterOptionDto("riskLevel", "Risk Level",
                    riskLevels.stream().map(v -> new FilterValueDto(v, v, 0)).toList()));
        }

        List<String> lifecycles = entitlementRepository.findDistinctLifecycles(resourceId, tenantId);
        if (!lifecycles.isEmpty()) {
            filters.add(new FilterOptionDto("lifecycle", "Lifecycle",
                    lifecycles.stream().map(v -> new FilterValueDto(v, v, 0)).toList()));
        }

        // "kind" key used (dev branch correction: more accurate than "type" from access-portal branch)
        List<String> kinds = entitlementRepository.findDistinctKinds(resourceId, tenantId);
        if (!kinds.isEmpty()) {
            filters.add(new FilterOptionDto("kind", "Kind",
                    kinds.stream().map(v -> new FilterValueDto(v, v, 0)).toList()));
        }

        return filters;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IdentityEntitlementViewDto> getEntitlementsByIdentityId(UUID identityId, String tenantId,
                                                                        String search, Pageable pageable) {
        List<UUID> shadowIds = shadowRepository.findShadowIdsByLinkedIdentityId(identityId, tenantId);

        if (shadowIds.isEmpty()) {
            logger.debug().message("No shadows found for identity")
                    .detail("identityId", identityId)
                    .log();
        }

        Page<EntitlementAssignment> assignmentPage;
        if (search != null && !search.isBlank()) {
            String searchPattern = "%" + search.trim().toLowerCase() + "%";
            assignmentPage = shadowIds.isEmpty()
                    ? assignmentRepository.findByIdentityIdAndTenantIdAndSearch(identityId, tenantId, searchPattern, pageable)
                    : assignmentRepository.findByIdentityIdOrShadowIdInAndTenantIdAndSearch(
                            identityId, shadowIds, tenantId, searchPattern, pageable);
        } else {
            assignmentPage = shadowIds.isEmpty()
                    ? assignmentRepository.findByIdentityIdAndTenantId(identityId, tenantId, pageable)
                    : assignmentRepository.findByIdentityIdOrShadowIdInAndTenantId(identityId, shadowIds, tenantId, pageable);
        }

        if (assignmentPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Batch-load entitlements for this page
        List<UUID> entitlementIds = assignmentPage.getContent().stream()
                .map(EntitlementAssignment::getEntitlementId)
                .distinct()
                .toList();
        Map<UUID, Entitlement> entitlementMap = new HashMap<>(entitlementIds.size());
        for (Entitlement e : entitlementRepository.findAllById(entitlementIds)) {
            entitlementMap.put(e.getId(), e);
        }

        // Batch-load shadows for this page
        List<UUID> pageShadowIds = assignmentPage.getContent().stream()
                .map(EntitlementAssignment::getShadowId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<UUID, Shadow> shadowMap = new HashMap<>(pageShadowIds.size());
        for (Shadow s : shadowRepository.findAllByIdInAndTenantId(pageShadowIds, tenantId)) {
            shadowMap.put(s.getId(), s);
        }

        // Batch-load application names for this page
        List<UUID> resourceIds = assignmentPage.getContent().stream()
                .map(EntitlementAssignment::getResourceId)
                .distinct()
                .toList();
        Map<UUID, String> appNameMap = applicationClient.getApplicationNames(tenantId, resourceIds);

        RoleEntitlementProvenanceContext provenanceContext = buildRoleEntitlementProvenanceContext(
                tenantId,
                assignmentPage.getContent());

        return assignmentPage.map(assignment -> {
            Entitlement entitlement = entitlementMap.get(assignment.getEntitlementId());
            Shadow shadow = shadowMap.get(assignment.getShadowId());
            String applicationName = appNameMap.getOrDefault(
                    assignment.getResourceId(), ApplicationClientConstants.UNKNOWN_APP);
            RoleEntitlementProvenance provenance = provenanceContext.resolve(assignment);

            return new IdentityEntitlementViewDto(
                    assignment.getId(),
                    assignment.getShadowId(),
                    shadow != null ? shadow.getResourceUid() : "Unknown",
                    assignment.getResourceId(),
                    applicationName,
                    assignment.getEntitlementId(),
                    entitlement != null ? entitlement.getDisplayName() : "Unknown",
                    entitlement != null ? entitlement.getDescription() : null,
                    entitlement != null ? entitlement.getKind() : "entitlement",
                    entitlement != null ? entitlement.getObjectClass() : null,
                    entitlement != null ? entitlement.getRiskLevel() : null,
                    entitlement != null ? entitlement.getClassification() : null,
                    normalizeAssignmentSource(assignment.getSource()),
                    provenance.accessPathType(),
                    provenance.accessRoleId(),
                    provenance.accessRoleName(),
                    provenance.sourceRoleId(),
                    provenance.sourceRoleName(),
                    provenance.direct(),
                    provenance.depth(),
                    provenance.rolePath(),
                    assignmentStatus(assignmentStatusEnum(assignment)),
                    assignmentStatusReason(assignment),
                    assignment.getAssignedAt(),
                    assignment.getAssignedByUserId(),
                    assignment.getAssignedByUserName(),
                    assignment.getUpdatedAt()
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public IdentityAccessBreakdownDto getIdentityAccessBreakdown(UUID identityId, String tenantId, String search) {
        Page<IdentityEntitlementViewDto> entitlementPage = getEntitlementsByIdentityId(
                identityId,
                tenantId,
                null,
                Pageable.unpaged());

        List<IdentityEntitlementViewDto> entitlementRows = entitlementPage.getContent();
        List<IdentityRoleAssignment> activeRoleAssignments = identityRoleAssignmentRepository
                .findByTenantIdAndIdentityIdAndStatus(tenantId, identityId, IdentityRoleAssignment.Status.ACTIVE);

        Set<UUID> roleIds = new LinkedHashSet<>();
        activeRoleAssignments.stream()
                .map(IdentityRoleAssignment::getRoleId)
                .filter(Objects::nonNull)
                .forEach(roleIds::add);
        entitlementRows.stream()
                .map(IdentityEntitlementViewDto::accessRoleId)
                .filter(Objects::nonNull)
                .forEach(roleIds::add);

        Map<UUID, AccessRole> rolesById = new HashMap<>();
        if (!roleIds.isEmpty()) {
            for (AccessRole role : accessRoleRepository.findByIdIn(List.copyOf(roleIds))) {
                if (tenantId.equals(role.getTenantId())) {
                    rolesById.put(role.getId(), role);
                }
            }
        }

        Map<UUID, IdentityRoleAssignment> assignmentsByRoleId = firstAssignmentByRole(activeRoleAssignments);
        Map<UUID, RoleAccessBuilder> roleGroups = new LinkedHashMap<>();
        for (IdentityRoleAssignment assignment : activeRoleAssignments) {
            UUID roleId = assignment.getRoleId();
            roleGroups.computeIfAbsent(roleId, id -> new RoleAccessBuilder(id, rolesById.get(id), assignment));
        }

        List<IdentityAccessEntitlementDto> standaloneEntitlements = new ArrayList<>();
        for (IdentityEntitlementViewDto row : entitlementRows) {
            IdentityAccessEntitlementDto entitlement = toAccessEntitlement(row);
            if (isRoleDerived(row)) {
                UUID roleId = row.accessRoleId();
                RoleAccessBuilder group = roleGroups.computeIfAbsent(roleId,
                        id -> new RoleAccessBuilder(id, rolesById.get(id), assignmentsByRoleId.get(id)));
                group.entitlements.add(entitlement);
            } else {
                standaloneEntitlements.add(entitlement);
            }
        }

        List<IdentityAccessRoleGroupDto> roleDtos = roleGroups.values().stream()
                .map(RoleAccessBuilder::toDto)
                .collect(Collectors.toCollection(ArrayList::new));
        IdentityAccessBreakdownDto unfiltered = buildAccessBreakdown(identityId, search, roleDtos, standaloneEntitlements);

        if (search == null || search.isBlank()) {
            return unfiltered;
        }
        return filterAccessBreakdown(unfiltered, search);
    }

    private Map<UUID, IdentityRoleAssignment> firstAssignmentByRole(List<IdentityRoleAssignment> assignments) {
        Map<UUID, IdentityRoleAssignment> result = new HashMap<>();
        for (IdentityRoleAssignment assignment : assignments) {
            if (assignment.getRoleId() == null) {
                continue;
            }
            result.merge(assignment.getRoleId(), assignment, this::earlierRoleAssignment);
        }
        return result;
    }

    private IdentityRoleAssignment earlierRoleAssignment(IdentityRoleAssignment left, IdentityRoleAssignment right) {
        if (left.getAssignedAt() == null) {
            return right;
        }
        if (right.getAssignedAt() == null) {
            return left;
        }
        return left.getAssignedAt() <= right.getAssignedAt() ? left : right;
    }

    private IdentityAccessBreakdownDto filterAccessBreakdown(IdentityAccessBreakdownDto source, String search) {
        String normalizedSearch = search.toLowerCase(Locale.ROOT).trim();
        List<IdentityAccessRoleGroupDto> roles = new ArrayList<>();

        for (IdentityAccessRoleGroupDto role : source.businessRoles()) {
            boolean roleMatches = matchesRole(role, normalizedSearch);
            List<IdentityAccessEntitlementDto> entitlements = roleMatches
                    ? role.entitlements()
                    : role.entitlements().stream()
                    .filter(entitlement -> matchesEntitlement(entitlement, normalizedSearch))
                    .collect(Collectors.toList());

            if (roleMatches || !entitlements.isEmpty()) {
                roles.add(new IdentityAccessRoleGroupDto(
                        role.roleId(),
                        role.roleName(),
                        role.roleType(),
                        role.roleStatus(),
                        role.riskLevel(),
                        role.businessUnit(),
                        role.description(),
                        role.assignmentSource(),
                        role.assignedAt(),
                        role.assignedByUserId(),
                        role.assignedByUserName(),
                        entitlements.size(),
                        entitlements));
            }
        }

        List<IdentityAccessEntitlementDto> standaloneEntitlements = source.standaloneEntitlements().stream()
                .filter(entitlement -> matchesEntitlement(entitlement, normalizedSearch))
                .collect(Collectors.toList());
        return buildAccessBreakdown(source.identityId(), source.search(), roles, standaloneEntitlements);
    }

    private IdentityAccessBreakdownDto buildAccessBreakdown(UUID identityId,
                                                            String search,
                                                            List<IdentityAccessRoleGroupDto> roles,
                                                            List<IdentityAccessEntitlementDto> standaloneEntitlements) {
        long roleEntitlements = roles.stream().mapToLong(IdentityAccessRoleGroupDto::entitlementCount).sum();
        long standaloneCount = standaloneEntitlements.size();
        return new IdentityAccessBreakdownDto(
                identityId,
                search,
                roleEntitlements + standaloneCount,
                roleEntitlements,
                standaloneCount,
                roles,
                standaloneEntitlements);
    }

    private boolean isRoleDerived(IdentityEntitlementViewDto row) {
        return row.accessRoleId() != null
                || "ROLE_DIRECT".equals(row.accessPathType())
                || "ROLE_INHERITED_CHILD".equals(row.accessPathType())
                || "ROLE_DERIVED_UNKNOWN".equals(row.accessPathType())
                || EntitlementAssignmentSourceConstants.SOURCE_ROLE_DERIVED.equalsIgnoreCase(row.source());
    }

    private IdentityAccessEntitlementDto toAccessEntitlement(IdentityEntitlementViewDto row) {
        return new IdentityAccessEntitlementDto(
                row.assignmentId(),
                row.shadowId(),
                row.accountName(),
                row.resourceId(),
                row.applicationName(),
                row.entitlementId(),
                row.entitlementName(),
                row.description(),
                row.kind(),
                row.objectClass(),
                row.riskLevel(),
                row.classification(),
                friendlyAssignmentSource(row),
                friendlyAccessPathType(row.accessPathType()),
                row.accessRoleId(),
                row.accessRoleName(),
                row.sourceRoleId(),
                row.sourceRoleName(),
                row.roleEntitlementDirect(),
                row.roleEntitlementDepth(),
                row.rolePath(),
                friendlyStatus(row.assignmentStatus()),
                row.assignmentStatusReason(),
                row.assignedAt(),
                row.assignedByUserId(),
                row.assignedByUserName(),
                row.updatedAt());
    }

    private String friendlyAssignmentSource(IdentityEntitlementViewDto row) {
        if (isRoleDerived(row)) {
            return "Role";
        }
        if (row.source() == null) {
            return null;
        }
        return switch (row.source().toUpperCase(Locale.ROOT)) {
            case "DIRECT" -> "Direct";
            case "DYNAMIC" -> "Dynamic";
            case "MANUAL" -> "Manual";
            case "IMPORT" -> "Import";
            default -> toTitleCase(row.source());
        };
    }

    private String friendlyAccessPathType(String accessPathType) {
        if (accessPathType == null) {
            return null;
        }
        return switch (accessPathType) {
            case "DIRECT_ENTITLEMENT" -> "Direct Entitlement";
            case "ROLE_DIRECT" -> "Role Direct";
            case "ROLE_INHERITED_CHILD" -> "Inherited From Child Role";
            case "ROLE_DERIVED_UNKNOWN" -> "Role Path Unknown";
            default -> toTitleCase(accessPathType.replace('_', ' '));
        };
    }

    private String friendlyStatus(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "ASSIGNED" -> "Assigned";
            case "PENDING" -> "Pending";
            default -> toTitleCase(status);
        };
    }

    private boolean matchesRole(IdentityAccessRoleGroupDto role, String search) {
        return contains(role.roleName(), search)
                || contains(role.roleType(), search)
                || contains(role.roleStatus(), search)
                || contains(role.riskLevel(), search)
                || contains(role.businessUnit(), search)
                || contains(role.description(), search)
                || contains(role.assignmentSource(), search)
                || contains(role.assignedByUserId(), search)
                || contains(role.assignedByUserName(), search);
    }

    private boolean matchesEntitlement(IdentityAccessEntitlementDto entitlement, String search) {
        return contains(entitlement.entitlementName(), search)
                || contains(entitlement.description(), search)
                || contains(entitlement.kind(), search)
                || contains(entitlement.objectClass(), search)
                || contains(entitlement.riskLevel(), search)
                || contains(entitlement.source(), search)
                || contains(entitlement.accessPathType(), search)
                || contains(entitlement.accountName(), search)
                || contains(entitlement.applicationName(), search)
                || contains(entitlement.accessRoleName(), search)
                || contains(entitlement.sourceRoleName(), search)
                || contains(entitlement.assignmentStatus(), search)
                || contains(entitlement.assignmentStatusReason(), search)
                || contains(entitlement.assignedByUserId(), search)
                || contains(entitlement.assignedByUserName(), search)
                || contains(entitlement.classification(), search)
                || containsRolePath(entitlement.rolePath(), search);
    }

    private boolean containsRolePath(List<IdentityEntitlementRolePathNodeDto> rolePath, String search) {
        if (rolePath == null) {
            return false;
        }
        return rolePath.stream().anyMatch(node -> contains(node.roleName(), search));
    }

    private boolean contains(Collection<String> values, String search) {
        return values != null && values.stream().anyMatch(value -> contains(value, search));
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private String toTitleCase(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String[] parts = value.toLowerCase(Locale.ROOT).split("\\s+");
        return Arrays.stream(parts)
                .map(part -> part.isEmpty() ? part : Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String roleDisplayName(AccessRole role) {
        if (role == null) {
            return "Unknown Role";
        }
        return role.getDisplayName() != null && !role.getDisplayName().isBlank()
                ? role.getDisplayName()
                : role.getName();
    }

    private String friendlyRoleAssignmentSource(IdentityRoleAssignment assignment) {
        if (assignment == null || assignment.getSourceType() == null) {
            return null;
        }
        return toTitleCase(assignment.getSourceType().name());
    }

    private class RoleAccessBuilder {
        private final UUID roleId;
        private final AccessRole role;
        private final IdentityRoleAssignment assignment;
        private final List<IdentityAccessEntitlementDto> entitlements = new ArrayList<>();

        private RoleAccessBuilder(UUID roleId, AccessRole role, IdentityRoleAssignment assignment) {
            this.roleId = roleId;
            this.role = role;
            this.assignment = assignment;
        }

        private IdentityAccessRoleGroupDto toDto() {
            return new IdentityAccessRoleGroupDto(
                    roleId,
                    roleDisplayName(role),
                    role != null ? role.getType() : null,
                    role != null ? role.getStatus() : null,
                    role != null ? role.getRiskLevel() : null,
                    role != null ? role.getBusinessUnit() : null,
                    role != null ? role.getDescription() : null,
                    friendlyRoleAssignmentSource(assignment),
                    assignment != null ? assignment.getAssignedAt() : null,
                    assignment != null ? assignment.getAssignedByUserId() : null,
                    assignment != null ? assignment.getAssignedByUserName() : null,
                    entitlements.size(),
                    List.copyOf(entitlements));
        }
    }

    private RoleEntitlementProvenanceContext buildRoleEntitlementProvenanceContext(String tenantId,
                                                                                   List<EntitlementAssignment> assignments) {
        Map<UUID, UUID> roleIdsByRuleId = loadRoleIdsByRuleId(tenantId, assignments);
        Map<UUID, UUID> accessRoleIdsByAssignmentId = new HashMap<>();
        Set<UUID> accessRoleIds = new LinkedHashSet<>();
        Set<UUID> entitlementIds = new LinkedHashSet<>();

        for (EntitlementAssignment assignment : assignments) {
            UUID accessRoleId = resolveAccessRoleId(assignment, roleIdsByRuleId);
            if (accessRoleId == null) {
                continue;
            }
            accessRoleIdsByAssignmentId.put(assignment.getId(), accessRoleId);
            accessRoleIds.add(accessRoleId);
            entitlementIds.add(assignment.getEntitlementId());
        }

        Map<String, RoleEntitlementClosure> closureByRoleEntitlement = loadClosureByRoleEntitlement(
                tenantId,
                accessRoleIds,
                entitlementIds);
        Map<UUID, AccessRole> rolesById = loadRolesById(tenantId, accessRoleIds, closureByRoleEntitlement.values());
        return new RoleEntitlementProvenanceContext(accessRoleIdsByAssignmentId, closureByRoleEntitlement, rolesById);
    }

    private Map<UUID, UUID> loadRoleIdsByRuleId(String tenantId, List<EntitlementAssignment> assignments) {
        Set<UUID> ruleIds = new LinkedHashSet<>();
        for (EntitlementAssignment assignment : assignments) {
            if (assignment.getAccessRoleId() == null && assignment.getRuleId() != null) {
                ruleIds.add(assignment.getRuleId());
            }
        }
        if (ruleIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, UUID> roleIdsByRuleId = new HashMap<>();
        for (AccessRoleAssignmentRule rule : accessRoleAssignmentRuleRepository.findByTenantIdAndIdIn(tenantId, ruleIds)) {
            roleIdsByRuleId.put(rule.getId(), rule.getRoleId());
        }
        return roleIdsByRuleId;
    }

    private UUID resolveAccessRoleId(EntitlementAssignment assignment, Map<UUID, UUID> roleIdsByRuleId) {
        if (assignment.getAccessRoleId() != null) {
            return assignment.getAccessRoleId();
        }
        return assignment.getRuleId() != null ? roleIdsByRuleId.get(assignment.getRuleId()) : null;
    }

    private Map<String, RoleEntitlementClosure> loadClosureByRoleEntitlement(String tenantId,
                                                                              Set<UUID> accessRoleIds,
                                                                              Set<UUID> entitlementIds) {
        if (accessRoleIds.isEmpty() || entitlementIds.isEmpty()) {
            return Map.of();
        }

        Map<String, RoleEntitlementClosure> closureByKey = new HashMap<>();
        for (RoleEntitlementClosure closure : roleEntitlementClosureRepository
                .findByTenantIdAndRoleIdInAndEntitlementIdIn(tenantId, accessRoleIds, entitlementIds)) {
            String key = roleEntitlementKey(closure.getRoleId(), closure.getEntitlementId());
            RoleEntitlementClosure existing = closureByKey.get(key);
            if (existing == null || compareClosure(closure, existing) < 0) {
                closureByKey.put(key, closure);
            }
        }
        return closureByKey;
    }

    private int compareClosure(RoleEntitlementClosure left, RoleEntitlementClosure right) {
        if (left.isDirect() != right.isDirect()) {
            return left.isDirect() ? -1 : 1;
        }
        return Integer.compare(left.getDepth(), right.getDepth());
    }

    private Map<UUID, AccessRole> loadRolesById(String tenantId,
                                                Set<UUID> accessRoleIds,
                                                Collection<RoleEntitlementClosure> closures) {
        Set<UUID> roleIds = new LinkedHashSet<>(accessRoleIds);
        for (RoleEntitlementClosure closure : closures) {
            roleIds.add(closure.getSourceRoleId());
            if (closure.getPathRoleIds() != null) {
                roleIds.addAll(closure.getPathRoleIds());
            }
        }
        if (roleIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, AccessRole> rolesById = new HashMap<>();
        for (AccessRole role : accessRoleRepository.findByIdIn(List.copyOf(roleIds))) {
            if (tenantId.equals(role.getTenantId())) {
                rolesById.put(role.getId(), role);
            }
        }
        return rolesById;
    }

    private String roleEntitlementKey(UUID roleId, UUID entitlementId) {
        return roleId + "|" + entitlementId;
    }


    private record RoleEntitlementProvenanceContext(
            Map<UUID, UUID> accessRoleIdsByAssignmentId,
            Map<String, RoleEntitlementClosure> closureByRoleEntitlement,
            Map<UUID, AccessRole> rolesById) {

        private RoleEntitlementProvenance resolve(EntitlementAssignment assignment) {
            UUID accessRoleId = accessRoleIdsByAssignmentId.get(assignment.getId());
            RoleEntitlementClosure closure = accessRoleId != null
                    ? closureByRoleEntitlement.get(accessRoleId + "|" + assignment.getEntitlementId())
                    : null;
            AccessRole accessRole = accessRoleId != null ? rolesById.get(accessRoleId) : null;
            AccessRole sourceRole = closure != null ? rolesById.get(closure.getSourceRoleId()) : null;
            List<IdentityEntitlementRolePathNodeDto> rolePath = buildRolePath(accessRoleId, closure);

            return new RoleEntitlementProvenance(
                    accessPathType(assignment, accessRoleId, closure),
                    accessRoleId,
                    roleDisplayName(accessRole),
                    closure != null ? closure.getSourceRoleId() : null,
                    roleDisplayName(sourceRole),
                    closure != null ? closure.isDirect() : null,
                    closure != null ? closure.getDepth() : null,
                    rolePath);
        }

        private List<IdentityEntitlementRolePathNodeDto> buildRolePath(UUID accessRoleId, RoleEntitlementClosure closure) {
            if (accessRoleId == null) {
                return List.of();
            }
            List<UUID> pathRoleIds = closure != null && closure.getPathRoleIds() != null && !closure.getPathRoleIds().isEmpty()
                    ? closure.getPathRoleIds()
                    : List.of(accessRoleId);
            List<IdentityEntitlementRolePathNodeDto> path = new ArrayList<>(pathRoleIds.size());
            for (int i = 0; i < pathRoleIds.size(); i++) {
                UUID roleId = pathRoleIds.get(i);
                path.add(new IdentityEntitlementRolePathNodeDto(roleId, roleDisplayName(rolesById.get(roleId)), i));
            }
            return path;
        }

        private String roleDisplayName(AccessRole role) {
            if (role == null) {
                return null;
            }
            return role.getDisplayName() != null && !role.getDisplayName().isBlank()
                    ? role.getDisplayName()
                    : role.getName();
        }

        private String accessPathType(EntitlementAssignment assignment, UUID accessRoleId, RoleEntitlementClosure closure) {
            if (accessRoleId == null) {
                return EntitlementAssignmentSourceConstants.SOURCE_ROLE_DERIVED.equalsIgnoreCase(assignment.getSource())
                        ? "ROLE_DERIVED_UNKNOWN"
                        : "DIRECT_ENTITLEMENT";
            }
            if (closure == null) {
                return "ROLE_DERIVED_UNKNOWN";
            }
            return closure.isDirect() ? "ROLE_DIRECT" : "ROLE_INHERITED_CHILD";
        }
    }

    private record RoleEntitlementProvenance(
            String accessPathType,
            UUID accessRoleId,
            String accessRoleName,
            UUID sourceRoleId,
            String sourceRoleName,
            Boolean direct,
            Integer depth,
            List<IdentityEntitlementRolePathNodeDto> rolePath) {
    }

    private List<AssignmentOutcome> assignDirectEntitlementToIdentity(String tenantId,
                                                                      UUID identityId,
                                                                      Entitlement entitlement,
                                                                      String assignedByUserId,
                                                                      String assignedByUserName) {
        List<Shadow> eligibleShadows = loadEligibleAssignmentShadows(tenantId, identityId, entitlement.getResourceId());
        if (eligibleShadows.isEmpty()) {
            return List.of(assignPendingDirectEntitlement(tenantId, identityId, entitlement,
                    assignedByUserId, assignedByUserName));
        }

        EntitlementAssignment pendingAssignment = assignmentRepository
                .findByTenantIdAndIdentityIdAndEntitlementIdAndShadowIdIsNullAndSourceIgnoreCase(
                        tenantId, identityId, entitlement.getId(), EntitlementAssignmentSourceConstants.SOURCE_DIRECT)
                .orElse(null);

        List<AssignmentOutcome> outcomes = new ArrayList<>();
        boolean pendingPromoted = false;
        for (Shadow shadow : eligibleShadows) {
            EntitlementAssignment existing = assignmentRepository
                    .findByShadowIdAndEntitlementIdAndTenantIdAndSourceIgnoreCase(
                            shadow.getId(), entitlement.getId(), tenantId, EntitlementAssignmentSourceConstants.SOURCE_DIRECT)
                    .orElse(null);

            if (existing != null) {
                outcomes.add(ensureDirectAssigned(existing, identityId, shadow.getId(), false,
                        assignedByUserId, assignedByUserName));
                continue;
            }

            if (!pendingPromoted && pendingAssignment != null) {
                outcomes.add(promotePendingDirectAssignment(pendingAssignment, shadow.getId(),
                        assignedByUserId, assignedByUserName));
                pendingPromoted = true;
                continue;
            }

            outcomes.add(createDirectAssignedAssignment(tenantId, identityId, shadow.getId(), entitlement,
                    assignedByUserId, assignedByUserName));
        }
        return outcomes;
    }

    private List<Shadow> loadEligibleAssignmentShadows(String tenantId, UUID identityId, UUID resourceId) {
        return shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, tenantId).stream()
                .filter(this::isEligibleAccountShadow)
                .filter(shadow -> resourceId != null && resourceId.toString().equals(shadow.getResourceId()))
                .toList();
    }

    private boolean isEligibleAccountShadow(Shadow shadow) {
        if (shadow == null) {
            return false;
        }
        if (shadow.getKind() != null && !"account".equalsIgnoreCase(shadow.getKind())) {
            return false;
        }
        return shadow.getSynchronizationSituation() != Shadow.SynchronizationSituation.DELETED;
    }

    private AssignmentOutcome assignPendingDirectEntitlement(String tenantId,
                                                             UUID identityId,
                                                             Entitlement entitlement,
                                                             String assignedByUserId,
                                                             String assignedByUserName) {
        EntitlementAssignment assignment = assignmentRepository
                .findByTenantIdAndIdentityIdAndEntitlementIdAndShadowIdIsNullAndSourceIgnoreCase(
                        tenantId, identityId, entitlement.getId(), EntitlementAssignmentSourceConstants.SOURCE_DIRECT)
                .orElse(null);

        if (assignment == null) {
            assignment = new EntitlementAssignment();
            assignment.setTenantId(tenantId);
            assignment.setIdentityId(identityId);
            assignment.setEntitlementId(entitlement.getId());
            assignment.setResourceId(entitlement.getResourceId());
            assignment.setSource(EntitlementAssignmentSourceConstants.SOURCE_DIRECT);
            assignment.setStatus(EntitlementAssignment.AssignmentStatus.PENDING);
            applyAssignedBy(assignment, assignedByUserId, assignedByUserName);
            EntitlementAssignment saved = assignmentRepository.save(assignment);
            logger.debug().message(EntitlementMemberConstants.LOG_MANUAL_ASSIGNMENT_PENDING)
                    .detail(EntitlementMemberConstants.DETAIL_IDENTITY_ID, identityId.toString())
                    .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlement.getId().toString())
                    .log();
            return new AssignmentOutcome(saved, EntitlementMemberConstants.ACTION_CREATED);
        }

        boolean changed = ensureDirectPendingAssignment(assignment, entitlement.getResourceId(),
                assignedByUserId, assignedByUserName);
        EntitlementAssignment result = changed ? assignmentRepository.save(assignment) : assignment;
        return new AssignmentOutcome(result,
                changed ? EntitlementMemberConstants.ACTION_UPDATED : EntitlementMemberConstants.ACTION_ALREADY_ASSIGNED);
    }

    private AssignmentOutcome ensureDirectAssigned(EntitlementAssignment assignment,
                                                   UUID identityId,
                                                   UUID shadowId,
                                                   boolean promoted,
                                                   String assignedByUserId,
                                                   String assignedByUserName) {
        boolean changed = promoted;
        boolean directSourceChanged = false;
        if (assignment.getShadowId() == null || !assignment.getShadowId().equals(shadowId)) {
            assignment.setShadowId(shadowId);
            changed = true;
        }
        if (assignment.getIdentityId() == null) {
            assignment.setIdentityId(identityId);
            changed = true;
        }
        if (!EntitlementAssignmentSourceConstants.SOURCE_DIRECT.equalsIgnoreCase(assignment.getSource())) {
            assignment.setSource(EntitlementAssignmentSourceConstants.SOURCE_DIRECT);
            directSourceChanged = true;
            changed = true;
        }
        if (assignment.getRuleId() != null) {
            assignment.setRuleId(null);
            changed = true;
        }
        if (assignment.getAccessRoleId() != null) {
            assignment.setAccessRoleId(null);
            changed = true;
        }
        if (assignment.getStatus() != EntitlementAssignment.AssignmentStatus.ASSIGNED) {
            assignment.setStatus(EntitlementAssignment.AssignmentStatus.ASSIGNED);
            changed = true;
        }
        if (directSourceChanged) {
            applyAssignedBy(assignment, assignedByUserId, assignedByUserName);
        } else if (applyMissingAssignedBy(assignment, assignedByUserId, assignedByUserName)) {
            changed = true;
        }
        EntitlementAssignment result = changed ? assignmentRepository.save(assignment) : assignment;
        return new AssignmentOutcome(result,
                changed ? EntitlementMemberConstants.ACTION_UPDATED : EntitlementMemberConstants.ACTION_ALREADY_ASSIGNED);
    }

    private AssignmentOutcome promotePendingDirectAssignment(EntitlementAssignment pendingAssignment,
                                                             UUID shadowId,
                                                             String assignedByUserId,
                                                             String assignedByUserName) {
        return ensureDirectAssigned(pendingAssignment, pendingAssignment.getIdentityId(), shadowId, true,
                assignedByUserId, assignedByUserName);
    }

    private AssignmentOutcome createDirectAssignedAssignment(String tenantId,
                                                            UUID identityId,
                                                            UUID shadowId,
                                                             Entitlement entitlement,
                                                             String assignedByUserId,
                                                             String assignedByUserName) {
        EntitlementAssignment assignment = new EntitlementAssignment();
        assignment.setTenantId(tenantId);
        assignment.setIdentityId(identityId);
        assignment.setShadowId(shadowId);
        assignment.setEntitlementId(entitlement.getId());
        assignment.setResourceId(entitlement.getResourceId());
        assignment.setSource(EntitlementAssignmentSourceConstants.SOURCE_DIRECT);
        assignment.setStatus(EntitlementAssignment.AssignmentStatus.ASSIGNED);
        applyAssignedBy(assignment, assignedByUserId, assignedByUserName);
        EntitlementAssignment saved = assignmentRepository.save(assignment);
        logger.debug().message(EntitlementMemberConstants.LOG_MANUAL_ASSIGNMENT_CREATED)
                .detail(EntitlementMemberConstants.DETAIL_IDENTITY_ID, identityId.toString())
                .detail(EntitlementMemberConstants.DETAIL_SHADOW_ID, shadowId.toString())
                .detail(EntitlementMemberConstants.DETAIL_ENTITLEMENT_ID, entitlement.getId().toString())
                .log();
        return new AssignmentOutcome(saved, EntitlementMemberConstants.ACTION_CREATED);
    }

    private boolean ensureDirectPendingAssignment(EntitlementAssignment assignment,
                                                  UUID resourceId,
                                                  String assignedByUserId,
                                                  String assignedByUserName) {
        boolean changed = false;
        boolean directSourceChanged = false;
        if (!EntitlementAssignmentSourceConstants.SOURCE_DIRECT.equalsIgnoreCase(assignment.getSource())) {
            assignment.setSource(EntitlementAssignmentSourceConstants.SOURCE_DIRECT);
            directSourceChanged = true;
            changed = true;
        }
        if (assignment.getResourceId() == null && resourceId != null) {
            assignment.setResourceId(resourceId);
            changed = true;
        }
        if (assignment.getRuleId() != null) {
            assignment.setRuleId(null);
            changed = true;
        }
        if (assignment.getAccessRoleId() != null) {
            assignment.setAccessRoleId(null);
            changed = true;
        }
        if (assignment.getStatus() != EntitlementAssignment.AssignmentStatus.PENDING) {
            assignment.setStatus(EntitlementAssignment.AssignmentStatus.PENDING);
            changed = true;
        }
        if (directSourceChanged) {
            applyAssignedBy(assignment, assignedByUserId, assignedByUserName);
        } else if (applyMissingAssignedBy(assignment, assignedByUserId, assignedByUserName)) {
            changed = true;
        }
        return changed;
    }

    private void applyAssignedBy(EntitlementAssignment assignment, String assignedByUserId, String assignedByUserName) {
        String normalizedUserId = normalizeActorValue(assignedByUserId);
        assignment.setAssignedByUserId(normalizedUserId);
        assignment.setAssignedByUserName(Optional.ofNullable(normalizeActorValue(assignedByUserName)).orElse(normalizedUserId));
    }

    private boolean applyMissingAssignedBy(EntitlementAssignment assignment, String assignedByUserId, String assignedByUserName) {
        String normalizedUserId = normalizeActorValue(assignedByUserId);
        String normalizedUserName = Optional.ofNullable(normalizeActorValue(assignedByUserName)).orElse(normalizedUserId);
        boolean changed = false;
        if ((assignment.getAssignedByUserId() == null || assignment.getAssignedByUserId().isBlank()) && normalizedUserId != null) {
            assignment.setAssignedByUserId(normalizedUserId);
            changed = true;
        }
        if ((assignment.getAssignedByUserName() == null || assignment.getAssignedByUserName().isBlank()) && normalizedUserName != null) {
            assignment.setAssignedByUserName(normalizedUserName);
            changed = true;
        }
        return changed;
    }

    private String normalizeActorValue(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private Map<UUID, List<EntitlementAssignment>> groupAssignmentsByIdentity(String tenantId,
                                                                               List<EntitlementAssignment> assignments) {
        Map<UUID, List<EntitlementAssignment>> grouped = new LinkedHashMap<>();
        for (EntitlementAssignment assignment : assignments) {
            UUID identityId = resolveAssignmentIdentity(tenantId, assignment);
            if (identityId == null) {
                continue;
            }
            grouped.computeIfAbsent(identityId, ignored -> new ArrayList<>()).add(assignment);
        }
        return grouped;
    }

    private UUID resolveAssignmentIdentity(String tenantId, EntitlementAssignment assignment) {
        if (assignment.getIdentityId() != null) {
            return assignment.getIdentityId();
        }
        if (assignment.getShadowId() == null) {
            return null;
        }
        return shadowRepository.findById(assignment.getShadowId())
                .filter(shadow -> tenantId.equals(shadow.getTenantId()))
                .map(Shadow::getLinkedIdentityId)
                .orElse(null);
    }

    private ManualEntitlementAssignmentResult toManualAssignmentResult(UUID identityId, AssignmentOutcome outcome) {
        EntitlementAssignment assignment = outcome.assignment();
        return new ManualEntitlementAssignmentResult(
                identityId,
                assignment.getShadowId(),
                assignment.getId(),
                assignmentStatus(assignmentStatusEnum(assignment)),
                assignmentStatusReason(assignment),
                outcome.action());
    }

    private SyncManualEntitlementMembersResponse toManualAssignmentAuditResponse(
            UUID entitlementId,
            int requestedIdentities,
            List<ManualEntitlementAssignmentResult> results) {
        Map<UUID, List<ManualEntitlementAssignmentResult>> resultsByIdentity = Optional.ofNullable(results).orElse(List.of()).stream()
                .filter(result -> result.identityId() != null)
                .collect(Collectors.groupingBy(
                        ManualEntitlementAssignmentResult::identityId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<SyncEntitlementMemberResult> added = new ArrayList<>();
        List<SyncEntitlementMemberResult> skipped = new ArrayList<>();
        resultsByIdentity.forEach((identityId, identityResults) -> {
            SyncEntitlementMemberResult auditResult = new SyncEntitlementMemberResult(identityId, identityResults);
            boolean alreadyAssigned = identityResults.stream()
                    .allMatch(result -> EntitlementMemberConstants.ACTION_ALREADY_ASSIGNED.equals(result.action()));
            if (alreadyAssigned) {
                skipped.add(auditResult);
            } else {
                added.add(auditResult);
            }
        });

        int totalDirectCount = added.size() + skipped.size();
        return new SyncManualEntitlementMembersResponse(
                entitlementId,
                requestedIdentities,
                added.size(),
                0,
                skipped.size(),
                0,
                totalDirectCount,
                added,
                List.of(),
                skipped,
                List.of());
    }

    private EntitlementAssignment.AssignmentStatus assignmentStatusEnum(EntitlementAssignment assignment) {
        return assignment.getStatus() != null
                ? assignment.getStatus()
                : EntitlementAssignment.AssignmentStatus.ASSIGNED;
    }

    private String assignmentStatus(EntitlementAssignment.AssignmentStatus status) {
        return status == EntitlementAssignment.AssignmentStatus.PENDING
                ? EntitlementMemberConstants.STATUS_PENDING
                : EntitlementMemberConstants.STATUS_ASSIGNED;
    }

    private String assignmentStatusReason(EntitlementAssignment assignment) {
        return assignmentStatusEnum(assignment) == EntitlementAssignment.AssignmentStatus.PENDING
                ? EntitlementMemberConstants.REASON_NO_ACCOUNT
                : null;
    }

    private String normalizeAssignmentSource(String source) {
        if (source == null) {
            return null;
        }
        if (EntitlementAssignmentSourceConstants.SOURCE_DYNAMIC.equalsIgnoreCase(source)) {
            return EntitlementAssignmentSourceConstants.SOURCE_DYNAMIC;
        }
        if (EntitlementAssignmentSourceConstants.SOURCE_ROLE_DERIVED.equalsIgnoreCase(source)) {
            return EntitlementAssignmentSourceConstants.SOURCE_ROLE_DERIVED;
        }
        if (EntitlementAssignmentSourceConstants.SOURCE_DIRECT.equalsIgnoreCase(source)
                || EntitlementAssignmentSourceConstants.LEGACY_SOURCE_IMPORT.equalsIgnoreCase(source)
                || EntitlementAssignmentSourceConstants.LEGACY_SOURCE_MANUAL.equalsIgnoreCase(source)) {
            return EntitlementAssignmentSourceConstants.SOURCE_DIRECT;
        }
        return source;
    }

    private record AssignmentOutcome(EntitlementAssignment assignment, String action) {
    }

    // ===================================================================
    //  Governance Mapping -- bridges EntitlementMappingEngine -> Entity
    //  (from dev branch -- new feature, not present in access-portal branch)
    // ===================================================================

    /**
     * Invokes the {@link EntitlementMappingEngine} with the connector attributes
     * from the sync DTO and applies the resulting governance fields to the entity.
     *
     * <p>For new entitlements, all mapped fields are set unconditionally.
     * For existing entitlements, mapped fields overwrite the current value
     * only when the mapping engine produces a non-null result -- this preserves
     * any manual governance edits for fields that have no mapping rule configured.</p>
     */
    private void applyGovernanceMappings(Entitlement entitlement, EntitlementSyncDto syncDto) {
        if (syncDto.attributes() == null || syncDto.attributes().isEmpty()) {
            return;
        }

        Optional<EntitlementMappingResult> resultOpt = mappingEngine.applyMappings(
                syncDto.tenantId(), syncDto.resourceId(), syncDto.attributes());

        if (resultOpt.isEmpty()) {
            return; // No mapping rules configured -- governance fields stay as-is
        }

        EntitlementMappingResult result = resultOpt.get();

        if (result.displayName() != null)           entitlement.setDisplayName(result.displayName());
        if (result.description() != null)           entitlement.setDescription(result.description());
        if (result.riskLevel() != null)             entitlement.setRiskLevel(result.riskLevel());
        if (result.riskReason() != null)            entitlement.setRiskReason(result.riskReason());
        if (result.lifecycle() != null)             entitlement.setLifecycle(result.lifecycle());
        if (result.classification() != null)        entitlement.setClassification(result.classification());
        if (result.identityType() != null)          entitlement.setIdentityType(result.identityType());
        if (result.privilegeLevel() != null)        entitlement.setPrivilegeLevel(result.privilegeLevel());
        if (result.requestable() != null)           entitlement.setRequestable(result.requestable());
        if (result.requireApproval() != null)       entitlement.setRequireApproval(result.requireApproval());
        if (result.allowedAutomatedGrant() != null) entitlement.setAllowedAutomatedGrant(result.allowedAutomatedGrant());
        if (result.ownerId() != null)               entitlement.setOwnerId(result.ownerId());
        if (result.maxDurationMs() != null)         entitlement.setMaxDurationMs(result.maxDurationMs());
        if (result.autoExpireDurationMs() != null)  entitlement.setAutoExpireDurationMs(result.autoExpireDurationMs());

        if (result.customAttributes() != null && !result.customAttributes().isEmpty()) {
            Map<String, Object> mergedAttributes = entitlement.getAttributes() != null
                    ? new HashMap<>(entitlement.getAttributes())
                    : new HashMap<>();
            mergedAttributes.putAll(result.customAttributes());
            entitlement.setAttributes(mergedAttributes);
        }

        logger.debug().message("Applied governance mappings to entitlement")
                .detail("entitlementSourceUid", syncDto.sourceUid())
                .detail("resourceId", syncDto.resourceId())
                .log();
    }

    // ===================================================================
    //  Internal Helpers
    // ===================================================================

    /**
     * Extracts membership references (group DNs, role IDs) from account attributes.
     * Merged union of both branches:
     * - access-portal: memberOf, groups, isMemberOf
     * - dev: memberOf, groups, __GROUPS__, ldapGroups, isMemberOf
     */
    private List<String> extractMembershipReferences(Map<String, Object> attributes) {
        List<String> refs = new ArrayList<>();
        for (String key : List.of("memberOf", "groups", "__GROUPS__", "ldapGroups", "isMemberOf")) {
            Object val = attributes.get(key);
            if (val instanceof List<?> list) {
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        // SCIM-style: {"value": "group-id", "display": "Group Name"}
                        Object v = map.get("value");
                        if (v != null) refs.add(v.toString());
                    } else if (item != null) {
                        String s = item.toString();
                        // Handle pipe-delimited multi-values from CSV connectors (from access-portal branch)
                        if (s.contains("|")) {
                            for (String part : s.split("\\|", -1)) {
                                String trimmed = part.trim();
                                if (!trimmed.isEmpty()) refs.add(trimmed);
                            }
                        } else if (!s.isBlank()) {
                            refs.add(s);
                        }
                    }
                }
            } else if (val instanceof String s && !s.isBlank()) {
                // Handle pipe-delimited single string value (from access-portal branch)
                if (s.contains("|")) {
                    for (String part : s.split("\\|", -1)) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty()) refs.add(trimmed);
                    }
                } else {
                    refs.add(s);
                }
            }
        }
        return refs;
    }


    /**
     * Builds a JPA Specification with all filter parameters.
     * Merged: dev branch base + assignmentSource, minAssignedUsers, maxAssignedUsers,
     * and JSONB classification filter from access-portal branch.
     */
    private Specification<Entitlement> buildSpecification(String tenantId, UUID resourceId,
                                                           String search, String riskLevel,
                                                           String lifecycle, String kind,
                                                           Boolean classified, Boolean requestable,
                                                           Boolean hasOwner, UUID ownerId,
                                                           String assignmentSource,
                                                           Long minAssignedUsers,
                                                           Long maxAssignedUsers) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("tenantId"), tenantId));

            if (resourceId != null) {
                predicates.add(cb.equal(root.get("resourceId"), resourceId));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("displayName")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("sourceUid")), pattern)
                ));
            }

            if (riskLevel != null && !riskLevel.isBlank()) {
                predicates.add(cb.equal(root.get("riskLevel"), riskLevel));
            }

            if (lifecycle != null && !lifecycle.isBlank()) {
                predicates.add(cb.equal(root.get("lifecycle"), lifecycle));
            }

            if (kind != null && !kind.isBlank()) {
                predicates.add(cb.equal(root.get("kind"), kind));
            }

            // JSONB classification filter (from access-portal branch)
            if (classified != null) {
                if (classified) {
                    predicates.add(cb.and(
                            cb.isNotNull(root.get("classification")),
                            cb.gt(
                                    cb.function("jsonb_array_length", Integer.class, root.get("classification")),
                                    0
                            )
                    ));
                } else {
                    predicates.add(cb.or(
                            cb.isNull(root.get("classification")),
                            cb.equal(
                                    cb.function("jsonb_array_length", Integer.class, root.get("classification")),
                                    0
                            )
                    ));
                }
            }

            if (requestable != null) {
                predicates.add(cb.equal(root.get("requestable"), requestable));
            }

            if (hasOwner != null) {
                predicates.add(hasOwner
                        ? cb.isNotNull(root.get("ownerId"))
                        : cb.isNull(root.get("ownerId")));
            }

            if (ownerId != null) {
                predicates.add(cb.equal(root.get("ownerId"), ownerId));
            }

            // assignmentSource subquery filter (from access-portal branch)
            if (assignmentSource != null && !assignmentSource.isBlank()) {
                var subquery = query.subquery(UUID.class);
                var assignmentRoot = subquery.from(EntitlementAssignment.class);
                subquery.select(assignmentRoot.get("entitlementId"))
                        .where(
                                cb.equal(assignmentRoot.get("entitlementId"), root.get("id")),
                                cb.equal(assignmentRoot.get("tenantId"), root.get("tenantId")),
                                cb.equal(assignmentRoot.get("source"), assignmentSource)
                        );
                predicates.add(cb.exists(subquery));
            }

            // Assigned users range filter via subquery (from access-portal branch)
            if (minAssignedUsers != null || maxAssignedUsers != null) {
                var subquery = query.subquery(Long.class);
                var assignmentRoot = subquery.from(EntitlementAssignment.class);
                subquery.select(cb.countDistinct(assignmentRoot.get("shadowId")))
                        .where(
                                cb.equal(assignmentRoot.get("entitlementId"), root.get("id")),
                                cb.equal(assignmentRoot.get("tenantId"), root.get("tenantId"))
                        );
                if (minAssignedUsers != null) {
                    predicates.add(cb.ge(subquery, minAssignedUsers));
                }
                if (maxAssignedUsers != null) {
                    predicates.add(cb.le(subquery, maxAssignedUsers));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Enriches a page of Entitlement entities with batch-loaded assignment counts
     * and application names, converting to response DTOs.
     */
    private Page<EntitlementResponseDto> enrichPage(Page<Entitlement> page) {
        if (page.isEmpty()) {
            return page.map(e -> toResponseDto(e, 0L));
        }

        String tenantId = page.getContent().get(0).getTenantId();
        Set<String> customKeys = loadCustomAttributeKeys(tenantId);

        // Batch-load assignment counts
        List<UUID> entitlementIds = page.getContent().stream()
                .map(Entitlement::getId)
                .toList();

        Map<UUID, Long> countMap = new HashMap<>();
        if (!entitlementIds.isEmpty()) {
            for (Object[] row : assignmentRepository.countByEntitlementIds(entitlementIds)) {
                countMap.put((UUID) row[0], (Long) row[1]);
            }
        }

        // Batch-load resource/application names
        List<UUID> resourceIds = page.getContent().stream()
                .map(Entitlement::getResourceId)
                .distinct()
                .toList();

        Map<UUID, String> resourceNames = Collections.emptyMap();
        if (!resourceIds.isEmpty()) {
            try {
                resourceNames = applicationClient.getApplicationNames(tenantId, resourceIds);
            } catch (Exception ex) {
                logger.warn().message("Failed to resolve application names for enrichment")
                        .detail("error", ex.getMessage())
                        .log();
            }
        }

        final Map<UUID, String> names = resourceNames;
        final Set<String> customAttributeKeys = customKeys;
        return page.map(e -> toResponseDto(
                e,
                countMap.getOrDefault(e.getId(), 0L),
                customAttributeKeys,
                names.getOrDefault(e.getResourceId(), null)));
    }

    private EntitlementResponseDto toResponseDto(Entitlement e, long assignedUsersCount) {
        Set<String> customKeys = loadCustomAttributeKeys(e.getTenantId());
        return toResponseDto(e, assignedUsersCount, customKeys, null);
    }

    private EntitlementResponseDto toResponseDto(Entitlement e, long assignedUsersCount, Set<String> customKeys) {
        return toResponseDto(e, assignedUsersCount, customKeys, null);
    }

    private EntitlementResponseDto toResponseDto(Entitlement e, long assignedUsersCount,
                                                 Set<String> customKeys, String resourceName) {
        return new EntitlementResponseDto(
                e.getId(),
                e.getTenantId(),
                e.getResourceId(),
                e.getSourceUid(),
                resourceName,
                e.getDisplayName(),
                e.getDescription(),
                e.getObjectClass(),
                e.getKind(),
                e.getAttributes(),
                extractCustomAttributes(e.getAttributes(), customKeys),
                e.getLifecycle(),
                e.getRiskLevel(),
                e.getRiskReason(),
                e.isRequestable(),
                e.getClassification(),
                e.getIdentityType(),
                e.getPrivilegeLevel(),
                e.getOwnerId(),
                e.isRequireApproval(),
                e.isAllowedAutomatedGrant(),
                e.getMaxDurationMs(),
                e.getAutoExpireDurationMs(),
                assignedUsersCount,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private Set<String> loadCustomAttributeKeys(String tenantId) {
        List<String> keys = entitlementSchemaService.getCustomAttributeKeys(tenantId);
        if (keys == null || keys.isEmpty()) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(keys);
    }

    private Map<String, Object> extractCustomAttributes(Map<String, Object> attributes, Set<String> customKeys) {
        if (attributes == null || attributes.isEmpty() || customKeys == null || customKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> custom = new LinkedHashMap<>();
        for (String key : customKeys) {
            if (attributes.containsKey(key)) {
                custom.put(key, attributes.get(key));
            }
        }
        return custom;
    }
}

