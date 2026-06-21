package com.aidentx.iam.identity_management.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.aidentx.iam.identity_management.model.dto.EmployeeCreateDto;
import com.aidentx.iam.identity_management.model.dto.IdentityBusinessRoleCardDto;
import com.aidentx.iam.identity_management.model.dto.IdentityResponseDto;
import com.aidentx.iam.identity_management.model.dto.IdentityStatisticsDto;
import com.aidentx.iam.identity_management.model.dto.NonEmployeeCreateDto;
import com.aidentx.iam.identity_management.model.dto.NonHumanCreateDto;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing the complete lifecycle of all identity types
 * within the IAM system.
 *
 * <p>
 * This service ensures strict tenant isolation for all operations and handles
 * the core business logic
 * for creating, updating, retrieving, and deleting identities. It supports
 * three main identity types:
 * Employees, Non-Employees, and Non-Human identities (service accounts).
 * </p>
 *
 * <p>
 * All operations are tenant-scoped to ensure multi-tenancy security and data
 * isolation.
 * The service validates uniqueness constraints and enforces business rules
 * specific to each
 * identity type.
 * </p>
 *
 * <p>
 * Key responsibilities:
 * </p>
 * <ul>
 * <li>Identity creation with type-specific validation</li>
 * <li>Tenant-isolated identity retrieval and search</li>
 * <li>Identity updates with before/after state tracking</li>
 * <li>Identity deletion with cascade handling</li>
 * <li>Uniqueness validation across identity types</li>
 * </ul>
 *
 * @since 1.0
 */
public interface IdentityService {

        /**
         * Creates a new Employee identity within a specified tenant.
         *
         * <p>
         * This method ensures that the new employee's core attributes (username, email,
         * employeeId)
         * are unique within the tenant. It also validates that the managerId (if
         * provided) exists
         * and belongs to the same tenant.
         * </p>
         *
         * @param tenantId The tenant identifier where the identity will be created.
         *                 Must not be null or blank.
         * @param dto      The data transfer object containing all necessary information
         *                 for a new employee,
         *                 including core attributes, extended attributes, and
         *                 organizational information.
         * @return A unified response DTO representing the newly created identity,
         *         including the generated
         *         UUID, timestamps, and all attribute data.
         * @throws IllegalArgumentException                             if tenantId is
         *                                                              null/blank or
         *                                                              dto is null
         * @throws com.aidentx.iam.exception.DuplicateIdentityException if username,
         *                                                              email, or
         *                                                              employeeId
         *                                                              already exists
         *                                                              within the
         *                                                              tenant
         */
        IdentityResponseDto createEmployee(String tenantId, EmployeeCreateDto dto);

        /**
         * Creates a new Non-Employee identity within a specified tenant.
         *
         * <p>
         * This method ensures that the new non-employee's core attributes (username,
         * email, contractId)
         * are unique within the tenant. It validates sponsor relationships and contract
         * information.
         * </p>
         *
         * @param tenantId The tenant identifier where the identity will be created.
         *                 Must not be null or blank.
         * @param dto      The data transfer object containing all necessary information
         *                 for a new non-employee,
         *                 including contract details, sponsor information, and
         *                 organizational data.
         * @return A unified response DTO representing the newly created identity,
         *         including the generated
         *         UUID, timestamps, and all attribute data.
         * @throws IllegalArgumentException                             if tenantId is
         *                                                              null/blank or
         *                                                              dto is null
         * @throws com.aidentx.iam.exception.DuplicateIdentityException if username,
         *                                                              email, or
         *                                                              contractId
         *                                                              already exists
         *                                                              within the
         *                                                              tenant
         */
        IdentityResponseDto createNonEmployee(String tenantId, NonEmployeeCreateDto dto);

        /**
         * Creates a new Non-Human identity within a specified tenant.
         *
         * <p>
         * This method creates service accounts or system identities with appropriate
         * validation
         * for service names and ownership information.
         * </p>
         *
         * @param tenantId The tenant identifier where the identity will be created.
         *                 Must not be null or blank.
         * @param dto      The data transfer object containing all necessary information
         *                 for a new non-human identity,
         *                 including service details, ownership information, and
         *                 configuration data.
         * @return A unified response DTO representing the newly created identity,
         *         including the generated
         *         UUID, timestamps, and all attribute data.
         * @throws IllegalArgumentException                             if tenantId is
         *                                                              null/blank or
         *                                                              dto is null
         * @throws com.aidentx.iam.exception.DuplicateIdentityException if serviceName
         *                                                              already exists
         *                                                              within the
         *                                                              tenant
         */
        IdentityResponseDto createNonHumanIdentity(String tenantId, NonHumanCreateDto dto);

