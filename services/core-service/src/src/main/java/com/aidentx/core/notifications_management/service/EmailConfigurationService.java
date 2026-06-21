package com.aidentx.core.notifications_management.service;

import com.aidentx.core.notifications_management.model.dto.*;

import java.util.UUID;

/**
 * Service interface for managing email configurations.
 * <p>
 * This service provides operations for creating, updating, retrieving, and deleting
 * email configurations for different tenants. It also includes functionality for
 * testing email configurations to ensure they work correctly.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
public interface EmailConfigurationService {
    
    /**
     * Creates a new email configuration for a tenant.
     * <p>
     * This method validates the input data, ensures no duplicate configuration
     * exists for the tenant, and creates a new email configuration with the
     * provided settings.
     * </p>
     * 
     * @param createDto the email configuration data
     * @return EmailConfigurationResponseDto containing the created configuration
     * @throws IllegalArgumentException if the input data is invalid
     * @throws RuntimeException if a configuration already exists for the tenant
     */
    EmailConfigurationResponseDto createEmailConfiguration(EmailConfigurationCreateDto createDto);
    
    /**
     * Creates or updates an email configuration for a tenant.
     * <p>
     * This method provides an upsert operation - if a configuration already exists
     * for the tenant, it will be updated with the new settings. Otherwise, a new
     * configuration will be created. This ensures each tenant has exactly one
     * email configuration.
     * </p>
     * 
     * @param createDto the email configuration data
     * @return EmailConfigurationResponseDto containing the created or updated configuration
     * @throws IllegalArgumentException if the input data is invalid
     */
    EmailConfigurationResponseDto createOrUpdateEmailConfiguration(EmailConfigurationCreateDto createDto);
    
    /**
     * Updates an existing email configuration.
     * <p>
     * This method allows partial updates of email configuration properties
     * while maintaining data integrity and validation.
     * </p>
     * 
     * @param id the unique identifier of the configuration to update
     * @param updateDto the email configuration update data
     * @return EmailConfigurationResponseDto containing the updated configuration
     * @throws IllegalArgumentException if the input data is invalid
     * @throws RuntimeException if the configuration is not found
     */
    EmailConfigurationResponseDto updateEmailConfiguration(UUID id, EmailConfigurationUpdateDto updateDto);
    
    /**
     * Updates the email configuration for a specific tenant.
     * <p>
     * This method finds and updates the email configuration for the specified tenant.
     * Since each tenant has only one email configuration, this method automatically
     * locates the configuration by tenant ID and applies the updates.
     * </p>
     * 
     * @param tenantId the unique identifier of the tenant
     * @param updateDto the email configuration update data
     * @return EmailConfigurationResponseDto containing the updated configuration
     * @throws IllegalArgumentException if the input data is invalid
     * @throws RuntimeException if no configuration is found for the tenant
     */
    EmailConfigurationResponseDto updateEmailConfigurationByTenant(String tenantId, EmailConfigurationUpdateDto updateDto);
    
    /**
     * Retrieves the email configuration for a specific tenant.
     * <p>
     * This method returns the email configuration associated with the provided
     * tenant ID. Sensitive information like passwords and API keys are excluded
     * from the response for security purposes.
     * </p>
     * 
     * @param tenantId the unique identifier of the tenant
     * @return EmailConfigurationResponseDto containing the configuration
     * @throws RuntimeException if no configuration is found for the tenant
     */
    EmailConfigurationResponseDto getEmailConfigurationByTenant(String tenantId);
    
    /**
     * Retrieves an email configuration by its unique identifier.
     * <p>
     * This method returns the email configuration with the specified ID.
     * Sensitive information like passwords and API keys are excluded
     * from the response for security purposes.
     * </p>
     * 
     * @param id the unique identifier of the configuration
     * @return EmailConfigurationResponseDto containing the configuration
     * @throws RuntimeException if no configuration is found with the given ID
     */
    EmailConfigurationResponseDto getEmailConfigurationById(UUID id);
    
    /**
     * Deletes an email configuration.
     * <p>
     * This method permanently removes the email configuration from the system.
     * This operation cannot be undone.
     * </p>
     * 
     * @param id the unique identifier of the configuration to delete
     * @throws RuntimeException if the configuration is not found
     */
    void deleteEmailConfiguration(UUID id);
    
    /**
     * Tests an email configuration by sending a test email using the provided configuration.
     * <p>
     * This method validates the email configuration by attempting to send
     * a test email using the provided settings without saving the configuration.
     * It returns true if the test is successful, false otherwise.
     * </p>
     * 
     * @param configDto the email configuration to test
     * @param testEmailAddress the email address to send the test email to
     * @return true if the test email was sent successfully, false otherwise
     * @throws IllegalArgumentException if the configuration or email address is invalid
     */
    boolean testEmailConfiguration(EmailConfigurationCreateDto configDto, String testEmailAddress);
    
    /**
     * Tests the complete email workflow with template processing.
     * <p>
     * This method combines template retrieval from the database, Velocity template
     * processing with provided variables, and email sending using the tenant's
     * email configuration. It provides a comprehensive test of the entire
     * email notification workflow.
     * </p>
     * 
     * @param testDto the email template test data including tenant, trigger type, language, and variables
     * @return EmailTemplateTestResultDto containing the test results and processed content
     * @throws IllegalArgumentException if the test data is invalid
     * @throws RuntimeException if template is not found or email configuration is missing
     */
    EmailTemplateTestResultDto testEmailTemplate(EmailTemplateTestDto testDto);
}
