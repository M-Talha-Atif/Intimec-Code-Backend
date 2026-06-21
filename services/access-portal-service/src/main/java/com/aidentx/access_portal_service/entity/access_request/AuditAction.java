package com.aidentx.access_portal_service.entity.access_request;

/**
 * Actions that can be audited on an access request.
 */
public enum AuditAction {
    /**
     * Request was created
     */
    REQUEST_CREATED,

    /**
     * Request was submitted for approval
     */
    REQUEST_SUBMITTED,

    /**
     * Request status was updated (general status change)
     */
    STATUS_CHANGED,

    /**
     * Request was fully approved
     */
    REQUEST_APPROVED,

    /**
     * Request was partially approved
     */
    REQUEST_PARTIALLY_APPROVED,

    /**
     * Request was rejected
     */
    REQUEST_REJECTED,

    /**
     * Request was withdrawn by requester
     */
    REQUEST_WITHDRAWN,

    /**
     * Request was cancelled by system or admin
     */
    REQUEST_CANCELLED,

    /**
     * Request was modified/updated (content changed)
     */
    REQUEST_MODIFIED,

    /**
     * Request was escalated
     */
    REQUEST_ESCALATED,

    /**
     * Comment was added to request
     */
    COMMENT_ADDED,

    /**
     * Attachment was added to request
     */
    ATTACHMENT_ADDED,

    /**
     * Attachment was removed from request
     */
    ATTACHMENT_REMOVED,

    /**
     * Approval step was assigned to an approver
     */
    APPROVAL_ASSIGNED,

    /**
     * Approval step was approved
     */
    APPROVAL_APPROVED,

    /**
     * Approval step was rejected
     */
    APPROVAL_REJECTED,

    /**
     * Approval step was delegated
     */
    APPROVAL_DELEGATED,

    /**
     * Approval step was completed (includes all decisions)
     */
    APPROVAL_STEP_COMPLETED,

    /**
     * Approver was changed for a step
     */
    APPROVER_CHANGED,

    /**
     * Provisioning process started
     */
    PROVISIONING_STARTED,

    /**
     * Access was fully provisioned
     */
    PROVISIONING_COMPLETED,

    /**
     * Access was partially provisioned
     */
    PARTIALLY_PROVISIONED,

    /**
     * Access provisioning failed
     */
    PROVISIONING_FAILED,

    /**
     * Access was deprovisioned
     */
    DEPROVISIONED,


    REQUEST_FULLY_APPROVED,


}