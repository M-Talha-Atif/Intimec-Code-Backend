package com.aidentx.iam.shadow_management.exception;

/**
 * Exception thrown when a duplicate shadow is detected.
 * A shadow is considered duplicate if another shadow with the same
 * resourceId, resourceUid, and tenantId already exists.
 */
public class DuplicateShadowException extends RuntimeException {

    private final String resourceId;
    private final String resourceUid;

    public DuplicateShadowException(String resourceId, String resourceUid) {
        super(String.format("Shadow already exists for resource '%s' with UID '%s'",
                resourceId, resourceUid));
        this.resourceId = resourceId;
        this.resourceUid = resourceUid;
    }

    /**
     * Constructor for cases where we only have a resourceUid (resourceId not used)
     */
    public DuplicateShadowException(String resourceUid) {
        super(String.format("Shadow already exists with UID '%s'", resourceUid));
        this.resourceId = null;
        this.resourceUid = resourceUid;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getResourceUid() {
        return resourceUid;
    }
}
