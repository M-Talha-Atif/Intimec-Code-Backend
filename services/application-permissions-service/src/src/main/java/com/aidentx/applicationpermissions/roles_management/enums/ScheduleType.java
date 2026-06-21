package com.aidentx.applicationpermissions.roles_management.enums;

/**
 * Enumeration for Dynamic Rule Schedule Types.
 * 
 * <p>Defines the reconciliation frequency for dynamic role assignment rules:</p>
 * <ul>
 *   <li><strong>IMMEDIATE</strong> - Continuous real-time reconciliation (cron job runs every minute)</li>
 *   <li><strong>DAILY</strong> - Reconciliation runs once per day at specified time</li>
 *   <li><strong>WEEKLY</strong> - Reconciliation runs once per week at specified time</li>
 *   <li><strong>MONTHLY</strong> - Reconciliation runs once per month at specified time</li>
 * </ul>
 * 
 * <p><strong>IMPORTANT:</strong> IMMEDIATE schedule means the reconciliation job continuously
 * monitors and assigns/removes roles in near real-time. When a new user is added to the system
 * who matches the rule conditions, they will be automatically assigned the role within minutes.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public enum ScheduleType {
    /**
     * Continuous real-time reconciliation mode.
     * 
     * <p>The reconciliation job runs <strong>every minute</strong>, constantly checking if users
     * match the ruleConditionScript and assigning/removing roles immediately when conditions change.</p>
     * 
     * <p><strong>Use Case:</strong> When you need near real-time role assignments. For example:
     * <ul>
     *   <li>A new user is created in the system</li>
     *   <li>User attributes are updated (department, location, etc.)</li>
     *   <li>The user should be immediately assigned roles based on their attributes</li>
     * </ul></p>
     * 
     * <p><strong>Performance Note:</strong> This mode is optimized for handling millions of users
     * using parallel processing and Spring Batch patterns.</p>
     * 
     * <p>Time field is <strong>null</strong> for IMMEDIATE execution.</p>
     */
    IMMEDIATE,
    
    /**
     * Daily scheduled reconciliation at a specific time.
     * 
     * <p>The reconciliation runs <strong>once per day</strong> at the specified time,
     * checking all users and assigning/removing roles based on the rule conditions.</p>
     * 
     * <p>Time field is <strong>required</strong> (e.g., "02:00" for 2 AM, "14:30" for 2:30 PM).</p>
     */
    DAILY,
    
    /**
     * Weekly scheduled reconciliation at a specific time.
     * 
     * <p>The reconciliation runs <strong>once per week</strong> at the specified time,
     * checking all users and assigning/removing roles based on the rule conditions.</p>
     * 
     * <p>Time field is <strong>required</strong> (e.g., "02:00" for 2 AM, "14:30" for 2:30 PM).</p>
     */
    WEEKLY,
    
    /**
     * Monthly scheduled reconciliation at a specific time.
     * 
     * <p>The reconciliation runs <strong>once per month</strong> at the specified time,
     * checking all users and assigning/removing roles based on the rule conditions.</p>
     * 
     * <p>Time field is <strong>required</strong> (e.g., "02:00" for 2 AM, "14:30" for 2:30 PM).</p>
     */
    MONTHLY
}