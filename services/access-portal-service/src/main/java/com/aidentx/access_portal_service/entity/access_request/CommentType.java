package com.aidentx.access_portal_service.entity.access_request;

/**
 * Type of comment on an access request.
 */
public enum CommentType {
    /**
     * General comment or note
     */
    GENERAL,

    /**
     * Request for additional information
     */
    INFO_REQUEST,

    /**
     * Response to an information request
     */
    INFO_RESPONSE,

    /**
     * Approval decision with explanation
     */
    APPROVAL_DECISION,

    /**
     * System-generated notification or status update
     */
    SYSTEM_NOTE,

    /**
     * Escalation justification
     */
    ESCALATION,

    /**
     * Withdrawal explanation
     */
    WITHDRAWAL
}