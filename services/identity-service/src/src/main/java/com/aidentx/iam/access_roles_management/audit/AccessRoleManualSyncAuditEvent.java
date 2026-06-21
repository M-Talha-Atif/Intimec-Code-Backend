package com.aidentx.iam.access_roles_management.audit;

import com.aidentx.iam.access_roles_management.model.entity.AccessRoleAuditLog;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable audit event for the desired-state manual member sync (PUT) operation.
 *
 * <p>Carries per-identity entitlement breakdown so downstream consumers see:</p>
 * <ul>
 *     <li>Which identities received the role</li>
 *     <li>Which entitlements were actually provisioned (already present in
 *         the {@code entitlement_assignments} table)</li>
 *     <li>Which entitlements are pending provisioning (resolved for the role
 *         but not yet present in the assignments table)</li>
 *     <li>Which entitlements were deprovisioned on revoke</li>
 * </ul>
 */
public record AccessRoleManualSyncAuditEvent(
        String actorId,
        String actorUsername,
        String tenantId,
        String action,
        String roleId,
        String roleName,
        String roleType,
        String riskLevel,
        Integer addedCount,
        Integer revokedCount,
        Integer skippedCount,
        Integer failedCount,
        Integer totalActiveCount,
        List<SyncMemberAuditDetail> addedMembers,
        List<SyncMemberAuditDetail> revokedMembers,
        List<SyncMemberAuditDetail> skippedMembers,
        List<FailedSyncMemberAuditDetail> failedMembers,
        JsonNode stateAfter,
        String description,
        Instant timestamp
) {

    /**
     * One identity affected by the sync, with the entitlement breakdown that resulted.
     */
    public record SyncMemberAuditDetail(
            String identityId,
            String identityName,
            String identityType,
            List<EntitlementAuditDetail> entitlementsProvisioned,
            List<EntitlementAuditDetail> entitlementsPending,
            List<EntitlementAuditDetail> entitlementsDeprovisioned
    ) {
    }

    public record FailedSyncMemberAuditDetail(
            String identityId,
            String identityName,
            String identityType,
            String reason
    ) {
    }

    public record EntitlementAuditDetail(
            String entitlementId,
            String entitlementName,
            String applicationId,
            String applicationName
    ) {
    }

    public static AccessRoleManualSyncAuditEvent summary(String actorId,
                                                         String actorUsername,
                                                         String tenantId,
                                                         String roleId,
                                                         String roleName,
                                                         String roleType,
                                                         String riskLevel,
                                                         Integer addedCount,
                                                         Integer revokedCount,
                                                         Integer skippedCount,
                                                         Integer failedCount,
                                                         Integer totalActiveCount,
                                                         List<SyncMemberAuditDetail> addedMembers,
                                                         List<SyncMemberAuditDetail> revokedMembers,
                                                         List<SyncMemberAuditDetail> skippedMembers,
                                                         List<FailedSyncMemberAuditDetail> failedMembers,
                                                         JsonNode stateAfter) {
        return new AccessRoleManualSyncAuditEvent(
                actorId,
                actorUsername,
                tenantId,
                AccessRoleManualAssignmentAuditConstants.ACTION_MEMBERS_MANUALLY_SYNCED,
                roleId,
                roleName,
                roleType,
                riskLevel,
                addedCount,
                revokedCount,
                skippedCount,
                failedCount,
                totalActiveCount,
                addedMembers,
                revokedMembers,
                skippedMembers,
                failedMembers,
                stateAfter,
                String.format(
                        AccessRoleManualAssignmentAuditConstants.DESC_MEMBERS_MANUALLY_SYNCED_TEMPLATE,
                        roleName,
                        actorUsername,
                        addedCount != null ? addedCount : 0,
                        revokedCount != null ? revokedCount : 0,
                        skippedCount != null ? skippedCount : 0,
                        failedCount != null ? failedCount : 0),
                Instant.now()
        );
    }

    public AccessRoleAuditLog toAuditLog() {
        AccessRoleAuditLog log = new AccessRoleAuditLog();
        log.setTenantId(tenantId);
        log.setRoleId(parseUuid(roleId));
        log.setRoleName(roleName);
        log.setRoleType(roleType);
        log.setEntityType(AccessRoleManualAssignmentAuditConstants.toEntityType(roleType));
        log.setActivityLabel(AccessRoleManualAssignmentAuditConstants.toSyncActivityLabel(roleType));
        log.setAction(action);
        log.setTriggerSource(AccessRoleManualAssignmentAuditConstants.TRIGGER_API);
        log.setDescription(description);
        log.setActorId(actorId);
        log.setActorUsername(actorUsername);
        log.setStateBefore(null);
        log.setStateAfter(stateAfter);
        log.setChangedFields(List.of());
        log.setAddedChildren(toAddedMemberRows(addedMembers));
        log.setRemovedChildren(toRevokedMemberRows(revokedMembers));
        log.setFailedChildren(toFailedMemberRows(failedMembers));
        log.setAddedEntitlements(toFlatEntitlementRows(addedMembers, false));
        log.setRemovedEntitlements(toFlatEntitlementRows(revokedMembers, true));
        return log;
    }

    private static List<Map<String, Object>> toAddedMemberRows(List<SyncMemberAuditDetail> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(members.size());
        for (SyncMemberAuditDetail member : members) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("identityId", member.identityId());
            row.put("identityName", member.identityName());
            row.put("identityType", member.identityType());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_PROVISIONED,
                    toEntitlementRows(member.entitlementsProvisioned(),
                            AccessRoleManualAssignmentAuditConstants.ENTITLEMENT_ACTION_PROVISIONED));
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_PENDING,
                    toEntitlementRows(member.entitlementsPending(),
                            AccessRoleManualAssignmentAuditConstants.ENTITLEMENT_ACTION_PENDING));
            result.add(row);
        }
        return result;
    }

    private static List<Map<String, Object>> toRevokedMemberRows(List<SyncMemberAuditDetail> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(members.size());
        for (SyncMemberAuditDetail member : members) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("identityId", member.identityId());
            row.put("identityName", member.identityName());
            row.put("identityType", member.identityType());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_DEPROVISIONED,
                    toEntitlementRows(member.entitlementsDeprovisioned(),
                            AccessRoleManualAssignmentAuditConstants.ENTITLEMENT_ACTION_DEPROVISIONED));
            result.add(row);
        }
        return result;
    }

    private static List<Map<String, Object>> toFailedMemberRows(List<FailedSyncMemberAuditDetail> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(members.size());
        for (FailedSyncMemberAuditDetail member : members) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("identityId", member.identityId());
            row.put("identityName", member.identityName());
            row.put("identityType", member.identityType());
            row.put("reason", member.reason());
            result.add(row);
        }
        return result;
    }

    private static List<Map<String, Object>> toEntitlementRows(List<EntitlementAuditDetail> entitlements,
                                                               String action) {
        if (entitlements == null || entitlements.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(entitlements.size());
        for (EntitlementAuditDetail ent : entitlements) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_ID, ent.entitlementId());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_NAME, ent.entitlementName());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_APPLICATION_ID, ent.applicationId());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_APPLICATION_NAME, ent.applicationName());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_ACTION, action);
            result.add(row);
        }
        return result;
    }

    /**
     * Flattens per-member entitlement breakdown into a single list with the
     * owning identity context attached to every entitlement row. This drives
     * the {@code added_entitlements} / {@code removed_entitlements} jsonb
     * columns consumed by the audit history UI.
     */
    private static List<Map<String, Object>> toFlatEntitlementRows(List<SyncMemberAuditDetail> members,
                                                                   boolean isRevoke) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (SyncMemberAuditDetail member : members) {
            if (isRevoke) {
                appendFlatEntitlements(result, member, member.entitlementsDeprovisioned(),
                        AccessRoleManualAssignmentAuditConstants.ENTITLEMENT_ACTION_DEPROVISIONED);
            } else {
                appendFlatEntitlements(result, member, member.entitlementsProvisioned(),
                        AccessRoleManualAssignmentAuditConstants.ENTITLEMENT_ACTION_PROVISIONED);
                appendFlatEntitlements(result, member, member.entitlementsPending(),
                        AccessRoleManualAssignmentAuditConstants.ENTITLEMENT_ACTION_PENDING);
            }
        }
        return result;
    }

    private static void appendFlatEntitlements(List<Map<String, Object>> sink,
                                               SyncMemberAuditDetail member,
                                               List<EntitlementAuditDetail> entitlements,
                                               String action) {
        if (entitlements == null || entitlements.isEmpty()) {
            return;
        }
        for (EntitlementAuditDetail ent : entitlements) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("identityId", member.identityId());
            row.put("identityName", member.identityName());
            row.put("identityType", member.identityType());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_ID, ent.entitlementId());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_NAME, ent.entitlementName());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_APPLICATION_ID, ent.applicationId());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_APPLICATION_NAME, ent.applicationName());
            row.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_ACTION, action);
            sink.add(row);
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}



