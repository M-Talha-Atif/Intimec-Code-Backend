package com.aidentx.iam.identity_management.model.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class ExtendedAttributeValueId implements Serializable {

    private UUID identityId;
    private UUID definitionId;

    public ExtendedAttributeValueId() {}

    public ExtendedAttributeValueId(UUID identityId, UUID definitionId) {
        this.identityId = identityId;
        this.definitionId = definitionId;
    }

    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    public UUID getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(UUID definitionId) {
        this.definitionId = definitionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtendedAttributeValueId that = (ExtendedAttributeValueId) o;
        return Objects.equals(identityId, that.identityId) && Objects.equals(definitionId, that.definitionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityId, definitionId);
    }
}
