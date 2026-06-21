package com.aidentx.authservice.service.Auth.sso;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.audit.AuditLogEventBuilder;
import com.aidentx.authservice.config.OAuth2Config;
import com.aidentx.authservice.enums.AuthenticationMethod;
import com.aidentx.authservice.exception.OAuth2RedirectException;
import com.aidentx.authservice.exception.UserNotFoundException;
import com.aidentx.authservice.service.DynamicTokenService;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.entity.UserSession;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.AuditLogEventService;
import com.aidentx.authservice.service.RefreshTokenService;
import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.authservice.service.UserAuthenticationMethodService;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.authservice.utils.RequestUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service class for handling OAuth2 authentication and authorization flows.
 * This service manages OAuth2 login initiation, callback handling, and user
 * authentication
 * through external OAuth2 providers. It integrates with the unified logging
 * system for
 * comprehensive audit logging of all OAuth2-related activities.
 */
@Service
public class OAuth2AuthService {

        private final IUnifiedLogger logger;
        private final SsoConfigurationManager ssoConfigurationManager;
        private final UserRepository userRepository;
        private final JwtUtil jwtUtil;
        private final DynamicTokenService dynamicTokenService;
        private final RefreshTokenService refreshTokenService;
        private final UserAuthenticationMethodService userAuthenticationMethodService;
        private final AuditLogEventService auditLogEventService;
        private final com.aidentx.authservice.service.UserSessionService userSessionService;

        @Value("${front.url.sso_callback}")
        private String frontUrl_callback;
        @Value("${front.url}")
        private String frontUrl;

        /**
         * Constructs a new OAuth2AuthService with required dependencies.
         *
         * @param unifiedLoggerFactory            Factory for creating unified loggers
         * @param ssoConfigurationManager         Service for managing SSO
         *                                        configurations
         * @param userRepository                  Repository for user data access
         * @param jwtUtil                         Utility for JWT token operations
         * @param dynamicTokenService             Service for dynamic token generation with tenant policies
         * @param refreshTokenService             Service for managing refresh tokens
         * @param userAuthenticationMethodService Service for tracking user
         *                                        authentication methods
         * @param auditLogEventService            Service for audit logging
         * @param userSessionService              Service for managing user sessions
         */
        public OAuth2AuthService(UnifiedLoggerFactory unifiedLoggerFactory,
                        SsoConfigurationManager ssoConfigurationManager,
                        UserRepository userRepository, JwtUtil jwtUtil, DynamicTokenService dynamicTokenService,
                        RefreshTokenService refreshTokenService,
                        UserAuthenticationMethodService userAuthenticationMethodService,
                        AuditLogEventService auditLogEventService,
                        com.aidentx.authservice.service.UserSessionService userSessionService) {
                this.logger = unifiedLoggerFactory.getLogger("OAuth2AuthService",
                                AppConstants.LOG_CATEGORY_AUTH_OAUTH2);
                this.ssoConfigurationManager = ssoConfigurationManager;
                this.userRepository = userRepository;
                this.jwtUtil = jwtUtil;
                this.dynamicTokenService = dynamicTokenService;
                this.refreshTokenService = refreshTokenService;
                this.userAuthenticationMethodService = userAuthenticationMethodService;
                this.auditLogEventService = auditLogEventService;
                this.userSessionService = userSessionService;
        }

