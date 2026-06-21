package com.aidentx.workflowsservice.dto.notification;

import java.util.List;

/**
 * DTO representing a notification event for Kafka messaging.
 * Matches core-service NotificationEventData structure.
 *
 * @author aidentx Development Team
 * @version 1.0
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
     * Record representing user-specific notification data.
     */
    public static class UserNotificationData {
        private String userId;
        private UserData userData;
        private TemplateVariables templateVariables;
        private List<NotificationAction> actions;
        
        public UserNotificationData() {}
        
        public UserNotificationData(String userId, UserData userData, TemplateVariables templateVariables) {
            this.userId = userId;
            this.userData = userData;
            this.templateVariables = templateVariables;
        }
        
        public UserNotificationData(String userId, UserData userData, TemplateVariables templateVariables, List<NotificationAction> actions) {
            this.userId = userId;
            this.userData = userData;
            this.templateVariables = templateVariables;
            this.actions = actions;
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
        
        public UserData() {}
        
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
     * Record representing template variables for notification processing.
     */
    public static class TemplateVariables {
        private String username;
        private String email;
        private String workflowName;
        private String workflowInstanceId;
        private String approvalUrl;
        private String approvalId;
        private String requestedBy;
        private String approverName;
        private String comment;
        
        // Enterprise IAM context fields for meaningful notifications
        private String requesterFullName;  // Full name of who made the request
        private String requesterDepartment; // Requester's department
        private String tileName;            // Name of the tile (access portal)
        private String roleName;            // Name of role being requested (not ID)
        private String roleDescription;     // Role description
        private String riskLevel;           // HIGH, MEDIUM, LOW
        private String targetUsers;         // Comma-separated user names who will get the role
        private int targetUserCount;        // Number of users affected
        private String justification;       // Business justification
        private String requestedAt;         // Human-readable request time
        private String deadlineAt;          // When approval times out
        private String applicationName;     // Application the role belongs to
        private String requestType;         // "Role Assignment", "Permission Grant", etc.
        
        // Phase 3: Delegation fields
        private String delegatorName;
        private String startDate;
        private String endDate;
        private String reason;
        // Phase 4: Escalation fields
        private String originalApproverName;
        private int escalationLevel;
        private int escalatedToCount;

        public TemplateVariables() {}
        
        public TemplateVariables(String username, String email) {
            this.username = username;
            this.email = email;
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
        
        public String getApproverName() {
            return approverName;
        }
        
        public void setApproverName(String approverName) {
            this.approverName = approverName;
        }
        
        public String getComment() {
            return comment;
        }
        
        public void setComment(String comment) {
            this.comment = comment;
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

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
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
        public String getOriginalApproverName() {
            return originalApproverName;
        }

        public void setOriginalApproverName(String originalApproverName) {
            this.originalApproverName = originalApproverName;
        }

        public int getEscalationLevel() {
            return escalationLevel;
        }

        public void setEscalationLevel(int escalationLevel) {
            this.escalationLevel = escalationLevel;
        }

        public int getEscalatedToCount() {
            return escalatedToCount;
        }

        public void setEscalatedToCount(int escalatedToCount) {
            this.escalatedToCount = escalatedToCount;
        }
    }
    
    /**
     * Record representing an actionable button in a notification.
     * Used for approval workflows where users can approve/reject/delegate directly from notification.
     */
    public static class NotificationAction {
        private String actionId;
        private String label;
        private String actionType; // "APPROVE", "REJECT", "DELEGATE"
        private String apiEndpoint;
        private String httpMethod;
        private String style; // "primary", "danger", "secondary"
        private boolean requiresComment;
        
        public NotificationAction() {}
        
        public NotificationAction(String actionId, String label, String actionType, 
                                 String apiEndpoint, String httpMethod, String style, 
                                 boolean requiresComment) {
            this.actionId = actionId;
            this.label = label;
            this.actionType = actionType;
            this.apiEndpoint = apiEndpoint;
            this.httpMethod = httpMethod;
            this.style = style;
            this.requiresComment = requiresComment;
        }
        
        public String getActionId() {
            return actionId;
        }
        
        public void setActionId(String actionId) {
            this.actionId = actionId;
        }
        
        public String getLabel() {
            return label;
        }
        
        public void setLabel(String label) {
            this.label = label;
        }
        
        public String getActionType() {
            return actionType;
        }
        
        public void setActionType(String actionType) {
            this.actionType = actionType;
        }
        
        public String getApiEndpoint() {
            return apiEndpoint;
        }
        
        public void setApiEndpoint(String apiEndpoint) {
            this.apiEndpoint = apiEndpoint;
        }
        
        public String getHttpMethod() {
            return httpMethod;
        }
        
        public void setHttpMethod(String httpMethod) {
            this.httpMethod = httpMethod;
        }
        
        public String getStyle() {
            return style;
        }
        
        public void setStyle(String style) {
            this.style = style;
        }
        
        public boolean isRequiresComment() {
            return requiresComment;
        }
        
        public void setRequiresComment(boolean requiresComment) {
            this.requiresComment = requiresComment;
        }
    }
}
