package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO for submitting a permission revocation request through an approval workflow.
 * 
 * <p>This DTO is used when a permission revocation requires approval from role owners.
 * Instead of immediately revoking the permission, it publishes an event that triggers
 * a workflow with approval nodes, and upon approval, the actual revocation is performed.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {
 *   "actionId": "afe66985-b039-4629-b353-68bb7b6a37ec",
 *   "justification": "User no longer requires this permission",
 *   "workflowId": "wf-role-permission-approval"
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class SubmitPermissionRevocationForApprovalRequest {
    
    @NotNull(message = AppConstants.VALIDATION_ACTION_ID_REQUIRED)
    private UUID actionId;

    @NotNull(message = AppConstants.VALIDATION_JUSTIFICATION_REQUIRED)
    private String justification;
    
    @NotNull(message = "Workflow ID is required when submitting for approval")
    private String workflowId;
    
    // Constructors
    public SubmitPermissionRevocationForApprovalRequest() {}
    
    public SubmitPermissionRevocationForApprovalRequest(UUID actionId, String justification, String workflowId) {
        this.actionId = actionId;
        this.justification = justification;
        this.workflowId = workflowId;
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
    
    public String getWorkflowId() {
        return workflowId;
    }
    
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
}
