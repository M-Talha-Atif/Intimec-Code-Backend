package com.aidentx.core.notifications_management.config;

/**
 * Application constants for notifications management module.
 *
 * @author aidentx Development Team
 * @version 1.0
 * @sinc    // Audit entity types
    public static final String ENTITY_TYPE_EMAIL_CONFIGURATION = "email_configurations";2025-08-18
 */
public class AppConstants {
    
    // Email Configuration Service Logger Constants
    public static final String EMAIL_CONFIG_SERVICE_LOGGER_NAME = "EmailConfigurationService";
    public static final String LOG_CATEGORY_EMAIL_CONFIG_SERVICE = "EMAIL_CONFIG_SERVICE";
    
    // Method Names
    public static final String METHOD_CREATE_EMAIL_CONFIG = "createEmailConfiguration";
    public static final String METHOD_UPDATE_EMAIL_CONFIG = "updateEmailConfiguration";
    public static final String METHOD_GET_EMAIL_CONFIG_BY_TENANT = "getEmailConfigurationByTenant";
    public static final String METHOD_DELETE_EMAIL_CONFIG = "deleteEmailConfiguration";
    public static final String METHOD_TEST_EMAIL_CONFIG = "testEmailConfiguration";
    
    // Log Messages - Email Configuration
    public static final String LOG_CREATING_EMAIL_CONFIG = "Creating email configuration for tenant: %s";
    public static final String LOG_EMAIL_CONFIG_CREATED = "Email configuration created successfully with ID: %s";
    public static final String LOG_UPDATING_EMAIL_CONFIG = "Updating email configuration with ID: %s";
    public static final String LOG_EMAIL_CONFIG_UPDATED = "Email configuration updated successfully with ID: %s";
    public static final String LOG_RETRIEVING_EMAIL_CONFIG = "Retrieving email configuration for tenant: %s";
    public static final String LOG_EMAIL_CONFIG_RETRIEVED = "Email configuration retrieved successfully for tenant: %s";
    public static final String LOG_DELETING_EMAIL_CONFIG = "Deleting email configuration with ID: %s";
    public static final String LOG_EMAIL_CONFIG_DELETED = "Email configuration deleted successfully with ID: %s";
    public static final String LOG_TESTING_EMAIL_CONFIG = "Testing email configuration with provided settings";
    public static final String LOG_EMAIL_CONFIG_TEST_SUCCESS = "Email configuration test completed successfully";
    public static final String LOG_EMAIL_CONFIG_TEST_FAILED = "Email configuration test failed for tenant: %s - %s";
    public static final String LOG_ENTERING_METHOD = "Entering method: %s";
    public static final String LOG_METHOD_ERROR = "Error in method %s: %s";
    
    // Error Messages - Email Configuration
    public static final String MSG_EMAIL_CONFIG_NOT_FOUND = "Email configuration not found for tenant: %s";
    public static final String MSG_EMAIL_CONFIG_NOT_FOUND_WITH_ID = "Email configuration not found with ID: %s";
    public static final String MSG_EMAIL_CONFIG_ALREADY_EXISTS = "Email configuration already exists for tenant: %s";
    
    // Validation Messages - Email Configuration
    public static final String VALIDATION_TENANT_ID_REQUIRED = "Tenant ID is required";
    public static final String VALIDATION_SMTP_HOSTNAME_REQUIRED = "SMTP hostname is required";
    public static final String VALIDATION_SMTP_PORT_REQUIRED = "SMTP port is required";
    public static final String VALIDATION_EMAIL_ADDRESS_REQUIRED = "Email address is required";
    public static final String VALIDATION_ENCRYPTION_TYPE_REQUIRED = "Encryption type is required";
    public static final String VALIDATION_AUTH_METHOD_REQUIRED = "Authentication method is required";
    public static final String VALIDATION_USERNAME_REQUIRED = "Username is required for username/password authentication";
    public static final String VALIDATION_PASSWORD_REQUIRED = "Password is required for username/password authentication";
    public static final String VALIDATION_API_KEY_REQUIRED = "API key is required for API key authentication";
    public static final String VALIDATION_INVALID_EMAIL_FORMAT = "Invalid email address format";
    public static final String VALIDATION_INVALID_SMTP_PORT = "SMTP port must be between 1 and 65535";
    
