package com.aidentx.core.notifications_management.config.audit;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.shared.security.RequestUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Publisher for notification audit events to Kafka.
 * <p>
 * This component converts notification-specific audit events to standardized
 * AuditLogKafkaEvent format and publishes them to the audit-log Kafka topic.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-22
 */
@Component
public class NotificationAuditEventPublisher {
    
    private static final String AUDIT_LOG_TOPIC = "audit-log";
    private static final String ACTOR_TYPE = "user";
    private static final String STATUS_SUCCESS = "success";
    private static final String RISK_LEVEL_LOW = "low";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    
    public NotificationAuditEventPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            UnifiedLoggerFactory loggerFactory,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = loggerFactory.getLogger(
            AppConstants.NOTIFICATION_AUDIT_EVENT_PUBLISHER_LOGGER_NAME, 
            AppConstants.LOG_CATEGORY_NOTIFICATION_AUDIT
        );
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publishes a notification audit event to Kafka.
     * 
     * @param event the notification audit event to publish
     */
    public void publishEvent(NotificationAuditEvent event) {
        try {
            logger.info()
                .message(AppConstants.AUDIT_LOG_CONVERTING_EVENT_TO_STANDARD_FORMAT)
                .detail("action", event.action())
                .detail("entityType", event.entityType())
                .detail("entityId", event.entityId())
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
            
            // Convert to standardized audit event
            AuditLogKafkaEvent standardEvent = convertToStandardEvent(event);
            
            // Use entityId as partition key for better distribution
            String partitionKey = event.entityId();
            
            logger.info()
                .message(AppConstants.AUDIT_LOG_PUBLISHING_EVENT_TO_KAFKA)
                .detail("topic", AUDIT_LOG_TOPIC)
                .detail("partitionKey", partitionKey)
                .detail("eventAction", event.action())
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
            
            kafkaTemplate.send(AUDIT_LOG_TOPIC, partitionKey, standardEvent);
            
            logger.info()
                .message(AppConstants.AUDIT_LOG_EVENT_PUBLISHED_SUCCESSFULLY)
                .detail("entityId", event.entityId())
                .detail("action", event.action())
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_EVENT)
                .detail("entityId", event.entityId())
                .detail("action", event.action())
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
        }
    }
    
    /**
     * Converts notification audit event to standardized format.
     */
    private AuditLogKafkaEvent convertToStandardEvent(NotificationAuditEvent event) {
        logger.info()
            .message("Converting notification audit event to standard format")
            .detail("action", event.action())
            .detail("entityType", event.entityType())
            .detail("userId", event.userId())
            .detail("username", event.username())
            .log();
            
        JsonNode details = createDetails(event);
        
        logger.info()
            .message("Created details for audit event")
            .detail("detailsIsNull", details == null)
            .detail("detailsContent", details != null ? details.toString() : "null")
            .log();
            
        AuditLogKafkaEvent standardEvent = new AuditLogKafkaEvent(
            UUID.fromString(event.userId()),     // actorId
            ACTOR_TYPE,                          // actorType
            event.username(),                    // actorName
            event.tenantId(),                    // tenantId
            UUID.fromString(event.entityId()),   // objectId
            event.entityType(),                  // objectType
            event.action().toLowerCase(),        // event
            STATUS_SUCCESS,                      // status
            RISK_LEVEL_LOW,                      // risk
            event.timestamp(),                   // timestamp
            details,                             // details - now populated with real data
            event.stateBefore(),                // stateBefore
            event.stateAfter(),                 // stateAfter
            createMetadata(event),              // metadata
            null,                               // errorDetails
            null,                               // governance
            createTargetObject(event)           // targetObject
        );
        
        logger.info()
            .message("Created standard audit event")
            .detail("standardEventCreated", standardEvent != null)
            .log();
            
        return standardEvent;
    }

    /**
     * Creates details for the audit event with message, ipAddress, userAgent, and identifier.
     */
    private JsonNode createDetails(NotificationAuditEvent event) {
        logger.info()
            .message("Creating audit details for notification event")
            .detail("action", event.action())
            .detail("entityType", event.entityType())
            .detail("entityName", event.entityName())
            .log();
            
        ObjectNode details = objectMapper.createObjectNode();
        
        // Create descriptive message
        String message = event.action().toLowerCase() + " " + event.entityType() + ": " + event.entityName();
        details.put("message", message);
        
        // Use actual IP address from event
        details.put("ipAddress", event.userIp());
        
        // Get User-Agent from HTTP request
        String userAgent = "system"; // fallback
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String requestUserAgent = RequestUtils.getUserAgent(request);
            if (requestUserAgent != null && !requestUserAgent.isEmpty()) {
                userAgent = requestUserAgent;
            }
        } catch (Exception e) {
            // No request available, keep default
            logger.debug()
                .message("Failed to get User-Agent from request")
                .detail("error", e.getMessage())
                .log();
        }
        details.put("userAgent", userAgent);
        
        // Use username as identifier
        details.put("identifier", event.username());
        
        logger.info()
            .message("Created audit details")
            .detail("details", details.toString())
            .log();
        
        return details;
    }
    
    /**
     * Creates metadata for the audit event.
     */
    private JsonNode createMetadata(NotificationAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("service", "core-service");
        metadata.put("module", "notification-management");
        metadata.put("userIp", event.userIp());
        return metadata;
    }
    
    /**
     * Creates target object information for the audit event.
     */
    private JsonNode createTargetObject(NotificationAuditEvent event) {
        ObjectNode targetObject = objectMapper.createObjectNode();
        targetObject.put("type", event.entityType());
        targetObject.put("objectId", event.entityId());
        targetObject.put("objectName", event.entityName()); // Use actual entity name instead of entity type
        return targetObject;
    }
}
