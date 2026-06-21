package com.aidentx.core.notifications_management.service;

import com.aidentx.core.notifications_management.enums.NotificationType;
import com.aidentx.core.notifications_management.model.dto.MarkNotificationViewedResponseDto;
import com.aidentx.core.notifications_management.model.dto.UserNotificationResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing user notifications.
 * <p>
 * This service provides operations for retrieving user notifications by type,
 * marking notifications as viewed, and managing notification pinning status.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
public interface UserNotificationService {
    
    /**
     * Retrieves all notifications of a specific type for the current user.
     * 
     * @param type the notification type to filter by
     * @return a list of user notifications of the specified type
     */
    List<UserNotificationResponseDto> getUserNotificationsByType(NotificationType type);
    
    /**
     * Marks a specific notification as viewed for the current user.
     * 
     * @param notificationId the UUID of the notification to mark as viewed
     * @return response indicating success or failure
     */
    MarkNotificationViewedResponseDto markNotificationAsViewed(UUID notificationId);
    
    /**
     * Pins a specific notification for the current user.
     * 
     * @param notificationId the UUID of the notification to pin
     * @return response indicating success or failure
     */
    MarkNotificationViewedResponseDto pinNotification(UUID notificationId);

    /**
     * Unpins a specific notification for the current user.
     * 
     * @param notificationId the UUID of the notification to unpin
     * @return response indicating success or failure
     */
    MarkNotificationViewedResponseDto unpinNotification(UUID notificationId);
    
    /**
     * Retrieves all pinned notifications for the current user.
     * 
     * @return a list of pinned user notifications
     */
    List<UserNotificationResponseDto> getPinnedNotifications();
    
    /**
     * Retrieves all pinned notifications of a specific type for the current user.
     * 
     * @param type the notification type to filter by
     * @return a list of pinned user notifications of the specified type
     */
    List<UserNotificationResponseDto> getPinnedNotificationsByType(NotificationType type);
}
