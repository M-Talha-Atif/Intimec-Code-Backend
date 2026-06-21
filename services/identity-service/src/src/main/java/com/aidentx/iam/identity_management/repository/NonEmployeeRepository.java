package com.aidentx.iam.identity_management.repository;

import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NonEmployeeRepository extends JpaRepository<NonEmployee, UUID> {
    Optional<NonEmployee> findByContractId(String contractId);

    /**
     * Finds a non-employee by tenant and email.
     *
     * @param tenantId The tenant ID.
     * @param email    The email to search for.
     * @return An Optional containing the NonEmployee if found.
     */
    Optional<NonEmployee> findByTenantIdAndEmail(String tenantId, String email);

    /**
     * Finds a non-employee by tenant and contract ID.
     *
     * @param tenantId   The tenant ID.
     * @param contractId The contract ID to search for.
     * @return An Optional containing the NonEmployee if found.
     */
    Optional<NonEmployee> findByTenantIdAndContractId(String tenantId, String contractId);

    /**
     * Finds a non-employee by tenant and username.
     *
     * @param tenantId The tenant ID.
     * @param username The username to search for.
     * @return An Optional containing the NonEmployee if found.
     */
    Optional<NonEmployee> findByTenantIdAndUsername(String tenantId, String username);

    /**
     * Finds non-employees by tenant and username containing the search term (case-insensitive).
     *
     * @param tenantId The tenant ID.
     * @param username The username search term.
     * @return A list of non-employees matching the search criteria.
     */
    List<NonEmployee> findByTenantIdAndUsernameContainingIgnoreCase(String tenantId, String username);
}