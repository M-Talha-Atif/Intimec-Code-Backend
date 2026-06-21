package com.aidentx.authservice.config;

/**
 * Application-wide constants including error messages, error titles,
 * HTTP status descriptions, and validation rules.
 */
public class AppConstants {
        // --- Logging levels and categories for RefreshTokenServiceImpl ---
        public static final String LOG_LEVEL_TRACE = "TRACE";
        public static final String LOG_LEVEL_DEBUG = "DEBUG";
        public static final String LOG_CATEGORY_REFRESH_TOKEN = "refresh-token";

        // --- Session Management Constants ---
        public static final String LOG_CATEGORY_SESSION = "SESSION";
        public static final String LOG_SESSION_CREATING = "Creating new user session";
        public static final String LOG_SESSION_CREATED_SUCCESS = "User session created successfully";
        public static final String LOG_SESSION_DELETING = "Deleting user session";
        public static final String LOG_SESSION_DELETED_SUCCESS = "User session deleted successfully";
        public static final String LOG_SESSION_DELETE_NON_EXISTENT = "Attempted to delete non-existent session";
        public static final String LOG_SESSION_DELETING_ALL_USER = "Deleting all sessions for user";
        public static final String LOG_SESSION_DELETED_ALL_USER = "All user sessions deleted";
        public static final String LOG_SESSION_RETRIEVING_FOR_TENANT = "Retrieving sessions for tenant";
        public static final String LOG_SESSION_CLEANUP_STARTED = "Cleaning up expired sessions for tenant";
        public static final String LOG_SESSION_DELETED_DURING_CLEANUP = "Deleted expired session during lazy cleanup";
        public static final String LOG_SESSION_CLEANUP_COMPLETED = "Lazy cleanup completed for tenant";
        public static final String LOG_SESSION_USING_TENANT_EXPIRATION = "Using tenant-specific refresh token expiration";
        public static final String LOG_SESSION_USING_DEFAULT_EXPIRATION = "Using default refresh token expiration (no tenant policy found)";
        
        public static final String OBJECT_TYPE_SESSION_CREATION = "SESSION_CREATION";
        public static final String OBJECT_TYPE_SESSION_DELETION = "SESSION_DELETION";
        public static final String OBJECT_TYPE_SESSION_RETRIEVAL = "SESSION_RETRIEVAL";
        public static final String OBJECT_TYPE_SESSION_CLEANUP = "SESSION_CLEANUP";

        // --- Logging messages for RefreshTokenServiceImpl ---
        public static final String LOG_MSG_CREATING_REFRESH_TOKEN = "Creating refresh token";
        public static final String LOG_MSG_REFRESH_TOKEN_CREATED_SUCCESS = "Successfully created refresh token for user ";
        public static final String LOG_MSG_REFRESH_TOKEN_CREATE_ERROR = "Error creating refresh token for user ";
        public static final String LOG_MSG_VALIDATING_REFRESH_TOKEN = "Validating refresh token";
        public static final String LOG_MSG_REFRESH_TOKEN_NOT_FOUND = "Refresh token not found in database.";
        public static final String LOG_MSG_REFRESH_TOKEN_EXPIRED = "Refresh token has expired. Expiry: ";
        public static final String LOG_MSG_REFRESH_TOKEN_VALID = "Refresh token is valid";
        public static final String LOG_MSG_REFRESH_TOKEN_VALIDATE_UNEXPECTED = "An unexpected error occurred during refresh token validation.";
        public static final String LOG_MSG_REVOKE_REFRESH_TOKEN = "Attempting to revoke refresh token";
        public static final String LOG_MSG_REFRESH_TOKEN_REVOKED_SUCCESS = "Successfully revoked refresh token.";
        public static final String LOG_MSG_REFRESH_TOKEN_REVOKE_NOT_FOUND = "Attempted to revoke a refresh token that was not found.";
        public static final String LOG_MSG_REFRESH_TOKEN_REVOKE_ERROR = "Error revoking refresh token";
        public static final String LOG_MSG_DELETE_ALL_REFRESH_TOKENS = "Deleting all refresh tokens for user";
        public static final String LOG_MSG_REFRESH_TOKEN_DELETE_ALL_SUCCESS = "Successfully deleted all refresh tokens for user ";
        public static final String LOG_MSG_REFRESH_TOKEN_DELETE_ALL_ERROR = "Error deleting all refresh tokens for user ";
        public static final String LOG_FILE_TYPE_NOT_FOUND = "Error: Invalid log file type '%s' requested.";

        // =========================================================================
        // SsoConfigurationManager Constants
        // =========================================================================

        // --- gRPC and HTTP Configuration ---
        public static final String GRPC_CLIENT_CONFIGURATION_SERVICE = "configuration-service";
        public static final String JSON_DATA_FIELD = "data";

        // --- HTTP Headers & gRPC Metadata Keys ---
        public static final String HEADER_USER_ID = "X-User-Id";
        public static final String HEADER_USERNAME = "X-Username";
        public static final String HEADER_IP_ADDRESS = "X-IP-Address";

        // --- System & Default Values ---
        public static final String UNKNOWN_IP_ADDRESS = "unknown";
        public static final String UNKNOWN_TENANT_ID = "unknown";
        public static final String SYSTEM_USER_ID = "system";
        public static final String SYSTEM_USERNAME = "system";

        // =========================================================================
        // WebSocket Configuration Constants
        // =========================================================================
        
        // --- WebSocket Endpoints & Prefixes ---
        public static final String WEBSOCKET_ENDPOINT = "/api/v1/auth/ws";
        public static final String WEBSOCKET_APP_PREFIX = "/app";
        public static final String WEBSOCKET_USER_PREFIX = "/user";
        public static final String WEBSOCKET_BROKER_TOPIC = "/topic";
        public static final String WEBSOCKET_BROKER_QUEUE = "/queue";
        
        // --- WebSocket Destinations ---
        public static final String WEBSOCKET_SESSION_TERMINATION_DEST = "/queue/session-termination";
        
        // --- WebSocket Headers ---
        public static final String WEBSOCKET_AUTH_HEADER = "Authorization";
        public static final String WEBSOCKET_AUTH_BEARER_PREFIX = "Bearer ";
        
        // --- WebSocket Logging Messages ---
        public static final String LOG_MSG_WEBSOCKET_CONFIGURING_BROKER = "Configuring WebSocket message broker";
        public static final String LOG_MSG_WEBSOCKET_BROKER_ENABLED = "Simple broker enabled";
        public static final String LOG_MSG_WEBSOCKET_APP_PREFIX_SET = "Application destination prefix set";
        public static final String LOG_MSG_WEBSOCKET_USER_PREFIX_SET = "User destination prefix set";
        public static final String LOG_MSG_WEBSOCKET_BROKER_CONFIGURED = "WebSocket message broker configured";
        public static final String LOG_MSG_WEBSOCKET_REGISTERING_ENDPOINTS = "Registering STOMP endpoints for WebSocket";
        public static final String LOG_MSG_WEBSOCKET_ENDPOINT_ADDED = "STOMP endpoint added with SockJS";
        public static final String LOG_MSG_WEBSOCKET_ENDPOINT_REGISTERED = "WebSocket STOMP endpoint registered";
        public static final String LOG_MSG_WEBSOCKET_CONFIGURING_INBOUND_CHANNEL = "Configuring client inbound channel interceptor";
        public static final String LOG_MSG_WEBSOCKET_PROCESSING_CONNECT = "Processing STOMP CONNECT command";
        public static final String LOG_MSG_WEBSOCKET_CONNECTION_AUTHENTICATED = "WebSocket connection authenticated";
        public static final String LOG_MSG_WEBSOCKET_INVALID_JWT = "Invalid JWT token for WebSocket connection";
        public static final String LOG_MSG_WEBSOCKET_NO_AUTH_HEADER = "WebSocket connection attempt without valid Authorization header";
        public static final String LOG_MSG_WEBSOCKET_JWT_VALIDATION_ERROR = "Error validating WebSocket JWT token";
        public static final String LOG_MSG_WEBSOCKET_VALIDATING_TOKEN = "Validating JWT token for WebSocket connection";
        public static final String LOG_MSG_WEBSOCKET_EXTRACTING_USERNAME = "Extracting username from JWT token";
        public static final String LOG_MSG_WEBSOCKET_USERNAME_EXTRACTED = "Username extracted from token";
        public static final String LOG_MSG_WEBSOCKET_SETTING_USER_CONTEXT = "Setting user context for WebSocket connection";
        public static final String LOG_MSG_WEBSOCKET_INBOUND_CHANNEL_CONFIGURED = "Client inbound channel interceptor configured";
        
        // --- WebSocket Logging Detail Keys ---
        public static final String LOG_DETAIL_WEBSOCKET_ENDPOINT = "endpoint";
        public static final String LOG_DETAIL_WEBSOCKET_SOCKJS = "sockJS";
        public static final String LOG_DETAIL_WEBSOCKET_BROKERS = "brokers";
        public static final String LOG_DETAIL_WEBSOCKET_APP_PREFIX = "appPrefix";
        public static final String LOG_DETAIL_WEBSOCKET_TOKEN_VALID = "tokenValid";
        public static final String LOG_DETAIL_SESSION_ID = "sessionId";
        public static final String LOG_DETAIL_TERMINATION_REASON = "terminationReason";
        public static final String LOG_DETAIL_AUTH_TOKEN_PRESENT = "authTokenPresent";
        public static final String LOG_DETAIL_TOKEN_LENGTH = "tokenLength";
        public static final String LOG_DETAIL_USER_PREFIX = "userPrefix";
        public static final String LOG_DETAIL_ALLOWED_ORIGINS = "allowedOrigins";
        
        // --- WebSocket Logging Categories ---
        public static final String LOG_CATEGORY_WEBSOCKET = "websocket";
        public static final String LOG_CATEGORY_SESSION_TERMINATION = "session-termination";
        
        // --- Session Termination Messages ---
        public static final String LOG_MSG_TERMINATING_SESSION = "Terminating user session";
        public static final String LOG_MSG_SESSION_TERMINATED_SUCCESS = "Session terminated successfully";
        public static final String LOG_MSG_SESSION_NOT_FOUND_FOR_TERMINATION = "Session not found for termination";
        public static final String LOG_MSG_SESSION_TERMINATION_NOTIFICATION_SENT = "Session termination notification sent via WebSocket";
        public static final String LOG_MSG_SESSION_TERMINATION_NOTIFICATION_FAILED = "Failed to send session termination notification";
        public static final String LOG_MSG_GETTING_SESSION_FOR_TERMINATION = "Retrieving session for termination";
        public static final String LOG_MSG_DELETING_SESSION_FROM_DB = "Deleting session from database";
        public static final String LOG_MSG_BROADCASTING_TERMINATION = "Broadcasting session termination to user";
        
        // --- Session Termination Reasons ---
        public static final String TERMINATION_REASON_ADMIN = "Terminated by administrator";
        public static final String TERMINATION_REASON_SECURITY = "Security policy violation";
        public static final String TERMINATION_REASON_MANUAL = "Manual termination";
        
        // --- WebSocket Message Types ---
        public static final String WEBSOCKET_MESSAGE_TYPE_SESSION_TERMINATED = "SESSION_TERMINATED";
        
        // --- Allowed Origins Pattern ---
        public static final String WEBSOCKET_ALLOWED_ORIGINS_PATTERN = "*";

        // --- Logger Configuration ---
        public static final String LOGGER_NAME_SSO_CONFIG_MANAGER = "SsoConfigManager";
        public static final String LOGGER_TOPIC_CONFIG_SSO = "config-sso";

        // --- Logging Messages ---
        public static final String LOG_MSG_GET_LDAP_CONFIG = "Attempting to get LDAP config via gRPC.";
        public static final String LOG_MSG_GET_SAML_CONFIG = "Attempting to get SAML config via gRPC.";
        public static final String LOG_MSG_GET_OAUTH2_CONFIG = "Attempting to get OAuth2 config via gRPC.";
        public static final String LOG_MSG_EXECUTE_GRPC_GET_CONFIG = "Executing gRPC getConfig call.";
        public static final String LOG_MSG_CONFIG_NOT_FOUND_GRPC = "Configuration not found via gRPC.";
        public static final String LOG_MSG_DATA_FIELD_MISSING_IN_RESPONSE = "The 'data' field is missing from the gRPC response wrapper.";
        public static final String LOG_MSG_CONFIG_RETRIEVED_SUCCESS = "Successfully retrieved and mapped configuration.";
        public static final String LOG_MSG_GRPC_CALL_FAILED = "gRPC call failed while getting configuration.";
        public static final String LOG_MSG_PROCESS_GRPC_RESPONSE_FAILED = "Failed to process gRPC response for configuration.";
        public static final String LOG_MSG_USER_CONTEXT_FROM_HEADERS = "Added user context from HTTP headers to gRPC metadata";
        public static final String LOG_MSG_USER_CONTEXT_FROM_SPRING_SECURITY = "Added user context from Spring Security to gRPC metadata";
        public static final String LOG_MSG_EXTRACT_USER_CONTEXT_FAILED = "Failed to extract user context from Spring Security for gRPC metadata";
        public static final String LOG_MSG_USING_SYSTEM_CONTEXT = "Using system user context for gRPC metadata";
        public static final String LOG_MSG_CANNOT_GET_HTTP_REQUEST = "Could not retrieve HTTP request context";

        // --- Logging Detail Keys ---
        public static final String LOG_DETAIL_GRPC_STATUS = "grpcStatus";
        public static final String LOG_DETAIL_USER_ID = "userId";
        public static final String LOG_DETAIL_USERNAME = "username";
        public static final String LOG_DETAIL_IP_ADDRESS = "ipAddress";
        public static final String LOG_DETAIL_ERROR = "error";

        /**
         * Minimum length required for user passwords.
         */
        public static final int PASSWORD_MIN_LENGTH = 8;

        /**
         * Error message template when a user is not found.
         * Usage: String.format(USER_NOT_FOUND, username)
         */
        public static final String USER_NOT_FOUND = "No user with username '%s' exists.";

        /**
         * Title for user not found errors.
         */
        public static final String USER_NOT_FOUND_TITLE = "User Not Found";

        /**
         * Error message when provided credentials are invalid.
         */
        public static final String WRONG_USERNAME_OR_PASSWORD = "Wrong Username or Password";

        /**
         * Title for unauthorized access errors.
         */
        public static final String UNAUTHORIZED_TITLE = "Unauthorized";

        /**
         * Error message template when an account is locked.
         * Usage: String.format(ACCOUNT_LOCKED, lockedUntil)
         */
        public static final String ACCOUNT_LOCKED = "Account is locked until %s.";

        /**
         * Title for account locked errors.
         */
        public static final String ACCOUNT_LOCKED_TITLE = "Account Locked";

        /**
         * Title for generic internal server errors.
         */
        public static final String GENERIC_ERROR_TITLE = "Internal Server Error";

        /**
         * Generic error message for unexpected server errors.
         */
        public static final String GENERIC_ERROR_MESSAGE = "An unexpected error occurred. Please contact support.";

        /**
         * Trace-level message for starting login process.
         * Usage: String.format(LOGIN_START, username)
         */
        public static final String LOGIN_START = "Starting login process for user: %s";

        /**
         * Debug-level message for received login request.
         * Usage: String.format(LOGIN_REQUEST_RECEIVED, loginRequest)
         */
        public static final String LOGIN_REQUEST_RECEIVED = "Received login request: %s";

        /**
         * Warn-level message when user not found during login.
         * Usage: String.format(LOGIN_USER_NOT_FOUND, username)
         */
        public static final String LOGIN_USER_NOT_FOUND = "Login attempt failed - user not found: %s";

        /**
         * Warn-level message when login fails due to account locked.
         * Usage: String.format(LOGIN_ACCOUNT_LOCKED, username, lockedUntil)
         */
        public static final String LOGIN_ACCOUNT_LOCKED = "Login attempt failed - account locked for user: %s until %s";

        /**
         * Logging only
         * Warn-level message when login fails due to invalid credentials.
         * Usage: String.format(LOGIN_INVALID_CREDENTIALS, username)
         */
        public static final String LOGIN_INVALID_CREDENTIALS = "Login failed due to invalid credentials for user: %s";

        /**
         * Info-level message on successful login.
         * Usage: String.format(LOGIN_SUCCESS, username)
         */
        public static final String LOGIN_SUCCESS = "Login success for user: %s";

        /**
         * Error-level message for unexpected login error.
         * Usage: String.format(LOGIN_UNEXPECTED_ERROR, username)
         */
        public static final String LOGIN_UNEXPECTED_ERROR = "Unexpected error during login for user: %s";

        /**
         * Error message for malformed refresh tokens.
         */
        public static final String ERROR_MALFORMED_REFRESH_TOKEN = "Malformed refresh token";

        /**
         * Error message for refresh tokens not found in the database.
         */
        public static final String ERROR_REFRESH_TOKEN_NOT_FOUND = "Refresh token not found";

        /**
         * Error message for revoked refresh tokens.
         */
        public static final String ERROR_REFRESH_TOKEN_REVOKED = "Refresh token revoked";

        /**
         * Error message for expired refresh tokens.
         */
        public static final String ERROR_REFRESH_TOKEN_EXPIRED = "Refresh token expired";

        /**
         * Error message for invalid refresh tokens (signature or claims).
         */
        public static final String ERROR_INVALID_REFRESH_TOKEN = "Invalid refresh token";

        /**
         * Error message when the user associated with the refresh token is not found.
         */
        public static final String ERROR_USER_NOT_FOUND_FOR_REFRESH_TOKEN = "User not found for the refresh token";

        /**
         * Info-level message for starting token refresh.
         * Usage: String.format(LOG_TOKEN_REFRESH_START, refreshToken)
         */
        public static final String LOG_TOKEN_REFRESH_START = "Starting token refresh for refresh token: %s";

        /**
         * Debug-level message for extracted user ID from refresh token.
         * Usage: String.format(LOG_EXTRACTED_USER_ID, userId)
         */
        public static final String LOG_EXTRACTED_USER_ID = "Extracted user ID from refresh token: %s";

        /**
         * Error-level message for malformed refresh token.
         * Usage: String.format(LOG_MALFORMED_REFRESH_TOKEN, refreshToken)
         */
        public static final String LOG_MALFORMED_REFRESH_TOKEN = "Malformed refresh token: %s";

        /**
         * Warn-level message for refresh token not found in database.
         * Usage: String.format(LOG_REFRESH_TOKEN_NOT_FOUND, refreshToken)
         */
        public static final String LOG_REFRESH_TOKEN_NOT_FOUND = "Refresh token not found in DB: %s";

        /**
         * Warn-level message for revoked refresh token.
         * Usage: String.format(LOG_REFRESH_TOKEN_REVOKED, refreshToken)
         */
        public static final String LOG_REFRESH_TOKEN_REVOKED = "Refresh token revoked: %s";

        /**
         * Warn-level message for expired refresh token.
         * Usage: String.format(LOG_REFRESH_TOKEN_EXPIRED, refreshToken)
         */
        public static final String LOG_REFRESH_TOKEN_EXPIRED = "Refresh token expired: %s";

        /**
         * Warn-level message for invalid refresh token signature or claims.
         * Usage: String.format(LOG_INVALID_REFRESH_TOKEN, refreshToken)
         */
        public static final String LOG_INVALID_REFRESH_TOKEN = "Invalid refresh token signature or claims: %s";

        /**
         * Warn-level message when user not found for the refresh token.
         * Usage: String.format(LOG_USER_NOT_FOUND_FOR_REFRESH_TOKEN, userId)
         */
        public static final String LOG_USER_NOT_FOUND_FOR_REFRESH_TOKEN = "User not found for refresh token, userId: %s";

        /**
         * Info-level message when new tokens are generated.
         * Usage: String.format(LOG_TOKENS_GENERATED, userId)
         */
        public static final String LOG_TOKENS_GENERATED = "Generated new access and refresh tokens for userId: %s";

        /**
         * Debug-level message when old refresh token is revoked.
         * Usage: String.format(LOG_OLD_REFRESH_TOKEN_REVOKED, oldRefreshToken)
         */
        public static final String LOG_OLD_REFRESH_TOKEN_REVOKED = "Revoked old refresh token: %s";

        /**
         * Debug-level message when new refresh token is saved.
         * Usage: String.format(LOG_NEW_REFRESH_TOKEN_SAVED, newRefreshToken)
         */
        public static final String LOG_NEW_REFRESH_TOKEN_SAVED = "Saved new refresh token to DB: %s";

        /**
         * Info-level message on successful tokens refresh.
         * Usage: String.format(LOG_TOKENS_REFRESH_SUCCESS, userId)
         */
        public static final String LOG_TOKENS_REFRESH_SUCCESS = "Successfully refreshed tokens for userId: %s";

        /**
         * Error-level message for InvalidRefreshTokenException caught.
         * Usage: String.format(LOG_INVALID_REFRESH_TOKEN_EXCEPTION, errorMessage)
         */
        public static final String LOG_INVALID_REFRESH_TOKEN_EXCEPTION = "Invalid refresh token exception: %s";

        /**
         * Error-level message for unexpected errors during token refresh.
         * Usage: LOG_UNEXPECTED_ERROR
         */
        public static final String LOG_UNEXPECTED_ERROR = "Unexpected error during token refresh";

        /**
         * Info-level message indicating the start of a refresh token creation process.
         * Usage: String.format(REFRESH_TOKEN_CREATE, refreshToken)
         */
        public static final String REFRESH_TOKEN_CREATE = "Starting refresh token creation: %s";

        /**
         * Info-level message indicating successful creation of a refresh token.
         * Usage: String.format(REFRESH_TOKEN_CREATED, refreshToken)
         */
        public static final String REFRESH_TOKEN_CREATED = "Refresh token created successfully: %s";

        /**
         * Info-level message for the validation process of a refresh token.
         * Usage: String.format(REFRESH_TOKEN_VALIDATE, refreshToken)
         */
        public static final String REFRESH_TOKEN_VALIDATE = "Validating refresh token: %s";

        /**
         * Warn-level message when a refresh token is not found.
         * Usage: String.format(REFRESH_TOKEN_NOT_FOUND, refreshToken)
         */
        public static final String REFRESH_TOKEN_NOT_FOUND = "Refresh token not found: %s";

        /**
         * Warn-level message when a refresh token has been revoked.
         * Usage: String.format(REFRESH_TOKEN_REVOKED, refreshToken)
         */
        public static final String REFRESH_TOKEN_REVOKED = "Refresh token has been revoked: %s";

