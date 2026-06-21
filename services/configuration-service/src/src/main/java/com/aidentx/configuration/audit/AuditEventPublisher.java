package com.aidentx.configuration.audit;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditEventPublisher {

    private static final String AUDIT_TOPIC = "audit-log";
    private static final String LOG_CATEGORY = "audit-publisher";

    private final IUnifiedLogger logger;
    private final KafkaTemplate<String, AuditLogKafkaEvent> kafkaTemplate;

    public AuditEventPublisher(KafkaTemplate<String, AuditLogKafkaEvent> kafkaTemplate,
            UnifiedLoggerFactory unifiedLoggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = unifiedLoggerFactory.getLogger("AuditEventPublisher", LOG_CATEGORY);
    }

    public void publishEvent(AuditLogKafkaEvent event) {
        try {
            logger.info()
                    .category(LOG_CATEGORY)
                    .message("Publishing audit event")
                    .details(Map.of(
                            "action", event.getEvent(),
                            "configKey", event.getObjectId().toString(),
                            "kafkaTopic", AUDIT_TOPIC))
                    .log();

            kafkaTemplate.send(AUDIT_TOPIC, event.getObjectId().toString(), event);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message("Failed to publish audit event to Kafka")
                    .details(Map.of(
                            "kafkaTopic", AUDIT_TOPIC,
                            "exceptionType", e.getClass().getSimpleName()))
                    .log();

        }
    }
}