        /**
         * Initiates the OAuth2 login process by checking if OAuth2 is enabled and
         * returning
         * the appropriate redirect URL.
         *
         * @return A Map containing either the redirect URL for OAuth2 authentication or
         *         an error message
         *         if OAuth2 is not enabled. The map will contain:
         *         - For success: "message", "redirectUrl", and "provider"
         *         - For error: "error" with the error message
         */
        public Map<String, Object> initiateOAuth2Login() {
                logger.trace().category(AppConstants.LOG_CATEGORY_SSO)
                                .message(AppConstants.TRACE_INITIATE_OAUTH2_LOGIN_METHOD)
                                .log();
                OAuth2Config oauth2Config = ssoConfigurationManager.getOAuth2Config();
                if (oauth2Config == null || !oauth2Config.isEnabled()) {
                        logger.warn().category(AppConstants.LOG_CATEGORY_SSO)
                                        .message(AppConstants.LOG_OAUTH2_AUTH_NOT_ENABLED)
                                        .detail("operation", AppConstants.OP_OAUTH2_LOGIN_INIT).log();
                        return java.util.Map.of("error", AppConstants.MSG_OAUTH2_AUTH_NOT_ENABLED);
                }
                logger.info().category(AppConstants.LOG_CATEGORY_SSO)
                                .message(String.format(AppConstants.LOG_OAUTH2_LOGIN_INIT, oauth2Config.getName()))
                                .details(Map.of("operation", AppConstants.OP_OAUTH2_LOGIN_INIT, "provider",
                                                oauth2Config.getName()))
                                .log();
                
                String redirectUrl = "/oauth2/authorization/" + oauth2Config.getProviderId();
                return java.util.Map.of("message", AppConstants.MSG_OAUTH2_REDIRECT, "redirectUrl",
                                redirectUrl, "provider", "OAUTH2");
        }

        /**
         * Handles the GET callback from the OAuth2 provider after user authentication.
         * Processes the authentication response and creates necessary tokens for the
         * user.
         *
         * @param code             OAuth2 authorization code
         * @param state            State parameter for CSRF protection
         * @param error            Error code if authentication failed
         * @param errorDescription Detailed error description
         * @param success          Whether the authentication was successful
         * @param message          Additional message from the OAuth2 provider
         * @param authentication   Spring Security Authentication object
         * @return Object containing authentication result or throws
         *         OAuth2RedirectException for redirect
         * @throws OAuth2RedirectException When the user needs to be redirected (success
         *                                 or failure)
         * @throws UserNotFoundException   When the authenticated user is not found in
         *                                 the system
         */
        @Transactional(noRollbackFor = OAuth2RedirectException.class)
        public Object handleOAuth2CallbackGet(String code, String state, String error, String errorDescription,
                        Boolean success, String message, Authentication authentication) {
                logger.trace().category(AppConstants.LOG_CATEGORY_SSO)
                                .message(AppConstants.TRACE_HANDLE_OAUTH2_CALLBACK_GET_METHOD).log();
                HttpServletRequest request = getCurrentHttpRequest();
                String ip = RequestUtils.getClientIP(request);
                String userAgent = RequestUtils.getUserAgent(request);

                if (error != null) {
                        logger.warn().category(AppConstants.LOG_CATEGORY_SSO)
                                        .message(String.format(AppConstants.LOG_OAUTH2_CALLBACK_ERROR, error,
                                                        errorDescription))
                                        .details(Map.of("operation", AppConstants.OP_OAUTH2_CALLBACK, "error", error,
                                                        "errorDescription", errorDescription))
                                        .log();
                        String userMessage = errorDescription != null ? errorDescription
                                        : AppConstants.MSG_OAUTH2_AUTH_FAILED;
                        String errorUrl = frontUrl + "?error="
                                        + java.net.URLEncoder.encode(userMessage,
                                                        java.nio.charset.StandardCharsets.UTF_8);
                        throw new OAuth2RedirectException(errorUrl, userMessage);
                }
                if (success != null && success && authentication != null) {
                        logger.info().category(AppConstants.LOG_CATEGORY_SSO).objectType("user")
                                        .objectId(authentication.getName())
                                        .message(String.format(AppConstants.LOG_OAUTH2_AUTH_SUCCESS,
                                                        authentication.getName()))
                                        .detail("operation", AppConstants.OP_OAUTH2_CALLBACK).log();
                        String email = extractEmailFromOAuth2Authentication(authentication);
                        User foundUser = (email != null) ? userRepository.findByEmail(email).orElse(null) : null;

                        if (email == null || foundUser == null) {
                                handleOAuth2LoginFailure(email, ip, userAgent);
                                String redirectMessage = email == null
                                                ? AppConstants.OAUTH2_AUTH_FAILED_EMAIL_EXTRACTION
                                                : AppConstants.WRONG_USERNAME_OR_PASSWORD;
                                String errorUrl = frontUrl + "?error="
                                                + java.net.URLEncoder.encode(redirectMessage,
                                                                java.nio.charset.StandardCharsets.UTF_8);
                                throw new OAuth2RedirectException(errorUrl, redirectMessage);
                        }

                        return processSuccessfulOAuth2Login(foundUser, ip, userAgent);
                }
                if (code == null || code.isEmpty()) {
                        logger.warn().category(AppConstants.LOG_CATEGORY_SSO)
                                        .message(AppConstants.LOG_OAUTH2_CALLBACK_NO_CODE)
                                        .detail("operation", AppConstants.OP_OAUTH2_CALLBACK).log();
                        return java.util.Map.of("error", "invalid_request", "error_description",
                                        AppConstants.MSG_OAUTH2_INVALID_REQUEST);
                }
                logger.info().category(AppConstants.LOG_CATEGORY_SSO)
                                .message(String.format(AppConstants.LOG_OAUTH2_CALLBACK_RECEIVED, code, state))
                                .details(Map.of("operation", AppConstants.OP_OAUTH2_CALLBACK, "code", code, "state",
                                                state))
                                .log();
                return java.util.Map.of("message", AppConstants.MSG_OAUTH2_AUTH_SUCCESS, "provider", "OAUTH2", "code",
                                code,
                                "state", state != null ? state : "no_state");
        }

