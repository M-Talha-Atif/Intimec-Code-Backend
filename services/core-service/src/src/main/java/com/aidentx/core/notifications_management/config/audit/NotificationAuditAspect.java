package com.aidentx.core.notifications_management.config.audit;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.model.dto.EmailConfigurationCreateDto;
import com.aidentx.core.notifications_management.model.dto.EmailConfigurationResponseDto;
import com.aidentx.core.notifications_management.model.dto.EmailConfigurationUpdateDto;
import com.aidentx.core.notifications_management.model.entity.EmailConfiguration;
import com.aidentx.core.notifications_management.repository.EmailConfigurationRepository;
import com.aidentx.core.notifications_management.service.impl.EmailConfigurationServiceImpl;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Aspect for auditing notification management operations.
 * <p>
 * This aspect intercepts CRUD operations on notification management entities
 * to capture audit information and publish it to Kafka for compliance and
 * monitoring purposes. Ensures sensitive data like passwords are excluded
 * from audit logs.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-22
 */
@Aspect
@Component
public class NotificationAuditAspect {

    private static final String LOG_CATEGORY = "notification-audit-aspect";
    
    private final NotificationSecurityContextService securityContextService;
    private final IUnifiedLogger logger;
    private final NotificationAuditEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final EmailConfigurationRepository repository;

