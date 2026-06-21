package com.aidentx.applicationpermissions.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized configuration for all Kafka Producers in the application-permissions-service.
 * 
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-01-21
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * A generic producer factory that can be reused by all KafkaTemplates in this
     * service.
     * It serializes keys as Strings and values as JSON.
     * 
     * @return A configured ProducerFactory.
     */
    @Bean
    public ProducerFactory<String, Object> genericProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Disable type information to avoid cross-service deserialization issues
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        // Ensure exactly-once semantics and reliability
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate for publishing identity schema audit events from role management.
     * 
     * @param factory The generic producer factory.
     * @return A KafkaTemplate named "identitySchemaAuditKafkaTemplate".
     */
    @Bean("identitySchemaAuditKafkaTemplate")
    public KafkaTemplate<String, Object> auditKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }
}