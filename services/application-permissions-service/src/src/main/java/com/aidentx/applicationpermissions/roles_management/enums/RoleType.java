package com.aidentx.applicationpermissions.roles_management.enums;

/**
 * Enumeration defining the different types of roles available in the system.
 * 
 * <p>This enum represents the hierarchical access levels for users within
 * the application, from highest privileges (ADMINISTRATOR) to lowest (VIEWER).</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public enum RoleType {
    
    /**
     * System role for internal system operations and automated processes.
     * This role is typically assigned to service accounts and system processes
     * that require elevated privileges for automated tasks.
     */
    SYSTEM,
    
    /**
     * Administrator role with full system access and management capabilities.
     * Users with this role can perform all operations including system configuration,
     * user management, and access to all resources.
     */
    ADMINISTRATOR,
    
    /**
     * Reviewer role with read and review capabilities.
     * Users with this role can review data, approve/reject requests, and access
     * most resources for review purposes but cannot modify system configurations.
     */
    REVIEWER,
    
    /**
     * Operator role with operational access to perform day-to-day tasks.
     * Users with this role can execute standard operations, create and modify
     * data within their scope, but cannot access administrative functions.
     */
    OPERATOR,
    
    /**
     * Viewer role with read-only access to the system.
     * Users with this role can only view data and reports within their
     * permitted scope but cannot modify any information.
     */
    VIEWER 
}