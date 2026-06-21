package com.aidentx.core.notifications_management.model.dto;

/**
 * DTO for mark notification as viewed responses.
 * <p>
 * This DTO provides feedback on the result of marking notifications as viewed,
 * including success status and any relevant message.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
public class MarkNotificationViewedResponseDto {
    
    private boolean success;
    private String message;
    
    /**
     * Default constructor.
     */
    public MarkNotificationViewedResponseDto() {}
    
    /**
     * Constructor with all fields.
     * 
     * @param success whether the operation was successful
     * @param message descriptive message about the operation result
     */
    public MarkNotificationViewedResponseDto(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    // Getters and Setters
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
}
