package com.aidentx.workflowsservice.config;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Kafka producer configuration for workflows-service.
 * Configures separate producers for user notifications and audit logs.
 * Access request events (JSON serialization)
 */
@Configuration
public class KafkaProducerConfig {
    
    private final IUnifiedLogger logger;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    public KafkaProducerConfig(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(KafkaProducerConfig.class, LOG_CATEGORY_WORKFLOW_CONFIG);
    }

    @PostConstruct
    public void init() {
        logger.info().message(LOG_KAFKA_PRODUCER_CONFIG_INITIALIZED).category(LOG_CATEGORY_WORKFLOW_CONFIG).log();
        logger.info().message(LOG_KAFKA_BOOTSTRAP_SERVERS).category(LOG_CATEGORY_WORKFLOW_CONFIG).detail("bootstrapServers", bootstrapServers).log();
        logger.info().message(LOG_KAFKA_NOTIFICATION_PRODUCER).category(LOG_CATEGORY_WORKFLOW_CONFIG).detail("clientId", "workflows-service-producer").log();
        logger.info().message(LOG_KAFKA_AUDIT_PRODUCER).category(LOG_CATEGORY_WORKFLOW_CONFIG).detail("clientId", "workflows-service-audit-producer").log();
    }

    /**
     * Kafka producer factory for user notifications (String serialization).
     */
    @Bean
    public ProducerFactory<String, String> notificationProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "workflows-service-producer");
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate for user notifications (manual JSON serialization).
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Kafka producer factory for audit logs (JSON serialization).
     */
    @Bean
    public ProducerFactory<String, Object> auditProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "workflows-service-audit-producer");
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate for audit logs (automatic JSON serialization).
     * Named to match pattern used in other services.
     */
    @Bean("identitySchemaAuditKafkaTemplate")
    public KafkaTemplate<String, Object> auditKafkaTemplate() {
        return new KafkaTemplate<>(auditProducerFactory());
    }

    // ===================================================================
    // NEW: Access Request Events Producer Configuration
    // ===================================================================

    /**
     * Kafka producer factory for access request events (JSON serialization).
     * Dedicated producer for publishing workflow lifecycle events to access-portal-service.
     */
    @Bean
    public ProducerFactory<String, Object> accessRequestEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "workflows-service-access-request-producer");
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // ensure durability, all replicas must ack
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // to avoid duplicate events

        // Trust all packages for JSON serialization (no type info headers needed)
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate for access request events (automatic JSON serialization).
     * Used by AccessPortalEventPublisher to send events to access-portal-service.
     */
    @Bean("accessRequestEventKafkaTemplate")
    public KafkaTemplate<String, Object> accessRequestEventKafkaTemplate() {
        return new KafkaTemplate<>(accessRequestEventProducerFactory());
    }
}
