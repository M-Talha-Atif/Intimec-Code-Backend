package com.aidentx.core.notifications_management.service;

import com.aidentx.core.notifications_management.model.dto.NotificationEventData;

/**
 * Service interface for processing notification events.
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-21
 */
public interface NotificationProcessingService {
    
    /**
     * Processes a notification event received from Kafka.
     * 
     * @param eventData the notification event data
     */
    void processNotificationEvent(NotificationEventData eventData);
}
