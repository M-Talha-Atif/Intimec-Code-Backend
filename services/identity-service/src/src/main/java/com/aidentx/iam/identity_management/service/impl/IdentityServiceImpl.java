package com.aidentx.iam.identity_management.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.aidentx.iam.events.IdentityEventPublisher;
import com.aidentx.iam.entitlement_management.repository.EntitlementAssignmentRepository;
import com.aidentx.shared.dgraph.DgraphService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aidentx.iam.identity_management.model.dto.EmployeeCreateDto;
import com.aidentx.iam.identity_management.model.dto.EmployeeUpdateDto;
import com.aidentx.iam.identity_management.model.dto.IdentityResponseDto;
import com.aidentx.iam.identity_management.model.dto.IdentityStatisticsDto;
import com.aidentx.iam.identity_management.model.dto.NonEmployeeCreateDto;
import com.aidentx.iam.identity_management.model.dto.NonEmployeeUpdateDto;
import com.aidentx.iam.identity_management.model.dto.NonHumanCreateDto;
import com.aidentx.iam.identity_management.model.dto.NonHumanUpdateDto;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.ExtendedAttributeDefinition;
import com.aidentx.iam.identity_management.model.entity.HumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.entity.Shard;
import com.aidentx.iam.identity_management.model.enums.IdentityStatus;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.model.enums.ShardStatus;
import com.aidentx.iam.identity_management.repository.EmployeeRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrArrayValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrBooleanValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrDateValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrNumberValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrObjectValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrStringValueRepository;
import com.aidentx.iam.identity_management.repository.ExtendedAttributeDefinitionRepository;
import com.aidentx.iam.identity_management.repository.HumanIdentityRepository;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.identity_management.repository.IdentityShardRepository;
import com.aidentx.iam.identity_management.repository.ShardRepository;
import com.aidentx.iam.identity_management.repository.specification.IdentitySpecifications;
import com.aidentx.iam.identity_management.service.ExtendedAttributeService;
import com.aidentx.iam.identity_management.service.IdentitySchemaService;
import com.aidentx.iam.identity_management.service.IdentityService;
import com.aidentx.iam.identity_management.service.strategy.EmployeeCreationStrategy;
import com.aidentx.iam.identity_management.service.strategy.NonEmployeeCreationStrategy;
import com.aidentx.iam.identity_management.service.strategy.NonHumanCreationStrategy;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aidentx.iam.identity_management.model.dto.IdentityBusinessRoleCardDto;
import com.aidentx.iam.access_roles_management.model.entity.AccessRole;
import com.aidentx.iam.access_roles_management.model.entity.IdentityRoleAssignment;
import com.aidentx.iam.access_roles_management.repository.AccessRoleRepository;
import com.aidentx.iam.access_roles_management.repository.IdentityRoleAssignmentRepository;
import org.springframework.data.domain.PageImpl;

/**
 * Production-ready implementation of the {@link IdentityService} interface.
 *
 * <p>
 * This service provides comprehensive identity lifecycle management with
 * enterprise-grade
 * features including tenant isolation, audit trails, validation, and error
 * handling. It serves
 * as the primary business logic layer for all identity operations in the IAM
 * system.
 * </p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 * <li><strong>Tenant Isolation:</strong> All operations are strictly scoped to
 * tenant boundaries</li>
 * <li><strong>Type Safety:</strong> Supports Employee, Non-Employee, and
 * Non-Human identity types</li>
 * <li><strong>Uniqueness Validation:</strong> Enforces unique constraints
 * across identity types</li>
 * <li><strong>Audit Integration:</strong> Automatic audit trail generation via
 * AOP</li>
 * <li><strong>Transactional Safety:</strong> All operations wrapped in
 * transactions</li>
 * <li><strong>Relationship Management:</strong> Handles manager/sponsor
 * relationships</li>
 * <li><strong>Extended Attributes:</strong> Support for custom attributes per
 * identity</li>
 * </ul>
 *
 * <h2>Architecture:</h2>
 * <p>
 * The implementation follows a strategy pattern for identity creation, allowing
 * type-specific
 * validation and business logic while maintaining a unified interface. Each
 * identity type
 * has its own creation strategy that handles type-specific requirements.
 * </p>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * This service is thread-safe as it maintains no mutable instance state and
 * relies on
 * thread-safe dependencies (repositories, strategies, etc.).
 * </p>
 *
 * @since 1.0
 * @see IdentityService
 * @see EmployeeCreationStrategy
 * @see NonEmployeeCreationStrategy
 * @see NonHumanCreationStrategy
 */
@Service
@Transactional // todo: remove this it's never a good idea to mark everything transactional
public class IdentityServiceImpl implements IdentityService {

        private record AccessTotals(long entitlements) {
                long total() {
                        return entitlements;
                }
        }

        private final IUnifiedLogger logger;
        private final IdentityRepository identityRepository;
        private final HumanIdentityRepository humanIdentityRepository;
        private final IdentityShardRepository identityShardRepository;
        private final EmployeeRepository employeeRepository;
        private final ExtendedAttributeService extendedAttributeService;
        private final ExtendedAttributeDefinitionRepository extendedAttributeDefinitionRepository;
        private final ExtAttrStringValueRepository extAttrStringValueRepository;
        private final ExtAttrBooleanValueRepository extAttrBooleanValueRepository;
        private final ExtAttrNumberValueRepository extAttrNumberValueRepository;
        private final ExtAttrDateValueRepository extAttrDateValueRepository;
        private final ExtAttrObjectValueRepository extAttrObjectValueRepository;
        private final ExtAttrArrayValueRepository extAttrArrayValueRepository;
        private final EmployeeCreationStrategy employeeCreationStrategy;
        private final NonEmployeeCreationStrategy nonEmployeeCreationStrategy;
        private final NonHumanCreationStrategy nonHumanCreationStrategy;
        private final IdentitySchemaService identitySchemaService;
        private final ObjectMapper objectMapper;
        private final ShardRepository shardRepository;
        private IdentityEventPublisher identityEventPublisher;
        private final AccessRoleRepository accessRoleRepository;
        private final IdentityRoleAssignmentRepository identityRoleAssignmentRepository;
        private final EntitlementAssignmentRepository entitlementAssignmentRepository;
        private final DgraphService dgraphService;

