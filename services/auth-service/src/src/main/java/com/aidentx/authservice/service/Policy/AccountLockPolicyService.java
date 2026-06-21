package com.aidentx.authservice.service.Policy;

import com.aidentx.authservice.model.dto.AccountLockPolicyRequest;
import com.aidentx.authservice.model.dto.AccountLockPolicyResponse;
import com.aidentx.authservice.model.dto.StatusMessageResponse;

/**
 * Service interface for account lock policy management operations.
 * <p>
 * This interface defines the contract for account lock policy CRUD operations
 * with tenant-specific isolation. Each tenant can have only one account lock policy.
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
public interface AccountLockPolicyService {

    /**
     * Creates a new account lock policy for the specified tenant.
     * <p>
     * Each tenant can have only one account lock policy. If a policy already exists
     * for the tenant, this method will throw an exception.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @param request  the account lock policy creation request
     * @return status response indicating success or failure
     * @throws com.aidentx.authservice.exception.PolicyAlreadyExistsException if policy already exists
     */
    StatusMessageResponse createAccountLockPolicy(String tenantId, AccountLockPolicyRequest request);

    /**
     * Updates the existing account lock policy for the specified tenant.
     * <p>
     * The policy must exist for the tenant, otherwise an exception will be thrown.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @param request  the account lock policy update request
     * @return status response indicating success or failure
     * @throws com.aidentx.authservice.exception.PolicyNotFoundException if policy does not exist
     */
    StatusMessageResponse updateAccountLockPolicy(String tenantId, AccountLockPolicyRequest request);

    /**
     * Retrieves the account lock policy for the specified tenant.
     * <p>
     * Returns the tenant's account lock policy if it exists.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @return the account lock policy response
     * @throws com.aidentx.authservice.exception.PolicyNotFoundException if policy does not exist
     */
    AccountLockPolicyResponse getAccountLockPolicy(String tenantId);

    /**
     * Deletes the account lock policy for the specified tenant.
     * <p>
     * Removes the tenant's account lock policy if it exists.
     * </p>
     *
     * @param tenantId the tenant identifier
     * @return status response indicating success or failure
     * @throws com.aidentx.authservice.exception.PolicyNotFoundException if policy does not exist
     */
    StatusMessageResponse deleteAccountLockPolicy(String tenantId);
}
