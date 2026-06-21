package com.aidentx.authservice.service.Auth;

/**
 * Application-wide constants including error messages, error titles,
 * HTTP status descriptions, and validation rules.
 */
public final class AppConstants {
    private AppConstants() {}

    // --- General & Shared ---
    public static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please contact support.";
    public static final String USER_EVENT_TYPE = "user";
    public static final String SUCCESS_EVENT_TYPE = "success";
    public static final String FAILURE_EVENT_TYPE = "failure";
    public static final String EVENT_RISK_LOW = "low";
    public static final String EVENT_RISK_MEDIUM = "medium";
    public static final String EVENT_RISK_HIGH = "high";
    public static final String UNKNOWN_TENANT_ID = "unknown";

    // --- Logging Categories ---
    public static final String LOG_CATEGORY_AUTH = "auth";
    public static final String LOG_CATEGORY_AUTH_LDAP = "auth-ldap";
    public static final String LOG_CATEGORY_AUTH_SAML = "auth-saml";
    public static final String LOG_CATEGORY_AUTH_OAUTH2 = "auth-oauth2";
    public static final String LOG_CATEGORY_TOKEN_MGMT = "token-management";
    public static final String LOG_CATEGORY_USER_MGMT = "user-management";
    public static final String LOG_CATEGORY_CONFIGURATION = "configuration";
    public static final String LOG_CATEGORY_SSO = "sso";
    public static final String LOG_CATEGORY_NOTIFICATION = "notification";

    // --- Operation Names ---
    public static final String OP_LOGIN = "LOGIN";
    public static final String OP_LDAP_LOGIN = "LDAP_LOGIN";
    public static final String OP_SAML_LOGIN = "SAML_LOGIN";
    public static final String OP_OAUTH2_LOGIN = "OAUTH2_LOGIN";
    public static final String OP_SAML_RESPONSE = "SAML_RESPONSE";
    public static final String OP_OAUTH2_CALLBACK = "OAUTH2_CALLBACK";
    public static final String OP_OAUTH2_CALLBACK_POST = "OAUTH2_CALLBACK_POST";
    public static final String OP_SAML_LOGIN_INIT = "SAML_LOGIN_INIT";
    public static final String OP_SAML_SSO_INIT = "SAML_SSO_INIT";
    public static final String OP_OAUTH2_LOGIN_INIT = "OAUTH2_LOGIN_INIT";
    public static final String OP_SAML_CALLBACK = "SAML_CALLBACK";
    public static final String OP_TOKEN_REFRESH = "TOKEN_REFRESH";
    public static final String OP_LOGOUT = "LOGOUT";
    public static final String OP_UPDATE_PASSWORD = "UPDATE_PASSWORD";
    public static final String OP_REQUEST_PASSWORD_RESET = "REQUEST_PASSWORD_RESET";
    public static final String OP_RESET_PASSWORD = "RESET_PASSWORD";
    public static final String OP_SEND_PASSWORD_RESET_EMAIL = "SEND_PASSWORD_RESET_EMAIL";
    public static final String OP_EXTRACT_SAML_ATTRIBUTES = "EXTRACT_SAML_ATTRIBUTES";
    public static final String OP_EXTRACT_OAUTH2_ATTRIBUTES = "EXTRACT_OAUTH2_ATTRIBUTES";

    // --- Audit Event Types ---
    public static final String LOGIN_EVENT = "login";
    public static final String LOGOUT_EVENT = "logout";
    public static final String PASSWORD_UPDATE_EVENT = "password update";
    public static final String PASSWORD_RESET_EVENT = "password reset";
    
    // --- Error & Exception Messages ---
    public static final String USER_NOT_FOUND = "No user with username '%s' exists.";
    public static final String ACCOUNT_LOCKED = "Account is locked until %s.";
    public static final String WRONG_USERNAME_OR_PASSWORD = "Wrong Username or Password";
    public static final String ERROR_MALFORMED_REFRESH_TOKEN = "Malformed refresh token";
    public static final String ERROR_REFRESH_TOKEN_NOT_FOUND = "Refresh token not found";
    public static final String ERROR_REFRESH_TOKEN_EXPIRED = "Refresh token expired";
    public static final String ERROR_INVALID_REFRESH_TOKEN = "Invalid refresh token";
    public static final String ERROR_USER_NOT_FOUND_FOR_REFRESH_TOKEN = "User not found for the refresh token";
    public static final String MSG_LDAP_AUTH_NOT_ENABLED = "LDAP authentication not enabled";
    public static final String MSG_LDAP_AUTH_NOT_CONFIGURED = "LDAP authentication not configured";
    public static final String MSG_LDAP_AUTH_ERROR = "Invalid LDAP credentials";
    public static final String MSG_SAML_AUTH_NOT_ENABLED = "SAML authentication not enabled";
    public static final String MSG_SAML_REDIRECT = "Redirect to SAML IdP";
    public static final String MSG_SAML_INIT = "Initiating SAML authentication";
    public static final String MSG_SAML_AUTH_FAILED = "SAML authentication failed";
    public static final String MSG_OAUTH2_AUTH_NOT_ENABLED = "OAuth2 authentication not enabled";
    public static final String MSG_OAUTH2_REDIRECT = "Redirect to OAuth2 provider";
    public static final String INVALID_OLD_PASSWORD_MSG = "Old password is incorrect.";
    public static final String PASSWORD_UPDATE_ERROR_MSG = "An unexpected error occurred while updating the password. Please try again later.";
    public static final String PASSWORD_RESET_LINK_EXPIRED_MSG = "The password reset link has expired. Please request a new one.";
    public static final String MSG_OAUTH2_AUTH_FAILED = "OAuth2 authentication failed"; // <-- MISSING
    public static final String MSG_OAUTH2_INVALID_REQUEST = "Authorization code is required"; // <-- MISSING
    public static final String MSG_SAML_AUTH_NOT_AVAILABLE = "SAML authentication not available"; // <-- MISSING
    public static final String SAML_CERTIFICATE_ERROR_MSG = "SAML signature or certificate validation failed. Please contact your administrator."; // <-- MISSING
    public static final String SAML_GENERIC_ERROR_MSG = "SAML authentication failed. Please try again or contact support."; // <-- MISSING
    public static final String DEFAULT_SAML_AUTH_ERROR_MESSAGE = "SAML authentication failed. Redirecting to front-end.";

    // --- Success Messages ---
    public static final String PASSWORD_UPDATE_SUCCESS_MSG = "Password updated successfully.";
    public static final String PASSWORD_RESET_GENERIC_MSG = "A password reset link has been sent.";
    public static final String PASSWORD_RESET_SUCCESS_MSG = "Your password has been reset successfully.";
    public static final String MSG_OAUTH2_AUTH_SUCCESS = "OAuth2 authentication successful"; // <-- MISSING
    
