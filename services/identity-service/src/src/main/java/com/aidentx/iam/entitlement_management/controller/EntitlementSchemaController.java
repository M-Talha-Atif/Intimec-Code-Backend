package com.aidentx.iam.entitlement_management.controller;

import com.aidentx.iam.entitlement_management.model.dto.EntitlementSchemaDtos;
import com.aidentx.iam.entitlement_management.service.EntitlementSchemaService;
import com.aidentx.iam.security.JwtTenantIdService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/identity/schema/entitlement")
public class EntitlementSchemaController {

    private final EntitlementSchemaService schemaService;
    private final JwtTenantIdService jwtTenantIdService;

    public EntitlementSchemaController(EntitlementSchemaService schemaService,
                                       JwtTenantIdService jwtTenantIdService) {
        this.schemaService = schemaService;
        this.jwtTenantIdService = jwtTenantIdService;
    }

    @GetMapping
    public ResponseEntity<EntitlementSchemaDtos.EntitlementSchemaResponseDto> getSchema() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        return ResponseEntity.ok(schemaService.getSchema(tenantId));
    }

    @PostMapping
    public ResponseEntity<EntitlementSchemaDtos.ExtendedAttributeDto> createDefinition(
            @Valid @RequestBody EntitlementSchemaDtos.DefinitionCreateDto createDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        EntitlementSchemaDtos.ExtendedAttributeDto created = schemaService.createDefinition(tenantId, createDto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/{systemKey}")
    public ResponseEntity<EntitlementSchemaDtos.ExtendedAttributeDto> getDefinitionBySystemKey(@PathVariable String systemKey) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        return ResponseEntity.ok(schemaService.getDefinitionBySystemKey(tenantId, systemKey));
    }

    @PutMapping("/{systemKey}")
    public ResponseEntity<EntitlementSchemaDtos.ExtendedAttributeDto> updateDefinition(
            @PathVariable String systemKey,
            @Valid @RequestBody EntitlementSchemaDtos.DefinitionUpdateDto updateDto) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        return ResponseEntity.ok(schemaService.updateDefinition(tenantId, systemKey, updateDto));
    }

    @DeleteMapping("/{systemKey}")
    public ResponseEntity<Void> deleteDefinition(@PathVariable String systemKey) {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        schemaService.deleteDefinition(tenantId, systemKey);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/definitions")
    public ResponseEntity<List<EntitlementSchemaDtos.ExtendedAttributeDto>> getDefinitions() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        return ResponseEntity.ok(schemaService.findDefinitions(tenantId));
    }

    @GetMapping("/statistics")
    public ResponseEntity<EntitlementSchemaDtos.SchemaStatisticsDto> getStatistics() {
        String tenantId = jwtTenantIdService.extractTenantIdFromRequest();
        return ResponseEntity.ok(schemaService.getSchemaStatistics(tenantId));
    }
}
