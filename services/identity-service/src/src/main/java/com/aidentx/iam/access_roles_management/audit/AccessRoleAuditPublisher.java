package com.aidentx.iam.access_roles_management.audit;

import java.util.Map;
import java.util.UUID;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.iam.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Publishes access role audit events to Kafka.
 *
 * <p>Converts {@link AccessRoleAuditEvent} instances into the standard
 * {@link AuditLogKafkaEvent} format and sends them to the configured audit
 * topic. Uses the role ID as the Kafka partition key to ensure all audit
 * events for the same role land on the same partition (ordered trail).</p>
 *
 * <p>All exceptions are caught and logged — audit publishing failures
 * never propagate to the caller and never break business operations.</p>
 */
@Service
public class AccessRoleAuditPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    /**
     * Constructs the publisher with Kafka, serialization, and logging dependencies.
     *
     * @param kafkaTemplate the Kafka template configured for audit events
     * @param objectMapper  Jackson object mapper for JSON construction
     * @param loggerFactory unified logger factory
     */
    public AccessRoleAuditPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAuditConstants.LOG_CATEGORY);
    }

    /**
     * Publishes an access role audit event to Kafka.
     *
     * <p>Converts the event to the standard {@link AuditLogKafkaEvent} format,
     * then sends it to the audit topic with the role ID as the partition key.</p>
     *
     * @param event the audit event to publish
     */
    public void publish(AccessRoleAuditEvent event) {
        try {

            String dynamicEntityType = AccessRoleAuditConstants.toEntityType(event.roleType());

            logger.info()
                    .category(AccessRoleAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditConstants.LOG_PUBLISHING_EVENT)
                    .details(Map.of(
                            AccessRoleAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleAuditConstants.METADATA_ENTITY_TYPE, dynamicEntityType,
                            AccessRoleAuditConstants.DETAIL_ROLE_ID,
                            safe(event.entityId()),
                            AccessRoleAuditConstants.METADATA_TENANT_ID,
                            safe(event.tenantId())
                    ))
                    .log();

            AuditLogKafkaEvent standardEvent = convertToStandardEvent(event);

            String partitionKey = event.entityId() != null ? event.entityId() : event.tenantId();
            kafkaTemplate.send(AppConstants.AUDIT_TOPIC_NAME, partitionKey, standardEvent);



            logger.info()
                    .category(AccessRoleAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditConstants.LOG_EVENT_PUBLISHED)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME,
                            AccessRoleAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleAuditConstants.DETAIL_ROLE_ID, safe(event.entityId())
                    ))
                    .log();

        } catch (Exception e) {
            logger.error()
                    .category(AccessRoleAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAuditConstants.LOG_EVENT_PUBLISH_FAILED)
                    .details(Map.of(
                            AccessRoleAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleAuditConstants.METADATA_ENTITY_TYPE, event.entityType(),
                            AccessRoleAuditConstants.DETAIL_ROLE_ID, safe(event.entityId()),
                            AccessRoleAuditConstants.DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    // ===================================================================
    //  Conversion to Standard Audit Event
    // ===================================================================


    /**
     * Converts an {@link AccessRoleAuditEvent} to the standard
     * {@link AuditLogKafkaEvent} consumed by the central audit service.
     *
     * <p>Key decisions:</p>
     * <ul>
     *   <li><strong>objectType</strong>: dynamically resolved from role type
     *       (e.g. "Business Role", "Technical Role")</li>
     *   <li><strong>action</strong>: uses the UI-friendly activity label
     *       (e.g. "Business Role Created") resolved via
     *       {@link AccessRoleAuditConstants#toActivityLabel}</li>
     * </ul>
     *
     * @param event the access role audit event
     * @return the standard Kafka audit event
     */
    private AuditLogKafkaEvent convertToStandardEvent(AccessRoleAuditEvent event) {
        UUID objectId = parseUuidSafe(event.entityId());
        UUID actorId = parseUuidSafe(event.actorId());

        String actorType = AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(event.actorId())
                ? AppConstants.AUDIT_ACTOR_TYPE_SYSTEM
                : AppConstants.AUDIT_ACTOR_TYPE_USER;

        String activityLabel = AccessRoleAuditConstants.toActivityLabel(
                event.action(), event.roleType());

        String dynamicEntityType = AccessRoleAuditConstants.toEntityType(event.roleType());

        return new AuditLogKafkaEvent(
                actorId,
                actorType,
                event.actorUsername(),
                event.tenantId(),
                objectId,
                dynamicEntityType,
                activityLabel,
                AppConstants.AUDIT_STATUS_SUCCESS,
                resolveRisk(event),
                event.timestamp(),
                buildDetails(event),
                event.stateBefore(),
                event.stateAfter(),
                buildMetadata(event),
                null,
                null,
                buildTargetObject(event)
        );
    }
    // ===================================================================
    //  Details Builder
    // ===================================================================

    /**
     * Builds the details JSON node for the audit event.
     *
     * <p>For role CRUD operations, includes role metadata fields.
     * For child/entitlement operations, includes rich detail arrays
     * with names, types, and risk levels of affected entities.</p>
     *
     * @param event the audit event
     * @return the details JSON node, or null if empty
     */
    private JsonNode buildDetails(AccessRoleAuditEvent event) {
        ObjectNode details = objectMapper.createObjectNode();

        // Common role fields
        putIfNotNull(details, AccessRoleAuditConstants.DETAIL_ROLE_NAME, event.entityName());
        putIfNotNull(details, AccessRoleAuditConstants.DETAIL_ROLE_TYPE, event.roleType());

        // Role CRUD specific fields
        putIfNotNull(details, AccessRoleAuditConstants.DETAIL_STATUS, event.status());
        putIfNotNull(details, AccessRoleAuditConstants.DETAIL_RISK_LEVEL, event.riskLevel());
        putIfNotNull(details, AccessRoleAuditConstants.DETAIL_BUSINESS_UNIT, event.businessUnit());
        putIfNotNull(details, AccessRoleAuditConstants.DETAIL_OWNER_ID, event.ownerId());

        // Added children with full details
        if (event.addedChildren() != null && !event.addedChildren().isEmpty()) {
            ArrayNode arr = details.putArray(AccessRoleAuditConstants.DETAIL_ADDED_CHILDREN);
            for (AccessRoleAuditEvent.ChildRoleAuditDetail child : event.addedChildren()) {
                ObjectNode childNode = objectMapper.createObjectNode();
                childNode.put("id", child.id());
                childNode.put("name", child.name());
                childNode.put("type", child.type());
                putIfNotNull(childNode, "riskLevel", child.riskLevel());
                arr.add(childNode);
            }
            details.put(AccessRoleAuditConstants.DETAIL_ADDED_COUNT, event.addedChildren().size());
            putIfNotNull(details, AccessRoleAuditConstants.DETAIL_HIGHEST_CHILD_RISK, event.riskLevel());
        }

        // Failed children with full details and reason
        if (event.failedChildren() != null && !event.failedChildren().isEmpty()) {
            ArrayNode arr = details.putArray(AccessRoleAuditConstants.DETAIL_FAILED_CHILDREN);
            for (AccessRoleAuditEvent.FailedChildRoleAuditDetail child : event.failedChildren()) {
                ObjectNode childNode = objectMapper.createObjectNode();
                childNode.put("id", child.id());
                childNode.put("name", child.name());
                childNode.put("type", child.type());
                putIfNotNull(childNode, "riskLevel", child.riskLevel());
                childNode.put("reason", child.reason());
                arr.add(childNode);
            }
            details.put(AccessRoleAuditConstants.DETAIL_FAILED_COUNT, event.failedChildren().size());
        }

        // Removed children with full details
        if (event.removedChildren() != null && !event.removedChildren().isEmpty()) {
            ArrayNode arr = details.putArray(AccessRoleAuditConstants.DETAIL_REMOVED_CHILDREN);
            for (AccessRoleAuditEvent.ChildRoleAuditDetail child : event.removedChildren()) {
                ObjectNode childNode = objectMapper.createObjectNode();
                childNode.put("id", child.id());
                childNode.put("name", child.name());
                childNode.put("type", child.type());
                putIfNotNull(childNode, "riskLevel", child.riskLevel());
                arr.add(childNode);
            }
            details.put(AccessRoleAuditConstants.DETAIL_REMOVED_COUNT, event.removedChildren().size());
            putIfNotNull(details, AccessRoleAuditConstants.DETAIL_HIGHEST_CHILD_RISK, event.riskLevel());
        }

        // Added entitlements with full details
        if (event.addedEntitlements() != null && !event.addedEntitlements().isEmpty()) {
            ArrayNode arr = details.putArray(AccessRoleAuditConstants.DETAIL_ADDED_ENTITLEMENTS);
            for (AccessRoleAuditEvent.EntitlementAuditDetail ent : event.addedEntitlements()) {
                ObjectNode entNode = objectMapper.createObjectNode();
                entNode.put("id", ent.id());
                entNode.put("name", ent.name());
                putIfNotNull(entNode, "riskLevel", ent.riskLevel());
                arr.add(entNode);
            }
            details.put(AccessRoleAuditConstants.DETAIL_ADDED_COUNT, event.addedEntitlements().size());
            putIfNotNull(details, AccessRoleAuditConstants.DETAIL_HIGHEST_ENTITLEMENT_RISK, event.riskLevel());
        }

        // Removed entitlements with full details
        if (event.removedEntitlements() != null && !event.removedEntitlements().isEmpty()) {
            ArrayNode arr = details.putArray(AccessRoleAuditConstants.DETAIL_REMOVED_ENTITLEMENTS);
            for (AccessRoleAuditEvent.EntitlementAuditDetail ent : event.removedEntitlements()) {
                ObjectNode entNode = objectMapper.createObjectNode();
                entNode.put("id", ent.id());
                entNode.put("name", ent.name());
                putIfNotNull(entNode, "riskLevel", ent.riskLevel());
                arr.add(entNode);
            }
            details.put(AccessRoleAuditConstants.DETAIL_REMOVED_COUNT, event.removedEntitlements().size());
            putIfNotNull(details, AccessRoleAuditConstants.DETAIL_HIGHEST_ENTITLEMENT_RISK, event.riskLevel());
        }

        return details.isEmpty() ? null : details;
    }

    // ===================================================================
    //  Metadata Builder
    // ===================================================================

    /**
     * Builds the metadata JSON node for the audit event.
     *
     * @param event the audit event
     * @return the metadata JSON node
     */
    private JsonNode buildMetadata(AccessRoleAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();

        String dynamicEntityType = AccessRoleAuditConstants.toEntityType(event.roleType());

        metadata.put(AccessRoleAuditConstants.METADATA_SOURCE, AccessRoleAuditConstants.METADATA_SOURCE_VALUE);
        metadata.put(AccessRoleAuditConstants.METADATA_TENANT_ID, event.tenantId());
        metadata.put(AccessRoleAuditConstants.METADATA_ENTITY_TYPE, dynamicEntityType);
        metadata.put(AccessRoleAuditConstants.DETAIL_ACTION, event.action());

        putIfNotNull(metadata, AccessRoleAuditConstants.METADATA_ROLE_ID, event.entityId());
        putIfNotNull(metadata, AccessRoleAuditConstants.METADATA_ROLE_NAME, event.entityName());
        putIfNotNull(metadata, AccessRoleAuditConstants.METADATA_RISK_LEVEL, event.riskLevel());

        return metadata;
    }

    // ===================================================================
    //  Target Object Builder
    // ===================================================================

    /**
     * Builds the target object JSON node identifying the audited entity.
     *
     * @param event the audit event
     * @return the target object JSON node
     */
    private JsonNode buildTargetObject(AccessRoleAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();

        String dynamicEntityType = AccessRoleAuditConstants.toEntityType(event.roleType());

        target.put(AccessRoleAuditConstants.TARGET_TYPE, dynamicEntityType);
        target.put(AccessRoleAuditConstants.TARGET_OBJECT_ID, safe(event.entityId()));
        target.put(AccessRoleAuditConstants.TARGET_OBJECT_NAME, safe(event.entityName()));

        return target;
    }

    // ===================================================================
    //  Risk Resolution
    // ===================================================================

    /**
     * Resolves the risk level string for the Kafka audit event.
     *
     * <p>Risk hierarchy: high &gt; medium &gt; low/null.</p>
     * <ul>
     *   <li>If the event's risk level is "high", audit risk is "high".</li>
     *   <li>If the event's risk level is "medium", audit risk is "medium".</li>
     *   <li>Otherwise, audit risk is "low".</li>
     * </ul>
     *
     * @param event the audit event
     * @return the audit risk string: "high", "medium", or "low"
     */
    private String resolveRisk(AccessRoleAuditEvent event) {

        String risk = event.riskLevel();
        if (risk != null) {
            String lower = risk.toLowerCase();
            if ("high".equals(lower)) {
                return AppConstants.AUDIT_RISK_HIGH;
            }
            if ("medium".equals(lower)) {
                return AppConstants.AUDIT_RISK_MEDIUM;
            }
        }

        return AppConstants.AUDIT_RISK_LOW;
    }

    // ===================================================================
    //  Utility Helpers
    // ===================================================================

    /**
     * Safely parses a UUID string. Returns a nil UUID if the input is null,
     * blank, or the system actor ID.
     *
     * @param uuidStr the string to parse
     * @return the parsed UUID, or a nil UUID as fallback
     */
    private UUID parseUuidSafe(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()
                || AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(uuidStr)) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }

    /**
     * Returns the value if non-null, otherwise the "N/A" placeholder.
     *
     * @param value the value to check
     * @return the value or "N/A"
     */
    private String safe(String value) {
        return value != null ? value : AccessRoleAuditConstants.NOT_AVAILABLE;
    }

    /**
     * Puts a string value into an ObjectNode only if non-null.
     *
     * @param node  the target JSON object
     * @param key   the field name
     * @param value the value (may be null)
     */
    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null) {
            node.put(key, value);
        }
    }
}