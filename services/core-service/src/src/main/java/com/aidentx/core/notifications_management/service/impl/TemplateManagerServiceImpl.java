package com.aidentx.core.notifications_management.service.impl;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerCreateDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerResponseDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerTestDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerTestResultDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerUpdateDto;
import com.aidentx.core.notifications_management.model.dto.TemplateVersionResponseDto;
import com.aidentx.core.notifications_management.model.entity.TemplateManager;
import com.aidentx.core.notifications_management.model.entity.TemplateManagerVersion;
import com.aidentx.core.notifications_management.repository.TemplateManagerRepository;
import com.aidentx.core.notifications_management.repository.TemplateManagerVersionRepository;
import com.aidentx.core.notifications_management.service.TemplateManagerService;
import com.aidentx.core.notifications_management.model.entity.EmailConfiguration;
import com.aidentx.core.notifications_management.repository.EmailConfigurationRepository;
import com.aidentx.core.notifications_management.service.VelocityTemplateService;
import com.aidentx.core.notifications_management.enums.AuthMethod;
import com.aidentx.core.shared.security.JwtUtil;
import com.aidentx.core.shared.security.SecurityConstants;
import com.aidentx.core.shared.security.TenantContextService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of TemplateManagerService for managing email template managers.
 * <p>
 * This service provides comprehensive template manager management including
 * creation, updates, retrieval, deletion, and validation of template settings.
 * All operations include proper validation, logging, and error handling.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-19
 */
@Service
@Transactional
public class TemplateManagerServiceImpl implements TemplateManagerService {
    
    private final TemplateManagerRepository templateManagerRepository;
    private final TemplateManagerVersionRepository templateVersionRepository;
    private final EmailConfigurationRepository emailConfigurationRepository;
    private final VelocityTemplateService velocityTemplateService;
    private final TenantContextService tenantContextService;
    private final JwtUtil jwtUtil;
    private final IUnifiedLogger logger;
    
    // Constants for JWT extraction
    private static final String SECURITY_ANONYMOUS_USER = "anonymousUser";
    private static final String SECURITY_BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    
    /**
     * Constructs a new TemplateManagerServiceImpl with required dependencies.
     * 
     * @param templateManagerRepository Repository for template manager persistence
     * @param templateVersionRepository Repository for template version history
     * @param emailConfigurationRepository Repository for email configuration access
     * @param velocityTemplateService Service for processing Velocity templates
     * @param tenantContextService Service for extracting tenant information from JWT
     * @param jwtUtil Utility for JWT token operations
     * @param loggerFactory Factory for creating unified loggers
     */
    public TemplateManagerServiceImpl(TemplateManagerRepository templateManagerRepository,
                                     TemplateManagerVersionRepository templateVersionRepository,
                                     EmailConfigurationRepository emailConfigurationRepository,
                                     VelocityTemplateService velocityTemplateService,
                                     TenantContextService tenantContextService,
                                     JwtUtil jwtUtil,
                                     UnifiedLoggerFactory loggerFactory) {
        this.templateManagerRepository = templateManagerRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.emailConfigurationRepository = emailConfigurationRepository;
        this.velocityTemplateService = velocityTemplateService;
        this.tenantContextService = tenantContextService;
        this.jwtUtil = jwtUtil;
        this.logger = loggerFactory.getLogger(AppConstants.TEMPLATE_MANAGER_SERVICE_LOGGER_NAME,
                                            AppConstants.LOG_CATEGORY_TEMPLATE_MANAGER_SERVICE);
    }
    
