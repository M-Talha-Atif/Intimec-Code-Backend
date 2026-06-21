package com.aidentx.iam.identity_management.model.dto;

import com.aidentx.iam.identity_management.model.enums.DataType;
import com.aidentx.iam.identity_management.model.enums.SchemaLifecycleState;
import com.aidentx.iam.identity_management.service.CoreAttributeMetadataService;

import java.util.List;

/**
 * DTOs for identity schema responses that combine core and extended attributes.
 */
public final class IdentitySchemaDtos {

    /**
     * Combined schema response containing both core and extended attributes
     */
    public record IdentitySchemaResponseDto(
        List<CoreAttributeDto> coreAttributes,
        List<ExtendedAttributeDto> extendedAttributes
    ) {}

    /**
     * DTO for core attribute information
     */
    public record CoreAttributeDto(
        String systemKey,
        String displayName,
        DataType dataType,
        boolean isRequired,
        boolean isRelationship,
        String relationshipType,
        String description,
        boolean isWritable
    ) {
        public static CoreAttributeDto from(CoreAttributeMetadataService.CoreAttributeMetadata metadata) {
            return new CoreAttributeDto(
                metadata.fieldName(),
                metadata.displayName(),
                metadata.dataType(),
                metadata.isRequired(),
                metadata.isRelationship(),
                metadata.relationshipType(),
                metadata.description(),
                true // Core attributes are always writable
            );
        }
    }

    /**
     * DTO for extended attribute information
     */
    public record ExtendedAttributeDto(
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
        Object defaultValue
    ) {}
}
