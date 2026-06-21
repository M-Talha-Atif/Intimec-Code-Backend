package com.aidentx.access_portal_service.config;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for event consumption
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:access-portal-service-group}")
    private String groupId;

    // Consumer factory for AuditLogKafkaEvent objects
    @Bean
    public ConsumerFactory<String, AuditLogKafkaEvent> auditLogConsumerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        // Important: Trust the AuditLogKafkaEvent package
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.aidentx.auditlogdto");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, AuditLogKafkaEvent.class.getName());

        return new DefaultKafkaConsumerFactory<>(
                config,
                new StringDeserializer(),
                new JsonDeserializer<>(AuditLogKafkaEvent.class)
        );
    }

    // Container factory for AuditLogKafkaEvent
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, AuditLogKafkaEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, AuditLogKafkaEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(auditLogConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        // Add error handler with better logging
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
                (record, exception) -> {
                    // Log the error with details
                    System.err.println("Failed to process message: " + record.value());
                    System.err.println("Exception: " + exception.getMessage());
                }
        ));

        return factory;
    }

    // Keep existing String consumer factory for other consumers if needed
    @Bean
    public ConsumerFactory<String, String> stringConsumerFactory() {
        Map<String, Object> config = new HashMap<>();

        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(stringConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler());

        return factory;
    }


           /**
         * Consumer factory for access request events (String JSON)
         * Listens to access-request-events topic from workflows-service
         */
         @Bean
         public ConsumerFactory<String, String> accessRequestEventConsumerFactory() {
             Map<String, Object> config = new HashMap<>();
             config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
             config.put(ConsumerConfig.GROUP_ID_CONFIG, "access-portal-service"); // Fixed group ID
             config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
             config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
             config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
             config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
             config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);

             return new DefaultKafkaConsumerFactory<>(config);
         }

         /**
          * Container factory for access request events
          */
         @Bean("accessRequestEventListenerContainerFactory")
         public ConcurrentKafkaListenerContainerFactory<String, String> accessRequestEventListenerContainerFactory() {
             ConcurrentKafkaListenerContainerFactory<String, String> factory =
                     new ConcurrentKafkaListenerContainerFactory<>();
             factory.setConsumerFactory(accessRequestEventConsumerFactory());
             factory.setConcurrency(3);
             factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

          factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
            (record, exception) -> {
                System.err.println("Failed to process access request event after retries: " + record.value());
                System.err.println("Exception: " + exception.getMessage());
                exception.printStackTrace();
            },
            new FixedBackOff(1000L, 3L) //  Retry 3 times with 1 second delay
    ));
             return factory;
         }



}