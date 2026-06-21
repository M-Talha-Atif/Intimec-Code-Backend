package com.aidentx.applicationpermissions.roles_management.service;

import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.roles_management.model.dto.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for user-role assignment operations.
 * Provides methods for assigning, removing, and retrieving user-role assignments
 * with tenant-aware access control and proper validation.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public interface UserRoleService {

        /**
     * Assigns a role to a user or multiple users (bulk assignment). Supports both single assignment (when userId is provided) and 
     * bulk assignment (when userIds is provided).
     * 
     * @param request the role assignment request (single or bulk)
     * @return the assignment response - single UserRoleResponse for single assignment, 
     *         or BulkUserRoleResponse for bulk assignment
     * @throws IllegalArgumentException if user(s) or role does not exist or belongs to different tenant
     * @throws IllegalStateException if the user already has the role assigned (single assignment only)
     */
    Object assignRole(AssignRoleRequest request);

    /**
     * Assigns a role to a user or multiple users within the specified tenant.
     * This method is used for scheduled execution contexts where HTTP request context is not available.
     * 
     * @param request the role assignment request (single or bulk)
     * @param tenantId the tenant ID for the assignment
     * @return the assignment response - single UserRoleResponse for single assignment, 
     *         or BulkUserRoleResponse for bulk assignment
     * @throws IllegalArgumentException if user(s) or role does not exist or belongs to different tenant
     * @throws IllegalStateException if the user already has the role assigned (single assignment only)
     */
    Object assignRole(AssignRoleRequest request, String tenantId);

    /**
     * Removes a role from a user within the current tenant.
     * 
     * @param request the role removal request
     * @return true if the role was successfully removed, false if assignment didn't exist
     * @throws IllegalArgumentException if user or role does not exist or belongs to different tenant
     */
    boolean removeRole(RemoveRoleRequest request);

    /**
     * Retrieves all roles assigned to a specific user within the current tenant.
     * 
     * @param userId the user ID to get roles for
     * @return list of user-role assignment responses
     * @throws IllegalArgumentException if user does not exist or belongs to different tenant
     */
    List<UserRoleResponse> getUserRoles(UUID userId);

    /**
     * Retrieves all users assigned to a specific role within the current tenant.
     * 
     * @param roleId the role ID to get users for
     * @return list of user-role assignment responses
     * @throws IllegalArgumentException if role does not exist or belongs to different tenant
     */
    List<UserRoleResponse> getRoleUsers(UUID roleId);

    /**
     * Retrieves all user-role assignments within the current tenant.
     * 
     * @return list of all user-role assignment responses
     */
    List<UserRoleResponse> getAllUserRoles();

    /**
     * Checks if a user has a specific role within the current tenant.
     * 
     * @param userId the user ID to check
     * @param roleId the role ID to check
     * @return true if the user has the role, false otherwise
     */
    boolean hasRole(UUID userId, UUID roleId);

    /**
     * Checks if a user has a specific role within the specified tenant.
     * This method is used for scheduled execution contexts where HTTP request context is not available.
     * 
     * @param userId the user ID to check
     * @param roleId the role ID to check
     * @param tenantId the tenant ID for the check
     * @return true if the user has the role, false otherwise
     */
    boolean hasRole(UUID userId, UUID roleId, String tenantId);

    /**
     * Removes all role assignments for a specific user within the current tenant.
     * 
     * @param userId the user ID to remove all roles from
     * @return number of role assignments removed
     * @throws IllegalArgumentException if user does not exist or belongs to different tenant
     */
    int removeAllUserRoles(UUID userId);

    /**
     * Removes all user assignments for a specific role within the current tenant.
     * 
     * @param roleId the role ID to remove all users from
     * @return number of user assignments removed
     * @throws IllegalArgumentException if role does not exist or belongs to different tenant
     */
    int removeAllRoleUsers(UUID roleId);
    
    /**
     * Gets all users who have a specific role within the specified tenant.
     * 
     * @param roleId the role ID to check
     * @param tenantId the tenant ID for the check
     * @return list of user IDs who have the role
     */
    List<UUID> getUsersWithRole(UUID roleId, String tenantId);
    
    /**
     * Removes a role from a specific user.
     * 
     * @param userId the user ID to remove the role from
     * @param roleId the role ID to remove
     * @throws IllegalArgumentException if user or role does not exist
     */
    void unassignRole(UUID userId, UUID roleId);
    
    /**
     * Assigns a role to a user with a specific assignment type (MANUAL or DYNAMIC).
     * This method is primarily used by the dynamic rule reconciliation service.
     * 
     * @param request the role assignment request
     * @param tenantId the tenant ID for the assignment
     * @param assignmentType the type of assignment (MANUAL or DYNAMIC)
     * @return the assignment response
     * @throws IllegalArgumentException if user or role does not exist or belongs to different tenant
     * @throws IllegalStateException if the user already has the role assigned
     */
    Object assignRoleWithType(AssignRoleRequest request, String tenantId, AssignmentType assignmentType);
    
    /**
     * Revokes a role from multiple users (bulk revocation).
     * 
     * @param request the bulk role revocation request
     * @return BulkRoleRevocationResponse with details of successful and failed revocations
     * @throws IllegalArgumentException if role does not exist or belongs to different tenant
     */
    BulkRoleRevocationResponse revokeRole(RevokeRoleRequest request);
    
    /**
     * Submits a user role assignment request for approval workflow.
     * This publishes an event that triggers a workflow with approval nodes.
     * Upon approval, the actual assignment is performed via HTTP callback.
     * 
     * @param request the approval submission request
     * @return Map with success status and message
     * @throws IllegalArgumentException if role does not exist or belongs to different tenant
     */
    Map<String, Object> submitUserRoleAssignmentForApproval(SubmitUserRoleAssignmentForApprovalRequest request);
    
    /**
     * Submits a user role revocation request for approval workflow.
     * This publishes an event that triggers a workflow with approval nodes.
     * Upon approval, the actual revocation is performed via HTTP callback.
     * 
     * @param request the approval submission request
     * @return Map with success status and message
     * @throws IllegalArgumentException if role does not exist or belongs to different tenant
     */
    Map<String, Object> submitUserRoleRevocationForApproval(SubmitUserRoleRevocationForApprovalRequest request);


    /**
     * Assigns multiple roles to multiple users (M×N assignment).
     * Supports both synchronous and asynchronous processing based on batch size.
     *
     * <p>For small batches (<1000 assignments), processes synchronously and returns full results.
     * For large batches (≥1000 assignments), processes asynchronously and returns batch job ID.</p>
     *
     * @param request the bulk multi-role assignment request
     * @return BatchJobResponse with batch ID and tracking URL
     * @throws IllegalArgumentException if roles or users do not exist or belong to different tenant
     */
    BulkMultiRoleAssignmentResponse assignMultipleRolesToMultipleUsers(BulkMultiRoleAssignmentRequest request);

    /**
     * Gets the status of a batch job by ID.
     *
     * @param batchId the batch job UUID
     * @return BatchJobResponse with current status and progress
     * @throws IllegalArgumentException if batch job not found or belongs to different tenant
     */
    BatchJobResponse getBatchJobStatus(UUID batchId);


    /**
     * Assigns multiple roles to multiple users based on access portal cart selections.
     * Each role can have different users, different justifications, and different validity periods.
     *
     * <p><b>Processing Strategy:</b></p>
     * <ul>
     *   <li>Small batches (<1000 assignments): Synchronous processing with immediate response</li>
     *   <li>Large batches (≥1000 assignments): Asynchronous processing via Kafka (TODO: NOT IMPLEMENTED YET)</li>
     * </ul>
     *
     * <p><b>Example Use Case:</b></p>
     * <pre>
     * User adds to cart:
     * - AdminRole for [User1, User2, User3] with "Need admin access" justification, 30 days validity
     * - ViewerRole for [User1, User4] with "Read-only access" justification, 60 days validity
     * - EditorRole for [User5] with "Content editing" justification, 90 days validity
     * </pre>
     *
     * @param request the cart-based assignment request containing role-user mappings
     * @return response with assignment results (successful, skipped, failed)
     * @throws IllegalArgumentException if any role or user does not exist or belongs to different tenant
     */
    CartBasedAssignmentResponse assignRolesFromCart(AssignRolesFromCartRequest request);
}