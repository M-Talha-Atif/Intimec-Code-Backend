package com.aidentx.iam.access_roles_management.audit;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/** Immutable audit event for a completed access-role import job. */
public record RoleImportJobAuditEvent(
        String actorId,
        String actorUsername,
        String tenantId,
        String jobId,
        String status,
        String riskLevel,
        JsonNode summary,
        Instant timestamp
) {

    public static RoleImportJobAuditEvent finished(
            String actorId,
            String actorUsername,
            String tenantId,
            String jobId,
            String status,
            String riskLevel,
            JsonNode summary) {
        String effectiveActor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
        String effectiveUsername = actorUsername == null || actorUsername.isBlank() ? effectiveActor : actorUsername;
        return new RoleImportJobAuditEvent(
                effectiveActor,
                effectiveUsername,
                tenantId,
                jobId,
                status,
                riskLevel,
                summary,
                Instant.now()
        );
    }
}
