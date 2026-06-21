package com.aidentx.workflowsservice.audit;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
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
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Publisher for workflow audit events to Kafka.
 * <p>
 * This component converts workflow-specific audit events to standardized
 * AuditLogKafkaEvent format and publishes them to the audit-log Kafka topic.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-12-01
 */
@Component
public class AuditEventPublisher {
    
    private static final String AUDIT_LOG_TOPIC = "audit-log";
    private static final String ACTOR_TYPE_USER = "user";
    private static final String ACTOR_TYPE_SYSTEM = "system";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_FAILURE = "failure";
    private static final String STATUS_WARNING  = "warning";
    private static final String RISK_LEVEL_LOW = "low";
    private static final String RISK_LEVEL_MEDIUM = "medium";
    private static final String RISK_LEVEL_HIGH = "high";
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    
    public AuditEventPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            UnifiedLoggerFactory loggerFactory,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = loggerFactory.getLogger("WorkflowAuditPublisher", "workflow-audit");
        this.objectMapper = objectMapper;
    }
    
    /**
     * Publishes a workflow audit event to Kafka.
     * 
     * @param event the workflow audit event to publish
     */
    public void publishEvent(WorkflowAuditEvent event) {
        try {
            logger.info()
                .message(LOG_CONVERTING_TO_STANDARD_AUDIT)
                .detail("action", event.action())
                .detail("entityType", event.entityType())
                .detail("entityId", event.entityId())
                .log();
            
            // Convert to standardized audit event
            AuditLogKafkaEvent standardEvent = convertToStandardEvent(event);
            
            // Use entityId as partition key for better distribution
            String partitionKey = event.entityId().toString();
            
            logger.info()
                .message(LOG_PUBLISHING_AUDIT_TO_KAFKA)
                .detail("topic", AUDIT_LOG_TOPIC)
                .detail("partitionKey", partitionKey)
                .detail("eventAction", event.action())
                .log();
            
            kafkaTemplate.send(AUDIT_LOG_TOPIC, partitionKey, standardEvent);
            
            logger.info()
                .message(LOG_AUDIT_EVENT_PUBLISHED)
                .detail("entityId", event.entityId())
                .detail("action", event.action())
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .message(LOG_FAILED_PUBLISH_AUDIT_TO_KAFKA)
                .detail("entityId", event.entityId())
                .detail("action", event.action())
                .detail("error", e.getMessage())
                .log();
        }
    }
    
    /**
     * Converts workflow audit event to standardized AuditLogKafkaEvent.
     */
    private AuditLogKafkaEvent convertToStandardEvent(WorkflowAuditEvent event) {
        // Determine risk level based on action
        String riskLevel = determineRiskLevel(event.action());
        
        // Determine status based on action (for icon color)
        String status = determineStatus(event.action());
        
        // Create metadata with HTTP context
        JsonNode metadata = createMetadata(event);
        
        // Create details node with workflow/execution information
        JsonNode details = createDetails(event);
        
        // Create targetObject with objectName extracted from workflow/execution/approval
        JsonNode targetObject = createTargetObject(event);
        
        return new AuditLogKafkaEvent(
            UUID.randomUUID(),              // id
            event.actorId(),                // actorId (can be null for system)
            event.actorType(),              // actorType
            event.actorName(),              // actorName (from event)
            event.tenantId(),               // tenantId
            event.entityId(),               // objectId
            event.entityType(),             // objectType
            event.action(),                 // event
            status,                         // status (determines icon color)
            riskLevel,                      // risk
            Instant.now(),                  // timestamp
            details,                        // details
            event.stateBefore(),            // stateBefore
            event.stateAfter(),             // stateAfter
            metadata,                       // metadata
            null,                           // errorDetails
            null,                           // governance
            targetObject                    // targetObject with objectName
        );
    }
    
    /**
     * Creates targetObject with objectName for proper display in audit logs.
     */
    private JsonNode createTargetObject(WorkflowAuditEvent event) {
        ObjectNode targetObject = objectMapper.createObjectNode();
        
        targetObject.put("objectId", event.entityId().toString());
        targetObject.put("type", event.entityType());
        
        // Extract object name based on entity type
        String objectName = extractObjectName(event);
        
        // Frontend expects "username" field for display name (not literal username, just display name)
        // Auth-service uses "objectName" but frontend reads "username" - send both for compatibility
        targetObject.put("username", objectName);
        targetObject.put("objectName", objectName);
        
        // Include the full state if available
        if (event.stateAfter() != null) {
            targetObject.set("state", event.stateAfter());
        } else if (event.stateBefore() != null) {
            targetObject.set("state", event.stateBefore());
        }
        
        return targetObject;
    }
    
    /**
     * Extracts the appropriate object name from the event based on entity type.
     */
    private String extractObjectName(WorkflowAuditEvent event) {
        String entityType = event.entityType();
        
        // For workflow execution, try to get workflow name from execution data FIRST
        if ("execution".equals(entityType) || "workflow-execution".equals(entityType)) {
            if (event.stateAfter() != null && event.stateAfter().has("workflowName")) {
                return event.stateAfter().get("workflowName").asText();
            }
            if (event.stateAfter() != null && event.stateAfter().has("workflow")) {
                JsonNode workflow = event.stateAfter().get("workflow");
                if (workflow.has("name")) {
                    return workflow.get("name").asText();
                }
            }
        }
        
        // For approval, try to get workflow name first
        if ("approval".equals(entityType) || "workflow-approval".equals(entityType)) {
            if (event.stateAfter() != null && event.stateAfter().has("workflowName")) {
                return event.stateAfter().get("workflowName").asText();
            }
            if (event.stateAfter() != null && event.stateAfter().has("workflow")) {
                JsonNode workflow = event.stateAfter().get("workflow");
                if (workflow.has("name")) {
                    return workflow.get("name").asText();
                }
            }
            if (event.stateAfter() != null && event.stateAfter().has("taskName")) {
                return event.stateAfter().get("taskName").asText();
            }
            if (event.stateAfter() != null && event.stateAfter().has("executionId")) {
                String executionId = event.stateAfter().get("executionId").asText();
                return "Approval for Execution " + executionId.substring(0, 8);
            }
        }
        
        // For workflow entity type (workflow CRUD operations), get name from state
        if ("workflow".equals(entityType)) {
            if (event.stateAfter() != null && event.stateAfter().has("name")) {
                return event.stateAfter().get("name").asText();
            }
            if (event.stateBefore() != null && event.stateBefore().has("name")) {
                return event.stateBefore().get("name").asText();
            }
        }
        
        // Generic fallback - try name field
        if (event.stateAfter() != null && event.stateAfter().has("name")) {
            return event.stateAfter().get("name").asText();
        }
        if (event.stateBefore() != null && event.stateBefore().has("name")) {
            return event.stateBefore().get("name").asText();
        }
        
        // Fallback: use entity type and ID
        return event.entityType() + " " + event.entityId().toString().substring(0, 8);
    }
    
    /**
     * Determines risk level based on action.
     */
    private String determineRiskLevel(String action) {
        return switch (action) {
            case "workflow-deleted", "approval-rejected", "execution-failed" -> RISK_LEVEL_HIGH;
            case "workflow-executed", "Workflow-Execution-Cancelled", "approval-approved", "approval-created" -> RISK_LEVEL_MEDIUM;
            case "workflow-simulated" -> RISK_LEVEL_LOW; // Simulations are low risk
            default -> RISK_LEVEL_LOW;
        };
    }
    
    /**
     * Determines status based on action (for icon color).
     * - success: green icon
     * - warning: yellow icon
     * - failure: red icon
     */
    private String determineStatus(String action) {
        return switch (action) {
            case "workflow-deleted", "approval-rejected", "execution-failed" -> STATUS_FAILURE;
            case "Workflow-Execution-Cancelled" -> STATUS_WARNING;
            default -> STATUS_SUCCESS;
        };
    }
    
    /**
     * Creates details node with workflow/execution information.
     * This provides rich contextual information displayed in audit logs UI.
     */
    private JsonNode createDetails(WorkflowAuditEvent event) {
        ObjectNode details = objectMapper.createObjectNode();
        
        // Create descriptive message (shown in Event Details)
        String objectName = extractObjectName(event);
        String message = createDescriptiveMessage(event.action(), event.entityType(), objectName);
        details.put("message", message);
        
        // Object Name - for frontend fallback display
        details.put("objectName", objectName);
        
        // IP Address (shown in Event Details)
        String ipAddress = event.clientIp() != null ? event.clientIp() : "Unknown";
        details.put("ipAddress", ipAddress);
        
        // User Agent (shown in Event Details)
        String userAgent = event.userAgent() != null ? event.userAgent() : "Unknown";
        details.put("userAgent", userAgent);
        
        // Identifier - use actor name for user-friendly display
        String identifier = event.actorName() != null ? event.actorName() : 
                           (event.actorId() != null ? event.actorId().toString() : "System");
        details.put("identifier", identifier);
        
        // Actor Name - explicitly for frontend fallback
        if (event.actorName() != null) {
            details.put("actorName", event.actorName());
        }
        
        // Location (can be enhanced with GeoIP later)
        details.put("location", "Unknown");
        
        // Additional context fields
        details.put("action", event.action());
        details.put("entityType", event.entityType());
        details.put("entityId", event.entityId().toString());
        details.put("actorType", event.actorType());
        
        if (event.actorId() != null) {
            details.put("actorId", event.actorId().toString());
        }
        
        if (event.actorName() != null) {
            details.put("actorName", event.actorName());
        }
        
        details.put("tenantId", event.tenantId());
        details.put("timestamp", Instant.now().toString());
        
        return details;
    }
    
    /**
     * Creates a human-readable descriptive message for the audit event.
     */
    private String createDescriptiveMessage(String action, String entityType, String objectName) {
        return switch (action) {
            case "workflow-created" -> "Created workflow: " + objectName;
            case "workflow-updated" -> "Updated workflow: " + objectName;
            case "workflow-deleted" -> "Deleted workflow: " + objectName;
            case "workflow-executed" -> "Executed workflow: " + objectName;
            case "workflow-simulated" -> "Simulated workflow: " + objectName;
            case "Workflow-Execution-Cancelled" -> "Cancelled execution: " + objectName;
            case "approval-created" -> "Created approval request for: " + objectName;
            case "approval-approved" -> "Approved: " + objectName;
            case "approval-rejected" -> "Rejected: " + objectName;
            case "approval-delegated" -> "Delegated approval: " + objectName;
            default -> action + " " + entityType + ": " + objectName;
        };
    }
    
    /**
     * Creates metadata for the audit event.
     */
    private JsonNode createMetadata(WorkflowAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();
        
        metadata.put("service", "workflows-service");
        metadata.put("entityType", event.entityType());
        
        // Add HTTP context if available
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                if (event.clientIp() != null) {
                    metadata.put("clientIp", event.clientIp());
                } else {
                    metadata.put("clientIp", getClientIpAddress(request));
                }
                
                if (event.userAgent() != null) {
                    metadata.put("userAgent", event.userAgent());
                } else {
                    String userAgent = request.getHeader("User-Agent");
                    if (userAgent != null) {
                        metadata.put("userAgent", userAgent);
                    }
                }
                
                metadata.put("requestMethod", request.getMethod());
                metadata.put("requestUri", request.getRequestURI());
            }
        } catch (Exception e) {
            // Request context not available (async thread, scheduled task, etc.)
            logger.debug()
                .message(LOG_REQUEST_CONTEXT_NOT_AVAILABLE)
                .log();
        }
        
        return metadata;
    }
    
    /**
     * Extracts client IP address from request.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        
        return request.getRemoteAddr();
    }
}
