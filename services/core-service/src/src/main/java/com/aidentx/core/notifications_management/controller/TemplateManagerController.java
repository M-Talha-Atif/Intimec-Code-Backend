package com.aidentx.core.notifications_management.controller;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerCreateDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerResponseDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerTestDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerTestResultDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerUpdateDto;
import com.aidentx.core.notifications_management.model.dto.TemplateVersionResponseDto;
import com.aidentx.core.notifications_management.service.TemplateManagerService;
import com.aidentx.core.shared.security.TenantContextService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * REST controller for managing email template managers with tenant-aware security.
 * <p>
 * This controller provides HTTP endpoints for creating, updating, retrieving,
 * and deleting email template managers. All operations are automatically
 * scoped to the tenant of the authenticated user extracted from JWT token.
 * Each template manager defines email templates for specific trigger types
 * and languages, supporting internationalization.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
@RestController
@RequestMapping("/api/v1/notifications/template-managers")
public class TemplateManagerController {
    
    private final TemplateManagerService templateManagerService;
    private final TenantContextService tenantContextService;

    /**
     * Constructor for TemplateManagerController.
     * 
     * @param templateManagerService the template manager service
     * @param tenantContextService the tenant context service for JWT tenant extraction
     */
    public TemplateManagerController(TemplateManagerService templateManagerService,
                                   TenantContextService tenantContextService) {
        this.templateManagerService = templateManagerService;
        this.tenantContextService = tenantContextService;
    }
    
