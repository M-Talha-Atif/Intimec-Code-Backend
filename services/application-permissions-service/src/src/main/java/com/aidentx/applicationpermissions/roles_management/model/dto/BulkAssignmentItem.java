package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO representing a single permission assignment item in a bulk operation.
 * 
 * <p>Each item contains all necessary information to assign one permission:</p>
 * <ul>
 *   <li>Action ID to create permission for</li>
 *   <li>Required justification for audit trail</li>
 *   <li>Optional conditional script for ABAC rules</li>
 * </ul>
 * 
 * <p>Example:</p>
 * <pre>
 * {
 *   "actionId": "afe66985-b039-4629-b353-68bb7b6a37ec",
 *   "justification": "Required for Q1 audit compliance",
 *   "conditionScript": "user.department == 'Finance'"
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkAssignmentItem {
    
    @NotNull(message = AppConstants.VALIDATION_ACTION_ID_REQUIRED)
    private UUID actionId;
    
    @NotNull(message = AppConstants.VALIDATION_JUSTIFICATION_REQUIRED)
    private String justification;
    
    private String conditionScript; // Optional - null means unconditional access
    
    // Constructors
    public BulkAssignmentItem() {}
    
    public BulkAssignmentItem(UUID actionId, String justification, String conditionScript) {
        this.actionId = actionId;
        this.justification = justification;
        this.conditionScript = conditionScript;
    }
    
    public BulkAssignmentItem(UUID actionId, String justification) {
        this(actionId, justification, null);
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
    
    /**
     * Check if this item includes a conditional rule.
     * 
     * @return true if conditionScript is provided and not empty
     */
    public boolean hasConditionalRule() {
        return conditionScript != null && !conditionScript.trim().isEmpty();
    }
    
    /**
     * Convert to AssignPermissionToRoleRequest for reusing existing logic.
     * 
     * @return equivalent AssignPermissionToRoleRequest
     */
    public AssignPermissionToRoleRequest toAssignPermissionRequest() {
        return new AssignPermissionToRoleRequest(actionId, justification, conditionScript);
    }
    
    @Override
    public String toString() {
        return "BulkAssignmentItem{" +
                "actionId=" + actionId +
                ", justification='" + justification + '\'' +
                ", hasCondition=" + hasConditionalRule() +
                '}';
    }
}
