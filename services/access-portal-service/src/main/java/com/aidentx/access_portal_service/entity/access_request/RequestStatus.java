package com.aidentx.access_portal_service.entity.access_request;

/**
 * Status of an access request throughout its lifecycle.
 */
public enum RequestStatus {
    /**
     * Request has been created but not yet submitted
     */
    DRAFT,

    /**
     * Request has been submitted and is awaiting approval
     */
    SUBMITTED,

    /**
     * Request is currently under review by approvers
     */
    PENDING_APPROVAL,

    /**
     * Request has been approved
     */
    APPROVED,

    /**
     * Request has been rejected
     */
    REJECTED,

    /**
     * Access has been provisioned to target systems
     */
    PROVISIONED,

    /**
     * Request was withdrawn by the requester
     */
    WITHDRAWN,

    /**
     * Request was cancelled by system or administrator
     */
    CANCELLED,

    /**
     * Request has been partially approved
     */
    PARTIALLY_APPROVED,

    /**
     * Access has been partially provisioned to target systems
     */
    PARTIALLY_PROVISIONED
}