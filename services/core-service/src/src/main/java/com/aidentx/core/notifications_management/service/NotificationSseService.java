package com.aidentx.core.notifications_management.service;

import com.aidentx.core.notifications_management.model.dto.NotificationSseDto;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Service interface for Server-Sent Events (SSE) notification operations.
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-25
 */
public interface NotificationSseService {
    
    /**
     * Creates an SSE connection for the current user.
     * User ID is extracted from JWT token.
     * 
     * @return SseEmitter for streaming notifications
     */
    SseEmitter createSseConnection();
    
    /**
     * Sends a real-time notification to a specific user.
     * 
     * @param userId the user ID to send notification to
     * @param notification the notification DTO to send
     */
    void sendNotificationToUser(String userId, NotificationSseDto notification);
    
    /**
     * Removes SSE connection for a user.
     * 
     * @param userId the user ID to remove connection for
     */
    void removeSseConnection(String userId);
}
