package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * DTO for submitting a user role assignment request through an approval workflow.
 * 
 * <p>This DTO is used when a user role assignment requires approval from role owners.
 * Instead of immediately assigning the role, it publishes an event that triggers
 * a workflow with approval nodes, and upon approval, the actual assignment is performed.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {
 *   "userIds": ["123e4567-e89b-12d3-a456-426614174000", "234e5678-e89b-12d3-a456-426614174001"],
 *   "roleId": "456e7890-e89b-12d3-a456-426614174002",
 *   "workflowId": "wf-user-role-approval",
 *   "justification": "Users need this role for Q1 audit project",
 *   "sendNotificationOnCompletion": true
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 * @see AssignRoleRequest
 */
@Schema(description = "Request to submit user role assignment through approval workflow")
public class SubmitUserRoleAssignmentForApprovalRequest {
    
    @Schema(description = "The IDs of the users to assign the role to", example = "[\"123e4567-e89b-12d3-a456-426614174000\", \"234e5678-e89b-12d3-a456-426614174001\"]", required = true)
    @NotNull(message = "User IDs are required")
    @Size(min = 1, max = 100, message = "At least 1 and maximum 100 users can be assigned at once")
    private List<UUID> userIds;

    @Schema(description = "The ID of the role to assign to the users", example = "456e7890-e89b-12d3-a456-426614174002", required = true)
    @NotNull(message = AppConstants.VALIDATION_ROLE_ID_REQUIRED)
    private UUID roleId;

    @Schema(description = "The ID of the workflow to execute for approval", example = "wf-user-role-approval", required = true)
    @NotNull(message = "Workflow ID is required when submitting for approval")
    private String workflowId;
    
    @Schema(description = "Justification for the role assignment", example = "Users need this role for Q1 audit project")
    private String justification;
    
    @Schema(description = "Whether to send notification to users upon completion of workflow", example = "true", defaultValue = "true")
    private Boolean sendNotificationOnCompletion;
    
    // Constructors
    public SubmitUserRoleAssignmentForApprovalRequest() {}
    
    public SubmitUserRoleAssignmentForApprovalRequest(List<UUID> userIds, UUID roleId, String workflowId, String justification, Boolean sendNotificationOnCompletion) {
        this.userIds = userIds;
        this.roleId = roleId;
        this.workflowId = workflowId;
        this.justification = justification;
        this.sendNotificationOnCompletion = sendNotificationOnCompletion;
    }
    
    // Getters and Setters
    public List<UUID> getUserIds() {
        return userIds;
    }
    
    public void setUserIds(List<UUID> userIds) {
        this.userIds = userIds;
    }
    
    public UUID getRoleId() {
        return roleId;
    }
    
    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }
    
    public String getWorkflowId() {
        return workflowId;
    }
    
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
    
    public String getJustification() {
        return justification;
    }
    
    public void setJustification(String justification) {
        this.justification = justification;
    }
    
    public Boolean getSendNotificationOnCompletion() {
        return sendNotificationOnCompletion;
    }
    
    public void setSendNotificationOnCompletion(Boolean sendNotificationOnCompletion) {
        this.sendNotificationOnCompletion = sendNotificationOnCompletion;
    }
    
    /**
     * Checks if notifications should be sent upon completion.
     * Defaults to true if sendNotificationOnCompletion is null.
     * 
     * @return true if notifications should be sent, false otherwise
     */
    public boolean shouldSendNotification() {
        return sendNotificationOnCompletion == null || sendNotificationOnCompletion;
    }
}
