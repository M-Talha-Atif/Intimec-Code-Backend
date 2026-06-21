package com.aidentx.authservice.repository;

import com.aidentx.authservice.model.entity.AccountLockPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link AccountLockPolicy} entities.
 * Extends {@link JpaRepository} to provide CRUD operations and query method support.
 * 
 * @author Aidentx
 * @since 1.0
 */
@Repository
public interface AccountLockPolicyRepository extends JpaRepository<AccountLockPolicy, UUID> {

    /**
     * Finds an account lock policy by tenant ID.
     *
     * @param tenantId the tenant ID to search for
     * @return an {@link Optional} containing the account lock policy if found, or empty if not
     */
    Optional<AccountLockPolicy> findByTenantId(String tenantId);

    /**
     * Checks if an account lock policy exists for the given tenant ID.
     *
     * @param tenantId the tenant ID to check
     * @return true if an account lock policy exists for the tenant, false otherwise
     */
    boolean existsByTenantId(String tenantId);

    /**
     * Deletes the account lock policy for the given tenant ID.
     *
     * @param tenantId the tenant ID whose account lock policy should be deleted
     * @return the number of entities deleted
     */
    int deleteByTenantId(String tenantId);
}
