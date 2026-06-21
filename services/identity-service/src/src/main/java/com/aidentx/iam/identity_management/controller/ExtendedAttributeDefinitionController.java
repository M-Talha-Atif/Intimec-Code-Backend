package com.aidentx.iam.identity_management.controller;

import com.aidentx.iam.identity_management.model.dto.ExtendedAttributeDtos.*;
import com.aidentx.iam.identity_management.service.ExtendedAttributeDefinitionService;
import com.aidentx.iam.security.JwtTenantIdService;
import jakarta.validation.Valid;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/identity/schema/")
public class ExtendedAttributeDefinitionController {

    private final ExtendedAttributeDefinitionService definitionService;
    private final JwtTenantIdService jwtTenantIdService;

    public ExtendedAttributeDefinitionController(ExtendedAttributeDefinitionService definitionService,
            JwtTenantIdService jwtTenantIdService) {
        this.definitionService = definitionService;
        this.jwtTenantIdService = jwtTenantIdService;
    }

    /**
     * Creates a new custom attribute definition for EMPLOYEE identity type.
     *
     * @param createDto The definition of the new attribute
     * @return The created attribute definition.
     */
    @PostMapping("/employee")
    public ResponseEntity<DefinitionResponseDto> createEmployeeDefinition(@Valid @RequestBody DefinitionCreateWithoutTypeDto createDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        DefinitionResponseDto createdDefinition = definitionService.createEmployeeDefinition(tenantId, createDto);
        return new ResponseEntity<>(createdDefinition, HttpStatus.CREATED);
    }

    /**
     * Creates a new custom attribute definition for NON_EMPLOYEE identity type.
     *
     * @param createDto The definition of the new attribute
     * @return The created attribute definition.
     */
    @PostMapping("/non_employee")
    public ResponseEntity<DefinitionResponseDto> createNonEmployeeDefinition(@Valid @RequestBody DefinitionCreateWithoutTypeDto createDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        DefinitionResponseDto createdDefinition = definitionService.createNonEmployeeDefinition(tenantId, createDto);
        return new ResponseEntity<>(createdDefinition, HttpStatus.CREATED);
    }

    /**
     * Creates a new custom attribute definition for NON_HUMAN identity type.
     *
     * @param createDto The definition of the new attribute
     * @return The created attribute definition.
     */
    @PostMapping("/non_human")
    public ResponseEntity<DefinitionResponseDto> createNonHumanDefinition(@Valid @RequestBody DefinitionCreateWithoutTypeDto createDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        DefinitionResponseDto createdDefinition = definitionService.createNonHumanDefinition(tenantId, createDto);
        return new ResponseEntity<>(createdDefinition, HttpStatus.CREATED);
    }

    /**
     * Updates an existing custom attribute definition for EMPLOYEE identity type.
     *
     * @param systemKey The system key of the definition to update.
     * @param updateDto The updated definition details.
     * @return The updated attribute definition.
     */
    @PutMapping("/employee/{systemKey}")
    public ResponseEntity<DefinitionResponseDto> updateEmployeeDefinition(
            @PathVariable String systemKey,
            @Valid @RequestBody DefinitionUpdateDto updateDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        DefinitionResponseDto updatedDefinition = definitionService.updateEmployeeDefinition(tenantId, systemKey, updateDto);
        return ResponseEntity.ok(updatedDefinition);
    }

    /**
     * Updates an existing custom attribute definition for NON_EMPLOYEE identity type.
     *
     * @param systemKey The system key of the definition to update.
     * @param updateDto The updated definition details.
     * @return The updated attribute definition.
     */
    @PutMapping("/non_employee/{systemKey}")
    public ResponseEntity<DefinitionResponseDto> updateNonEmployeeDefinition(
            @PathVariable String systemKey,
            @Valid @RequestBody DefinitionUpdateDto updateDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        DefinitionResponseDto updatedDefinition = definitionService.updateNonEmployeeDefinition(tenantId, systemKey, updateDto);
        return ResponseEntity.ok(updatedDefinition);
    }

    /**
     * Updates an existing custom attribute definition for NON_HUMAN identity type.
     *
     * @param systemKey The system key of the definition to update.
     * @param updateDto The updated definition details.
     * @return The updated attribute definition.
     */
    @PutMapping("/non_human/{systemKey}")
    public ResponseEntity<DefinitionResponseDto> updateNonHumanDefinition(
            @PathVariable String systemKey,
            @Valid @RequestBody DefinitionUpdateDto updateDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        DefinitionResponseDto updatedDefinition = definitionService.updateNonHumanDefinition(tenantId, systemKey, updateDto);
        return ResponseEntity.ok(updatedDefinition);
    }

