package com.aidentx.iam.model.dto.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * A generic event DTO for commands that modify relationships between objects in the identity graph.
 * This is the contract for the 'identity-relationship-events' Kafka topic.
 */
public record RelationshipChangeEvent(
    @NotNull UUID subjectId,         // The UUID of the source of the relationship (e.g., the Identity).
    @NotNull UUID objectId,          // The UUID of the target of the relationship (e.g., the Role).
    @NotBlank String relationshipType, // The name of the DGraph predicate (e.g., "HAS_ROLE").
    @NotNull Action action            // The operation to perform (CREATE or DELETE).
) {
    /**
     * Defines the action to be taken on the relationship.
     */
    public enum Action {
        CREATE,
        DELETE
    }
}