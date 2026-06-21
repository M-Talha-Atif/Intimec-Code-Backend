package com.aidentx.iam.entitlement_management.audit;

import com.aidentx.iam.config.AppConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EntitlementGovernanceAuditEvent(
        String actorId,
        String actorUsername,
        String action,
        String entityType,
        String entityId,
        String entityName,
        String tenantId,
        String resourceName,
        String lifecycle,
        String riskLevel,
        String kind,
        String riskReason,
        Boolean requestable,
        String privilegeLevel,
        String ownerId,
        Boolean requireApproval,
        Boolean allowedAutomatedGrant,
        Long maxDurationMs,
        Long autoExpireDurationMs,
        String identityType,
        JsonNode stateBefore,
        JsonNode stateAfter,
        Instant timestamp
) {

    // ===================================================================
    //  Factory Method Governance Update
    // ===================================================================

    public static EntitlementGovernanceAuditEvent governanceUpdated(
            String actorId, String actorUsername, String tenantId,
            String entityId, String entityName,
            String resourceName,
            JsonNode stateBefore, JsonNode stateAfter) {
        return new EntitlementGovernanceAuditEvent(
                actorId, actorUsername,
                AppConstants.AUDIT_EVENT_ENTITLEMENT_GOVERNANCE_UPDATED,
                AppConstants.AUDIT_ENTITY_TYPE_ENTITLEMENT,
                entityId, entityName,
                tenantId, resourceName,
                null, null, null, null, null, null, null, null, null, null, null, null,
                stateBefore, stateAfter,
                Instant.now()
        );
    }

    // ===================================================================
    //  Factory Method Governance Update (full context)
    // ===================================================================

    public static EntitlementGovernanceAuditEvent governanceUpdatedFull(
            String actorId, String actorUsername, String tenantId,
            String entityId, String entityName,
            String resourceName,
            String lifecycle, String riskLevel, String kind,
            String riskReason, Boolean requestable,
            String privilegeLevel, String ownerId,
            Boolean requireApproval, Boolean allowedAutomatedGrant,
            Long maxDurationMs, Long autoExpireDurationMs,
            String identityType,
            JsonNode stateBefore, JsonNode stateAfter) {
        return new EntitlementGovernanceAuditEvent(
                actorId, actorUsername,
                AppConstants.AUDIT_EVENT_ENTITLEMENT_GOVERNANCE_UPDATED,
                AppConstants.AUDIT_ENTITY_TYPE_ENTITLEMENT,
                entityId, entityName,
                tenantId, resourceName,
                lifecycle, riskLevel, kind, riskReason, requestable,
                privilegeLevel, ownerId,
                requireApproval, allowedAutomatedGrant,
                maxDurationMs, autoExpireDurationMs,
                identityType,
                stateBefore, stateAfter,
                Instant.now()
        );
    }
}