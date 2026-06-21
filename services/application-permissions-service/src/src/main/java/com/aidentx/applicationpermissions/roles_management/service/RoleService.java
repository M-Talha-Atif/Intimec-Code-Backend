package com.aidentx.applicationpermissions.roles_management.service;

import com.aidentx.applicationpermissions.roles_management.model.dto.AssignPermissionToRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitPermissionAssignmentForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitPermissionRevocationForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitBulkPermissionAssignmentForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.SubmitBulkPermissionRevocationForApprovalRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignPermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkAssignmentResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRevokePermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.BulkRevocationResult;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdatePermissionRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.CreateRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.UpdateRoleRequest;
import com.aidentx.applicationpermissions.roles_management.model.dto.RoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.GetUsersByRoleResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.GetRoleMembersResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.GetRoleOwnersResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.PermissionDetailsResponse;
import com.aidentx.applicationpermissions.roles_management.model.dto.RoleStatsDTO;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service interface for role management operations.
 * Provides methods for creating, updating, retrieving, and deleting roles
 * with tenant-aware access control.
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public interface RoleService {

    /**
     * Creates a new role for the current tenant.
     * 
     * @param request the role creation request
     * @return the created role response
     */
    RoleResponse createRole(CreateRoleRequest request);

    /**
     * Updates an existing role for the current tenant.
     * 
     * @param id the role ID to update
     * @param request the role update request
     * @return the updated role response
     */
    RoleResponse updateRole(UUID id, UpdateRoleRequest request);

    /**
     * Retrieves a role by ID for the current tenant.
     * 
     * @param id the role ID to retrieve
     * @return the role response
     */
    RoleResponse getRoleById(UUID id);

    /**
     * Retrieves all roles for the current tenant.
     * 
     * @return list of role responses
     */
    List<RoleResponse> getAllRoles();

    /**
     * Retrieves all roles for the current tenant with pagination.
     * 
     * @param page the page number (0-based)
     * @param size the page size
     * @return page of role responses
     */
    Page<RoleResponse> getAllRolesPaginated(int page, int size);

    /**
     * Retrieves all roles for the current tenant with pagination and filters.
     * 
     * @param page the page number (0-based)
     * @param size the page size
     * @param search search term for role name
     * @param roleType filter by role type
     * @param isActive filter by active status
     * @param riskLevel filter by risk level
     * @param tags filter by tags
     * @return page of role responses
     */
    Page<RoleResponse> getAllRolesPaginated(int page, int size, String search, String roleType, 
                                           Boolean isActive, String riskLevel, List<String> tags);

    /**
     * Retrieves all unique tags from roles in the current tenant.
     * 
     * @return list of unique tags
     */
    List<String> getAllUniqueTags();

    /**
     * Deletes a role by ID or submits it for approval if required.
     * 
     * @param id the role ID to delete
     * @param workflowId the workflow ID to handle the deletion (optional)
     * @return true if role was deleted immediately, false if submitted for approval
     */
    /**
     * Delete a role by ID.
     * @param id the role ID
     * @param executionId the workflow execution ID if called from within a workflow (null for user-initiated)
     * @return true if role was deleted, false if submitted for approval
     */
    boolean deleteRole(UUID id, UUID executionId);

    /**
     * Deletes multiple roles in bulk for the specified tenant.
     * 
     * @param roleIds list of role IDs to delete
     * @param tenantId the tenant ID
     * @return map containing deletion results with success/failed counts and details
     */
    Map<String, Object> bulkDeleteRoles(List<UUID> roleIds, String tenantId);

    /**
     * Copies an existing role with all its owners.
     * 
     * <p>Creates a duplicate of the specified role with a new ID and copies all role owners.
     * The copied role will have the same properties but new generated IDs.</p>
     * 
     * @param roleId the ID of the role to copy
     * @return the response containing the newly created role
     */
    RoleResponse copyRole(UUID roleId);

    /**
     * Assigns a permission to a role with optional conditional rules.
     * 
     * <p>This method creates a new Permission for the specified Action,
     * associates it with the Role via RolePermission, and optionally
     * creates a ConditionalRule if conditionScript is provided.</p>
     * 
     * @param roleId the role ID to assign permission to
     * @param request the permission assignment request containing actionId, justification, and optional conditionScript
     * @return a map containing the created entities' IDs
     */
    Map<String, UUID> assignPermissionToRole(UUID roleId, AssignPermissionToRoleRequest request);

    /**
     * Submits a permission assignment request for approval workflow.
     * 
     * <p>Instead of immediately assigning the permission, this method publishes
     * an event to Kafka that triggers the specified workflow. The workflow will
     * send approval notifications to role owners, and upon approval, execute
     * an HTTP callback to perform the actual assignment.</p>
     * 
     * @param roleId the role ID to assign permission to
     * @param request the submit-for-approval request containing actionId, justification, conditionScript, and workflowId
     * @return a map containing submission details including eventId and workflow information
     * @throws IllegalArgumentException if role has no owners or workflow ID is invalid
     */
    Map<String, Object> submitPermissionAssignmentForApproval(UUID roleId, SubmitPermissionAssignmentForApprovalRequest request);

    /**
     * Submits a permission revocation request for approval workflow.
     * 
     * <p>Instead of immediately revoking the permission, this method publishes
     * an event to Kafka that triggers the specified workflow. The workflow will
     * send approval notifications to role owners, and upon approval, execute
     * an HTTP callback to perform the actual revocation.</p>
     * 
     * @param roleId the role ID to revoke permission from
     * @param request the submit-for-approval request containing actionId, justification, and workflowId
     * @return a map containing submission details including eventId and workflow information
     * @throws IllegalArgumentException if role has no owners or workflow ID is invalid
     */
    Map<String, Object> submitPermissionRevocationForApproval(UUID roleId, SubmitPermissionRevocationForApprovalRequest request);

    /**
     * Assigns multiple permissions to a role in a single atomic transaction.
     * 
     * <p>This method performs bulk permission assignments with the following guarantees:</p>
     * <ul>
     *   <li><strong>Atomicity:</strong> All assignments succeed or none do (rollback on any failure)</li>
     *   <li><strong>Validation:</strong> All actions are validated before any assignments</li>
     *   <li><strong>Consistency:</strong> Duplicate assignments are detected and prevented</li>
     *   <li><strong>Performance:</strong> Optimized batch processing with cache eviction</li>
     * </ul>
     * 
     * <p>Use cases:</p>
     * <ul>
     *   <li>Rapid role configuration (assign 10+ permissions at once)</li>
     *   <li>Workflow-driven approvals (execute after approval flow)</li>
     *   <li>Role templates (apply standard permission sets)</li>
     * </ul>
     * 
     * <p>Transaction behavior:</p>
     * If any assignment fails (invalid action, duplicate, inactive role, etc.),
     * the entire operation is rolled back and no changes are persisted.
     * 
     * @param roleId the role ID to assign permissions to
     * @param request the bulk assignment request containing list of assignments (1-100 items)
     * @return result object containing success status, permission IDs, and any errors
     * @throws IllegalArgumentException if role is inactive or request is invalid
     * @throws RuntimeException if transaction fails or database error occurs
     */
    BulkAssignmentResult assignPermissionsBulk(UUID roleId, BulkAssignPermissionRequest request);

    /**
     * Submits bulk permission assignments for approval workflow.
     * 
     * <p>This method does NOT assign permissions immediately. Instead, it:</p>
     * <ul>
     *   <li>Validates the role and actions exist</li>
     *   <li>Verifies role has owners for approval</li>
     *   <li>Publishes a permission.bulk.assigned event to Kafka</li>
     *   <li>Returns PENDING_APPROVAL status</li>
     * </ul>
     * 
     * <p>The actual assignments occur after owner approval via HTTP callback.</p>
     * 
     * @param roleId the role ID to assign permissions to
     * @param request the bulk assignment request with workflow ID
     * @return result containing pending status and workflow information
     * @throws IllegalArgumentException if role has no owners or is inactive
     * @throws RuntimeException if event publishing fails
     */
    Map<String, Object> submitBulkPermissionAssignmentForApproval(UUID roleId, SubmitBulkPermissionAssignmentForApprovalRequest request);

    /**
     * Revokes multiple permissions from a role in a single atomic operation.
     * 
     * <p>This method removes multiple permissions efficiently by:</p>
     * <ul>
     *   <li>Phase 1: Validating all action IDs exist</li>
     *   <li>Phase 2: Checking all permissions exist for the role</li>
     *   <li>Phase 3: Deleting all permissions atomically</li>
     * </ul>
     * 
     * <p>Benefits of bulk revocation:</p>
     * <ul>
     *   <li>Atomic transaction - all or nothing</li>
     *   <li>Single cache eviction instead of N evictions</li>
     *   <li>Reduced network overhead (single API call)</li>
     *   <li>Better audit trail (single bulk event)</li>
     * </ul>
     * 
     * <p>Transaction behavior:</p>
     * If any revocation fails (action not found, permission doesn't exist, etc.),
     * the entire operation is rolled back and no changes are persisted.
     * 
     * @param roleId the role ID to revoke permissions from
     * @param request the bulk revocation request containing list of action IDs (1-100 items)
     * @return result object containing success status, permission IDs, and any errors
     * @throws IllegalArgumentException if role is inactive or request is invalid
     * @throws RuntimeException if transaction fails or database error occurs
     */
    BulkRevocationResult revokePermissionsBulk(UUID roleId, BulkRevokePermissionRequest request);

    /**
     * Submits bulk permission revocations for approval workflow.
     * 
     * <p>This method does NOT revoke permissions immediately. Instead, it:</p>
     * <ul>
     *   <li>Validates the role and actions exist</li>
     *   <li>Verifies role has owners for approval</li>
     *   <li>Publishes a permission.bulk.revoked event to Kafka</li>
     *   <li>Returns PENDING_APPROVAL status</li>
     * </ul>
     * 
     * <p>The actual revocations occur after owner approval via HTTP callback.</p>
     * 
     * @param roleId the role ID to revoke permissions from
     * @param request the bulk revocation request with workflow ID
     * @return result containing pending status and workflow information
     * @throws IllegalArgumentException if role has no owners or is inactive
     * @throws RuntimeException if event publishing fails
     */
    Map<String, Object> submitBulkPermissionRevocationForApproval(UUID roleId, SubmitBulkPermissionRevocationForApprovalRequest request);

    /**
     * Deletes a permission from a role.
     * 
     * <p>This method removes the Permission, RolePermission association, and ConditionalRule
     * for the specified role and action combination.</p>
     * 
     * @param roleId the role ID to remove permission from
     * @param actionId the action ID to remove permission for
     * @return a map containing the IDs of deleted entities
     */
    Map<String, UUID> deletePermissionFromRole(UUID roleId, UUID actionId);

    /**
     * Updates a permission for a role.
     * 
     * <p>This method updates the Permission's justification and ConditionalRule
     * for the specified role and action combination.</p>
     * 
     * @param roleId the role ID to update permission for
     * @param actionId the action ID to update permission for
     * @param request the permission update request containing new justification and condition script
     * @return a map containing the IDs of updated entities
     */
    Map<String, UUID> updatePermissionForRole(UUID roleId, UUID actionId, AssignPermissionToRoleRequest request);

    /**
     * Updates a permission for a role using UpdatePermissionRequest.
     * 
     * <p>This method updates the Permission's justification and ConditionalRule
     * for the specified role and action combination.</p>
     * 
     * @param roleId the role ID to update permission for
     * @param actionId the action ID to update permission for
     * @param request the permission update request containing new justification and condition script
     * @return a map containing the IDs of updated entities
     */
    Map<String, UUID> updatePermissionForRole(UUID roleId, UUID actionId, UpdatePermissionRequest request);

    /**
     * Retrieves permission details for a specific role and action.
     * 
     * <p>This method fetches the Permission, ConditionalRule, and associated metadata
     * for the specified role and action combination.</p>
     * 
     * @param roleId the role ID to get permission details for
     * @param actionId the action ID to get permission details for
     * @return permission details including justification and conditional script
     */
    PermissionDetailsResponse getPermissionDetails(UUID roleId, UUID actionId);

    /**
     * Retrieves all users in the tenant with their assignment status for a specific role.
     * 
     * <p>This method fetches all users from the auth-service via gRPC and determines
     * whether each user has the specified role. Users with the role are sorted first,
     * followed by users without the role.</p>
     * 
     * @param roleId the role ID to check assignments for
     * @param page page number (0-indexed)
     * @param size page size
     * @param statusFilter filter by user status (ALL, ACTIVE, INACTIVE)
     * @param search search query to filter by username, displayName, or email
     * @return response containing all users with hasRole status
     */
    GetUsersByRoleResponse getUsersByRole(UUID roleId, Integer page, Integer size, String statusFilter, String search);

    /**
     * Retrieves only manually assigned users for a specific role.
     * 
     * <p>This method is similar to getUsersByRole but filters to only include users
     * who have been manually assigned the role (assignmentType = MANUAL). Dynamic rule-based
     * assignments are excluded. Supports pagination, status filtering, and search functionality.</p>
     * 
     * @param roleId the role ID to check assignments for
     * @param page page number (0-indexed)
     * @param size page size
     * @param statusFilter filter by user status (ALL, ACTIVE, INACTIVE)
     * @param search search query to filter by username, displayName, or email
     * @return response containing manually assigned users only
     */
    GetUsersByRoleResponse getManuallyAssignedUsersByRole(UUID roleId, Integer page, Integer size, String statusFilter, String search);

    /**
     * Retrieves role members with their assignment type information.
     * 
     * <p>This method returns all users assigned to a specific role along with their
     * assignment type (MANUAL or DYNAMIC). Supports pagination, filtering by status,
     * and searching by username, displayName, or email.</p>
     * 
     * @param roleId the role ID to retrieve members for
     * @param page page number (0-indexed)
     * @param size page size
     * @param statusFilter filter by user status (ALL, ACTIVE, INACTIVE)
     * @param search search query to filter by username, displayName, or email
     * @return response containing role members with assignment type information
     */
    GetRoleMembersResponse getRoleMembers(UUID roleId, Integer page, Integer size, String statusFilter, String search);

    /**
     * Retrieves all users in the tenant with their ownership status for a specific role.
     * 
     * <p>This method fetches all users from the auth-service via gRPC and determines
     * whether each user is an owner of the specified role. Users who are owners are sorted first,
     * followed by users who are not owners.</p>
     * 
     * @param roleId the role ID to check ownership for
     * @return response containing all users with isOwner status
     */
    GetRoleOwnersResponse getRoleOwners(UUID roleId);

    /**
     * Retrieves role statistics for the current tenant.
     * 
     * <p>Returns aggregated statistics including total role count and count of administrator roles.</p>
     * 
     * @return role statistics DTO
     */
    RoleStatsDTO getRoleStats();

    /**
     * Retrieves all roles owned by the current user.
     * 
     * <p>Returns roles where the current user is designated as an owner/manager.
     * Includes role metadata, associated permissions, user counts, and other relevant details
     * for the responsibilities dashboard.</p>
     * 
     * @param userId the ID of the current user
     * @return response containing list of owned roles with permissions
     */
    com.aidentx.applicationpermissions.roles_management.model.dto.OwnedRolesResponse getOwnedRolesByUser(UUID userId);
}