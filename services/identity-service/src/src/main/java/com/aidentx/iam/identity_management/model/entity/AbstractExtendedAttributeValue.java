package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * An abstract base class for all extended attribute value entities.
 * It defines the composite primary key shared by all value tables.
 */
@MappedSuperclass
@IdClass(ExtendedAttributeValueId.class)
public abstract class AbstractExtendedAttributeValue implements Serializable {

    @Id
    @Column(name = "identity_id")
    private UUID identityId;

    @Id
    @Column(name = "definition_id")
    private UUID definitionId;

    // --- Getters, Setters, hashCode, equals ---

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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AbstractExtendedAttributeValue that = (AbstractExtendedAttributeValue) o;
        return Objects.equals(identityId, that.identityId) && Objects.equals(definitionId, that.definitionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityId, definitionId);
    }
}