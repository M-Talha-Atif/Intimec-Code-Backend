package com.aidentx.iam.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import com.aidentx.iam.events.IdentityLifecycleEvent;
import com.aidentx.iam.events.ReconciliationRequiredEvent;
import com.aidentx.iam.model.dto.events.RelationshipChangeEvent;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;

/**
 * Kafka consumer configuration for receiving schema field change events from
 * core-service.
 * 
 * This configures a consumer factory and listener container factory for the
 * 'schema-field-changes' topic.
 * The consumer expects JSON messages that map to SchemaFieldChangeEvent.
 *
 * @author aidentx
 * @since 1.0
 */
@Configuration
public class KafkaConsumerConfig {

    private final IUnifiedLogger logger;

    public KafkaConsumerConfig(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(KafkaConsumerConfig.class);
    }

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // The topic where failed reconciliation messages will be sent.
    @Value("identity-reconciliation-dlq")
    private String reconciliationDlqTopic;

    /**
     * Creates a generic ConsumerFactory for consuming JSON-based events.
     * It's configured to deserialize keys as Strings and values into generic
     * Objects,
     * allowing the listener to specify the target payload type.
     *
     * @return A ConsumerFactory for consuming JSON messages.
     */
    @Bean
    public ConsumerFactory<String, Object> jsonConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "identity-service-policy-sync-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        // Configure the JsonDeserializer to trust packages where our event DTOs are
        // located.
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>();
        jsonDeserializer.addTrustedPackages(
                "com.aidentx.iam.events",
                "com.aidentx.iam.access_roles_management.event");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), jsonDeserializer);
    }

    /**
     * The ConcurrentKafkaListenerContainerFactory bean that our listeners will use.
     * It is configured for manual acknowledgment, which gives our listeners full
     * control
     * over message processing confirmation.
     *
     * @return A configured listener container factory.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> jsonKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(jsonConsumerFactory());
        // Set acknowledgment mode to MANUAL, as our listeners use
        // Acknowledgment.acknowledge().
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, RelationshipChangeEvent> relationshipEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "identities-relationship-consumer-group");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<RelationshipChangeEvent> deserializer = new JsonDeserializer<>(RelationshipChangeEvent.class);
        deserializer.addTrustedPackages("*");

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RelationshipChangeEvent> relationshipEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RelationshipChangeEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(relationshipEventConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }

    @Bean
    public ConsumerFactory<String, ReconciliationRequiredEvent> reconciliationConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "identity-service-reconciliation-group");
        // For reconciliation, we use automatic commits because the DefaultErrorHandler
        // manages offsets for retries and DLQ, which works best with this mode.
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(ReconciliationRequiredEvent.class, false) // `false` disables type header removal
        );
    }

    @Bean
    public DefaultErrorHandler reconciliationErrorHandler(
            @Qualifier("reconciliationObjectKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate) {
        // Publishes failed records to the DLQ topic after retries are exhausted.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> {
                    logger.error(exception).message("Message sent to DLQ after exhausting retries.")
                            .detail("targetTopic", reconciliationDlqTopic)
                            .detail("sourceTopic", record.topic())
                            .detail("key", record.key())
                            .log();
                    return new org.apache.kafka.common.TopicPartition(reconciliationDlqTopic, record.partition());
                });

        // Retry 3 times with a 5-second delay between attempts.
        FixedBackOff backOff = new FixedBackOff(5000L, 3L);

        // The error handler that combines the retry and DLQ logic.
        return new DefaultErrorHandler(recoverer, backOff);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ReconciliationRequiredEvent> reconciliationKafkaListenerContainerFactory(
            ConsumerFactory<String, ReconciliationRequiredEvent> reconciliationConsumerFactory,
            DefaultErrorHandler reconciliationErrorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, ReconciliationRequiredEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(reconciliationConsumerFactory);
        factory.setCommonErrorHandler(reconciliationErrorHandler);

        return factory;
    }

    @Bean
    public ConsumerFactory<String, IdentityLifecycleEvent> lifecycleEventConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "core-service-provisioning-group");
        // Don't auto-commit. Let the listener handle it.
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(IdentityLifecycleEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, IdentityLifecycleEvent> lifecycleEventKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, IdentityLifecycleEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(lifecycleEventConsumerFactory());
        // For simple, non-critical events, basic AckMode can be used.
        // For more critical operations, a retry/DLQ error handler would be added here.
        return factory;
    }
}