    // --- Log Messages ---
    public static final String TRACE_LOGIN_METHOD_STARTED = "Login method started";
    public static final String DEBUG_LOGIN_REQUEST_RECEIVED = "Login request received";
    public static final String ACCOUNT_LOCKED_TITLE = "Account Locked";
    public static final String WARN_USER_NOT_FOUND_LOGIN = "User not found during login attempt";
    public static final String WARN_ACCOUNT_LOCKED_LOGIN = "Login attempt failed - account is locked";
    public static final String WARN_INVALID_CREDENTIALS_LOGIN = "Login attempt failed - invalid credentials";
    public static final String DEBUG_AUDIT_EVENT_CREATION = "Creating audit event for successful login";
    public static final String INFO_USER_LOGIN_SUCCESS = "User login successful";
    public static final String ERROR_UNEXPECTED_LOGIN = "Unexpected error during login: %s";
    public static final String TRACE_REFRESH_TOKEN_STARTED = "Refresh access token method started";
    public static final String INFO_TOKEN_REFRESH_STARTED = "Token refresh process started";
    public static final String DEBUG_USER_ID_EXTRACTED = "User ID extracted from refresh token";
    public static final String ERROR_MALFORMED_TOKEN = "Malformed refresh token provided";
    public static final String WARN_REFRESH_TOKEN_NOT_FOUND = "Refresh token not found in database";
    public static final String WARN_REFRESH_TOKEN_EXPIRED = "Refresh token has expired";
    public static final String WARN_REFRESH_TOKEN_VALIDATION_FAILED = "Refresh token validation failed";
    public static final String WARN_USER_NOT_FOUND_FOR_TOKEN = "User not found for refresh token";
    public static final String INFO_NEW_TOKENS_GENERATED = "New tokens generated successfully";
    public static final String INFO_NEW_ACCESS_TOKEN_GENERATED = "New access token generated successfully";
    public static final String DEBUG_OLD_TOKEN_REVOKED = "Old refresh token revoked";
    public static final String DEBUG_REFRESH_TOKEN_REUSED = "Existing refresh token reused";
    public static final String DEBUG_NEW_TOKEN_SAVED = "New refresh token saved to database";
    public static final String INFO_TOKEN_REFRESH_COMPLETED = "Token refresh completed successfully";
    public static final String WARN_INVALID_REFRESH_TOKEN_EXCEPTION = "Invalid refresh token exception: %s";
    public static final String ERROR_UNEXPECTED_TOKEN_REFRESH = "Unexpected error during token refresh: %s";
    public static final String TRACE_LOGOUT_METHOD = "AuthServiceImpl.logout() called";
    public static final String USER_LOGOUT_SUCCESS = "User logout successful: %s";
    public static final String TRACE_GET_AUTH_PROVIDERS_METHOD = "AuthServiceImpl.getAuthProviders() called";
    public static final String DEBUG_LDAP_PROVIDER_ENABLED = "LDAP provider enabled: %s";
    public static final String DEBUG_SAML_PROVIDER_ENABLED = "SAML provider enabled: %s";
    public static final String DEBUG_OAUTH2_PROVIDER_ENABLED = "OAuth2 provider enabled: %s";
    public static final String INFO_PROVIDERS_LIST_BUILT = "Providers list built, count: %d";
    public static final String TRACE_LDAP_LOGIN_METHOD = "AuthServiceImpl.ldapLogin() called";
    public static final String LOG_LDAP_AUTH_NOT_ENABLED = "LDAP authentication not enabled for user: %s";
    public static final String LOG_LDAP_AUTH_SUCCESS = "LDAP authentication successful for user: %s";
    public static final String USER_NOT_FOUND_IN_DB_POST_LDAP = "User not found in DB post-LDAP authentication";
    public static final String LOGIN_SUCCESS = "Login success for user: %s";
    public static final String LOG_LDAP_AUTH_INVALID = "LDAP authentication failed for user: %s";
    public static final String TRACE_INITIATE_SAML_LOGIN_METHOD = "AuthServiceImpl.initiateSamlLogin() called";
    public static final String LOG_SAML_AUTH_NOT_ENABLED = "SAML authentication not enabled";
    public static final String LOG_SAML_LOGIN_INIT = "Initiating SAML login for provider: %s";
    public static final String TRACE_INITIATE_SAML_SSO_METHOD = "AuthServiceImpl.initiateSamlSso() called";
    public static final String LOG_SAML_SSO_ATTEMPTED_BUT_NOT_ENABLED = "SAML SSO initiation attempted but SAML is not enabled";
    public static final String LOG_SAML_SSO_INIT = "Initiating SAML SSO for provider: %s";
    public static final String TRACE_HANDLE_SAML_RESPONSE_METHOD = "AuthServiceImpl.handleSamlResponse() called";
    public static final String LOG_SAML_PROCESSING_RESPONSE = "Saml2WebSsoAuthenticationFilter - Processing SAML Response";
    public static final String LOG_SAML_AUTH_RESPONSE_NULL = "SAML authentication response received but authentication is null";
    public static final String SAML_PRINCIPAL_ATTRIBUTES_AVAILABLE = "SAML principal attributes available: %s";
    public static final String SAML_AUTH_NAME_AS_EMAIL = "Using authentication name as email: %s";
    public static final String SAML_AUTH_FAILED_EMAIL_EXTRACTION = "Failed to extract email from SAML response";
    public static final String SAML_AUTH_FAILED_USER_NOT_FOUND = "SAML user not found for email: %s";
    public static final String TRACE_INITIATE_OAUTH2_LOGIN_METHOD = "AuthServiceImpl.initiateOAuth2Login() called";
    public static final String LOG_OAUTH2_AUTH_NOT_ENABLED = "OAuth2 authentication not enabled";
    public static final String LOG_OAUTH2_LOGIN_INIT = "Initiating OAuth2 login for provider: %s";
    public static final String TRACE_HANDLE_OAUTH2_CALLBACK_GET_METHOD = "AuthServiceImpl.handleOAuth2CallbackGet() called";
    public static final String LOG_OAUTH2_CALLBACK_ERROR = "OAuth2 callback received error: %s - %s";
    public static final String LOG_OAUTH2_AUTH_SUCCESS = "OAuth2 authentication successful for user: %s";
    public static final String OAUTH2_USER_ATTRIBUTES_AVAILABLE = "OAuth2 user attributes available: %s";
    public static final String OAUTH2_AUTH_NAME_AS_EMAIL = "Using OAuth2 authentication name as email: %s";
    public static final String OAUTH2_AUTH_FAILED_EMAIL_EXTRACTION = "Failed to extract email from OAuth2 response";
    public static final String OAUTH2_AUTH_FAILED_USER_NOT_FOUND = "OAuth2 user not found for email: %s";
    public static final String LOG_OAUTH2_CALLBACK_NO_CODE = "OAuth2 callback received without authorization code and without successful authentication";
    public static final String LOG_OAUTH2_CALLBACK_RECEIVED = "OAuth2 callback received with code: %s and state: %s";
    public static final String TRACE_HANDLE_OAUTH2_CALLBACK_METHOD = "AuthServiceImpl.handleOAuth2Callback() called";
    public static final String TRACE_HANDLE_SAML_CALLBACK_METHOD = "AuthServiceImpl.handleSamlCallback() called";
    public static final String LOG_SAML_CALLBACK_FAILED = "SAML authentication failed: %s";
    public static final String LOG_SAML_CALLBACK_NO_AUTH = "SAML callback accessed but no valid authentication found";
    public static final String LOG_SAML_CALLBACK_SUCCESS = "SAML authentication callback successful for user: %s";
    public static final String TRACE_UPDATE_PASSWORD_METHOD = "AuthServiceImpl.updatePassword() called for user: %s";
    public static final String INVALID_OLD_PASSWORD = "Invalid old password for user: %s";
    public static final String PASSWORD_UPDATE_SUCCESS = "Password updated successfully for user: %s";
    public static final String PASSWORD_UPDATE_ERROR = "Unexpected error during password update for user: %s";
    public static final String TRACE_PASSWORD_RESET_REQUEST = "AuthServiceImpl.requestPasswordReset() called for email: %s";
    public static final String PASSWORD_RESET_USER_NOT_FOUND = "No user found with email: %s for password reset";
    public static final String PASSWORD_RESET_TOKEN_UPDATED = "Password reset token updated for user: %s";
    public static final String PASSWORD_RESET_TOKEN_CREATED = "Password reset token created for user: %s";
    public static final String PASSWORD_RESET_EMAIL_SENT = "Password reset email sent to: %s";
    public static final String PASSWORD_RESET_UNEXPECTED_ERROR = "Unexpected error in requestPasswordReset for email: %s";
    public static final String PASSWORD_RESET_EMAIL_SUBJECT = "Password Reset Request";
    public static final String PASSWORD_RESET_EMAIL_CONTENT = "<p>To reset your password, click the button below:</p><a href='%s' style='padding:10px 20px;background:#1976d2;color:#fff;text-decoration:none;border-radius:4px;'>Reset Password</a><p>If you did not request a password reset, you can ignore this email.</p>";
    public static final String PASSWORD_RESET_EMAIL_FAILED = "Failed to send password reset email to: %s";
    public static final String ACCOUNT_NOT_ACTIVATED_MSG = "Your account has not been activated yet. Please use the \"Forgot Password\" option to set your password or contact your administrator.";
    public static final String TRACE_RESET_PASSWORD = "AuthServiceImpl.resetPassword() called for token: %s";
    public static final String PASSWORD_RESET_TOKEN_NOT_FOUND = "Password reset token not found: %s";
    public static final String PASSWORD_RESET_TOKEN_EXPIRED = "Password reset token expired: %s";
    public static final String PASSWORD_RESET_SUCCESS = "Password reset successful for user: %s";
    public static final String SAML_CONFIG_NOT_FOUND = "SAML configuration is not found";
    public static final String SAML_USER_ATTRIBUTES_EXTRACTED = "SAML user attributes extracted: %s";
    public static final String SAML_USER_ATTRIBUTE_EXTRACTION_ERROR = "Error extracting SAML user attributes";
    public static final String SAML_ATTRIBUTE_FOUND_CONFIGURED = "SAML attribute found using configured mapping: %s";
    public static final String SAML_ATTRIBUTE_FOUND_FALLBACK = "SAML attribute found using fallback mapping: %s";
    public static final String SAML_ATTRIBUTE_NOT_FOUND = "SAML attribute not found in SAML response.";
    public static final String OAUTH2_CONFIG_NOT_FOUND_USING_DEFAULTS = "OAuth2 configuration not found, using default attribute mappings";
    public static final String OAUTH2_USER_ATTRIBUTES_EXTRACTED = "OAuth2 user attributes extracted: %s";
    public static final String OAUTH2_USER_ATTRIBUTE_EXTRACTION_ERROR = "Error extracting OAuth2 user attributes";
    public static final String OAUTH2_ATTRIBUTE_FOUND_CONFIGURED = "OAuth2 attribute found using configured mapping: %s";
    public static final String OAUTH2_ATTRIBUTE_FOUND_FALLBACK = "OAuth2 attribute found using fallback mapping: %s";
    public static final String OAUTH2_ATTRIBUTE_NOT_FOUND = "OAuth2 attribute not found";