    // Detail Keys for Logging
    public static final String DETAIL_TENANT_ID = "tenantId";
    public static final String DETAIL_EMAIL_CONFIG_ID = "emailConfigId";
    public static final String DETAIL_SMTP_HOSTNAME = "smtpHostname";
    public static final String DETAIL_SMTP_PORT = "smtpPort";
    public static final String DETAIL_EMAIL_ADDRESS = "emailAddress";
    public static final String DETAIL_ENCRYPTION_TYPE = "encryptionType";
    public static final String DETAIL_AUTH_METHOD = "authMethod";
    public static final String DETAIL_TEST_RESULT = "testResult";
    public static final String DETAIL_ERROR_MESSAGE = "errorMessage";
    public static final String DETAIL_TEST_EMAIL_TO = "testEmailTo";
    public static final String DETAIL_TEST_EMAIL_FROM = "testEmailFrom";
    
    // Email Test Constants
    public static final String EMAIL_TEST_SUBJECT = "Test Email Configuration - AIdentX Notifications";
    public static final String EMAIL_TEST_BODY = "This is a test email to verify your email configuration is working correctly.\n\n" +
            "Configuration Details:\n" +
            "- SMTP Host: %s\n" +
            "- SMTP Port: %d\n" +
            "- Encryption: %s\n" +
            "- Authentication: %s\n\n" +
            "If you received this email, your configuration is working properly.\n\n" +
            "Best regards,\n" +
            "AIdentX Notifications System";
            
    // Email Error Messages
    public static final String EMAIL_TEST_ERROR_SMTP = "SMTP error during email test: %s";
    public static final String MSG_EMAIL_SEND_FAILED = "Failed to send test email: %s";
    public static final String MSG_EMAIL_AUTH_FAILED = "Email authentication failed";
    public static final String MSG_EMAIL_CONNECTION_FAILED = "Failed to connect to SMTP server";
    public static final String MSG_EMAIL_INVALID_CREDENTIALS = "Invalid email credentials";
    
    // Template Manager Constants
    public static final String TEMPLATE_MANAGER_SERVICE_LOGGER_NAME = "TemplateManagerService";
    public static final String LOG_CATEGORY_TEMPLATE_MANAGER_SERVICE = "template_manager_service";
    
    // Template Manager Method Names
    public static final String METHOD_CREATE_TEMPLATE_MANAGER = "createTemplateManager";
    public static final String METHOD_UPDATE_TEMPLATE_MANAGER = "updateTemplateManager";
    public static final String METHOD_GET_TEMPLATE_MANAGER_BY_ID = "getTemplateManagerById";
    public static final String METHOD_GET_TEMPLATE_MANAGERS_BY_TENANT = "getTemplateManagersByTenant";
    public static final String METHOD_DELETE_TEMPLATE_MANAGER = "deleteTemplateManager";
    
    // Template Manager Log Messages
    public static final String LOG_CREATING_TEMPLATE_MANAGER = "Creating template manager for tenant: %s";
    public static final String LOG_TEMPLATE_MANAGER_CREATED = "Template manager created with ID: %s";
    public static final String LOG_UPDATING_TEMPLATE_MANAGER = "Updating template manager with ID: %s";
    public static final String LOG_TEMPLATE_MANAGER_UPDATED = "Template manager updated with ID: %s";
    public static final String LOG_RETRIEVING_TEMPLATE_MANAGER = "Retrieving template manager with ID: %s";
    public static final String LOG_TEMPLATE_MANAGER_RETRIEVED = "Template manager retrieved with ID: %s";
    public static final String LOG_RETRIEVING_TEMPLATE_MANAGERS_BY_TENANT = "Retrieving template managers for tenant: %s";
    public static final String LOG_TEMPLATE_MANAGERS_RETRIEVED_BY_TENANT = "Retrieved %d template managers for tenant: %s";
    public static final String LOG_DELETING_TEMPLATE_MANAGER = "Deleting template manager with ID: %s";
    public static final String LOG_TEMPLATE_MANAGER_DELETED = "Template manager deleted with ID: %s";
    
