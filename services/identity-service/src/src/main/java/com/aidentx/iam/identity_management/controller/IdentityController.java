package com.aidentx.iam.identity_management.controller;

import java.util.List;
import java.util.UUID;

import com.aidentx.iam.entitlement_management.service.EntitlementService;
import com.aidentx.iam.identity_management.model.dto.EmployeeCreateDto;
import com.aidentx.iam.identity_management.model.dto.EmployeeUpdateDto;
import com.aidentx.iam.identity_management.model.dto.IdentityAccessBreakdownDto;
import com.aidentx.iam.identity_management.model.dto.IdentityBusinessRoleCardDto;
import com.aidentx.iam.identity_management.model.dto.IdentityEntitlementViewDto;
import com.aidentx.iam.identity_management.model.dto.IdentityResponseDto;
import com.aidentx.iam.identity_management.model.dto.IdentitySchemaDtos;
import com.aidentx.iam.identity_management.model.dto.IdentityStatisticsDto;
import com.aidentx.iam.identity_management.model.dto.NonEmployeeCreateDto;
import com.aidentx.iam.identity_management.model.dto.NonEmployeeUpdateDto;
import com.aidentx.iam.identity_management.model.dto.NonHumanCreateDto;
import com.aidentx.iam.identity_management.model.dto.NonHumanUpdateDto;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.service.IdentitySchemaService;
import com.aidentx.iam.identity_management.service.IdentityService;
import com.aidentx.iam.security.JwtTenantIdService;
import com.aidentx.iam.shadow_management.model.dto.GetShadowsByIdentityResponse;
import com.aidentx.iam.shadow_management.service.ShadowService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing identity lifecycle operations in the IAM system.
 *
 * <p>
 * This controller provides a comprehensive REST API for identity management
 * with strict
 * tenant isolation and comprehensive validation. It supports all CRUD
 * operations for three
 * identity types: Employees, Non-Employees, and Non-Human identities (service
 * accounts).
 * </p>
 *
 * <h2>Security Features:</h2>
 * <ul>
 * <li><strong>Tenant Isolation:</strong> All operations are automatically
 * scoped to the tenant
 * extracted from the JWT token</li>
 * <li><strong>Authentication Required:</strong> All endpoints require valid JWT
 * authentication</li>
 * <li><strong>Input Validation:</strong> All request bodies are validated using
 * Bean Validation</li>
 * <li><strong>Audit Logging:</strong> All operations are automatically audited
 * via AOP</li>
 * </ul>
 *
 * <h2>API Design:</h2>
 * <ul>
 * <li><strong>Type-Specific Endpoints:</strong> Separate endpoints for each
 * identity type
 * for better API clarity and type safety</li>
 * <li><strong>Standard HTTP Methods:</strong> Uses standard REST conventions
 * (GET, POST, PUT, DELETE)</li>
 * <li><strong>Consistent Response Format:</strong> All endpoints return unified
 * response DTOs</li>
 * <li><strong>Error Handling:</strong> Comprehensive error responses with
 * appropriate HTTP status codes</li>
 * </ul>
 *
 * <h2>Base Path:</h2>
 * <p>
 * All endpoints are under {@code /api/v1/identity}
 * </p>
 *
 * @see IdentityService
 * @see JwtTenantIdService
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/identity")
public class IdentityController {

    private final IUnifiedLogger logger;
    private final IdentityService identityService;
    private final IdentitySchemaService identitySchemaService;
    private final JwtTenantIdService jwtTenantIdService;
    private final ShadowService shadowService;
    private final EntitlementService entitlementService;

    /**
     * Constructs a new IdentityController with all required dependencies.
     *
     * @param identityService       Service for identity business logic operations
     * @param identitySchemaService Service for retrieving identity schemas
     * @param unifiedLoggerFactory  Factory for creating unified loggers
     * @param jwtTenantIdService    Service for extracting tenant ID from JWT tokens
     * @param shadowService         Service for shadow operations
     * @param entitlementService    Service for entitlement operations
     */
    public IdentityController(IdentityService identityService,
                              IdentitySchemaService identitySchemaService,
                              UnifiedLoggerFactory unifiedLoggerFactory,
                              JwtTenantIdService jwtTenantIdService,
                              ShadowService shadowService,
                              EntitlementService entitlementService) {
        this.identityService = identityService;
        this.identitySchemaService = identitySchemaService;
        this.logger = unifiedLoggerFactory.getLogger(IdentityController.class);
        this.jwtTenantIdService = jwtTenantIdService;
        this.shadowService = shadowService;
        this.entitlementService = entitlementService;
    }

