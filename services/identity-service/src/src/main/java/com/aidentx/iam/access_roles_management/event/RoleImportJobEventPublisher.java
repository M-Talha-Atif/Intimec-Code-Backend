package com.aidentx.iam.access_roles_management.event;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleImportConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes access-role import job events to Kafka.
 *
 * <p>The import service calls this only after the job/item transaction commits.
 * That ordering is important: the consumer may run immediately, so all queued
 * rows must be visible in the database before the event is delivered.</p>
 *
 * <p>The job id is used as the Kafka message key so all events for the same job
 * are routed consistently by Kafka partitioning.</p>
 */
@Component
public class RoleImportJobEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IUnifiedLogger logger;
    private final String topic;

    public RoleImportJobEventPublisher(
            @Qualifier("roleImportKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            UnifiedLoggerFactory loggerFactory,
            @Value("${" + AccessRoleImportConstants.CONFIG_TOPIC + ":" + AccessRoleImportConstants.TOPIC_DEFAULT + "}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleImportConstants.LOG_CATEGORY);
        this.topic = topic;
    }

    /**
     * Sends the job-start event to the configured topic.
     *
     * @param event immutable import job event containing job, tenant, and actor context
     */
    public void publish(RoleImportJobStartedEvent event) {
        logger.info()
                .category(AccessRoleImportConstants.LOG_CATEGORY)
                .message(AccessRoleImportConstants.LOG_PUBLISH)
                .detail(AccessRoleImportConstants.DETAIL_JOB_ID, event.jobId().toString())
                .detail(AccessRoleImportConstants.DETAIL_TENANT_ID, event.tenantId())
                .detail(AccessRoleImportConstants.DETAIL_ACTOR_ID, event.actorId())
                .detail("topic", topic)
                .log();
        kafkaTemplate.send(topic, event.jobId().toString(), event);
    }
}


