package com.aidentx.iam.identity_management.service;

import com.aidentx.iam.model.dto.events.RelationshipChangeEvent;
import com.aidentx.iam.model.dto.events.RelationshipChangeEvent.Action;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * A dedicated service for publishing relationship change events to Kafka.
 * This service acts as the primary mechanism for the core-service to command
 * the identities-service to update the Dgraph relationship graph.
 */
@Service
public class RelationshipEventPublisher {

    private static final String RELATIONSHIP_TOPIC = "identity-relationship-events";

    private final IUnifiedLogger logger;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RelationshipEventPublisher(UnifiedLoggerFactory loggerFactory,
            @Qualifier("relationshipKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        this.logger = loggerFactory.getLogger("RelationshipEventPublisher", "provisioning-events");
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes an event to create or delete a relationship between two objects.
     *
     * @param subjectId        The source of the relationship (e.g., the Identity's
     *                         UUID).
     * @param objectId         The target of the relationship (e.g., the Role's
     *                         UUID).
     * @param relationshipType The Dgraph predicate for the relationship (e.g.,
     *                         "HAS_ROLE").
     * @param action           The action to perform (CREATE or DELETE).
     */
    public void publishRelationshipChangeEvent(UUID subjectId, UUID objectId, String relationshipType, Action action) {
        try {
            RelationshipChangeEvent event = new RelationshipChangeEvent(subjectId, objectId, relationshipType, action);

            // Use the subject's ID as the Kafka key to ensure events for the same identity
            // are processed in order by the same consumer partition.
            String kafkaKey = subjectId.toString();

            kafkaTemplate.send(RELATIONSHIP_TOPIC, kafkaKey, event);

            logger.info().message("Successfully published relationship change event to Kafka.")
                    .detail("topic", RELATIONSHIP_TOPIC)
                    .detail("kafkaKey", kafkaKey)
                    .detail("relationshipType", relationshipType)
                    .detail("action", action)
                    .log();
        } catch (Exception e) {
            logger.error(e).message("Failed to publish relationship change event.")
                    .detail("subjectId", subjectId)
                    .detail("objectId", objectId)
                    .log();
            // Re-throw to ensure the calling transaction is rolled back.
            throw new RuntimeException("Failed to publish Kafka event for relationship change.", e);
        }
    }
}