package com.aidentx.applicationpermissions.roles_management.model.dto;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for updating a permission's justification and conditional rules.
 * 
 * <p>This DTO is specifically designed for permission updates where the roleId and actionId
 * are provided as URL path parameters, so only the justification and optional conditional
 * script need to be provided in the request body.</p>
 * 
 * <p>Example usage:</p>
 * <pre>
 * {
 *   "justification": "Updated role needs enhanced access for new responsibilities",
 *   "conditionScript": "user.department == 'Finance' && user.level >= 'Manager'"
 * }
 * </pre>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class UpdatePermissionRequest {
    
    @NotNull(message = AppConstants.VALIDATION_JUSTIFICATION_REQUIRED)
    private String justification;
    
    private String conditionScript; // Optional - null means no conditions
    
    // Constructors
    public UpdatePermissionRequest() {}
    
    public UpdatePermissionRequest(String justification, String conditionScript) {
        this.justification = justification;
        this.conditionScript = conditionScript;
    }
    
    public UpdatePermissionRequest(String justification) {
        this(justification, null);
    }
    
    // Getters and Setters
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