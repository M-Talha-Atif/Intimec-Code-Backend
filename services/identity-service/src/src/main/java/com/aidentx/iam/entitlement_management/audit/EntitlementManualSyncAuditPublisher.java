package com.aidentx.iam.entitlement_management.audit;
import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.iam.config.AppConstants;
import com.aidentx.iam.entitlement_management.config.constants.EntitlementAuditConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.UUID;
/** Publishes entitlement manual member sync audit events to Kafka. */
@Service
public class EntitlementManualSyncAuditPublisher {
    private static final UUID ZERO_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
    private static final String ENTITY_TYPE_ENTITLEMENT = "Entitlement";
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    public EntitlementManualSyncAuditPublisher(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger(getClass(), EntitlementAuditConstants.LOG_CATEGORY);
    }
    public void publish(EntitlementManualSyncAuditEvent event) {
        try {
            logger.info()
                    .category(EntitlementAuditConstants.LOG_CATEGORY)
                    .message("Publishing entitlement manual member sync audit event to Kafka")
                    .details(Map.of(
                            EntitlementAuditConstants.DETAIL_ACTION, event.action(),
                            EntitlementAuditConstants.DETAIL_ENTITLEMENT_ID, safe(event.entitlementId()),
                            EntitlementAuditConstants.DETAIL_ENTITLEMENT_NAME, safe(event.entitlementName())))
                    .log();
            kafkaTemplate.send(AppConstants.AUDIT_TOPIC_NAME, resolvePartitionKey(event), convertToStandardEvent(event));
        } catch (Exception ex) {
            logger.error()
                    .category(EntitlementAuditConstants.LOG_CATEGORY)
                    .message("Failed to publish entitlement manual member sync audit event to Kafka")
                    .details(Map.of(
                            EntitlementAuditConstants.DETAIL_ACTION, event.action(),
                            EntitlementAuditConstants.DETAIL_ENTITLEMENT_ID, safe(event.entitlementId()),
                            EntitlementAuditConstants.DETAIL_EXCEPTION_TYPE, ex.getClass().getSimpleName()))
                    .log();
        }
    }
    AuditLogKafkaEvent convertToStandardEvent(EntitlementManualSyncAuditEvent event) {
        UUID actorId = parseUuidSafe(event.actorId());
        UUID objectId = parseUuidSafe(event.entitlementId());
        String actorType = AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(event.actorId())
                ? AppConstants.AUDIT_ACTOR_TYPE_SYSTEM
                : AppConstants.AUDIT_ACTOR_TYPE_USER;
        return new AuditLogKafkaEvent(
                actorId, actorType, event.actorUsername(), event.tenantId(), objectId,
                ENTITY_TYPE_ENTITLEMENT, EntitlementAuditConstants.ACTIVITY_MEMBERS_MANUALLY_SYNCED,
                AppConstants.AUDIT_STATUS_SUCCESS, resolveRisk(event.riskLevel()), event.timestamp(),
                buildDetails(event), null, event.stateAfter(), buildMetadata(event), null, null, buildTargetObject(event));
    }
    private JsonNode buildDetails(EntitlementManualSyncAuditEvent event) {
        ObjectNode details = objectMapper.createObjectNode();
        putIfNotNull(details, EntitlementAuditConstants.DETAIL_MESSAGE, event.description());
        putIfNotNull(details, EntitlementAuditConstants.DETAIL_ENTITLEMENT_ID, event.entitlementId());
        putIfNotNull(details, EntitlementAuditConstants.DETAIL_ENTITLEMENT_NAME, event.entitlementName());
        putIfNotNull(details, EntitlementAuditConstants.DETAIL_RISK_LEVEL, event.riskLevel());
        putIntIfNotNull(details, EntitlementAuditConstants.DETAIL_ADDED_COUNT, event.addedCount());
        putIntIfNotNull(details, EntitlementAuditConstants.DETAIL_REMOVED_COUNT, event.removedCount());
        putIntIfNotNull(details, EntitlementAuditConstants.DETAIL_SKIPPED_COUNT, event.skippedCount());
        putIntIfNotNull(details, EntitlementAuditConstants.DETAIL_FAILED_COUNT, event.failedCount());
        putIntIfNotNull(details, EntitlementAuditConstants.DETAIL_TOTAL_DIRECT_COUNT, event.totalDirectCount());
        addMembers(details, EntitlementAuditConstants.DETAIL_ADDED_MEMBERS, event.addedMembers());
        addMembers(details, EntitlementAuditConstants.DETAIL_REMOVED_MEMBERS, event.removedMembers());
        addMembers(details, EntitlementAuditConstants.DETAIL_SKIPPED_MEMBERS, event.skippedMembers());
        addFailedMembers(details, EntitlementAuditConstants.DETAIL_FAILED_MEMBERS, event.failedMembers());
        return details;
    }
    private JsonNode buildMetadata(EntitlementManualSyncAuditEvent event) {
        ObjectNode metadata = objectMapper.createObjectNode();
        putIfNotNull(metadata, AppConstants.AUDIT_METADATA_SOURCE, AppConstants.AUDIT_SOURCE_ENTITLEMENT_SERVICE);
        putIfNotNull(metadata, AppConstants.AUDIT_METADATA_TENANT_ID, event.tenantId());
        putIfNotNull(metadata, AppConstants.AUDIT_METADATA_ENTITY_TYPE, ENTITY_TYPE_ENTITLEMENT);
        putIfNotNull(metadata, EntitlementAuditConstants.DETAIL_RISK_LEVEL, event.riskLevel());
        putIfNotNull(metadata, EntitlementAuditConstants.DETAIL_ENTITLEMENT_ID, event.entitlementId());
        putIfNotNull(metadata, EntitlementAuditConstants.DETAIL_ENTITLEMENT_NAME, event.entitlementName());
        return metadata;
    }
    private JsonNode buildTargetObject(EntitlementManualSyncAuditEvent event) {
        ObjectNode target = objectMapper.createObjectNode();
        putIfNotNull(target, AppConstants.AUDIT_TARGET_TYPE, ENTITY_TYPE_ENTITLEMENT);
        putIfNotNull(target, AppConstants.AUDIT_TARGET_OBJECT_ID, event.entitlementId());
        putIfNotNull(target, AppConstants.AUDIT_TARGET_OBJECT_NAME, event.entitlementName());
        return target;
    }
    private void addMembers(ObjectNode parent, String fieldName, List<EntitlementManualSyncAuditEvent.MemberAuditDetail> members) {
        if (members == null || members.isEmpty()) return;
        ArrayNode arrayNode = parent.putArray(fieldName);
        for (EntitlementManualSyncAuditEvent.MemberAuditDetail member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            putIfNotNull(node, "identityId", member.identityId());
            putIfNotNull(node, "identityName", member.identityName());
            putIfNotNull(node, "identityType", member.identityType());
            addAssignments(node, member.assignments());
            arrayNode.add(node);
        }
    }
    private void addFailedMembers(ObjectNode parent, String fieldName, List<EntitlementManualSyncAuditEvent.FailedMemberAuditDetail> members) {
        if (members == null || members.isEmpty()) return;
        ArrayNode arrayNode = parent.putArray(fieldName);
        for (EntitlementManualSyncAuditEvent.FailedMemberAuditDetail member : members) {
            ObjectNode node = objectMapper.createObjectNode();
            putIfNotNull(node, "identityId", member.identityId());
            putIfNotNull(node, "identityName", member.identityName());
            putIfNotNull(node, "identityType", member.identityType());
            putIfNotNull(node, "reason", member.reason());
            arrayNode.add(node);
        }
    }
    private void addAssignments(ObjectNode memberNode, List<EntitlementManualSyncAuditEvent.AssignmentAuditDetail> assignments) {
        if (assignments == null || assignments.isEmpty()) return;
        ArrayNode arrayNode = memberNode.putArray("assignments");
        for (EntitlementManualSyncAuditEvent.AssignmentAuditDetail assignment : assignments) {
            ObjectNode node = objectMapper.createObjectNode();
            putIfNotNull(node, "assignmentId", assignment.assignmentId());
            putIfNotNull(node, "shadowId", assignment.shadowId());
            putIfNotNull(node, "status", assignment.status());
            putIfNotNull(node, EntitlementAuditConstants.DETAIL_ACTION, assignment.action());
            arrayNode.add(node);
        }
    }
    private String resolvePartitionKey(EntitlementManualSyncAuditEvent event) {
        return event.entitlementId() != null ? event.entitlementId() : event.tenantId();
    }
    private String resolveRisk(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) return AppConstants.AUDIT_RISK_LOW;
        String normalized = riskLevel.trim().toLowerCase();
        if (normalized.contains("critical") || normalized.contains("high")) return AppConstants.AUDIT_RISK_HIGH;
        if (normalized.contains("medium") || normalized.contains("moderate")) return AppConstants.AUDIT_RISK_MEDIUM;
        return AppConstants.AUDIT_RISK_LOW;
    }
    private UUID parseUuidSafe(String value) {
        if (value == null || value.isBlank() || AppConstants.AUDIT_ACTOR_SYSTEM_ID.equals(value)) return ZERO_UUID;
        try { return UUID.fromString(value); } catch (IllegalArgumentException ex) { return ZERO_UUID; }
    }
    private String safe(String value) { return value != null ? value : EntitlementAuditConstants.DETAIL_NOT_AVAILABLE; }
    private void putIfNotNull(ObjectNode node, String key, String value) {
        if (value != null) node.put(key, value);
    }
    private void putIntIfNotNull(ObjectNode node, String key, Integer value) {
        if (value != null) node.put(key, value);
    }
}