    // ============================================
    // POLICY MANAGEMENT CONSTANTS
    // ============================================
    
    // Trace Messages
    public static final String TRACE_CREATE_PASSWORD_POLICY = "PolicyService.createPasswordPolicy() called for tenant: %s";
    public static final String TRACE_UPDATE_PASSWORD_POLICY = "PolicyService.updatePasswordPolicy() called for tenant: %s";
    public static final String TRACE_GET_PASSWORD_POLICY = "PolicyService.getPasswordPolicy() called for tenant: %s";
    public static final String TRACE_DELETE_PASSWORD_POLICY = "PolicyService.deletePasswordPolicy() called for tenant: %s";
    public static final String TRACE_CREATE_ACCOUNT_LOCK_POLICY = "PolicyService.createAccountLockPolicy() called for tenant: %s";
    public static final String TRACE_UPDATE_ACCOUNT_LOCK_POLICY = "PolicyService.updateAccountLockPolicy() called for tenant: %s";
    public static final String TRACE_GET_ACCOUNT_LOCK_POLICY = "PolicyService.getAccountLockPolicy() called for tenant: %s";
    public static final String TRACE_DELETE_ACCOUNT_LOCK_POLICY = "PolicyService.deleteAccountLockPolicy() called for tenant: %s";

    // Success Messages
    public static final String PASSWORD_POLICY_CREATED_SUCCESS = "Password policy created successfully for tenant: %s";
    public static final String PASSWORD_POLICY_UPDATED_SUCCESS = "Password policy updated successfully for tenant: %s";
    public static final String PASSWORD_POLICY_RETRIEVED_SUCCESS = "Password policy retrieved successfully for tenant: %s";
    public static final String PASSWORD_POLICY_DELETED_SUCCESS = "Password policy deleted successfully for tenant: %s";
    public static final String ACCOUNT_LOCK_POLICY_CREATED_SUCCESS = "Account lock policy created successfully for tenant: %s";
    public static final String ACCOUNT_LOCK_POLICY_UPDATED_SUCCESS = "Account lock policy updated successfully for tenant: %s";
    public static final String ACCOUNT_LOCK_POLICY_RETRIEVED_SUCCESS = "Account lock policy retrieved successfully for tenant: %s";
    public static final String ACCOUNT_LOCK_POLICY_DELETED_SUCCESS = "Account lock policy deleted successfully for tenant: %s";

