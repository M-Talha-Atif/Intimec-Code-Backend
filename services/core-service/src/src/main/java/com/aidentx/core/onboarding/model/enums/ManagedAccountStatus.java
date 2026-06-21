package com.aidentx.core.onboarding.model.enums;

/**
 * Represents the lifecycle status of a ManagedAccount from the IGA platform's
 * perspective.
 * This is a standardized, internal status used for policy, correlation, and
 * access reviews.
 * It is distinct from the application-specific native status stored in the
 * attributes.
 */
public enum ManagedAccountStatus {

    // --- Core Lifecycle States ---

    /**
     * The account is linked to an active identity and is considered enabled and in
     * use.
     */
    ACTIVE,

    /**
     * The account is linked to an identity but is administratively disabled on the
     * target system.
     * This is the target state for a "leaver" process.
     */
    INACTIVE,

    // --- Discovery & Correlation States ---

    /**
     * The account has just been discovered by aggregation and has not yet been
     * processed
     * by the correlation engine. This is its initial "purgatory" state.
     */
    UNMATCHED,

    /**
     * The account exists on the target system but, after a correlation run, could
     * not be
     * linked to any active identity. This represents a potential security risk.
     */
    ORPHANED,

    // --- Security & Temporary States ---

    /**
     * The account is temporarily locked on the target system, typically due to
     * excessive failed login attempts. This is distinct from being INACTIVE.
     */
    LOCKED,

    // --- Provisioning States (for future use) ---

    /**
     * A request to create this account has been sent, but confirmation has not yet
     * been received.
     */
    PROVISIONING,

    /**
     * A request to delete this account has been sent. Upon confirmation from the
     * next aggregation run, the status will change to DELETED.
     */
    DEPROVISIONING,

    /**
     * The request to delete this account has failed.
     */
    DEPROVISIONING_FAILED,

    /**
     * The request to create this account has failed.
     */
    PROVISIONING_FAILED,
    /**
     * The account was confirmed to no longer exist on the target system during an
     * aggregation run.
     * This record is a "tombstone" kept for auditing and historical purposes.
     */
    DELETED,

    /*
     * The account is linked to multiple identities with the same attribute value.
     * This requires manual review and resolution.
     */
    MULTI_MATCHED
}