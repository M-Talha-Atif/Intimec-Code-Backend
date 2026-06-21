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

@Service
public class EntitlementGovernanceAuditPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public EntitlementGovernanceAuditPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AppConstants.AUDIT_LOG_CATEGORY);
    }

    public void publish(EntitlementGovernanceAuditEvent event) {
        try {
            logger.info()
                    .category(AppConstants.AUDIT_LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_PUBLISHING_ENTITLEMENT_GOVERNANCE_EVENT)
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
                    .message(AppConstants.AUDIT_LOG_ENTITLEMENT_GOVERNANCE_EVENT_PUBLISHED)
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
                    .message(AppConstants.AUDIT_LOG_ENTITLEMENT_GOVERNANCE_EVENT_FAILED)
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

    private AuditLogKafkaEvent convertToStandardEvent(EntitlementGovernanceAuditEvent event) {
        UUID objectId = parseUuidSafe(event.entityId());
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
                event.entityType(),
                event.action(),
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

    private JsonNode buildDetails(EntitlementGovernanceAuditEvent event) {
        ObjectNode details = objectMapper.createObjectNode();

        if (event.entityName() != null) {
            details.put(AppConstants.AUDIT_DETAIL_ENTITLEMENT_NAME, event.entityName());
        }
        if (event.entityId() != null) {
            details.put(AppConstants.AUDIT_DETAIL_ENTITLEMENT_ID, event.entityId());
        }
        if (event.resourceName() != null) {
            details.put(AppConstants.AUDIT_DETAIL_RESOURCE_NAME, event.resourceName());
        }
        if (event.lifecycle() != null) {
            details.put(AppConstants.AUDIT_DETAIL_LIFECYCLE, event.lifecycle());
        }
        if (event.riskLevel() != null) {
            details.put(AppConstants.AUDIT_DETAIL_RISK_LEVEL, event.riskLevel());
        }
        if (event.kind() != null) {
            details.put(AppConstants.AUDIT_DETAIL_KIND, event.kind());
        }
        if (event.riskReason() != null) {
            details.put(AppConstants.AUDIT_DETAIL_RISK_REASON, event.riskReason());
        }
        if (event.requestable() != null) {
            details.put(AppConstants.AUDIT_DETAIL_REQUESTABLE, event.requestable());
        }
        if (event.privilegeLevel() != null) {
            details.put(AppConstants.AUDIT_DETAIL_PRIVILEGE_LEVEL, event.privilegeLevel());
        }
        if (event.ownerId() != null) {
            details.put(AppConstants.AUDIT_DETAIL_OWNER_ID, event.ownerId());
        }
        if (event.requireApproval() != null) {
            details.put(AppConstants.AUDIT_DETAIL_REQUIRE_APPROVAL, event.requireApproval());
        }
        if (event.allowedAutomatedGrant() != null) {
            details.put(AppConstants.AUDIT_DETAIL_ALLOWED_AUTOMATED_GRANT, event.allowedAutomatedGrant());
        }
        if (event.maxDurationMs() != null) {
            details.put(AppConstants.AUDIT_DETAIL_MAX_DURATION_MS, event.maxDurationMs());
        }
        if (event.autoExpireDurationMs() != null) {
            details.put(AppConstants.AUDIT_DETAIL_AUTO_EXPIRE_DURATION_MS, event.autoExpireDurationMs());
        }
        if (event.identityType() != null) {
            details.put(AppConstants.AUDIT_DETAIL_IDENTITY_TYPE, event.identityType());
        }

        return details.isEmpty() ? null : details;
    }

    private JsonNode buildMetadata(EntitlementGovernanceAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();

        metadata.put(AppConstants.AUDIT_METADATA_SOURCE, AppConstants.AUDIT_SOURCE_ENTITLEMENT_SERVICE);
        metadata.put(AppConstants.AUDIT_METADATA_TENANT_ID, event.tenantId());
        metadata.put(AppConstants.AUDIT_METADATA_ENTITY_TYPE, event.entityType());
        metadata.put(AppConstants.AUDIT_DETAIL_ACTION, event.action());

        if (event.entityId() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_ENTITLEMENT_ID, event.entityId());
        }
        if (event.entityName() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_ENTITLEMENT_NAME, event.entityName());
        }
        if (event.resourceName() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_RESOURCE_NAME, event.resourceName());
        }
        if (event.riskLevel() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_RISK_LEVEL, event.riskLevel());
        }
        if (event.privilegeLevel() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_PRIVILEGE_LEVEL, event.privilegeLevel());
        }
        if (event.ownerId() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_OWNER_ID, event.ownerId());
        }
        if (event.identityType() != null) {
            metadata.put(AppConstants.AUDIT_METADATA_IDENTITY_TYPE, event.identityType());
        }

        return metadata;
    }

    private JsonNode buildTargetObject(EntitlementGovernanceAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();

        target.put(AppConstants.AUDIT_TARGET_TYPE, event.entityType());
        target.put(AppConstants.AUDIT_TARGET_OBJECT_ID,
                event.entityId() != null ? event.entityId() : AppConstants.AUDIT_NOT_AVAILABLE);
        target.put(AppConstants.AUDIT_TARGET_OBJECT_NAME,
                event.entityName() != null ? event.entityName() : AppConstants.AUDIT_NOT_AVAILABLE);

        if (event.resourceName() != null) {
            target.put(AppConstants.AUDIT_METADATA_RESOURCE_NAME, event.resourceName());
        }

        return target;
    }

    private String resolveRisk(EntitlementGovernanceAuditEvent event) {
        // Risk level changes and privilege level changes are high-risk operations
        if (event.riskLevel() != null || event.privilegeLevel() != null) {
            return AppConstants.AUDIT_RISK_MEDIUM;
        }
        return AppConstants.AUDIT_RISK_LOW;
    }

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
}