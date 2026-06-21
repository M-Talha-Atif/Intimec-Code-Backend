package com.aidentx.access_portal_service.entity.access_request;

/**
 * Decision made by an approver on an approval step.
 */
public enum ApprovalDecision {
    /**
     * No decision has been made yet
     */
    PENDING,

    /**
     * Request approved
     */
    APPROVED,

    /**
     * Request rejected
     */
    REJECTED,

    /**
     * More information requested before decision
     */
    INFO_REQUESTED,

    /**
     * Request escalated to higher authority
     */
    ESCALATED,

    /**
     * Approval delegated to another user
     */
    DELEGATED
}