        /**
         * Warn-level message when a refresh token has expired.
         * Usage: String.format(REFRESH_TOKEN_EXPIRED, refreshToken)
         */
        public static final String REFRESH_TOKEN_EXPIRED = "Refresh token expired: %s";

        /**
         * Info-level message indicating a refresh token is valid.
         * Usage: String.format(REFRESH_TOKEN_VALID, refreshToken)
         */
        public static final String REFRESH_TOKEN_VALID = "Refresh token is valid: %s";

        /**
         * Info-level message indicating the start of revoking a refresh token.
         * Usage: String.format(REFRESH_TOKEN_REVOKE_START, refreshToken)
         */
        public static final String REFRESH_TOKEN_REVOKE_START = "Starting to revoke refresh token: %s";

        /**
         * Info-level message indicating successful revocation of a refresh token.
         * Usage: String.format(REFRESH_TOKEN_REVOKED_SUCCESS, refreshToken)
         */
        public static final String REFRESH_TOKEN_REVOKED_SUCCESS = "Refresh token revoked successfully: %s";

        /**
         * Warn-level message when a refresh token to revoke is not found.
         * Usage: String.format(REFRESH_TOKEN_REVOKE_NOT_FOUND, refreshToken)
         */
        public static final String REFRESH_TOKEN_REVOKE_NOT_FOUND = "Refresh token to revoke not found: %s";

        /**
         * Info-level message indicating the start of deleting all refresh tokens.
         * Usage: REFRESH_TOKEN_DELETE_ALL_START
         */
        public static final String REFRESH_TOKEN_DELETE_ALL_START = "Starting deletion of all refresh tokens";

        /**
         * Info-level message indicating successful deletion of all refresh tokens.
         * Usage: REFRESH_TOKEN_DELETE_ALL_SUCCESS
         */
        public static final String REFRESH_TOKEN_DELETE_ALL_SUCCESS = "Successfully deleted all refresh tokens";

        /**
         * Error-level message indicating an error occurred during JWT token extraction.
         * Usage: JWT_EXTRACTION_ERROR
         */
        public static final String JWT_EXTRACTION_ERROR = "Error extracting JWT token";

        /**
         * Error message logged when creating a refresh token fails.
         * Placeholder: userId
         */
        public static final String REFRESH_TOKEN_CREATE_ERROR = "Error creating refresh token for user %s";

        /**
         * Generic exception message thrown when refresh token creation fails.
         */
        public static final String REFRESH_TOKEN_CREATE_EXCEPTION = "Failed to create refresh token";

        /**
         * Error log for unexpected errors during refresh token validation.
         * Placeholder: token string
         */
        public static final String REFRESH_TOKEN_VALIDATE_UNEXPECTED_ERROR = "Unexpected error during refresh token validation: %s";

        /**
         * Exception message for refresh token not found or revoked.
         */
        public static final String ERROR_REFRESH_TOKEN_NOT_FOUND_OR_REVOKED = "Refresh token not found or has been revoked";

        /**
         * Exception message for unexpected error during refresh token validation.
         */
        public static final String ERROR_REFRESH_TOKEN_VALIDATE_UNEXPECTED = "Unexpected error during refresh token validation";

        /**
         * Error log message when an error occurs during refresh token revocation.
         * Placeholder: token string
         */
        public static final String REFRESH_TOKEN_REVOKE_ERROR = "Error revoking refresh token: %s";

        /**
         * Exception message thrown when refresh token revocation fails.
         */
        public static final String ERROR_REFRESH_TOKEN_REVOKE_FAILED = "Failed to revoke refresh token";

        /**
         * Error log message when an error occurs during deletion of all refresh tokens
         * for a user.
         * Placeholder: user ID
         */
        public static final String REFRESH_TOKEN_DELETE_ALL_ERROR = "Error deleting all refresh tokens for user: %s";

        /**
         * Exception message thrown when deletion of all refresh tokens for a user
         * fails.
         */
        public static final String ERROR_REFRESH_TOKEN_DELETE_ALL_FAILED = "Failed to delete all refresh tokens for user";

        /**
         * Template message for successful log level update.
         * Use String.format to insert the logger name.
         */
        public static final String LOG_LEVEL_UPDATE_SUCCESS = "Log level updated for %s";

        /** Message for successful user logout, expects user ID */
        public static final String USER_LOGOUT_SUCCESS = "User logout successful: %s";

        /** Message for failed user logout, expects user ID and failure reason */
        public static final String USER_LOGOUT_FAILED = "User logout failed: %s. Reason: %s";

        /** Message for runtime exception, expects error details */
        public static final String RUNTIME_EXCEPTION = "Runtime exception occurred: %s";

        /** Message for not found exception, expects entity details */
        public static final String NOT_FOUND_EXCEPTION = "Entity not found: %s";

        /**
         * Message returned when user is not authenticated.
         */
        public static final String NOT_AUTHENTICATED = "Not authenticated";

        /**
         * Message returned when the authentication principal is invalid or unexpected.
         */
        public static final String INVALID_AUTH_PRINCIPAL = "Invalid authentication principal";

        /**
         * Message returned when logout operation succeeds.
         */
        public static final String LOGOUT_SUCCESS = "Logged out successfully";

        /**
         * Message returned when logout operation fails.
         */
        public static final String LOGOUT_FAILURE_PREFIX = "Failed to log out: ";

        /**
         * SSO/Provider logging message templates for use in AuthServiceImpl logging.
         * These are used for log.info, log.warn, and log.debug statements.
         */
        public static final String LOG_LDAP_PROVIDER_ENABLED = "LDAP provider enabled: %s";
        public static final String LOG_SAML_PROVIDER_ENABLED = "SAML provider enabled: %s";
        public static final String LOG_OAUTH2_PROVIDER_ENABLED = "OAuth2 provider enabled: %s";
        public static final String LOG_PROVIDERS_LIST_BUILT = "Providers list built, count: %d";
        public static final String LOG_LDAP_AUTH_NOT_ENABLED = "LDAP authentication not enabled for user: %s";
        public static final String LDAP_AUTH_NOT_ENABLED = "LDAP authentication not enabled for user: %s";
        public static final String LDAP_AUTH_FAILED_FALLBACK = "LDAP authentication failed, falling back to database authentication";
        public static final String LDAP_AUTH_FAILED_FOR_USER = "LDAP authentication failed for user: %s";
        public static final String LDAP_AUTH_FAILED_MSG = "LDAP authentication failed";
        public static final String INVALID_CREDENTIALS_MSG = "Invalid credentials";
        public static final String DB_AUTH_FAILED_FOR_USER = "Database authentication failed for user: %s";
        public static final String AUTH_FAILED_MSG = "Authentication failed";
        public static final String LDAP_PROVIDER_CREATION_FAILED = "Failed to create LDAP authentication provider";
        public static final String LDAP_PROVIDER_CREATION_FAILED_MSG = "Failed to create LDAP authentication provider";
        public static final String LDAP_STATUS_CHECK_FAILED = "Failed to check LDAP status";
        public static final String LDAP_CONFIG_GET_FAILED = "Failed to get LDAP configuration";
        public static final String LOG_SAML_AUTH_NOT_ENABLED = "SAML authentication not enabled";
        public static final String LOG_OAUTH2_AUTH_NOT_ENABLED = "OAuth2 authentication not enabled";
        public static final String LOG_LDAP_AUTH_SUCCESS = "LDAP authentication successful for user: %s";
        public static final String LOG_LDAP_AUTH_INVALID = "LDAP authentication failed for user: %s";
        public static final String LOG_SAML_LOGIN_INIT = "Initiating SAML login for provider: %s";
        public static final String LOG_SAML_SSO_INIT = "Initiating SAML SSO for provider: %s";
        public static final String LOG_SAML_SSO_ATTEMPTED_BUT_NOT_ENABLED = "SAML SSO initiation attempted but SAML is not enabled";
        public static final String LOG_SAML_AUTH_RESPONSE_NULL = "SAML authentication response received but authentication is null";
        public static final String LOG_SAML_AUTH_SUCCESS = "SAML authentication successful for user: %s";
        public static final String LOG_OAUTH2_LOGIN_INIT = "Initiating OAuth2 login for provider: %s";
        public static final String LOG_OAUTH2_CALLBACK_ERROR = "OAuth2 callback received error: %s - %s";
        public static final String LOG_OAUTH2_AUTH_SUCCESS = "OAuth2 authentication successful for user: %s";
        public static final String LOG_OAUTH2_CALLBACK_NO_CODE = "OAuth2 callback received without authorization code and without successful authentication";
        public static final String LOG_OAUTH2_CALLBACK_RECEIVED = "OAuth2 callback received with code: %s and state: %s";
        public static final String LOG_SAML_CALLBACK_FAILED = "SAML authentication failed: %s";
        public static final String LOG_SAML_CALLBACK_NO_AUTH = "SAML callback accessed but no valid authentication found";
        public static final String LOG_SAML_CALLBACK_SUCCESS = "SAML authentication callback successful for user: %s";
        public static final String LOG_SAML_PROCESSING_RESPONSE = "Saml2WebSsoAuthenticationFilter - Processing SAML Response";

        /**
         * SSO/Provider API response messages for user-facing responses in
         * AuthServiceImpl.
         * These are used for API responses instead of hardcoded strings.
         */
        public static final String SECURITY_LDAP_PROVIDER_ENABLED_LOG = "Dynamic LDAP authentication provider configured";
        public static final String SECURITY_LDAP_DISABLED_OR_NOT_CONFIGURED_LOG = "Database-only authentication provider configured (LDAP disabled or not configured)";
        public static final String MSG_LDAP_AUTH_SUCCESS = "LDAP authentication successful";
        public static final String MSG_LDAP_AUTH_ERROR = "Invalid LDAP credentials";
        public static final String MSG_LDAP_AUTH_NOT_ENABLED = "LDAP authentication not enabled";
        public static final String MSG_LDAP_AUTH_NOT_CONFIGURED = "LDAP authentication not configured";
        public static final String MSG_SAML_AUTH_SUCCESS = "SAML authentication successful";
        public static final String MSG_SAML_AUTH_NOT_ENABLED = "SAML authentication not enabled";
        public static final String MSG_SAML_AUTH_NOT_AVAILABLE = "SAML authentication not available";
        public static final String MSG_SAML_AUTH_NOT_FOUND = "No valid SAML authentication found";
        public static final String MSG_SAML_AUTH_FAILED = "SAML authentication failed";
        public static final String MSG_SAML_REDIRECT = "Redirect to SAML IdP";
        public static final String MSG_SAML_INIT = "Initiating SAML authentication";
        public static final String MSG_OAUTH2_AUTH_SUCCESS = "OAuth2 authentication successful";
        public static final String MSG_OAUTH2_AUTH_NOT_ENABLED = "OAuth2 authentication not enabled";
        public static final String MSG_OAUTH2_AUTH_FAILED = "OAuth2 authentication failed";
        public static final String MSG_OAUTH2_REDIRECT = "Redirect to OAuth2 provider";
        public static final String MSG_OAUTH2_INVALID_REQUEST = "Authorization code is required";
        public static final String OAUTH2_CONFIG_NOT_FOUND_OR_DISABLED = "OAuth2 configuration not found or disabled for registration ID: %s";
        public static final String OAUTH2_GET_ALL_CLIENT_REGISTRATIONS_FAILED = "Failed to get all OAuth2 client registrations";
        public static final String OAUTH2_STATUS_CHECK_FAILED = "Failed to check OAuth2 status";

        // JWT Utility log messages
        public static final String LOG_JWT_GENERATE_TOKEN = "Generating JWT access token for user: %s";
        public static final String LOG_JWT_GENERATE_REFRESH_TOKEN = "Generating JWT refresh token for user: %s";
        public static final String LOG_JWT_EXTRACT_CLAIMS = "Extracting claims from JWT token";
        public static final String LOG_JWT_VALIDATE_TOKEN = "Validating JWT access token for user: %s";
        public static final String LOG_JWT_VALIDATE_REFRESH_TOKEN = "Validating JWT refresh token for user: %s";
        public static final String LOG_JWT_EXTRACT_USER_ID = "Extracting user ID from JWT token";
        public static final String LOG_JWT_EXTRACT_EMAIL = "Extracting email from JWT token";
        public static final String LOG_JWT_TOKEN_EXPIRED = "JWT token is expired";
        public static final String LOG_JWT_GET_EXPIRATION = "Getting JWT access token expiration";
        public static final String LOG_JWT_GET_REFRESH_EXPIRATION = "Getting JWT refresh token expiration";
        public static final String LOG_JWT_SIGNING_KEY = "Creating JWT signing key";
        public static final String LOG_JWT_GENERATE_TOKEN_SUCCESS = "JWT access token generated successfully for user: %s";
        public static final String LOG_JWT_GENERATE_TOKEN_ERROR = "Error generating JWT access token for user: %s";
        public static final String LOG_JWT_GENERATE_REFRESH_TOKEN_SUCCESS = "JWT refresh token generated successfully for user: %s";
        public static final String LOG_JWT_GENERATE_REFRESH_TOKEN_ERROR = "Error generating JWT refresh token for user: %s";
        public static final String LOG_JWT_VALIDATE_TOKEN_SUCCESS = "JWT access token validated successfully for user: %s";
        public static final String LOG_JWT_VALIDATE_TOKEN_ERROR = "JWT access token validation failed for user: %s";
        public static final String LOG_JWT_VALIDATE_REFRESH_TOKEN_SUCCESS = "JWT refresh token validated successfully for user: %s";
        public static final String LOG_JWT_VALIDATE_REFRESH_TOKEN_ERROR = "JWT refresh token validation failed for user: %s";
        public static final String LOG_JWT_EXTRACT_CLAIMS_SUCCESS = "Claims extracted from JWT token successfully";
        public static final String LOG_JWT_EXTRACT_CLAIMS_ERROR = "Error extracting claims from JWT token";
        public static final String LOG_JWT_EXTRACT_USER_ID_SUCCESS = "User ID extracted from JWT token successfully";
        public static final String LOG_JWT_EXTRACT_USER_ID_ERROR = "Error extracting user ID from JWT token";
        public static final String LOG_JWT_EXTRACT_EMAIL_SUCCESS = "Email extracted from JWT token successfully";
        public static final String LOG_JWT_EXTRACT_EMAIL_ERROR = "Error extracting email from JWT token";

        // JWT Utility log messages for password reset
        public static final String LOG_JWT_GENERATE_PASSWORD_RESET_TOKEN = "Generating password reset JWT for user: %s";
        public static final String LOG_JWT_GENERATE_PASSWORD_RESET_TOKEN_SUCCESS = "Password reset JWT generated for user: %s";
        public static final String LOG_JWT_GENERATE_PASSWORD_RESET_TOKEN_ERROR = "Error generating password reset JWT for user: %s";
        public static final String LOG_JWT_VALIDATE_PASSWORD_RESET_TOKEN = "Validating password reset JWT for user: %s";
        public static final String LOG_JWT_VALIDATE_PASSWORD_RESET_TOKEN_ERROR = "Error validating password reset JWT for user: %s";

        // Trace log messages for AuthServiceImpl methods
        public static final String TRACE_LOGIN_METHOD = "AuthServiceImpl.login() called";
        public static final String TRACE_REFRESH_ACCESS_TOKEN_METHOD = "AuthServiceImpl.refreshAccessToken() called";
        public static final String TRACE_LOGOUT_METHOD = "AuthServiceImpl.logout() called";
        public static final String TRACE_GET_AUTH_PROVIDERS_METHOD = "AuthServiceImpl.getAuthProviders() called";
        public static final String TRACE_LDAP_LOGIN_METHOD = "AuthServiceImpl.ldapLogin() called";
        public static final String TRACE_INITIATE_SAML_LOGIN_METHOD = "AuthServiceImpl.initiateSamlLogin() called";
        public static final String TRACE_INITIATE_SAML_SSO_METHOD = "AuthServiceImpl.initiateSamlSso() called";
        public static final String TRACE_HANDLE_SAML_RESPONSE_METHOD = "AuthServiceImpl.handleSamlResponse() called";
        public static final String TRACE_INITIATE_OAUTH2_LOGIN_METHOD = "AuthServiceImpl.initiateOAuth2Login() called";
        public static final String TRACE_HANDLE_OAUTH2_CALLBACK_GET_METHOD = "AuthServiceImpl.handleOAuth2CallbackGet() called";
        public static final String TRACE_HANDLE_OAUTH2_CALLBACK_METHOD = "AuthServiceImpl.handleOAuth2Callback() called";
        public static final String TRACE_HANDLE_SAML_CALLBACK_METHOD = "AuthServiceImpl.handleSamlCallback() called";
        public static final String SAML_CONFIG_NOT_FOUND_OR_DISABLED = "SAML configuration not found or disabled for registration ID: %s";
        public static final String SAML_RELYING_PARTY_REGISTRATION_FAILED = "Failed to find SAML relying party registration for ID: %s";
        public static final String SAML_CONFIG_MAPPING_FAILED = "Failed to map SAML config to RelyingPartyRegistration";
        public static final String SAML_GET_ALL_RELYING_PARTY_REGISTRATIONS_FAILED = "Failed to get all SAML relying party registrations";
        public static final String SAML_STATUS_CHECK_FAILED = "Failed to check SAML status";
        /** Trace-level message for starting password update process. */
        public static final String TRACE_UPDATE_PASSWORD_METHOD = "AuthServiceImpl.updatePassword() called for user: %s";
        /** Warn-level message for invalid old password during password update. */
        public static final String INVALID_OLD_PASSWORD = "Invalid old password for user: %s";
        /** Error message for invalid old password (user-facing). */
        public static final String INVALID_OLD_PASSWORD_MSG = "Old password is incorrect.";
        /** Info-level message for successful password update. */
        public static final String PASSWORD_UPDATE_SUCCESS = "Password updated successfully for user: %s";
        /** Success message for password update (user-facing). */
        public static final String PASSWORD_UPDATE_SUCCESS_MSG = "Password updated successfully.";
        /** Error log message for unexpected error during password update. */
        public static final String PASSWORD_UPDATE_ERROR = "Unexpected error during password update for user: %s";
        /** User-facing error message for unexpected password update error. */
        public static final String PASSWORD_UPDATE_ERROR_MSG = "An unexpected error occurred while updating the password. Please try again later.";
        /** Error message for invalid password . */
        public static final String INVALID_OLD_PASSWORD_TITLE = "Invalid Old Password";

        // Password reset flow messages
        public static final String TRACE_PASSWORD_RESET_REQUEST = "AuthServiceImpl.requestPasswordReset() called for email: %s";
        public static final String PASSWORD_RESET_EMAIL_SENT = "Password reset email sent to: %s";
        public static final String PASSWORD_RESET_EMAIL_FAILED = "Failed to send password reset email to: %s";
        public static final String PASSWORD_RESET_TOKEN_CREATED = "Password reset token created for user: %s";
        public static final String PASSWORD_RESET_USER_NOT_FOUND = "No user found with email: %s for password reset";
        public static final String PASSWORD_RESET_GENERIC_MSG = "A password reset link has been sent.";
        public static final String PASSWORD_RESET_EMAIL_SUBJECT = "Password Reset Request";
        public static final String PASSWORD_RESET_EMAIL_CONTENT = "<p>To reset your password, click the button below:</p>"
                        + "<a href='%s' style='padding:10px 20px;background:#1976d2;color:#fff;text-decoration:none;border-radius:4px;'>Reset Password</a>"
                        + "<p>If you did not request a password reset, you can ignore this email.</p>";
        public static final String PASSWORD_RESET_UNEXPECTED_ERROR = "Unexpected error in requestPasswordReset for email: %s";

        // Password reset completion flow
        public static final String TRACE_RESET_PASSWORD = "AuthServiceImpl.resetPassword() called for token: %s";
        public static final String PASSWORD_RESET_TOKEN_NOT_FOUND = "Password reset token not found: %s";
        public static final String PASSWORD_RESET_TOKEN_EXPIRED = "Password reset token expired: %s";
        public static final String PASSWORD_RESET_SUCCESS = "Password reset successful for user: %s";
        public static final String PASSWORD_RESET_SUCCESS_MSG = "Your password has been reset successfully.";
        public static final String PASSWORD_RESET_LINK_EXPIRED_MSG = "The password reset link has expired. Please request a new one.";

        // SSO Configuration Management Messages
        public static final String SSO_PRELOAD_START_INFO = "Preloading all SSO configurations into cache...";
        public static final String SSO_PRELOAD_START_TRACE = "Starting preload operation for all SSO configurations";
        public static final String SSO_PRELOAD_COMPLETED_INFO = "SSO configurations preloading completed";
        public static final String SSO_PRELOAD_COMPLETED_TRACE = "Preload operation for all SSO configurations completed";

        // LDAP Configuration Messages
        public static final String LDAP_PRELOAD_ATTEMPT_TRACE = "Attempting to preload LDAP configuration";
        public static final String LDAP_PRELOAD_SUCCESS_DEBUG = "Preloaded LDAP configuration into cache";
        public static final String LDAP_PRELOAD_SUCCESS_TRACE = "LDAP configuration successfully preloaded and cached";
        public static final String LDAP_PRELOAD_NOT_FOUND_TRACE = "No LDAP configuration found to preload";
        public static final String LDAP_PRELOAD_FAILED_WARN = "Failed to preload LDAP configuration";
        public static final String LDAP_PRELOAD_FAILED_TRACE = "LDAP configuration preload failed: %s";

        public static final String LDAP_LOAD_START_TRACE = "Loading LDAP configuration from configuration service";
        public static final String LDAP_LOAD_FOUND_TRACE = "LDAP configuration data found in service, mapping to config object";
        public static final String LDAP_LOAD_MAPPED_TRACE = "LDAP configuration successfully mapped from JSON with isActive: %s";
        public static final String LDAP_LOAD_NOT_FOUND_DEBUG = "LDAP configuration not found in configuration service";
        public static final String LDAP_LOAD_NOT_FOUND_TRACE = "No LDAP configuration data found in service";
        public static final String LDAP_LOAD_FAILED_ERROR = "Failed to load LDAP configuration from configuration service";
        public static final String LDAP_LOAD_FAILED_TRACE = "Exception during LDAP configuration load: %s";

