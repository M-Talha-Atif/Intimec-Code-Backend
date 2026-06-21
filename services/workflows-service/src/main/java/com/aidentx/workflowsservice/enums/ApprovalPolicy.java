package com.aidentx.workflowsservice.enums;

/**
 * Approval policy for multi-approval nodes
 * Determines how many approvals are needed for the approval group to complete
 * Follows the same pattern as FailurePolicy for parallel branches
 * 
 * @author aidentx Development Team
 * @version 1.0
 * @see FailurePolicy
 */
public enum ApprovalPolicy {
    /**
     * ALL: All approvers must approve (current default behavior)
     * Use when unanimous consent required
     * Example: High-risk access requiring security + compliance + manager approval
     * Workflow continues only when ALL approvals are APPROVED
     */
    ALL,
    
    /**
     * ANY: First approval wins (fail-fast for approvals)
     * Use when any authorized person can approve
     * Example: Helpdesk escalation - any available manager can approve
     * Workflow continues as soon as ANY approval is APPROVED
     */
    ANY,
    
    /**
     * THRESHOLD: N of M must approve (configurable threshold)
     * Use when majority consensus needed
     * Example: Board vote - 3 of 5 directors must approve
     * Workflow continues when threshold number of approvals are APPROVED
     */
    THRESHOLD,
    
    /**
     * FAIL_FAST: First rejection immediately fails (opposite of ANY)
     * Use when any dissent blocks the request
     * Example: Security review - any security officer can block
     * Workflow continues (with rejection) as soon as ANY approval is REJECTED
     */
    FAIL_FAST
}
