package com.aidentx.applicationpermissions.roles_management.repository;

import com.aidentx.applicationpermissions.roles_management.model.entity.RoleOwners;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for RoleOwners entity operations.
 * 
 * <p>Provides data access methods for managing role ownership relationships,
 * including finding owners by role, finding roles by owner, and managing
 * the owner-role associations.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Repository
public interface RoleOwnersRepository extends JpaRepository<RoleOwners, UUID> {

    /**
     * Find all role owners for a specific role.
     * 
     * @param roleId the ID of the role
     * @return list of RoleOwners for the specified role
     */
    List<RoleOwners> findByRoleId(UUID roleId);

    /**
     * Find all roles owned by a specific user.
     * 
     * @param userId the ID of the user
     * @return list of RoleOwners for the specified user
     */
    List<RoleOwners> findByUserId(UUID userId);

    /**
     * Find a specific role ownership relationship.
     * 
     * @param roleId the ID of the role
     * @param userId the ID of the user
     * @return Optional RoleOwners if the relationship exists
     */
    Optional<RoleOwners> findByRoleIdAndUserId(UUID roleId, UUID userId);

    /**
     * Check if a user is an owner of a specific role.
     * 
     * @param roleId the ID of the role
     * @param userId the ID of the user
     * @return true if the user is an owner of the role, false otherwise
     */
    boolean existsByRoleIdAndUserId(UUID roleId, UUID userId);

    /**
     * Get all user IDs who are owners of a specific role.
     * 
     * @param roleId the ID of the role
     * @return list of user IDs who are owners of the role
     */
    @Query("SELECT ro.userId FROM RoleOwners ro WHERE ro.roleId = :roleId")
    List<UUID> findUserIdsByRoleId(@Param("roleId") UUID roleId);

    /**
     * Get all role IDs owned by a specific user.
     * 
     * @param userId the ID of the user
     * @return list of role IDs owned by the user
     */
    @Query("SELECT ro.roleId FROM RoleOwners ro WHERE ro.userId = :userId")
    List<UUID> findRoleIdsByUserId(@Param("userId") UUID userId);

    /**
     * Count the number of owners for a specific role.
     * 
     * @param roleId the ID of the role
     * @return number of owners for the role
     */
    long countByRoleId(UUID roleId);

    /**
     * Count the number of roles owned by a specific user.
     * 
     * @param userId the ID of the user
     * @return number of roles owned by the user
     */
    long countByUserId(UUID userId);

    /**
     * Delete all role ownership relationships for a specific role.
     * 
     * @param roleId the ID of the role
     */
    void deleteByRoleId(UUID roleId);

    /**
     * Delete all role ownership relationships for a specific user.
     * 
     * @param userId the ID of the user
     */
    void deleteByUserId(UUID userId);

    /**
     * Delete a specific role ownership relationship.
     * 
     * @param roleId the ID of the role
     * @param userId the ID of the user
     */
    void deleteByRoleIdAndUserId(UUID roleId, UUID userId);
}