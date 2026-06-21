package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.dto.response.ManualRoleMemberGrantResultDto;
import com.aidentx.iam.access_roles_management.model.dto.response.SyncManualMembersResultDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.service.AccessRoleAuditLogService;
import com.aidentx.iam.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Publishes and persists summary audit records for bulk manual role grants.
 */
@Service
public class AccessRoleManualAssignmentAuditService {

    private final AccessRoleManualAssignmentAuditPublisher publisher;
    private final AccessRoleAuditLogService auditLogService;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public AccessRoleManualAssignmentAuditService(AccessRoleManualAssignmentAuditPublisher publisher,
                                                  AccessRoleAuditLogService auditLogService,
                                                  ObjectMapper objectMapper,
                                                  UnifiedLoggerFactory loggerFactory) {
        this.publisher = publisher;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY);
    }

    public void publishManualGrantAudit(String tenantId,
                                        AccessRole role,
                                        ManualRoleMemberGrantResultDto result) {
        try {
            AccessRoleManualAssignmentAuditEvent event = AccessRoleManualAssignmentAuditEvent.summary(
                    resolveActorId(),
                    resolveActorUsername(),
                    tenantId,
                    role != null && role.getId() != null ? role.getId().toString() : null,
                    role != null ? role.getName() : null,
                    role != null ? role.getType() : null,
                    role != null ? role.getRiskLevel() : null,
                    result != null ? result.getRequestedCount() : 0,
                    result != null ? result.getAddedCount() : 0,
                    result != null ? result.getSkippedCount() : 0,
                    result != null ? result.getFailedCount() : 0,
                    toAddedMembers(result),
                    toSkippedMembers(result),
                    toFailedMembers(result),
                    buildStateAfter(role, result));
            publisher.publish(event);
            auditLogService.persist(event.toAuditLog());
        } catch (Exception ex) {
            logger.error()
                    .category(AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleManualAssignmentAuditConstants.LOG_MANUAL_GRANT_AUDIT_FAILED)
                    .details(Map.of(
                            AccessRoleManualAssignmentAuditConstants.DETAIL_TENANT_ID,
                            tenantId != null ? tenantId : AccessRoleManualAssignmentAuditConstants.NOT_AVAILABLE,
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_ID,
                            role != null && role.getId() != null
                                    ? role.getId().toString()
                                    : AccessRoleManualAssignmentAuditConstants.NOT_AVAILABLE,
                            AccessRoleManualAssignmentAuditConstants.DETAIL_EXCEPTION_TYPE,
                            ex.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    private ObjectNode buildStateAfter(AccessRole role, ManualRoleMemberGrantResultDto result) {
        ObjectNode state = objectMapper.createObjectNode();
        if (role != null && role.getId() != null) {
            state.put(AccessRoleManualAssignmentAuditConstants.STATE_ROLE_ID, role.getId().toString());
        }
        if (role != null && role.getName() != null) {
            state.put(AccessRoleManualAssignmentAuditConstants.STATE_ROLE_NAME, role.getName());
        }
        if (role != null && role.getType() != null) {
            state.put(AccessRoleManualAssignmentAuditConstants.STATE_ROLE_TYPE, role.getType());
        }
        if (role != null && role.getRiskLevel() != null) {
            state.put(AccessRoleManualAssignmentAuditConstants.STATE_RISK_LEVEL, role.getRiskLevel());
        }
        state.put(AccessRoleManualAssignmentAuditConstants.STATE_REQUESTED_COUNT, result != null ? result.getRequestedCount() : 0);
        state.put(AccessRoleManualAssignmentAuditConstants.STATE_ADDED_COUNT, result != null ? result.getAddedCount() : 0);
        state.put(AccessRoleManualAssignmentAuditConstants.STATE_SKIPPED_COUNT, result != null ? result.getSkippedCount() : 0);
        state.put(AccessRoleManualAssignmentAuditConstants.STATE_FAILED_COUNT, result != null ? result.getFailedCount() : 0);
        addGrantedMembers(state, AccessRoleManualAssignmentAuditConstants.STATE_ADDED_IDENTITIES,
                result != null ? result.getAdded() : List.of());
        addGrantedMembers(state, AccessRoleManualAssignmentAuditConstants.STATE_SKIPPED_IDENTITIES,
                result != null ? result.getSkipped() : List.of());
        addFailedMembers(state, AccessRoleManualAssignmentAuditConstants.STATE_FAILED_IDENTITIES,
                result != null ? result.getFailed() : List.of());
        return state;
    }

    private List<AccessRoleManualAssignmentAuditEvent.MemberAuditDetail> toAddedMembers(ManualRoleMemberGrantResultDto result) {
        if (result == null || result.getAdded() == null) {
            return List.of();
        }
        return result.getAdded().stream()
                .map(member -> new AccessRoleManualAssignmentAuditEvent.MemberAuditDetail(
                        member.getIdentityId() != null ? member.getIdentityId().toString() : null,
                        member.getIdentityName(),
                        member.getIdentityType()))
                .toList();
    }

    private List<AccessRoleManualAssignmentAuditEvent.MemberAuditDetail> toSkippedMembers(ManualRoleMemberGrantResultDto result) {
        if (result == null || result.getSkipped() == null) {
            return List.of();
        }
        return result.getSkipped().stream()
                .map(member -> new AccessRoleManualAssignmentAuditEvent.MemberAuditDetail(
                        member.getIdentityId() != null ? member.getIdentityId().toString() : null,
                        member.getIdentityName(),
                        member.getIdentityType()))
                .toList();
    }

    private List<AccessRoleManualAssignmentAuditEvent.FailedMemberAuditDetail> toFailedMembers(ManualRoleMemberGrantResultDto result) {
        if (result == null || result.getFailed() == null) {
            return List.of();
        }
        return result.getFailed().stream()
                .map(member -> new AccessRoleManualAssignmentAuditEvent.FailedMemberAuditDetail(
                        member.getIdentityId() != null ? member.getIdentityId().toString() : null,
                        member.getIdentityName(),
                        member.getIdentityType(),
                        member.getReason()))
                .toList();
    }

    private void addGrantedMembers(ObjectNode state,
                                   String fieldName,
                                   List<ManualRoleMemberGrantResultDto.GrantedIdentity> members) {
        ArrayNode arrayNode = state.putArray(fieldName);
        for (ManualRoleMemberGrantResultDto.GrantedIdentity member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            if (member.getIdentityId() != null) {
                node.put("identityId", member.getIdentityId().toString());
            }
            if (member.getIdentityName() != null) {
                node.put("identityName", member.getIdentityName());
            }
            if (member.getIdentityType() != null) {
                node.put("identityType", member.getIdentityType());
            }
            arrayNode.add(node);
        }
    }

    private void addFailedMembers(ObjectNode state,
                                  String fieldName,
                                  List<ManualRoleMemberGrantResultDto.FailedIdentity> members) {
        ArrayNode arrayNode = state.putArray(fieldName);
        for (ManualRoleMemberGrantResultDto.FailedIdentity member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            if (member.getIdentityId() != null) {
                node.put("identityId", member.getIdentityId().toString());
            }
            if (member.getIdentityName() != null) {
                node.put("identityName", member.getIdentityName());
            }
            if (member.getIdentityType() != null) {
                node.put("identityType", member.getIdentityType());
            }
            if (member.getReason() != null) {
                node.put("reason", member.getReason());
            }
            arrayNode.add(node);
        }
    }

    private String resolveActorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return AppConstants.AUDIT_ACTOR_SYSTEM_ID;
    }

    private String resolveActorUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return AppConstants.AUDIT_ACTOR_SYSTEM_USERNAME;
    }

    // ===================================================================
    //  Manual Sync (PUT) Audit
    // ===================================================================

    /**
     * Publishes a rich audit event for the manual member sync operation.
     *
     * <p>The event captures, per identity, which entitlements were actually
     * provisioned (already present in {@code entitlement_assignments}), which
     * are pending provisioning (resolved for the role but not yet present in
     * the assignments table), and which were deprovisioned on revoke.</p>
     */
    public void publishManualSyncAudit(String tenantId,
                                       AccessRole role,
                                       SyncManualMembersResultDto result) {
        try {
            AccessRoleManualSyncAuditEvent event = AccessRoleManualSyncAuditEvent.summary(
                    resolveActorId(),
                    resolveActorUsername(),
                    tenantId,
                    role != null && role.getId() != null ? role.getId().toString() : null,
                    role != null ? role.getName() : null,
                    role != null ? role.getType() : null,
                    role != null ? role.getRiskLevel() : null,
                    result != null ? result.getAddedCount() : 0,
                    result != null ? result.getRevokedCount() : 0,
                    result != null ? result.getSkippedCount() : 0,
                    result != null ? result.getFailedCount() : 0,
                    result != null ? result.getTotalActiveCount() : 0,
                    toSyncMembers(result != null ? result.getAdded() : List.of(), false),
                    toSyncMembers(result != null ? result.getRevoked() : List.of(), true),
                    toSyncMembers(result != null ? result.getSkipped() : List.of(), false),
                    toFailedSyncMembers(result != null ? result.getFailed() : List.of()),
                    buildSyncStateAfter(role, result));
            publisher.publish(event);
            auditLogService.persist(event.toAuditLog());
        } catch (Exception ex) {
            logger.error()
                    .category(AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleManualAssignmentAuditConstants.LOG_MANUAL_SYNC_AUDIT_FAILED)
                    .details(Map.of(
                            AccessRoleManualAssignmentAuditConstants.DETAIL_TENANT_ID,
                            tenantId != null ? tenantId : AccessRoleManualAssignmentAuditConstants.NOT_AVAILABLE,
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_ID,
                            role != null && role.getId() != null
                                    ? role.getId().toString()
                                    : AccessRoleManualAssignmentAuditConstants.NOT_AVAILABLE,
                            AccessRoleManualAssignmentAuditConstants.DETAIL_EXCEPTION_TYPE,
                            ex.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    private List<AccessRoleManualSyncAuditEvent.SyncMemberAuditDetail> toSyncMembers(
            List<SyncManualMembersResultDto.MemberSyncEntry> entries,
            boolean isRevoke) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .map(entry -> new AccessRoleManualSyncAuditEvent.SyncMemberAuditDetail(
                        entry.getIdentityId() != null ? entry.getIdentityId().toString() : null,
                        entry.getIdentityName(),
                        entry.getIdentityType(),
                        isRevoke ? List.of() : toEntitlementDetails(entry.getEntitlementImpact(), true),
                        isRevoke ? List.of() : toEntitlementDetails(entry.getEntitlementImpact(), false),
                        isRevoke ? toEntitlementDetails(entry.getEntitlementImpact(), null) : List.of()))
                .toList();
    }

    /**
     * @param onlyProvisioned {@code true} = only entitlements already present in the
     *                        assignments table (provisioned); {@code false} = only those
     *                        not yet present (pending); {@code null} = all (used for revoke
     *                        where every impacted entitlement is being deprovisioned).
     */
    private List<AccessRoleManualSyncAuditEvent.EntitlementAuditDetail> toEntitlementDetails(
            List<SyncManualMembersResultDto.EntitlementImpact> impacts,
            Boolean onlyProvisioned) {
        if (impacts == null || impacts.isEmpty()) {
            return List.of();
        }
        return impacts.stream()
                .filter(impact -> onlyProvisioned == null || impact.isExistedInAssignmentTable() == onlyProvisioned)
                .map(impact -> new AccessRoleManualSyncAuditEvent.EntitlementAuditDetail(
                        impact.getEntitlementId() != null ? impact.getEntitlementId().toString() : null,
                        impact.getEntitlementName(),
                        impact.getApplicationId() != null ? impact.getApplicationId().toString() : null,
                        impact.getApplicationName()))
                .toList();
    }

    private List<AccessRoleManualSyncAuditEvent.FailedSyncMemberAuditDetail> toFailedSyncMembers(
            List<SyncManualMembersResultDto.FailedMember> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .map(entry -> new AccessRoleManualSyncAuditEvent.FailedSyncMemberAuditDetail(
                        entry.getIdentityId() != null ? entry.getIdentityId().toString() : null,
                        entry.getIdentityName(),
                        entry.getIdentityType(),
                        entry.getReason()))
                .toList();
    }

    private ObjectNode buildSyncStateAfter(AccessRole role, SyncManualMembersResultDto result) {
        ObjectNode state = objectMapper.createObjectNode();
        if (role != null && role.getId() != null) {
            state.put(AccessRoleManualAssignmentAuditConstants.STATE_ROLE_ID, role.getId().toString());
        }
        if (role != null && role.getName() != null) {
            state.put(AccessRoleManualAssignmentAuditConstants.STATE_ROLE_NAME, role.getName());
        }
        if (role != null && role.getType() != null) {
            state.put(AccessRoleManualAssignmentAuditConstants.STATE_ROLE_TYPE, role.getType());
        }
        if (role != null && role.getRiskLevel() != null) {
            state.put(AccessRoleManualAssignmentAuditConstants.STATE_RISK_LEVEL, role.getRiskLevel());
        }
        state.put(AccessRoleManualAssignmentAuditConstants.STATE_ADDED_COUNT, result != null ? result.getAddedCount() : 0);
        state.put(AccessRoleManualAssignmentAuditConstants.STATE_REVOKED_COUNT, result != null ? result.getRevokedCount() : 0);
        state.put(AccessRoleManualAssignmentAuditConstants.STATE_SKIPPED_COUNT, result != null ? result.getSkippedCount() : 0);
        state.put(AccessRoleManualAssignmentAuditConstants.STATE_FAILED_COUNT, result != null ? result.getFailedCount() : 0);
        state.put(AccessRoleManualAssignmentAuditConstants.STATE_TOTAL_ACTIVE_COUNT,
                result != null ? result.getTotalActiveCount() : 0);
        addSyncMembersToState(state, AccessRoleManualAssignmentAuditConstants.STATE_ADDED_IDENTITIES,
                result != null ? result.getAdded() : List.of(), false);
        addSyncMembersToState(state, AccessRoleManualAssignmentAuditConstants.STATE_REVOKED_IDENTITIES,
                result != null ? result.getRevoked() : List.of(), true);
        addSyncMembersToState(state, AccessRoleManualAssignmentAuditConstants.STATE_SKIPPED_IDENTITIES,
                result != null ? result.getSkipped() : List.of(), false);
        addFailedSyncMembersToState(state, AccessRoleManualAssignmentAuditConstants.STATE_FAILED_IDENTITIES,
                result != null ? result.getFailed() : List.of());
        return state;
    }

    private void addSyncMembersToState(ObjectNode state, String fieldName,
                                       List<SyncManualMembersResultDto.MemberSyncEntry> members,
                                       boolean isRevoke) {
        ArrayNode arrayNode = state.putArray(fieldName);
        for (SyncManualMembersResultDto.MemberSyncEntry member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            if (member.getIdentityId() != null) {
                node.put("identityId", member.getIdentityId().toString());
            }
            if (member.getIdentityName() != null) {
                node.put("identityName", member.getIdentityName());
            }
            if (member.getIdentityType() != null) {
                node.put("identityType", member.getIdentityType());
            }
            addImpactsToNode(node,
                    AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_PROVISIONED,
                    member.getEntitlementImpact(), isRevoke ? null : Boolean.TRUE, isRevoke);
            addImpactsToNode(node,
                    AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_PENDING,
                    member.getEntitlementImpact(), isRevoke ? null : Boolean.FALSE, isRevoke);
            if (isRevoke) {
                addImpactsToNode(node,
                        AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_DEPROVISIONED,
                        member.getEntitlementImpact(), null, true);
            }
            arrayNode.add(node);
        }
    }

    private void addImpactsToNode(ObjectNode node, String fieldName,
                                  List<SyncManualMembersResultDto.EntitlementImpact> impacts,
                                  Boolean provisionedFilter,
                                  boolean isRevoke) {
        if (impacts == null || impacts.isEmpty()) {
            return;
        }
        // For non-revoke flows we render provisioned/pending arrays only
        // (deprovisioned array is skipped). For revoke flows we render only
        // the deprovisioned array (provisioned/pending arrays are skipped).
        if (!isRevoke && (fieldName.equals(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_DEPROVISIONED))) {
            return;
        }
        if (isRevoke && !fieldName.equals(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_DEPROVISIONED)) {
            return;
        }
        ArrayNode arrayNode = node.putArray(fieldName);
        for (SyncManualMembersResultDto.EntitlementImpact impact : impacts) {
            if (provisionedFilter != null && impact.isExistedInAssignmentTable() != provisionedFilter) {
                continue;
            }
            ObjectNode entNode = objectMapper.createObjectNode();
            if (impact.getEntitlementId() != null) {
                entNode.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_ID,
                        impact.getEntitlementId().toString());
            }
            if (impact.getEntitlementName() != null) {
                entNode.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_NAME, impact.getEntitlementName());
            }
            if (impact.getApplicationId() != null) {
                entNode.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_APPLICATION_ID,
                        impact.getApplicationId().toString());
            }
            if (impact.getApplicationName() != null) {
                entNode.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_APPLICATION_NAME,
                        impact.getApplicationName());
            }
            arrayNode.add(entNode);
        }
    }

    private void addFailedSyncMembersToState(ObjectNode state, String fieldName,
                                             List<SyncManualMembersResultDto.FailedMember> members) {
        ArrayNode arrayNode = state.putArray(fieldName);
        for (SyncManualMembersResultDto.FailedMember member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            if (member.getIdentityId() != null) {
                node.put("identityId", member.getIdentityId().toString());
            }
            if (member.getIdentityName() != null) {
                node.put("identityName", member.getIdentityName());
            }
            if (member.getIdentityType() != null) {
                node.put("identityType", member.getIdentityType());
            }
            if (member.getReason() != null) {
                node.put("reason", member.getReason());
            }
            arrayNode.add(node);
        }
    }
}
