package com.aidentx.workflowsservice.enums;

/**
 * Phase 7: Failure policy for parallel branch execution
 * Determines how the workflow should respond when parallel branches fail
 */
public enum FailurePolicy {
    /**
     * FAIL_FAST: If ANY branch fails, immediately fail the entire workflow execution
     * Use when all branches are critical and none can fail
     * Example: Employee onboarding where account creation, badge issuance, and system access ALL required
     */
    FAIL_FAST,
    
    /**
     * FAIL_SAFE: Continue execution even if some branches fail
     * Merge only successful branch contexts at join
     * Use when some branches are optional or can be retried later
     * Example: Sending notifications to multiple channels - email failure shouldn't stop SMS
     */
    FAIL_SAFE,
    
    /**
     * THRESHOLD: Fail if more than X% of branches fail (configurable threshold)
     * Use when you need majority success but can tolerate some failures
     * Example: Multi-region deployment - acceptable if 1 out of 5 regions fails
     */
    THRESHOLD
}
