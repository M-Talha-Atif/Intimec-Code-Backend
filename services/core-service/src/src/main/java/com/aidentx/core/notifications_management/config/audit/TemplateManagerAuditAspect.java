package com.aidentx.core.notifications_management.config.audit;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerCreateDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerResponseDto;
import com.aidentx.core.notifications_management.model.dto.TemplateManagerUpdateDto;
import com.aidentx.core.notifications_management.model.entity.TemplateManager;
import com.aidentx.core.notifications_management.repository.TemplateManagerRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Aspect for auditing template manager operations.
 * <p>
 * This aspect intercepts template manager create, update, and delete operations
 * to generate audit events. It captures the operation details, user information,
 * and relevant data changes for compliance and monitoring purposes.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-22
 */
@Aspect
@Component
public class TemplateManagerAuditAspect {
    
    private final NotificationSecurityContextService securityContextService;
    private final TemplateManagerAuditEventPublisher auditEventPublisher;
    private final ObjectMapper objectMapper;
    private final IUnifiedLogger logger;
    private final TemplateManagerRepository templateManagerRepository;
    
    /**
     * Constructor for the TemplateManagerAuditAspect.
     * 
     * @param securityContextService Security context service for user information
     * @param auditEventPublisher Publisher for audit events
     * @param objectMapper Object mapper for JSON processing
     * @param loggerFactory Factory for creating loggers
     * @param templateManagerRepository Repository for template manager operations
     */
    @Autowired
    public TemplateManagerAuditAspect(
            NotificationSecurityContextService securityContextService,
            TemplateManagerAuditEventPublisher auditEventPublisher,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory loggerFactory,
            TemplateManagerRepository templateManagerRepository) {
        this.securityContextService = securityContextService;
        this.auditEventPublisher = auditEventPublisher;
        this.objectMapper = objectMapper;
        this.templateManagerRepository = templateManagerRepository;
        this.logger = loggerFactory.getLogger(
            AppConstants.TEMPLATE_MANAGER_AUDIT_ASPECT_LOGGER_NAME,
            AppConstants.LOG_CATEGORY_TEMPLATE_MANAGER_AUDIT
        );
    }
    
