package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.model.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for RolePermission entity operations.
 * 
 * <p>Provides standard CRUD operations and custom queries for managing
 * role-permission associations in the RBAC system.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

    /**
     * Find all permissions assigned to a specific role.
     * 
     * @param roleId the ID of the role
     * @return list of role-permission associations for the role
     */
    List<RolePermission> findByRoleId(UUID roleId);

    /**
     * Find all roles that have a specific permission.
     * 
     * @param permissionId the ID of the permission
     * @return list of role-permission associations for the permission
     */
    List<RolePermission> findByPermissionId(UUID permissionId);

    /**
     * Find a specific role-permission association.
     * 
     * @param roleId the ID of the role
     * @param permissionId the ID of the permission
     * @return Optional containing the association if found
     */
    Optional<RolePermission> findByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    /**
     * Check if a role has a specific permission.
     * 
     * @param roleId the ID of the role
     * @param permissionId the ID of the permission
     * @return true if association exists, false otherwise
     */
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    /**
     * Count the number of permissions assigned to a role.
     * 
     * @param roleId the ID of the role
     * @return count of permissions
     */
    Long countByRoleId(UUID roleId);

    /**
     * Count the number of roles that have a permission.
     * 
     * @param permissionId the ID of the permission
     * @return count of roles
     */
    Long countByPermissionId(UUID permissionId);

    /**
     * Delete all permissions for a specific role.
     * 
     * @param roleId the ID of the role
     */
    void deleteByRoleId(UUID roleId);

    /**
     * Delete all role assignments for a specific permission.
     * 
     * @param permissionId the ID of the permission
     */
    void deleteByPermissionId(UUID permissionId);
}