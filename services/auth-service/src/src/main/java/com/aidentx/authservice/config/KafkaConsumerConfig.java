package com.aidentx.authservice.config;

import com.aidentx.authservice.events.IdentityLifecycleEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer configuration for Auth service.
 * Configures consumer for identity lifecycle events.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Consumer factory for IdentityLifecycleEvent messages.
     */
    @Bean
    public ConsumerFactory<String, IdentityLifecycleEvent> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "auth-service-identity-sync");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.aidentx.authservice.events,com.aidentx.iam.events");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, IdentityLifecycleEvent.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit for transactional processing
        // Allow each poll to return up to 250 records so a burst of import events
        // (up to 500/batch) is drained quickly across the 3 consumer threads.
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 250);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(IdentityLifecycleEvent.class, false)
        );
    }

    /**
     * Kafka listener container factory for concurrent message processing.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IdentityLifecycleEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, IdentityLifecycleEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // Process 3 partitions concurrently
        factory.getContainerProperties().setAckMode(org.springframework.kafka.listener.ContainerProperties.AckMode.RECORD);
        return factory;
    }
}