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

/** Publishes a single summary audit event when an access-role import job finishes. */
@Service
public class RoleImportJobAuditPublisher {

    private static final UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;

    public RoleImportJobAuditPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), RoleImportJobAuditConstants.LOG_CATEGORY);
    }

    public void publish(RoleImportJobAuditEvent event) {
        try {
            logger.info()
                    .category(RoleImportJobAuditConstants.LOG_CATEGORY)
                    .message(RoleImportJobAuditConstants.LOG_PUBLISHING_EVENT)
                    .details(Map.of(
                            RoleImportJobAuditConstants.DETAIL_ACTION,
                            RoleImportJobAuditConstants.ACTION_ROLE_IMPORT_JOB_FINISHED,
                            RoleImportJobAuditConstants.DETAIL_TENANT_ID, safe(event.tenantId()),
                            RoleImportJobAuditConstants.DETAIL_JOB_ID, safe(event.jobId()),
                            RoleImportJobAuditConstants.DETAIL_STATUS, safe(event.status())
                    ))
                    .log();

            kafkaTemplate.send(AppConstants.AUDIT_TOPIC_NAME, resolvePartitionKey(event), convertToStandardEvent(event));

            logger.info()
                    .category(RoleImportJobAuditConstants.LOG_CATEGORY)
                    .message(RoleImportJobAuditConstants.LOG_EVENT_PUBLISHED)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME,
                            RoleImportJobAuditConstants.DETAIL_ACTION,
                            RoleImportJobAuditConstants.ACTION_ROLE_IMPORT_JOB_FINISHED,
                            RoleImportJobAuditConstants.DETAIL_JOB_ID, safe(event.jobId())
                    ))
                    .log();
        } catch (Exception e) {
            logger.error()
                    .category(RoleImportJobAuditConstants.LOG_CATEGORY)
                    .message(RoleImportJobAuditConstants.LOG_EVENT_PUBLISH_FAILED)
                    .details(Map.of(
                            RoleImportJobAuditConstants.DETAIL_ACTION,
                            RoleImportJobAuditConstants.ACTION_ROLE_IMPORT_JOB_FINISHED,
                            RoleImportJobAuditConstants.DETAIL_TENANT_ID, safe(event.tenantId()),
                            RoleImportJobAuditConstants.DETAIL_JOB_ID, safe(event.jobId()),
                            RoleImportJobAuditConstants.DETAIL_EXCEPTION_TYPE, e.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    private AuditLogKafkaEvent convertToStandardEvent(RoleImportJobAuditEvent event) {
        UUID jobId = parseUuidSafe(event.jobId());
        UUID actorId = parseUuidSafe(event.actorId());
        String actorType = AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(event.actorId())
                ? AppConstants.AUDIT_ACTOR_TYPE_SYSTEM
                : AppConstants.AUDIT_ACTOR_TYPE_USER;

        return new AuditLogKafkaEvent(
                actorId,
                actorType,
                event.actorUsername(),
                event.tenantId(),
                jobId,
                RoleImportJobAuditConstants.ENTITY_TYPE,
                RoleImportJobAuditConstants.ACTIVITY_ROLE_IMPORT_JOB_FINISHED,
                auditStatus(event.status()),
                resolveRisk(event.riskLevel()),
                event.timestamp(),
                event.summary(),
                null,
                event.summary(),
                buildMetadata(event),
                null,
                null,
                buildTargetObject(event)
        );
    }

    private JsonNode buildMetadata(RoleImportJobAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();
        metadata.put(RoleImportJobAuditConstants.METADATA_SOURCE, RoleImportJobAuditConstants.METADATA_SOURCE_VALUE);
        metadata.put(RoleImportJobAuditConstants.METADATA_TENANT_ID, event.tenantId());
        metadata.put(RoleImportJobAuditConstants.METADATA_ENTITY_TYPE, RoleImportJobAuditConstants.ENTITY_TYPE);
        metadata.put(RoleImportJobAuditConstants.DETAIL_ACTION, RoleImportJobAuditConstants.ACTION_ROLE_IMPORT_JOB_FINISHED);
        metadata.put(RoleImportJobAuditConstants.DETAIL_JOB_ID, event.jobId());
        metadata.put(RoleImportJobAuditConstants.DETAIL_STATUS, event.status());
        return metadata;
    }

    private JsonNode buildTargetObject(RoleImportJobAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();
        target.put(RoleImportJobAuditConstants.TARGET_TYPE, RoleImportJobAuditConstants.ENTITY_TYPE);
        target.put(RoleImportJobAuditConstants.TARGET_OBJECT_ID, event.jobId());
        target.put(RoleImportJobAuditConstants.TARGET_OBJECT_NAME, RoleImportJobAuditConstants.ENTITY_TYPE + " " + event.jobId());
        return target;
    }

    private String auditStatus(String jobStatus) {
        return "FAILED".equals(jobStatus) ? AppConstants.AUDIT_STATUS_FAILURE : AppConstants.AUDIT_STATUS_SUCCESS;
    }

    private String resolveRisk(String riskLevel) {
        if (riskLevel == null) {
            return AppConstants.AUDIT_RISK_LOW;
        }
        String normalized = riskLevel.trim().toLowerCase();
        if (AppConstants.AUDIT_RISK_HIGH.equals(normalized)) {
            return AppConstants.AUDIT_RISK_HIGH;
        }
        if (AppConstants.AUDIT_RISK_MEDIUM.equals(normalized)) {
            return AppConstants.AUDIT_RISK_MEDIUM;
        }
        return AppConstants.AUDIT_RISK_LOW;
    }

    private String resolvePartitionKey(RoleImportJobAuditEvent event) {
        return event.jobId() != null ? event.jobId() : event.tenantId();
    }

    private UUID parseUuidSafe(String value) {
        if (value == null || value.isBlank() || AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(value)) {
            return NIL_UUID;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return NIL_UUID;
        }
    }

    private String safe(String value) {
        return value != null ? value : AppConstants.AUDIT_NOT_AVAILABLE;
    }
}
