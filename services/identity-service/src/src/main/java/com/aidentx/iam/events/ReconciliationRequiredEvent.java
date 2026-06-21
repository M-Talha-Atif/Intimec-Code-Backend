package com.aidentx.iam.events;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An event published by the identity-service when new authoritative source data
 * has been staged for an identity, signaling that a reconciliation is required
 * to update the canonical "golden record".
 */
public record ReconciliationRequiredEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("identityId") UUID identityId,
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("eventTimestamp") Instant eventTimestamp,
        @JsonProperty("eventSource") String eventSource) {
    public ReconciliationRequiredEvent(UUID identityId, String tenantId) {
        this(UUID.randomUUID(), identityId, tenantId, Instant.now(), "identity-service");
    }
}