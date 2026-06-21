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
 * Publishes manual access-role member grant audit events to Kafka.
 */
@Service
public class AccessRoleManualAssignmentAuditPublisher {

    private static final UUID ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    private final AccessRoleExecutionAuditRiskResolver riskResolver;

    public AccessRoleManualAssignmentAuditPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory,
            AccessRoleExecutionAuditRiskResolver riskResolver) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY);
        this.riskResolver = riskResolver;
    }

    public void publish(AccessRoleManualAssignmentAuditEvent event) {
        try {
            logger.info()
                    .category(AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleManualAssignmentAuditConstants.LOG_PUBLISHING_EVENT)
                    .details(Map.of(
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_TENANT_ID, safe(event.tenantId()),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_ID, safe(event.roleId())
                    ))
                    .log();

            kafkaTemplate.send(AppConstants.AUDIT_TOPIC_NAME, resolvePartitionKey(event), convertToStandardEvent(event));

            logger.info()
                    .category(AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleManualAssignmentAuditConstants.LOG_EVENT_PUBLISHED)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME,
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_ID, safe(event.roleId())
                    ))
                    .log();
        } catch (Exception ex) {
            logger.error()
                    .category(AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleManualAssignmentAuditConstants.LOG_EVENT_PUBLISH_FAILED)
                    .details(Map.of(
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_TENANT_ID, safe(event.tenantId()),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_EXCEPTION_TYPE, ex.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    private AuditLogKafkaEvent convertToStandardEvent(AccessRoleManualAssignmentAuditEvent event) {
        UUID objectId = parseUuidSafe(event.roleId());
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
                AccessRoleManualAssignmentAuditConstants.toEntityType(event.roleType()),
                AccessRoleManualAssignmentAuditConstants.toActivityLabel(event.roleType()),
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

    private JsonNode buildDetails(AccessRoleManualAssignmentAuditEvent event) {
        ObjectNode details = objectMapper.createObjectNode();
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_MESSAGE, event.description());
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_ID, event.roleId());
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_NAME, event.roleName());
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_TYPE, event.roleType());
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_RISK_LEVEL, event.riskLevel());
        putIntIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_REQUESTED_COUNT, event.requestedCount());
        putIntIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ADDED_COUNT, event.addedCount());
        putIntIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_SKIPPED_COUNT, event.skippedCount());
        putIntIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_FAILED_COUNT, event.failedCount());
        addMembers(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ADDED_IDENTITIES, event.addedMembers());
        addMembers(details, AccessRoleManualAssignmentAuditConstants.DETAIL_SKIPPED_IDENTITIES, event.skippedMembers());
        addFailedMembers(details, AccessRoleManualAssignmentAuditConstants.DETAIL_FAILED_IDENTITIES, event.failedMembers());
        return details;
    }

    private JsonNode buildMetadata(AccessRoleManualAssignmentAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();
        putIfNotNull(metadata, AccessRoleManualAssignmentAuditConstants.METADATA_SOURCE,
                AccessRoleManualAssignmentAuditConstants.METADATA_SOURCE_VALUE);
        putIfNotNull(metadata, AccessRoleManualAssignmentAuditConstants.DETAIL_TENANT_ID, event.tenantId());
        putIfNotNull(metadata, AccessRoleManualAssignmentAuditConstants.METADATA_ENTITY_TYPE,
                AccessRoleManualAssignmentAuditConstants.toEntityType(event.roleType()));
        putIfNotNull(metadata, AccessRoleManualAssignmentAuditConstants.METADATA_RISK_LEVEL, event.riskLevel());
        return metadata;
    }

    private JsonNode buildTargetObject(AccessRoleManualAssignmentAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();
        putIfNotNull(target, AccessRoleManualAssignmentAuditConstants.TARGET_TYPE,
                AccessRoleManualAssignmentAuditConstants.toEntityType(event.roleType()));
        putIfNotNull(target, AccessRoleManualAssignmentAuditConstants.TARGET_OBJECT_ID, event.roleId());
        putIfNotNull(target, AccessRoleManualAssignmentAuditConstants.TARGET_OBJECT_NAME, event.roleName());
        return target;
    }

    private void addMembers(ObjectNode details,
                            String fieldName,
                            java.util.List<AccessRoleManualAssignmentAuditEvent.MemberAuditDetail> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        ArrayNode arrayNode = details.putArray(fieldName);
        for (AccessRoleManualAssignmentAuditEvent.MemberAuditDetail member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            putIfNotNull(node, "identityId", member.identityId());
            putIfNotNull(node, "identityName", member.identityName());
            putIfNotNull(node, "identityType", member.identityType());
            arrayNode.add(node);
        }
    }

    private void addFailedMembers(ObjectNode details,
                                  String fieldName,
                                  java.util.List<AccessRoleManualAssignmentAuditEvent.FailedMemberAuditDetail> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        ArrayNode arrayNode = details.putArray(fieldName);
        for (AccessRoleManualAssignmentAuditEvent.FailedMemberAuditDetail member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            putIfNotNull(node, "identityId", member.identityId());
            putIfNotNull(node, "identityName", member.identityName());
            putIfNotNull(node, "identityType", member.identityType());
            putIfNotNull(node, "reason", member.reason());
            arrayNode.add(node);
        }
    }

    private String resolvePartitionKey(AccessRoleManualAssignmentAuditEvent event) {
        return event.roleId() != null ? event.roleId() : event.tenantId();
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
            return ZERO_UUID;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return ZERO_UUID;
        }
    }

    private String safe(String value) {
        return value != null ? value : AccessRoleManualAssignmentAuditConstants.NOT_AVAILABLE;
    }

    // ===================================================================
    //  Sync (PUT desired-state) Event Publication
    // ===================================================================

    public void publish(AccessRoleManualSyncAuditEvent event) {
        try {
            logger.info()
                    .category(AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleManualAssignmentAuditConstants.LOG_PUBLISHING_SYNC_EVENT)
                    .details(Map.of(
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_TENANT_ID, safe(event.tenantId()),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_ID, safe(event.roleId())
                    ))
                    .log();

            kafkaTemplate.send(AppConstants.AUDIT_TOPIC_NAME, resolveSyncPartitionKey(event), convertSyncToStandardEvent(event));

            logger.info()
                    .category(AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleManualAssignmentAuditConstants.LOG_SYNC_EVENT_PUBLISHED)
                    .details(Map.of(
                            AppConstants.AUDIT_DETAIL_KAFKA_TOPIC, AppConstants.AUDIT_TOPIC_NAME,
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_ID, safe(event.roleId())
                    ))
                    .log();
        } catch (Exception ex) {
            logger.error()
                    .category(AccessRoleManualAssignmentAuditConstants.LOG_CATEGORY)
                    .message(AccessRoleManualAssignmentAuditConstants.LOG_SYNC_EVENT_PUBLISH_FAILED)
                    .details(Map.of(
                            AccessRoleManualAssignmentAuditConstants.DETAIL_ACTION, event.action(),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_TENANT_ID, safe(event.tenantId()),
                            AccessRoleManualAssignmentAuditConstants.DETAIL_EXCEPTION_TYPE, ex.getClass().getSimpleName()
                    ))
                    .log();
        }
    }

    private AuditLogKafkaEvent convertSyncToStandardEvent(AccessRoleManualSyncAuditEvent event) {
        UUID objectId = parseUuidSafe(event.roleId());
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
                AccessRoleManualAssignmentAuditConstants.toEntityType(event.roleType()),
                AccessRoleManualAssignmentAuditConstants.toSyncActivityLabel(event.roleType()),
                AppConstants.AUDIT_STATUS_SUCCESS,
                resolveRisk(event.riskLevel()),
                event.timestamp(),
                buildSyncDetails(event),
                null,
                event.stateAfter(),
                buildSyncMetadata(event),
                null,
                null,
                buildSyncTargetObject(event)
        );
    }

    private JsonNode buildSyncDetails(AccessRoleManualSyncAuditEvent event) {
        ObjectNode details = objectMapper.createObjectNode();
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_MESSAGE, event.description());
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_ID, event.roleId());
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_NAME, event.roleName());
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ROLE_TYPE, event.roleType());
        putIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_RISK_LEVEL, event.riskLevel());
        putIntIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ADDED_COUNT, event.addedCount());
        putIntIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_REVOKED_COUNT, event.revokedCount());
        putIntIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_SKIPPED_COUNT, event.skippedCount());
        putIntIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_FAILED_COUNT, event.failedCount());
        putIntIfNotNull(details, AccessRoleManualAssignmentAuditConstants.DETAIL_TOTAL_ACTIVE_COUNT, event.totalActiveCount());
        addSyncMembers(details, AccessRoleManualAssignmentAuditConstants.DETAIL_ADDED_IDENTITIES, event.addedMembers());
        addSyncMembers(details, AccessRoleManualAssignmentAuditConstants.DETAIL_REVOKED_IDENTITIES, event.revokedMembers());
        addSyncMembers(details, AccessRoleManualAssignmentAuditConstants.DETAIL_SKIPPED_IDENTITIES, event.skippedMembers());
        addFailedSyncMembers(details, AccessRoleManualAssignmentAuditConstants.DETAIL_FAILED_IDENTITIES, event.failedMembers());
        return details;
    }

    private JsonNode buildSyncMetadata(AccessRoleManualSyncAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();
        putIfNotNull(metadata, AccessRoleManualAssignmentAuditConstants.METADATA_SOURCE,
                AccessRoleManualAssignmentAuditConstants.METADATA_SOURCE_VALUE);
        putIfNotNull(metadata, AccessRoleManualAssignmentAuditConstants.DETAIL_TENANT_ID, event.tenantId());
        putIfNotNull(metadata, AccessRoleManualAssignmentAuditConstants.METADATA_ENTITY_TYPE,
                AccessRoleManualAssignmentAuditConstants.toEntityType(event.roleType()));
        putIfNotNull(metadata, AccessRoleManualAssignmentAuditConstants.METADATA_RISK_LEVEL, event.riskLevel());
        return metadata;
    }

    private JsonNode buildSyncTargetObject(AccessRoleManualSyncAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();
        putIfNotNull(target, AccessRoleManualAssignmentAuditConstants.TARGET_TYPE,
                AccessRoleManualAssignmentAuditConstants.toEntityType(event.roleType()));
        putIfNotNull(target, AccessRoleManualAssignmentAuditConstants.TARGET_OBJECT_ID, event.roleId());
        putIfNotNull(target, AccessRoleManualAssignmentAuditConstants.TARGET_OBJECT_NAME, event.roleName());
        return target;
    }

    private void addSyncMembers(ObjectNode details,
                                String fieldName,
                                java.util.List<AccessRoleManualSyncAuditEvent.SyncMemberAuditDetail> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        ArrayNode arrayNode = details.putArray(fieldName);
        for (AccessRoleManualSyncAuditEvent.SyncMemberAuditDetail member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            putIfNotNull(node, "identityId", member.identityId());
            putIfNotNull(node, "identityName", member.identityName());
            putIfNotNull(node, "identityType", member.identityType());
            addEntitlementArray(node,
                    AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_PROVISIONED,
                    member.entitlementsProvisioned(),
                    AccessRoleManualAssignmentAuditConstants.ENTITLEMENT_ACTION_PROVISIONED);
            addEntitlementArray(node,
                    AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_PENDING,
                    member.entitlementsPending(),
                    AccessRoleManualAssignmentAuditConstants.ENTITLEMENT_ACTION_PENDING);
            addEntitlementArray(node,
                    AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENTS_DEPROVISIONED,
                    member.entitlementsDeprovisioned(),
                    AccessRoleManualAssignmentAuditConstants.ENTITLEMENT_ACTION_DEPROVISIONED);
            arrayNode.add(node);
        }
    }

    private void addFailedSyncMembers(ObjectNode details,
                                      String fieldName,
                                      java.util.List<AccessRoleManualSyncAuditEvent.FailedSyncMemberAuditDetail> members) {
        if (members == null || members.isEmpty()) {
            return;
        }
        ArrayNode arrayNode = details.putArray(fieldName);
        for (AccessRoleManualSyncAuditEvent.FailedSyncMemberAuditDetail member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            putIfNotNull(node, "identityId", member.identityId());
            putIfNotNull(node, "identityName", member.identityName());
            putIfNotNull(node, "identityType", member.identityType());
            putIfNotNull(node, "reason", member.reason());
            arrayNode.add(node);
        }
    }

    private void addEntitlementArray(ObjectNode parent,
                                     String fieldName,
                                     java.util.List<AccessRoleManualSyncAuditEvent.EntitlementAuditDetail> entitlements,
                                     String action) {
        if (entitlements == null || entitlements.isEmpty()) {
            return;
        }
        ArrayNode arrayNode = parent.putArray(fieldName);
        for (AccessRoleManualSyncAuditEvent.EntitlementAuditDetail ent : entitlements) {
            ObjectNode node = objectMapper.createObjectNode();
            putIfNotNull(node, AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_ID, ent.entitlementId());
            putIfNotNull(node, AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_NAME, ent.entitlementName());
            putIfNotNull(node, AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_APPLICATION_ID, ent.applicationId());
            putIfNotNull(node, AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_APPLICATION_NAME, ent.applicationName());
            node.put(AccessRoleManualAssignmentAuditConstants.MEMBER_ENTITLEMENT_ACTION, action);
            arrayNode.add(node);
        }
    }

    private String resolveSyncPartitionKey(AccessRoleManualSyncAuditEvent event) {
        return event.roleId() != null ? event.roleId() : event.tenantId();
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

