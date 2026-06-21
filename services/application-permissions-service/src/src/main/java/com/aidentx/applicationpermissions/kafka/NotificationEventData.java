package com.aidentx.applicationpermissions.kafka;

import java.util.List;

/**
 * DTO representing a notification event for Kafka messaging in application-permissions-service.
 * <p>
 * This DTO matches the structure expected by the core-service for notification processing.
 * It will be serialized to JSON and sent via Kafka to the core-service.
 * </p>
 * 
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-10-20
 */
public class NotificationEventData {
    
    private OperationType operationType;
    private String tenantId;
    private List<UserNotificationData> data;
    
    /**
     * Default constructor for JSON serialization.
     */
    public NotificationEventData() {}
    
    /**
     * Creates a new notification event with tenant context.
     * 
     * @param operationType the operation that triggered this notification
     * @param tenantId the tenant ID for multi-tenant isolation
     * @param data the list of user notification data
     */
    public NotificationEventData(OperationType operationType, String tenantId, List<UserNotificationData> data) {
        this.operationType = operationType;
        this.tenantId = tenantId;
        this.data = data;
    }
    
    /**
     * Gets the operation type that triggered this notification.
     * 
     * @return the operation type
     */
    public OperationType getOperationType() {
        return operationType;
    }
    
    /**
     * Sets the operation type that triggered this notification.
     * 
     * @param operationType the operation type to set
     */
    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }
    
    /**
     * Gets the tenant ID for multi-tenant isolation.
     * 
     * @return the tenant ID
     */
    public String getTenantId() {
        return tenantId;
    }
    
    /**
     * Sets the tenant ID for multi-tenant isolation.
     * 
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    

    
    /**
     * Gets the list of user notification data.
     * 
     * @return the user notification data list
     */
    public List<UserNotificationData> getData() {
        return data;
    }
    
    /**
     * Sets the list of user notification data.
     * 
     * @param data the user notification data list to set
     */
    public void setData(List<UserNotificationData> data) {
        this.data = data;
    }
    
    /**
     * Enumeration of operation types that can trigger notifications.
     */
    public enum OperationType {
        ASSIGN_INTERNAL_ROLE,
        REVOKE_INTERNAL_ROLE,
        PASSWORD_RESET,
        ACCESS_REQUEST_APPROVED,
        PASSWORD_REMINDER_RESET
    }
    
    /**
     * Class representing user-specific notification data.
     */
    public static class UserNotificationData {
        private String userId;
        private UserData userData;
        private TemplateVariables templateVariables;
        
        /**
         * Default constructor for JSON serialization.
         */
        public UserNotificationData() {}
        
        /**
         * Creates new user notification data.
         * 
         * @param userId the user ID
         * @param userData the user data
         * @param templateVariables the template variables
         */
        public UserNotificationData(String userId, UserData userData, TemplateVariables templateVariables) {
            this.userId = userId;
            this.userData = userData;
            this.templateVariables = templateVariables;
        }
        
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
     * Class representing user data.
     */
    public static class UserData {
        private String email;
        private String username;
        
        /**
         * Default constructor for JSON serialization.
         */
        public UserData() {}
        
        /**
         * Creates new user data.
         * 
         * @param email the user email
         * @param username the username
         */
        public UserData(String email, String username) {
            this.email = email;
            this.username = username;
        }
        
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
     * Class representing template variables for Velocity processing.
     */
    public static class TemplateVariables {
        private String username;
        private String email;
        private String roleName;
        
        /**
         * Default constructor for JSON serialization.
         */
        public TemplateVariables() {}
        
        /**
         * Creates new template variables for role assignment.
         * 
         * @param username the username for template
         * @param email the email for template
         * @param roleName the role name for template
         */
        public TemplateVariables(String username, String email, String roleName) {
            this.username = username;
            this.email = email;
            this.roleName = roleName;
        }
        
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
        
        public String getRoleName() {
            return roleName;
        }
        
        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }
    }
}