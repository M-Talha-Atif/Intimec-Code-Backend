package com.aidentx.applicationpermissions.roles_management.service;

/**
 * Service for scheduled execution of Global Dynamic Rules.
 * 
 * <p>This service handles the automated execution of global dynamic rules
 * that are configured with DAILY, WEEKLY, or MONTHLY schedules.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public interface ScheduledGlobalDynamicRuleExecutionService {
    
    /**
     * Check and execute global dynamic rules that are due for execution.
     * This method is triggered by a scheduled task every minute.
     */
    void checkAndExecuteScheduledRules();
}
