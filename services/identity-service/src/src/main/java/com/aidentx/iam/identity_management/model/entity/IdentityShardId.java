package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents the composite primary key for the IdentityShard entity.
 * It uses the identityId and the immutable resourceId of the source
 * application.
 */
@Embeddable
public class IdentityShardId implements Serializable {

    @Column(name = "identity_id")
    private UUID identityId;

    @Column(name = "resource_id")
    private UUID resourceId;

    public IdentityShardId() {
    }

    public IdentityShardId(UUID identityId, UUID resourceId) {
        this.identityId = identityId;
        this.resourceId = resourceId;
    }

    // Getters, Setters, hashCode, and equals
    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        IdentityShardId that = (IdentityShardId) o;
        return Objects.equals(identityId, that.identityId) && Objects.equals(resourceId, that.resourceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityId, resourceId);
    }
}