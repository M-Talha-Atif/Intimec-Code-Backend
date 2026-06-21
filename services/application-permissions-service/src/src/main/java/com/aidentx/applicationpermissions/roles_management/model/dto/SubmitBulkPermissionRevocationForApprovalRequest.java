package com.aidentx.applicationpermissions.roles_management.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * DTO for submitting bulk permission revocations through an approval workflow.
 * 
 * <p>This DTO is used when multiple permission revocations require approval from role owners.
 * Instead of immediately revoking the permissions, it publishes an event that triggers
 * a workflow with approval nodes, and upon approval, the actual revocations are performed.</p>
 * 
 * <p>Use cases:</p>
 * <ul>
 *   <li>Bulk role cleanup requiring approval (revoke 10+ permissions at once)</li>
 *   <li>Compliance-driven permission removal with authorization</li>
 *   <li>Role template adjustments requiring owner approval</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {
 *   "actionIds": [
 *     "uuid1",
 *     "uuid2",
 *     "uuid3"
 *   ],
 *   "justification": "Compliance requirement to remove these permissions",
 *   "workflowId": "wf-bulk-permission-approval"
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 * @see BulkRevokePermissionRequest
 */
public class SubmitBulkPermissionRevocationForApprovalRequest {
    
    @NotEmpty(message = "Action IDs list cannot be empty")
    @Size(min = 1, max = 100, message = "Action IDs must contain between 1 and 100 items")
    private List<UUID> actionIds;
    
    @NotNull(message = "Justification is required when submitting for approval")
    private String justification;
    
    @NotNull(message = "Workflow ID is required when submitting for approval")
    private String workflowId;
    
    // Constructors
    public SubmitBulkPermissionRevocationForApprovalRequest() {}
    
    public SubmitBulkPermissionRevocationForApprovalRequest(List<UUID> actionIds, String justification, String workflowId) {
        this.actionIds = actionIds;
        this.justification = justification;
        this.workflowId = workflowId;
    }
    
    // Getters and Setters
    public List<UUID> getActionIds() {
        return actionIds;
    }
    
    public void setActionIds(List<UUID> actionIds) {
        this.actionIds = actionIds;
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
    
    /**
     * Get the number of actions to revoke.
     * 
     * @return count of action IDs
     */
    public int getRevocationCount() {
        return actionIds != null ? actionIds.size() : 0;
    }
    
    @Override
    public String toString() {
        return "SubmitBulkPermissionRevocationForApprovalRequest{" +
                "revocationCount=" + getRevocationCount() +
                ", justification='" + justification + '\'' +
                ", workflowId='" + workflowId + '\'' +
                '}';
    }
}
