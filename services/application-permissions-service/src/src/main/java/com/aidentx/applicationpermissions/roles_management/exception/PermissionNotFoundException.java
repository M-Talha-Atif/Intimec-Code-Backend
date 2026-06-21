package com.aidentx.applicationpermissions.roles_management.exception;

/**
 * Exception thrown when attempting to delete a permission that doesn't exist for a specific role and action.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class PermissionNotFoundException extends RuntimeException {
    
    private final String roleId;
    private final String actionId;

    /**
     * Constructs a new PermissionNotFoundException with the specified detail message, role ID, and action ID.
     *
     * @param message the detail message
     * @param roleId the role ID
     * @param actionId the action ID
     */
    public PermissionNotFoundException(String message, String roleId, String actionId) {
        super(message);
        this.roleId = roleId;
        this.actionId = actionId;
    }

    /**
     * Gets the role ID associated with this exception.
     *
     * @return the role ID
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * Gets the action ID associated with this exception.
     *
     * @return the action ID
     */
    public String getActionId() {
        return actionId;
    }
}