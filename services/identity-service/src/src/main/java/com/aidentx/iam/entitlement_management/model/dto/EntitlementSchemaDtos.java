package com.aidentx.iam.entitlement_management.model.dto;

import com.aidentx.iam.entitlement_management.model.enums.SchemaLifecycleState;
import com.aidentx.iam.identity_management.model.enums.DataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EntitlementSchemaDtos {

    private EntitlementSchemaDtos() {
    }

    public record EntitlementSchemaResponseDto(
            List<CoreAttributeDto> coreAttributes,
            List<ExtendedAttributeDto> extendedAttributes
    ) {
    }

    public record CoreAttributeDto(
            String fieldName,
            String displayName,
            DataType dataType,
            boolean isRequired,
            boolean isRelationship,
            String relationshipType,
            String description,
            boolean isWritable
    ) {
    }

    public record ExtendedAttributeDto(
            UUID id,
            String systemKey,
            String displayName,
            DataType dataType,
            String description,
            Integer displayOrder,
            boolean isRequired,
            boolean isUnique,
            boolean isSensitive,
            boolean isWritable,
            SchemaLifecycleState lifecycleState,
            Object defaultValue,
            Long createdAt,
            Long updatedAt
    ) {
    }

    public record DefinitionCreateDto(
            @NotBlank String systemKey,
            @NotBlank String displayName,
            @NotNull DataType dataType,
            String description,
            Boolean isRequired,
            Boolean isUnique,
            Boolean isSensitive,
            Boolean isWritable,
            SchemaLifecycleState lifecycleState,
            Integer displayOrder,
            Object defaultValue
    ) {
    }

    public record DefinitionUpdateDto(
            String displayName,
            String description,
            Boolean isRequired,
            Boolean isUnique,
            Boolean isSensitive,
            Boolean isWritable,
            SchemaLifecycleState lifecycleState,
            Integer displayOrder,
            Object defaultValue,
            Boolean clearDefaultValue
    ) {
    }

    public record SchemaStatisticsDto(
            String identityType,
            int totalAttributes,
            int requiredAttributes,
            int optionalAttributes,
            Map<DataType, Integer> dataTypeDistribution,
            int attributesWithDefaultValues,
            int sensitiveAttributes,
            int uniqueAttributes,
            int writableAttributes,
            int readOnlyAttributes
    ) {
    }
}
