package com.aidentx.iam.identity_management.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import java.util.Map;

import com.aidentx.iam.identity_management.model.enums.DataType;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.model.enums.SchemaLifecycleState;

public final class ExtendedAttributeDtos {

        /**
         * DTO for creating a new custom attribute definition without identity type (for
         * specific endpoints).
         */
        public record DefinitionCreateWithoutTypeDto(
                        @NotBlank String systemKey,
                        @NotBlank String displayName,
                        @NotNull DataType dataType,
                        String description,
                        Boolean isRequired,
                        Boolean isUnique,
                        Boolean isSensitive,
                        Boolean isWritable,
                        SchemaLifecycleState lifecycleState,
                        Object defaultValue // Default value for existing identities when making attribute required
        ) {
        }

        /**
         * DTO for updating an existing custom attribute definition.
         * All fields are optional.
         */
        public record DefinitionUpdateDto(
                        String displayName,
                        String description,
                        Boolean isRequired,
                        Boolean isUnique,
                        Boolean isSensitive,
                        Boolean isWritable,
                        SchemaLifecycleState lifecycleState,
                        Object defaultValue, // Default value for existing identities when making attribute required
                        Boolean clearDefaultValue // Flag to explicitly clear the default value
        ) {
        }

        /**
         * DTO for API responses, showing the full definition.
         */
        public record DefinitionResponseDto(
                        UUID id,
                        String tenantId,
                        IdentityType identityType,
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
                        Instant createdAt,
                        Instant updatedAt) {
        }

        /**
         * DTO for schema statistics of an identity type.
         */
        public record SchemaStatisticsDto(
                        IdentityType identityType,
                        int totalAttributes,
                        int requiredAttributes,
                        int optionalAttributes,
                        Map<DataType, Integer> dataTypeDistribution,
                        int attributesWithDefaultValues,
                        int sensitiveAttributes,
                        int uniqueAttributes,
                        int writableAttributes,
                        int readOnlyAttributes) {
        }
}
