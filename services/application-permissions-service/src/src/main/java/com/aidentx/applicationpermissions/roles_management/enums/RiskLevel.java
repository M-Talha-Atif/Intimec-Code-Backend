package com.aidentx.applicationpermissions.roles_management.enums;

/**
 * Enumeration representing the risk level associated with a role.
 * 
 * <p>Risk levels help categorize roles based on their potential impact
 * on system security and business operations. Higher risk levels typically
 * require additional approval processes and monitoring.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public enum RiskLevel {
    
    /**
     * Low risk level - minimal impact on system security.
     * Typically assigned to read-only or basic operational roles.
     */
    Low,
    
    /**
     * Medium risk level - moderate impact on system security.
     * Assigned to roles with limited administrative capabilities.
     */
    Medium,
    
    /**
     * High risk level - significant impact on system security.
     * Assigned to roles with administrative or privileged access.
     */
    High
}