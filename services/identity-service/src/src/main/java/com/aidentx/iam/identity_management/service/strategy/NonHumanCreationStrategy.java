package com.aidentx.iam.identity_management.service.strategy;

import com.aidentx.iam.exception.DuplicateIdentityException;
import com.aidentx.iam.identity_management.model.dto.NonHumanCreateDto;
import com.aidentx.iam.identity_management.model.dto.NonHumanUpdateDto;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.entity.NonHumanIdentity;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.repository.NonHumanIdentityRepository;
import com.aidentx.iam.identity_management.service.ExtendedAttributeService;
import com.aidentx.iam.identity_management.service.IdentityLifecycleStateMachine;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Strategy for creating and updating NonHumanIdentity entities.
 */
@Component
public class NonHumanCreationStrategy implements IdentityCreationStrategy {

    private final IUnifiedLogger logger;
    private final NonHumanIdentityRepository nonHumanIdentityRepository;
    private final ExtendedAttributeService extendedAttributeService;

    public NonHumanCreationStrategy(UnifiedLoggerFactory loggerFactory,
            NonHumanIdentityRepository nonHumanIdentityRepository,
            ExtendedAttributeService extendedAttributeService) {
        this.logger = loggerFactory.getLogger(NonHumanCreationStrategy.class);
        this.nonHumanIdentityRepository = nonHumanIdentityRepository;
        this.extendedAttributeService = extendedAttributeService;
    }

    @Override
    public UUID create(String tenantId, Object dto, Map<String, Object> extendedAttributes) {
        if (!(dto instanceof NonHumanCreateDto nonHumanDto)) {
            throw new IllegalArgumentException("Invalid DTO type for NonHuman creation");
        }

        logger.info().message("Creating new Non-Human identity.").detail("serviceName", nonHumanDto.serviceName())
                .log();

        // Validate uniqueness of service name within the tenant
        if (nonHumanIdentityRepository.findByTenantIdAndServiceName(tenantId, nonHumanDto.serviceName()).isPresent()) {
            throw new DuplicateIdentityException("Service name '" + nonHumanDto.serviceName()
                    + "' already exists in tenant '" + tenantId + "'");
        }

        NonHumanIdentity nonHuman = new NonHumanIdentity();
        nonHuman.setTenantId(tenantId);
        nonHuman.setStatus(nonHumanDto.status() != null ? nonHumanDto.status() : nonHuman.getStatus());

        nonHuman.setServiceName(nonHumanDto.serviceName());
        nonHuman.setDescription(nonHumanDto.description());
        nonHuman.setOwnerTeam(nonHumanDto.ownerTeam());

        NonHumanIdentity savedNonHuman = nonHumanIdentityRepository.save(nonHuman);

        extendedAttributeService.saveExtendedAttributesForCreation(savedNonHuman.getId(), tenantId,
                IdentityType.NON_HUMAN, extendedAttributes);

        // Return the created identity - the full mapping will be done by the service
        return savedNonHuman.getId();
    }

    @Override
    public void update(Identity identity, Object dto, Map<String, Object> extendedAttributes) {
        if (!(identity instanceof NonHumanIdentity nonHuman)) {
            throw new IllegalArgumentException("Invalid identity type for NonHuman update");
        }

        if (!(dto instanceof NonHumanUpdateDto nonHumanDto)) {
            throw new IllegalArgumentException("Invalid DTO type for NonHuman update");
        }

        // Update fields
        if (nonHumanDto.serviceName() != null)
            nonHuman.setServiceName(nonHumanDto.serviceName());
        if (nonHumanDto.description() != null)
            nonHuman.setDescription(nonHumanDto.description());
        if (nonHumanDto.ownerTeam() != null)
            nonHuman.setOwnerTeam(nonHumanDto.ownerTeam());
        if (nonHumanDto.status() != null) {
            IdentityLifecycleStateMachine.validateTransition(nonHuman.getStatus(), nonHumanDto.status());
            nonHuman.setStatus(nonHumanDto.status());
        }

        nonHumanIdentityRepository.save(nonHuman);

        if (extendedAttributes != null) {
            extendedAttributeService.validateExtendedAttributeUpdate(nonHuman.getTenantId(), IdentityType.NON_HUMAN,
                    extendedAttributes);
            extendedAttributeService.saveExtendedAttributesForUpdate(nonHuman.getId(), nonHuman.getTenantId(),
                    IdentityType.NON_HUMAN, extendedAttributes);
        }
    }

    @Override
    public IdentityType getIdentityType() {
        return IdentityType.NON_HUMAN;
    }
}