    // Error Messages
    public static final String PASSWORD_POLICY_NOT_FOUND = "Password policy not found for tenant: %s";
    public static final String PASSWORD_POLICY_ALREADY_EXISTS = "Password policy already exists for tenant: %s";
    public static final String PASSWORD_POLICY_CREATE_ERROR = "Failed to create password policy for tenant: %s";
    public static final String PASSWORD_POLICY_UPDATE_ERROR = "Failed to update password policy for tenant: %s";
    public static final String PASSWORD_POLICY_DELETE_ERROR = "Failed to delete password policy for tenant: %s";
    public static final String ACCOUNT_LOCK_POLICY_NOT_FOUND = "Account lock policy not found for tenant: %s";
    public static final String ACCOUNT_LOCK_POLICY_ALREADY_EXISTS = "Account lock policy already exists for tenant: %s";
    public static final String ACCOUNT_LOCK_POLICY_CREATE_ERROR = "Failed to create account lock policy for tenant: %s";
    public static final String ACCOUNT_LOCK_POLICY_UPDATE_ERROR = "Failed to update account lock policy for tenant: %s";
    public static final String ACCOUNT_LOCK_POLICY_DELETE_ERROR = "Failed to delete account lock policy for tenant: %s";
    public static final String MSG_LOCKOUT_EXPIRED = "Previous lockout has expired, clearing expired lock state";
    public static final String MSG_ACCOUNT_LOCK_CHECK = "Account lock check";
    // Response Messages
    public static final String PASSWORD_POLICY_CREATED_MSG = "Password policy created successfully.";
    public static final String PASSWORD_POLICY_UPDATED_MSG = "Password policy updated successfully.";
    public static final String PASSWORD_POLICY_DELETED_MSG = "Password policy deleted successfully.";
    public static final String ACCOUNT_LOCK_POLICY_CREATED_MSG = "Account lock policy created successfully.";
    public static final String ACCOUNT_LOCK_POLICY_UPDATED_MSG = "Account lock policy updated successfully.";
    public static final String ACCOUNT_LOCK_POLICY_DELETED_MSG = "Account lock policy deleted successfully.";
    public static final String PASSWORD_POLICY_NOT_FOUND_MSG = "Password policy not found.";
    public static final String PASSWORD_POLICY_ALREADY_EXISTS_MSG = "Password policy already exists for this tenant.";
    public static final String ACCOUNT_LOCK_POLICY_NOT_FOUND_MSG = "Account lock policy not found.";
    public static final String ACCOUNT_LOCK_POLICY_ALREADY_EXISTS_MSG = "Account lock policy already exists for this tenant.";

    // Operation Constants
    public static final String OP_CREATE_PASSWORD_POLICY = "CREATE_PASSWORD_POLICY";
    public static final String OP_UPDATE_PASSWORD_POLICY = "UPDATE_PASSWORD_POLICY";
    public static final String OP_GET_PASSWORD_POLICY = "GET_PASSWORD_POLICY";
    public static final String OP_DELETE_PASSWORD_POLICY = "DELETE_PASSWORD_POLICY";
    public static final String OP_CREATE_ACCOUNT_LOCK_POLICY = "CREATE_ACCOUNT_LOCK_POLICY";
    public static final String OP_UPDATE_ACCOUNT_LOCK_POLICY = "UPDATE_ACCOUNT_LOCK_POLICY";
    public static final String OP_GET_ACCOUNT_LOCK_POLICY = "GET_ACCOUNT_LOCK_POLICY";
    public static final String OP_DELETE_ACCOUNT_LOCK_POLICY = "DELETE_ACCOUNT_LOCK_POLICY";

    // Log Categories
    public static final String LOG_CATEGORY_POLICY_MGMT = "policy-management";

    // ============================================
    // VALIDATION MESSAGES
    // ============================================
    
    // Password Policy Validation Messages
    public static final String VALIDATION_MIN_PASSWORD_LENGTH_REQUIRED = "Minimum password length is required";
    public static final String VALIDATION_MIN_PASSWORD_LENGTH_MIN_VALUE = "Minimum password length must be at least 1";
    public static final String VALIDATION_PASSWORD_HISTORY_COUNT_REQUIRED = "Password history count is required";
    public static final String VALIDATION_PASSWORD_HISTORY_COUNT_MIN_VALUE = "Password history count must be 0 or greater";
    public static final String VALIDATION_REQUIRE_UPPERCASE_REQUIRED = "Require uppercase setting is required";
    public static final String VALIDATION_REQUIRE_LOWERCASE_REQUIRED = "Require lowercase setting is required";
    public static final String VALIDATION_REQUIRE_DIGITS_REQUIRED = "Require digits setting is required";
    public static final String VALIDATION_REQUIRE_SPECIAL_CHARS_REQUIRED = "Require special chars setting is required";
    public static final String VALIDATION_DISALLOW_USERNAME_OR_EMAIL_REQUIRED = "Disallow username or email setting is required";
    public static final String VALIDATION_MAX_PASSWORD_VALIDITY_DAYS_REQUIRED = "Max password validity days is required";
    public static final String VALIDATION_MAX_PASSWORD_VALIDITY_DAYS_MIN_VALUE = "Max password validity days must be at least 1";
    public static final String VALIDATION_PASSWORD_EXPIRATION_NOTIFICATION_DAYS_REQUIRED = "Password expiration notification days is required";
    public static final String VALIDATION_PASSWORD_EXPIRATION_NOTIFICATION_DAYS_MIN_VALUE = "Password expiration notification days must be at least 1";
    
    // Account Lock Policy Validation Messages
    public static final String VALIDATION_MAX_FAILED_LOGIN_ATTEMPTS_REQUIRED = "Maximum failed login attempts is required";
    public static final String VALIDATION_MAX_FAILED_LOGIN_ATTEMPTS_MIN_VALUE = "Maximum failed login attempts must be at least 1";
    public static final String VALIDATION_LOCKOUT_DURATION_MINUTES_REQUIRED = "Lockout duration in minutes is required";
    public static final String VALIDATION_LOCKOUT_DURATION_MINUTES_MIN_VALUE = "Lockout duration must be at least 1 minute";
    public static final String VALIDATION_INCREASE_DELAY_AFTER_FAILURE_SECONDS_REQUIRED = "Increase delay after failure seconds is required";
    public static final String VALIDATION_INCREASE_DELAY_AFTER_FAILURE_SECONDS_MIN_VALUE = "Increase delay after failure seconds must be 0 or greater";
    
    // Password Validation Error Messages
    public static final String PASSWORD_VALIDATION_GENERAL_ERROR = "An error occurred during password validation";
    public static final String PASSWORD_VALIDATION_MIN_LENGTH = "Password must be at least %d characters long";
    public static final String PASSWORD_VALIDATION_REQUIRE_UPPERCASE = "Password must contain at least one uppercase letter";
    public static final String PASSWORD_VALIDATION_REQUIRE_LOWERCASE = "Password must contain at least one lowercase letter";
    public static final String PASSWORD_VALIDATION_REQUIRE_DIGITS = "Password must contain at least one digit";
    public static final String PASSWORD_VALIDATION_REQUIRE_SPECIAL_CHARS = "Password must contain at least one special character";
    public static final String PASSWORD_VALIDATION_USERNAME_NOT_ALLOWED = "Password cannot contain your username";
    public static final String PASSWORD_VALIDATION_EMAIL_NOT_ALLOWED = "Password cannot contain your email address";
    public static final String PASSWORD_VALIDATION_BANNED_SUBSTRING = "Password cannot contain the word '%s'";
    public static final String PASSWORD_VALIDATION_PASSWORD_HISTORY = "Password cannot be one of your last %d passwords";
    
