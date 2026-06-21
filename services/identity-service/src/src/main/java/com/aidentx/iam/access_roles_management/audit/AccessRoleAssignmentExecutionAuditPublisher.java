package com.aidentx.iam.access_roles_management.audit;

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
 * Publishes access-role assignment execution audit events to Kafka.
 */
@Service
public class AccessRoleAssignmentExecutionAuditPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    private final AccessRoleExecutionAuditRiskResolver riskResolver;

    public AccessRoleAssignmentExecutionAuditPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory,
            AccessRoleExecutionAuditRiskResolver riskResolver) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleAssignmentExecutionAuditConstants.LOG_CATEGORY);
        this.riskResolver = riskResolver;
    }

    public void publish(AccessRoleAssignmentExecutionAuditEvent event) {
        try {
            logger.info()
                    .category(AccessRoleAssignmentExecutionAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentExecutionAuditConstants.LOG_PUBLISHING_EVENT)
                    .details(Map.of(
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_TENANT_ID, safe(event.tenantId()),
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_ID, safe(event.ruleId())
                    ))
                    .log();

            AuditLogKafkaEvent standardEvent = convertToStandardEvent(event);
            kafkaTemplate.send(AppConstants.AUDIT_TOPIC_NAME, resolvePartitionKey(event), standardEvent);

            logger.info()
                    .category(AccessRoleAssignmentExecutionAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentExecutionAuditConstants.LOG_EVENT_PUBLISHED)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME,
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_ID, safe(event.ruleId())
                    ))
                    .log();
        } catch (Exception e) {
            logger.error()
                    .category(AccessRoleAssignmentExecutionAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleAssignmentExecutionAuditConstants.LOG_EVENT_PUBLISH_FAILED)
                    .details(Map.of(
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_TENANT_ID, safe(event.tenantId()),
                            AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    private AuditLogKafkaEvent convertToStandardEvent(AccessRoleAssignmentExecutionAuditEvent event) {
        UUID objectId = parseUuidSafe(event.ruleId());
        UUID actorId = parseUuidSafe(event.actorId());
        String actorType = AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(event.actorId())
                ? AppConstants.AUDIT_ACTOR_TYPE_SYSTEM
                : AppConstants.AUDIT_ACTOR_TYPE_USER;

        return new AuditLogKafkaEvent(
                actorId,
                actorType,
                event.actorUsername(),
                event.tenantId(),
                objectId,
                event.action().equals(AccessRoleAssignmentExecutionAuditConstants.ACTION_RULES_EXECUTED)
                        ? AccessRoleAssignmentExecutionAuditConstants.ENTITY_TYPE_ALL
                        : AccessRoleAssignmentRuleAuditConstants.toEntityType(event.roleType()),
                event.action().equals(AccessRoleAssignmentExecutionAuditConstants.ACTION_RULES_EXECUTED)
                        ? AccessRoleAssignmentExecutionAuditConstants.ACTIVITY_RULES_EXECUTED
                        : AccessRoleAssignmentExecutionAuditConstants.toSingleActivityLabel(event.roleType()),
                AppConstants.AUDIT_STATUS_SUCCESS,
                resolveRisk(event.riskLevel()),
                event.timestamp(),
                buildDetails(event),
                null,
                event.stateAfter(),
                buildMetadata(event),
                null,
                null,
                buildTargetObject(event)
        );
    }

    private JsonNode buildDetails(AccessRoleAssignmentExecutionAuditEvent event) {
        ObjectNode details = objectMapper.createObjectNode();
        putIfNotNull(details, AppConstants.AUDIT_DETAIL_MESSAGE, event.description());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_ID, event.executionId());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_ID, event.ruleId());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_RULE_NAME, event.ruleName());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_ID, event.roleId());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_NAME, event.roleName());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_TYPE, event.roleType());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLE_STATUS, event.roleStatus());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_RISK_LEVEL, event.riskLevel());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_TRIGGER_TYPE, event.triggerType());
        putIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_STATUS, event.executionStatus());
        putIntIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_EXECUTION_COUNT, event.executionCount());
        putIntIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_IDENTITIES_MATCHED, event.identitiesMatched());
        putIntIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_ASSIGNED, event.rolesAssigned());
        putIntIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_REACTIVATED, event.rolesReactivated());
        putIntIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ROLES_REVOKED, event.rolesRevoked());
        putIntIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_SKIPPED_ALREADY_ASSIGNED, event.skippedAlreadyAssigned());
        putIntIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_SKIPPED_NOT_MATCHED, event.skippedNotMatched());
        putIntIfNotNull(details, AccessRoleAssignmentExecutionAuditConstants.DETAIL_ERROR_COUNT, event.errorCount());
        return details;
    }

    private JsonNode buildMetadata(AccessRoleAssignmentExecutionAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();
        putIfNotNull(metadata, AccessRoleAssignmentExecutionAuditConstants.METADATA_SOURCE,
                AccessRoleAssignmentExecutionAuditConstants.METADATA_SOURCE_VALUE);
        putIfNotNull(metadata, AccessRoleAssignmentExecutionAuditConstants.DETAIL_TENANT_ID, event.tenantId());
        putIfNotNull(metadata, AccessRoleAssignmentExecutionAuditConstants.METADATA_ENTITY_TYPE,
                event.action().equals(AccessRoleAssignmentExecutionAuditConstants.ACTION_RULES_EXECUTED)
                        ? AccessRoleAssignmentExecutionAuditConstants.ENTITY_TYPE_ALL
                        : AccessRoleAssignmentRuleAuditConstants.toEntityType(event.roleType()));
        putIfNotNull(metadata, AccessRoleAssignmentExecutionAuditConstants.METADATA_RISK_LEVEL, event.riskLevel());
        return metadata;
    }

    private JsonNode buildTargetObject(AccessRoleAssignmentExecutionAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();
        if (AccessRoleAssignmentExecutionAuditConstants.ACTION_RULES_EXECUTED.equals(event.action())) {
            putIfNotNull(target, AccessRoleAssignmentExecutionAuditConstants.TARGET_TYPE,
                    AccessRoleAssignmentExecutionAuditConstants.ENTITY_TYPE_ALL);
            putIfNotNull(target, AccessRoleAssignmentExecutionAuditConstants.TARGET_OBJECT_NAME,
                    AccessRoleAssignmentExecutionAuditConstants.ENTITY_TYPE_ALL);
            return target;
        }
        putIfNotNull(target, AccessRoleAssignmentExecutionAuditConstants.TARGET_TYPE,
                AccessRoleAssignmentRuleAuditConstants.toEntityType(event.roleType()));
        putIfNotNull(target, AccessRoleAssignmentExecutionAuditConstants.TARGET_OBJECT_ID, event.ruleId());
        putIfNotNull(target, AccessRoleAssignmentExecutionAuditConstants.TARGET_OBJECT_NAME, event.ruleName());
        return target;
    }

    private String resolvePartitionKey(AccessRoleAssignmentExecutionAuditEvent event) {
        if (event.ruleId() != null) {
            return event.ruleId();
        }
        return event.tenantId();
    }

    private String resolveRisk(String riskLevel) {
        String normalized = riskResolver.normalize(riskLevel);
        if (AppConstants.AUDIT_RISK_HIGH.equals(normalized)) {
            return AppConstants.AUDIT_RISK_HIGH;
        }
        if (AppConstants.AUDIT_RISK_MEDIUM.equals(normalized)) {
            return AppConstants.AUDIT_RISK_MEDIUM;
        }
        return AppConstants.AUDIT_RISK_LOW;
    }

    private UUID parseUuidSafe(String value) {
        if (value == null || value.isBlank() || AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(value)) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
    }

    private String safe(String value) {
        return value != null ? value : AccessRoleAssignmentExecutionAuditConstants.NOT_AVAILABLE;
    }

    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null) {
            node.put(key, value);
        }
    }

    private void putIntIfNotNull(ObjectNode node, String key, Integer value) {
        if (value != null) {
            node.put(key, value);
        }
    }
}

