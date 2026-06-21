package com.aidentx.iam.access_roles_management.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.aidentx.auth.grpc.GetUsersByIdentityIdsRequest;
import com.aidentx.auth.grpc.GetUsersByIdentityIdsResponse;
import com.aidentx.auth.grpc.UserGrpcServiceGrpc;
import com.aidentx.auth.grpc.UserInfo;
import com.aidentx.iam.access_roles_management.audit.AccessRoleAuditAction;
import com.aidentx.iam.access_roles_management.audit.AuditAccessRoleChange;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleConstants;
import com.aidentx.iam.access_roles_management.mapper.AccessRoleMapper;
import com.aidentx.iam.access_roles_management.model.dto.request.AccessRoleSaveSimulationRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.CreateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.request.UpdateAccessRoleRequest;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDeleteImpactResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDetailResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDirectoryBusinessUnitSummaryDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDirectoryGroupSummaryDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleDirectorySummaryResponseDto;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleGroupedMemberResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMemberEntitlementResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMemberResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleMembersSummaryResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleSaveSimulationResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AddChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleComposition;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.model.entity.RoleEntitlementClosure;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentExecutionItemRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentExecutionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAuditLogRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleCompositionRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleEntitlementRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.repository.RoleEntitlementClosureRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleCompositionService;
import com.aidentx.iam.access_roles_management.service.AccessRoleEntitlementService;
import com.aidentx.iam.access_roles_management.service.AccessRoleService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementAssignmentService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementClosureService;
import com.aidentx.iam.entitlement_management.client.ApplicationClient;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.organization_management.model.entity.OrgUnit;
import com.aidentx.iam.organization_management.repository.OrgUnitRepository;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.grpc.StatusRuntimeException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link AccessRoleService}.
 *
 * <p>Handles the core lifecycle operations for access roles including creation,
 * retrieval, update, deletion, and lightweight directory summary/list retrieval.</p>
 */
@Service
@Transactional
public class AccessRoleServiceImpl implements AccessRoleService {

    private final AccessRoleRepository roleRepository;
    private final AccessRoleEntitlementRepository entitlementLinkRepository;
    private final AccessRoleCompositionRepository compositionRepository;
    private final AccessRoleEntitlementService entitlementService;
    private final AccessRoleCompositionService compositionService;
    private final RoleEntitlementAssignmentService roleEntitlementAssignmentService;
    private final RoleEntitlementClosureService roleEntitlementClosureService;
    private final IdentityRoleAssignmentRepository identityRoleAssignmentRepository;
    private final RoleEntitlementClosureRepository roleEntitlementClosureRepository;
    private final AccessRoleAssignmentRuleRepository assignmentRuleRepository;
    private final AccessRoleAssignmentExecutionRepository assignmentExecutionRepository;
    private final AccessRoleAssignmentExecutionItemRepository assignmentExecutionItemRepository;
    private final AccessRoleAuditLogRepository auditLogRepository;
    private final EntitlementRepository entitlementRepository;
    private final IdentityRepository identityRepository;
    private final OrgUnitRepository orgUnitRepository;
    private final ApplicationClient applicationClient;
    private final AccessRoleMapper roleMapper;
    private final IUnifiedLogger logger;
    private final ShadowRepository shadowRepository;
    private final EntitlementAssignmentRepository entitlementAssignmentRepository;
    private final UserGrpcServiceGrpc.UserGrpcServiceBlockingStub authUserClient;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param roleRepository            the access role JPA repository
     * @param entitlementLinkRepository the role-entitlement link repository
     * @param compositionRepository     the role composition repository
     * @param entitlementService        the entitlement link service
     * @param compositionService        the composition link service
     * @param roleMapper                the MapStruct mapper for role entity/DTO conversions
     * @param loggerFactory             the unified logger factory
     */
    public AccessRoleServiceImpl(
            AccessRoleRepository roleRepository,
            AccessRoleEntitlementRepository entitlementLinkRepository,
            AccessRoleCompositionRepository compositionRepository,
            AccessRoleEntitlementService entitlementService,
            AccessRoleCompositionService compositionService,
            RoleEntitlementAssignmentService roleEntitlementAssignmentService,
            RoleEntitlementClosureService roleEntitlementClosureService,
            IdentityRoleAssignmentRepository identityRoleAssignmentRepository,
            RoleEntitlementClosureRepository roleEntitlementClosureRepository,
            AccessRoleAssignmentRuleRepository assignmentRuleRepository,
            AccessRoleAssignmentExecutionRepository assignmentExecutionRepository,
            AccessRoleAssignmentExecutionItemRepository assignmentExecutionItemRepository,
            AccessRoleAuditLogRepository auditLogRepository,
            EntitlementRepository entitlementRepository,
            IdentityRepository identityRepository,
            OrgUnitRepository orgUnitRepository,
            ApplicationClient applicationClient,
            AccessRoleMapper roleMapper,
            UnifiedLoggerFactory loggerFactory,
            ShadowRepository shadowRepository,
            EntitlementAssignmentRepository entitlementAssignmentRepository,
            UserGrpcServiceGrpc.UserGrpcServiceBlockingStub authUserClient) {
        this.roleRepository = roleRepository;
        this.entitlementLinkRepository = entitlementLinkRepository;
        this.compositionRepository = compositionRepository;
        this.entitlementService = entitlementService;
        this.compositionService = compositionService;
        this.roleEntitlementAssignmentService = roleEntitlementAssignmentService;
        this.roleEntitlementClosureService = roleEntitlementClosureService;
        this.identityRoleAssignmentRepository = identityRoleAssignmentRepository;
        this.roleEntitlementClosureRepository = roleEntitlementClosureRepository;
        this.assignmentRuleRepository = assignmentRuleRepository;
        this.assignmentExecutionRepository = assignmentExecutionRepository;
        this.assignmentExecutionItemRepository = assignmentExecutionItemRepository;
        this.auditLogRepository = auditLogRepository;
        this.entitlementRepository = entitlementRepository;
        this.identityRepository = identityRepository;
        this.orgUnitRepository = orgUnitRepository;
        this.applicationClient = applicationClient;
        this.roleMapper = roleMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleConstants.LOG_CATEGORY);
        this.shadowRepository = shadowRepository;
        this.entitlementAssignmentRepository = entitlementAssignmentRepository;
        this.authUserClient = authUserClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @AuditAccessRoleChange(action = AccessRoleAuditAction.ROLE_CREATED, roleIdParamIndex = -1)
    public AccessRoleResponse createRole(String tenantId, CreateAccessRoleRequest request) {
        applyCreateDefaults(request);
        validateOrganizationIfPresent(tenantId, request.getOrganization());
        roleRepository.findByTenantIdAndName(tenantId, request.getName()).ifPresent(existing -> {
            logger.warn()
                    .category(AccessRoleConstants.LOG_CATEGORY)
                    .message(AccessRoleConstants.LOG_ROLE_NAME_DUPLICATE)
                    .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleConstants.DETAIL_ROLE_NAME, request.getName())
                    .log();
            throw new IllegalArgumentException(
                    String.format(AccessRoleConstants.ERROR_ROLE_NAME_EXISTS, request.getName()));
        });

        AccessRole entity = roleMapper.createRequestToEntity(request);
        entity.setTenantId(tenantId);
        AccessRole saved = roleRepository.save(entity);

        if (request.getEntitlementIds() != null && !request.getEntitlementIds().isEmpty()) {
            entitlementService.addEntitlements(tenantId, saved.getId(), request.getEntitlementIds());
        }

