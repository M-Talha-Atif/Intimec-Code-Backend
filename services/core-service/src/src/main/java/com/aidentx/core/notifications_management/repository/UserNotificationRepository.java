package com.aidentx.core.notifications_management.repository;

import com.aidentx.core.notifications_management.enums.NotificationType;
import com.aidentx.core.notifications_management.model.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link UserNotification} entities.
 * <p>
 * This repository provides CRUD operations and custom query methods for user notifications,
 * including filtering by user, notification, and viewing status.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
    
    /**
     * Retrieves all notifications assigned to a specific user.
     * 
     * @param userId the UUID of the user
     * @return a list of user notifications for the specified user
     */
    List<UserNotification> findByUserId(UUID userId);
    
    /**
     * Retrieves notifications for a specific user filtered by viewing status.
     * 
     * @param userId the UUID of the user
     * @param isViewed the viewing status to filter by (true for viewed, false for unviewed)
     * @return a list of user notifications matching the criteria
     */
    List<UserNotification> findByUserIdAndIsViewed(UUID userId, Boolean isViewed);
    
    /**
     * Retrieves all user assignments for a specific notification.
     * 
     * @param notificationId the UUID of the notification
     * @return a list of user notifications for the specified notification
     */
    List<UserNotification> findByNotificationId(UUID notificationId);
    
    /**
     * Finds a specific user notification relationship.
     * 
     * @param userId the UUID of the user
     * @param notificationId the UUID of the notification
     * @return an optional containing the user notification if found
     */
    Optional<UserNotification> findByUserIdAndNotificationId(UUID userId, UUID notificationId);
    
    /**
     * Counts the number of unread notifications for a specific user.
     * 
     * @param userId the UUID of the user
     * @return the count of unread notifications
     */
    @Query("SELECT COUNT(un) FROM UserNotification un WHERE un.userId = :userId AND un.isViewed = false")
    Long countUnreadNotificationsByUserId(@Param("userId") UUID userId);
    
    /**
     * Counts the total number of notifications assigned to a user.
     * 
     * @param userId the UUID of the user
     * @return the total count of notifications for the user
     */
    Long countByUserId(UUID userId);
    
    /**
     * Marks all notifications as viewed for a specific user.
     * 
     * @param userId the UUID of the user
     * @return the number of updated records
     */
    @Modifying
    @Query("UPDATE UserNotification un SET un.isViewed = true WHERE un.userId = :userId AND un.isViewed = false")
    int markAllAsViewedByUserId(@Param("userId") UUID userId);
    
    /**
     * Deletes all user notification relationships for a specific notification.
     * 
     * @param notificationId the UUID of the notification
     */
    void deleteByNotificationId(UUID notificationId);
    
    /**
     * Retrieves user notifications with notification details for a specific user and notification type.
     * 
     * @param userId the UUID of the user
     * @param notificationType the type of notifications to retrieve
     * @return a list of user notifications with notification details
     */
    @Query("SELECT un FROM UserNotification un JOIN Notification n ON un.notificationId = n.id " +
           "WHERE un.userId = :userId AND n.type = :notificationType " +
           "ORDER BY n.id DESC")
    List<UserNotification> findByUserIdAndNotificationType(@Param("userId") UUID userId,
                                                           @Param("notificationType") NotificationType notificationType);
    
    /**
     * Marks a specific notification as viewed for a user.
     * 
     * @param userId the UUID of the user
     * @param notificationId the UUID of the notification
     * @return the number of updated records (should be 1 if successful)
     */
    @Modifying
    @Query("UPDATE UserNotification un SET un.isViewed = true " +
           "WHERE un.userId = :userId AND un.notificationId = :notificationId AND un.isViewed = false")
    int markAsViewedByUserIdAndNotificationId(@Param("userId") UUID userId, 
                                            @Param("notificationId") UUID notificationId);
    
    /**
     * Updates the pinned status of a specific notification for a user.
     * 
     * @param userId the UUID of the user
     * @param notificationId the UUID of the notification
     * @param isPinned the new pinned status (true to pin, false to unpin)
     * @return the number of updated records (should be 1 if successful)
     */
    @Modifying
    @Query("UPDATE UserNotification un SET un.isPinned = :isPinned " +
           "WHERE un.userId = :userId AND un.notificationId = :notificationId")
    int updatePinnedStatusByUserIdAndNotificationId(@Param("userId") UUID userId, 
                                                   @Param("notificationId") UUID notificationId,
                                                   @Param("isPinned") Boolean isPinned);
    
    /**
     * Retrieves all pinned notifications for a specific user.
     * 
     * @param userId the UUID of the user
     * @return a list of pinned user notifications for the specified user
     */
    List<UserNotification> findByUserIdAndIsPinned(UUID userId, Boolean isPinned);
    
    /**
     * Retrieves pinned notifications for a specific user filtered by notification type.
     * 
     * @param userId the UUID of the user
     * @param notificationType the type of notifications to retrieve
     * @param isPinned the pinned status to filter by
     * @return a list of pinned user notifications matching the criteria
     */
    @Query("SELECT un FROM UserNotification un JOIN Notification n ON un.notificationId = n.id " +
           "WHERE un.userId = :userId AND n.type = :notificationType AND un.isPinned = :isPinned " +
           "ORDER BY n.id DESC")
    List<UserNotification> findByUserIdAndNotificationTypeAndIsPinned(@Param("userId") UUID userId,
                                                                     @Param("notificationType") NotificationType notificationType,
                                                                     @Param("isPinned") Boolean isPinned);

    /**
     * Marks old WORKFLOW_APPROVAL_REQUESTED notifications as viewed for a specific user.
     * Only marks notifications older than 1 hour (created_at < current_time - 3600 seconds).
     * 
     * @param userId the UUID of the user
     * @return the number of updated records
     */
}
