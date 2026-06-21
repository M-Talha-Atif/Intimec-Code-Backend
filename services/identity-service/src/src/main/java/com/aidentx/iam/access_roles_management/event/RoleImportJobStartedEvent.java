package com.aidentx.iam.access_roles_management.event;

import java.util.UUID;

/** Kafka event that asks identity-service to process a persisted import job. */
public record RoleImportJobStartedEvent(UUID jobId, String tenantId, String actorId) {
}

