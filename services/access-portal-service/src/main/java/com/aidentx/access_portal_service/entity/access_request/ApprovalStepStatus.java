package com.aidentx.access_portal_service.entity.access_request;

/**
 * Status of an approval step in the workflow.
 */
public enum ApprovalStepStatus {
    /**
     * Approval step is pending assignment to an approver
     */
    PENDING_ASSIGNMENT,

    /**
     * Approval step has been assigned to an approver
     */
    ASSIGNED,

    /**
     * Approver is reviewing the request
     */
    IN_REVIEW,

    /**
     * Approval step has been completed
     */
    COMPLETED,

    /**
     * Approval step was skipped due to auto-approval or escalation
     */
    SKIPPED,

    /**
     * Approval step has been approved
     */

    APPROVED,

    /**
     * Approval step has been rejected
     */
    REJECTED,


    /**
     * Approval step has been delegated to another user
     */
    DELEGATED,

    /**
     * Approval step has expired without response
     */
    EXPIRED
}