    /**
     * Creates a new Employee identity within the current tenant.
     *
     * <p>
     * This endpoint creates a new employee identity with comprehensive validation
     * including uniqueness checks for username, email, and employeeId within the
     * tenant.
     * The operation is automatically audited and tenant-isolated.
     * </p>
     *
     * <h3>Validation Performed:</h3>
     * <ul>
     * <li>JWT token validation and tenant extraction</li>
     * <li>Bean validation on the request DTO</li>
     * <li>Uniqueness validation for username, email, and employeeId</li>
     * <li>Manager relationship validation (if managerId provided)</li>
     * </ul>
     *
     * <h3>Response:</h3>
     * <p>
     * Returns HTTP 201 (Created) with the complete identity data including
     * generated ID
     * and timestamps. Returns HTTP 400 (Bad Request) if validation fails.
     * </p>
     *
     * @param createDto The validated DTO containing all required employee
     *                  information
     *                  including core attributes, organizational data, and extended
     *                  attributes
     * @return ResponseEntity containing the newly created identity with HTTP 201
     * status
     * @throws org.springframework.web.bind.MethodArgumentNotValidException if DTO
     *                                                                      validation
     *                                                                      fails
     * @throws com.aidentx.iam.exception.DuplicateIdentityException         if
     *                                                                      uniqueness
     *                                                                      constraints
     *                                                                      are
     *                                                                      violated
     */
    @PostMapping("/employee")
    public ResponseEntity<IdentityResponseDto> createEmployee(@Valid @RequestBody EmployeeCreateDto createDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }
        logger.info().message("Received request to create a new employee.").detail("tenantId", tenantId).log();

        IdentityResponseDto createdIdentity = identityService.createEmployee(tenantId, createDto);

