package com.aidentx.iam.access_roles_management.service;

import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AccessRoleRelationshipResponse;
import com.aidentx.iam.access_roles_management.model.dto.response.AddChildRolesResultDto;
import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipDirection;
import com.aidentx.iam.access_roles_management.model.enums.RoleRelationshipMode;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing parent-child composition links between roles.
 */
public interface AccessRoleCompositionService {

      /**
     * Adds child roles to a parent role with partial success support.
     * <p>
     * Each child role is validated individually. Valid children are linked,
     * invalid ones (circular dependency, self-reference) are collected as failures.
     * The operation does not abort on individual failures.
     * </p>
     *
     * @param tenantId     the tenant identifier
     * @param parentRoleId the parent role ID
     * @param childRoleIds the child role IDs to add
     * @return result containing added, skipped, and failed child role IDs
     * @throws EntityNotFoundException if the parent role does not exist
     */
    AddChildRolesResultDto addChildRoles(String tenantId, UUID parentRoleId, List<UUID> childRoleIds);

    /**
     * Removes child roles from a parent role.
     *
     * @param tenantId     the tenant identifier
     * @param parentRoleId the parent role ID
     * @param childRoleIds the child role IDs to remove
     */
    void removeChildRoles(String tenantId, UUID parentRoleId, List<UUID> childRoleIds);

    /**
     * Retrieves all child roles of a given parent role.
     *
     * @param parentRoleId the parent role ID
     * @return list of child role responses
     */
    List<AccessRoleResponse> getChildRoles(UUID parentRoleId);

    /**
     * Retrieves all parent roles of a given child role.
     *
     * @param childRoleId the child role ID
     * @return list of parent role responses
     */
    List<AccessRoleResponse> getParentRoles(UUID childRoleId);

    /**
     * Retrieves a unified relationship view for a role.
     *
     * @param tenantId tenant scope
     * @param roleId root role id
     * @param mode direct, hierarchy, or conflict view
     * @param direction child, parent, or both traversal direction
     * @return unified relationship response
     */
    AccessRoleRelationshipResponse getUnifiedRelationships(
            String tenantId,
            UUID roleId,
            RoleRelationshipMode mode,
            RoleRelationshipDirection direction);

    /**
     * Retrieves all child role IDs for a given parent role.
     *
     * @param parentRoleId the parent role ID
     * @return list of child role UUIDs
     */
    List<UUID> getChildRoleIds(UUID parentRoleId);
}
