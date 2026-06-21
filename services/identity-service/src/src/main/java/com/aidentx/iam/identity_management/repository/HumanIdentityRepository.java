package com.aidentx.iam.identity_management.repository;

import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repository for handling uniqueness checks across HumanIdentity entities
 * (Employee and NonEmployee). Since HumanIdentity is a MappedSuperclass,
 * this repository delegates to the specific repositories for actual queries.
 */
@Repository
public class HumanIdentityRepository {

    private final EmployeeRepository employeeRepository;
    private final NonEmployeeRepository nonEmployeeRepository;

    public HumanIdentityRepository(EmployeeRepository employeeRepository,
                                   NonEmployeeRepository nonEmployeeRepository) {
        this.employeeRepository = employeeRepository;
        this.nonEmployeeRepository = nonEmployeeRepository;
    }

    /**
     * Checks if a username already exists for any HumanIdentity (Employee or NonEmployee)
     * within a specific tenant.
     *
     * @param tenantId The tenant ID
     * @param username The username to check
     * @return true if the username exists, false otherwise
     */
    public boolean existsByTenantIdAndUsername(String tenantId, String username) {
        return employeeRepository.findByTenantIdAndUsername(tenantId, username).isPresent() ||
               nonEmployeeRepository.findByTenantIdAndUsername(tenantId, username).isPresent();
    }

    /**
     * Checks if an email already exists for any HumanIdentity (Employee or NonEmployee)
     * within a specific tenant.
     *
     * @param tenantId The tenant ID
     * @param email The email to check
     * @return true if the email exists, false otherwise
     */
    public boolean existsByTenantIdAndEmail(String tenantId, String email) {
        return employeeRepository.findByTenantIdAndEmail(tenantId, email).isPresent() ||
               nonEmployeeRepository.findByTenantIdAndEmail(tenantId, email).isPresent();
    }

    /**
     * Searches for HumanIdentity entities (Employee and NonEmployee) by tenant ID and username containing the search term.
     *
     * @param tenantId The tenant ID
     * @param username The username search term
     * @return A list of identities matching the search criteria
     */
    public List<Identity> searchByTenantIdAndUsername(String tenantId, String username) {
        List<Identity> results = new ArrayList<>();
        results.addAll(employeeRepository.findByTenantIdAndUsernameContainingIgnoreCase(tenantId, username));
        results.addAll(nonEmployeeRepository.findByTenantIdAndUsernameContainingIgnoreCase(tenantId, username));
        return results;
    }

    /**
     * Finds an Employee by tenant ID and username.
     *
     * @param tenantId The tenant ID
     * @param username The username
     * @return Optional containing the Employee if found
     */
    public Optional<Employee> findEmployeeByTenantIdAndUsername(String tenantId, String username) {
        return employeeRepository.findByTenantIdAndUsername(tenantId, username);
    }

    /**
     * Finds a NonEmployee by tenant ID and username.
     *
     * @param tenantId The tenant ID
     * @param username The username
     * @return Optional containing the NonEmployee if found
     */
    public Optional<NonEmployee> findNonEmployeeByTenantIdAndUsername(String tenantId, String username) {
        return nonEmployeeRepository.findByTenantIdAndUsername(tenantId, username);
    }

    /**
     * Finds an Employee by tenant ID and email.
     *
     * @param tenantId The tenant ID
     * @param email The email
     * @return Optional containing the Employee if found
     */
    public Optional<Employee> findEmployeeByTenantIdAndEmail(String tenantId, String email) {
        return employeeRepository.findByTenantIdAndEmail(tenantId, email);
    }

    /**
     * Finds a NonEmployee by tenant ID and email.
     *
     * @param tenantId The tenant ID
     * @param email The email
     * @return Optional containing the NonEmployee if found
     */
    public Optional<NonEmployee> findNonEmployeeByTenantIdAndEmail(String tenantId, String email) {
        return nonEmployeeRepository.findByTenantIdAndEmail(tenantId, email);
    }
}