    // Template Manager Validation Messages
    public static final String VALIDATION_TEMPLATE_NAME_REQUIRED = "Template name is required";
    public static final String VALIDATION_TEMPLATE_SUBJECT_REQUIRED = "Template subject is required";
    public static final String VALIDATION_EMAIL_BODY_REQUIRED = "Email body is required";
    public static final String VALIDATION_TRIGGER_TYPE_REQUIRED = "Trigger type is required";
    public static final String VALIDATION_LANGUAGE_REQUIRED = "Language is required";
    
    // Template Manager Error Messages
    public static final String MSG_TEMPLATE_MANAGER_NOT_FOUND = "Template manager not found with ID: %s";
    public static final String MSG_TEMPLATE_MANAGER_NOT_FOUND_FOR_TENANT = "No template managers found for tenant: %s";
    public static final String MSG_TEMPLATE_MANAGER_ALREADY_EXISTS = "Template manager already exists for tenant %s, trigger type %s, and language %s";
    
    // Template Manager Detail Keys
    public static final String DETAIL_TEMPLATE_MANAGER_ID = "templateManagerId";
    public static final String DETAIL_TEMPLATE_NAME = "templateName";
    public static final String DETAIL_TRIGGER_TYPE = "triggerType";
    public static final String DETAIL_LANGUAGE = "language";
    public static final String DETAIL_IS_ENABLED = "isEnabled";
    public static final String DETAIL_TEMPLATE_COUNT = "templateCount";
    
    // Notification Processing Service Constants
    public static final String NOTIFICATION_PROCESSING_SERVICE_LOGGER_NAME = "NotificationProcessingService";
    public static final String LOG_CATEGORY_NOTIFICATION_PROCESSING = "notification-processing";
    
    // System User Constants
    public static final String SYSTEM_USER_ID = "system";
    public static final String SYSTEM_USER_NAME = "system";
    
    // Notification Processing Log Messages
    public static final String LOG_NOTIFICATION_EVENT_RECEIVED = "Notification event received for processing";
    public static final String LOG_NOTIFICATION_EVENT_SUCCESS = "Successfully processed notification event";
    public static final String LOG_NOTIFICATION_EVENT_FAILED = "Failed to process notification event";
    public static final String LOG_USER_NOTIFICATION_SUCCESS = "Successfully processed notification for user";
    public static final String LOG_USER_NOTIFICATION_FAILED = "Failed to process notification for user";
    public static final String LOG_EMAIL_SENT_SUCCESS = "Email sent successfully";
    public static final String LOG_EMAIL_SEND_FAILED = "Failed to send email";
    public static final String LOG_TEMPLATE_NOT_FOUND = "Template not found for tenant";
    public static final String LOG_EMAIL_CONFIG_NOT_FOUND = "Email configuration not found for tenant";
    public static final String LOG_INVALID_OPERATION_TYPE = "Invalid operation type provided";
    
    // Additional Notification Processing Log Messages
    public static final String LOG_EMAIL_SENT_SUCCESSFULLY = "Email sent successfully";
    public static final String LOG_EMAIL_NOT_SENT_MISSING_CONFIG = "Email not sent - template or configuration missing";
    public static final String LOG_CREATING_NOTIFICATION_WITH_CLEAN_MESSAGE = "Creating notification with clean message";
    public static final String LOG_PROCESSING_PASSWORD_REMINDER_RESET = "Processing PASSWORD_REMINDER_RESET in generateCleanNotificationMessage";
    public static final String LOG_SUCCESSFULLY_CREATED_DYNAMIC_PASSWORD_MESSAGE = "Successfully created dynamic password expiration message";
    public static final String LOG_FAILED_PARSE_EXPIRATION_DAYS_GENERATE_CLEAN = "Failed to parse expirationDays from template variables in generateCleanNotificationMessage";
    public static final String LOG_TEMPLATE_VARIABLES_MISSING_GENERATE_CLEAN = "Template variables missing or incomplete for PASSWORD_REMINDER_RESET in generateCleanNotificationMessage";
    public static final String LOG_FAILED_EXTRACT_EXPIRATION_DAYS_FROM_BODY = "Failed to extract expiration days from email body";
    public static final String LOG_FAILED_EXTRACT_ROLE_NAME_FROM_BODY = "Failed to extract role name from processed body";
    public static final String LOG_FAILED_PARSE_EXPIRATION_DAYS_DEFAULT_BODY = "Failed to parse expirationDays from template variables";
    public static final String LOG_TEMPLATE_VARIABLES_MISSING_DEFAULT_BODY = "Template variables missing or invalid for PASSWORD_REMINDER_RESET notification";
    
