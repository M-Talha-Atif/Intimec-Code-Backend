package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleCompositionConstants;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleConstants;
import com.aidentx.iam.access_roles_management.mapper.AccessRoleMapper;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AddChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRelationshipResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRelationshipResponse.ConflictEntitlement;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRelationshipResponse.RelationshipEntitlement;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRelationshipResponse.RoleRelationshipNode;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRelationshipResponse.RoleSummary;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRelationshipResponse.Summary;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleComposition;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleEntitlement;
import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipDirection;
import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipMode;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleCompositionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleCompositionService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementClosureService;
import com.aidentx.iam.access_roles_management.util.AccessRoleCompositionValidator;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AccessRoleCompositionService}.
 * <p>
 * Manages parent-child relationships between access roles.
 * Enforces circular dependency prevention using BFS-based cycle detection.
 * </p>
 */
@Service
@Transactional
public class AccessRoleCompositionServiceImpl implements AccessRoleCompositionService {

    private static final int DIRECT_DEPTH = 1;
    private static final int MAX_HIERARCHY_DEPTH = 10;

    private final AccessRoleCompositionRepository compositionRepository;
    private final AccessRoleEntitlementRepository entitlementLinkRepository;
    private final EntitlementRepository entitlementRepository;
    private final AccessRoleRepository roleRepository;
    private final AccessRoleMapper roleMapper;
    private final RoleEntitlementClosureService closureService;
    private final IUnifiedLogger logger;

    /**
     * Constructs the service with persistence, mapping, and logging dependencies.
     *
     * @param compositionRepository the role composition repository
     * @param roleRepository        the access role repository
     * @param roleMapper            the MapStruct mapper
     * @param closureService        closure refresh service for effective role entitlements
     * @param loggerFactory         the unified logger factory
     */
    public AccessRoleCompositionServiceImpl(
            AccessRoleCompositionRepository compositionRepository,
            AccessRoleEntitlementRepository entitlementLinkRepository,
            EntitlementRepository entitlementRepository,
            AccessRoleRepository roleRepository,
            AccessRoleMapper roleMapper,
            RoleEntitlementClosureService closureService,
            UnifiedLoggerFactory loggerFactory) {
        this.compositionRepository = compositionRepository;
        this.entitlementLinkRepository = entitlementLinkRepository;
        this.entitlementRepository = entitlementRepository;
        this.roleRepository = roleRepository;
        this.roleMapper = roleMapper;
        this.closureService = closureService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleCompositionConstants.LOG_CATEGORY);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Processes each child role individually:
     * <ul>
     *   <li>Self-references are rejected with a reason.</li>
     *   <li>Circular dependencies are rejected with a reason.</li>
     *   <li>Duplicate links are skipped silently.</li>
     *   <li>Valid children are persisted.</li>
     * </ul>
     * The operation never throws on individual child failures, allowing
     * partial success so the frontend can show per-child error messages.
     * </p>
     *
     * @throws EntityNotFoundException if the parent role does not exist
     */
    @Override
    public AddChildRolesResultDto addChildRoles(String tenantId, UUID parentRoleId, List<UUID> childRoleIds) {
        validateRoleExists(parentRoleId);

        List<UUID> added = new java.util.ArrayList<>();
        List<UUID> skipped = new java.util.ArrayList<>();
        List<AddChildRolesResultDto.FailedChild> failed = new java.util.ArrayList<>();

        for (UUID childRoleId : childRoleIds) {
            // Self-reference check
            if (parentRoleId.equals(childRoleId)) {
                logger.warn()
                        .category(AccessRoleCompositionConstants.LOG_CATEGORY)
                        .message(AccessRoleCompositionConstants.LOG_SELF_REFERENCE)
                        .detail(AccessRoleCompositionConstants.DETAIL_PARENT_ROLE_ID, parentRoleId.toString())
                        .detail(AccessRoleCompositionConstants.DETAIL_CHILD_ROLE_ID, childRoleId.toString())
                        .log();
                failed.add(new AddChildRolesResultDto.FailedChild(
                        childRoleId, AccessRoleConstants.ERROR_SELF_COMPOSITION));
                continue;
            }

            // Circular dependency check
            if (AccessRoleCompositionValidator.wouldCreateCycle(parentRoleId, childRoleId, compositionRepository)) {
                logger.warn()
                        .category(AccessRoleCompositionConstants.LOG_CATEGORY)
                        .message(AccessRoleCompositionConstants.LOG_CYCLE_DETECTED)
                        .detail(AccessRoleCompositionConstants.DETAIL_PARENT_ROLE_ID, parentRoleId.toString())
                        .detail(AccessRoleCompositionConstants.DETAIL_CHILD_ROLE_ID, childRoleId.toString())
                        .log();
                failed.add(new AddChildRolesResultDto.FailedChild(
                        childRoleId, AccessRoleConstants.ERROR_CIRCULAR_COMPOSITION));
                continue;
            }

            // Duplicate check
            if (compositionRepository.existsByParentRoleIdAndChildRoleId(parentRoleId, childRoleId)) {
                logger.debug()
                        .category(AccessRoleCompositionConstants.LOG_CATEGORY)
                        .message(AccessRoleCompositionConstants.LOG_CHILD_LINK_SKIPPED)
                        .detail(AccessRoleCompositionConstants.DETAIL_PARENT_ROLE_ID, parentRoleId.toString())
                        .detail(AccessRoleCompositionConstants.DETAIL_CHILD_ROLE_ID, childRoleId.toString())
                        .log();
                skipped.add(childRoleId);
                continue;
            }

            // Valid  persist the link
            AccessRoleComposition link = new AccessRoleComposition();
            link.setParentRoleId(parentRoleId);
            link.setChildRoleId(childRoleId);
            compositionRepository.save(link);
            closureService.refreshAfterCompositionChanged(tenantId, parentRoleId, childRoleId);
            added.add(childRoleId);
        }

        logger.info()
                .category(AccessRoleCompositionConstants.LOG_CATEGORY)
                .message(AccessRoleCompositionConstants.LOG_CHILDREN_ADDED)
                .detail(AccessRoleCompositionConstants.DETAIL_PARENT_ROLE_ID, parentRoleId.toString())
                .detail(AccessRoleCompositionConstants.DETAIL_ADDED_COUNT, String.valueOf(added.size()))
                .detail("skippedCount", String.valueOf(skipped.size()))
                .detail("failedCount", String.valueOf(failed.size()))
                .log();

        return new AddChildRolesResultDto(added, skipped, failed);
    }

