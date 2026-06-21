package com.aidentx.access_portal_service.entity.access_request;

/**
 * Status of access provisioning for an approved request.
 */
public enum ProvisioningStatus {
    /**
     * Provisioning has not started yet
     */
    PENDING,

    /**
     * Provisioning is scheduled for future execution
     */
    SCHEDULED,

    /**
     * Provisioning is in progress
     */
    IN_PROGRESS,

    /**
     * Provisioning completed successfully
     */
    COMPLETED,

    /**
     * Provisioning failed
     */
    FAILED,

    /**
     * Provisioning is being retried after failure
     */
    RETRYING,

    /**
     * Provisioning was cancelled
     */
    CANCELLED,

    /**
     * Access has been deprovisioned
     */
    DEPROVISIONED
}