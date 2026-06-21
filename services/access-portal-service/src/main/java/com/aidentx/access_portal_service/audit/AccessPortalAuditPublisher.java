package com.aidentx.access_portal_service.audit;

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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class AccessPortalAuditPublisher {

    private static final String AUDIT_LOG_TOPIC = "audit-log";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;

    public AccessPortalAuditPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate")
            KafkaTemplate<String, Object> kafkaTemplate,
            UnifiedLoggerFactory loggerFactory,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = loggerFactory.getLogger(
                getClass(),
                "access-portal-audit"
        );
        this.objectMapper = objectMapper;
    }

    public void publish(AccessPortalAuditEvent event) {
        try {
            AuditLogKafkaEvent auditEvent = toStandardEvent(event);

            kafkaTemplate.send(
                    AUDIT_LOG_TOPIC,
                    event.entityId().toString(),
                    auditEvent
            );

            logger.info()
                    .message("Access portal audit event published")
                    .detail("entityType", event.entityType())
                    .detail("action", event.action())
                    .detail("entityId", event.entityId())
                    .log();

        } catch (Exception ex) {
            logger.error(ex)
                    .message("Failed to publish access portal audit event")
                    .detail("action", event.action())
                    .detail("entityId", event.entityId())
                    .log();
        }
    }

    private JsonNode buildTargetObject(AccessPortalAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();

        target.put("type", event.entityType());          // TILE
        target.put("objectId", event.entityId().toString());

        // If stateAfter is TileAuditDTO, extract name & category
        if (event.stateAfter() != null && event.stateAfter().has("title")) {
            target.put("objectName", event.stateAfter().get("title").asText());
            target.put("category", "tile");
        }

        return target;
    }


    private AuditLogKafkaEvent toStandardEvent(AccessPortalAuditEvent event) {
        // Extract object name for description
        String objectName = extractObjectName(event);
        String message = createDescriptiveMessage(event.action(), event.entityType(), objectName, event.stateAfter());

        // Build details with message
        ObjectNode details = objectMapper.createObjectNode();
        details.put("message", message);

        // Extract IP and User Agent
        String clientIp = extractClientIp(event.clientIp());
        String userAgent = extractUserAgent(event.userAgent());

        details.put("ipAddress", clientIp);
        details.put("userAgent", userAgent);

        return new AuditLogKafkaEvent(
                UUID.randomUUID(),
                event.actorId(),
                event.actorType(),
                event.actorName(),
                event.tenantId(),
                event.entityId(),
                event.entityType(),
                event.action(),
                "success",
                event.riskLevel(),
                Instant.now(),
                details,                        // NOW includes message, ipAddress, userAgent
                event.stateBefore(),
                event.stateAfter(),
                buildMetadata(event, clientIp, userAgent),
                null,
                null,
                buildTargetObject(event)
        );
    }

    /**
     * Extract object name from state for description
     */
    private String extractObjectName(AccessPortalAuditEvent event) {
        if (event.stateAfter() != null) {
            if (event.stateAfter().has("title")) {
                return event.stateAfter().get("title").asText();
            }
            if (event.stateAfter().has("itemName")) {
                return event.stateAfter().get("itemName").asText();
            }
            if (event.stateAfter().has("id")) {
                return event.stateAfter().get("id").asText();
            }
        }
        return event.entityType();
    }

    /**
     * Creates a human-readable descriptive message for the audit event
     */
    private String createDescriptiveMessage(String action, String entityType, String objectName, JsonNode stateAfter) {
        return switch (action) {
            case "tile-created" -> "Created tile: " + objectName;
            case "tile-updated" -> "Updated tile: " + objectName;
            case "tile-deleted" -> "Deleted tile: " + objectName;
            case "cart-item-added" -> "Added item to cart: " + objectName;
            case "cart-item-removed" -> "Removed item from cart: " + objectName;
            case "access-request-submitted" -> buildRequestSubmittedMessage(stateAfter);
            case "access-request-withdrawn" -> buildRequestWithdrawnMessage(stateAfter);
            default -> action + " " + entityType + ": " + objectName;
        };
    }

    /**
     * Build user-friendly message for request submission
     * Example: "Submitted access request for 3 roles (System Administrator, Viewer) for 2 users (John Doe, Jane Smith)"
     */
    /**
     * Build user-friendly message for request submission
     * Example: "Submitted access request for 3 roles (System Administrator, Viewer) for 2 users (John Doe, Jane Smith)"
     */
    private String buildRequestSubmittedMessage(JsonNode stateAfter) {
        if (stateAfter == null || !stateAfter.has("items")) {
            return "Submitted access request";
        }

        JsonNode items = stateAfter.get("items");
        int itemCount = items.size();

        // Extract role names and beneficiary info
        List<String> roleNames = new ArrayList<>();
        boolean hasOthersRequest = false;
        Set<String> allBeneficiaryNames = new LinkedHashSet<>(); // Use Set to avoid duplicates

        for (JsonNode item : items) {
            // Extract role name
            if (item.has("itemName")) {
                roleNames.add(item.get("itemName").asText());
            }

            // Check request target
            if (item.has("requestTarget") && "OTHERS".equalsIgnoreCase(item.get("requestTarget").asText())) {
                hasOthersRequest = true;
            }

            // Extract beneficiary names from beneficiaryNames field
            if (item.has("beneficiaryNames")) {
                JsonNode beneficiaryNamesNode = item.get("beneficiaryNames");
                String beneficiaryNamesStr = null;

                // Handle both String and Array types
                if (beneficiaryNamesNode.isTextual()) {
                    beneficiaryNamesStr = beneficiaryNamesNode.asText();
                } else if (beneficiaryNamesNode.isArray()) {
                    // If it's an array, join the elements
                    List<String> namesList = new ArrayList<>();
                    for (JsonNode nameNode : beneficiaryNamesNode) {
                        namesList.add(nameNode.asText());
                    }
                    beneficiaryNamesStr = String.join(",", namesList);
                }

                // Parse comma-separated names
                if (beneficiaryNamesStr != null && !beneficiaryNamesStr.isEmpty()) {
                    String[] nameArray = beneficiaryNamesStr.split(",");
                    for (String name : nameArray) {
                        String trimmedName = name.trim();
                        if (!trimmedName.isEmpty()) {
                            allBeneficiaryNames.add(trimmedName);
                        }
                    }
                }
            }
        }

        // Build message
        StringBuilder message = new StringBuilder("Submitted access request for ");
        message.append(itemCount).append(" role").append(itemCount > 1 ? "s" : "");

        // Add role names (first 2)
        if (!roleNames.isEmpty()) {
            message.append(" (");
            message.append(roleNames.stream().limit(2).collect(Collectors.joining(", ")));
            if (roleNames.size() > 2) {
                message.append(", and ").append(roleNames.size() - 2).append(" more");
            }
            message.append(")");
        }

        // Add beneficiary info
        if (!hasOthersRequest) {
            // All items are for SELF
            message.append(" for self");
        } else if (!allBeneficiaryNames.isEmpty()) {
            // Request is for OTHERS with beneficiary names
            List<String> beneficiaryList = new ArrayList<>(allBeneficiaryNames);
            message.append(" for ").append(beneficiaryList.size())
                    .append(" user").append(beneficiaryList.size() > 1 ? "s" : "");
            message.append(" (");
            message.append(beneficiaryList.stream().limit(2).collect(Collectors.joining(", ")));
            if (beneficiaryList.size() > 2) {
                message.append(", and ").append(beneficiaryList.size() - 2).append(" more");
            }
            message.append(")");
        } else {
            // Request is for OTHERS but no names available
            message.append(" for other users");
        }

        return message.toString();
    }

    /**
     * Build user-friendly message for request withdrawal
     */
    private String buildRequestWithdrawnMessage(JsonNode stateBefore) {
        if (stateBefore == null || !stateBefore.has("items")) {
            return "Withdrew access request";
        }

        JsonNode items = stateBefore.get("items");
        int itemCount = items.size();

        return "Withdrew access request for " + itemCount + " role" + (itemCount > 1 ? "s" : "");
    }

    /**
     * Extract and normalize client IP address
     * Converts ::1 to 127.0.0.1 for localhost
     */
    private String extractClientIp(String clientIp) {
        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                HttpServletRequest req = attr.getRequest();

                // Try X-Forwarded-For header first (for proxied requests)
                String forwardedFor = req.getHeader("X-Forwarded-For");
                if (forwardedFor != null && !forwardedFor.isEmpty()) {
                    clientIp = forwardedFor.split(",")[0].trim();
                } else if (clientIp == null) {
                    clientIp = req.getRemoteAddr();
                }
            }

            // Normalize IPv6 localhost to IPv4
            if ("0:0:0:0:0:0:0:1".equals(clientIp) || "::1".equals(clientIp)) {
                return "127.0.0.1";
            }

            return clientIp != null ? clientIp : "unknown";
        } catch (Exception e) {
            return clientIp != null ? clientIp : "unknown";
        }
    }

    /**
     * Extract user agent from request
     */
    private String extractUserAgent(String userAgent) {
        try {
            if (userAgent != null) {
                return userAgent;
            }

            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                HttpServletRequest req = attr.getRequest();
                userAgent = req.getHeader("User-Agent");
            }

            return userAgent != null ? userAgent : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private JsonNode buildMetadata(AccessPortalAuditEvent event, String clientIp, String userAgent) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put("service", "access-portal-service");
        metadata.put("entityType", event.entityType());

        try {
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                HttpServletRequest req = attr.getRequest();
                metadata.put("requestMethod", req.getMethod());
                metadata.put("requestUri", req.getRequestURI());
                metadata.put("clientIp", clientIp);
                metadata.put("userAgent", userAgent);
            }
        } catch (Exception ignored) {
        }

        return metadata;
    }
}
