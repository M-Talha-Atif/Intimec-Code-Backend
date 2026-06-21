package com.aidentx.iam.identity_management.service.impl;

import com.aidentx.iam.identity_management.model.entity.ExtendedAttributeDefinition;
import com.aidentx.iam.identity_management.model.enums.DataType;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.model.entity.ExtAttrArrayValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrBooleanValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrDateValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrNumberValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrObjectValue;
import com.aidentx.iam.identity_management.model.entity.ExtAttrStringValue;
import com.aidentx.iam.identity_management.repository.*;
import com.aidentx.iam.identity_management.service.ExtendedAttributeService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service implementation for managing extended attributes for identities.
 */
@Service
@Transactional
public class ExtendedAttributeServiceImpl implements ExtendedAttributeService {

    private final IUnifiedLogger logger;
    private final ExtendedAttributeDefinitionRepository definitionRepository;
    private final ExtAttrStringValueRepository stringValueRepository;
    private final ExtAttrNumberValueRepository numberValueRepository;
    private final ExtAttrBooleanValueRepository booleanValueRepository;
    private final ExtAttrDateValueRepository dateValueRepository;
    private final ExtAttrArrayValueRepository arrayValueRepository;
    private final ExtAttrObjectValueRepository objectValueRepository;
    private final ExtendedAttributeValueRepository extendedAttributeValueRepository;
    private final ObjectMapper objectMapper;

    public ExtendedAttributeServiceImpl(UnifiedLoggerFactory loggerFactory,
            ExtendedAttributeDefinitionRepository definitionRepository,
            ExtAttrStringValueRepository stringValueRepository,
            ExtAttrNumberValueRepository numberValueRepository,
            ExtAttrBooleanValueRepository booleanValueRepository,
            ExtAttrDateValueRepository dateValueRepository,
            ExtAttrArrayValueRepository arrayValueRepository,
            ExtAttrObjectValueRepository objectValueRepository,
            ExtendedAttributeValueRepository extendedAttributeValueRepository) {
        this.logger = loggerFactory.getLogger(ExtendedAttributeServiceImpl.class);
        this.definitionRepository = definitionRepository;
        this.stringValueRepository = stringValueRepository;
        this.numberValueRepository = numberValueRepository;
        this.booleanValueRepository = booleanValueRepository;
        this.dateValueRepository = dateValueRepository;
        this.arrayValueRepository = arrayValueRepository;
        this.objectValueRepository = objectValueRepository;
        this.extendedAttributeValueRepository = extendedAttributeValueRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    @Transactional
    public void saveExtendedAttributesForCreation(UUID identityId, String tenantId, IdentityType identityType,
            Map<String, Object> attributes) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        List<ExtendedAttributeDefinition> definitions = definitionRepository
                .findByTenantIdAndIdentityType(tenantId, identityType);

        Map<String, ExtendedAttributeDefinition> defMap = definitions.stream()
                .collect(Collectors.toMap(ExtendedAttributeDefinition::getSystemKey, Function.identity()));

        // For creation: ensure all required attributes have values (either provided or
        // default)
        for (ExtendedAttributeDefinition def : definitions) {
            if (def.getIsRequired()) {
                String systemKey = def.getSystemKey();
                boolean isProvided = attributes.containsKey(systemKey) && attributes.get(systemKey) != null;

                if (!isProvided) {
                    // Apply default value if available
                    if (def.getDefaultValue() != null) {
                        Object defaultValue = parseDefaultValue(def.getDefaultValue(), def.getDataType());
                        attributes.put(systemKey, defaultValue);
                    } else {
                        throw new IllegalArgumentException(
                                String.format(
                                        "Required extended attribute '%s' must be provided for %s creation (no default value available)",
                                        systemKey, identityType.getValue()));
                    }
                }
            }
        }

        // Save all attributes (provided and defaults)
        saveAttributeValues(identityId, attributes, defMap);
    }

    @Override
    public void saveExtendedAttributesForUpdate(UUID identityId, String tenantId, IdentityType identityType,
            Map<String, Object> attributes) {
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        List<ExtendedAttributeDefinition> definitions = definitionRepository
                .findByTenantIdAndIdentityType(tenantId, identityType);

        Map<String, ExtendedAttributeDefinition> defMap = definitions.stream()
                .collect(Collectors.toMap(ExtendedAttributeDefinition::getSystemKey, Function.identity()));

        // For update: ensure all required attributes have values (provided, existing,
        // or default)
        for (ExtendedAttributeDefinition def : definitions) {
            if (def.getIsRequired()) {
                String systemKey = def.getSystemKey();
                boolean isProvided = attributes.containsKey(systemKey) && attributes.get(systemKey) != null;
                boolean alreadyExists = checkIfIdentityHasAttributeValue(identityId, def.getId(), def.getDataType());

                if (!isProvided && !alreadyExists) {
                    // Apply default value if available
                    if (def.getDefaultValue() != null) {
                        Object defaultValue = parseDefaultValue(def.getDefaultValue(), def.getDataType());
                        attributes.put(systemKey, defaultValue);
                    } else {
                        throw new IllegalArgumentException(
                                String.format(
                                        "Required extended attribute '%s' must be provided for %s update (no default value available and no existing value)",
                                        systemKey, identityType.getValue()));
                    }
                }
            }
        }

        // Save all attributes (provided and defaults)
        saveAttributeValues(identityId, attributes, defMap);
    }

