package com.aidentx.core.notifications_management.service.impl;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.enums.NotificationType;
import com.aidentx.core.notifications_management.model.dto.NotificationEventData;
import com.aidentx.core.notifications_management.model.dto.NotificationSseDto;
import com.aidentx.core.notifications_management.model.entity.EmailConfiguration;
import com.aidentx.core.notifications_management.model.entity.Notification;
import com.aidentx.core.notifications_management.model.entity.TemplateManager;
import com.aidentx.core.notifications_management.model.entity.UserNotification;
import com.aidentx.core.notifications_management.repository.EmailConfigurationRepository;
import com.aidentx.core.notifications_management.repository.NotificationRepository;
import com.aidentx.core.notifications_management.repository.TemplateManagerRepository;
import com.aidentx.core.notifications_management.repository.UserNotificationRepository;
import com.aidentx.core.notifications_management.service.NotificationProcessingService;
import com.aidentx.core.notifications_management.service.NotificationSseService;
import com.aidentx.core.notifications_management.service.VelocityTemplateService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Implementation of NotificationProcessingService for handling Kafka notification events.
 * <p>
 * This service processes notification events received from Kafka with full multi-tenant support.
 * Each event contains a tenant ID that ensures notifications are processed using the correct
 * tenant's email templates and configurations, providing complete tenant isolation.
 * </p>
 * 
 * @author Aidentx Development Team
 * @version 1.0
 * @since 2025-08-21
 */
@Service
@Transactional
public class NotificationProcessingServiceImpl implements NotificationProcessingService {
    
    private final NotificationRepository notificationRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final TemplateManagerRepository templateManagerRepository;
    private final EmailConfigurationRepository emailConfigurationRepository;
    private final VelocityTemplateService velocityTemplateService;
    private final NotificationSseService notificationSseService;
    private final IUnifiedLogger logger;
    
    /**
     * Constructor for NotificationProcessingServiceImpl.
     * 
     * @param notificationRepository repository for notification entities
     * @param userNotificationRepository repository for user-notification relationships
     * @param templateManagerRepository repository for email templates
     * @param emailConfigurationRepository repository for email configurations
     * @param velocityTemplateService service for processing Velocity templates
     * @param notificationSseService service for SSE real-time notifications
     * @param loggerFactory factory for creating unified loggers
     */
    @Autowired
    public NotificationProcessingServiceImpl(
            NotificationRepository notificationRepository,
            UserNotificationRepository userNotificationRepository,
            TemplateManagerRepository templateManagerRepository,
            EmailConfigurationRepository emailConfigurationRepository,
            VelocityTemplateService velocityTemplateService,
            NotificationSseService notificationSseService,
            UnifiedLoggerFactory loggerFactory) {
        this.notificationRepository = notificationRepository;
        this.userNotificationRepository = userNotificationRepository;
        this.templateManagerRepository = templateManagerRepository;
        this.emailConfigurationRepository = emailConfigurationRepository;
        this.velocityTemplateService = velocityTemplateService;
        this.notificationSseService = notificationSseService;
        this.logger = loggerFactory.getLogger(AppConstants.NOTIFICATION_PROCESSING_SERVICE_LOGGER_NAME, AppConstants.LOG_CATEGORY_NOTIFICATION_PROCESSING);
    }
    
