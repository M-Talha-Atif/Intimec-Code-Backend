package com.aidentx.core.notifications_management.model.dto;

import com.aidentx.core.notifications_management.enums.NotificationType;
import com.aidentx.core.notifications_management.enums.OperationType;

import java.util.UUID;

/**
 * DTO for user notification responses.
 * <p>
 * This DTO combines notification content with user-specific viewing status,
 * providing all necessary information for user notification display.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
public class UserNotificationResponseDto {
    
    private UUID id;
    private UUID notificationId;
    private String description;
    private String details;
    private NotificationType type;
    private OperationType operation;
    private Boolean isViewed;
    private Boolean isPinned;
    
    /**
     * Default constructor.
     */
    public UserNotificationResponseDto() {}
    
    /**
     * Constructor with all fields.
     * 
     * @param id the user notification relationship ID
     * @param notificationId the notification ID
     * @param description the notification description
     * @param details the notification details
     * @param type the notification type
     * @param operation the operation that triggered the notification
     * @param isViewed whether the notification has been viewed by the user
     * @param isPinned whether the notification is pinned by the user
     */
    public UserNotificationResponseDto(UUID id, UUID notificationId, String description, 
                                     String details, NotificationType type, OperationType operation, 
                                     Boolean isViewed, Boolean isPinned) {
        this.id = id;
        this.notificationId = notificationId;
        this.description = description;
        this.details = details;
        this.type = type;
        this.operation = operation;
        this.isViewed = isViewed;
        this.isPinned = isPinned;
    }
    
    // Getters and Setters
    
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getNotificationId() {
        return notificationId;
    }
    
    public void setNotificationId(UUID notificationId) {
        this.notificationId = notificationId;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    public OperationType getOperation() {
        return operation;
    }
    
    public void setOperation(OperationType operation) {
        this.operation = operation;
    }
    
    public Boolean getIsViewed() {
        return isViewed;
    }
    
    public void setIsViewed(Boolean isViewed) {
        this.isViewed = isViewed;
    }
    
    public Boolean getIsPinned() {
        return isPinned;
    }
    
    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }
}