    /**
     * Helper method to save attribute values to the database.
     */
    private void saveAttributeValues(UUID identityId, Map<String, Object> attributes,
            Map<String, ExtendedAttributeDefinition> defMap) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            ExtendedAttributeDefinition def = defMap.get(entry.getKey());
            if (def != null && entry.getValue() != null) {
                switch (def.getDataType()) {
                    case STRING:
                        ExtAttrStringValue strVal = new ExtAttrStringValue();
                        strVal.setIdentityId(identityId);
                        strVal.setDefinitionId(def.getId());
                        strVal.setValue(entry.getValue().toString());
                        stringValueRepository.save(strVal);
                        break;
                    case NUMBER:
                        ExtAttrNumberValue numVal = new ExtAttrNumberValue();
                        numVal.setIdentityId(identityId);
                        numVal.setDefinitionId(def.getId());
                        numVal.setValue(new BigDecimal(entry.getValue().toString()));
                        numberValueRepository.save(numVal);
                        break;
                    case BOOLEAN:
                        ExtAttrBooleanValue boolVal = new ExtAttrBooleanValue();
                        boolVal.setIdentityId(identityId);
                        boolVal.setDefinitionId(def.getId());
                        boolVal.setValue(Boolean.parseBoolean(entry.getValue().toString()));
                        booleanValueRepository.save(boolVal);
                        break;
                    case DATE:
                        ExtAttrDateValue dateVal = new ExtAttrDateValue();
                        dateVal.setIdentityId(identityId);
                        dateVal.setDefinitionId(def.getId());
                        // Assumes the value is an ISO-8601 string or similar parsable format
                        dateVal.setValue(Instant.parse(entry.getValue().toString()));
                        dateValueRepository.save(dateVal);
                        break;
                    case ARRAY:
                        try {
                            ExtAttrArrayValue arrayVal = new ExtAttrArrayValue();
                            arrayVal.setIdentityId(identityId);
                            arrayVal.setDefinitionId(def.getId());
                            arrayVal.setValue(objectMapper.valueToTree(entry.getValue()));
                            arrayValueRepository.save(arrayVal);
                        } catch (Exception e) {
                            logger.error().message("Failed to serialize array attribute value")
                                    .detail("attribute", entry.getKey())
                                    .detail("error", e.getMessage())
                                    .log();
                            throw new IllegalArgumentException("Invalid value for array attribute: " + entry.getKey());
                        }
                        break;
                    case OBJECT:
                        try {
                            ExtAttrObjectValue objectVal = new ExtAttrObjectValue();
                            objectVal.setIdentityId(identityId);
                            objectVal.setDefinitionId(def.getId());
                            objectVal.setValue(objectMapper.valueToTree(entry.getValue()));
                            objectValueRepository.save(objectVal);
                        } catch (Exception e) {
                            logger.error().message("Failed to serialize object attribute value")
                                    .detail("attribute", entry.getKey())
                                    .detail("error", e.getMessage())
                                    .log();
                            throw new IllegalArgumentException("Invalid value for object attribute: " + entry.getKey());
                        }
                        break;
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> findExtendedAttributesForIdentity(UUID identityId, String tenantId,
            IdentityType identityType) {
        Map<String, Object> extendedAttributes = new HashMap<>();

        // Use single query to fetch all values with their definitions
        List<Object[]> results = extendedAttributeValueRepository
                .findAllValuesWithDefinitions(identityId, tenantId, identityType.getValue());

        // Process native query results: [id, created_at, data_type, description,
        // display_name, display_order, identity_type, is_required, is_sensitive,
        // is_unique, is_writable, system_key, tenant_id, updated_at, value_type,
        // string_value, number_value, boolean_value, date_value]
        for (Object[] row : results) {
            String systemKey = (String) row[11]; // system_key
            String valueType = (String) row[14]; // value_type

            Object value = null;
            switch (valueType) {
                case "STRING":
                    value = row[15]; // string_value
                    break;
                case "NUMBER":
                    value = row[16]; // number_value
                    break;
                case "BOOLEAN":
                    value = row[17]; // boolean_value
                    break;
                case "DATE":
                    value = row[18]; // date_value
                    break;
                case "ARRAY":
                    // Parse JSONB array back to List
                    if (row[15] != null) {
                        try {
                            // Handle both JsonNode and String representations
                            if (row[15] instanceof JsonNode jsonNode) {
                                value = objectMapper.convertValue(jsonNode, new TypeReference<List<Object>>() {
                                });
                            } else if (row[15] instanceof String jsonString) {
                                value = objectMapper.readValue(jsonString, new TypeReference<List<Object>>() {
                                });
                            }
                        } catch (Exception e) {
                            logger.warn().message("Failed to parse array value")
                                    .detail("systemKey", systemKey)
                                    .detail("valueType", row[15] != null ? row[15].getClass().getName() : "null")
                                    .detail("error", e.getMessage())
                                    .log();
                        }
                    }
                    break;
                case "OBJECT":
                    // Parse JSONB object back to Map
                    if (row[15] != null) {
                        try {
                            // Handle both JsonNode and String representations
                            if (row[15] instanceof JsonNode jsonNode) {
                                value = objectMapper.convertValue(jsonNode, new TypeReference<Map<String, Object>>() {
                                });
                            } else if (row[15] instanceof String jsonString) {
                                value = objectMapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {
                                });
                            }
                        } catch (Exception e) {
                            logger.warn().message("Failed to parse object value")
                                    .detail("systemKey", systemKey)
                                    .detail("valueType", row[15] != null ? row[15].getClass().getName() : "null")
                                    .detail("error", e.getMessage())
                                    .log();
                        }
                    }
                    break;
            }

            if (value != null) {
                extendedAttributes.put(systemKey, value);
            }
        }

        return extendedAttributes;
    }

    @Override
    public void deleteExtendedAttributes(UUID identityId, String tenantId, IdentityType identityType) {
        // Delete from all extended attribute value tables
        stringValueRepository.deleteByIdentityId(identityId);
        numberValueRepository.deleteByIdentityId(identityId);
        booleanValueRepository.deleteByIdentityId(identityId);
        dateValueRepository.deleteByIdentityId(identityId);
        arrayValueRepository.deleteByIdentityId(identityId);
        objectValueRepository.deleteByIdentityId(identityId);
    }

    @Override
    public void validateExtendedAttributeUpdate(String tenantId, IdentityType identityType,
            Map<String, Object> extendedAttributes) {
        if (extendedAttributes == null || extendedAttributes.isEmpty()) {
            return;
        }

        // Get all required extended attribute definitions for this identity type
        List<ExtendedAttributeDefinition> requiredDefs = definitionRepository
                .findByTenantIdAndIdentityTypeAndIsRequired(tenantId, identityType, true);

        for (ExtendedAttributeDefinition def : requiredDefs) {
            String systemKey = def.getSystemKey();

            // Check if this required attribute is being provided in the update
            if (!extendedAttributes.containsKey(systemKey)) {
                // Required attribute is not being provided in the update
                // This is actually OK for updates - the identity might already have the value
                // The real validation should happen in saveExtendedAttributes
                logger.info().message("Required extended attribute not provided in update")
                        .detail("attribute", systemKey)
                        .detail("identityType", identityType.getValue()).log();
            }
        }
    }

    /**
     * Counts how many identities have a value for a specific extended attribute.
     */
    private boolean checkIfIdentityHasAttributeValue(UUID identityId, UUID definitionId, DataType dataType) {
        return switch (dataType) {
            case STRING -> stringValueRepository.existsByIdentityIdAndDefinitionId(identityId, definitionId);
            case NUMBER -> numberValueRepository.existsByIdentityIdAndDefinitionId(identityId, definitionId);
            case BOOLEAN -> booleanValueRepository.existsByIdentityIdAndDefinitionId(identityId, definitionId);
            case DATE -> dateValueRepository.existsByIdentityIdAndDefinitionId(identityId, definitionId);
            case ARRAY -> arrayValueRepository.existsByIdentityIdAndDefinitionId(identityId, definitionId);
            case OBJECT -> objectValueRepository.existsByIdentityIdAndDefinitionId(identityId, definitionId);
        };
    }

    private Object parseDefaultValue(String defaultValueJson, DataType dataType) {
        if (defaultValueJson == null || defaultValueJson.trim().isEmpty()) {
            return null;
        }

        try {
            return switch (dataType) {
                case STRING -> objectMapper.readValue(defaultValueJson, String.class);
                case NUMBER -> objectMapper.readValue(defaultValueJson, BigDecimal.class);
                case BOOLEAN -> objectMapper.readValue(defaultValueJson, Boolean.class);
                case DATE -> objectMapper.readValue(defaultValueJson, LocalDate.class);
                case ARRAY -> objectMapper.readValue(defaultValueJson, List.class);
                case OBJECT -> objectMapper.readValue(defaultValueJson, Map.class);
            };
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid default value format for data type " + dataType + ": " + e.getMessage());
        }
    }
}