    // Password Validation Logging Messages
    public static final String LOG_PASSWORD_VALIDATION_START = "Starting password validation";
    public static final String LOG_PASSWORD_VALIDATION_SUCCESS = "Password validation successful";
    public static final String LOG_PASSWORD_VALIDATION_FAILED = "Password validation failed";
    public static final String LOG_PASSWORD_VALIDATION_ERROR = "Error during password validation";
    public static final String LOG_PASSWORD_HISTORY_CLEANUP_FAILED = "Failed to clean up password history, using default count";
    
    // Password History Logging Constants
    public static final String LOG_PASSWORD_HISTORY_SAVE_ATTEMPT = "Attempting to save password to history";
    public static final String LOG_PASSWORD_HISTORY_SAVE_SUCCESS = "Password successfully saved to history";
    public static final String LOG_PASSWORD_HISTORY_SAVE_FAILED = "Failed to save password to history";
    public static final String LOG_PASSWORD_REUSE_CHECK_START = "Checking password reuse";
    public static final String LOG_PASSWORD_REUSE_CHECK_COMPLETED = "Password reuse check completed";
    public static final String LOG_PASSWORD_REUSE_CHECK_ERROR = "Error checking password reuse";
    public static final String LOG_PASSWORD_HISTORY_ALL_CLEANED = "All password history cleaned (history disabled)";
    public static final String LOG_PASSWORD_HISTORY_CLEANUP_START = "Cleaning old password history";
    public static final String LOG_PASSWORD_HISTORY_CLEANUP_SUCCESS = "Old password history cleaned successfully";
    public static final String LOG_PASSWORD_HISTORY_COUNT_FAILED = "Failed to get password history count";
    
    // Account Lockout Logging Constants
    public static final String LOG_ACCOUNT_LOCK_EXPIRED = "Account lock period has expired";
    public static final String LOG_FAILED_LOGIN_ATTEMPT_PROCESSED = "Processing failed login attempt";
    public static final String LOG_NO_ACCOUNT_LOCK_POLICY_FOUND = "No account lock policy found for tenant";
    public static final String LOG_FAILED_LOGIN_PROCESS_ERROR = "Error processing failed login attempt";
    public static final String LOG_RESET_FAILED_ATTEMPTS = "Resetting failed login attempts after successful login";
    public static final String LOG_RESET_FAILED_ATTEMPTS_ERROR = "Error resetting failed login attempts";
    public static final String LOG_PROGRESSIVE_LOCKOUT_APPLIED = "Progressive lockout delay applied";
    public static final String LOG_ACCOUNT_LOCKED = "Account locked due to maximum failed login attempts";
    public static final String LOG_ACCOUNT_CURRENTLY_LOCKED = "Account is currently locked";
    
    // Password Validation Exception Messages
    public static final String PASSWORD_VALIDATION_FAILED_TITLE = "Password Validation Failed";
    public static final String PASSWORD_VALIDATION_FAILED_MESSAGE = "Password validation failed";
    
    // --- Audit Event Messages ---
    public static final String DEBUG_PREPARING_AUDIT_EVENT = "Preparing to send audit event";
    public static final String INFO_AUDIT_EVENT_SENT = "Audit event sent successfully";
    public static final String ERROR_AUDIT_EVENT_FAILED = "Failed to send audit event";
    public static final String DEBUG_TOKEN_EXTRACTION_FAILED = "Failed to extract actor name from token";
    public static final String DEBUG_ACTOR_ID_EXTRACTION_FAILED = "Failed to extract actor ID from token";
    
    // Password Policy Audit Messages
    public static final String DEBUG_PASSWORD_POLICY_AUDIT_PREPARE = "Preparing to send password policy audit event";
    public static final String INFO_PASSWORD_POLICY_AUDIT_SUCCESS = "Password policy audit event sent successfully";
    public static final String ERROR_PASSWORD_POLICY_AUDIT_FAILED = "Failed to send password policy audit event";
    
    // Account Lock Policy Audit Messages  
    public static final String DEBUG_ACCOUNT_LOCK_AUDIT_PREPARE = "Preparing to send account lock policy audit event";
    public static final String INFO_ACCOUNT_LOCK_AUDIT_SUCCESS = "Account lock policy audit event sent successfully";
    public static final String ERROR_ACCOUNT_LOCK_AUDIT_FAILED = "Failed to send account lock policy audit event";
    
    // REST Controller Messages - Account Lock Policy
    public static final String REST_CREATE_ACCOUNT_LOCK_POLICY = "REST: Create account lock policy for tenant: %s";
    public static final String REST_UPDATE_ACCOUNT_LOCK_POLICY = "REST: Update account lock policy for tenant: %s";
    public static final String REST_GET_ACCOUNT_LOCK_POLICY = "REST: Get account lock policy for tenant: %s";
    public static final String REST_DELETE_ACCOUNT_LOCK_POLICY = "REST: Delete account lock policy for tenant: %s";
    
    // REST Controller Messages - Password Policy
    public static final String REST_CREATE_PASSWORD_POLICY = "REST: Create password policy for tenant: %s";
    public static final String REST_UPDATE_PASSWORD_POLICY = "REST: Update password policy for tenant: %s";
    public static final String REST_GET_PASSWORD_POLICY = "REST: Get password policy for tenant: %s";
    public static final String REST_DELETE_PASSWORD_POLICY = "REST: Delete password policy for tenant: %s";
    
    // Kafka Producer Messages
    public static final String INFO_KAFKA_PRODUCER_INITIALIZED = "AuditKafkaProducerService initialized";
    public static final String DEBUG_KAFKA_SENDING_AUDIT = "Sending audit log to Kafka";
    public static final String INFO_KAFKA_AUDIT_SENT = "Audit log sent successfully to Kafka";
    public static final String ERROR_KAFKA_AUDIT_FAILED = "Failed to send audit log to Kafka";
    
    // Notification Event Producer Messages
    public static final String ERROR_USER_NO_TENANT_ID = "Cannot send notification: User has no tenant ID";
    public static final String INFO_PASSWORD_RESET_NOTIFICATION_SENT = "Password reset notification event sent to Kafka with tenant context";
    public static final String ERROR_NOTIFICATION_JSON_SERIALIZATION = "Failed to serialize notification event to JSON";
    public static final String ERROR_PASSWORD_RESET_NOTIFICATION_FAILED = "Failed to send password reset notification event";
    
    // ============================================
    // EXPIRED PASSWORD MANAGEMENT CONSTANTS
    // ============================================
    
    // Operation Names
    public static final String OP_UPDATE_EXPIRED_PASSWORD = "UPDATE_EXPIRED_PASSWORD";
    public static final String OP_CHECK_PASSWORD_EXPIRATION = "CHECK_PASSWORD_EXPIRATION";
    
    // Log Category
    public static final String LOG_CATEGORY_PASSWORD_EXPIRATION = "PASSWORD_EXPIRATION";
    
