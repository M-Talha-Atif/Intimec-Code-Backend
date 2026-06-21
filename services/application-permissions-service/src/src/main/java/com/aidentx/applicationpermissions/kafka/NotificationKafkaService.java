package com.aidentx.applicationpermissions.kafka;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending notification events via Kafka.
 * <p>
 * This service handles sending notification events from application-permissions-service
 * to the core-service via Kafka messaging. It ensures proper tenant isolation and
 * handles role assignment notifications.
 * </p>
 * 
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-10-20
 */
@Service
public class NotificationKafkaService {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final IUnifiedLogger logger;
    private final String notificationTopic;
    
    @Autowired
    public NotificationKafkaService(
            @Qualifier("identitySchemaAuditKafkaTemplate") KafkaTemplate<String, Object> kafkaTemplate,
            UnifiedLoggerFactory loggerFactory,
            @Value("${notification.kafka.topic:user-notifications}") String notificationTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.logger = loggerFactory.getLogger(
                AppConstants.NOTIFICATION_KAFKA_SERVICE_LOGGER_NAME, 
                AppConstants.LOG_CATEGORY_NOTIFICATION_KAFKA);
        this.notificationTopic = notificationTopic;
    }
    
    /**
     * Sends a notification event to Kafka for processing by core-service.
     * 
     * @param eventData the notification event data
     * @param tenantId the tenant ID for message partitioning
     */
    public void sendNotificationEvent(NotificationEventData eventData, String tenantId) {
        try {
            logger.info()
                .message(AppConstants.LOG_SENDING_NOTIFICATION_EVENT_TO_KAFKA)
                .detail(AppConstants.DETAIL_KEY_OPERATION_TYPE, eventData.getOperationType().name())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, eventData.getTenantId())
                .detail(AppConstants.DETAIL_KEY_USER_COUNT, eventData.getData().size())
                .detail(AppConstants.DETAIL_KEY_TOPIC, notificationTopic)
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_KAFKA_EVENT)
                .log();
            
            // Use tenant ID as the message key for proper partitioning
            kafkaTemplate.send(notificationTopic, tenantId, eventData);
            
            logger.debug()
                .message(AppConstants.LOG_NOTIFICATION_EVENT_SENT_SUCCESSFULLY)
                .detail(AppConstants.DETAIL_KEY_OPERATION_TYPE, eventData.getOperationType().name())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, eventData.getTenantId())
                .detail(AppConstants.DETAIL_KEY_TOPIC, notificationTopic)
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_KAFKA_EVENT)
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_FAILED_TO_SEND_NOTIFICATION_EVENT_TO_KAFKA)
                .detail(AppConstants.DETAIL_KEY_OPERATION_TYPE, eventData.getOperationType().name())
                .detail(AppConstants.DETAIL_KEY_TENANT_ID, eventData.getTenantId())
                .detail(AppConstants.DETAIL_KEY_TOPIC, notificationTopic)
                .detail(AppConstants.DETAIL_KEY_ERROR, e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_KAFKA_EVENT)
                .log();
        }
    }
}