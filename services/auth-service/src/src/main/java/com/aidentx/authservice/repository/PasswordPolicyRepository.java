package com.aidentx.authservice.repository;

import com.aidentx.authservice.model.entity.PasswordPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing {@link PasswordPolicy} entities.
 * Extends {@link JpaRepository} to provide CRUD operations and query method support.
 * 
 * @author Aidentx
 * @since 1.0
 */
@Repository
public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicy, UUID> {

    /**
     * Finds a password policy by tenant ID.
     *
     * @param tenantId the tenant ID to search for
     * @return an {@link Optional} containing the password policy if found, or empty if not
     */
    Optional<PasswordPolicy> findByTenantId(String tenantId);

    /**
     * Checks if a password policy exists for the given tenant ID.
     *
     * @param tenantId the tenant ID to check
     * @return true if a password policy exists for the tenant, false otherwise
     */
    boolean existsByTenantId(String tenantId);

    /**
     * Deletes the password policy for the given tenant ID.
     *
     * @param tenantId the tenant ID whose password policy should be deleted
     * @return the number of entities deleted
     */
    int deleteByTenantId(String tenantId);

    /**
     * Finds all password policies that have expired (policyExpiresAt < currentTime).
     * Only returns policies that have a valid policyExpiresAt value (not null).
     *
     * @param currentTimeMillis the current time in milliseconds
     * @return list of expired password policies
     */
    @Query("SELECT p FROM PasswordPolicy p WHERE p.policyExpiresAt IS NOT NULL AND p.policyExpiresAt < :currentTimeMillis")
    List<PasswordPolicy> findExpiredPolicies(@Param("currentTimeMillis") long currentTimeMillis);

    /**
     * Counts the number of password policies that have expired (policyExpiresAt < currentTime).
     * Only counts policies that have a valid policyExpiresAt value (not null).
     *
     * @param currentTimeMillis the current time in milliseconds
     * @return count of expired password policies
     */
    @Query("SELECT COUNT(p) FROM PasswordPolicy p WHERE p.policyExpiresAt IS NOT NULL AND p.policyExpiresAt < :currentTimeMillis")
    long countExpiredPolicies(@Param("currentTimeMillis") long currentTimeMillis);

    /**
     * Updates only the isEmailSent field for a specific policy.
     * This prevents unwanted updates to other fields like policyExpiresAt.
     *
     * @param policyId the ID of the policy to update
     * @param isEmailSent the new value for isEmailSent field
     * @return number of rows updated
     */
    @Modifying
    @Query("UPDATE PasswordPolicy p SET p.isEmailSent = :isEmailSent WHERE p.id = :policyId")
    int updateIsEmailSent(@Param("policyId") UUID policyId, @Param("isEmailSent") Boolean isEmailSent);
}