        public static final String LDAP_GET_CACHE_CHECK_TRACE = "Retrieving LDAP configuration - checking cache first";
        public static final String LDAP_GET_CACHE_HIT_DEBUG = "Returning cached LDAP configuration";
        public static final String LDAP_GET_CACHE_HIT_TRACE = "LDAP configuration retrieved from cache successfully";
        public static final String LDAP_GET_CACHE_MISS_TRACE = "LDAP configuration not in cache or expired, loading from source";
        public static final String LDAP_GET_LOADED_TRACE = "LDAP configuration loaded from source and cached";
        public static final String LDAP_GET_NOT_FOUND_TRACE = "No LDAP configuration found in source";

        public static final String LDAP_SAVE_START_TRACE = "Starting LDAP configuration save operation";
        public static final String LDAP_SAVE_SERIALIZED_TRACE = "LDAP configuration serialized to JSON for storage";
        public static final String LDAP_SAVE_SUCCESS_INFO = "LDAP configuration saved successfully";
        public static final String LDAP_SAVE_SUCCESS_TRACE = "LDAP configuration persisted to configuration service";
        public static final String LDAP_SAVE_CACHE_CLEARED_TRACE = "LDAP configuration cache cleared after save";
        public static final String LDAP_SAVE_AUDIT_SUCCESS = "LDAP configuration updated - enabled: %s, name: %s";
        public static final String LDAP_SAVE_FAILED_ERROR = "Failed to save LDAP configuration";
        public static final String LDAP_SAVE_FAILED_TRACE = "LDAP configuration save operation failed: %s";
        public static final String LDAP_SAVE_AUDIT_FAILED = "LDAP configuration update failed: %s";
        public static final String LDAP_SAVE_EXCEPTION = "Failed to save LDAP configuration";

        // SAML Configuration Messages
        public static final String SAML_PRELOAD_ATTEMPT_TRACE = "Attempting to preload SAML configuration";
        public static final String SAML_PRELOAD_SUCCESS_DEBUG = "Preloaded SAML configuration into cache";
        public static final String SAML_PRELOAD_SUCCESS_TRACE = "SAML configuration successfully preloaded and cached";
        public static final String SAML_PRELOAD_NOT_FOUND_TRACE = "No SAML configuration found to preload";
        public static final String SAML_PRELOAD_FAILED_WARN = "Failed to preload SAML configuration";
        public static final String SAML_PRELOAD_FAILED_TRACE = "SAML configuration preload failed: %s";

        public static final String SAML_LOAD_START_TRACE = "Loading SAML configuration from configuration service";
        public static final String SAML_LOAD_FOUND_TRACE = "SAML configuration data found in service, mapping to config object";
        public static final String SAML_LOAD_MAPPED_TRACE = "SAML configuration successfully mapped from JSON with isActive: %s";
        public static final String SAML_LOAD_NOT_FOUND_DEBUG = "SAML configuration not found in configuration service";
        public static final String SAML_LOAD_NOT_FOUND_TRACE = "No SAML configuration data found in service";
        public static final String SAML_LOAD_FAILED_ERROR = "Failed to load SAML configuration from configuration service";
        public static final String SAML_LOAD_FAILED_TRACE = "Exception during SAML configuration load: %s";

        public static final String SAML_GET_CACHE_CHECK_TRACE = "Retrieving SAML configuration - checking cache first";
        public static final String SAML_GET_CACHE_HIT_DEBUG = "Returning cached SAML configuration";
        public static final String SAML_GET_CACHE_HIT_TRACE = "SAML configuration retrieved from cache successfully";
        public static final String SAML_GET_CACHE_MISS_TRACE = "SAML configuration not in cache or expired, loading from source";
        public static final String SAML_GET_LOADED_TRACE = "SAML configuration loaded from source and cached";
        public static final String SAML_GET_NOT_FOUND_TRACE = "No SAML configuration found in source";

        public static final String SAML_SAVE_START_TRACE = "Starting SAML configuration save operation";
        public static final String SAML_SAVE_SERIALIZED_TRACE = "SAML configuration serialized to JSON for storage";
        public static final String SAML_SAVE_SUCCESS_INFO = "SAML configuration saved successfully";
        public static final String SAML_SAVE_SUCCESS_TRACE = "SAML configuration persisted to configuration service";
        public static final String SAML_SAVE_CACHE_CLEARED_TRACE = "SAML configuration cache cleared after save";
        public static final String SAML_SAVE_AUDIT_SUCCESS = "SAML configuration updated - enabled: %s, name: %s";
        public static final String SAML_SAVE_FAILED_ERROR = "Failed to save SAML configuration";
        public static final String SAML_SAVE_FAILED_TRACE = "SAML configuration save operation failed: %s";
        public static final String SAML_SAVE_AUDIT_FAILED = "SAML configuration update failed: %s";
        public static final String SAML_SAVE_EXCEPTION = "Failed to save SAML configuration";

        // OAuth2 Configuration Messages
        public static final String OAUTH2_PRELOAD_ATTEMPT_TRACE = "Attempting to preload OAuth2 configuration";
        public static final String OAUTH2_PRELOAD_SUCCESS_DEBUG = "Preloaded OAuth2 configuration into cache";
        public static final String OAUTH2_PRELOAD_SUCCESS_TRACE = "OAuth2 configuration successfully preloaded and cached";
        public static final String OAUTH2_PRELOAD_NOT_FOUND_TRACE = "No OAuth2 configuration found to preload";
        public static final String OAUTH2_PRELOAD_FAILED_WARN = "Failed to preload OAuth2 configuration";
        public static final String OAUTH2_PRELOAD_FAILED_TRACE = "OAuth2 configuration preload failed: %s";

        public static final String OAUTH2_LOAD_START_TRACE = "Loading OAuth2 configuration from configuration service";
        public static final String OAUTH2_LOAD_FOUND_TRACE = "OAuth2 configuration data found in service, mapping to config object";
        public static final String OAUTH2_LOAD_MAPPED_TRACE = "OAuth2 configuration successfully mapped from JSON with isActive: %s";
        public static final String OAUTH2_LOAD_NOT_FOUND_DEBUG = "OAuth2 configuration not found in configuration service";
        public static final String OAUTH2_LOAD_NOT_FOUND_TRACE = "No OAuth2 configuration data found in service";
        public static final String OAUTH2_LOAD_FAILED_ERROR = "Failed to load OAuth2 configuration from configuration service";
        public static final String OAUTH2_LOAD_FAILED_TRACE = "Exception during OAuth2 configuration load: %s";

        public static final String OAUTH2_GET_CACHE_CHECK_TRACE = "Retrieving OAuth2 configuration - checking cache first";
        public static final String OAUTH2_GET_CACHE_HIT_DEBUG = "Returning cached OAuth2 configuration";
        public static final String OAUTH2_GET_CACHE_HIT_TRACE = "OAuth2 configuration retrieved from cache successfully";
        public static final String OAUTH2_GET_CACHE_MISS_TRACE = "OAuth2 configuration not in cache or expired, loading from source";
        public static final String OAUTH2_GET_LOADED_TRACE = "OAuth2 configuration loaded from source and cached";
        public static final String OAUTH2_GET_NOT_FOUND_TRACE = "No OAuth2 configuration found in source";

        public static final String OAUTH2_SAVE_START_TRACE = "Starting OAuth2 configuration save operation";
        public static final String OAUTH2_SAVE_SERIALIZED_TRACE = "OAuth2 configuration serialized to JSON for storage";
        public static final String OAUTH2_SAVE_SUCCESS_INFO = "OAuth2 configuration saved successfully";
        public static final String OAUTH2_SAVE_SUCCESS_TRACE = "OAuth2 configuration persisted to configuration service";
        public static final String OAUTH2_SAVE_CACHE_CLEARED_TRACE = "OAuth2 configuration cache cleared after save";
        public static final String OAUTH2_SAVE_AUDIT_SUCCESS = "OAuth2 configuration updated - enabled: %s, name: %s, providerId: %s";
        public static final String OAUTH2_SAVE_FAILED_ERROR = "Failed to save OAuth2 configuration";
        public static final String OAUTH2_SAVE_FAILED_TRACE = "OAuth2 configuration save operation failed: %s";
        public static final String OAUTH2_SAVE_AUDIT_FAILED = "OAuth2 configuration update failed: %s";
        public static final String OAUTH2_SAVE_EXCEPTION = "Failed to save OAuth2 configuration";

        // SSO Controller Messages
        public static final String SSO_GET_ALL_REQUEST_TRACE = "Received request to get all SSO configurations";
        public static final String SSO_GET_ALL_RETRIEVING_TRACE = "Retrieving LDAP, SAML, and OAuth2 configurations";
        public static final String SSO_GET_ALL_SUCCESS_TRACE = "All SSO configurations retrieved successfully";
        public static final String SSO_GET_ALL_AUDIT = "All SSO configurations viewed via API";

        public static final String SSO_SAVE_LDAP_REQUEST_TRACE = "Received request to save LDAP configuration";
        public static final String SSO_SAVE_LDAP_CALLING_TRACE = "Calling dynamic SSO config service to save LDAP configuration";
        public static final String SSO_SAVE_LDAP_SUCCESS_MSG = "LDAP configuration saved successfully";
        public static final String SSO_SAVE_LDAP_SUCCESS_TRACE = "LDAP configuration save request completed successfully";
        public static final String SSO_SAVE_LDAP_AUDIT_SUCCESS = "LDAP configuration saved via API - enabled: %s";
        public static final String SSO_SAVE_LDAP_FAILED_TRACE = "LDAP configuration save request failed: %s";
        public static final String SSO_SAVE_LDAP_FAILED_MSG = "Failed to save LDAP configuration: %s";
        public static final String SSO_SAVE_LDAP_AUDIT_FAILED = "LDAP configuration save via API failed: %s";

        public static final String SSO_SAVE_SAML_REQUEST_TRACE = "Received request to save SAML configuration";
        public static final String SSO_SAVE_SAML_CALLING_TRACE = "Calling dynamic SSO config service to save SAML configuration";
        public static final String SSO_SAVE_SAML_SUCCESS_MSG = "SAML configuration saved successfully";
        public static final String SSO_SAVE_SAML_SUCCESS_TRACE = "SAML configuration save request completed successfully";
        public static final String SSO_SAVE_SAML_AUDIT_SUCCESS = "SAML configuration saved via API - enabled: %s";
        public static final String SSO_SAVE_SAML_FAILED_TRACE = "SAML configuration save request failed: %s";
        public static final String SSO_SAVE_SAML_FAILED_MSG = "Failed to save SAML configuration: %s";
        public static final String SSO_SAVE_SAML_AUDIT_FAILED = "SAML configuration save via API failed: %s";

        public static final String SSO_SAVE_OAUTH2_REQUEST_TRACE = "Received request to save OAuth2 configuration";
        public static final String SSO_SAVE_OAUTH2_CALLING_TRACE = "Calling dynamic SSO config service to save OAuth2 configuration";
        public static final String SSO_SAVE_OAUTH2_SUCCESS_MSG = "OAuth2 configuration saved successfully";
        public static final String SSO_SAVE_OAUTH2_SUCCESS_TRACE = "OAuth2 configuration save request completed successfully";
        public static final String SSO_SAVE_OAUTH2_AUDIT_SUCCESS = "OAuth2 configuration saved via API - enabled: %s";
        public static final String SSO_SAVE_OAUTH2_FAILED_TRACE = "OAuth2 configuration save request failed: %s";
        public static final String SSO_SAVE_OAUTH2_FAILED_MSG = "Failed to save OAuth2 configuration: %s";
        public static final String SSO_SAVE_OAUTH2_AUDIT_FAILED = "OAuth2 configuration save via API failed: %s";

        // Common status messages
        public static final String STATUS_SUCCESS = "success";
        public static final String STATUS_ERROR = "error";
        // Common cache keys
        public static final String CACHE_KEY_DEFAULT = "default";

        // Common field names
        public static final String FIELD_UNNAMED = "unnamed";
        public static final String FIELD_UNKNOWN = "unknown";

        // SAML user-friendly error messages
        public static final String SAML_CERTIFICATE_ERROR_MSG = "SAML signature or certificate validation failed. Please contact your administrator.";
        public static final String SAML_GENERIC_ERROR_MSG = "SAML authentication failed. Please try again or contact support.";

        // --- ConfigurationEncryptionService trace and error messages ---
        public static final String ENCRYPTION_FULL_APPLIED_TRACE = "Applying full encryption for %s/%s";
        public static final String ENCRYPTION_FIELD_LEVEL_APPLIED_TRACE = "Applying field-level encryption for %s/%s";
        public static final String ENCRYPTION_NO_RULE_TRACE = "No encryption rule, checking for any sensitive fields for %s/%s";
        public static final String DECRYPTION_FULL_APPLIED_TRACE = "Decrypting full configuration for %s/%s";
        public static final String DECRYPTION_FIELD_LEVEL_APPLIED_TRACE = "Decrypting sensitive fields for %s/%s";

        public static final String CONFIGURATION_IN_SERVICE_CHECK_FAILED_WARN = "Failed to check configuration service for provider: ";
        public static final String UNKNOWN_PROVIDER_FOR_EXISTENCE_CHECK_WARN = "Unknown provider for existence check: ";
        public static final String PROVIDER_CONFIG_CHECK_FAILED_ERROR = "Failed to check existence of configuration for provider: ";
        public static final String PROVIDER_CONFIG_SUMMARY_FAILED_ERROR = "Failed to generate provider configuration summary";
        public static final String UNKNOWN_PROVIDER_FOR_DELETION_WARN = "Unknown provider for deletion: ";
        public static final String UNKNOWN_PROVIDER_FOR_DISABLING_WARN = "Unknown provider for disabling: ";
        public static final String EXTRACT_USER_ID_FROM_AUTH_FAILED_WARN = "Failed to extract user ID from authentication context: ";

        // Logger message constants for delete/disable/update operations
        public static final String DELETE_OPERATION_START_TRACE = "Starting delete operation for %s configuration";
        public static final String DELETE_OPERATION_FAILED_UNKNOWN_PROVIDER_TRACE = "Delete operation failed - unknown provider: %s";
        public static final String DELETE_ATTEMPT_INFO = "Attempting to delete %s configuration (key: %s)";
        public static final String DELETE_ATTEMPT_TRACE = "Deleting %s configuration with key: %s";
        public static final String DELETE_SUCCESS_TRACE = "%s configuration successfully deleted from service";
        public static final String DELETE_SUCCESS_INFO = "Successfully deleted %s configuration";
        public static final String DELETE_NOT_FOUND_WARN = "Configuration for %s was not found or could not be deleted";
        public static final String DELETE_NOT_FOUND_TRACE = "%s configuration deletion failed - not found or couldn't delete";
        public static final String DELETE_FAILED_ERROR = "Failed to delete configuration for provider: ";
        public static final String DELETE_FAILED_TRACE = "Exception during %s configuration deletion: %s";
        public static final String DISABLE_OPERATION_START_TRACE = "Starting disable operation for %s configuration";
        public static final String DISABLE_OPERATION_FAILED_UNKNOWN_PROVIDER_TRACE = "Disable operation failed - unknown provider: %s";
        public static final String DISABLE_ATTEMPT_INFO = "Attempting to disable %s configuration";
        public static final String DISABLE_NOT_FOUND_WARN = "Configuration for %s was not found";
        public static final String DISABLE_NOT_FOUND_TRACE = "%s configuration disable failed - configuration not found";
        public static final String DISABLE_FAILED_ERROR = "Failed to disable configuration for provider: ";
        public static final String DISABLE_FAILED_TRACE = "Exception during %s configuration disable: %s";
        public static final String LDAP_DISABLE_FOUND_TRACE = "LDAP configuration found, disabling it";
        public static final String LDAP_DISABLE_SUCCESS_INFO = "LDAP configuration disabled successfully";
        public static final String LDAP_DISABLE_SUCCESS_TRACE = "LDAP configuration disable operation completed";
        public static final String LDAP_DISABLE_AUDIT = "LDAP configuration disabled";
        public static final String LDAP_DISABLE_NOT_FOUND_TRACE = "LDAP configuration not found for disable operation";
        public static final String SAML_DISABLE_FOUND_TRACE = "SAML configuration found, disabling it";
        public static final String SAML_DISABLE_SUCCESS_INFO = "SAML configuration disabled successfully";
        public static final String SAML_DISABLE_SUCCESS_TRACE = "SAML configuration disable operation completed";
        public static final String SAML_DISABLE_AUDIT = "SAML configuration disabled";
        public static final String SAML_DISABLE_NOT_FOUND_TRACE = "SAML configuration not found for disable operation";
        public static final String OAUTH2_DISABLE_FOUND_TRACE = "OAuth2 configuration found, disabling it";
        public static final String OAUTH2_DISABLE_SUCCESS_INFO = "OAuth2 configuration disabled successfully";
        public static final String OAUTH2_DISABLE_SUCCESS_TRACE = "OAuth2 configuration disable operation completed";
        public static final String OAUTH2_DISABLE_AUDIT = "OAuth2 configuration disabled";
        public static final String OAUTH2_DISABLE_NOT_FOUND_TRACE = "OAuth2 configuration not found for disable operation";
        public static final String UPDATE_ISACTIVE_TRACE = "Updated isActive field to %s for configuration %s";
        public static final String UPDATE_ISACTIVE_NOT_FOUND_WARN = "Configuration entity not found for key: %s";
        public static final String UPDATE_ISACTIVE_FAILED_ERROR = "Failed to update isActive status for configuration %s";

        // SAML and SSO callback constants for query params and user messages
        public static final String TOKEN_QUERY_PARAM = "token";
        public static final String REFRESH_QUERY_PARAM = "refresh";

        // --- KeyManagementServiceImpl log and error messages ---
        public static final String LOG_INIT_MASTER_KEY_INFRA = "Initializing master encryption key and storage infrastructure...";
        public static final String LOG_MASTER_KEY_LOADED = "Master Key successfully loaded and decrypted from %s";
        public static final String LOG_MASTER_KEY_NOT_FOUND = "Master Key file not found. Generating new master key...";
        public static final String LOG_MASTER_KEY_GENERATED = "New Master Key generated and stored successfully.";
        public static final String LOG_MASTER_KEY_INIT_FATAL = "FATAL: Could not initialize Master Key. Halting.";
        public static final String ERR_MASTER_KEY_INIT = "Failed to initialize Master Key";
        public static final String LOG_INIT_STORAGE_INFRA = "Initializing storage infrastructure...";
        public static final String LOG_STORAGE_INFRA_COMPLETE = "Storage infrastructure initialization complete.";
        public static final String LOG_CONFIG_KEY_DB_LOAD = "Loading configuration key for type '%s' from database.";
        public static final String ERR_DECRYPT_CONFIG_KEY = "Failed to decrypt configuration key for type: %s";
        public static final String LOG_NO_ACTIVE_KEY = "No active key found for type '%s'. Generating a new one.";
        public static final String ERR_GENERATE_SAVE_KEY = "Failed to generate and save new key for type: %s";
        public static final String LOG_MASTER_KEY_ROTATE_INIT = "--- MASTER KEY ROTATION INITIATED --- This is a highly sensitive operation.";
        public static final String LOG_MASTER_KEY_ROTATE_STEP1 = "Step 1/5: New master key generated in memory.";
        public static final String LOG_MASTER_KEY_ROTATE_STEP2 = "Step 2/5: Found %s configuration keys to re-encrypt.";
        public static final String LOG_MASTER_KEY_ROTATE_STEP3 = "Step 3/5: All configuration keys have been re-encrypted with the new master key.";
        public static final String LOG_MASTER_KEY_ROTATE_STEP4_BACKUP = "Step 4/5: Old master key backed up to %s";
        public static final String LOG_MASTER_KEY_ROTATE_STEP4_WRITE = "Step 4/5: New master key has been written to the filesystem.";
        public static final String LOG_MASTER_KEY_ROTATE_STEP5 = "Step 5/5: In-memory master key updated and configuration key cache cleared.";
        public static final String LOG_MASTER_KEY_ROTATE_SUCCESS = "--- MASTER KEY ROTATION COMPLETED SUCCESSFULLY ---";
        public static final String LOG_MASTER_KEY_ROTATE_FATAL = "FATAL: Master key rotation failed. System may be in an inconsistent state. Manual intervention required.";
        public static final String ERR_MASTER_KEY_ROTATE = "Master key rotation failed catastrophically.";
        public static final String LOG_DERIVE_BOOTSTRAP_KEY = "Deriving bootstrap key from system fingerprint...";
        public static final String LOG_CREATE_APP_DIR = "Creating application directory: %s";
        public static final String LOG_DIR_CREATED = "Directory created successfully: %s";
        public static final String LOG_DIR_EXISTS_RACE = "Directory already exists (race condition): %s";
        public static final String LOG_DIR_ALREADY_EXISTS = "Directory already exists: %s";
        public static final String LOG_SET_PERMISSIONS_FAIL = "Failed to set permissions on directory: %s. This may not be critical on all systems";
        public static final String LOG_UNEXPECTED_CREATE_DIR = "Unexpected error creating directory: %s";
        public static final String ERR_UNEXPECTED_CREATE_DIR = "Unexpected error creating directory: %s";
        public static final String LOG_INSTALL_UUID_NOT_FOUND = "Installation UUID file not found. Generating a new one at: %s";
        public static final String LOG_INSTALL_UUID_SAVED = "Successfully generated and saved new Installation UUID.";
        public static final String LOG_INSTALL_UUID_SAVED_DIRECT = "Successfully generated and saved new Installation UUID (direct write).";
        public static final String LOG_INSTALL_UUID_EXISTS = "Installation UUID file already exists: %s";
        public static final String LOG_EXISTING_UUID_EMPTY = "Existing UUID file is empty, regenerating...";
        public static final String LOG_EXISTING_UUID_VALID = "Existing UUID validated successfully";
        public static final String LOG_EXISTING_UUID_INVALID = "Existing UUID file contains invalid UUID, regenerating...";
        public static final String LOG_SET_PERMISSIONS_UUID_FAIL = "Failed to set permissions on UUID file: %s. This may not be critical on all systems.";
        public static final String LOG_SET_PERMISSIONS = "Set POSIX permissions '%s' on %s";
        public static final String LOG_POSIX_NOT_SUPPORTED = "POSIX permissions not supported on current file system for path: %s";
        public static final String LOG_CANNOT_SET_POSIX = "Cannot set POSIX permissions on the current OS/file system. Please ensure that the file/directory ACLs for '%s' are set manually to restrict access. Reason: %s";
        public static final String LOG_UNEXPECTED_SET_PERMISSIONS = "Unexpected error setting permissions on %s";
        public static final String ERR_UNEXPECTED_SET_PERMISSIONS = "Unexpected error setting permissions: %s";
        public static final String LOG_RAW_MASTER_KEY_CLEARED = "Raw master key has been cleared from memory.";
        public static final String LOG_MASTER_SECRET_FROM_PROPS = "Using master secret from properties";
        public static final String ERR_MASTER_SECRET_NOT_FOUND = "Master secret not found in properties. Please set 'encryption.master_key.master_secret' in application properties";
        public static final String LOG_HOSTNAME_FALLBACK = "Could not determine hostname, using 'localhost' as fallback.";
        public static final String LOG_MAC_FALLBACK = "Could not determine MAC address.";
        public static final String LOG_SYSTEM_FINGERPRINT_MASTER_SECRET = "  - Master Secret: [PRESENT]";
        public static final String LOG_ATOMIC_MOVE_NOT_SUPPORTED = "Atomic move not supported, using direct write";
        public static final String ERR_PATH_NOT_DIRECTORY = "Path exists but is not a directory: %s";
        public static final String ERR_CANNOT_CREATE_DIR_PARENT = "Cannot create directory, parent path doesn't exist: %s";
        public static final String ERR_FAILED_CREATE_DIR = "Failed to create directory: %s";
        public static final String ERR_CANNOT_SET_PERMISSIONS = "Cannot set permissions, file doesn't exist: %s";
        public static final String ERR_FAILED_SET_PERMISSIONS = "Failed to set permissions on: %s";
        public static final String ERR_CANNOT_CREATE_UUID_PARENT = "Cannot create UUID file, parent directory doesn't exist: %s";
        public static final String ERR_FAILED_CREATE_UUID = "Failed to create UUID file: %s";
        public static final String ERR_CANNOT_VALIDATE_UUID = "Cannot validate existing UUID file: %s";

