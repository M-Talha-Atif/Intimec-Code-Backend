package com.aidentx.workflowsservice.dto.notification;

/**
 * Rich context for approval notifications in enterprise IAM workflows.
 * Contains human-readable information for meaningful approval notifications.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
public class ApprovalNotificationContext {
    
    private String requesterFullName;     // Full name of who made the request
    private String requesterDepartment;   // Requester's department
    private String tileName;              // Name of the tile (access portal tile)
    private String roleName;              // Name of role being requested (not ID)
    private String roleDescription;       // Role description
    private String riskLevel;             // HIGH, MEDIUM, LOW
    private String targetUsers;           // Comma-separated user names who will get the role
    private int targetUserCount;          // Number of users affected
    private String justification;         // Business justification
    private String requestedAt;           // Human-readable request time
    private String deadlineAt;            // When approval times out
    private String applicationName;       // Application the role belongs to
    private String requestType;           // "Role Assignment", "Permission Grant", etc.

    public ApprovalNotificationContext() {}

    // Builder pattern for convenient construction
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ApprovalNotificationContext context = new ApprovalNotificationContext();

        public Builder requesterFullName(String requesterFullName) {
            context.requesterFullName = requesterFullName;
            return this;
        }

        public Builder requesterDepartment(String requesterDepartment) {
            context.requesterDepartment = requesterDepartment;
            return this;
        }

        public Builder tileName(String tileName) {
            context.tileName = tileName;
            return this;
        }

        public Builder roleName(String roleName) {
            context.roleName = roleName;
            return this;
        }

        public Builder roleDescription(String roleDescription) {
            context.roleDescription = roleDescription;
            return this;
        }

        public Builder riskLevel(String riskLevel) {
            context.riskLevel = riskLevel;
            return this;
        }

        public Builder targetUsers(String targetUsers) {
            context.targetUsers = targetUsers;
            return this;
        }

        public Builder targetUserCount(int targetUserCount) {
            context.targetUserCount = targetUserCount;
            return this;
        }

        public Builder justification(String justification) {
            context.justification = justification;
            return this;
        }

        public Builder requestedAt(String requestedAt) {
            context.requestedAt = requestedAt;
            return this;
        }

        public Builder deadlineAt(String deadlineAt) {
            context.deadlineAt = deadlineAt;
            return this;
        }

        public Builder applicationName(String applicationName) {
            context.applicationName = applicationName;
            return this;
        }

        public Builder requestType(String requestType) {
            context.requestType = requestType;
            return this;
        }

        public ApprovalNotificationContext build() {
            return context;
        }
    }

    // Getters
    public String getRequesterFullName() {
        return requesterFullName;
    }

    public String getRequesterDepartment() {
        return requesterDepartment;
    }

    public String getTileName() {
        return tileName;
    }

    public String getRoleName() {
        return roleName;
    }

    public String getRoleDescription() {
        return roleDescription;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public String getTargetUsers() {
        return targetUsers;
    }

    public int getTargetUserCount() {
        return targetUserCount;
    }

    public String getJustification() {
        return justification;
    }

    public String getRequestedAt() {
        return requestedAt;
    }

    public String getDeadlineAt() {
        return deadlineAt;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getRequestType() {
        return requestType;
    }

    // Setters
    public void setRequesterFullName(String requesterFullName) {
        this.requesterFullName = requesterFullName;
    }

    public void setRequesterDepartment(String requesterDepartment) {
        this.requesterDepartment = requesterDepartment;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public void setRoleDescription(String roleDescription) {
        this.roleDescription = roleDescription;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public void setTargetUsers(String targetUsers) {
        this.targetUsers = targetUsers;
    }

    public void setTargetUserCount(int targetUserCount) {
        this.targetUserCount = targetUserCount;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public void setRequestedAt(String requestedAt) {
        this.requestedAt = requestedAt;
    }

    public void setDeadlineAt(String deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }
}