    // Notification Processing Object Types
    public static final String LOG_OBJECT_TYPE_NOTIFICATION_EVENT = "NotificationEvent";
    public static final String LOG_OBJECT_TYPE_EMAIL = "Email";
    
    // Notification Processing Error Messages
    public static final String ERROR_TEMPLATE_NOT_FOUND = "Template not found for tenant: %s";
    public static final String ERROR_EMAIL_CONFIG_NOT_FOUND = "Email configuration not found for tenant: %s";
    public static final String ERROR_NOTIFICATION_PROCESSING_FAILED = "Failed to process notification event";
    public static final String ERROR_EMAIL_SEND_FAILED = "Failed to send email to: %s";
    public static final String ERROR_INVALID_OPERATION_TYPE = "Invalid operation type: %s";
    
    // Notification Kafka Consumer Constants
    public static final String KAFKA_CONSUMER_LOGGER_NAME = "NotificationKafkaConsumer";
    public static final String LOG_CATEGORY_KAFKA_CONSUMER = "NOTIFICATION_KAFKA";
    
    // Kafka Consumer Log Messages
    public static final String LOG_KAFKA_EVENT_RECEIVED = "Received notification event from Kafka";
    public static final String LOG_KAFKA_EVENT_DESERIALIZED = "Deserialized notification event";
    public static final String LOG_KAFKA_EVENT_PROCESSING_FAILED = "Failed to process notification event from Kafka";
    
    // Kafka Consumer Detail Keys
    public static final String DETAIL_KAFKA_TOPIC = "topic";
    public static final String DETAIL_KAFKA_PARTITION = "partition";
    public static final String DETAIL_KAFKA_OFFSET = "offset";
    public static final String DETAIL_KAFKA_KEY = "key";
    public static final String DETAIL_KAFKA_MESSAGE_VALUE = "messageValue";
    public static final String DETAIL_OPERATION_TYPE = "operationType";
    public static final String DETAIL_USER_COUNT = "userCount";
    
    // Notification Audit Constants
    public static final String NOTIFICATION_AUDIT_EVENT_PUBLISHER_LOGGER_NAME = "NotificationAuditEventPublisher";
    public static final String NOTIFICATION_AUDIT_ASPECT_LOGGER_NAME = "NotificationAuditAspect";
    public static final String LOG_CATEGORY_NOTIFICATION_AUDIT = "notification-audit";
    
    // Template Manager Audit Constants
    public static final String TEMPLATE_MANAGER_AUDIT_EVENT_PUBLISHER_LOGGER_NAME = "TemplateManagerAuditEventPublisher";
    public static final String TEMPLATE_MANAGER_AUDIT_ASPECT_LOGGER_NAME = "TemplateManagerAuditAspect";
    public static final String LOG_CATEGORY_TEMPLATE_MANAGER_AUDIT = "template-manager-audit";
    
    // Audit Event Types
    public static final String AUDIT_EVENT_CREATE = "CREATE";
    public static final String AUDIT_EVENT_UPDATE = "UPDATE";
    public static final String AUDIT_EVENT_DELETE = "DELETE";
    
    // Audit Entity Types
    public static final String ENTITY_TYPE_EMAIL_CONFIGURATION = "email_configurations";
    public static final String ENTITY_TYPE_TEMPLATE_MANAGER = "email_template";
    public static final String ENTITY_TYPE_NOTIFICATION = "NOTIFICATION";
    
