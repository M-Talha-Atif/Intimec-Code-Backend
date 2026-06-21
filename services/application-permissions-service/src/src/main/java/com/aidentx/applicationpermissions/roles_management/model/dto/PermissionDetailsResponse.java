package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.util.UUID;

/**
 * Response DTO for permission details including conditional rules.
 * Used when retrieving details of a specific permission assigned to a role.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class PermissionDetailsResponse {
    
    private UUID permissionId;
    private UUID actionId;
    private String actionName;
    private UUID roleId;
    private String roleName;
    private String justification;
    private String conditionScript;
    private boolean hasConditionalRule;
    private UUID conditionalRuleId;
    
    // Constructors
    public PermissionDetailsResponse() {}
    
    public PermissionDetailsResponse(UUID permissionId, UUID actionId, String actionName, 
                                    UUID roleId, String roleName, String justification, 
                                    String conditionScript, boolean hasConditionalRule, 
                                    UUID conditionalRuleId) {
        this.permissionId = permissionId;
        this.actionId = actionId;
        this.actionName = actionName;
        this.roleId = roleId;
        this.roleName = roleName;
        this.justification = justification;
        this.conditionScript = conditionScript;
        this.hasConditionalRule = hasConditionalRule;
        this.conditionalRuleId = conditionalRuleId;
    }
    
    // Getters and Setters
    public UUID getPermissionId() {
        return permissionId;
    }
    
    public void setPermissionId(UUID permissionId) {
        this.permissionId = permissionId;
    }
    
    public UUID getActionId() {
        return actionId;
    }
    
    public void setActionId(UUID actionId) {
        this.actionId = actionId;
    }
    
    public String getActionName() {
        return actionName;
    }
    
    public void setActionName(String actionName) {
        this.actionName = actionName;
    }
    
    public UUID getRoleId() {
        return roleId;
    }
    
    public void setRoleId(UUID roleId) {
        this.roleId = roleId;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
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
    
    public boolean isHasConditionalRule() {
        return hasConditionalRule;
    }
    
    public void setHasConditionalRule(boolean hasConditionalRule) {
        this.hasConditionalRule = hasConditionalRule;
    }
    
    public UUID getConditionalRuleId() {
        return conditionalRuleId;
    }
    
    public void setConditionalRuleId(UUID conditionalRuleId) {
        this.conditionalRuleId = conditionalRuleId;
    }
}
