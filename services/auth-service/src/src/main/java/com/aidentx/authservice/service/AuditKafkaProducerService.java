package com.aidentx.authservice.service;

import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending audit log events to Kafka.
 */
@Service
public class AuditKafkaProducerService {

    private final IUnifiedLogger logger;

    /** Kafka template used to publish messages. */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /** Kafka topic to which audit log events will be sent. */
    private final String auditLogTopic;

    /**
     * Constructs a new {@code AuditKafkaProducerService}.
     *
     * @param unifiedLoggerFactory Factory for creating unified logger
     * @param kafkaTemplate   the KafkaTemplate used for sending messages
     * @param auditLogTopic   the topic name for audit logs, injected from application properties
     */
    public AuditKafkaProducerService(UnifiedLoggerFactory unifiedLoggerFactory,
                                     KafkaTemplate<String, Object> kafkaTemplate,
                                     @Value("${auditlog.kafka.topic}") String auditLogTopic) {
        this.logger = unifiedLoggerFactory.getLogger("AuditKafkaProducerService", AppConstants.LOG_CATEGORY_AUTH);
        this.kafkaTemplate = kafkaTemplate;
        this.auditLogTopic = auditLogTopic;
        logger.info().message(AppConstants.INFO_KAFKA_PRODUCER_INITIALIZED).detail("topic", auditLogTopic).log();
    }

    /**
     * Sends an audit log event to the configured Kafka topic.
     *
     * @param auditLog the audit log object to be sent
     */
    public void sendAuditLog(Object auditLog) {
        try {
            logger.debug().message(AppConstants.DEBUG_KAFKA_SENDING_AUDIT)
                    .detail("topic", auditLogTopic)
                    .detail("auditLogType", auditLog != null ? auditLog.getClass().getSimpleName() : "null")
                    .log();
            
            kafkaTemplate.send(auditLogTopic, auditLog);
            
            logger.info().message(AppConstants.INFO_KAFKA_AUDIT_SENT)
                    .detail("topic", auditLogTopic)
                    .log();
                    
        } catch (Exception e) {
            logger.error(e).message(AppConstants.ERROR_KAFKA_AUDIT_FAILED)
                    .detail("topic", auditLogTopic)
                    .detail("auditLogType", auditLog != null ? auditLog.getClass().getSimpleName() : "null")
                    .log();
        }
    }
}