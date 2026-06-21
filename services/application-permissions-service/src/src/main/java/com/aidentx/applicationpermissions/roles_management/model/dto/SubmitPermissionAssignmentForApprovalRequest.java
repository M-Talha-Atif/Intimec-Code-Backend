package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO for submitting a permission assignment request through an approval workflow.
 * 
 * <p>This DTO is used when a permission assignment requires approval from role owners.
 * Instead of immediately assigning the permission, it publishes an event that triggers
 * a workflow with approval nodes, and upon approval, the actual assignment is performed.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {
 *   "actionId": "afe66985-b039-4629-b353-68bb7b6a37ec",
 *   "justification": "Role needs to view audit logs for compliance monitoring",
 *   "conditionScript": "user.department = 'Finance' OR user.globalOrg = 'HQ'",
 *   "workflowId": "wf-role-permission-approval"
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 * @see AssignPermissionToRoleRequest
 */
public class SubmitPermissionAssignmentForApprovalRequest {
    
    @NotNull(message = AppConstants.VALIDATION_ACTION_ID_REQUIRED)
    private UUID actionId;

    @NotNull(message = AppConstants.VALIDATION_JUSTIFICATION_REQUIRED)
    private String justification;
    
    private String conditionScript; // Optional - null means no conditions
    
    @NotNull(message = "Workflow ID is required when submitting for approval")
    private String workflowId;
    
    // Constructors
    public SubmitPermissionAssignmentForApprovalRequest() {}
    
    public SubmitPermissionAssignmentForApprovalRequest(UUID actionId, String justification, String conditionScript, String workflowId) {
        this.actionId = actionId;
        this.justification = justification;
        this.conditionScript = conditionScript;
        this.workflowId = workflowId;
    }
    
    public SubmitPermissionAssignmentForApprovalRequest(UUID actionId, String justification, String workflowId) {
        this(actionId, justification, null, workflowId);
    }
    
    // Getters and Setters
    public UUID getActionId() {
        return actionId;
    }
    
    public void setActionId(UUID actionId) {
        this.actionId = actionId;
    }
    
    public String getJustification() {
        return justification;
    }
    
    public void setJustification(String justification) {
        this.justification = justification;
    }
    
    public String getConditionScript() {
        return conditionScript;
    }
    
    public void setConditionScript(String conditionScript) {
        this.conditionScript = conditionScript;
    }
    
    public String getWorkflowId() {
        return workflowId;
    }
    
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
    
    /**
     * Check if this request includes conditional rules.
     * 
     * @return true if conditionScript is provided and not empty
     */
    public boolean hasConditionalRule() {
        return conditionScript != null && !conditionScript.trim().isEmpty();
    }
}