        // --- KeyManagementServiceImpl Logger Configuration ---
        public static final String KEY_MANAGEMENT_LOGGER_NAME = "EncKeyManagement";
        public static final String KEY_MANAGEMENT_LOGGER_CATEGORY = "encryption-key";
        public static final String KEY_MANAGEMENT_CATEGORY = "key-management";

        // --- KeyManagementServiceImpl Additional Log Messages ---
        public static final String LOG_STORAGE_INFRA_INIT = "Initializing storage infrastructure";
        public static final String LOG_MASTER_KEY_LOADED_SUCCESS = "Master key loaded successfully";
        public static final String LOG_NEW_MASTER_KEY_GENERATED_SUCCESS = "New master key generated successfully";
        public static final String LOG_CONFIG_KEY_LOADING = "Loading configuration key from database";
        public static final String LOG_NEW_CONFIG_KEY_GENERATING = "No active key found, generating new one";
        public static final String LOG_MASTER_KEY_ROTATION_INITIATED = "Master key rotation initiated";
        public static final String LOG_NEW_MASTER_KEY_FOR_ROTATION = "New master key generated for rotation";
        public static final String LOG_RE_ENCRYPTING_CONFIG_KEYS = "Re-encrypting configuration keys with new master key";
        public static final String LOG_CONFIG_KEYS_RE_ENCRYPTED = "Configuration keys re-encrypted and saved";
        public static final String LOG_OLD_MASTER_KEY_BACKED_UP = "Old master key backed up";
        public static final String LOG_NEW_MASTER_KEY_WRITTEN = "New master key written to storage";
        public static final String LOG_MASTER_KEY_ROTATION_STEP5 = "Master key rotation step 5 complete";
        public static final String LOG_MASTER_KEY_ROTATION_SUCCESS = "Master key rotation completed successfully";
        public static final String LOG_FATAL_MASTER_KEY_ROTATION = "Fatal error during master key rotation";
        public static final String LOG_DERIVING_BOOTSTRAP_KEY = "Deriving bootstrap key";
        public static final String LOG_CREATING_APP_DIRECTORY = "Creating application directory";
        public static final String LOG_MASTER_KEY_GENERATED_STORED = "Master key generated and stored successfully";
        public static final String LOG_FAILED_CREATE_DIR_ACCESS_DENIED = "Failed to create directory - access denied";
        public static final String LOG_CANNOT_CREATE_DIR_NO_PARENT = "Cannot create directory - parent path does not exist";
        public static final String LOG_FAILED_CREATE_DIR_FILESYSTEM = "Failed to create directory - filesystem error";
        public static final String LOG_FAILED_CREATE_DIR_IO = "Failed to create directory - IO error";
        public static final String LOG_FAILED_SET_DIR_PERMISSIONS = "Failed to set permissions on directory";
        public static final String LOG_UNEXPECTED_DIR_CREATION = "Unexpected error during directory creation";
        public static final String LOG_UUID_FILE_NOT_FOUND = "Install UUID file not found";
        public static final String LOG_UUID_SAVED_SUCCESS = "Install UUID saved successfully";
        public static final String LOG_UUID_SAVED_DIRECT = "Install UUID saved directly";
        public static final String LOG_ATOMIC_MOVE_FALLBACK = "Atomic move not supported, falling back to direct write";
        public static final String LOG_FAILED_CREATE_UUID_ACCESS_DENIED = "Failed to create UUID file - access denied";
        public static final String LOG_CANNOT_CREATE_UUID_NO_PARENT = "Cannot create UUID file - parent directory does not exist";
        public static final String LOG_FAILED_CREATE_UUID_FILESYSTEM = "Failed to create UUID file - filesystem error";
        public static final String LOG_FAILED_CREATE_UUID_IO = "Failed to create UUID file - IO error";
        public static final String LOG_EXISTING_UUID_INVALID_FORMAT = "Existing UUID is invalid format";
        public static final String LOG_CANNOT_VALIDATE_UUID_FILE = "Cannot validate UUID file";
        public static final String LOG_FAILED_SET_UUID_PERMISSIONS = "Failed to set permissions on UUID file";
        public static final String LOG_UNEXPECTED_UUID_CREATION = "Unexpected error during UUID file creation";
        public static final String LOG_FAILED_PARSE_PERMISSIONS = "Failed to parse permissions string";
        public static final String LOG_FAILED_SET_PERMISSIONS_ACCESS_DENIED = "Failed to set permissions - access denied";
        public static final String LOG_CANNOT_SET_PERMISSIONS_NO_FILE = "Cannot set permissions - file does not exist";
        public static final String LOG_FAILED_SET_PERMISSIONS_IO = "Failed to set permissions - IO error";
        public static final String LOG_CANNOT_SET_POSIX_PERMISSIONS = "Cannot set POSIX permissions on this filesystem";

        // --- KeyManagementServiceImpl Constants ---
        public static final String MASTER_KEY_ALGORITHM = "AES";
        public static final String AES_ALGORITHM = "AES";
        public static final String BACKUP_FILE_EXTENSION = ".bak";
        public static final String TEMP_FILE_EXTENSION = ".tmp";
        public static final String MASTER_SECRET_PRESENT = "  - Master Secret: [PRESENT]";
        public static final String PERMISSIONS_700 = "rwx------";
        public static final String PERMISSIONS_600 = "rw-------";

        // Common algorithm and encryption constants
        public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
        public static final String ENCRYPTION_ALGORITHM_AES_GCM = "AES/GCM/NoPadding";
        public static final int PBKDF2_ITERATIONS = 100000;
        public static final int AES_KEY_LENGTH_BITS = 256;
        public static final int GCM_IV_LENGTH_BYTES = 12;
        public static final int GCM_TAG_LENGTH_BITS = 128;

        // --- LdapAuthenticationConfig log and error messages ---
        public static final String LDAP_DISABLED_OR_NOT_CONFIGURED = "LDAP is disabled or not configured, using dummy context source";
        public static final String LDAP_CONFIG_INVALID_OR_INCOMPLETE = "LDAP configuration is invalid or incomplete, using dummy context source";
        public static final String LDAP_CONTEXT_SOURCE_FAILED = "Failed to create LDAP context source, falling back to dummy context source";
        public static final String LDAP_DISABLED_OR_NOT_CONFIGURED_BIND_AUTH = "LDAP is disabled or not configured, using dummy bind authenticator";
        public static final String LDAP_CONFIG_INVALID_OR_INCOMPLETE_BIND_AUTH = "LDAP configuration is invalid or incomplete, using dummy bind authenticator";
        public static final String LDAP_BIND_AUTH_FAILED = "Failed to create LDAP bind authenticator, falling back to dummy authenticator";
        public static final String LDAP_DISABLED_OR_NOT_CONFIGURED_USER_SEARCH = "LDAP is disabled or not configured, using dummy user search";
        public static final String LDAP_CONFIG_INVALID_OR_INCOMPLETE_USER_SEARCH = "LDAP configuration is invalid or incomplete, using dummy user search";
        public static final String LDAP_USER_SEARCH_FAILED = "Failed to create LDAP user search, falling back to dummy search";
        public static final String LDAP_DISABLED_OR_NOT_CONFIGURED_AUTHORITIES = "LDAP is disabled or not configured, using dummy authorities populator";
        public static final String LDAP_CONFIG_INVALID_OR_INCOMPLETE_AUTHORITIES = "LDAP configuration is invalid or incomplete, using dummy authorities populator";
        public static final String LDAP_AUTHORITIES_POPULATOR_FAILED = "Failed to create LDAP authorities populator, falling back to dummy populator";
        public static final String LDAP_DISABLED_OR_NOT_CONFIGURED_AUTH_PROVIDER = "LDAP is disabled or not configured, using empty authentication provider";
        public static final String LDAP_CONFIG_INVALID_OR_INCOMPLETE_AUTH_PROVIDER = "LDAP configuration is invalid or incomplete, using empty authentication provider";
        public static final String LDAP_AUTH_PROVIDER_FAILED = "Failed to create LDAP authentication provider, falling back to empty provider";
        // --- OAuth2AuthenticationConfig log and error messages ---
        public static final String OAUTH2_DISABLED_OR_NOT_CONFIGURED = "OAuth2 is disabled or not configured, using empty repository";
        public static final String OAUTH2_CONFIG_INVALID_OR_INCOMPLETE = "OAuth2 configuration is invalid or incomplete, using empty repository";
        public static final String OAUTH2_CLIENT_REGISTRATION_CREATED = "Created OAuth2 client registration for provider: %s";
        public static final String OAUTH2_CONFIG_FAILED = "Failed to create OAuth2 configuration, falling back to empty repository";
        public static final String OAUTH2_JWK_SET_URI = "OAuth2 JWK Set URI from config: %s";
        public static final String OAUTH2_JWK_SET_URI_CONFIGURED = "JWK Set URI configured: %s";
        public static final String OAUTH2_JWK_SET_URI_EMPTY = "JWK Set URI is null or empty - this will cause signature verification issues for OIDC";
        public static final String OAUTH2_CLIENT_REGISTRATION_SUMMARY = "OAuth2 client registration created for %s: clientId=%s, redirectUri=%s";
        public static final String OAUTH2_CLIENT_REGISTRATION_FAILED = "Failed to create OAuth2 client registration for provider: %s";
        public static final String OAUTH2_CLIENT_REGISTRATION_FAILED_MSG = "Failed to create OAuth2 client registration";
        // --- SAML Authentication Config log and error messages ---
        public static final String SAML_DISABLED_OR_NOT_CONFIGURED = "SAML is disabled or not configured, using empty repository";
        public static final String SAML_CONFIG_INVALID_OR_INCOMPLETE = "SAML configuration is invalid or incomplete, using empty repository";
        public static final String SAML_NO_IDP_VERIFICATION_CERT = "No IdP verification certificate configured for SAML";
        public static final String SAML_FAILED_TO_CREATE_CONFIG = "Failed to create SAML configuration, falling back to empty repository";
        public static final String SAML_NO_IDP_X509_CERT = "No IdP X509 certificate configured - SAML signature verification will be disabled";
        public static final String SAML_FAILED_TO_LOAD_IDP_CERT = "Failed to load IdP verification certificate";
        public static final String SAML_UNKNOWN_NAMEID_FORMAT = "Unknown NameID format: '%s', using unspecified";

        // --- StoragePathProvider log and error messages ---
        public static final String STORAGE_INIT_START = "Initializing storage paths and security infrastructure...";
        public static final String STORAGE_INIT_COMPLETE = "Storage paths and security initialization complete.";
        public static final String STORAGE_INIT_FATAL = "FATAL: Failed to initialize storage paths and security infrastructure.";
        public static final String STORAGE_INIT_RUNTIME_EXCEPTION = "Failed to initialize storage infrastructure";
        public static final String STORAGE_EXPLICIT_BASE_DIR = "Using explicit base directory from configuration: %s";
        public static final String STORAGE_OS_DEFAULT_PATH = "Using OS-specific default path: %s";

        // --- SecureConfigurationExample log and error messages ---
        public static final String SAML_CONFIG_ARTIFACTS_STORED = "SAML configuration and artifacts stored securely (encrypted)";
        public static final String SAML_CONFIG_STORE_FAILED = "Failed to store SAML configuration";
        public static final String DB_CONFIG_STORED = "Database configuration stored securely (encrypted)";
        public static final String DB_CONFIG_STORE_FAILED = "Failed to store database configuration";
        public static final String UI_THEME_STORED = "UI theme configuration stored (not encrypted)";
        public static final String UI_THEME_STORE_FAILED = "Failed to store UI theme configuration";
        public static final String SAML_CERT_ARTIFACT_RETRIEVED = "Retrieved SAML certificate artifact";
        public static final String SAML_KEYSTORE_ARTIFACT_RETRIEVED = "Retrieved SAML keystore artifact";
        public static final String SAML_ARTIFACTS_RETRIEVED = "Retrieved %s artifacts for SAML configuration";
        public static final String SAML_ARTIFACT_TYPE = "Artifact type: %s";
        public static final String SAML_ARTIFACT_CONTENT_AVAILABLE = "  - Content available: %s";
        public static final String SAML_ARTIFACT_ENCRYPTED = "  - Encrypted: %s";
        public static final String SAML_ARTIFACT_FILE_PATH = "  - File path: %s";
        public static final String SAML_ARTIFACTS_RETRIEVE_FAILED = "Failed to retrieve configuration artifacts";
        public static final String SAML_CONFIG_RETRIEVED = "Retrieved SAML configuration for entity: %s";
        public static final String SAML_CONFIG_ENCRYPT_ASSERTIONS = "Encrypt assertions: %s";
        public static final String SAML_CERT_ARTIFACT_SUCCESS = "Certificate artifact retrieved successfully";
        public static final String SAML_KEYSTORE_ARTIFACT_SUCCESS = "Keystore artifact retrieved successfully";
        public static final String SAML_CONFIG_NOT_FOUND = "SAML configuration not found";
        public static final String SAML_CONFIG_RETRIEVE_FAILED = "Failed to retrieve SAML configuration";
        public static final String SAML_CONFIG_ENCRYPTED = "SAML config encrypted: %s";
        public static final String THEME_CONFIG_ENCRYPTED = "Theme config encrypted: %s";
        public static final String SAML_ENCRYPTION_METADATA = "SAML encryption - Algorithm: %s, Key Type: %s";
        public static final String RAW_ENCRYPTED_SAML_DATA = "Raw encrypted SAML data structure:";
        public static final String RAW_ENCRYPTED = "- Encrypted: %s";
        public static final String RAW_ALGORITHM = "- Algorithm: %s";
        public static final String RAW_KEY_TYPE = "- Key Type: %s";
        public static final String RAW_DATA_LENGTH = "- Data Length: %s characters";
        public static final String CONFIG_NOT_ENCRYPTED = "Configuration is not encrypted";
        public static final String RAW_CONFIG_VIEW_FAILED = "Failed to view raw configuration data";
        public static final String SAML_CERT_UPDATE_SUCCESS = "Updated SAML certificate artifact";
        public static final String SAML_CERT_UPDATE_FAILED = "Failed to update SAML certificate";
        public static final String SAML_KEYSTORE_REMOVE_SUCCESS = "Successfully removed SAML keystore artifact";
        public static final String SAML_KEYSTORE_REMOVE_NOT_FOUND = "SAML keystore artifact not found or could not be deleted";
        public static final String SAML_KEYSTORE_REMOVE_FAILED = "Failed to remove SAML keystore artifact";

        // --- EncryptionServiceImpl log and error messages ---
        public static final String ENCRYPTION_FAILED = "Encryption failed for configType '%s'";
        public static final String ENCRYPTION_PROCESS_FAILED = "Encryption process failed";
        public static final String DECRYPTION_FAILED = "Decryption failed for configType '%s'";
        public static final String DECRYPTION_PROCESS_FAILED = "Decryption process failed for configType: %s";
        public static final String DECRYPTION_TAG_MISMATCH = "Decryption failed due to authentication tag mismatch for configType '%s'. Possible data tampering.";
        public static final String DECRYPTION_INTEGRITY_FAILED = "Data integrity check failed. The encrypted data may have been tampered with.";

        // --- FieldEncryptionServiceImpl log and error messages ---
        public static final String ENCRYPT_SENSITIVE_FIELDS_START = "Starting encryptSensitiveFields: configType=%s, encryptEntireConfig=%s";
        public static final String ENCRYPT_SENSITIVE_FIELDS_NULL = "Input JSON data is null or empty for configType: %s";
        public static final String ENCRYPT_FULL_CONFIG = "Applying full configuration encryption for configType: %s";
        public static final String ENCRYPT_JSON_TO_STRING = "Converted JSON to string for full encryption, size: %s characters";
        public static final String ENCRYPT_FULL_CONFIG_SUCCESS = "Successfully encrypted entire configuration for configType: %s";
        public static final String ENCRYPT_FULL_CONFIG_COMPLETE = "Completed full configuration encryption for configType: %s";
        public static final String ENCRYPT_FIELD_LEVEL = "Applying field-level encryption for configType: %s";
        public static final String ENCRYPT_FIELD_LEVEL_COMPLETE = "Completed field-level encryption for configType: %s";
        public static final String ENCRYPT_SENSITIVE_FIELDS_FAILED = "Failed to encrypt sensitive fields for configType: %s";
        public static final String ENCRYPT_SENSITIVE_FIELDS_EXCEPTION = "Exception details for encryption of configType %s: %s";
        public static final String ENCRYPT_FIELD_PROCESS_FAILED = "Failed to process field: %s";
        public static final String ENCRYPTION_RUNTIME_EXCEPTION = "Field encryption failed";
        public static final String DECRYPT_SENSITIVE_FIELDS_START = "Starting decryptSensitiveFields for configType: %s";
        public static final String DECRYPT_SENSITIVE_FIELDS_NULL = "Input JSON data is null or empty for decryption, configType: %s";
        public static final String DECRYPT_FIELD_LEVEL = "Processing node for field-level decryption, configType: %s";
        public static final String DECRYPT_SENSITIVE_FIELDS_COMPLETE = "Completed decryptSensitiveFields for configType: %s";
        public static final String DECRYPT_SENSITIVE_FIELDS_FAILED = "Failed to decrypt sensitive fields for configType: %s";
        public static final String DECRYPT_SENSITIVE_FIELDS_EXCEPTION = "Exception details for decryption of configType %s: %s";
        public static final String DECRYPT_FIELD_PROCESS_FAILED = "Failed to decrypt field: %s";
        public static final String DECRYPTION_RUNTIME_EXCEPTION = "Field decryption failed";
        public static final String INVALID_CRYPTO_FIELD_STRUCTURE = "Invalid $crypto field structure";
        public static final String MAC_VERIFICATION_FAILED = "MAC verification failed - data may have been tampered with";

        // --- ConfigurationServiceImpl log and error messages ---
        public static final String CONFIG_DELETE_ARTIFACT_FILE = "Deleted artifact file: %s";
        public static final String CONFIG_DELETE_ARTIFACT_FILE_FAILED = "Failed to delete artifact file: %s";
        public static final String CONFIG_DELETE = "Deleted configuration: %s %s";
        public static final String CONFIG_REMOVE_ARTIFACT = "Removed artifact %s from configuration %s %s";
        public static final String CONFIG_NO_INLINE_OR_ARTIFACTS = "Configuration %s %s has no inline data or related artifacts";
        public static final String CONFIG_DECRYPT_FULL_INLINE = "Applied full decryption to inline configuration: %s %s";
        public static final String CONFIG_DECRYPT_FULL_INLINE_FAILED = "Failed to decrypt full-encrypted inline data: %s %s";
        public static final String CONFIG_DECRYPT_FIELD_INLINE = "Applied field-level decryption to inline configuration: %s %s";
        public static final String CONFIG_DECRYPT_FIELD_INLINE_FAILED = "Failed to decrypt field-level encrypted data: %s %s";
        public static final String CONFIG_DECRYPT_FIELD_FILE = "Applied field-level decryption to file-based configuration: %s %s";
        public static final String CONFIG_DECRYPT_FIELD_FILE_FAILED = "Failed to decrypt sensitive fields for file-based configuration: %s %s";
        public static final String CONFIG_READ_FILE_FAILED = "Failed to read configuration file: %s";
        public static final String CONFIG_DECRYPT_FILE_FAILED = "Failed to decrypt configuration file: %s";
        public static final String CONFIG_SAVE_FAILED = "Failed to save configuration: %s %s with %s";
        public static final String CONFIG_SAVE_INLINE_FAILED = "Failed to save inline configuration: %s %s";
        public static final String CONFIG_SAVE_INLINE_EXCEPTION = "Exception details for saveConfigurationInline %s/%s: %s";
        public static final String CONFIG_SAVE = "Saved configuration: %s %s with %s (encrypted: %s)";
        public static final String CONFIG_SAVE_INLINE = "Saved inline configuration: %s %s (encryption applied: %s)";
        public static final String CONFIG_CREATE_DIR = "Created directory structure: %s";
        public static final String CONFIG_SAVE_ENCRYPTED_FILE = "Saved encrypted %s file: %s";
        public static final String CONFIG_SAVE_FILE = "Saved %s file: %s";
        public static final String CONFIG_DELETE_ARTIFACT_FILE_OLD = "Deleted old artifact file: %s";
        public static final String CONFIG_DELETE_ARTIFACT_FILE_OLD_FAILED = "Failed to delete old artifact file: %s";
        public static final String CONFIG_REPLACE_ARTIFACT = "Replaced existing artifact of type '%s' for configuration %s/%s";
        public static final String CONFIG_ADD_ARTIFACT = "Added new artifact of type '%s' for configuration %s/%s";
        public static final String CONFIG_CREATE_WITH_ARTIFACT = "Created new configuration %s/%s with artifact of type '%s'";
        public static final String CONFIG_INCREMENT_VERSION = "Incremented version to %s for configuration: %s %s (artifact update)";
        public static final String CONFIG_PATCH_START = "Starting partialUpdateConfiguration: configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_DETAILS = "Patch request details: active=%s, encryptEntireConfig=%s, dataPresent=%s";
        public static final String CONFIG_PATCH_SEARCH = "Searching for existing configuration: %s/%s";
        public static final String CONFIG_PATCH_NOT_FOUND = "Configuration not found for patch operation: %s/%s";
        public static final String CONFIG_PATCH_FOUND = "Found existing configuration with version: %s for %s/%s";
        public static final String CONFIG_PATCH_INLINE = "Processing inline configuration patch for %s/%s";
        public static final String CONFIG_PATCH_NO_INLINE = "Configuration has no inline data for patch operation: %s/%s";
        public static final String CONFIG_PATCH_FAILED = "Failed to partially update configuration: %s %s";
        public static final String CONFIG_PATCH_EXCEPTION = "Exception details for partial update %s/%s: %s";
        public static final String CONFIG_PATCH_UPDATE = "Updating inline configuration for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_RETRIEVE_DATA = "Retrieving current data for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_CHECK_ENCRYPTED_FIELDS = "Checking for encrypted fields in configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_DECRYPT = "Decrypting sensitive fields for configType=%s, configKey=%s";
        public static final String CONFIG_VERSION_INCREMENT_TRACE = "Configuration version incremented from %s to %s for configType=%s, configKey=%s";

