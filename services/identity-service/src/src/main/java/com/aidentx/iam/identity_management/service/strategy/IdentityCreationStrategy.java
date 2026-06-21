package com.aidentx.iam.identity_management.service.strategy;

import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.enums.IdentityType;

import java.util.Map;
import java.util.UUID;

/**
 * Strategy interface for creating and updating different types of identities.
 */
public interface IdentityCreationStrategy {

    /**
     * Creates a new identity of the specific type.
     *
     * @param tenantId           The tenant ID
     * @param dto                The creation DTO
     * @param extendedAttributes The extended attributes
     * @return The ID of the created identity
     */
    UUID create(String tenantId, Object dto, Map<String, Object> extendedAttributes);

    /**
     * Updates an existing identity of the specific type.
     *
     * @param identity           The identity to update
     * @param dto                The update DTO
     * @param extendedAttributes The extended attributes
     */
    void update(Identity identity, Object dto, Map<String, Object> extendedAttributes);

    /**
     * Returns the identity type this strategy handles.
     *
     * @return The identity type
     */
    IdentityType getIdentityType();
}
