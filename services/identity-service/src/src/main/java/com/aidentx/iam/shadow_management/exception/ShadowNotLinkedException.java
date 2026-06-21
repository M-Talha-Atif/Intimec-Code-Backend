package com.aidentx.iam.shadow_management.exception;

import java.util.UUID;

/**
 * Exception thrown when attempting to operate on a shadow that is not linked
 * but the operation requires it to be linked.
 */
public class ShadowNotLinkedException extends RuntimeException {

    private final UUID shadowId;

    public ShadowNotLinkedException(UUID shadowId) {
        super(String.format("Shadow %s is not linked to any identity", shadowId));
        this.shadowId = shadowId;
    }

    public UUID getShadowId() {
        return shadowId;
    }
}