    /**
     * {@inheritDoc}
     *
     * @throws EntityNotFoundException if the parent role does not exist
     */
    @Override
    public void removeChildRoles(String tenantId, UUID parentRoleId, List<UUID> childRoleIds) {
        validateRoleExists(parentRoleId);

        int removedCount = 0;
        for (UUID childRoleId : childRoleIds) {
            if (compositionRepository.existsByParentRoleIdAndChildRoleId(parentRoleId, childRoleId)) {
                compositionRepository.deleteByParentRoleIdAndChildRoleId(parentRoleId, childRoleId);
                closureService.refreshAfterCompositionChanged(tenantId, parentRoleId, childRoleId);
                removedCount++;
            }
        }

        logger.info()
                .category(AccessRoleCompositionConstants.LOG_CATEGORY)
                .message(AccessRoleCompositionConstants.LOG_CHILDREN_REMOVED)
                .detail(AccessRoleCompositionConstants.DETAIL_PARENT_ROLE_ID, parentRoleId.toString())
                .detail(AccessRoleCompositionConstants.DETAIL_REMOVED_COUNT, String.valueOf(removedCount))
                .log();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Enriches each child role response with its entitlement count
     * using a single batch query to avoid N+1 performance issues.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<AccessRoleResponse> getChildRoles(UUID parentRoleId) {
        List<UUID> childIds = compositionRepository.findByParentRoleId(parentRoleId)
                .stream()
                .map(AccessRoleComposition::getChildRoleId)
                .collect(Collectors.toList());

        List<AccessRoleResponse> children = roleRepository.findByIdIn(childIds)
                .stream()
                .map(roleMapper::entityToResponse)
                .collect(Collectors.toList());

        // Enrich with entitlement counts in a single batch query
        enrichWithEntitlementCounts(children, childIds);

        logger.info()
                .category(AccessRoleCompositionConstants.LOG_CATEGORY)
                .message(AccessRoleCompositionConstants.LOG_CHILDREN_FETCHED)
                .detail(AccessRoleCompositionConstants.DETAIL_PARENT_ROLE_ID, parentRoleId.toString())
                .detail(AccessRoleCompositionConstants.DETAIL_CHILDREN_COUNT, String.valueOf(children.size()))
                .log();

        return children;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Enriches each parent role response with its entitlement count
     * using a single batch query to avoid N+1 performance issues.
     * </p>
     */
    @Override
    @Transactional(readOnly = true)
    public List<AccessRoleResponse> getParentRoles(UUID childRoleId) {
        List<UUID> parentIds = compositionRepository.findByChildRoleId(childRoleId)
                .stream()
                .map(AccessRoleComposition::getParentRoleId)
                .collect(Collectors.toList());

        List<AccessRoleResponse> parents = roleRepository.findByIdIn(parentIds)
                .stream()
                .map(roleMapper::entityToResponse)
                .collect(Collectors.toList());

        // Enrich with entitlement counts in a single batch query
        enrichWithEntitlementCounts(parents, parentIds);

        logger.info()
                .category(AccessRoleCompositionConstants.LOG_CATEGORY)
                .message(AccessRoleCompositionConstants.LOG_PARENTS_FETCHED)
                .detail(AccessRoleCompositionConstants.DETAIL_CHILD_ROLE_ID, childRoleId.toString())
                .detail(AccessRoleCompositionConstants.DETAIL_PARENTS_COUNT, String.valueOf(parents.size()))
                .log();

        return parents;
    }

    /**
      * {@inheritDoc}
      */
     @Override
     @Transactional(readOnly = true)
     public List<UUID> getChildRoleIds(UUID parentRoleId) {
         return compositionRepository.findByParentRoleId(parentRoleId)
                 .stream()
                 .map(AccessRoleComposition::getChildRoleId)
                 .collect(Collectors.toList());
     }

    /**
     * Enriches a list of role responses with their entitlement counts.
     * <p>
     * Uses individual count queries per role. For large lists, consider
     * adding a batch count query to the repository.
     * </p>
     *
     * @param roles   the role responses to enrich
     * @param roleIds the corresponding role IDs (same order as entities were loaded)
     */
    private void enrichWithEntitlementCounts(List<AccessRoleResponse> roles, List<UUID> roleIds) {
        if (roles.isEmpty()) return;

        Map<UUID, Long> countMap = new java.util.HashMap<>();
        for (UUID roleId : roleIds) {
            countMap.put(roleId, entitlementLinkRepository.countByRoleId(roleId));
        }

        for (AccessRoleResponse role : roles) {
            Long count = countMap.getOrDefault(role.getId(), 0L);
            role.setEntitlementCount(count);
        }
    }


    /**
     * Validates that the role exists or throws {@link EntityNotFoundException}.
     *
     * @param roleId the role ID to validate
     */
    private void validateRoleExists(UUID roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    logger.warn()
                            .category(AccessRoleCompositionConstants.LOG_CATEGORY)
                            .message(AccessRoleConstants.LOG_ROLE_NOT_FOUND)
                            .detail(AccessRoleCompositionConstants.DETAIL_PARENT_ROLE_ID, roleId.toString())
                            .log();
                    return new EntityNotFoundException(
                            String.format(AccessRoleConstants.ERROR_ROLE_NOT_FOUND, roleId));
                });
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public AccessRoleRelationshipResponse getUnifiedRelationships(
            String tenantId,
            UUID roleId,
            RoleRelationshipMode mode,
            RoleRelationshipDirection direction) {
        RoleRelationshipMode effectiveMode = mode == null ? RoleRelationshipMode.DIRECT : mode;
        RoleRelationshipDirection effectiveDirection = direction == null ? RoleRelationshipDirection.BOTH : direction;
        int maxDepth = effectiveMode == RoleRelationshipMode.DIRECT ? DIRECT_DEPTH : MAX_HIERARCHY_DEPTH;

        AccessRole rootRole = validateTenantRole(tenantId, roleId);
        Map<UUID, AccessRole> rolesById = new LinkedHashMap<>();
        rolesById.put(rootRole.getId(), rootRole);

        TraversalResult children = effectiveDirection.includesChildren()
                ? traverseRelationships(tenantId, roleId, true, maxDepth, rolesById)
                : TraversalResult.empty();
        TraversalResult parents = effectiveDirection.includesParents()
                ? traverseRelationships(tenantId, roleId, false, maxDepth, rolesById)
                : TraversalResult.empty();

        EntitlementContext entitlementContext = loadEntitlementContext(tenantId, roleId, rolesById.keySet(), effectiveMode);
        RoleSummary rootSummary = toRoleSummary(rootRole, entitlementContext.entitlementIdsByRole().get(rootRole.getId()));
        List<UUID> rootPath = List.of(rootRole.getId());
        List<RoleRelationshipNode> childNodes = buildRelationshipNodes(
                children.edgesByRole(), true, roleId, 1, rootPath, rolesById, entitlementContext, effectiveMode);
        List<RoleRelationshipNode> parentNodes = buildRelationshipNodes(
                parents.edgesByRole(), false, roleId, 1, rootPath, rolesById, entitlementContext, effectiveMode);
        List<ConflictEntitlement> conflicts = buildConflictEntitlements(entitlementContext, rolesById);
        Set<UUID> returnedRoleIds = collectReturnedRoleIds(roleId, childNodes, parentNodes);

        Summary summary = new Summary(
                effectiveMode == RoleRelationshipMode.CONFLICT ? childNodes.size() : children.directCount(),
                effectiveMode == RoleRelationshipMode.CONFLICT ? parentNodes.size() : parents.directCount(),
                effectiveMode == RoleRelationshipMode.CONFLICT ? countRelationshipNodes(childNodes) : children.relatedRoleIds().size(),
                effectiveMode == RoleRelationshipMode.CONFLICT ? countRelationshipNodes(parentNodes) : parents.relatedRoleIds().size(),
                effectiveMode == RoleRelationshipMode.CONFLICT ? returnedRoleIds.size() : rolesById.size(),
                entitlementContext.visibleEntitlementIds().size(),
                conflicts.size());

        logger.info()
                .category(AccessRoleCompositionConstants.LOG_CATEGORY)
                .message(AccessRoleCompositionConstants.LOG_RELATIONSHIPS_FETCHED)
                .detail(AccessRoleCompositionConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleCompositionConstants.DETAIL_MODE, effectiveMode.name())
                .detail(AccessRoleCompositionConstants.DETAIL_DIRECTION, effectiveDirection.name())
                .detail(AccessRoleCompositionConstants.DETAIL_RELATED_ROLE_COUNT, String.valueOf(rolesById.size() - 1))
                .log();

        return new AccessRoleRelationshipResponse(
                roleId,
                tenantId,
                effectiveMode,
                effectiveDirection,
                maxDepth,
                rootSummary,
                relationshipEntitlements(roleId, entitlementContext, rolesById),
                childNodes,
                parentNodes,
                conflicts,
                summary);
    }

    private TraversalResult traverseRelationships(String tenantId,
                                                  UUID rootRoleId,
                                                  boolean childrenDirection,
                                                  int maxDepth,
                                                  Map<UUID, AccessRole> rolesById) {
        Map<UUID, List<UUID>> edgesByRole = new LinkedHashMap<>();
        Set<UUID> relatedRoleIds = new LinkedHashSet<>();
        Set<UUID> visitedForExpansion = new LinkedHashSet<>();
        Set<UUID> frontier = new LinkedHashSet<>();
        frontier.add(rootRoleId);
        visitedForExpansion.add(rootRoleId);
        int directCount = 0;

        for (int depth = 1; depth <= maxDepth && !frontier.isEmpty(); depth++) {
            List<AccessRoleComposition> links = childrenDirection
                    ? compositionRepository.findByParentRoleIdIn(frontier)
                    : compositionRepository.findByChildRoleIdIn(frontier);
            if (links.isEmpty()) {
                break;
            }

            Set<UUID> candidateRoleIds = links.stream()
                    .map(link -> childrenDirection ? link.getChildRoleId() : link.getParentRoleId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Map<UUID, AccessRole> candidateRoles = loadTenantRoles(tenantId, candidateRoleIds);
            rolesById.putAll(candidateRoles);

            Set<UUID> nextFrontier = new LinkedHashSet<>();
            for (AccessRoleComposition link : links) {
                UUID from = childrenDirection ? link.getParentRoleId() : link.getChildRoleId();
                UUID related = childrenDirection ? link.getChildRoleId() : link.getParentRoleId();
                if (!frontier.contains(from) || !candidateRoles.containsKey(related)) {
                    continue;
                }
                edgesByRole.computeIfAbsent(from, ignored -> new ArrayList<>()).add(related);
                relatedRoleIds.add(related);
                if (depth == 1) {
                    directCount++;
                }
                if (visitedForExpansion.add(related)) {
                    nextFrontier.add(related);
                }
            }
            frontier = nextFrontier;
        }
        return new TraversalResult(edgesByRole, relatedRoleIds, directCount);
    }

    private Map<UUID, AccessRole> loadTenantRoles(String tenantId, Collection<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return roleRepository.findByIdIn(new LinkedHashSet<>(roleIds)).stream()
                .filter(role -> tenantId.equals(role.getTenantId()))
                .collect(Collectors.toMap(
                        AccessRole::getId,
                        role -> role,
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private EntitlementContext loadEntitlementContext(String tenantId,
                                                      UUID rootRoleId,
                                                      Collection<UUID> roleIds,
                                                      RoleRelationshipMode mode) {
        if (roleIds == null || roleIds.isEmpty()) {
            return EntitlementContext.empty();
        }
        List<AccessRoleEntitlement> links = entitlementLinkRepository.findByRoleIdIn(roleIds);
        Map<UUID, List<UUID>> entitlementIdsByRole = new LinkedHashMap<>();
        Map<UUID, Set<UUID>> roleIdsByEntitlement = new LinkedHashMap<>();
        for (AccessRoleEntitlement link : links) {
            if (!roleIds.contains(link.getRoleId())) {
                continue;
            }
            entitlementIdsByRole.computeIfAbsent(link.getRoleId(), ignored -> new ArrayList<>()).add(link.getEntitlementId());
            roleIdsByEntitlement.computeIfAbsent(link.getEntitlementId(), ignored -> new LinkedHashSet<>()).add(link.getRoleId());
        }
        Set<UUID> rootEntitlementIds = new LinkedHashSet<>(
                entitlementIdsByRole.getOrDefault(rootRoleId, Collections.emptyList()));
        Set<UUID> conflictEntitlementIds = roleIdsByEntitlement.entrySet().stream()
                .filter(entry -> rootEntitlementIds.contains(entry.getKey()))
                .filter(entry -> entry.getValue().stream().anyMatch(conflictRoleId -> !rootRoleId.equals(conflictRoleId)))
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> visibleEntitlementIds = mode == RoleRelationshipMode.CONFLICT
                ? conflictEntitlementIds
                : roleIdsByEntitlement.keySet();
        Map<UUID, Entitlement> entitlementsById = entitlementRepository.findAllById(visibleEntitlementIds).stream()
                .filter(entitlement -> tenantId.equals(entitlement.getTenantId()))
                .collect(Collectors.toMap(
                        Entitlement::getId,
                        entitlement -> entitlement,
                        (left, right) -> left,
                        LinkedHashMap::new));
        return new EntitlementContext(
                entitlementIdsByRole,
                roleIdsByEntitlement,
                conflictEntitlementIds,
                new LinkedHashSet<>(entitlementsById.keySet()),
                entitlementsById);
    }

    private List<RoleRelationshipNode> buildRelationshipNodes(Map<UUID, List<UUID>> edgesByRole,
                                                              boolean childrenDirection,
                                                              UUID currentRoleId,
                                                              int depth,
                                                              List<UUID> pathRoleIds,
                                                              Map<UUID, AccessRole> rolesById,
                                                               EntitlementContext entitlementContext,
                                                               RoleRelationshipMode mode) {
        List<UUID> relatedRoleIds = edgesByRole.getOrDefault(currentRoleId, Collections.emptyList());
        List<RoleRelationshipNode> nodes = new ArrayList<>();
        for (UUID relatedRoleId : relatedRoleIds) {
            if (pathRoleIds.contains(relatedRoleId)) {
                continue;
            }
            AccessRole relatedRole = rolesById.get(relatedRoleId);
            if (relatedRole == null) {
                continue;
            }
            List<UUID> childPath = new ArrayList<>(pathRoleIds);
            childPath.add(relatedRoleId);
            List<RoleRelationshipNode> childRoles = childrenDirection
                    ? buildRelationshipNodes(edgesByRole, true, relatedRoleId, depth + 1, childPath, rolesById, entitlementContext, mode)
                    : Collections.emptyList();
            List<RoleRelationshipNode> parentRoles = childrenDirection
                    ? Collections.emptyList()
                    : buildRelationshipNodes(edgesByRole, false, relatedRoleId, depth + 1, childPath, rolesById, entitlementContext, mode);
            List<RelationshipEntitlement> entitlements = relationshipEntitlements(relatedRoleId, entitlementContext, rolesById);
            if (mode == RoleRelationshipMode.CONFLICT && entitlements.isEmpty() && childRoles.isEmpty() && parentRoles.isEmpty()) {
                continue;
            }
            nodes.add(new RoleRelationshipNode(
                    toRoleSummary(relatedRole, entitlementContext.entitlementIdsByRole().get(relatedRoleId)),
                    depth,
                    List.copyOf(childPath),
                    entitlements,
                    childRoles,
                    parentRoles));
        }
        return nodes;
    }

    private Set<UUID> collectReturnedRoleIds(UUID rootRoleId,
                                             List<RoleRelationshipNode> childNodes,
                                             List<RoleRelationshipNode> parentNodes) {
        Set<UUID> roleIds = new LinkedHashSet<>();
        roleIds.add(rootRoleId);
        collectNodeRoleIds(childNodes, roleIds);
        collectNodeRoleIds(parentNodes, roleIds);
        return roleIds;
    }

    private void collectNodeRoleIds(List<RoleRelationshipNode> nodes, Set<UUID> roleIds) {
        for (RoleRelationshipNode node : nodes) {
            roleIds.add(node.role().id());
            collectNodeRoleIds(node.childRoles(), roleIds);
            collectNodeRoleIds(node.parentRoles(), roleIds);
        }
    }

    private int countRelationshipNodes(List<RoleRelationshipNode> nodes) {
        int count = 0;
        for (RoleRelationshipNode node : nodes) {
            count++;
            count += countRelationshipNodes(node.childRoles());
            count += countRelationshipNodes(node.parentRoles());
        }
        return count;
    }

    private List<RelationshipEntitlement> relationshipEntitlements(UUID roleId,
                                                                    EntitlementContext entitlementContext,
                                                                    Map<UUID, AccessRole> rolesById) {
        return entitlementContext.entitlementIdsByRole().getOrDefault(roleId, Collections.emptyList()).stream()
                .filter(entitlementContext.visibleEntitlementIds()::contains)
                .map(entitlementId -> toRelationshipEntitlement(entitlementId, entitlementContext, rolesById))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private RelationshipEntitlement toRelationshipEntitlement(UUID entitlementId,
                                                              EntitlementContext entitlementContext,
                                                              Map<UUID, AccessRole> rolesById) {
        Entitlement entitlement = entitlementContext.entitlementsById().get(entitlementId);
        if (entitlement == null) {
            return null;
        }
        boolean conflict = entitlementContext.conflictEntitlementIds().contains(entitlementId);
        List<UUID> conflictRoleIds = List.copyOf(entitlementContext.roleIdsByEntitlement()
                .getOrDefault(entitlementId, Collections.emptySet()));
        return new RelationshipEntitlement(
                entitlement.getId(),
                entitlement.getDisplayName(),
                entitlement.getDescription(),
                entitlement.getResourceId(),
                entitlement.getSourceUid(),
                entitlement.getObjectClass(),
                entitlement.getKind(),
                entitlement.getLifecycle(),
                entitlement.getRiskLevel(),
                entitlement.getPrivilegeLevel(),
                conflict,
                conflict ? buildConflictReason(entitlement, conflictRoleIds, rolesById) : null,
                conflictRoleIds);
    }

    private List<ConflictEntitlement> buildConflictEntitlements(EntitlementContext entitlementContext,
                                                                Map<UUID, AccessRole> rolesById) {
        return entitlementContext.conflictEntitlementIds().stream()
                .filter(entitlementContext.visibleEntitlementIds()::contains)
                .map(entitlementId -> {
                    RelationshipEntitlement entitlement = toRelationshipEntitlement(entitlementId, entitlementContext, rolesById);
                    if (entitlement == null) {
                        return null;
                    }
                    List<RoleSummary> roles = entitlementContext.roleIdsByEntitlement()
                            .getOrDefault(entitlementId, Collections.emptySet())
                            .stream()
                            .map(rolesById::get)
                            .filter(Objects::nonNull)
                            .map(role -> toRoleSummary(role, entitlementContext.entitlementIdsByRole().get(role.getId())))
                            .collect(Collectors.toList());
                    return new ConflictEntitlement(
                            entitlement,
                            roles,
                            roles.size(),
                            entitlement.conflictReason());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String buildConflictReason(Entitlement entitlement, List<UUID> conflictRoleIds, Map<UUID, AccessRole> rolesById) {
        String roleNames = conflictRoleIds.stream()
                .map(rolesById::get)
                .filter(Objects::nonNull)
                .map(this::roleDisplayName)
                .filter(name -> name != null && !name.isBlank())
                .collect(Collectors.joining(", "));
        if (roleNames.isBlank()) {
            return AccessRoleCompositionConstants.CONFLICT_REASON_ENTITLEMENT_OVERLAP;
        }
        return String.format(
                AccessRoleCompositionConstants.CONFLICT_REASON_ENTITLEMENT_OVERLAP_WITH_ROLES,
                entitlementDisplayName(entitlement),
                roleNames);
    }

    private String entitlementDisplayName(Entitlement entitlement) {
        if (entitlement.getDisplayName() != null && !entitlement.getDisplayName().isBlank()) {
            return entitlement.getDisplayName();
        }
        if (entitlement.getSourceUid() != null && !entitlement.getSourceUid().isBlank()) {
            return entitlement.getSourceUid();
        }
        return entitlement.getId().toString();
    }

    private String roleDisplayName(AccessRole role) {
        if (role.getDisplayName() != null && !role.getDisplayName().isBlank()) {
            return role.getDisplayName();
        }
        if (role.getName() != null && !role.getName().isBlank()) {
            return role.getName();
        }
        return role.getId().toString();
    }

    private RoleSummary toRoleSummary(AccessRole role, List<UUID> entitlementIds) {
        int entitlementCount = entitlementIds == null ? 0 : new HashSet<>(entitlementIds).size();
        return new RoleSummary(
                role.getId(),
                role.getName(),
                role.getDisplayName(),
                role.getType(),
                role.getStatus(),
                role.getRiskLevel(),
                entitlementCount);
    }

    private AccessRole validateTenantRole(String tenantId, UUID roleId) {
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

    private record TraversalResult(
            Map<UUID, List<UUID>> edgesByRole,
            Set<UUID> relatedRoleIds,
            int directCount
    ) {
        private static TraversalResult empty() {
            return new TraversalResult(Collections.emptyMap(), Collections.emptySet(), 0);
        }
    }

    private record EntitlementContext(
            Map<UUID, List<UUID>> entitlementIdsByRole,
            Map<UUID, Set<UUID>> roleIdsByEntitlement,
            Set<UUID> conflictEntitlementIds,
            Set<UUID> visibleEntitlementIds,
            Map<UUID, Entitlement> entitlementsById
    ) {
        private static EntitlementContext empty() {
            return new EntitlementContext(
                    Collections.emptyMap(),
                    Collections.emptyMap(),
                    Collections.emptySet(),
                    Collections.emptySet(),
                    Collections.emptyMap());
        }
    }

}