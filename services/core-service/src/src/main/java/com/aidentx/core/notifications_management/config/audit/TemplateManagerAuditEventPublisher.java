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
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service for publishing template manager audit events to Kafka.
 * <p>
 * This service converts template manager audit events to the standard
 * audit log format and publishes them to the audit-log Kafka topic
 * for centralized audit processing.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-22
 */
@Service
public class TemplateManagerAuditEventPublisher {
    
    private static final String AUDIT_TOPIC = "audit-log";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    
    /**
     * Constructor for TemplateManagerAuditEventPublisher.
     * 
     * @param kafkaTemplate Kafka template for sending audit events
     * @param loggerFactory Factory for creating loggers
     * @param objectMapper Object mapper for JSON processing
     */
    public TemplateManagerAuditEventPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            UnifiedLoggerFactory loggerFactory,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = loggerFactory.getLogger(
            AppConstants.TEMPLATE_MANAGER_AUDIT_EVENT_PUBLISHER_LOGGER_NAME, 
            AppConstants.LOG_CATEGORY_TEMPLATE_MANAGER_AUDIT
        );
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publishes a template manager audit event to Kafka.
     * 
     * @param auditEvent the template manager audit event to publish
     */
    public void publishAuditEvent(TemplateManagerAuditEvent auditEvent) {
        try {
            logger.info()
                .message(AppConstants.AUDIT_LOG_CONVERTING_EVENT_TO_STANDARD_FORMAT)
                .detail("entityType", AppConstants.ENTITY_TYPE_TEMPLATE_MANAGER)
                .detail("action", auditEvent.action())
                .detail("entityId", auditEvent.templateManagerId())
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
            
            AuditLogKafkaEvent standardEvent = convertToStandardEvent(auditEvent);
            
            logger.info()
                .message(AppConstants.AUDIT_LOG_PUBLISHING_EVENT_TO_KAFKA)
                .detail("partitionKey", auditEvent.templateManagerId().toString())
                .detail("topic", AUDIT_TOPIC)
                .detail("eventAction", auditEvent.action())
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
            
            kafkaTemplate.send(AUDIT_TOPIC, auditEvent.templateManagerId().toString(), standardEvent);
            
            logger.info()
                .message(AppConstants.AUDIT_LOG_EVENT_PUBLISHED_SUCCESSFULLY)
                .detail("action", auditEvent.action())
                .detail("entityId", auditEvent.templateManagerId())
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
                    
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_EVENT)
                .detail("action", auditEvent.action())
                .detail("entityId", auditEvent.templateManagerId())
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
        }
    }
    
    /**
     * Converts a template manager audit event to the standard audit log format.
     * 
     * @param auditEvent the template manager audit event
     * @return the standardized audit log event
     */
    private AuditLogKafkaEvent convertToStandardEvent(TemplateManagerAuditEvent auditEvent) {
        // For DELETE operations, stateBefore should contain the deleted object, stateAfter should be null
        // For CREATE operations, stateBefore should be null, stateAfter should contain the created object  
        // For UPDATE operations, stateBefore should contain old state, stateAfter should contain new state
        
        JsonNode stateBefore = null;
        JsonNode stateAfter = null;
        
        // Check if the event has explicit stateBefore/stateAfter
        if (auditEvent.stateBefore() != null || auditEvent.stateAfter() != null) {
            // Use the explicit states provided
            stateBefore = auditEvent.stateBefore() != null ? objectMapper.valueToTree(auditEvent.stateBefore()) : null;
            stateAfter = auditEvent.stateAfter() != null ? objectMapper.valueToTree(auditEvent.stateAfter()) : null;
        } else {
            // Fall back to legacy logic for backward compatibility
            if ("DELETE".equalsIgnoreCase(auditEvent.action())) {
                // For delete: what was there before (the deleted object) and nothing after
                stateBefore = objectMapper.valueToTree(auditEvent.templateData());
                stateAfter = null;
            } else if ("CREATE".equalsIgnoreCase(auditEvent.action())) {
                // For create: nothing before and the created object after
                stateBefore = null;
                stateAfter = objectMapper.valueToTree(auditEvent.templateData());
            } else {
                // For update and other operations: assume it's the new state
                stateBefore = null; // We don't currently capture the old state for updates
                stateAfter = objectMapper.valueToTree(auditEvent.templateData());
            }
        }
        
        return new AuditLogKafkaEvent(
            auditEvent.userId(),                            // actorId (UUID)
            "user",                                         // actorType
            auditEvent.username(),                          // actorName
            auditEvent.tenantId(),                          // tenantId
            auditEvent.templateManagerId(),                 // objectId (UUID)
            AppConstants.ENTITY_TYPE_TEMPLATE_MANAGER,      // objectType
            auditEvent.action().toLowerCase(),              // event
            "success",                                      // status
            "low",                                          // risk
            auditEvent.timestamp(),                         // timestamp
            createDetails(auditEvent),                      // details - now populated with real data
            stateBefore,                                    // stateBefore - correct based on operation type
            stateAfter,                                     // stateAfter - correct based on operation type
            createMetadata(auditEvent),                     // metadata
            null,                                           // errorDetails
            null,                                           // governance
            createTargetObject(auditEvent)                  // targetObject
        );
    }

    /**
     * Creates details for the audit event with message, ipAddress, userAgent, and identifier.
     */
    private JsonNode createDetails(TemplateManagerAuditEvent auditEvent) {
        ObjectNode details = objectMapper.createObjectNode();
        
        // Create descriptive message using template name from templateData, stateAfter, or stateBefore
        String templateName = "template";
        
        // Try to get template name from templateData first
        if (auditEvent.templateData() != null && auditEvent.templateData().containsKey("templateName")) {
            Object templateNameObj = auditEvent.templateData().get("templateName");
            if (templateNameObj != null) {
                templateName = templateNameObj.toString();
            }
        }
        // If not found, try stateAfter
        else if (auditEvent.stateAfter() != null && auditEvent.stateAfter().containsKey("templateName")) {
            Object templateNameObj = auditEvent.stateAfter().get("templateName");
            if (templateNameObj != null) {
                templateName = templateNameObj.toString();
            }
        }
        // If still not found, try stateBefore
        else if (auditEvent.stateBefore() != null && auditEvent.stateBefore().containsKey("templateName")) {
            Object templateNameObj = auditEvent.stateBefore().get("templateName");
            if (templateNameObj != null) {
                templateName = templateNameObj.toString();
            }
        }
        
        String message = auditEvent.action().toLowerCase() + " template_manager: " + templateName;
        details.put("message", message);
        
        // Get IP address from HTTP request (since TemplateManagerAuditEvent doesn't have userIp)
        String clientIp = "unknown";
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String requestIp = RequestUtils.getClientIP(request);
            if (requestIp != null && !requestIp.isEmpty()) {
                clientIp = requestIp;
            }
        } catch (Exception e) {
            // No request available, keep default
        }
        details.put("ipAddress", clientIp);
        
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
        }
        details.put("userAgent", userAgent);
        
        // Use username as identifier
        details.put("identifier", auditEvent.username());
        
        return details;
    }
    
    /**
     * Creates metadata for the audit event.
     * 
     * @param auditEvent the template manager audit event
     * @return the metadata JsonNode
     */
    private JsonNode createMetadata(TemplateManagerAuditEvent auditEvent) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("service", "core-service");
        metadata.put("module", "template-management");
        metadata.put("username", auditEvent.username());
        return metadata;
    }
    
    /**
     * Creates the target object information for the audit event.
     * 
     * @param auditEvent the template manager audit event
     * @return the target object JsonNode
     */
    private JsonNode createTargetObject(TemplateManagerAuditEvent auditEvent) {
        ObjectNode targetObject = objectMapper.createObjectNode();
        targetObject.put("type", AppConstants.ENTITY_TYPE_TEMPLATE_MANAGER);
        targetObject.put("objectId", auditEvent.templateManagerId().toString());
        
        // Extract templateName from templateData, stateAfter, or stateBefore for objectName
        String objectName = "Template Manager " + auditEvent.templateManagerId().toString();
        
        // Try to get template name from templateData first
        if (auditEvent.templateData() != null && auditEvent.templateData().containsKey("templateName")) {
            objectName = auditEvent.templateData().get("templateName").toString();
        }
        // If not found, try stateAfter
        else if (auditEvent.stateAfter() != null && auditEvent.stateAfter().containsKey("templateName")) {
            objectName = auditEvent.stateAfter().get("templateName").toString();
        }
        // If still not found, try stateBefore
        else if (auditEvent.stateBefore() != null && auditEvent.stateBefore().containsKey("templateName")) {
            objectName = auditEvent.stateBefore().get("templateName").toString();
        }
        
        targetObject.put("objectName", objectName);
        
        return targetObject;
    }
}
