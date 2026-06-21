package com.aidentx.iam.identity_management.service.impl;

import com.aidentx.iam.identity_management.model.dto.ExtendedAttributeDtos.*;
import com.aidentx.iam.identity_management.model.entity.ExtAttrArrayValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrBooleanValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrDateValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrNumberValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrObjectValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrStringValue;
import com.aidentx.iam.identity_management.model.entity.ExtendedAttributeDefinition;
import com.aidentx.iam.identity_management.model.entity.Identity;
import com.aidentx.iam.identity_management.model.enums.DataType;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.model.enums.SchemaLifecycleState;
import com.aidentx.iam.identity_management.repository.ExtAttrArrayValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrBooleanValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrDateValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrNumberValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrObjectValueRepository;
import com.aidentx.iam.identity_management.repository.ExtAttrStringValueRepository;
import com.aidentx.iam.identity_management.repository.ExtendedAttributeDefinitionRepository;
import com.aidentx.iam.identity_management.repository.IdentityRepository;
import com.aidentx.iam.identity_management.service.CoreAttributeMetadataService;
import com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ExtendedAttributeDefinitionServiceImpl implements ExtendedAttributeDefinitionService {

    private final ExtendedAttributeDefinitionRepository definitionRepository;
    private final IdentityRepository identityRepository;
    private final ExtAttrStringValueRepository stringValueRepository;
    private final ExtAttrNumberValueRepository numberValueRepository;
    private final ExtAttrBooleanValueRepository booleanValueRepository;
    private final ExtAttrDateValueRepository dateValueRepository;
    private final ExtAttrArrayValueRepository arrayValueRepository;
    private final ExtAttrObjectValueRepository objectValueRepository;
    private final CoreAttributeMetadataService coreAttributeService;
    private final ObjectMapper objectMapper;

    public ExtendedAttributeDefinitionServiceImpl(ExtendedAttributeDefinitionRepository definitionRepository,
            IdentityRepository identityRepository,
            ExtAttrStringValueRepository stringValueRepository,
            ExtAttrNumberValueRepository numberValueRepository,
            ExtAttrBooleanValueRepository booleanValueRepository,
            ExtAttrDateValueRepository dateValueRepository,
            ExtAttrArrayValueRepository arrayValueRepository,
            ExtAttrObjectValueRepository objectValueRepository,
            CoreAttributeMetadataService coreAttributeService,
            ObjectMapper objectMapper) {
        this.definitionRepository = definitionRepository;
        this.identityRepository = identityRepository;
        this.stringValueRepository = stringValueRepository;
        this.numberValueRepository = numberValueRepository;
        this.booleanValueRepository = booleanValueRepository;
        this.dateValueRepository = dateValueRepository;
        this.arrayValueRepository = arrayValueRepository;
        this.objectValueRepository = objectValueRepository;
        this.coreAttributeService = coreAttributeService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DefinitionResponseDto> findEmployeeDefinitions(String tenantId) {
        return findDefinitionsByIdentityType(tenantId, IdentityType.EMPLOYEE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DefinitionResponseDto> findNonEmployeeDefinitions(String tenantId) {
        return findDefinitionsByIdentityType(tenantId, IdentityType.NON_EMPLOYEE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DefinitionResponseDto> findNonHumanDefinitions(String tenantId) {
        return findDefinitionsByIdentityType(tenantId, IdentityType.NON_HUMAN);
    }

    private List<DefinitionResponseDto> findDefinitionsByIdentityType(String tenantId, IdentityType identityType) {
        List<ExtendedAttributeDefinition> definitions = definitionRepository.findByTenantIdAndIdentityType(tenantId,
                identityType);

        return definitions.stream()
                .map(this::mapEntityToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public DefinitionResponseDto createEmployeeDefinition(String tenantId, DefinitionCreateWithoutTypeDto createDto) {
        return createDefinitionInternal(tenantId, IdentityType.EMPLOYEE, createDto);
    }

    @Override
    public DefinitionResponseDto createNonEmployeeDefinition(String tenantId,
            DefinitionCreateWithoutTypeDto createDto) {
        return createDefinitionInternal(tenantId, IdentityType.NON_EMPLOYEE, createDto);
    }

    @Override
    public DefinitionResponseDto createNonHumanDefinition(String tenantId, DefinitionCreateWithoutTypeDto createDto) {
        return createDefinitionInternal(tenantId, IdentityType.NON_HUMAN, createDto);
    }

    private DefinitionResponseDto createDefinitionInternal(String tenantId, IdentityType identityType,
            DefinitionCreateWithoutTypeDto createDto) {
        // Check if a definition with the same systemKey already exists for this tenant
        // and type
        if (definitionRepository.existsByTenantIdAndIdentityTypeAndSystemKey(
                tenantId, identityType, createDto.systemKey())) {
            throw new IllegalArgumentException(
                    "A definition with systemKey '" + createDto.systemKey() +
                            "' already exists for identity type '" + identityType + "' in tenant '"
                            + tenantId + "'");
        }

        // Check for conflicts with core attribute names
        validateNoCoreAttributeConflict(createDto.systemKey(), identityType);

        // Validate if this is a required attribute and existing identities would be
        // affected
        if (createDto.isRequired()) {
            if (createDto.defaultValue() == null) {
                validateRequiredAttributeCreation(tenantId, identityType);
            }
            // Note: Default value application happens after saving the definition
        }

        ExtendedAttributeDefinition newDef = new ExtendedAttributeDefinition();
        newDef.setTenantId(tenantId);
        newDef.setIdentityType(identityType);
        newDef.setSystemKey(createDto.systemKey());
        newDef.setDisplayName(createDto.displayName());
        newDef.setDataType(createDto.dataType());
        newDef.setDescription(createDto.description());
        newDef.setIsRequired(createDto.isRequired() != null ? createDto.isRequired() : false);
        newDef.setIsUnique(createDto.isUnique() != null ? createDto.isUnique() : false);
        newDef.setIsSensitive(createDto.isSensitive() != null ? createDto.isSensitive() : false);
        newDef.setIsWritable(createDto.isWritable() != null ? createDto.isWritable() : true);
        newDef.setLifecycleState(resolveLifecycleState(createDto.lifecycleState()));
        newDef.setDefaultValue(serializeDefaultValue(createDto.defaultValue(), createDto.dataType()));

        ExtendedAttributeDefinition savedDef = definitionRepository.save(newDef);

        // If default value is provided for a required attribute, apply it to existing
        // identities
        if (createDto.isRequired() && createDto.defaultValue() != null) {
            applyDefaultValueToExistingIdentities(tenantId, identityType, savedDef.getId(),
                    createDto.defaultValue(), createDto.dataType());
        }

        return mapEntityToResponseDto(savedDef);
    }

    @Override
    public DefinitionResponseDto updateEmployeeDefinition(String tenantId, String systemKey,
            DefinitionUpdateDto updateDto) {
        return updateDefinitionInternal(tenantId, systemKey, updateDto, IdentityType.EMPLOYEE);
    }

    @Override
    public DefinitionResponseDto updateNonEmployeeDefinition(String tenantId, String systemKey,
            DefinitionUpdateDto updateDto) {
        return updateDefinitionInternal(tenantId, systemKey, updateDto, IdentityType.NON_EMPLOYEE);
    }

    @Override
    public DefinitionResponseDto updateNonHumanDefinition(String tenantId, String systemKey,
            DefinitionUpdateDto updateDto) {
        return updateDefinitionInternal(tenantId, systemKey, updateDto, IdentityType.NON_HUMAN);
    }

    private DefinitionResponseDto updateDefinitionInternal(String tenantId, String systemKey,
            DefinitionUpdateDto updateDto, IdentityType identityType) {
        ExtendedAttributeDefinition existingDef = definitionRepository
                .findByTenantIdAndIdentityTypeAndSystemKey(tenantId, identityType, systemKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Extended attribute definition not found with system key: " + systemKey + " for tenant: "
                                + tenantId + " and identity type: " + identityType));

        // Validate that the system key doesn't conflict with core attributes
        validateNoCoreAttributeConflict(systemKey, existingDef.getIdentityType());

        // Update only non-null fields
        if (updateDto.displayName() != null) {
            existingDef.setDisplayName(updateDto.displayName());
        }
        if (updateDto.description() != null) {
            existingDef.setDescription(updateDto.description());
        }
        if (updateDto.isRequired() != null) {
            // If changing to required, validate existing identities or apply default value
            if (updateDto.isRequired() && !existingDef.getIsRequired()) {
                Object defaultValueToUse = updateDto.defaultValue();
                if (defaultValueToUse == null && existingDef.getDefaultValue() != null) {
                    // Use the existing default value from the definition
                    defaultValueToUse = parseDefaultValue(existingDef.getDefaultValue(), existingDef.getDataType());
                }

                if (defaultValueToUse != null) {
                    // Apply default value to existing identities
                    applyDefaultValueToExistingIdentities(existingDef.getTenantId(), existingDef.getIdentityType(),
                            existingDef.getId(), defaultValueToUse, existingDef.getDataType());
                } else {
                    validateRequiredAttributeCreation(existingDef);
                }
            }
            existingDef.setIsRequired(updateDto.isRequired());
        }
        if (updateDto.isUnique() != null) {
            existingDef.setIsUnique(updateDto.isUnique());
        }
        if (updateDto.isSensitive() != null) {
            existingDef.setIsSensitive(updateDto.isSensitive());
        }
        if (updateDto.isWritable() != null) {
            existingDef.setIsWritable(updateDto.isWritable());
        }
        if (updateDto.lifecycleState() != null) {
            existingDef.setLifecycleState(resolveLifecycleState(updateDto.lifecycleState()));
        }

        // Handle default value updates
        if (updateDto.defaultValue() != null || Boolean.TRUE.equals(updateDto.clearDefaultValue())) {
            Object newDefaultValue = null;
            if (updateDto.defaultValue() != null) {
                newDefaultValue = updateDto.defaultValue();
                existingDef.setDefaultValue(serializeDefaultValue(newDefaultValue, existingDef.getDataType()));
            } else if (Boolean.TRUE.equals(updateDto.clearDefaultValue())) {
                existingDef.setDefaultValue(null);
            }

            // Apply the new default value to existing identities if we're updating the
            // default value
            if (newDefaultValue != null) {
                applyDefaultValueToExistingIdentities(existingDef.getTenantId(), existingDef.getIdentityType(),
                        existingDef.getId(), newDefaultValue, existingDef.getDataType());
            }
        }

        ExtendedAttributeDefinition savedDef = definitionRepository.save(existingDef);
        return mapEntityToResponseDto(savedDef);
    }

    @Override
    public void deleteEmployeeDefinitionBySystemKey(String tenantId, String systemKey) {
        deleteDefinitionInternal(tenantId, systemKey, IdentityType.EMPLOYEE);
    }

    @Override
    public void deleteNonEmployeeDefinitionBySystemKey(String tenantId, String systemKey) {
        deleteDefinitionInternal(tenantId, systemKey, IdentityType.NON_EMPLOYEE);
    }

    @Override
    public void deleteNonHumanDefinitionBySystemKey(String tenantId, String systemKey) {
        deleteDefinitionInternal(tenantId, systemKey, IdentityType.NON_HUMAN);
    }

    private void deleteDefinitionInternal(String tenantId, String systemKey, IdentityType identityType) {
        ExtendedAttributeDefinition existingDef = definitionRepository
                .findByTenantIdAndIdentityTypeAndSystemKey(tenantId, identityType, systemKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Extended attribute definition not found with systemKey: " + systemKey + " for tenant: "
                                + tenantId + " and identity type: " + identityType));
        existingDef.setLifecycleState(SchemaLifecycleState.RETIRED);
        definitionRepository.save(existingDef);
    }

    private DefinitionResponseDto mapEntityToResponseDto(ExtendedAttributeDefinition entity) {
        // System.out.println("DEBUG: mapEntityToResponseDto called for entity ID: " +
        // entity.getId());
        Object defaultValue = entity.getDefaultValue() == null ? null
                : parseDefaultValue(entity.getDefaultValue(), entity.getDataType());
        // System.out.println("DEBUG: mapEntityToResponseDto completed parsing default
        // value");
        return new DefinitionResponseDto(
                entity.getId(),
                entity.getTenantId(),
                entity.getIdentityType(),
                entity.getSystemKey(),
                entity.getDisplayName(),
                entity.getDataType(),
                entity.getDescription(),
                entity.getDisplayOrder(),
                entity.getIsRequired(),
                entity.getIsUnique(),
                entity.getIsSensitive(),
                entity.getIsWritable(),
                resolveLifecycleState(entity.getLifecycleState()),
                defaultValue,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private SchemaLifecycleState resolveLifecycleState(SchemaLifecycleState lifecycleState) {
        return lifecycleState != null ? lifecycleState : SchemaLifecycleState.ACTIVE;
    }

    private void validateRequiredAttributeCreation(String tenantId, IdentityType identityType) {
        // Check if there are existing identities for this tenant and identity type
        long existingIdentitiesCount = identityRepository.countByTenantIdAndIdentityType(
                tenantId,
                identityType);

        if (existingIdentitiesCount > 0) {
            throw new IllegalArgumentException(
                    "Cannot create required extended attribute because " + existingIdentitiesCount +
                            " existing identities of type '" + identityType.getValue() +
                            "' would not have this required field. Please provide default values or " +
                            "make the attribute optional.");
        }
    }

    private void validateRequiredAttributeCreation(ExtendedAttributeDefinition definition) {
        validateRequiredAttributeCreation(definition.getTenantId(), definition.getIdentityType());
    }

    private Object parseDefaultValue(String defaultValueJson, DataType dataType) {
        if (defaultValueJson == null || defaultValueJson.trim().isEmpty()) {
            return null;
        }

        // System.out.println("DEBUG: parseDefaultValue called with dataType=" +
        // dataType + ", json=" + defaultValueJson);

        try {
            switch (dataType) {
                case STRING:
                    return objectMapper.readValue(defaultValueJson, String.class);
                case NUMBER:
                    return objectMapper.readValue(defaultValueJson, BigDecimal.class);
                case BOOLEAN:
                    return objectMapper.readValue(defaultValueJson, Boolean.class);
                case DATE:
                    return objectMapper.readValue(defaultValueJson, LocalDate.class);
                case ARRAY:
                    Object arrayResult = objectMapper.readValue(defaultValueJson, new TypeReference<List<Object>>() {
                    });
                    // System.out.println("DEBUG: parsed ARRAY result type: " +
                    // arrayResult.getClass().getName());
                    return arrayResult;
                case OBJECT:
                    Object objectResult = objectMapper.readValue(defaultValueJson,
                            new TypeReference<Map<String, Object>>() {
                            });
                    // System.out.println("DEBUG: parsed OBJECT result type: " +
                    // objectResult.getClass().getName());
                    return objectResult;
                default:
                    throw new IllegalArgumentException("Unsupported data type: " + dataType);
            }
        } catch (Exception e) {
            // System.out.println("DEBUG: parseDefaultValue failed with error: " +
            // e.getMessage());
            throw new IllegalArgumentException(
                    "Invalid default value format for data type " + dataType + ": " + e.getMessage());
        }
    }

    private String serializeDefaultValue(Object defaultValue, DataType dataType) {
        if (defaultValue == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(defaultValue);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Cannot serialize default value for data type " + dataType + ": " + e.getMessage());
        }
    }

    private void applyDefaultValueToExistingIdentities(String tenantId, IdentityType identityType, UUID definitionId,
            Object defaultValue, DataType dataType) {
        // Get all existing identities for this tenant and type
        List<Identity> existingIdentities = identityRepository.findByTenantIdAndIdentityType(tenantId,
                identityType);

        // For each existing identity, create an extended attribute value with the
        // default value
        // System.out.println("DEBUG: Applying default value to " +
        // existingIdentities.size() + " existing identities");
        for (int i = 0; i < existingIdentities.size(); i++) {
            Identity identity = existingIdentities.get(i);
            // System.out.println("DEBUG: Processing identity " + (i + 1) + "/" +
            // existingIdentities.size() +
            // " - ID: " + identity.getId());
            createExtendedAttributeValue(identity.getId(), definitionId, defaultValue, dataType);
            // System.out.println("DEBUG: Completed processing identity " + (i + 1));
        }
    }

    private void createExtendedAttributeValue(UUID identityId, UUID definitionId, Object value, DataType dataType) {
        // System.out.println("DEBUG: createExtendedAttributeValue called with
        // dataType=" + dataType +
        // ", value type=" + (value != null ? value.getClass().getName() : "null") +
        // ", value=" + value);

        switch (dataType) {
            case STRING -> {
                ExtAttrStringValue stringValue = new ExtAttrStringValue();
                stringValue.setIdentityId(identityId);
                stringValue.setDefinitionId(definitionId);
                stringValue.setValue((String) value);
                stringValueRepository.save(stringValue);
            }
            case NUMBER -> {
                ExtAttrNumberValue numberValue = new ExtAttrNumberValue();
                numberValue.setIdentityId(identityId);
                numberValue.setDefinitionId(definitionId);
                numberValue.setValue((BigDecimal) value);
                numberValueRepository.save(numberValue);
            }
            case BOOLEAN -> {
                ExtAttrBooleanValue booleanValue = new ExtAttrBooleanValue();
                booleanValue.setIdentityId(identityId);
                booleanValue.setDefinitionId(definitionId);
                booleanValue.setValue((Boolean) value);
                booleanValueRepository.save(booleanValue);
            }
            case DATE -> {
                ExtAttrDateValue dateValue = new ExtAttrDateValue();
                dateValue.setIdentityId(identityId);
                dateValue.setDefinitionId(definitionId);
                // Convert LocalDate to Instant if needed
                if (value instanceof LocalDate localDate) {
                    dateValue.setValue(localDate.atStartOfDay().toInstant(java.time.ZoneOffset.UTC));
                } else {
                    dateValue.setValue((Instant) value);
                }
                dateValueRepository.save(dateValue);
            }
            case ARRAY, OBJECT -> {
                // Store complex types as JSONB in their respective tables
                try {
                    if (dataType == DataType.ARRAY) {
                        ExtAttrArrayValue arrayValue = new ExtAttrArrayValue();
                        arrayValue.setIdentityId(identityId);
                        arrayValue.setDefinitionId(definitionId);
                        JsonNode jsonNode = objectMapper.valueToTree(value);
                        // System.out.println("DEBUG: ARRAY - converting value to JsonNode: " +
                        // jsonNode);
                        arrayValue.setValue(jsonNode);
                        arrayValueRepository.save(arrayValue);
                    } else {
                        ExtAttrObjectValue objectValue = new ExtAttrObjectValue();
                        objectValue.setIdentityId(identityId);
                        objectValue.setDefinitionId(definitionId);
                        JsonNode jsonNode = objectMapper.valueToTree(value);
                        // System.out.println("DEBUG: OBJECT - converting value to JsonNode: " +
                        // jsonNode +
                        // ", JsonNode type: " + jsonNode.getClass().getName());
                        objectValue.setValue(jsonNode);
                        // System.out.println("DEBUG: About to save ExtAttrObjectValue with JsonNode
                        // value");
                        try {
                            objectValueRepository.save(objectValue);
                            // System.out.println("DEBUG: Successfully saved ExtAttrObjectValue");
                        } catch (Exception e) {
                            // System.out.println("DEBUG: Error saving ExtAttrObjectValue: " +
                            // e.getMessage());
                            e.printStackTrace();
                            throw e;
                        }
                    }
                } catch (Exception e) {
                    // System.out.println("DEBUG: Exception in ARRAY/OBJECT case: " +
                    // e.getMessage());
                    e.printStackTrace();
                    throw new IllegalArgumentException(
                            "Failed to serialize complex attribute value: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Validates that the system key doesn't conflict with core attribute names
     */
    private void validateNoCoreAttributeConflict(String systemKey, IdentityType identityType) {
        List<CoreAttributeMetadataService.CoreAttributeMetadata> coreAttributes = coreAttributeService
                .getCoreAttributesForIdentityType(identityType);

        boolean hasConflict = coreAttributes.stream()
                .anyMatch(coreAttr -> coreAttr.fieldName().equals(systemKey));

        if (hasConflict) {
            throw new IllegalArgumentException(
                    "Cannot create extended attribute with system key '" + systemKey +
                            "' because it conflicts with a core attribute name for identity type '" +
                            identityType.getValue() + "'");
        }
    }

    @Override
    public SchemaStatisticsDto getEmployeeSchemaStatistics(String tenantId) {
        return getSchemaStatisticsInternal(tenantId, IdentityType.EMPLOYEE);
    }

    @Override
    public SchemaStatisticsDto getNonEmployeeSchemaStatistics(String tenantId) {
        return getSchemaStatisticsInternal(tenantId, IdentityType.NON_EMPLOYEE);
    }

    @Override
    public SchemaStatisticsDto getNonHumanSchemaStatistics(String tenantId) {
        return getSchemaStatisticsInternal(tenantId, IdentityType.NON_HUMAN);
    }

    private SchemaStatisticsDto getSchemaStatisticsInternal(String tenantId, IdentityType identityType) {
        // Get core attributes
        List<CoreAttributeMetadataService.CoreAttributeMetadata> coreAttributes = coreAttributeService
                .getCoreAttributesForIdentityType(identityType);

        // Get extended attributes
        List<ExtendedAttributeDefinition> extendedDefinitions = definitionRepository
                .findByTenantIdAndIdentityType(tenantId, identityType);

        // Calculate total attributes
        int totalAttributes = coreAttributes.size() + extendedDefinitions.size();

        // Calculate required attributes (all core attributes are required, plus
        // extended required ones)
        int requiredAttributes = coreAttributes.size() + (int) extendedDefinitions.stream()
                .filter(def -> Boolean.TRUE.equals(def.getIsRequired())).count();

        int optionalAttributes = totalAttributes - requiredAttributes;

        // Combine data type distributions
        Map<DataType, Integer> dataTypeDistribution = new HashMap<>();

        // Add core attribute data types
        for (CoreAttributeMetadataService.CoreAttributeMetadata coreAttr : coreAttributes) {
            dataTypeDistribution.merge(coreAttr.dataType(), 1, Integer::sum);
        }

        // Add extended attribute data types
        for (ExtendedAttributeDefinition extAttr : extendedDefinitions) {
            dataTypeDistribution.merge(extAttr.getDataType(), 1, Integer::sum);
        }

        // Extended attribute specific properties
        int attributesWithDefaultValues = (int) extendedDefinitions.stream()
                .filter(def -> def.getDefaultValue() != null).count();

        int sensitiveAttributes = (int) extendedDefinitions.stream()
                .filter(def -> Boolean.TRUE.equals(def.getIsSensitive())).count();

        int uniqueAttributes = (int) extendedDefinitions.stream()
                .filter(def -> Boolean.TRUE.equals(def.getIsUnique())).count();

        // Current schema-management semantics treat writability as an explicit
        // attribute capability. Core attributes do not expose writable metadata
        // here, so only count attributes that are explicitly marked writable.
        int writableAttributes = (int) extendedDefinitions.stream()
                .filter(def -> Boolean.TRUE.equals(def.getIsWritable())).count();

        int readOnlyAttributes = totalAttributes - writableAttributes;

        return new SchemaStatisticsDto(
                identityType,
                totalAttributes,
                requiredAttributes,
                optionalAttributes,
                dataTypeDistribution,
                attributesWithDefaultValues,
                sensitiveAttributes,
                uniqueAttributes,
                writableAttributes,
                readOnlyAttributes);
    }

    @Override
    public DefinitionResponseDto getEmployeeDefinitionBySystemKey(String tenantId, String systemKey) {
        return getDefinitionBySystemKeyInternal(tenantId, systemKey, IdentityType.EMPLOYEE);
    }

    @Override
    public DefinitionResponseDto getNonEmployeeDefinitionBySystemKey(String tenantId, String systemKey) {
        return getDefinitionBySystemKeyInternal(tenantId, systemKey, IdentityType.NON_EMPLOYEE);
    }

    @Override
    public DefinitionResponseDto getNonHumanDefinitionBySystemKey(String tenantId, String systemKey) {
        return getDefinitionBySystemKeyInternal(tenantId, systemKey, IdentityType.NON_HUMAN);
    }

    private DefinitionResponseDto getDefinitionBySystemKeyInternal(String tenantId, String systemKey,
            IdentityType identityType) {
        ExtendedAttributeDefinition definition = definitionRepository
                .findByTenantIdAndIdentityTypeAndSystemKey(tenantId, identityType, systemKey)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Extended attribute definition not found with systemKey: " + systemKey +
                                " for identity type: " + identityType + " and tenant: " + tenantId));

        return mapEntityToResponseDto(definition);
    }
}
