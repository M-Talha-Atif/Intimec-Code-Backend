package com.aidentx.iam.identity_management.service.strategy;

import com.aidentx.iam.exception.DuplicateIdentityException;
import com.aidentx.iam.identity_management.model.dto.NonEmployeeCreateDto;
import com.aidentx.iam.identity_management.model.dto.NonEmployeeUpdateDto;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonEmployee;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.HumanIdentityRepository;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.identity_management.repository.NonEmployeeRepository;
import com.aidentx.iam.identity_management.service.ExtendedAttributeService;
import com.aidentx.iam.identity_management.service.IdentityLifecycleStateMachine;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Strategy for creating and updating NonEmployee identities.
 */
@Component
public class NonEmployeeCreationStrategy implements IdentityCreationStrategy {

    private final IUnifiedLogger logger;
    private final NonEmployeeRepository nonEmployeeRepository;
    private final IdentityRepository identityRepository;
    private final HumanIdentityRepository humanIdentityRepository;
    private final ExtendedAttributeService extendedAttributeService;

    public NonEmployeeCreationStrategy(UnifiedLoggerFactory loggerFactory,
            NonEmployeeRepository nonEmployeeRepository,
            IdentityRepository identityRepository,
            HumanIdentityRepository humanIdentityRepository,
            ExtendedAttributeService extendedAttributeService) {
        this.logger = loggerFactory.getLogger(NonEmployeeCreationStrategy.class);
        this.nonEmployeeRepository = nonEmployeeRepository;
        this.identityRepository = identityRepository;
        this.humanIdentityRepository = humanIdentityRepository;
        this.extendedAttributeService = extendedAttributeService;
    }

    @Override
    public UUID create(String tenantId, Object dto, Map<String, Object> extendedAttributes) {
        if (!(dto instanceof NonEmployeeCreateDto nonEmployeeDto)) {
            throw new IllegalArgumentException("Invalid DTO type for NonEmployee creation");
        }

        logger.info().message("Creating new Non-Employee identity.").detail("username", nonEmployeeDto.username())
                .log();

        // Validate uniqueness using centralized repository
        if (humanIdentityRepository.existsByTenantIdAndUsername(tenantId, nonEmployeeDto.username())) {
            throw new DuplicateIdentityException("Username '" + nonEmployeeDto.username()
                    + "' already exists in tenant '" + tenantId + "'");
        }
        if (humanIdentityRepository.existsByTenantIdAndEmail(tenantId, nonEmployeeDto.email())) {
            throw new DuplicateIdentityException(
                    "Email '" + nonEmployeeDto.email() + "' already exists in tenant '" + tenantId + "'");
        }

        // Validate uniqueness of contract ID within the tenant
        if (nonEmployeeRepository.findByTenantIdAndContractId(tenantId, nonEmployeeDto.contractId()).isPresent()) {
            throw new DuplicateIdentityException("Contract ID '" + nonEmployeeDto.contractId()
                    + "' already exists in tenant '" + tenantId + "'");
        }

        NonEmployee nonEmployee = new NonEmployee();
        nonEmployee.setTenantId(tenantId);
        nonEmployee.setUsername(nonEmployeeDto.username());
        nonEmployee.setEmail(nonEmployeeDto.email());
        nonEmployee.setPasswordHash(nonEmployeeDto.password()); // TODO: Hash password
        nonEmployee.setFirstName(nonEmployeeDto.firstName());
        nonEmployee.setLastName(nonEmployeeDto.lastName());
        nonEmployee.setDisplayName(nonEmployeeDto.firstName() + " " + nonEmployeeDto.lastName());
        nonEmployee.setStatus(nonEmployeeDto.status() != null ? nonEmployeeDto.status() : nonEmployee.getStatus());

        nonEmployee.setContractId(nonEmployeeDto.contractId());
        nonEmployee.setVendorName(nonEmployeeDto.vendorName());
        nonEmployee.setRiskLevel(nonEmployeeDto.riskLevel());
        nonEmployee.setContractEndDate(nonEmployeeDto.contractEndDate());

        if (nonEmployeeDto.sponsorId() != null) {
            identityRepository.findByTenantIdAndId(tenantId, nonEmployeeDto.sponsorId())
                    .ifPresent(nonEmployee::setSponsor);
        }

        NonEmployee savedNonEmployee = nonEmployeeRepository.save(nonEmployee);

        extendedAttributeService.saveExtendedAttributesForCreation(savedNonEmployee.getId(), tenantId,
                IdentityType.NON_EMPLOYEE, extendedAttributes);

        // Return the created identity - the full mapping will be done by the service
        return savedNonEmployee.getId();
    }

