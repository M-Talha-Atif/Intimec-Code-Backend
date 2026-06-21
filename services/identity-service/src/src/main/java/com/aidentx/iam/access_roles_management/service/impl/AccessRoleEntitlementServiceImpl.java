package com.aidentx.iam.access_roles_management.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleConstants;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleEntitlementConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementComparisonResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementComparisonResponse.ComparedEntitlement;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementComparisonResponse.ComparedRole;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementComparisonResponse.RoleEntitlementPresence;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementDistributionResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementDistributionResponse.ApplicationDistribution;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementDistributionResponse.ChildRoleBreakdown;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementDistributionResponse.DistributionSection;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementDistributionResponse.PrivilegeLevelDistribution;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleEntitlementDistributionResponse.Summary;
import com.aidentx.iam.access_roles_management.model.dto.response.ApplicationEntitlementGroupDto;
import com.aidentx.iam.access_roles_management.model.dto.response.EntitlementRoleDtos.EntitlementRoleAssignmentResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.EntitlementRoleDtos.EntitlementRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.GroupedRoleEntitlementsDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleEntitlement;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleEntitlementService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementClosureService;
import com.aidentx.iam.entitlement_management.client.ApplicationClient;
import com.aidentx.iam.entitlement_management.model.dto.EntitlementDtos.EntitlementResponseDto;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AccessRoleEntitlementService}.
 * <p>
 * Manages the many-to-many relationship between access roles and entitlements.
 * Handles duplicate detection to prevent linking the same entitlement twice.
 * </p>
 * <p>
 * This service bridges two bounded contexts:
 * <ul>
 *   <li>access_roles_management — owns the role-entitlement link table</li>
 *   <li>entitlement_management — owns entitlement entities, assignments, and application resolution</li>
 * </ul>
 * Cross-context access is done through injected repositories and the ApplicationClient RPC.
 * </p>
 */
@Service
@Transactional
public class AccessRoleEntitlementServiceImpl implements AccessRoleEntitlementService {

    private enum ComparisonSourceFilter {
        ALL,
        DIRECT,
        INHERITED
    }

