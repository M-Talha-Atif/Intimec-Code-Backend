package com.aidentx.core.notifications_management.model.entity;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing the relationship between users and notifications.
 * <p>
 * This entity manages which notifications are assigned to which users
 * and tracks their viewing status. It serves as a junction table between
 * users and notifications with additional metadata.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Entity
@Table(name = "user_notifications")
public class UserNotification {
    
    /**
     * Unique identifier for the user notification relationship.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    /**
     * UUID of the user who should receive this notification.
     */
    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;
    
    /**
     * UUID of the notification assigned to the user.
     */
    @Column(name = "notification_id", nullable = false, columnDefinition = "UUID")
    private UUID notificationId;
    
    /**
     * Flag indicating whether the notification has been viewed by the user.
     */
    @Column(name = "is_viewed", nullable = false)
    private Boolean isViewed = false;
    
    /**
     * Flag indicating whether the notification has been pinned by the user.
     */
    @Column(name = "is_pinned", nullable = false)
    private Boolean isPinned = false;
    
    /**
     * Default constructor for JPA.
     */
    public UserNotification() {}
    
    /**
     * Creates a new user notification relationship.
     * The notification is marked as unviewed and unpinned by default.
     * 
     * @param userId the UUID of the user
     * @param notificationId the UUID of the notification
     */
    public UserNotification(UUID userId, UUID notificationId) {
        this.userId = userId;
        this.notificationId = notificationId;
        this.isViewed = false;
        this.isPinned = false;
    }
    
    /**
     * Gets the unique identifier of the user notification relationship.
     * 
     * @return the relationship ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Sets the unique identifier of the user notification relationship.
     * 
     * @param id the relationship ID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }
    
    /**
     * Gets the UUID of the user.
     * 
     * @return the user ID
     */
    public UUID getUserId() {
        return userId;
    }
    
    /**
     * Sets the UUID of the user.
     * 
     * @param userId the user ID to set
     */
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    /**
     * Gets the UUID of the notification.
     * 
     * @return the notification ID
     */
    public UUID getNotificationId() {
        return notificationId;
    }
    
    /**
     * Sets the UUID of the notification.
     * 
     * @param notificationId the notification ID to set
     */
    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }
    
    /**
     * Gets whether the notification has been viewed by the user.
     * 
     * @return true if viewed, false otherwise
     */
    public Boolean getIsViewed() {
        return isViewed;
    }
    
    /**
     * Sets whether the notification has been viewed by the user.
     * 
     * @param isViewed true if viewed, false otherwise
     */
    public void setIsViewed(Boolean isViewed) {
        this.isViewed = isViewed;
    }
    
    /**
     * Gets whether the notification has been pinned by the user.
     * 
     * @return true if pinned, false otherwise
     */
    public Boolean getIsPinned() {
        return isPinned;
    }
    
    /**
     * Sets whether the notification has been pinned by the user.
     * 
     * @param isPinned true if pinned, false otherwise
     */
    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }
}