    @Override
    public void update(Identity identity, Object dto, Map<String, Object> extendedAttributes) {
        if (!(identity instanceof NonEmployee nonEmployee)) {
            throw new IllegalArgumentException("Invalid identity type for NonEmployee update");
        }

        if (!(dto instanceof NonEmployeeUpdateDto nonEmployeeDto)) {
            throw new IllegalArgumentException("Invalid DTO type for NonEmployee update");
        }

        // Update fields
        if (nonEmployeeDto.email() != null)
            nonEmployee.setEmail(nonEmployeeDto.email());
        if (nonEmployeeDto.username() != null)
            nonEmployee.setUsername(nonEmployeeDto.username());
        if (nonEmployeeDto.password() != null)
            nonEmployee.setPasswordHash(nonEmployeeDto.password());
        if (nonEmployeeDto.firstName() != null)
            nonEmployee.setFirstName(nonEmployeeDto.firstName());
        if (nonEmployeeDto.lastName() != null)
            nonEmployee.setLastName(nonEmployeeDto.lastName());
        if (nonEmployeeDto.contractId() != null)
            nonEmployee.setContractId(nonEmployeeDto.contractId());
        if (nonEmployeeDto.vendorName() != null)
            nonEmployee.setVendorName(nonEmployeeDto.vendorName());
        if (nonEmployeeDto.riskLevel() != null)
            nonEmployee.setRiskLevel(nonEmployeeDto.riskLevel());
        if (nonEmployeeDto.contractEndDate() != null)
            nonEmployee.setContractEndDate(nonEmployeeDto.contractEndDate());
        if (nonEmployeeDto.status() != null) {
            IdentityLifecycleStateMachine.validateTransition(nonEmployee.getStatus(), nonEmployeeDto.status());
            nonEmployee.setStatus(nonEmployeeDto.status());
        }

        // Update display name if first or last name changed
        if (nonEmployeeDto.firstName() != null || nonEmployeeDto.lastName() != null) {
            String firstName = nonEmployeeDto.firstName() != null ? nonEmployeeDto.firstName()
                    : nonEmployee.getFirstName();
            String lastName = nonEmployeeDto.lastName() != null ? nonEmployeeDto.lastName() : nonEmployee.getLastName();
            nonEmployee.setDisplayName(firstName + " " + lastName);
        }

        // Update sponsor if provided
        if (nonEmployeeDto.sponsorId() != null) {
            identityRepository.findByTenantIdAndId(nonEmployee.getTenantId(), nonEmployeeDto.sponsorId())
                    .ifPresent(nonEmployee::setSponsor);
        }

        nonEmployeeRepository.save(nonEmployee);

        if (extendedAttributes != null) {
            extendedAttributeService.validateExtendedAttributeUpdate(nonEmployee.getTenantId(),
                    IdentityType.NON_EMPLOYEE, extendedAttributes);
            extendedAttributeService.saveExtendedAttributesForUpdate(nonEmployee.getId(), nonEmployee.getTenantId(),
                    IdentityType.NON_EMPLOYEE, extendedAttributes);
        }
    }

    @Override
    public IdentityType getIdentityType() {
        return IdentityType.NON_EMPLOYEE;
    }
}
