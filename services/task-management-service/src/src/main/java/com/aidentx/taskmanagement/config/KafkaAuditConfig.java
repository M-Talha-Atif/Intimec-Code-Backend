package com.aidentx.taskmanagement.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer configuration for IGA task audit events.
 * <p>
 * Defines a dedicated {@code KafkaTemplate} bean used exclusively by
 * {@link com.aidentx.taskmanagement.audit.IgaTaskAuditPublisher} to forward
 * task lifecycle audit events to the centralized {@code audit-log} Kafka topic.
 * </p>
 */
@Configuration
public class KafkaAuditConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * KafkaTemplate for publishing IGA task audit events to the {@code audit-log} topic.
     *
     * @return a configured {@link KafkaTemplate}
     */
    @Bean("igaTaskAuditKafkaTemplate")
    public KafkaTemplate<String, Object> igaTaskAuditKafkaTemplate() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(props));
    }
}
