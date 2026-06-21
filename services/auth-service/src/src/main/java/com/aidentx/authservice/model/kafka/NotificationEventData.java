package com.aidentx.authservice.model.kafka;

import java.util.List;

/**
 * Kafka message structure for notification events.
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-21
 */
public class NotificationEventData {
    
    public enum OperationType {
        PASSWORD_RESET,
        PASSWORD_REMINDER_RESET,
        USER_CREATED,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED
    }
    
    private OperationType operationType;
    private String tenantId;
    private List<UserNotificationData> data;
    
    public NotificationEventData() {}
    
    public NotificationEventData(OperationType operationType, String tenantId, List<UserNotificationData> data) {
        this.operationType = operationType;
        this.tenantId = tenantId;
        this.data = data;
    }
    
    // Getters and Setters
    public OperationType getOperationType() {
        return operationType;
    }
    
    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public List<UserNotificationData> getData() {
        return data;
    }
    
    public void setData(List<UserNotificationData> data) {
        this.data = data;
    }
    
    /**
     * User notification data containing user information and template variables.
     */
    public static class UserNotificationData {
        private String userId;
        private UserData userData;
        private TemplateVariables templateVariables;
        
        public UserNotificationData() {}
        
        public UserNotificationData(String userId, UserData userData, TemplateVariables templateVariables) {
            this.userId = userId;
            this.userData = userData;
            this.templateVariables = templateVariables;
        }
        
        // Getters and Setters
        public String getUserId() {
            return userId;
        }
        
        public void setUserId(String userId) {
            this.userId = userId;
        }
        
        public UserData getUserData() {
            return userData;
        }
        
        public void setUserData(UserData userData) {
            this.userData = userData;
        }
        
        public TemplateVariables getTemplateVariables() {
            return templateVariables;
        }
        
        public void setTemplateVariables(TemplateVariables templateVariables) {
            this.templateVariables = templateVariables;
        }
    }
    
    /**
     * Basic user data for notification processing.
     */
    public static class UserData {
        private String email;
        private String username;
        
        public UserData() {}
        
        public UserData(String email, String username) {
            this.email = email;
            this.username = username;
        }
        
        // Getters and Setters
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
    }
    
    /**
     * Template variables for email content generation.
     */
    public static class TemplateVariables {
        private String username;
        private String email;
        private String expirationDays;
        
        public TemplateVariables() {}
        
        public TemplateVariables(String username, String email) {
            this.username = username;
            this.email = email;
        }
        
        public TemplateVariables(String username, String email, String expirationDays) {
            this.username = username;
            this.email = email;
            this.expirationDays = expirationDays;
        }
        
        // Getters and Setters
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getEmail() {
            return email;
        }
        
        public void setEmail(String email) {
            this.email = email;
        }
        
        public String getExpirationDays() {
            return expirationDays;
        }
        
        public void setExpirationDays(String expirationDays) {
            this.expirationDays = expirationDays;
        }
    }
}
