package com.aidentx.applicationpermissions.roles_management.config.audit;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
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

import java.util.Map;
import java.util.UUID;

/**
 * Publisher for role audit events to Kafka.
 * <p>
 * This component converts role-specific audit events to standardized
 * AuditLogKafkaEvent format and publishes them to the audit-log Kafka topic.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-01-21
 */
@Component
public class RoleAuditEventPublisher {
    
    private static final String AUDIT_LOG_TOPIC = "audit-log";
    private static final String ACTOR_TYPE_USER = "user";
    private static final String ACTOR_TYPE_SYSTEM = "system";
    private static final String STATUS_SUCCESS = "success";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    
    public RoleAuditEventPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            UnifiedLoggerFactory loggerFactory,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = loggerFactory.getLogger(
            AppConstants.ROLE_AUDIT_EVENT_PUBLISHER_LOGGER_NAME, 
            AppConstants.LOG_CATEGORY_ROLE_AUDIT
        );
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publishes a role audit event to Kafka.
     * 
     * @param event the role audit event to publish
     */
    public void publishEvent(RoleAuditEvent event) {
        try {
            logger.info()
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message(AppConstants.AUDIT_LOG_CONVERTING_EVENT_TO_STANDARD_FORMAT)
                .details(Map.of(
                    "action", event.action(),
                    "entityType", event.entityType(),
                    "entityId", event.entityId()
                ))
                .log();
            
            // Convert to standardized audit event
            AuditLogKafkaEvent standardEvent = convertToStandardEvent(event);
            
            // Use entityId as partition key for better distribution
            String partitionKey = event.entityId();
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message(AppConstants.AUDIT_LOG_PUBLISHING_EVENT_TO_KAFKA)
                .details(Map.of(
                    "topic", AUDIT_LOG_TOPIC,
                    "partitionKey", partitionKey,
                    "eventAction", event.action()
                ))
                .log();
            
            kafkaTemplate.send(AUDIT_LOG_TOPIC, partitionKey, standardEvent);
            
            logger.info()
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message(AppConstants.AUDIT_LOG_EVENT_PUBLISHED_SUCCESSFULLY)
                .details(Map.of(
                    "entityId", event.entityId(),
                    "action", event.action()
                ))
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_EVENT)
                .details(Map.of(
                    "entityId", event.entityId(),
                    "action", event.action(),
                    "error", e.getMessage()
                ))
                .log();
        }
    }
    
    /**
     * Converts role audit event to standardized format.
     */
    private AuditLogKafkaEvent convertToStandardEvent(RoleAuditEvent event) {
        logger.info()
            .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
            .message(AppConstants.CONVERTING_ROLE_AUDIT_EVENT)
            .details(Map.of(
                "action", event.action(),
                "entityType", event.entityType(),
                "userId", event.userId(),
                "username", event.username()
            ))
            .log();
            
        JsonNode details = createDetails(event);
        
        logger.info()
            .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
            .message(AppConstants.CREATED_DETAILS_FOR_AUDIT_EVENT)
            .details(Map.of(
                "detailsIsNull", details == null,
                "detailsContent", details != null ? details.toString() : "null"
            ))
            .log();
            
        // Determine actor type based on user ID (system UUID vs normal user)
        String actorType = "00000000-0000-0000-0000-000000000000".equals(event.userId()) ? 
            ACTOR_TYPE_SYSTEM : ACTOR_TYPE_USER;
            
        AuditLogKafkaEvent standardEvent = new AuditLogKafkaEvent(
            UUID.fromString(event.userId()),     // actorId
            actorType,                           // actorType (dynamic: system or user)
            event.username(),                    // actorName
            event.tenantId(),                    // tenantId
            UUID.fromString(event.entityId()),   // objectId
            event.entityType(),                  // objectType
            event.action().toLowerCase(),        // event
            STATUS_SUCCESS,                      // status
            event.riskLevel(),                   // risk (dynamic from role)
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
            .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
            .message(AppConstants.CREATED_STANDARD_AUDIT_EVENT)
            .details(Map.of("standardEventCreated", standardEvent != null))
            .log();
            
        return standardEvent;
    }

    /**
     * Creates details for the audit event with message, ipAddress, userAgent, and identifier.
     */
    private JsonNode createDetails(RoleAuditEvent event) {
        logger.info()
            .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
            .message(AppConstants.CREATING_AUDIT_DETAILS_FOR_ROLE_EVENT)
            .details(Map.of(
                "action", event.action(),
                "entityType", event.entityType(),
                "entityName", event.entityName()
            ))
            .log();
            
        ObjectNode details = objectMapper.createObjectNode();
        
        // Create descriptive message based on action type
        String message;
        if ("create dynamic rule".equals(event.action())) {
            message = String.format(AppConstants.AUDIT_MSG_CREATE_DYNAMIC_RULE, event.entityName());
        } else if ("update dynamic rule".equals(event.action())) {
            message = String.format(AppConstants.AUDIT_MSG_UPDATE_DYNAMIC_RULE, event.entityName());
        } else if ("assign permission".equals(event.action())) {
            message = String.format(AppConstants.AUDIT_MSG_ASSIGN_PERMISSION, event.entityName());
        } else if ("delete permission".equals(event.action())) {
            message = String.format(AppConstants.AUDIT_MSG_DELETE_PERMISSION, event.entityName());
        } else if ("update permission".equals(event.action())) {
            message = String.format(AppConstants.AUDIT_MSG_UPDATE_PERMISSION, event.entityName());
        } else if ("assign role".equals(event.action())) {
            message = String.format(AppConstants.AUDIT_MSG_ASSIGN_ROLE, event.entityName());
        } else if ("revoke role".equals(event.action())) {
            message = String.format(AppConstants.AUDIT_MSG_REVOKE_ROLE, event.entityName());
        } else if ("role reconciliation".equals(event.action())) {
            message = String.format(AppConstants.AUDIT_MSG_ROLE_RECONCILIATION, event.entityName());
        } else {
            // For other actions, use the standard format
            message = event.action().toLowerCase() + ": " + event.entityName();
        }
        details.put("message", message);
        
        // Use actual IP address from event
        details.put("ipAddress", event.userIp());
        
        // Get User-Agent from HTTP request
        String userAgent = "system"; // fallback
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            String requestUserAgent = request.getHeader("User-Agent");
            if (requestUserAgent != null && !requestUserAgent.isEmpty()) {
                userAgent = requestUserAgent;
            }
        } catch (Exception e) {
            // No request available, keep default
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message(AppConstants.FAILED_TO_GET_USER_AGENT)
                .details(Map.of("error", e.getMessage()))
                .log();
        }
        details.put("userAgent", userAgent);
        
        // Use username as identifier
        details.put("identifier", event.username());
        
        logger.info()
            .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
            .message(AppConstants.CREATED_AUDIT_DETAILS)
            .details(Map.of("details", details.toString()))
            .log();
        
        return details;
    }
    
    /**
     * Creates metadata for the audit event.
     */
    private JsonNode createMetadata(RoleAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();
        
        // For dynamic rule creation events, include the rule details in metadata
        if ("create dynamic rule".equals(event.action())) {
            // Copy all fields from stateAfter to metadata for dynamic rule creation
            if (event.stateAfter() != null && event.stateAfter().isObject()) {
                ObjectNode stateAfter = (ObjectNode) event.stateAfter();
                stateAfter.fields().forEachRemaining(entry -> {
                    metadata.set(entry.getKey(), entry.getValue());
                });
            }
        } else {
            // For other events, use the basic metadata structure
            metadata.put("service", "application-permissions-service");
            metadata.put("module", "role-management");
            metadata.put("userIp", event.userIp());
        }
        
        return metadata;
    }
    
    /**
     * Creates target object information for the audit event.
     */
    private JsonNode createTargetObject(RoleAuditEvent event) {
        ObjectNode targetObject = objectMapper.createObjectNode();
        
        // For dynamic rule creation events, the target is always a role
        if ("create dynamic rule".equals(event.action())) {
            targetObject.put("type", "role");
        } else {
            targetObject.put("type", event.entityType());
        }
        
        targetObject.put("objectId", event.entityId());
        targetObject.put("objectName", event.entityName()); // Use actual entity name instead of entity type
        return targetObject;
    }
    
    /**
     * Safely parses a string to UUID, handling cases where the string might not be a valid UUID format.
     * 
     * @param value the string value to parse
     * @param fieldName the name of the field for logging purposes
     * @return a valid UUID or a generated UUID if parsing fails
     */
    private UUID safeParseUUID(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            logger.warn()
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message(AppConstants.EMPTY_OR_NULL_UUID_GENERATING_DEFAULT)
                .details(Map.of("fieldName", fieldName, "value", value))
                .log();
            return UUID.randomUUID();
        }
        
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            logger.warn()
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message(AppConstants.INVALID_UUID_FORMAT_GENERATING_DEFAULT)
                .details(Map.of(
                    "fieldName", fieldName,
                    "invalidValue", value,
                    "error", e.getMessage()
                ))
                .log();
            
            // Generate a deterministic UUID based on the invalid value to maintain consistency
            // This ensures the same invalid value always generates the same UUID
            return UUID.nameUUIDFromBytes(value.getBytes());
        }
    }
}