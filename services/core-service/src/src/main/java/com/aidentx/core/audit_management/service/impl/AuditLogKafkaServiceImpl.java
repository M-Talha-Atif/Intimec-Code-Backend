package com.aidentx.core.audit_management.service.impl;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.core.audit_management.config.AppConstants;
import com.aidentx.core.audit_management.model.entity.AuditLog;
import com.aidentx.core.audit_management.repository.AuditLogRepository;
import com.aidentx.core.audit_management.service.AuditLogKafkaService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Kafka listener service for AuditLog events.
 *
 * Listens to the 'audit-log' topic and saves incoming audit log messages to the database.
 *
 * The method processAuditLog is triggered automatically by Spring Kafka when a message is received.
 */
@Service
public class AuditLogKafkaServiceImpl implements AuditLogKafkaService {

    private final IUnifiedLogger logger;
    private final AuditLogRepository auditLogRepository;

    public AuditLogKafkaServiceImpl(AuditLogRepository auditLogRepository, UnifiedLoggerFactory loggerFactory) {
        this.auditLogRepository = auditLogRepository;
        this.logger = loggerFactory.getLogger("AuditLogKafkaService", AppConstants.LOG_CATEGORY_KAFKA_CONSUMER);
    }

    /**
     * Kafka listener for the 'audit-log' topic.
     *
     * @param event the audit log event received from Kafka
     */
    @Override
    @KafkaListener(topics = "audit-log", groupId = "${spring.kafka.consumer.group-id:auditlog-service-group}", containerFactory = "auditLogKafkaListenerContainerFactory")
    public void processAuditLog(AuditLogKafkaEvent event) {
        logger.trace().message(AppConstants.LOG_KAFKA_EVENT_RECEIVED)
                .detail("event", event)
                .log();
        try {
            // Generate a UUID if the event ID is null
            UUID eventId = event.getId() != null ? event.getId() : UUID.randomUUID();
            
            AuditLog auditLog = new AuditLog();
            auditLog.setId(eventId);
            auditLog.setActorId(event.getActorId());
            auditLog.setActorType(event.getActorType());
            auditLog.setActorName(event.getActorName());
            
            // Use direct tenantId field from the event - clean and efficient!
            String tenantId = event.getTenantId();
            if (tenantId == null || tenantId.trim().isEmpty()) {
                tenantId = "unknown";
                logger.warn()
                    .message(AppConstants.LOG_KAFKA_TENANT_ID_NULL_OR_EMPTY)
                    .detail("eventId", eventId)
                    .log();
            }
            auditLog.setTenantId(tenantId);
            
            auditLog.setObjectId(event.getObjectId());
            String formattedObjectType = formatObjectType(event.getObjectType());
            logger.info().message("Formatted objectType")
                .detail("original", event.getObjectType())
                .detail("formatted", formattedObjectType)
                .log();
            auditLog.setObjectType(formattedObjectType);
            auditLog.setEvent(event.getEvent());
            auditLog.setStatus(event.getStatus().toLowerCase());
            auditLog.setRisk(event.getRisk().toLowerCase());
            auditLog.setTimestamp(event.getTimestamp());
            auditLog.setDetails(event.getDetails());
            auditLog.setStateBefore(event.getStateBefore());
            auditLog.setStateAfter(event.getStateAfter());
            auditLog.setMetadata(event.getStateAfter()); 
            auditLog.setErrorDetails(event.getErrorDetails());
            auditLog.setGovernance(event.getGovernance());
            auditLog.setTargetObject(event.getTargetObject());
            auditLogRepository.save(auditLog);

            logger.info().message(AppConstants.LOG_KAFKA_PROCESS_SUCCESS)
                    .objectId(eventId.toString())
                    .objectType("AuditLog")
                    .detail("kafkaEventId", eventId)
                    .detail("eventType", event.getEvent())
                    .detail("tenantId", tenantId)
                    .log();
        } catch (Exception e) {
            String objectIdForLog = (event.getId() != null) ? event.getId().toString() : "generated-uuid";
            logger.error(e)
                .message(AppConstants.LOG_KAFKA_PROCESS_FAILURE)
                .objectId(objectIdForLog)
                .objectType("AuditLog")
                .detail("kafkaEvent", event)
                .log();
        }
    }
    
    /**
     * Formats the object type by capitalizing each word and replacing underscores with spaces.
     * 
     * @param objectType the original object type (e.g., "template_managers", "session_control")
     * @return formatted object type (e.g., "Template Managers", "Session Control")
     */
    private String formatObjectType(String objectType) {
        if (objectType == null || objectType.trim().isEmpty()) {
            return objectType;
        }
        
        // Replace underscores with spaces (keep hyphens as-is for consistency)
        String formatted = objectType.replace("_", " ");
        
        // Split by spaces and capitalize each word
        String[] words = formatted.split(" ");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < words.length; i++) {
            if (words[i].length() > 0) {
                // Capitalize first letter of each word
                result.append(Character.toUpperCase(words[i].charAt(0)));
                if (words[i].length() > 1) {
                    result.append(words[i].substring(1).toLowerCase());
                }
            }
            
            // Add space between words (except for the last word)
            if (i < words.length - 1) {
                result.append(" ");
            }
        }
        
        return result.toString();
    }
}