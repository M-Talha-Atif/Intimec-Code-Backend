package com.aidentx.iam.access_roles_management.audit;

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

import java.util.Map;
import java.util.UUID;

/**
 * Publishes access-role assignment rule audit events to Kafka.
 *
 * <p>This publisher converts domain-specific
 * {@link AccessRoleAssignmentRuleAuditEvent} records into the platform-standard
 * {@link AuditLogKafkaEvent} schema consumed by the central audit service.</p>
 */
@Service
public class AccessRoleAssignmentRuleAuditPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public AccessRoleAssignmentRuleAuditPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY);
    }

    /**
     * Publishes an access-role assignment rule audit event to Kafka.
     *
     * @param event audit event to publish
     */
    public void publish(AccessRoleAssignmentRuleAuditEvent event) {
        try {
            String dynamicEntityType = AccessRoleAssignmentRuleAuditConstants.toEntityType(event.roleType());

            logger.info()
                    .category(AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleAuditConstants.LOG_PUBLISHING_EVENT)
                    .details(Map.of(
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleAssignmentRuleAuditConstants.METADATA_ENTITY_TYPE, dynamicEntityType,
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_RULE_ID, safe(event.ruleId()),
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_TENANT_ID, safe(event.tenantId())
                    ))
                    .log();

            AuditLogKafkaEvent standardEvent = convertToStandardEvent(event);
            String partitionKey = resolvePartitionKey(event);
            kafkaTemplate.send(AppConstants.AUDIT_TOPIC_NAME, partitionKey, standardEvent);

            logger.info()
                    .category(AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleAuditConstants.LOG_EVENT_PUBLISHED)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME,
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_RULE_ID, safe(event.ruleId())
                    ))
                    .log();
        } catch (Exception e) {
            logger.error()
                    .category(AccessRoleAssignmentRuleAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentRuleAuditConstants.LOG_EVENT_PUBLISH_FAILED)
                    .details(Map.of(
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleAssignmentRuleAuditConstants.METADATA_ENTITY_TYPE,
                            AccessRoleAssignmentRuleAuditConstants.toEntityType(event.roleType()),
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_RULE_ID, safe(event.ruleId()),
                            AccessRoleAssignmentRuleAuditConstants.DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    private AuditLogKafkaEvent convertToStandardEvent(AccessRoleAssignmentRuleAuditEvent event) {
        UUID objectId = parseUuidSafe(event.ruleId());
        UUID actorId = parseUuidSafe(event.actorId());

        String actorType = AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(event.actorId())
                ? AppConstants.AUDIT_ACTOR_TYPE_SYSTEM
                : AppConstants.AUDIT_ACTOR_TYPE_USER;

        String activityLabel = AccessRoleAssignmentRuleAuditConstants.toActivityLabel(event.action(), event.roleType());
        String entityType = AccessRoleAssignmentRuleAuditConstants.toEntityType(event.roleType());

        return new AuditLogKafkaEvent(
                actorId,
                actorType,
                event.actorUsername(),
                event.tenantId(),
                objectId,
                entityType,
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

    private JsonNode buildDetails(AccessRoleAssignmentRuleAuditEvent event) {
        ObjectNode details = objectMapper.createObjectNode();

        putIfNotNull(details, AppConstants.AUDIT_DETAIL_MESSAGE, event.description());
        putIfNotNull(details, AccessRoleAssignmentRuleAuditConstants.DETAIL_RULE_ID, event.ruleId());
        putIfNotNull(details, AccessRoleAssignmentRuleAuditConstants.DETAIL_RULE_NAME, event.ruleName());
        putIfNotNull(details, AccessRoleAssignmentRuleAuditConstants.DETAIL_ROLE_ID, event.roleId());
        putIfNotNull(details, AccessRoleAssignmentRuleAuditConstants.DETAIL_ROLE_NAME, event.roleName());
        putIfNotNull(details, AccessRoleAssignmentRuleAuditConstants.DETAIL_ROLE_TYPE, event.roleType());
        putIfNotNull(details, AccessRoleAssignmentRuleAuditConstants.DETAIL_ROLE_STATUS, event.roleStatus());
        putIfNotNull(details, AccessRoleAssignmentRuleAuditConstants.DETAIL_RISK_LEVEL, event.riskLevel());
        putIfNotNull(details, AccessRoleAssignmentRuleAuditConstants.DETAIL_SCHEDULE_TYPE, event.scheduleType());
        details.put(AccessRoleAssignmentRuleAuditConstants.DETAIL_ENABLED, Boolean.toString(event.enabled()));

        if (event.priority() != null) {
            details.put(AccessRoleAssignmentRuleAuditConstants.DETAIL_PRIORITY, event.priority().intValue());
        }
        if (event.conditionGroupCount() != null) {
            details.put(AccessRoleAssignmentRuleAuditConstants.DETAIL_CONDITION_GROUP_COUNT, event.conditionGroupCount().intValue());
        }
        if (event.identityTypes() != null && !event.identityTypes().isEmpty()) {
            ArrayNode identityTypes = details.putArray(AccessRoleAssignmentRuleAuditConstants.DETAIL_IDENTITY_TYPES);
            event.identityTypes().forEach(identityTypes::add);
        }

        return details.isEmpty() ? null : details;
    }

    private JsonNode buildMetadata(AccessRoleAssignmentRuleAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.METADATA_SOURCE,
                AccessRoleAssignmentRuleAuditConstants.METADATA_SOURCE_VALUE);
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.METADATA_TENANT_ID, safe(event.tenantId()));
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.METADATA_ENTITY_TYPE,
                AccessRoleAssignmentRuleAuditConstants.toEntityType(event.roleType()));
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.DETAIL_ACTION, safe(event.action()));
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.METADATA_RULE_ID, event.ruleId());
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.METADATA_RULE_NAME, event.ruleName());
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.METADATA_ROLE_ID, event.roleId());
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.METADATA_ROLE_NAME, event.roleName());
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.METADATA_ROLE_TYPE, event.roleType());
        putIfNotNull(metadata, AccessRoleAssignmentRuleAuditConstants.METADATA_RISK_LEVEL, event.riskLevel());
        putIfNotNull(metadata, AppConstants.AUDIT_DETAIL_MESSAGE, event.description());
        return metadata;
    }

    private JsonNode buildTargetObject(AccessRoleAssignmentRuleAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();
        putIfNotNull(target, AccessRoleAssignmentRuleAuditConstants.TARGET_TYPE,
                AccessRoleAssignmentRuleAuditConstants.toEntityType(event.roleType()));
        putIfNotNull(target, AccessRoleAssignmentRuleAuditConstants.TARGET_OBJECT_ID, safe(event.ruleId()));
        putIfNotNull(target, AccessRoleAssignmentRuleAuditConstants.TARGET_OBJECT_NAME, safe(event.ruleName()));
        return target;
    }

    private String resolvePartitionKey(AccessRoleAssignmentRuleAuditEvent event) {
        if (event.ruleId() != null) {
            return event.ruleId();
        }
        if (event.roleId() != null) {
            return event.roleId();
        }
        return event.tenantId();
    }

    private String resolveRisk(AccessRoleAssignmentRuleAuditEvent event) {
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

    private UUID parseUuidSafe(String uuidStr) {
        if (uuidStr == null || uuidStr.isBlank() || AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(uuidStr)) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }

    private String safe(String value) {
        return value != null ? value : AccessRoleAssignmentRuleAuditConstants.NOT_AVAILABLE;
    }

    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null) {
            node.put(key, value);
        }
    }
}