        /**
         * Handles the OAuth2 callback for successful authentication.
         * Validates the user exists in the system and updates their last login time.
         *
         * @param authentication Spring Security Authentication object containing OAuth2
         *                       user details
         * @return Map containing authentication success details including username and
         *         provider
         * @throws UserNotFoundException If the authenticated user is not found in the
         *                               system
         */
        public Map<String, Object> handleOAuth2Callback(Authentication authentication) {
                logger.trace().category(AppConstants.LOG_CATEGORY_SSO)
                                .message(AppConstants.TRACE_HANDLE_OAUTH2_CALLBACK_METHOD)
                                .log();
                String email = extractEmailFromOAuth2Authentication(authentication);
                User user = (email != null) ? userRepository.findByEmail(email).orElse(null) : null;

                if (user == null) {
                        handleOAuth2LoginFailure(email, RequestUtils.getClientIP(getCurrentHttpRequest()),
                                        RequestUtils.getUserAgent(getCurrentHttpRequest()));
                        throw new UserNotFoundException(email == null ? AppConstants.OAUTH2_AUTH_FAILED_EMAIL_EXTRACTION
                                        : String.format(AppConstants.USER_NOT_FOUND, email));
                }

                logger.info().category(AppConstants.LOG_CATEGORY_SSO).objectType("user")
                                .objectId(authentication.getName())
                                .message(String.format(AppConstants.LOG_OAUTH2_AUTH_SUCCESS, authentication.getName()))
                                .detail("operation", AppConstants.OP_OAUTH2_CALLBACK_POST).log();
                user.setLastLoginAt(Instant.now());
                userRepository.save(user);
                return java.util.Map.of("message", AppConstants.MSG_OAUTH2_AUTH_SUCCESS, "provider", "OAUTH2",
                                "username",
                                authentication.getName());
        }

