package com.aidentx.applicationpermissions.roles_management.model.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * DTO for bulk permission assignment operations.
 * 
 * <p>This request allows assigning multiple permissions to a role in a single
 * atomic transaction. All assignments must succeed, or the entire operation
 * will be rolled back to maintain data consistency.</p>
 * 
 * <p>Use cases:</p>
 * <ul>
 *   <li>Rapid role configuration (assign 10+ permissions at once)</li>
 *   <li>Workflow-driven approvals (batch operations after approval)</li>
 *   <li>Role templates (apply standard permission sets)</li>
 * </ul>
 * 
 * <p>Example request:</p>
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
 *   ]
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class BulkAssignPermissionRequest {
    
    @NotEmpty(message = "Assignments list cannot be empty")
    @Size(min = 1, max = 100, message = "Assignments must contain between 1 and 100 items")
    @Valid
    private List<BulkAssignmentItem> assignments;
    
    // Constructors
    public BulkAssignPermissionRequest() {}
    
    public BulkAssignPermissionRequest(List<BulkAssignmentItem> assignments) {
        this.assignments = assignments;
    }
    
    // Getters and Setters
    public List<BulkAssignmentItem> getAssignments() {
        return assignments;
    }
    
    public void setAssignments(List<BulkAssignmentItem> assignments) {
        this.assignments = assignments;
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
        return "BulkAssignPermissionRequest{" +
                "assignmentCount=" + getAssignmentCount() +
                '}';
    }
}
