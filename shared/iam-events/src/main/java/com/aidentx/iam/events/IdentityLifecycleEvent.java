package com.aidentx.iam.events;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

/**
 * An event published by the identity-service whenever a significant change
 * occurs
 * to an Identity's lifecycle. This event is the primary trigger for the
 * provisioning engine in the core-service to take action on managed accounts.
 */
public record IdentityLifecycleEvent(
        /**
         * A unique identifier for this specific event instance.
         */
        @JsonProperty("eventId") UUID eventId,

        /**
         * The UUID of the Identity that this event pertains to.
         */
        @JsonProperty("identityId") UUID identityId,

        /**
         * The tenant that the Identity belongs to.
         */
        @JsonProperty("tenantId") String tenantId,

        /**
         * The type of lifecycle change that occurred.
         */
        @JsonProperty("eventType") EventType eventType,

        /**
         * The timestamp of when the event was generated.
         */
        @JsonProperty("eventTimestamp") Instant eventTimestamp,

        /**
         * The name of the service that published the event.
         */
        @JsonProperty("eventSource") String eventSource) {
    /**
     * Enumerates the possible types of identity lifecycle events that can trigger
     * provisioning actions.
     */
    public enum EventType {
        /**
         * Signifies that a new Identity has been created, typically representing a new
         * hire.
         * This triggers "Joiner" or "birthright" provisioning workflows.
         */
        IDENTITY_CREATED,

        /**
         * Signifies that one or more attributes on the Identity's golden record have
         * changed.
         * This triggers "Mover" workflows to keep downstream accounts in sync.
         */
        IDENTITY_UPDATED,

        /**
         * Signifies that the Identity has been terminated (e.g., an employee has left).
         * This triggers "Leaver" workflows to disable or delete associated accounts.
         */
        IDENTITY_TERMINATED
    }

    /**
     * A convenience constructor to create a new event with a generated eventId and
     * source.
     * 
     * @param identityId The ID of the affected Identity.
     * @param tenantId   The tenant of the Identity.
     * @param eventType  The type of lifecycle event.
     */
    public IdentityLifecycleEvent(UUID identityId, String tenantId, EventType eventType) {
        this(
                UUID.randomUUID(),
                identityId,
                tenantId,
                eventType,
                Instant.now(),
                "identity-service");
    }
}