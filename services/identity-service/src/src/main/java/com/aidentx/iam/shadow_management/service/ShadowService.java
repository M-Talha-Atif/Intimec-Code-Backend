package com.aidentx.iam.shadow_management.service;

import com.aidentx.iam.shadow_management.model.dto.*;
import org.springframework.data.domain.Page;

import java.util.UUID;

/**
 * Service interface for managing Shadow objects in the Identity Governance and
 * Reconciliation Engine.
 *
 * <p>
 * Shadow objects are persistent representations of resource objects (accounts,
 * groups, entitlements)
 * in the identity-service. They serve as the intermediary layer between
 * external resources and
 * canonical Identity objects.
 * </p>
 *
 * <p>
 * Key operations include:
 * </p>
 *
 * @since 1.0
 */
public interface ShadowService {

    /**
     * Retrieves a paginated list of shadow objects with optional filtering.
     *
     * @param resourceUid Filter by resource unique identifier
     * @param objectClass Filter by ConnID object class
     * @param situation   Filter by synchronization situation
     * @param linked      Filter by link status (true = linked, false = unlinked)
     * @param page        Page number (0-based)
     * @param size        Page size
     * @param sort        Sort specification (e.g., "createdAt,desc")
     * @param tenantId    The tenant ID for isolation
     * @return Paginated list of shadows
     */
    Page<ShadowResponse> listShadows(String resourceUid, String objectClass, String situation,
            Boolean linked, Integer page, Integer size, String sort, String tenantId);

    /**
     * Retrieves a specific shadow object by its ID.
     *
     * @param shadowId The shadow ID
     * @param tenantId The tenant ID for isolation
     * @return The shadow response or empty if not found
     */
    ShadowResponse getShadow(UUID shadowId, String tenantId);

    /**
     * Creates a new shadow object.
     *
     * @param createRequest The create request containing shadow data
     * @param tenantId      The tenant ID for isolation
     * @return The created shadow response
     * @throws DuplicateShadowException if a shadow with same resourceUid and
     *                                  tenantId already exists
     */
    ShadowResponse createShadow(ShadowCreateRequest createRequest, String tenantId);

    /**
     * Updates an existing shadow object.
     *
     * @param shadowId      The shadow ID
     * @param updateRequest The update request containing new shadow data
     * @param tenantId      The tenant ID for isolation
     * @return The updated shadow response
     * @throws ShadowNotFoundException if shadow not found
     */
    ShadowResponse updateShadow(UUID shadowId, ShadowUpdateRequest updateRequest, String tenantId);

    /**
     * Deletes a shadow object.
     *
     * @param shadowId The shadow ID
     * @param tenantId The tenant ID for isolation
     * @return true if deleted, false if not found
     */
    boolean deleteShadow(UUID shadowId, String tenantId);

    /**
     * Links a shadow object to an identity.
     *
     * @param shadowId   The shadow ID
     * @param identityId The identity ID to link to
     * @param tenantId   The tenant ID for isolation
     * @return The updated shadow response
     * @throws ShadowNotFoundException      if shadow not found
     * @throws ShadowAlreadyLinkedException if shadow is already linked
     */
    ShadowResponse linkShadowToIdentity(UUID shadowId, UUID identityId, String tenantId);

    /**
     * Unlinks a shadow object from its associated identity.
     *
     * @param shadowId The shadow ID
     * @param tenantId The tenant ID for isolation
     * @return The updated shadow response
     * @throws ShadowNotFoundException  if shadow not found
     * @throws ShadowNotLinkedException if shadow is not linked
     */
    ShadowResponse unlinkShadowFromIdentity(UUID shadowId, String tenantId);

    /**
     * Retrieves all shadow objects linked to a specific identity.
     *
     * @param identityId  The identity ID
     * @param resourceUid Optional filter by resource UID
     * @param objectClass Optional filter by object class
     * @param tenantId    The tenant ID for isolation
     * @return List of shadows for the identity
     */
    GetShadowsByIdentityResponse getShadowsByIdentity(UUID identityId, String resourceUid,
            String objectClass, String tenantId);
}