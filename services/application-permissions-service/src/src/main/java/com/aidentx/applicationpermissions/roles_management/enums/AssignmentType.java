package com.aidentx.applicationpermissions.roles_management.enums;

/**
 * Enumeration defining how a role is assigned to a user.
 * 
 * <p>This enum represents the source or method through which a user-role
 * relationship was established in the system.</p>
 * 
 * <p>Assignment types are used to distinguish between:</p>
 * <ul>
 *   <li><strong>MANUAL:</strong> Direct assignments made by administrators via the UI or API</li>
 *   <li><strong>DYNAMIC:</strong> Automatic assignments triggered by dynamic rules based on user attributes</li>
 * </ul>
 * 
 * <p>This distinction is critical for:</p>
 * <ul>
 *   <li>Audit trails and compliance reporting</li>
 *   <li>Understanding role assignment patterns</li>
 *   <li>Calculating role statistics (e.g., "hasManualAssignments", "hasDynamicRules")</li>
 *   <li>UI display and filtering logic</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public enum AssignmentType {
    
    /**
     * Manual assignment type.
     * 
     * <p>Indicates that a role was explicitly assigned to a user through direct action
     * by an administrator or privileged user. These assignments are typically made via:</p>
     * <ul>
     *   <li>The role assignment UI interface</li>
     *   <li>Direct API calls to /api/v1/user-roles/assign endpoint</li>
     *   <li>Batch user-role import operations</li>
     * </ul>
     * 
     * <p>Manual assignments remain in effect until explicitly revoked by an administrator
     * and are not affected by changes to dynamic rules or user attributes.</p>
     */
    MANUAL,
    
    /**
     * Dynamic assignment type.
     * 
     * <p>Indicates that a role was automatically assigned to a user based on evaluation
     * of dynamic assignment rules. These assignments are triggered when:</p>
     * <ul>
     *   <li>A new dynamic rule is created and evaluated against existing users</li>
     *   <li>User attributes change and match dynamic rule conditions</li>
     *   <li>The CDC (Change Data Capture) system detects relevant user updates</li>
     *   <li>Scheduled rule reconciliation processes execute</li>
     * </ul>
     * 
     * <p>Dynamic assignments may be automatically revoked when:</p>
     * <ul>
     *   <li>User attributes no longer match the dynamic rule conditions</li>
     *   <li>The dynamic rule is deactivated or deleted</li>
     *   <li>Rule evaluation determines the user should no longer have the role</li>
     * </ul>
     */
    DYNAMIC
}
