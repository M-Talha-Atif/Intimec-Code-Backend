package com.aidentx.applicationpermissions.roles_management.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO for submitting bulk permission assignments through an approval workflow.
 * 
 * <p>This DTO is used when multiple permission assignments require approval from role owners.
 * Instead of immediately assigning the permissions, it publishes an event that triggers
 * a workflow with approval nodes, and upon approval, the actual assignments are performed.</p>
 * 
 * <p>Use cases:</p>
 * <ul>
 *   <li>Bulk role configuration requiring approval (assign 10+ permissions at once)</li>
 *   <li>Workflow-driven batch operations with owner approval</li>
 *   <li>Role template applications requiring authorization</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {
 *   "assignments": [
 *     {
 *       "actionId": "uuid1",
 *       "justification": "Required for audit",
 *       "conditionScript": "user.department == 'Finance'"
 *     },
 *     {
 *       "actionId": "uuid2",
 *       "justification": "Required for audit",
 *       "conditionScript": null
 *     }
 *   ],
 *   "workflowId": "wf-bulk-permission-approval"
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 * @see BulkAssignPermissionRequest
 */
public class SubmitBulkPermissionAssignmentForApprovalRequest {
    
    @NotEmpty(message = "Assignments list cannot be empty")
    @Size(min = 1, max = 100, message = "Assignments must contain between 1 and 100 items")
    @Valid
    private List<BulkAssignmentItem> assignments;
    
    @NotNull(message = "Workflow ID is required when submitting for approval")
    private String workflowId;
    
    // Constructors
    public SubmitBulkPermissionAssignmentForApprovalRequest() {}
    
    public SubmitBulkPermissionAssignmentForApprovalRequest(List<BulkAssignmentItem> assignments, String workflowId) {
        this.assignments = assignments;
        this.workflowId = workflowId;
    }
    
    // Getters and Setters
    public List<BulkAssignmentItem> getAssignments() {
        return assignments;
    }
    
    public void setAssignments(List<BulkAssignmentItem> assignments) {
        this.assignments = assignments;
    }
    
    public String getWorkflowId() {
        return workflowId;
    }
    
    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }
    
    /**
     * Get the number of assignments in this bulk request.
     * 
     * @return count of assignments
     */
    public int getAssignmentCount() {
        return assignments != null ? assignments.size() : 0;
    }
    
    @Override
    public String toString() {
        return "SubmitBulkPermissionAssignmentForApprovalRequest{" +
                "assignmentCount=" + getAssignmentCount() +
                ", workflowId='" + workflowId + '\'' +
                '}';
    }
}
