package com.aidentx.core.notifications_management.config;

import com.aidentx.core.notifications_management.model.dto.NotificationEventData;
import com.aidentx.core.notifications_management.service.NotificationProcessingService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for notification events that handles multi-user notification processing.
 * <p>
 * This consumer listens to notification events from Kafka topics and processes them
 * for multiple users simultaneously. It receives auto-deserialized NotificationEventData objects
 * and delegates the processing to NotificationProcessingService.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Multi-user notification support from single Kafka event</li>
 *   <li>Auto-deserialization via Kafka configuration</li>
 *   <li>Detailed logging for monitoring and debugging</li>
 *   <li>Fault tolerance with proper error logging</li>
 * </ul>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-21
 */
@Component
public class NotificationKafkaConsumer {
    
    private final NotificationProcessingService notificationProcessingService;
    private final IUnifiedLogger logger;
    
    /**
     * Constructor for NotificationKafkaConsumer.
     * 
     * @param notificationProcessingService service for processing notification events
     * @param loggerFactory factory for creating unified loggers
     */
    @Autowired
    public NotificationKafkaConsumer(
            NotificationProcessingService notificationProcessingService,
            UnifiedLoggerFactory loggerFactory) {
        this.notificationProcessingService = notificationProcessingService;
        this.logger = loggerFactory.getLogger(AppConstants.KAFKA_CONSUMER_LOGGER_NAME, AppConstants.LOG_CATEGORY_KAFKA_CONSUMER);
    }
    /**
     * Handles notification events received from Kafka.
     * <p>
     * This method processes incoming Kafka records by:
     * <ul>
     *   <li>Logging Kafka record details (topic, partition, offset)</li>
     *   <li>Processing the auto-deserialized NotificationEventData</li>
     *   <li>Delegating processing to NotificationProcessingService</li>
     *   <li>Handling errors gracefully with comprehensive logging</li>
     * </ul>
     * </p>
     * 
     * @param record the Kafka consumer record containing notification event data
     */
    @KafkaListener(topics = "${notification.kafka.topic:user-notifications}", 
                   groupId = "${notification.kafka.group-id:core-service-notifications}")
    public void handleNotificationEvent(ConsumerRecord<String, NotificationEventData> record) {
        logger.info()
            .message(AppConstants.LOG_KAFKA_EVENT_RECEIVED)
            .detail(AppConstants.DETAIL_KAFKA_TOPIC, record.topic())
            .detail(AppConstants.DETAIL_KAFKA_PARTITION, record.partition())
            .detail(AppConstants.DETAIL_KAFKA_OFFSET, record.offset())
            .detail(AppConstants.DETAIL_KAFKA_KEY, record.key())
            .log();
        
        try {
            // Get the already deserialized notification event data
            NotificationEventData eventData = record.value();
            
            logger.info()
                .message(AppConstants.LOG_KAFKA_EVENT_DESERIALIZED)
                .detail(AppConstants.DETAIL_OPERATION_TYPE, eventData.getOperationType().name())
                .detail(AppConstants.DETAIL_USER_COUNT, eventData.getData().size())
                .log();
            
            // Process the notification event
            notificationProcessingService.processNotificationEvent(eventData);
            
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_KAFKA_EVENT_PROCESSING_FAILED)
                .detail(AppConstants.DETAIL_KAFKA_TOPIC, record.topic())
                .detail(AppConstants.DETAIL_KAFKA_PARTITION, record.partition())
                .detail(AppConstants.DETAIL_KAFKA_OFFSET, record.offset())
                .detail(AppConstants.DETAIL_KAFKA_MESSAGE_VALUE, record.value())
                .log();
        }
    }
}