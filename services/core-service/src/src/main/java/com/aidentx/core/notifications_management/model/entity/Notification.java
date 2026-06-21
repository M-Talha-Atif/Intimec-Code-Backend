package com.aidentx.core.notifications_management.model.entity;

import com.aidentx.core.notifications_management.enums.NotificationType;
import com.aidentx.core.notifications_management.enums.OperationType;
import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entity representing a notification in the system.
 * <p>
 * This entity stores information about notifications that can be sent to users,
 * including their content, type, and associated operation that triggered them.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Entity
@Table(name = "notifications")
public class Notification {
    
    /**
     * Unique identifier for the notification.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", columnDefinition = "UUID")
    private UUID id;
    
    /**
     * Detailed content or additional information about the notification.
     */
    @Column(name = "details", columnDefinition = "TEXT")
    private String details;
    
    /**
     * Brief description or title of the notification.
     */
    @Column(name = "description", nullable = false)
    private String description;
    
    /**
     * Type of the notification (e.g., NOTIFICATIONS, ANNOUNCEMENTS, ALERTS).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType type;
    
    /**
     * Operation type that triggered this notification.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private OperationType operation;
    
    /**
     * Timestamp when the notification was created (epoch time).
     */
    @Column(name = "created_at", nullable = false)
    private Long createdAt;
    
    /**
     * Timestamp when the notification was last updated (epoch time).
     */
    @Column(name = "updated_at")
    private Long updatedAt;
    
    /**
     * ID of the user who created this notification (from JWT sub field).
     */
    @Column(name = "created_by_id", nullable = false)
    private String createdById;
    
    /**
     * Username of the user who created this notification (from JWT username field).
     */
    @Column(name = "created_by_name", nullable = false)
    private String createdByName;
    
    /**
     * Default constructor for JPA.
     */
    public Notification() {}
    
    /**
     * Creates a new notification with the specified parameters.
     * 
     * @param details detailed content of the notification
     * @param description brief description or title of the notification
     * @param type the type of notification
     * @param operation the operation that triggered this notification
     * @param createdById the ID of the user who created this notification
     * @param createdByName the username of the user who created this notification
     */
    public Notification(String details, String description, NotificationType type, OperationType operation,
                       String createdById, String createdByName) {
        this.details = details;
        this.description = description;
        this.type = type;
        this.operation = operation;
        this.createdById = createdById;
        this.createdByName = createdByName;
        this.createdAt = System.currentTimeMillis();
    }
    
    /**
     * Gets the unique identifier of the notification.
     * 
     * @return the notification ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Sets the unique identifier of the notification.
     * 
     * @param id the notification ID to set
     */
    public void setId(UUID id) {
        this.id = id;
    }
    
    /**
     * Gets the detailed content of the notification.
     * 
     * @return the notification details
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * Sets the detailed content of the notification.
     * 
     * @param details the details to set
     */
    public void setDetails(String details) {
        this.details = details;
    }
    
    /**
     * Gets the brief description or title of the notification.
     * 
     * @return the notification description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the brief description or title of the notification.
     * 
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the type of the notification.
     * 
     * @return the notification type
     */
    public NotificationType getType() {
        return type;
    }
    
    /**
     * Sets the type of the notification.
     * 
     * @param type the notification type to set
     */
    public void setType(NotificationType type) {
        this.type = type;
    }
    
    /**
     * Gets the operation that triggered this notification.
     * 
     * @return the operation type
     */
    public OperationType getOperation() {
        return operation;
    }
    
    /**
     * Sets the operation that triggered this notification.
     * 
     * @param operation the operation type to set
     */
    public void setOperation(OperationType operation) {
        this.operation = operation;
    }
    
    /**
     * Gets the timestamp when the notification was created.
     * 
     * @return the creation timestamp (epoch time)
     */
    public Long getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the timestamp when the notification was created.
     * 
     * @param createdAt the creation timestamp (epoch time) to set
     */
    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Gets the timestamp when the notification was last updated.
     * 
     * @return the update timestamp (epoch time)
     */
    public Long getUpdatedAt() {
        return updatedAt;
    }
    
    /**
     * Sets the timestamp when the notification was last updated.
     * 
     * @param updatedAt the update timestamp (epoch time) to set
     */
    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * Gets the ID of the user who created this notification.
     * 
     * @return the creator user ID
     */
    public String getCreatedById() {
        return createdById;
    }
    
    /**
     * Sets the ID of the user who created this notification.
     * 
     * @param createdById the creator user ID to set
     */
    public void setCreatedById(String createdById) {
        this.createdById = createdById;
    }
    
    /**
     * Gets the username of the user who created this notification.
     * 
     * @return the creator username
     */
    public String getCreatedByName() {
        return createdByName;
    }
    
    /**
     * Sets the username of the user who created this notification.
     * 
     * @param createdByName the creator username to set
     */
    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
}
