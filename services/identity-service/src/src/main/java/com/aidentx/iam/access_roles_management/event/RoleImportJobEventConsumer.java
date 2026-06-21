package com.aidentx.iam.access_roles_management.event;

import com.aidentx.iam.access_roles_management.config.constants.AccessRoleImportConstants;
import com.aidentx.iam.access_roles_management.service.import_jobs.RoleImportJobWorker;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Consumes role import job events and invokes the in-service worker.
 *
 * <p>The listener uses manual acknowledgment. A Kafka message is acknowledged
 * only after {@link RoleImportJobWorker#process(java.util.UUID)} returns. If the
 * worker throws a runtime exception, the message is not acknowledged and the
 * configured Kafka error handling/retry behavior can take over.</p>
 */
@Component
public class RoleImportJobEventConsumer {

    private final RoleImportJobWorker worker;
    private final IUnifiedLogger logger;

    public RoleImportJobEventConsumer(RoleImportJobWorker worker, UnifiedLoggerFactory loggerFactory) {
        this.worker = worker;
        this.logger = loggerFactory.getLogger(getClass(), AccessRoleImportConstants.LOG_CATEGORY);
    }

    /**
     * Handles one role-import start event from Kafka.
     *
     * @param event role import job event
     * @param acknowledgment manual Kafka acknowledgment handle
     */
    @KafkaListener(
            topics = "${access-roles.import.topic:role-import-events}",
            groupId = "${access-roles.import.consumer-group:identity-service-role-import}",
            containerFactory = "jsonKafkaListenerContainerFactory")
    public void onRoleImportJobStarted(RoleImportJobStartedEvent event, Acknowledgment acknowledgment) {
        logger.info()
                .category(AccessRoleImportConstants.LOG_CATEGORY)
                .message(AccessRoleImportConstants.LOG_CONSUME)
                .detail(AccessRoleImportConstants.DETAIL_JOB_ID, event.jobId().toString())
                .detail(AccessRoleImportConstants.DETAIL_TENANT_ID, event.tenantId())
                .detail(AccessRoleImportConstants.DETAIL_ACTOR_ID, event.actorId())
                .log();
        worker.process(event.jobId());
        acknowledgment.acknowledge();
        logger.info()
                .category(AccessRoleImportConstants.LOG_CATEGORY)
                .message(AccessRoleImportConstants.LOG_ACK)
                .detail(AccessRoleImportConstants.DETAIL_JOB_ID, event.jobId().toString())
                .detail(AccessRoleImportConstants.DETAIL_TENANT_ID, event.tenantId())
                .log();
    }
}