    // Audit Log Messages
    public static final String AUDIT_LOG_CONVERTING_EVENT_TO_STANDARD_FORMAT = "Converting audit event to standard format";
    public static final String AUDIT_LOG_PUBLISHING_EVENT_TO_KAFKA = "Publishing audit event to Kafka";
    public static final String AUDIT_LOG_EVENT_PUBLISHED_SUCCESSFULLY = "Audit event published successfully";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_EVENT = "Failed to publish audit event";
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_EMAIL_CONFIG_CREATION = "Audit aspect triggered for email configuration creation";
    public static final String AUDIT_LOG_AUDITING_EMAIL_CONFIG_CREATION = "Auditing email configuration creation";
    public static final String AUDIT_LOG_AUDITING_EMAIL_CONFIG_UPDATE = "Auditing email configuration update";
    public static final String AUDIT_LOG_AUDITING_EMAIL_CONFIG_DELETION = "Auditing email configuration deletion";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_EMAIL_CONFIG_CREATION = "Successfully published audit event for email configuration creation";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_EMAIL_CONFIG_CREATION = "Failed to publish audit event for email configuration creation";
    public static final String AUDIT_LOG_AUDITING_EMAIL_CONFIG_CREATE_OR_UPDATE = "Auditing email configuration create or update";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_EMAIL_CONFIG_CREATE_OR_UPDATE = "Successfully published audit event for email configuration create or update";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_EMAIL_CONFIG_CREATE_OR_UPDATE = "Failed to publish audit event for email configuration create or update";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_EMAIL_CONFIG_UPDATE = "Successfully published audit event for email configuration update";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_EMAIL_CONFIG_UPDATE = "Failed to publish audit event for email configuration update";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_EMAIL_CONFIG_DELETION = "Successfully published audit event for email configuration deletion";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_EMAIL_CONFIG_DELETION = "Failed to publish audit event for email configuration deletion";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_EMAIL_CONFIG_STATE_BEFORE_UPDATE = "Could not retrieve email configuration state before update";
    public static final String AUDIT_LOG_COULD_NOT_RETRIEVE_EMAIL_CONFIG_STATE_BEFORE_DELETION = "Could not retrieve email configuration state before deletion";
    public static final String AUDIT_LOG_CREATING_AUDIT_EVENT = "Creating audit event";
    public static final String AUDIT_LOG_FAILED_TO_CREATE_OR_PUBLISH_EMAIL_CONFIG_AUDIT_EVENT = "Failed to create or publish email configuration audit event";
    
    // Template Manager Audit Log Messages
    public static final String AUDIT_LOG_ASPECT_TRIGGERED_TEMPLATE_MANAGER_CREATION = "Audit aspect triggered for template manager creation";
    public static final String AUDIT_LOG_AUDITING_TEMPLATE_MANAGER_CREATION = "Auditing template manager creation";
    public static final String AUDIT_LOG_AUDITING_TEMPLATE_MANAGER_UPDATE = "Auditing template manager update";
    public static final String AUDIT_LOG_AUDITING_TEMPLATE_MANAGER_DELETION = "Auditing template manager deletion";
    public static final String AUDIT_LOG_AUDITING_TEMPLATE_MANAGER_RESTORE = "Auditing template manager restore";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_TEMPLATE_MANAGER_CREATION = "Successfully published audit event for template manager creation";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_TEMPLATE_MANAGER_CREATION = "Failed to publish audit event for template manager creation";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_TEMPLATE_MANAGER_UPDATE = "Successfully published audit event for template manager update";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_TEMPLATE_MANAGER_UPDATE = "Failed to publish audit event for template manager update";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_TEMPLATE_MANAGER_DELETION = "Successfully published audit event for template manager deletion";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_TEMPLATE_MANAGER_DELETION = "Failed to publish audit event for template manager deletion";
    public static final String AUDIT_LOG_SUCCESSFULLY_PUBLISHED_AUDIT_EVENT_TEMPLATE_MANAGER_RESTORE = "Successfully published audit event for template manager restore";
    public static final String AUDIT_LOG_FAILED_TO_PUBLISH_AUDIT_EVENT_TEMPLATE_MANAGER_RESTORE = "Failed to publish audit event for template manager restore";
    public static final String AUDIT_LOG_FAILED_TO_SANITIZE_TEMPLATE_MANAGER_DATA = "Failed to sanitize template manager data";
    
