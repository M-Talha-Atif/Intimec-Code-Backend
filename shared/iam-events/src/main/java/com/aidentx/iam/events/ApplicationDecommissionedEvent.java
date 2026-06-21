package com.aidentx.iam.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by the core-service when an application is decommissioned.
 * This event triggers a cleanup of all related governance policies in the
 * identity-service.
 */
public record ApplicationDecommissionedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("resourceId") UUID resourceId, // The immutable ID of the deleted Application
        @JsonProperty("eventTimestamp") Instant eventTimestamp) {
    public ApplicationDecommissionedEvent(String tenantId, UUID resourceId) {
        this(UUID.randomUUID(), tenantId, resourceId, Instant.now());
    }
}