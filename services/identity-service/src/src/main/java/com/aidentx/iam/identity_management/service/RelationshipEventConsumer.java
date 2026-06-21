package com.aidentx.iam.identity_management.service;

import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.model.dto.events.RelationshipChangeEvent;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.shared.dgraph.DgraphService;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RelationshipEventConsumer {

    private final IUnifiedLogger logger;
    private final IdentityRepository identityRepository;
    private final DgraphService dgraphService; // <-- USE the new shared service

    public RelationshipEventConsumer(UnifiedLoggerFactory loggerFactory,
            IdentityRepository identityRepository,
            DgraphService dgraphService) { // <-- INJECT the new shared service
        this.logger = loggerFactory.getLogger("RelationshipEventConsumer", "identity-relationships");
        this.identityRepository = identityRepository;
        this.dgraphService = dgraphService;
    }

    @KafkaListener(topics = "identity-relationship-events", containerFactory = "relationshipEventKafkaListenerContainerFactory")
    public void handleRelationshipChangeEvent(RelationshipChangeEvent event, Acknowledgment acknowledgment) {
        logger.info().message("Received relationship change event.")
                .detail("subjectId", event.subjectId())
                .detail("objectId", event.objectId())
                .detail("relationship", event.relationshipType())
                .detail("action", event.action())
                .log();

        try {
            // Step 1: Get the Identity to find its Dgraph UID. This is the "subject" of the
            // relationship.
            Identity identity = identityRepository.findById(event.subjectId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Subject Identity not found for relationship event: " + event.subjectId()));

            String subjectDgraphUid = identity.getDgraphNodeId();
            if (subjectDgraphUid == null || subjectDgraphUid.isBlank()) {
                throw new IllegalStateException(
                        "Identity is missing its Dgraph UID, cannot process relationship: " + identity.getId());
            }

            // Step 2: Find the Dgraph UID of the target object (e.g., the Role). This is
            // the "object" of the relationship.
            String targetIdPredicate = getPredicateForObjectType(event.relationshipType());
            String dqlQuery = String.format("{ q(func: eq(%s, \"%s\")) { uid } }", targetIdPredicate, event.objectId());

            String targetDgraphUid = dgraphService.query(dqlQuery, "q")
                    .flatMap(this::extractUidFromJson) // Helper method to parse the JSON result
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Target object not found in Dgraph for relationship event: " + event.objectId()));

            // Step 3: Call the generic shared service to perform the requested action.
            String predicate = event.relationshipType().toLowerCase();
            if (event.action() == RelationshipChangeEvent.Action.CREATE) {
                dgraphService.createRelationship(subjectDgraphUid, predicate, targetDgraphUid);
            } else if (event.action() == RelationshipChangeEvent.Action.DELETE) {
                dgraphService.deleteRelationship(subjectDgraphUid, predicate, targetDgraphUid);
            }

            acknowledgment.acknowledge();
            logger.info().message("Successfully processed relationship change event.")
                    .detail("subjectId", event.subjectId()).detail("objectId", event.objectId()).log();

        } catch (Exception e) {
            logger.error(e).message("Failed to process relationship change event. Acknowledging to avoid retries.")
                    .log();
            // Acknowledge a "poison pill" message to prevent it from blocking the consumer.
            // In a more advanced setup, this would be routed to a Dead Letter Queue.
            acknowledgment.acknowledge();
        }
    }

    /**
     * Maps a business relationship type from an event to the specific Dgraph
     * predicate
     * used to query for the TARGET object's canonical ID.
     */
    private String getPredicateForObjectType(String relationshipType) {
        return switch (relationshipType.toUpperCase()) {
            case "HAS_ROLE" -> "role.id";
            case "HAS_ENTITLEMENT" -> "entitlement.id";
            case "REPORTS_TO" -> "identity.id"; // The manager (target object) is also an identity
            default ->
                throw new IllegalArgumentException("Unknown or unsupported relationship type: " + relationshipType);
        };
    }

    /**
     * Helper method to safely parse the JSON result from the shared DgraphService.
     */
    private Optional<String> extractUidFromJson(JsonElement jsonElement) {
        if (jsonElement == null || !jsonElement.isJsonArray() || jsonElement.getAsJsonArray().isEmpty()) {
            return Optional.empty();
        }
        JsonObject firstResult = jsonElement.getAsJsonArray().get(0).getAsJsonObject();
        return Optional.ofNullable(firstResult.get("uid")).map(JsonElement::getAsString);
    }
}