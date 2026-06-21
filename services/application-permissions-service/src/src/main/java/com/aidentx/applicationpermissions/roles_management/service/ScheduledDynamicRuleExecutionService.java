package com.aidentx.applicationpermissions.roles_management.service;

/**
 * Service interface for scheduled dynamic rule execution.
 * 
 * <p>This service handles the automated execution of dynamic rules based on their
 * configured schedule types (DAILY, WEEKLY, MONTHLY). It provides:</p>
 * <ul>
 *   <li>Multi-threaded execution with tenant isolation</li>
 *   <li>Circuit breaker pattern for resilience</li>
 *   <li>Execution history tracking for duplicate prevention</li>
 *   <li>Comprehensive monitoring and metrics</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public interface ScheduledDynamicRuleExecutionService {

    /**
     * Check if the circuit breaker is currently open.
     * When open, scheduled executions are skipped to prevent cascading failures.
     * 
     * @return true if circuit breaker is open, false otherwise
     */
    boolean isCircuitBreakerOpen();

    /**
     * Get the current count of consecutive failures.
     * Used for monitoring and alerting purposes.
     * 
     * @return number of consecutive failures
     */
    int getConsecutiveFailures();

    /**
     * Manually trigger scheduled execution.
     * Useful for testing, debugging, or manual intervention scenarios.
     * 
     * <p>This method bypasses the normal scheduled timing but still respects
     * the circuit breaker state and execution history for duplicate prevention.</p>
     */
    void triggerScheduledExecution();
}