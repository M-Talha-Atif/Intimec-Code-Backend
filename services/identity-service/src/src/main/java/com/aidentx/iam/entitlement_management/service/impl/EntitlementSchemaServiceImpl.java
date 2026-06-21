package com.aidentx.iam.entitlement_management.service.impl;

import com.aidentx.iam.entitlement_management.model.dto.EntitlementSchemaDtos;
import com.aidentx.iam.entitlement_management.model.entity.EntitlementAttributeDefinition;
import com.aidentx.iam.entitlement_management.model.enums.SchemaLifecycleState;
import com.aidentx.iam.entitlement_management.repository.EntitlementAttributeDefinitionRepository;
import com.aidentx.iam.entitlement_management.service.EntitlementSchemaService;
import com.aidentx.iam.identity_management.model.enums.DataType;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class EntitlementSchemaServiceImpl implements EntitlementSchemaService {

    private final EntitlementAttributeDefinitionRepository repository;
    private final IUnifiedLogger logger;

    private static final List<EntitlementSchemaDtos.CoreAttributeDto> CORE_ATTRIBUTES = List.of(
            new EntitlementSchemaDtos.CoreAttributeDto("displayName", "Display Name", DataType.STRING, true, false, null,
                    "Human-readable entitlement name.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("description", "Description", DataType.STRING, false, false, null,
                    "Description of the entitlement.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("kind", "Kind", DataType.STRING, false, false, null,
                    "Connector-defined entitlement kind.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("lifecycle", "Lifecycle", DataType.STRING, false, false, null,
                    "Governance lifecycle state.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("riskLevel", "Risk Level", DataType.STRING, false, false, null,
                    "Governance risk classification.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("riskReason", "Risk Reason", DataType.STRING, false, false, null,
                    "Reason for the risk classification.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("requestable", "Requestable", DataType.BOOLEAN, false, false, null,
                    "Whether this entitlement is requestable.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("classification", "Classification", DataType.ARRAY, false, false, null,
                    "Governance classification labels.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("identityType", "Identity Type", DataType.STRING, false, false, null,
                    "Applicable identity type.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("privilegeLevel", "Privilege Level", DataType.STRING, false, false, null,
                    "Privilege level classification.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("ownerId", "Owner Id", DataType.STRING, false, false, null,
                    "Owner identity identifier.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("requireApproval", "Require Approval", DataType.BOOLEAN, false, false, null,
                    "Whether assignment requires approval.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("allowedAutomatedGrant", "Allowed Automated Grant", DataType.BOOLEAN, false, false, null,
                    "Whether automated grant is allowed.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("maxDurationMs", "Max Duration (ms)", DataType.NUMBER, false, false, null,
                    "Maximum assignment duration in milliseconds.", false),
            new EntitlementSchemaDtos.CoreAttributeDto("autoExpireDurationMs", "Auto Expire Duration (ms)", DataType.NUMBER, false, false, null,
                    "Auto-expire duration in milliseconds.", false)
    );

    public EntitlementSchemaServiceImpl(EntitlementAttributeDefinitionRepository repository,
                                        UnifiedLoggerFactory loggerFactory) {
        this.repository = repository;
        this.logger = loggerFactory.getLogger(EntitlementSchemaServiceImpl.class);
    }

    @Override
    public EntitlementSchemaDtos.EntitlementSchemaResponseDto getSchema(String tenantId) {
        List<EntitlementSchemaDtos.ExtendedAttributeDto> extended = findDefinitions(tenantId);
        return new EntitlementSchemaDtos.EntitlementSchemaResponseDto(CORE_ATTRIBUTES, extended);
    }

    @Override
    public EntitlementSchemaDtos.ExtendedAttributeDto createDefinition(String tenantId,
                                                                       EntitlementSchemaDtos.DefinitionCreateDto createDto) {
        String normalizedKey = normalizeSystemKey(createDto.systemKey());
        if (repository.existsByTenantIdAndSystemKey(tenantId, normalizedKey)) {
            throw new IllegalArgumentException("Entitlement attribute with system key '" + normalizedKey + "' already exists");
        }

        EntitlementAttributeDefinition entity = new EntitlementAttributeDefinition();
        entity.setTenantId(tenantId);
        entity.setSystemKey(normalizedKey);
        entity.setDisplayName(createDto.displayName());
        entity.setDataType(createDto.dataType());
        entity.setDescription(createDto.description());
        entity.setDisplayOrder(createDto.displayOrder() != null ? createDto.displayOrder() : 0);
        entity.setIsRequired(createDto.isRequired() != null ? createDto.isRequired() : false);
        entity.setIsUnique(createDto.isUnique() != null ? createDto.isUnique() : false);
        entity.setIsSensitive(createDto.isSensitive() != null ? createDto.isSensitive() : false);
        entity.setIsWritable(createDto.isWritable() != null ? createDto.isWritable() : true);
        entity.setLifecycleState(resolveLifecycleState(createDto.lifecycleState()));
        entity.setDefaultValue(createDto.defaultValue());

        EntitlementAttributeDefinition saved = repository.save(entity);
        return toDto(saved);
    }

    @Override
    public EntitlementSchemaDtos.ExtendedAttributeDto updateDefinition(String tenantId,
                                                                       String systemKey,
                                                                       EntitlementSchemaDtos.DefinitionUpdateDto updateDto) {
        EntitlementAttributeDefinition existing = repository
                .findByTenantIdAndSystemKey(tenantId, normalizeSystemKey(systemKey))
                .orElseThrow(() -> new IllegalArgumentException("Entitlement attribute definition not found: " + systemKey));

        if (updateDto.displayName() != null) {
            existing.setDisplayName(updateDto.displayName());
        }
        if (updateDto.description() != null) {
            existing.setDescription(updateDto.description());
        }
        if (updateDto.isRequired() != null) {
            existing.setIsRequired(updateDto.isRequired());
        }
        if (updateDto.isUnique() != null) {
            existing.setIsUnique(updateDto.isUnique());
        }
        if (updateDto.isSensitive() != null) {
            existing.setIsSensitive(updateDto.isSensitive());
        }
        if (updateDto.isWritable() != null) {
            existing.setIsWritable(updateDto.isWritable());
        }
        if (updateDto.lifecycleState() != null) {
            existing.setLifecycleState(resolveLifecycleState(updateDto.lifecycleState()));
        }
        if (updateDto.displayOrder() != null) {
            existing.setDisplayOrder(updateDto.displayOrder());
        }
        if (Boolean.TRUE.equals(updateDto.clearDefaultValue())) {
            existing.setDefaultValue(null);
        } else if (updateDto.defaultValue() != null) {
            existing.setDefaultValue(updateDto.defaultValue());
        }

        EntitlementAttributeDefinition saved = repository.save(existing);
        return toDto(saved);
    }

    @Override
    public void deleteDefinition(String tenantId, String systemKey) {
        EntitlementAttributeDefinition existing = repository
                .findByTenantIdAndSystemKey(tenantId, normalizeSystemKey(systemKey))
                .orElseThrow(() -> new IllegalArgumentException("Entitlement attribute definition not found: " + systemKey));
        existing.setLifecycleState(SchemaLifecycleState.RETIRED);
        repository.save(existing);
    }

    @Override
    public EntitlementSchemaDtos.ExtendedAttributeDto getDefinitionBySystemKey(String tenantId, String systemKey) {
        EntitlementAttributeDefinition existing = repository
                .findByTenantIdAndSystemKey(tenantId, normalizeSystemKey(systemKey))
                .orElseThrow(() -> new IllegalArgumentException("Entitlement attribute definition not found: " + systemKey));
        return toDto(existing);
    }

    @Override
    public List<EntitlementSchemaDtos.ExtendedAttributeDto> findDefinitions(String tenantId) {
        List<EntitlementAttributeDefinition> entities = repository.findByTenantIdOrderByDisplayOrderAscDisplayNameAsc(tenantId);
        List<EntitlementSchemaDtos.ExtendedAttributeDto> result = new ArrayList<>(entities.size());
        for (EntitlementAttributeDefinition entity : entities) {
            result.add(toDto(entity));
        }
        return result;
    }

    @Override
    public EntitlementSchemaDtos.SchemaStatisticsDto getSchemaStatistics(String tenantId) {
        List<EntitlementAttributeDefinition> custom = repository.findByTenantIdOrderByDisplayOrderAscDisplayNameAsc(tenantId);

        int totalAttributes = CORE_ATTRIBUTES.size() + custom.size();
        int requiredCore = (int) CORE_ATTRIBUTES.stream().filter(EntitlementSchemaDtos.CoreAttributeDto::isRequired).count();
        int requiredCustom = (int) custom.stream().filter(def -> Boolean.TRUE.equals(def.getIsRequired())).count();
        int requiredAttributes = requiredCore + requiredCustom;
        int optionalAttributes = totalAttributes - requiredAttributes;

        Map<DataType, Integer> distribution = new EnumMap<>(DataType.class);
        for (DataType dataType : DataType.values()) {
            distribution.put(dataType, 0);
        }
        for (EntitlementSchemaDtos.CoreAttributeDto core : CORE_ATTRIBUTES) {
            distribution.put(core.dataType(), distribution.get(core.dataType()) + 1);
        }
        for (EntitlementAttributeDefinition ext : custom) {
            distribution.put(ext.getDataType(), distribution.get(ext.getDataType()) + 1);
        }

        int attributesWithDefaultValues = (int) custom.stream().filter(def -> def.getDefaultValue() != null).count();
        int sensitiveAttributes = (int) custom.stream().filter(def -> Boolean.TRUE.equals(def.getIsSensitive())).count();
        int uniqueAttributes = (int) custom.stream().filter(def -> Boolean.TRUE.equals(def.getIsUnique())).count();
        int writableAttributes = (int) custom.stream().filter(def -> Boolean.TRUE.equals(def.getIsWritable())).count();
        int readOnlyAttributes = totalAttributes - writableAttributes;

        return new EntitlementSchemaDtos.SchemaStatisticsDto(
                "ENTITLEMENT",
                totalAttributes,
                requiredAttributes,
                optionalAttributes,
                distribution,
                attributesWithDefaultValues,
                sensitiveAttributes,
                uniqueAttributes,
                writableAttributes,
                readOnlyAttributes
        );
    }

    @Override
    public List<String> getCustomAttributeKeys(String tenantId) {
        return repository.findByTenantIdOrderByDisplayOrderAscDisplayNameAsc(tenantId)
                .stream()
                .filter(def -> {
                    SchemaLifecycleState state = resolveLifecycleState(def.getLifecycleState());
                    return state == SchemaLifecycleState.ACTIVE || state == SchemaLifecycleState.DEPRECATED;
                })
                .map(EntitlementAttributeDefinition::getSystemKey)
                .toList();
    }

    private SchemaLifecycleState resolveLifecycleState(SchemaLifecycleState lifecycleState) {
        return lifecycleState != null ? lifecycleState : SchemaLifecycleState.ACTIVE;
    }

    private String normalizeSystemKey(String systemKey) {
        if (systemKey == null || systemKey.isBlank()) {
            throw new IllegalArgumentException("systemKey is required");
        }
        return systemKey.trim().toLowerCase(Locale.ROOT);
    }

    private EntitlementSchemaDtos.ExtendedAttributeDto toDto(EntitlementAttributeDefinition entity) {
        return new EntitlementSchemaDtos.ExtendedAttributeDto(
                entity.getId(),
                entity.getSystemKey(),
                entity.getDisplayName(),
                entity.getDataType(),
                entity.getDescription(),
                entity.getDisplayOrder(),
                Boolean.TRUE.equals(entity.getIsRequired()),
                Boolean.TRUE.equals(entity.getIsUnique()),
                Boolean.TRUE.equals(entity.getIsSensitive()),
                Boolean.TRUE.equals(entity.getIsWritable()),
                resolveLifecycleState(entity.getLifecycleState()),
                entity.getDefaultValue(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
