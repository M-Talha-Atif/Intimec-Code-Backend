package com.aidentx.iam.config;

import com.aidentx.iam.events.IdentityLifecycleEvent;
import com.aidentx.iam.events.ReconciliationRequiredEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralized configuration for all Kafka Producers in the Identity service.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${access-roles.import.topic:role-import-events}")
    private String roleImportEventsTopic;

    /**
     * A generic producer factory that can be reused by all KafkaTemplates.
     * It serializes keys as Strings and values as JSON.
     *
     * <p>Throughput optimizations applied for high-volume import workloads:
     * <ul>
     *   <li>{@code linger.ms=5}: waits up to 5ms before sending, allowing multiple
     *       identity.created events from the same import batch to be coalesced into a
     *       single network write instead of N individual round-trips.</li>
     *   <li>{@code batch.size=65536} (64KB): larger accumulator buffer so burst events
     *       from a 500-record import batch fill a meaningful payload per Kafka send.</li>
     *   <li>{@code compression.type=gzip}: JSON payloads compress well with gzip
     *       using pure Java (no native libs), avoiding Snappy glibc issues on Alpine.</li>
     *   <li>{@code acks=all} + {@code enable.idempotence=true}: exactly-once producer
     *       semantics — no duplicate users even if the import job retries a batch.</li>
     * </ul>
     */
    @Bean
    public ProducerFactory<String, Object> genericProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // Batching: coalesce burst events from import batches into fewer network round-trips
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536);
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Declares the identity-lifecycle-events topic with 3 partitions.
     * Spring Kafka's KafkaAdmin will auto-create or validate this on startup,
     * ensuring consumer concurrency=3 is always fully utilized.
     */
    @Bean
    public KafkaAdmin.NewTopics identityLifecycleTopics() {
        return new KafkaAdmin.NewTopics(
            TopicBuilder.name("identity-lifecycle-events")
                .partitions(3)
                .replicas(1)
                .build()
        );
    }

    /**
     * Declares the access-role import job topic used by the async role import worker.
     */
    @Bean
    public KafkaAdmin.NewTopics roleImportTopics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(roleImportEventsTopic)
                        .partitions(3)
                        .replicas(1)
                        .build()
        );
    }

    /**
     * KafkaTemplate for publishing IDENTITY audit events.
     * This is the bean the application is failing to find.
     */
    @Bean("identityAuditKafkaTemplate")
    public KafkaTemplate<String, Object> identityAuditKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    /**
     * KafkaTemplate for publishing SCHEMA audit events. Note the distinct qualifier
     * name.
     */
    @Bean("identitySchemaAuditKafkaTemplate")
    public KafkaTemplate<String, Object> identitySchemaAuditKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    /**
     * KafkaTemplate for publishing asynchronous access-role import job events.
     */
    @Bean("roleImportKafkaTemplate")
    public KafkaTemplate<String, Object> roleImportKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    /**
     * KafkaTemplate for publishing relationship change command events.
     */
    @Bean("relationshipKafkaTemplate")
    public KafkaTemplate<String, Object> relationshipKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    /**
     * KafkaTemplate for publishing reconciliation DLQ records as Object.
     * This avoids ambiguity when injecting a KafkaTemplate<String, Object>.
     */
    @Bean("reconciliationObjectKafkaTemplate")
    public KafkaTemplate<String, Object> reconciliationObjectKafkaTemplate(ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    /**
     * KafkaTemplate for publishing identity reconciliation events.
     * Provides the exact generic type needed by IdentityServiceImpl.
     */
    @Bean("reconciliationKafkaTemplate")
    public KafkaTemplate<String, ReconciliationRequiredEvent> reconciliationKafkaTemplate(
            ProducerFactory<String, Object> factory) {
        // Reuse the generic factory; type is erased at runtime and Spring Kafka handles
        // serialization.
        @SuppressWarnings({ "rawtypes", "unchecked" })
        KafkaTemplate<String, ReconciliationRequiredEvent> template = new KafkaTemplate(factory);
        return template;
    }

    /**
     * A dedicated, strongly-typed KafkaTemplate for publishing identity lifecycle
     * events.
     * This ensures type safety for producers.
     */
    @Bean("lifecycleEventKafkaTemplate")
    public KafkaTemplate<String, IdentityLifecycleEvent> lifecycleEventKafkaTemplate(
            ProducerFactory<String, Object> factory) {
        // We can safely reuse the generic producer factory.
        // Spring's JsonSerializer will correctly handle the 'IdentityLifecycleEvent'
        // type.
        @SuppressWarnings({ "rawtypes", "unchecked" })
        KafkaTemplate<String, IdentityLifecycleEvent> template = new KafkaTemplate(factory);
        return template;
    }
}