    public NotificationAuditAspect(
            NotificationAuditEventPublisher eventPublisher,
            ObjectMapper objectMapper,
            UnifiedLoggerFactory unifiedLoggerFactory,
            NotificationSecurityContextService securityContextService,
            EmailConfigurationRepository repository) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.logger = unifiedLoggerFactory.getLogger(
            AppConstants.NOTIFICATION_AUDIT_ASPECT_LOGGER_NAME, 
            LOG_CATEGORY
        );
        this.securityContextService = securityContextService;
        this.repository = repository;
    }

    // ==================== EMAIL CONFIGURATION POINTCUTS ====================

    @Pointcut("execution(public * com.aidentx.core.notifications_management.service.impl.EmailConfigurationServiceImpl.createEmailConfiguration(..))")
    public void createEmailConfigPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.core.notifications_management.service.impl.EmailConfigurationServiceImpl.createOrUpdateEmailConfiguration(..))")
    public void createOrUpdateEmailConfigPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.core.notifications_management.service.impl.EmailConfigurationServiceImpl.updateEmailConfiguration(..))")
    public void updateEmailConfigPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.core.notifications_management.service.impl.EmailConfigurationServiceImpl.updateEmailConfigurationByTenant(..))")
    public void updateEmailConfigByTenantPointcut() {
    }

    @Pointcut("execution(public * com.aidentx.core.notifications_management.service.impl.EmailConfigurationServiceImpl.deleteEmailConfiguration(..))")
    public void deleteEmailConfigPointcut() {
    }

    // ==================== EMAIL CONFIGURATION AUDIT METHODS ====================

    @Around("createEmailConfigPointcut()")
    public Object auditCreateEmailConfig(ProceedingJoinPoint pjp) throws Throwable {
        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_ASPECT_TRIGGERED_EMAIL_CONFIG_CREATION)
                .log();
                
        EmailConfigurationCreateDto createDto = (EmailConfigurationCreateDto) pjp.getArgs()[0];

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_EMAIL_CONFIG_CREATION)
                .details(Map.of(
                        "tenantId", createDto.getTenantId(),
                        "smtpHostname", createDto.getSmtpHostname(),
                        "smtpPort", createDto.getSmtpPort().toString(),
                        "emailAddress", createDto.getEmailAddress()))
                .log();

        // Execute the method
        EmailConfigurationResponseDto result = (EmailConfigurationResponseDto) pjp.proceed();

        try {
            // Sanitize result and publish audit event (remove password)
            JsonNode stateAfter = sanitizeEmailConfigData(objectMapper.valueToTree(result));
            publishEmailConfigAuditEvent(
                AppConstants.AUDIT_EVENT_CREATE, 
                result.getId().toString(), 
                "Email server", 
                result.getTenantId(), 
                null, 
                stateAfter
            );
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_EMAIL_CONFIG_CREATION)
                    .details(Map.of("emailConfigId", result.getId().toString()))
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_EMAIL_CONFIG_CREATION)
                    .details(Map.of("emailConfigId", result.getId().toString(), "error", e.getMessage()))
                    .log();
        }

        return result;
    }

    @Around("createOrUpdateEmailConfigPointcut()")
    public Object auditCreateOrUpdateEmailConfig(ProceedingJoinPoint pjp) throws Throwable {
        EmailConfigurationCreateDto createDto = (EmailConfigurationCreateDto) pjp.getArgs()[0];

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_EMAIL_CONFIG_CREATE_OR_UPDATE)
                .details(Map.of(
                        "tenantId", createDto.getTenantId(),
                        "smtpHostname", createDto.getSmtpHostname(),
                        "emailAddress", createDto.getEmailAddress()))
                .log();

        // Execute the method
        EmailConfigurationResponseDto result = (EmailConfigurationResponseDto) pjp.proceed();

        try {
            // Determine if this was a create or update based on response
            String action = result.getCreatedAt().equals(result.getUpdatedAt()) ? 
                AppConstants.AUDIT_EVENT_CREATE : AppConstants.AUDIT_EVENT_UPDATE;
            
            JsonNode stateAfter = sanitizeEmailConfigData(objectMapper.valueToTree(result));
            publishEmailConfigAuditEvent(
                action, 
                result.getId().toString(), 
                "Email server", 
                result.getTenantId(), 
                null, 
                stateAfter
            );
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_EMAIL_CONFIG_CREATE_OR_UPDATE)
                    .details(Map.of("emailConfigId", result.getId().toString(), "action", action))
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_EMAIL_CONFIG_CREATE_OR_UPDATE)
                    .details(Map.of("emailConfigId", result.getId().toString(), "error", e.getMessage()))
                    .log();
        }

        return result;
    }

    @Around("updateEmailConfigPointcut()")
    public Object auditUpdateEmailConfig(ProceedingJoinPoint pjp) throws Throwable {
        UUID configId = (UUID) pjp.getArgs()[0];
        EmailConfigurationUpdateDto updateDto = (EmailConfigurationUpdateDto) pjp.getArgs()[1];

        // Get state before update
        EmailConfigurationServiceImpl service = (EmailConfigurationServiceImpl) pjp.getTarget();
        JsonNode stateBefore = null;
        String emailAddress = "UNKNOWN";
        String tenantId = "UNKNOWN";

        try {
            EmailConfiguration currentConfigEntity = repository.findById(configId).orElse(null);
            if (currentConfigEntity != null) {
                stateBefore = sanitizeEmailConfigData(objectMapper.valueToTree(currentConfigEntity));
                emailAddress = currentConfigEntity.getEmailAddress();
                tenantId = currentConfigEntity.getTenantId();
            }
        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_COULD_NOT_RETRIEVE_EMAIL_CONFIG_STATE_BEFORE_UPDATE)
                    .details(Map.of("configId", configId.toString(), "error", e.getMessage()))
                    .log();
        }

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_EMAIL_CONFIG_UPDATE)
                .details(Map.of("configId", configId.toString()))
                .log();

        // Execute the method
        EmailConfigurationResponseDto result = (EmailConfigurationResponseDto) pjp.proceed();

        try {
            // Sanitize result and publish audit event
            JsonNode stateAfter = sanitizeEmailConfigData(objectMapper.valueToTree(result));
            publishEmailConfigAuditEvent(
                AppConstants.AUDIT_EVENT_UPDATE, 
                result.getId().toString(), 
                "Email server", 
                result.getTenantId() != null ? result.getTenantId() : tenantId, 
                stateBefore, 
                stateAfter
            );
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_EMAIL_CONFIG_UPDATE)
                    .details(Map.of("emailConfigId", result.getId().toString()))
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_EMAIL_CONFIG_UPDATE)
                    .details(Map.of("emailConfigId", result.getId().toString(), "error", e.getMessage()))
                    .log();
        }

        return result;
    }

    @Around("updateEmailConfigByTenantPointcut()")
    public Object auditUpdateEmailConfigByTenant(ProceedingJoinPoint pjp) throws Throwable {
        String tenantId = (String) pjp.getArgs()[0];
        EmailConfigurationUpdateDto updateDto = (EmailConfigurationUpdateDto) pjp.getArgs()[1];

        // Get state before update
        EmailConfigurationServiceImpl service = (EmailConfigurationServiceImpl) pjp.getTarget();
        JsonNode stateBefore = null;
        String emailAddress = "UNKNOWN";

        try {
            // Find the existing configuration by tenant ID for state before
            EmailConfiguration currentConfigEntity = repository.findByTenantId(tenantId).orElse(null);
            if (currentConfigEntity != null) {
                stateBefore = sanitizeEmailConfigData(objectMapper.valueToTree(currentConfigEntity));
                emailAddress = currentConfigEntity.getEmailAddress();
            }
        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_COULD_NOT_RETRIEVE_EMAIL_CONFIG_STATE_BEFORE_UPDATE)
                    .details(Map.of("tenantId", tenantId, "error", e.getMessage()))
                    .log();
        }

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_EMAIL_CONFIG_UPDATE)
                .details(Map.of("tenantId", tenantId))
                .log();

        // Execute the method
        EmailConfigurationResponseDto result = (EmailConfigurationResponseDto) pjp.proceed();

        try {
            // Sanitize result and publish audit event
            JsonNode stateAfter = sanitizeEmailConfigData(objectMapper.valueToTree(result));
            publishEmailConfigAuditEvent(
                AppConstants.AUDIT_EVENT_UPDATE,
                result.getId().toString(),
                "Email server",
                tenantId,
                stateBefore,
                stateAfter
            );
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_EMAIL_CONFIG_UPDATE)
                    .details(Map.of("emailConfigId", result.getId().toString()))
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_EMAIL_CONFIG_UPDATE)
                    .details(Map.of("emailConfigId", result.getId().toString(), "error", e.getMessage()))
                    .log();
        }

        return result;
    }

    @Around("deleteEmailConfigPointcut()")
    public Object auditDeleteEmailConfig(ProceedingJoinPoint pjp) throws Throwable {
        UUID configId = (UUID) pjp.getArgs()[0];

        // Get state before deletion
        EmailConfigurationServiceImpl service = (EmailConfigurationServiceImpl) pjp.getTarget();
        JsonNode stateBefore = null;
        String emailAddress = null;
        String tenantId = null;

        try {
            EmailConfiguration currentConfigEntity = repository.findById(configId).orElse(null);
            if (currentConfigEntity != null) {
                stateBefore = sanitizeEmailConfigData(objectMapper.valueToTree(currentConfigEntity));
                emailAddress = currentConfigEntity.getEmailAddress();
                tenantId = currentConfigEntity.getTenantId();
            } else {
                return pjp.proceed(); // Continue without audit if config doesn't exist
            }
        } catch (Exception e) {
            logger.warn()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_COULD_NOT_RETRIEVE_EMAIL_CONFIG_STATE_BEFORE_DELETION)
                    .details(Map.of("configId", configId.toString(), "error", e.getMessage()))
                    .log();
            return pjp.proceed(); // Continue without audit if config doesn't exist
        }

        logger.info()
                .category(LOG_CATEGORY)
                .message(AppConstants.AUDIT_LOG_AUDITING_EMAIL_CONFIG_DELETION)
                .details(Map.of("configId", configId.toString()))
                .log();

        // Execute the method
        Object result = pjp.proceed();

        try {
            // Publish audit event
            publishEmailConfigAuditEvent(
                AppConstants.AUDIT_EVENT_DELETE, 
                configId.toString(), 
                "Email server", 
                tenantId, 
                stateBefore, 
                null
            );
            
            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_EMAIL_CONFIG_DELETION)
                    .details(Map.of("emailConfigId", configId.toString()))
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_EMAIL_CONFIG_DELETION)
                    .details(Map.of("emailConfigId", configId.toString(), "error", e.getMessage()))
                    .log();
        }

        return result;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Sanitizes email configuration data by removing sensitive information like passwords.
     * 
     * @param jsonNode the original JSON node
     * @return sanitized JSON node without sensitive data
     */
    private JsonNode sanitizeEmailConfigData(JsonNode jsonNode) {
        if (jsonNode.isObject()) {
            ObjectNode originalNode = (ObjectNode) jsonNode;
            ObjectNode sanitizedNode = objectMapper.createObjectNode();
            
            // Only include relevant email configuration fields, exclude internal/metadata fields
            String[] allowedFields = {
                "smtpHostname",
                "smtpPort", 
                "encryptionType",
                "emailAddress",
                "authMethod",
                "username"
                // Note: password is intentionally excluded for security
            };
            
            for (String field : allowedFields) {
                if (originalNode.has(field)) {
                    sanitizedNode.set(field, originalNode.get(field));
                }
            }
            
            return sanitizedNode;
        }
        return jsonNode;
    }

    /**
     * Publishes email configuration audit event.
     */
    private void publishEmailConfigAuditEvent(String action, String configId, String emailAddress, 
            String tenantId, JsonNode stateBefore, JsonNode stateAfter) {
        try {
            AuditUser user = securityContextService.getAuditUser();

            logger.info()
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_CREATING_AUDIT_EVENT)
                    .details(Map.of(
                            "action", action,
                            "configId", configId,
                            "userId", user.id(),
                            "username", user.username()))
                    .log();

            NotificationAuditEvent event = NotificationAuditEvent.create(
                    user.id(),
                    user.username(),
                    user.ipAddress(),
                    action,
                    AppConstants.ENTITY_TYPE_EMAIL_CONFIGURATION,
                    configId,
                    emailAddress,
                    tenantId,
                    stateBefore,
                    stateAfter);
            
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            logger.error(e)
                    .category(LOG_CATEGORY)
                    .message(AppConstants.AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_EMAIL_CONFIG_AUDIT_EVENT)
                    .details(Map.of(
                            "action", action,
                            "configId", configId,
                            "error", e.getMessage()))
                    .log();
        }
    }
}