    // Success Messages
    public static final String EXPIRED_PASSWORD_UPDATE_SUCCESS_MSG = "Password updated successfully";
    
    // Error Messages  
    public static final String EXPIRED_PASSWORD_POLICY_NOT_FOUND_MSG = "Password policy not found for tenant";
    public static final String EXPIRED_PASSWORD_NO_EXPIRATION_POLICY_MSG = "No password expiration policy configured";
    public static final String EXPIRED_PASSWORD_NOT_EXPIRED_MSG = "Password is not expired yet. This endpoint only works when expired is true.";
    public static final String EXPIRED_PASSWORD_CONFIRMATION_MISMATCH_MSG = "New password and confirmation do not match";
    public static final String EXPIRED_PASSWORD_USER_NOT_FOUND_MSG = "User not found";
    public static final String EXPIRED_PASSWORD_CURRENT_INCORRECT_MSG = "Current password is incorrect";
    public static final String EXPIRED_PASSWORD_VALIDATION_FAILED_MSG = "Password validation failed";
    public static final String EXPIRED_PASSWORD_NOT_EXPIRED_ERROR_MSG = "Password is not expired. This endpoint only works for expired passwords.";
    public static final String EXPIRED_PASSWORD_INTERNAL_ERROR_MSG = "Internal server error occurred";
    public static final String EXPIRED_PASSWORD_INVALID_TOKEN_MSG = "Invalid or missing JWT token";
    public static final String EXPIRED_PASSWORD_TOKEN_EXTRACTION_ERROR_MSG = "Failed to extract user information from token";
    
    // Exception Messages
    public static final String EXPIRED_PASSWORD_RUNTIME_POLICY_NOT_FOUND = "Password policy not found for tenant";
    
    // Log Messages - Trace
    public static final String TRACE_EXPIRED_PASSWORD_UPDATE_METHOD = "UpdateExpiredPasswordService.updateExpiredPassword() called";
    public static final String TRACE_EXPIRED_PASSWORD_CHECK_METHOD = "UpdateExpiredPasswordService.isPasswordExpired() called";
    public static final String TRACE_EXPIRED_PASSWORD_DEBUG_METHOD = "UpdateExpiredPasswordService.getPasswordExpirationInfo() called";
    public static final String TRACE_EXPIRED_PASSWORD_CONTROLLER_UPDATE = "ExpiredPasswordController.updateExpiredPassword() called";
    public static final String TRACE_EXPIRED_PASSWORD_CONTROLLER_CHECK = "ExpiredPasswordController.checkPasswordExpiration() called";
    
    // Log Messages - Info
    public static final String INFO_EXPIRED_PASSWORD_UPDATE_SUCCESS = "Expired password updated successfully";
    public static final String INFO_EXPIRED_PASSWORD_UPDATE_SUCCESS_VIA_DTO = "Expired password updated successfully via DTO";
    public static final String INFO_EXPIRED_PASSWORD_CHECK_COMPLETED = "Password expiration check completed";
    
    // Log Messages - Debug
    public static final String DEBUG_EXPIRED_PASSWORD_POLICY_NO_EXPIRATION = "Password policy has no expiration time";
    public static final String DEBUG_EXPIRED_PASSWORD_EXPIRATION_CHECK = "Password expiration check completed";
    
    // Log Messages - Warn
    public static final String WARN_EXPIRED_PASSWORD_UPDATE_WHEN_NOT_EXPIRED = "Attempted to update password when not expired";
    public static final String WARN_EXPIRED_PASSWORD_USER_NOT_FOUND = "User not found for password update";
    public static final String WARN_EXPIRED_PASSWORD_NO_POLICY = "No password policy found for tenant during password update";
    public static final String WARN_EXPIRED_PASSWORD_VALIDATION_FAILED = "New password failed validation against current policy";
    public static final String WARN_EXPIRED_PASSWORD_NO_POLICY_EXPIRATION = "No password policy found for tenant";
    
    // Log Messages - Error
    public static final String ERROR_EXPIRED_PASSWORD_UPDATE = "Error updating expired password";
    public static final String ERROR_EXPIRED_PASSWORD_UPDATE_VIA_DTO = "Error updating expired password via DTO";
    public static final String ERROR_EXPIRED_PASSWORD_CHECK = "Error checking password expiration";
    public static final String ERROR_EXPIRED_PASSWORD_DEBUG_INFO = "Error getting password expiration info";
    public static final String ERROR_EXPIRED_PASSWORD_TOKEN_EXTRACTION = "Failed to extract user information from JWT token";
    
    // ============================================
    // AUDIT EVENT MESSAGES FOR POLICIES
    // ============================================
    
    // Password Policy Audit Messages
    public static final String AUDIT_PASSWORD_POLICY_ALREADY_EXISTS = "Password policy already exists";
    public static final String AUDIT_PASSWORD_POLICY_CREATED_SUCCESSFULLY = "Password policy created successfully";
    public static final String AUDIT_PASSWORD_POLICY_CREATION_FAILED = "Password policy creation failed";
    public static final String AUDIT_PASSWORD_POLICY_NOT_FOUND_FOR_UPDATE = "Password policy not found for update";
    public static final String AUDIT_PASSWORD_POLICY_UPDATED_SUCCESSFULLY = "Password policy updated successfully";
    public static final String AUDIT_PASSWORD_POLICY_UPDATE_FAILED = "Password policy update failed";
    public static final String AUDIT_PASSWORD_POLICY_NOT_FOUND_FOR_DELETION = "Password policy not found for deletion";
    public static final String AUDIT_PASSWORD_POLICY_DELETED_SUCCESSFULLY = "Password policy deleted successfully";
    public static final String AUDIT_PASSWORD_POLICY_DELETION_FAILED = "Password policy deletion failed";
    
    // Account Lock Policy Audit Messages
    public static final String AUDIT_ACCOUNT_LOCK_POLICY_ALREADY_EXISTS = "Account lock policy already exists";
    public static final String AUDIT_ACCOUNT_LOCK_POLICY_CREATED_SUCCESSFULLY = "Account lock policy created successfully";
    public static final String AUDIT_ACCOUNT_LOCK_POLICY_CREATION_FAILED = "Account lock policy creation failed";
    public static final String AUDIT_ACCOUNT_LOCK_POLICY_NOT_FOUND_FOR_UPDATE = "Account lock policy not found for update";
    public static final String AUDIT_ACCOUNT_LOCK_POLICY_UPDATED_SUCCESSFULLY = "Account lock policy updated successfully";
    public static final String AUDIT_ACCOUNT_LOCK_POLICY_UPDATE_FAILED = "Account lock policy update failed";
    public static final String AUDIT_ACCOUNT_LOCK_POLICY_NOT_FOUND_FOR_DELETION = "Account lock policy not found for deletion";
    public static final String AUDIT_ACCOUNT_LOCK_POLICY_DELETED_SUCCESSFULLY = "Account lock policy deleted successfully";
    public static final String AUDIT_ACCOUNT_LOCK_POLICY_DELETION_FAILED = "Account lock policy deletion failed";
    