        if (request.getChildRoleIds() != null && !request.getChildRoleIds().isEmpty()) {
            AddChildRolesResultDto childResult = compositionService.addChildRoles(
                    tenantId, saved.getId(), request.getChildRoleIds());

            if (childResult.hasFailures()) {
                logger.warn()
                        .category(AccessRoleConstants.LOG_CATEGORY)
                        .message("Some child roles could not be added during role creation")
                        .detail(AccessRoleConstants.DETAIL_ROLE_ID, saved.getId().toString())
                        .detail("failedCount", String.valueOf(childResult.getFailed().size()))
                        .log();
            }
        }

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_ROLE_CREATED)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, saved.getId().toString())
                .detail(AccessRoleConstants.DETAIL_ROLE_NAME, saved.getName())
                .detail(AccessRoleConstants.DETAIL_ROLE_TYPE, saved.getType())
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        AccessRoleResponse response = normalizedResponse(saved);
        response.setEntitlementCount(entitlementLinkRepository.countByRoleId(saved.getId()));
        enrichOrganizationNames(tenantId, List.of(response));
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleMemberResponse> getRoleMembers(String tenantId,
                                                         UUID roleId,
                                                         String search,
                                                         List<String> identityTypes,
                                                         List<String> identityRiskLevels,
                                                         List<String> accessPathTypes,
                                                         List<String> assignmentSourceTypes,
                                                         List<UUID> applicationIds,
                                                         List<UUID> entitlementIds,
                                                         Pageable pageable) {
        List<AccessRoleMemberResponse> rows = getFilteredRoleMemberRows(
                tenantId,
                roleId,
                search,
                identityTypes,
                identityRiskLevels,
                accessPathTypes,
                assignmentSourceTypes,
                applicationIds,
                entitlementIds);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message("Access role members fetched")
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleConstants.DETAIL_COUNT, String.valueOf(rows.size()))
                .log();

        return toPage(rows, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleGroupedMemberResponse> getRoleMembersGrouped(String tenantId,
                                                                       UUID roleId,
                                                                       String search,
                                                                       Pageable pageable) {
        List<AccessRoleGroupedMemberResponse> groupedMembers = getRoleMemberSummaries(tenantId, roleId, search);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message("Grouped access role members fetched")
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleConstants.DETAIL_COUNT, String.valueOf(groupedMembers.size()))
                .log();

        return toGroupedPage(groupedMembers, pageable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccessRoleMembersSummaryResponse getRoleMembersSummary(String tenantId, UUID roleId, Integer activityWindowDays) {
        findTenantRoleOrThrow(tenantId, roleId);

        int effectiveActivityWindowDays = normalizeActivityWindowDays(activityWindowDays);
        java.time.Instant activityCutoffAt = java.time.Instant.now()
                .minus(java.time.Duration.ofDays(effectiveActivityWindowDays));

        long totalMembers = identityRoleAssignmentRepository.countDistinctIdentityIdByTenantIdAndRoleIdAndStatus(
                tenantId,
                roleId,
                IdentityRoleAssignment.Status.ACTIVE);
        List<IdentityRoleAssignment> assignments = identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                tenantId,
                roleId,
                IdentityRoleAssignment.Status.ACTIVE);
        String assignmentMode = resolveAssignmentMode(assignments);
        Set<UUID> assignedIdentityIds = assignments.stream()
                .map(IdentityRoleAssignment::getIdentityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, java.time.Instant> lastLoginByIdentityId = loadAuthLastLoginByIdentityId(tenantId, assignedIdentityIds);
        long activeMembers = assignedIdentityIds.stream()
                .filter(identityId -> isActiveWithinWindow(lastLoginByIdentityId.get(identityId), activityCutoffAt))
                .count();
        long inactiveMembers = Math.max(0, totalMembers - activeMembers);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message("Access role member summary fetched")
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleConstants.DETAIL_COUNT, String.valueOf(totalMembers))
                .detail("activityWindowDays", String.valueOf(effectiveActivityWindowDays))
                .detail("activeMembers", String.valueOf(activeMembers))
                .detail("inactiveMembers", String.valueOf(inactiveMembers))
                .detail("assignmentMode", assignmentMode)
                .log();

        return new AccessRoleMembersSummaryResponse(
                roleId,
                tenantId,
                effectiveActivityWindowDays,
                activityCutoffAt,
                totalMembers,
                activeMembers,
                inactiveMembers,
                assignmentMode);
    }

    private int normalizeActivityWindowDays(Integer activityWindowDays) {
        if (activityWindowDays == null) {
            return AccessRoleConstants.DEFAULT_ACTIVITY_WINDOW_DAYS;
        }
        if (activityWindowDays <= 0) {
            throw new IllegalArgumentException("activityWindowDays must be greater than 0");
        }
        return activityWindowDays;
    }

    private Map<UUID, java.time.Instant> loadAuthLastLoginByIdentityId(String tenantId, Set<UUID> identityIds) {
        if (identityIds == null || identityIds.isEmpty() || authUserClient == null) {
            return Map.of();
        }

        try {
            GetUsersByIdentityIdsRequest request = GetUsersByIdentityIdsRequest.newBuilder()
                    .setTenantId(tenantId)
                    .addAllIdentityIds(identityIds.stream().map(UUID::toString).toList())
                    .build();
            GetUsersByIdentityIdsResponse response = authUserClient.getUsersByIdentityIds(request);
            if (response == null || response.getUsersCount() == 0) {
                return Map.of();
            }

            Map<UUID, java.time.Instant> lastLoginByIdentityId = new HashMap<>();
            for (UserInfo user : response.getUsersList()) {
                String identityIdValue = user.getIdentityId();
                long lastLoginAt = user.getLastLoginAt();
                if (!identityIdValue.isBlank() && lastLoginAt > 0) {
                    UUID identityId = parseUuidOrNull(identityIdValue);
                    if (identityId != null) {
                        lastLoginByIdentityId.put(identityId, java.time.Instant.ofEpochMilli(lastLoginAt));
                    }
                }
            }
            return lastLoginByIdentityId;
        } catch (StatusRuntimeException e) {
            logger.warn()
                    .category(AccessRoleConstants.LOG_CATEGORY)
                    .message("Auth user lookup failed while computing role member activity")
                    .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail("status", e.getStatus().toString())
                    .log();
            return Map.of();
        }
    }

    private UUID parseUuidOrNull(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            logger.warn()
                    .category(AccessRoleConstants.LOG_CATEGORY)
                    .message("Auth user lookup returned an invalid identity ID")
                    .detail("identityId", value)
                    .log();
            return null;
        }
    }

    private boolean isActiveWithinWindow(java.time.Instant lastLoginAt, java.time.Instant activityCutoffAt) {
        return lastLoginAt != null && !lastLoginAt.isBefore(activityCutoffAt);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleMemberEntitlementResponse> getRoleMemberEntitlements(String tenantId,
                                                                               UUID roleId,
                                                                               UUID assignmentId,
                                                                               String search,
                                                                               List<String> accessPathTypes,
                                                                               List<UUID> applicationIds,
                                                                               List<UUID> entitlementIds,
                                                                               Pageable pageable) {
        List<AccessRoleMemberEntitlementResponse> entitlements = getRoleMemberEntitlementRows(
                tenantId,
                roleId,
                assignmentId,
                search,
                accessPathTypes,
                applicationIds,
                entitlementIds);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message("Access role member entitlement drilldown fetched")
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleConstants.DETAIL_ASSIGNMENT_ID, assignmentId.toString())
                .detail(AccessRoleConstants.DETAIL_COUNT, String.valueOf(entitlements.size()))
                .log();

        return toMemberEntitlementPage(entitlements, pageable);
    }

    private List<AccessRoleGroupedMemberResponse> getRoleMemberSummaries(String tenantId,
                                                                         UUID roleId,
                                                                         String search) {
        AccessRole role = findTenantRoleOrThrow(tenantId, roleId);

        List<IdentityRoleAssignment> assignments = identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                tenantId,
                roleId,
                IdentityRoleAssignment.Status.ACTIVE);
        List<RoleEntitlementClosure> closures = roleEntitlementClosureRepository.findByTenantIdAndRoleId(tenantId, roleId);
        Map<UUID, Identity> identityById = loadIdentities(tenantId, assignments);
        Map<UUID, AccessRole> roleById = loadRoles(tenantId, role, closures);

        int entitlementCount = (int) closures.stream()
                .map(RoleEntitlementClosure::getEntitlementId)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        List<String> childRoleNames = closures.stream()
                .filter(closure -> !closure.isDirect())
                .map(RoleEntitlementClosure::getSourceRoleId)
                .filter(Objects::nonNull)
                .distinct()
                .map(roleById::get)
                .map(this::resolveRoleName)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        String assignmentPathSummary = buildAssignmentPathSummary(closures, childRoleNames.size());

        // Compute provisioned entitlement count for pending calculation
        int provisionedCount = 0;
        if (entitlementAssignmentRepository != null) {
            provisionedCount = entitlementAssignmentRepository
                    .findDistinctEntitlementIdsByTenantIdAndAccessRoleId(tenantId, roleId).size();
        }
        int pendingEntitlementCount = Math.max(0, entitlementCount - provisionedCount);

        return assignments.stream()
                .map(assignment -> new AccessRoleGroupedMemberResponse(
                        assignment.getId(),
                        assignment.getIdentityId(),
                        resolveIdentityName(identityById.get(assignment.getIdentityId()), assignment.getIdentityId()),
                        identityById.get(assignment.getIdentityId()) != null
                                && identityById.get(assignment.getIdentityId()).getIdentityType() != null
                                ? identityById.get(assignment.getIdentityId()).getIdentityType().name()
                                : null,
                        resolveIdentityRiskLevel(identityById.get(assignment.getIdentityId())),
                        role.getId(),
                        resolveRoleName(role),
                        assignment.getAssignedAt(),
                        resolveDisplayAssignmentSourceType(assignment),
                        entitlementCount,
                        pendingEntitlementCount,
                        childRoleNames,
                        assignmentPathSummary))
                .filter(member -> matchesGroupedMemberSearch(member, search))
                .sorted(Comparator
                        .comparing(AccessRoleGroupedMemberResponse::roleAssignedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(AccessRoleGroupedMemberResponse::identityName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private String resolveAssignmentMode(List<IdentityRoleAssignment> assignments) {
        boolean hasDynamic = assignments.stream()
                .map(IdentityRoleAssignment::getSourceType)
                .anyMatch(IdentityRoleAssignment.SourceType.RULE::equals);

        List<String> modes = new ArrayList<>();
        if (hasDynamic) {
            modes.add("DYNAMIC");
        }
        modes.add("MANUAL");
        return String.join(" + ", modes);
    }

    private List<AccessRoleMemberEntitlementResponse> getRoleMemberEntitlementRows(String tenantId,
                                                                                   UUID roleId,
                                                                                   UUID assignmentId,
                                                                                   String search,
                                                                                   List<String> accessPathTypes,
                                                                                   List<UUID> applicationIds,
                                                                                   List<UUID> entitlementIds) {
        AccessRole role = findTenantRoleOrThrow(tenantId, roleId);
        IdentityRoleAssignment assignment = identityRoleAssignmentRepository
                .findByIdAndTenantIdAndRoleIdAndStatus(
                        assignmentId,
                        tenantId,
                        roleId,
                        IdentityRoleAssignment.Status.ACTIVE)
                .orElseThrow(() -> new EntityNotFoundException("Active role member assignment not found with id: " + assignmentId));

        List<RoleEntitlementClosure> closures = roleEntitlementClosureRepository.findByTenantIdAndRoleId(tenantId, roleId);
        if (closures.isEmpty()) {
            return List.of();
        }

        Map<UUID, Identity> identityById = loadIdentities(tenantId, List.of(assignment));
        Map<UUID, Entitlement> entitlementById = loadEntitlements(tenantId, closures);
        Map<UUID, String> applicationNames = loadApplicationNames(tenantId, entitlementById.values().stream().toList());
        Map<UUID, AccessRole> roleById = loadRoles(tenantId, role, closures);
        Identity identity = identityById.get(assignment.getIdentityId());

        // ----- Assignment status lookup: identity's account coverage + provisioned entitlements -----
        // Build set of resourceIds (as strings) where identity has an account (shadow)
        Set<String> identityResourceIds = Set.of();
        if (shadowRepository != null) {
            List<Object[]> shadowRows = shadowRepository.findResourceIdsByIdentityIds(
                    List.of(assignment.getIdentityId()), tenantId);
            identityResourceIds = shadowRows.stream()
                    .map(row -> (String) row[1])
                    .collect(Collectors.toSet());
        }

        // Build set of entitlement IDs that are actually provisioned for this role
        Set<UUID> provisionedEntitlementIds = Set.of();
        if (entitlementAssignmentRepository != null) {
            provisionedEntitlementIds = new LinkedHashSet<>(
                    entitlementAssignmentRepository.findDistinctEntitlementIdsByTenantIdAndAccessRoleId(tenantId, roleId));
        }

        final Set<String> finalIdentityResourceIds = identityResourceIds;
        final Set<UUID> finalProvisionedEntitlementIds = provisionedEntitlementIds;

        List<AccessRoleMemberResponse> rows = closures.stream()
                .map(closure -> toMemberResponse(
                        assignment,
                        role,
                        closure,
                        identity,
                        entitlementById.get(closure.getEntitlementId()),
                        applicationNames,
                        roleById))
                .filter(row -> row.entitlementId() != null)
                .toList();

        rows = filterMemberEntitlementRows(rows, search, accessPathTypes, applicationIds, entitlementIds);

        return rows.stream()
                .map(row -> {
                    String status;
                    String reason;
                    if (finalProvisionedEntitlementIds.contains(row.entitlementId())) {
                        status = "ASSIGNED";
                        reason = null;
                    } else if (row.applicationId() != null
                            && !finalIdentityResourceIds.contains(row.applicationId().toString())) {
                        status = "PENDING_NO_ACCOUNT";
                        reason = "Identity has no account on the target application";
                    } else {
                        status = "NOT_ASSIGNED";
                        reason = "Entitlement not yet provisioned to identity's account";
                    }
                    return toMemberEntitlementResponse(row, status, reason);
                })
                .sorted(Comparator
                        .comparing(AccessRoleMemberEntitlementResponse::applicationName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(AccessRoleMemberEntitlementResponse::entitlementName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    private List<AccessRoleMemberResponse> getFilteredRoleMemberRows(String tenantId,
                                                                     UUID roleId,
                                                                     String search,
                                                                     List<String> identityTypes,
                                                                     List<String> identityRiskLevels,
                                                                     List<String> accessPathTypes,
                                                                     List<String> assignmentSourceTypes,
                                                                     List<UUID> applicationIds,
                                                                     List<UUID> entitlementIds) {
        AccessRole role = findTenantRoleOrThrow(tenantId, roleId);

        List<IdentityRoleAssignment> assignments = identityRoleAssignmentRepository.findByTenantIdAndRoleIdAndStatus(
                tenantId,
                roleId,
                IdentityRoleAssignment.Status.ACTIVE);

        List<RoleEntitlementClosure> closures = roleEntitlementClosureRepository.findByTenantIdAndRoleId(tenantId, roleId);
        Map<UUID, Identity> identityById = loadIdentities(tenantId, assignments);
        Map<UUID, Entitlement> entitlementById = loadEntitlements(tenantId, closures);
        Map<UUID, String> applicationNames = loadApplicationNames(tenantId, entitlementById.values().stream().toList());
        Map<UUID, AccessRole> roleById = loadRoles(tenantId, role, closures);

        List<AccessRoleMemberResponse> rows = new ArrayList<>();
        if (closures.isEmpty()) {
            for (IdentityRoleAssignment assignment : assignments) {
                rows.add(toMemberResponse(
                        assignment,
                        role,
                        null,
                        identityById.get(assignment.getIdentityId()),
                        null,
                        Map.of(),
                        Map.of()));
            }
        } else {
            for (IdentityRoleAssignment assignment : assignments) {
                Identity identity = identityById.get(assignment.getIdentityId());
                for (RoleEntitlementClosure closure : closures) {
                    Entitlement entitlement = entitlementById.get(closure.getEntitlementId());
                    rows.add(toMemberResponse(
                            assignment,
                            role,
                            closure,
                            identity,
                            entitlement,
                            applicationNames,
                            roleById));
                }
            }
        }

        rows = filterMemberRows(
                rows,
                search,
                identityTypes,
                identityRiskLevels,
                accessPathTypes,
                assignmentSourceTypes,
                applicationIds,
                entitlementIds);

        rows.sort(Comparator
                .comparing(AccessRoleMemberResponse::roleAssignedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(AccessRoleMemberResponse::identityName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(AccessRoleMemberResponse::entitlementName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        return rows;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccessRoleDetailResponse getRoleDetail(String tenantId, UUID roleId) {
        AccessRole entity = findRoleOrThrow(roleId);

        AccessRoleDetailResponse response = new AccessRoleDetailResponse();
        AccessRoleResponse base = normalizedResponse(entity);
        response.setId(base.getId());
        response.setTenantId(base.getTenantId());
        response.setName(base.getName());
        response.setDisplayName(base.getDisplayName());
        response.setDescription(base.getDescription());
        response.setType(base.getType());
        response.setStatus(base.getStatus());
        response.setBusinessUnit(base.getBusinessUnit());
        response.setOrganization(base.getOrganization());
        response.setOrganizationName(base.getOrganizationName());
        response.setBusinessFunction(base.getBusinessFunction());
        response.setClassification(base.getClassification());
        response.setComplianceTags(base.getComplianceTags());
        response.setRiskLevel(base.getRiskLevel());
        response.setRiskReason(base.getRiskReason());
        response.setOwnerId(base.getOwnerId());
        response.setRequestable(base.getRequestable());
        response.setRequireApproval(base.getRequireApproval());
        response.setCertificationSchedule(base.getCertificationSchedule());
        response.setCreatedAt(base.getCreatedAt());
        response.setUpdatedAt(base.getUpdatedAt());

        response.setEntitlementIds(entitlementService.getEntitlementIds(roleId));
        response.setChildRoles(compositionService.getChildRoles(roleId));
        response.setParentRoles(compositionService.getParentRoles(roleId));
        response.setEntitlementCount(response.getEntitlementIds().size());
        enrichOrganizationNames(tenantId, List.of(response));
        enrichOrganizationNames(tenantId, response.getChildRoles());
        enrichOrganizationNames(tenantId, response.getParentRoles());

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_ROLE_FETCHED)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<AccessRoleResponse> listRoles(
            String tenantId,
            List<String> types,
            List<String> statuses,
            List<String> businessUnits,
            List<String> riskLevels,
            String search,
            Pageable pageable
    ) {
        List<String> normalizedTypes = normalizeFilters(types);
        List<String> normalizedStatuses = normalizeFilters(statuses);
        List<String> normalizedBusinessUnits = normalizeFilters(businessUnits);
        List<String> normalizedRiskLevels = normalizeFilters(riskLevels);
        String normalizedSearch = normalizeFilter(search);

        Specification<AccessRole> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

            if (!normalizedTypes.isEmpty()) {
                predicates.add(root.get("type").in(normalizedTypes));
            }

            if (!normalizedStatuses.isEmpty()) {
                predicates.add(root.get("status").in(normalizedStatuses));
            }

            if (!normalizedBusinessUnits.isEmpty()) {
                predicates.add(root.get("businessUnit").in(normalizedBusinessUnits));
            }

            if (!normalizedRiskLevels.isEmpty()) {
                List<String> loweredRiskLevels = normalizedRiskLevels.stream()
                        .map(String::toLowerCase)
                        .toList();

                predicates.add(criteriaBuilder.lower(root.get("riskLevel")).in(loweredRiskLevels));
            }

            if (normalizedSearch != null) {
                String likePattern = "%" + normalizedSearch.toLowerCase() + "%";
                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
                                criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("displayName"), "")), likePattern),
                                criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")), likePattern)
                        )
                );
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<AccessRole> page = roleRepository.findAll(specification, pageable);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_ROLES_LISTED)
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_COUNT, String.valueOf(page.getTotalElements()))
                .log();

        Page<AccessRoleResponse> responsePage = page.map(entity -> {
            AccessRoleResponse response = normalizedResponse(entity);
            response.setEntitlementCount(entitlementLinkRepository.countByRoleId(entity.getId()));
            return response;
        });
        enrichOrganizationNames(tenantId, responsePage.getContent());
        return responsePage;
    }

    /**
     * Normalizes an optional list of filter values by trimming whitespace,
     * removing blank values, and returning a non-null list.
     *
     * @param values raw filter values
     * @return normalized non-empty filter values, or empty list when none exist
     */
    private List<String> normalizeFilters(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .map(this::normalizeFilter)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccessRoleDirectorySummaryResponseDto getRoleDirectorySummary(
            String tenantId,
            List<String> statuses,
            List<String> riskLevels,
            String search
    ) {
        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_ROLE_DIRECTORY_SUMMARY_START)
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        List<String> normalizedStatuses = normalizeFilters(statuses);
        List<String> normalizedRiskLevels = normalizeFilters(riskLevels);
        String normalizedSearch = normalizeFilter(search);

        Specification<AccessRole> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));

            if (!normalizedStatuses.isEmpty()) {
                predicates.add(root.get("status").in(normalizedStatuses));
            }

            if (!normalizedRiskLevels.isEmpty()) {
                List<String> loweredRiskLevels = normalizedRiskLevels.stream()
                        .map(String::toLowerCase)
                        .toList();

                predicates.add(criteriaBuilder.lower(root.get("riskLevel")).in(loweredRiskLevels));
            }

            if (normalizedSearch != null) {
                String likePattern = "%" + normalizedSearch.toLowerCase() + "%";
                predicates.add(
                        criteriaBuilder.or(
                                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
                                criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("displayName"), "")), likePattern),
                                criteriaBuilder.like(criteriaBuilder.lower(criteriaBuilder.coalesce(root.get("description"), "")), likePattern)
                        )
                );
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        List<AccessRole> roles = roleRepository.findAll(specification);

        Map<String, AccessRoleDirectoryBusinessUnitSummaryDto> groupedByBusinessUnit = new LinkedHashMap<>();
        long totalRoles = 0L;

        for (AccessRole role : roles) {
            String businessUnit = role.getBusinessUnit();
            if (businessUnit == null || businessUnit.isBlank()) {
                businessUnit = AccessRoleConstants.DEFAULT_BUSINESS_UNIT;
            }

            String type = defaultIfBlank(role.getType(), AccessRoleConstants.DEFAULT_ROLE_TYPE);
            if (type == null || type.isBlank()) {
                continue;
            }

            AccessRoleDirectoryBusinessUnitSummaryDto unitDto = groupedByBusinessUnit.computeIfAbsent(
                    businessUnit,
                    key -> {
                        AccessRoleDirectoryBusinessUnitSummaryDto dto =
                                new AccessRoleDirectoryBusinessUnitSummaryDto();
                        dto.setBusinessUnit(key);
                        dto.setGroups(new ArrayList<>());
                        dto.setTotalRoles(0L);
                        return dto;
                    }
            );

            AccessRoleDirectoryGroupSummaryDto existingGroup = unitDto.getGroups().stream()
                    .filter(group -> type.equals(group.getGroupKey()))
                    .findFirst()
                    .orElse(null);

            if (existingGroup == null) {
                existingGroup = new AccessRoleDirectoryGroupSummaryDto(type, 0L);
                unitDto.getGroups().add(existingGroup);
            }

            existingGroup.setCount(existingGroup.getCount() + 1);
            unitDto.setTotalRoles(unitDto.getTotalRoles() + 1);
            totalRoles++;
        }

        AccessRoleDirectorySummaryResponseDto response = new AccessRoleDirectorySummaryResponseDto();
        response.setTotalRoles(totalRoles);
        response.setBusinessUnits(new ArrayList<>(groupedByBusinessUnit.values()));

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_ROLE_DIRECTORY_SUMMARY_COMPLETE)
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_COUNT, String.valueOf(response.getTotalRoles()))
                .log();

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @AuditAccessRoleChange(action = AccessRoleAuditAction.ROLE_UPDATED)
    public AccessRoleResponse updateRole(String tenantId, UUID roleId, UpdateAccessRoleRequest request) {
        AccessRole entity = findTenantRoleOrThrow(tenantId, roleId);
        if (request.getOrganization() != null) {
            validateOrganizationIfPresent(tenantId, request.getOrganization());
        }
        roleMapper.updateEntityFromRequest(request, entity);
        AccessRole saved = roleRepository.save(entity);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_ROLE_UPDATED)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, saved.getId().toString())
                .detail(AccessRoleConstants.DETAIL_ROLE_NAME, saved.getName())
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        AccessRoleResponse response = normalizedResponse(saved);
        response.setEntitlementCount(entitlementLinkRepository.countByRoleId(saved.getId()));
        enrichOrganizationNames(tenantId, List.of(response));
        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccessRoleDeleteImpactResponse getDeleteImpact(String tenantId, UUID roleId) {
        findTenantRoleOrThrow(tenantId, roleId);

        long activeAssignmentsRevoked = identityRoleAssignmentRepository.countByTenantIdAndRoleIdAndStatus(
                tenantId,
                roleId,
                IdentityRoleAssignment.Status.ACTIVE);
        long distinctUsersAffected = identityRoleAssignmentRepository.countDistinctIdentityIdByTenantIdAndRoleIdAndStatus(
                tenantId,
                roleId,
                IdentityRoleAssignment.Status.ACTIVE);
        long directEntitlementLinksRemoved = entitlementLinkRepository.countByRoleId(roleId);
        long effectiveEntitlementRowsRemoved = roleEntitlementClosureRepository.countByTenantIdAndRoleId(tenantId, roleId);
        long distinctEffectiveEntitlementsAffected = roleEntitlementClosureRepository
                .countDistinctEntitlementIdByTenantIdAndRoleId(tenantId, roleId);
        long childRoleLinksRemoved = compositionRepository.countByParentRoleId(roleId);
        long parentRoleLinksRemoved = compositionRepository.countByChildRoleId(roleId);
        long parentRolesToRefresh = compositionRepository.countDistinctParentRoleIdByChildRoleId(roleId);
        long assignmentRulesImpacted = assignmentRuleRepository.countByTenantIdAndRoleId(tenantId, roleId);
        long enabledAssignmentRulesImpacted = assignmentRuleRepository.countByTenantIdAndRoleIdAndEnabledTrue(tenantId, roleId);
        long executionRunsImpacted = assignmentExecutionRepository.countByTenantIdAndRoleId(tenantId, roleId);
        long executionItemsImpacted = assignmentExecutionItemRepository.countByTenantIdAndRoleId(tenantId, roleId);
        long auditRecordsImpacted = auditLogRepository.countByTenantIdAndRoleId(tenantId, roleId);
        long totalLinksRemoved = directEntitlementLinksRemoved + childRoleLinksRemoved + parentRoleLinksRemoved;

        return new AccessRoleDeleteImpactResponse(
                1,
                activeAssignmentsRevoked,
                distinctUsersAffected,
                directEntitlementLinksRemoved,
                effectiveEntitlementRowsRemoved,
                distinctEffectiveEntitlementsAffected,
                childRoleLinksRemoved,
                parentRoleLinksRemoved,
                parentRolesToRefresh,
                assignmentRulesImpacted,
                enabledAssignmentRulesImpacted,
                executionRunsImpacted,
                executionItemsImpacted,
                auditRecordsImpacted,
                totalLinksRemoved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public AccessRoleSaveSimulationResponse simulateRoleSaveImpact(String tenantId,
                                                                   UUID roleId,
                                                                   AccessRoleSaveSimulationRequest request,
                                                                   boolean includeUnchanged) {
        AccessRole role = findTenantRoleOrThrow(tenantId, roleId);
        AccessRoleSaveSimulationRequest effectiveRequest = request != null ? request : new AccessRoleSaveSimulationRequest();
        boolean includeUnchangedRows = includeUnchanged || Boolean.TRUE.equals(effectiveRequest.getIncludeUnchanged());

        List<IdentityRoleAssignment> currentAssignments = identityRoleAssignmentRepository
                .findByTenantIdAndRoleIdAndStatus(tenantId, roleId, IdentityRoleAssignment.Status.ACTIVE);
        Set<UUID> currentIdentityIds = currentAssignments.stream()
                .map(IdentityRoleAssignment::getIdentityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> proposedIdentityIds = requestedIdsOrCurrent(effectiveRequest.getIdentityIds(), currentIdentityIds);

        Set<UUID> currentDirectEntitlementIds = entitlementLinkRepository.findByRoleId(roleId).stream()
                .map(link -> link.getEntitlementId())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> proposedDirectEntitlementIds = requestedIdsOrCurrent(
                effectiveRequest.getEntitlementIds(),
                currentDirectEntitlementIds);

        Set<UUID> currentChildRoleIds = compositionRepository.findByParentRoleId(roleId).stream()
                .map(AccessRoleComposition::getChildRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> proposedChildRoleIds = requestedIdsOrCurrent(effectiveRequest.getChildRoleIds(), currentChildRoleIds);

        List<SimulationClosure> currentClosures = roleEntitlementClosureRepository.findByTenantIdAndRoleId(tenantId, roleId).stream()
                .map(this::toSimulationClosure)
                .toList();
        List<SimulationClosure> proposedClosures = buildProposedSimulationClosures(
                tenantId,
                roleId,
                proposedDirectEntitlementIds,
                proposedChildRoleIds);

        Set<UUID> allIdentityIds = union(currentIdentityIds, proposedIdentityIds);
        Map<UUID, Identity> identityById = loadIdentitiesById(tenantId, allIdentityIds);
        Map<UUID, Entitlement> entitlementById = loadEntitlementsById(tenantId, union(
                currentClosures.stream().map(SimulationClosure::entitlementId).collect(Collectors.toCollection(LinkedHashSet::new)),
                proposedClosures.stream().map(SimulationClosure::entitlementId).collect(Collectors.toCollection(LinkedHashSet::new))));
        Map<UUID, String> applicationNames = loadApplicationNames(tenantId, entitlementById.values().stream().toList());
        Map<UUID, AccessRole> roleById = loadRolesForSimulation(tenantId, role, currentClosures, proposedClosures, union(currentChildRoleIds, proposedChildRoleIds));

        Set<UUID> addedIdentityIds = difference(proposedIdentityIds, currentIdentityIds);
        Set<UUID> removedIdentityIds = difference(currentIdentityIds, proposedIdentityIds);
        Set<UUID> addedDirectEntitlements = difference(proposedDirectEntitlementIds, currentDirectEntitlementIds);
        Set<UUID> removedDirectEntitlements = difference(currentDirectEntitlementIds, proposedDirectEntitlementIds);
        Set<UUID> addedChildRoles = difference(proposedChildRoleIds, currentChildRoleIds);
        Set<UUID> removedChildRoles = difference(currentChildRoleIds, proposedChildRoleIds);

        List<AccessRoleSaveSimulationResponse.IdentityImpact> affectedIdentities = new ArrayList<>();
        List<AccessRoleSaveSimulationResponse.EntitlementImpact> affectedEntitlements = new ArrayList<>();
        long addedEntitlementRows = 0;
        long removedEntitlementRows = 0;
        long unchangedEntitlementRows = 0;
        int highestRiskRank = 1;

        for (UUID identityId : allIdentityIds) {
            boolean currentlyAssigned = currentIdentityIds.contains(identityId);
            boolean willBeAssigned = proposedIdentityIds.contains(identityId);
            Identity identity = identityById.get(identityId);
            String identityName = resolveIdentityName(identity, identityId);
            String identityRisk = normalizeSimulationRisk(resolveIdentityRiskLevel(identity));
            highestRiskRank = Math.max(highestRiskRank, riskRank(identityRisk));

            Set<SimulationClosure> identityCurrentClosures = currentlyAssigned
                    ? new LinkedHashSet<>(currentClosures)
                    : new LinkedHashSet<>();
            Set<SimulationClosure> identityProposedClosures = willBeAssigned
                    ? new LinkedHashSet<>(proposedClosures)
                    : new LinkedHashSet<>();

            Set<SimulationClosure> entitlementRowsToAdd = difference(identityProposedClosures, identityCurrentClosures);
            Set<SimulationClosure> entitlementRowsToRemove = difference(identityCurrentClosures, identityProposedClosures);
            Set<SimulationClosure> entitlementRowsUnchanged = intersection(identityCurrentClosures, identityProposedClosures);

            List<AccessRoleSaveSimulationResponse.EntitlementImpact> entitlementImpacts = new ArrayList<>();
            for (SimulationClosure closure : entitlementRowsToAdd) {
                AccessRoleSaveSimulationResponse.EntitlementImpact impact = toSimulationEntitlementImpact(
                        identityId,
                        identityName,
                        role,
                        closure,
                        entitlementById.get(closure.entitlementId()),
                        applicationNames,
                        roleById,
                        "Entitlement Added",
                        "Will Be Assigned",
                        false,
                        true);
                entitlementImpacts.add(impact);
                affectedEntitlements.add(impact);
                addedEntitlementRows++;
                highestRiskRank = Math.max(highestRiskRank, riskRank(impact.riskLevel()));
            }
            for (SimulationClosure closure : entitlementRowsToRemove) {
                AccessRoleSaveSimulationResponse.EntitlementImpact impact = toSimulationEntitlementImpact(
                        identityId,
                        identityName,
                        role,
                        closure,
                        entitlementById.get(closure.entitlementId()),
                        applicationNames,
                        roleById,
                        "Entitlement Revoked",
                        "Will Be Revoked",
                        true,
                        false);
                entitlementImpacts.add(impact);
                affectedEntitlements.add(impact);
                removedEntitlementRows++;
                highestRiskRank = Math.max(highestRiskRank, riskRank(impact.riskLevel()));
            }
            if (includeUnchangedRows) {
                for (SimulationClosure closure : entitlementRowsUnchanged) {
                    AccessRoleSaveSimulationResponse.EntitlementImpact impact = toSimulationEntitlementImpact(
                            identityId,
                            identityName,
                            role,
                            closure,
                            entitlementById.get(closure.entitlementId()),
                            applicationNames,
                            roleById,
                            "Entitlement Unchanged",
                            "Already Assigned",
                            true,
                            true);
                    entitlementImpacts.add(impact);
                    affectedEntitlements.add(impact);
                    unchangedEntitlementRows++;
                }
            }

            List<AccessRoleSaveSimulationResponse.ChildRoleImpact> childRoleImpacts = buildChildRoleImpacts(
                    currentlyAssigned,
                    willBeAssigned,
                    currentChildRoleIds,
                    proposedChildRoleIds,
                    roleById,
                    includeUnchangedRows);
            AccessRoleSaveSimulationResponse.RoleImpact roleImpact = buildRoleImpact(
                    roleId,
                    proposedRoleName(role, effectiveRequest),
                    currentlyAssigned,
                    willBeAssigned,
                    includeUnchangedRows);
            boolean hasChange = currentlyAssigned != willBeAssigned
                    || !entitlementRowsToAdd.isEmpty()
                    || !entitlementRowsToRemove.isEmpty()
                    || childRoleImpacts.stream().anyMatch(impact -> !impact.impactType().endsWith("Unchanged"));
            if (hasChange || includeUnchangedRows && (!entitlementImpacts.isEmpty() || roleImpact != null || !childRoleImpacts.isEmpty())) {
                List<String> impactTypes = buildIdentityImpactTypes(roleImpact, childRoleImpacts, entitlementImpacts);
                affectedIdentities.add(new AccessRoleSaveSimulationResponse.IdentityImpact(
                        identityId,
                        identityName,
                        identity != null && identity.getIdentityType() != null ? identity.getIdentityType().name() : null,
                        identityRisk,
                        primaryImpactType(impactTypes),
                        impactTypes,
                        roleImpact,
                        childRoleImpacts,
                        entitlementImpacts));
            }
        }

        long directEntitlementChanges = addedDirectEntitlements.size() + removedDirectEntitlements.size();
        long childRoleChanges = addedChildRoles.size() + removedChildRoles.size();
        int impactScore = calculateImpactScore(
                addedIdentityIds.size(),
                removedIdentityIds.size(),
                addedEntitlementRows,
                removedEntitlementRows,
                directEntitlementChanges,
                childRoleChanges,
                highestRiskRank);
        String impactSeverity = impactSeverity(impactScore, highestRiskRank);
        AccessRoleSaveSimulationResponse.ImpactScoreDetails impactScoreDetails = impactScoreDetails(
                addedIdentityIds.size(),
                removedIdentityIds.size(),
                addedEntitlementRows,
                removedEntitlementRows,
                directEntitlementChanges,
                childRoleChanges,
                highestRiskRank,
                impactScore);
        AccessRoleSaveSimulationResponse.Summary summary = new AccessRoleSaveSimulationResponse.Summary(
                affectedIdentities.stream().filter(this::hasActualSimulationChange).count(),
                addedIdentityIds.size(),
                removedIdentityIds.size(),
                addedEntitlementRows,
                removedEntitlementRows,
                includeUnchangedRows ? unchangedEntitlementRows : null,
                directEntitlementChanges,
                childRoleChanges);

        return new AccessRoleSaveSimulationResponse(
                roleId,
                proposedRoleName(role, effectiveRequest),
                "ROLE_SAVE",
                impactScore,
                impactSeverity,
                impactScoreDetails,
                summary,
                affectedIdentities,
                affectedEntitlements);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @AuditAccessRoleChange(action = AccessRoleAuditAction.ROLE_DELETED)
    public void deleteRole(String tenantId, UUID roleId) {
        AccessRole entity = findTenantRoleOrThrow(tenantId, roleId);
        Set<UUID> formerParentRoleIds = compositionRepository.findByChildRoleId(roleId).stream()
                .map(AccessRoleComposition::getParentRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<IdentityRoleAssignment> activeAssignments = identityRoleAssignmentRepository
                .findByTenantIdAndRoleIdAndStatus(tenantId, roleId, IdentityRoleAssignment.Status.ACTIVE);
        for (IdentityRoleAssignment assignment : activeAssignments) {
            assignment.revoke();
        }
        if (!activeAssignments.isEmpty()) {
            identityRoleAssignmentRepository.saveAllAndFlush(activeAssignments);
        }
        for (IdentityRoleAssignment assignment : activeAssignments) {
            roleEntitlementAssignmentService.revokeRoleEntitlements(
                    tenantId,
                    assignment.getIdentityId(),
                    roleId,
                    assignment.getAssignmentRuleId());
        }

        entitlementLinkRepository.deleteByRoleId(roleId);
        compositionRepository.deleteByParentRoleId(roleId);
        compositionRepository.deleteByChildRoleId(roleId);
        roleEntitlementClosureRepository.deleteByTenantIdAndRoleId(tenantId, roleId);
        for (UUID parentRoleId : formerParentRoleIds) {
            roleEntitlementClosureService.rebuildRoleAndAncestors(tenantId, parentRoleId);
        }
        roleRepository.delete(entity);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(AccessRoleConstants.LOG_ROLE_DELETED)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleConstants.DETAIL_ROLE_NAME, entity.getName())
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .log();
    }

    /**
     * Normalizes an optional filter value by trimming surrounding whitespace
     * and converting blank strings to {@code null}.
     *
     * @param value raw filter value
     * @return trimmed value or {@code null} when empty
     */
    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<AccessRoleMemberResponse> filterMemberRows(List<AccessRoleMemberResponse> rows,
                                                            String search,
                                                            List<String> identityTypes,
                                                            List<String> identityRiskLevels,
                                                            List<String> accessPathTypes,
                                                            List<String> assignmentSourceTypes,
                                                            List<UUID> applicationIds,
                                                            List<UUID> entitlementIds) {
        String normalizedSearch = normalizeFilter(search);
        Set<String> normalizedIdentityTypes = normalizeFilterSet(identityTypes);
        Set<String> normalizedIdentityRiskLevels = normalizeFilterSet(identityRiskLevels);
        Set<String> normalizedAccessPathTypes = normalizeFilterSet(accessPathTypes);
        Set<String> normalizedAssignmentSourceTypes = normalizeAssignmentSourceFilterSet(assignmentSourceTypes);
        Set<UUID> normalizedApplicationIds = normalizeUuidFilterSet(applicationIds);
        Set<UUID> normalizedEntitlementIds = normalizeUuidFilterSet(entitlementIds);

        return rows.stream()
                .filter(row -> matchesMemberSearch(row, normalizedSearch))
                .filter(row -> matchesTextFilter(row.identityType(), normalizedIdentityTypes))
                .filter(row -> matchesTextFilter(row.identityRiskLevel(), normalizedIdentityRiskLevels))
                .filter(row -> matchesTextFilter(row.accessPathType(), normalizedAccessPathTypes))
                .filter(row -> matchesTextFilter(row.assignmentSourceType(), normalizedAssignmentSourceTypes))
                .filter(row -> matchesUuidFilter(row.applicationId(), normalizedApplicationIds))
                .filter(row -> matchesUuidFilter(row.entitlementId(), normalizedEntitlementIds))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<AccessRoleMemberResponse> filterMemberEntitlementRows(List<AccessRoleMemberResponse> rows,
                                                                       String search,
                                                                       List<String> accessPathTypes,
                                                                       List<UUID> applicationIds,
                                                                       List<UUID> entitlementIds) {
        String normalizedSearch = normalizeFilter(search);
        Set<String> normalizedAccessPathTypes = normalizeFilterSet(accessPathTypes);
        Set<UUID> normalizedApplicationIds = normalizeUuidFilterSet(applicationIds);
        Set<UUID> normalizedEntitlementIds = normalizeUuidFilterSet(entitlementIds);

        return rows.stream()
                .filter(row -> matchesMemberEntitlementSearch(row, normalizedSearch))
                .filter(row -> matchesTextFilter(row.accessPathType(), normalizedAccessPathTypes))
                .filter(row -> matchesUuidFilter(row.applicationId(), normalizedApplicationIds))
                .filter(row -> matchesUuidFilter(row.entitlementId(), normalizedEntitlementIds))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private Set<String> normalizeFilterSet(List<String> values) {
        return normalizeFilters(values).stream()
                .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Set<String> normalizeAssignmentSourceFilterSet(List<String> values) {
        return normalizeFilters(values).stream()
                .map(this::normalizeAssignmentSourceFilterValue)
                .collect(Collectors.toSet());
    }

    private String normalizeAssignmentSourceFilterValue(String value) {
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        return "import".equals(normalized) ? "manual" : normalized;
    }

    private Set<UUID> normalizeUuidFilterSet(List<UUID> values) {
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean matchesMemberSearch(AccessRoleMemberResponse row, String search) {
        if (search == null) {
            return true;
        }
        String needle = search.toLowerCase(java.util.Locale.ROOT);
        return containsIgnoreCase(row.identityName(), needle)
                || containsIgnoreCase(row.entitlementName(), needle)
                || containsIgnoreCase(row.applicationName(), needle)
                || containsIgnoreCase(row.roleName(), needle)
                || containsIgnoreCase(row.sourceRoleName(), needle)
                || containsIgnoreCase(row.childRoleName(), needle)
                || containsIgnoreCase(row.identityId(), needle)
                || containsIgnoreCase(row.entitlementId(), needle)
                || containsIgnoreCase(row.applicationId(), needle);
    }

    private boolean matchesGroupedMemberSearch(AccessRoleGroupedMemberResponse row, String search) {
        if (search == null) {
            return true;
        }
        String needle = search.toLowerCase(java.util.Locale.ROOT);
        return containsIgnoreCase(row.identityName(), needle);
    }

    private boolean matchesMemberEntitlementSearch(AccessRoleMemberResponse row, String search) {
        if (search == null) {
            return true;
        }
        String needle = search.toLowerCase(java.util.Locale.ROOT);
        return containsIgnoreCase(row.entitlementName(), needle)
                || containsIgnoreCase(row.applicationName(), needle)
                || containsIgnoreCase(row.sourceRoleName(), needle)
                || containsIgnoreCase(row.childRoleName(), needle)
                || containsIgnoreCase(buildAssignmentPath(row.rolePath()), needle);
    }

    private boolean matchesTextFilter(String value, Set<String> filters) {
        if (filters.isEmpty()) {
            return true;
        }
        if (value == null) {
            return false;
        }
        String normalizedValue = value.toLowerCase(java.util.Locale.ROOT);
        if (filters.contains(normalizedValue)) {
            return true;
        }

        String normalizedAccessPathValue = normalizeAccessPathFilterValue(normalizedValue);
        if (normalizedAccessPathValue == null) {
            return false;
        }

        return filters.stream()
                .anyMatch(filter -> normalizedAccessPathValue.equals(normalizeAccessPathFilterValue(filter)));
    }

    private boolean matchesUuidFilter(UUID value, Set<UUID> filters) {
        return filters.isEmpty() || value != null && filters.contains(value);
    }

    private boolean containsIgnoreCase(Object value, String needle) {
        return value != null && value.toString().toLowerCase(java.util.Locale.ROOT).contains(needle);
    }

    private Page<AccessRoleMemberResponse> toPage(List<AccessRoleMemberResponse> rows, Pageable pageable) {
        Pageable effectivePageable = pageable != null ? pageable : Pageable.unpaged();
        if (effectivePageable.isUnpaged()) {
            return new PageImpl<>(rows);
        }

        int start = (int) Math.min(effectivePageable.getOffset(), rows.size());
        int end = Math.min(start + effectivePageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(start, end), effectivePageable, rows.size());
    }

    private Page<AccessRoleGroupedMemberResponse> toGroupedPage(List<AccessRoleGroupedMemberResponse> rows, Pageable pageable) {
        Pageable effectivePageable = pageable != null ? pageable : Pageable.unpaged();
        if (effectivePageable.isUnpaged()) {
            return new PageImpl<>(rows);
        }

        int start = (int) Math.min(effectivePageable.getOffset(), rows.size());
        int end = Math.min(start + effectivePageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(start, end), effectivePageable, rows.size());
    }

    private Page<AccessRoleMemberEntitlementResponse> toMemberEntitlementPage(List<AccessRoleMemberEntitlementResponse> rows,
                                                                              Pageable pageable) {
        Pageable effectivePageable = pageable != null ? pageable : Pageable.unpaged();
        if (effectivePageable.isUnpaged()) {
            return new PageImpl<>(rows);
        }

        int start = (int) Math.min(effectivePageable.getOffset(), rows.size());
        int end = Math.min(start + effectivePageable.getPageSize(), rows.size());
        return new PageImpl<>(rows.subList(start, end), effectivePageable, rows.size());
    }

    private Set<UUID> requestedIdsOrCurrent(List<UUID> requestedIds, Set<UUID> currentIds) {
        if (requestedIds == null) {
            return new LinkedHashSet<>(currentIds);
        }
        return requestedIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<SimulationClosure> buildProposedSimulationClosures(String tenantId,
                                                                    UUID roleId,
                                                                    Set<UUID> directEntitlementIds,
                                                                    Set<UUID> childRoleIds) {
        List<SimulationClosure> closures = new ArrayList<>();
        for (UUID entitlementId : directEntitlementIds) {
            closures.add(new SimulationClosure(
                    entitlementId,
                    roleId,
                    true,
                    0,
                    List.of(roleId)));
        }
        for (UUID childRoleId : childRoleIds) {
            List<RoleEntitlementClosure> childClosures = roleEntitlementClosureRepository.findByTenantIdAndRoleId(tenantId, childRoleId);
            for (RoleEntitlementClosure childClosure : childClosures) {
                List<UUID> childPath = childClosure.getPathRoleIds() != null && !childClosure.getPathRoleIds().isEmpty()
                        ? childClosure.getPathRoleIds()
                        : List.of(childRoleId);
                List<UUID> parentPath = new ArrayList<>();
                parentPath.add(roleId);
                parentPath.addAll(childPath);
                closures.add(new SimulationClosure(
                        childClosure.getEntitlementId(),
                        childClosure.getSourceRoleId(),
                        false,
                        childClosure.getDepth() + 1,
                        parentPath));
            }
        }
        return closures.stream().distinct().toList();
    }

    private SimulationClosure toSimulationClosure(RoleEntitlementClosure closure) {
        List<UUID> pathRoleIds = closure.getPathRoleIds() != null && !closure.getPathRoleIds().isEmpty()
                ? closure.getPathRoleIds()
                : closure.isDirect()
                ? List.of(closure.getRoleId())
                : List.of(closure.getRoleId(), closure.getSourceRoleId());
        return new SimulationClosure(
                closure.getEntitlementId(),
                closure.getSourceRoleId(),
                closure.isDirect(),
                closure.getDepth(),
                pathRoleIds);
    }

    private Map<UUID, Identity> loadIdentitiesById(String tenantId, Set<UUID> identityIds) {
        if (identityIds == null || identityIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Identity> identities = new HashMap<>();
        identityRepository.findAllById(identityIds).forEach(identity -> {
            if (tenantId.equals(identity.getTenantId())) {
                identities.put(identity.getId(), identity);
            }
        });
        return identities;
    }

    private Map<UUID, Entitlement> loadEntitlementsById(String tenantId, Set<UUID> entitlementIds) {
        if (entitlementIds == null || entitlementIds.isEmpty()) {
            return Map.of();
        }
        return entitlementRepository.findAllById(entitlementIds).stream()
                .filter(entitlement -> tenantId.equals(entitlement.getTenantId()))
                .collect(Collectors.toMap(
                        Entitlement::getId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    private Map<UUID, AccessRole> loadRolesForSimulation(String tenantId,
                                                         AccessRole role,
                                                         List<SimulationClosure> currentClosures,
                                                         List<SimulationClosure> proposedClosures,
                                                         Set<UUID> childRoleIds) {
        Set<UUID> roleIds = new LinkedHashSet<>();
        roleIds.add(role.getId());
        if (childRoleIds != null) {
            roleIds.addAll(childRoleIds);
        }
        currentClosures.forEach(closure -> collectSimulationClosureRoleIds(closure, roleIds));
        proposedClosures.forEach(closure -> collectSimulationClosureRoleIds(closure, roleIds));
        Map<UUID, AccessRole> roles = roleRepository.findByIdIn(roleIds).stream()
                .filter(candidate -> tenantId.equals(candidate.getTenantId()))
                .collect(Collectors.toMap(AccessRole::getId, Function.identity(), (existing, replacement) -> existing));
        roles.put(role.getId(), role);
        return roles;
    }

    private void collectSimulationClosureRoleIds(SimulationClosure closure, Set<UUID> roleIds) {
        if (closure.sourceRoleId() != null) {
            roleIds.add(closure.sourceRoleId());
        }
        if (closure.pathRoleIds() != null) {
            roleIds.addAll(closure.pathRoleIds());
        }
    }

    private AccessRoleSaveSimulationResponse.EntitlementImpact toSimulationEntitlementImpact(
            UUID identityId,
            String identityName,
            AccessRole role,
            SimulationClosure closure,
            Entitlement entitlement,
            Map<UUID, String> applicationNames,
            Map<UUID, AccessRole> roleById,
            String impactType,
            String status,
            boolean currentlyAssigned,
            boolean willBeAssigned) {
        UUID childRoleId = !closure.direct() ? firstChildRoleId(role.getId(), closure.pathRoleIds()) : null;
        AccessRole sourceRole = roleById.get(closure.sourceRoleId());
        AccessRole childRole = childRoleId != null ? roleById.get(childRoleId) : null;
        String riskLevel = normalizeSimulationRisk(entitlement != null ? entitlement.getRiskLevel() : null);
        return new AccessRoleSaveSimulationResponse.EntitlementImpact(
                identityId,
                identityName,
                closure.entitlementId(),
                resolveEntitlementName(entitlement),
                entitlement != null ? entitlement.getResourceId() : null,
                entitlement != null ? applicationNames.get(entitlement.getResourceId()) : null,
                impactType,
                status,
                "Role",
                status.contains("Assigned") || status.contains("Revoked") ? status.replace("Will Be ", "") : status,
                currentlyAssigned,
                willBeAssigned,
                riskLevel,
                role.getId(),
                resolveRoleName(role),
                childRoleId,
                resolveRoleName(childRole != null ? childRole : sourceRole),
                buildSimulationRolePath(closure.pathRoleIds(), roleById));
    }

    private UUID firstChildRoleId(UUID roleId, List<UUID> pathRoleIds) {
        if (pathRoleIds == null || pathRoleIds.size() < 2) {
            return null;
        }
        return Objects.equals(pathRoleIds.get(0), roleId) ? pathRoleIds.get(1) : pathRoleIds.get(0);
    }

    private List<AccessRoleSaveSimulationResponse.RolePathNode> buildSimulationRolePath(List<UUID> pathRoleIds,
                                                                                        Map<UUID, AccessRole> roleById) {
        if (pathRoleIds == null || pathRoleIds.isEmpty()) {
            return List.of();
        }
        List<AccessRoleSaveSimulationResponse.RolePathNode> path = new ArrayList<>();
        for (int i = 0; i < pathRoleIds.size(); i++) {
            UUID pathRoleId = pathRoleIds.get(i);
            path.add(new AccessRoleSaveSimulationResponse.RolePathNode(
                    pathRoleId,
                    resolveRoleName(roleById.get(pathRoleId)),
                    i));
        }
        return path;
    }

    private List<AccessRoleSaveSimulationResponse.ChildRoleImpact> buildChildRoleImpacts(boolean currentlyAssigned,
                                                                                        boolean willBeAssigned,
                                                                                        Set<UUID> currentChildRoleIds,
                                                                                        Set<UUID> proposedChildRoleIds,
                                                                                        Map<UUID, AccessRole> roleById,
                                                                                        boolean includeUnchanged) {
        Set<UUID> added = !currentlyAssigned && willBeAssigned
                ? proposedChildRoleIds
                : willBeAssigned ? difference(proposedChildRoleIds, currentChildRoleIds) : Set.of();
        Set<UUID> removed = currentlyAssigned && !willBeAssigned
                ? currentChildRoleIds
                : currentlyAssigned ? difference(currentChildRoleIds, proposedChildRoleIds) : Set.of();
        Set<UUID> unchanged = currentlyAssigned && willBeAssigned && includeUnchanged
                ? intersection(currentChildRoleIds, proposedChildRoleIds)
                : Set.of();

        List<AccessRoleSaveSimulationResponse.ChildRoleImpact> impacts = new ArrayList<>();
        added.forEach(childRoleId -> impacts.add(toChildRoleImpact(childRoleId, roleById, "Child Role Added", "Will Be Inherited", false, true)));
        removed.forEach(childRoleId -> impacts.add(toChildRoleImpact(childRoleId, roleById, "Child Role Removed", "Will No Longer Be Inherited", true, false)));
        unchanged.forEach(childRoleId -> impacts.add(toChildRoleImpact(childRoleId, roleById, "Child Role Unchanged", "Already Inherited", true, true)));
        return impacts;
    }

    private AccessRoleSaveSimulationResponse.ChildRoleImpact toChildRoleImpact(UUID childRoleId,
                                                                               Map<UUID, AccessRole> roleById,
                                                                               String impactType,
                                                                               String status,
                                                                               boolean currentlyInherited,
                                                                               boolean willBeInherited) {
        return new AccessRoleSaveSimulationResponse.ChildRoleImpact(
                childRoleId,
                resolveRoleName(roleById.get(childRoleId)),
                impactType,
                status,
                currentlyInherited,
                willBeInherited);
    }

    private AccessRoleSaveSimulationResponse.RoleImpact buildRoleImpact(UUID roleId,
                                                                        String roleName,
                                                                        boolean currentlyAssigned,
                                                                        boolean willBeAssigned,
                                                                        boolean includeUnchanged) {
        if (!currentlyAssigned && willBeAssigned) {
            return new AccessRoleSaveSimulationResponse.RoleImpact(roleId, roleName, "Role Added", "Will Be Assigned", false, true);
        }
        if (currentlyAssigned && !willBeAssigned) {
            return new AccessRoleSaveSimulationResponse.RoleImpact(roleId, roleName, "Role Removed", "Will Be Revoked", true, false);
        }
        if (includeUnchanged && currentlyAssigned) {
            return new AccessRoleSaveSimulationResponse.RoleImpact(roleId, roleName, "Role Unchanged", "Already Assigned", true, true);
        }
        return null;
    }

    private String proposedRoleName(AccessRole role, AccessRoleSaveSimulationRequest request) {
        return firstNonBlank(request.getRoleName(), resolveRoleName(role));
    }

    private List<String> buildIdentityImpactTypes(AccessRoleSaveSimulationResponse.RoleImpact roleImpact,
                                                  List<AccessRoleSaveSimulationResponse.ChildRoleImpact> childRoleImpacts,
                                                  List<AccessRoleSaveSimulationResponse.EntitlementImpact> entitlementImpacts) {
        Set<String> impactTypes = new LinkedHashSet<>();
        if (roleImpact != null) {
            impactTypes.add(roleImpact.impactType());
        }
        childRoleImpacts.stream()
                .map(AccessRoleSaveSimulationResponse.ChildRoleImpact::impactType)
                .forEach(impactTypes::add);
        entitlementImpacts.stream()
                .map(AccessRoleSaveSimulationResponse.EntitlementImpact::impactType)
                .forEach(impactTypes::add);
        return impactTypes.isEmpty() ? List.of("No Change") : new ArrayList<>(impactTypes);
    }

    private boolean hasActualSimulationChange(AccessRoleSaveSimulationResponse.IdentityImpact identityImpact) {
        return identityImpact.impactTypes() != null
                && identityImpact.impactTypes().stream()
                .anyMatch(impactType -> !"No Change".equals(impactType) && !impactType.endsWith("Unchanged"));
    }

    private String primaryImpactType(List<String> impactTypes) {
        if (impactTypes == null || impactTypes.isEmpty()) {
            return "No Change";
        }
        return impactTypes.stream()
                .filter(impactType -> !impactType.endsWith("Unchanged"))
                .findFirst()
                .orElse(impactTypes.getFirst());
    }

    private String normalizeSimulationRisk(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return "Low";
        }
        return switch (riskLevel.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "high", "critical", "severe" -> "High";
            case "medium", "moderate" -> "Medium";
            default -> "Low";
        };
    }

    private int riskRank(String riskLevel) {
        return switch (normalizeSimulationRisk(riskLevel)) {
            case "High" -> 3;
            case "Medium" -> 2;
            default -> 1;
        };
    }

    private int calculateImpactScore(long usersAddedToRole,
                                     long usersRemovedFromRole,
                                     long addedEntitlements,
                                     long removedEntitlements,
                                     long directEntitlementChanges,
                                     long childRoleChanges,
                                     int highestRiskRank) {
        long riskBoost = highestRiskRank == 3 ? 25 : highestRiskRank == 2 ? 10 : 0;
        long score = usersAddedToRole * 5
                + usersRemovedFromRole * 5
                + addedEntitlements * 3
                + removedEntitlements * 4
                + directEntitlementChanges * 2
                + childRoleChanges * 4
                + riskBoost;
        return (int) Math.min(100, score);
    }

    private AccessRoleSaveSimulationResponse.ImpactScoreDetails impactScoreDetails(long usersAddedToRole,
                                                                                   long usersRemovedFromRole,
                                                                                   long addedEntitlements,
                                                                                   long removedEntitlements,
                                                                                   long directEntitlementChanges,
                                                                                   long childRoleChanges,
                                                                                   int highestRiskRank,
                                                                                   int impactScore) {
        Map<String, Integer> weights = new LinkedHashMap<>();
        weights.put("usersAddedToRole", 5);
        weights.put("usersRemovedFromRole", 5);
        weights.put("addedEntitlements", 3);
        weights.put("removedEntitlements", 4);
        weights.put("directEntitlementChanges", 2);
        weights.put("childRoleChanges", 4);
        weights.put("riskBoostHigh", 25);
        weights.put("riskBoostMedium", 10);
        weights.put("riskBoostLow", 0);

        Map<String, Long> inputs = new LinkedHashMap<>();
        inputs.put("usersAddedToRole", usersAddedToRole);
        inputs.put("usersRemovedFromRole", usersRemovedFromRole);
        inputs.put("addedEntitlements", addedEntitlements);
        inputs.put("removedEntitlements", removedEntitlements);
        inputs.put("directEntitlementChanges", directEntitlementChanges);
        inputs.put("childRoleChanges", childRoleChanges);

        long riskBoost = highestRiskRank == 3 ? 25 : highestRiskRank == 2 ? 10 : 0;
        Map<String, Long> contributions = new LinkedHashMap<>();
        contributions.put("usersAddedToRole", usersAddedToRole * 5);
        contributions.put("usersRemovedFromRole", usersRemovedFromRole * 5);
        contributions.put("addedEntitlements", addedEntitlements * 3);
        contributions.put("removedEntitlements", removedEntitlements * 4);
        contributions.put("directEntitlementChanges", directEntitlementChanges * 2);
        contributions.put("childRoleChanges", childRoleChanges * 4);
        contributions.put("riskBoost", riskBoost);
        long rawScore = contributions.values().stream().mapToLong(Long::longValue).sum();

        return new AccessRoleSaveSimulationResponse.ImpactScoreDetails(
                "min(100, usersAddedToRole*5 + usersRemovedFromRole*5 + addedEntitlements*3 + removedEntitlements*4 + directEntitlementChanges*2 + childRoleChanges*4 + riskBoost)",
                100,
                weights,
                inputs,
                contributions,
                riskLabel(highestRiskRank),
                riskBoost,
                rawScore);
    }

    private String riskLabel(int riskRank) {
        return switch (riskRank) {
            case 3 -> "High";
            case 2 -> "Medium";
            default -> "Low";
        };
    }

    private String impactSeverity(int impactScore, int highestRiskRank) {
        if (highestRiskRank == 3 || impactScore >= 71) {
            return "High";
        }
        if (highestRiskRank == 2 || impactScore >= 31) {
            return "Medium";
        }
        return "Low";
    }

    private <T> Set<T> union(Set<T> left, Set<T> right) {
        Set<T> result = new LinkedHashSet<>();
        if (left != null) {
            result.addAll(left);
        }
        if (right != null) {
            result.addAll(right);
        }
        return result;
    }

    private <T> Set<T> difference(Set<T> left, Set<T> right) {
        Set<T> result = new LinkedHashSet<>();
        if (left != null) {
            result.addAll(left);
        }
        if (right != null) {
            result.removeAll(right);
        }
        return result;
    }

    private <T> Set<T> intersection(Set<T> left, Set<T> right) {
        Set<T> result = new LinkedHashSet<>();
        if (left != null) {
            result.addAll(left);
        }
        if (right != null) {
            result.retainAll(right);
        } else {
            result.clear();
        }
        return result;
    }

    private record SimulationClosure(
            UUID entitlementId,
            UUID sourceRoleId,
            boolean direct,
            int depth,
            List<UUID> pathRoleIds
    ) {
    }

    private Map<UUID, Identity> loadIdentities(String tenantId, List<IdentityRoleAssignment> assignments) {
        Set<UUID> identityIds = assignments.stream()
                .map(IdentityRoleAssignment::getIdentityId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (identityIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Identity> identities = new HashMap<>();
        identityRepository.findAllById(identityIds).forEach(identity -> {
            if (tenantId.equals(identity.getTenantId())) {
                identities.put(identity.getId(), identity);
            }
        });
        return identities;
    }

    private Map<UUID, Entitlement> loadEntitlements(String tenantId, List<RoleEntitlementClosure> closures) {
        List<UUID> entitlementIds = closures.stream()
                .map(RoleEntitlementClosure::getEntitlementId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (entitlementIds.isEmpty()) {
            return Map.of();
        }

        return entitlementRepository.findAllById(entitlementIds).stream()
                .filter(entitlement -> tenantId.equals(entitlement.getTenantId()))
                .collect(Collectors.toMap(
                        Entitlement::getId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
    }

    private Map<UUID, String> loadApplicationNames(String tenantId, List<Entitlement> entitlements) {
        List<UUID> resourceIds = entitlements.stream()
                .map(Entitlement::getResourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (resourceIds.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> names = applicationClient.getApplicationNames(tenantId, resourceIds);
        return names != null ? names : Map.of();
    }

    private Map<UUID, AccessRole> loadRoles(String tenantId, AccessRole role, List<RoleEntitlementClosure> closures) {
        Set<UUID> roleIds = new LinkedHashSet<>();
        roleIds.add(role.getId());

        for (RoleEntitlementClosure closure : closures) {
            if (closure.getSourceRoleId() != null) {
                roleIds.add(closure.getSourceRoleId());
            }
            if (closure.getPathRoleIds() != null) {
                roleIds.addAll(closure.getPathRoleIds());
            }
        }

        Map<UUID, AccessRole> roles = roleRepository.findByIdIn(roleIds).stream()
                .filter(candidate -> tenantId.equals(candidate.getTenantId()))
                .collect(Collectors.toMap(AccessRole::getId, Function.identity()));
        roles.put(role.getId(), role);
        return roles;
    }

    private AccessRoleMemberResponse toMemberResponse(IdentityRoleAssignment assignment,
                                                       AccessRole assignedRole,
                                                       RoleEntitlementClosure closure,
                                                       Identity identity,
                                                       Entitlement entitlement,
                                                       Map<UUID, String> applicationNames,
                                                       Map<UUID, AccessRole> roleById) {
        AccessRole sourceRole = closure != null ? roleById.get(closure.getSourceRoleId()) : null;
        UUID childRoleId = closure != null && !closure.isDirect() ? closure.getSourceRoleId() : null;
        String childRoleName = closure != null && !closure.isDirect() ? resolveRoleName(sourceRole) : null;

        return new AccessRoleMemberResponse(
                assignment.getId(),
                assignment.getIdentityId(),
                resolveIdentityName(identity, assignment.getIdentityId()),
                identity != null && identity.getIdentityType() != null ? identity.getIdentityType().name() : null,
                resolveIdentityRiskLevel(identity),
                assignedRole.getId(),
                resolveRoleName(assignedRole),
                assignment.getAssignedAt(),
                entitlement != null ? entitlement.getId() : null,
                resolveEntitlementName(entitlement),
                entitlement != null ? entitlement.getResourceId() : null,
                entitlement != null ? applicationNames.get(entitlement.getResourceId()) : null,
                resolveAccessPathType(closure),
                closure != null ? closure.getSourceRoleId() : null,
                resolveRoleName(sourceRole),
                childRoleId,
                childRoleName,
                closure != null ? closure.isDirect() : null,
                closure != null ? closure.getDepth() : null,
                buildRolePath(assignedRole, closure, roleById),
                resolveDisplayAssignmentSourceType(assignment));
    }

    private String resolveDisplayAssignmentSourceType(IdentityRoleAssignment assignment) {
        if (assignment == null || assignment.getSourceType() == null) {
            return null;
        }
        if (assignment.getSourceType() == IdentityRoleAssignment.SourceType.IMPORT) {
            return IdentityRoleAssignment.SourceType.MANUAL.name();
        }
        return assignment.getSourceType().name();
    }

    private AccessRoleMemberEntitlementResponse toMemberEntitlementResponse(
            AccessRoleMemberResponse row,
            String assignmentStatus,
            String assignmentStatusReason) {
        return new AccessRoleMemberEntitlementResponse(
                row.entitlementId(),
                row.entitlementName(),
                row.applicationId(),
                row.applicationName(),
                row.accessPathType(),
                buildAccessPathLabel(row),
                row.childRoleId(),
                row.childRoleName(),
                buildInheritanceDepthLabel(row.roleEntitlementDepth()),
                buildAssignmentPath(row.rolePath()),
                assignmentStatus,
                assignmentStatusReason);
    }

    private String resolveIdentityName(Identity identity, UUID identityId) {
        if (identity instanceof HumanIdentity humanIdentity) {
            String displayName = firstNonBlank(
                    humanIdentity.getDisplayName(),
                    joinName(humanIdentity.getFirstName(), humanIdentity.getLastName()),
                    humanIdentity.getUsername(),
                    humanIdentity.getEmail());
            if (displayName != null) {
                return displayName;
            }
        }

        if (identity instanceof NonHumanIdentity nonHumanIdentity) {
            String serviceName = normalizeFilter(nonHumanIdentity.getServiceName());
            if (serviceName != null) {
                return serviceName;
            }
        }

        return identityId != null ? identityId.toString() : null;
    }

    private String resolveIdentityRiskLevel(Identity identity) {
        if (identity instanceof HumanIdentity humanIdentity) {
            return humanIdentity.getRiskLevel();
        }
        if (identity instanceof NonHumanIdentity nonHumanIdentity) {
            return nonHumanIdentity.getRiskLevel();
        }
        return null;
    }

    private String resolveRoleName(AccessRole role) {
        if (role == null) {
            return null;
        }
        return firstNonBlank(role.getDisplayName(), role.getName(), role.getId() != null ? role.getId().toString() : null);
    }

    private String resolveEntitlementName(Entitlement entitlement) {
        if (entitlement == null) {
            return null;
        }
        return firstNonBlank(entitlement.getDisplayName(), entitlement.getSourceUid(), entitlement.getId().toString());
    }

    private String resolveAccessPathType(RoleEntitlementClosure closure) {
        if (closure == null) {
            return "No Entitlement";
        }
        return closure.isDirect() ? "Direct Role" : "Child Role";
    }

    private String normalizeAccessPathFilterValue(String value) {
        if (value == null) {
            return null;
        }

        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "direct_through_role", "direct role", "direct" -> "direct role";
            case "child_role", "child role", "child" -> "child role";
            case "no_entitlement", "no entitlement" -> "no entitlement";
            default -> null;
        };
    }

    private String buildAssignmentPathSummary(List<RoleEntitlementClosure> closures, int childRoleCount) {
        if (closures == null || closures.isEmpty()) {
            return "Direct role assignment";
        }

        boolean hasDirect = closures.stream().anyMatch(RoleEntitlementClosure::isDirect);
        if (childRoleCount <= 0) {
            return hasDirect ? "Direct entitlements through this role" : "Role assignment without effective entitlements";
        }

        String childRoleSegment = childRoleCount == 1
                ? "1 child role"
                : childRoleCount + " child roles";

        if (hasDirect) {
            return "Direct + inherited through " + childRoleSegment;
        }
        return "Inherited through " + childRoleSegment;
    }

    private String buildAccessPathLabel(AccessRoleMemberResponse row) {
        if (row == null) {
            return null;
        }
        if (Boolean.TRUE.equals(row.roleEntitlementDirect())) {
            return "Directly linked to this role";
        }
        if (row.childRoleName() != null) {
            return "Inherited through child role " + row.childRoleName();
        }
        return "Inherited through child role";
    }

    private String buildInheritanceDepthLabel(Integer depth) {
        if (depth == null) {
            return null;
        }
        if (depth <= 0) {
            return "Direct";
        }
        if (depth == 1) {
            return "Inherited from 1 child role";
        }
        return "Inherited from " + depth + " child roles";
    }

    private String buildAssignmentPath(List<AccessRoleMemberResponse.RolePathNode> rolePath) {
        if (rolePath == null || rolePath.isEmpty()) {
            return null;
        }
        return rolePath.stream()
                .map(AccessRoleMemberResponse.RolePathNode::roleName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" → "));
    }

    private List<AccessRoleMemberResponse.RolePathNode> buildRolePath(AccessRole assignedRole,
                                                                       RoleEntitlementClosure closure,
                                                                       Map<UUID, AccessRole> roleById) {
        if (closure == null) {
            return List.of();
        }

        List<UUID> pathIds;
        if (closure.getPathRoleIds() != null && !closure.getPathRoleIds().isEmpty()) {
            pathIds = closure.getPathRoleIds();
        } else if (closure.isDirect()) {
            pathIds = List.of(assignedRole.getId());
        } else {
            pathIds = List.of(assignedRole.getId(), closure.getSourceRoleId());
        }

        List<AccessRoleMemberResponse.RolePathNode> path = new ArrayList<>();
        for (int i = 0; i < pathIds.size(); i++) {
            UUID pathRoleId = pathIds.get(i);
            AccessRole pathRole = roleById.get(pathRoleId);
            path.add(new AccessRoleMemberResponse.RolePathNode(
                    pathRoleId,
                    resolveRoleName(pathRole),
                    i));
        }
        return path;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeFilter(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String joinName(String firstName, String lastName) {
        String first = normalizeFilter(firstName);
        String last = normalizeFilter(lastName);
        if (first == null && last == null) {
            return null;
        }
        if (first == null) {
            return last;
        }
        if (last == null) {
            return first;
        }
        return first + " " + last;
    }

    /**
     * Finds a role by ID or throws {@link EntityNotFoundException}.
     *
     * @param roleId the role ID
     * @return the found entity
     */
    private AccessRole findRoleOrThrow(UUID roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> {
                    logger.warn()
                            .category(AccessRoleConstants.LOG_CATEGORY)
                            .message(AccessRoleConstants.LOG_ROLE_NOT_FOUND)
                            .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                            .log();
                    return new EntityNotFoundException(
                            String.format(AccessRoleConstants.ERROR_ROLE_NOT_FOUND, roleId));
                });
    }

    private void applyCreateDefaults(CreateAccessRoleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Role request is required");
        }
        request.setName(defaultIfBlank(request.getName(), null));
        if (request.getName() == null) {
            throw new IllegalArgumentException("Role name is required");
        }
        request.setDisplayName(defaultIfBlank(request.getDisplayName(), request.getName()));
        request.setType(defaultIfBlank(request.getType(), AccessRoleConstants.DEFAULT_ROLE_TYPE));
        request.setBusinessUnit(defaultIfBlank(request.getBusinessUnit(), AccessRoleConstants.DEFAULT_BUSINESS_UNIT));
        request.setStatus(defaultIfBlank(request.getStatus(), AccessRoleConstants.DEFAULT_STATUS));
        request.setRequestable(Boolean.TRUE.equals(request.getRequestable()));
        request.setRequireApproval(Boolean.TRUE.equals(request.getRequireApproval()));
    }

    private AccessRoleResponse normalizedResponse(AccessRole entity) {
        AccessRoleResponse response = roleMapper.entityToResponse(entity);
        normalizeResponse(response);
        return response;
    }

    private void normalizeResponse(AccessRoleResponse response) {
        if (response == null) {
            return;
        }
        response.setDisplayName(defaultIfBlank(response.getDisplayName(), response.getName()));
        response.setType(defaultIfBlank(response.getType(), AccessRoleConstants.DEFAULT_ROLE_TYPE));
        response.setBusinessUnit(defaultIfBlank(response.getBusinessUnit(), AccessRoleConstants.DEFAULT_BUSINESS_UNIT));
        response.setStatus(defaultIfBlank(response.getStatus(), AccessRoleConstants.DEFAULT_STATUS));
        response.setRequestable(Boolean.TRUE.equals(response.getRequestable()));
        response.setRequireApproval(Boolean.TRUE.equals(response.getRequireApproval()));
    }

    private void normalizeResponses(List<? extends AccessRoleResponse> responses) {
        if (responses != null) {
            responses.forEach(this::normalizeResponse);
        }
    }

    private static String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    private void validateOrganizationIfPresent(String tenantId, UUID organization) {
        if (organization == null) {
            return;
        }
        orgUnitRepository.findByIdAndTenantId(organization, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format(AccessRoleConstants.ERROR_ORGANIZATION_NOT_FOUND, tenantId)));
    }

    private void enrichOrganizationNames(String tenantId, List<? extends AccessRoleResponse> responses) {
        normalizeResponses(responses);
        if (responses == null || responses.isEmpty()) {
            return;
        }

        Set<UUID> organizationIds = responses.stream()
                .filter(Objects::nonNull)
                .map(AccessRoleResponse::getOrganization)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (organizationIds.isEmpty()) {
            return;
        }

        Map<UUID, String> organizationNameById = orgUnitRepository.findAllById(organizationIds).stream()
                .filter(orgUnit -> tenantId.equals(orgUnit.getTenantId()))
                .collect(Collectors.toMap(
                        OrgUnit::getId,
                        OrgUnit::getName,
                        (existing, replacement) -> existing));
        for (AccessRoleResponse response : responses) {
            if (response != null && response.getOrganization() != null) {
                response.setOrganizationName(organizationNameById.get(response.getOrganization()));
            }
        }
    }

    private AccessRole findTenantRoleOrThrow(String tenantId, UUID roleId) {
        return roleRepository.findById(roleId)
                .filter(role -> tenantId.equals(role.getTenantId()))
                .orElseThrow(() -> {
                    logger.warn()
                            .category(AccessRoleConstants.LOG_CATEGORY)
                            .message(AccessRoleConstants.LOG_ROLE_NOT_FOUND)
                            .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                            .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                            .log();
                    return new EntityNotFoundException(
                            String.format(AccessRoleConstants.ERROR_ROLE_NOT_FOUND, roleId));
                });
    }
}

