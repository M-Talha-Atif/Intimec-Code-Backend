package com.aidentx.authservice.service.Auth.sso;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.audit.AuditLogEventBuilder;
import com.aidentx.authservice.config.SamlConfig;
import com.aidentx.authservice.enums.AuthenticationMethod;
import com.aidentx.authservice.exception.SamlRedirectException;
import com.aidentx.authservice.exception.UserNotFoundException;
import com.aidentx.authservice.model.dto.LoginSuccessResponse;
import com.aidentx.authservice.model.dto.TokenResponse;
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

@Service
public class SamlAuthService {

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

    public SamlAuthService(UnifiedLoggerFactory unifiedLoggerFactory, SsoConfigurationManager ssoConfigurationManager,
            UserRepository userRepository, JwtUtil jwtUtil, DynamicTokenService dynamicTokenService,
            RefreshTokenService refreshTokenService,
            UserAuthenticationMethodService userAuthenticationMethodService,
            AuditLogEventService auditLogEventService,
            com.aidentx.authservice.service.UserSessionService userSessionService) {
        this.logger = unifiedLoggerFactory.getLogger("SamlAuthService", AppConstants.LOG_CATEGORY_AUTH_SAML);
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
     * Initiates SAML login by providing redirect information.
     *
     * @return a map with SAML redirect information
     */
    public Map<String, Object> initiateSamlLogin() {
        logger.trace().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.TRACE_INITIATE_SAML_LOGIN_METHOD)
                .log();
        SamlConfig samlConfig = ssoConfigurationManager.getSamlConfig();
        if (samlConfig == null || !samlConfig.isEnabled()) {
            logger.warn().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.LOG_SAML_AUTH_NOT_ENABLED)
                    .detail("operation", AppConstants.OP_SAML_LOGIN_INIT).log();
            return java.util.Map.of("error", AppConstants.MSG_SAML_AUTH_NOT_ENABLED);
        }
        logger.info().category(AppConstants.LOG_CATEGORY_SSO)
                .message(String.format(AppConstants.LOG_SAML_LOGIN_INIT, samlConfig.getName()))
                .details(Map.of("operation", AppConstants.OP_SAML_LOGIN_INIT, "provider", samlConfig.getName())).log();
        return java.util.Map.of("message", AppConstants.MSG_SAML_REDIRECT, "redirectUrl",
                "/saml2/authenticate/saml-provider", "provider", "SAML");
    }

    /**
     * Initiates SAML SSO by providing redirect information.
     *
     * @return a map with SAML SSO redirect information
     */
    public Map<String, Object> initiateSamlSso() {
        logger.trace().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.TRACE_INITIATE_SAML_SSO_METHOD)
                .log();
        SamlConfig samlConfig = ssoConfigurationManager.getSamlConfig();
        if (samlConfig == null || !samlConfig.isEnabled()) {
            logger.warn().category(AppConstants.LOG_CATEGORY_SSO)
                    .message(AppConstants.LOG_SAML_SSO_ATTEMPTED_BUT_NOT_ENABLED)
                    .detail("operation", AppConstants.OP_SAML_SSO_INIT).log();
            return java.util.Map.of("error", AppConstants.MSG_SAML_AUTH_NOT_AVAILABLE, "message",
                    AppConstants.MSG_SAML_AUTH_NOT_ENABLED);
        }
        logger.info().category(AppConstants.LOG_CATEGORY_SSO)
                .message(String.format(AppConstants.LOG_SAML_SSO_INIT, samlConfig.getName()))
                .details(Map.of("operation", AppConstants.OP_SAML_SSO_INIT, "provider", samlConfig.getName())).log();
        return java.util.Map.of("message", AppConstants.MSG_SAML_INIT, "redirectUrl",
                "/saml2/authenticate/saml-provider", "provider", "SAML", "idpSsoUrl", samlConfig.getIdpSsoUrl());
    }

    /**
     * Handles the SAML authentication response after a user attempts to log in via
     * SAML.
     * <p>
     * This method extracts the user's email from the SAML authentication principal,
     * verifies
     * the user's existence in the system, generates JWT access and refresh tokens,
     * updates
     * the user's last login timestamp, logs audit events for success or failure,
     * and
     * finally redirects the user to the front-end application with the tokens.
     * </p>
     *
     * @param authentication the SAML authentication object containing user details
     *                       and attributes
     * @return a {@link LoginSuccessResponse} containing the authentication tokens
     *         and user details
     * @throws UserNotFoundException if the authentication is null, or if the user
     *                               email cannot be
     *                               extracted, or if no matching user is found in
     *                               the system
     * @throws SamlRedirectException to redirect the client to the front-end
     *                               application with tokens
     */
    @Transactional(noRollbackFor = SamlRedirectException.class)
    public LoginSuccessResponse<TokenResponse> handleSamlResponse(Authentication authentication) {
        logger.trace().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.TRACE_HANDLE_SAML_RESPONSE_METHOD)
                .log();
        logger.info().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.LOG_SAML_PROCESSING_RESPONSE)
                .detail("operation", AppConstants.OP_SAML_RESPONSE).log();
        if (authentication == null) {
            logger.warn().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.LOG_SAML_AUTH_RESPONSE_NULL)
                    .detail("operation", AppConstants.OP_SAML_RESPONSE).log();
            throw new UserNotFoundException(AppConstants.MSG_SAML_AUTH_FAILED);
        }
        String email = extractEmailFromSamlAuthentication(authentication);
        User foundUser = null;

        if (email != null) {
            foundUser = userRepository.findByEmail(email).orElse(null);
        }

        if (email == null || foundUser == null) {
            handleSamlLoginFailure(email);
            String failureMessage = email == null ? AppConstants.SAML_AUTH_FAILED_EMAIL_EXTRACTION
                    : String.format(AppConstants.USER_NOT_FOUND, email);
            throw new UserNotFoundException(failureMessage);
        }

        return processSuccessfulSamlLogin(foundUser);
    }

    /**
     * Handles SAML authentication callback.
     *
     * @param success        success parameter from the redirect
     * @param error          error parameter from the redirect
     * @param message        error message parameter from the redirect
     * @param authentication the authentication object
     * @return a {@link LoginSuccessResponse} containing a {@link TokenResponse}
     *         with
     *         the JWT token and its expiration details if authentication is
     *         successful
     */
    @Transactional(noRollbackFor = SamlRedirectException.class)
    public LoginSuccessResponse<TokenResponse> handleSamlCallback(Boolean success, Boolean error, String message,
            Authentication authentication) {
        logger.trace().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.TRACE_HANDLE_SAML_CALLBACK_METHOD)
                .log();

        if (Boolean.TRUE.equals(error)) {
            logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                    .message(String.format(AppConstants.LOG_SAML_CALLBACK_FAILED, message))
                    .details(Map.of("operation", AppConstants.OP_SAML_CALLBACK, "errorMessage", message)).log();
            String userMessage = message != null && message.toLowerCase().matches(".*(signature|certificate).*")
                    ? AppConstants.SAML_CERTIFICATE_ERROR_MSG
                    : (message != null && !message.isBlank() ? message : AppConstants.SAML_GENERIC_ERROR_MSG);
            String errorUrl = frontUrl + "?error="
                    + java.net.URLEncoder.encode(userMessage, java.nio.charset.StandardCharsets.UTF_8);
            throw new SamlRedirectException(errorUrl, userMessage);
        }

        if (authentication == null || !authentication.isAuthenticated()) {
            logger.debug().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.LOG_SAML_CALLBACK_NO_AUTH)
                    .detail("operation", AppConstants.OP_SAML_CALLBACK).log();
            throw new SamlRedirectException(frontUrl, AppConstants.SAML_GENERIC_ERROR_MSG);
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (email == null || user == null) {
            handleSamlLoginFailure(email);
            String errorUrl = frontUrl + "?error=" + java.net.URLEncoder.encode(AppConstants.WRONG_USERNAME_OR_PASSWORD,
                    java.nio.charset.StandardCharsets.UTF_8);
            throw new SamlRedirectException(errorUrl, AppConstants.WRONG_USERNAME_OR_PASSWORD);
        }

        return processSuccessfulSamlLogin(user);
    }

    private LoginSuccessResponse<TokenResponse> processSuccessfulSamlLogin(User user) {
        // Generate tokens with dynamic expiration based on tenant's session control policy
        DynamicTokenService.TokenPair tokenPair = dynamicTokenService.generateTokenPair(
            user.getId(), user.getUsername(), user.getTenantId(), user.getIdentityId());
        String accessToken = tokenPair.getJwtToken();
        String refreshTokenStr = tokenPair.getRefreshToken();
        
        // Get dynamic expiration time for response
        long expiresInMs = dynamicTokenService.getJwtExpirationForTenant(user.getTenantId());
        long expiresIn = expiresInMs / 1000; // Convert to seconds for response
        
        // Clean up old refresh tokens and save new one
        refreshTokenService.deleteAllUserTokens(user);
        refreshTokenService.createRefreshToken(user, refreshTokenStr);

        // Create session for the user and capture sessionId
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        UserSession session = userSessionService.createSession(user.getId(), user.getTenantId(), refreshTokenStr, "SAML", httpRequest);
        String sessionId = session.getSessionId().toString();

        userAuthenticationMethodService.trackUserMethod(user, AuthenticationMethod.SAML);

        HttpServletRequest request = getCurrentHttpRequest();
        createAndSendSamlAuditEvent(user, RequestUtils.getClientIP(request), RequestUtils.getUserAgent(request));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        logger.info().category(AppConstants.LOG_CATEGORY_AUTH).objectType("user").objectId(user.getUsername())
                .message(String.format(AppConstants.LOGIN_SUCCESS, user.getUsername()))
                .details(Map.of("operation", AppConstants.OP_SAML_LOGIN, "userId", user.getId().toString(),
                        "authMethod", "SAML", "clientIP", RequestUtils.getClientIP(request), "userAgent",
                        RequestUtils.getUserAgent(request)))
                .log();

        String redirectUrl = String.format("%s%s?token=%s&refresh=%s&sessionId=%s", frontUrl, frontUrl_callback, accessToken,
                refreshTokenStr, sessionId);
        throw new SamlRedirectException(redirectUrl, null);
    }

    // --- Private Helper Methods ---

    private String extractEmailFromSamlAuthentication(Authentication authentication) {
        if (authentication
                .getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (authentication
                .getPrincipal() instanceof org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal samlPrincipal) {
            Map<String, String> userAttributes = extractSamlUserAttributes(samlPrincipal);
            String email = userAttributes.get("email");
            if (email == null) {
                logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                        .message(String.format(AppConstants.SAML_PRINCIPAL_ATTRIBUTES_AVAILABLE,
                                samlPrincipal.getAttributes().keySet()))
                        .detail("attributes", samlPrincipal.getAttributes().toString()).log();
            }
            return email;
        } else {
            String email = authentication.getName();
            logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                    .message(String.format(AppConstants.SAML_AUTH_NAME_AS_EMAIL, email))
                    .detail("principalType", authentication.getPrincipal().getClass().getName()).log();
            return email;
        }
    }

    private void handleSamlLoginFailure(String email) {
        HttpServletRequest request = getCurrentHttpRequest();
        String ip = RequestUtils.getClientIP(request);
        String userAgent = RequestUtils.getUserAgent(request);
        String safeEmail = email != null ? email : "unknown";
        String failureMessage = email == null ? AppConstants.SAML_AUTH_FAILED_EMAIL_EXTRACTION
                : String.format(AppConstants.SAML_AUTH_FAILED_USER_NOT_FOUND, email);

        auditLogEventService.sendAuditEvent(
                AuditLogEventBuilder.createEventWithTenant(null, AppConstants.USER_EVENT_TYPE, safeEmail, AppConstants.UNKNOWN_TENANT_ID,
                        null, AppConstants.USER_EVENT_TYPE, safeEmail, AppConstants.LOGIN_EVENT,
                        AppConstants.FAILURE_EVENT_TYPE, AppConstants.EVENT_RISK_LOW, ip, userAgent, failureMessage,
                        null, null, null, null));

        if (email == null) {
            logger.error().message(AppConstants.SAML_AUTH_FAILED_EMAIL_EXTRACTION)
                    .category(AppConstants.LOG_CATEGORY_SSO).detail("operation", AppConstants.OP_SAML_RESPONSE).log();
        } else {
            logger.warn().category(AppConstants.LOG_CATEGORY_SSO).objectType("user").objectId(email)
                    .message(String.format(AppConstants.SAML_AUTH_FAILED_USER_NOT_FOUND, email))
                    .detail("operation", AppConstants.OP_SAML_RESPONSE).log();
        }
    }

    private void createAndSendSamlAuditEvent(User user, String ip, String userAgent) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode customMetadata = objectMapper.createObjectNode();
        customMetadata.put("username", user.getUsername());
        customMetadata.put("email", user.getEmail());
        customMetadata.put("type", "user");
        ObjectNode stateBefore = objectMapper.createObjectNode();
        stateBefore.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "");
        ObjectNode stateAfter = objectMapper.createObjectNode();
        stateAfter.put("lastLoginAt", Instant.now().toString());

        auditLogEventService.sendAuditEvent(
                AuditLogEventBuilder.createEventWithTenant(user.getId(), AppConstants.USER_EVENT_TYPE, user.getUsername(),
                        user.getTenantId(), user.getId(), AppConstants.USER_EVENT_TYPE, user.getUsername(), AppConstants.LOGIN_EVENT,
                        AppConstants.SUCCESS_EVENT_TYPE, AppConstants.EVENT_RISK_LOW, ip, userAgent,
                        String.format(AppConstants.LOGIN_SUCCESS, user.getUsername()), stateBefore, stateAfter, null,
                        customMetadata));
    }

    /**
     * Extracts user attributes from SAML authentication using configured attribute
     * mappings.
     * Uses the dynamic SAML configuration to map SAML attributes to user
     * properties.
     *
     * @param samlPrincipal the SAML authenticated principal
     * @return a map containing extracted user attributes (email, firstName,
     *         lastName, displayName)
     */
    private Map<String, String> extractSamlUserAttributes(
            org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal samlPrincipal) {

        Map<String, String> userAttributes = new HashMap<>();

        try {
            SamlConfig samlConfig = ssoConfigurationManager.getSamlConfig();
            if (samlConfig == null) {
                logger.warn().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.SAML_CONFIG_NOT_FOUND)
                        .detail("operation", AppConstants.OP_EXTRACT_SAML_ATTRIBUTES).log();
                return extractDefaultSamlAttributes(samlPrincipal);
            }

            String email = extractAttributeWithFallbacks(samlPrincipal, samlConfig.getEmailAttribute(),
                    Arrays.asList("email", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
                            "emailAddress", "mail"));
            if (email != null)
                userAttributes.put("email", email);
            String firstName = extractAttributeWithFallbacks(samlPrincipal, samlConfig.getFirstNameAttribute(),
                    Arrays.asList("firstName", "givenName",
                            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname", "first_name"));
            if (firstName != null)
                userAttributes.put("firstName", firstName);
            String lastName = extractAttributeWithFallbacks(samlPrincipal, samlConfig.getLastNameAttribute(),
                    Arrays.asList("lastName", "surname",
                            "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname", "last_name", "sn"));
            if (lastName != null)
                userAttributes.put("lastName", lastName);
            String displayName = extractAttributeWithFallbacks(samlPrincipal, samlConfig.getDisplayNameAttribute(),
                    Arrays.asList("displayName", "name", "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name",
                            "fullName", "cn"));
            if (displayName != null)
                userAttributes.put("displayName", displayName);

            logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                    .message(String.format(AppConstants.SAML_USER_ATTRIBUTES_EXTRACTED, userAttributes.keySet()))
                    .detail("operation", AppConstants.OP_EXTRACT_SAML_ATTRIBUTES)
                    .detail("extractedAttributes", userAttributes.toString()).log();
            return userAttributes;
        } catch (Exception e) {
            logger.error(e).category(AppConstants.LOG_CATEGORY_SSO)
                    .message(AppConstants.SAML_USER_ATTRIBUTE_EXTRACTION_ERROR)
                    .detail("operation", AppConstants.OP_EXTRACT_SAML_ATTRIBUTES).log();
            return extractDefaultSamlAttributes(samlPrincipal);
        }
    }

    /**
     * Extracts a SAML attribute using configured mapping with fallback to common
     * attribute names.
     *
     * @param samlPrincipal       the SAML authenticated principal
     * @param configuredAttribute the attribute name from configuration
     * @param fallbackAttributes  a list of fallback attribute names to try
     * @return the first found attribute value, or null if not found
     */
    private String extractAttributeWithFallbacks(
            org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal samlPrincipal,
            String configuredAttribute,
            List<String> fallbackAttributes) {
        if (configuredAttribute != null && !configuredAttribute.isEmpty()) {
            Object value = samlPrincipal.getFirstAttribute(configuredAttribute);
            if (value != null) {
                logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                        .message(String.format(AppConstants.SAML_ATTRIBUTE_FOUND_CONFIGURED, configuredAttribute))
                        .log();
                return value.toString();
            }
        }
        for (String attributeName : fallbackAttributes) {
            Object value = samlPrincipal.getFirstAttribute(attributeName);
            if (value != null) {
                logger.debug().category(AppConstants.LOG_CATEGORY_SSO)
                        .message(String.format(AppConstants.SAML_ATTRIBUTE_FOUND_FALLBACK, attributeName)).log();
                return value.toString();
            }
        }
        logger.warn().category(AppConstants.LOG_CATEGORY_SSO).message(AppConstants.SAML_ATTRIBUTE_NOT_FOUND)
                .detail("configuredAttribute", configuredAttribute)
                .detail("fallbackAttributes", fallbackAttributes.toString()).log();
        return null;
    }

    /**
     * Extracts default SAML attributes when configuration is not available.
     * Only attempts to extract the email attribute using common names.
     *
     * @param samlPrincipal the SAML authenticated principal
     * @return a map containing the extracted email attribute if found
     */
    private Map<String, String> extractDefaultSamlAttributes(
            org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal samlPrincipal) {
        Map<String, String> userAttributes = new HashMap<>();
        Object emailObj = samlPrincipal.getFirstAttribute("email");
        if (emailObj == null) {
            emailObj = samlPrincipal
                    .getFirstAttribute("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress");
        }
        if (emailObj != null) {
            userAttributes.put("email", emailObj.toString());
        } else {
            logger.warn().message(AppConstants.SAML_ATTRIBUTE_NOT_FOUND).category(AppConstants.LOG_CATEGORY_SSO)
                    .details(Map.of("operation", "EXTRACT_DEFAULT_SAML_ATTRIBUTES", "attributeName", "email"))
                    .log();
        }
        return userAttributes;
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}