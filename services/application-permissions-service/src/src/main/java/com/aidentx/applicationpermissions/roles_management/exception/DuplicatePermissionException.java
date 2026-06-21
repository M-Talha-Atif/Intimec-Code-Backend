package com.aidentx.applicationpermissions.roles_management.exception;

/**
 * Exception thrown when attempting to assign a permission that already exists for a role-action combination.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class DuplicatePermissionException extends RuntimeException {

    private final String roleId;
    private final String actionId;

    /**
     * Creates a new DuplicatePermissionException.
     * 
     * @param message the exception message
     * @param roleId the role ID that already has the permission
     * @param actionId the action ID for which permission already exists
     */
    public DuplicatePermissionException(String message, String roleId, String actionId) {
        super(message);
        this.roleId = roleId;
        this.actionId = actionId;
    }

    /**
     * Creates a new DuplicatePermissionException with cause.
     * 
     * @param message the exception message
     * @param roleId the role ID that already has the permission
     * @param actionId the action ID for which permission already exists
     * @param cause the underlying cause
     */
    public DuplicatePermissionException(String message, String roleId, String actionId, Throwable cause) {
        super(message, cause);
        this.roleId = roleId;
        this.actionId = actionId;
    }

    /**
     * Gets the role ID that already has the permission.
     * 
     * @return the role ID
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * Gets the action ID for which permission already exists.
     * 
     * @return the action ID
     */
    public String getActionId() {
        return actionId;
    }
}