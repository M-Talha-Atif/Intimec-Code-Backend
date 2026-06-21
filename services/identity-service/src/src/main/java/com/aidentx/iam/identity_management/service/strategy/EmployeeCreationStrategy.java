package com.aidentx.iam.identity_management.service.strategy;

import com.aidentx.iam.events.IdentityEventPublisher;
import com.aidentx.iam.exception.DuplicateIdentityException;
import com.aidentx.iam.identity_management.model.dto.EmployeeCreateDto;
import com.aidentx.iam.identity_management.model.dto.EmployeeUpdateDto;
import com.aidentx.iam.identity_management.model.entity.Employee;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.EmployeeRepository;
import com.aidentx.iam.identity_management.repository.HumanIdentityRepository;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.identity_management.service.ExtendedAttributeService;
import com.aidentx.iam.identity_management.service.IdentityLifecycleStateMachine;
import com.aidentx.iam.organization_management.model.enums.AssignmentType;
import com.aidentx.iam.organization_management.service.IdentityOrgSyncService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Strategy for creating and updating Employee identities.
 */
@Component
public class EmployeeCreationStrategy implements IdentityCreationStrategy {

    private final IUnifiedLogger logger;
    private final EmployeeRepository employeeRepository;
    private final IdentityRepository identityRepository;
    private final HumanIdentityRepository humanIdentityRepository;
    private final ExtendedAttributeService extendedAttributeService;
    private final IdentityEventPublisher identityEventPublisher;
    private final IdentityOrgSyncService identityOrgSyncService;

    public EmployeeCreationStrategy(UnifiedLoggerFactory loggerFactory,
            EmployeeRepository employeeRepository,
            IdentityRepository identityRepository,
            HumanIdentityRepository humanIdentityRepository,
            ExtendedAttributeService extendedAttributeService,
            IdentityEventPublisher identityEventPublisher,
            IdentityOrgSyncService identityOrgSyncService) {
        this.logger = loggerFactory.getLogger(EmployeeCreationStrategy.class);
        this.employeeRepository = employeeRepository;
        this.identityRepository = identityRepository;
        this.humanIdentityRepository = humanIdentityRepository;
        this.extendedAttributeService = extendedAttributeService;
        this.identityEventPublisher = identityEventPublisher;
        this.identityOrgSyncService = identityOrgSyncService;
    }

    @Override
    public UUID create(String tenantId, Object dto, Map<String, Object> extendedAttributes) {
        if (!(dto instanceof EmployeeCreateDto employeeDto)) {
            throw new IllegalArgumentException("Invalid DTO type for Employee creation");
        }

        logger.info().message("Creating new Employee identity.").detail("username", employeeDto.username()).log();

        // Validate uniqueness using centralized repository
        if (humanIdentityRepository.existsByTenantIdAndUsername(tenantId, employeeDto.username())) {
            throw new DuplicateIdentityException("Username '" + employeeDto.username()
                    + "' already exists in tenant '" + tenantId + "'");
        }
        if (humanIdentityRepository.existsByTenantIdAndEmail(tenantId, employeeDto.email())) {
            throw new DuplicateIdentityException(
                    "Email '" + employeeDto.email() + "' already exists in tenant '" + tenantId + "'");
        }

        // Validate uniqueness of employee ID within the tenant
        if (employeeRepository.findByTenantIdAndEmployeeId(tenantId, employeeDto.employeeId()).isPresent()) {
            throw new DuplicateIdentityException("Employee ID '" + employeeDto.employeeId()
                    + "' already exists in tenant '" + tenantId + "'");
        }

        Employee employee = new Employee();
        employee.setTenantId(tenantId);
        employee.setUsername(employeeDto.username());
        employee.setEmail(employeeDto.email());
        employee.setPasswordHash(employeeDto.password()); // Assume password is pre-hashed
        employee.setFirstName(employeeDto.firstName());
        employee.setLastName(employeeDto.lastName());
        employee.setDisplayName(employeeDto.firstName() + " " + employeeDto.lastName());
        employee.setStatus(employeeDto.status() != null ? employeeDto.status() : employee.getStatus());
        employee.setIdentityType(IdentityType.EMPLOYEE); // added by talha
        employee.setEmployeeId(employeeDto.employeeId());
        employee.setJobTitle(employeeDto.jobTitle());
        employee.setDepartment(employeeDto.department());
        employee.setRiskLevel(employeeDto.riskLevel());
        employee.setHireDate(employeeDto.hireDate());

        if (employeeDto.managerId() != null) {
            identityRepository.findByTenantIdAndId(tenantId, employeeDto.managerId())
                    .ifPresent(employee::setManager);
        }

        Employee savedEmployee = employeeRepository.save(employee);

        extendedAttributeService.saveExtendedAttributesForCreation(savedEmployee.getId(), tenantId,
                IdentityType.EMPLOYEE, extendedAttributes);

        identityEventPublisher.publishIdentityCreatedEvent(
                savedEmployee,
                "SYSTEM" // will pass created by after configuration of controller function
        );

        // Phase 2: best-effort sync of department string -> org-unit assignment.
        identityOrgSyncService.syncPrimaryFunctionalAssignment(
                tenantId, savedEmployee.getId(), employeeDto.department(), "SYSTEM",
                AssignmentType.HR_OF_RECORD);

        // Return the created identity - the full mapping will be done by the service
        return savedEmployee.getId();
    }



