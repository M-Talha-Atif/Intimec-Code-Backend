package com.aidentx.core.onboarding.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.aidentx.core.onboarding.model.dto.ApplicationResponseDTO;
import com.aidentx.core.onboarding.model.entity.Application;
import com.aidentx.core.onboarding.model.entity.ApplicationSchemaAttribute;
import com.aidentx.iam.contracts.dto.OnboardingRequestDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ApplicationMapper {

    private final ObjectMapper objectMapper;

    public ApplicationMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ApplicationResponseDTO toResponse(Application app) {
        return toResponse(app, null, null, null);
    }

    public ApplicationResponseDTO toResponse(Application app, OnboardingRequestDTO.CorrelationDTO correlation,
            OnboardingRequestDTO.AttributeMappingPolicyDTO attributeMappings) {
        return toResponse(app, correlation, attributeMappings, null);
    }

    public ApplicationResponseDTO toResponse(Application app, OnboardingRequestDTO.CorrelationDTO correlation,
            OnboardingRequestDTO.AttributeMappingPolicyDTO attributeMappings,
            OnboardingRequestDTO.SynchronizationPolicyDTO synchronizationPolicy) {
        OnboardingRequestDTO.DetailsDTO details = mapDetails(app);
        Map<String, Object> connector = mapConnector(app);
        OnboardingRequestDTO.SchemaDTO schema = mapSchema(app);
        OnboardingRequestDTO.AccessDTO access = mapAccess(app);

        return new ApplicationResponseDTO(
                app.getId(),
                details,
                connector,
                schema,
                correlation,
                attributeMappings,
                access,
                synchronizationPolicy,
                app.getCreatedAt(),
                app.getUpdatedAt());
    }

    private OnboardingRequestDTO.DetailsDTO mapDetails(Application app) {
        try {
            List<OnboardingRequestDTO.OwnerDTO> owners = app.getTechnicalOwners() != null ? objectMapper
                    .convertValue(app.getTechnicalOwners(), new TypeReference<List<OnboardingRequestDTO.OwnerDTO>>() {
                    }) : null;

            List<OnboardingRequestDTO.OwnerDTO> businessOwners = app.getBusinessOwners() != null ? objectMapper
                    .convertValue(app.getBusinessOwners(), new TypeReference<List<OnboardingRequestDTO.OwnerDTO>>() {
                    }) : null;

            List<String> categories = app.getCategories() != null
                    ? objectMapper.convertValue(app.getCategories(), new TypeReference<List<String>>() {
                    })
                    : null;

            List<String> complianceLabels = app.getComplianceLabels() != null
                    ? objectMapper.convertValue(app.getComplianceLabels(), new TypeReference<List<String>>() {
                    })
                    : null;

            List<OnboardingRequestDTO.ScopeDTO> scopes = app.getScopes() != null ? objectMapper
                    .convertValue(app.getScopes(), new TypeReference<List<OnboardingRequestDTO.ScopeDTO>>() {
                    }) : null;

            OnboardingRequestDTO.DepartmentDTO department = app.getDepartment() != null
                    ? objectMapper.convertValue(app.getDepartment(), OnboardingRequestDTO.DepartmentDTO.class)
                    : null;

            String connectorId = null;
            if (app.getConnectorConfiguration() != null && app.getConnectorConfiguration().has("connectorId")) {
                connectorId = app.getConnectorConfiguration().get("connectorId").asText();
            }

            return new OnboardingRequestDTO.DetailsDTO(
                    app.getName(),
                    owners,
                    businessOwners,
                    connectorId,
                    categories,
                    app.isAuthoritative(),
                    app.getIdentityType(),
                    app.getRiskLevel(),
                    complianceLabels,
                    scopes,
                    department,
                    app.getDescription(),
                    app.getAssociatedSchemaId() != null ? app.getAssociatedSchemaId().toString() : null);
        } catch (Exception e) {
            // In case of mapping error, return a partial object or log
            return new OnboardingRequestDTO.DetailsDTO(
                    app.getName(), null, null, null, null, app.isAuthoritative(),
                    app.getIdentityType(), app.getRiskLevel(), null, null, null, app.getDescription(), null);
        }
    }

    private Map<String, Object> mapConnector(Application app) {
        if (app.getConnectorConfiguration() == null)
            return null;
        return objectMapper.convertValue(app.getConnectorConfiguration(), new TypeReference<Map<String, Object>>() {
        });
    }

    private OnboardingRequestDTO.SchemaDTO mapSchema(Application app) {
        if (app.getSchemaAttributes() == null || app.getSchemaAttributes().isEmpty())
            return null;

        Map<String, List<ApplicationSchemaAttribute>> grouped = app.getSchemaAttributes().stream()
                .collect(Collectors.groupingBy(ApplicationSchemaAttribute::getObjectType));

        List<OnboardingRequestDTO.SchemaDefinitionDTO> definitions = grouped.entrySet().stream()
                .map(entry -> {
                    String objectType = entry.getKey();
                    List<ApplicationSchemaAttribute> attrs = entry.getValue();
                    
                    // Assuming all attributes for an object type share the same type
                    String type = attrs.isEmpty() ? null : attrs.get(0).getObjectTypeKind();

                    String displayAttribute = attrs.stream()
                            .filter(ApplicationSchemaAttribute::isDisplayAttribute)
                            .map(ApplicationSchemaAttribute::getName)
                            .findFirst()
                            .orElse(null);

                    String descriptionAttribute = attrs.stream()
                            .filter(ApplicationSchemaAttribute::isDescriptionAttribute)
                            .map(ApplicationSchemaAttribute::getName)
                            .findFirst()
                            .orElse(null);

                    List<OnboardingRequestDTO.SchemaAttributeDTO> attrDtos = attrs.stream()
                            .map(this::mapSchemaAttribute)
                            .collect(Collectors.toList());

                    return new OnboardingRequestDTO.SchemaDefinitionDTO(
                            objectType,
                            OnboardingRequestDTO.ObjectTypeKind.valueOf(type),
                            displayAttribute,
                            descriptionAttribute,
                            attrDtos,
                            Collections.emptyList());
                })
                .collect(Collectors.toList());

        return new OnboardingRequestDTO.SchemaDTO(definitions);
    }

    private OnboardingRequestDTO.SchemaAttributeDTO mapSchemaAttribute(ApplicationSchemaAttribute attr) {
        List<String> flags = Collections.emptyList();
        if (attr.getFlags() != null) {
            try {
                flags = objectMapper.convertValue(attr.getFlags(), new TypeReference<List<String>>() {
                });
            } catch (Exception ignored) {
            }
        }

        return new OnboardingRequestDTO.SchemaAttributeDTO(
                attr.getName(),
                attr.getType(),
                attr.getCategory(),
                attr.getDescription(),
                flags);
    }

    private OnboardingRequestDTO.AccessDTO mapAccess(Application app) {
        if (app.getAccessPolicy() == null)
            return null;
        return objectMapper.convertValue(app.getAccessPolicy(), OnboardingRequestDTO.AccessDTO.class);
    }
}
