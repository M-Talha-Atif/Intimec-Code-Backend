package com.aidentx.iam.shadow_management.exception;

import java.util.UUID;

/**
 * Exception thrown when a shadow is not found.
 */
public class ShadowNotFoundException extends RuntimeException {

    private final UUID shadowId;

    public ShadowNotFoundException(UUID shadowId) {
        super(String.format("Shadow not found with ID: %s", shadowId));
        this.shadowId = shadowId;
    }

    public ShadowNotFoundException(String resourceId, String resourceUid) {
        super(String.format("Shadow not found for resource '%s' with UID '%s'", resourceId, resourceUid));
        this.shadowId = null;
    }

    public UUID getShadowId() {
        return shadowId;
    }
}
