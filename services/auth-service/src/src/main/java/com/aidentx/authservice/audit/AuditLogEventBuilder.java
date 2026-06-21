package com.aidentx.authservice.audit;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.authservice.config.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.UUID;

/**
 * Builder utility for creating standardized AuditLogKafkaEvent instances for
 * various types of audit events (authentication, configuration, system events,
 * etc.).
 * Centralizes and simplifies audit event construction for production-grade
 * code.
 * <p>
 * Provides both generic methods for any type of audit event and convenience
 * methods
 * for specific event types like user authentication and configuration changes.
 */
public class AuditLogEventBuilder {

    // ===================================================================================
    // Constants
    // ===================================================================================

    // --- JSON Field Keys ---
    private static final String KEY_IP_ADDRESS = "ipAddress";
    private static final String KEY_USER_AGENT = "userAgent";
    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_OWNER = "owner";
    private static final String KEY_DEPARTMENT = "department";
    private static final String KEY_TYPE = "type";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_EVENT_TYPE = "eventType";
    private static final String KEY_OBJECT_ID = "objectId";
    private static final String KEY_OBJECT_NAME = "objectName";

    // ===================================================================================
    // Member Variables
    // ===================================================================================

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AuditLogEventBuilder() {
    }

    /**
     * Creates a general AuditLogKafkaEvent for any audit event.
     *
     * @param userId         The user ID (can be null)
     * @param actorType      The actor type (user, system, admin, etc.)
     * @param actorName      The actor name (username, system name, etc.) (can be
     *                       null)
     * @param objectId       The object ID - UUID of the target object (can be
     *                       null)
     * @param objectType     The object type (user, session, token, configuration,
     *                       etc.)
     * @param identifier     The identifier (username/email for users, name for
     *                       other objects)
     * @param eventType      The event type (LOGIN_EVENT, CONFIG_UPDATE, etc.)
     * @param eventOutcome   The outcome (SUCCESS_EVENT_TYPE, FAILURE_EVENT_TYPE)
     * @param riskLevel      The risk level (EVENT_RISK_LOW, EVENT_RISK_MEDIUM,
     *                       etc.)
     * @param clientIp       The client IP address
     * @param userAgent      The user agent string
     * @param message        The event message
     * @param stateBefore    The state before the event (can be null)
     * @param stateAfter     The state after the event (can be null)
     * @param customDetails  Additional custom details (can be null)
     * @param customMetadata Additional custom metadata (can be null)
     * @return AuditLogKafkaEvent for the event
     */
    public static AuditLogKafkaEvent createEvent(UUID userId, String actorType, String actorName, UUID objectId, String objectType, String identifier, String eventType, String eventOutcome, String riskLevel, String clientIp, String userAgent, String message, ObjectNode stateBefore, ObjectNode stateAfter, ObjectNode customDetails, ObjectNode customMetadata) {
        // For backward compatibility, use unknown tenant ID
        return createEventWithTenant(userId, actorType, actorName, AppConstants.UNKNOWN_TENANT_ID, objectId, objectType, identifier, eventType, eventOutcome, riskLevel, clientIp, userAgent, message, stateBefore, stateAfter, customDetails, customMetadata);
    }

    /**
     * Creates a general AuditLogKafkaEvent for any audit event with tenantId.
     *
     * @param userId         The user ID (can be null)
     * @param actorType      The actor type (user, system, admin, etc.)
     * @param actorName      The actor name (username, system name, etc.) (can be
     *                       null)
     * @param tenantId       The tenant ID
     * @param objectId       The object ID - UUID of the target object (can be
     *                       null)
     * @param objectType     The object type (user, session, token, configuration,
     *                       etc.)
     * @param identifier     The identifier (username/email for users, name for
     *                       other objects)
     * @param eventType      The event type (LOGIN_EVENT, CONFIG_UPDATE, etc.)
     * @param eventOutcome   The outcome (SUCCESS_EVENT_TYPE, FAILURE_EVENT_TYPE)
     * @param riskLevel      The risk level (EVENT_RISK_LOW, EVENT_RISK_MEDIUM,
     *                       etc.)
     * @param clientIp       The client IP address
     * @param userAgent      The user agent string
     * @param message        The event message
     * @param stateBefore    The state before the event (can be null)
     * @param stateAfter     The state after the event (can be null)
     * @param customDetails  Additional custom details (can be null)
     * @param customMetadata Additional custom metadata (can be null)
     * @return AuditLogKafkaEvent for the event
     */
    public static AuditLogKafkaEvent createEventWithTenant(UUID userId, String actorType, String actorName, String tenantId, UUID objectId, String objectType, String identifier, String eventType, String eventOutcome, String riskLevel, String clientIp, String userAgent, String message, ObjectNode stateBefore, ObjectNode stateAfter, ObjectNode customDetails, ObjectNode customMetadata) {

        ObjectNode details = objectMapper.createObjectNode();
        details.put(KEY_IP_ADDRESS, clientIp);
        details.put(KEY_USER_AGENT, userAgent);
        details.put(KEY_IDENTIFIER, identifier);
        details.put(KEY_MESSAGE, message);

        // Add custom details if provided
        if (customDetails != null) {
            customDetails.fieldNames().forEachRemaining(fieldName -> details.set(fieldName, customDetails.get(fieldName)));
        }

        ObjectNode governance = objectMapper.createObjectNode();
        governance.set(KEY_OWNER, NullNode.getInstance());
        governance.set(KEY_DEPARTMENT, NullNode.getInstance());

        ObjectNode targetObject = objectMapper.createObjectNode();
        /**
         *
         * {
         * type: objectType,
         * objectId: objectId
         * if User:
         * username: identifier,
         * else:
         * objectName: identifier
         * }
         *
         */
        targetObject.put(KEY_TYPE, objectType);
        if (objectId != null) {
            targetObject.put(KEY_OBJECT_ID, objectId.toString());
        }
        if (identifier != null) {
            targetObject.put(KEY_OBJECT_NAME, identifier);
        }

        ObjectNode finalStateBefore = stateBefore != null ? stateBefore : objectMapper.createObjectNode();
        ObjectNode finalStateAfter = stateAfter != null ? stateAfter : objectMapper.createObjectNode();

        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put(KEY_USERNAME, identifier);
        metadata.put(KEY_EVENT_TYPE, eventOutcome);

        // Add custom metadata if provided
        if (customMetadata != null) {
            customMetadata.fieldNames().forEachRemaining(fieldName -> metadata.set(fieldName, customMetadata.get(fieldName)));
        }

        return new AuditLogKafkaEvent(userId, // actorId (UUID) - who performed the action
                actorType, // actorType (String) - configurable
                actorName, // actorName (String) - configurable
                tenantId, // tenantId (String) - tenant identifier
                objectId, // objectId (UUID) - the target of the action (now configurable)
                objectType, // objectType (String) - configurable
                eventType.toLowerCase(), // event (String)
                eventOutcome.toLowerCase(), // status (String)
                riskLevel.toLowerCase(), // risk (String)
                Instant.now(), // timestamp (Instant)
                details, // details (JsonNode)
                finalStateBefore, // stateBefore (JsonNode)
                finalStateAfter, // stateAfter (JsonNode)
                metadata, // metadata (JsonNode)
                eventOutcome.equalsIgnoreCase(AppConstants.SUCCESS_EVENT_TYPE) ? null : message, // errorDetails
                // (String) - only
                // for failures
                governance, // governance (JsonNode)
                targetObject // targetObject (JsonNode)
        );
    }
}