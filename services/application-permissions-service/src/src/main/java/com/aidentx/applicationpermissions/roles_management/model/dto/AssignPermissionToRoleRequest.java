package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO for assigning a permission to a role with optional conditional rules.
 * 
 * <p>This DTO encapsulates the information needed to:</p>
 * <ul>
 *   <li>Create a new Permission for a specific Action</li>
 *   <li>Associate the Permission with a Role via RolePermission</li>
 *   <li>Optionally create a ConditionalRule for additional access control</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {
 *   "actionId": "afe66985-b039-4629-b353-68bb7b6a37ec",
 *   "justification": "Role needs to view audit logs for compliance monitoring",
 *   "conditionScript": "user.department = 'Finance' OR user.globalOrg = 'HQ'"
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class AssignPermissionToRoleRequest {
    
    @NotNull(message = AppConstants.VALIDATION_ACTION_ID_REQUIRED)
    private UUID actionId;

    @NotNull(message = AppConstants.VALIDATION_JUSTIFICATION_REQUIRED)
    private String justification;
    
    private String conditionScript; // Optional - null means no conditions
    
    // Constructors
    public AssignPermissionToRoleRequest() {}
    
    public AssignPermissionToRoleRequest(UUID actionId, String justification, String conditionScript) {
        this.actionId = actionId;
        this.justification = justification;
        this.conditionScript = conditionScript;
    }
    
    public AssignPermissionToRoleRequest(UUID actionId, String justification) {
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
     * Check if this request includes conditional rules.
     * 
     * @return true if conditionScript is provided and not empty
     */
    public boolean hasConditionalRule() {
        return conditionScript != null && !conditionScript.trim().isEmpty();
    }
}