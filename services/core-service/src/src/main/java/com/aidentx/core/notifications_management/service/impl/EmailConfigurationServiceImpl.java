package com.aidentx.core.notifications_management.service.impl;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.enums.AuthMethod;
import com.aidentx.core.notifications_management.model.dto.*;
import com.aidentx.core.notifications_management.model.entity.EmailConfiguration;
import com.aidentx.core.notifications_management.model.entity.TemplateManager;
import com.aidentx.core.notifications_management.repository.EmailConfigurationRepository;
import com.aidentx.core.notifications_management.repository.TemplateManagerRepository;
import com.aidentx.core.notifications_management.service.EmailConfigurationService;
import com.aidentx.core.notifications_management.service.VelocityTemplateService;
import com.aidentx.core.shared.security.JwtUtil;
import com.aidentx.core.shared.security.SecurityConstants;
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

import java.util.Properties;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Implementation of EmailConfigurationService for managing email configurations.
 * <p>
 * This service provides comprehensive email configuration management including
 * creation, updates, retrieval, deletion, and testing of email settings.
 * All operations include proper validation, logging, and error handling.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Service
@Transactional
public class EmailConfigurationServiceImpl implements EmailConfigurationService {
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );
    
    private final EmailConfigurationRepository emailConfigurationRepository;
    private final TemplateManagerRepository templateManagerRepository;
    private final VelocityTemplateService velocityTemplateService;
    private final JwtUtil jwtUtil;
    private final IUnifiedLogger logger;
    
    // Constants for JWT extraction
    private static final String SECURITY_ANONYMOUS_USER = "anonymousUser";
    private static final String SECURITY_BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    
    /**
     * Constructs a new EmailConfigurationServiceImpl with required dependencies.
     * 
     * @param emailConfigurationRepository Repository for email configuration persistence
     * @param templateManagerRepository Repository for template manager persistence
     * @param velocityTemplateService Service for processing Velocity templates
     * @param jwtUtil Utility for JWT token operations
     * @param loggerFactory Factory for creating unified loggers
     */
    public EmailConfigurationServiceImpl(EmailConfigurationRepository emailConfigurationRepository,
                                        TemplateManagerRepository templateManagerRepository,
                                        VelocityTemplateService velocityTemplateService,
                                        JwtUtil jwtUtil,
                                        UnifiedLoggerFactory loggerFactory) {
        this.emailConfigurationRepository = emailConfigurationRepository;
        this.templateManagerRepository = templateManagerRepository;
        this.velocityTemplateService = velocityTemplateService;
        this.jwtUtil = jwtUtil;
        this.logger = loggerFactory.getLogger(AppConstants.EMAIL_CONFIG_SERVICE_LOGGER_NAME,
                                            AppConstants.LOG_CATEGORY_EMAIL_CONFIG_SERVICE);
    }
    
    /**
     * Creates a new email configuration for a tenant.
     * 
     * @param createDto the email configuration data
     * @return EmailConfigurationResponseDto containing the created configuration
     */
    @Override
    public EmailConfigurationResponseDto createEmailConfiguration(EmailConfigurationCreateDto createDto) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_CREATE_EMAIL_CONFIG))
                .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                .log();
        
        try {
            // Validate input data
            validateCreateDto(createDto);
            
            logger.info().message(String.format(AppConstants.LOG_CREATING_EMAIL_CONFIG, createDto.getTenantId()))
                    .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                    .detail(AppConstants.DETAIL_SMTP_HOSTNAME, createDto.getSmtpHostname())
                    .detail(AppConstants.DETAIL_EMAIL_ADDRESS, createDto.getEmailAddress())
                    .log();
            
            // Check if configuration already exists for tenant
            if (emailConfigurationRepository.existsByTenantId(createDto.getTenantId())) {
                throw new RuntimeException(String.format(AppConstants.MSG_EMAIL_CONFIG_ALREADY_EXISTS, 
                        createDto.getTenantId()));
            }
            
            // Create new email configuration
            EmailConfiguration emailConfig = createEntityFromDto(createDto);
            EmailConfiguration savedConfig = emailConfigurationRepository.save(emailConfig);
            
            logger.info().message(String.format(AppConstants.LOG_EMAIL_CONFIG_CREATED, savedConfig.getId()))
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, savedConfig.getId())
                    .detail(AppConstants.DETAIL_TENANT_ID, savedConfig.getTenantId())
                    .log();
            
            return createResponseDto(savedConfig);
            
        } catch (DataIntegrityViolationException e) {
            // Handle unique constraint violation
            String errorMsg = String.format(AppConstants.MSG_EMAIL_CONFIG_ALREADY_EXISTS, createDto.getTenantId());
            logger.error(e).message(errorMsg)
                    .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                    .log();
            throw new RuntimeException(errorMsg, e);
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_CREATE_EMAIL_CONFIG, e.getMessage()))
                    .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                    .log();
            throw e;
        }
    }
    
    /**
     * Updates an existing email configuration.
     * 
     * @param id the unique identifier of the configuration to update
     * @param updateDto the email configuration update data
     * @return EmailConfigurationResponseDto containing the updated configuration
     */
    @Override
    public EmailConfigurationResponseDto updateEmailConfiguration(UUID id, EmailConfigurationUpdateDto updateDto) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_UPDATE_EMAIL_CONFIG))
                .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                .log();
        
        try {
            logger.info().message(String.format(AppConstants.LOG_UPDATING_EMAIL_CONFIG, id))
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                    .log();
            
            EmailConfiguration existingConfig = findConfigurationById(id);
            
            // Validate update data
            validateUpdateDto(updateDto);
            
            // Update configuration
            updateEntityFromDto(updateDto, existingConfig);
            EmailConfiguration updatedConfig = emailConfigurationRepository.save(existingConfig);
            
            logger.info().message(String.format(AppConstants.LOG_EMAIL_CONFIG_UPDATED, updatedConfig.getId()))
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, updatedConfig.getId())
                    .detail(AppConstants.DETAIL_TENANT_ID, updatedConfig.getTenantId())
                    .log();
            
            return createResponseDto(updatedConfig);
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_UPDATE_EMAIL_CONFIG, e.getMessage()))
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                    .log();
            throw e;
        }
    }
    
    /**
     * Updates the email configuration for a specific tenant.
     * 
     * @param tenantId the unique identifier of the tenant
     * @param updateDto the email configuration update data
     * @return EmailConfigurationResponseDto containing the updated configuration
     */
    @Override
    public EmailConfigurationResponseDto updateEmailConfigurationByTenant(String tenantId, EmailConfigurationUpdateDto updateDto) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, "updateEmailConfigurationByTenant"))
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .log();
        
        try {
            logger.info().message(String.format("Updating email configuration for tenant: %s", tenantId))
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .log();
            
            // Find the configuration by tenant ID
            EmailConfiguration existingConfig = emailConfigurationRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new RuntimeException(String.format(
                            AppConstants.MSG_EMAIL_CONFIG_NOT_FOUND, tenantId)));
            
            // Validate update data
            validateUpdateDto(updateDto);
            
            // Update configuration
            updateEntityFromDto(updateDto, existingConfig);
            EmailConfiguration updatedConfig = emailConfigurationRepository.save(existingConfig);
            
            logger.info().message(String.format(AppConstants.LOG_EMAIL_CONFIG_UPDATED, updatedConfig.getId()))
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, updatedConfig.getId())
                    .detail(AppConstants.DETAIL_TENANT_ID, updatedConfig.getTenantId())
                    .log();
            
            return createResponseDto(updatedConfig);
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    "updateEmailConfigurationByTenant", e.getMessage()))
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .log();
            throw e;
        }
    }
    
    /**
     * Retrieves the email configuration for a specific tenant.
     * 
     * @param tenantId the unique identifier of the tenant
     * @return EmailConfigurationResponseDto containing the configuration
     */
    @Override
    @Transactional(readOnly = true)
    public EmailConfigurationResponseDto getEmailConfigurationByTenant(String tenantId) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_GET_EMAIL_CONFIG_BY_TENANT))
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .log();
        
        try {
            logger.info().message(String.format(AppConstants.LOG_RETRIEVING_EMAIL_CONFIG, tenantId))
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .log();
            
            EmailConfiguration config = emailConfigurationRepository.findByTenantId(tenantId)
                    .orElseThrow(() -> new RuntimeException(String.format(AppConstants.MSG_EMAIL_CONFIG_NOT_FOUND, tenantId)));
            
            logger.debug().message(String.format(AppConstants.LOG_EMAIL_CONFIG_RETRIEVED, tenantId))
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, config.getId())
                    .log();
            
            return createResponseDto(config);
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_GET_EMAIL_CONFIG_BY_TENANT, e.getMessage()))
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .log();
            throw e;
        }
    }
    
    /**
     * Deletes an email configuration.
     * 
     * @param id the unique identifier of the configuration to delete
     */
    @Override
    public void deleteEmailConfiguration(UUID id) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_DELETE_EMAIL_CONFIG))
                .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                .log();
        
        try {
            logger.info().message(String.format(AppConstants.LOG_DELETING_EMAIL_CONFIG, id))
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                    .log();
            
            EmailConfiguration config = findConfigurationById(id);
            emailConfigurationRepository.delete(config);
            
            logger.info().message(String.format(AppConstants.LOG_EMAIL_CONFIG_DELETED, id))
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                    .detail(AppConstants.DETAIL_TENANT_ID, config.getTenantId())
                    .log();
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_DELETE_EMAIL_CONFIG, e.getMessage()))
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                    .log();
            throw e;
        }
    }
    
    /**
     * Creates or updates an email configuration for a tenant.
     * If a configuration already exists for the tenant, it will be updated.
     * Otherwise, a new configuration will be created.
     * 
     * @param createDto the email configuration data
     * @return EmailConfigurationResponseDto containing the created or updated configuration
     */
    @Override
    public EmailConfigurationResponseDto createOrUpdateEmailConfiguration(EmailConfigurationCreateDto createDto) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, "createOrUpdateEmailConfiguration"))
                .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                .log();
        
        try {
            // Validate input data
            validateCreateDto(createDto);
            
            // Check if configuration already exists for tenant
            return emailConfigurationRepository.findByTenantId(createDto.getTenantId())
                    .map(existingConfig -> {
                        // Update existing configuration
                        logger.info().message(String.format("Updating existing email configuration for tenant: %s", createDto.getTenantId()))
                                .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                                .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, existingConfig.getId())
                                .log();
                        
                        updateEntityFromCreateDto(createDto, existingConfig);
                        EmailConfiguration updatedConfig = emailConfigurationRepository.save(existingConfig);
                        
                        logger.info().message(String.format(AppConstants.LOG_EMAIL_CONFIG_UPDATED, updatedConfig.getId()))
                                .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, updatedConfig.getId())
                                .detail(AppConstants.DETAIL_TENANT_ID, updatedConfig.getTenantId())
                                .log();
                        
                        return createResponseDto(updatedConfig);
                    })
                    .orElseGet(() -> {
                        // Create new configuration
                        logger.info().message(String.format(AppConstants.LOG_CREATING_EMAIL_CONFIG, createDto.getTenantId()))
                                .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                                .detail(AppConstants.DETAIL_SMTP_HOSTNAME, createDto.getSmtpHostname())
                                .detail(AppConstants.DETAIL_EMAIL_ADDRESS, createDto.getEmailAddress())
                                .log();
                        
                        EmailConfiguration emailConfig = createEntityFromDto(createDto);
                        EmailConfiguration savedConfig = emailConfigurationRepository.save(emailConfig);
                        
                        logger.info().message(String.format(AppConstants.LOG_EMAIL_CONFIG_CREATED, savedConfig.getId()))
                                .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, savedConfig.getId())
                                .detail(AppConstants.DETAIL_TENANT_ID, savedConfig.getTenantId())
                                .log();
                        
                        return createResponseDto(savedConfig);
                    });
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    "createOrUpdateEmailConfiguration", e.getMessage()))
                    .detail(AppConstants.DETAIL_TENANT_ID, createDto.getTenantId())
                    .log();
            throw e;
        }
    }
    
    /**
     * Tests an email configuration by sending a test email.
     * 
     * @param tenantId the unique identifier of the tenant
     * @param testEmailAddress the email address to send the test email to
     * @return true if the test email was sent successfully, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean testEmailConfiguration(EmailConfigurationCreateDto configDto, String testEmailAddress) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, AppConstants.METHOD_TEST_EMAIL_CONFIG))
                .detail("testEmailAddress", testEmailAddress)
                .log();
        
        try {
            logger.info().message(AppConstants.LOG_TESTING_EMAIL_CONFIG)
                    .detail("testEmailAddress", testEmailAddress)
                    .detail("smtpHost", configDto.getSmtpHostname())
                    .detail("smtpPort", configDto.getSmtpPort())
                    .log();
            
            // Validate configuration DTO
            validateCreateDto(configDto);
            
            // Validate test email address
            if (!isValidEmail(testEmailAddress)) {
                throw new IllegalArgumentException(AppConstants.VALIDATION_INVALID_EMAIL_FORMAT);
            }
            
            // Create email properties
            Properties props = new Properties();
            props.put("mail.smtp.host", configDto.getSmtpHostname());
            props.put("mail.smtp.port", String.valueOf(configDto.getSmtpPort()));
            props.put("mail.smtp.auth", "true");
            
            // Configure encryption if specified
            if (configDto.getEncryptionType() != null) {
                switch (configDto.getEncryptionType()) {
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
            
            // Create authenticator if password auth is used
            Authenticator authenticator = null;
            if (configDto.getAuthMethod() == AuthMethod.USERNAME_PASSWORD && 
                configDto.getUsername() != null && configDto.getPassword() != null) {
                authenticator = new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(configDto.getUsername(), configDto.getPassword());
                    }
                };
            }
            
            // Create session
            Session session = Session.getInstance(props, authenticator);
            
            // Create and send test message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(configDto.getEmailAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(testEmailAddress));
            message.setSubject(AppConstants.EMAIL_TEST_SUBJECT);
            message.setText(String.format(AppConstants.EMAIL_TEST_BODY, 
                configDto.getSmtpHostname(), 
                configDto.getSmtpPort(),
                configDto.getEncryptionType().name(),
                configDto.getAuthMethod().name()));
            
            // Send the message
            Transport.send(message);
            
            logger.info().message(AppConstants.LOG_EMAIL_CONFIG_TEST_SUCCESS)
                    .detail("testEmailAddress", testEmailAddress)
                    .detail("smtpHost", configDto.getSmtpHostname())
                    .log();
            
            return true;
            
        } catch (MessagingException e) {
            String errorMsg = String.format(AppConstants.EMAIL_TEST_ERROR_SMTP, e.getMessage());
            logger.error(e).message(errorMsg)
                    .detail("testEmailAddress", testEmailAddress)
                    .detail("smtpHost", configDto.getSmtpHostname())
                    .detail("smtpPort", configDto.getSmtpPort())
                    .log();
            return false;
            
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, 
                    AppConstants.METHOD_TEST_EMAIL_CONFIG, e.getMessage()))
                    .detail("testEmailAddress", testEmailAddress)
                    .log();
            throw e;
        }
    }
    
    /**
     * Tests the complete email workflow with template processing.
     * 
     * @param testDto the email template test data including tenant, trigger type, language, and variables
     * @return EmailTemplateTestResultDto containing the test results and processed content
     */
    @Override
    @Transactional(readOnly = true)
    public EmailTemplateTestResultDto testEmailTemplate(EmailTemplateTestDto testDto) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, "testEmailTemplate"))
                .detail(AppConstants.DETAIL_TENANT_ID, testDto.getTenantId())
                .detail(AppConstants.DETAIL_TRIGGER_TYPE, testDto.getTriggerType())
                .detail(AppConstants.DETAIL_LANGUAGE, testDto.getLanguage())
                .log();
        
        try {
            // Validate input
            validateTemplateTestDto(testDto);
            
            logger.info().message("Testing complete email template workflow")
                    .detail(AppConstants.DETAIL_TENANT_ID, testDto.getTenantId())
                    .detail(AppConstants.DETAIL_TRIGGER_TYPE, testDto.getTriggerType())
                    .detail(AppConstants.DETAIL_LANGUAGE, testDto.getLanguage())
                    .detail("testEmailAddress", testDto.getTestEmailAddress())
                    .log();
            
            // 1. Retrieve template from database
            TemplateManager template = templateManagerRepository
                    .findByTenantIdAndTriggerTypeAndLanguage(
                            testDto.getTenantId(), 
                            testDto.getTriggerType(), 
                            testDto.getLanguage())
                    .filter(t -> t.getIsEnabled()) // Filter for enabled templates
                    .orElseThrow(() -> new RuntimeException(String.format(
                            "No enabled template found for tenant %s, trigger type %s, and language %s",
                            testDto.getTenantId(), testDto.getTriggerType(), testDto.getLanguage())));
            
            logger.debug().message("Template retrieved from database")
                    .detail(AppConstants.DETAIL_TEMPLATE_MANAGER_ID, template.getId())
                    .detail(AppConstants.DETAIL_TEMPLATE_NAME, template.getTemplateName())
                    .log();
            
            // 2. Retrieve email configuration for tenant
            EmailConfiguration emailConfig = emailConfigurationRepository
                    .findByTenantId(testDto.getTenantId())
                    .orElseThrow(() -> new RuntimeException(String.format(
                            "No email configuration found for tenant: %s", testDto.getTenantId())));
            
            logger.debug().message("Email configuration retrieved")
                    .detail(AppConstants.DETAIL_SMTP_HOSTNAME, emailConfig.getSmtpHostname())
                    .detail(AppConstants.DETAIL_SMTP_PORT, emailConfig.getSmtpPort())
                    .detail(AppConstants.DETAIL_EMAIL_ADDRESS, emailConfig.getEmailAddress())
                    .log();
            
            // 3. Process template with Velocity
            String[] processedContent = velocityTemplateService.processEmailTemplate(
                    template.getTemplateSubject(),
                    template.getEmailBody(),
                    testDto.getTemplateVariables()
            );
            
            String processedSubject = processedContent[0];
            String processedBody = processedContent[1];
            
            logger.debug().message("Template processed with Velocity")
                    .detail("originalSubject", template.getTemplateSubject())
                    .detail("processedSubject", processedSubject)
                    .detail("variableCount", testDto.getTemplateVariables().size())
                    .log();
            
            // 4. Send email using processed template
            boolean emailSent = sendProcessedEmail(emailConfig, testDto.getTestEmailAddress(), 
                    processedSubject, processedBody);
            
            if (emailSent) {
                logger.info().message("Email template test completed successfully")
                        .detail(AppConstants.DETAIL_TENANT_ID, testDto.getTenantId())
                        .detail(AppConstants.DETAIL_TEMPLATE_NAME, template.getTemplateName())
                        .detail("testEmailAddress", testDto.getTestEmailAddress())
                        .log();
                
                return new EmailTemplateTestResultDto(
                        processedSubject,
                        processedBody,
                        template.getTemplateName(),
                        emailConfig.getSmtpHostname(),
                        emailConfig.getSmtpPort().intValue(),
                        emailConfig.getEmailAddress(),
                        testDto.getTestEmailAddress()
                );
            } else {
                return new EmailTemplateTestResultDto("Failed to send email");
            }
            
        } catch (Exception e) {
            logger.error(e).message(String.format("Email template test failed: %s", e.getMessage()))
                    .detail(AppConstants.DETAIL_TENANT_ID, testDto.getTenantId())
                    .detail(AppConstants.DETAIL_TRIGGER_TYPE, testDto.getTriggerType())
                    .detail(AppConstants.DETAIL_LANGUAGE, testDto.getLanguage())
                    .log();
            return new EmailTemplateTestResultDto("Email template test failed: " + e.getMessage());
        }
    }
    
    /**
     * Validates the template test DTO.
     * 
     * @param testDto the DTO to validate
     */
    private void validateTemplateTestDto(EmailTemplateTestDto testDto) {
        if (testDto.getTenantId() == null || testDto.getTenantId().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_TENANT_ID_REQUIRED);
        }
        if (testDto.getTriggerType() == null) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_TRIGGER_TYPE_REQUIRED);
        }
        if (testDto.getLanguage() == null) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_LANGUAGE_REQUIRED);
        }
        if (testDto.getTestEmailAddress() == null || testDto.getTestEmailAddress().trim().isEmpty()) {
            throw new IllegalArgumentException("Test email address is required");
        }
        if (!isValidEmail(testDto.getTestEmailAddress())) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_INVALID_EMAIL_FORMAT);
        }
        if (testDto.getTemplateVariables() == null) {
            throw new IllegalArgumentException("Template variables are required");
        }
    }
    
    /**
     * Sends an email using processed template content.
     * 
     * @param emailConfig the email configuration
     * @param toEmail the recipient email address
     * @param subject the processed email subject
     * @param body the processed email body
     * @return true if email was sent successfully, false otherwise
     */
    private boolean sendProcessedEmail(EmailConfiguration emailConfig, String toEmail, 
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
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);
            
            Transport.send(message);
            return true;
            
        } catch (MessagingException e) {
            logger.error(e).message("Failed to send processed email: " + e.getMessage())
                    .detail("toEmail", toEmail)
                    .detail("smtpHost", emailConfig.getSmtpHostname())
                    .log();
            return false;
        }
    }
    
    /**
     * Validates the create DTO for required fields and constraints.
     * 
     * @param createDto the DTO to validate
     */
    private void validateCreateDto(EmailConfigurationCreateDto createDto) {
        if (createDto.getTenantId() == null || createDto.getTenantId().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_TENANT_ID_REQUIRED);
        }
        if (createDto.getSmtpHostname() == null || createDto.getSmtpHostname().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_SMTP_HOSTNAME_REQUIRED);
        }
        if (createDto.getSmtpPort() == null) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_SMTP_PORT_REQUIRED);
        }
        if (createDto.getSmtpPort() < 1 || createDto.getSmtpPort() > 65535) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_INVALID_SMTP_PORT);
        }
        if (createDto.getEmailAddress() == null || createDto.getEmailAddress().trim().isEmpty()) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_EMAIL_ADDRESS_REQUIRED);
        }
        if (!isValidEmail(createDto.getEmailAddress())) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_INVALID_EMAIL_FORMAT);
        }
        if (createDto.getEncryptionType() == null) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_ENCRYPTION_TYPE_REQUIRED);
        }
        if (createDto.getAuthMethod() == null) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_AUTH_METHOD_REQUIRED);
        }
        
        // Validate authentication-specific fields
        validateAuthenticationFields(createDto.getAuthMethod(), createDto.getUsername(), 
                createDto.getPassword(), createDto.getApiKey());
    }
    
    /**
     * Validates the update DTO for constraints.
     * 
     * @param updateDto the DTO to validate
     */
    private void validateUpdateDto(EmailConfigurationUpdateDto updateDto) {
        if (updateDto.getSmtpPort() != null && (updateDto.getSmtpPort() < 1 || updateDto.getSmtpPort() > 65535)) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_INVALID_SMTP_PORT);
        }
        if (updateDto.getEmailAddress() != null && !isValidEmail(updateDto.getEmailAddress())) {
            throw new IllegalArgumentException(AppConstants.VALIDATION_INVALID_EMAIL_FORMAT);
        }
        
        // Validate authentication-specific fields if auth method is being updated
        if (updateDto.getAuthMethod() != null) {
            validateAuthenticationFields(updateDto.getAuthMethod(), updateDto.getUsername(), 
                    updateDto.getPassword(), updateDto.getApiKey());
        }
    }
    
    /**
     * Validates authentication-specific fields based on the auth method.
     * 
     * @param authMethod the authentication method
     * @param username the username
     * @param password the password
     * @param apiKey the API key
     */
    private void validateAuthenticationFields(AuthMethod authMethod, String username, String password, String apiKey) {
        switch (authMethod) {
            case USERNAME_PASSWORD:
                if (username == null || username.trim().isEmpty()) {
                    throw new IllegalArgumentException(AppConstants.VALIDATION_USERNAME_REQUIRED);
                }
                if (password == null || password.trim().isEmpty()) {
                    throw new IllegalArgumentException(AppConstants.VALIDATION_PASSWORD_REQUIRED);
                }
                break;
            case API_KEY:
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    throw new IllegalArgumentException(AppConstants.VALIDATION_API_KEY_REQUIRED);
                }
                break;
            case OAUTH2:
                // OAuth2 validation can be added here if needed
                break;
        }
    }
    
    /**
     * Validates email address format.
     * 
     * @param email the email address to validate
     * @return true if valid, false otherwise
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * Creates an EmailConfiguration entity from a create DTO.
     * 
     * @param createDto the create DTO
     * @return the created entity
     */
    private EmailConfiguration createEntityFromDto(EmailConfigurationCreateDto createDto) {
        String userId = extractUserIdFromCurrentRequest();
        String username = extractUsernameFromCurrentRequest();
        
        EmailConfiguration entity = new EmailConfiguration(
                createDto.getTenantId(),
                createDto.getSmtpHostname(),
                createDto.getSmtpPort(),
                createDto.getEncryptionType(),
                createDto.getEmailAddress(),
                createDto.getAuthMethod(),
                userId,
                username
        );
        
        // Set authentication fields
        entity.setUsername(createDto.getUsername());
        entity.setPassword(createDto.getPassword());
        entity.setApiKey(createDto.getApiKey());
        
        return entity;
    }
    
    /**
     * Updates an EmailConfiguration entity from a create DTO.
     * 
     * @param createDto the create DTO
     * @param entity the entity to update
     */
    private void updateEntityFromCreateDto(EmailConfigurationCreateDto createDto, EmailConfiguration entity) {
        entity.setSmtpHostname(createDto.getSmtpHostname());
        entity.setSmtpPort(createDto.getSmtpPort());
        entity.setEncryptionType(createDto.getEncryptionType());
        entity.setEmailAddress(createDto.getEmailAddress());
        entity.setAuthMethod(createDto.getAuthMethod());
        entity.setUsername(createDto.getUsername());
        entity.setPassword(createDto.getPassword());
        entity.setApiKey(createDto.getApiKey());
        
        // Set updated timestamp
        entity.setUpdatedAt(System.currentTimeMillis());
    }
    
    /**
     * Updates an EmailConfiguration entity from an update DTO.
     * 
     * @param updateDto the update DTO
     * @param entity the entity to update
     */
    private void updateEntityFromDto(EmailConfigurationUpdateDto updateDto, EmailConfiguration entity) {
        if (updateDto.getSmtpHostname() != null) {
            entity.setSmtpHostname(updateDto.getSmtpHostname());
        }
        if (updateDto.getSmtpPort() != null) {
            entity.setSmtpPort(updateDto.getSmtpPort());
        }
        if (updateDto.getEncryptionType() != null) {
            entity.setEncryptionType(updateDto.getEncryptionType());
        }
        if (updateDto.getEmailAddress() != null) {
            entity.setEmailAddress(updateDto.getEmailAddress());
        }
        if (updateDto.getAuthMethod() != null) {
            entity.setAuthMethod(updateDto.getAuthMethod());
        }
        if (updateDto.getUsername() != null) {
            entity.setUsername(updateDto.getUsername());
        }
        if (updateDto.getPassword() != null) {
            entity.setPassword(updateDto.getPassword());
        }
        if (updateDto.getApiKey() != null) {
            entity.setApiKey(updateDto.getApiKey());
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
     * Creates a response DTO from an EmailConfiguration entity.
     * 
     * @param entity the entity
     * @return the response DTO
     */
    private EmailConfigurationResponseDto createResponseDto(EmailConfiguration entity) {
        EmailConfigurationResponseDto responseDto = new EmailConfigurationResponseDto();
        responseDto.setId(entity.getId());
        responseDto.setTenantId(entity.getTenantId());
        responseDto.setSmtpHostname(entity.getSmtpHostname());
        responseDto.setSmtpPort(entity.getSmtpPort());
        responseDto.setEncryptionType(entity.getEncryptionType());
        responseDto.setEmailAddress(entity.getEmailAddress());
        responseDto.setAuthMethod(entity.getAuthMethod());
        responseDto.setUsername(entity.getUsername());
        responseDto.setPassword(entity.getPassword());
        responseDto.setApiKey(entity.getApiKey());
        responseDto.setCreatedAt(entity.getCreatedAt());
        responseDto.setUpdatedAt(entity.getUpdatedAt());
        responseDto.setCreatedById(entity.getCreatedById());
        responseDto.setCreatedByName(entity.getCreatedByName());
        return responseDto;
    }
    
    /**
     * Retrieves an email configuration by its unique identifier.
     * 
     * @param id the unique identifier of the configuration
     * @return EmailConfigurationResponseDto containing the configuration
     */
    @Override
    public EmailConfigurationResponseDto getEmailConfigurationById(UUID id) {
        logger.trace().message(String.format(AppConstants.LOG_ENTERING_METHOD, "getEmailConfigurationById"))
                .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                .log();
        
        try {
            EmailConfiguration config = findConfigurationById(id);
            EmailConfigurationResponseDto response = createResponseDto(config);
            
            logger.info().message("Email configuration retrieved successfully by ID")
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                    .detail(AppConstants.DETAIL_TENANT_ID, config.getTenantId())
                    .log();
            
            return response;
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.LOG_METHOD_ERROR, "getEmailConfigurationById", e.getMessage()))
                    .detail(AppConstants.DETAIL_EMAIL_CONFIG_ID, id)
                    .log();
            throw e;
        }
    }
    
    /**
     * Finds an email configuration by ID with standardized error handling.
     * 
     * @param id the configuration ID
     * @return the email configuration entity
     */
    private EmailConfiguration findConfigurationById(UUID id) {
        return emailConfigurationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(String.format(AppConstants.MSG_EMAIL_CONFIG_NOT_FOUND_WITH_ID, id)));
    }
}