    // Security Context Service Audit Log Messages
    public static final String AUDIT_LOG_EXTRACTED_USER_INFO_FROM_JWT_TOKEN = "Extracted user information from JWT token";
    public static final String AUDIT_LOG_FAILED_TO_EXTRACT_USER_INFO_FROM_JWT_TOKEN = "Failed to extract user information from JWT token";
    public static final String AUDIT_LOG_DEBUG_AUTHENTICATION_OBJECT = "Debug authentication object";
    public static final String AUDIT_LOG_EXTRACTING_USER_INFO_FROM_JWT_CLAIMS = "Extracting user information from JWT claims";
    public static final String AUDIT_LOG_EXTRACTING_USER_INFO_FROM_SPRING_SECURITY_USER = "Extracting user information from Spring Security User";
    public static final String AUDIT_LOG_FAILED_TO_EXTRACT_USER_INFO_FROM_SECURITY_CONTEXT = "Failed to extract user information from security context";
    public static final String AUDIT_LOG_FAILED_TO_EXTRACT_JWT_TOKEN_FROM_REQUEST = "Failed to extract JWT token from request";
    public static final String AUDIT_LOG_FAILED_TO_EXTRACT_IP_ADDRESS_FROM_REQUEST = "Failed to extract IP address from request";
    
    // Audit Object Types
    public static final String LOG_OBJECT_TYPE_AUDIT_EVENT = "AuditEvent";
    
    // SSE Controller Constants
    public static final String LOG_CATEGORY_NOTIFICATIONS_SSE = "NOTIFICATIONS_SSE";
    public static final String OBJECT_TYPE_SSE_CONNECTION = "SSE_CONNECTION";
    
    // SSE Controller Log Messages
    public static final String LOG_SSE_CONNECTION_REQUEST = "SSE connection request received";
    public static final String LOG_SSE_CONNECTION_ESTABLISHED = "SSE connection established";
    
    // Real-time notification log messages
    public static final String LOG_REAL_TIME_NOTIFICATION_SENT = "Real-time notification sent via SSE";
    public static final String LOG_REAL_TIME_NOTIFICATION_FAILED = "Failed to send real-time notification";
    public static final String LOG_REAL_TIME_NOTIFICATION_SSE_FAILED = "Failed to send real-time notification via SSE";
    
    // SSE Service Constants
    public static final String SSE_SERVICE_LOGGER_NAME = "NotificationSseService";
    public static final String LOG_CATEGORY_NOTIFICATIONS_SSE_SERVICE = "NOTIFICATIONS_SSE_SERVICE";
    public static final String OBJECT_TYPE_SSE = "SSE_CONNECTION";
    
    // Security constants for SSE
    public static final String SECURITY_ANONYMOUS_USER = "anonymousUser";
    public static final String SECURITY_BEARER_PREFIX = "Bearer ";
    public static final int BEARER_PREFIX_LENGTH = 7;
    public static final String AUTH_TOKEN_COOKIE_NAME = "auth_token";
    
    // SSE Service Log Messages
    public static final String LOG_SSE_CONNECTION_CREATED = "SSE connection created for user";
    public static final String LOG_SSE_CONNECTION_REMOVED = "SSE connection removed for user";
    public static final String LOG_SSE_NOTIFICATION_SENT = "Real-time notification sent to user";
    public static final String LOG_SSE_NOTIFICATION_FAILED = "Failed to send real-time notification";
    public static final String LOG_SSE_HEARTBEAT_SENT = "SSE heartbeat sent to user";
    public static final String LOG_USER_ID_EXTRACTED = "User ID extracted from JWT token";
    public static final String LOG_SSE_CONNECTION_COMPLETED = "SSE connection completed";
    public static final String LOG_SSE_CONNECTION_TIMED_OUT = "SSE connection timed out";
    public static final String LOG_SSE_CONNECTION_ERROR = "SSE connection error";
    public static final String LOG_SSE_INITIAL_MESSAGE_FAILED = "Failed to send initial SSE message";
    public static final String LOG_SSE_SERIALIZATION_FAILED = "Failed to serialize notification data";
    public static final String LOG_SSE_NO_ACTIVE_CONNECTION = "No active SSE connection found for user";
    public static final String LOG_SSE_CONNECTION_COMPLETION_ERROR = "Error completing SSE connection";
    public static final String LOG_SSE_USER_ID_EXTRACTION_FAILED = "Failed to extract user ID from JWT token";
    public static final String LOG_SSE_HEARTBEAT_FAILED = "Heartbeat failed, removing SSE connection";
    public static final String LOG_SSE_USER_ID_EXTRACTION_ERROR = "Unable to extract user ID from JWT token";
    