    /**
     * Processes a notification event received from Kafka with tenant-aware processing.
     * <p>
     * This method extracts the tenant ID from the event data and ensures all operations
     * (template lookup, email configuration, etc.) are scoped to the correct tenant.
     * </p>
     * 
     * @param eventData the notification event data containing tenant ID, operation type and user data
     * @throws IllegalArgumentException if tenant ID is missing or operation type is invalid
     * @throws RuntimeException if template or email configuration is not found for the tenant
     */
    @Override
    public void processNotificationEvent(NotificationEventData eventData) {
        // Extract and validate tenant ID from the event data
        String tenantId = validateAndExtractTenant(eventData);
        
        logger.info()
            .message(AppConstants.LOG_NOTIFICATION_EVENT_RECEIVED)
            .detail("operationType", eventData.getOperationType().name())
            .detail("userCount", eventData.getData().size())
            .detail("tenantId", tenantId)
            .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
            .log();
        
        try {
            // Convert and validate the Kafka enum to core service enum
            com.aidentx.core.notifications_management.enums.OperationType coreOperationType = 
                com.aidentx.core.notifications_management.enums.OperationType.valueOf(eventData.getOperationType().name());
            
            // Find tenant-specific template for this operation
            logger.info()
                .message(AppConstants.LOG_DEBUG_LOOKING_FOR_TEMPLATE)
                .detail("tenantId", tenantId)
                .detail("operationType", coreOperationType.name())
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                .log();
            
            List<TemplateManager> templates = templateManagerRepository.findByTenantIdAndTriggerTypeAndIsEnabled(
                tenantId, coreOperationType, true);
            
            logger.info()
                .message(AppConstants.LOG_DEBUG_TEMPLATE_SEARCH_RESULT)
                .detail("tenantId", tenantId)
                .detail("operationType", coreOperationType.name())
                .detail("templatesFound", templates.size())
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                .log();
                
            TemplateManager template = null;
            if (templates.isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_DEBUG_NO_TEMPLATE_FOUND)
                    .detail("tenantId", tenantId)
                    .detail("operationType", coreOperationType.name())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                    .log();
            } else {
                template = templates.get(0); // Use first enabled template
            }
            
            // Get tenant-specific email configuration
            logger.info()
                .message(AppConstants.LOG_DEBUG_LOOKING_FOR_EMAIL_CONFIG)
                .detail("tenantId", tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                .log();
                
            Optional<EmailConfiguration> emailConfigOpt = emailConfigurationRepository.findByTenantId(tenantId);
            
            logger.info()
                .message(AppConstants.LOG_DEBUG_EMAIL_CONFIG_SEARCH_RESULT)
                .detail("tenantId", tenantId)
                .detail("configFound", emailConfigOpt.isPresent())
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                .log();
                
            EmailConfiguration emailConfig = null;
            if (emailConfigOpt.isEmpty()) {
                logger.warn()
                    .message(AppConstants.LOG_DEBUG_NO_EMAIL_CONFIG_FOUND)
                    .detail("tenantId", tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                    .log();
            } else {
                emailConfig = emailConfigOpt.get();
            }
            
            // Process each user in the event data
            int successCount = 0;
            int failureCount = 0;
            
            for (NotificationEventData.UserNotificationData userData : eventData.getData()) {
                try {
                    processUserNotification(userData, template, emailConfig, coreOperationType, tenantId);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    logger.error(e)
                        .message(AppConstants.LOG_USER_NOTIFICATION_FAILED)
                        .detail("userId", userData.getUserId())
                        .detail("username", userData.getUserData().getUsername())
                        .detail("operationType", coreOperationType.name())
                        .detail("tenantId", tenantId)
                        .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                        .log();
                }
            }
            
            logger.info()
                .message(AppConstants.LOG_NOTIFICATION_EVENT_SUCCESS)
                .detail("operationType", coreOperationType.name())
                .detail("totalUsers", eventData.getData().size())
                .detail("successCount", successCount)
                .detail("failureCount", failureCount)
                .detail("tenantId", tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_NOTIFICATION_EVENT_FAILED)
                .detail("operationType", eventData.getOperationType().name())
                .detail("tenantId", tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                .log();
            throw e;
        }
    }
    
    /**
     * Validates and extracts tenant ID from the notification event data.
     * 
     * @param eventData the notification event data
     * @return the validated tenant ID
     * @throws IllegalArgumentException if tenant ID is null or empty
     */
    private String validateAndExtractTenant(NotificationEventData eventData) {
        String tenantId = eventData.getTenantId();
        if (tenantId == null || tenantId.trim().isEmpty()) {
            throw new IllegalArgumentException("Tenant ID is required in notification event data");
        }
        return tenantId.trim();
    }
    
        /**
     * Processes notification for a single user with tenant context.
     * <p>
     * This method handles:
     * <ul>
     *   <li>Creating template variables for Velocity processing</li>
     *   <li>Processing email subject and body templates</li>
     *   <li>Sending email to the user</li>
     *   <li>Creating notification record</li>
     *   <li>Creating user-notification relationship</li>
     * </ul>
     * </p>
     * 
     * @param userData the user data containing email, username, and template variables
     * @param template the tenant-specific email template to use
     * @param emailConfig the tenant-specific email configuration for SMTP
     * @param operationType the type of operation triggering the notification
     * @param tenantId the tenant ID for logging and context
     * @throws MessagingException if email sending fails
     * @throws RuntimeException if notification processing fails
     */
    private void processUserNotification(NotificationEventData.UserNotificationData userData,
                                       TemplateManager template,
                                       EmailConfiguration emailConfig,
                                       com.aidentx.core.notifications_management.enums.OperationType operationType,
                                       String tenantId) {
        try {
            String processedSubject = null;
            String processedBody = null;
            boolean emailSent = false;
            
            // Process template and send email only if both template and config are available
            if (template != null && emailConfig != null) {
                // Create template variables map for Velocity
                Map<String, Object> templateVariables = createTemplateVariables(userData);
                
                // Process template subject and body with Velocity
                processedSubject = velocityTemplateService.processTemplate(
                    template.getTemplateSubject(), templateVariables);
                processedBody = velocityTemplateService.processTemplate(
                    template.getEmailBody(), templateVariables);
                
                // Send email
                sendEmail(emailConfig, userData.getUserData().getEmail(), processedSubject, processedBody, tenantId);
                emailSent = true;
                
                logger.info()
                    .message(AppConstants.LOG_EMAIL_SENT_SUCCESSFULLY)
                    .detail("userId", userData.getUserId())
                    .detail("email", userData.getUserData().getEmail())
                    .detail("tenantId", tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                    .log();
            } else {
                // Use default subject and body when template is not available
                processedSubject = getDefaultSubject(operationType);
                processedBody = getDefaultBody(operationType, userData);
                
                logger.warn()
                    .message(AppConstants.LOG_EMAIL_NOT_SENT_MISSING_CONFIG)
                    .detail("userId", userData.getUserId())
                    .detail("hasTemplate", template != null)
                    .detail("hasEmailConfig", emailConfig != null)
                    .detail("tenantId", tenantId)
                    .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                    .log();
            }
            
            // Always create notification record regardless of email sending
            Notification notification = createNotification(processedBody, processedSubject, operationType, userData);
            Notification savedNotification = notificationRepository.save(notification);
            
            // Create user-notification relationship
            UserNotification userNotification = new UserNotification(
                UUID.fromString(userData.getUserId()),
                savedNotification.getId()
            );
            userNotificationRepository.save(userNotification);
            
            // Send real-time notification via SSE
            try {
                sendRealTimeNotification(userData.getUserId(), savedNotification, userNotification, userData.getActions());
            } catch (Exception sseException) {
                // Don't fail the whole process if SSE fails
                logger.warn().message(AppConstants.LOG_REAL_TIME_NOTIFICATION_SSE_FAILED)
                        .detail("userId", userData.getUserId())
                        .detail("notificationId", savedNotification.getId())
                        .detail("tenantId", tenantId)
                        .detail("error", sseException.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                        .log();
            }
            
            logger.info()
                .message(AppConstants.LOG_USER_NOTIFICATION_SUCCESS)
                .detail("userId", userData.getUserId())
                .detail("username", userData.getUserData().getUsername())
                .detail("email", userData.getUserData().getEmail())
                .detail("notificationId", savedNotification.getId())
                .detail("operationType", operationType.name())
                .detail("emailSent", emailSent)
                .detail("tenantId", tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                .log();
                
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_USER_NOTIFICATION_FAILED)
                .detail("userId", userData.getUserId())
                .detail("username", userData.getUserData().getUsername())
                .detail("operationType", operationType.name())
                .detail("tenantId", tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                .log();
            throw new RuntimeException(AppConstants.LOG_NOTIFICATION_CREATION_FAILED + ": " + userData.getUserId(), e);
        }
    }
    
    /**
     * Sends an email using the tenant-specific SMTP configuration.
     * <p>
     * Creates SMTP session with authentication and sends HTML email.
     * Logs success or failure of email delivery with tenant context.
     * </p>
     * 
     * @param emailConfig the tenant-specific email configuration containing SMTP settings
     * @param recipientEmail the recipient's email address
     * @param subject the email subject
     * @param body the email body (HTML format)
     * @param tenantId the tenant ID for logging context
     * @throws MessagingException if email creation or sending fails
     * @throws RuntimeException if email sending fails
     */
    private void sendEmail(EmailConfiguration emailConfig, String recipientEmail, String subject, String body, String tenantId) {
        try {
            // Create SMTP properties
            Properties props = createSmtpProperties(emailConfig);
            
            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(emailConfig.getUsername(), emailConfig.getPassword());
                }
            });
            
            // Create message
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(emailConfig.getEmailAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
            message.setSubject(subject);
            message.setText(body, "utf-8", "html");
            
            // Send email
            Transport.send(message);
            
            logger.info()
                .message(AppConstants.LOG_EMAIL_SENT_SUCCESS)
                .detail("recipient", recipientEmail)
                .detail("subject", subject)
                .detail("smtpHost", emailConfig.getSmtpHostname())
                .detail("smtpPort", emailConfig.getSmtpPort())
                .detail("tenantId", tenantId)
                .objectType(AppConstants.LOG_OBJECT_TYPE_EMAIL)
                .log();
                
        } catch (MessagingException e) {
            logger.error(e)
                .message(AppConstants.LOG_EMAIL_SEND_FAILED)
                .detail("recipient", recipientEmail)
                .detail("subject", subject)
                .detail("smtpHost", emailConfig.getSmtpHostname())
                .detail("tenantId", tenantId)
                .detail("errorType", "MessagingException")
                .objectType(AppConstants.LOG_OBJECT_TYPE_EMAIL)
                .log();
            throw new RuntimeException(String.format(AppConstants.ERROR_EMAIL_SEND_FAILED, recipientEmail), e);
        } catch (Exception e) {
            logger.error(e)
                .message(AppConstants.LOG_EMAIL_SEND_FAILED)
                .detail("recipient", recipientEmail)
                .detail("subject", subject)
                .detail("tenantId", tenantId)
                .detail("errorType", e.getClass().getSimpleName())
                .objectType(AppConstants.LOG_OBJECT_TYPE_EMAIL)
                .log();
            throw new RuntimeException(String.format(AppConstants.ERROR_EMAIL_SEND_FAILED, recipientEmail), e);
        }
    }
    
    /**
     * Creates template variables map from user notification data.
     * <p>
     * Extracts variables from the user data for Velocity template processing.
     * Currently supports username, email, expirationDays, and roleName variables.
     * </p>
     * 
     * @param userData the user notification data containing template variables
     * @return Map containing template variables for Velocity processing
     */
    private Map<String, Object> createTemplateVariables(NotificationEventData.UserNotificationData userData) {
        Map<String, Object> templateVariables = new HashMap<>();
        templateVariables.put("username", userData.getTemplateVariables().getUsername());
        templateVariables.put("email", userData.getTemplateVariables().getEmail());
        
        // Only add expirationDays if it's available (for PASSWORD_REMINDER_RESET operations)
        if (userData.getTemplateVariables().getExpirationDays() != null) {
            templateVariables.put("expirationDays", userData.getTemplateVariables().getExpirationDays());
        }
        
        // Only add roleName if it's available (for ASSIGN_INTERNAL_ROLE operations)
        if (userData.getTemplateVariables().getRoleName() != null) {
            templateVariables.put("roleName", userData.getTemplateVariables().getRoleName());
        }
        
        return templateVariables;
    }
    
    /**
     * Creates a notification entity for database storage.
     * <p>
     * Creates clean notification message for app display based on operation type
     * rather than using HTML email body content. This ensures app notifications
     * show clean, user-friendly messages while HTML emails continue to use styled templates.
     * Uses configured system user for audit tracking.
     * Determines notification type based on operation type - password expiration notifications
     * are classified as ALERTS while other operations are NOTIFICATIONS.
     * </p>
     * 
     * @param processedBody the processed email body content (used for extracting variables)
     * @param processedSubject the processed email subject
     * @param operationType the operation type triggering the notification
     * @param userData the user notification data containing template variables
     * @return Notification entity ready for persistence
     */
    private Notification createNotification(String processedBody, String processedSubject, 
                                          com.aidentx.core.notifications_management.enums.OperationType operationType,
                                          NotificationEventData.UserNotificationData userData) {
        // Determine notification type based on operation
        NotificationType notificationType = (operationType == com.aidentx.core.notifications_management.enums.OperationType.PASSWORD_REMINDER_RESET) 
            ? NotificationType.ALERTS 
            : NotificationType.NOTIFICATIONS;
        
        // Generate clean notification message based on operation type
        String cleanNotificationMessage = generateCleanNotificationMessage(operationType, userData);
        
        // Generate description based on operation type and username
        String notificationDescription = generateNotificationDescription(operationType, userData, processedSubject);
        
        // Debug logging to see what we're actually passing
        logger.debug()
            .message(AppConstants.LOG_CREATING_NOTIFICATION_WITH_CLEAN_MESSAGE)
            .detail("cleanNotificationMessage", cleanNotificationMessage)
            .detail("notificationDescription", notificationDescription)
            .detail("operationType", operationType.name())
            .log();
                
        return new Notification(
            cleanNotificationMessage,      // details - clean message for app display
            notificationDescription,       // description - notification title with username
            notificationType,
            operationType,
            AppConstants.SYSTEM_USER_ID,  // Using system user for audit
            AppConstants.SYSTEM_USER_NAME // Using system user name for audit
        );
    }
    
    /**
     * Creates clean notification message for app display based on operation type.
     * <p>
     * For ASSIGN_INTERNAL_ROLE notifications, the clean message is generated using template variables
     * instead of parsing the email body. The role name comes from the templateVariables.
     * </p>
     * 
     * @param operationType the operation type triggering the notification
     * @param templateVariables the template variables containing role name
     * @return clean notification message string
     */
    private String generateCleanNotificationMessage(com.aidentx.core.notifications_management.enums.OperationType operationType, 
                                                   NotificationEventData.UserNotificationData userData) {
        switch (operationType) {
            case PASSWORD_RESET:
                return AppConstants.NOTIFICATION_MSG_PASSWORD_RESET;
                
            case PASSWORD_REMINDER_RESET:
                // ALWAYS use expiration days from template variables (no fallback)
                logger.info()
                    .message(AppConstants.LOG_PROCESSING_PASSWORD_REMINDER_RESET)
                    .detail("hasTemplateVariables", userData.getTemplateVariables() != null)
                    .detail("expirationDaysValue", userData.getTemplateVariables() != null ? userData.getTemplateVariables().getExpirationDays() : "null")
                    .log();
                    
                // Use template variables to create dynamic message
                if (userData.getTemplateVariables() != null && 
                    userData.getTemplateVariables().getExpirationDays() != null && 
                    !userData.getTemplateVariables().getExpirationDays().trim().isEmpty()) {
                    try {
                        int expirationDays = Integer.parseInt(userData.getTemplateVariables().getExpirationDays());
                        String message = String.format(AppConstants.NOTIFICATION_MSG_PASSWORD_EXPIRY_DYNAMIC, expirationDays);
                        logger.info()
                            .message(AppConstants.LOG_SUCCESSFULLY_CREATED_DYNAMIC_PASSWORD_MESSAGE)
                            .detail("expirationDays", expirationDays)
                            .detail("message", message)
                            .log();
                        return message;
                    } catch (NumberFormatException e) {
                        logger.error().message(AppConstants.LOG_FAILED_PARSE_EXPIRATION_DAYS_GENERATE_CLEAN)
                                .detail("expirationDaysValue", userData.getTemplateVariables().getExpirationDays())
                                .detail("error", e.getMessage())
                                .log();
                        return AppConstants.NOTIFICATION_MSG_PASSWORD_EXPIRY_FALLBACK;
                    }
                } else {
                    logger.error().message(AppConstants.LOG_TEMPLATE_VARIABLES_MISSING_GENERATE_CLEAN)
                            .detail("hasTemplateVariables", userData.getTemplateVariables() != null)
                            .detail("expirationDaysValue", userData.getTemplateVariables() != null ? userData.getTemplateVariables().getExpirationDays() : "null")
                            .log();
                    return AppConstants.NOTIFICATION_MSG_PASSWORD_EXPIRY_FALLBACK;
                }
                
            case ASSIGN_INTERNAL_ROLE:
                // Get role name from template variables
                String roleName = userData.getTemplateVariables().getRoleName();
                if (roleName != null && !roleName.trim().isEmpty()) {
                    return String.format(AppConstants.NOTIFICATION_MSG_ROLE_ASSIGNED, roleName);
                } else {
                    return String.format(AppConstants.NOTIFICATION_MSG_ROLE_ASSIGNED, "a new role");
                }
                
            case REVOKE_INTERNAL_ROLE:
                // Get role name from template variables for revocation
                String revokedRoleName = userData.getTemplateVariables().getRoleName();
                if (revokedRoleName != null && !revokedRoleName.trim().isEmpty()) {
                    return String.format(AppConstants.NOTIFICATION_MSG_ROLE_REVOKED, revokedRoleName);
                } else {
                    return String.format(AppConstants.NOTIFICATION_MSG_ROLE_REVOKED, "a role");
                }
                
            case WORKFLOW_APPROVAL_REQUESTED:
                // Store approval details with enterprise context as JSON in the notification details
                String workflowName = userData.getTemplateVariables() != null && userData.getTemplateVariables().getWorkflowName() != null
                    ? userData.getTemplateVariables().getWorkflowName()
                    : "Workflow";
                String requestedBy = userData.getTemplateVariables() != null && userData.getTemplateVariables().getRequestedBy() != null
                    ? userData.getTemplateVariables().getRequestedBy()
                    : "Unknown";
                String approvalId = userData.getTemplateVariables() != null && userData.getTemplateVariables().getApprovalId() != null
                    ? userData.getTemplateVariables().getApprovalId()
                    : null;
                
                // Build JSON with enterprise IAM context for meaningful approvals
                StringBuilder jsonBuilder = new StringBuilder();
                jsonBuilder.append("{\"type\":\"workflow_approval\"");
                jsonBuilder.append(",\"workflowName\":\"").append(escapeJson(workflowName)).append("\"");
                jsonBuilder.append(",\"requestedBy\":\"").append(escapeJson(requestedBy)).append("\"");
                jsonBuilder.append(",\"approvalId\":\"").append(escapeJson(approvalId)).append("\"");
                
                // Add enterprise context fields if available
                var templateVars = userData.getTemplateVariables();
                if (templateVars != null) {
                    if (templateVars.getRequesterFullName() != null) {
                        jsonBuilder.append(",\"requesterFullName\":\"").append(escapeJson(templateVars.getRequesterFullName())).append("\"");
                    }
                    if (templateVars.getRequesterDepartment() != null) {
                        jsonBuilder.append(",\"requesterDepartment\":\"").append(escapeJson(templateVars.getRequesterDepartment())).append("\"");
                    }
                    if (templateVars.getTileName() != null) {
                        jsonBuilder.append(",\"tileName\":\"").append(escapeJson(templateVars.getTileName())).append("\"");
                    }
                    if (templateVars.getRoleName() != null) {
                        jsonBuilder.append(",\"roleName\":\"").append(escapeJson(templateVars.getRoleName())).append("\"");
                    }
                    if (templateVars.getRoleDescription() != null) {
                        jsonBuilder.append(",\"roleDescription\":\"").append(escapeJson(templateVars.getRoleDescription())).append("\"");
                    }
                    if (templateVars.getRiskLevel() != null) {
                        jsonBuilder.append(",\"riskLevel\":\"").append(escapeJson(templateVars.getRiskLevel())).append("\"");
                    }
                    if (templateVars.getTargetUsers() != null) {
                        jsonBuilder.append(",\"targetUsers\":\"").append(escapeJson(templateVars.getTargetUsers())).append("\"");
                    }
                    if (templateVars.getTargetUserCount() > 0) {
                        jsonBuilder.append(",\"targetUserCount\":").append(templateVars.getTargetUserCount());
                    }
                    if (templateVars.getJustification() != null) {
                        jsonBuilder.append(",\"justification\":\"").append(escapeJson(templateVars.getJustification())).append("\"");
                    }
                    if (templateVars.getRequestedAt() != null) {
                        jsonBuilder.append(",\"requestedAt\":\"").append(escapeJson(templateVars.getRequestedAt())).append("\"");
                    }
                    if (templateVars.getDeadlineAt() != null) {
                        jsonBuilder.append(",\"deadlineAt\":\"").append(escapeJson(templateVars.getDeadlineAt())).append("\"");
                    }
                    if (templateVars.getApplicationName() != null) {
                        jsonBuilder.append(",\"applicationName\":\"").append(escapeJson(templateVars.getApplicationName())).append("\"");
                    }
                    if (templateVars.getRequestType() != null) {
                        jsonBuilder.append(",\"requestType\":\"").append(escapeJson(templateVars.getRequestType())).append("\"");
                    }
                }
                
                jsonBuilder.append(",\"message\":\"Approval request for ").append(escapeJson(workflowName)).append("\"}");
                return jsonBuilder.toString();

            // ==================== Phase 3: Delegation Notifications ====================
            case DELEGATION_CREATED:
                String delegatorName = userData.getTemplateVariables() != null && userData.getTemplateVariables().getDelegatorName() != null
                    ? userData.getTemplateVariables().getDelegatorName()
                    : "A user";
                String startDate = userData.getTemplateVariables() != null && userData.getTemplateVariables().getStartDate() != null
                    ? userData.getTemplateVariables().getStartDate()
                    : "soon";
                String endDate = userData.getTemplateVariables() != null && userData.getTemplateVariables().getEndDate() != null
                    ? userData.getTemplateVariables().getEndDate()
                    : "later";
                return String.format("{\"type\":\"delegation_created\",\"delegatorName\":\"%s\",\"startDate\":\"%s\",\"endDate\":\"%s\",\"message\":\"%s has delegated their approvals to you from %s to %s\"}",
                    delegatorName, startDate, endDate, delegatorName, startDate, endDate);

            case APPROVAL_AUTO_DELEGATED:
                String origApprover = userData.getTemplateVariables() != null && userData.getTemplateVariables().getDelegatorName() != null
                    ? userData.getTemplateVariables().getDelegatorName()
                    : "Someone";
                String delegatedWorkflowName = userData.getTemplateVariables() != null && userData.getTemplateVariables().getWorkflowName() != null
                    ? userData.getTemplateVariables().getWorkflowName()
                    : "Workflow";
                String delegatedApprovalId = userData.getTemplateVariables() != null && userData.getTemplateVariables().getApprovalId() != null
                    ? userData.getTemplateVariables().getApprovalId()
                    : null;
                return String.format("{\"type\":\"approval_auto_delegated\",\"originalApprover\":\"%s\",\"workflowName\":\"%s\",\"approvalId\":\"%s\",\"message\":\"Approval delegated from %s for %s\"}",
                    origApprover, delegatedWorkflowName, delegatedApprovalId, origApprover, delegatedWorkflowName);

            // ==================== Phase 4: Escalation Notifications ====================
            case APPROVAL_ESCALATED:
                String escalatedOrigApprover = userData.getTemplateVariables() != null && userData.getTemplateVariables().getDelegatorName() != null
                    ? userData.getTemplateVariables().getDelegatorName()
                    : "Previous approver";
                String escalatedWorkflowName = userData.getTemplateVariables() != null && userData.getTemplateVariables().getWorkflowName() != null
                    ? userData.getTemplateVariables().getWorkflowName()
                    : "Workflow";
                String escalatedApprovalId = userData.getTemplateVariables() != null && userData.getTemplateVariables().getApprovalId() != null
                    ? userData.getTemplateVariables().getApprovalId()
                    : null;
                return String.format("{\"type\":\"approval_escalated\",\"originalApprover\":\"%s\",\"workflowName\":\"%s\",\"approvalId\":\"%s\",\"message\":\"Approval escalated from %s for %s\"}",
                    escalatedOrigApprover, escalatedWorkflowName, escalatedApprovalId, escalatedOrigApprover, escalatedWorkflowName);

            case APPROVAL_ESCALATED_FROM_YOU:
                String escalatedToName = userData.getTemplateVariables() != null && userData.getTemplateVariables().getDelegatorName() != null
                    ? userData.getTemplateVariables().getDelegatorName()
                    : "another approver";
                String yourEscalatedWorkflowName = userData.getTemplateVariables() != null && userData.getTemplateVariables().getWorkflowName() != null
                    ? userData.getTemplateVariables().getWorkflowName()
                    : "Workflow";
                return String.format("{\"type\":\"approval_escalated_from_you\",\"escalatedTo\":\"%s\",\"workflowName\":\"%s\",\"message\":\"Your approval for %s has been escalated to %s\"}",
                    escalatedToName, yourEscalatedWorkflowName, yourEscalatedWorkflowName, escalatedToName);
                
            default:
                // Fallback to using a generic message for unknown operation types
                return AppConstants.LOG_FALLBACK_NOTIFICATION_MESSAGE;
        }
    }
    
    /**
     * Generates notification description based on operation type and username.
     * <p>
     * Creates operation-specific description that includes the username from the Kafka event.
     * This description is used as the notification title in the app.
     * </p>
     * 
     * @param operationType the operation type triggering the notification
     * @param userData the user notification data containing username
     * @param processedSubject the processed email subject (used as fallback)
     * @return notification description string
     */
    private String generateNotificationDescription(com.aidentx.core.notifications_management.enums.OperationType operationType,
                                                   NotificationEventData.UserNotificationData userData,
                                                   String processedSubject) {
        String username = userData.getUserData().getUsername();
        
        switch (operationType) {
            case ASSIGN_INTERNAL_ROLE:
                return String.format(AppConstants.INTERNAL_ROLE_ASSIGNMENT_MSG, username);
                
            case REVOKE_INTERNAL_ROLE:
                return String.format(AppConstants.INTERNAL_ROLE_REVOCATION_MSG, username);
                
            case PASSWORD_RESET:
                return String.format(AppConstants.PASSWORD_RESET_MSG, username);

            case PASSWORD_REMINDER_RESET:
                return String.format(AppConstants.PASSWORD_REMINDER_RESET_MSG, username);
            
            case WORKFLOW_APPROVAL_REQUESTED:
                String workflowNameDesc = userData.getTemplateVariables() != null && userData.getTemplateVariables().getWorkflowName() != null
                    ? userData.getTemplateVariables().getWorkflowName()
                    : "Workflow";
                return String.format("Approval Required: %s", workflowNameDesc);

            // ==================== Phase 3: Delegation Notifications ====================
            case DELEGATION_CREATED:
                String delegatorDesc = userData.getTemplateVariables() != null && userData.getTemplateVariables().getDelegatorName() != null
                    ? userData.getTemplateVariables().getDelegatorName()
                    : "A user";
                return String.format("Delegation Setup: %s has delegated their approvals to you", delegatorDesc);

            case APPROVAL_AUTO_DELEGATED:
                String origApproverDesc = userData.getTemplateVariables() != null && userData.getTemplateVariables().getDelegatorName() != null
                    ? userData.getTemplateVariables().getDelegatorName()
                    : "Someone";
                String delegatedWorkflowDesc = userData.getTemplateVariables() != null && userData.getTemplateVariables().getWorkflowName() != null
                    ? userData.getTemplateVariables().getWorkflowName()
                    : "Workflow";
                return String.format("Delegated Approval: %s (from %s)", delegatedWorkflowDesc, origApproverDesc);

            // ==================== Phase 4: Escalation Notifications ====================
            case APPROVAL_ESCALATED:
                String escalatedWorkflowDesc = userData.getTemplateVariables() != null && userData.getTemplateVariables().getWorkflowName() != null
                    ? userData.getTemplateVariables().getWorkflowName()
                    : "Workflow";
                return String.format("Escalated Approval: %s", escalatedWorkflowDesc);

            case APPROVAL_ESCALATED_FROM_YOU:
                String yourEscalatedWorkflowDesc = userData.getTemplateVariables() != null && userData.getTemplateVariables().getWorkflowName() != null
                    ? userData.getTemplateVariables().getWorkflowName()
                    : "Workflow";
                return String.format("Your approval was escalated: %s", yourEscalatedWorkflowDesc);

            default:
                // Fallback to processed subject if available, otherwise generic message
                return processedSubject != null ? processedSubject : String.format(AppConstants.GENERIC_NOTIFICATION_MSG, username);
        }
    }
    
    /**
     * Extracts expiration days from the processed email body for PASSWORD_REMINDER_RESET notifications.
     * <p>
     * Attempts to find the expirationDays variable value from the email template.
     * Falls back to a default value if extraction fails.
     * </p>
     * 
     * @param processedBody the processed email body containing variable substitutions
     * @return the number of expiration days, defaults to 7 if not found
     */
    private int extractExpirationDaysFromBody(String processedBody) {
        try {
            // Look for common patterns in the email body that might contain expiration days
            // Pattern 1: "expire in X days" or "expires in X days"
            java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("expire[s]? in (\\d+) day[s]?", 
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher1 = pattern1.matcher(processedBody);
            if (matcher1.find()) {
                return Integer.parseInt(matcher1.group(1));
            }
            
            // Pattern 2: "X days remaining" or "X days left"
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("(\\d+) days? (?:remaining|left)", 
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher2 = pattern2.matcher(processedBody);
            if (matcher2.find()) {
                return Integer.parseInt(matcher2.group(1));
            }
            
            // Pattern 3: Look for any number followed by "days" as fallback
            java.util.regex.Pattern pattern3 = java.util.regex.Pattern.compile("(\\d+)\\s*days?", 
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher3 = pattern3.matcher(processedBody);
            if (matcher3.find()) {
                return Integer.parseInt(matcher3.group(1));
            }
            
        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_EXPIRATION_DAYS_EXTRACTION_FAILED)
                    .detail("processedBody", processedBody)
                    .detail("error", e.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                    .log();
        }
        
        // No hardcoded fallback - if extraction fails, return -1 to indicate failure
        logger.warn().message(AppConstants.LOG_FAILED_EXTRACT_EXPIRATION_DAYS_FROM_BODY)
                .detail("processedBody", processedBody)
                .log();
        return -1;
    }
    
    /**
     * Extracts role name from the processed email body for ASSIGN_INTERNAL_ROLE notifications.
     * <p>
     * For role assignment notifications, we primarily get the role name from template variables
     * rather than parsing the email body. This method is used as a fallback when template 
     * variables are not available.
     * </p>
     * 
     * @param processedBody the processed email body containing variable substitutions
     * @return the role name, defaults to "a new role" if not found
     */
    private String extractRoleNameFromBody(String processedBody) {
        try {
            // Look for common patterns in the email body that might contain role name
            // Pattern 1: "role: RoleName" or "role RoleName"
            java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("role:?\\s+([\\w\\s-_]+)", 
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher1 = pattern1.matcher(processedBody);
            if (matcher1.find()) {
                return matcher1.group(1).trim();
            }
            
            // Pattern 2: "assigned to RoleName" or "promoted to RoleName"
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile("(?:assigned to|promoted to)\\s+([\\w\\s-_]+)", 
                java.util.regex.Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher matcher2 = pattern2.matcher(processedBody);
            if (matcher2.find()) {
                return matcher2.group(1).trim();
            }
            
            // Pattern 3: Look for content between quotes that might be a role name
            java.util.regex.Pattern pattern3 = java.util.regex.Pattern.compile("\"([\\w\\s-_]+)\"");
            java.util.regex.Matcher matcher3 = pattern3.matcher(processedBody);
            if (matcher3.find()) {
                return matcher3.group(1).trim();
            }
            
        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_FAILED_EXTRACT_ROLE_NAME_FROM_BODY)
                    .detail("processedBody", processedBody)
                    .detail("error", e.getMessage())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                    .log();
        }
        
        // Default role name if extraction fails
        return "a new role";
    }
    
    /**
     * Creates SMTP properties for email session configuration.
     * <p>
     * Configures SMTP properties including authentication, TLS/SSL, and connection settings
     * based on the provided email configuration. Handles different encryption types
     * including STARTTLS, SSL/TLS, and unencrypted connections.
     * </p>
     * 
     * @param emailConfig the email configuration containing SMTP settings
     * @return Properties object configured for SMTP session
     */
    private Properties createSmtpProperties(EmailConfiguration emailConfig) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", emailConfig.getSmtpHostname());
        props.put("mail.smtp.port", emailConfig.getSmtpPort().toString());
        
        switch (emailConfig.getEncryptionType()) {
            case STARTTLS:
                props.put("mail.smtp.starttls.enable", "true");
                break;
            case SSL_TLS:
                props.put("mail.smtp.ssl.enable", "true");
                break;
            case NONE:
                // No additional encryption properties needed
                break;
        }
        
        return props;
    }
    
    /**
     * Sends a real-time notification to the user via SSE.
     * 
     * @param userId the user ID to send notification to
     * @param notification the notification entity
     * @param userNotification the user-notification relationship
     * @param actions optional list of action buttons for interactive notifications
     */
    private void sendRealTimeNotification(String userId, Notification notification, UserNotification userNotification, 
                                         List<NotificationEventData.NotificationAction> actions) {
        try {
            // Create SSE notification DTO with actions
            NotificationSseDto sseNotification = new NotificationSseDto(
                notification.getId().toString(),
                notification.getType(),
                notification.getDescription(),
                notification.getDetails(),
                notification.getOperation(),
                notification.getCreatedAt(),
                notification.getCreatedByName(),
                userNotification.getIsViewed(),
                userNotification.getId().toString(),
                actions  // Pass through action buttons for workflow approvals
            );
            
            // Send via SSE service
            notificationSseService.sendNotificationToUser(userId, sseNotification);
            
            logger.debug().message(AppConstants.LOG_REAL_TIME_NOTIFICATION_SENT)
                    .detail("userId", userId)
                    .detail("notificationId", notification.getId())
                    .detail("notificationType", notification.getType().name())
                    .detail("hasActions", actions != null && !actions.isEmpty())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                    .log();
                    
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_REAL_TIME_NOTIFICATION_FAILED)
                    .detail("userId", userId)
                    .detail("notificationId", notification.getId())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_NOTIFICATION_EVENT)
                    .log();
            throw e;
        }
    }
    
    /**
     * Generates a default subject when template is not available.
     * 
     * @param operationType the operation type
     * @return default subject string
     */
        private String getDefaultSubject(com.aidentx.core.notifications_management.enums.OperationType operationType) {
            switch (operationType) {
                case PASSWORD_REMINDER_RESET:
                    return AppConstants.DEFAULT_SUBJECT_PASSWORD_REMINDER;
                case PASSWORD_RESET:
                    return AppConstants.DEFAULT_SUBJECT_PASSWORD_RESET;
                case ACCESS_REQUEST_APPROVED:
                    return AppConstants.DEFAULT_SUBJECT_ACCESS_APPROVED;
                case ASSIGN_INTERNAL_ROLE:
                    return AppConstants.DEFAULT_SUBJECT_ROLE_ASSIGNED;
                case REVOKE_INTERNAL_ROLE:
                    return AppConstants.DEFAULT_SUBJECT_ROLE_REVOKED;
                default:
                    return AppConstants.DEFAULT_SUBJECT_SYSTEM;
            }
        }    /**
     * Generates a default body when template is not available.
     * 
     * @param operationType the operation type
     * @param userData the user data
     * @return default body string
     */
    private String getDefaultBody(com.aidentx.core.notifications_management.enums.OperationType operationType, 
                                  NotificationEventData.UserNotificationData userData) {
        String username = userData.getUserData().getUsername();
        switch (operationType) {
            case PASSWORD_REMINDER_RESET:
                // ALWAYS use expiration days from template variables (set by auth service from password policy)
                if (userData.getTemplateVariables() != null && 
                    userData.getTemplateVariables().getExpirationDays() != null && 
                    !userData.getTemplateVariables().getExpirationDays().trim().isEmpty()) {
                    try {
                        int expirationDays = Integer.parseInt(userData.getTemplateVariables().getExpirationDays());
                        return String.format(AppConstants.DEFAULT_EMAIL_PASSWORD_EXPIRY_DYNAMIC, username, expirationDays);
                    } catch (NumberFormatException e) {
                        logger.warn().message(AppConstants.LOG_FAILED_PARSE_EXPIRATION_DAYS_DEFAULT_BODY)
                                .detail("expirationDaysValue", userData.getTemplateVariables().getExpirationDays())
                                .detail("error", e.getMessage())
                                .log();
                    }
                }
                // If template variables are missing or invalid, this is an error condition
                logger.error().message(AppConstants.LOG_TEMPLATE_VARIABLES_MISSING_DEFAULT_BODY)
                        .detail("hasTemplateVariables", userData.getTemplateVariables() != null)
                        .detail("expirationDaysValue", userData.getTemplateVariables() != null ? userData.getTemplateVariables().getExpirationDays() : "null")
                        .log();
                return String.format(AppConstants.DEFAULT_EMAIL_PASSWORD_EXPIRY_FALLBACK, username);
            case PASSWORD_RESET:
                return String.format(AppConstants.DEFAULT_EMAIL_PASSWORD_RESET, username);
            case ACCESS_REQUEST_APPROVED:
                return String.format(AppConstants.DEFAULT_EMAIL_ACCESS_APPROVED, username);
            case ASSIGN_INTERNAL_ROLE:
                String roleName = userData.getTemplateVariables().getRoleName();
                if (roleName != null && !roleName.trim().isEmpty()) {
                    return String.format(AppConstants.DEFAULT_EMAIL_ROLE_ASSIGNED_WITH_NAME, username, roleName);
                } else {
                    return String.format(AppConstants.DEFAULT_EMAIL_ROLE_ASSIGNED_GENERIC, username);
                }
            case REVOKE_INTERNAL_ROLE:
                String revokedRoleName = userData.getTemplateVariables().getRoleName();
                if (revokedRoleName != null && !revokedRoleName.trim().isEmpty()) {
                    return String.format(AppConstants.DEFAULT_EMAIL_ROLE_REVOKED_WITH_NAME, username, revokedRoleName);
                } else {
                    return String.format(AppConstants.DEFAULT_EMAIL_ROLE_REVOKED_GENERIC, username);
                }
            default:
                return String.format(AppConstants.DEFAULT_EMAIL_SYSTEM_NOTIFICATION, username);
        }
    }
    
    /**
     * Escapes special characters in a string for safe JSON inclusion.
     * Handles null values, backslashes, quotes, and control characters.
     *
     * @param value the string to escape
     * @return the escaped string safe for JSON, or empty string if null
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}