        return new ResponseEntity<>(createdIdentity, HttpStatus.CREATED);
    }

    /**
     * Creates a new Non-Employee identity.
     *
     * @param createDto The DTO containing non-employee data.
     * @return The newly created identity.
     */
    @PostMapping("/non-employee")
    public ResponseEntity<IdentityResponseDto> createNonEmployee(
            @Valid @RequestBody NonEmployeeCreateDto createDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }
        logger.info().message("Received request to create a new non-employee.").detail("tenantId", tenantId)
                .log();

        IdentityResponseDto createdIdentity = identityService.createNonEmployee(tenantId, createDto);

        return new ResponseEntity<>(createdIdentity, HttpStatus.CREATED);
    }

    /**
     * Creates a new Non-Human identity.
     *
     * @param createDto The DTO containing non-human identity data.
     * @return The newly created identity.
     */
    @PostMapping("/non-human")
    public ResponseEntity<IdentityResponseDto> createNonHumanIdentity(
            @Valid @RequestBody NonHumanCreateDto createDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        logger.info().message("Received request to create a new non-human identity.")
                .detail("tenantId", tenantId).log();

        IdentityResponseDto createdIdentity = identityService.createNonHumanIdentity(tenantId, createDto);

        return new ResponseEntity<>(createdIdentity, HttpStatus.CREATED);
    }

    /**
     * Retrieves a complete, unified view of any identity by its ID within the
     * current tenant.
     *
     * @param id The UUID of the identity.
     * @return A unified response DTO containing core and extended attributes, or
     * 404 Not Found.
     */
    @GetMapping("/{id}")
    public ResponseEntity<IdentityResponseDto> getIdentityById(@PathVariable UUID id) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }
        logger.info().message("Received request to find identity by ID.").detail("id", id)
                .detail("tenantId", tenantId).log();

        return identityService.findByTenantIdAndId(tenantId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- Additional Endpoints for Complete CRUD Operations ---

    /**
     * Searches for identities by username within the current tenant.
     * This is a basic search endpoint - more advanced search could be added.
     *
     * @param username The username to search for (partial match).
     * @return List of matching identities.
     */
    @GetMapping("/search")
    public ResponseEntity<List<IdentityResponseDto>> searchIdentities(@RequestParam String username) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        logger.info().message("Received request to search identities by username.")
                .detail("username", username).detail("tenantId", tenantId).log();

        List<IdentityResponseDto> results = identityService.searchByUsername(tenantId, username);
        return ResponseEntity.ok(results);
    }

    /**
     * Lists all identities for the current tenant with pagination.
     * This endpoint would be useful for admin interfaces.
     *
     * @param page Page number (0-based).
     * @param size Page size.
     * @return Paginated list of identities.
     */
    @GetMapping
    public ResponseEntity<List<IdentityResponseDto>> listIdentities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        logger.info().message("Received request to list identities.")
                .detail("page", page).detail("size", size).detail("tenantId", tenantId).log();

        List<IdentityResponseDto> results = identityService.findAllByTenantId(tenantId, page, size);
        return ResponseEntity.ok(results);
    }

    /**
     * Lists identities filtered by identity type with Spring Data pagination.
     *
     * <p>
     * Returns a standard Spring Data Page response containing the content array,
     * pagination metadata (totalElements, totalPages, number, size), and sort info.
     * This is the primary endpoint for the identity directory views.
     * </p>
     *
     * @param entityType The identity type slug: employee, non_employee, or non_human.
     * @param page       Page number (0-based).
     * @param size       Page size.
     * @param sort       Sort parameter (e.g. "createdAt,desc").
     * @return A paginated list of identities for the given type.
     */
    @GetMapping("/list/{entityType}")
    public ResponseEntity<?> listIdentitiesByType(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }

        IdentityType identityType;
        try {
            identityType = IdentityType.fromValue(entityType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error",
                            "Invalid entity type: " + entityType
                                    + ". Must be one of: employee, non_employee, non_human"));
        }

        logger.info().message("Listing identities by type.")
                .detail("entityType", entityType)
                .detail("identityType", identityType)
                .detail("page", page).detail("size", size).detail("sort", sort)
                .detail("search", search).detail("status", status)
                .detail("department", department)
                .detail("tenantId", tenantId).log();

        org.springframework.data.domain.Page<IdentityResponseDto> result =
                identityService.findByType(tenantId, identityType, page, size, sort,
                        search, status, department);
        return ResponseEntity.ok(result);
    }

    /**
     * Updates an existing Employee identity.
     *
     * @param id        The UUID of the employee to update.
     * @param updateDto The DTO with strongly-typed employee fields.
     * @return The updated identity.
     */
    @PutMapping("/employee/{id}")
    public ResponseEntity<IdentityResponseDto> updateEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody EmployeeUpdateDto updateDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }
        logger.info().message("Received request to update employee.").detail("id", id)
                .detail("tenantId", tenantId).log();

        IdentityResponseDto updatedIdentity = identityService.updateIdentity(tenantId, id, updateDto);
        return ResponseEntity.ok(updatedIdentity);
    }

    /**
     * Updates an existing Non-Employee identity.
     *
     * @param id        The UUID of the non-employee to update.
     * @param updateDto The DTO with strongly-typed non-employee fields.
     * @return The updated identity.
     */
    @PutMapping("/non-employee/{id}")
    public ResponseEntity<IdentityResponseDto> updateNonEmployee(
            @PathVariable UUID id,
            @Valid @RequestBody NonEmployeeUpdateDto updateDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }
        logger.info().message("Received request to update non-employee.").detail("id", id)
                .detail("tenantId", tenantId).log();

        IdentityResponseDto updatedIdentity = identityService.updateIdentity(tenantId, id, updateDto);
        return ResponseEntity.ok(updatedIdentity);
    }

    /**
     * Updates an existing Non-Human identity.
     *
     * @param id        The UUID of the non-human identity to update.
     * @param updateDto The DTO with strongly-typed non-human identity fields.
     * @return The updated identity.
     */
    @PutMapping("/non-human/{id}")
    public ResponseEntity<IdentityResponseDto> updateNonHumanIdentity(
            @PathVariable UUID id,
            @Valid @RequestBody NonHumanUpdateDto updateDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }
        logger.info().message("Received request to update non-human identity.").detail("id", id)
                .detail("tenantId", tenantId).log();

        IdentityResponseDto updatedIdentity = identityService.updateIdentity(tenantId, id, updateDto);
        return ResponseEntity.ok(updatedIdentity);
    }

    /**
     * Deletes an identity by ID.
     *
     * @param id The UUID of the identity to delete.
     * @return No content response.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIdentity(@PathVariable UUID id) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }
        logger.info().message("Received request to delete identity.").detail("id", id)
                .detail("tenantId", tenantId).log();

        identityService.deleteIdentity(tenantId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Retrieves the complete schema for EMPLOYEE identity type including both core
     * and
     * extended attributes.
     *
     * <p>
     * This endpoint provides the full schema definition for creating or updating
     * identities
     * of EMPLOYEE type. It includes both the core attributes defined in the
     * entity classes
     * and any custom extended attributes defined for the tenant.
     * </p>
     *
     * <h3>Response:</h3>
     * <p>
     * Returns HTTP 200 with the complete schema including:
     * <ul>
     * <li>Core attributes from the entity class (dynamically extracted)</li>
     * <li>Extended attributes defined in the database for this tenant and type</li>
     * </ul>
     * </p>
     *
     * @return ResponseEntity containing the complete identity schema
     */
    @GetMapping("/schema/employee")
    public ResponseEntity<IdentitySchemaDtos.IdentitySchemaResponseDto> getEmployeeIdentitySchema() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }

        IdentitySchemaDtos.IdentitySchemaResponseDto schema = identitySchemaService
                .getEmployeeIdentitySchema(tenantId);
        return ResponseEntity.ok(schema);
    }

    /**
     * Retrieves the complete schema for NON_EMPLOYEE identity type including both
     * core and
     * extended attributes.
     *
     * <p>
     * This endpoint provides the full schema definition for creating or updating
     * identities
     * of NON_EMPLOYEE type. It includes both the core attributes defined in the
     * entity classes
     * and any custom extended attributes defined for the tenant.
     * </p>
     *
     * <h3>Response:</h3>
     * <p>
     * Returns HTTP 200 with the complete schema including:
     * <ul>
     * <li>Core attributes from the entity class (dynamically extracted)</li>
     * <li>Extended attributes defined in the database for this tenant and type</li>
     * </ul>
     * </p>
     *
     * @return ResponseEntity containing the complete identity schema
     */
    @GetMapping("/schema/non_employee")
    public ResponseEntity<IdentitySchemaDtos.IdentitySchemaResponseDto> getNonEmployeeIdentitySchema() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }

        IdentitySchemaDtos.IdentitySchemaResponseDto schema = identitySchemaService
                .getNonEmployeeIdentitySchema(tenantId);
        return ResponseEntity.ok(schema);
    }

    /**
     * Retrieves the complete schema for NON_HUMAN identity type including both core
     * and
     * extended attributes.
     *
     * <p>
     * This endpoint provides the full schema definition for creating or updating
     * identities
     * of NON_HUMAN type. It includes both the core attributes defined in the
     * entity classes
     * and any custom extended attributes defined for the tenant.
     * </p>
     *
     * <h3>Response:</h3>
     * <p>
     * Returns HTTP 200 with the complete schema including:
     * <ul>
     * <li>Core attributes from the entity class (dynamically extracted)</li>
     * <li>Extended attributes defined in the database for this tenant and type</li>
     * </ul>
     * </p>
     *
     * @return ResponseEntity containing the complete identity schema
     */
    @GetMapping("/schema/non_human")
    public ResponseEntity<IdentitySchemaDtos.IdentitySchemaResponseDto> getNonHumanIdentitySchema() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }

        IdentitySchemaDtos.IdentitySchemaResponseDto schema = identitySchemaService
                .getNonHumanIdentitySchema(tenantId);
        return ResponseEntity.ok(schema);
    }

    /**
     * GET /api/v1/identity/statistics
     *
     * <p>
     * Retrieves comprehensive statistics about identities within the current
     * tenant.
     * Statistics are calculated on-the-fly and include counts by identity type,
     * extended attribute usage, and other relevant metrics.
     * </p>
     *
     * <h2>Response:</h2>
     * <p>
     * Returns {@link IdentityStatisticsDto} containing:
     * </p>
     * <ul>
     * <li>totalIdentities - Total number of identities in the tenant</li>
     * <li>identitiesByType - Count breakdown by identity type (EMPLOYEE,
     * NON_EMPLOYEE, NON_HUMAN)</li>
     * <li>totalExtendedAttributes - Total number of extended attributes</li>
     * <li>extendedAttributeUsage - Usage count by extended attribute
     * definition</li>
     * <li>activeIdentities - Number of active identities</li>
     * <li>inactiveIdentities - Number of inactive identities</li>
     * </ul>
     *
     * @return ResponseEntity containing identity statistics for the current tenant
     */
    @GetMapping("/statistics")
    public ResponseEntity<IdentityStatisticsDto> getIdentityStatistics() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }

        try {
            IdentityStatisticsDto statistics = identityService.getIdentityStatistics(tenantId);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error().message("Failed to retrieve identity statistics")
                    .detail("tenantId", tenantId)
                    .detail("error", e.getMessage())
                    .log();
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/v1/identity/statistics/employee
     *
     * <p>
     * Retrieves detailed statistics specifically for Employee identities within the
     * current tenant.
     * Statistics are calculated on-the-fly and include status breakdown and
     * extended attribute usage.
     * </p>
     *
     * @return ResponseEntity containing employee identity statistics
     */
    @GetMapping("/statistics/employee")
    public ResponseEntity<IdentityStatisticsDto.IdentityTypeStatistics> getEmployeeStatistics() {
        return getStatisticsByType(IdentityType.EMPLOYEE);
    }

    /**
     * GET /api/v1/identity/statistics/non-employee
     *
     * <p>
     * Retrieves detailed statistics specifically for Non-Employee identities within
     * the current tenant.
     * Statistics are calculated on-the-fly and include status breakdown and
     * extended attribute usage.
     * </p>
     *
     * @return ResponseEntity containing non-employee identity statistics
     */
    @GetMapping("/statistics/non_employee")
    public ResponseEntity<IdentityStatisticsDto.IdentityTypeStatistics> getNonEmployeeStatistics() {
        return getStatisticsByType(IdentityType.NON_EMPLOYEE);
    }

    /**
     * GET /api/v1/identity/statistics/non-human
     *
     * <p>
     * Retrieves detailed statistics specifically for Non-Human identities within
     * the current tenant.
     * Statistics are calculated on-the-fly and include status breakdown and
     * extended attribute usage.
     * </p>
     *
     * @return ResponseEntity containing non-human identity statistics
     */
    @GetMapping("/statistics/non_human")
    public ResponseEntity<IdentityStatisticsDto.IdentityTypeStatistics> getNonHumanStatistics() {
        return getStatisticsByType(IdentityType.NON_HUMAN);
    }

    /**
     * GET /api/v1/identity/{identityId}/shadows - Get shadows for an identity
     *
     * <p>
     * Retrieves all shadow objects linked to a specific identity.
     * Useful for viewing all resource accounts/entitlements associated with a user.
     * </p>
     *
     * @param identityId  The identity ID
     * @param resourceUid Optional filter by resource UID
     * @param objectClass Optional filter by object class
     * @return List of shadows for the identity
     */
    @GetMapping("/{identityId}/shadows")
    public ResponseEntity<GetShadowsByIdentityResponse> getShadowsByIdentity(
            @PathVariable UUID identityId,
            @RequestParam(required = false) String resourceUid,
            @RequestParam(required = false) String objectClass) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }

        logger.debug().message("Getting shadows for identity")
                .detail("identityId", identityId)
                .detail("resourceUid", resourceUid)
                .detail("objectClass", objectClass)
                .detail("tenantId", tenantId)
                .log();

        GetShadowsByIdentityResponse response = shadowService.getShadowsByIdentity(
                identityId, resourceUid, objectClass, tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/identity/employees/by-manager/{managerId}
     *
     * <p>
     * Retrieves all employees who have the specified manager.
     * This endpoint is used to get a manager's direct reports.
     * </p>
     *
     * @param managerId The identity ID of the manager
     * @return List of employee identities managed by the specified manager
     */
    @GetMapping("/employees/by-manager/{managerId}")
    public ResponseEntity<List<IdentityResponseDto>> getEmployeesByManager(@PathVariable UUID managerId) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }

        logger.info()
                .message("Received request to get employees by manager")
                .detail("managerId", managerId.toString())
                .detail("tenantId", tenantId)
                .log();

        List<IdentityResponseDto> employees = identityService.getEmployeesByManager(tenantId, managerId);
        return ResponseEntity.ok(employees);
    }

    /**
     * Returns all entitlements assigned to an identity across all of its accounts.
     * <p>
     * This endpoint resolves the chain: Identity -> Shadows (accounts) -> Assignments -> Entitlements.
     * Results are paginated and include account context (which account holds the entitlement)
     * alongside entitlement catalog details (name, risk level, classification).
     * <p>
     * Example: GET /api/v1/identity/550e8400-e29b-41d4-a716-446655440000/entitlements?page=0&size=20
     *
     * @param identityId the UUID of the identity
     * @param tenantId   tenant identifier from request header
     * @param pageable   pagination parameters (page, size, sort)
     * @param search     optional text search to filter entitlements by name or description
     * @return paginated list of entitlement assignments with account and entitlement details
     */
    @GetMapping("/{identityId}/entitlements")
    public ResponseEntity<Page<IdentityEntitlementViewDto>> getIdentityEntitlements(
            @PathVariable UUID identityId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<IdentityEntitlementViewDto> result = entitlementService
                .getEntitlementsByIdentityId(identityId, tenantId, search, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * Returns grouped identity access for UI: business roles with their entitlements
     * and standalone direct/dynamic/manual/import entitlements. Duplicate entitlement
     * assignment rows are preserved by assignmentId.
     * <p>
     * Example: GET /api/v1/identity/{identityId}/access-breakdown?search=finance
     *
     * @param identityId the UUID of the identity
     * @param tenantId   tenant identifier from request header
     * @param search     optional text filter across role and entitlement fields
     * @return grouped access breakdown for the identity
     */
    @GetMapping("/{identityId}/access-breakdown")
    public ResponseEntity<IdentityAccessBreakdownDto> getIdentityAccessBreakdown(
            @PathVariable UUID identityId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String search) {

        logger.info().message("Received request to get identity access breakdown")
                .detail("identityId", identityId.toString())
                .detail("tenantId", tenantId)
                .detail("search", search)
                .log();

        IdentityAccessBreakdownDto result = entitlementService
                .getIdentityAccessBreakdown(identityId, tenantId, search);

        return ResponseEntity.ok(result);
    }

    /**
     * Returns paginated, searchable lightweight business/access role cards
     * assigned to an identity.
     * <p>
     * Example: GET /api/v1/identity/{identityId}/business-roles?search=finance&page=0&size=10
     *
     * @param identityId the UUID of the identity
     * @param tenantId   tenant identifier from request header
     * @param search     optional text filter on role name, type, or description
     * @param pageable   pagination parameters (page, size, sort)
     * @return paginated list of lightweight role cards
     */
    @GetMapping("/{identityId}/business-roles")
    public ResponseEntity<Page<IdentityBusinessRoleCardDto>> getBusinessRolesByIdentity(
            @PathVariable UUID identityId,
            @RequestHeader("X-Tenant-Id") String tenantId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {

        logger.info().message("Received request to get business roles for identity")
                .detail("identityId", identityId.toString())
                .detail("tenantId", tenantId)
                .detail("search", search)
                .log();

        Page<IdentityBusinessRoleCardDto> result = identityService
                .getBusinessRolesByIdentity(tenantId, identityId, search, pageable);

        return ResponseEntity.ok(result);
    }

    /**
     * Helper method to get statistics for a specific identity type.
     */
    private ResponseEntity<IdentityStatisticsDto.IdentityTypeStatistics> getStatisticsByType(
            IdentityType identityType) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            logger.error().message("Tenant ID is missing or empty in JWT token").log();
            return ResponseEntity.badRequest().build();
        }

        try {
            IdentityStatisticsDto.IdentityTypeStatistics statistics = identityService
                    .getIdentityTypeStatistics(tenantId, identityType);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error().message("Failed to retrieve statistics for identity type")
                    .detail("tenantId", tenantId)
                    .detail("identityType", identityType)
                    .detail("error", e.getMessage())
                    .log();
            return ResponseEntity.internalServerError().build();
        }
    }
}