    /**
     * Audits template manager creation operations.
     * 
     * @param joinPoint the method execution join point
     * @return the result of the method execution
     * @throws Throwable if an error occurs during method execution
     */
    @Around("execution(* com.aidentx.core.notifications_management.service.impl.TemplateManagerServiceImpl.createTemplateManager(..))")
    public Object auditTemplateManagerCreation(ProceedingJoinPoint joinPoint) throws Throwable {
        logger.info().message(AppConstants.AUDIT_LOG_ASPECT_TRIGGERED_TEMPLATE_MANAGER_CREATION).log();
        
        Object[] args = joinPoint.getArgs();
        TemplateManagerCreateDto createDto = (TemplateManagerCreateDto) args[0];
        
        logger.info()
            .message(AppConstants.AUDIT_LOG_AUDITING_TEMPLATE_MANAGER_CREATION)
            .detail("tenantId", createDto.getTenantId())
            .detail("triggerType", createDto.getTriggerType())
            .detail("language", createDto.getLanguage())
            .log();
        
        // Execute the actual method
        Object result = joinPoint.proceed();
        
        if (result instanceof TemplateManagerResponseDto responseDto) {
            try {
                // Get user information
                AuditUser auditUser = securityContextService.getAuditUser();
                
                logger.info()
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT)
                    .detail("templateManagerId", responseDto.getId())
                    .detail("action", "CREATE")
                    .detail("userId", auditUser.id())
                    .detail("username", auditUser.username())
                    .log();
                
                // Create sanitized template data
                Map<String, Object> templateData = sanitizeTemplateManagerData(createDto);
                
                // Ensure template name is included (it should be from createDto, but make sure)
                templateData.put("templateName", responseDto.getTemplateName());
                
                // Create and publish audit event
                TemplateManagerAuditEvent auditEvent = TemplateManagerAuditEvent.create(
                    responseDto.getId(),
                    "CREATE",
                    UUID.fromString(auditUser.id()),
                    auditUser.username(),
                    createDto.getTenantId(),
                    templateData
                );
                
                auditEventPublisher.publishAuditEvent(auditEvent);
                
                logger.info()
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_TEMPLATE_MANAGER_CREATION)
                    .detail("templateManagerId", responseDto.getId())
                    .log();
                        
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_TEMPLATE_MANAGER_CREATION)
                    .detail("templateManagerId", responseDto.getId())
                    .log();
            }
        }
        
        return result;
    }
    
    /**
     * Audits template manager update operations.
     * 
     * @param joinPoint the method execution join point
     * @return the result of the method execution
     * @throws Throwable if an error occurs during method execution
     */
    @Around("execution(* com.aidentx.core.notifications_management.service.impl.TemplateManagerServiceImpl.updateTemplateManager(..))")
    public Object auditTemplateManagerUpdate(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        UUID templateManagerId = (UUID) args[0];
        TemplateManagerUpdateDto updateDto = (TemplateManagerUpdateDto) args[1];
        
        logger.info()
            .message(AppConstants.AUDIT_LOG_AUDITING_TEMPLATE_MANAGER_UPDATE)
            .detail("templateManagerId", templateManagerId)
            .log();
        
        // Capture the state before update
        Map<String, Object> stateBefore = null;
        try {
            Optional<TemplateManager> existingTemplate = templateManagerRepository.findById(templateManagerId);
            if (existingTemplate.isPresent()) {
                stateBefore = sanitizeTemplateManagerData(existingTemplate.get());
            }
        } catch (Exception e) {
            logger.warn()
                .message("Could not retrieve template manager state before update")
                .detail("templateManagerId", templateManagerId)
                .detail("error", e.getMessage())
                .log();
        }
        
        // Execute the actual method
        Object result = joinPoint.proceed();
        
        if (result instanceof TemplateManagerResponseDto responseDto) {
            try {
                // Get user information
                AuditUser auditUser = securityContextService.getAuditUser();
                
                logger.info()
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT)
                    .detail("templateManagerId", templateManagerId)
                    .detail("action", "UPDATE")
                    .detail("userId", auditUser.id())
                    .detail("username", auditUser.username())
                    .log();
                
                // Create sanitized template data for state after
                Map<String, Object> stateAfter = sanitizeTemplateManagerData(updateDto);
                
                // Add the template name from the response to the audit data
                stateAfter.put("templateName", responseDto.getTemplateName());
                
                // Create and publish audit event with both states
                TemplateManagerAuditEvent auditEvent = TemplateManagerAuditEvent.createWithStates(
                    templateManagerId,
                    "UPDATE",
                    UUID.fromString(auditUser.id()),
                    auditUser.username(),
                    responseDto.getTenantId(),
                    stateBefore,
                    stateAfter
                );
                
                auditEventPublisher.publishAuditEvent(auditEvent);
                
                logger.info()
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_TEMPLATE_MANAGER_UPDATE)
                    .detail("templateManagerId", templateManagerId)
                    .log();
                        
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_TEMPLATE_MANAGER_UPDATE)
                    .detail("templateManagerId", templateManagerId)
                    .log();
            }
        }
        
        return result;
    }
    
    /**
     * Audits template manager deletion operations.
     * 
     * @param joinPoint the method execution join point
     * @return the result of the method execution
     * @throws Throwable if an error occurs during method execution
     */
    @Around("execution(* com.aidentx.core.notifications_management.service.impl.TemplateManagerServiceImpl.deleteTemplateManager(..))")
    public Object auditTemplateManagerDeletion(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        UUID templateManagerId = (UUID) args[0];
        
        logger.info()
            .message(AppConstants.AUDIT_LOG_AUDITING_TEMPLATE_MANAGER_DELETION)
            .detail("templateManagerId", templateManagerId)
            .log();
        
        // Fetch template information BEFORE deletion
        TemplateManager templateBeforeDeletion = null;
        String tenantId = null;
        Map<String, Object> templateData = new HashMap<>();
        
        try {
            Optional<TemplateManager> templateOpt = templateManagerRepository.findById(templateManagerId);
            if (templateOpt.isPresent()) {
                templateBeforeDeletion = templateOpt.get();
                tenantId = templateBeforeDeletion.getTenantId();
                
                // Create template data with the information we have
                templateData.put("templateName", templateBeforeDeletion.getTemplateName());
                templateData.put("tenantId", templateBeforeDeletion.getTenantId());
                templateData.put("triggerType", templateBeforeDeletion.getTriggerType().toString());
                templateData.put("language", templateBeforeDeletion.getLanguage().toString());
                templateData.put("isEnabled", templateBeforeDeletion.getIsEnabled());
                templateData.put("templateSubject", templateBeforeDeletion.getTemplateSubject());
                templateData.put("emailBody", templateBeforeDeletion.getEmailBody());
            }
        } catch (Exception e) {
            logger.warn()
                .message("Failed to fetch template before deletion for audit")
                .detail("templateManagerId", templateManagerId)
                .detail("error", e.getMessage())
                .log();
        }
        
        // Execute the actual method (delete the template)
        Object result = joinPoint.proceed();
        
        try {
            // Get user information
            AuditUser auditUser = securityContextService.getAuditUser();
            
            logger.info()
                .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT)
                .detail("templateManagerId", templateManagerId)
                .detail("action", "DELETE")
                .detail("userId", auditUser.id())
                .detail("username", auditUser.username())
                .log();
            
            // Create and publish audit event with proper data
            TemplateManagerAuditEvent auditEvent = TemplateManagerAuditEvent.create(
                templateManagerId,
                "DELETE",
                UUID.fromString(auditUser.id()),
                auditUser.username(),
                tenantId, // Now we have the actual tenantId
                templateData // Now we have the actual template data
            );
            
            auditEventPublisher.publishAuditEvent(auditEvent);
            
            logger.info()
                .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_TEMPLATE_MANAGER_DELETION)
                .detail("templateManagerId", templateManagerId)
                .log();
                    
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_TEMPLATE_MANAGER_DELETION)
                .detail("templateManagerId", templateManagerId)
                .log();
        }
        
        return result;
    }
    
    /**
     * Audits template manager restore operations.
     * 
     * @param joinPoint the join point representing the method execution
     * @return the result of the method execution
     * @throws Throwable if an error occurs during method execution
     */
    @Around("execution(* com.aidentx.core.notifications_management.service.impl.TemplateManagerServiceImpl.restoreTemplateVersion(..))")
    public Object auditTemplateManagerRestore(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        UUID templateManagerId = (UUID) args[0];
        Integer versionNumber = (Integer) args[1];
        String changeComment = (String) args[2];
        
        logger.info()
            .message(AppConstants.AUDIT_LOG_AUDITING_TEMPLATE_MANAGER_RESTORE)
            .detail("templateManagerId", templateManagerId)
            .detail("versionNumber", versionNumber)
            .log();
        
        // Capture the state before restore (current active version)
        Map<String, Object> stateBefore = null;
        try {
            Optional<TemplateManager> existingTemplate = templateManagerRepository.findById(templateManagerId);
            if (existingTemplate.isPresent()) {
                stateBefore = sanitizeTemplateManagerData(existingTemplate.get());
            }
        } catch (Exception e) {
            logger.warn()
                .message("Could not retrieve template manager state before restore")
                .detail("templateManagerId", templateManagerId)
                .detail("error", e.getMessage())
                .log();
        }
        
        // Execute the actual method
        Object result = joinPoint.proceed();
        
        if (result instanceof TemplateManagerResponseDto responseDto) {
            try {
                // Get user information
                AuditUser auditUser = securityContextService.getAuditUser();
                
                logger.info()
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT)
                    .detail("templateManagerId", templateManagerId)
                    .detail("action", "RESTORE")
                    .detail("versionNumber", versionNumber)
                    .detail("userId", auditUser.id())
                    .detail("username", auditUser.username())
                    .log();
                
                // Create sanitized template data for state after
                Map<String, Object> stateAfter = sanitizeTemplateManagerData(responseDto);
                
                // Add restore-specific information
                stateAfter.put("restoredFromVersion", versionNumber);
                if (changeComment != null && !changeComment.trim().isEmpty()) {
                    stateAfter.put("changeComment", changeComment);
                }
                
                // Create and publish audit event with both states
                TemplateManagerAuditEvent auditEvent = TemplateManagerAuditEvent.createWithStates(
                    templateManagerId,
                    "RESTORE",
                    UUID.fromString(auditUser.id()),
                    auditUser.username(),
                    responseDto.getTenantId(),
                    stateBefore,
                    stateAfter
                );
                
                auditEventPublisher.publishAuditEvent(auditEvent);
                
                logger.info()
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_TEMPLATE_MANAGER_RESTORE)
                    .detail("templateManagerId", templateManagerId)
                    .detail("versionNumber", versionNumber)
                    .log();
                        
            } catch (Exception e) {
                logger.error(e)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_TEMPLATE_MANAGER_RESTORE)
                    .detail("templateManagerId", templateManagerId)
                    .detail("versionNumber", versionNumber)
                    .log();
            }
        }
        
        return result;
    }
    
    /**
     * Sanitizes template manager data for audit logging.
     * Removes or masks sensitive information while preserving important audit details.
     * 
     * @param data the template manager data to sanitize
     * @return sanitized data map
     */
    private Map<String, Object> sanitizeTemplateManagerData(Object data) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = objectMapper.convertValue(data, Map.class);
            Map<String, Object> sanitized = new HashMap<>();
            
            // Only include relevant template fields, exclude internal/metadata fields
            String[] allowedFields = {
                "language",
                "emailBody", 
                "isEnabled",
                "triggerType",
                "templateName",
                "templateSubject"
            };
            
            for (String field : allowedFields) {
                if (dataMap.containsKey(field)) {
                    sanitized.put(field, dataMap.get(field));
                }
            }
            
            return sanitized;
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.AUDIT_LOG_FAILED_TO_SANITIZE_TEMPLATE_MANAGER_DATA)
                .log();
            return new HashMap<>();
        }
    }
}
