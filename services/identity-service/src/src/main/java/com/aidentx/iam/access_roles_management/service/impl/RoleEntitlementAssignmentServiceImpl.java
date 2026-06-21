package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementAssignmentService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementResolutionService;
import com.aidentx.iam.entitlement_management.model.entity.Entitlement;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAssignment;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.iam.entitlement_management.repository.EntitlementRepository;
import com.aidentx.iam.shadow_management.model.entity.Shadow;
import com.aidentx.iam.shadow_management.repository.ShadowRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default role-to-entitlement propagation service.
 *
 * <p>When an identity receives an access role, this service resolves the role's effective
 * direct + inherited entitlements and assigns them to the identity's linked account shadows.
 * When that role is revoked, the service removes only the entitlement assignments that are no
 * longer justified by any remaining active access-role assignments for the same identity.</p>
 */
@Service
public class RoleEntitlementAssignmentServiceImpl implements RoleEntitlementAssignmentService {

    private final RoleEntitlementResolutionService resolutionService;
    private final EntitlementRepository entitlementRepository;
    private final EntitlementAssignmentRepository entitlementAssignmentRepository;
    private final ShadowRepository shadowRepository;
    private final IdentityRoleAssignmentRepository identityRoleAssignmentRepository;
    private final IUnifiedLogger logger;

