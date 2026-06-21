package com.aidentx.core.audit_management.enums;

/**
 * Enum representing the roles available in the authentication system.
 * These roles determine the user's access level and permissions.
 * <p>
 * Possible values:
 * <ul>
 *   <li>SUPER_ADMIN - Has complete access to all system functions and configuration.</li>
 *   <li>USER - Standard user with basic application access.</li>
 * </ul>
 * <p>
 * These roles are used in conjunction with Spring Security's role-based access control.
 */
public enum Role {
    SUPER_ADMIN,
    USER
}