    /**
     * Creates a new template manager for the current user's tenant.
     * <p>
     * This endpoint creates a new email template manager with the provided configuration.
     * The tenant ID, user ID, and username are automatically extracted from the JWT token.
     * Each template manager is specific to a tenant, trigger type, and language combination.
     * The template can include Velocity template variables for dynamic content.
     * </p>
     * 
     * @param createDto the template manager creation data
     * @return ResponseEntity containing the created template manager
     */
    @PostMapping
    public ResponseEntity<TemplateManagerResponseDto> createTemplateManager(
            @RequestBody TemplateManagerCreateDto createDto) {
        // Extract tenant context from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();
        String userId = tenantContextService.getCurrentUserId();
        String username = tenantContextService.getCurrentUsername();

        // Set tenant context in the DTO
        createDto.setTenantId(tenantId);

        // Call service with the updated DTO
        TemplateManagerResponseDto response = templateManagerService.createTemplateManager(createDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    /**
     * Updates an existing template manager.
     * <p>
     * This endpoint allows partial updates of template manager properties.
     * Only allows updates if the template manager belongs to the current user's tenant.
     * Only provided fields will be updated, allowing for flexible modifications
     * while preserving existing data.
     * </p>
     * 
     * @param id the unique identifier of the template manager to update
     * @param updateDto the template manager update data
     * @return ResponseEntity containing the updated template manager
     */
    @PutMapping("/{id}")
    public ResponseEntity<TemplateManagerResponseDto> updateTemplateManager(
            @PathVariable UUID id,
            @RequestBody TemplateManagerUpdateDto updateDto) {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();

        // First, get the existing template to validate tenant ownership
        TemplateManagerResponseDto existingTemplate = templateManagerService.getTemplateManagerById(id);

        // Validate that the template belongs to the current user's tenant
        if (!tenantId.equals(existingTemplate.getTenantId())) {
            throw new RuntimeException(String.format(
                AppConstants.MSG_TEMPLATE_TENANT_ACCESS_DENIED,
                existingTemplate.getTenantId(), tenantId));
        }

        // Proceed with update since tenant access is validated
        TemplateManagerResponseDto response = templateManagerService.updateTemplateManager(id, updateDto);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retrieves a template manager by its unique identifier.
     * <p>
     * This endpoint returns the complete template manager information
     * including the template content and configuration settings.
     * Only allows access if the template manager belongs to the current user's tenant.
     * </p>
     * 
     * @param id the unique identifier of the template manager
     * @return ResponseEntity containing the template manager
     */
    @GetMapping("/{id}")
    public ResponseEntity<TemplateManagerResponseDto> getTemplateManagerById(
            @PathVariable UUID id) {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();

        // Get the template and validate tenant ownership
        TemplateManagerResponseDto response = templateManagerService.getTemplateManagerById(id);

        // Validate that the template belongs to the current user's tenant
        if (!tenantId.equals(response.getTenantId())) {
            throw new RuntimeException(String.format(
                AppConstants.MSG_TEMPLATE_TENANT_ACCESS_DENIED,
                response.getTenantId(), tenantId));
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all template managers for the current user's tenant.
     * <p>
     * This endpoint returns all template managers associated with the current user's tenant.
     * The tenant ID is automatically extracted from the JWT token.
     * This includes both enabled and disabled templates, allowing
     * administrators to view the complete template configuration for their tenant.
     * </p>
     *
     * @return ResponseEntity containing the list of template managers
     */
    @GetMapping("/current")
    public ResponseEntity<List<TemplateManagerResponseDto>> getCurrentTenantTemplateManagers() {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();

        List<TemplateManagerResponseDto> response = templateManagerService.getTemplateManagersByTenant(tenantId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retrieves all template managers for a specific tenant.
     * <p>
     * This endpoint returns all template managers associated with the provided
     * tenant ID. Only accessible if the requesting user belongs to the specified tenant.
     * This includes both enabled and disabled templates, allowing
     * administrators to view the complete template configuration for a tenant.
     * </p>
     * 
     * @param tenantId the unique identifier of the tenant
     * @return ResponseEntity containing the list of template managers
     * @deprecated Use /current endpoint instead for automatic tenant resolution
     */
    @GetMapping("/tenant/{tenantId}")
    @Deprecated
    public ResponseEntity<List<TemplateManagerResponseDto>> getTemplateManagersByTenant(
            @PathVariable String tenantId) {
        // Validate that the user belongs to the requested tenant
        tenantContextService.validateTenantAccess(tenantId);

        List<TemplateManagerResponseDto> response = templateManagerService.getTemplateManagersByTenant(tenantId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Deletes a template manager.
     * <p>
     * This endpoint permanently removes a template manager from the system.
     * Only allows deletion if the template manager belongs to the current user's tenant.
     * This operation cannot be undone. Ensure that the template is not
     * currently in use before deletion.
     * </p>
     * 
     * @param id the unique identifier of the template manager to delete
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplateManager(@PathVariable UUID id) {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();

        // First, get the existing template to validate tenant ownership
        TemplateManagerResponseDto existingTemplate = templateManagerService.getTemplateManagerById(id);

        // Validate that the template belongs to the current user's tenant
        if (!tenantId.equals(existingTemplate.getTenantId())) {
            throw new RuntimeException(String.format(
                AppConstants.MSG_TEMPLATE_TENANT_ACCESS_DENIED,
                existingTemplate.getTenantId(), tenantId));
        }

        // Proceed with deletion since tenant access is validated
        templateManagerService.deleteTemplateManager(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Gets available template variables for a specific trigger type.
     * <p>
     * This endpoint returns the list of available variables that can be used
     * in notification templates for the specified trigger type.
     * Currently supports: PASSWORD_RESET
     * </p>
     * 
     * @param triggerType the trigger type (e.g., PASSWORD_RESET)
     * @return ResponseEntity containing the list of available variables
     */
    @GetMapping("/variables")
    public ResponseEntity<List<Map<String, Object>>> getAllTemplateVariables() {

        List<Map<String, Object>> responseList = new ArrayList<>();

        // PASSWORD_RESET
        Map<String, Object> passwordReset = new HashMap<>();
        passwordReset.put("triggerType", "PASSWORD_RESET");
        passwordReset.put("templateVariables", Arrays.asList("email", "username"));
        responseList.add(passwordReset);

        // PASSWORD_REMINDER_RESET
        Map<String, Object> passwordReminderReset = new HashMap<>();
        passwordReminderReset.put("triggerType", "PASSWORD_REMINDER_RESET");
        passwordReminderReset.put("templateVariables", Arrays.asList("email", "username", "expirationDays"));
        responseList.add(passwordReminderReset);

        // ASSIGN_INTERNAL_ROLE
        Map<String, Object> assignInternalRole = new HashMap<>();
        assignInternalRole.put("triggerType", "ASSIGN_INTERNAL_ROLE");
        assignInternalRole.put("templateVariables", Arrays.asList("username", "email", "roleName"));
        responseList.add(assignInternalRole);

        // REVOKE_INTERNAL_ROLE
        Map<String, Object> revokeInternalRole = new HashMap<>();
        revokeInternalRole.put("triggerType", "REVOKE_INTERNAL_ROLE");
        revokeInternalRole.put("templateVariables", Arrays.asList("username", "email", "roleName"));
        responseList.add(revokeInternalRole);

        return ResponseEntity.ok(responseList);
    }

    
    // ===== VERSIONING ENDPOINTS =====
    
    /**
     * Retrieves version history for a specific template.
     * <p>
     * This endpoint returns all versions of a template ordered by version number 
     * in descending order (newest first). Access is restricted to templates
     * owned by the current user's tenant.
     * </p>
     * 
     * @param id the unique identifier of the template
     * @return ResponseEntity containing the list of template versions
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<TemplateVersionResponseDto>> getTemplateVersionHistory(@PathVariable UUID id) {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();

        // First, get the existing template to validate tenant ownership
        TemplateManagerResponseDto existingTemplate = templateManagerService.getTemplateManagerById(id);

        // Validate that the template belongs to the current user's tenant
        if (!tenantId.equals(existingTemplate.getTenantId())) {
            throw new RuntimeException(String.format(
                AppConstants.MSG_TEMPLATE_TENANT_ACCESS_DENIED,
                existingTemplate.getTenantId(), tenantId));
        }

        // Proceed with getting version history since tenant access is validated
        List<TemplateVersionResponseDto> versions = templateManagerService.getTemplateVersionHistory(id);
        return ResponseEntity.ok(versions);
    }
    
    /**
     * Retrieves a specific version of a template.
     * <p>
     * This endpoint returns the data for a specific version number of a template.
     * Access is restricted to templates owned by the current user's tenant.
     * </p>
     * 
     * @param id the unique identifier of the template
     * @param versionNumber the version number to retrieve
     * @return ResponseEntity containing the template version data
     */
    @GetMapping("/{id}/versions/{versionNumber}")
    public ResponseEntity<TemplateVersionResponseDto> getTemplateVersion(
            @PathVariable UUID id, 
            @PathVariable Integer versionNumber) {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();

        // First, get the existing template to validate tenant ownership
        TemplateManagerResponseDto existingTemplate = templateManagerService.getTemplateManagerById(id);

        // Validate that the template belongs to the current user's tenant
        if (!tenantId.equals(existingTemplate.getTenantId())) {
            throw new RuntimeException(String.format(
                AppConstants.MSG_TEMPLATE_TENANT_ACCESS_DENIED,
                existingTemplate.getTenantId(), tenantId));
        }

        // Proceed with getting specific version since tenant access is validated
        TemplateVersionResponseDto version = templateManagerService.getTemplateVersion(id, versionNumber);
        return ResponseEntity.ok(version);
    }
    
    /**
     * Restores a template to a previous version.
     * <p>
     * This endpoint reverts the template to a previous version by setting the current
     * version pointer to the specified version number. No new version is created during
     * restore - it simply makes an existing version the active one. Only UPDATE operations
     * create new versions. Access is restricted to templates owned by the current user's tenant.
     * </p>
     * 
     * @param id the unique identifier of the template
     * @param versionNumber the version number to restore to
     * @param restoreRequest optional request body containing changeComment
     * @return ResponseEntity containing the restored template
     */
    @PostMapping("/{id}/versions/{versionNumber}/restore")
    public ResponseEntity<TemplateManagerResponseDto> restoreTemplateVersion(
            @PathVariable UUID id, 
            @PathVariable Integer versionNumber,
            @RequestBody(required = false) Map<String, String> restoreRequest) {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();

        // First, get the existing template to validate tenant ownership
        TemplateManagerResponseDto existingTemplate = templateManagerService.getTemplateManagerById(id);

        // Validate that the template belongs to the current user's tenant
        if (!tenantId.equals(existingTemplate.getTenantId())) {
            throw new RuntimeException(String.format(
                AppConstants.MSG_TEMPLATE_TENANT_ACCESS_DENIED,
                existingTemplate.getTenantId(), tenantId));
        }

        // Extract change comment from request body if provided
        String changeComment = null;
        if (restoreRequest != null) {
            changeComment = restoreRequest.get("changeComment");
        }

        // Proceed with restoration since tenant access is validated
        TemplateManagerResponseDto restoredTemplate = templateManagerService.restoreTemplateVersion(id, versionNumber, changeComment);
        return ResponseEntity.ok(restoredTemplate);
    }
    
    /**
     * Tests a template manager by sending a test email with resolved variables.
     * <p>
     * This endpoint allows testing template managers by providing template content
     * and variables. The tenant is automatically extracted from the JWT token,
     * and the tenant's email configuration is used for sending the test email.
     * Template variables are processed and resolved before sending.
     * </p>
     * 
     * @param testDto the template test data including content and variables
     * @return ResponseEntity containing the test results with processed content
     */
    @PostMapping("/test")
    public ResponseEntity<TemplateManagerTestResultDto> testTemplateManager(
            @RequestBody TemplateManagerTestDto testDto) {
        // Service will extract tenant ID from JWT token internally
        TemplateManagerTestResultDto result = templateManagerService.testTemplateManager(testDto);
        return ResponseEntity.ok(result);
    }

}
