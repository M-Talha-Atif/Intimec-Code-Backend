package com.aidentx.iam.identity_management.service;

import com.aidentx.iam.identity_management.model.dto.ExtendedAttributeDtos.*;
import java.util.List;

public interface ExtendedAttributeDefinitionService {

    /**
     * Retrieves all custom attribute definitions for EMPLOYEE identity type within a tenant.
     *
     * @param tenantId The tenant ID.
     * @return A list of attribute definitions.
     */
    List<DefinitionResponseDto> findEmployeeDefinitions(String tenantId);

    /**
     * Retrieves all custom attribute definitions for NON_EMPLOYEE identity type within a tenant.
     *
     * @param tenantId The tenant ID.
     * @return A list of attribute definitions.
     */
    List<DefinitionResponseDto> findNonEmployeeDefinitions(String tenantId);

    /**
     * Retrieves all custom attribute definitions for NON_HUMAN identity type within a tenant.
     *
     * @param tenantId The tenant ID.
     * @return A list of attribute definitions.
     */
    List<DefinitionResponseDto> findNonHumanDefinitions(String tenantId);

    /**
     * Creates a new custom attribute definition for EMPLOYEE identity type.
     *
     * @param tenantId The tenant ID.
     * @param createDto The DTO with the definition details.
     * @return The newly created definition.
     */
    DefinitionResponseDto createEmployeeDefinition(String tenantId, DefinitionCreateWithoutTypeDto createDto);

    /**
     * Creates a new custom attribute definition for NON_EMPLOYEE identity type.
     *
     * @param tenantId The tenant ID.
     * @param createDto The DTO with the definition details.
     * @return The newly created definition.
     */
    DefinitionResponseDto createNonEmployeeDefinition(String tenantId, DefinitionCreateWithoutTypeDto createDto);

    /**
     * Creates a new custom attribute definition for NON_HUMAN identity type.
     *
     * @param tenantId The tenant ID.
     * @param createDto The DTO with the definition details.
     * @return The newly created definition.
     */
    DefinitionResponseDto createNonHumanDefinition(String tenantId, DefinitionCreateWithoutTypeDto createDto);

    /**
     * Updates an existing custom attribute definition for EMPLOYEE identity type.
     *
     * @param tenantId The tenant ID.
     * @param systemKey The system key of the definition to update.
     * @param updateDto The DTO with the updated details.
     * @return The updated definition.
     */
    DefinitionResponseDto updateEmployeeDefinition(String tenantId, String systemKey, DefinitionUpdateDto updateDto);

    /**
     * Updates an existing custom attribute definition for NON_EMPLOYEE identity type.
     *
     * @param tenantId The tenant ID.
     * @param systemKey The system key of the definition to update.
     * @param updateDto The DTO with the updated details.
     * @return The updated definition.
     */
    DefinitionResponseDto updateNonEmployeeDefinition(String tenantId, String systemKey, DefinitionUpdateDto updateDto);

    /**
     * Updates an existing custom attribute definition for NON_HUMAN identity type.
     *
     * @param tenantId The tenant ID.
     * @param systemKey The system key of the definition to update.
     * @param updateDto The DTO with the updated details.
     * @return The updated definition.
     */
    DefinitionResponseDto updateNonHumanDefinition(String tenantId, String systemKey, DefinitionUpdateDto updateDto);

    /**
     * Deletes a custom attribute definition for EMPLOYEE identity type.
     *
     * @param tenantId The tenant ID.
     * @param systemKey The system key of the definition to delete.
     */
    void deleteEmployeeDefinitionBySystemKey(String tenantId, String systemKey);

    /**
     * Deletes a custom attribute definition for NON_EMPLOYEE identity type.
     *
     * @param tenantId The tenant ID.
     * @param systemKey The system key of the definition to delete.
     */
    void deleteNonEmployeeDefinitionBySystemKey(String tenantId, String systemKey);

    /**
     * Deletes a custom attribute definition for NON_HUMAN identity type.
     *
     * @param tenantId The tenant ID.
     * @param systemKey The system key of the definition to delete.
     */
    void deleteNonHumanDefinitionBySystemKey(String tenantId, String systemKey);

    /**
     * Gets schema statistics for EMPLOYEE identity type.
     *
     * @param tenantId The tenant ID.
     * @return Statistics about the schema for the identity type.
     */
    SchemaStatisticsDto getEmployeeSchemaStatistics(String tenantId);

    /**
     * Gets schema statistics for NON_EMPLOYEE identity type.
     *
     * @param tenantId The tenant ID.
     * @return Statistics about the schema for the identity type.
     */
    SchemaStatisticsDto getNonEmployeeSchemaStatistics(String tenantId);

    /**
     * Gets schema statistics for NON_HUMAN identity type.
     *
     * @param tenantId The tenant ID.
     * @return Statistics about the schema for the identity type.
     */
    SchemaStatisticsDto getNonHumanSchemaStatistics(String tenantId);

    /**
     * Gets a single attribute definition for EMPLOYEE identity type by system key.
     *
     * @param tenantId The tenant ID.
     * @param systemKey The system key of the definition.
     * @return The attribute definition.
     */
    DefinitionResponseDto getEmployeeDefinitionBySystemKey(String tenantId, String systemKey);

    /**
     * Gets a single attribute definition for NON_EMPLOYEE identity type by system key.
     *
     * @param tenantId The tenant ID.
     * @param systemKey The system key of the definition.
     * @return The attribute definition.
     */
    DefinitionResponseDto getNonEmployeeDefinitionBySystemKey(String tenantId, String systemKey);

    /**
     * Gets a single attribute definition for NON_HUMAN identity type by system key.
     *
     * @param tenantId The tenant ID.
     * @param systemKey The system key of the definition.
     * @return The attribute definition.
     */
    DefinitionResponseDto getNonHumanDefinitionBySystemKey(String tenantId, String systemKey);
}
