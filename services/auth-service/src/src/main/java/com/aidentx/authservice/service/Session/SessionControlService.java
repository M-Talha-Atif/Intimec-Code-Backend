package com.aidentx.authservice.service.Session;

import com.aidentx.authservice.model.dto.SessionControlRequest;
import com.aidentx.authservice.model.dto.SessionControlResponse;
import com.aidentx.authservice.model.dto.StatusMessageResponse;

/**
 * Service interface for session control policy management operations.
 * <p>
 * This interface defines the contract for session control policy CRUD operations
 * with tenant-specific isolation. Each tenant can have only one session control policy.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
public interface SessionControlService {

    /**
     * Creates a new session control policy for the specified tenant.
     * <p>
     * Each tenant can have only one session control policy. If a policy already exists
     * for the tenant, this method will throw an exception.
     * JWT expiration is provided in minutes and refresh token expiration in hours,
     * both are converted to milliseconds internally for storage.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @param request  the session control policy creation request
     * @return status response indicating success or failure
     * @throws com.aidentx.authservice.exception.PolicyAlreadyExistsException if policy already exists
     */
    StatusMessageResponse createSessionControl(String tenantId, SessionControlRequest request);

    /**
     * Updates the existing session control policy for the specified tenant.
     * <p>
     * The policy must exist for the tenant, otherwise an exception will be thrown.
     * JWT expiration is provided in minutes and refresh token expiration in hours,
     * both are converted to milliseconds internally for storage.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @param request  the session control policy update request
     * @return status response indicating success or failure
     * @throws com.aidentx.authservice.exception.PolicyNotFoundException if policy does not exist
     */
    StatusMessageResponse updateSessionControl(String tenantId, SessionControlRequest request);

    /**
     * Retrieves the session control policy for the specified tenant.
     * <p>
     * Returns the tenant's session control policy if it exists.
     * Expiration times are returned both in milliseconds and convenient units (minutes/hours).
     * </p>
     *
     * @param tenantId the tenant identifier
     * @return the session control policy response
     * @throws com.aidentx.authservice.exception.PolicyNotFoundException if policy does not exist
     */
    SessionControlResponse getSessionControl(String tenantId);

    /**
     * Deletes the session control policy for the specified tenant.
     * <p>
     * Removes the tenant's session control policy if it exists.
     * If the policy doesn't exist, the operation is considered successful (idempotent).
     * </p>
     *
     * @param tenantId the tenant identifier
     * @return status response indicating success or failure
     */
    StatusMessageResponse deleteSessionControl(String tenantId);
}
