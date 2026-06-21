package com.aidentx.authservice.service.Auth.sso;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.audit.AuditLogEventBuilder;
import com.aidentx.authservice.config.LdapConfig;
import com.aidentx.authservice.enums.AuthenticationMethod;
import com.aidentx.authservice.exception.UserNotFoundException;
import com.aidentx.authservice.model.dto.LoginRequest;
import com.aidentx.authservice.model.dto.LoginSuccessResponse;
import com.aidentx.authservice.model.dto.TokenResponse;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.entity.UserSession;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.AuditLogEventService;
import com.aidentx.authservice.service.DynamicTokenService;
import com.aidentx.authservice.service.IpGeolocationService;
import com.aidentx.authservice.service.RefreshTokenService;
import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.authservice.service.UserAuthenticationMethodService;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.authservice.utils.RequestUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;

@Service
public class LdapAuthService {

    private final IUnifiedLogger logger;
    private final SsoConfigurationManager ssoConfigurationManager;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final DynamicTokenService dynamicTokenService;
    private final RefreshTokenService refreshTokenService;
    private final UserAuthenticationMethodService userAuthenticationMethodService;
    private final AuditLogEventService auditLogEventService;
    private final com.aidentx.authservice.service.UserSessionService userSessionService;
    private final IpGeolocationService ipGeolocationService;

    public LdapAuthService(UnifiedLoggerFactory unifiedLoggerFactory, SsoConfigurationManager ssoConfigurationManager,
            @Autowired(required = false) AuthenticationManager authenticationManager, UserRepository userRepository,
            JwtUtil jwtUtil, DynamicTokenService dynamicTokenService, RefreshTokenService refreshTokenService,
            UserAuthenticationMethodService userAuthenticationMethodService,
            AuditLogEventService auditLogEventService,
            com.aidentx.authservice.service.UserSessionService userSessionService,
            IpGeolocationService ipGeolocationService) {
        this.logger = unifiedLoggerFactory.getLogger("LdapAuthService", AppConstants.LOG_CATEGORY_AUTH_LDAP);
        this.ssoConfigurationManager = ssoConfigurationManager;
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.dynamicTokenService = dynamicTokenService;
        this.refreshTokenService = refreshTokenService;
        this.userAuthenticationMethodService = userAuthenticationMethodService;
        this.auditLogEventService = auditLogEventService;
        this.userSessionService = userSessionService;
        this.ipGeolocationService = ipGeolocationService;
    }

