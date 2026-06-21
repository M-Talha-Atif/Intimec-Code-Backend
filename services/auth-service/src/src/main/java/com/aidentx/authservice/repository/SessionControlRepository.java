package com.aidentx.authservice.repository;

import com.aidentx.authservice.model.entity.SessionControl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link SessionControl} entities.
 * <p>
 * Extends {@link JpaRepository} to provide CRUD operations and query method support
 * for session control policy management. Each tenant can have only one session control policy.
 * </p>
 * 
 * @author Aidentx
 * @since 1.0
 */
@Repository
public interface SessionControlRepository extends JpaRepository<SessionControl, UUID> {

    /**
     * Finds a session control policy by tenant ID.
     * <p>
     * Since each tenant can have only one session control policy,
     * this method returns an Optional containing the policy if found.
     * </p>
     *
     * @param tenantId the tenant ID to search for
     * @return an {@link Optional} containing the session control policy if found, or empty if not
     */
    Optional<SessionControl> findByTenantId(String tenantId);

    /**
     * Checks if a session control policy exists for the given tenant ID.
     * <p>
     * This is useful for validation before creating new policies to ensure
     * tenant uniqueness constraints.
     * </p>
     *
     * @param tenantId the tenant ID to check
     * @return true if a session control policy exists for the tenant, false otherwise
     */
    boolean existsByTenantId(String tenantId);

    /**
     * Deletes a session control policy by tenant ID.
     * <p>
     * Removes the session control policy for the specified tenant.
     * Returns the number of entities deleted (0 or 1).
     * </p>
     *
     * @param tenantId the tenant ID whose session control policy should be deleted
     * @return the number of entities deleted
     */
    int deleteByTenantId(String tenantId);
}
