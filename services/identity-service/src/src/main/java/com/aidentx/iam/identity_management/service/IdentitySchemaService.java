package com.aidentx.iam.identity_management.service;

import com.aidentx.iam.identity_management.model.dto.ExtendedAttributeDtos.DefinitionResponseDto;
import com.aidentx.iam.identity_management.model.dto.IdentitySchemaDtos;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.service.CoreAttributeMetadataService.CoreAttributeMetadata;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for providing combined schema information (core + extended
 * attributes) for identity types.
 */
@Service
public class IdentitySchemaService {

    private final IUnifiedLogger logger;
    private final CoreAttributeMetadataService coreAttributeService;
    private final ExtendedAttributeDefinitionService extendedAttributeService;

    public IdentitySchemaService(UnifiedLoggerFactory loggerFactory,
            CoreAttributeMetadataService coreAttributeService,
            ExtendedAttributeDefinitionService extendedAttributeService) {
        this.logger = loggerFactory.getLogger(IdentitySchemaService.class);
        this.coreAttributeService = coreAttributeService;
        this.extendedAttributeService = extendedAttributeService;
    }

    /**
     * Gets the complete schema for EMPLOYEE identity type including both core and
     * extended attributes.
     *
     * @param tenantId The tenant ID
     * @return Combined schema with core and extended attributes
     */
    public IdentitySchemaDtos.IdentitySchemaResponseDto getEmployeeIdentitySchema(String tenantId) {
        return getIdentitySchemaInternal(tenantId, IdentityType.EMPLOYEE);
    }

    /**
     * Gets the complete schema for NON_EMPLOYEE identity type including both core
     * and extended attributes.
     *
     * @param tenantId The tenant ID
     * @return Combined schema with core and extended attributes
     */
    public IdentitySchemaDtos.IdentitySchemaResponseDto getNonEmployeeIdentitySchema(String tenantId) {
        return getIdentitySchemaInternal(tenantId, IdentityType.NON_EMPLOYEE);
    }

    /**
     * Gets the complete schema for NON_HUMAN identity type including both core and
     * extended attributes.
     *
     * @param tenantId The tenant ID
     * @return Combined schema with core and extended attributes
     */
    public IdentitySchemaDtos.IdentitySchemaResponseDto getNonHumanIdentitySchema(String tenantId) {
        return getIdentitySchemaInternal(tenantId, IdentityType.NON_HUMAN);
    }

    private IdentitySchemaDtos.IdentitySchemaResponseDto getIdentitySchemaInternal(String tenantId,
            IdentityType identityType) {
        logger.info().message("Retrieving identity schema")
                .detail("tenantId", tenantId)
                .detail("identityType", identityType.getValue())
                .log();

        // Get core attributes
        List<CoreAttributeMetadata> coreAttributes = coreAttributeService
                .getCoreAttributesForIdentityType(identityType);
        List<IdentitySchemaDtos.CoreAttributeDto> coreAttributeDtos = coreAttributes.stream()
                .map(IdentitySchemaDtos.CoreAttributeDto::from)
                .collect(Collectors.toList());

        // Get extended attributes
        List<DefinitionResponseDto> extendedDefinitions;
        switch (identityType) {
            case EMPLOYEE -> extendedDefinitions = extendedAttributeService.findEmployeeDefinitions(tenantId);
            case NON_EMPLOYEE -> extendedDefinitions = extendedAttributeService.findNonEmployeeDefinitions(tenantId);
            case NON_HUMAN -> extendedDefinitions = extendedAttributeService.findNonHumanDefinitions(tenantId);
            default -> throw new IllegalArgumentException("Unsupported identity type: " + identityType);
        }
        List<IdentitySchemaDtos.ExtendedAttributeDto> extendedAttributeDtos = extendedDefinitions.stream()
                .map(this::convertToExtendedAttributeDto)
                .collect(Collectors.toList());

        return new IdentitySchemaDtos.IdentitySchemaResponseDto(coreAttributeDtos, extendedAttributeDtos);
    }

    private IdentitySchemaDtos.ExtendedAttributeDto convertToExtendedAttributeDto(DefinitionResponseDto definition) {
        return new IdentitySchemaDtos.ExtendedAttributeDto(
                definition.systemKey(),
                definition.displayName(),
                definition.dataType(),
                definition.description(),
                definition.displayOrder(),
                definition.isRequired(),
                definition.isUnique(),
                definition.isSensitive(),
                definition.isWritable(),
                definition.lifecycleState(),
                definition.defaultValue());
    }
}
