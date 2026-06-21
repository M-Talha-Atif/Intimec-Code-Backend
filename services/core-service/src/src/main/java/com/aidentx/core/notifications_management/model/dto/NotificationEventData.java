package com.aidentx.core.notifications_management.model.dto;

import com.aidentx.core.notifications_management.enums.OperationType;

import java.util.List;

/**
 * DTO representing a notification event for Kafka messaging.
 * <p>
 * This DTO contains all the information needed to process notifications
 * across different services using Kafka messaging. Each event includes
 * tenant context to ensure proper multi-tenant isolation.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-21
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
     * Creates a new notification event.
     * 
     * @param operationType the operation that triggered this notification
     * @param data the list of user notification data
     * @deprecated Use constructor with tenantId for proper multi-tenant support
     */
    @Deprecated
    public NotificationEventData(OperationType operationType, List<UserNotificationData> data) {
        this.operationType = operationType;
        this.data = data;
    }
    
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
     * Record representing user-specific notification data.
     */
    public static class UserNotificationData {
        private String userId;
        private UserData userData;
        private TemplateVariables templateVariables;
        private List<NotificationAction> actions;
        
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
        
        public List<NotificationAction> getActions() {
            return actions;
        }
        
        public void setActions(List<NotificationAction> actions) {
            this.actions = actions;
        }
    }
    
    /**
     * Record representing user data.
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
     * Record representing template variables for Velocity processing.
     */
    public static class TemplateVariables {
        private String username;
        private String email;
        private String expirationDays;
        private String roleName;
        // Workflow-specific fields
        private String workflowName;
        private String workflowInstanceId;
        private String approvalUrl;
        private String approvalId;
        private String requestedBy;
        // Phase 3: Delegation-specific fields
        private String delegatorName;
        private String startDate;
        private String endDate;
        private String reason;
        // Phase 4: Escalation-specific fields
        private Integer escalationLevel;
        private String originalApproverName;
        // Enterprise IAM context fields for meaningful notifications
        private String requesterFullName;
        private String requesterDepartment;
        private String tileName;
        private String roleDescription;
        private String riskLevel;
        private String targetUsers;
        private int targetUserCount;
        private String justification;
        private String requestedAt;
        private String deadlineAt;
        private String applicationName;
        private String requestType;
        
        /**
         * Default constructor for JSON serialization.
         */
        public TemplateVariables() {}
        
        /**
         * Creates new template variables.
         * 
         * @param username the username for template
         * @param email the email for template
         */
        public TemplateVariables(String username, String email) {
            this.username = username;
            this.email = email;
        }
        
        /**
         * Creates new template variables with expiration days.
         * 
         * @param username the username for template
         * @param email the email for template
         * @param expirationDays the expiration days for template
         */
        public TemplateVariables(String username, String email, String expirationDays) {
            this.username = username;
            this.email = email;
            this.expirationDays = expirationDays;
        }
        
        /**
         * Creates new template variables with role name.
         * 
         * @param username the username for template
         * @param email the email for template
         * @param roleName the role name for template
         */
        public TemplateVariables(String username, String email, String roleName, boolean isRoleTemplate) {
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
        
        public String getExpirationDays() {
            return expirationDays;
        }
        
        public void setExpirationDays(String expirationDays) {
            this.expirationDays = expirationDays;
        }
        
        public String getRoleName() {
            return roleName;
        }
        
        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }
        
        public String getWorkflowName() {
            return workflowName;
        }
        
        public void setWorkflowName(String workflowName) {
            this.workflowName = workflowName;
        }
        
        public String getWorkflowInstanceId() {
            return workflowInstanceId;
        }
        
        public void setWorkflowInstanceId(String workflowInstanceId) {
            this.workflowInstanceId = workflowInstanceId;
        }
        
        public String getApprovalUrl() {
            return approvalUrl;
        }
        
        public void setApprovalUrl(String approvalUrl) {
            this.approvalUrl = approvalUrl;
        }
        
        public String getApprovalId() {
            return approvalId;
        }
        
        public void setApprovalId(String approvalId) {
            this.approvalId = approvalId;
        }
        
        public String getRequestedBy() {
            return requestedBy;
        }
        
        public void setRequestedBy(String requestedBy) {
            this.requestedBy = requestedBy;
        }

        // Phase 3: Delegation getters/setters
        public String getDelegatorName() {
            return delegatorName;
        }

        public void setDelegatorName(String delegatorName) {
            this.delegatorName = delegatorName;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        // Phase 4: Escalation getters/setters
        public Integer getEscalationLevel() {
            return escalationLevel;
        }

        public void setEscalationLevel(Integer escalationLevel) {
            this.escalationLevel = escalationLevel;
        }

        public String getOriginalApproverName() {
            return originalApproverName;
        }

        public void setOriginalApproverName(String originalApproverName) {
            this.originalApproverName = originalApproverName;
        }

        // Enterprise IAM context getters/setters
        public String getRequesterFullName() {
            return requesterFullName;
        }

        public void setRequesterFullName(String requesterFullName) {
            this.requesterFullName = requesterFullName;
        }

        public String getRequesterDepartment() {
            return requesterDepartment;
        }

        public void setRequesterDepartment(String requesterDepartment) {
            this.requesterDepartment = requesterDepartment;
        }

        public String getTileName() {
            return tileName;
        }

        public void setTileName(String tileName) {
            this.tileName = tileName;
        }

        public String getRoleDescription() {
            return roleDescription;
        }

        public void setRoleDescription(String roleDescription) {
            this.roleDescription = roleDescription;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public String getTargetUsers() {
            return targetUsers;
        }

        public void setTargetUsers(String targetUsers) {
            this.targetUsers = targetUsers;
        }

        public int getTargetUserCount() {
            return targetUserCount;
        }

        public void setTargetUserCount(int targetUserCount) {
            this.targetUserCount = targetUserCount;
        }

        public String getJustification() {
            return justification;
        }

        public void setJustification(String justification) {
            this.justification = justification;
        }

        public String getRequestedAt() {
            return requestedAt;
        }

        public void setRequestedAt(String requestedAt) {
            this.requestedAt = requestedAt;
        }

        public String getDeadlineAt() {
            return deadlineAt;
        }

        public void setDeadlineAt(String deadlineAt) {
            this.deadlineAt = deadlineAt;
        }

        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }

        public String getRequestType() {
            return requestType;
        }

        public void setRequestType(String requestType) {
            this.requestType = requestType;
        }
    }
    
    /**
     * Record representing an action button for interactive notifications.
     * Used for workflow approvals and other interactive notification types.
     */
    public static class NotificationAction {
        private String type;
        private String label;
        private String variant;
        private String endpoint;
        private String method;
        private Boolean requiresComment;
        
        /**
         * Default constructor for JSON serialization.
         */
        public NotificationAction() {}
        
        /**
         * Creates new notification action.
         * 
         * @param type the action type (e.g., "approve", "reject", "delegate")
         * @param label the display label for the button
         * @param variant the button style variant (e.g., "success", "danger")
         * @param endpoint the API endpoint to call
         * @param method the HTTP method (e.g., "POST", "PUT")
         * @param requiresComment whether this action requires a comment
         */
        public NotificationAction(String type, String label, String variant, 
                                 String endpoint, String method, Boolean requiresComment) {
            this.type = type;
            this.label = label;
            this.variant = variant;
            this.endpoint = endpoint;
            this.method = method;
            this.requiresComment = requiresComment;
        }
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public String getLabel() {
            return label;
        }
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public String getVariant() {
            return variant;
        }
        
        public void setVariant(String variant) {
            this.variant = variant;
        }
        
        public String getEndpoint() {
            return endpoint;
        }
        
        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }
        
        public String getMethod() {
            return method;
        }
        
        public void setMethod(String method) {
            this.method = method;
        }
        
        public Boolean getRequiresComment() {
            return requiresComment;
        }
        
        public void setRequiresComment(Boolean requiresComment) {
            this.requiresComment = requiresComment;
        }
    }
}
