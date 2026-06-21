package com.aidentx.core.notifications_management.service;

import com.aidentx.core.notifications_management.model.dto.TemplateManagerCreateDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerResponseDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerTestDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerTestResultDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerUpdateDto;
import com.aidentx.core.notifications_management.model.dto.TemplateVersionResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing template managers.
 * <p>
 * This service provides operations for creating, updating, retrieving, and deleting
 * email template managers for different tenants. It includes functionality for
 * managing templates with internationalization support and operation-specific triggers.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
public interface TemplateManagerService {
    
    /**
     * Creates a new template manager.
     * <p>
     * This method validates the input data, ensures no duplicate template
     * exists for the same tenant, trigger type, and language combination,
     * and creates a new template manager with the provided settings.
     * </p>
     * 
     * @param createDto the template manager creation data
     * @return TemplateManagerResponseDto containing the created template manager
     * @throws IllegalArgumentException if the input data is invalid
     * @throws RuntimeException if a template already exists for the same combination
     */
    TemplateManagerResponseDto createTemplateManager(TemplateManagerCreateDto createDto);
    
    /**
     * Updates an existing template manager.
     * <p>
     * This method allows partial updates of template manager properties
     * while maintaining data integrity and validation.
     * </p>
     * 
     * @param id the unique identifier of the template manager to update
     * @param updateDto the template manager update data
     * @return TemplateManagerResponseDto containing the updated template manager
     * @throws IllegalArgumentException if the input data is invalid
     * @throws RuntimeException if the template manager is not found
     */
    TemplateManagerResponseDto updateTemplateManager(UUID id, TemplateManagerUpdateDto updateDto);
    
    /**
     * Retrieves a template manager by its unique identifier.
     * <p>
     * This method returns the template manager information for the provided ID.
     * </p>
     * 
     * @param id the unique identifier of the template manager
     * @return TemplateManagerResponseDto containing the template manager information
     * @throws RuntimeException if no template manager is found with the given ID
     */
    TemplateManagerResponseDto getTemplateManagerById(UUID id);
    
    /**
     * Retrieves all template managers for a specific tenant.
     * <p>
     * This method returns all template managers associated with the provided
     * tenant ID, including both enabled and disabled templates.
     * </p>
     * 
     * @param tenantId the unique identifier of the tenant
     * @return List of TemplateManagerResponseDto containing the template managers
     */
    List<TemplateManagerResponseDto> getTemplateManagersByTenant(String tenantId);
    
    /**
     * Deletes a template manager.
     * <p>
     * This method permanently removes the template manager from the system.
     * This operation cannot be undone.
     * </p>
     * 
     * @param id the unique identifier of the template manager to delete
     * @throws RuntimeException if the template manager is not found
     */
    void deleteTemplateManager(UUID id);
    
    /**
     * Retrieves version history for a specific template.
     * <p>
     * This method returns all versions of a template ordered by version number
     * in descending order (newest first).
     * </p>
     * 
     * @param templateId the unique identifier of the template
     * @return List of TemplateVersionResponseDto containing version history
     * @throws RuntimeException if the template is not found
     */
    List<TemplateVersionResponseDto> getTemplateVersionHistory(UUID templateId);
    
    /**
     * Retrieves a specific version of a template.
     * <p>
     * This method returns the template data for a specific version number.
     * </p>
     * 
     * @param templateId the unique identifier of the template
     * @param versionNumber the version number to retrieve
     * @return TemplateVersionResponseDto containing the version data
     * @throws RuntimeException if the template or version is not found
     */
    TemplateVersionResponseDto getTemplateVersion(UUID templateId, Integer versionNumber);
    
    /**
     * Restores a template to a previous version.
     * <p>
     * This method creates a new version by copying data from a previous version
     * and marks it as the current active version.
     * </p>
     * 
     * @param templateId the unique identifier of the template
     * @param versionNumber the version number to restore to
     * @param changeComment optional comment explaining the restoration
     * @return TemplateManagerResponseDto containing the restored template
     * @throws RuntimeException if the template or version is not found
     */
    TemplateManagerResponseDto restoreTemplateVersion(UUID templateId, Integer versionNumber, String changeComment);
    
    /**
     * Tests template processing by sending a test email with resolved variables.
     * <p>
     * This method processes the provided template content with variables and sends
     * a test email using the tenant's email configuration. The tenant is automatically
     * extracted from JWT token in the current request context.
     * </p>
     * 
     * @param testDto the template test data including content and variables
     * @return TemplateManagerTestResultDto containing the test results
     * @throws RuntimeException if email configuration is not found or email sending fails
     */
    TemplateManagerTestResultDto testTemplateManager(TemplateManagerTestDto testDto);
}
