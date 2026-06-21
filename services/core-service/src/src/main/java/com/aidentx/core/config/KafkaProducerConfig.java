package com.aidentx.core.config;

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
 * Centralized configuration for all Kafka Producers in the core-service.
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
        // Ensure exactly-once semantics and reliability
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate for publishing identity schema audit events.
     * 
     * @param factory The generic producer factory.
     * @return A KafkaTemplate named "identitySchemaAuditKafkaTemplate".
     */
    @Bean("identitySchemaAuditKafkaTemplate")
    public KafkaTemplate<String, Object> auditKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    /**
     * KafkaTemplate for publishing schema field change events.
     * 
     * @param factory The generic producer factory.
     * @return A KafkaTemplate named "schemaFieldChangeKafkaTemplate".
     */
    @Bean("schemaFieldChangeKafkaTemplate")
    public KafkaTemplate<String, Object> schemaFieldChangeKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    /**
     * KafkaTemplate for publishing relationship change command events.
     * 
     * @param factory The generic producer factory.
     * @return A KafkaTemplate named "relationshipKafkaTemplate".
     */
    @Bean("relationshipKafkaTemplate")
    public KafkaTemplate<String, Object> relationshipKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    /**
     * KafkaTemplate for publishing aggregation lifecycle events (e.g.
     * AggregationCompletedEvent).
     * A dedicated bean is provided so that services can unambiguously inject the
     * correct template
     * using @Qualifier without requiring a @Primary designation among existing
     * templates.
     * 
     * @param factory The generic producer factory.
     * @return A KafkaTemplate named "aggregationEventsKafkaTemplate".
     */
    @Bean("aggregationEventsKafkaTemplate")
    public KafkaTemplate<String, Object> aggregationEventsKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    /**
     * KafkaTemplate for publishing application lifecycle events, such as when an
     * application is registered or decommissioned.
     * 
     * @param factory The generic producer factory.
     * @return A KafkaTemplate named "applicationLifecycleEventsKafkaTemplate".
     */
    @Bean("applicationLifecycleEventsKafkaTemplate")
    public KafkaTemplate<String, Object> applicationLifecycleEventsKafkaTemplate(
            ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }
}