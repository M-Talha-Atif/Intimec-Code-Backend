package com.aidentx.core.notifications_management.controller;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.model.dto.*;
import com.aidentx.core.notifications_management.service.EmailConfigurationService;
import com.aidentx.core.shared.security.TenantContextService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing email configurations with tenant-aware security.
 * <p>
 * This controller provides HTTP endpoints for creating, updating, retrieving,
 * deleting, and testing email configurations. All operations are automatically
 * scoped to the tenant of the authenticated user extracted from JWT token.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@RestController
@RequestMapping("/api/v1/notifications/email-configurations")
public class EmailConfigurationController {
    
    private final EmailConfigurationService emailConfigurationService;
    private final TenantContextService tenantContextService;

    /**
     * Constructor for EmailConfigurationController.
     * 
     * @param emailConfigurationService the email configuration service
     * @param tenantContextService the tenant context service for JWT tenant extraction
     */
    @Autowired
    public EmailConfigurationController(EmailConfigurationService emailConfigurationService,
                                      TenantContextService tenantContextService) {
        this.emailConfigurationService = emailConfigurationService;
        this.tenantContextService = tenantContextService;
    }
    
    /**
     * Creates a new email configuration for the current user's tenant.
     * The tenant ID is automatically extracted from the JWT token.
     * 
     * @param createDto the email configuration creation data
     * @return ResponseEntity containing the created email configuration
     */
    @PostMapping
    public ResponseEntity<EmailConfigurationResponseDto> createEmailConfiguration(
            @RequestBody EmailConfigurationCreateDto createDto) {
        // Extract tenant ID from JWT token and set it in the DTO
        String tenantId = tenantContextService.getCurrentTenantId();
        createDto.setTenantId(tenantId);

        EmailConfigurationResponseDto response = emailConfigurationService.createEmailConfiguration(createDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    /**
     * Creates or updates an email configuration for the current user's tenant.
     * The tenant ID is automatically extracted from the JWT token.
     * <p>
     * This endpoint provides an upsert operation - if a configuration already exists
     * for the tenant, it will be updated. Otherwise, a new configuration will be created.
     * This ensures each tenant has exactly one email configuration.
     * </p>
     * 
     * @param createDto the email configuration creation data
     * @return ResponseEntity containing the created or updated email configuration
     */
    @PostMapping("/upsert")
    public ResponseEntity<EmailConfigurationResponseDto> createOrUpdateEmailConfiguration(
            @RequestBody EmailConfigurationCreateDto createDto) {
        // Extract tenant ID from JWT token and set it in the DTO
        String tenantId = tenantContextService.getCurrentTenantId();
        createDto.setTenantId(tenantId);

        EmailConfigurationResponseDto response = emailConfigurationService.createOrUpdateEmailConfiguration(createDto);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Updates the email configuration for the current user's tenant.
     * Automatically finds and updates the configuration based on tenant ID from JWT.
     * Since each tenant has only one email configuration, no ID parameter is needed.
     *
     * @param updateDto the email configuration update data
     * @return ResponseEntity containing the updated email configuration
     */
    @PutMapping
    public ResponseEntity<EmailConfigurationResponseDto> updateEmailConfiguration(
            @RequestBody EmailConfigurationUpdateDto updateDto) {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();

        // Update the configuration for the current tenant
        EmailConfigurationResponseDto response = emailConfigurationService.updateEmailConfigurationByTenant(tenantId, updateDto);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retrieves the email configuration for the current user's tenant.
     * The tenant ID is automatically extracted from the JWT token.
     *
     * @return ResponseEntity containing the email configuration
     */
    @GetMapping("/current")
    public ResponseEntity<EmailConfigurationResponseDto> getCurrentTenantEmailConfiguration() {
        // Extract tenant ID from JWT token
        String tenantId = tenantContextService.getCurrentTenantId();

        EmailConfigurationResponseDto response = emailConfigurationService.getEmailConfigurationByTenant(tenantId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the email configuration for a specific tenant.
     * Only accessible if the requesting user belongs to the specified tenant.
     *
     * @param tenantId the unique identifier of the tenant
     * @return ResponseEntity containing the email configuration
     * @deprecated Use /current endpoint instead for automatic tenant resolution
     */
    @GetMapping("/tenant/{tenantId}")
    @Deprecated
    public ResponseEntity<EmailConfigurationResponseDto> getEmailConfigurationByTenant(
            @PathVariable String tenantId) {
        // Validate that the user belongs to the requested tenant
        tenantContextService.validateTenantAccess(tenantId);

        EmailConfigurationResponseDto response = emailConfigurationService.getEmailConfigurationByTenant(tenantId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Deletes an email configuration.
     * Only allows deletion if the configuration belongs to the current user's tenant.
     *
     * @param id the unique identifier of the configuration to delete
     * @return ResponseEntity with no content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmailConfiguration(@PathVariable UUID id) {
        // Extract tenant ID from JWT token for validation
        String tenantId = tenantContextService.getCurrentTenantId();

        // First, get the existing configuration to validate tenant ownership
        EmailConfigurationResponseDto existingConfig = emailConfigurationService.getEmailConfigurationById(id);

        // Validate that the configuration belongs to the current user's tenant
        if (!tenantId.equals(existingConfig.getTenantId())) {
            throw new RuntimeException(String.format(
                AppConstants.MSG_EMAIL_CONFIG_TENANT_ACCESS_DENIED,
                existingConfig.getTenantId(), tenantId));
        }

        // Proceed with deletion since tenant access is validated
        emailConfigurationService.deleteEmailConfiguration(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Tests an email configuration by sending a test email using the provided configuration.
     * <p>
     * This endpoint allows testing email configurations without saving them to the database.
     * It validates the configuration and attempts to send a test email to the specified address.
     * The configuration is automatically associated with the current user's tenant.
     * </p>
     * 
     * @param configDto the email configuration to test
     * @param testEmailAddress the email address to send the test email to
     * @return ResponseEntity containing the test result
     */
    @PostMapping("/test")
    public ResponseEntity<Boolean> testEmailConfiguration(
            @RequestBody EmailConfigurationCreateDto configDto,
            @RequestParam String testEmailAddress) {
        // Extract tenant ID from JWT token and set it in the DTO
        String tenantId = tenantContextService.getCurrentTenantId();
        configDto.setTenantId(tenantId);

        boolean testResult = emailConfigurationService.testEmailConfiguration(configDto, testEmailAddress);
        return ResponseEntity.ok(testResult);
    }
    
    /**
     * Tests the complete email workflow with template processing.
     * <p>
     * This endpoint combines template retrieval, Velocity processing, and email sending
     * to test the complete email notification workflow. It retrieves the template
     * from the database, processes it with provided variables, and sends the email
     * using the current user's tenant email configuration.
     * </p>
     * 
     * @param testDto the email template test data including variables
     * @return ResponseEntity containing the test result with processed content
     */
    @PostMapping("/test-template")
    public ResponseEntity<EmailTemplateTestResultDto> testEmailTemplate(
            @RequestBody EmailTemplateTestDto testDto) {
        // Extract tenant ID from JWT token for tenant-aware template lookup
        String tenantId = tenantContextService.getCurrentTenantId();

        // TODO: Pass tenant context to service for tenant-aware template lookup
        // For now, use existing method - tenant filtering will be added to service
        EmailTemplateTestResultDto result = emailConfigurationService.testEmailTemplate(testDto);
        return ResponseEntity.ok(result);
    }
}