    // JWT Context Service Constants
    public static final String LOG_NO_REQUEST_CONTEXT = "No request context available";
    public static final String LOG_NO_JWT_TOKEN_FOUND = "No valid JWT token found in Authorization header";
    public static final String LOG_JWT_TOKEN_EXTRACTED = "JWT token extracted from request";
    public static final String LOG_TENANT_ID_EXTRACTED = "Tenant ID extracted from JWT token";
    public static final String LOG_TENANT_ACCESS_DENIED = "Access denied: User belongs to tenant '%s' but tried to access tenant '%s'";
    
    // Error Messages - JWT Context
    public static final String MSG_NO_REQUEST_CONTEXT = "No request context available";
    public static final String MSG_NO_JWT_TOKEN = "No valid JWT token found in Authorization header";
    public static final String MSG_TENANT_ACCESS_DENIED = "Access denied: User belongs to tenant '%s' but tried to access tenant '%s'";
    public static final String MSG_TENANT_VALIDATION_FAILED = "Tenant validation failed for user";
    
    // Template Manager Tenant Access Messages
    public static final String MSG_TEMPLATE_TENANT_ACCESS_DENIED = "Access denied: Template belongs to tenant '%s' but user belongs to tenant '%s'";
    
    // Email Configuration Tenant Access Messages
    public static final String MSG_EMAIL_CONFIG_TENANT_ACCESS_DENIED = "Access denied: Email configuration belongs to tenant '%s' but user belongs to tenant '%s'";

    // Clean notification messages for app notifications (separate from HTML email templates)
    public static final String NOTIFICATION_MSG_PASSWORD_RESET = "Your password has been reset successfully";
    public static final String NOTIFICATION_MSG_ROLE_ASSIGNED = "Role %s has been assigned to you";
    
    // Dynamic notification messages with proper formatting
    public static final String NOTIFICATION_MSG_PASSWORD_EXPIRY_DYNAMIC = "Your password will expire in %d days. Please change your password to avoid account lockout.";
    public static final String NOTIFICATION_MSG_PASSWORD_EXPIRY_FALLBACK = "Your password will expire soon. Please change your password.";
    public static final String INTERNAL_ROLE_ASSIGNMENT_MSG = "Internal Role assignment to %s";
    public static final String INTERNAL_ROLE_REVOCATION_MSG = "Internal Role revoked from %s";
    public static final String PASSWORD_RESET_MSG = "Password reset for %s";
    public static final String PASSWORD_REMINDER_RESET_MSG = "Password expiration reminder for %s";
    public static final String GENERIC_NOTIFICATION_MSG = "Notification for %s";
    public static final String NOTIFICATION_MSG_ROLE_REVOKED = "Role %s has been revoked";
    
    // Default email body messages
    public static final String DEFAULT_EMAIL_PASSWORD_EXPIRY_DYNAMIC = "Dear %s, your password will expire in %d days. Please change your password before it expires.";
    public static final String DEFAULT_EMAIL_PASSWORD_EXPIRY_FALLBACK = "Dear %s, your password will expire soon. Please change your password before it expires.";
    public static final String DEFAULT_EMAIL_PASSWORD_RESET = "Password has been reset for user %s. You can now log in with your new password.";
    public static final String DEFAULT_EMAIL_ACCESS_APPROVED = "Your access request has been approved, %s. You now have the requested permissions.";
    public static final String DEFAULT_EMAIL_ROLE_ASSIGNED_WITH_NAME = "Congratulations %s! You have been assigned the role: %s";
    public static final String DEFAULT_EMAIL_ROLE_ASSIGNED_GENERIC = "Congratulations %s! You have been assigned a new role.";
    public static final String DEFAULT_EMAIL_ROLE_REVOKED_WITH_NAME = "Dear %s, the role '%s' has been revoked from your account.";
    public static final String DEFAULT_EMAIL_ROLE_REVOKED_GENERIC = "Dear %s, a role has been revoked from your account.";
    public static final String DEFAULT_EMAIL_SYSTEM_NOTIFICATION = "System notification for user %s.";
    
