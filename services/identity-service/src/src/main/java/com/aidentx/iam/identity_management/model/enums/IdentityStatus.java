package com.aidentx.iam.identity_management.model.enums;

/**
 * Enumeration representing the possible status values for an identity.
 * <p>
 * This enum defines the lifecycle states an identity can be in within the system,
 * providing clear status tracking for identity management operations.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
public enum IdentityStatus {
    
    /**
     * Active status - the identity is currently active and operational.
     */
    ACTIVE("Active"),
    
    /**
     * Inactive status - the identity is temporarily inactive but not deleted.
     */
    INACTIVE("Inactive"),
    
    /**
     * Suspended status - the identity is suspended due to policy violations or security concerns.
     */
    SUSPENDED("Suspended"),
    
    /**
     * Terminated status - the identity has been terminated (e.g., employee left).
     */
    TERMINATED("Terminated"),
    
    /**
     * Pending status - the identity is being processed and not yet fully active.
     */
    PENDING("Pending"),
    
    /**
     * Expired status - the identity has expired based on end date or policy.
     */
    EXPIRED("Expired"),
    
    /**
     * Archived status - the identity has been archived for historical purposes.
     */
    ARCHIVED("Archived");
    
    private final String displayName;

    /**
     * Constructor for IdentityStatus enum.
     *
     * @param displayName the human-readable name for this status
     */
    IdentityStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name for this status.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a string representation of this status.
     *
     * @return the display name
     */
    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Checks if this status represents an active state.
     *
     * @return true if the status is ACTIVE, false otherwise
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * Checks if this status represents an inactive state.
     *
     * @return true if the status is INACTIVE, SUSPENDED, TERMINATED, EXPIRED, or ARCHIVED
     */
    public boolean isInactive() {
        return this == INACTIVE || this == SUSPENDED || this == TERMINATED || 
               this == EXPIRED || this == ARCHIVED;
    }

    /**
     * Checks if this status represents a temporary state.
     *
     * @return true if the status is PENDING, SUSPENDED, or INACTIVE
     */
    public boolean isTemporary() {
        return this == PENDING || this == SUSPENDED || this == INACTIVE;
    }

    /**
     * Checks if this status represents a final state.
     *
     * @return true if the status is TERMINATED, EXPIRED, or ARCHIVED
     */
    public boolean isFinal() {
        return this == TERMINATED || this == EXPIRED || this == ARCHIVED;
    }

    /**
     * Gets all active statuses.
     *
     * @return array of statuses that represent active states
     */
    public static IdentityStatus[] getActiveStatuses() {
        return new IdentityStatus[]{ACTIVE};
    }

    /**
     * Gets all inactive statuses.
     *
     * @return array of statuses that represent inactive states
     */
    public static IdentityStatus[] getInactiveStatuses() {
        return new IdentityStatus[]{INACTIVE, SUSPENDED, TERMINATED, EXPIRED, ARCHIVED};
    }

    /**
     * Gets all temporary statuses.
     *
     * @return array of statuses that represent temporary states
     */
    public static IdentityStatus[] getTemporaryStatuses() {
        return new IdentityStatus[]{PENDING, SUSPENDED, INACTIVE};
    }

    /**
     * Gets all final statuses.
     *
     * @return array of statuses that represent final states
     */
    public static IdentityStatus[] getFinalStatuses() {
        return new IdentityStatus[]{TERMINATED, EXPIRED, ARCHIVED};
    }
}