    // ============================================
    // LOGGER OBJECT TYPE CONSTANTS
    // ============================================
    
    public static final String LOG_OBJECT_TYPE_PASSWORD_POLICY = "password_policy";
    public static final String LOG_OBJECT_TYPE_ACCOUNT_LOCK_POLICY = "account_lock_policy";
    
    // ============================================
    // AUDIT EVENT TYPE CONSTANTS
    // ============================================
    
    public static final String AUDIT_EVENT_CREATE = "create";
    public static final String AUDIT_EVENT_UPDATE = "update";
    public static final String AUDIT_EVENT_DELETE = "delete";
    
    // ============================================
    // SERVICE HELPER CONSTANTS
    // ============================================
    
    public static final String SYSTEM_USER = "system";
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    
    // ============================================
    // LOGGER DETAIL KEYS
    // ============================================
    
    public static final String LOG_DETAIL_OPERATION = "operation";
    public static final String LOG_DETAIL_TENANT_ID = "tenantId";
    public static final String LOG_DETAIL_POLICY_ID = "policyId";
    
    // ============================================
    // ERROR MESSAGE TEMPLATES FOR LOGGER
    // ============================================
    
    public static final String ERROR_RETRIEVE_PASSWORD_POLICY_FOR_TENANT = "Failed to retrieve password policy for tenant: %s";
    public static final String ERROR_RETRIEVE_ACCOUNT_LOCK_POLICY_FOR_TENANT = "Failed to retrieve account lock policy for tenant: %s";

    // ============================================
    // PASSWORD POLICY EXPIRATION CRON JOB CONSTANTS
    // ============================================
    
    // Info Messages
    public static final String INFO_CRON_POLICY_EXTENSION_START = "Starting password policy expiration extension cron job";
    public static final String INFO_CRON_NO_EXPIRED_POLICIES = "No expired password policies found";
    public static final String INFO_CRON_FOUND_EXPIRED_POLICIES = "Found %d expired password policies to extend";
    public static final String INFO_CRON_POLICY_EXTENSION_COMPLETE = "Password policy extension complete. Extended: %d, Skipped: %d";
    public static final String INFO_CRON_POLICY_EXPIRATION_EXTENDED = "Policy expiration extended for policy ID: %s, tenant: %s, old expiration: %d, new expiration: %d, validity days: %d";
    public static final String INFO_CRON_MANUAL_TRIGGER = "Manual password policy extension triggered";
    
    // Debug Messages
    public static final String DEBUG_CRON_POLICY_EXTENDED = "Successfully extended policy ID: %s for tenant: %s";
    public static final String DEBUG_CRON_POLICY_SKIPPED = "Skipped policy ID: %s for tenant: %s (no validity days configured)";
    public static final String DEBUG_CRON_POLICY_EXTENSION_DISABLED = "Password policy automatic extension is disabled";
    public static final String DEBUG_CRON_EMAIL_FLAG_RESET = "Email flag reset for policy ID: %s, tenant: %s";
    
    // Warning Messages
    public static final String WARN_CRON_POLICY_NO_VALIDITY_DAYS = "Policy ID: %s for tenant: %s has no maxPasswordValidityDays configured, skipping extension";
    public static final String WARN_CRON_POLICY_LIMIT_REACHED = "Found %d expired policies but processing only %d due to maxPoliciesPerRun limit";
    
    // Error Messages
    public static final String ERROR_CRON_POLICY_EXTENSION_FAILED = "Failed to extend policy ID: %s for tenant: %s";
    public static final String ERROR_CRON_POLICY_EXTENSION_GENERAL = "General error occurred during password policy extension cron job";
    
    // ============================================
    // PASSWORD EXPIRATION NOTIFICATION CRON JOB CONSTANTS
    // ============================================
    
    // Info Messages
    public static final String INFO_CRON_NOTIFICATION_START = "Starting password expiration notification cron job";
    public static final String INFO_CRON_NO_USERS_FOR_NOTIFICATION = "No users found requiring password expiration notifications";
    public static final String INFO_CRON_FOUND_USERS_FOR_NOTIFICATION = "Found %d users requiring password expiration notifications across %d tenants";
    public static final String INFO_CRON_NOTIFICATION_COMPLETE = "Password expiration notification complete. Notifications sent: %d, Failed: %d";
    public static final String INFO_CRON_NOTIFICATION_SENT = "Password expiration notification sent for user: %s, email: %s, tenant: %s, days until expiration: %d";
    public static final String INFO_CRON_MANUAL_NOTIFICATION_TRIGGER = "Manual password expiration notification triggered";
    
    // Debug Messages
    public static final String DEBUG_CRON_PROCESSING_TENANT = "Processing tenant: %s with %d users requiring notifications";
    public static final String DEBUG_CRON_USER_NOTIFICATION_PREPARED = "Prepared notification for user: %s, email: %s, expiration days: %d";
    public static final String DEBUG_CRON_NOTIFICATION_EVENT_SENT = "Notification event sent to Kafka for tenant: %s with %d users";
    public static final String DEBUG_CRON_NOTIFICATION_DISABLED = "Password expiration notification is disabled";
    
    // Warning Messages
    public static final String WARN_CRON_NOTIFICATION_LIMIT_REACHED = "Found %d users requiring notifications but processing only %d due to maxUsersPerRun limit";
    public static final String WARN_CRON_USER_NO_EMAIL = "User ID: %s has no email address, skipping notification";
    public static final String WARN_CRON_USER_NO_PASSWORD_SET_DATE = "User ID: %s has no password set date, skipping notification";
    public static final String WARN_CRON_USER_NO_VALIDITY_DAYS = "User ID: %s has no password validity days when set, skipping notification";
    public static final String WARN_CRON_TENANT_NO_POLICY = "No password policy or notification days configured for tenant: %s";
    
    // Error Messages
    public static final String ERROR_CRON_NOTIFICATION_FAILED = "Failed to send notification for user: %s, email: %s, tenant: %s";
    public static final String ERROR_CRON_NOTIFICATION_GENERAL = "General error occurred during password expiration notification cron job";
    public static final String ERROR_CRON_KAFKA_NOTIFICATION_FAILED = "Failed to send notification event to Kafka for tenant: %s";
    
    // Kafka Event Operation Type
    public static final String OPERATION_TYPE_PASSWORD_REMINDER_RESET = "PASSWORD_REMINDER_RESET";
    public static final String EMAIL_NOTIFICATION_FLAG_RESET = "Email notification flag reset for new policy period";
    
    // ============================================
    // NOTIFICATION EVENT PRODUCER CONSTANTS
    // ============================================
    
    // Info Messages
    public static final String INFO_PASSWORD_EXPIRATION_NOTIFICATION_SENT = "Password expiration notification event sent to Kafka";
    
    // Warning Messages
    public static final String WARN_NOTIFICATION_NO_DATA = "No notification data provided for tenant";
    