    /**
     * Deletes an extended attribute definition for EMPLOYEE identity type.
     *
     * @param systemKey The system key of the definition to delete.
     * @return No content response.
     */
    @DeleteMapping("/employee/{systemKey}")
    public ResponseEntity<Void> deleteEmployeeDefinitionBySystemKey(@PathVariable String systemKey) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        definitionService.deleteEmployeeDefinitionBySystemKey(tenantId, systemKey);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes an extended attribute definition for NON_EMPLOYEE identity type.
     *
     * @param systemKey The system key of the definition to delete.
     * @return No content response.
     */
    @DeleteMapping("/non_employee/{systemKey}")
    public ResponseEntity<Void> deleteNonEmployeeDefinitionBySystemKey(@PathVariable String systemKey) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        definitionService.deleteNonEmployeeDefinitionBySystemKey(tenantId, systemKey);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deletes an extended attribute definition for NON_HUMAN identity type.
     *
     * @param systemKey The system key of the definition to delete.
     * @return No content response.
     */
    @DeleteMapping("/non_human/{systemKey}")
    public ResponseEntity<Void> deleteNonHumanDefinitionBySystemKey(@PathVariable String systemKey) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        definitionService.deleteNonHumanDefinitionBySystemKey(tenantId, systemKey);
        return ResponseEntity.noContent().build();
    }

    /**
     * Gets schema statistics for EMPLOYEE identity type.
     *
     * @return Statistics about the schema for the identity type.
     */
    @GetMapping("/employee/statistics")
    public ResponseEntity<SchemaStatisticsDto> getEmployeeSchemaStatistics() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        SchemaStatisticsDto statistics = definitionService.getEmployeeSchemaStatistics(tenantId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Gets schema statistics for NON_EMPLOYEE identity type.
     *
     * @return Statistics about the schema for the identity type.
     */
    @GetMapping("/non_employee/statistics")
    public ResponseEntity<SchemaStatisticsDto> getNonEmployeeSchemaStatistics() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        SchemaStatisticsDto statistics = definitionService.getNonEmployeeSchemaStatistics(tenantId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Gets schema statistics for NON_HUMAN identity type.
     *
     * @return Statistics about the schema for the identity type.
     */
    @GetMapping("/non_human/statistics")
    public ResponseEntity<SchemaStatisticsDto> getNonHumanSchemaStatistics() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        SchemaStatisticsDto statistics = definitionService.getNonHumanSchemaStatistics(tenantId);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Gets all attribute definitions for EMPLOYEE identity type.
     *
     * @return A list of attribute definitions.
     */
    @GetMapping("/employee/definitions")
    public ResponseEntity<List<DefinitionResponseDto>> getEmployeeDefinitions() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        List<DefinitionResponseDto> definitions = definitionService.findEmployeeDefinitions(tenantId);
        return ResponseEntity.ok(definitions);
    }

    /**
     * Gets all attribute definitions for NON_EMPLOYEE identity type.
     *
     * @return A list of attribute definitions.
     */
    @GetMapping("/non_employee/definitions")
    public ResponseEntity<List<DefinitionResponseDto>> getNonEmployeeDefinitions() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        List<DefinitionResponseDto> definitions = definitionService.findNonEmployeeDefinitions(tenantId);
        return ResponseEntity.ok(definitions);
    }

    /**
     * Gets all attribute definitions for NON_HUMAN identity type.
     *
     * @return A list of attribute definitions.
     */
    @GetMapping("/non_human/definitions")
    public ResponseEntity<List<DefinitionResponseDto>> getNonHumanDefinitions() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        List<DefinitionResponseDto> definitions = definitionService.findNonHumanDefinitions(tenantId);
        return ResponseEntity.ok(definitions);
    }

    /**
     * Gets a single attribute definition for EMPLOYEE identity type by system key.
     *
     * @param systemKey The system key of the definition.
     * @return The attribute definition.
     */
    @GetMapping("/employee/{systemKey}")
    public ResponseEntity<DefinitionResponseDto> getEmployeeDefinitionBySystemKey(@PathVariable String systemKey) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        DefinitionResponseDto definition = definitionService.getEmployeeDefinitionBySystemKey(tenantId, systemKey);
        return ResponseEntity.ok(definition);
    }

    /**
     * Gets a single attribute definition for NON_EMPLOYEE identity type by system key.
     *
     * @param systemKey The system key of the definition.
     * @return The attribute definition.
     */
    @GetMapping("/non_employee/{systemKey}")
    public ResponseEntity<DefinitionResponseDto> getNonEmployeeDefinitionBySystemKey(@PathVariable String systemKey) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        DefinitionResponseDto definition = definitionService.getNonEmployeeDefinitionBySystemKey(tenantId, systemKey);
        return ResponseEntity.ok(definition);
    }

    /**
     * Gets a single attribute definition for NON_HUMAN identity type by system key.
     *
     * @param systemKey The system key of the definition.
     * @return The attribute definition.
     */
    @GetMapping("/non_human/{systemKey}")
    public ResponseEntity<DefinitionResponseDto> getNonHumanDefinitionBySystemKey(@PathVariable String systemKey) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        DefinitionResponseDto definition = definitionService.getNonHumanDefinitionBySystemKey(tenantId, systemKey);
        return ResponseEntity.ok(definition);
    }
}
