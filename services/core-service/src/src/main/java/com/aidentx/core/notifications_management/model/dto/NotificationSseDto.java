package com.aidentx.core.notifications_management.model.dto;

import com.aidentx.core.notifications_management.enums.NotificationType;
import com.aidentx.core.notifications_management.enums.OperationType;
import java.util.List;

/**
 * Data Transfer Object for real-time notification SSE events.
 * <p>
 * This DTO represents the notification data sent via Server-Sent Events
 * to frontend clients for real-time updates.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-25
 */
public class NotificationSseDto {
    
    private String id;
    private NotificationType type;
    private String description;
    private String details;
    private OperationType operation;
    private Long createdAt;
    private String createdBy;
    private Boolean isViewed;
    private String userNotificationId;
    private List<NotificationEventData.NotificationAction> actions;
    
    /**
     * Default constructor for JSON serialization.
     */
    public NotificationSseDto() {}
    
    /**
     * Constructor with all fields.
     * 
     * @param id notification ID
     * @param type notification type
     * @param description notification description
     * @param details notification details
     * @param operation operation type
     * @param createdAt creation timestamp
     * @param createdBy creator name
     * @param isViewed whether viewed by user
     * @param userNotificationId user notification relationship ID
     */
    public NotificationSseDto(String id, NotificationType type, String description, String details,
                             OperationType operation, Long createdAt, String createdBy,
                             Boolean isViewed, String userNotificationId) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.details = details;
        this.operation = operation;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.isViewed = isViewed;
        this.userNotificationId = userNotificationId;
    }
    
    /**
     * Constructor with all fields including actions.
     * 
     * @param id notification ID
     * @param type notification type
     * @param description notification description
     * @param details notification details
     * @param operation operation type
     * @param createdAt creation timestamp
     * @param createdBy creator name
     * @param isViewed whether viewed by user
     * @param userNotificationId user notification relationship ID
     * @param actions list of action buttons for interactive notifications
     */
    public NotificationSseDto(String id, NotificationType type, String description, String details,
                             OperationType operation, Long createdAt, String createdBy,
                             Boolean isViewed, String userNotificationId, 
                             List<NotificationEventData.NotificationAction> actions) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.details = details;
        this.operation = operation;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.isViewed = isViewed;
        this.userNotificationId = userNotificationId;
        this.actions = actions;
    }
    
    // Getters and setters
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public void setType(NotificationType type) {
        this.type = type;
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
    
    public OperationType getOperation() {
        return operation;
    }
    
    public void setOperation(OperationType operation) {
        this.operation = operation;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public Boolean getIsViewed() {
        return isViewed;
    }
    
    public void setIsViewed(Boolean isViewed) {
        this.isViewed = isViewed;
    }
    
    public String getUserNotificationId() {
        return userNotificationId;
    }
    
    public void setUserNotificationId(String userNotificationId) {
        this.userNotificationId = userNotificationId;
    }
    
    public List<NotificationEventData.NotificationAction> getActions() {
        return actions;
    }
    
    public void setActions(List<NotificationEventData.NotificationAction> actions) {
        this.actions = actions;
    }
}