    /**
     * Authenticates a user against the configured LDAP server.
     *
     * @param request the login request containing username and password
     * @return a map with authentication result and user details
     */
    @Transactional
    public LoginSuccessResponse<TokenResponse> ldapLogin(LoginRequest request) {
        logger.trace().message(AppConstants.TRACE_LDAP_LOGIN_METHOD).detail("identifier", request.getIdentifier())
                .log();
        LdapConfig ldapConfig = ssoConfigurationManager.getLdapConfig();
        HttpServletRequest req = getCurrentHttpRequest();
        String ip = RequestUtils.getClientIP(req);
        String userAgent = RequestUtils.getUserAgent(req);
        ObjectMapper objectMapper = new ObjectMapper();

        if (ldapConfig == null || !ldapConfig.isEnabled()) {
            logger.warn().objectType("user").objectId(request.getIdentifier())
                    .message(String.format(AppConstants.LOG_LDAP_AUTH_NOT_ENABLED, request.getIdentifier()))
                    .details(Map.of("operation", AppConstants.OP_LDAP_LOGIN, "clientIP", ip, "userAgent", userAgent))
                    .log();
            throw new UserNotFoundException(AppConstants.MSG_LDAP_AUTH_NOT_ENABLED);
        }
        if (authenticationManager == null) {
            logger.warn().objectType("user").objectId(request.getIdentifier())
                    .message(AppConstants.MSG_LDAP_AUTH_NOT_CONFIGURED).detail("operation", AppConstants.OP_LDAP_LOGIN)
                    .details(Map.of("clientIP", ip, "userAgent", userAgent)).log();
            throw new UserNotFoundException(AppConstants.MSG_LDAP_AUTH_NOT_CONFIGURED);
        }
        try {
            org.springframework.security.core.Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword()));
            logger.info().objectType("user").objectId(authentication.getName())
                    .message(String.format(AppConstants.LOG_LDAP_AUTH_SUCCESS, authentication.getName()))
                    .details(Map.of("operation", AppConstants.OP_LDAP_LOGIN, "clientIP", ip, "userAgent", userAgent))
                    .log();

            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElseThrow(() -> {
                logger.warn().objectType("user").objectId(username)
                        .message(String.format(AppConstants.USER_NOT_FOUND, username))
                        .detail("operation", AppConstants.OP_LDAP_LOGIN)
                        .failureReason(AppConstants.USER_NOT_FOUND_IN_DB_POST_LDAP).log();
                ObjectNode customMetadata = objectMapper.createObjectNode();
                customMetadata.put("username", username);
                customMetadata.put("email", username);
                customMetadata.put("type", "user");
                auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(null, AppConstants.USER_EVENT_TYPE,
                        username, AppConstants.UNKNOWN_TENANT_ID, null, AppConstants.USER_EVENT_TYPE, username, AppConstants.LOGIN_EVENT,
                        AppConstants.FAILURE_EVENT_TYPE, AppConstants.EVENT_RISK_LOW, ip, userAgent,
                        String.format(AppConstants.USER_NOT_FOUND, username), null, null, null, customMetadata));
                throw new UserNotFoundException(String.format(AppConstants.USER_NOT_FOUND, username));
            });

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
            HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
            UserSession session = userSessionService.createSession(user.getId(), user.getTenantId(), refreshTokenStr, "LDAP", httpRequest);
            String sessionId = session.getSessionId().toString();

            userAuthenticationMethodService.trackUserMethod(user, AuthenticationMethod.LDAP);
            TokenResponse tokenResponse = new TokenResponse(accessToken, expiresIn, refreshTokenStr, sessionId);

            ObjectNode stateAfter = objectMapper.createObjectNode();
            stateAfter.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "");
            ObjectNode customMetadata = objectMapper.createObjectNode();
            customMetadata.put("username", user.getUsername());
            customMetadata.put("email", user.getEmail());
            customMetadata.put("type", "user");
            ObjectNode stateBefore = objectMapper.createObjectNode();
            stateBefore.put("lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "");
            
            // Get location from IP address for LDAP login
            String location = ipGeolocationService.getLocation(ip);
            ObjectNode customDetails = objectMapper.createObjectNode();
            if (location != null) {
                customDetails.put("location", location);
            }
            
            auditLogEventService
                    .sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(user.getId(), AppConstants.USER_EVENT_TYPE,
                            user.getUsername(), user.getTenantId(), user.getId(), AppConstants.USER_EVENT_TYPE, user.getUsername(),
                            AppConstants.LOGIN_EVENT, AppConstants.SUCCESS_EVENT_TYPE, AppConstants.EVENT_RISK_LOW, ip,
                            userAgent, String.format(AppConstants.LOGIN_SUCCESS, user.getUsername()), stateBefore,
                            stateAfter, customDetails, customMetadata));
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            logger.info().objectType("user").objectId(user.getUsername())
                    .message(String.format(AppConstants.LOGIN_SUCCESS, user.getUsername()))
                    .details(Map.of("operation", AppConstants.OP_LDAP_LOGIN, "userId", user.getId().toString(),
                            "authMethod", "LDAP", "clientIP", ip, "userAgent", userAgent))
                    .log();
            return new LoginSuccessResponse<>(200, tokenResponse);
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.warn().objectType("user").objectId(request.getIdentifier())
                    .message(String.format(AppConstants.LOG_LDAP_AUTH_INVALID, request.getIdentifier()))
                    .detail("operation", AppConstants.OP_LDAP_LOGIN).failureReason("INVALID_LDAP_CREDENTIALS")
                    .details(Map.of("clientIP", ip, "userAgent", userAgent, "exceptionType",
                            e.getClass().getSimpleName(), "exceptionMessage", e.getMessage()))
                    .log();
            throw new UserNotFoundException(AppConstants.MSG_LDAP_AUTH_ERROR);
        }
    }

    private HttpServletRequest getCurrentHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }
}