    private final AccessRoleEntitlementRepository entitlementLinkRepository;
    private final EntitlementRepository entitlementRepository;
    private final EntitlementAssignmentRepository assignmentRepository;
    private final ApplicationClient applicationClient;
    private final AccessRoleRepository roleRepository;
    private final IdentityRoleAssignmentRepository identityRoleAssignmentRepository;
    private final RoleEntitlementClosureRepository closureRepository;
    private final RoleEntitlementClosureService closureService;
    private final IUnifiedLogger logger;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param entitlementLinkRepository role-entitlement link repository (owned by this module)
     * @param entitlementRepository     entitlement JPA repository (cross-module, used for batch queries)
     * @param assignmentRepository      entitlement assignment repository (cross-module, used for count queries)
     * @param applicationClient         RPC client for resolving application/resource names
     * @param roleRepository            access role repository for existence validation
     * @param identityRoleAssignmentRepository identity-role assignment repository for assigned-user counts
     * @param closureService            closure refresh service for effective role entitlements
     * @param loggerFactory             unified logger factory
     */
    public AccessRoleEntitlementServiceImpl(
            AccessRoleEntitlementRepository entitlementLinkRepository,
            EntitlementRepository entitlementRepository,
            EntitlementAssignmentRepository assignmentRepository,
            ApplicationClient applicationClient,
            AccessRoleRepository roleRepository,
            IdentityRoleAssignmentRepository identityRoleAssignmentRepository,
            RoleEntitlementClosureRepository closureRepository,
            RoleEntitlementClosureService closureService,
            UnifiedLoggerFactory loggerFactory) {
        this.entitlementLinkRepository = entitlementLinkRepository;
        this.entitlementRepository = entitlementRepository;
        this.assignmentRepository = assignmentRepository;
        this.applicationClient = applicationClient;
        this.roleRepository = roleRepository;
        this.identityRoleAssignmentRepository = identityRoleAssignmentRepository;
        this.closureRepository = closureRepository;
        this.closureService = closureService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleEntitlementConstants.LOG_CATEGORY);
    }

    // ===================================================================
    //  Write Operations — add / remove entitlement links
    // ===================================================================

    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementRoleResponse> getRolesForEntitlement(String tenantId, UUID entitlementId, Pageable pageable) {
        validateEntitlementBelongsToTenant(entitlementId, tenantId);

        List<AccessRoleEntitlement> links = entitlementLinkRepository.findByEntitlementId(entitlementId);
        if (links.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> roleIds = links.stream()
                .map(AccessRoleEntitlement::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, AccessRole> rolesById = roleRepository.findByIdIn(roleIds).stream()
                .filter(role -> tenantId.equals(role.getTenantId()))
                .collect(Collectors.toMap(AccessRole::getId, role -> role));

        List<EntitlementRoleResponse> rows = links.stream()
                .filter(link -> rolesById.containsKey(link.getRoleId()))
                .map(link -> toEntitlementRoleResponse(entitlementId, link, rolesById.get(link.getRoleId())))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        if (start >= rows.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, rows.size());
        }
        int end = Math.min(start + pageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(start, end), pageable, rows.size());
    }

    @Override
    public EntitlementRoleAssignmentResponse addRolesToEntitlement(String tenantId, UUID entitlementId, List<UUID> roleIds) {
        validateEntitlementBelongsToTenant(entitlementId, tenantId);
        List<UUID> uniqueRoleIds = normalizeRoleIds(roleIds);
        validateRolesBelongToTenant(uniqueRoleIds, tenantId);

        List<UUID> addedRoleIds = new ArrayList<>();
        List<UUID> skippedRoleIds = new ArrayList<>();

        for (UUID roleId : uniqueRoleIds) {
            if (entitlementLinkRepository.existsByRoleIdAndEntitlementId(roleId, entitlementId)) {
                skippedRoleIds.add(roleId);
                continue;
            }

            AccessRoleEntitlement link = new AccessRoleEntitlement();
            link.setRoleId(roleId);
            link.setEntitlementId(entitlementId);
            entitlementLinkRepository.save(link);
            addedRoleIds.add(roleId);
        }

        refreshChangedRoles(tenantId, addedRoleIds);
        return entitlementRoleMutationResponse(entitlementId, roleIds, uniqueRoleIds, addedRoleIds,
                Collections.emptyList(), Collections.emptyList(), skippedRoleIds, 0, skippedRoleIds.size());
    }

    @Override
    public EntitlementRoleAssignmentResponse removeRolesFromEntitlement(String tenantId, UUID entitlementId, List<UUID> roleIds) {
        validateEntitlementBelongsToTenant(entitlementId, tenantId);
        List<UUID> uniqueRoleIds = normalizeRoleIds(roleIds);
        validateRolesBelongToTenant(uniqueRoleIds, tenantId);

        Set<UUID> existingRoleIds = entitlementLinkRepository.findByEntitlementId(entitlementId).stream()
                .map(AccessRoleEntitlement::getRoleId)
                .collect(Collectors.toSet());
        List<UUID> removedRoleIds = uniqueRoleIds.stream()
                .filter(existingRoleIds::contains)
                .collect(Collectors.toList());
        List<UUID> skippedRoleIds = uniqueRoleIds.stream()
                .filter(roleId -> !existingRoleIds.contains(roleId))
                .collect(Collectors.toList());

        if (!removedRoleIds.isEmpty()) {
            entitlementLinkRepository.deleteByRoleIdInAndEntitlementId(removedRoleIds, entitlementId);
            refreshChangedRoles(tenantId, removedRoleIds);
        }

        return entitlementRoleMutationResponse(entitlementId, roleIds, uniqueRoleIds, Collections.emptyList(),
                removedRoleIds, Collections.emptyList(), skippedRoleIds, skippedRoleIds.size(), 0);
    }

    @Override
    public EntitlementRoleAssignmentResponse syncRolesForEntitlement(String tenantId, UUID entitlementId, List<UUID> roleIds) {
        validateEntitlementBelongsToTenant(entitlementId, tenantId);
        List<UUID> desiredRoleIds = normalizeRoleIds(roleIds);
        validateRolesBelongToTenant(desiredRoleIds, tenantId);

        Set<UUID> desiredSet = new LinkedHashSet<>(desiredRoleIds);
        Set<UUID> existingSet = entitlementLinkRepository.findByEntitlementId(entitlementId).stream()
                .map(AccessRoleEntitlement::getRoleId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UUID> addedRoleIds = desiredRoleIds.stream()
                .filter(roleId -> !existingSet.contains(roleId))
                .collect(Collectors.toList());
        List<UUID> removedRoleIds = existingSet.stream()
                .filter(roleId -> !desiredSet.contains(roleId))
                .collect(Collectors.toList());
        List<UUID> unchangedRoleIds = desiredRoleIds.stream()
                .filter(existingSet::contains)
                .collect(Collectors.toList());

        for (UUID roleId : addedRoleIds) {
            AccessRoleEntitlement link = new AccessRoleEntitlement();
            link.setRoleId(roleId);
            link.setEntitlementId(entitlementId);
            entitlementLinkRepository.save(link);
        }
        if (!removedRoleIds.isEmpty()) {
            entitlementLinkRepository.deleteByRoleIdInAndEntitlementId(removedRoleIds, entitlementId);
        }

        List<UUID> changedRoleIds = new ArrayList<>();
        changedRoleIds.addAll(addedRoleIds);
        changedRoleIds.addAll(removedRoleIds);
        refreshChangedRoles(tenantId, changedRoleIds);

        return entitlementRoleMutationResponse(entitlementId, roleIds, desiredRoleIds, addedRoleIds,
                removedRoleIds, unchangedRoleIds, Collections.emptyList(), 0, 0);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Skips entitlements that are already linked to avoid duplicates.
     * </p>
     *
     * @throws EntityNotFoundException if the role does not exist
     */
    @Override
    public void addEntitlements(String tenantId, UUID roleId, List<UUID> entitlementIds) {
        validateRoleExists(roleId);

        int addedCount = 0;
        for (UUID entitlementId : entitlementIds) {
            if (entitlementLinkRepository.existsByRoleIdAndEntitlementId(roleId, entitlementId)) {
                logger.debug()
                        .category(AccessRoleEntitlementConstants.LOG_CATEGORY)
                        .message(AccessRoleEntitlementConstants.LOG_ENTITLEMENT_LINK_SKIPPED)
                        .detail(AccessRoleEntitlementConstants.DETAIL_ROLE_ID, roleId.toString())
                        .detail(AccessRoleEntitlementConstants.DETAIL_ENTITLEMENT_ID, entitlementId.toString())
                        .log();
                continue;
            }

            AccessRoleEntitlement link = new AccessRoleEntitlement();
            link.setRoleId(roleId);
            link.setEntitlementId(entitlementId);
            entitlementLinkRepository.save(link);
            addedCount++;
        }

        if (addedCount > 0) {
            closureService.refreshAfterEntitlementsChanged(tenantId, roleId);
        }

        logger.info()
                .category(AccessRoleEntitlementConstants.LOG_CATEGORY)
                .message(AccessRoleEntitlementConstants.LOG_ENTITLEMENTS_ADDED)
                .detail(AccessRoleEntitlementConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleEntitlementConstants.DETAIL_ADDED_COUNT, String.valueOf(addedCount))
                .log();
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if the role does not exist
     */
    @Override
    public void removeEntitlements(String tenantId, UUID roleId, List<UUID> entitlementIds) {
        validateRoleExists(roleId);

        int removedCount = 0;
        for (UUID entitlementId : entitlementIds) {
            if (entitlementLinkRepository.existsByRoleIdAndEntitlementId(roleId, entitlementId)) {
                entitlementLinkRepository.deleteByRoleIdAndEntitlementId(roleId, entitlementId);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            closureService.refreshAfterEntitlementsChanged(tenantId, roleId);
        }

        logger.info()
                .category(AccessRoleEntitlementConstants.LOG_CATEGORY)
                .message(AccessRoleEntitlementConstants.LOG_ENTITLEMENTS_REMOVED)
                .detail(AccessRoleEntitlementConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleEntitlementConstants.DETAIL_REMOVED_COUNT, String.valueOf(removedCount))
                .log();
    }

    // ===================================================================
    //  Read Operations — flat list and grouped views
    // ===================================================================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<UUID> getEntitlementIds(UUID roleId) {
        List<UUID> ids = entitlementLinkRepository.findByRoleId(roleId)
                .stream()
                .map(AccessRoleEntitlement::getEntitlementId)
                .collect(Collectors.toList());

        logger.debug()
                .category(AccessRoleEntitlementConstants.LOG_CATEGORY)
                .message(AccessRoleEntitlementConstants.LOG_ENTITLEMENTS_FETCHED)
                .detail(AccessRoleEntitlementConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleEntitlementConstants.DETAIL_TOTAL_COUNT, String.valueOf(ids.size()))
                .log();

        return ids;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Loads all linked entitlement IDs, paginates the ID list in-memory,
     * then batch-resolves entitlement details using the entitlement repository directly
     * (not findById per item) to avoid N+1 queries.
     * Application names are resolved in a single batch RPC call.
     * </p>
     *
     * @param roleId   the role whose linked entitlements should be resolved
     * @param tenantId the tenant identifier for application name resolution
     * @param pageable pagination and sorting parameters
     * @return paginated list of fully resolved entitlement details
     * @throws EntityNotFoundException if the role does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EntitlementResponseDto> getEntitlementDetails(UUID roleId, String tenantId, Pageable pageable) {
        validateRoleExists(roleId);

        // Load all linked entitlement IDs (lightweight: only UUIDs from link table)
        List<UUID> allEntitlementIds = loadLinkedEntitlementIds(roleId);

        if (allEntitlementIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // Manual pagination over the ID list
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allEntitlementIds.size());

        if (start >= allEntitlementIds.size()) {
            return Page.empty(pageable);
        }

        List<UUID> pageIds = allEntitlementIds.subList(start, end);

        // Batch-load entitlement entities for this page (1 query instead of N)
        List<Entitlement> entities = entitlementRepository.findAllById(pageIds);

        // Batch-load assignment counts (1 query)
        Map<UUID, Long> countMap = batchLoadAssignmentCounts(
                entities.stream().map(Entitlement::getId).collect(Collectors.toList())
        );

        // Batch-resolve application names (1 RPC)
        List<UUID> resourceIds = entities.stream()
                .map(Entitlement::getResourceId)
                .distinct()
                .collect(Collectors.toList());
        Map<UUID, String> appNameMap = applicationClient.getApplicationNames(tenantId, resourceIds);

        // Map entities to DTOs
        List<EntitlementResponseDto> details = entities.stream()
                .map(e -> toResponseDto(e, countMap.getOrDefault(e.getId(), 0L), appNameMap))
                .collect(Collectors.toList());

        logger.debug()
                .category(AccessRoleEntitlementConstants.LOG_CATEGORY)
                .message(AccessRoleEntitlementConstants.LOG_ENTITLEMENT_DETAILS_FETCHED)
                .detail(AccessRoleEntitlementConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleEntitlementConstants.DETAIL_REQUESTED_COUNT, String.valueOf(pageIds.size()))
                .detail(AccessRoleEntitlementConstants.DETAIL_RESOLVED_COUNT, String.valueOf(details.size()))
                .log();

        return new PageImpl<>(details, pageable, allEntitlementIds.size());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Two-level paginated retrieval of role entitlements grouped by application.
     * </p>
     * <p>
     * Query plan (bounded, no N+1):
     * <ol>
     *   <li>Load all linked entitlement IDs for the role (1 query — link table only)</li>
     *   <li>Find distinct resourceIds from those entitlements (1 query)</li>
     *   <li>Paginate resourceIds in-memory for app-level pagination (0 queries)</li>
     *   <li>For each app on current page, load paginated entitlements (appSize queries, bounded to page size)</li>
     *   <li>Batch-load assignment counts for ALL entitlements on current page (1 query)</li>
     *   <li>Batch-load high-risk counts for ALL apps on current page (1 query via batch method)</li>
     *   <li>Resolve application names in one batch RPC call (1 call)</li>
     * </ol>
     * Total: 4 + appSize queries + 1 RPC. For appSize=5, that's 10 operations max.
     * </p>
     *
     * @param roleId       the role whose entitlements to retrieve
     * @param tenantId     tenant identifier for application name resolution
     * @param appPageable  pagination for the application level
     * @param entPageable  pagination for the entitlement level within each application
     * @return grouped response with nested pagination
     * @throws EntityNotFoundException if the role does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public GroupedRoleEntitlementsDto getGroupedEntitlementDetails(
            UUID roleId,
            String tenantId,
            Pageable appPageable,
            Pageable entPageable,
            String search) {

        validateRoleExists(roleId);
        String normalizedSearch = normalizeSearch(search);

        // Step 1: Load all linked entitlement IDs (lightweight: link table only)
        List<UUID> allEntitlementIds = loadLinkedEntitlementIds(roleId);

        if (allEntitlementIds.isEmpty()) {
            return new GroupedRoleEntitlementsDto(0, 0, Page.empty(appPageable));
        }

        // Step 2: Find all distinct applications for linked entitlements
        List<UUID> allResourceIds = entitlementRepository.findDistinctResourceIdsByEntitlementIds(allEntitlementIds);

        if (allResourceIds.isEmpty()) {
            return new GroupedRoleEntitlementsDto(0, allEntitlementIds.size(), Page.empty(appPageable));
        }

        Map<UUID, String> allAppNameMap = applicationClient.getApplicationNames(tenantId, allResourceIds);

        if (normalizedSearch != null) {
            return getSearchFilteredGroupedEntitlementDetails(
                    roleId,
                    allEntitlementIds,
                    allResourceIds,
                    allAppNameMap,
                    normalizedSearch,
                    appPageable,
                    entPageable);
        }

        int totalApplications = allResourceIds.size();

        // Step 3: Paginate application list in-memory
        int appStart = (int) appPageable.getOffset();
        int appEnd = Math.min(appStart + appPageable.getPageSize(), allResourceIds.size());

        if (appStart >= allResourceIds.size()) {
            return new GroupedRoleEntitlementsDto(totalApplications, allEntitlementIds.size(), Page.empty(appPageable));
        }

        List<UUID> pageResourceIds = allResourceIds.subList(appStart, appEnd);

        // Step 4: For each app on current page, load paginated entitlements
        Map<UUID, Page<Entitlement>> entitlementPages = new LinkedHashMap<>();
        List<UUID> allPageEntitlementIds = new ArrayList<>();

        for (UUID resourceId : pageResourceIds) {
            Page<Entitlement> entPage = entitlementRepository.findByIdInAndResourceId(
                    allEntitlementIds, resourceId, entPageable);
            entitlementPages.put(resourceId, entPage);
            entPage.getContent().forEach(e -> allPageEntitlementIds.add(e.getId()));
        }

        // Step 6: Batch-load assignment counts for all entitlements on this page (1 query)
        Map<UUID, Long> countMap = batchLoadAssignmentCounts(allPageEntitlementIds);

        // Step 7: Batch-load high-risk counts per app (1 query per app — bounded by appSize)
        Map<UUID, Long> highRiskMap = new HashMap<>();
        for (UUID resourceId : pageResourceIds) {
            highRiskMap.put(resourceId,
                    entitlementRepository.countHighRiskByIdsAndResourceId(allEntitlementIds, resourceId));
        }

        // Step 8: Assemble application groups
        List<ApplicationEntitlementGroupDto> appGroups = new ArrayList<>();

        for (UUID resourceId : pageResourceIds) {
            Page<Entitlement> entPage = entitlementPages.get(resourceId);
            String appName = allAppNameMap.getOrDefault(resourceId, "Unknown Application");
            long entitlementCount = entPage.getTotalElements();
            long highRiskCount = highRiskMap.getOrDefault(resourceId, 0L);

            Page<EntitlementResponseDto> dtosPage = entPage.map(e ->
                    toResponseDto(e, countMap.getOrDefault(e.getId(), 0L), allAppNameMap));

            appGroups.add(new ApplicationEntitlementGroupDto(
                    resourceId, appName, entitlementCount, highRiskCount, dtosPage));
        }

        Page<ApplicationEntitlementGroupDto> applicationsPage = new PageImpl<>(
                appGroups, appPageable, totalApplications);

        logger.debug()
                .category(AccessRoleEntitlementConstants.LOG_CATEGORY)
                .message(AccessRoleEntitlementConstants.LOG_GROUPED_ENTITLEMENTS_FETCHED)
                .detail(AccessRoleEntitlementConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail("totalApplications", String.valueOf(totalApplications))
                .detail("totalEntitlements", String.valueOf(allEntitlementIds.size()))
                .detail("appPage", String.valueOf(appPageable.getPageNumber()))
                .detail("entPage", String.valueOf(entPageable.getPageNumber()))
                .log();

        return new GroupedRoleEntitlementsDto(totalApplications, allEntitlementIds.size(), applicationsPage);
    }

    private GroupedRoleEntitlementsDto getSearchFilteredGroupedEntitlementDetails(UUID roleId,
                                                                                  List<UUID> allEntitlementIds,
                                                                                  List<UUID> allResourceIds,
                                                                                  Map<UUID, String> appNameMap,
                                                                                  String normalizedSearch,
                                                                                  Pageable appPageable,
                                                                                  Pageable entPageable) {
        List<Entitlement> linkedEntitlements = entitlementRepository.findAllById(allEntitlementIds);
        Map<UUID, List<Entitlement>> entitlementsByResource = linkedEntitlements.stream()
                .filter(entitlement -> entitlement.getResourceId() != null)
                .collect(Collectors.groupingBy(
                        Entitlement::getResourceId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<UUID, List<Entitlement>> matchedEntitlementsByResource = new LinkedHashMap<>();
        for (UUID resourceId : allResourceIds) {
            List<Entitlement> appEntitlements = entitlementsByResource.getOrDefault(resourceId, Collections.emptyList());
            boolean appNameMatches = containsIgnoreCase(appNameMap.get(resourceId), normalizedSearch);
            List<Entitlement> matchedEntitlements = appNameMatches
                    ? appEntitlements
                    : appEntitlements.stream()
                            .filter(entitlement -> matchesEntitlementSearch(entitlement, normalizedSearch))
                            .toList();

            if (!matchedEntitlements.isEmpty()) {
                matchedEntitlementsByResource.put(resourceId, matchedEntitlements);
            }
        }

        List<UUID> matchedResourceIds = new ArrayList<>(matchedEntitlementsByResource.keySet());
        int totalApplications = matchedResourceIds.size();
        long totalEntitlements = matchedEntitlementsByResource.values().stream()
                .mapToLong(List::size)
                .sum();

        int appStart = (int) appPageable.getOffset();
        int appEnd = Math.min(appStart + appPageable.getPageSize(), totalApplications);
        if (appStart >= totalApplications) {
            return new GroupedRoleEntitlementsDto(totalApplications, totalEntitlements, Page.empty(appPageable));
        }

        List<UUID> pageResourceIds = matchedResourceIds.subList(appStart, appEnd);
        List<UUID> allPageEntitlementIds = pageResourceIds.stream()
                .flatMap(resourceId -> matchedEntitlementsByResource.getOrDefault(resourceId, Collections.emptyList()).stream())
                .map(Entitlement::getId)
                .toList();
        Map<UUID, Long> countMap = batchLoadAssignmentCounts(allPageEntitlementIds);

        List<ApplicationEntitlementGroupDto> appGroups = new ArrayList<>();
        for (UUID resourceId : pageResourceIds) {
            List<Entitlement> appEntitlements = matchedEntitlementsByResource.getOrDefault(resourceId, Collections.emptyList());
            Page<Entitlement> entPage = toPage(appEntitlements, entPageable);
            String appName = appNameMap.getOrDefault(resourceId, "Unknown Application");
            long highRiskCount = appEntitlements.stream()
                    .filter(entitlement -> "HIGH".equalsIgnoreCase(entitlement.getRiskLevel()))
                    .count();

            Page<EntitlementResponseDto> dtosPage = entPage.map(entitlement ->
                    toResponseDto(entitlement, countMap.getOrDefault(entitlement.getId(), 0L), appNameMap));

            appGroups.add(new ApplicationEntitlementGroupDto(
                    resourceId,
                    appName,
                    appEntitlements.size(),
                    highRiskCount,
                    dtosPage));
        }

        logger.debug()
                .category(AccessRoleEntitlementConstants.LOG_CATEGORY)
                .message(AccessRoleEntitlementConstants.LOG_GROUPED_ENTITLEMENTS_FETCHED)
                .detail(AccessRoleEntitlementConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail("search", normalizedSearch)
                .detail("totalApplications", String.valueOf(totalApplications))
                .detail("totalEntitlements", String.valueOf(totalEntitlements))
                .detail("appPage", String.valueOf(appPageable.getPageNumber()))
                .detail("entPage", String.valueOf(entPageable.getPageNumber()))
                .log();

        return new GroupedRoleEntitlementsDto(
                totalApplications,
                totalEntitlements,
                new PageImpl<>(appGroups, appPageable, totalApplications));
    }

    @Override
    @Transactional(readOnly = true)
    public AccessRoleEntitlementComparisonResponse compareRoleEntitlements(String tenantId,
                                                                           UUID roleAId,
                                                                           UUID roleBId,
                                                                           String sourceFilter,
                                                                           Pageable pageable) {
        AccessRole roleA = findRoleForTenantOrThrow(tenantId, roleAId);
        AccessRole roleB = findRoleForTenantOrThrow(tenantId, roleBId);
        ComparisonSourceFilter normalizedFilter = normalizeComparisonSourceFilter(sourceFilter);

        Page<Object[]> overlapPage = closureRepository.findOverlappingEntitlementSummaries(
                tenantId,
                roleAId,
                roleBId,
                normalizedFilter.name(),
                pageable);

        List<UUID> entitlementIds = overlapPage.getContent().stream()
                .map(row -> (UUID) row[0])
                .toList();
        Map<UUID, Entitlement> entitlementById = loadEntitlementsById(
                tenantId,
                new LinkedHashSet<>(entitlementIds));
        List<UUID> resourceIds = entitlementById.values().stream()
                .map(Entitlement::getResourceId)
                .filter(resourceId -> resourceId != null)
                .distinct()
                .toList();
        Map<UUID, String> applicationNames = resourceIds.isEmpty()
                ? Collections.emptyMap()
                : applicationClient.getApplicationNames(tenantId, resourceIds);

        List<ComparedEntitlement> rows = overlapPage.getContent().stream()
                .map(row -> toComparedEntitlement(row, entitlementById.get((UUID) row[0]), applicationNames))
                .toList();

        return new AccessRoleEntitlementComparisonResponse(
                toComparedRole(roleA),
                toComparedRole(roleB),
                normalizedFilter.name(),
                overlapPage.getTotalElements(),
                new PageImpl<>(rows, pageable, overlapPage.getTotalElements()));
    }

    @Override
    @Transactional(readOnly = true)
    public AccessRoleEntitlementDistributionResponse getEntitlementDistribution(String tenantId, UUID roleId) {
        AccessRole role = findRoleForTenantOrThrow(tenantId, roleId);
        long assignedUserCount = identityRoleAssignmentRepository.countDistinctIdentityIdByTenantIdAndRoleIdAndStatus(
                tenantId,
                roleId,
                IdentityRoleAssignment.Status.ACTIVE);

        List<RoleEntitlementClosure> closureRows = closureRepository.findByTenantIdAndRoleId(tenantId, roleId);

        Set<UUID> directEntitlementIds = closureRows.stream()
                .filter(RoleEntitlementClosure::isDirect)
                .map(RoleEntitlementClosure::getEntitlementId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<UUID> inheritedEntitlementIds = closureRows.stream()
                .filter(row -> !row.isDirect())
                .map(RoleEntitlementClosure::getEntitlementId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<UUID> effectiveEntitlementIds = closureRows.stream()
                .map(RoleEntitlementClosure::getEntitlementId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, Entitlement> entitlementById = loadEntitlementsById(tenantId, effectiveEntitlementIds);

        List<Entitlement> directEntitlements = resolveEntitlements(directEntitlementIds, entitlementById);
        List<Entitlement> inheritedEntitlements = resolveEntitlements(inheritedEntitlementIds, entitlementById);
        List<Entitlement> effectiveEntitlements = resolveEntitlements(effectiveEntitlementIds, entitlementById);

        List<UUID> resourceIds = effectiveEntitlements.stream()
                .map(Entitlement::getResourceId)
                .filter(resourceId -> resourceId != null)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, String> applicationNames = resourceIds.isEmpty()
                ? Collections.emptyMap()
                : applicationClient.getApplicationNames(tenantId, resourceIds);

        DistributionSection direct = buildDistributionSection(directEntitlements, applicationNames);
        DistributionSection inherited = buildDistributionSection(inheritedEntitlements, applicationNames);
        DistributionSection effective = buildDistributionSection(effectiveEntitlements, applicationNames);
        List<ChildRoleBreakdown> childRoleBreakdown = buildChildRoleBreakdown(
                tenantId,
                closureRows,
                entitlementById,
                applicationNames);

        Summary summary = new Summary(
                direct.entitlementCount(),
                inherited.entitlementCount(),
                effective.entitlementCount(),
                direct.applicationCount(),
                inherited.applicationCount(),
                effective.applicationCount(),
                childRoleBreakdown.size(),
                assignedUserCount
        );

        return new AccessRoleEntitlementDistributionResponse(
                role.getId(),
                role.getName(),
                displayNameOrName(role),
                role.getTenantId(),
                summary,
                direct,
                inherited,
                effective,
                childRoleBreakdown
        );
    }

    // ===================================================================
    //  Private Helpers
    // ===================================================================

    private AccessRole findRoleForTenantOrThrow(String tenantId, UUID roleId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }

        return roleRepository.findById(roleId)
                .filter(role -> tenantId.equals(role.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(AccessRoleConstants.ERROR_ROLE_NOT_FOUND, roleId)));
    }

    private ComparisonSourceFilter normalizeComparisonSourceFilter(String sourceFilter) {
        if (sourceFilter == null || sourceFilter.isBlank()) {
            return ComparisonSourceFilter.ALL;
        }
        try {
            return ComparisonSourceFilter.valueOf(sourceFilter.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("sourceFilter must be one of: ALL, DIRECT, INHERITED");
        }
    }

    private ComparedRole toComparedRole(AccessRole role) {
        return new ComparedRole(
                role.getId(),
                role.getName(),
                role.getDisplayName(),
                role.getType(),
                role.getRiskLevel());
    }

    private ComparedEntitlement toComparedEntitlement(Object[] row,
                                                      Entitlement entitlement,
                                                      Map<UUID, String> applicationNames) {
        UUID entitlementId = (UUID) row[0];
        UUID applicationId = entitlement != null ? entitlement.getResourceId() : null;
        String applicationName = applicationId != null
                ? applicationNames.getOrDefault(applicationId, "Unknown Application")
                : null;

        return new ComparedEntitlement(
                entitlementId,
                entitlement != null ? entitlement.getDisplayName() : null,
                entitlement != null ? entitlement.getDescription() : null,
                applicationId,
                applicationName,
                entitlement != null ? entitlement.getSourceUid() : null,
                entitlement != null ? entitlement.getObjectClass() : null,
                entitlement != null ? entitlement.getKind() : null,
                entitlement != null ? entitlement.getLifecycle() : null,
                entitlement != null ? entitlement.getRiskLevel() : null,
                entitlement != null ? entitlement.getRiskReason() : null,
                entitlement != null ? entitlement.getIdentityType() : null,
                entitlement != null ? entitlement.getPrivilegeLevel() : null,
                toPresence(row[1], row[2], row[3]),
                toPresence(row[4], row[5], row[6]));
    }

    private RoleEntitlementPresence toPresence(Object directCountValue,
                                               Object inheritedCountValue,
                                               Object sourceRoleCountValue) {
        long directCount = numberToLong(directCountValue);
        long inheritedCount = numberToLong(inheritedCountValue);
        boolean direct = directCount > 0;
        boolean inherited = inheritedCount > 0;
        List<String> accessTypes = new ArrayList<>(2);
        if (direct) {
            accessTypes.add(ComparisonSourceFilter.DIRECT.name());
        }
        if (inherited) {
            accessTypes.add(ComparisonSourceFilter.INHERITED.name());
        }
        return new RoleEntitlementPresence(
                direct,
                inherited,
                accessTypes,
                numberToLong(sourceRoleCountValue));
    }

    private long numberToLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    private Map<UUID, Entitlement> loadEntitlementsById(String tenantId, Set<UUID> entitlementIds) {
        if (entitlementIds == null || entitlementIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return entitlementRepository.findAllById(entitlementIds).stream()
                .filter(entitlement -> tenantId.equals(entitlement.getTenantId()))
                .collect(Collectors.toMap(
                        Entitlement::getId,
                        entitlement -> entitlement,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private List<Entitlement> resolveEntitlements(Set<UUID> entitlementIds, Map<UUID, Entitlement> entitlementById) {
        if (entitlementIds == null || entitlementIds.isEmpty()) {
            return Collections.emptyList();
        }

        return entitlementIds.stream()
                .map(entitlementById::get)
                .filter(entitlement -> entitlement != null)
                .collect(Collectors.toList());
    }

    private DistributionSection buildDistributionSection(List<Entitlement> entitlements,
                                                         Map<UUID, String> applicationNames) {
        List<Entitlement> uniqueEntitlements = uniqueEntitlements(entitlements);

        return new DistributionSection(
                uniqueEntitlements.size(),
                countDistinctApplications(uniqueEntitlements),
                buildPrivilegeLevelDistribution(uniqueEntitlements),
                buildApplicationDistribution(uniqueEntitlements, applicationNames)
        );
    }

    private List<Entitlement> uniqueEntitlements(List<Entitlement> entitlements) {
        if (entitlements == null || entitlements.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, Entitlement> unique = new LinkedHashMap<>();
        for (Entitlement entitlement : entitlements) {
            if (entitlement != null && entitlement.getId() != null) {
                unique.putIfAbsent(entitlement.getId(), entitlement);
            }
        }
        return new ArrayList<>(unique.values());
    }

    private int countDistinctApplications(List<Entitlement> entitlements) {
        return (int) entitlements.stream()
                .map(Entitlement::getResourceId)
                .filter(resourceId -> resourceId != null)
                .distinct()
                .count();
    }

    private List<PrivilegeLevelDistribution> buildPrivilegeLevelDistribution(List<Entitlement> entitlements) {
        List<Entitlement> uniqueEntitlements = uniqueEntitlements(entitlements);
        int total = uniqueEntitlements.size();

        if (total == 0) {
            return Collections.emptyList();
        }

        Map<String, Long> counts = uniqueEntitlements.stream()
                .collect(Collectors.groupingBy(
                        entitlement -> normalizePrivilegeLevel(entitlement.getPrivilegeLevel()),
                        TreeMap::new,
                        Collectors.counting()
                ));

        return counts.entrySet().stream()
                .sorted(Comparator
                        .<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                        .reversed()
                        .thenComparing(Map.Entry::getKey))
                .map(entry -> new PrivilegeLevelDistribution(
                        entry.getKey(),
                        entry.getValue(),
                        percentage(entry.getValue(), total)
                ))
                .collect(Collectors.toList());
    }

    private List<ApplicationDistribution> buildApplicationDistribution(List<Entitlement> entitlements,
                                                                       Map<UUID, String> applicationNames) {
        List<Entitlement> uniqueEntitlements = uniqueEntitlements(entitlements);

        if (uniqueEntitlements.isEmpty()) {
            return Collections.emptyList();
        }

        Map<UUID, Long> counts = uniqueEntitlements.stream()
                .map(Entitlement::getResourceId)
                .filter(resourceId -> resourceId != null)
                .collect(Collectors.groupingBy(
                        resourceId -> resourceId,
                        LinkedHashMap::new,
                        Collectors.counting()
                ));

        return counts.entrySet().stream()
                .map(entry -> new ApplicationDistribution(
                        entry.getKey(),
                        applicationNames.getOrDefault(entry.getKey(), "Unknown Application"),
                        entry.getValue()
                ))
                .sorted(Comparator
                        .comparing(ApplicationDistribution::entitlementCount)
                        .reversed()
                        .thenComparing(ApplicationDistribution::applicationName))
                .collect(Collectors.toList());
    }

    private List<ChildRoleBreakdown> buildChildRoleBreakdown(String tenantId,
                                                             List<RoleEntitlementClosure> closureRows,
                                                             Map<UUID, Entitlement> entitlementById,
                                                             Map<UUID, String> applicationNames) {
        Map<UUID, Set<UUID>> entitlementIdsBySourceRole = closureRows.stream()
                .filter(row -> !row.isDirect())
                .collect(Collectors.groupingBy(
                        RoleEntitlementClosure::getSourceRoleId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                RoleEntitlementClosure::getEntitlementId,
                                Collectors.toCollection(LinkedHashSet::new)
                        )
                ));

        if (entitlementIdsBySourceRole.isEmpty()) {
            return Collections.emptyList();
        }

        Set<UUID> sourceRoleIds = entitlementIdsBySourceRole.keySet();
        Map<UUID, AccessRole> rolesById = roleRepository.findByIdIn(sourceRoleIds).stream()
                .filter(role -> tenantId.equals(role.getTenantId()))
                .collect(Collectors.toMap(
                        AccessRole::getId,
                        role -> role,
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        return entitlementIdsBySourceRole.entrySet().stream()
                .map(entry -> {
                    UUID sourceRoleId = entry.getKey();
                    AccessRole sourceRole = rolesById.get(sourceRoleId);
                    List<Entitlement> entitlements = resolveEntitlements(entry.getValue(), entitlementById);
                    DistributionSection section = buildDistributionSection(entitlements, applicationNames);

                    return new ChildRoleBreakdown(
                            sourceRoleId,
                            sourceRole != null ? sourceRole.getName() : "Unknown Role",
                            sourceRole != null ? displayNameOrName(sourceRole) : "Unknown Role",
                            section.entitlementCount(),
                            section.applicationCount(),
                            section.privilegeLevelDistribution()
                    );
                })
                .filter(row -> row.entitlementCount() > 0)
                .sorted(Comparator
                        .comparing(ChildRoleBreakdown::entitlementCount)
                        .reversed()
                        .thenComparing(ChildRoleBreakdown::childRoleName))
                .collect(Collectors.toList());
    }

    private String normalizePrivilegeLevel(String privilegeLevel) {
        if (privilegeLevel == null || privilegeLevel.isBlank()) {
            return "Unspecified";
        }

        String normalized = privilegeLevel.trim()
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ");
        String lower = normalized.toLowerCase(java.util.Locale.ROOT);

        return switch (lower) {
            case "read" -> "Read";
            case "write" -> "Write";
            case "admin" -> "Admin";
            case "privileged" -> "Privileged";
            default -> toTitleCase(normalized);
        };
    }

    private String displayNameOrName(AccessRole role) {
        if (role.getDisplayName() != null && !role.getDisplayName().isBlank()) {
            return role.getDisplayName();
        }
        if (role.getName() != null && !role.getName().isBlank()) {
            return role.getName();
        }
        return "Unknown Role";
    }

    private String toTitleCase(String value) {
        String[] words = value.toLowerCase(java.util.Locale.ROOT).split(" ");
        List<String> titleCasedWords = new ArrayList<>(words.length);
        for (String word : words) {
            if (!word.isBlank()) {
                titleCasedWords.add(Character.toUpperCase(word.charAt(0)) + word.substring(1));
            }
        }
        return titleCasedWords.isEmpty() ? "Unspecified" : String.join(" ", titleCasedWords);
    }

    private double percentage(long count, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round((count * 10000.0) / total) / 100.0;
    }

    private void validateEntitlementBelongsToTenant(UUID entitlementId, String tenantId) {
        if (entitlementId == null) {
            throw new IllegalArgumentException("Entitlement ID is required");
        }
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
        Entitlement entitlement = entitlementRepository.findById(entitlementId)
                .orElseThrow(() -> new EntityNotFoundException("Entitlement not found: " + entitlementId));
        if (!tenantId.equals(entitlement.getTenantId())) {
            throw new EntityNotFoundException("Entitlement not found: " + entitlementId);
        }
    }

    private List<UUID> normalizeRoleIds(List<UUID> roleIds) {
        if (roleIds == null) {
            throw new IllegalArgumentException("Role IDs list is required");
        }
        LinkedHashSet<UUID> uniqueRoleIds = new LinkedHashSet<>();
        for (UUID roleId : roleIds) {
            if (roleId == null) {
                throw new IllegalArgumentException("Role ID is required");
            }
            uniqueRoleIds.add(roleId);
        }
        return new ArrayList<>(uniqueRoleIds);
    }

    private String normalizeSearch(String search) {
        if (search == null) {
            return null;
        }
        String trimmed = search.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean matchesEntitlementSearch(Entitlement entitlement, String search) {
        if (entitlement == null) {
            return false;
        }
        return containsIgnoreCase(entitlement.getId(), search)
                || containsIgnoreCase(entitlement.getDisplayName(), search)
                || containsIgnoreCase(entitlement.getDescription(), search)
                || containsIgnoreCase(entitlement.getSourceUid(), search)
                || containsIgnoreCase(entitlement.getObjectClass(), search)
                || containsIgnoreCase(entitlement.getKind(), search)
                || containsIgnoreCase(entitlement.getLifecycle(), search)
                || containsIgnoreCase(entitlement.getRiskLevel(), search)
                || containsIgnoreCase(entitlement.getRiskReason(), search)
                || containsIgnoreCase(entitlement.getIdentityType(), search)
                || containsIgnoreCase(entitlement.getPrivilegeLevel(), search)
                || containsAnyIgnoreCase(entitlement.getClassification(), search)
                || containsAnyIgnoreCase(entitlement.getAttributes(), search);
    }

    private boolean containsAnyIgnoreCase(List<String> values, String search) {
        return values != null && values.stream().anyMatch(value -> containsIgnoreCase(value, search));
    }

    private boolean containsAnyIgnoreCase(Map<String, Object> values, String search) {
        if (values == null || values.isEmpty()) {
            return false;
        }
        return values.entrySet().stream()
                .anyMatch(entry -> containsIgnoreCase(entry.getKey(), search)
                        || containsIgnoreCase(entry.getValue(), search));
    }

    private boolean containsIgnoreCase(Object value, String search) {
        return value != null
                && search != null
                && value.toString().toLowerCase(java.util.Locale.ROOT)
                .contains(search.toLowerCase(java.util.Locale.ROOT));
    }

    private Page<Entitlement> toPage(List<Entitlement> entitlements, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= entitlements.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, entitlements.size());
        }
        int end = Math.min(start + pageable.getPageSize(), entitlements.size());
        return new PageImpl<>(entitlements.subList(start, end), pageable, entitlements.size());
    }

    private void validateRolesBelongToTenant(List<UUID> roleIds, String tenantId) {
        if (roleIds.isEmpty()) {
            return;
        }
        Map<UUID, AccessRole> rolesById = roleRepository.findByIdIn(roleIds).stream()
                .collect(Collectors.toMap(AccessRole::getId, role -> role));

        for (UUID roleId : roleIds) {
            AccessRole role = rolesById.get(roleId);
            if (role == null || !tenantId.equals(role.getTenantId())) {
                throw new EntityNotFoundException(String.format(AccessRoleConstants.ERROR_ROLE_NOT_FOUND, roleId));
            }
        }
    }

    private void refreshChangedRoles(String tenantId, List<UUID> roleIds) {
        for (UUID roleId : new LinkedHashSet<>(roleIds)) {
            closureService.refreshAfterEntitlementsChanged(tenantId, roleId);
        }
    }

    private EntitlementRoleResponse toEntitlementRoleResponse(UUID entitlementId,
                                                              AccessRoleEntitlement link,
                                                              AccessRole role) {
        return new EntitlementRoleResponse(
                link.getId(),
                entitlementId,
                role.getId(),
                role.getName(),
                role.getDisplayName(),
                role.getType(),
                role.getStatus(),
                role.getRiskLevel(),
                role.getBusinessUnit(),
                link.getCreatedAt() != null ? link.getCreatedAt().toEpochMilli() : null
        );
    }

    private EntitlementRoleAssignmentResponse entitlementRoleMutationResponse(UUID entitlementId,
                                                                              List<UUID> requestedRoleIds,
                                                                              List<UUID> uniqueRoleIds,
                                                                              List<UUID> addedRoleIds,
                                                                              List<UUID> removedRoleIds,
                                                                              List<UUID> unchangedRoleIds,
                                                                              List<UUID> skippedRoleIds,
                                                                              int skippedNotAssignedCount,
                                                                              int skippedAlreadyAssignedCount) {
        return new EntitlementRoleAssignmentResponse(
                entitlementId,
                requestedRoleIds != null ? requestedRoleIds.size() : 0,
                uniqueRoleIds.size(),
                addedRoleIds.size(),
                removedRoleIds.size(),
                unchangedRoleIds.size(),
                skippedAlreadyAssignedCount,
                skippedNotAssignedCount,
                addedRoleIds,
                removedRoleIds,
                unchangedRoleIds,
                skippedRoleIds,
                uniqueRoleIds
        );
    }

    /**
     * Loads all linked entitlement IDs for a role from the link table.
     * Returns only UUIDs to minimize memory footprint.
     *
     * @param roleId the role ID
     * @return list of entitlement UUIDs linked to this role
     */
    private List<UUID> loadLinkedEntitlementIds(UUID roleId) {
        return entitlementLinkRepository.findByRoleId(roleId)
                .stream()
                .map(AccessRoleEntitlement::getEntitlementId)
                .collect(Collectors.toList());
    }

    /**
     * Batch-loads assignment counts for a list of entitlement IDs in a single query.
     * Returns a map of entitlementId → count.
     *
     * @param entitlementIds list of entitlement UUIDs to count
     * @return map of entitlementId → assigned user count
     */
    private Map<UUID, Long> batchLoadAssignmentCounts(List<UUID> entitlementIds) {
        if (entitlementIds == null || entitlementIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<UUID, Long> countMap = new HashMap<>(entitlementIds.size());
        for (Object[] row : assignmentRepository.countByEntitlementIds(entitlementIds)) {
            countMap.put((UUID) row[0], (Long) row[1]);
        }
        return countMap;
    }

    /**
     * Maps an {@link Entitlement} entity to an {@link EntitlementResponseDto},
     * enriching it with the assigned user count and resolved application name.
     * <p>
     * Centralizes DTO construction to avoid duplicating the 26-arg constructor
     * across multiple methods. Any field changes to the record only need to be
     * updated here.
     * </p>
     *
     * @param e              the entitlement entity
     * @param assignedCount  pre-loaded assigned user count for this entitlement
     * @param appNameMap     pre-loaded map of resourceId → application name
     * @return fully populated response DTO
     */
    private EntitlementResponseDto toResponseDto(Entitlement e, long assignedCount, Map<UUID, String> appNameMap) {
        String appName = appNameMap.getOrDefault(e.getResourceId(), "Unknown Application");
        return new EntitlementResponseDto(
                e.getId(),
                e.getTenantId(),
                e.getResourceId(),
                e.getSourceUid(),
                appName,
                e.getDisplayName(),
                e.getDescription(),
                e.getObjectClass(),
                e.getKind(),
                e.getAttributes(),
                java.util.Collections.emptyMap(),
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
                assignedCount,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    /**
     * Validates that the role exists or throws {@link EntityNotFoundException}.
     *
     * @param roleId the role ID to validate
     * @throws EntityNotFoundException if the role does not exist
     */
    private void validateRoleExists(UUID roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    logger.warn()
                            .category(AccessRoleEntitlementConstants.LOG_CATEGORY)
                            .message(AccessRoleConstants.LOG_ROLE_NOT_FOUND)
                            .detail(AccessRoleEntitlementConstants.DETAIL_ROLE_ID, roleId.toString())
                            .log();
                    return new EntityNotFoundException(
                            String.format(AccessRoleConstants.ERROR_ROLE_NOT_FOUND, roleId));
                });
    }
}