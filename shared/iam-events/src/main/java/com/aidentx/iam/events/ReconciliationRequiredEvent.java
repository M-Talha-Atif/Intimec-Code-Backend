package com.aidentx.iam.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Published internally by the identity-service when an Identity's underlying
 * data shards change.
 * This event triggers the reconciliation engine to re-assemble the Golden
 * Record.
 */
public record ReconciliationRequiredEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("identityId") UUID identityId, // The Identity to re-evaluate
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("eventTimestamp") Instant eventTimestamp) {
    public ReconciliationRequiredEvent(UUID identityId, String tenantId) {
        this(UUID.randomUUID(), identityId, tenantId, Instant.now());
    }
}