    /**
     * Creates a new template manager.
     * 
     * @param createDto the template manager creation data
     * @return TemplateManagerResponseDto containing the created template manager
     */
    @Override
    public TemplateManagerResponseDto createTemplateManager(TemplateManagerCreateDto createDto) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_CREATE_TEMPLATE_MANAGER))
                .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                .log();
        
        try {
            // Validate input data
            validateCreateDto(createDto);
            
            logger.info().message(String.format(AppConstants.LOG_CREATING_TEMPLATE_MANAGER, createDto.getTenantId()))
                    .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                    .detail(AppConstants.DETAIL_TEMPLATE_NAME, createDto.getTemplateName())
                    .detail(AppConstants.DETAIL_TRIGGER_TYPE, createDto.getTriggerType())
                    .detail(AppConstants.DETAIL_LANGUAGE, createDto.getLanguage())
                    .log();
            
            // Check if template already exists for tenant, trigger type, and language
            if (templateManagerRepository.existsByTenantIdAndTriggerTypeAndLanguage(
                    createDto.getTenantId(), createDto.getTriggerType(), createDto.getLanguage())) {
                throw new RuntimeException(String.format(AppConstants.MSG_TEMPLATE_MANAGER_ALREADY_EXISTS, 
                        createDto.getTenantId(), createDto.getTriggerType(), createDto.getLanguage()));
            }
            
            // Create new template manager
            TemplateManager templateManager = createEntityFromDto(createDto);
            TemplateManager savedTemplate = templateManagerRepository.save(templateManager);
            
            // Create first version for the template
            createNewVersion(savedTemplate, "Initial template creation", TemplateManagerVersion.ChangeType.CREATED);
            
            logger.info().message(String.format(AppConstants.LOG_TEMPLATE_MANAGER_CREATED, savedTemplate.getId()))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, savedTemplate.getId())
                    .detail(AppConstants.DETAIL_TENANT_ID, savedTemplate.getTenantId())
                    .detail(AppConstants.DETAIL_TEMPLATE_NAME, savedTemplate.getTemplateName())
                    .log();
            
            return createResponseDto(savedTemplate);
            
        } catch (DataIntegrityViolationException e) {
            // Handle unique constraint violation
            String errorMsg = String.format(AppConstants.MSG_TEMPLATE_MANAGER_ALREADY_EXISTS, 
                    createDto.getTenantId(), createDto.getTriggerType(), createDto.getLanguage());
            logger.error(e).message(errorMsg)
                    .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                    .detail(AppConstants.DETAIL_TRIGGER_TYPE, createDto.getTriggerType())
                    .detail(AppConstants.DETAIL_LANGUAGE, createDto.getLanguage())
                    .log();
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_CREATE_TEMPLATE_MANAGER, e.getMessage()))
                    .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                    .log();
            throw e;
        }
    }
    
    /**
     * Updates an existing template manager.
     * 
     * @param id the unique identifier of the template manager to update
     * @param updateDto the template manager update data
     * @return TemplateManagerResponseDto containing the updated template manager
     */
    @Override
    public TemplateManagerResponseDto updateTemplateManager(UUID id, TemplateManagerUpdateDto updateDto) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_UPDATE_TEMPLATE_MANAGER))
                .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                .log();
        
        try {
            logger.info().message(String.format(AppConstants.LOG_UPDATING_TEMPLATE_MANAGER, id))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                    .log();
            
            TemplateManager existingTemplate = findTemplateManagerById(id);
            
            // Validate update data
            validateUpdateDto(updateDto);
            
            // Update template manager
            updateEntityFromDto(updateDto, existingTemplate);
            
            // Get the highest version number and increment by 1 to avoid duplicates
            Integer maxVersionNumber = templateVersionRepository.findMaxVersionNumber(id, existingTemplate.getTenantId())
                    .orElse(0); // Default to 0 if no versions exist yet
            
            // Set the new version to be the highest existing version + 1
            existingTemplate.setCurrentVersion(maxVersionNumber + 1);
            existingTemplate.setUpdatedAt(System.currentTimeMillis());
            existingTemplate.setUpdatedById(extractUserIdFromCurrentRequest());
            existingTemplate.setUpdatedByName(extractUsernameFromCurrentRequest());
            
            TemplateManager updatedTemplate = templateManagerRepository.save(existingTemplate);
            
            // Create new version for the update
            String changeComment = updateDto.getChangeComment() != null ? updateDto.getChangeComment() : "Template updated";
            createNewVersion(updatedTemplate, changeComment, TemplateManagerVersion.ChangeType.UPDATED);
            
            logger.info().message(String.format(AppConstants.LOG_TEMPLATE_MANAGER_UPDATED, updatedTemplate.getId()))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, updatedTemplate.getId())
                    .detail(AppConstants.DETAIL_TENANT_ID, updatedTemplate.getTenantId())
                    .detail(AppConstants.DETAIL_TEMPLATE_NAME, updatedTemplate.getTemplateName())
                    .log();
            
            return createResponseDto(updatedTemplate);
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_UPDATE_TEMPLATE_MANAGER, e.getMessage()))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                    .log();
            throw e;
        }
    }
    
    /**
     * Retrieves a template manager by its unique identifier.
     * 
     * @param id the unique identifier of the template manager
     * @return TemplateManagerResponseDto containing the template manager information
     */
    @Override
    @Transactional(readOnly = true)
    public TemplateManagerResponseDto getTemplateManagerById(UUID id) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_GET_TEMPLATE_MANAGER_BY_ID))
                .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                .log();
        
        try {
            logger.info().message(String.format(AppConstants.LOG_RETRIEVING_TEMPLATE_MANAGER, id))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                    .log();
            
            TemplateManager template = findTemplateManagerById(id);
            
            logger.debug().message(String.format(AppConstants.LOG_TEMPLATE_MANAGER_RETRIEVED, id))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                    .detail(AppConstants.DETAIL_TENANT_ID, template.getTenantId())
                    .detail(AppConstants.DETAIL_TEMPLATE_NAME, template.getTemplateName())
                    .log();
            
            return createResponseDto(template);
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_GET_TEMPLATE_MANAGER_BY_ID, e.getMessage()))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                    .log();
            throw e;
        }
    }
    
    /**
     * Retrieves all template managers for a specific tenant.
     * 
     * @param tenantId the unique identifier of the tenant
     * @return List of TemplateManagerResponseDto containing the template managers
     */
    @Override
    @Transactional(readOnly = true)
    public List<TemplateManagerResponseDto> getTemplateManagersByTenant(String tenantId) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_GET_TEMPLATE_MANAGERS_BY_TENANT))
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .log();
        
        try {
            logger.info().message(String.format(AppConstants.LOG_RETRIEVING_TEMPLATE_MANAGERS_BY_TENANT, tenantId))
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .log();
            
            List<TemplateManager> templates = templateManagerRepository.findByTenantId(tenantId);
            
            List<TemplateManagerResponseDto> responseDtos = templates.stream()
                    .map(this::createResponseDto)
                    .collect(Collectors.toList());
            
            logger.debug().message(String.format(AppConstants.LOG_TEMPLATE_MANAGERS_RETRIEVED_BY_TENANT, 
                    responseDtos.size(), tenantId))
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_TEMPLATE_COUNT, responseDtos.size())
                    .log();
            
            return responseDtos;
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_GET_TEMPLATE_MANAGERS_BY_TENANT, e.getMessage()))
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .log();
            throw e;
        }
    }
    
    /**
     * Deletes a template manager.
     * 
     * @param id the unique identifier of the template manager to delete
     */
    @Override
    public void deleteTemplateManager(UUID id) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_DELETE_TEMPLATE_MANAGER))
                .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                .log();
        
        try {
            logger.info().message(String.format(AppConstants.LOG_DELETING_TEMPLATE_MANAGER, id))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                    .log();
            
            TemplateManager template = findTemplateManagerById(id);
            templateManagerRepository.delete(template);
            
            logger.info().message(String.format(AppConstants.LOG_TEMPLATE_MANAGER_DELETED, id))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                    .detail(AppConstants.DETAIL_TENANT_ID, template.getTenantId())
                    .detail(AppConstants.DETAIL_TEMPLATE_NAME, template.getTemplateName())
                    .log();
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_DELETE_TEMPLATE_MANAGER, e.getMessage()))
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, id)
                    .log();
            throw e;
        }
    }
    
    /**
     * Validates the create DTO for required fields and constraints.
     * 
     * @param createDto the DTO to validate
     */
    private void validateCreateDto(TemplateManagerCreateDto createDto) {
        if (createDto.getTenantId() == null || createDto.getTenantId().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_TENANT_ID_REQUIRED);
        }
        if (createDto.getTemplateName() == null || createDto.getTemplateName().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_TEMPLATE_NAME_REQUIRED);
        }
        if (createDto.getTriggerType() == null) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_TRIGGER_TYPE_REQUIRED);
        }
        if (createDto.getLanguage() == null) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_LANGUAGE_REQUIRED);
        }
        if (createDto.getTemplateSubject() == null || createDto.getTemplateSubject().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_TEMPLATE_SUBJECT_REQUIRED);
        }
        if (createDto.getEmailBody() == null || createDto.getEmailBody().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_EMAIL_BODY_REQUIRED);
        }
    }
    
    /**
     * Validates the update DTO for constraints.
     * 
     * @param updateDto the DTO to validate
     */
    private void validateUpdateDto(TemplateManagerUpdateDto updateDto) {
        if (updateDto.getTemplateName() != null && updateDto.getTemplateName().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_TEMPLATE_NAME_REQUIRED);
        }
        if (updateDto.getTemplateSubject() != null && updateDto.getTemplateSubject().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_TEMPLATE_SUBJECT_REQUIRED);
        }
        if (updateDto.getEmailBody() != null && updateDto.getEmailBody().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_EMAIL_BODY_REQUIRED);
        }
    }
    
    /**
     * Creates a TemplateManager entity from a create DTO.
     * 
     * @param createDto the create DTO
     * @return the created entity
     */
    private TemplateManager createEntityFromDto(TemplateManagerCreateDto createDto) {
        String userId = extractUserIdFromCurrentRequest();
        String username = extractUsernameFromCurrentRequest();
        
        return new TemplateManager(
                createDto.getTenantId(),
                createDto.getTemplateName(),
                createDto.getTriggerType(),
                createDto.getLanguage(),
                createDto.getTemplateSubject(),
                createDto.getEmailBody(),
                userId,
                username
        );
    }
    
    /**
     * Updates a TemplateManager entity from an update DTO.
     * 
     * @param updateDto the update DTO
     * @param entity the entity to update
     */
    private void updateEntityFromDto(TemplateManagerUpdateDto updateDto, TemplateManager entity) {
        if (updateDto.getTemplateName() != null) {
            entity.setTemplateName(updateDto.getTemplateName());
        }
        if (updateDto.getTriggerType() != null) {
            entity.setTriggerType(updateDto.getTriggerType());
        }
        if (updateDto.getIsEnabled() != null) {
            entity.setIsEnabled(updateDto.getIsEnabled());
        }
        if (updateDto.getLanguage() != null) {
            entity.setLanguage(updateDto.getLanguage());
        }
        if (updateDto.getTemplateSubject() != null) {
            entity.setTemplateSubject(updateDto.getTemplateSubject());
        }
        if (updateDto.getEmailBody() != null) {
            entity.setEmailBody(updateDto.getEmailBody());
        }
        
        // Set updated timestamp
        entity.setUpdatedAt(System.currentTimeMillis());
    }
    
    /**
     * Extracts user ID from the current JWT token in the request.
     * 
     * @return the user ID string, or "system" if extraction fails
     */
    private String extractUserIdFromCurrentRequest() {
        try {
            // Get authentication from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !SECURITY_ANONYMOUS_USER.equals(authentication.getPrincipal())) {

                // Try to get JWT token from current request
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();
                if (requestAttributes != null) {
                    String authHeader = requestAttributes.getRequest()
                            .getHeader(SecurityConstants.AUTHORIZATION_HEADER);
                    if (authHeader != null && authHeader.startsWith(SECURITY_BEARER_PREFIX)) {
                        String token = authHeader.substring(BEARER_PREFIX_LENGTH); // Remove "Bearer " prefix

                        // Extract user ID from JWT token using JwtUtil
                        return jwtUtil.extractUserId(token);
                    }
                }

                // Fallback to authentication name if JWT extraction fails
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.warn().message("Failed to extract user ID from current request").log();
        }
        
        return "system"; // Default fallback
    }
    
    /**
     * Extracts username from the current JWT token in the request.
     * 
     * @return the username string, or "system" if extraction fails
     */
    private String extractUsernameFromCurrentRequest() {
        try {
            // Get authentication from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !SECURITY_ANONYMOUS_USER.equals(authentication.getPrincipal())) {

                // Try to get JWT token from current request
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();
                if (requestAttributes != null) {
                    String authHeader = requestAttributes.getRequest()
                            .getHeader(SecurityConstants.AUTHORIZATION_HEADER);
                    if (authHeader != null && authHeader.startsWith(SECURITY_BEARER_PREFIX)) {
                        String token = authHeader.substring(BEARER_PREFIX_LENGTH); // Remove "Bearer " prefix

                        // Extract username from JWT token using JwtUtil
                        return jwtUtil.extractUsername(token);
                    }
                }

                // Fallback to authentication name if JWT extraction fails
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.warn().message("Failed to extract username from current request").log();
        }
        
        return "system"; // Default fallback
    }

    /**
     * Creates a response DTO from a TemplateManager entity.
     * 
     * @param entity the entity
     * @return the response DTO
     */
    private TemplateManagerResponseDto createResponseDto(TemplateManager entity) {
        TemplateManagerResponseDto responseDto = new TemplateManagerResponseDto();
        responseDto.setId(entity.getId());
        responseDto.setTenantId(entity.getTenantId());
        responseDto.setTemplateName(entity.getTemplateName());
        responseDto.setTriggerType(entity.getTriggerType());
        responseDto.setIsEnabled(entity.getIsEnabled());
        responseDto.setLanguage(entity.getLanguage());
        responseDto.setTemplateSubject(entity.getTemplateSubject());
        responseDto.setEmailBody(entity.getEmailBody());
        responseDto.setCreatedAt(entity.getCreatedAt());
        responseDto.setUpdatedAt(entity.getUpdatedAt());
        responseDto.setCreatedById(entity.getCreatedById());
        responseDto.setCreatedByName(entity.getCreatedByName());
        responseDto.setCurrentVersion(entity.getCurrentVersion());
        responseDto.setUpdatedById(entity.getUpdatedById());
        responseDto.setUpdatedByName(entity.getUpdatedByName());
        return responseDto;
    }
    
    /**
     * Finds a template manager by ID with standardized error handling.
     * 
     * @param id the template manager ID
     * @return the template manager entity
     */
    private TemplateManager findTemplateManagerById(UUID id) {
        return templateManagerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(String.format(AppConstants.MSG_TEMPLATE_MANAGER_NOT_FOUND, id)));
    }
    
    // ===== VERSIONING METHODS =====
    
    /**
     * Creates a new version for a template.
     * 
     * @param template the template to create a version for
     * @param changeComment comment describing the changes
     * @param changeType the type of change
     */
    private void createNewVersion(TemplateManager template, String changeComment, TemplateManagerVersion.ChangeType changeType) {
        String userId = extractUserIdFromCurrentRequest();
        String username = extractUsernameFromCurrentRequest();
        
        // Mark all existing versions as not current
        templateVersionRepository.markAllVersionsAsNotCurrent(template.getId(), template.getTenantId());
        
        // Create new version
        TemplateManagerVersion version = new TemplateManagerVersion(
                template, 
                template.getCurrentVersion(), 
                userId, 
                username, 
                changeComment, 
                changeType
        );
        
        templateVersionRepository.save(version);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<TemplateVersionResponseDto> getTemplateVersionHistory(UUID templateId) {
        logger.trace().message("Retrieving version history for template")
                .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, templateId)
                .log();
        
        try {
            // Verify template exists and extract tenant from current request
            TemplateManager template = findTemplateManagerById(templateId);
            
            // Get version history
            List<TemplateManagerVersion> versions = templateVersionRepository
                    .findByTemplateIdAndTenantIdOrderByVersionNumberDesc(templateId, template.getTenantId());
            
            return versions.stream()
                    .map(TemplateVersionResponseDto::new)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error(e).message("Failed to retrieve template version history")
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, templateId)
                    .log();
            throw e;
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public TemplateVersionResponseDto getTemplateVersion(UUID templateId, Integer versionNumber) {
        logger.trace().message("Retrieving specific template version")
                .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, templateId)
                .detail("versionNumber", versionNumber)
                .log();
        
        try {
            // Verify template exists and extract tenant from current request
            TemplateManager template = findTemplateManagerById(templateId);
            
            // Get specific version
            TemplateManagerVersion version = templateVersionRepository
                    .findByTemplateIdAndTenantIdAndVersionNumber(templateId, template.getTenantId(), versionNumber)
                    .orElseThrow(() -> new RuntimeException(String.format(
                            "Template version %d not found for template %s", versionNumber, templateId)));
            
            return new TemplateVersionResponseDto(version);
                    
        } catch (Exception e) {
            logger.error(e).message("Failed to retrieve template version")
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, templateId)
                    .detail("versionNumber", versionNumber)
                    .log();
            throw e;
        }
    }
    
    @Override
    public TemplateManagerResponseDto restoreTemplateVersion(UUID templateId, Integer versionNumber, String changeComment) {
        logger.trace().message("Restoring template to previous version")
                .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, templateId)
                .detail("versionNumber", versionNumber)
                .log();
        
        try {
            // Verify template exists and extract tenant from current request
            TemplateManager currentTemplate = findTemplateManagerById(templateId);
            
            // Get the version to restore
            TemplateManagerVersion versionToRestore = templateVersionRepository
                    .findByTemplateIdAndTenantIdAndVersionNumber(templateId, currentTemplate.getTenantId(), versionNumber)
                    .orElseThrow(() -> new RuntimeException(String.format(
                            "Template version %d not found for template %s", versionNumber, templateId)));
            
            // Update current template with version data
            currentTemplate.setTemplateName(versionToRestore.getTemplateName());
            currentTemplate.setTriggerType(versionToRestore.getTriggerType());
            currentTemplate.setIsEnabled(versionToRestore.getIsEnabled());
            currentTemplate.setLanguage(versionToRestore.getLanguage());
            currentTemplate.setTemplateSubject(versionToRestore.getTemplateSubject());
            currentTemplate.setEmailBody(versionToRestore.getEmailBody());
            
            // Set current version to the restored version number (don't increment!)
            currentTemplate.setCurrentVersion(versionNumber);
            currentTemplate.setUpdatedAt(System.currentTimeMillis());
            currentTemplate.setUpdatedById(extractUserIdFromCurrentRequest());
            currentTemplate.setUpdatedByName(extractUsernameFromCurrentRequest());
            
            TemplateManager savedTemplate = templateManagerRepository.save(currentTemplate);
            
            // Do NOT create a new version entry for restore operation
            // The template now points to the existing version that was restored
            
            logger.info().message("Template restored to previous version")
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, templateId)
                    .detail("restoredToVersion", versionNumber)
                    .detail("currentVersion", savedTemplate.getCurrentVersion())
                    .log();
            
            return createResponseDto(savedTemplate);
                    
        } catch (Exception e) {
            logger.error(e).message("Failed to restore template version")
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, templateId)
                    .detail("versionNumber", versionNumber)
                    .log();
            throw e;
        }
    }

    /**
     * Tests template processing by sending a test email with provided content.
     * <p>
     * This method extracts the tenant from JWT, processes the provided template 
     * content with Velocity template engine using provided variables, and sends 
     * a test email using the tenant's email configuration.
     * </p>
     * 
     * @param testDto the template test data with subject, body, and email address
     * @return TemplateManagerTestResultDto containing test results and processed content
     * @throws RuntimeException if email configuration missing or send fails
     */
    @Override
    public TemplateManagerTestResultDto testTemplateManager(TemplateManagerTestDto testDto) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, "testTemplateManager"))
                .detail("testEmailAddress", testDto.getTestEmailAddress())
                .log();
        
        try {
            // Extract tenant ID from JWT
            String tenantId = tenantContextService.getCurrentTenantId();
            
            logger.info().message("Testing template processing with extracted tenant")
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .detail("testEmailAddress", testDto.getTestEmailAddress())
                    .log();

            // 1. Retrieve email configuration for tenant
            EmailConfiguration emailConfig = emailConfigurationRepository
                    .findByTenantId(tenantId)
                    .orElseThrow(() -> new RuntimeException(String.format(
                            "No email configuration found for tenant: %s", tenantId)));

            logger.debug().message("Email configuration retrieved")
                    .detail(AppConstants.DETAIL_SMTP_HOSTNAME, emailConfig.getSmtpHostname())
                    .detail(AppConstants.DETAIL_SMTP_PORT, emailConfig.getSmtpPort())
                    .detail(AppConstants.DETAIL_EMAIL_ADDRESS, emailConfig.getEmailAddress())
                    .log();

            // 2. Use provided template content
            String templateSubject = testDto.getTemplateSubject() != null ? testDto.getTemplateSubject() : "Test Subject";
            String templateBody = testDto.getEmailBody();

            // 3. Process template with Velocity (use empty map if no variables provided)
            Map<String, Object> templateVariables = testDto.getTemplateVariables();
            if (templateVariables == null) {
                templateVariables = new java.util.HashMap<>();
            }
            
            String[] processedContent = velocityTemplateService.processEmailTemplate(
                    templateSubject,
                    templateBody,
                    templateVariables
            );

            String processedSubject = processedContent[0];
            String processedBody = processedContent[1];

            logger.debug().message("Template processed with Velocity")
                    .detail("originalSubject", templateSubject)
                    .detail("processedSubject", processedSubject)
                    .detail("variableCount", testDto.getTemplateVariables() != null ? testDto.getTemplateVariables().size() : 0)
                    .log();

            // 4. Send email using processed template
            boolean emailSent = sendProcessedEmail(emailConfig, testDto.getTestEmailAddress(), 
                    processedSubject, processedBody);

            if (emailSent) {
                logger.info().message("Template test completed successfully")
                        .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                        .detail("testEmailAddress", testDto.getTestEmailAddress())
                        .log();

                return new TemplateManagerTestResultDto(
                        true,
                        "Test email sent successfully",
                        processedSubject,
                        processedBody,
                        testDto.getTestEmailAddress(),
                        testDto.getTemplateVariables(),
                        String.format("%s:%d (%s)", 
                                emailConfig.getSmtpHostname(), 
                                emailConfig.getSmtpPort(), 
                                emailConfig.getEmailAddress())
                );
            } else {
                return new TemplateManagerTestResultDto(
                        false,
                        "Failed to send test email",
                        processedSubject,
                        processedBody,
                        testDto.getTestEmailAddress(),
                        testDto.getTemplateVariables(),
                        String.format("%s:%d (%s)", 
                                emailConfig.getSmtpHostname(), 
                                emailConfig.getSmtpPort(), 
                                emailConfig.getEmailAddress())
                );
            }

        } catch (Exception e) {
            logger.error(e).message(String.format("Template test failed: %s", e.getMessage()))
                    .detail("testEmailAddress", testDto.getTestEmailAddress())
                    .log();
            return new TemplateManagerTestResultDto(
                    false,
                    "Template test failed: " + e.getMessage(),
                    null,
                    null,
                    testDto.getTestEmailAddress(),
                    testDto.getTemplateVariables(),
                    null
            );
        }
    }

    /**
     * Sends an email using the provided email configuration and processed content.
     * <p>
     * This method handles the actual SMTP email sending using the processed template content.
     * </p>
     * 
     * @param emailConfig the email configuration containing SMTP settings
     * @param toAddress the recipient email address
     * @param subject the processed email subject
     * @param body the processed email body
     * @return true if email was sent successfully, false otherwise
     */
    private boolean sendProcessedEmail(EmailConfiguration emailConfig, String toAddress, 
                                     String subject, String body) {
        try {
            // Create email properties
            Properties props = new Properties();
            props.put("mail.smtp.host", emailConfig.getSmtpHostname());
            props.put("mail.smtp.port", String.valueOf(emailConfig.getSmtpPort()));
            props.put("mail.smtp.auth", "true");
            
            // Configure encryption
            if (emailConfig.getEncryptionType() != null) {
                switch (emailConfig.getEncryptionType()) {
                    case STARTTLS:
                        props.put("mail.smtp.starttls.enable", "true");
                        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                        break;
                    case SSL_TLS:
                        props.put("mail.smtp.ssl.enable", "true");
                        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                        break;
                    case NONE:
                        // No encryption
                        break;
                }
            }
            
            // Create authenticator
            Authenticator authenticator = null;
            if (emailConfig.getAuthMethod() == AuthMethod.USERNAME_PASSWORD && 
                emailConfig.getUsername() != null && emailConfig.getPassword() != null) {
                authenticator = new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(emailConfig.getUsername(), emailConfig.getPassword());
                    }
                };
            }
            
            // Create session and send email
            Session session = Session.getInstance(props, authenticator);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailConfig.getEmailAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");
            
            logger.info().message("Sending email via SMTP")
                    .detail("toAddress", toAddress)
                    .detail("subject", subject)
                    .detail("smtpHost", emailConfig.getSmtpHostname())
                    .detail("smtpPort", emailConfig.getSmtpPort())
                    .log();
            
            Transport.send(message);
            
            logger.info().message("Email sent successfully")
                    .detail("toAddress", toAddress)
                    .detail("smtpHost", emailConfig.getSmtpHostname())
                    .log();
            
            return true;
            
        } catch (MessagingException e) {
            logger.error(e).message("Failed to send email: " + e.getMessage())
                    .detail("toAddress", toAddress)
                    .detail("smtpHost", emailConfig.getSmtpHostname())
                    .detail("smtpPort", emailConfig.getSmtpPort())
                    .log();
            return false;
        }
    }
}