    // Default email subjects
    public static final String DEFAULT_SUBJECT_PASSWORD_REMINDER = "Password Reset Request";
    public static final String DEFAULT_SUBJECT_PASSWORD_RESET = "Password Reset Notification";
    public static final String DEFAULT_SUBJECT_ACCESS_APPROVED = "Access Request Approved";
    public static final String DEFAULT_SUBJECT_ROLE_ASSIGNED = "Role Assignment Notification";
    public static final String DEFAULT_SUBJECT_ROLE_REVOKED = "Role Revocation Notification";
    public static final String DEFAULT_SUBJECT_SYSTEM = "System Notification";

    // User Notification Pin/Unpin Messages
    public static final String MSG_NOTIFICATION_PINNED_SUCCESS = "Notification pinned successfully";
    public static final String MSG_NOTIFICATION_UNPINNED_SUCCESS = "Notification unpinned successfully";
    public static final String MSG_NOTIFICATION_NOT_FOUND_FOR_USER = "Notification not found for user";
    
    // User Notification Pin/Unpin Log Messages
    public static final String LOG_PINNING_NOTIFICATION = "Pinning notification";
    public static final String LOG_UNPINNING_NOTIFICATION = "Unpinning notification";
    public static final String LOG_NOTIFICATION_PINNED_SUCCESS = "Successfully pinned notification";
    public static final String LOG_NOTIFICATION_UNPINNED_SUCCESS = "Successfully unpinned notification";
    public static final String LOG_NOTIFICATION_PIN_NOT_FOUND = "Notification not found for pinning";
    public static final String LOG_NOTIFICATION_UNPIN_NOT_FOUND = "Notification not found for unpinning";
    public static final String LOG_RETRIEVING_PINNED_NOTIFICATIONS = "Retrieving pinned notifications";
    public static final String LOG_PINNED_NOTIFICATIONS_RETRIEVED = "Successfully retrieved pinned notifications";
    
    // User Notification Controller Log Messages  
    public static final String LOG_PIN_NOTIFICATION_REQUEST = "PUT request to pin notification";
    public static final String LOG_UNPIN_NOTIFICATION_REQUEST = "PUT request to unpin notification";
    public static final String LOG_GET_PINNED_NOTIFICATIONS_REQUEST = "GET request for pinned notifications";
    public static final String LOG_GET_PINNED_ANNOUNCEMENTS_REQUEST = "GET request for pinned announcements";
    public static final String LOG_GET_PINNED_ALERTS_REQUEST = "GET request for pinned alerts";
    public static final String LOG_NOTIFICATION_PINNING_FAILED = "Notification pinning failed";
    public static final String LOG_NOTIFICATION_UNPINNING_FAILED = "Notification unpinning failed";

    // Debug log messages for notification processing
    public static final String CREATING_NOTIFICATION_WITH_CLEAN_MESSAGE = "Creating notification with clean message";
    public static final String LOG_DEBUG_LOOKING_FOR_TEMPLATE = "Looking for template";
    public static final String LOG_DEBUG_TEMPLATE_SEARCH_RESULT = "Template search result";
    public static final String LOG_DEBUG_NO_TEMPLATE_FOUND = "No template found";
    public static final String LOG_DEBUG_LOOKING_FOR_EMAIL_CONFIG = "Looking for email configuration";
    public static final String LOG_DEBUG_EMAIL_CONFIG_SEARCH_RESULT = "Email configuration search result";
    public static final String LOG_DEBUG_NO_EMAIL_CONFIG_FOUND = "No email configuration found";
    public static final String LOG_NOTIFICATION_CREATION_FAILED = "Failed to process notification for user";
    public static final String LOG_EXPIRATION_DAYS_EXTRACTION_FAILED = "Failed to extract expiration days from email body, using default";
    public static final String LOG_FALLBACK_NOTIFICATION_MESSAGE = "You have a new notification";
    private AppConstants() {
        // Private constructor to prevent instantiation
    }
}
