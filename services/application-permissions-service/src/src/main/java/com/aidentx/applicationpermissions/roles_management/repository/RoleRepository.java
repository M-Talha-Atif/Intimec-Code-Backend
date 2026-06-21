package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.model.entity.Role;
import com.aidentx.applicationpermissions.roles_management.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Role entity operations.
 * 
 * <p>Provides standard CRUD operations and custom queries for managing
 * roles in the role-based access control system.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    /**
     * Find a role by its name within a specific tenant.
     * 
     * @param roleName the name of the role
     * @param tenantId the tenant identifier
     * @return Optional containing the role if found
     */
    Optional<Role> findByRoleNameAndTenantId(String roleName, String tenantId);

    /**
     * Find all roles belonging to a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of roles for the tenant
     */
    List<Role> findByTenantId(String tenantId);

    /**
     * Find all roles belonging to a specific tenant with pagination.
     * 
     * @param tenantId the tenant identifier
     * @param pageable the pagination information
     * @return page of roles for the tenant
     */
    Page<Role> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Find all roles of a specific type within a tenant.
     * 
     * @param roleType the role type
     * @param tenantId the tenant identifier
     * @return list of roles with the specified type
     */
    List<Role> findByRoleTypeAndTenantId(RoleType roleType, String tenantId);
    
    /**
     * Find all roles of a specific type (across all tenants).
     * Used for bootstrap to assign permissions to all ADMINISTRATOR roles.
     * 
     * @param roleType the role type
     * @return list of roles with the specified type
     */
    List<Role> findByRoleType(RoleType roleType);

    /**
     * Find roles by name pattern within a tenant.
     * 
     * @param namePattern the pattern to search for in role names
     * @param tenantId the tenant identifier
     * @return list of matching roles
     */
    List<Role> findByRoleNameContainingIgnoreCaseAndTenantId(String namePattern, String tenantId);

    /**
     * Find all roles within a tenant ordered by role type and name.
     * 
     * @param tenantId the tenant identifier
     * @return list of roles ordered by type and name
     */
    List<Role> findByTenantIdOrderByRoleTypeAscRoleNameAsc(String tenantId);

    /**
     * Check if a role exists with the given name in a tenant.
     * 
     * @param roleName the name to check
     * @param tenantId the tenant identifier
     * @return true if role exists, false otherwise
     */
    boolean existsByRoleNameAndTenantId(String roleName, String tenantId);

    /**
     * Count the total number of users assigned to this role.
     * 
     * @param roleId the ID of the role
     * @return count of assigned users
     */
    @Query("SELECT COUNT(ur) FROM UserRole ur WHERE ur.roleId = :roleId")
    Long countUsersByRoleId(@Param("roleId") UUID roleId);

    /**
     * Count the total number of permissions assigned to this role.
     * 
     * @param roleId the ID of the role
     * @return count of assigned permissions
     */
    @Query("SELECT COUNT(rp) FROM RolePermission rp WHERE rp.roleId = :roleId")
    Long countPermissionsByRoleId(@Param("roleId") UUID roleId);

    /**
     * Find a role by its ID within a specific tenant.
     * 
     * @param id the role ID
     * @param tenantId the tenant identifier
     * @return Optional containing the role if found and belongs to tenant
     */
    Optional<Role> findByIdAndTenantId(UUID id, String tenantId);

    /**
     * Check if a role exists with the given ID in a tenant.
     * 
     * @param id the role ID to check
     * @param tenantId the tenant identifier
     * @return true if role exists and belongs to tenant, false otherwise
     */
    boolean existsByIdAndTenantId(UUID id, String tenantId);

    /**
     * Count total roles for a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return count of roles for the tenant
     */
    Long countByTenantId(String tenantId);

    /**
     * Count roles by role type for a specific tenant.
     * 
     * @param roleType the role type
     * @param tenantId the tenant identifier
     * @return count of roles with the specified type
     */
    Long countByRoleTypeAndTenantId(RoleType roleType, String tenantId);

    /**
     * Count inactive roles for a specific tenant.
     * 
     * @param isActive the active status (false for inactive)
     * @param tenantId the tenant identifier
     * @return count of inactive roles
     */
    Long countByIsActiveAndTenantId(boolean isActive, String tenantId);

    /**
     * Find all roles owned by a specific user within a tenant.
     * 
     * @param ownerId the user ID of the role owner
     * @param tenantId the tenant identifier
     * @return list of roles owned by the user
     */
    @Query("SELECT r FROM Role r WHERE r.id IN " +
           "(SELECT ro.roleId FROM RoleOwners ro WHERE ro.userId = :ownerId) " +
           "AND r.tenantId = :tenantId " +
           "ORDER BY r.roleType ASC, r.roleName ASC")
    List<Role> findByOwnerIdAndTenantId(@Param("ownerId") UUID ownerId, @Param("tenantId") String tenantId);
}