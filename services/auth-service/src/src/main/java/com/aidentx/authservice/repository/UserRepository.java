package com.aidentx.authservice.repository;

import com.aidentx.authservice.enums.Role;
import com.aidentx.authservice.enums.UserStatus;
import com.aidentx.authservice.model.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link User} entities.
 * Extends {@link JpaRepository} to provide CRUD operations and query method
 * support.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findByEmail(String email);

    /**
     * Finds a user by their username.
     *
     * @param username the username to search for
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by their Identity service ID.
     * Used for synchronizing with identity lifecycle events.
     *
     * @param identityId the identity ID from Identity service
     * @return Optional containing the user if found
     */
    Optional<User> findByIdentityId(UUID identityId);

    /**
     * Finds a user by their unique ID.
     *
     * @param id the UUID of the user
     * @return an {@link Optional} containing the user if found, or empty if not
     */
    Optional<User> findById(UUID id);

    /**
     * Finds all users by their tenant ID.
     *
     * @param tenantId the tenant ID to search for
     * @return a list of users belonging to the specified tenant
     */
    List<User> findByTenantId(String tenantId);

    /**
     * Finds all users by tenant ID with pagination.
     *
     * @param tenantId the tenant ID to search for
     * @param pageable pagination parameters
     * @return a page of users belonging to the specified tenant
     */
    org.springframework.data.domain.Page<User> findByTenantId(String tenantId, Pageable pageable);

    /**
     * Search users by tenant ID and search term (searches username and email).
     *
     * @param tenantId   the tenant ID
     * @param searchTerm search term to match against username or email
     *                   (case-insensitive)
     * @param pageable   pagination parameters
     * @return a page of matching users
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.tenantId = :tenantId
            AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                 OR LOWER(u.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))
            """)
    org.springframework.data.domain.Page<User> searchByTenantIdAndTerm(
            @Param("tenantId") String tenantId,
            @Param("searchTerm") String searchTerm,
            Pageable pageable);

    /**
     * Batch fetch users by list of IDs and tenant ID.
     *
     * @param ids      list of user UUIDs
     * @param tenantId the tenant ID
     * @return list of matching users
     */
    List<User> findByIdInAndTenantId(List<UUID> ids, String tenantId);

    /**
     * Batch fetch users by Identity-service identity IDs and tenant ID.
     *
     * @param identityIds list of identity UUIDs from Identity service
     * @param tenantId    the tenant ID
     * @return list of matching users
     */
    List<User> findByIdentityIdInAndTenantId(List<UUID> identityIds, String tenantId);

    /**
     * Finds users whose passwords are about to expire and need notification.
     * Uses password policy's passwordExpirationNotificationDays to determine
     * notification threshold.
     * 
     * @param currentTime the current timestamp in milliseconds
     * @param pageable    pagination parameters
     * @return list of users requiring password expiration notifications
     */
    @Query("""
            SELECT u FROM User u
            JOIN PasswordPolicy pp ON u.tenantId = pp.tenantId
            WHERE u.email IS NOT NULL
            AND pp.passwordExpirationNotificationDays IS NOT NULL
            AND pp.passwordExpirationNotificationDays > 0
            AND pp.policyExpiresAt IS NOT NULL
            AND (pp.policyExpiresAt - :currentTime) <= (pp.passwordExpirationNotificationDays * 86400000)
            AND pp.policyExpiresAt > :currentTime
            ORDER BY u.tenantId ASC
            """)
    List<User> findUsersRequiringPasswordExpirationNotification(@Param("currentTime") long currentTime,
            Pageable pageable);

    /**
     * Counts users whose passwords are about to expire and need notification.
     * 
     * @param currentTime the current timestamp in milliseconds
     * @return count of users requiring password expiration notifications
     */
    @Query("""
            SELECT COUNT(u) FROM User u
            JOIN PasswordPolicy pp ON u.tenantId = pp.tenantId
            WHERE u.email IS NOT NULL
            AND pp.passwordExpirationNotificationDays IS NOT NULL
            AND pp.passwordExpirationNotificationDays > 0
            AND pp.policyExpiresAt IS NOT NULL
            AND (pp.policyExpiresAt - :currentTime) <= (pp.passwordExpirationNotificationDays * 86400000)
            AND pp.policyExpiresAt > :currentTime
            """)
    long countUsersRequiringPasswordExpirationNotification(@Param("currentTime") long currentTime);

    /**
     * Finds users by tenant ID whose passwords are about to expire and need
     * notification.
     * 
     * @param tenantId    the tenant ID to filter by
     * @param currentTime the current timestamp in milliseconds
     * @return list of users requiring password expiration notifications for
     *         specific tenant
     */
    @Query("""
            SELECT u FROM User u
            JOIN PasswordPolicy pp ON u.tenantId = pp.tenantId
            WHERE u.tenantId = :tenantId
            AND u.email IS NOT NULL
            AND pp.passwordExpirationNotificationDays IS NOT NULL
            AND pp.passwordExpirationNotificationDays > 0
            AND pp.policyExpiresAt IS NOT NULL
            AND (pp.policyExpiresAt - :currentTime) <= (pp.passwordExpirationNotificationDays * 86400000)
            AND pp.policyExpiresAt > :currentTime
            ORDER BY u.email ASC
            """)
    List<User> findUsersRequiringPasswordExpirationNotificationByTenant(@Param("tenantId") String tenantId,
            @Param("currentTime") long currentTime);

    /**
     * Finds user IDs by role names
     * Used for role-based approver resolution in workflow multi-approval nodes
     * Phase 1 (Week 1-2) enhancement
     *
     * @param roles List of roles to search for
     * @return List of user UUIDs who have any of the specified roles
     */
    @Query("""
            SELECT DISTINCT u.id FROM User u
            WHERE u.id IN (
                SELECT ur.user.id FROM UserRole ur
                WHERE ur.role IN :roles
            )
            AND u.status = :status
            ORDER BY u.email ASC
            """)
    List<UUID> findUserIdsByRoles(@Param("roles") List<Role> roles, @Param("status") UserStatus status);



    @Query("SELECT u.id, u.identityId FROM User u WHERE u.id IN :ids AND u.tenantId = :tenantId")
    List<Object[]> findIdentityIdsByIdsAndTenantId(
            @Param("ids") List<UUID> ids,
            @Param("tenantId") String tenantId);
}
