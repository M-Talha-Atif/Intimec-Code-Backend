
package com.aidentx.iam.identity_management.repository;

import com.aidentx.iam.identity_management.model.entity.Employee;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    Optional<Employee> findByEmployeeId(String employeeId);

    /**
     * Finds an employee by tenant and employee ID.
     *
     * @param tenantId The tenant ID.
     * @param employeeId The employee ID to search for.
     * @return An Optional containing the Employee if found.
     */
    Optional<Employee> findByTenantIdAndEmployeeId(String tenantId, String employeeId);

    Optional<Employee> findByTenantIdAndEmail(String tenantId, String email);

    /**
     * Finds an employee by tenant and username.
     *
     * @param tenantId The tenant ID.
     * @param username The username to search for.
     * @return An Optional containing the Employee if found.
     */
    Optional<Employee> findByTenantIdAndUsername(String tenantId, String username);

    /**
     * Finds employees by tenant and username containing the search term (case-insensitive).
     *
     * @param tenantId The tenant ID.
     * @param username The username search term.
     * @return A list of employees matching the search criteria.
     */
    List<Employee> findByTenantIdAndUsernameContainingIgnoreCase(String tenantId, String username);

    /**
     * Batch fetches employees with their managers eagerly loaded.
     * Uses JOIN FETCH to avoid N+1 query problem when accessing manager.
     *
     * @param ids List of identity IDs to fetch
     * @return List of employees with manager data pre-loaded
     */
    @Query("SELECT DISTINCT e FROM Employee e " +
            "LEFT JOIN FETCH e.manager " +
            "WHERE e.id IN :ids")
    List<Employee> findAllByIdWithManager(@Param("ids") List<UUID> ids);

    /**
     * Gets user_id (from users table) for a given identity_id.
     * This resolves identity_id to the corresponding user account ID.
     *
     * @param identityId The identity ID to look up
     * @return The user ID if found, null otherwise
     */
    @Query(value = "SELECT u.id FROM users u WHERE u.identity_id = :identityId", nativeQuery = true)
    UUID findUserIdByIdentityId(@Param("identityId") UUID identityId);

    /**
     * Batch fetches user_ids for multiple identity_ids.
     * Returns pairs of [identity_id, user_id] for efficient mapping.
     *
     * @param identityIds List of identity IDs to look up
     * @return List of Object arrays containing [identity_id, user_id] pairs
     */
    @Query(value = "SELECT u.identity_id, u.id FROM users u WHERE u.identity_id IN :identityIds", nativeQuery = true)
    List<Object[]> findUserIdsByIdentityIds(@Param("identityIds") List<UUID> identityIds);

    /**
     * Finds all employees where the manager_id matches the given manager identity ID.
     * This is used to get direct reports for a manager.
     *
     * @param tenantId The tenant ID
     * @param managerId The manager's identity ID
     * @return List of employees managed by the given manager
     */
    List<Employee> findByTenantIdAndManager_Id(String tenantId, UUID managerId);

    /**
     * Finds all employees with an unresolved manager reference (managerRef is set
     * but the manager FK relationship is null). Used by the post-import manager
     * resolution pass to resolve raw connector references to actual Identity entities.
     *
     * @param tenantId The tenant ID
     * @return List of employees with pending manager resolution
     */
    List<Employee> findByTenantIdAndManagerRefIsNotNullAndManagerIsNull(String tenantId);

    /**
     * Paginated lookup of employees with a non-blank {@code department} value.
     * Used by the reconciliation org-assignment pass to re-resolve every
     * employee's primary FUNCTIONAL org unit against the latest org tree
     * (picks up newly-created units, retries previously-failed resolutions).
     */
    @Query("SELECT e FROM Employee e WHERE e.tenantId = :tenantId AND e.department IS NOT NULL AND e.department <> ''")
    Page<Employee> findByTenantIdWithDepartment(@Param("tenantId") String tenantId, Pageable pageable);
}