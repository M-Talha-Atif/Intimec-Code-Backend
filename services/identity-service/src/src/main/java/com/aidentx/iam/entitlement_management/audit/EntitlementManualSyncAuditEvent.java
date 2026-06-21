package com.aidentx.iam.entitlement_management.audit;

import com.aidentx.iam.entitlement_management.config.constants.EntitlementAuditConstants;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAuditLog;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Audit event for desired-state manual entitlement member sync.
 *
 * <p>Captures exactly which identities were added, removed, skipped, or failed
 * for an entitlement sync operation. The event can be persisted to the local
 * entitlement audit log and published to the platform audit Kafka topic.</p>
 */
public record EntitlementManualSyncAuditEvent(
        String actorId,
        String actorUsername,
        String tenantId,
        String action,
        String entitlementId,
        String entitlementName,
        String resourceId,
        String riskLevel,
        Integer addedCount,
        Integer removedCount,
        Integer skippedCount,
        Integer failedCount,
        Integer totalDirectCount,
        List<MemberAuditDetail> addedMembers,
        List<MemberAuditDetail> removedMembers,
        List<MemberAuditDetail> skippedMembers,
        List<FailedMemberAuditDetail> failedMembers,
        JsonNode stateAfter,
        String description,
        Instant timestamp
) {

    /** One identity affected by the entitlement member sync. */
    public record MemberAuditDetail(
            String identityId,
            String identityName,
            String identityType,
            List<AssignmentAuditDetail> assignments
    ) {
    }

    /** One identity that could not be processed. */
    public record FailedMemberAuditDetail(
            String identityId,
            String identityName,
            String identityType,
            String reason
    ) {
    }

    /** One entitlement assignment row affected by the sync. */
    public record AssignmentAuditDetail(
            String assignmentId,
            String shadowId,
            String status,
            String action
    ) {
    }

    public static EntitlementManualSyncAuditEvent summary(String actorId,
                                                          String actorUsername,
                                                          String tenantId,
                                                          String entitlementId,
                                                          String entitlementName,
                                                          String resourceId,
                                                          String riskLevel,
                                                          Integer addedCount,
                                                          Integer removedCount,
                                                          Integer skippedCount,
                                                          Integer failedCount,
                                                          Integer totalDirectCount,
                                                          List<MemberAuditDetail> addedMembers,
                                                          List<MemberAuditDetail> removedMembers,
                                                          List<MemberAuditDetail> skippedMembers,
                                                          List<FailedMemberAuditDetail> failedMembers,
                                                          JsonNode stateAfter) {
        return new EntitlementManualSyncAuditEvent(
                actorId,
                actorUsername,
                tenantId,
                EntitlementAuditConstants.ACTION_MEMBERS_MANUALLY_SYNCED,
                entitlementId,
                entitlementName,
                resourceId,
                riskLevel,
                addedCount,
                removedCount,
                skippedCount,
                failedCount,
                totalDirectCount,
                addedMembers,
                removedMembers,
                skippedMembers,
                failedMembers,
                stateAfter,
                String.format(
                        EntitlementAuditConstants.DESC_MEMBERS_MANUALLY_SYNCED_TEMPLATE,
                        entitlementName,
                        actorUsername,
                        addedCount != null ? addedCount : 0,
                        removedCount != null ? removedCount : 0,
                        skippedCount != null ? skippedCount : 0,
                        failedCount != null ? failedCount : 0),
                Instant.now());
    }

    public EntitlementAuditLog toAuditLog() {
        EntitlementAuditLog log = new EntitlementAuditLog();
        log.setTenantId(tenantId);
        log.setEntitlementId(parseUuid(entitlementId));
        log.setEntitlementName(entitlementName);
        log.setResourceId(parseUuid(resourceId));
        log.setAction(action);
        log.setTriggerSource(EntitlementAuditConstants.TRIGGER_API);
        log.setDescription(description);
        log.setActorId(actorId);
        log.setActorUsername(actorUsername);
        log.setStateBefore(null);
        log.setStateAfter(stateAfter);
        log.setChangedFields(List.of());
        log.setAssignmentsCreated(addedCount);
        log.setAssignmentsRevoked(removedCount);
        return log;
    }

    private static UUID parseUuid(String value) {
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

