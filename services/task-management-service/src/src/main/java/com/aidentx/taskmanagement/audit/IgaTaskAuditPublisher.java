package com.aidentx.taskmanagement.audit;

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

import java.util.Base64;
import java.util.UUID;

/**
 * Publishes IGA task lifecycle audit events to the centralized Kafka audit-log topic.
 * <p>
 * Converts {@link IgaTaskAuditEvent} instances to the shared
 * {@link AuditLogKafkaEvent} format consumed by the audit log service.
 * </p>
 */
@Component
public class IgaTaskAuditPublisher {

    private static final String AUDIT_LOG_TOPIC = "audit-log";
    private static final String OBJECT_TYPE = "iga_task";
    private static final String STATUS_SUCCESS = "success";
    private static final String SYSTEM_USER_ID = "00000000-0000-0000-0000-000000000000";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;

    public IgaTaskAuditPublisher(
            @Qualifier("igaTaskAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            UnifiedLoggerFactory loggerFactory,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = loggerFactory.getLogger(IgaTaskAuditPublisher.class);
        this.objectMapper = objectMapper;
    }

    /**
     * Publishes an IGA task audit event to the Kafka audit-log topic.
     *
     * @param event the task audit event to publish
     */
    public void publish(IgaTaskAuditEvent event) {
        try {
            AuditLogKafkaEvent kafkaEvent = toKafkaEvent(event);
            kafkaTemplate.send(AUDIT_LOG_TOPIC, event.taskId(), kafkaEvent);
            logger.info()
                    .message("IGA task audit event published")
                    .detail("action", event.action())
                    .detail("taskId", event.taskId())
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .message("Failed to publish IGA task audit event")
                    .detail("taskId", event.taskId())
                    .detail("action", event.action())
                    .log();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private AuditLogKafkaEvent toKafkaEvent(IgaTaskAuditEvent event) {
        String actorId = event.userId() != null ? event.userId() : SYSTEM_USER_ID;
        String actorType = SYSTEM_USER_ID.equals(actorId) ? "system" : "user";
        String actorName = resolveUsername();
        if (actorName == null || actorName.isEmpty()) actorName = actorId;

        return new AuditLogKafkaEvent(
                UUID.fromString(actorId),
                actorType,
                actorName,
                event.tenantId(),
                UUID.fromString(event.taskId()),
                OBJECT_TYPE,
                event.action(),
                STATUS_SUCCESS,
                event.riskLevel(),
                event.timestamp(),
                buildDetails(event),
                event.stateBefore(),
                event.stateAfter(),
                buildMetadata(event),
                null,
                buildGovernance(event.riskLevel()),
                buildTargetObject(event)
        );
    }

    private JsonNode buildDetails(IgaTaskAuditEvent event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("description", toHumanReadable(event.action()) + ": " + event.taskName());
        node.put("ipAddress", event.userIp() != null ? event.userIp() : "unknown");
        node.put("userAgent", resolveUserAgent());
        return node;
    }

    private JsonNode buildTargetObject(IgaTaskAuditEvent event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("id", event.taskId());
        node.put("name", event.taskName());
        node.put("username", event.taskName());
        node.put("objectName", event.taskName());
        node.put("type", OBJECT_TYPE);
        if (event.applicationId() != null) node.put("applicationId", event.applicationId());
        return node;
    }

    private JsonNode buildGovernance(String riskLevel) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("riskLevel", riskLevel);
        return node;
    }

    private JsonNode buildMetadata(IgaTaskAuditEvent event) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("service", "task-management-service");
        node.put("module", "task-management");
        if (event.applicationId() != null) node.put("applicationId", event.applicationId());
        if (event.userIp() != null) node.put("userIp", event.userIp());
        return node;
    }

    private String resolveUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            String ua = request.getHeader("User-Agent");
            return ua != null && !ua.isEmpty() ? ua : "unknown";
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private String resolveUsername() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    byte[] decodedPayload = Base64.getUrlDecoder().decode(parts[1]);
                    JsonNode payload = objectMapper.readTree(decodedPayload);
                    JsonNode usernameNode = payload.get("username");
                    if (usernameNode != null && !usernameNode.isNull()) {
                        return usernameNode.asText();
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String toHumanReadable(String action) {
        if (action == null) return "";
        return action.replace('_', ' ');
    }
}
