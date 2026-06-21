package com.aidentx.authservice.service.Policy;

import com.aidentx.authservice.model.dto.PasswordPolicyRequest;
import com.aidentx.authservice.model.dto.PasswordPolicyResponse;
import com.aidentx.authservice.model.dto.StatusMessageResponse;

/**
 * Service interface for password policy management operations.
 * <p>
 * This interface defines the contract for password policy CRUD operations
 * with tenant-specific isolation. Each tenant can have only one password policy.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
public interface PasswordPolicyService {

    /**
     * Creates a new password policy for the specified tenant.
     * <p>
     * Each tenant can have only one password policy. If a policy already exists
     * for the tenant, this method will throw an exception.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @param request  the password policy creation request
     * @return status response indicating success or failure
     * @throws com.aidentx.authservice.exception.PolicyAlreadyExistsException if policy already exists
     */
    StatusMessageResponse createPasswordPolicy(String tenantId, PasswordPolicyRequest request);

    /**
     * Updates the existing password policy for the specified tenant.
     * <p>
     * The policy must exist for the tenant, otherwise an exception will be thrown.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @param request  the password policy update request
     * @return status response indicating success or failure
     * @throws com.aidentx.authservice.exception.PolicyNotFoundException if policy does not exist
     */
    StatusMessageResponse updatePasswordPolicy(String tenantId, PasswordPolicyRequest request);

    /**
     * Retrieves the password policy for the specified tenant.
     * <p>
     * Returns the tenant's password policy if it exists.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @return the password policy response
     * @throws com.aidentx.authservice.exception.PolicyNotFoundException if policy does not exist
     */
    PasswordPolicyResponse getPasswordPolicy(String tenantId);

    /**
     * Deletes the password policy for the specified tenant.
     * <p>
     * Removes the tenant's password policy if it exists.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @return status response indicating success or failure
     * @throws com.aidentx.authservice.exception.PolicyNotFoundException if policy does not exist
     */
    StatusMessageResponse deletePasswordPolicy(String tenantId);
}
