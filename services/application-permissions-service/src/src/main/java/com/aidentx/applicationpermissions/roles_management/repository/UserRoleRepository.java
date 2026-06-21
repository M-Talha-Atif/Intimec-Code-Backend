package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.enums.AssignmentType;
import com.aidentx.applicationpermissions.roles_management.model.entity.UserRole;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for UserRole entity operations.
 * 
 * <p>Provides standard CRUD operations and custom queries for managing
 * user-role associations in the RBAC system.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    /**
     * Find all roles assigned to a specific user.
     * 
     * @param userId the ID of the user
     * @return list of user-role associations for the user
     */
    List<UserRole> findByUserId(UUID userId);

    /**
     * Find all users assigned to a specific role.
     * 
     * @param roleId the ID of the role
     * @return list of user-role associations for the role
     */
    List<UserRole> findByRoleId(UUID roleId);

    /**
     * Find all user-role associations within a specific tenant.
     * 
     * @param tenantId the tenant identifier
     * @return list of user-role associations for the tenant
     */
    List<UserRole> findByTenantId(String tenantId);

    /**
     * Find a specific user-role association.
     * 
     * @param userId the ID of the user
     * @param roleId the ID of the role
     * @return Optional containing the association if found
     */
    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    /**
     * Find user-role associations for a user within a specific tenant.
     * 
     * @param userId the ID of the user
     * @param tenantId the tenant identifier
     * @return list of user-role associations
     */
    List<UserRole> findByUserIdAndTenantId(UUID userId, String tenantId);

    /**
     * Find user-role associations for a role within a specific tenant.
     * CRITICAL: Bypasses ALL caching to ensure fresh data during reconciliation.
     * Uses native query to avoid Hibernate caching entirely.
     * 
     * @param roleId the ID of the role
     * @param tenantId the tenant identifier
     * @return list of user-role associations
     */
    @Query(value = "SELECT * FROM user_role WHERE role_id = :roleId AND tenant_id = :tenantId", nativeQuery = true)
    @QueryHints({
        @QueryHint(name = "org.hibernate.cacheable", value = "false"),
        @QueryHint(name = "org.hibernate.cacheMode", value = "IGNORE")
    })
    List<UserRole> findByRoleIdAndTenantId(@Param("roleId") UUID roleId, @Param("tenantId") String tenantId);

    /**
     * Find user IDs that have a specific role within a tenant.
     * 
     * @param roleId the ID of the role
     * @param tenantId the tenant identifier
     * @return list of user IDs that have the role
     */
    @Query("SELECT ur.userId FROM UserRole ur WHERE ur.roleId = :roleId AND ur.tenantId = :tenantId")
    List<UUID> findUserIdsByRoleIdAndTenantId(@Param("roleId") UUID roleId, @Param("tenantId") String tenantId);

    /**
     * Find user IDs that have a specific role within a tenant filtered by assignment type.
     * 
     * @param roleId the ID of the role
     * @param tenantId the tenant identifier
     * @param assignmentType the assignment type (MANUAL or DYNAMIC)
     * @return list of user IDs that have the role with the specified assignment type
     */
    @Query("SELECT ur.userId FROM UserRole ur WHERE ur.roleId = :roleId AND ur.tenantId = :tenantId AND ur.assignmentType = :assignmentType")
    List<UUID> findUserIdsByRoleIdAndTenantIdAndAssignmentType(
        @Param("roleId") UUID roleId, 
        @Param("tenantId") String tenantId, 
        @Param("assignmentType") AssignmentType assignmentType);

    /**
     * Check if a user is assigned to a specific role.
     * 
     * @param userId the ID of the user
     * @param roleId the ID of the role
     * @return true if association exists, false otherwise
     */
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);

    /**
     * Count the number of roles assigned to a user.
     * 
     * @param userId the ID of the user
     * @return count of roles
     */
    Long countByUserId(UUID userId);

    /**
     * Count the number of users assigned to a role.
     * 
     * @param roleId the ID of the role
     * @return count of users
     */
    Long countByRoleId(UUID roleId);

    /**
     * Count the number of users assigned to a role within a tenant.
     * 
     * @param roleId the ID of the role
     * @param tenantId the tenant identifier
     * @return count of users assigned to the role in the tenant
     */
    Long countByRoleIdAndTenantId(UUID roleId, String tenantId);

    /**
     * Delete all role assignments for a specific user.
     * 
     * @param userId the ID of the user
     */
    void deleteByUserId(UUID userId);

    /**
     * Delete all user assignments for a specific role.
     * 
     * @param roleId the ID of the role
     */
    void deleteByRoleId(UUID roleId);

    /**
     * Delete all associations within a specific tenant.
     * 
     * @param tenantId the tenant identifier
     */
    void deleteByTenantId(String tenantId);

    /**
     * Find a specific user-role association within a tenant.
     * 
     * @param userId the user identifier
     * @param roleId the role identifier  
     * @param tenantId the tenant identifier
     * @return optional user-role assignment if exists
     */
    Optional<UserRole> findByUserIdAndRoleIdAndTenantId(UUID userId, UUID roleId, String tenantId);

    /**
     * Check if a user has a specific role within a tenant.
     * 
     * @param userId the user identifier
     * @param roleId the role identifier
     * @param tenantId the tenant identifier
     * @return true if the user has the role, false otherwise
     */
    boolean existsByUserIdAndRoleIdAndTenantId(UUID userId, UUID roleId, String tenantId);

    /**
     * Delete a specific user-role assignment.
     * 
     * @param userId the user identifier
     * @param roleId the role identifier
     * @param tenantId the tenant identifier
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId AND ur.roleId = :roleId AND ur.tenantId = :tenantId")
    int deleteByUserIdAndRoleIdAndTenantId(@Param("userId") UUID userId, @Param("roleId") UUID roleId, @Param("tenantId") String tenantId);

    /**
     * Delete a specific user-role assignment ONLY if it has DYNAMIC assignment type.
     * This is critical for dynamic rule reconciliation to avoid removing MANUAL assignments.
     * 
     * @param userId the user identifier
     * @param roleId the role identifier  
     * @param tenantId the tenant identifier
     * @param assignmentType the assignment type to filter by (should be DYNAMIC)
     * @return number of deleted records (0 or 1)
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId AND ur.roleId = :roleId AND ur.tenantId = :tenantId AND ur.assignmentType = :assignmentType")
    int deleteByUserIdAndRoleIdAndTenantIdAndAssignmentType(
        @Param("userId") UUID userId, 
        @Param("roleId") UUID roleId, 
        @Param("tenantId") String tenantId,
        @Param("assignmentType") AssignmentType assignmentType
    );

    /**
     * Delete all role assignments for a specific user within a tenant.
     * 
     * @param userId the user identifier
     * @param tenantId the tenant identifier
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.userId = :userId AND ur.tenantId = :tenantId")
    int deleteByUserIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") String tenantId);

    /**
     * Delete all user assignments for a specific role within a tenant.
     * 
     * @param roleId the role identifier
     * @param tenantId the tenant identifier
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.roleId = :roleId AND ur.tenantId = :tenantId")
    int deleteByRoleIdAndTenantId(@Param("roleId") UUID roleId, @Param("tenantId") String tenantId);

    /**
     * Find all ACTIVE role IDs for a user within a specific tenant.
     * Only returns roles where isActive = true.
     * 
     * @param userId the ID of the user
     * @param tenantId the tenant identifier
     * @return list of active role IDs
     */
    @Query("SELECT ur.roleId FROM UserRole ur JOIN Role r ON ur.roleId = r.id WHERE ur.userId = :userId AND ur.tenantId = :tenantId AND r.isActive = true")
    List<UUID> findActiveRoleIdsByUserIdAndTenantId(@Param("userId") UUID userId, @Param("tenantId") String tenantId);

    /**
     * Check if a role has any assignments with a specific assignment type.
     * Used to dynamically calculate hasManualAssignments and hasDynamicRules flags.
     * 
     * @param roleId the ID of the role
     * @param assignmentType the type of assignment to check for
     * @return true if at least one assignment of the specified type exists, false otherwise
     */
    boolean existsByRoleIdAndAssignmentType(UUID roleId, AssignmentType assignmentType);

    /**
     * Check if a role has any assignments with a specific assignment type within a tenant.
     * Used to dynamically calculate hasManualAssignments and hasDynamicRules flags for tenant-specific queries.
     * 
     * @param roleId the ID of the role
     * @param tenantId the tenant identifier
     * @param assignmentType the type of assignment to check for
     * @return true if at least one assignment of the specified type exists, false otherwise
     */
    boolean existsByRoleIdAndTenantIdAndAssignmentType(UUID roleId, String tenantId, AssignmentType assignmentType);


    /**
     * Finds all user-role assignments for specific users and roles within a tenant.
     * Used for duplicate detection in bulk assignments.
     *
     * @param tenantId tenant identifier
     * @param userIds list of user UUIDs
     * @param roleIds list of role UUIDs
     * @return list of existing assignments
     */
    List<UserRole> findByTenantIdAndUserIdInAndRoleIdIn(
            String tenantId,
            List<UUID> userIds,
            List<UUID> roleIds
    );
}