        // --- ConfigurationServiceImpl centralized logging (added missing constants)
        // ---
        public static final String CONFIG_SAVE_INLINE_START = "Starting saveConfigurationInline for configType=%s, configKey=%s, encryptEntireConfig=%s";
        public static final String CONFIG_CONVERT_DATA = "Converting data to JsonNode for configType=%s, configKey=%s";
        public static final String CONFIG_DATA_ALREADY_JSON = "Data is already a JsonNode for configType=%s, configKey=%s";
        public static final String CONFIG_CONVERTED_STRING_TO_JSON = "Converted JSON String to JsonNode for configType=%s, configKey=%s";
        public static final String CONFIG_CONVERTED_OBJECT_TO_JSON = "Converted Object '%s' to JsonNode for configType=%s, configKey=%s";
        public static final String CONFIG_DETERMINE_ENCRYPTION = "Determining encryption for configType=%s, configKey=%s";
        public static final String CONFIG_ENCRYPTION_CHECK_RESULT = "Encryption check result: shouldEncrypt=%s, encryptEntireConfig=%s, configType=%s, configKey=%s";
        public static final String CONFIG_APPLY_ENCRYPTION = "Applying encryption for configType=%s, configKey=%s, mode=%s";
        public static final String CONFIG_ENCRYPTION_APPLIED = "Encryption applied for configType=%s, configKey=%s, mode=%s";
        public static final String CONFIG_APPLY_FIELD_ENCRYPTION_CHECK = "Checking if field-level encryption should be applied for configType=%s, configKey=%s";
        public static final String CONFIG_FIELD_ENCRYPTION_APPLIED = "Field-level encryption applied for configType=%s, configKey=%s";
        public static final String CONFIG_CHECK_EXISTING = "Checking for existing configuration for configType=%s, configKey=%s";
        public static final String CONFIG_UPDATE_EXISTING = "Updating existing configuration for configType=%s, configKey=%s";
        public static final String CONFIG_CREATE_NEW = "Creating new configuration for configType=%s, configKey=%s";
        public static final String CONFIG_NEW_CREATED = "New configuration created with version %s for configType=%s, configKey=%s";
        public static final String CONFIG_SAVE_REPO = "Saving configuration to repository for configType=%s, configKey=%s";
        public static final String CONFIG_SAVE_INLINE_SUCCESS = "Successfully saved inline configuration for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_DECRYPTED = "Decrypted sensitive fields for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_COPY_DATA = "Creating mutable copy of current data for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_UPDATE_ACTIVE = "Updating 'active' flag to %s for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_PROCESS_DATA = "Processing patch data for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_DATA_ALREADY_JSON = "Patch data is already a JsonNode for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_CONVERTED_STRING_TO_JSON = "Converted patch String to JsonNode for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_CONVERTED_OBJECT_TO_JSON = "Converted patch Object '%s' to JsonNode for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_MERGE = "Merging patch data into current data for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_MERGE_SUCCESS = "Successfully merged patch data for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_APPLY_ENCRYPTION = "Applying encryption for patch on configType=%s, configKey=%s, mode=%s";
        public static final String CONFIG_PATCH_ENCRYPTION = "Applying encryption for patch on configType=%s, configKey=%s, mode=%s";
        public static final String CONFIG_PATCH_APPLY_FIELD = "Applying field-level encryption for patch on configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_FIELD_ENCRYPTION_APPLIED = "Field-level encryption applied for patch on configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_UPDATE_CONFIG = "Updating configuration object for patch on configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_INCREMENT_VERSION_TRACE = "Configuration version incremented from %s to %s for patch operation on configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_SAVE = "Saving patched configuration to repository for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_SAVE_INFO = "Patched inline configuration: %s %s (active: %s)";
        public static final String CONFIG_PATCH_SUCCESS = "Successfully patched configuration for configType=%s, configKey=%s";
        public static final String CONFIG_PATCH_ENCRYPTION_APPLIED = "Encryption for patch applied on configType=%s, configKey=%s, mode=%s";

        // Configuration types and SSO types
        public static final String CONFIG_TYPE_SSO = "sso";
        public static final String SSO_TYPE_LDAP = "ldap";
        public static final String SSO_TYPE_SAML = "saml";
        public static final String SSO_TYPE_OAUTH2 = "oauth2";

        // Configuration audit logging messages
        public static final String LOG_CONFIG_AUDIT_SAVE_SUCCESS = "Configuration audit log saved successfully: action=%s, configType=%s, configKey=%s";
        public static final String LOG_CONFIG_AUDIT_SAVE_ERROR = "Failed to save configuration audit log: action=%s, configType=%s, configKey=%s";
        public static final String LOG_CONFIG_AUDIT_RECENT_ERROR = "Failed to retrieve recent configuration audit logs";
        public static final String LOG_CONFIG_AUDIT_USER_ERROR = "Failed to retrieve configuration audit logs for user: %s";
        public static final String LOG_CONFIG_AUDIT_TYPE_ERROR = "Failed to retrieve configuration audit logs for config type: %s";
        public static final String LOG_CONFIG_AUDIT_TIMERANGE_ERROR = "Failed to retrieve configuration audit logs for time range: %s to %s";

        public static final String USER_NOT_FOUND_BY_ID = "User not found with id: %s";

        public static final String EXCEPTION_LDAP_CONFIG_SAVE = "Failed to save LDAP configuration";
        public static final String EXCEPTION_SAML_CONFIG_SAVE = "Failed to save SAML configuration";
        public static final String EXCEPTION_OAUTH2_CONFIG_SAVE = "Failed to save OAuth2 configuration";

        public static final String AUDIT_LDAP_CONFIG_UPDATED = "LDAP configuration updated - enabled: %s, name: %s";
        public static final String AUDIT_SAML_CONFIG_UPDATED = "SAML configuration updated - enabled: %s, name: %s";
        public static final String AUDIT_OAUTH2_CONFIG_UPDATED = "OAuth2 configuration updated - enabled: %s, name: %s, providerId: %s";

        public static final String LDAP_CONFIG_NOT_FOUND = "LDAP configuration is not found";
        public static final String OAUTH2_CONFIG_NOT_FOUND = "OAuth2 configuration is not found";
        public static final String PROVIDER_CONFIG_SUMMARY_FAILED_MESSAGE = "Failed to generate summary: %s";

        // SAML attribute extraction log messages
        public static final String SAML_ATTRIBUTE_FOUND_CONFIGURED = "SAML attribute found using configured mapping: %s";
        public static final String SAML_ATTRIBUTE_FOUND_FALLBACK = "SAML attribute found using fallback mapping: %s";
        public static final String SAML_ATTRIBUTE_NOT_FOUND = "SAML attribute not found in SAML response.";

        // --- SAML Attribute Extraction and Logging ---
        /** Debug: List available SAML principal attributes */
        public static final String SAML_PRINCIPAL_ATTRIBUTES_AVAILABLE = "SAML principal attributes available: %s";
        /** Debug: SAML attribute key/value (value redacted) */
        public static final String SAML_ATTRIBUTE_DEBUG = "SAML attribute: %s = %s";
        /** Debug: Fallback to authentication name as email */
        public static final String SAML_AUTH_NAME_AS_EMAIL = "Using authentication name as email: %s";
        /** Error: Failed to extract email from SAML response */
        public static final String SAML_AUTH_FAILED_EMAIL_EXTRACTION = "Failed to extract email from SAML response";
        /** Warn: SAML user not found by extracted email */
        public static final String SAML_AUTH_FAILED_USER_NOT_FOUND = "SAML user not found for email: %s";
        /** Info: SAML user attributes extracted (do not log values) */
        public static final String SAML_USER_ATTRIBUTES_EXTRACTED = "SAML user attributes extracted for email: %s";
        /** Error: Exception during SAML user attribute extraction */
        public static final String SAML_USER_ATTRIBUTE_EXTRACTION_ERROR = "Error extracting SAML user attributes: %s";

        // Password reset token update log
        public static final String PASSWORD_RESET_TOKEN_UPDATED = "Password reset token updated for user: %s";

        // --- OAuth2 Attribute Extraction and Logging ---
        /** Debug: List available OAuth2 user attributes */
        public static final String OAUTH2_USER_ATTRIBUTES_AVAILABLE = "OAuth2 user attributes available: %s";
        /**
         * Debug: OAuth2 attribute key/value (value redacted)
         * 
         * /** Debug: Fallback to authentication name as email
         */
        public static final String OAUTH2_AUTH_NAME_AS_EMAIL = "Using OAuth2 authentication name as email: %s";
        /** Error: Failed to extract email from OAuth2 response */
        public static final String OAUTH2_AUTH_FAILED_EMAIL_EXTRACTION = "Failed to extract email from OAuth2 response";
        /** Warn: OAuth2 user not found by extracted email */
        public static final String OAUTH2_AUTH_FAILED_USER_NOT_FOUND = "OAuth2 user not found for email: %s";
        /** Info: OAuth2 user attributes extracted (do not log values) */
        public static final String OAUTH2_USER_ATTRIBUTES_EXTRACTED = "OAuth2 user attributes extracted for email: %s";
        /** Error: Exception during OAuth2 user attribute extraction */
        public static final String OAUTH2_USER_ATTRIBUTE_EXTRACTION_ERROR = "Error extracting OAuth2 user attributes: %s";
        /** Debug: OAuth2 attribute found using configured mapping */
        public static final String OAUTH2_ATTRIBUTE_FOUND_CONFIGURED = "OAuth2 attribute found using configured mapping: %s";
        /** Debug: OAuth2 attribute found using fallback mapping */
        public static final String OAUTH2_ATTRIBUTE_FOUND_FALLBACK = "OAuth2 attribute found using fallback mapping: %s";
        /** Warn: OAuth2 attribute not found */
        public static final String OAUTH2_ATTRIBUTE_NOT_FOUND = "OAuth2 attribute not found";
        /** Warn: OAuth2 configuration not found, using default mappings */
        public static final String OAUTH2_CONFIG_NOT_FOUND_USING_DEFAULTS = "OAuth2 configuration not found, using default attribute mappings";
        /** Debug: OAuth2 attribute key/value (value redacted) */
        public static final String OAUTH2_ATTRIBUTE_DEBUG = "OAuth2 attribute: %s = %s";
        public static final String AUDIT_LOG_ASYNC_SUCCESS = "Audit log saved asynchronously for action: %s userId: %s";
        public static final String AUDIT_LOG_ASYNC_ERROR = "Failed to save audit log for action: %s userId: %s error: %s";
        public static final String AUDIT_LOG_SYNC_SUCCESS = "Audit log saved synchronously for action: %s userId: %s";
        public static final String AUDIT_LOG_SYNC_ERROR = "Failed to save audit log synchronously for action: %s userId: %s error: %s";
        public static final String CONFIG_AUDIT_LOG_ASYNC_SUCCESS = "Configuration audit log saved asynchronously for action: %s configType: %s configKey: %s userId: %s";
        public static final String CONFIG_AUDIT_LOG_ASYNC_ERROR = "Failed to save configuration audit log for action: %s configType: %s configKey: %s";
        // ===================== ConfigurationController Audit/Event Messages
        // =====================
        public static final String AUDIT_VIEWED_CONFIGURATION = "Viewed configuration: %s";
        public static final String AUDIT_CONFIGURATION_NOT_FOUND = "Configuration not found: %s/%s";
        public static final String AUDIT_LISTED_CONFIGURATIONS_BY_TYPE = "Listed configurations by type: %s";
        public static final String AUDIT_LIST_CONFIGURATIONS_BY_TYPE_FAILED = "Failed to list configurations by type: %s, error: %s";
        public static final String AUDIT_LISTED_ALL_CONFIGURATIONS = "Listed all configurations";
        public static final String AUDIT_CREATED_OR_UPDATED_CONFIGURATION = "Created or updated configuration: %s/%s";
        public static final String AUDIT_CREATE_OR_UPDATE_CONFIGURATION_FAILED = "Failed to create or update configuration: %s/%s, error: %s";
        public static final String AUDIT_CREATE_OR_UPDATE_CONFIGURATION_ERROR = "Error creating or updating configuration: %s/%s, error: %s";
        public static final String AUDIT_DELETED_CONFIGURATION = "Deleted configuration: %s/%s";
        public static final String AUDIT_DELETE_CONFIGURATION_NOT_FOUND = "Configuration to delete not found: %s/%s";
        public static final String AUDIT_DELETE_CONFIGURATION_FAILED = "Failed to delete configuration: %s/%s, error: %s";
        public static final String AUDIT_CHECKED_CONFIGURATION_EXISTS = "Checked if configuration exists: %s/%s";
        public static final String AUDIT_CHECK_CONFIGURATION_EXISTS_FAILED = "Failed to check if configuration exists: %s/%s, error: %s";
        public static final String AUDIT_LISTED_SUPPORTED_CONFIGURATION_TYPES = "Listed supported configuration types";
        public static final String AUDIT_VIEWED_RAW_CONFIGURATION_DATA = "Viewed raw configuration data: %s/%s";
        public static final String AUDIT_RAW_CONFIGURATION_DATA_NOT_FOUND = "Raw configuration data not found: %s/%s";
        public static final String AUDIT_VIEW_RAW_CONFIGURATION_DATA_FAILED = "Failed to view raw configuration data: %s/%s, error: %s";
        public static final String AUDIT_VIEW_RAW_CONFIGURATION_DATA_ERROR = "Error viewing raw configuration data: %s/%s, error: %s";
        public static final String AUDIT_SAVED_CONFIGURATION = "Saved configuration for %s provider";
        public static final String AUDIT_SAVE_CONFIGURATION_FAILED = "Failed to save configuration for %s provider";
        public static final String AUDIT_FETCH_CONFIGURATION_ERROR = "Configuration for %s provider isn't found";
        public static final String AUDIT_CONFIG_TOGGLE_FAILED = "Failed to toggle config for %s provider";
        public static final String AUDIT_CONFIG_DELETED_FAILED = "Failed to toggle config for %s provider";
        public static final String AUDIT_ARTIFACT_RETRIEVE = "Successfully retrieved related artifacts";
        public static final String AUDIT_ARTIFACT_RETRIEVE_ALL = "Successfully retrieved all artifacts";
        public static final String AUDIT_ARTIFACT_DELETE_FAILED = "Failed to delete artifact";
        public static final String AUDIT_ARTIFACT_UPDATE = "Successfully updated artifact";
        public static final String AUDIT_ARTIFACT_DELETE = "Successfully deleted artifact";

        // ===================== AuthController/SSO Messages =====================
        // --- ConfigurationServiceImpl artifact/file operation log messages ---
        public static final String CONFIG_ARTIFACT_FILE_INFO_FAILED = "Failed to get artifact file info: %s";
        public static final String CONFIG_ARTIFACT_FILE_NOT_FOUND = "Artifact file not found: %s";
        public static final String CONFIG_ARTIFACT_FILE_READ_FAILED = "Failed to read artifact file: %s";
        public static final String CONFIG_ARTIFACT_FILE_DECRYPT_FAILED = "Failed to decrypt artifact file: %s";
        public static final String CONFIG_ARTIFACT_FILE_METADATA_FAILED = "Failed to get artifact file metadata: %s";
        public static final String CONFIG_ARTIFACT_FILE_NOT_FOUND_MSG = "Artifact file not found.";
        public static final String CONFIG_ARTIFACT_FILE_READ_FAILED_MSG = "Failed to read artifact file: ";
        public static final String CONFIG_ARTIFACT_FILE_DECRYPT_FAILED_MSG = "Failed to decrypt artifact file: ";

        // --- EncryptionServiceImpl trace log messages ---
        public static final String TRACE_ENCRYPTION_START = "Starting encryption for configType=%s";
        public static final String TRACE_ENCRYPTION_IV_GENERATED = "Generated IV for encryption for configType=%s";
        public static final String TRACE_ENCRYPTION_CIPHER_INIT = "Initialized encryption cipher for configType=%s";
        public static final String TRACE_ENCRYPTION_COMPLETE = "Encryption complete for configType=%s";
        public static final String TRACE_ENCRYPTION_CONTAINER_SERIALIZED = "Serialized encrypted data container for configType=%s";
        public static final String TRACE_DECRYPTION_START = "Starting decryption for configType=%s";
        public static final String TRACE_DECRYPTION_CONTAINER_DECODED = "Decoded encrypted data container for configType=%s";
        public static final String TRACE_DECRYPTION_CONTAINER_DESERIALIZED = "Deserialized encrypted data container for configType=%s";
        public static final String TRACE_DECRYPTION_CIPHER_INIT = "Initialized decryption cipher for configType=%s";
        public static final String TRACE_DECRYPTION_COMPLETE = "Decryption complete for configType=%s";
        // --- KeyManagementServiceImpl trace log messages ---
        public static final String TRACE_KEYMGMT_GET_CONFIG_KEY_START = "Starting getConfigurationKey for configType=%s";
        public static final String TRACE_KEYMGMT_KEY_FOUND_DB = "Found configuration key in DB for configType=%s";
        public static final String TRACE_KEYMGMT_KEY_DECRYPTED = "Decrypted configuration key for configType=%s";
        public static final String TRACE_KEYMGMT_KEY_NOT_FOUND_DB = "No configuration key found in DB for configType=%s";
        public static final String TRACE_KEYMGMT_NEW_KEY_GENERATED = "Generated new configuration key for configType=%s";
        public static final String TRACE_KEYMGMT_NEW_KEY_ENCRYPTED = "Encrypted new configuration key for configType=%s";
        public static final String TRACE_KEYMGMT_NEW_KEY_SAVED = "Saved new configuration key to DB for configType=%s";
        public static final String TRACE_KEYMGMT_ROTATE_START = "Starting master key rotation";
        public static final String TRACE_KEYMGMT_ROTATE_NEW_MASTER_GENERATED = "Generated new master key for rotation";
        public static final String TRACE_KEYMGMT_ROTATE_CONFIG_KEYS_FETCHED = "Fetched %s configuration keys for rotation";
        public static final String TRACE_KEYMGMT_ROTATE_CONFIG_KEY_DECRYPTED = "Decrypted configuration key for keyType=%s";
        public static final String TRACE_KEYMGMT_ROTATE_CONFIG_KEY_REENCRYPTED = "Re-encrypted configuration key for keyType=%s";
        public static final String TRACE_KEYMGMT_ROTATE_CONFIG_KEYS_SAVED = "Saved all re-encrypted configuration keys to DB";
        public static final String TRACE_KEYMGMT_ROTATE_MASTER_KEY_ENCRYPTED = "Encrypted new master key for storage";
        public static final String TRACE_KEYMGMT_ROTATE_MASTER_KEY_BACKED_UP = "Backed up old master key to %s";
        public static final String TRACE_KEYMGMT_ROTATE_MASTER_KEY_WRITTEN = "Wrote new master key to filesystem";
        public static final String TRACE_KEYMGMT_ROTATE_INMEMORY_UPDATED = "Updated in-memory master key reference";
        public static final String TRACE_KEYMGMT_ROTATE_COMPLETE = "Master key rotation complete";

        // --- ConfigurationRequest validation messages ---
        public static final String CONFIG_DATA_REQUIRED_MSG = "Configuration data is required";
        public static final String CONFIG_TYPE_REQUIRED_MSG = "Configuration type is required";

        // --- LoginRequest validation messages ---
        public static final String USERNAME_REQUIRED_MSG = "Username is required";
        public static final String PASSWORD_REQUIRED_MSG = "Password is required";
        public static final String PASSWORD_MIN_LENGTH_MSG = "Password must be at least " + PASSWORD_MIN_LENGTH
                        + " characters long";

        // JSON / Conversion
        public static final String JSONNODE_TO_STRING_ERROR = "Failed to convert JsonNode to String";
        public static final String STRING_TO_JSONNODE_ERROR = "Failed to convert String to JsonNode";

        // SAML
        public static final String NO_REDIRECT_URL_PROVIDED = "No redirect URL was provided for SAML login";

        // Generic provider-related
        public static final String UNKNOWN_PROVIDER_MSG = "Unknown authentication provider: %s";

        // Configuration messages
        public static final String PROVIDER_CONFIG_UPDATE_SUCCESS_MSG = "Configuration updated successfully for provider: %s";
        public static final String PROVIDER_CONFIG_UPDATE_FAILED_MSG = "Failed to update configuration for provider: %s. Reason: %s";
        public static final String PROVIDER_CONFIG_UPDATE_FAILED_LOG = "Failed to update provider config: %s";

        // LDAP / SAML / OAuth2
        public static final String LDAP_CONFIG_NOT_FOUND_MSG = "LDAP configuration not found";
        public static final String SAML_CONFIG_NOT_FOUND_MSG = "SAML configuration not found";
        public static final String OAUTH2_CONFIG_NOT_FOUND_MSG = "OAuth2 configuration not found";

        // Toggle
        public static final String PROVIDER_TOGGLE_SUCCESS_MSG = "Provider %s has been %s successfully";
        public static final String PROVIDER_TOGGLE_FAILED_MSG = "Failed to toggle provider %s. Reason: %s";
        public static final String ENABLED_MSG = "ENABLED";
        public static final String DISABLED_MSG = "DISABLED";

