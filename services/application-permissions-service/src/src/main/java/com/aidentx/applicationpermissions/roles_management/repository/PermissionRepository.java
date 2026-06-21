package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.model.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Permission entity operations.
 * 
 * <p>Provides standard CRUD operations and custom queries for managing
 * permissions in the role-based access control system.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    /**
     * Find permissions by action ID.
     * 
     * @param actionId the action ID to find permissions for
     * @return list of permissions for the action
     */
    List<Permission> findByActionId(UUID actionId);

    /**
     * Count the total number of roles that have this permission.
     * 
     * @param permissionId the ID of the permission
     * @return count of roles with this permission
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp WHERE rp.permissionId = :permissionId")
    Long countRolesByPermissionId(@Param("permissionId") UUID permissionId);

    /**
     * Count the total number of ACTIVE roles that have this permission.
     * Only counts roles where isActive = true.
     * 
     * @param permissionId the ID of the permission
     * @return count of active roles with this permission
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp JOIN Role r ON rp.roleId = r.id WHERE rp.permissionId = :permissionId AND r.isActive = true")
    Long countActiveRolesByPermissionId(@Param("permissionId") UUID permissionId);

    /**
     * Find permissions assigned to a specific role.
     * 
     * @param roleId the ID of the role
     * @return list of permissions assigned to the role
     */
    @Query("SELECT p FROM Permission p JOIN RolePermission rp ON p.id = rp.permissionId WHERE rp.roleId = :roleId")
    List<Permission> findByRoleId(@Param("roleId") UUID roleId);

    /**
     * Find permissions for a specific user (through their roles).
     * 
     * @param userId the ID of the user
     * @return list of permissions available to the user
     */
    @Query("SELECT DISTINCT p FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permissionId " +
           "JOIN UserRole ur ON rp.roleId = ur.roleId " +
           "WHERE ur.userId = :userId")
    List<Permission> findByUserId(@Param("userId") UUID userId);

    /**
     * Check if a role already has a permission for a specific action.
     * 
     * @param roleId the ID of the role
     * @param actionId the ID of the action
     * @return true if role already has permission for this action, false otherwise
     */
    @Query("SELECT COUNT(p) > 0 FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permissionId " +
           "WHERE rp.roleId = :roleId AND p.actionId = :actionId")
    boolean existsByRoleIdAndActionId(@Param("roleId") UUID roleId, @Param("actionId") UUID actionId);

    /**
     * Check if a role already has a permission for a specific action with the same conditional rule.
     * 
     * @param roleId the ID of the role
     * @param actionId the ID of the action
     * @param conditionScript the condition script (can be null)
     * @param justification the justification text
     * @return true if role already has identical permission assignment, false otherwise
     */
    @Query("SELECT COUNT(p) > 0 FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permissionId " +
           "JOIN ConditionalRule cr ON p.id = cr.permissionId " +
           "WHERE rp.roleId = :roleId AND p.actionId = :actionId " +
           "AND ((:conditionScript IS NULL AND cr.conditionScript IS NULL) OR cr.conditionScript = :conditionScript) " +
           "AND cr.justification = :justification")
    boolean existsByRoleIdAndActionIdAndConditionalRule(
        @Param("roleId") UUID roleId, 
        @Param("actionId") UUID actionId,
        @Param("conditionScript") String conditionScript,
        @Param("justification") String justification
    );

    /**
     * Find existing permission for a role and action combination.
     * 
     * @param roleId the ID of the role
     * @param actionId the ID of the action
     * @return Optional containing the permission if found
     */
    @Query("SELECT p FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permissionId " +
           "WHERE rp.roleId = :roleId AND p.actionId = :actionId")
    Optional<Permission> findByRoleIdAndActionId(@Param("roleId") UUID roleId, @Param("actionId") UUID actionId);

    /**
     * Find existing permissions for a role and multiple action IDs.
     * Used for bulk operations to detect duplicates.
     * 
     * @param roleId the ID of the role
     * @param actionIds list of action IDs to check
     * @return List of existing permissions for the role-actions combination
     */
    @Query("SELECT p FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permissionId " +
           "WHERE rp.roleId = :roleId AND p.actionId IN :actionIds")
    List<Permission> findByRoleIdAndActionIdIn(@Param("roleId") UUID roleId, @Param("actionIds") List<UUID> actionIds);

    /**
     * Find all permissions for a role and action combination.
     * 
     * @param roleId the ID of the role
     * @param actionId the ID of the action
     * @return List of permissions for the role-action combination
     */
    @Query("SELECT p FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permissionId " +
           "WHERE rp.roleId = :roleId AND p.actionId = :actionId")
    List<Permission> findAllByRoleIdAndActionId(@Param("roleId") UUID roleId, @Param("actionId") UUID actionId);

    /**
     * Find existing permission for a role, action, and conditional rule combination.
     * 
     * @param roleId the ID of the role
     * @param actionId the ID of the action
     * @param conditionScript the condition script (can be null)
     * @param justification the justification text
     * @return Optional containing the permission if found
     */
    @Query("SELECT p FROM Permission p " +
           "JOIN RolePermission rp ON p.id = rp.permissionId " +
           "JOIN ConditionalRule cr ON p.id = cr.permissionId " +
           "WHERE rp.roleId = :roleId AND p.actionId = :actionId " +
           "AND ((:conditionScript IS NULL AND cr.conditionScript IS NULL) OR cr.conditionScript = :conditionScript) " +
           "AND cr.justification = :justification")
    Optional<Permission> findByRoleIdAndActionIdAndConditionalRule(
        @Param("roleId") UUID roleId, 
        @Param("actionId") UUID actionId,
        @Param("conditionScript") String conditionScript,
        @Param("justification") String justification
    );
}