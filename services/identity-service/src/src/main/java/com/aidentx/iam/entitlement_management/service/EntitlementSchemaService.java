package com.aidentx.iam.entitlement_management.service;

import com.aidentx.iam.entitlement_management.model.dto.EntitlementSchemaDtos;

import java.util.List;

public interface EntitlementSchemaService {
    EntitlementSchemaDtos.EntitlementSchemaResponseDto getSchema(String tenantId);

    EntitlementSchemaDtos.ExtendedAttributeDto createDefinition(String tenantId,
                                                                EntitlementSchemaDtos.DefinitionCreateDto createDto);

    EntitlementSchemaDtos.ExtendedAttributeDto updateDefinition(String tenantId,
                                                                String systemKey,
                                                                EntitlementSchemaDtos.DefinitionUpdateDto updateDto);

    void deleteDefinition(String tenantId, String systemKey);

    EntitlementSchemaDtos.ExtendedAttributeDto getDefinitionBySystemKey(String tenantId, String systemKey);

    List<EntitlementSchemaDtos.ExtendedAttributeDto> findDefinitions(String tenantId);

    EntitlementSchemaDtos.SchemaStatisticsDto getSchemaStatistics(String tenantId);

    List<String> getCustomAttributeKeys(String tenantId);
}