        // Delete
        public static final String PROVIDER_DELETE_SUCCESS_MSG = "Provider %s deleted successfully";
        public static final String PROVIDER_DELETE_NOT_FOUND_MSG = "Provider %s not found";
        public static final String PROVIDER_DELETE_FAILED_MSG = "Failed to delete provider %s. Reason: %s";
        public static final String PROVIDER_DELETE_FAILED_LOG = "Failed to delete provider: %s";

        // Security Configuration Messages
        public static final String SECURITY_SAML_ENABLED_LOG = "SSO session management enabled (SAML: %s, OAuth2: %s)";
        public static final String SECURITY_SAML_CONFIG_ERROR_LOG = "Error checking SAML configuration: %s";
        public static final String SECURITY_OAUTH2_CONFIG_ERROR_LOG = "Error checking OAuth2 configuration: %s";
        public static final String SECURITY_SAML_LOGIN_CONFIG_LOG = "Configuring SAML login";
        public static final String SECURITY_SAML_AUTH_SUCCESS_LOG = "SAML authentication successful for user: %s";
        public static final String SECURITY_SAML_CALLBACK_SUCCESS_URL = "/api/v1/auth/saml/callback?success=true";
        public static final String SECURITY_SAML_AUTH_FAILED_LOG = "SAML authentication failed: %s";
        public static final String SECURITY_SAML_CALLBACK_ERROR_URL = "/api/v1/auth/saml/callback?error=true&message=";
        public static final String SECURITY_OAUTH2_LOGIN_CONFIG_LOG = "Configuring OAuth2 login";
        public static final String SECURITY_OAUTH2_AUTH_SUCCESS_LOG = "OAuth2 authentication successful for user: %s";
        public static final String SECURITY_OAUTH2_AUTH_FAILED_LOG = "OAuth2 authentication failed: %s";
        public static final String SECURITY_OAUTH2_CALLBACK_SUCCESS_URL = "/api/v1/auth/oauth2/callback?success=true";
        public static final String SECURITY_OAUTH2_CALLBACK_ERROR_URL = "/api/v1/auth/oauth2/callback?error=true&message=";
        public static final String SECURITY_LDAP_CONFIG_ERROR_LOG = "Error getting LDAP configuration: %s";
        public static final String SECURITY_INVALID_CREDENTIALS_MSG = "Invalid credentials";
        public static final String SECURITY_UNAUTHORIZED_JSON_RESPONSE = "{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}";

        public static final String CONFIG_SAVED_UPDATED_EVENT = "saved/updated via API";
        public static final String CONFIG_SAVE_UPDATE_FAILED_EVENT = "save/update via API failed: ";
        public static final String CONFIG_PATCHED_EVENT = "patched via API";
        public static final String CONFIG_PATCH_FAILED_EVENT = "patch via API failed: ";
        public static final String CONFIG_TOGGLE_FAILED_EVENT = "toggle via API failed: ";
        public static final String CONFIG_DELETED_EVENT = "deleted via API";
        public static final String AUDIT_VIEWED_ALL_CONFIGS = "Viewed all SSO configurations";
        public static final String AUDIT_ACTION_NOT_FOUND_WARN = "Could not find a matching ConfigurationAuditAction for provider '%s' and event '%s'. Error: %s";
        public static final String AUDIT_LOG_FAILED_ERROR = "Failed to create audit log for SSO operation on provider '%s'";

        public static final String LOG_ENTER_GET_ALL_SSO_CONFIGS = "Entering getAllSsoConfigurations";
        public static final String LOG_EXIT_GET_ALL_SSO_CONFIGS = "Exiting getAllSsoConfigurations";
        public static final String LOG_ENTER_GET_SSO_CONFIGURATION_FOR_PROVIDER = "Entering getSsoConfiguration for provider: %s";
        public static final String LOG_PROVIDER_CONFIG_NOT_FOUND = "Provider config not found: %s";
        public static final String LOG_EXIT_GET_SSO_CONFIGURATION_FOR_PROVIDER = "Exiting getSsoConfiguration for provider: %s";
        public static final String LOG_ENTER_SAVE_SSO_CONFIGURATION_FOR_PROVIDER = "Entering saveSsoConfiguration for provider: %s";
        public static final String LOG_REQUEST_BODY_EMPTY_OR_INVALID_FOR_PROVIDER = "Request body is empty or invalid for provider: %s";
        public static final String LOG_SAVING_SSO_PROVIDER = "Saving SSO provider: %s, Full Encryption: %s";
        public static final String LOG_UNKNOWN_SSO_PROVIDER = "Unknown SSO provider: %s";
        public static final String LOG_SUCCESSFULLY_SAVED_SSO_PROVIDER = "Successfully saved SSO provider: %s";
        public static final String LOG_ENTER_TOGGLE_PROVIDER_FOR_PROVIDER = "Entering toggleProvider for provider: %s";
        public static final String LOG_UNKNOWN_PROVIDER_IN_TOGGLE_PROVIDER = "Unknown provider in toggleProvider: %s";
        public static final String LOG_PROVIDER_CONFIG_NOT_FOUND_IN_TOGGLE_PROVIDER = "Provider config not found in toggleProvider: %s";
        public static final String LOG_TOGGLING_PROVIDER = "Toggling provider: %s to state: %s (preserving encryption: %s)";
        public static final String LOG_SUCCESSFULLY_TOGGLED_PROVIDER = "Successfully toggled provider: %s to state: %s";
        public static final String LOG_ENTER_UPDATE_PROVIDER_CONFIG_FOR_PROVIDER = "Entering updateProviderConfig for provider: %s";
        public static final String LOG_UNKNOWN_PROVIDER_IN_UPDATE_PROVIDER_CONFIG = "Unknown provider in updateProviderConfig: %s";
        public static final String LOG_SUCCESSFULLY_PATCHED_PROVIDER_CONFIG = "Successfully patched provider config: %s version: %s";
        public static final String LOG_ENTER_DELETE_PROVIDER_CONFIG_FOR_PROVIDER = "Entering deleteProviderConfig for provider: %s";
        public static final String LOG_SUCCESSFULLY_DELETED_PROVIDER_CONFIG = "Successfully deleted provider config: %s";
        public static final String LOG_PROVIDER_CONFIG_NOT_FOUND_OR_COULD_NOT_BE_DELETED = "Provider config not found or could not be deleted: %s";
        public static final String LOG_AUDIT_LOG_CREATED_FOR_PROVIDER = "Audit log created for provider: %s, event: %s, success: %s";

        public static final String LOG_ENTER_GET_CONFIGURATION = "Entering getConfiguration for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_CONFIGURATION_SUCCESS = "Exiting getConfiguration (found) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_CONFIGURATION_NOT_FOUND = "Exiting getConfiguration (not found) for configType: %s, configKey: %s";
        public static final String CONFIG_NOT_FOUND_MSG = "Configuration not found for the specified type and key.";

        public static final String LOG_ENTER_GET_CONFIGURATIONS_BY_TYPE = "Entering getConfigurationsByType for configType: %s";
        public static final String LOG_EXIT_GET_CONFIGURATIONS_BY_TYPE_SUCCESS = "Exiting getConfigurationsByType (success) for configType: %s";
        public static final String LOG_EXIT_GET_CONFIGURATIONS_BY_TYPE_BAD_REQUEST = "Exiting getConfigurationsByType (bad request) for configType: %s";
        public static final String LOG_ENTER_GET_ALL_CONFIGURATIONS = "Entering getAllConfigurations";
        public static final String LOG_EXIT_GET_ALL_CONFIGURATIONS = "Exiting getAllConfigurations";
        public static final String LOG_ENTER_SAVE_CONFIGURATION = "Entering saveConfiguration for configType: %s, configKey: %s";
        public static final String LOG_EXIT_SAVE_CONFIGURATION_SUCCESS = "Exiting saveConfiguration (success) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_SAVE_CONFIGURATION_BAD_REQUEST = "Exiting saveConfiguration (bad request) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_SAVE_CONFIGURATION_ERROR = "Exiting saveConfiguration (error) for configType: %s, configKey: %s";
        public static final String LOG_ENTER_DELETE_CONFIGURATION = "Entering deleteConfiguration for configType: %s, configKey: %s";
        public static final String LOG_EXIT_DELETE_CONFIGURATION_SUCCESS = "Exiting deleteConfiguration (success) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_DELETE_CONFIGURATION_NOT_FOUND = "Exiting deleteConfiguration (not found) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_DELETE_CONFIGURATION_BAD_REQUEST = "Exiting deleteConfiguration (bad request) for configType: %s, configKey: %s";
        public static final String LOG_ENTER_CONFIGURATION_EXISTS = "Entering configurationExists for configType: %s, configKey: %s";
        public static final String LOG_EXIT_CONFIGURATION_EXISTS_SUCCESS = "Exiting configurationExists (success) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_CONFIGURATION_EXISTS_BAD_REQUEST = "Exiting configurationExists (bad request) for configType: %s, configKey: %s";
        public static final String LOG_ENTER_GET_SUPPORTED_CONFIGURATION_TYPES = "Entering getSupportedConfigurationTypes";
        public static final String LOG_ENTER_GET_RELATED_ARTIFACTS = "Entering getRelatedArtifacts for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_RELATED_ARTIFACTS_SUCCESS = "Exiting getRelatedArtifacts (success) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_RELATED_ARTIFACTS_NOT_FOUND = "Exiting getRelatedArtifacts (not found) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_RELATED_ARTIFACTS_BAD_REQUEST = "Exiting getRelatedArtifacts (bad request) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_RELATED_ARTIFACTS_ERROR = "Exiting getRelatedArtifacts (error) for configType: %s, configKey: %s";
        public static final String LOG_ENTER_GET_CONFIGURATION_ARTIFACT = "Entering getConfigurationArtifact for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_GET_CONFIGURATION_ARTIFACT_SUCCESS = "Exiting getConfigurationArtifact (success) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_GET_CONFIGURATION_ARTIFACT_NOT_FOUND = "Exiting getConfigurationArtifact (not found) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_GET_CONFIGURATION_ARTIFACT_BAD_REQUEST = "Exiting getConfigurationArtifact (bad request) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_GET_CONFIGURATION_ARTIFACT_ERROR = "Exiting getConfigurationArtifact (error) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_ENTER_UPDATE_CONFIGURATION_ARTIFACT = "Entering updateConfigurationArtifact for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_UPDATE_CONFIGURATION_ARTIFACT_SUCCESS = "Exiting updateConfigurationArtifact (success) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_UPDATE_CONFIGURATION_ARTIFACT_BAD_REQUEST = "Exiting updateConfigurationArtifact (bad request) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_UPDATE_CONFIGURATION_ARTIFACT_ERROR = "Exiting updateConfigurationArtifact (error) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_ENTER_DELETE_CONFIGURATION_ARTIFACT = "Entering deleteConfigurationArtifact for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_DELETE_CONFIGURATION_ARTIFACT_SUCCESS = "Exiting deleteConfigurationArtifact (success) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_DELETE_CONFIGURATION_ARTIFACT_NOT_FOUND = "Exiting deleteConfigurationArtifact (not found) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_DELETE_CONFIGURATION_ARTIFACT_BAD_REQUEST = "Exiting deleteConfigurationArtifact (bad request) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_EXIT_DELETE_CONFIGURATION_ARTIFACT_ERROR = "Exiting deleteConfigurationArtifact (error) for configType: %s, configKey: %s, artifactType: %s";
        public static final String LOG_ENTER_GET_ENCRYPTION_STATUS = "Entering getEncryptionStatus for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_ENCRYPTION_STATUS_SUCCESS = "Exiting getEncryptionStatus (success) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_ENCRYPTION_STATUS_BAD_REQUEST = "Exiting getEncryptionStatus (bad request) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_ENCRYPTION_STATUS_ERROR = "Exiting getEncryptionStatus (error) for configType: %s, configKey: %s";
        public static final String LOG_ENTER_GET_RAW_CONFIGURATION_DATA = "Entering getRawConfigurationData for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_RAW_CONFIGURATION_DATA_SUCCESS = "Exiting getRawConfigurationData (success) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_RAW_CONFIGURATION_DATA_NOT_FOUND = "Exiting getRawConfigurationData (not found) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_RAW_CONFIGURATION_DATA_BAD_REQUEST = "Exiting getRawConfigurationData (bad request) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_GET_RAW_CONFIGURATION_DATA_ERROR = "Exiting getRawConfigurationData (error) for configType: %s, configKey: %s";
        public static final String LOG_ENTER_PARTIAL_UPDATE_CONFIGURATION = "Entering partialUpdateConfiguration for configType: %s, configKey: %s";
        public static final String LOG_EXIT_PARTIAL_UPDATE_CONFIGURATION_SUCCESS = "Exiting partialUpdateConfiguration (success) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_PARTIAL_UPDATE_CONFIGURATION_BAD_REQUEST = "Exiting partialUpdateConfiguration (bad request) for configType: %s, configKey: %s";
        public static final String LOG_EXIT_PARTIAL_UPDATE_CONFIGURATION_ERROR = "Exiting partialUpdateConfiguration (error) for configType: %s, configKey: %s";

        public static final String CONFIG_LISTED_ALL_MSG = "Listed all configurations";

        // --- ConfigurationArtifactService trace and error messages ---
        public static final String CONFIG_ARTIFACT_SAVE_TRACE = "Saving artifact: type=%s, path=%s, encrypted=%s";
        public static final String CONFIG_ARTIFACT_CONTENT_NOT_FOUND = "Artifact content not found or inactive: %s";
        public static final String CONFIG_ARTIFACT_DECRYPTED_TRACE = "Decrypted artifact file: %s";
        public static final String CONFIG_ARTIFACT_READ_TRACE = "Read artifact file: %s";
        public static final String CONFIG_ARTIFACT_READ_OR_DECRYPT_FAILED = "Failed to read or decrypt artifact content.";
        public static final String CONFIG_ARTIFACT_METADATA_TRACE = "Processed artifact metadata for file: %s";
        public static final String CONFIG_DELETE_ARTIFACT_FILE_TRACE = "Deleted artifact file (trace): %s";
        public static final String CONFIG_DELETE_ALL_ARTIFACTS_TRACE = "Deleted all artifact files for configuration: %s";

        public static final String CONFIG_PATCH_STARTED = "Starting PATCH for %s/%s. Encrypt entire config: %s";
        public static final String CONFIG_NOT_FOUND = "Configuration not found: %s/%s";
        public static final String FAILED_PATCH_DATA_PROCESSING = "Failed during patch data processing for %s/%s";
        public static final String FAILED_TO_PROCESS_PATCH_UPDATE = "Failed to process patch update for %s/%s";
        public static final String SUCCESSFULLY_PATCHED_CONFIGURATION = "Successfully patched configuration for %s/%s";
        public static final String DELETED_CONFIGURATION = "Deleted configuration: %s/%s";
        public static final String ERROR_GETTING_DECRYPTING_DATA = "Error getting/decrypting data for %s/%s";
        public static final String FAILED_TO_SAVE_INLINE_CONFIGURATION = "Failed to save inline configuration for %s/%s";
        public static final String FAILED_TO_SAVE_ARTIFACT_CONFIGURATION = "Failed to save artifact configuration for %s/%s";

        public static final String LOG_ENTER_GET_CONFIGURATION_SERVICE = "Entering getConfigurationService";
        public static final String LOG_EXIT_GET_CONFIGURATION_SERVICE = "Exiting getConfigurationService";
        public static final String LOG_ENTER_GET_LDAP_CONFIG = "Entering getLdapConfig";
        public static final String LOG_EXIT_GET_LDAP_CONFIG = "Exiting getLdapConfig";
        public static final String LOG_ENTER_GET_SAML_CONFIG = "Entering getSamlConfig";
        public static final String LOG_EXIT_GET_SAML_CONFIG = "Exiting getSamlConfig";
        public static final String LOG_ENTER_GET_OAUTH2_CONFIG = "Entering getOAuth2Config";
        public static final String LOG_EXIT_GET_OAUTH2_CONFIG = "Exiting getOAuth2Config";
        public static final String LOG_ENTER_SAVE_LDAP_CONFIG = "Entering saveLdapConfig";
        public static final String LOG_EXIT_SAVE_LDAP_CONFIG = "Exiting saveLdapConfig";
        public static final String LOG_ENTER_SAVE_SAML_CONFIG = "Entering saveSamlConfig";
        public static final String LOG_EXIT_SAVE_SAML_CONFIG = "Exiting saveSamlConfig";
        public static final String LOG_ENTER_SAVE_OAUTH2_CONFIG = "Entering saveOAuth2Config";
        public static final String LOG_EXIT_SAVE_OAUTH2_CONFIG = "Exiting saveOAuth2Config";
        public static final String LOG_ENTER_DELETE_PROVIDER_CONFIG = "Entering deleteProviderConfig for provider: %s";
        public static final String LOG_EXIT_DELETE_PROVIDER_CONFIG_NOT_FOUND = "Exiting deleteProviderConfig: provider not found: %s";
        public static final String LOG_EXIT_DELETE_PROVIDER_CONFIG = "Exiting deleteProviderConfig for provider: %s";
        public static final String LOG_ENTER_GET_CONFIG_FOR_KEY = "Entering getConfig for key: %s, class: %s";
        public static final String LOG_EXIT_GET_CONFIG_FOR_KEY_NOT_FOUND = "Exiting getConfig: not found for key: %s, class: %s";
        public static final String LOG_EXIT_GET_CONFIG_FOR_KEY_SUCCESS = "Exiting getConfig: success for key: %s, class: %s";
        public static final String LOG_EXIT_GET_CONFIG_FOR_KEY_ERROR = "Exiting getConfig: error for key: %s, class: %s";
        public static final String LOG_ENTER_SAVE_CONFIG_FOR_KEY = "Entering saveConfig for key: %s";
        public static final String LOG_EXIT_SAVE_CONFIG_FOR_KEY = "Exiting saveConfig for key: %s";
        public static final String DEFAULT_SAML_AUTH_ERROR_MESSAGE = "SAML authentication failed. Redirecting to front-end.";

        public static final String FAILURE_EVENT_TYPE = "failure";
        public static final String SUCCESS_EVENT_TYPE = "success";

        public static final String LOGIN_EVENT = "login";
        public static final String LOGOUT_EVENT = "logout";
        public static final String PASSWORD_UPDATE_EVENT = "password update";
        public static final String PASSWORD_RESET_EVENT = "password reset";
        public static final String CONFIG_VIEW_EVENT = "VIEW";
        public static final String CONFIG_DELETE_EVENT = "DELETE";
        public static final String CONFIG_UPDATE_EVENT = "UPDATE";

        public static final String EVENT_RISK_LOW = "low";
        public static final String EVENT_RISK_MEDIUM = "medium";
        public static final String EVENT_RISK_HIGH = "high";

        public static final String USER_EVENT_TYPE = "user";
        public static final String CONFIG_EVENT_TYPE = "CONFIG";

        // Audit log messages
        public static final String AUDIT_LOG_SAVE_SUCCESS_ASYNC = "Audit log saved successfully (async)";
        public static final String AUDIT_LOG_SAVE_FAILURE_ASYNC = "Failed to save audit log (async): ";

        public static final String AUDIT_LOG_SAVE_SUCCESS_SYNC = "Audit log saved successfully (sync)";
        public static final String AUDIT_LOG_SAVE_FAILURE_SYNC = "Failed to save audit log (sync): ";

        // Audit log details keys
        public static final String OPERATION = "AUDIT_LOG_SAVE";
        public static final String OBJECT_TYPE = "auditLog";
        public static final String OBJECT_ID_UNKNOWN = "unknown";

        // Async flag values
        public static final String ASYNC_TRUE = "true";
        public static final String ASYNC_FALSE = "false";

        // Exception detail keys
        public static final String EXCEPTION_TYPE = "exceptionType";
        public static final String EXCEPTION_MESSAGE = "exceptionMessage";

        // Common details keys
        public static final String DETAIL_ACTION = "action";
        public static final String DETAIL_USER_ID = "userId";
        public static final String DETAIL_CLIENT_IP = "clientIP";
        public static final String DETAIL_USER_AGENT = "userAgent";
        public static final String DETAIL_ASYNC = "async";

        // SSO Configuration Manager unique constants
        public static final String SSO_CONFIG_MANAGER_LOGGER_NAME = "SsoConfigManager";
        public static final String SSO_CONFIG_MANAGER_LOG_CATEGORY = "config-sso";
        public static final String LOG_CONFIG_NOT_FOUND_FOR_KEY = "Configuration not found for key";
        public static final String LOG_CONFIG_RETRIEVED_MAPPED_SUCCESS = "Successfully retrieved and mapped config";
        public static final String ERROR_MAPPING_CONFIG_FOR_KEY = "Error mapping configuration for key: ";
        public static final String LOG_ATTEMPTED_DELETE_UNKNOWN_PROVIDER = "Attempted to delete unknown provider";
        public static final String DETAIL_KEY_CONFIG_KEY = "configKey";
        public static final String DETAIL_KEY_CONFIG_CLASS = "configClass";
        public static final String DETAIL_KEY_CONFIG_ID = "configId";
        public static final String DETAIL_KEY_PROVIDER = "provider";
        public static final String PROVIDER_LDAP = "ldap";
        public static final String PROVIDER_SAML = "saml";
        public static final String PROVIDER_OAUTH2 = "oauth2";
        public static final String CONFIG_TYPE_KEY = "config";

        // --- SSO Controller logging and error messages ---
        public static final String LOG_REQUEST_BODY_EMPTY_OR_INVALID = "Request body is empty or invalid for provider: ";
        public static final String LOG_ATTEMPT_TO_SAVE_UNKNOWN_PROVIDER = "Attempt to save unknown SSO provider: ";
        public static final String LOG_ATTEMPT_TO_TOGGLE_UNKNOWN_PROVIDER = "Attempt to toggle unknown provider: ";
        public static final String LOG_CONFIG_NOT_FOUND_FOR_PROVIDER_IN_TOGGLE = "Configuration not found for provider in toggle: ";
        public static final String LOG_ATTEMPT_TO_PATCH_UNKNOWN_PROVIDER = "Attempt to patch unknown provider: ";
        public static final String LOG_PROVIDER_CONFIG_NOT_FOUND_OR_COULD_NOT_DELETE = "Provider config not found or could not be deleted: ";
        public static final String LOG_FAILED_TO_DESERIALIZE_SSO_CONFIG = "Failed to deserialize SSO config to ";
        public static final String LOG_FAILED_TO_UPDATE_PROVIDER_CONFIG = "Failed to update provider config for ";
        public static final String LOG_FAILED_TO_TOGGLE_PROVIDER = "Failed to toggle provider ";
        public static final String LOG_FAILED_TO_PATCH_PROVIDER_CONFIG = "Failed to patch provider config for ";

