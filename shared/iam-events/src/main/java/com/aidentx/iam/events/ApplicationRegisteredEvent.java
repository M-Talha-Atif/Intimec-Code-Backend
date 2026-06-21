package com.aidentx.iam.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by the core-service when a new application is successfully
 * onboarded.
 * This event notifies the identity-service that a new data source is available
 * for
 * governance.
 */
public record ApplicationRegisteredEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("tenantId") String tenantId,
        @JsonProperty("resourceId") UUID resourceId, // The immutable ID of the Application entity
        @JsonProperty("sourceName") String sourceName, // The human-readable name for display/logging

        // This is the UUID of the IdentitySchema this source is primarily associated
        // with.
        // It can be null for non-authoritative sources that aren't tied to a specific
        // context yet.
        @JsonProperty("associatedSchemaId") UUID associatedSchemaId,

        @JsonProperty("eventTimestamp") Instant eventTimestamp) {
    public ApplicationRegisteredEvent(String tenantId, UUID resourceId, String sourceName, UUID associatedSchemaId) {
        this(UUID.randomUUID(), tenantId, resourceId, sourceName, associatedSchemaId, Instant.now());
    }
}