    @Override
    public void update(Identity identity, Object dto, Map<String, Object> extendedAttributes) {
        if (!(identity instanceof Employee employee)) {
            throw new IllegalArgumentException("Invalid identity type for Employee update");
        }

        if (!(dto instanceof EmployeeUpdateDto employeeDto)) {
            throw new IllegalArgumentException("Invalid DTO type for Employee update");
        }

        // Update fields
        if (employeeDto.email() != null)
            employee.setEmail(employeeDto.email());
        if (employeeDto.username() != null)
            employee.setUsername(employeeDto.username());
        if (employeeDto.password() != null)
            employee.setPasswordHash(employeeDto.password());
        if (employeeDto.firstName() != null)
            employee.setFirstName(employeeDto.firstName());
        if (employeeDto.lastName() != null)
            employee.setLastName(employeeDto.lastName());
        if (employeeDto.employeeId() != null)
            employee.setEmployeeId(employeeDto.employeeId());
        if (employeeDto.jobTitle() != null)
            employee.setJobTitle(employeeDto.jobTitle());
        if (employeeDto.department() != null)
            employee.setDepartment(employeeDto.department());
        if (employeeDto.riskLevel() != null)
            employee.setRiskLevel(employeeDto.riskLevel());
        if (employeeDto.hireDate() != null)
            employee.setHireDate(employeeDto.hireDate());
        if (employeeDto.status() != null) {
            IdentityLifecycleStateMachine.validateTransition(employee.getStatus(), employeeDto.status());
            employee.setStatus(employeeDto.status());
        }

        // Update display name if first or last name changed
        if (employeeDto.firstName() != null || employeeDto.lastName() != null) {
            String firstName = employeeDto.firstName() != null ? employeeDto.firstName() : employee.getFirstName();
            String lastName = employeeDto.lastName() != null ? employeeDto.lastName() : employee.getLastName();
            employee.setDisplayName(firstName + " " + lastName);
        }

        // Update manager if provided
        if (employeeDto.managerId() != null) {
            identityRepository.findByTenantIdAndId(employee.getTenantId(), employeeDto.managerId())
                    .ifPresent(employee::setManager);
        }

        employeeRepository.save(employee);

        // Phase 2: keep org-unit assignment in sync if department changed.
        // This is a governance-driven move (IGA administrator action), not an HR feed update.
        if (employeeDto.department() != null) {
            identityOrgSyncService.syncPrimaryFunctionalAssignment(
                    employee.getTenantId(), employee.getId(), employeeDto.department(), "SYSTEM",
                    AssignmentType.GOVERNANCE_MOVER);
        }

        if (extendedAttributes != null) {
            extendedAttributeService.validateExtendedAttributeUpdate(employee.getTenantId(), IdentityType.EMPLOYEE,
                    extendedAttributes);
            extendedAttributeService.saveExtendedAttributesForUpdate(employee.getId(), employee.getTenantId(),
                    IdentityType.EMPLOYEE, extendedAttributes);
        }
    }

    @Override
    public IdentityType getIdentityType() {
        return IdentityType.EMPLOYEE;
    }
}