        // SSO Controller toggle messages
        public static final String LOG_TOGGLING_PROVIDER_STATE = "Toggling provider state";
        public static final String LOG_SUCCESSFULLY_TOGGLED_PROVIDER_TO_STATE = "Successfully toggled provider %s to %s";

        // SSO Controller patch messages
        public static final String LOG_SUCCESSFULLY_PATCHED_PROVIDER_WITH_VERSION = "Successfully patched provider config for %s, new version: %s";

        // SSO Controller audit messages
        public static final String LOG_AUDIT_ACTION_NOT_FOUND = "Could not find audit action for provider '%s' and event '%s'";
        public static final String LOG_FAILED_TO_CREATE_AUDIT_LOG = "Failed to create audit log for provider: ";
        public static final String LOG_COULD_NOT_EXTRACT_USER_ID = "Could not extract user ID from security context: ";

        // SSO Controller system constants
        public static final String SYSTEM_VALUE = "system";

        // SSO Controller logger constants
        public static final String SSO_CONFIG_CONTROLLER_LOGGER_NAME = "SsoConfigController";
        public static final String SSO_CONFIG_CONTROLLER_LOG_CATEGORY = "config-sso";

        // SSO Controller trace messages
        public static final String LOG_ENTERING_GET_ALL_SSO_CONFIGURATIONS = "Entering getAllSsoConfigurations";
        public static final String LOG_EXITING_GET_ALL_SSO_CONFIGURATIONS = "Exiting getAllSsoConfigurations";
        public static final String LOG_ENTERING_GET_SSO_CONFIGURATION = "Entering getSsoConfiguration";
        public static final String LOG_EXITING_GET_SSO_CONFIGURATION = "Exiting getSsoConfiguration";
        public static final String LOG_ENTERING_SAVE_SSO_CONFIGURATION = "Entering saveSsoConfiguration";
        public static final String LOG_ENTERING_TOGGLE_PROVIDER = "Entering toggleProvider";
        public static final String LOG_ENTERING_UPDATE_PROVIDER_CONFIG = "Entering updateProviderConfig";
        public static final String LOG_ENTERING_DELETE_PROVIDER_CONFIG = "Entering deleteProviderConfig";
        public static final String LOG_SAVING_SSO_PROVIDER_CONFIGURATION = "Saving SSO provider configuration";
        public static final String LOG_SUCCESSFULLY_DELETED_SSO_PROVIDER = "Successfully deleted provider config: %s";

        // SSO Controller status/state constants
        public static final String ENABLED_LOWERCASE = "enabled";
        public static final String DISABLED_LOWERCASE = "disabled";
        public static final String VERSION_KEY = "version";
        public static final String STATUS_KEY = "status";
        public static final String MESSAGE_KEY = "message";
        public static final String SSO = "SSO";
        public static final String ALL = "ALL";
        public static final String ENCRYPT_ENTIRE_CONFIGRATION = "encryptEntireConfig";
        public static final String IS_FULLY_ENCRYPED = "isFullyEncrypted";
        public static final String LOG_PATCH_STARTED_FORMAT = "Patch operation started for config: %s/%s. Encrypt entire config: %b";
        public static final String LOG_PATCH_SUCCESS_FORMAT = "Successfully patched configuration: %s/%s";
        public static final String LOG_DELETED_CONFIG_FORMAT = "Deleted configuration: %s/%s";
        public static final String LOG_SAVING_INLINE_CONFIG = "Saving inline configuration";
        public static final String LOG_SAVING_ARTIFACT_CONFIG = "Saving artifact configuration";

        // --- Logger Configuration ---
        public static final String LOGGER_NAME = "ConfigurationController";
        public static final String LOG_CATEGORY = "config";

        // --- Log Detail Keys ---
        public static final String DETAIL_CONFIG_TYPE = "configType";
        public static final String DETAIL_CONFIG_KEY = "configKey";
        public static final String DETAIL_EXCEPTION_TYPE = "exceptionType";
        public static final String DETAIL_EXCEPTION_MESSAGE = "exceptionMessage";
        public static final String DETAIL_STACK_TRACE = "stackTrace";
        public static final String DETAIL_COUNT = "count";
        public static final String DETAIL_EXISTS = "exists";

        // --- Method Names (for consistent logging) ---
        public static final String METHOD_GET_CONFIGURATION = "getConfiguration";
        public static final String METHOD_GET_CONFIGS_BY_TYPE = "getConfigurationsByType";
        public static final String METHOD_GET_ALL_CONFIGS = "getAllConfigurations";
        public static final String METHOD_SAVE_CONFIGURATION = "saveConfiguration";
        public static final String METHOD_DELETE_CONFIGURATION = "deleteConfiguration";
        public static final String METHOD_CONFIGURATION_EXISTS = "configurationExists";
        public static final String METHOD_GET_SUPPORTED_TYPES = "getSupportedConfigurationTypes";
        public static final String METHOD_GET_RAW_DATA = "getRawConfigurationData";
        public static final String METHOD_PARTIAL_UPDATE = "partialUpdateConfiguration";

        // --- General Log Message Formats ---
        public static final String LOG_ENTERING_METHOD_FORMAT = "Entering %s";
        public static final String LOG_BAD_REQUEST_IN_METHOD_FORMAT = "Bad request in %s";
        public static final String LOG_ERROR_IN_METHOD_FORMAT = "Error in %s";

        // --- Specific Log & Audit Messages (Plain & Formatted) ---
        public static final String LOG_SUCCESS_GET_CONFIG = "Successfully retrieved configuration";
        public static final String LOG_CONFIG_NOT_FOUND = "Configuration not found";
        public static final String LOG_SUCCESS_GET_BY_TYPE = "Successfully retrieved configurations by type";
        public static final String LOG_SUCCESS_GET_ALL = "Successfully retrieved all configurations";
        public static final String LOG_SUCCESS_SAVE_FORMAT = "Successfully saved configuration %s/%s";
        public static final String LOG_ERROR_SAVING_FORMAT = "Error saving configuration for %s/%s";
        public static final String LOG_SUCCESS_DELETE_FORMAT = "Successfully deleted configuration %s/%s";
        public static final String LOG_WARN_DELETE_NONEXISTENT_FORMAT = "Attempt to delete non-existent configuration: %s/%s";
        public static final String LOG_CONFIG_EXISTS_COMPLETE = "Configuration existence check complete";
        public static final String LOG_SUCCESS_GET_RAW = "Successfully retrieved raw configuration data";
        public static final String LOG_RAW_CONFIG_NOT_FOUND = "Raw configuration not found";
        public static final String LOG_ERROR_GET_RAW_FORMAT = "Error retrieving raw configuration data for %s/%s";
        public static final String LOG_SUCCESS_PATCH_FORMAT = "Successfully patched configuration %s/%s";
        public static final String LOG_ERROR_PATCHING_FORMAT = "Error patching configuration for %s/%s";

        // --- Audit Message Formats ---
        public static final String AUDIT_VIEWED_CONFIGURATION_FORMAT = "Viewed configuration: %s/%s";
        public static final String AUDIT_CONFIGURATION_NOT_FOUND_FORMAT = "Attempted to view non-existent configuration: %s/%s";
        public static final String AUDIT_VIEW_FAILED_BAD_REQUEST_FORMAT = "Failed to view configuration due to bad request: %s";
        public static final String AUDIT_VIEW_ERROR_FORMAT = "Error viewing configuration: %s";
        public static final String AUDIT_LISTED_CONFIGURATIONS_BY_TYPE_FORMAT = "Listed all configurations of type: %s";
        public static final String AUDIT_LIST_CONFIGURATIONS_BY_TYPE_FAILED_FORMAT = "Failed to list configurations of type %s: %s";
        public static final String AUDIT_CREATED_OR_UPDATED_CONFIGURATION_FORMAT = "Created or updated configuration: %s/%s";
        public static final String AUDIT_CREATE_OR_UPDATE_CONFIGURATION_FAILED_FORMAT = "Failed to create/update configuration %s/%s: %s";
        public static final String AUDIT_CREATE_OR_UPDATE_CONFIGURATION_ERROR_FORMAT = "Error creating/updating configuration %s/%s: %s";
        public static final String AUDIT_DELETED_CONFIGURATION_FORMAT = "Deleted configuration: %s/%s";
        public static final String AUDIT_DELETE_CONFIGURATION_NOT_FOUND_FORMAT = "Attempted to delete non-existent configuration: %s/%s";
        public static final String AUDIT_DELETE_CONFIGURATION_FAILED_FORMAT = "Failed to delete configuration %s/%s: %s";
        public static final String AUDIT_CHECKED_CONFIGURATION_EXISTS_FORMAT = "Checked existence of configuration: %s/%s";
        public static final String AUDIT_CHECK_CONFIGURATION_EXISTS_FAILED_FORMAT = "Failed to check existence of configuration %s/%s: %s";
        public static final String AUDIT_VIEWED_RAW_CONFIGURATION_DATA_FORMAT = "Viewed raw configuration data for: %s/%s";
        public static final String AUDIT_RAW_CONFIGURATION_DATA_NOT_FOUND_FORMAT = "Attempted to view non-existent raw configuration data for: %s/%s";
        public static final String AUDIT_VIEW_RAW_CONFIGURATION_DATA_FAILED_FORMAT = "Failed to view raw configuration data for %s/%s: %s";
        public static final String AUDIT_VIEW_RAW_CONFIGURATION_DATA_ERROR_FORMAT = "Error viewing raw configuration data for %s/%s: %s";
        public static final String AUDIT_PATCHED_CONFIGURATION_FORMAT = "Patched configuration: %s/%s";
        public static final String AUDIT_PATCH_CONFIGURATION_FAILED_FORMAT = "Failed to patch configuration %s/%s: %s";
        public static final String AUDIT_PATCH_CONFIGURATION_ERROR_FORMAT = "Error patching configuration %s/%s: %s";

        // Logger names
        public static final String LDAP_LOGGER_NAME = "LdapAuthProvider";
        public static final String LDAP_LOGGER_CONTEXT = "ldap-auth";

        // Log messages
        public static final String MSG_AUTH_ATTEMPT = "DynamicLdapAuthenticationProvider attempting to authenticate user";
        public static final String MSG_LDAP_NOT_ENABLED = "LDAP is not enabled or configured. This provider will not handle the authentication request.";
        public static final String MSG_LDAP_AUTH = "Performing LDAP authentication for user";
        public static final String MSG_LDAP_CONFIG_CHANGED = "LDAP configuration has changed or is not initialized. Creating new LdapAuthenticationProvider instance.";
        public static final String MSG_CREATING_LDAP_PROVIDER = "Creating new LDAP provider instance";
        public static final String MSG_USING_BIND_DN = "Using bindDn for LDAP search";
        public static final String MSG_NO_BIND_DN = "No bindDn/bindPassword provided. Using user's credentials for search.";
        public static final String MSG_LDAP_PROVIDER_SUCCESS = "Successfully constructed new LDAP authentication provider with referral following enabled.";
        public static final String MSG_FAILED_CHECK_LDAP_STATUS = "Failed to check LDAP status";
        public static final String MSG_FAILED_GET_LDAP_CONFIG = "Failed to get LDAP configuration";
        public static final String MSG_FATAL_LDAP_CREATION = "Fatal error while creating LDAP authentication provider. Authentication will fail until this is resolved.";

        // Exception messages
        public static final String EXC_MSG_LDAP_PROVIDER_CREATION = "Failed to create LDAP authentication provider";

        // ===================================================================
        // SAML CONFIGURATION CONSTANTS
        // ===================================================================

        // SAML Logger Names and Categories
        public static final String SAML_LOGGER_NAME = "SsoConfigManager";
        public static final String SAML_LOGGER_CATEGORY = "config-sso";
        public static final String SAML_CONFIG_CATEGORY = "saml-config";

        // SAML Registration IDs
        public static final String SAML_PROVIDER_REGISTRATION_ID = "saml-provider";

        // SAML Binding Types
        public static final String SAML_BINDING_REDIRECT = "REDIRECT";
        public static final String SAML_BINDING_POST = "POST";

        // SAML Certificate Headers
        public static final String CERT_BEGIN_HEADER = "-----BEGIN CERTIFICATE-----";
        public static final String CERT_END_HEADER = "-----END CERTIFICATE-----";
        public static final String CERT_TYPE_X509 = "X.509";

        // SAML NameID Format URNs
        public static final String NAMEID_FORMAT_UNSPECIFIED = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";
        public static final String NAMEID_FORMAT_EMAIL = "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress";
        public static final String NAMEID_FORMAT_ENTITY = "urn:oasis:names:tc:SAML:2.0:nameid-format:entity";
        public static final String NAMEID_FORMAT_KERBEROS = "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos";
        public static final String NAMEID_FORMAT_PERSISTENT = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";
        public static final String NAMEID_FORMAT_TRANSIENT = "urn:oasis:names:tc:SAML:2.0:nameid-format:transient";
        public static final String NAMEID_FORMAT_WINDOWS_DOMAIN = "urn:oasis:names:tc:SAML:1.1:nameid-format:WindowsDomainQualifiedName";
        public static final String NAMEID_FORMAT_X509_SUBJECT = "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName";
        public static final String NAMEID_URN_PREFIX = "urn:oasis:names:tc:SAML:";

        // SAML NameID Format Short Names
        public static final String NAMEID_SHORT_EMAIL = "emailaddress";
        public static final String NAMEID_SHORT_EMAIL_ALT = "email";
        public static final String NAMEID_SHORT_ENTITY = "entity";
        public static final String NAMEID_SHORT_KERBEROS = "kerberos";
        public static final String NAMEID_SHORT_PERSISTENT = "persistent";
        public static final String NAMEID_SHORT_TRANSIENT = "transient";
        public static final String NAMEID_SHORT_UNSPECIFIED = "unspecified";
        public static final String NAMEID_SHORT_WINDOWS_DOMAIN = "windowsdomainqualifiedname";
        public static final String NAMEID_SHORT_X509_SUBJECT = "x509subjectname";

        // SAML Log Messages
        public static final String LOG_SAML_CONFIG_NOT_FOUND = "SAML config not found or disabled for registrationId";
        public static final String LOG_SAML_FIND_REGISTRATION_FAILED = "Failed to find SAML relying party registration for id: ";
        public static final String LOG_SAML_MAPPING_FAILED = "Failed to map SamlConfig to RelyingPartyRegistration";
        public static final String LOG_SAML_CERT_MISSING = "IdP verification certificate is missing or invalid. Assertions cannot be verified.";
        public static final String LOG_SAML_CERT_NOT_CONFIGURED = "IdP X.509 certificate is not configured.";
        public static final String LOG_SAML_CERT_PARSE_FAILED = "Failed to load or parse IdP certificate";
        public static final String LOG_SAML_NAMEID_UNKNOWN = "Unknown NameID format specified, falling back to unspecified.";
        public static final String LOG_SAML_GET_ALL_REGISTRATIONS_FAILED = "Failed to retrieve all SAML relying party registrations";
        public static final String LOG_SAML_STATUS_CHECK_FAILED = "Failed to check SAML status";

        // SAML Log Detail Keys
        public static final String LOG_DETAIL_REGISTRATION_ID = "registrationId";
        public static final String LOG_DETAIL_PROVIDED_FORMAT = "providedFormat";

        // SAML Regex Patterns
        public static final String WHITESPACE_REGEX = "\\s";

        // ===================================================================
        // OAUTH2 CONFIGURATION CONSTANTS
        // ===================================================================

        // OAuth2 Logger Names and Categories
        public static final String OAUTH2_LOGGER_NAME = "OAuth2ClientRegistration";
        public static final String OAUTH2_LOGGER_CATEGORY = "oauth2-config";

        // OAuth2 Default Values
        public static final String OAUTH2_DEFAULT_CLIENT_NAME = "OAuth2 Provider";
        public static final String OAUTH2_DEFAULT_USERNAME_ATTRIBUTE = "sub";

        // OAuth2 Default Scopes
        public static final String OAUTH2_SCOPE_OPENID = "openid";
        public static final String OAUTH2_SCOPE_PROFILE = "profile";
        public static final String OAUTH2_SCOPE_EMAIL = "email";

        // OAuth2 Log Messages
        public static final String LOG_OAUTH2_CONFIG_NOT_FOUND = "OAuth2 config not found or disabled for registrationId";
        public static final String LOG_OAUTH2_FIND_REGISTRATION_FAILED = "Failed to find OAuth2 client registration for id: ";
        public static final String LOG_OAUTH2_CONFIG_INVALID = "OAuth2 configuration is invalid or incomplete";
        public static final String LOG_OAUTH2_GET_ALL_REGISTRATIONS_FAILED = "Failed to retrieve all OAuth2 client registrations";
        public static final String LOG_OAUTH2_STATUS_CHECK_FAILED = "Failed to check OAuth2 status";

        // OAuth2 Authentication Config Constants
        public static final String OAUTH2_AUTH_CONFIG_LOGGER_NAME = "Oauth2AuthConfig";
        public static final String OAUTH2_AUTH_CONFIG_LOGGER_CATEGORY = "config-oauth2";

        // OAuth2 Authentication Config Messages (Additional)
        public static final String LOG_OAUTH2_CLIENT_REGISTERED = "Successfully created and registered OAuth2 client";
        public static final String LOG_OAUTH2_JWK_SET_URI_CONFIGURED = "Configuring JWK Set URI for OAuth2 client";
        public static final String LOG_OAUTH2_JWK_SET_URI_EMPTY = "JWK Set URI is empty or not provided for OAuth2 client";
        public static final String LOG_OAUTH2_BUILDING_REGISTRATION = "Building OAuth2 client registration";

        // OAuth2 Log Detail Keys
        public static final String LOG_DETAIL_JWK_SET_URI = "jwkSetUri";
        public static final String LOG_DETAIL_CLIENT_NAME = "clientName";
        public static final String LOG_DETAIL_CLIENT_ID = "clientId";
        public static final String LOG_DETAIL_REDIRECT_URI = "redirectUri";

        // ===================================================================
        // ARTIFACT SERVICE CONSTANTS
        // ===================================================================

        // Artifact Service Logger Configuration
        public static final String ARTIFACT_SERVICE_LOGGER_NAME = "ConfigurationArtifactService";
        public static final String ARTIFACT_SERVICE_LOGGER_CATEGORY = "config-artifact";

        // Artifact Configuration Values
        public static final String ARTIFACT_CONFIG_SUBTYPE = "config";
        public static final String ARTIFACT_BASE_PATH_TEMPLATE = "./encryption-storage/configurations/%s/%s";
        public static final String ARTIFACT_ENCRYPTED_EXTENSION = ".enc";
        public static final String ARTIFACT_MAP_KEY_PREFIX = "artifact_";
        public static final String ARTIFACT_NULL_VALUE = "null";

        // Artifact Map Keys
        public static final String ARTIFACT_MAP_TYPE = "artifactType";
        public static final String ARTIFACT_MAP_FILE_PATH = "filePath";
        public static final String ARTIFACT_MAP_FILE_NAME = "fileName";
        public static final String ARTIFACT_MAP_ENCRYPTED = "encrypted";
        public static final String ARTIFACT_MAP_CONTENT = "content";
        public static final String ARTIFACT_MAP_ERROR = "error";

        // Artifact Log Messages
        public static final String LOG_ARTIFACT_SAVED = "Artifact saved to disk";
        public static final String LOG_ARTIFACT_ENCRYPTED_SAVED = "Saved encrypted file for %s to %s";
        public static final String LOG_ARTIFACT_SAVED_PLAIN = "Saved file for %s to %s";
        public static final String LOG_ARTIFACT_NOT_RETRIEVED = "Artifact content not retrieved; artifact is null, inactive, or has no file path";
        public static final String LOG_ARTIFACT_DECRYPTED = "Successfully decrypted artifact content";
        public static final String LOG_ARTIFACT_READ = "Successfully read artifact content";
        public static final String LOG_ARTIFACT_READ_FAILED = "Failed to read artifact file: %s";
        public static final String LOG_ARTIFACT_DECRYPT_FAILED = "Failed to decrypt artifact file: %s";
        public static final String LOG_ARTIFACT_METADATA_PROCESSED = "Processed artifact metadata";
        public static final String LOG_ARTIFACT_DELETED = "Deleted artifact file: %s";
        public static final String LOG_ARTIFACT_PHYSICAL_DELETED = "Physical artifact file deleted";
        public static final String LOG_ARTIFACT_DELETE_FAILED = "Failed to delete physical artifact file: %s %s";
        public static final String LOG_ARTIFACT_BULK_DELETE = "Deleting physical artifact as part of bulk delete";

        // Artifact Log Detail Keys
        public static final String LOG_DETAIL_FILE_PATH = "filePath";
        public static final String LOG_DETAIL_ENCRYPTED = "encrypted";
        public static final String LOG_DETAIL_ARTIFACT = "artifact";

        // File Extensions
        public static final String EXT_CERTIFICATE = ".crt";
        public static final String EXT_KEYSTORE = ".jks";
        public static final String EXT_KEY = ".key";
        public static final String EXT_PUBLIC_KEY = ".pub";
        public static final String EXT_PEM = ".pem";
        public static final String EXT_P12 = ".p12";
        public static final String EXT_XML = ".xml";
        public static final String EXT_PROPERTIES = ".properties";
        public static final String EXT_YAML = ".yml";
        public static final String EXT_JSON = ".json";
        public static final String EXT_TEXT = ".txt";
        public static final String EXT_DEFAULT = ".dat";

        // File Type Names
        public static final String FILE_TYPE_CERTIFICATE = "certificate";
        public static final String FILE_TYPE_CERT = "cert";
        public static final String FILE_TYPE_KEYSTORE = "keystore";
        public static final String FILE_TYPE_JKS = "jks";
        public static final String FILE_TYPE_KEY = "key";
        public static final String FILE_TYPE_PRIVATE_KEY = "privatekey";
        public static final String FILE_TYPE_PUBLIC_KEY = "publickey";
        public static final String FILE_TYPE_PEM = "pem";
        public static final String FILE_TYPE_P12 = "p12";
        public static final String FILE_TYPE_PFX = "pfx";
        public static final String FILE_TYPE_XML = "xml";
        public static final String FILE_TYPE_PROPERTIES = "properties";
        public static final String FILE_TYPE_YAML = "yaml";
        public static final String FILE_TYPE_YML = "yml";
        public static final String FILE_TYPE_JSON = "json";
        public static final String FILE_TYPE_TEXT = "text";
        public static final String FILE_TYPE_TXT = "txt";