        /**
         * Constructs a new IdentityServiceImpl with all required dependencies.
         *
         * <p>
         * All parameters are required and must not be null. The service uses
         * constructor
         * injection to ensure all dependencies are properly initialized at startup.
         * </p>
         *
         * @param loggerFactory                         Factory for creating unified
         *                                              loggers
         * @param identityRepository                    Repository for base identity
         *                                              operations
         * @param humanIdentityRepository               Repository for human identity
         *                                              uniqueness checks
         * @param identityShardRepository               Repository for identity shard
         *                                              management
         * @param employeeRepository                    Repository for employee-specific
         *                                              operations
         * @param extendedAttributeService              Service for managing extended
         *                                              attributes
         * @param identitySchemaService                 Service for managing identity
         *                                              schemas
         * @param extendedAttributeDefinitionRepository Repository for extended
         *                                              attribute definitions
         * @param extAttrStringValueRepository          Repository for string extended
         *                                              attribute values
         * @param extAttrBooleanValueRepository         Repository for boolean extended
         *                                              attribute values
         * @param extAttrNumberValueRepository          Repository for number extended
         *                                              attribute values
         * @param extAttrDateValueRepository            Repository for date extended
         *                                              attribute values
         * @param extAttrObjectValueRepository          Repository for object extended
         *                                              attribute values
         * @param extAttrArrayValueRepository           Repository for array extended
         *                                              attribute values
         * @param employeeCreationStrategy              Strategy for employee-specific
         *                                              creation logic
         * @param nonEmployeeCreationStrategy           Strategy for
         *                                              non-employee-specific creation
         *                                              logic
         * @param nonHumanCreationStrategy              Strategy for non-human identity
         *                                              creation logic
         */
        public IdentityServiceImpl(UnifiedLoggerFactory loggerFactory,
                        IdentityRepository identityRepository,
                        HumanIdentityRepository humanIdentityRepository,
                        IdentityShardRepository identityShardRepository,
                        EmployeeRepository employeeRepository,
                        ExtendedAttributeService extendedAttributeService,
                        IdentitySchemaService identitySchemaService,
                        ExtendedAttributeDefinitionRepository extendedAttributeDefinitionRepository,
                        ExtAttrStringValueRepository extAttrStringValueRepository,
                        ExtAttrBooleanValueRepository extAttrBooleanValueRepository,
                        ExtAttrNumberValueRepository extAttrNumberValueRepository,
                        ExtAttrDateValueRepository extAttrDateValueRepository,
                        ExtAttrObjectValueRepository extAttrObjectValueRepository,
                        ExtAttrArrayValueRepository extAttrArrayValueRepository,
                        EmployeeCreationStrategy employeeCreationStrategy,
                        NonEmployeeCreationStrategy nonEmployeeCreationStrategy,
                        NonHumanCreationStrategy nonHumanCreationStrategy,
                        ObjectMapper objectMapper,
                        ShardRepository shardRepository,
                                   IdentityEventPublisher identityEventPublisher,
                        AccessRoleRepository accessRoleRepository,
                        IdentityRoleAssignmentRepository identityRoleAssignmentRepository,
                        EntitlementAssignmentRepository entitlementAssignmentRepository,
                        DgraphService dgraphService) {
                this.logger = loggerFactory.getLogger(IdentityServiceImpl.class);
                this.identityRepository = identityRepository;
                this.humanIdentityRepository = humanIdentityRepository;
                this.identityShardRepository = identityShardRepository;
                this.employeeRepository = employeeRepository;
                this.extendedAttributeService = extendedAttributeService;
                this.identitySchemaService = identitySchemaService;
                this.extendedAttributeDefinitionRepository = extendedAttributeDefinitionRepository;
                this.extAttrStringValueRepository = extAttrStringValueRepository;
                this.extAttrBooleanValueRepository = extAttrBooleanValueRepository;
                this.extAttrNumberValueRepository = extAttrNumberValueRepository;
                this.extAttrDateValueRepository = extAttrDateValueRepository;
                this.extAttrObjectValueRepository = extAttrObjectValueRepository;
                this.extAttrArrayValueRepository = extAttrArrayValueRepository;
                this.employeeCreationStrategy = employeeCreationStrategy;
                this.nonEmployeeCreationStrategy = nonEmployeeCreationStrategy;
                this.nonHumanCreationStrategy = nonHumanCreationStrategy;
                this.objectMapper = objectMapper;
                this.shardRepository = shardRepository;
                this.identityEventPublisher = identityEventPublisher;
                this.accessRoleRepository = accessRoleRepository;
                this.identityRoleAssignmentRepository = identityRoleAssignmentRepository;
                this.entitlementAssignmentRepository = entitlementAssignmentRepository;
                this.dgraphService = dgraphService;

        }

