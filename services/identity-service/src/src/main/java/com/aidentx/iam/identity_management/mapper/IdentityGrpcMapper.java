package com.aidentx.iam.identity_management.mapper;

import com.aidentx.dal.protos.identity.EmployeeAttributes;
import com.aidentx.dal.protos.identity.IdentityMessage;
import com.aidentx.dal.protos.identity.NonHumanAttributes;
import com.aidentx.iam.identity_management.model.dto.IdentityResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Struct;
import com.google.protobuf.StringValue;
import com.google.protobuf.util.JsonFormat;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class IdentityGrpcMapper {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Maps our new, unified IdentityResponseDto to the gRPC IdentityMessage.
     * This method handles the `oneof` logic by delegating to type-specific mappers.
     *
     * @param dto The unified response DTO from the service layer.
     * @return The fully constructed gRPC IdentityMessage.
     */
    public IdentityMessage toIdentityMessage(IdentityResponseDto dto) {
        if (dto == null) {
            return null;
        }

        IdentityMessage.Builder builder = IdentityMessage.newBuilder();

        // Map common fields
        builder.setId(uuidToString(dto.id()));
        builder.setTenantId(dto.tenantId());
        builder.setIdentityType(dto.identityType());
        builder.setStatus(enumToString(dto.status()));
        builder.setCreatedAt(instantToEpochSeconds(dto.createdAt()));
        if (dto.dgraphNodeId() != null) {
            builder.setDgraphNodeId(StringValue.of(dto.dgraphNodeId()));
        }

        Map<String, Object> grpcExtendedAttributes = buildGrpcExtendedAttributes(dto);
        if (!grpcExtendedAttributes.isEmpty()) {
            builder.setExtendedAttributes(mapToStruct(grpcExtendedAttributes));
        }

        // --- Core `oneof` Logic ---
        // Based on the identity type, populate the correct `oneof` field.
        if (dto.coreAttributes() != null && !dto.coreAttributes().isEmpty()) {
            switch (dto.identityType()) {
                case "EMPLOYEE":
                    builder.setEmployeeAttributes(mapToEmployeeAttributes(dto.coreAttributes()));
                    break;
                case "NON_HUMAN":
                    builder.setNonHumanAttributes(mapToNonHumanAttributes(dto.coreAttributes()));
                    break;
                // Add cases for NON_EMPLOYEE etc. as they are implemented
                default:
                    // Handle unknown or null type
                    break;
            }
        }

        return builder.build();
    }

    /**
     * Helper method to map a Map to EmployeeAttributes.
     */
    protected EmployeeAttributes mapToEmployeeAttributes(Map<String, Object> coreAttributes) {
        EmployeeAttributes.Builder builder = EmployeeAttributes.newBuilder();
        builder.setUsername(getString(coreAttributes, "username"));
        builder.setEmail(getString(coreAttributes, "email"));
        builder.setFirstName(getString(coreAttributes, "firstName"));
        builder.setLastName(getString(coreAttributes, "lastName"));
        builder.setDisplayName(getString(coreAttributes, "displayName"));
        builder.setEmployeeId(getString(coreAttributes, "employeeId"));
        builder.setJobTitle(getString(coreAttributes, "jobTitle"));
        builder.setDepartment(getString(coreAttributes, "department"));
        if (coreAttributes.containsKey("managerId")) {
            builder.setManagerId(StringValue.of(getString(coreAttributes, "managerId")));
        }
        return builder.build();
    }

    /**
     * Helper method to map a Map to NonHumanAttributes.
     */
    protected NonHumanAttributes mapToNonHumanAttributes(Map<String, Object> coreAttributes) {
        NonHumanAttributes.Builder builder = NonHumanAttributes.newBuilder();
        builder.setServiceName(getString(coreAttributes, "serviceName"));
        builder.setOwnerTeam(getString(coreAttributes, "ownerTeam"));
        builder.setDescription(getString(coreAttributes, "description"));
        return builder.build();
    }

    private Map<String, Object> buildGrpcExtendedAttributes(IdentityResponseDto dto) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        if (dto.extendedAttributes() != null) {
            attributes.putAll(dto.extendedAttributes());
        }
        if (dto.riskLevel() != null) {
            attributes.putIfAbsent("riskLevel", dto.riskLevel());
        } else if (dto.coreAttributes() != null && dto.coreAttributes().get("riskLevel") != null) {
            attributes.putIfAbsent("riskLevel", dto.coreAttributes().get("riskLevel"));
        }
        if (dto.totalAccess() != null) {
            attributes.putIfAbsent("totalAccess", dto.totalAccess());
        }
        return attributes;
    }

    /**
     * Converts a Java Map to a Protobuf Struct.
     */
    @Named("mapToStruct")
    public Struct mapToStruct(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return Struct.newBuilder().build();
        }
        try {
            String jsonString = objectMapper.writeValueAsString(map);
            Struct.Builder builder = Struct.newBuilder();
            JsonFormat.parser().ignoringUnknownFields().merge(jsonString, builder);
            return builder.build();
        } catch (com.fasterxml.jackson.core.JsonProcessingException | InvalidProtocolBufferException e) {
            throw new RuntimeException("Failed to convert Map to Protobuf Struct", e);
        }
    }

    // --- Utility and Helper Methods ---

    @Named("uuidToString")
    String uuidToString(@Nullable UUID uuid) {
        return uuid != null ? uuid.toString() : "";
    }

    @Named("enumToString")
    String enumToString(@Nullable Enum<?> anEnum) {
        return anEnum != null ? anEnum.name() : "";
    }

    // Corrected to map Instant to long (epoch seconds) as per the proto definition.
    @Named("instantToEpochSeconds")
    long instantToEpochSeconds(@Nullable Instant instant) {
        return instant != null ? instant.getEpochSecond() : 0L;
    }

    // Safely gets a string from the map, handling nulls.
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : "";
    }
}