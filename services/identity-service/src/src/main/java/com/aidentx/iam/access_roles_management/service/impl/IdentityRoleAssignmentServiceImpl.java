package com.aidentx.iam.access_roles_management.service.impl;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleAssignmentRuleConstants;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAssignmentRule;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.AccessRoleAssignmentRuleRepository;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import com.aidentx.iam.access_roles_management.service.IdentityRoleAssignmentService;
import com.aidentx.iam.access_roles_management.service.RoleEntitlementAssignmentService;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Idempotent implementation of identity-role assignment lifecycle operations.
 *
 * <p>The service validates tenant ownership for identities, roles, and optional
 * access-role assignment rules before persistence. It never creates duplicate
 * rows for the same identity-role-source tuple.</p>
 */
@Service
public class IdentityRoleAssignmentServiceImpl implements IdentityRoleAssignmentService {

    private final IdentityRoleAssignmentRepository assignmentRepository;
    private final IdentityRepository identityRepository;
    private final AccessRoleRepository accessRoleRepository;
    private final AccessRoleAssignmentRuleRepository ruleRepository;
    private final RoleEntitlementAssignmentService roleEntitlementAssignmentService;
    private final IUnifiedLogger logger;

    public IdentityRoleAssignmentServiceImpl(IdentityRoleAssignmentRepository assignmentRepository,
                                             IdentityRepository identityRepository,
                                             AccessRoleRepository accessRoleRepository,
                                             AccessRoleAssignmentRuleRepository ruleRepository,
                                             RoleEntitlementAssignmentService roleEntitlementAssignmentService,
                                             UnifiedLoggerFactory loggerFactory) {
        this.assignmentRepository = assignmentRepository;
        this.identityRepository = identityRepository;
        this.accessRoleRepository = accessRoleRepository;
        this.ruleRepository = ruleRepository;
        this.roleEntitlementAssignmentService = roleEntitlementAssignmentService;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentRuleConstants.LOG_CATEGORY);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public IdentityRoleAssignment assignRole(String tenantId,
                                             UUID identityId,
                                             UUID roleId,
                                             IdentityRoleAssignment.SourceType sourceType,
                                             UUID assignmentRuleId,
                                             String assignedByUserId,
                                             String assignedByUserName) {
        String sourceKey = IdentityRoleAssignment.buildDefaultSourceKey(sourceType, assignmentRuleId);
        return assignRoleInternal(tenantId, identityId, roleId, sourceType, assignmentRuleId, sourceKey,
                assignedByUserId, assignedByUserName);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public IdentityRoleAssignment assignRole(String tenantId,
                                             UUID identityId,
                                             UUID roleId,
                                             IdentityRoleAssignment.SourceType sourceType,
                                             UUID assignmentRuleId,
                                             String sourceKey,
                                             String assignedByUserId,
                                             String assignedByUserName) {
        return assignRoleInternal(tenantId, identityId, roleId, sourceType, assignmentRuleId, sourceKey,
                assignedByUserId, assignedByUserName);
    }

    private IdentityRoleAssignment assignRoleInternal(String tenantId,
                                                      UUID identityId,
                                                      UUID roleId,
                                                      IdentityRoleAssignment.SourceType sourceType,
                                                      UUID assignmentRuleId,
                                                      String sourceKey,
                                                      String assignedByUserId,
                                                      String assignedByUserName) {
        validateRequiredInputs(tenantId, identityId, roleId);
        IdentityRoleAssignment.SourceType effectiveSource = normalizeSourceType(sourceType);
        String effectiveSourceKey = normalizeSourceKey(effectiveSource, assignmentRuleId, sourceKey);
        validateTenantOwnership(tenantId, identityId, roleId, assignmentRuleId);

        Optional<IdentityRoleAssignment> existing = assignmentRepository
                .findByTenantIdAndIdentityIdAndRoleIdAndSourceTypeAndSourceKey(
                        tenantId, identityId, roleId, effectiveSource, effectiveSourceKey);

        if (existing.isPresent()) {
            IdentityRoleAssignment assignment = existing.get();
            assignment.setAssignmentRuleId(assignmentRuleId);
            if (assignment.getStatus() == IdentityRoleAssignment.Status.REVOKED) {
                assignment.reactivate();
                applyAssignedBy(assignment, assignedByUserId, assignedByUserName);
                IdentityRoleAssignment saved = assignmentRepository.save(assignment);
                roleEntitlementAssignmentService.applyRoleEntitlements(tenantId, identityId, roleId, assignmentRuleId, effectiveSource,
                        saved.getAssignedByUserId(), saved.getAssignedByUserName());
                logAssignmentEvent(AccessRoleAssignmentRuleConstants.LOG_ROLE_ASSIGNMENT_REACTIVATED,
                        tenantId, identityId, roleId, effectiveSource, effectiveSourceKey);
                return saved;
            }
            if (applyMissingAssignedBy(assignment, assignedByUserId, assignedByUserName)) {
                assignment = assignmentRepository.save(assignment);
            }
            roleEntitlementAssignmentService.applyRoleEntitlements(tenantId, identityId, roleId, assignmentRuleId, effectiveSource,
                    assignment.getAssignedByUserId(), assignment.getAssignedByUserName());
            logAssignmentEvent(AccessRoleAssignmentRuleConstants.LOG_ROLE_ASSIGNMENT_ACTIVE_REUSED,
                    tenantId, identityId, roleId, effectiveSource, effectiveSourceKey);
            return assignment;
        }

        IdentityRoleAssignment assignment = new IdentityRoleAssignment();
        assignment.setTenantId(tenantId);
        assignment.setIdentityId(identityId);
        assignment.setRoleId(roleId);
        assignment.setSourceType(effectiveSource);
        assignment.setAssignmentRuleId(assignmentRuleId);
        assignment.setSourceKey(effectiveSourceKey);
        assignment.setStatus(IdentityRoleAssignment.Status.ACTIVE);
        applyAssignedBy(assignment, assignedByUserId, assignedByUserName);

        IdentityRoleAssignment saved = assignmentRepository.save(assignment);
        roleEntitlementAssignmentService.applyRoleEntitlements(tenantId, identityId, roleId, assignmentRuleId, effectiveSource,
                saved.getAssignedByUserId(), saved.getAssignedByUserName());
        logAssignmentEvent(AccessRoleAssignmentRuleConstants.LOG_ROLE_ASSIGNMENT_CREATED,
                tenantId, identityId, roleId, effectiveSource, effectiveSourceKey);
        return saved;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Optional<IdentityRoleAssignment> revokeRole(String tenantId,
                                                       UUID identityId,
                                                       UUID roleId,
                                                       IdentityRoleAssignment.SourceType sourceType,
                                                       UUID assignmentRuleId) {
        String sourceKey = IdentityRoleAssignment.buildDefaultSourceKey(sourceType, assignmentRuleId);
        return revokeRoleInternal(tenantId, identityId, roleId, sourceType, assignmentRuleId, sourceKey);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public Optional<IdentityRoleAssignment> revokeRole(String tenantId,
                                                       UUID identityId,
                                                       UUID roleId,
                                                       IdentityRoleAssignment.SourceType sourceType,
                                                       UUID assignmentRuleId,
                                                       String sourceKey) {
        return revokeRoleInternal(tenantId, identityId, roleId, sourceType, assignmentRuleId, sourceKey);
    }

    private Optional<IdentityRoleAssignment> revokeRoleInternal(String tenantId,
                                                               UUID identityId,
                                                               UUID roleId,
                                                               IdentityRoleAssignment.SourceType sourceType,
                                                               UUID assignmentRuleId,
                                                               String sourceKey) {
        validateRequiredInputs(tenantId, identityId, roleId);
        IdentityRoleAssignment.SourceType effectiveSource = normalizeSourceType(sourceType);
        String effectiveSourceKey = normalizeSourceKey(effectiveSource, assignmentRuleId, sourceKey);

        Optional<IdentityRoleAssignment> existing = assignmentRepository
                .findByTenantIdAndIdentityIdAndRoleIdAndSourceTypeAndSourceKey(
                        tenantId, identityId, roleId, effectiveSource, effectiveSourceKey);

        if (existing.isEmpty()) {
            logger.debug()
                    .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ASSIGNMENT_REVOKE_SKIPPED)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SOURCE_TYPE, effectiveSource.name())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SOURCE_KEY, effectiveSourceKey)
                    .log();
            return Optional.empty();
        }

        IdentityRoleAssignment assignment = existing.get();
        if (assignment.getStatus() == IdentityRoleAssignment.Status.REVOKED) {
            return Optional.of(assignment);
        }

        assignment.revoke();
        IdentityRoleAssignment saved = assignmentRepository.save(assignment);
        if (hasAnotherActiveAssignmentForSameRole(tenantId, identityId, roleId, saved.getId())) {
            logger.info()
                    .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleConstants.LOG_ROLE_ENTITLEMENT_REVOKE_SKIPPED_ACTIVE_ASSIGNMENT)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SOURCE_TYPE, effectiveSource.name())
                    .detail(AccessRoleAssignmentRuleConstants.DETAIL_SOURCE_KEY, effectiveSourceKey)
                    .log();
        } else {
            roleEntitlementAssignmentService.revokeRoleEntitlements(tenantId, identityId, roleId, assignmentRuleId);
        }
        logAssignmentEvent(AccessRoleAssignmentRuleConstants.LOG_ROLE_ASSIGNMENT_REVOKED,
                tenantId, identityId, roleId, effectiveSource, effectiveSourceKey);
        return Optional.of(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<IdentityRoleAssignment> findActiveAssignmentsForIdentity(String tenantId, UUID identityId) {
        validateTenantId(tenantId);
        if (identityId == null) {
            throw new IllegalArgumentException("Identity ID is required");
        }
        return assignmentRepository.findByTenantIdAndIdentityIdAndStatus(
                tenantId, identityId, IdentityRoleAssignment.Status.ACTIVE);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<IdentityRoleAssignment> findActiveAssignmentsForRole(String tenantId, UUID roleId) {
        validateTenantId(tenantId);
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }
        return assignmentRepository.findByTenantIdAndRoleIdAndStatus(
                tenantId, roleId, IdentityRoleAssignment.Status.ACTIVE);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<IdentityRoleAssignment> findActiveAssignmentsByRule(String tenantId, UUID assignmentRuleId) {
        validateTenantId(tenantId);
        if (assignmentRuleId == null) {
            throw new IllegalArgumentException("Assignment rule ID is required");
        }
        return assignmentRepository.findByTenantIdAndAssignmentRuleIdAndStatus(
                tenantId, assignmentRuleId, IdentityRoleAssignment.Status.ACTIVE);
    }

    private void validateRequiredInputs(String tenantId, UUID identityId, UUID roleId) {
        validateTenantId(tenantId);
        if (identityId == null) {
            throw new IllegalArgumentException("Identity ID is required");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("Role ID is required");
        }
    }

    private void validateTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("Tenant ID is required");
        }
    }

    private void validateTenantOwnership(String tenantId, UUID identityId, UUID roleId, UUID assignmentRuleId) {
        identityRepository.findByTenantIdAndId(tenantId, identityId)
                .orElseThrow(() -> new IllegalArgumentException("Identity not found for tenant: " + identityId));

        AccessRole role = accessRoleRepository.findById(roleId)
                .filter(candidate -> tenantId.equals(candidate.getTenantId()))
                .orElseThrow(() -> new IllegalArgumentException("Access role not found for tenant: " + roleId));

        if (assignmentRuleId != null) {
            AccessRoleAssignmentRule rule = ruleRepository.findByIdAndTenantId(assignmentRuleId, tenantId)
                    .orElseThrow(() -> new IllegalArgumentException("Access role assignment rule not found for tenant: " + assignmentRuleId));
            if (!role.getId().equals(rule.getRoleId())) {
                throw new IllegalArgumentException("Assignment rule does not target role: " + roleId);
            }
        }
    }

    private boolean hasAnotherActiveAssignmentForSameRole(String tenantId,
                                                          UUID identityId,
                                                          UUID roleId,
                                                          UUID revokedAssignmentId) {
        return revokedAssignmentId != null
                && assignmentRepository.existsActiveAssignmentForSameRoleExcludingAssignment(
                        tenantId,
                        identityId,
                        roleId,
                        IdentityRoleAssignment.Status.ACTIVE,
                        revokedAssignmentId);
    }

    private IdentityRoleAssignment.SourceType normalizeSourceType(IdentityRoleAssignment.SourceType sourceType) {
        return sourceType != null ? sourceType : IdentityRoleAssignment.SourceType.MANUAL;
    }

    private String normalizeSourceKey(IdentityRoleAssignment.SourceType sourceType, UUID assignmentRuleId, String sourceKey) {
        if (sourceKey != null && !sourceKey.isBlank()) {
            return sourceKey.trim();
        }
        return IdentityRoleAssignment.buildDefaultSourceKey(sourceType, assignmentRuleId);
    }

    private void applyAssignedBy(IdentityRoleAssignment assignment, String assignedByUserId, String assignedByUserName) {
        String normalizedUserId = normalizeActorValue(assignedByUserId);
        assignment.setAssignedByUserId(normalizedUserId);
        assignment.setAssignedByUserName(Optional.ofNullable(normalizeActorValue(assignedByUserName)).orElse(normalizedUserId));
    }

    private boolean applyMissingAssignedBy(IdentityRoleAssignment assignment, String assignedByUserId, String assignedByUserName) {
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

    private void logAssignmentEvent(String message,
                                    String tenantId,
                                    UUID identityId,
                                    UUID roleId,
                                    IdentityRoleAssignment.SourceType sourceType,
                                    String sourceKey) {
        logger.info()
                .category(AccessRoleAssignmentRuleConstants.LOG_CATEGORY)
                .message(message)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_TENANT_ID, tenantId)
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_IDENTITY_ID, identityId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_ROLE_ID, roleId.toString())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SOURCE_TYPE, sourceType.name())
                .detail(AccessRoleAssignmentRuleConstants.DETAIL_SOURCE_KEY, sourceKey)
                .log();
    }
}