        /**
         * Retrieves a complete identity by tenant and ID with strict tenant isolation.
         *
         * <p>
         * This method ensures that users can only access identities within their own
         * tenant,
         * providing an additional security layer beyond authentication.
         * </p>
         *
         * @param tenantId The tenant identifier for security isolation. Must not be
         *                 null or blank.
         * @param id       The unique identifier of the identity to retrieve. Must not
         *                 be null.
         * @return An Optional containing the unified response DTO if the identity
         *         exists and belongs
         *         to the specified tenant, empty Optional otherwise.
         * @throws IllegalArgumentException if tenantId is null/blank or id is null
         */
        Optional<IdentityResponseDto> findByTenantIdAndId(String tenantId, UUID id);

        /**
         * Searches for identities by username within a specific tenant.
         *
         * <p>
         * Performs a case-insensitive partial match search on username across all
         * identity types
         * (Employee, Non-Employee, Non-Human) within the tenant. This is useful for
         * user lookup
         * and identity management interfaces.
         * </p>
         *
         * @param tenantId The tenant identifier for security isolation. Must not be
         *                 null or blank.
         * @param username The username fragment to search for. Supports partial
         *                 matching.
         * @return A list of matching identities ordered by relevance. Empty list if no
         *         matches found.
         * @throws IllegalArgumentException if tenantId is null/blank or username is
         *                                  null
         */
        List<IdentityResponseDto> searchByUsername(String tenantId, String username);

        /**
         * Retrieves all identities for a specific tenant with pagination support.
         *
         * <p>
         * This method is designed for administrative interfaces and bulk operations.
         * Results are paginated to ensure performance with large datasets.
         * </p>
         *
         * @param tenantId The tenant identifier for security isolation. Must not be
         *                 null or blank.
         * @param page     The page number (0-based indexing). Must be >= 0.
         * @param size     The number of items per page. Must be > 0.
         * @return A list of identities for the requested page. Empty list if page is
         *         beyond available data.
         * @throws IllegalArgumentException if tenantId is null/blank, page < 0, or size
         *                                  <= 0
         */
        List<IdentityResponseDto> findAllByTenantId(String tenantId, int page, int size);

        /**
         * Retrieves identities filtered by type for a specific tenant with Spring Data pagination.
         *
         * @param tenantId     The tenant identifier for security isolation.
         * @param identityType The identity type to filter by.
         * @param page         The page number (0-based).
         * @param size         The number of items per page.
         * @param sort         The sort parameter (e.g. "createdAt,desc").
         * @return A Spring Data Page of identity response DTOs.
         */
        org.springframework.data.domain.Page<IdentityResponseDto> findByType(
                        String tenantId, IdentityType identityType, int page, int size, String sort,
                        String search, String status, String department);

        /**
         * Updates an existing identity with type-specific validation.
         *
         * <p>
         * This method handles updates for all identity types with appropriate
         * validation
         * based on the identity type. It ensures uniqueness constraints are maintained
         * and
         * validates relationships (e.g., manager chains, sponsor relationships).
         * </p>
         *
         * @param tenantId The tenant identifier for security isolation. Must not be
         *                 null or blank.
         * @param id       The unique identifier of the identity to update. Must not be
         *                 null.
         * @param dto      The update DTO containing modified data. Must be of the
         *                 correct type
         *                 (EmployeeUpdateDto, NonEmployeeUpdateDto, or
         *                 NonHumanUpdateDto) matching
         *                 the existing identity's type.
         * @return The updated identity with all changes applied and timestamps
         *         refreshed.
         * @throws IllegalArgumentException                             if tenantId is
         *                                                              null/blank, id
         *                                                              is null, or dto
         *                                                              type
         *                                                              doesn't match
         *                                                              the identity
         *                                                              type
         * @throws com.aidentx.iam.exception.DuplicateIdentityException if updated
         *                                                              attributes
         *                                                              conflict with
         *                                                              uniqueness
         *                                                              constraints
         */
        IdentityResponseDto updateIdentity(String tenantId, UUID id, Object dto);

