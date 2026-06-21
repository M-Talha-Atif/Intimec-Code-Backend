package com.aidentx.iam.entitlement_management.audit;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.iam.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Publishes {@link EntitlementRuleAuditEvent} instances to Kafka as
 * standardised {@link AuditLogKafkaEvent} messages.
 *
 * <p>This publisher is the single exit point for all entitlement rule audit
 * events. It converts the domain-specific event record into the platform's
 * standard audit schema, enriches it with details, metadata, and target
 * object context, and sends it to the configured Kafka audit topic.
 *
 * <p>The human-readable {@code description} field from the event record is
 * included in both {@code details} and {@code stateAfter} (for evaluation
 * events) so that it is available to the audit UI regardless of which
 * field the frontend chooses to render.
 *
 * <h3>Failure handling</h3>
 * <p>All exceptions during publishing are caught and logged they never
 * propagate to the caller. This guarantees that audit infrastructure
 * failures cannot break business operations.
 *
 * @see EntitlementRuleAuditEvent
 * @see EntitlementRuleAuditAspect
 */
@Service
public class EntitlementRuleAuditPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    /**
     * Constructs the publisher with Kafka, serialisation, and logging dependencies.
     *
     * @param kafkaTemplate the Kafka template configured for the audit schema registry
     * @param objectMapper  the Jackson object mapper for JSON node construction
     * @param loggerFactory the unified logger factory for structured logging
     */
    public EntitlementRuleAuditPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AppConstants.AUDIT_LOG_CATEGORY);
    }

    /**
     * Converts and publishes an entitlement rule audit event to Kafka.
     *
     * <p>The event is converted to the platform's standard
     * {@link AuditLogKafkaEvent} schema and sent to the configured audit
     * topic. The entity ID (or tenant ID as fallback) is used as the
     * Kafka partition key for ordering guarantees.
     *
     * @param event the domain audit event to publish
     */
    public void publish(EntitlementRuleAuditEvent event) {
        try {
            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_PUBLISHING_ENTITLEMENT_EVENT)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_ACTION, event.action(),
                            AppConstants.AUDIT_DETAIL_ENTITY_TYPE, event.entityType(),
                            AppConstants.AUDIT_DETAIL_ENTITY_ID,
                                    event.entityId() != null ? event.entityId() : AppConstants.AUDIT_NOT_AVAILABLE,
                            AppConstants.AUDIT_METADATA_TENANT_ID,
                                    event.tenantId() != null ? event.tenantId() : AppConstants.AUDIT_NOT_AVAILABLE
                    ))
                    .log();

            AuditLogKafkaEvent standardEvent = convertToStandardEvent(event);

            String partitionKey = event.entityId() != null ? event.entityId() : event.tenantId();
            kafkaTemplate.send(AppConstants.AUDIT_TOPIC_NAME, partitionKey, standardEvent);

            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_ENTITLEMENT_EVENT_PUBLISHED)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME,
                            AppConstants.AUDIT_DETAIL_ACTION, event.action(),
                            AppConstants.AUDIT_DETAIL_ENTITY_ID,
                                    event.entityId() != null ? event.entityId() : AppConstants.AUDIT_NOT_AVAILABLE
                    ))
                    .log();

        } catch (Exception e) {
            logger.error()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_ENTITLEMENT_EVENT_FAILED)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_ACTION, event.action(),
                            AppConstants.AUDIT_DETAIL_ENTITY_TYPE, event.entityType(),
                            AppConstants.AUDIT_DETAIL_ENTITY_ID,
                                    event.entityId() != null ? event.entityId() : AppConstants.AUDIT_NOT_AVAILABLE,
                            AppConstants.AUDIT_DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    /**
     * Converts a domain audit event to the platform's standard Kafka event schema.
     *
     * <p>For evaluation events ({@code RULE_EVALUATED}, {@code ENTITLEMENT_ASSIGNED},
     * {@code ENTITLEMENT_REVOKED}), the {@code stateAfter} is replaced with a
     * structured evaluation summary since there is no entity state change.
     *
     * @param event the domain audit event
     * @return the standardised Kafka audit event
     */
    private AuditLogKafkaEvent convertToStandardEvent(EntitlementRuleAuditEvent event) {
        UUID objectId = parseUuidSafe(event.entityId());
        UUID actorId = parseUuidSafe(event.actorId());

        String actorType = AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(event.actorId())
                ? AppConstants.AUDIT_ACTOR_TYPE_SYSTEM
                : AppConstants.AUDIT_ACTOR_TYPE_USER;

        // For evaluation events, put summary into stateAfter since there's no state change
        JsonNode stateBefore = event.stateBefore();
        JsonNode stateAfter = event.stateAfter();

        if (isEvaluationEvent(event.action())) {
            stateAfter = buildEvaluationSummary(event);
        }

        return new AuditLogKafkaEvent(
                actorId,
                actorType,
                event.actorUsername(),
                event.tenantId(),
                objectId,
                event.entityType(),
                event.action(),
                AppConstants.AUDIT_STATUS_SUCCESS,
                resolveRisk(event.action()),
                event.timestamp(),
                buildDetails(event),
                stateBefore,
                stateAfter,
                buildMetadata(event),
                null,
                null,
                buildTargetObject(event)
        );
    }

    /**
     * Checks if the action is an evaluation-related event.
     *
     * @param action the audit action string
     * @return {@code true} if the action is an evaluation, assignment, or revocation event
     */
    private boolean isEvaluationEvent(String action) {
        return AppConstants.AUDIT_EVENT_RULE_EVALUATED.equals(action)
                || AppConstants.AUDIT_EVENT_ENTITLEMENT_ASSIGNED.equals(action)
                || AppConstants.AUDIT_EVENT_ENTITLEMENT_REVOKED.equals(action);
    }

    /**
     * Builds an evaluation summary as {@code stateAfter} for evaluation events.
     *
     * <p>This ensures the evaluation context (metrics, identity info, description)
     * is visible in the audit UI even if metadata is not rendered. The description
     * is placed first for immediate visibility.
     *
     * @param event the audit event containing evaluation metrics
     * @return a JSON node with the evaluation summary, or {@code null} if empty
     */
    private JsonNode buildEvaluationSummary(EntitlementRuleAuditEvent event) {
        ObjectNode summary = objectMapper.createObjectNode();

        if (event.ruleName() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_RULE_NAME, event.ruleName());
        }
        if (event.entitlementName() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_ENTITLEMENT_NAME, event.entitlementName());
        }
        if (event.triggerSource() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_TRIGGER_SOURCE, event.triggerSource());
        }
        if (event.executionId() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_EXECUTION_ID, event.executionId());
        }

        // Evaluation metrics
        if (event.identitiesMatched() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_IDENTITIES_MATCHED, event.identitiesMatched());
        }
        if (event.assignmentsCreated() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_ASSIGNMENTS_CREATED, event.assignmentsCreated());
        }
        if (event.assignmentsRevoked() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_ASSIGNMENTS_REVOKED, event.assignmentsRevoked());
        }
        if (event.skippedNoShadow() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_SKIPPED_NO_SHADOW, event.skippedNoShadow());
        }
        if (event.durationMs() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_DURATION_MS, event.durationMs());
        }

        // Assignment/revocation identity context
        if (event.identityId() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_IDENTITY_ID, event.identityId());
        }
        if (event.identityName() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_IDENTITY_NAME, event.identityName());
        }
        if (event.shadowId() != null) {
            summary.put(AppConstants.AUDIT_DETAIL_SHADOW_ID, event.shadowId());
        }

        return summary.isEmpty() ? null : summary;
    }

    /**
     * Builds the details JSON node for the standard audit event.
     *
     * <p>Contains the human-readable description, common entity context,
     * evaluation metrics, and assignment/revocation identity context.
     * The description is placed first so it appears at the top of the
     * details section in audit UIs.
     *
     * @param event the audit event
     * @return a JSON node with event details, or {@code null} if empty
     */
    private JsonNode buildDetails(EntitlementRuleAuditEvent event) {
        ObjectNode details = objectMapper.createObjectNode();

        // Description first human-readable summary for UI display
        if (event.description() != null) {
            details.put(AppConstants.AUDIT_DETAIL_MESSAGE, event.description());
        }

        // Common context
        if (event.entityName() != null) {
            details.put(AppConstants.AUDIT_DETAIL_RULE_NAME, event.entityName());
        }
        if (event.entitlementName() != null) {
            details.put(AppConstants.AUDIT_DETAIL_ENTITLEMENT_NAME, event.entitlementName());
        }
        if (event.entitlementId() != null) {
            details.put(AppConstants.AUDIT_DETAIL_ENTITLEMENT_ID, event.entitlementId());
        }
        if (event.resourceId() != null) {
            details.put(AppConstants.AUDIT_DETAIL_RESOURCE_ID, event.resourceId());
        }
        if (event.triggerSource() != null) {
            details.put(AppConstants.AUDIT_DETAIL_TRIGGER_SOURCE, event.triggerSource());
        }

        // Evaluation context
        if (event.executionId() != null) {
            details.put(AppConstants.AUDIT_DETAIL_EXECUTION_ID, event.executionId());
        }
        if (event.identitiesMatched() != null) {
            details.put(AppConstants.AUDIT_DETAIL_IDENTITIES_MATCHED, event.identitiesMatched());
        }
        if (event.assignmentsCreated() != null) {
            details.put(AppConstants.AUDIT_DETAIL_ASSIGNMENTS_CREATED, event.assignmentsCreated());
        }
        if (event.assignmentsRevoked() != null) {
            details.put(AppConstants.AUDIT_DETAIL_ASSIGNMENTS_REVOKED, event.assignmentsRevoked());
        }
        if (event.skippedNoShadow() != null) {
            details.put(AppConstants.AUDIT_DETAIL_SKIPPED_NO_SHADOW, event.skippedNoShadow());
        }
        if (event.durationMs() != null) {
            details.put(AppConstants.AUDIT_DETAIL_DURATION_MS, event.durationMs());
        }

        // Assignment/Revocation context ruleId when not the primary entity
        if (event.ruleId() != null) {
            details.put(AppConstants.AUDIT_DETAIL_RULE_ID, event.ruleId());
        }
        if (event.ruleName() != null) {
            details.put(AppConstants.AUDIT_DETAIL_RULE_NAME, event.ruleName());
        }
        if (event.identityId() != null) {
            details.put(AppConstants.AUDIT_DETAIL_IDENTITY_ID, event.identityId());
        }
        if (event.identityName() != null) {
            details.put(AppConstants.AUDIT_DETAIL_IDENTITY_NAME, event.identityName());
        }
        if (event.shadowId() != null) {
            details.put(AppConstants.AUDIT_DETAIL_SHADOW_ID, event.shadowId());
        }
        if (event.identityTypes() != null && !event.identityTypes().isEmpty()) {
            details.set(AppConstants.AUDIT_DETAIL_IDENTITY_TYPES, objectMapper.valueToTree(event.identityTypes()));
        }

        return details.isEmpty() ? null : details;
    }

    /**
     * Builds the metadata JSON node for the standard audit event.
     *
     * <p>Contains system-level context: source service, tenant, entity type,
     * action, and all relevant IDs for cross-referencing and filtering in
     * the audit UI.
     *
     * @param event the audit event
     * @return a JSON node with event metadata (never null)
     */
    private JsonNode buildMetadata(EntitlementRuleAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();

        metadata.put(AppConstants.AUDIT_METADATA_SOURCE, AppConstants.AUDIT_SOURCE_ENTITLEMENT_SERVICE);
        metadata.put(AppConstants.AUDIT_METADATA_TENANT_ID, event.tenantId());
        metadata.put(AppConstants.AUDIT_METADATA_ENTITY_TYPE, event.entityType());
        metadata.put(AppConstants.AUDIT_DETAIL_ACTION, event.action());

        // Description in metadata for full-text search capability
        if (event.description() != null) {
            metadata.put(AppConstants.AUDIT_DETAIL_MESSAGE, event.description());
        }

        if (event.triggerSource() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_TRIGGER_SOURCE, event.triggerSource());
        }
        if (event.entitlementId() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_ENTITLEMENT_ID, event.entitlementId());
        }
        if (event.entitlementName() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_ENTITLEMENT_NAME, event.entitlementName());
        }
        if (event.resourceId() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_RESOURCE_ID, event.resourceId());
        }
        if (event.identityTypes() != null && !event.identityTypes().isEmpty()) {
            metadata.set(AppConstants.AUDIT_METADATA_IDENTITY_TYPES, objectMapper.valueToTree(event.identityTypes()));
        }
        if (event.ruleId() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_RULE_ID, event.ruleId());
        }
        if (event.ruleName() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_RULE_NAME, event.ruleName());
        }
        if (event.identityId() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_IDENTITY_ID, event.identityId());
        }
        if (event.identityName() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_IDENTITY_NAME, event.identityName());
        }
        if (event.shadowId() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_SHADOW_ID, event.shadowId());
        }
        if (event.executionId() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_EXECUTION_ID, event.executionId());
        }
        if (event.identitiesMatched() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_IDENTITIES_MATCHED, event.identitiesMatched());
        }
        if (event.assignmentsCreated() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_ASSIGNMENTS_CREATED, event.assignmentsCreated());
        }
        if (event.assignmentsRevoked() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_ASSIGNMENTS_REVOKED, event.assignmentsRevoked());
        }
        if (event.skippedNoShadow() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_SKIPPED_NO_SHADOW, event.skippedNoShadow());
        }
        if (event.durationMs() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_DURATION_MS, event.durationMs());
        }

        return metadata;
    }

    /**
     * Builds the target object JSON node identifying what entity was affected.
     *
     * @param event the audit event
     * @return a JSON node with the target object context
     */
    private JsonNode buildTargetObject(EntitlementRuleAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();

        target.put(AppConstants.AUDIT_TARGET_TYPE, event.entityType());
        target.put(AppConstants.AUDIT_TARGET_OBJECT_ID,
                event.entityId() != null ? event.entityId() : AppConstants.AUDIT_NOT_AVAILABLE);
        target.put(AppConstants.AUDIT_TARGET_OBJECT_NAME,
                event.entityName() != null ? event.entityName() : AppConstants.AUDIT_NOT_AVAILABLE);

        if (event.entitlementId() != null) {
            target.put(AppConstants.AUDIT_METADATA_ENTITLEMENT_ID, event.entitlementId());
        }
        if (event.resourceId() != null) {
            target.put(AppConstants.AUDIT_METADATA_RESOURCE_ID, event.resourceId());
        }

        return target;
    }

    /**
     * Resolves the risk level for an audit action.
     *
     * <p>Risk classification:
     * <ul>
     *   <li><b>High</b>: rule deletion, entitlement revocation</li>
     *   <li><b>Medium</b>: rule creation, entitlement assignment, global schedule update</li>
     *   <li><b>Low</b>: all other actions (updates, evaluations, toggles)</li>
     * </ul>
     *
     * @param action the audit action string
     * @return the risk level constant
     */
    private String resolveRisk(String action) {
        if (AppConstants.AUDIT_EVENT_RULE_DELETED.equals(action)
                || AppConstants.AUDIT_EVENT_ENTITLEMENT_REVOKED.equals(action)) {
            return AppConstants.AUDIT_RISK_HIGH;
        }
        if (AppConstants.AUDIT_EVENT_RULE_CREATED.equals(action)
                || AppConstants.AUDIT_EVENT_ENTITLEMENT_ASSIGNED.equals(action)
                || AppConstants.AUDIT_EVENT_GLOBAL_SCHEDULE_UPDATED.equals(action)) {
            return AppConstants.AUDIT_RISK_MEDIUM;
        }
        return AppConstants.AUDIT_RISK_LOW;
    }

    /**
     * Safely parses a UUID string, returning a nil UUID for null, blank,
     * or non-parseable values.
     *
     * @param uuidStr the UUID string to parse
     * @return the parsed UUID, or {@code 00000000-0000-0000-0000-000000000000} on failure
     */
    private UUID parseUuidSafe(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank()
                || AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(uuidStr)
                || AppConstants.AUDIT_GLOBAL_ENTITY_ID.equals(uuidStr)) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }
}