package com.aidentx.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.kafka.NotificationEventData;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Kafka producer service for sending notification events.
 * 
 * @author Aidentx Development Team
 * @version 1.0
 * @since 2025-08-21
 */
@Service
public class NotificationEventProducer {
    
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    
    @Value("${notification.kafka.topic:user-notifications}")
    private String notificationTopic;
    
    public NotificationEventProducer(
            @Qualifier("stringKafkaTemplate") KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.logger = loggerFactory.getLogger("NotificationEventProducer", "NOTIFICATION_KAFKA");
    }
    
    /**
     * Sends a password reset notification event to Kafka with tenant context.
     *
     * @param user the user who successfully reset their password
     */
    public void sendPasswordResetNotification(User user) {
        try {
            // Extract tenant ID from user
            String tenantId = user.getTenantId();
            if (tenantId == null || tenantId.trim().isEmpty()) {
                logger.error()
                    .message(AppConstants.ERROR_USER_NO_TENANT_ID)
                    .detail("userId", user.getId().toString())
                    .detail("username", user.getUsername())
                    .log();
                throw new IllegalArgumentException(AppConstants.ERROR_USER_MISSING_TENANT_ID);
            }
            
            // Create user data
            NotificationEventData.UserData userData = new NotificationEventData.UserData(
                user.getEmail(), 
                user.getUsername()
            );
            
            // Create template variables for Velocity processing
            NotificationEventData.TemplateVariables templateVariables = new NotificationEventData.TemplateVariables(
                user.getUsername(),
                user.getEmail()
            );
            
            // Create user notification data
            NotificationEventData.UserNotificationData userNotificationData = new NotificationEventData.UserNotificationData(
                user.getId().toString(),
                userData,
                templateVariables
            );
            
            // Create the notification event with tenant context
            NotificationEventData eventData = new NotificationEventData(
                NotificationEventData.OperationType.PASSWORD_RESET,
                tenantId,
                Collections.singletonList(userNotificationData)
            );
            
            // Serialize to JSON
            String eventJson = objectMapper.writeValueAsString(eventData);
            
            // Send to Kafka
            kafkaTemplate.send(notificationTopic, user.getId().toString(), eventJson);
            
            logger.info()
                .message(AppConstants.INFO_PASSWORD_RESET_NOTIFICATION_SENT)
                .detail("userId", user.getId().toString())
                .detail("username", user.getUsername())
                .detail("email", user.getEmail())
                .detail("tenantId", tenantId)
                .detail("topic", notificationTopic)
                .log();
                
        } catch (JsonProcessingException e) {
            logger.error(e)
                .message(AppConstants.ERROR_NOTIFICATION_JSON_SERIALIZATION)
                .detail("userId", user.getId().toString())
                .detail("username", user.getUsername())
                .detail("tenantId", user.getTenantId())
                .log();
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ERROR_PASSWORD_RESET_NOTIFICATION_FAILED)
                .detail("userId", user.getId().toString())
                .detail("username", user.getUsername())
                .detail("tenantId", user.getTenantId())
                .log();
        }
    }

    /**
     * Sends a password expiration notification event to Kafka for multiple users.
     *
     * @param eventData the notification event data containing users and template variables
     */
    public void sendPasswordExpirationNotificationEvent(NotificationEventData eventData) {
        try {
            // Validate event data
            if (eventData == null || eventData.getTenantId() == null || eventData.getTenantId().trim().isEmpty()) {
                logger.error()
                    .message(AppConstants.ERROR_NOTIFICATION_INVALID_EVENT_DATA)
                    .log();
                throw new IllegalArgumentException(AppConstants.ERROR_NOTIFICATION_MISSING_TENANT_ID);
            }

            if (eventData.getData() == null || eventData.getData().isEmpty()) {
                logger.warn()
                    .message(AppConstants.WARN_NOTIFICATION_NO_DATA)
                    .detail("tenantId", eventData.getTenantId())
                    .log();
                return;
            }
            
            // Serialize to JSON
            String eventJson = objectMapper.writeValueAsString(eventData);
            
            // Use tenant ID as the Kafka key for partitioning
            kafkaTemplate.send(notificationTopic, eventData.getTenantId(), eventJson);
            
            logger.info()
                .message(AppConstants.INFO_PASSWORD_EXPIRATION_NOTIFICATION_SENT)
                .detail("tenantId", eventData.getTenantId())
                .detail("userCount", eventData.getData().size())
                .detail("operationType", eventData.getOperationType())
                .detail("topic", notificationTopic)
                .log();
                
        } catch (JsonProcessingException e) {
            logger.error(e)
                .message(AppConstants.ERROR_PASSWORD_EXPIRATION_NOTIFICATION_JSON_FAILED)
                .detail("tenantId", eventData != null ? eventData.getTenantId() : "unknown")
                .log();
            throw new RuntimeException(AppConstants.ERROR_NOTIFICATION_SERIALIZATION_FAILED, e);
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.ERROR_PASSWORD_EXPIRATION_NOTIFICATION_KAFKA_FAILED)
                .detail("tenantId", eventData != null ? eventData.getTenantId() : "unknown")
                .log();
            throw new RuntimeException(AppConstants.ERROR_NOTIFICATION_SEND_FAILED, e);
        }
    }
}
