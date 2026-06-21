package com.aidentx.access_portal_service.config;


import lombok.extern.slf4j.Slf4j;
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

/**
 * Kafka producer configuration for access-portal-service.
 * Configures separate producers for user notifications and audit logs.
 */
@Slf4j
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @PostConstruct
    public void init() {
        log.info("Kafka Producer Configuration initialized");
        log.info("Kafka Bootstrap Servers: {}", bootstrapServers);
        log.info("Notification Producer: access-portal-service-producer");
        log.info("Audit Log Producer: access-portal-service-audit-producer");
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
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "access-portal-service-producer");
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); // all replicas must acknowledge
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // ensure exactly-once delivery

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
        configProps.put(ProducerConfig.CLIENT_ID_CONFIG, "access-portal-service-audit-producer");
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
}