    public RoleEntitlementAssignmentServiceImpl(RoleEntitlementResolutionService resolutionService,
                                                EntitlementRepository entitlementRepository,
                                                EntitlementAssignmentRepository entitlementAssignmentRepository,
                                                ShadowRepository shadowRepository,
                                                IdentityRoleAssignmentRepository identityRoleAssignmentRepository,
                                                UnifiedLoggerFactory loggerFactory) {
        this.resolutionService = resolutionService;
        this.entitlementRepository = entitlementRepository;
        this.entitlementAssignmentRepository = entitlementAssignmentRepository;
        this.shadowRepository = shadowRepository;
        this.identityRoleAssignmentRepository = identityRoleAssignmentRepository;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentRuleConstants.LOG_CATEGORY);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void applyRoleEntitlements(String tenantId,
                                      UUID identityId,
                                      UUID roleId,
                                      UUID assignmentRuleId,
                                      IdentityRoleAssignment.SourceType sourceType,
                                      String assignedByUserId,
                                      String assignedByUserName) {
        validateInputs(tenantId, identityId, roleId);

        String assignmentSource = AccessRoleAssignmentRuleConstants.ENTITLEMENT_ASSIGNMENT_SOURCE_ROLE_DERIVED;

        logger.info()
                .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ENTITLEMENT_SYNC_STARTED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, stringifyUuid(assignmentRuleId))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SOURCE_TYPE, assignmentSource)
                .log();

        List<Entitlement> effectiveEntitlements = loadEffectiveEntitlements(tenantId, roleId);
        if (effectiveEntitlements.isEmpty()) {
            logger.debug()
                    .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ENTITLEMENT_SYNC_SKIPPED_NO_ENTITLEMENTS)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                    .log();
            return;
        }

        List<Shadow> eligibleShadows = loadEligibleAccountShadows(tenantId, identityId);

        List<RoleEntitlementTarget> targets = buildTargets(effectiveEntitlements, eligibleShadows, identityId);
        if (targets.isEmpty()) {
            logger.debug()
                    .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ENTITLEMENT_SYNC_SKIPPED_NO_SHADOWS)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENTITLEMENT_COUNT, String.valueOf(effectiveEntitlements.size()))
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SHADOW_COUNT, String.valueOf(eligibleShadows.size()))
                    .log();
            return;
        }

        Map<String, EntitlementAssignment> existingAssignments = loadExistingAssignments(tenantId, targets, assignmentSource);
        List<EntitlementAssignment> assignmentsToSave = new ArrayList<>();
        for (RoleEntitlementTarget target : targets) {
            EntitlementAssignment existingAssignment = existingAssignments.get(target.key());
            if (existingAssignment == null && target.shadowId() != null) {
                existingAssignment = existingAssignments.get(identityAssignmentKey(target.identityId(), target.entitlementId()));
            }
            if (existingAssignment != null) {
                boolean changed = false;
                if (target.shadowId() != null && existingAssignment.getShadowId() == null) {
                    existingAssignment.setShadowId(target.shadowId());
                    existingAssignment.setStatus(EntitlementAssignment.AssignmentStatus.ASSIGNED);
                    changed = true;
                }
                if (isRoleDerivedAssignment(existingAssignment) && existingAssignment.getAccessRoleId() == null) {
                    existingAssignment.setAccessRoleId(roleId);
                    changed = true;
                }
                if (applyMissingAssignedBy(existingAssignment, assignedByUserId, assignedByUserName)) {
                    changed = true;
                }
                if (changed) {
                    assignmentsToSave.add(existingAssignment);
                }
                continue;
            }
            EntitlementAssignment assignment = new EntitlementAssignment();
            assignment.setTenantId(tenantId);
            assignment.setShadowId(target.shadowId());
            assignment.setIdentityId(target.identityId());
            assignment.setEntitlementId(target.entitlementId());
            assignment.setResourceId(target.resourceId());
            assignment.setSource(assignmentSource);
            assignment.setStatus(target.shadowId() == null
                    ? EntitlementAssignment.AssignmentStatus.PENDING
                    : EntitlementAssignment.AssignmentStatus.ASSIGNED);
            assignment.setRuleId(assignmentRuleId);
            assignment.setAccessRoleId(roleId);
            applyAssignedBy(assignment, assignedByUserId, assignedByUserName);
            assignmentsToSave.add(assignment);
        }

        if (!assignmentsToSave.isEmpty()) {
            entitlementAssignmentRepository.saveAll(assignmentsToSave);
        }

        logger.info()
                .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ENTITLEMENT_SYNC_COMPLETED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENTITLEMENT_COUNT, String.valueOf(effectiveEntitlements.size()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SHADOW_COUNT, String.valueOf(eligibleShadows.size()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_EXISTING_COUNT, String.valueOf(existingAssignments.size()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_CREATED_COUNT, String.valueOf(assignmentsToSave.size()))
                .log();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void revokeRoleEntitlements(String tenantId, UUID identityId, UUID roleId, UUID assignmentRuleId) {
        validateInputs(tenantId, identityId, roleId);

        logger.info()
                .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ENTITLEMENT_REVOKE_STARTED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_RULE_ID, stringifyUuid(assignmentRuleId))
                .log();

        List<Entitlement> roleEntitlements = loadEffectiveEntitlements(tenantId, roleId);
        if (roleEntitlements.isEmpty()) {
            logger.debug()
                    .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ENTITLEMENT_REVOKE_SKIPPED)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENTITLEMENT_COUNT, String.valueOf(0))
                    .log();
            return;
        }

        List<Shadow> eligibleShadows = loadEligibleAccountShadows(tenantId, identityId);

        List<RoleEntitlementTarget> targets = buildTargets(roleEntitlements, eligibleShadows, identityId);
        Map<String, EntitlementAssignment> existingAssignments = loadExistingAssignments(
                tenantId, targets, AccessRoleAssignmentRuleConstants.ENTITLEMENT_ASSIGNMENT_SOURCE_ROLE_DERIVED);
        if (existingAssignments.isEmpty()) {
            logger.debug()
                    .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ENTITLEMENT_REVOKE_SKIPPED)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENTITLEMENT_COUNT, String.valueOf(roleEntitlements.size()))
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SHADOW_COUNT, String.valueOf(eligibleShadows.size()))
                    .log();
            return;
        }

        List<IdentityRoleAssignment> activeAssignments = findActiveRoleAssignments(tenantId, identityId);
        Set<UUID> retainedEntitlementIds = resolveRetainedEntitlementIds(tenantId, activeAssignments);
        List<EntitlementAssignment> assignmentsToDelete = existingAssignments.values().stream()
                .filter(this::isRoleDerivedAssignment)
                .filter(assignment -> !retainedEntitlementIds.contains(assignment.getEntitlementId()))
                .toList();

        if (!assignmentsToDelete.isEmpty()) {
            entitlementAssignmentRepository.deleteAll(assignmentsToDelete);
        }

        logger.info()
                .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ENTITLEMENT_REVOKE_COMPLETED)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ENTITLEMENT_COUNT, String.valueOf(roleEntitlements.size()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SHADOW_COUNT, String.valueOf(eligibleShadows.size()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ACTIVE_ROLE_COUNT, String.valueOf(activeAssignments.size()))
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_REMOVED_COUNT, String.valueOf(assignmentsToDelete.size()))
                .log();
    }

    private List<Entitlement> loadEffectiveEntitlements(String tenantId, UUID roleId) {
        List<UUID> entitlementIds = resolutionService.resolveEffectiveEntitlementIds(tenantId, roleId);
        if (entitlementIds.isEmpty()) {
            return List.of();
        }
        return entitlementRepository.findAllById(entitlementIds).stream()
                .filter(entitlement -> tenantId.equals(entitlement.getTenantId()))
                .toList();
    }

    private List<Shadow> loadEligibleAccountShadows(String tenantId, UUID identityId) {
        return shadowRepository.findByLinkedIdentityIdAndTenantId(identityId, tenantId).stream()
                .filter(this::isEligibleAccountShadow)
                .toList();
    }

    private boolean isEligibleAccountShadow(Shadow shadow) {
        if (shadow == null) {
            return false;
        }
        if (!AccessRoleAssignmentRuleConstants.SHADOW_KIND_ACCOUNT.equalsIgnoreCase(shadow.getKind())) {
            return false;
        }
        return shadow.getSynchronizationSituation() != Shadow.SynchronizationSituation.DELETED;
    }

    private List<RoleEntitlementTarget> buildTargets(List<Entitlement> entitlements, List<Shadow> shadows, UUID identityId) {
        Map<String, List<Shadow>> shadowsByResourceId = shadows.stream()
                .collect(Collectors.groupingBy(Shadow::getResourceId, LinkedHashMap::new, Collectors.toList()));

        List<RoleEntitlementTarget> targets = new ArrayList<>();
        for (Entitlement entitlement : entitlements) {
            List<Shadow> resourceShadows = shadowsByResourceId.get(entitlement.getResourceId().toString());
            if (resourceShadows == null || resourceShadows.isEmpty()) {
                targets.add(new RoleEntitlementTarget(
                        null,
                        identityId,
                        entitlement.getId(),
                        entitlement.getResourceId(),
                        identityAssignmentKey(identityId, entitlement.getId())));
                continue;
            }
            for (Shadow shadow : resourceShadows) {
                targets.add(new RoleEntitlementTarget(
                        shadow.getId(),
                        identityId,
                        entitlement.getId(),
                        entitlement.getResourceId(),
                        assignmentKey(shadow.getId(), entitlement.getId())));
            }
        }
        return targets;
    }

    private Map<String, EntitlementAssignment> loadExistingAssignments(String tenantId,
                                                                       List<RoleEntitlementTarget> targets,
                                                                       String assignmentSource) {
        if (targets.isEmpty()) {
            return Map.of();
        }
        Set<UUID> shadowIds = targets.stream()
                .map(RoleEntitlementTarget::shadowId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> identityIds = targets.stream()
                .map(RoleEntitlementTarget::identityId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> entitlementIds = targets.stream()
                .map(RoleEntitlementTarget::entitlementId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (entitlementIds.isEmpty()) {
            return Map.of();
        }
        Map<String, EntitlementAssignment> existing = new LinkedHashMap<>();
        if (!shadowIds.isEmpty()) {
            entitlementAssignmentRepository.findByTenantIdAndShadowIdInAndEntitlementIdInAndSource(
                            tenantId, shadowIds, entitlementIds, assignmentSource)
                    .forEach(assignment -> existing.putIfAbsent(
                            assignmentKey(assignment.getShadowId(), assignment.getEntitlementId()), assignment));
        }
        if (!identityIds.isEmpty()) {
            entitlementAssignmentRepository.findPendingByTenantIdAndIdentityIdInAndEntitlementIdInAndSource(
                            tenantId, identityIds, entitlementIds, assignmentSource)
                    .forEach(assignment -> existing.putIfAbsent(
                            identityAssignmentKey(assignment.getIdentityId(), assignment.getEntitlementId()), assignment));
        }
        return existing;
    }

    private List<IdentityRoleAssignment> findActiveRoleAssignments(String tenantId, UUID identityId) {
        return identityRoleAssignmentRepository.findByTenantIdAndIdentityIdAndStatus(
                tenantId,
                identityId,
                IdentityRoleAssignment.Status.ACTIVE);
    }

    private Set<UUID> resolveRetainedEntitlementIds(String tenantId, List<IdentityRoleAssignment> activeAssignments) {
        Set<UUID> retainedEntitlements = new LinkedHashSet<>();
        for (IdentityRoleAssignment assignment : activeAssignments) {
            retainedEntitlements.addAll(resolutionService.resolveEffectiveEntitlementIds(tenantId, assignment.getRoleId()));
        }
        return retainedEntitlements;
    }

    private boolean isRoleDerivedAssignment(EntitlementAssignment assignment) {
        String source = assignment.getSource();
        return AccessRoleAssignmentRuleConstants.ENTITLEMENT_ASSIGNMENT_SOURCE_ROLE_DERIVED.equalsIgnoreCase(source);
    }

    private String assignmentKey(UUID shadowId, UUID entitlementId) {
        return shadowId + AccessRoleAssignmentRuleConstants.ASSIGNMENT_KEY_SEPARATOR + entitlementId;
    }

    private String identityAssignmentKey(UUID identityId, UUID entitlementId) {
        return "identity" + AccessRoleAssignmentRuleConstants.ASSIGNMENT_KEY_SEPARATOR
                + identityId + AccessRoleAssignmentRuleConstants.ASSIGNMENT_KEY_SEPARATOR + entitlementId;
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

    private void validateInputs(String tenantId, UUID identityId, UUID roleId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_TENANT_REQUIRED);
        }
        if (identityId == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_IDENTITY_ID_REQUIRED);
        }
        if (roleId == null) {
            throw new IllegalArgumentException(AccessRoleAssignmentRuleConstants.ERROR_ROLE_ID_REQUIRED);
        }
    }

    private String stringifyUuid(UUID value) {
        return value != null ? value.toString() : null;
    }


    private record RoleEntitlementTarget(UUID shadowId, UUID identityId, UUID entitlementId, UUID resourceId, String key) {
    }
}

