package com.aidentx.core.config;

import com.aidentx.iam.events.AggregationCompletedEvent;
import com.aidentx.iam.events.IdentityLifecycleEvent;
import com.aidentx.core.notifications_management.model.dto.NotificationEventData;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized configuration for all Kafka Consumers in the core-service.
 */
@Configuration("provisioningKafkaConsumerConfig")
public class ProvisioningKafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // --- Consumer for Aggregation Completed Events (for CorrelationService) ---

    @Bean
    public ConsumerFactory<String, AggregationCompletedEvent> aggregationEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "core-service-correlation-group");
        // Using a JsonDeserializer that is aware of the specific event type.
        // `false` means it will trust all packages, which is fine for internal
        // services.
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(AggregationCompletedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AggregationCompletedEvent> aggregationEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AggregationCompletedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(aggregationEventConsumerFactory());
        // A more advanced implementation would add a retry/DLQ error handler here.
        return factory;
    }

    // --- NEW: Consumer for Identity Lifecycle Events (for ProvisioningService) ---

    @Bean
    public ConsumerFactory<String, IdentityLifecycleEvent> lifecycleEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "core-service-provisioning-group");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(IdentityLifecycleEvent.class, false));
    }

    /**
     * This is the bean that was missing. It is named
     * 'lifecycleEventKafkaListenerContainerFactory'
     * and is specifically configured to handle IdentityLifecycleEvent messages.
     * The @KafkaListener in ProvisioningService refers to this bean by name.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IdentityLifecycleEvent> lifecycleEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, IdentityLifecycleEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(lifecycleEventConsumerFactory());
        // A more advanced implementation would add a retry/DLQ error handler here as
        // well.
        return factory;
    }

    // --- Consumer for Notification Events ---

    @Bean
    public ConsumerFactory<String, NotificationEventData> notificationEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "core-service-notifications");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(NotificationEventData.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEventData> notificationEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEventData> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationEventConsumerFactory());
        return factory;
    }
}