        /**
         * Permanently deletes an identity and all associated data within a tenant.
         *
         * <p>
         * This operation cascades to delete all related data including extended
         * attributes,
         * audit logs, and relationships. This action cannot be undone.
         * </p>
         *
         * @param tenantId The tenant identifier for security isolation. Must not be
         *                 null or blank.
         * @param id       The unique identifier of the identity to delete. Must not be
         *                 null.
         * @throws IllegalArgumentException                            if tenantId is
         *                                                             null/blank or id
         *                                                             is null
         * @throws com.aidentx.iam.exception.IdentityNotFoundException if the identity
         *                                                             doesn't exist
         *                                                             or doesn't belong
         *                                                             to the specified
         *                                                             tenant
         */
        void deleteIdentity(String tenantId, UUID id);

        /**
         * Retrieves comprehensive statistics about identities within a tenant.
         *
         * <p>
         * This method calculates statistics on-the-fly and includes counts by identity
         * type,
         * extended attribute usage, and other relevant metrics.
         * </p>
         *
         * @param tenantId The tenant identifier for security isolation. Must not be
         *                 null or blank.
         * @return A statistics DTO containing various identity-related metrics and
         *         counts.
         * @throws IllegalArgumentException if tenantId is null or blank
         */
        IdentityStatisticsDto getIdentityStatistics(String tenantId);

        /**
         * Retrieves statistics for a specific identity type within a tenant.
         *
         * <p>
         * This method calculates detailed statistics for the specified identity type,
         * including status breakdown and extended attribute usage.
         * </p>
         *
         * @param tenantId     The tenant identifier for security isolation. Must not be
         *                     null or blank.
         * @param identityType The identity type to get statistics for. Must not be
         *                     null.
         * @return Statistics specific to the requested identity type.
         * @throws IllegalArgumentException if tenantId is null/blank or identityType is
         *                                  null
         */
        IdentityStatisticsDto.IdentityTypeStatistics getIdentityTypeStatistics(String tenantId,
                        IdentityType identityType);

        /**
         * Retrieves all employees managed by a specific manager within a tenant.
         *
         * <p>
         * This method returns direct reports (employees where manager_id equals the
         * specified managerId). Indirect reports (reports of reports) are not included.
         * </p>
         *
         * @param tenantId  The tenant identifier for security isolation. Must not be
         *                  null or blank.
         * @param managerId The identity ID of the manager. Must not be null.
         * @return List of employee identities managed by the specified manager.
         * @throws IllegalArgumentException if tenantId is null/blank or managerId is
         *                                  null
         */
        List<IdentityResponseDto> getEmployeesByManager(String tenantId, UUID managerId);

        /**
         * Processes authoritative data from authoritative systems to create or update
         * identities. This is the primary entry point for Joiner/Mover flows.
         * 
         * <p>
         * Assumes attributes are mapped before being processed (it was stated in the
         * docs that the core-service handles all transofrmations and mappings).
         * Responsinbilities of this method include lookup, validation
         * against schema if identity is new, creation of the identity record,
         * persisting the shards, triggering reconciliation event
         * </p>
         * 
         * @param tenantId     The tenant identifier where the identity will be created
         *                     or updated. Must not be null or blank.
         * @param resourceId   The unique identifier from the authoritative source
         *                     system.
         * @param sourceName   The readable name of the authoritative source system
         * @param data         A map of the data attributes from the authoritative
         *                     source
         * @param identityType
         * @return DTO of the found or newly created identity
         */
        IdentityResponseDto processAuthoritativeData(String tenantId, String resourceId, String sourceName,
                        List<Map<String, Object>> data, IdentityType identityType);

        /**
         * Returns a paginated, searchable list of lightweight business/access role
         * cards assigned to an identity within a tenant.
         *
         * @param tenantId   tenant identifier for security isolation
         * @param identityId the identity whose roles are requested
         * @param search     optional text filter applied to role name or description
         * @param pageable   pagination and sort parameters
         * @return a page of lightweight role cards
         */
        Page<IdentityBusinessRoleCardDto> getBusinessRolesByIdentity(
                        String tenantId, UUID identityId, String search, Pageable pageable);
}