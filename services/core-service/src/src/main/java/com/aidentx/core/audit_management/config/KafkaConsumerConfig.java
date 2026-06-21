package com.aidentx.core.audit_management.config;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for receiving AuditLog messages from Kafka and deserializing them.
 *
 * This configures a consumer factory and listener container factory for the 'audit-log' topic.
 * The consumer expects JSON messages that map to the AuditLog entity.
 *
 * - Topic: audit-log
 * - Group ID: auditlog-service-group
 * - Bootstrap servers: configured via spring.kafka.bootstrap-servers
 *
 * Usage: Inject the listener container factory into your @KafkaListener beans.
 */
@Configuration
public class KafkaConsumerConfig {

    /**
     * Kafka bootstrap server(s), injected from application properties.
     */
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:auditlog-service-group}")
    private String consumerGroupId;

    /**
     * Creates a {@link ConsumerFactory} configured to deserialize JSON messages into {@link AuditLogKafkaEvent} objects.
     *
     * @return a {@code ConsumerFactory} for consuming {@code AuditLogKafkaEvent} messages
     */
    @Bean
    public ConsumerFactory<String, AuditLogKafkaEvent> auditLogConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*"); // Allows deserializing objects from any package
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AuditLogKafkaEvent.class.getName());
        return new DefaultKafkaConsumerFactory<>(
            props,
            new StringDeserializer(),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(AuditLogKafkaEvent.class))
        );
    }

    /**
     * Creates a {@link ConcurrentKafkaListenerContainerFactory} for processing {@link AuditLogKafkaEvent} messages.
     * <p>
     * This factory is used by Spring Kafka to create listener containers that consume messages
     * using the configured {@link ConsumerFactory}.
     *
     * @return a configured Kafka listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuditLogKafkaEvent> auditLogKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AuditLogKafkaEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(auditLogConsumerFactory());
        
        // Configure error handling for deserialization failures
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
            (consumerRecord, exception) -> {
                System.err.println("Error processing Kafka message: " + exception.getMessage());
                System.err.println("Skipping problematic message at offset: " + consumerRecord.offset());
            }
        ));
        
        return factory;
    }
}

