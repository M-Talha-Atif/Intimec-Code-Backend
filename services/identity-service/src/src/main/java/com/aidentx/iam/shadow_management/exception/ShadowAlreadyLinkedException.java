package com.aidentx.iam.shadow_management.exception;

import java.util.UUID;

/**
 * Exception thrown when attempting to link a shadow that is already linked.
 */
public class ShadowAlreadyLinkedException extends RuntimeException {

    private final UUID shadowId;
    private final UUID existingIdentityId;

    public ShadowAlreadyLinkedException(UUID shadowId, UUID existingIdentityId) {
        super(String.format("Shadow %s is already linked to identity %s", shadowId, existingIdentityId));
        this.shadowId = shadowId;
        this.existingIdentityId = existingIdentityId;
    }

    public UUID getShadowId() {
        return shadowId;
    }

    public UUID getExistingIdentityId() {
        return existingIdentityId;
    }
}
