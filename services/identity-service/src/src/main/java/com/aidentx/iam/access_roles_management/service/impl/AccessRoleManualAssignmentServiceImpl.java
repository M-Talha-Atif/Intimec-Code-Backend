package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.audit.AccessRoleManualAssignmentAuditService;
import com.aidentx.iam.access_roles_management.config.constants.AccessRoleConstants;
import com.aidentx.iam.access_roles_management.model.dto.response.ManualRoleMemberGrantResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncManualMembersResultDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.service.AccessRoleManualAssignmentService;
import com.aidentx.iam.access_roles_management.service.IdentityRoleAssignmentService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementAssignmentService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementResolutionService;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignment;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates bulk manual role grants and sync (desired-state) operations
 * while delegating propagation to the existing identity-role assignment lifecycle service.
 */
@Service
@Transactional
public class AccessRoleManualAssignmentServiceImpl implements AccessRoleManualAssignmentService {

    private static final String MANUAL_SOURCE_KEY = IdentityRoleAssignment.SourceType.MANUAL.name();
    private static final String LOG_MANUAL_GRANTS_STARTED = "Bulk manual role grants started";
    private static final String LOG_MANUAL_GRANTS_COMPLETED = "Bulk manual role grants completed";
    private static final String LOG_MANUAL_GRANT_FAILED = "Manual role grant failed for identity";
    private static final String LOG_MANUAL_SYNC_STARTED = "Manual member sync started";
    private static final String LOG_MANUAL_SYNC_COMPLETED = "Manual member sync completed";
    private static final String LOG_MANUAL_SYNC_GRANT = "Sync: granting role to identity";
    private static final String LOG_MANUAL_SYNC_REVOKE = "Sync: revoking role from identity";
    private static final String LOG_MANUAL_SYNC_ENTITLEMENT_IMPACT = "Entitlement impact for identity";
    private static final String ERROR_IDENTITY_NOT_FOUND = "Identity not found for tenant";
    private static final String DETAIL_IDENTITY_ID = "identityId";

    private final AccessRoleRepository accessRoleRepository;
    private final IdentityRepository identityRepository;
    private final IdentityRoleAssignmentRepository assignmentRepository;
    private final IdentityRoleAssignmentService identityRoleAssignmentService;
    private final RoleEntitlementAssignmentService roleEntitlementAssignmentService;
    private final RoleEntitlementResolutionService roleEntitlementResolutionService;
    private final EntitlementRepository entitlementRepository;
    private final EntitlementAssignmentRepository entitlementAssignmentRepository;
    private final ShadowRepository shadowRepository;
    private final AccessRoleManualAssignmentAuditService auditService;
    private final IUnifiedLogger logger;

