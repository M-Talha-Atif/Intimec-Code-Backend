package com.aidentx.iam.config.audit;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.iam.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditEventPublisher {

    private final IUnifiedLogger logger;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public AuditEventPublisher(@Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = unifiedLoggerFactory.getLogger(AppConstants.AUDIT_LOGGER_NAME, AppConstants.AUDIT_LOG_CATEGORY);
    }

    /**
     * Publishes an identity audit event to Kafka using the standardized AuditLogKafkaEvent.
     * 
     * @param event the audit event to publish
     */
    public void publishEvent(IdentityAuditEvent event) {
        try {
            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_CONVERTING_EVENT)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_ACTION, event.action(),
                            AppConstants.AUDIT_DETAIL_ENTITY_TYPE, event.entityType(),
                            AppConstants.AUDIT_DETAIL_ENTITY_ID, event.entityId(),
                            AppConstants.AUDIT_METADATA_IDENTITY_NAME, event.identityName() != null ? event.identityName() : AppConstants.AUDIT_NOT_AVAILABLE,
                            AppConstants.AUDIT_METADATA_TENANT_ID, event.tenantId() != null ? event.tenantId() : AppConstants.AUDIT_NOT_AVAILABLE))
                    .log();

            // Convert our custom event to the standardized AuditLogKafkaEvent
            AuditLogKafkaEvent standardEvent = convertToStandardEvent(event);
            
            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_PUBLISHING_EVENT)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME,
                            AppConstants.AUDIT_DETAIL_EVENT_TYPE, standardEvent.getEvent(),
                            AppConstants.AUDIT_DETAIL_OBJECT_ID, standardEvent.getObjectId().toString()))
                    .log();

            // Use entityId as the Kafka key for partitioning
            kafkaTemplate.send(AppConstants.AUDIT_TOPIC_NAME, event.entityId(), standardEvent);
            
            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESS_SENT)
                    .details(Map.of(AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME))
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_PUBLISH)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_ENTITY_TYPE, event.entityType(),
                            AppConstants.AUDIT_DETAIL_ENTITY_ID, event.entityId(),
                            AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME,
                            AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName()))
                    .log();
        }
    }
    
    /**
     * Converts our custom IdentityAuditEvent to the standardized AuditLogKafkaEvent
     * that the existing Kafka consumer can process and save to database.
     */
    private AuditLogKafkaEvent convertToStandardEvent(IdentityAuditEvent event) {
        return new AuditLogKafkaEvent(
                java.util.UUID.fromString(event.userId()), // actorId
                AppConstants.AUDIT_ACTOR_TYPE_USER, // actorType (using constant)
                event.username(), // actorName
                event.tenantId(), // tenantId
                java.util.UUID.fromString(event.entityId()), // objectId
                event.entityType(), // objectType (IDENTITY)
                event.action().toLowerCase(), // event (using lowercase)
                AppConstants.AUDIT_STATUS_SUCCESS, // status (using lowercase constant)
                AppConstants.AUDIT_RISK_LOW, // risk (using lowercase constant)
                event.timestamp(), // timestamp
                null, // details - could be enhanced with additional context
                event.stateBefore(), // stateBefore
                event.stateAfter(), // stateAfter
                createMetadata(event), // metadata with identity and tenant info
                null, // errorDetails - null for successful operations
                null, // governance - could be enhanced with compliance info
                createTargetObject(event) // targetObject with identity details
        );
    }
    
    /**
     * Creates metadata JSON with identity-specific information.
     */
    private com.fasterxml.jackson.databind.JsonNode createMetadata(IdentityAuditEvent event) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode metadata = mapper.createObjectNode();
        
        metadata.put(AppConstants.AUDIT_METADATA_IDENTITY_NAME, event.identityName());
        metadata.put(AppConstants.AUDIT_METADATA_TENANT_ID, event.tenantId());
        metadata.put(AppConstants.AUDIT_METADATA_ENTITY_TYPE, event.entityType());
        metadata.put(AppConstants.AUDIT_METADATA_USER_IP, event.userIp());
        metadata.put(AppConstants.AUDIT_METADATA_SOURCE, AppConstants.AUDIT_SOURCE_IDENTITY_SERVICE);
        
        return metadata;
    }
    
    /**
     * Creates target object JSON with entity details.
     */
    private com.fasterxml.jackson.databind.JsonNode createTargetObject(IdentityAuditEvent event) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.node.ObjectNode targetObject = mapper.createObjectNode();
        
        targetObject.put(AppConstants.AUDIT_TARGET_TYPE, event.entityType());
        targetObject.put(AppConstants.AUDIT_TARGET_OBJECT_ID, event.entityId());
        targetObject.put(AppConstants.AUDIT_TARGET_OBJECT_NAME, event.identityName());
        
        return targetObject;
    }
}