        // ===================================================================
        // CONFIGURATION ENCRYPTION SERVICE CONSTANTS
        // ===================================================================

        // Configuration Encryption Service Logger Configuration
        public static final String CONFIG_ENCRYPTION_SERVICE_LOGGER_NAME = "ConfigurationEncryptionService";
        public static final String CONFIG_ENCRYPTION_SERVICE_LOGGER_CATEGORY = "encryption-config";

        // Encryption Categories
        public static final String ENCRYPTION_CATEGORY = "config-encryption";
        public static final String DECRYPTION_CATEGORY = "config-decryption";

        // Encryption JSON Fields
        public static final String ENCRYPTION_FIELD_ENCRYPTED = "encrypted";
        public static final String ENCRYPTION_FIELD_DATA = "data";

        // Encryption Log Messages
        public static final String LOG_FULL_ENCRYPTION_APPLIED = "Applying full encryption based on request flag";
        public static final String LOG_FIELD_ENCRYPTION_APPLIED = "Applying field-level encryption based on rules";
        public static final String LOG_NO_ENCRYPTION_RULE_MATCHED = "No encryption rule matched, checking for sensitive fields anyway";
        public static final String LOG_FULL_DECRYPTION_APPLIED = "Applying full decryption based on envelope";
        public static final String LOG_FIELD_DECRYPTION_APPLIED = "Applying field-level decryption";

        // Encryption Log Detail Keys
        public static final String LOG_DETAIL_CONFIG_TYPE = "configType";
        public static final String LOG_DETAIL_CONFIG_KEY = "configKey";

        // Encryption Algorithm
        public static final String ENCRYPTION_ALGORITHM_AES_256_GCM = "AES-256-GCM";
        public static final String SSO_OAUTH2_CONFIG = "oauth2_config";
        public static final String SSO_SAML_CONFIG = "saml_config";
        public static final String SSO_LDAP_CONFIG = "ldap_config";

        // ===================================================================
        // SESSION CONTROL CONSTANTS
        // ===================================================================

        // Session Control validation messages
        public static final String VALIDATION_JWT_EXPIRATION_MINUTES_REQUIRED = "JWT expiration time in minutes is required";
        public static final String VALIDATION_JWT_EXPIRATION_MINUTES_MIN_VALUE = "JWT expiration must be at least 1 minute";
        public static final String VALIDATION_REFRESH_TOKEN_EXPIRATION_HOURS_REQUIRED = "Refresh token expiration time in hours is required";
        public static final String VALIDATION_REFRESH_TOKEN_EXPIRATION_HOURS_MIN_VALUE = "Refresh token expiration must be at least 1 hour";
        public static final String VALIDATION_FORCE_LOGOUT_ON_PASSWORD_CHANGE_REQUIRED = "Force logout on password change setting is required";

        // Session Control success messages
        public static final String SESSION_CONTROL_CREATED_SUCCESS = "Session control policy created successfully";
        public static final String SESSION_CONTROL_UPDATED_SUCCESS = "Session control policy updated successfully";
        public static final String SESSION_CONTROL_DELETED_SUCCESS = "Session control policy deleted successfully";

        // Session Control error messages
        public static final String SESSION_CONTROL_ALREADY_EXISTS = "Session control policy already exists for this tenant";
        public static final String SESSION_CONTROL_NOT_FOUND = "Session control policy not found for this tenant";
        public static final String SESSION_CONTROL_DELETE_FAILED = "Failed to delete session control policy";

        // Session Control audit messages
        public static final String AUDIT_SESSION_CONTROL_CREATED = "create";
        public static final String AUDIT_SESSION_CONTROL_UPDATED = "update";
        public static final String AUDIT_SESSION_CONTROL_DELETED = "delete";

        // Session Control log messages
        public static final String LOG_SESSION_CONTROL_CREATE_START = "Creating session control policy for tenant: %s";
        public static final String LOG_SESSION_CONTROL_CREATE_SUCCESS = "Session control policy created successfully for tenant: %s";
        public static final String LOG_SESSION_CONTROL_CREATE_ERROR = "Failed to create session control policy for tenant: %s";
        public static final String LOG_SESSION_CONTROL_UPDATE_START = "Updating session control policy for tenant: %s";
        public static final String LOG_SESSION_CONTROL_UPDATE_SUCCESS = "Session control policy updated successfully for tenant: %s";
        public static final String LOG_SESSION_CONTROL_UPDATE_ERROR = "Failed to update session control policy for tenant: %s";
        public static final String LOG_SESSION_CONTROL_DELETE_START = "Deleting session control policy for tenant: %s";
        public static final String LOG_SESSION_CONTROL_DELETE_SUCCESS = "Session control policy deleted successfully for tenant: %s";
        public static final String LOG_SESSION_CONTROL_DELETE_ERROR = "Failed to delete session control policy for tenant: %s";
        public static final String LOG_SESSION_CONTROL_GET_START = "Retrieving session control policy for tenant: %s";
        public static final String LOG_SESSION_CONTROL_GET_SUCCESS = "Session control policy retrieved successfully for tenant: %s";
        public static final String LOG_SESSION_CONTROL_GET_ERROR = "Failed to retrieve session control policy for tenant: %s";

        // Session Control conversion messages
        public static final String LOG_CONVERTING_JWT_MINUTES_TO_MS = "Converting JWT expiration from %d minutes to %d milliseconds";
        public static final String LOG_CONVERTING_REFRESH_HOURS_TO_MS = "Converting refresh token expiration from %d hours to %d milliseconds";

        // Session Control log category
        public static final String LOG_CATEGORY_SESSION_CONTROL = "session-control";

        // Session Control REST endpoint messages
        public static final String REST_CREATE_SESSION_CONTROL = "Creating session control policy for tenant: %s";
        public static final String REST_UPDATE_SESSION_CONTROL = "Updating session control policy for tenant: %s";
        public static final String REST_GET_SESSION_CONTROL = "Retrieving session control policy for tenant: %s";
        public static final String REST_DELETE_SESSION_CONTROL = "Deleting session control policy for tenant: %s";
        public static final String REST_GET_SESSION_EXPIRATION_TIMES = "Getting session expiration times for tenant: %s";

        // Session Control warning messages
        public static final String WARN_SESSION_CONTROL_ALREADY_EXISTS = "Session control policy already exists for tenant: %s";
        public static final String WARN_SESSION_CONTROL_NOT_FOUND = "Session control policy not found for tenant: %s";
        public static final String WARN_SESSION_CONTROL_NOT_FOUND_USING_DEFAULTS = "Session control policy not found for tenant: %s, using default values";

        // Session Control error messages  
        public static final String ERROR_DELETING_SESSION_CONTROL = "Error deleting session control policy for tenant: %s";
        public static final String ERROR_MISSING_INVALID_AUTH_HEADER = "Missing or invalid Authorization header";
        public static final String ERROR_FAILED_EXTRACT_TENANT_ID = "Failed to extract tenant ID from JWT token";
        public static final String ERROR_INVALID_MISSING_JWT = "Invalid or missing JWT token";
        public static final String ERROR_INVALID_JWT_CANNOT_EXTRACT_TENANT = "Invalid JWT token: cannot extract tenant ID";
        public static final String ERROR_FAILED_DELETE_SESSION_CONTROL = "Failed to delete session control policy";

        // Dynamic Token Service log messages
        public static final String LOG_GENERATING_JWT_DYNAMIC = "Generating JWT token with dynamic expiration";
        public static final String LOG_GENERATING_REFRESH_DYNAMIC = "Generating refresh token with dynamic expiration";
        public static final String LOG_USING_TENANT_JWT_EXPIRATION = "Using tenant-specific JWT expiration from session control policy";
        public static final String LOG_NO_POLICY_DEFAULT_JWT = "No session control policy found for tenant, using default JWT expiration";
        public static final String LOG_USING_TENANT_REFRESH_EXPIRATION = "Using tenant-specific refresh token expiration from session control policy";
        public static final String LOG_NO_POLICY_DEFAULT_REFRESH = "No session control policy found for tenant, using default refresh token expiration";
        public static final String LOG_RETRIEVED_FORCE_LOGOUT_SETTING = "Retrieved force logout setting from session control policy";
        public static final String LOG_NO_POLICY_DEFAULT_FORCE_LOGOUT = "No session control policy found for tenant, using default force logout setting (false)";
        public static final String LOG_RETRIEVED_JWT_EXPIRATION_POLICY = "Retrieved JWT expiration from session control policy";
        public static final String LOG_USING_DEFAULT_JWT_EXPIRATION = "Using default JWT expiration";
        public static final String LOG_RETRIEVED_REFRESH_EXPIRATION_POLICY = "Retrieved refresh token expiration from session control policy";
        public static final String LOG_USING_DEFAULT_REFRESH_EXPIRATION = "Using default refresh token expiration";
        public static final String LOG_GENERATING_TOKEN_PAIR = "Generating JWT and refresh token pair with tenant-specific settings";
        
        // Audit Log Detail Keys
        public static final String AUDIT_DETAIL_USER_ID = "userId";
        public static final String AUDIT_DETAIL_USERNAME = "username";
        public static final String AUDIT_DETAIL_TENANT_ID = "tenantId";
        public static final String AUDIT_DETAIL_JWT_EXPIRATION_MS = "jwtExpirationMs";
        public static final String AUDIT_DETAIL_JWT_EXPIRATION_MINUTES = "jwtExpirationMinutes";
        public static final String AUDIT_DETAIL_REFRESH_TOKEN_EXPIRATION_MS = "refreshTokenExpirationMs";
        public static final String AUDIT_DETAIL_REFRESH_TOKEN_EXPIRATION_HOURS = "refreshTokenExpirationHours";
        public static final String AUDIT_DETAIL_FORCE_LOGOUT_ON_PASSWORD_CHANGE = "forceLogoutOnPasswordChange";
        public static final String AUDIT_DETAIL_DEFAULT_JWT_EXPIRATION_MS = "defaultJwtExpirationMs";

        // Update Expired Password Constants
        public static final String ERROR_CURRENT_PASSWORD_INCORRECT = "Current password is incorrect";
        public static final String ERROR_PASSWORD_CONFIRMATION_MISMATCH = "Password confirmation does not match";
        public static final String ERROR_PASSWORD_NOT_EXPIRED = "Password is not expired";
        public static final String ERROR_USER_NOT_FOUND_FOR_PASSWORD_UPDATE = "User not found";
        public static final String SUCCESS_EXPIRED_PASSWORD_UPDATED = "Expired password updated successfully";
        public static final String LOG_UPDATING_EXPIRED_PASSWORD = "Updating expired password for user";
        public static final String LOG_PASSWORD_EXPIRATION_TRACKED = "Password expiration tracking updated";
        public static final String LOG_EXPIRED_PASSWORD_UPDATE_SUCCESS = "Expired password updated successfully";
        public static final String LOG_CATEGORY_PASSWORD_EXPIRY = "password-expiry";
        public static final String FAILED_TO_CREATE_SESSION_CONTROL_POLICY = "Failed to create session control policy";
        public static final String FAILED_TO_UPDATE_SESSION_CONTROL_POLICY = "Failed to update session control policy";    
        public static final String FAILED_TO_RETRIEVE_SESSION_CONTROL_POLICY = "Failed to retrieve session control policy";
        public static final String FAILED_TO_DELETE_SESSION_CONTROL_POLICY = "Failed to delete session control policy";
        public static final String PREPARING_SESSION_CONTROL_AUDIT_EVENT = "Preparing session control audit event";
        public static final String SESSION_CONTROL_AUDIT_EVENT_SENT_SUCCESSFULLY = "Session control audit event sent successfully";
        public static final String FAILED_TO_SEND_SESSION_CONTROL_AUDIT_EVENT = "Failed to send session control audit event";

        // ============================================
        // VALIDATION MESSAGES
        // ============================================
        
        public static final String VALIDATION_CURRENT_PASSWORD_REQUIRED = "Current password is required";
        public static final String VALIDATION_NEW_PASSWORD_REQUIRED = "New password is required";
        public static final String VALIDATION_PASSWORD_CONFIRMATION_REQUIRED = "Password confirmation is required";

        // ============================================
        // JWT UTILITY CONSTANTS
        // ============================================

        // --- JWT Claims ---
        public static final String JWT_CLAIM_KEY_TYPE = "type";
        public static final String JWT_CLAIM_VALUE_REFRESH = "refresh";
        public static final String JWT_CLAIM_VALUE_PASSWORD_RESET = "password_reset";

        // --- JWT Log Messages ---
        public static final String JWT_LOG_GENERATING_ACCESS_TOKEN = "Generating access token";
        public static final String JWT_LOG_GENERATING_ACCESS_TOKEN_CUSTOM = "Generating access token with custom expiration";
        public static final String JWT_LOG_GENERATING_REFRESH_TOKEN = "Generating refresh token";
        public static final String JWT_LOG_GENERATING_REFRESH_TOKEN_CUSTOM = "Generating refresh token with custom expiration";
        public static final String JWT_LOG_GENERATING_PWD_RESET_TOKEN = "Generating password reset token";
        public static final String JWT_LOG_EXTRACTING_ALL_CLAIMS = "Extracting all claims from token";
        public static final String JWT_LOG_SUCCESS_EXTRACTED_CLAIMS = "Successfully extracted claims";
        public static final String JWT_LOG_ERROR_EXTRACTING_CLAIMS = "Error extracting claims from token";
        public static final String JWT_LOG_VALIDATING_REFRESH_TOKEN = "Validating refresh token";
        public static final String JWT_LOG_VALIDATING_PWD_RESET_TOKEN = "Validating password reset token";

        // --- JWT Log Message Formats ---
        public static final String JWT_LOG_SUCCESS_GEN_TOKEN_FORMAT = "Successfully generated %s token for user: %s";
        public static final String JWT_LOG_ERROR_GEN_TOKEN_FORMAT = "Error generating %s token for user: %s";
        public static final String JWT_LOG_SUCCESS_VALIDATE_TOKEN_FORMAT = "%s token validation successful for user: %s";
        public static final String JWT_LOG_WARN_VALIDATE_TOKEN_FAILED_FORMAT = "%s token validation failed for user: %s";
        public static final String JWT_LOG_ERROR_VALIDATE_TOKEN_FORMAT = "Error validating %s token for user: %s";

        // --- JWT Token Types (for logging) ---
        public static final String JWT_TOKEN_TYPE_ACCESS = "access";
        public static final String JWT_TOKEN_TYPE_REFRESH = "refresh";
        public static final String JWT_TOKEN_TYPE_PWD_RESET = "password reset";

        // --- JWT Validation Error Messages ---
        public static final String JWT_ERROR_INVALID_SIGNATURE = "Invalid JWT signature: %s";
        public static final String JWT_ERROR_MALFORMED_TOKEN = "Invalid JWT token: %s";
        public static final String JWT_ERROR_TOKEN_EXPIRED = "JWT token is expired: %s";
        public static final String JWT_ERROR_UNSUPPORTED_TOKEN = "JWT token is unsupported: %s";
        public static final String JWT_ERROR_EMPTY_CLAIMS = "JWT claims string is empty: %s";

        // --- JWT Extraction Error Messages ---
        public static final String JWT_ERROR_EXTRACTING_USER_ID = "Error extracting user ID from JWT token: %s";
        public static final String JWT_ERROR_EXTRACTING_USERNAME = "Error extracting username from JWT token: %s";
        public static final String JWT_ERROR_EXTRACTING_TENANT_ID = "Error extracting tenantId from JWT token: %s";
        public static final String JWT_ERROR_EXTRACTING_EXPIRATION = "Error extracting expiration from JWT token: %s";

        // =========================================================================
        // IP Geolocation Service Constants
        // =========================================================================
        
        // --- IP Geolocation API Configuration ---
        public static final String IP_API_URL = "http://ip-api.com/json/";
        public static final String IP_API_QUERY_FIELDS = "?fields=status,message,country,city";
        
        // --- Location Values ---
        public static final String LOCATION_LOCAL = "Local";
        public static final String LOCATION_UNKNOWN = "Unknown";
        
        // --- IP Geolocation Response Status ---
        public static final String IP_GEO_STATUS_SUCCESS = "success";
        
        // --- IP Geolocation Logging Messages ---
        public static final String LOG_MSG_IP_GEO_LOOKUP = "Looking up geolocation for IP address";
        public static final String LOG_MSG_IP_GEO_FAILED = "Failed to get geolocation for IP";

        // =========================================================================
        // Session Termination Service Constants
        // =========================================================================
        
        // --- Session Termination Endpoint ---
        public static final String ENDPOINT_TERMINATE_SESSION = "/api/v1/auth/sessions/{sessionId}";
        
        // --- Session Termination Response Messages ---
        public static final String SESSION_TERMINATED_SUCCESS = "Session terminated successfully";
        public static final String SESSION_TERMINATION_FAILED = "Failed to terminate session";
        public static final String SESSION_NOT_FOUND = "Session not found";
        public static final String SESSION_UNAUTHORIZED_TERMINATION = "You are not authorized to terminate this session";
        public static final String SESSION_CANNOT_TERMINATE_CURRENT = "Cannot terminate your current active session";
        
        // --- Session Termination Trace Messages ---
        public static final String TRACE_TERMINATE_SESSION_START = "Starting session termination process";
        public static final String TRACE_TERMINATE_SESSION_VALIDATING_AUTH = "Validating authorization for session termination";
        public static final String TRACE_TERMINATE_SESSION_LOADING_SESSION = "Loading session data from database";
        public static final String TRACE_TERMINATE_SESSION_CHECKING_TENANT = "Checking tenant authorization for session";
        public static final String TRACE_TERMINATE_SESSION_DELETING_REFRESH_TOKEN = "Deleting refresh token associated with session";
        public static final String TRACE_TERMINATE_SESSION_DELETING_SESSION = "Deleting session from database";
        public static final String TRACE_TERMINATE_SESSION_SENDING_NOTIFICATION = "Sending WebSocket termination notification to user";
        public static final String TRACE_TERMINATE_SESSION_COMPLETED = "Session termination process completed";
        
        // --- Session Termination Debug Messages ---
        public static final String DEBUG_TERMINATE_SESSION_FOUND = "Session found in database";
        public static final String DEBUG_TERMINATE_SESSION_USER_EXTRACTED = "Extracted user information from session";
        public static final String DEBUG_TERMINATE_SESSION_TENANT_MATCH = "Tenant authorization validated successfully";
        public static final String DEBUG_TERMINATE_SESSION_REFRESH_TOKEN_DELETED = "Refresh token deleted successfully";
        public static final String DEBUG_TERMINATE_SESSION_SESSION_DELETED = "Session deleted from database";
        public static final String DEBUG_TERMINATE_SESSION_NOTIFICATION_SENT = "WebSocket notification sent to user";
        public static final String DEBUG_TERMINATE_SESSION_USER_NOT_CONNECTED = "User not connected via WebSocket, notification skipped";
        
        // --- Session Termination Info Messages ---
        public static final String INFO_TERMINATE_SESSION_SUCCESS = "Session terminated successfully for user";
        public static final String INFO_TERMINATE_SESSION_BY_ADMIN = "Session terminated by administrator";
        
        // --- Session Termination Error Messages ---
        public static final String ERROR_TERMINATE_SESSION_NOT_FOUND = "Session not found for termination";
        public static final String ERROR_TERMINATE_SESSION_UNAUTHORIZED = "Unauthorized session termination attempt";
        public static final String ERROR_TERMINATE_SESSION_TENANT_MISMATCH = "Tenant mismatch during session termination";
        public static final String ERROR_TERMINATE_SESSION_FAILED = "Failed to terminate session";
        public static final String ERROR_TERMINATE_SESSION_DELETE_TOKEN = "Error deleting refresh token during session termination";
        public static final String ERROR_TERMINATE_SESSION_DELETE_SESSION = "Error deleting session from database";
        public static final String ERROR_TERMINATE_SESSION_SEND_NOTIFICATION = "Error sending WebSocket termination notification";
        
        // --- Session Termination Detail Keys ---
        public static final String DETAIL_SESSION_ID = "sessionId";
        public static final String DETAIL_TARGET_USER_ID = "targetUserId";
        public static final String DETAIL_REQUESTING_USER_ID = "requestingUserId";
        public static final String DETAIL_TENANT_ID = "tenantId";
        public static final String DETAIL_REFRESH_TOKEN = "refreshToken";
        public static final String DETAIL_WEBSOCKET_CONNECTED = "websocketConnected";
        public static final String DETAIL_NOTIFICATION_SENT = "notificationSent";
        
        // --- WebSocket Termination Notification ---
        public static final String WEBSOCKET_TERMINATION_MESSAGE = "Your session has been terminated";
        public static final String WEBSOCKET_TERMINATION_REASON_ADMIN = "Session terminated by administrator";
        public static final String WEBSOCKET_TERMINATION_REASON_SECURITY = "Session terminated for security reasons";
        public static final String WEBSOCKET_TERMINATION_TYPE = "SESSION_TERMINATED";
        
        // --- Session Termination WebSocket Logging ---
        public static final String INFO_WEBSOCKET_NOTIFICATION_ATTEMPT = "Attempting to send WebSocket notification to session";
        public static final String INFO_WEBSOCKET_CALLING_SEND = "Calling convertAndSendToUser for session notification";
        public static final String INFO_WEBSOCKET_NOTIFICATION_SENT = "WebSocket notification sent successfully";
        public static final String ERROR_WEBSOCKET_NOTIFICATION_FAILED = "Failed to send WebSocket notification";
        
        // --- Audit Log Constants ---
        public static final String AUDIT_EVENT_TERMINATE = "Terminate Session";
        public static final String AUDIT_OBJECT_TYPE_SESSION = "Session";
        public static final String AUDIT_ACTOR_TYPE_USER = "user";
        public static final String AUDIT_TARGET_OBJECT_TYPE_USER = "user";
        
        // --- Audit Log Messages ---
        public static final String DEBUG_AUDIT_LOG_SENT_SESSION_TERMINATION = "Audit log event sent for session termination";
        public static final String ERROR_AUDIT_LOG_FAILED_SESSION_TERMINATION = "Failed to send audit log event for session termination";
        public static final String AUDIT_MESSAGE_SESSION_TERMINATED = "User session terminated for user: %s";

}