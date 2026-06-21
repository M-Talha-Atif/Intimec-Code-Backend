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
 * Immutable audit event for bulk manual access-role member grants.
 */
public record AccessRoleManualAssignmentAuditEvent(
        String actorId,
        String actorUsername,
        String tenantId,
        String action,
        String roleId,
        String roleName,
        String roleType,
        String riskLevel,
        Integer requestedCount,
        Integer addedCount,
        Integer skippedCount,
        Integer failedCount,
        List<MemberAuditDetail> addedMembers,
        List<MemberAuditDetail> skippedMembers,
        List<FailedMemberAuditDetail> failedMembers,
        JsonNode stateAfter,
        String description,
        Instant timestamp
) {

    public record MemberAuditDetail(String identityId, String identityName, String identityType) {
    }

    public record FailedMemberAuditDetail(String identityId, String identityName, String identityType, String reason) {
    }

    public static AccessRoleManualAssignmentAuditEvent summary(String actorId,
                                                               String actorUsername,
                                                               String tenantId,
                                                               String roleId,
                                                               String roleName,
                                                               String roleType,
                                                               String riskLevel,
                                                               Integer requestedCount,
                                                               Integer addedCount,
                                                               Integer skippedCount,
                                                               Integer failedCount,
                                                               List<MemberAuditDetail> addedMembers,
                                                               List<MemberAuditDetail> skippedMembers,
                                                               List<FailedMemberAuditDetail> failedMembers,
                                                               JsonNode stateAfter) {
        return new AccessRoleManualAssignmentAuditEvent(
                actorId,
                actorUsername,
                tenantId,
                AccessRoleManualAssignmentAuditConstants.ACTION_MEMBERS_MANUALLY_GRANTED,
                roleId,
                roleName,
                roleType,
                riskLevel,
                requestedCount,
                addedCount,
                skippedCount,
                failedCount,
                addedMembers,
                skippedMembers,
                failedMembers,
                stateAfter,
                String.format(
                        AccessRoleManualAssignmentAuditConstants.DESC_MEMBERS_MANUALLY_GRANTED_TEMPLATE,
                        roleName,
                        actorUsername,
                        addedCount != null ? addedCount : 0,
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
        log.setActivityLabel(AccessRoleManualAssignmentAuditConstants.toActivityLabel(roleType));
        log.setAction(action);
        log.setTriggerSource(AccessRoleManualAssignmentAuditConstants.TRIGGER_API);
        log.setDescription(description);
        log.setActorId(actorId);
        log.setActorUsername(actorUsername);
        log.setStateBefore(null);
        log.setStateAfter(stateAfter);
        log.setChangedFields(List.of());
        log.setAddedChildren(toMemberList(addedMembers));
        log.setRemovedChildren(null);
        log.setFailedChildren(toFailedMemberList(failedMembers));
        log.setAddedEntitlements(null);
        log.setRemovedEntitlements(null);
        return log;
    }

    private static List<Map<String, Object>> toMemberList(List<MemberAuditDetail> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(members.size());
        for (MemberAuditDetail member : members) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("identityId", member.identityId());
            row.put("identityName", member.identityName());
            row.put("identityType", member.identityType());
            result.add(row);
        }
        return result;
    }

    private static List<Map<String, Object>> toFailedMemberList(List<FailedMemberAuditDetail> members) {
        if (members == null || members.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>(members.size());
        for (FailedMemberAuditDetail member : members) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("identityId", member.identityId());
            row.put("identityName", member.identityName());
            row.put("identityType", member.identityType());
            row.put("reason", member.reason());
            result.add(row);
        }
        return result;
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