        /**
         * Extracts the email address from an OAuth2 authentication token.
         * Handles both OAuth2AuthenticationToken and regular Authentication objects.
         *
         * @param authentication The authentication object to extract email from
         * @return The extracted email address or null if not found
         */
        private String extractEmailFromOAuth2Authentication(Authentication authentication) {
                if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Token) {
                        Map<String, String> userAttributes = extractOAuth2UserAttributes(oauth2Token);
                        String email = userAttributes.get("email");
                        if (email == null) {
                                logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                                                .message(String.format(AppConstants.OAUTH2_USER_ATTRIBUTES_AVAILABLE,
                                                                oauth2Token.getPrincipal().getAttributes()))
                                                .detail("attributes",
                                                                oauth2Token.getPrincipal().getAttributes().toString())
                                                .log();
                        }
                        return email;
                } else {
                        String email = authentication.getName();
                        logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                                        .message(String.format(AppConstants.OAUTH2_AUTH_NAME_AS_EMAIL, email))
                                        .detail("principalType", authentication.getPrincipal().getClass().getName())
                                        .log();
                        return email;
                }
        }

        /**
         * Handles OAuth2 login failures by logging the event and sending audit logs.
         *
         * @param email     The email address that failed to authenticate
         * @param ip        The client IP address
         * @param userAgent The client user agent string
         */
        private void handleOAuth2LoginFailure(String email, String ip, String userAgent) {
                String safeEmail = email != null ? email : "unknown";
                String failureMessage = email == null ? AppConstants.OAUTH2_AUTH_FAILED_EMAIL_EXTRACTION
                                : String.format(AppConstants.OAUTH2_AUTH_FAILED_USER_NOT_FOUND, email);
                auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(null, AppConstants.USER_EVENT_TYPE,
                                safeEmail, AppConstants.UNKNOWN_TENANT_ID, null, AppConstants.USER_EVENT_TYPE, safeEmail, AppConstants.LOGIN_EVENT,
                                AppConstants.FAILURE_EVENT_TYPE, AppConstants.EVENT_RISK_LOW, ip, userAgent,
                                failureMessage, null, null,
                                null, null));
                if (email == null) {
                        logger.error().category(AppConstants.LOG_CATEGORY_SSO)
                                        .message(AppConstants.OAUTH2_AUTH_FAILED_EMAIL_EXTRACTION)
                                        .detail("operation", AppConstants.OP_OAUTH2_CALLBACK).log();
                } else {
                        logger.warn().category(AppConstants.LOG_CATEGORY_SSO).objectType("user").objectId(email)
                                        .message(String.format(AppConstants.OAUTH2_AUTH_FAILED_USER_NOT_FOUND, email))
                                        .detail("operation", AppConstants.OP_OAUTH2_CALLBACK).log();
                }
        }

        /**
         * Processes a successful OAuth2 login by generating tokens, updating user data,
         * and creating audit logs.
         *
         * @param user      The authenticated user
         * @param ip        The client IP address
         * @param userAgent The client user agent string
         * @return Object containing tokens and redirect URL
         * @throws OAuth2RedirectException to redirect the user with the generated
         *                                 tokens
         */
        private Object processSuccessfulOAuth2Login(User user, String ip, String userAgent) {
                // Generate tokens with dynamic expiration based on tenant's session control policy
                DynamicTokenService.TokenPair tokenPair = dynamicTokenService.generateTokenPair(
                    user.getId(), user.getUsername(), user.getTenantId(), user.getIdentityId());
                String accessToken = tokenPair.getJwtToken();
                String refreshTokenStr = tokenPair.getRefreshToken();
                
                // Clean up old refresh tokens and save new one
                refreshTokenService.deleteAllUserTokens(user);
                refreshTokenService.createRefreshToken(user, refreshTokenStr);

                // Create session for the user and capture sessionId
                HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
                UserSession session = userSessionService.createSession(user.getId(), user.getTenantId(), refreshTokenStr, "OAUTH2", httpRequest);
                String sessionId = session.getSessionId().toString();

                userAuthenticationMethodService.trackUserMethod(user, AuthenticationMethod.OAUTH2);

                createAndSendOAuth2AuditEvent(user, ip, userAgent);

                user.setLastLoginAt(Instant.now());
                userRepository.save(user);
                
                logger.info().category(AppConstants.LOG_CATEGORY_AUTH).objectType("user").objectId(user.getUsername())
                                .message(String.format(AppConstants.LOGIN_SUCCESS, user.getUsername()))
                                .details(Map.of("authMethod", "OAUTH2", "operation", AppConstants.OP_OAUTH2_LOGIN,
                                                "clientIP", ip, "userId", user.getId().toString(), "userAgent",
                                                userAgent))
                                .log();
                String redirectUrl = String.format("%s%s?token=%s&refresh=%s&sessionId=%s", frontUrl, frontUrl_callback, accessToken,
                                refreshTokenStr, sessionId);
                throw new OAuth2RedirectException(redirectUrl, null);
        }

        /**
         * Creates and sends an audit event for successful OAuth2 authentication.
         *
         * @param user      The authenticated user
         * @param ip        The client IP address
         * @param userAgent The client user agent string
         */
        private void createAndSendOAuth2AuditEvent(User user, String ip, String userAgent) {
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode customMetadata = objectMapper.createObjectNode();
                customMetadata.put("username", user.getUsername());
                customMetadata.put("email", user.getEmail());
                customMetadata.put("type", "user");
                customMetadata.put("authenticationMethod", "OAUTH2");
                ObjectNode stateBefore = objectMapper.createObjectNode();
                stateBefore.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "");
                ObjectNode stateAfter = objectMapper.createObjectNode();
                stateAfter.put("lastLoginAt", Instant.now().toString());

                auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(user.getId(),
                                AppConstants.USER_EVENT_TYPE,
                                user.getUsername(), user.getTenantId(), user.getId(), AppConstants.USER_EVENT_TYPE, user.getUsername(),
                                AppConstants.LOGIN_EVENT, AppConstants.SUCCESS_EVENT_TYPE, AppConstants.EVENT_RISK_LOW,
                                ip, userAgent,
                                String.format(AppConstants.LOGIN_SUCCESS, user.getUsername()), stateBefore, stateAfter,
                                null,
                                customMetadata));
        }

        /**
         * Extracts user attributes from an OAuth2 authentication token based on
         * configured mappings.
         *
         * @param oauth2Token The OAuth2 authentication token
         * @return Map of user attributes (email, firstName, lastName, displayName)
         */
        private Map<String, String> extractOAuth2UserAttributes(
                        org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Token) {
                Map<String, String> userAttributes = new HashMap<>();
                try {
                        OAuth2Config oauth2Config = ssoConfigurationManager.getOAuth2Config();
                        if (oauth2Config == null) {
                                logger.warn().category(AppConstants.LOG_CATEGORY_SSO)
                                                .message(AppConstants.OAUTH2_CONFIG_NOT_FOUND_USING_DEFAULTS)
                                                .detail("operation", AppConstants.OP_EXTRACT_OAUTH2_ATTRIBUTES).log();
                                return extractDefaultOAuth2Attributes(oauth2Token);
                        }
                        Map<String, Object> attributes = oauth2Token.getPrincipal().getAttributes();
                        String email = extractOAuth2AttributeWithFallbacks(attributes,
                                        oauth2Config.getEmailAttributeMapping(),
                                        Arrays.asList("email", "emailAddress", "mail", "user_email"));
                        if (email != null)
                                userAttributes.put("email", email);
                        String firstName = extractOAuth2AttributeWithFallbacks(attributes,
                                        oauth2Config.getFirstNameAttributeMapping(),
                                        Arrays.asList("given_name", "firstName", "first_name", "givenName"));
                        if (firstName != null)
                                userAttributes.put("firstName", firstName);
                        String lastName = extractOAuth2AttributeWithFallbacks(attributes,
                                        oauth2Config.getLastNameAttributeMapping(),
                                        Arrays.asList("family_name", "lastName", "last_name", "surname"));
                        if (lastName != null)
                                userAttributes.put("lastName", lastName);
                        String displayName = extractOAuth2AttributeWithFallbacks(attributes,
                                        oauth2Config.getDisplayNameAttributeMapping(),
                                        Arrays.asList("name", "displayName", "display_name", "fullName", "full_name"));
                        if (displayName != null)
                                userAttributes.put("displayName", displayName);
                        logger.debug()
                                        .message(String.format(AppConstants.OAUTH2_USER_ATTRIBUTES_EXTRACTED,
                                                        userAttributes.keySet()))
                                        .category(AppConstants.LOG_CATEGORY_SSO)
                                        .detail("operation", AppConstants.OP_EXTRACT_OAUTH2_ATTRIBUTES)
                                        .detail("extractedAttributes", userAttributes.toString()).log();
                        return userAttributes;
                } catch (Exception e) {
                        logger.error(e).message(AppConstants.OAUTH2_USER_ATTRIBUTE_EXTRACTION_ERROR)
                                        .category(AppConstants.LOG_CATEGORY_SSO)
                                        .detail("operation", AppConstants.OP_EXTRACT_OAUTH2_ATTRIBUTES).log();
                        return extractDefaultOAuth2Attributes(oauth2Token);
                }
        }

        /**
         * Extracts an OAuth2 attribute using configured mapping with fallback
         * attributes.
         *
         * @param attributes          The OAuth2 user attributes map
         * @param configuredAttribute The configured attribute name
         * @param fallbackAttributes  List of fallback attribute names
         * @return The extracted attribute value or null if not found
         */
        private String extractOAuth2AttributeWithFallbacks(Map<String, Object> attributes, String configuredAttribute,
                        List<String> fallbackAttributes) {
                if (configuredAttribute != null && !configuredAttribute.isEmpty()) {
                        Object value = attributes.get(configuredAttribute);
                        if (value != null) {
                                logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                                                .message(String.format(AppConstants.OAUTH2_ATTRIBUTE_FOUND_CONFIGURED,
                                                                configuredAttribute))
                                                .log();
                                return value.toString();
                        }
                }
                for (String attributeName : fallbackAttributes) {
                        Object value = attributes.get(attributeName);
                        if (value != null) {
                                logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                                                .message(String.format(AppConstants.OAUTH2_ATTRIBUTE_FOUND_FALLBACK,
                                                                attributeName))
                                                .log();
                                return value.toString();
                        }
                }
                logger.warn().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.OAUTH2_ATTRIBUTE_NOT_FOUND)
                                .detail("configuredAttribute", configuredAttribute)
                                .detail("fallbackAttributes", fallbackAttributes.toString()).log();
                return null;
        }

        /**
         * Extracts default OAuth2 attributes when no configuration is available.
         *
         * @param oauth2Token The OAuth2 authentication token
         * @return Map containing basic user attributes (primarily email)
         */
        private Map<String, String> extractDefaultOAuth2Attributes(
                        org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauth2Token) {
                Map<String, String> userAttributes = new HashMap<>();
                Map<String, Object> attributes = oauth2Token.getPrincipal().getAttributes();
                Object emailObj = attributes.get("email");
                if (emailObj == null)
                        emailObj = attributes.get("emailAddress");
                if (emailObj == null)
                        emailObj = attributes.get("mail");
                if (emailObj != null) {
                        userAttributes.put("email", emailObj.toString());
                } else {
                        logger.warn().category(AppConstants.LOG_CATEGORY_SSO)
                                        .message(AppConstants.OAUTH2_ATTRIBUTE_NOT_FOUND)
                                        .detail("operation", "EXTRACT_DEFAULT_OAUTH2_ATTRIBUTES")
                                        .detail("attributeName", "email").log();
                }
                return userAttributes;
        }

        /**
         * Gets the current HTTP request from the RequestContextHolder.
         *
         * @return The current HttpServletRequest or null if not available
         */
        private HttpServletRequest getCurrentHttpRequest() {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                                .getRequestAttributes();
                return attributes != null ? attributes.getRequest() : null;
        }
}