    // Error Messages
    public static final String ERROR_NOTIFICATION_INVALID_EVENT_DATA = "Invalid notification event data: missing tenant ID";
    public static final String ERROR_NOTIFICATION_MISSING_TENANT_ID = "Notification event data must have a valid tenant ID";
    public static final String ERROR_USER_MISSING_TENANT_ID = "User must have a valid tenant ID to send notifications";
    public static final String ERROR_PASSWORD_EXPIRATION_NOTIFICATION_JSON_FAILED = "Failed to serialize password expiration notification event to JSON";
    public static final String ERROR_PASSWORD_EXPIRATION_NOTIFICATION_KAFKA_FAILED = "Failed to send password expiration notification event to Kafka";
    public static final String ERROR_NOTIFICATION_SERIALIZATION_FAILED = "Failed to serialize notification event";
    public static final String ERROR_NOTIFICATION_SEND_FAILED = "Failed to send notification event";
    
    // ============================================
    // CONTROLLER CONSTANTS
    // ============================================
    
    // Debug Messages
    public static final String DEBUG_CONTROLLER_GET_NOTIFICATION_COUNT = "Getting users requiring password expiration notification count";
    public static final String DEBUG_CONTROLLER_GET_USERS_NOTIFICATION = "Getting users requiring password expiration notifications";
    public static final String DEBUG_CONTROLLER_GET_NOTIFICATION_STATUS = "Getting password expiration notification status";
    public static final String DEBUG_CONTROLLER_GET_EXPIRED_POLICY_COUNT = "Getting expired password policy count";
    public static final String DEBUG_CONTROLLER_NOTIFICATION_STATUS_RETRIEVED = "Password expiration notification status retrieved";
    
    // Info Messages
    public static final String INFO_CONTROLLER_NOTIFICATION_COUNT_RETRIEVED = "Retrieved users requiring notification count";
    public static final String INFO_CONTROLLER_USERS_NOTIFICATION_RETRIEVED = "Retrieved users requiring notifications";
    public static final String INFO_CONTROLLER_MANUAL_TRIGGER_REQUEST = "Manual password expiration notification trigger requested";
    public static final String INFO_CONTROLLER_MANUAL_TRIGGER_COMPLETED = "Manual password expiration notification trigger completed";
    public static final String INFO_CONTROLLER_TEST_TRIGGER_REQUEST = "Test cron job trigger requested";
    public static final String INFO_CONTROLLER_TEST_TRIGGER_COMPLETED = "Test cron job trigger completed successfully";
    public static final String INFO_CONTROLLER_POLICY_EXTENSION_TRIGGERED = "Manual password policy extension triggered via REST API";
    
    // Success Messages
    public static final String SUCCESS_CONTROLLER_NOTIFICATION_TRIGGER = "Password expiration notification job triggered successfully";
    public static final String SUCCESS_CONTROLLER_TEST_CRON_EXECUTED = "Cron job executed successfully";
    public static final String SUCCESS_CONTROLLER_NOTIFICATION_SERVICE_ACTIVE = "Password expiration notification service is active";
    public static final String SUCCESS_CONTROLLER_POLICY_EXTENSION_TRIGGERED = "Password policy extension job triggered successfully";
    public static final String SUCCESS_CONTROLLER_POLICY_SERVICE_RUNNING = "Password policy expiration service is running";
    
    // Note Messages
    public static final String NOTE_CONTROLLER_EXPIRATION_CALC_REQUIRES_POLICY = "Expiration calculation requires password policy lookup";
    
    // Error Messages
    public static final String ERROR_CONTROLLER_GET_NOTIFICATION_COUNT = "Failed to get users requiring notification count";
    public static final String ERROR_CONTROLLER_NOTIFICATION_COUNT_FAILED = "Failed to retrieve users requiring notification count";
    public static final String ERROR_CONTROLLER_GET_USERS_NOTIFICATION = "Failed to get users requiring notifications";
    public static final String ERROR_CONTROLLER_USERS_NOTIFICATION_FAILED = "Failed to retrieve users requiring notifications";
    public static final String ERROR_CONTROLLER_TRIGGER_NOTIFICATION = "Failed to trigger password expiration notifications";
    public static final String ERROR_CONTROLLER_NOTIFICATION_TRIGGER_FAILED = "Failed to trigger password expiration notification job";
    public static final String ERROR_CONTROLLER_TEST_CRON_FAILED = "Failed to execute test cron job";
    public static final String ERROR_CONTROLLER_CRON_EXECUTION_FAILED = "Failed to execute cron job: ";
    public static final String ERROR_CONTROLLER_GET_NOTIFICATION_STATUS = "Failed to get notification status";
    public static final String ERROR_CONTROLLER_NOTIFICATION_STATUS_FAILED = "Failed to retrieve notification status";
    public static final String ERROR_CONTROLLER_GET_EXPIRED_POLICY_COUNT = "Failed to get expired policy count";
    public static final String ERROR_CONTROLLER_EXPIRED_POLICY_COUNT_FAILED = "Failed to get expired policy count";
    public static final String ERROR_CONTROLLER_TRIGGER_POLICY_EXTENSION = "Failed to trigger password policy extension";
    public static final String ERROR_CONTROLLER_POLICY_EXTENSION_FAILED = "Failed to trigger password policy extension";
    
    // Service Names
    public static final String SERVICE_NAME_PASSWORD_POLICY_EXPIRATION = "PasswordPolicyExpirationService";
    public static final String NO_PASSWORD_POLICY_CONFIGURED = "No password policy or notification days configured for tenant: ";
    public static final String PASSWORD_EXPIRATION_EMAILS_ALREADY_SENT = "Password expiration emails already sent for this policy period";
    public static final String PASSWORD_POLICY_UPDATED_EMAILS_MARKED_SENT = "Password policy updated - emails marked as sent"; 
    public static final String MISSING_OR_INVALID_AUTH_HEADER = "Missing or invalid Authorization header";
    public static final String FAILED_TO_EXTRACT_TENANT_ID_FROM_JWT = "Failed to extract tenant ID from JWT token";
    public static final String PASSWORD_POLICY_EXTENSION_CRON = "Password policy extension cron - checking for expired policies";
    public static final String EXPIRED_POLICY_FOUND = "Found expired policy for extension";
    
    // --- Session Cleanup Constants ---
    public static final String LOG_SESSION_CLEANUP_STARTED = "Cleaning up expired sessions for tenant";
    public static final String LOG_SESSION_CLEANUP_COMPLETED = "Lazy cleanup completed for tenant";
    public static final String LOG_SESSION_DELETED_DURING_CLEANUP = "Deleted expired session during lazy cleanup";
    public static final String LOG_SESSION_USING_TENANT_EXPIRATION = "Using tenant-specific refresh token expiration";
    public static final String LOG_SESSION_USING_DEFAULT_EXPIRATION = "Using default refresh token expiration (no tenant policy found)";
    public static final String LOG_SESSION_RETRIEVING_FOR_TENANT = "Retrieving sessions for tenant";
    public static final String LOG_SESSION_LAZY_CLEANUP_EXECUTED = "Lazy cleanup: Deleted expired sessions";
}