        @Override
        public IdentityResponseDto createEmployee(String tenantId, EmployeeCreateDto dto) {
                logger.info().message("Creating new employee identity").detail("tenantId", tenantId).log();

                UUID createdId = employeeCreationStrategy.create(tenantId, dto, dto.extendedAttributes());

                // Fetch and return the created identity
                IdentityResponseDto result = findByTenantIdAndId(tenantId, createdId)
                                .orElseThrow(() -> new IllegalStateException("Could not re-fetch created employee"));

                logger.info().message("Successfully created employee identity")
                                .detail("tenantId", tenantId)
                                .detail("identityId", createdId)
                                .log();

                return result;
        }

        @Override
        public IdentityResponseDto createNonEmployee(String tenantId, NonEmployeeCreateDto dto) {
                logger.info().message("Creating new non-employee identity").detail("tenantId", tenantId).log();

                UUID createdId = nonEmployeeCreationStrategy.create(tenantId, dto, dto.extendedAttributes());

                // Fetch and return the created identity
                IdentityResponseDto result = findByTenantIdAndId(tenantId, createdId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Could not re-fetch created non-employee"));

                logger.info().message("Successfully created non-employee identity")
                                .detail("tenantId", tenantId)
                                .detail("identityId", createdId)
                                .log();

                return result;
        }

        @Override
        public IdentityResponseDto createNonHumanIdentity(String tenantId, NonHumanCreateDto dto) {
                logger.info().message("Creating new non-human identity").detail("tenantId", tenantId).log();

                UUID createdId = nonHumanCreationStrategy.create(tenantId, dto, dto.extendedAttributes());

                // Fetch and return the created identity
                IdentityResponseDto result = findByTenantIdAndId(tenantId, createdId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Could not re-fetch created non-human identity"));

                logger.info().message("Successfully created non-human identity")
                                .detail("tenantId", tenantId)
                                .detail("identityId", createdId)
                                .log();

                return result;
        }

        /**
         * Tenant-specific method to find an identity by ID within a specific tenant.
         *
         * <p>
         * This method provides additional security by ensuring strict tenant isolation.
         * Users can only access identities that belong to their tenant, preventing
         * cross-tenant data access vulnerabilities.
         * </p>
         *
         * @param tenantId The tenant identifier for security isolation. Must not be
         *                 null or blank.
         * @param id       The unique identifier of the identity to retrieve. Must not
         *                 be null.
         * @return An Optional containing the unified response DTO if the identity
         *         exists
         *         and belongs to the specified tenant, empty Optional otherwise.
         * @throws IllegalArgumentException if tenantId is null/blank or id is null
         */
        @Override
        @Transactional(readOnly = true)
        public Optional<IdentityResponseDto> findByTenantIdAndId(String tenantId, UUID id) {
                logger.debug().message("Finding identity by tenant and ID")
                                .detail("tenantId", tenantId)
                                .detail("identityId", id)
                                .log();

                return identityRepository.findByTenantIdAndId(tenantId, id)
                                .map(identity -> mapIdentityToResponseDto(identity,
                                                loadAccessTotals(tenantId, List.of(identity.getId()))
                                                                .getOrDefault(identity.getId(), new AccessTotals(0))));
        }

        /**
         * Central private method to map any Identity subclass to the unified response
         * DTO.
         */
        private IdentityResponseDto mapIdentityToResponseDto(Identity identity) {
                return mapIdentityToResponseDto(identity, new AccessTotals(0));
        }

        private IdentityResponseDto mapIdentityToResponseDto(Identity identity, AccessTotals accessTotals) {
                Map<String, Object> coreAttributes = new HashMap<>();
                String riskLevel = resolveIdentityRiskLevel(identity);

                if (identity instanceof Employee) {
                        Employee employee = (Employee) identity;
                        coreAttributes.put("username", employee.getUsername());
                        coreAttributes.put("email", employee.getEmail());
                        coreAttributes.put("firstName", employee.getFirstName());
                        coreAttributes.put("lastName", employee.getLastName());
                        coreAttributes.put("displayName", employee.getDisplayName());
                        coreAttributes.put("riskLevel", riskLevel);
                        coreAttributes.put("employeeId", employee.getEmployeeId());
                        coreAttributes.put("jobTitle", employee.getJobTitle());
                        coreAttributes.put("department", employee.getDepartment());
                        coreAttributes.put("hireDate", employee.getHireDate());
                        coreAttributes.put("terminationDate", employee.getTerminationDate());
                        coreAttributes.put("managerRef", employee.getManagerRef());
                        if (employee.getManager() != null) {
                                coreAttributes.put("managerId", employee.getManager().getId());
                                // Expose resolved manager display name for UI
                                if (employee.getManager() instanceof HumanIdentity) {
                                        coreAttributes.put("managerDisplayName",
                                                ((HumanIdentity) employee.getManager()).getDisplayName());
                                }
                        }
                } else if (identity instanceof NonEmployee) {
                        NonEmployee nonEmployee = (NonEmployee) identity;
                        coreAttributes.put("username", nonEmployee.getUsername());
                        coreAttributes.put("email", nonEmployee.getEmail());
                        coreAttributes.put("firstName", nonEmployee.getFirstName());
                        coreAttributes.put("lastName", nonEmployee.getLastName());
                        coreAttributes.put("displayName", nonEmployee.getDisplayName());
                        coreAttributes.put("riskLevel", riskLevel);
                        coreAttributes.put("contractId", nonEmployee.getContractId());
                        coreAttributes.put("vendorName", nonEmployee.getVendorName());
                        coreAttributes.put("contractEndDate", nonEmployee.getContractEndDate());
                        if (nonEmployee.getSponsor() != null) {
                                coreAttributes.put("sponsorId", nonEmployee.getSponsor().getId());
                        }
                } else if (identity instanceof NonHumanIdentity) {
                        NonHumanIdentity nonHuman = (NonHumanIdentity) identity;
                        coreAttributes.put("serviceName", nonHuman.getServiceName());
                        coreAttributes.put("ownerTeam", nonHuman.getOwnerTeam());
                        coreAttributes.put("riskLevel", riskLevel);
                        coreAttributes.put("description", nonHuman.getDescription());
                }

                coreAttributes.put("totalAccess", accessTotals.total());

                // Get identity type name from the actual class
                String identityTypeName = identity.getClass().getSimpleName().toUpperCase();
                if (identityTypeName.contains("EMPLOYEE")) {
                        identityTypeName = "EMPLOYEE";
                } else if (identityTypeName.contains("NONEMPLOYEE")) {
                        identityTypeName = "NON_EMPLOYEE";
                } else if (identityTypeName.contains("NONHUMAN")) {
                        identityTypeName = "NON_HUMAN";
                }

                IdentityType identityType = IdentityType.fromValue(identityTypeName);
                Map<String, Object> extendedAttributes = extendedAttributeService.findExtendedAttributesForIdentity(
                                identity.getId(),
                                identity.getTenantId(), identityType);

                return new IdentityResponseDto(
                                identity.getId(),
                                identity.getDgraphNodeId(),
                                identity.getTenantId(),
                                identityTypeName,
                                identity.getStatus(),
                                 riskLevel,
                                 accessTotals.total(),
                                coreAttributes,
                                extendedAttributes,
                                smartEpochToInstant(identity.getCreatedAt()),
                                smartEpochToInstant(identity.getUpdatedAt()));
        }

        private List<IdentityResponseDto> mapIdentitiesToResponseDtos(String tenantId, List<? extends Identity> identities) {
                Map<UUID, AccessTotals> totalsByIdentityId = loadAccessTotals(tenantId, identityIds(identities));
                return identities.stream()
                                .map(identity -> mapIdentityToResponseDto(identity,
                                                totalsByIdentityId.getOrDefault(identity.getId(), new AccessTotals(0))))
                                .collect(Collectors.toList());
        }

        private String resolveIdentityRiskLevel(Identity identity) {
                if (identity instanceof HumanIdentity humanIdentity) {
                        return humanIdentity.getRiskLevel();
                }
                if (identity instanceof NonHumanIdentity nonHumanIdentity) {
                        return nonHumanIdentity.getRiskLevel();
                }
                return null;
        }

        private List<UUID> identityIds(Collection<? extends Identity> identities) {
                return identities.stream()
                                .map(Identity::getId)
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .collect(Collectors.toList());
        }

        private Map<UUID, AccessTotals> loadAccessTotals(String tenantId, List<UUID> identityIds) {
                if (identityIds == null || identityIds.isEmpty()) {
                        return Map.of();
                }
                Map<UUID, Long> entitlementCounts = rowsToLongMap(
                                entitlementAssignmentRepository.countAssignmentRowsByTenantIdAndIdentityIdIn(
                                                tenantId, identityIds));

                Map<UUID, AccessTotals> totals = new HashMap<>();
                for (UUID identityId : identityIds) {
                        totals.put(identityId, new AccessTotals(entitlementCounts.getOrDefault(identityId, 0L)));
                }
                return totals;
        }

        private Map<UUID, Long> rowsToLongMap(List<Object[]> rows) {
                Map<UUID, Long> result = new HashMap<>();
                if (rows == null) {
                        return result;
                }
                for (Object[] row : rows) {
                        if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                                continue;
                        }
                        UUID identityId = row[0] instanceof UUID uuid ? uuid : UUID.fromString(row[0].toString());
                        long count = row[1] instanceof Number number ? number.longValue() : Long.parseLong(row[1].toString());
                        result.put(identityId, count);
                }
                return result;
        }

        /**
         * Converts an epoch timestamp to Instant, handling both epoch-seconds and
         * epoch-milliseconds transparently. Values below 10^11 are treated as
         * epoch seconds; values at or above 10^11 as epoch milliseconds.
         */
        private Instant smartEpochToInstant(Long epochValue) {
                if (epochValue == null) {
                        return null;
                }
                long millis = epochValue < 100_000_000_000L ? epochValue * 1000L : epochValue;
                return Instant.ofEpochMilli(millis);
        }

        @Override
        @Transactional(readOnly = true)
        public List<IdentityResponseDto> searchByUsername(String tenantId, String username) {
                logger.info().message("Searching identities by username.").detail("tenantId", tenantId)
                                .detail("username", username).log();

                // Use the repository method for efficient database-level search
                List<Identity> identities = humanIdentityRepository.searchByTenantIdAndUsername(tenantId, username);

                return mapIdentitiesToResponseDtos(tenantId, identities);
        }

        @Override
        @Transactional(readOnly = true)
        public List<IdentityResponseDto> getEmployeesByManager(String tenantId, UUID managerId) {
                logger.info().message("Finding employees by manager.").detail("tenantId", tenantId)
                                .detail("managerId", managerId).log();

                // Query employees who have this manager
                List<Employee> employees = employeeRepository.findByTenantIdAndManager_Id(tenantId, managerId);

                // Convert to response DTOs using the standard mapping method
                return mapIdentitiesToResponseDtos(tenantId, employees);
        }

        @Override
        @Transactional(readOnly = true)
        public List<IdentityResponseDto> findAllByTenantId(String tenantId, int page, int size) {
                logger.info().message("Finding all identities by tenant with pagination.").detail("tenantId", tenantId)
                                .detail("page", page).detail("size", size).log();

                Specification<Identity> spec = IdentitySpecifications.byTenantId(tenantId);
                Pageable pageable = PageRequest.of(page, size);
                Page<Identity> identityPage = identityRepository.findAll(spec, pageable);

                return mapIdentitiesToResponseDtos(tenantId, identityPage.getContent());
        }

        @Override
        @Transactional(readOnly = true)
        public Page<IdentityResponseDto> findByType(String tenantId, IdentityType identityType,
                        int page, int size, String sort, String search, String statusFilter,
                        String departmentFilter) {
                logger.info().message("Finding identities by type with pagination and filters.")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .detail("page", page).detail("size", size).detail("sort", sort)
                                .detail("search", search).detail("status", statusFilter)
                                .detail("department", departmentFilter).log();

                String[] sortParts = sort.split(",");
                String sortField = sortParts[0];
                org.springframework.data.domain.Sort.Direction direction =
                                sortParts.length > 1 && "asc".equalsIgnoreCase(sortParts[1])
                                                ? org.springframework.data.domain.Sort.Direction.ASC
                                                : org.springframework.data.domain.Sort.Direction.DESC;

                Pageable pageable = PageRequest.of(page, size,
                                org.springframework.data.domain.Sort.by(direction, sortField));

                boolean hasSearch = search != null && !search.isBlank();
                boolean hasStatus = statusFilter != null && !statusFilter.isBlank();
                boolean hasDepartment = departmentFilter != null && !departmentFilter.isBlank()
                                && !"all".equalsIgnoreCase(departmentFilter);

                if (!hasSearch && !hasStatus && !hasDepartment) {
                        // Fast path — no filters
                        Page<Identity> identityPage = identityRepository.findByTenantIdAndIdentityType(
                                        tenantId, identityType, pageable);
                        List<IdentityResponseDto> content = mapIdentitiesToResponseDtos(tenantId, identityPage.getContent());
                        return new PageImpl<>(content, pageable, identityPage.getTotalElements());
                }

                Specification<Identity> spec = (root, query, cb) -> {
                        java.util.List<jakarta.persistence.criteria.Predicate> predicates =
                                        new java.util.ArrayList<>();
                        predicates.add(cb.equal(root.get("tenantId"), tenantId));
                        predicates.add(cb.equal(root.get("identityType"), identityType));

                        if (hasStatus) {
                                try {
                                        com.aidentx.iam.identity_management.model.enums.IdentityStatus statusEnum =
                                                        com.aidentx.iam.identity_management.model.enums.IdentityStatus
                                                                        .valueOf(statusFilter.toUpperCase());
                                        predicates.add(cb.equal(root.get("status"), statusEnum));
                                } catch (IllegalArgumentException ignored) {
                                        // skip unknown status values
                                }
                        }

                        if (hasSearch || hasDepartment) {
                                String pattern = hasSearch ? "%" + search.toLowerCase() + "%" : null;
                                if (identityType == IdentityType.EMPLOYEE
                                                || identityType == IdentityType.NON_EMPLOYEE) {
                                        jakarta.persistence.criteria.Root<Employee> empRoot =
                                                        cb.treat(root, Employee.class);
                                        if (hasSearch) {
                                                predicates.add(cb.or(
                                                                cb.like(cb.lower(empRoot.get("firstName")), pattern),
                                                                cb.like(cb.lower(empRoot.get("lastName")), pattern),
                                                                cb.like(cb.lower(empRoot.get("email")), pattern),
                                                                cb.like(cb.lower(empRoot.get("displayName")), pattern),
                                                                cb.like(cb.lower(empRoot.get("username")), pattern)));
                                        }
                                        if (hasDepartment && identityType == IdentityType.EMPLOYEE) {
                                                predicates.add(cb.equal(empRoot.get("department"),
                                                                departmentFilter));
                                        }
                                } else if (identityType == IdentityType.NON_HUMAN && hasSearch) {
                                        jakarta.persistence.criteria.Root<NonHumanIdentity> nhRoot =
                                                        cb.treat(root, NonHumanIdentity.class);
                                        predicates.add(cb.like(cb.lower(nhRoot.get("serviceName")),
                                                        pattern));
                                }
                        }

                        return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
                };

                Page<Identity> identityPage = identityRepository.findAll(spec, pageable);
                List<IdentityResponseDto> content = mapIdentitiesToResponseDtos(tenantId, identityPage.getContent());
                return new PageImpl<>(content, pageable, identityPage.getTotalElements());
        }

        @Override
        @Transactional
        public IdentityResponseDto updateIdentity(String tenantId, UUID id, Object dto) {
                logger.info().message("Updating identity.").detail("tenantId", tenantId).detail("id", id).log();

                Identity identity = identityRepository.findByTenantIdAndId(tenantId, id)
                                .orElseThrow(() -> new IllegalArgumentException("Identity not found: " + id));

                // Handle different update DTO types based on identity type
                switch (identity.getIdentityType()) {
                        case EMPLOYEE:
                                if (!(dto instanceof EmployeeUpdateDto)) {
                                        throw new IllegalArgumentException(
                                                        "Invalid update DTO type for Employee identity");
                                }
                                employeeCreationStrategy.update(identity, dto,
                                                ((EmployeeUpdateDto) dto).extendedAttributes());
                                break;

                        case NON_EMPLOYEE:
                                if (!(dto instanceof NonEmployeeUpdateDto)) {
                                        throw new IllegalArgumentException(
                                                        "Invalid update DTO type for Non-Employee identity");
                                }
                                nonEmployeeCreationStrategy.update(identity, dto,
                                                ((NonEmployeeUpdateDto) dto).extendedAttributes());
                                break;

                        case NON_HUMAN:
                                if (!(dto instanceof NonHumanUpdateDto)) {
                                        throw new IllegalArgumentException(
                                                        "Invalid update DTO type for Non-Human identity");
                                }
                                nonHumanCreationStrategy.update(identity, dto,
                                                ((NonHumanUpdateDto) dto).extendedAttributes());
                                break;

                        default:
                                throw new IllegalArgumentException(
                                                "Unknown identity type: " + identity.getIdentityType());
                }

                return mapIdentityToResponseDto(identity,
                                loadAccessTotals(tenantId, List.of(identity.getId()))
                                                .getOrDefault(identity.getId(), new AccessTotals(0)));
        }

        @Override
        @Transactional
        public void deleteIdentity(String tenantId, UUID id) {
                logger.info().message("Deleting identity.").detail("tenantId", tenantId).detail("id", id).log();

                Identity identity = identityRepository.findByTenantIdAndId(tenantId, id)
                                .orElseThrow(() -> new IllegalArgumentException("Identity not found: " + id));

                // Delete extended attributes first
                extendedAttributeService.deleteExtendedAttributes(id, tenantId, identity.getIdentityType());

                // Delete the identity
                identityRepository.delete(identity);
        }

        @Override
        @Transactional(readOnly = true)
        public IdentityStatisticsDto getIdentityStatistics(String tenantId) {
                logger.info().message("Calculating identity statistics.").detail("tenantId", tenantId).log();

                if (tenantId == null || tenantId.trim().isEmpty()) {
                        throw new IllegalArgumentException("Tenant ID cannot be null or empty");
                }

                // Calculate total identities and per-type statistics
                long totalIdentities = 0;
                Map<IdentityType, Long> identitiesByType = new HashMap<>();
                Map<IdentityType, IdentityStatisticsDto.IdentityTypeStatistics> statisticsByType = new HashMap<>();

                for (IdentityType type : IdentityType.values()) {
                        long typeCount = identityRepository.countByTenantIdAndIdentityType(tenantId, type);
                        identitiesByType.put(type, typeCount);
                        totalIdentities += typeCount;

                        // Calculate status breakdown for this identity type
                        IdentityStatisticsDto.IdentityTypeStatistics typeStats = calculateTypeStatistics(tenantId,
                                        type);
                        statisticsByType.put(type, typeStats);
                }

                // Calculate overall extended attribute statistics
                long totalExtendedAttributes = 0;
                Map<String, Long> extendedAttributeUsage = new HashMap<>();

                // Aggregate extended attribute statistics from all types
                for (IdentityStatisticsDto.IdentityTypeStatistics typeStats : statisticsByType.values()) {
                        totalExtendedAttributes += typeStats.getExtendedAttributesCount();

                        // Merge usage maps
                        for (Map.Entry<String, Long> entry : typeStats.getExtendedAttributeUsage().entrySet()) {
                                extendedAttributeUsage.merge(entry.getKey(), entry.getValue(), Long::sum);
                        }
                }

                // Calculate overall active/inactive counts
                long activeIdentities = 0;
                long inactiveIdentities = 0;

                for (IdentityStatisticsDto.IdentityTypeStatistics typeStats : statisticsByType.values()) {
                        activeIdentities += typeStats.getActiveCount();
                        inactiveIdentities += typeStats.getInactiveCount() + typeStats.getSuspendedCount() +
                                        typeStats.getTerminatedCount() + typeStats.getExpiredCount() +
                                        typeStats.getArchivedCount() + typeStats.getPendingCount();
                }

                return new IdentityStatisticsDto(
                                totalIdentities,
                                identitiesByType,
                                statisticsByType,
                                totalExtendedAttributes,
                                extendedAttributeUsage,
                                activeIdentities,
                                inactiveIdentities);
        }

        @Override
        @Transactional(readOnly = true)
        public IdentityStatisticsDto.IdentityTypeStatistics getIdentityTypeStatistics(String tenantId,
                        IdentityType identityType) {
                logger.info().message("Calculating statistics for specific identity type.")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                if (tenantId == null || tenantId.trim().isEmpty()) {
                        throw new IllegalArgumentException("Tenant ID cannot be null or empty");
                }
                if (identityType == null) {
                        throw new IllegalArgumentException("Identity type cannot be null");
                }

                return calculateTypeStatistics(tenantId, identityType);
        }

        /**
         * Calculate detailed statistics for a specific identity type.
         */
        private IdentityStatisticsDto.IdentityTypeStatistics calculateTypeStatistics(String tenantId,
                        IdentityType identityType) {
                logger.debug().message("Calculating statistics for identity type.")
                                .detail("tenantId", tenantId)
                                .detail("identityType", identityType)
                                .log();

                long totalCount = identityRepository.countByTenantIdAndIdentityType(tenantId, identityType);

                // Use efficient repository methods for status counting
                long activeCount = identityRepository.countByTenantIdAndIdentityTypeAndStatus(tenantId, identityType,
                                IdentityStatus.ACTIVE);
                long inactiveCount = identityRepository.countByTenantIdAndIdentityTypeAndStatus(tenantId, identityType,
                                IdentityStatus.INACTIVE);
                long suspendedCount = identityRepository.countByTenantIdAndIdentityTypeAndStatus(tenantId, identityType,
                                IdentityStatus.SUSPENDED);
                long terminatedCount = identityRepository.countByTenantIdAndIdentityTypeAndStatus(tenantId,
                                identityType, IdentityStatus.TERMINATED);
                long pendingCount = identityRepository.countByTenantIdAndIdentityTypeAndStatus(tenantId, identityType,
                                IdentityStatus.PENDING);
                long expiredCount = identityRepository.countByTenantIdAndIdentityTypeAndStatus(tenantId, identityType,
                                IdentityStatus.EXPIRED);
                long archivedCount = identityRepository.countByTenantIdAndIdentityTypeAndStatus(tenantId, identityType,
                                IdentityStatus.ARCHIVED);

                // Calculate extended attributes for this type
                long extendedAttributesCount = 0;
                Map<String, Long> extendedAttributeUsage = new HashMap<>();

                try {
                        // Get all extended attribute definitions for this identity type and tenant
                        List<ExtendedAttributeDefinition> definitions = extendedAttributeDefinitionRepository
                                        .findByTenantIdAndIdentityType(tenantId, identityType);

                        // Count usage for each definition across all value repositories
                        for (ExtendedAttributeDefinition definition : definitions) {
                                UUID definitionId = definition.getId();
                                String definitionName = definition.getDisplayName() != null
                                                ? definition.getDisplayName()
                                                : definition.getSystemKey();

                                // Count values across all data type repositories
                                long stringCount = extAttrStringValueRepository.countByDefinitionId(definitionId);
                                long booleanCount = extAttrBooleanValueRepository.countByDefinitionId(definitionId);
                                long numberCount = extAttrNumberValueRepository.countByDefinitionId(definitionId);
                                long dateCount = extAttrDateValueRepository.countByDefinitionId(definitionId);
                                long objectCount = extAttrObjectValueRepository.countByDefinitionId(definitionId);
                                long arrayCount = extAttrArrayValueRepository.countByDefinitionId(definitionId);

                                // Sum all counts for this definition
                                long totalUsage = stringCount + booleanCount + numberCount +
                                                dateCount + objectCount + arrayCount;

                                if (totalUsage > 0) {
                                        extendedAttributeUsage.put(definitionName, totalUsage);
                                        extendedAttributesCount += totalUsage;
                                }
                        }

                        logger.debug().message("Calculated extended attribute statistics for identity type.")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .detail("definitionsCount", definitions.size())
                                        .detail("totalExtendedAttributes", extendedAttributesCount)
                                        .log();

                } catch (Exception e) {
                        logger.warn().message("Failed to calculate extended attribute statistics.")
                                        .detail("tenantId", tenantId)
                                        .detail("identityType", identityType)
                                        .detail("error", e.getMessage())
                                        .log();
                        // Continue with zero counts if extended attribute calculation fails
                }

                return new IdentityStatisticsDto.IdentityTypeStatistics(
                                totalCount, activeCount, inactiveCount, suspendedCount, terminatedCount,
                                pendingCount, expiredCount, archivedCount, extendedAttributesCount,
                                extendedAttributeUsage);
        }

        // I assume a list of different 'data' per tenantId and resourceId at a time
        @Override
        @Transactional
        public IdentityResponseDto processAuthoritativeData(String tenantId, String resourceId, String sourceName,
                        List<Map<String, Object>> data, IdentityType identityType) {
                logger.info().message("Processing authoritative data for identity.")
                                .detail("tenantId", tenantId)
                                .detail("resourceId", resourceId)
                                .detail("sourceName", sourceName)
                                .detail("identityType", identityType)
                                .log();
                List<Shard> stagedShards = stageDataAsShards(tenantId, resourceId,
                                sourceName, data);
                return null;
        }

        // this just saves the shard before correlation
        private List<Shard> stageDataAsShards(String tenantId, String resourceId, String resourceName,
                        List<Map<String, Object>> data) {
                List<Shard> shards = new ArrayList<>();
                for (Map<String, Object> entry : data) {
                        String objectUid = (String) entry.get("objectUid");
                        if (objectUid == null || objectUid.trim().isEmpty()) {
                                logger.warn().message("Skipping data entry with missing objectUid.")
                                                .detail("tenantId", tenantId)
                                                .detail("resourceId", resourceId)
                                                .detail("entry", entry)
                                                .log();
                                continue; // skip entries without objectUid
                        }

                        Shard shard = new Shard();
                        shard.setTenantId(tenantId);
                        shard.setResourceId(resourceId);
                        shard.setResourceName(resourceName);
                        shard.setKind((String) entry.getOrDefault("kind", "generic")); // I set these defaults for now
                        shard.setIntent((String) entry.getOrDefault("intent", "default"));
                        shard.setObjectClass((String) entry.get("objectClass"));
                        shard.setObjectUid(objectUid);
                        shard.setShardAttributes(objectMapper.valueToTree(entry)); // to JsonNode
                        shard.setStatus(ShardStatus.PENDING_CORRLATION);

                        shards.add(shard);
                }
                return shardRepository.saveAll(shards);
        }

        private void processShardCorrelation(Shard shard, boolean isAuthoritative) {
                List<Identity> matches = findIdentityMatchesByExpression(shard);
                if (matches.size() > 1) {
                        shard.setStatus(ShardStatus.DISPUTED);
                        shard.setCorrelatedIdentity(null);
                        shard.setUpdatedAt(Instant.now());
                        shardRepository.save(shard);
                }

                else if (matches.isEmpty()) {
                        if (isAuthoritative) {
                                logger.info().message(
                                                "No identity match found for authoritative shard, creating new identity.")
                                                .detail("tenantId", shard.getTenantId())
                                                .detail("resourceId", shard.getResourceId())
                                                .detail("objectUid", shard.getObjectUid())
                                                .log();
                        } else {
                                shard.setStatus(ShardStatus.UNMATCHED); // I think we may have this as a configuration?
                                shard.setCorrelatedIdentity(null);
                                shard.setUpdatedAt(Instant.now());
                                shardRepository.save(shard);
                        }
                }
        }

        // this is correlation by the logical expression defined per resource
        private List<Identity> findIdentityMatchesByExpression(Shard shard) {
                return new ArrayList<>();
        }

        // private void validateRequiredAttributesForCreation(String tenantId,
        // IdentityType identityType,
        // Map<String, Object> data) {
        // IdentitySchemaResponseDto schema =
        // identitySchemaService.getIdentitySchema(tenantId,
        // identityType);

        // // required core attributes
        // List<String> requiredAttributes = new ArrayList<>();
        // schema.coreAttributes().stream().filter(CoreAttributeDto::isRequired)
        // .map(CoreAttributeDto::systemKey).forEach(requiredAttributes::add);

        // // required extended attributes
        // schema.extendedAttributes().stream().filter(ExtendedAttributeDto::isRequired)
        // .map(ExtendedAttributeDto::systemKey)
        // .forEach(requiredAttributes::add);

        // List<String> missingAttributes = requiredAttributes.stream().filter(attr ->
        // !data.containsKey(attr)
        // || data.get(attr) == null || data.get(attr).toString().trim().isEmpty())
        // .collect(Collectors.toList());
        // if (!missingAttributes.isEmpty()) {
        // logger.warn().message("Missing required attributes for identity creation.")
        // .detail("tenantId", tenantId)
        // .detail("identityType", identityType)
        // .detail("missingAttributes", missingAttributes)
        // .log();
        // // probably move to dlq topic for manual inspection
        // throw new IllegalArgumentException("Missing required attributes: " +
        // missingAttributes);
        // }
        // }

        @Override
        @Transactional(readOnly = true)
        public Page<IdentityBusinessRoleCardDto> getBusinessRolesByIdentity(
                        String tenantId, UUID identityId, String search, Pageable pageable) {
                logger.info().message("Fetching business roles for identity")
                                .detail("tenantId", tenantId)
                                .detail("identityId", identityId)
                                .detail("search", search)
                                .log();

                // 1. Validate identity belongs to tenant
                identityRepository.findByTenantIdAndId(tenantId, identityId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Identity not found: " + identityId));

                // 2. Get all ACTIVE role assignments for this identity
                List<IdentityRoleAssignment> assignments =
                                identityRoleAssignmentRepository.findByTenantIdAndIdentityIdAndStatus(
                                                tenantId, identityId, IdentityRoleAssignment.Status.ACTIVE);

                if (assignments.isEmpty()) {
                        return new PageImpl<>(List.of(), pageable, 0);
                }

                // 3. Batch-load all assigned AccessRole entities
                List<UUID> roleIds = assignments.stream()
                                .map(IdentityRoleAssignment::getRoleId)
                                .distinct()
                                .collect(Collectors.toList());

                Map<UUID, AccessRole> roleMap = accessRoleRepository.findByIdIn(roleIds).stream()
                                .filter(r -> tenantId.equals(r.getTenantId()))
                                .collect(Collectors.toMap(AccessRole::getId, r -> r));

                // 4. Build cards — one card per distinct role (pick earliest assignment)
                Map<UUID, IdentityRoleAssignment> bestAssignment = new java.util.LinkedHashMap<>();
                for (IdentityRoleAssignment a : assignments) {
                        bestAssignment.merge(a.getRoleId(), a, (existing, candidate) ->
                                (candidate.getAssignedAt() != null && existing.getAssignedAt() != null
                                        && candidate.getAssignedAt() < existing.getAssignedAt())
                                        ? candidate : existing);
                }

                List<IdentityBusinessRoleCardDto> cards = bestAssignment.entrySet().stream()
                                .filter(e -> roleMap.containsKey(e.getKey()))
                                .map(e -> {
                                        AccessRole role = roleMap.get(e.getKey());
                                        IdentityRoleAssignment a = e.getValue();
                                        return new IdentityBusinessRoleCardDto(
                                                role.getId(),
                                                role.getDisplayName() != null ? role.getDisplayName() : role.getName(),
                                                role.getType(),
                                                role.getStatus(),
                                                role.getRiskLevel(),
                                                role.getBusinessUnit(),
                                                role.getDescription(),
                                                a.getSourceType() != null ? a.getSourceType().name() : null,
                                                a.getAssignedAt());
                                })
                                .collect(Collectors.toList());

                // 5. Apply search filter on role name and description
                if (search != null && !search.isBlank()) {
                        String lowerSearch = search.toLowerCase(java.util.Locale.ROOT);
                        cards = cards.stream()
                                .filter(c -> matchesCardSearch(c, lowerSearch))
                                .collect(Collectors.toList());
                }

                // 6. Paginate in-memory
                int total = cards.size();
                int start = (int) pageable.getOffset();
                int end = Math.min(start + pageable.getPageSize(), total);
                List<IdentityBusinessRoleCardDto> pageContent =
                                start >= total ? List.of() : cards.subList(start, end);

                return new PageImpl<>(pageContent, pageable, total);
        }

        private boolean matchesCardSearch(IdentityBusinessRoleCardDto card, String lowerSearch) {
                if (card.roleName() != null && card.roleName().toLowerCase(java.util.Locale.ROOT).contains(lowerSearch)) {
                        return true;
                }
                if (card.description() != null && card.description().toLowerCase(java.util.Locale.ROOT).contains(lowerSearch)) {
                        return true;
                }
                if (card.roleType() != null && card.roleType().toLowerCase(java.util.Locale.ROOT).contains(lowerSearch)) {
                        return true;
                }
                return false;
        }

}