    public AccessRoleManualAssignmentServiceImpl(AccessRoleRepository accessRoleRepository,
                                                 IdentityRepository identityRepository,
                                                 IdentityRoleAssignmentRepository assignmentRepository,
                                                 IdentityRoleAssignmentService identityRoleAssignmentService,
                                                 RoleEntitlementAssignmentService roleEntitlementAssignmentService,
                                                 RoleEntitlementResolutionService roleEntitlementResolutionService,
                                                 EntitlementRepository entitlementRepository,
                                                 EntitlementAssignmentRepository entitlementAssignmentRepository,
                                                 ShadowRepository shadowRepository,
                                                 AccessRoleManualAssignmentAuditService auditService,
                                                 UnifiedLoggerFactory loggerFactory) {
        this.accessRoleRepository = accessRoleRepository;
        this.identityRepository = identityRepository;
        this.assignmentRepository = assignmentRepository;
        this.identityRoleAssignmentService = identityRoleAssignmentService;
        this.roleEntitlementAssignmentService = roleEntitlementAssignmentService;
        this.roleEntitlementResolutionService = roleEntitlementResolutionService;
        this.entitlementRepository = entitlementRepository;
        this.entitlementAssignmentRepository = entitlementAssignmentRepository;
        this.shadowRepository = shadowRepository;
        this.auditService = auditService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleConstants.LOG_CATEGORY);
    }

    @Override
    public ManualRoleMemberGrantResultDto grantRoleToMembers(String tenantId,
                                                             UUID roleId,
                                                             List<UUID> identityIds,
                                                             String assignedByUserId,
                                                             String assignedByUserName) {
        validateTenantId(tenantId);
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }

        AccessRole role = accessRoleRepository.findById(roleId)
                .filter(candidate -> tenantId.equals(candidate.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException(String.format(AccessRoleConstants.ERROR_ROLE_NOT_FOUND, roleId)));

        List<UUID> requestedIdentityIds = distinctIds(identityIds);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(LOG_MANUAL_GRANTS_STARTED)
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleConstants.DETAIL_COUNT, String.valueOf(requestedIdentityIds.size()))
                .log();

        Map<UUID, Identity> identitiesById = loadIdentitiesById(tenantId, requestedIdentityIds);
        List<ManualRoleMemberGrantResultDto.GrantedIdentity> added = new ArrayList<>();
        List<ManualRoleMemberGrantResultDto.GrantedIdentity> skipped = new ArrayList<>();
        List<ManualRoleMemberGrantResultDto.FailedIdentity> failed = new ArrayList<>();

        for (UUID identityId : requestedIdentityIds) {
            processIdentityGrant(tenantId, roleId, identityId, identitiesById.get(identityId),
                    assignedByUserId, assignedByUserName, added, skipped, failed);
        }

        ManualRoleMemberGrantResultDto result = new ManualRoleMemberGrantResultDto(added, skipped, failed);
        auditService.publishManualGrantAudit(tenantId, role, result);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(LOG_MANUAL_GRANTS_COMPLETED)
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail("requestedCount", String.valueOf(result.getRequestedCount()))
                .detail("addedCount", String.valueOf(result.getAddedCount()))
                .detail("skippedCount", String.valueOf(result.getSkippedCount()))
                .detail("failedCount", String.valueOf(result.getFailedCount()))
                .log();

        return result;
    }

    private Map<UUID, Identity> loadIdentitiesById(String tenantId, List<UUID> identityIds) {
        if (identityIds.isEmpty()) {
            return Map.of();
        }
        return identityRepository.findByTenantIdAndIdIn(tenantId, identityIds).stream()
                .collect(LinkedHashMap::new, (map, identity) -> map.put(identity.getId(), identity), Map::putAll);
    }

    private List<UUID> distinctIds(List<UUID> identityIds) {
        if (identityIds == null || identityIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> uniqueIds = new LinkedHashSet<>(identityIds);
        return new ArrayList<>(uniqueIds);
    }

    private ManualRoleMemberGrantResultDto.GrantedIdentity toGrantedIdentity(Identity identity) {
        return new ManualRoleMemberGrantResultDto.GrantedIdentity(
                identity.getId(),
                resolveIdentityName(identity),
                resolveIdentityType(identity));
    }

    private void processIdentityGrant(String tenantId,
                                      UUID roleId,
                                      UUID identityId,
                                      Identity identity,
                                      String assignedByUserId,
                                      String assignedByUserName,
                                      List<ManualRoleMemberGrantResultDto.GrantedIdentity> added,
                                      List<ManualRoleMemberGrantResultDto.GrantedIdentity> skipped,
                                      List<ManualRoleMemberGrantResultDto.FailedIdentity> failed) {
        if (identity == null) {
            failed.add(new ManualRoleMemberGrantResultDto.FailedIdentity(identityId, null, null, ERROR_IDENTITY_NOT_FOUND));
            return;
        }

        ManualRoleMemberGrantResultDto.GrantedIdentity identitySummary = toGrantedIdentity(identity);
        try {
            Optional<IdentityRoleAssignment> existingAssignment = assignmentRepository
                    .findByTenantIdAndIdentityIdAndRoleIdAndSourceTypeAndSourceKey(
                            tenantId,
                            identityId,
                            roleId,
                            IdentityRoleAssignment.SourceType.MANUAL,
                            MANUAL_SOURCE_KEY);

            identityRoleAssignmentService.assignRole(
                    tenantId,
                    identityId,
                    roleId,
                    IdentityRoleAssignment.SourceType.MANUAL,
                    null,
                    MANUAL_SOURCE_KEY,
                    assignedByUserId,
                    assignedByUserName);

            if (existingAssignment.isPresent()
                    && existingAssignment.get().getStatus() == IdentityRoleAssignment.Status.ACTIVE) {
                skipped.add(identitySummary);
                return;
            }
            added.add(identitySummary);
        } catch (Exception ex) {
            logGrantFailure(tenantId, roleId, identityId, ex);
            failed.add(new ManualRoleMemberGrantResultDto.FailedIdentity(
                    identityId,
                    identitySummary.getIdentityName(),
                    identitySummary.getIdentityType(),
                    ex.getMessage() != null ? ex.getMessage() : "Manual role grant failed"));
        }
    }

    private void logGrantFailure(String tenantId, UUID roleId, UUID identityId, Exception ex) {
        logger.error()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(LOG_MANUAL_GRANT_FAILED)
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(DETAIL_IDENTITY_ID, identityId.toString())
                .detail(AccessRoleConstants.DETAIL_EXCEPTION_TYPE, ex.getClass().getSimpleName())
                .detail(AccessRoleConstants.DETAIL_ERROR, ex.getMessage() != null ? ex.getMessage() : AccessRoleConstants.DETAIL_NOT_AVAILABLE)
                .log();
    }

    private String resolveIdentityName(Identity identity) {
        if (identity instanceof HumanIdentity humanIdentity) {
            if (humanIdentity.getDisplayName() != null && !humanIdentity.getDisplayName().isBlank()) {
                return humanIdentity.getDisplayName();
            }
            if (humanIdentity.getUsername() != null && !humanIdentity.getUsername().isBlank()) {
                return humanIdentity.getUsername();
            }
        }
        if (identity instanceof NonHumanIdentity nonHumanIdentity
                && nonHumanIdentity.getServiceName() != null
                && !nonHumanIdentity.getServiceName().isBlank()) {
            return nonHumanIdentity.getServiceName();
        }
        return identity.getId() != null ? identity.getId().toString() : AccessRoleConstants.DETAIL_NOT_AVAILABLE;
    }

    private String resolveIdentityType(Identity identity) {
        if (identity.getIdentityType() != null) {
            return identity.getIdentityType().name();
        }
        if (identity instanceof HumanIdentity) {
            return "HUMAN";
        }
        if (identity instanceof NonHumanIdentity) {
            return "NON_HUMAN";
        }
        return "IDENTITY";
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
    }

    // ===================================================================
    //  Sync Manual Members (Desired-State PUT)
    // ===================================================================

    @Override
    public SyncManualMembersResultDto syncManualMembers(String tenantId,
                                                        UUID roleId,
                                                        List<UUID> desiredIdentityIds,
                                                        String assignedByUserId,
                                                        String assignedByUserName) {
        validateTenantId(tenantId);
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }

        AccessRole role = accessRoleRepository.findById(roleId)
                .filter(candidate -> tenantId.equals(candidate.getTenantId()))
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format(AccessRoleConstants.ERROR_ROLE_NOT_FOUND, roleId)));

        Set<UUID> desiredSet = desiredIdentityIds != null
                ? new LinkedHashSet<>(desiredIdentityIds)
                : Set.of();

        // Load current active MANUAL assignments for this role
        List<IdentityRoleAssignment> currentManualAssignments = assignmentRepository
                .findByTenantIdAndRoleIdAndSourceTypeAndStatus(
                        tenantId, roleId,
                        IdentityRoleAssignment.SourceType.MANUAL,
                        IdentityRoleAssignment.Status.ACTIVE);

        Set<UUID> currentSet = currentManualAssignments.stream()
                .map(IdentityRoleAssignment::getIdentityId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Compute diff
        Set<UUID> toAdd = new LinkedHashSet<>(desiredSet);
        toAdd.removeAll(currentSet);

        Set<UUID> toRevoke = new LinkedHashSet<>(currentSet);
        toRevoke.removeAll(desiredSet);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(LOG_MANUAL_SYNC_STARTED)
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail("desiredCount", String.valueOf(desiredSet.size()))
                .detail("currentCount", String.valueOf(currentSet.size()))
                .detail("toAddCount", String.valueOf(toAdd.size()))
                .detail("toRevokeCount", String.valueOf(toRevoke.size()))
                .log();

        // Resolve effective entitlements for this role (for impact logging)
        List<UUID> effectiveEntitlementIds = roleEntitlementResolutionService
                .resolveEffectiveEntitlementIds(tenantId, roleId);
        Map<UUID, Entitlement> entitlementMap = loadEntitlementMap(effectiveEntitlementIds);

        // Load all identities involved in any change
        Set<UUID> allInvolvedIds = new LinkedHashSet<>();
        allInvolvedIds.addAll(toAdd);
        allInvolvedIds.addAll(toRevoke);
        Map<UUID, Identity> identitiesById = loadIdentitiesById(tenantId, new ArrayList<>(allInvolvedIds));

        // Process grants
        List<SyncManualMembersResultDto.MemberSyncEntry> added = new ArrayList<>();
        List<SyncManualMembersResultDto.MemberSyncEntry> skipped = new ArrayList<>();
        List<SyncManualMembersResultDto.FailedMember> failed = new ArrayList<>();

        for (UUID identityId : toAdd) {
            Identity identity = identitiesById.get(identityId);
            if (identity == null) {
                failed.add(new SyncManualMembersResultDto.FailedMember(
                        identityId, null, null, ERROR_IDENTITY_NOT_FOUND));
                continue;
            }
            try {
                logger.info()
                        .category(AccessRoleConstants.LOG_CATEGORY)
                        .message(LOG_MANUAL_SYNC_GRANT)
                        .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                        .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                        .detail(DETAIL_IDENTITY_ID, identityId.toString())
                        .detail("identityName", resolveIdentityName(identity))
                        .log();

                identityRoleAssignmentService.assignRole(
                        tenantId, identityId, roleId,
                        IdentityRoleAssignment.SourceType.MANUAL,
                        null, MANUAL_SOURCE_KEY,
                        assignedByUserId,
                        assignedByUserName);

                List<SyncManualMembersResultDto.EntitlementImpact> impact =
                        buildEntitlementImpact(tenantId, identityId, entitlementMap, "PROVISIONED");

                logEntitlementImpact(tenantId, roleId, identityId, impact, "GRANT");

                added.add(new SyncManualMembersResultDto.MemberSyncEntry(
                        identityId, resolveIdentityName(identity),
                        resolveIdentityType(identity), impact));

            } catch (Exception ex) {
                logGrantFailure(tenantId, roleId, identityId, ex);
                failed.add(new SyncManualMembersResultDto.FailedMember(
                        identityId, resolveIdentityName(identity),
                        resolveIdentityType(identity),
                        ex.getMessage() != null ? ex.getMessage() : "Manual role grant failed"));
            }
        }

        // Process revocations
        List<SyncManualMembersResultDto.MemberSyncEntry> revoked = new ArrayList<>();

        for (UUID identityId : toRevoke) {
            Identity identity = identitiesById.get(identityId);
            String identityName = identity != null ? resolveIdentityName(identity) : identityId.toString();
            String identityType = identity != null ? resolveIdentityType(identity) : "IDENTITY";
            try {
                logger.info()
                        .category(AccessRoleConstants.LOG_CATEGORY)
                        .message(LOG_MANUAL_SYNC_REVOKE)
                        .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                        .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                        .detail(DETAIL_IDENTITY_ID, identityId.toString())
                        .detail("identityName", identityName)
                        .log();

                // Capture entitlement impact BEFORE revocation
                List<SyncManualMembersResultDto.EntitlementImpact> impact =
                        buildEntitlementImpact(tenantId, identityId, entitlementMap, "DEPROVISIONED");

                identityRoleAssignmentService.revokeRole(
                        tenantId, identityId, roleId,
                        IdentityRoleAssignment.SourceType.MANUAL,
                        null, MANUAL_SOURCE_KEY);

                // Revoke role-derived entitlement assignments
                roleEntitlementAssignmentService.revokeRoleEntitlements(
                        tenantId, identityId, roleId, null);

                logEntitlementImpact(tenantId, roleId, identityId, impact, "REVOKE");

                revoked.add(new SyncManualMembersResultDto.MemberSyncEntry(
                        identityId, identityName, identityType, impact));

            } catch (Exception ex) {
                logGrantFailure(tenantId, roleId, identityId, ex);
                failed.add(new SyncManualMembersResultDto.FailedMember(
                        identityId, identityName, identityType,
                        ex.getMessage() != null ? ex.getMessage() : "Manual role revoke failed"));
            }
        }

        // Count total active MANUAL assignments after sync
        int totalActiveCount = assignmentRepository
                .findByTenantIdAndRoleIdAndSourceTypeAndStatus(
                        tenantId, roleId,
                        IdentityRoleAssignment.SourceType.MANUAL,
                        IdentityRoleAssignment.Status.ACTIVE)
                .size();

        SyncManualMembersResultDto result = new SyncManualMembersResultDto(
                added, revoked, skipped, failed, totalActiveCount);

        // Publish rich audit event (Kafka + DB) capturing per-identity entitlement
        // breakdown (provisioned / pending / deprovisioned). Failures are swallowed
        // inside the audit service so they cannot break the sync transaction.
        auditService.publishManualSyncAudit(tenantId, role, result);

        logger.info()
                .category(AccessRoleConstants.LOG_CATEGORY)
                .message(LOG_MANUAL_SYNC_COMPLETED)
                .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail("addedCount", String.valueOf(result.getAddedCount()))
                .detail("revokedCount", String.valueOf(result.getRevokedCount()))
                .detail("skippedCount", String.valueOf(result.getSkippedCount()))
                .detail("failedCount", String.valueOf(result.getFailedCount()))
                .detail("totalActiveCount", String.valueOf(result.getTotalActiveCount()))
                .log();

        return result;
    }

    // ===================================================================
    //  Entitlement Impact Helpers
    // ===================================================================

    private Map<UUID, Entitlement> loadEntitlementMap(List<UUID> entitlementIds) {
        if (entitlementIds == null || entitlementIds.isEmpty()) {
            return Map.of();
        }
        return entitlementRepository.findAllById(entitlementIds).stream()
                .collect(Collectors.toMap(Entitlement::getId, e -> e, (a, b) -> a, LinkedHashMap::new));
    }

    /**
     * Builds entitlement impact entries for a given identity, checking whether each
     * entitlement already has an assignment row in the {@code entitlement_assignments} table
     * for any of the identity's linked shadows.
     */
    private List<SyncManualMembersResultDto.EntitlementImpact> buildEntitlementImpact(
            String tenantId,
            UUID identityId,
            Map<UUID, Entitlement> entitlementMap,
            String action) {

        if (entitlementMap.isEmpty()) {
            return List.of();
        }

        // Load identity's shadows to check entitlement_assignments
        List<Shadow> shadows = shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, tenantId);
        Set<UUID> shadowIds = shadows.stream().map(Shadow::getId).collect(Collectors.toSet());

        // Check which entitlements already exist in assignment table
        Set<UUID> existingEntitlementIds = Set.of();
        if (!shadowIds.isEmpty()) {
            Collection<UUID> entitlementIds = entitlementMap.keySet();
            List<EntitlementAssignment> existingAssignments = entitlementAssignmentRepository
                    .findByTenantIdAndShadowIdInAndEntitlementIdIn(tenantId, shadowIds, entitlementIds);
            existingEntitlementIds = existingAssignments.stream()
                    .map(EntitlementAssignment::getEntitlementId)
                    .collect(Collectors.toSet());
        }

        List<SyncManualMembersResultDto.EntitlementImpact> impacts = new ArrayList<>();
        for (Entitlement ent : entitlementMap.values()) {
            boolean existsInTable = existingEntitlementIds.contains(ent.getId());
            impacts.add(new SyncManualMembersResultDto.EntitlementImpact(
                    ent.getId(),
                    ent.getDisplayName(),
                    ent.getResourceId(),
                    null, // application name resolved at display layer
                    action,
                    existsInTable));
        }
        return impacts;
    }

    private void logEntitlementImpact(String tenantId,
                                      UUID roleId,
                                      UUID identityId,
                                      List<SyncManualMembersResultDto.EntitlementImpact> impacts,
                                      String syncAction) {
        for (SyncManualMembersResultDto.EntitlementImpact impact : impacts) {
            logger.info()
                    .category(AccessRoleConstants.LOG_CATEGORY)
                    .message(LOG_MANUAL_SYNC_ENTITLEMENT_IMPACT)
                    .detail(AccessRoleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleConstants.DETAIL_ROLE_ID, roleId.toString())
                    .detail(DETAIL_IDENTITY_ID, identityId.toString())
                    .detail("entitlementId", impact.getEntitlementId().toString())
                    .detail("entitlementName", impact.getEntitlementName() != null ? impact.getEntitlementName() : "N/A")
                    .detail("action", impact.getAction())
                    .detail("syncAction", syncAction)
                    .detail("existedInAssignmentTable", String.valueOf(impact.isExistedInAssignmentTable()))
                    .log();
        }
    }
}

