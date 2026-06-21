package com.aidentx.authservice.service.Auth;

import com.aidentx.authservice.service.IpGeolocationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.audit.AuditLogEventBuilder;
import com.aidentx.authservice.config.LdapConfig;
import com.aidentx.authservice.config.OAuth2Config;
import com.aidentx.authservice.config.SamlConfig;
import com.aidentx.authservice.exception.AccountLockedException;
import com.aidentx.authservice.exception.InvalidCredentialsException;
import com.aidentx.authservice.exception.UserNotFoundException;
import com.aidentx.authservice.model.dto.*;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.entity.UserSession;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.Auth.AccountLockoutService;
import com.aidentx.authservice.service.AuditLogEventService;
import com.aidentx.authservice.service.DynamicTokenService;
import com.aidentx.authservice.service.RefreshTokenService;
import com.aidentx.authservice.service.SsoConfigurationManager;
import com.aidentx.authservice.service.UserSessionService;
import com.aidentx.authservice.service.SessionTerminationService;
import com.aidentx.authservice.service.Auth.sso.LdapAuthService;
import com.aidentx.authservice.service.Auth.sso.OAuth2AuthService;
import com.aidentx.authservice.service.Auth.sso.SamlAuthService;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.authservice.utils.RequestUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of the {@link AuthService} interface.
 * <p>
 * This service acts as a coordinator for authentication-related operations. It
 * handles
 * the core database-backed user login and delegates specialized tasks like
 * password management,
 * token refreshing, and SSO flows (LDAP, SAML, OAuth2) to dedicated services.
 * It also manages audit logging for all authentication events.
 * </p>
 */
@Service
public class AuthServiceImpl implements AuthService {

        private final IUnifiedLogger logger;
        private final UserRepository userRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtUtil jwtUtil;
        private final DynamicTokenService dynamicTokenService;
        private final RefreshTokenService refreshTokenService;
        private final AuditLogEventService auditLogEventService;
        private final SsoConfigurationManager ssoConfigurationManager;
        private final AccountLockoutService accountLockoutService;
        private final UserSessionService userSessionService;
        private final IpGeolocationService ipGeolocationService;
        private final SessionTerminationService sessionTerminationService;

        // Injected specialty services
        private final LdapAuthService ldapAuthService;
        private final SamlAuthService samlAuthService;
        private final OAuth2AuthService oAuth2AuthService;
        private final PasswordService passwordService;
        private final TokenService tokenService;

        /**
         * Constructs a new AuthServiceImpl instance with required dependencies.
         *
         * @param userRepository          Repository to fetch user data
         * @param passwordEncoder         Encoder used to verify user password
         * @param jwtUtil                 Utility to generate JWT tokens
         * @param dynamicTokenService     Service for dynamic token generation with tenant policies
         * @param refreshTokenService     Service to handle refresh token creation and
         *                                validation
         * @param auditLogEventService    Service to send audit events
         * @param ssoConfigurationManager Manager for SSO configurations
         * @param accountLockoutService   Service to handle account lockout policies
         * @param unifiedLoggerFactory    Factory to create unified loggers
         * @param ldapAuthService         Service for LDAP authentication
         * @param samlAuthService         Service for SAML authentication
         * @param oAuth2AuthService       Service for OAuth2 authentication
         * @param passwordService         Service for password management
         * @param tokenService               Service for token refresh operations
         * @param userSessionService         Service for managing user sessions
         * @param ipGeolocationService       Service for IP geolocation
         * @param sessionTerminationService  Service for terminating sessions with WebSocket notifications
         */
        public AuthServiceImpl(
                        UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil,
                        DynamicTokenService dynamicTokenService,
                        RefreshTokenService refreshTokenService,
                        AuditLogEventService auditLogEventService,
                        SsoConfigurationManager ssoConfigurationManager,
                        AccountLockoutService accountLockoutService,
                        UnifiedLoggerFactory unifiedLoggerFactory,
                        LdapAuthService ldapAuthService,
                        SamlAuthService samlAuthService,
                        OAuth2AuthService oAuth2AuthService,
                        PasswordService passwordService,
                        TokenService tokenService,
                        UserSessionService userSessionService,
                        IpGeolocationService ipGeolocationService,
                        SessionTerminationService sessionTerminationService) {
                this.userRepository = userRepository;
                this.passwordEncoder = passwordEncoder;
                this.jwtUtil = jwtUtil;
                this.dynamicTokenService = dynamicTokenService;
                this.refreshTokenService = refreshTokenService;
                this.auditLogEventService = auditLogEventService;
                this.ssoConfigurationManager = ssoConfigurationManager;
                this.accountLockoutService = accountLockoutService;
                this.userSessionService = userSessionService;
                this.ipGeolocationService = ipGeolocationService;
                this.sessionTerminationService = sessionTerminationService;
                this.logger = unifiedLoggerFactory.getLogger("AuthService", AppConstants.LOG_CATEGORY_AUTH);

                this.ldapAuthService = ldapAuthService;
                this.samlAuthService = samlAuthService;
                this.oAuth2AuthService = oAuth2AuthService;
                this.passwordService = passwordService;
                this.tokenService = tokenService;
        }

        /**
         * Authenticates a user by verifying their username and password.
         * If authentication is successful, generates both a JWT access token and a
         * refresh token,
         * saves the refresh token in the database, and returns them.
         * On failure, appropriate exceptions are thrown and audit logs are recorded
         * with client metadata.
         *
         * @param loginRequest The login credentials provided by the user
         * @return a successful login response containing a JWT access token,
         *         refresh token string, and expiry duration
         * @throws UserNotFoundException       if the user doesn't exist
         * @throws AccountLockedException      if the user account is locked
         * @throws InvalidCredentialsException if the password is incorrect
         */
        @Override
        @Transactional
        public LoginSuccessResponse<TokenResponse> login(LoginRequest loginRequest) {
                logger.trace().category(AppConstants.LOG_CATEGORY_AUTH).message(AppConstants.TRACE_LOGIN_METHOD_STARTED)
                                .detail("identifier", loginRequest.getIdentifier()).log();
                logger.debug().category(AppConstants.LOG_CATEGORY_AUTH).objectType("user")
                                .objectId(loginRequest.getIdentifier())
                                .message(AppConstants.DEBUG_LOGIN_REQUEST_RECEIVED)
                                .detail("requestType", "LOGIN_REQUEST")
                                .detail("identifier", loginRequest.getIdentifier()).log();

                HttpServletRequest request = getCurrentHttpRequest();

                // Debug: Log all forwarded headers
                logger.info().category(AppConstants.LOG_CATEGORY_AUTH)
                        .message("Detecting client IP - checking headers")
                        .detail("X-Forwarded-For", request.getHeader("X-Forwarded-For"))
                        .detail("X-Real-IP", request.getHeader("X-Real-IP"))
                        .detail("RemoteAddr", request.getRemoteAddr())
                        .log();

                String clientIp = RequestUtils.getClientIP(request);
                String userAgent = RequestUtils.getUserAgent(request);

                logger.info().category(AppConstants.LOG_CATEGORY_AUTH)
                        .message("Client IP detected")
                        .detail("clientIp", clientIp)
                        .detail("userAgent", userAgent)
                        .log();

                ObjectMapper objectMapper = new ObjectMapper();

                try {
                        User user = userRepository.findByUsername(loginRequest.getIdentifier())
                                        .orElseThrow(() -> {
                                                // Get location from IP address for failed login
                                                String location = ipGeolocationService.getLocation(clientIp);
                                                ObjectNode customDetails = objectMapper.createObjectNode();
                                                if (location != null) {
                                                        customDetails.put("location", location);
                                                }
                                                auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(
                                                                null, AppConstants.USER_EVENT_TYPE,
                                                                loginRequest.getIdentifier(), AppConstants.UNKNOWN_TENANT_ID, null,
                                                                AppConstants.USER_EVENT_TYPE,
                                                                loginRequest.getIdentifier(), AppConstants.LOGIN_EVENT,
                                                                AppConstants.FAILURE_EVENT_TYPE,
                                                                AppConstants.EVENT_RISK_LOW, clientIp, userAgent,
                                                                String.format(AppConstants.USER_NOT_FOUND,
                                                                                loginRequest.getIdentifier()),
                                                                null, null, customDetails, null));
                                                logger.warn().category(AppConstants.LOG_CATEGORY_AUTH)
                                                                .objectType("user")
                                                                .objectId(loginRequest.getIdentifier())
                                                                .message(AppConstants.WARN_USER_NOT_FOUND_LOGIN)
                                                                .detail("operation", AppConstants.OP_LOGIN)
                                                                .detail("clientIP", clientIp)
                                                                .detail("userAgent", userAgent)
                                                                .failureReason("USER_NOT_FOUND").log();
                                                return new UserNotFoundException(
                                                                String.format(AppConstants.USER_NOT_FOUND,
                                                                                loginRequest.getIdentifier()));
                                        });

                        // Check if account is locked using lockout service
                        if (accountLockoutService.isAccountLocked(user)) {
                                ObjectNode stateBefore = objectMapper.createObjectNode().put("lastLoginAt",
                                                user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "");
                                ObjectNode customMetadata = objectMapper.createObjectNode()
                                                .put("username", user.getUsername()).put("email", user.getEmail())
                                                .put("type", "user");
                                // Get location from IP address for locked account
                                String location = ipGeolocationService.getLocation(clientIp);
                                ObjectNode customDetails = objectMapper.createObjectNode();
                                if (location != null) {
                                        customDetails.put("location", location);
                                }
                                auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(user.getId(),
                                                AppConstants.USER_EVENT_TYPE, user.getUsername(), user.getTenantId(), user.getId(),
                                                AppConstants.USER_EVENT_TYPE, user.getUsername(),
                                                AppConstants.LOGIN_EVENT, AppConstants.FAILURE_EVENT_TYPE,
                                                AppConstants.EVENT_RISK_LOW, clientIp, userAgent,
                                                AppConstants.ACCOUNT_LOCKED_TITLE, stateBefore, null, customDetails,
                                                customMetadata));
                                logger.warn().category(AppConstants.LOG_CATEGORY_AUTH).objectType("user")
                                                .objectId(loginRequest.getIdentifier())
                                                .message(AppConstants.WARN_ACCOUNT_LOCKED_LOGIN)
                                                .details(Map.of("operation", AppConstants.OP_LOGIN, "clientIP",
                                                                clientIp, "userAgent", userAgent, "lockedUntil",
                                                                user.getLockedUntil() != null ? user.getLockedUntil().toString() : "N/A", "userId",
                                                                user.getId().toString()))
                                                .failureReason("ACCOUNT_LOCKED")
                                                .log();
                                throw new AccountLockedException(user.getLockedUntil() != null ? user.getLockedUntil().toString() : "Account is locked");
                        }


                        // Detect unactivated accounts (provisioned via sync, password never set)
                        if (user.getPasswordHash() == null) {
                                throw new InvalidCredentialsException(AppConstants.ACCOUNT_NOT_ACTIVATED_MSG);
                        }

                        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
                                // Process failed login attempt
                                accountLockoutService.processFailedLoginAttempt(user);

                                ObjectNode stateBefore = objectMapper.createObjectNode().put("lastLoginAt",
                                                user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "");
                                ObjectNode customMetadata = objectMapper.createObjectNode()
                                                .put("username", user.getUsername()).put("email", user.getEmail())
                                                .put("type", "user");
                                // Get location from IP address for invalid credentials
                                String location = ipGeolocationService.getLocation(clientIp);
                                ObjectNode customDetails = objectMapper.createObjectNode();
                                if (location != null) {
                                        customDetails.put("location", location);
                                }
                                auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(user.getId(),
                                                AppConstants.USER_EVENT_TYPE, user.getUsername(), user.getTenantId(), user.getId(),
                                                AppConstants.USER_EVENT_TYPE, user.getUsername(),
                                                AppConstants.LOGIN_EVENT, AppConstants.FAILURE_EVENT_TYPE,
                                                AppConstants.EVENT_RISK_LOW, clientIp, userAgent,
                                                AppConstants.WRONG_USERNAME_OR_PASSWORD, stateBefore, null, customDetails,
                                                customMetadata));
                                logger.warn().category(AppConstants.LOG_CATEGORY_AUTH).objectType("user")
                                                .objectId(loginRequest.getIdentifier())
                                                .message(AppConstants.WARN_INVALID_CREDENTIALS_LOGIN)
                                                .details(Map.of("operation", AppConstants.OP_LOGIN, "clientIP",
                                                                clientIp, "userAgent", userAgent, "userId",
                                                                user.getId().toString()))
                                                .failureReason("INVALID_CREDENTIALS")
                                                .log();
                                throw new InvalidCredentialsException(AppConstants.WRONG_USERNAME_OR_PASSWORD);
                        }

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
                        UserSession session = userSessionService.createSession(user.getId(), user.getTenantId(), refreshTokenStr, "PASSWORD", request);
                        String sessionId = session.getSessionId().toString();

                        // Capture state BEFORE updating lastLoginAt (MUST be done before resetFailedLoginAttempts)
                        ObjectNode stateBefore = objectMapper.createObjectNode().put("lastLoginAt",
                                        user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "");

                        // Update lastLoginAt with current timestamp
                        Instant loginTimestamp = Instant.now();
                        user.setLastLoginAt(loginTimestamp);
                        userRepository.save(user);

                        // Reset failed login attempts on successful login (after saving lastLoginAt)
                        accountLockoutService.resetFailedLoginAttempts(user);

                        // Capture state AFTER updating lastLoginAt
                        ObjectNode stateAfter = objectMapper.createObjectNode().put("lastLoginAt",
                                        loginTimestamp.toString());

                        ObjectNode customMetadata = objectMapper.createObjectNode().put("username", user.getUsername())
                                        .put("email", user.getEmail()).put("type", "user");

                        // Get location from IP address
                        String location = ipGeolocationService.getLocation(clientIp);

                        // Create custom details with location
                        ObjectNode customDetails = objectMapper.createObjectNode();
                        if (location != null) {
                                customDetails.put("location", location);
                        }

                        auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(user.getId(),
                                        AppConstants.USER_EVENT_TYPE, user.getUsername(), user.getTenantId(), user.getId(),
                                        AppConstants.USER_EVENT_TYPE, user.getUsername(), AppConstants.LOGIN_EVENT,
                                        AppConstants.SUCCESS_EVENT_TYPE, AppConstants.EVENT_RISK_LOW, clientIp,
                                        userAgent, String.format(AppConstants.LOGIN_SUCCESS, user.getUsername()),
                                        stateBefore, stateAfter, customDetails, customMetadata));
                        logger.info().category(AppConstants.LOG_CATEGORY_AUTH).objectType("user")
                                        .objectId(user.getUsername()).message(AppConstants.INFO_USER_LOGIN_SUCCESS)
                                        .details(Map.of("operation", AppConstants.OP_LOGIN, "userId",
                                                        user.getId().toString(), "authMethod", "DATABASE", "clientIP",
                                                        clientIp, "userAgent", userAgent, "tokenGenerated", true,
                                                        "lastLoginAt", user.getLastLoginAt().toString()))
                                        .log();
                        return new LoginSuccessResponse<>(200,
                                        new TokenResponse(accessToken, expiresIn, refreshTokenStr, sessionId));
                } catch (UserNotFoundException | AccountLockedException | InvalidCredentialsException e) {
                        throw e;
                } catch (Exception e) {
                        logger.error(e).category(AppConstants.LOG_CATEGORY_AUTH).objectType("user")
                                        .objectId(loginRequest.getIdentifier())
                                        .message(String.format(AppConstants.ERROR_UNEXPECTED_LOGIN, e.getMessage()))
                                        .details(Map.of("operation", AppConstants.OP_LOGIN, "clientIP", clientIp,
                                                        "userAgent", userAgent))
                                        .log();
                        throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, e);
                }
        }

        @Override
        @Transactional
        public void logout(UUID userId, String refreshToken) {
                logger.trace().category(AppConstants.LOG_CATEGORY_AUTH).message(AppConstants.TRACE_LOGOUT_METHOD)
                                .details(Map.of("operation", AppConstants.OP_LOGOUT, "userId", userId.toString()))
                                .log();
                User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException(
                                String.format(AppConstants.USER_NOT_FOUND, userId)));
                
                // If refreshToken is provided, delete only that specific token and session
                // Otherwise, delete all tokens and sessions for the user
                if (refreshToken != null && !refreshToken.isBlank()) {
                        refreshTokenService.deleteByToken(refreshToken);
                        userSessionService.deleteSessionByRefreshToken(refreshToken);
                } else {
                        refreshTokenService.deleteAllUserTokens(user);
                        userSessionService.deleteAllUserSessions(userId);
                }

                HttpServletRequest request = getCurrentHttpRequest();
                String clientIp = RequestUtils.getClientIP(request);
                String userAgent = RequestUtils.getUserAgent(request);
                ObjectMapper objectMapper = new ObjectMapper();
                ObjectNode customMetadata = objectMapper.createObjectNode().put("username", user.getUsername())
                                .put("email", user.getEmail()).put("type", "user");
                
                // Get location from IP address
                String location = ipGeolocationService.getLocation(clientIp);
                
                // Create custom details with location
                ObjectNode customDetails = objectMapper.createObjectNode();
                if (location != null) {
                        customDetails.put("location", location);
                }
                
                auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(user.getId(),
                                AppConstants.USER_EVENT_TYPE, user.getUsername(), user.getTenantId(), user.getId(),
                                AppConstants.USER_EVENT_TYPE, user.getUsername(), AppConstants.LOGOUT_EVENT,
                                AppConstants.SUCCESS_EVENT_TYPE, AppConstants.EVENT_RISK_LOW, clientIp, userAgent,
                                String.format(AppConstants.USER_LOGOUT_SUCCESS, user.getUsername()), null, null, customDetails,
                                customMetadata));
                logger.info().category(AppConstants.LOG_CATEGORY_AUTH).objectType("user").objectId(user.getUsername())
                                .message(String.format(AppConstants.USER_LOGOUT_SUCCESS, userId))
                                .details(Map.of("operation", AppConstants.OP_LOGOUT, "userId", userId.toString(),
                                                "clientIP", clientIp, "userAgent", userAgent))
                                .log();
        }

        @Override
        public boolean isSessionValid(UUID sessionId) {
                logger.trace().category(AppConstants.LOG_CATEGORY_AUTH)
                                .message("Validating session")
                                .detail("sessionId", sessionId.toString())
                                .log();
                
                return userSessionService.existsBySessionId(sessionId);
        }

        @Override
        public List<com.aidentx.authservice.model.dto.UserSessionResponse> getAllSessions(String tenantId) {
                logger.trace().category(AppConstants.LOG_CATEGORY_AUTH)
                                .message("Getting all sessions for tenant")
                                .detail("tenantId", tenantId)
                                .log();
                
                List<com.aidentx.authservice.model.entity.UserSession> sessions = userSessionService.getSessionsByTenantId(tenantId);
                List<com.aidentx.authservice.model.dto.UserSessionResponse> responses = new java.util.ArrayList<>();
                
                for (com.aidentx.authservice.model.entity.UserSession session : sessions) {
                        // Get user details
                        Optional<com.aidentx.authservice.model.entity.User> userOpt = userRepository.findById(session.getUserId());
                        if (userOpt.isPresent()) {
                                com.aidentx.authservice.model.entity.User user = userOpt.get();
                                com.aidentx.authservice.model.dto.UserSessionResponse.UserInfo userInfo = 
                                        new com.aidentx.authservice.model.dto.UserSessionResponse.UserInfo(
                                                user.getId(), 
                                                user.getUsername(), 
                                                user.getEmail()
                                        );
                                
                                // Get location for the IP address
                                String location = ipGeolocationService.getLocation(session.getIpAddress());
                                logger.info().category(AppConstants.LOG_CATEGORY_AUTH)
                                                .message("Building session response with location")
                                                .detail("sessionId", session.getSessionId().toString())
                                                .detail("ipAddress", session.getIpAddress())
                                                .detail("location", location)
                                                .detail("device", session.getDevice())
                                                .log();
                                
                                com.aidentx.authservice.model.dto.UserSessionResponse response = 
                                        new com.aidentx.authservice.model.dto.UserSessionResponse(
                                                session.getSessionId(),
                                                userInfo,
                                                session.getRefreshToken(),
                                                session.getIpAddress(),
                                                location,
                                                session.getUserAgent(),
                                                session.getDevice(),
                                                session.getIsMobile(),
                                                session.getIsDesktop(),
                                                session.getCreatedAt(),
                                                session.getLastActivityAt(),
                                                session.getAuthMethod(),
                                                session.getTenantId(),
                                                false
                                        );
                                responses.add(response);
                        }
                }
                
                logger.info().category(AppConstants.LOG_CATEGORY_AUTH)
                                .message("Retrieved sessions for tenant")
                                .detail("tenantId", tenantId)
                                .detail("sessionCount", String.valueOf(responses.size()))
                                .log();
                
                return responses;
        }

        @Override
        public org.springframework.data.domain.Page<com.aidentx.authservice.model.dto.UserSessionResponse> getAllSessionsPaginated(
                        String tenantId, int page, int size, String search, String currentRefreshToken) {
                logger.trace().category(AppConstants.LOG_CATEGORY_AUTH)
                                .message("Getting paginated sessions for tenant")
                                .detail("tenantId", tenantId)
                                .detail("page", String.valueOf(page))
                                .detail("size", String.valueOf(size))
                                .detail("search", search)
                                .detail("currentRefreshToken", currentRefreshToken != null ? "present" : "null")
                                .log();
                
                // Lazy cleanup: Delete expired sessions before retrieving
                int deletedCount = userSessionService.cleanupExpiredSessionsForTenant(tenantId);
                if (deletedCount > 0) {
                        logger.info().category(AppConstants.LOG_CATEGORY_AUTH)
                                        .message("Lazy cleanup: Deleted expired sessions")
                                        .detail("tenantId", tenantId)
                                        .detail("deletedCount", String.valueOf(deletedCount))
                                        .log();
                }
                
                // Get all sessions for tenant
                List<com.aidentx.authservice.model.entity.UserSession> allSessions = 
                        userSessionService.getSessionsByTenantId(tenantId);
                
                List<com.aidentx.authservice.model.dto.UserSessionResponse> allResponses = 
                        new java.util.ArrayList<>();
                
                for (com.aidentx.authservice.model.entity.UserSession session : allSessions) {
                        Optional<com.aidentx.authservice.model.entity.User> userOpt = 
                                userRepository.findById(session.getUserId());
                        if (userOpt.isPresent()) {
                                com.aidentx.authservice.model.entity.User user = userOpt.get();
                                com.aidentx.authservice.model.dto.UserSessionResponse.UserInfo userInfo = 
                                        new com.aidentx.authservice.model.dto.UserSessionResponse.UserInfo(
                                                user.getId(), 
                                                user.getUsername(), 
                                                user.getEmail()
                                        );
                                
                                String location = ipGeolocationService.getLocation(session.getIpAddress());
                                logger.info().category(AppConstants.LOG_CATEGORY_AUTH)
                                                .message("Building session response with location")
                                                .detail("sessionId", session.getSessionId().toString())
                                                .detail("ipAddress", session.getIpAddress())
                                                .detail("location", location)
                                                .detail("device", session.getDevice())
                                                .log();
                                
                                // Check if this is the current session by comparing refresh tokens
                                boolean isCurrent = currentRefreshToken != null && 
                                                   session.getRefreshToken() != null && 
                                                   session.getRefreshToken().equals(currentRefreshToken);
                                
                                com.aidentx.authservice.model.dto.UserSessionResponse response = 
                                        new com.aidentx.authservice.model.dto.UserSessionResponse(
                                                session.getSessionId(),
                                                userInfo,
                                                session.getRefreshToken(),
                                                session.getIpAddress(),
                                                location,
                                                session.getUserAgent(),
                                                session.getDevice(),
                                                session.getIsMobile(),
                                                session.getIsDesktop(),
                                                session.getCreatedAt(),
                                                session.getLastActivityAt(),
                                                session.getAuthMethod(),
                                                session.getTenantId(),
                                                isCurrent
                                        );
                                allResponses.add(response);
                        }
                }
                
                // Sort: current session first, then by creation time descending
                allResponses.sort((a, b) -> {
                        if (a.getIsCurrent() != null && b.getIsCurrent() != null) {
                                if (a.getIsCurrent() && !b.getIsCurrent()) return -1;
                                if (!a.getIsCurrent() && b.getIsCurrent()) return 1;
                        }
                        return Long.compare(b.getCreatedAt(), a.getCreatedAt());
                });
                
                // Filter by search query if provided
                List<com.aidentx.authservice.model.dto.UserSessionResponse> filteredResponses = allResponses;
                if (search != null && !search.trim().isEmpty()) {
                        String searchLower = search.toLowerCase().trim();
                        filteredResponses = allResponses.stream()
                                .filter(response -> 
                                        response.getUser().getUsername().toLowerCase().contains(searchLower) ||
                                        response.getUser().getEmail().toLowerCase().contains(searchLower) ||
                                        response.getIpAddress().toLowerCase().contains(searchLower) ||
                                        (response.getLocation() != null && response.getLocation().toLowerCase().contains(searchLower)) ||
                                        (response.getDevice() != null && response.getDevice().toLowerCase().contains(searchLower))
                                )
                                .collect(java.util.stream.Collectors.toList());
                        
                        logger.debug().category(AppConstants.LOG_CATEGORY_AUTH)
                                .message("Filtered sessions by search query")
                                .detail("originalCount", String.valueOf(allResponses.size()))
                                .detail("filteredCount", String.valueOf(filteredResponses.size()))
                                .detail("search", searchLower)
                                .log();
                }
                
                // Manual pagination
                int totalElements = filteredResponses.size();
                int totalPages = (int) Math.ceil((double) totalElements / size);
                int start = page * size;
                int end = Math.min(start + size, totalElements);
                
                List<com.aidentx.authservice.model.dto.UserSessionResponse> pageContent = 
                        start >= totalElements ? new java.util.ArrayList<>() : 
                        filteredResponses.subList(start, end);
                
                logger.info().category(AppConstants.LOG_CATEGORY_AUTH)
                                .message("Retrieved paginated sessions for tenant")
                                .detail("tenantId", tenantId)
                                .detail("page", String.valueOf(page))
                                .detail("size", String.valueOf(size))
                                .detail("totalElements", String.valueOf(totalElements))
                                .detail("totalPages", String.valueOf(totalPages))
                                .log();
                
                return new org.springframework.data.domain.PageImpl<>(
                        pageContent,
                        org.springframework.data.domain.PageRequest.of(page, size),
                        totalElements
                );
        }

        @Override
        public String extractTenantIdFromToken(String token) {
                logger.trace().category(AppConstants.LOG_CATEGORY_AUTH)
                                .message("Extracting tenant ID from token")
                                .log();
                try {
                        return jwtUtil.extractTenantId(token);
                } catch (Exception e) {
                        logger.error(e).category(AppConstants.LOG_CATEGORY_AUTH)
                                        .message("Failed to extract tenant ID from token")
                                        .log();
                        throw new RuntimeException("Invalid token", e);
                }
        }

        @Override
        public String extractUserIdFromToken(String token) {
                logger.trace().category(AppConstants.LOG_CATEGORY_AUTH)
                                .message("Extracting user ID from token")
                                .log();
                try {
                        return jwtUtil.extractUserId(token);
                } catch (Exception e) {
                        logger.error(e).category(AppConstants.LOG_CATEGORY_AUTH)
                                        .message("Failed to extract user ID from token")
                                        .log();
                        return null;
                }
        }

        @Override
        public Map<String, Object> getAuthProviders() {
                logger.trace().category(AppConstants.LOG_CATEGORY_CONFIGURATION)
                                .message(AppConstants.TRACE_GET_AUTH_PROVIDERS_METHOD).log();
                java.util.List<java.util.Map<String, Object>> providers = new java.util.ArrayList<>();
                LdapConfig ldapConfig = ssoConfigurationManager.getLdapConfig();
                if (ldapConfig != null && ldapConfig.isEnabled()) {
                        logger.debug().category(AppConstants.LOG_CATEGORY_CONFIGURATION)
                                        .message(String.format(AppConstants.DEBUG_LDAP_PROVIDER_ENABLED,
                                                        ldapConfig.getName()))
                                        .details(new java.util.HashMap<String, Object>() {{
                                                put("provider", "LDAP");
                                                put("providerName", ldapConfig.getName() != null ? ldapConfig.getName() : "");
                                        }})
                                        .log();
                        java.util.Map<String, Object> ldapProvider = new java.util.HashMap<>();
                        ldapProvider.put("name", ldapConfig.getName());
                        ldapProvider.put("id", "ldap");
                        ldapProvider.put("type", "LDAP");
                        ldapProvider.put("authorizationUri", "/api/v1/auth/ldap/login");
                        providers.add(ldapProvider);
                }
                SamlConfig samlConfig = ssoConfigurationManager.getSamlConfig();
                if (samlConfig != null && samlConfig.isEnabled()) {
                        logger.debug().category(AppConstants.LOG_CATEGORY_CONFIGURATION)
                                        .message(String.format(AppConstants.DEBUG_SAML_PROVIDER_ENABLED,
                                                        samlConfig.getName()))
                                        .details(new java.util.HashMap<String, Object>() {{
                                                put("provider", "SAML");
                                                put("providerName", samlConfig.getName() != null ? samlConfig.getName() : "");
                                        }})
                                        .log();
                        java.util.Map<String, Object> samlProvider = new java.util.HashMap<>();
                        samlProvider.put("name", samlConfig.getName());
                        samlProvider.put("type", "SAML");
                        samlProvider.put("id", "saml");
                        samlProvider.put("authorizationUri", "/api/v1/auth/saml/login");
                        providers.add(samlProvider);
                }
                OAuth2Config oauth2Config = ssoConfigurationManager.getOAuth2Config();
                if (oauth2Config != null && oauth2Config.isEnabled()) {
                        logger.debug().category(AppConstants.LOG_CATEGORY_CONFIGURATION)
                                        .message(String.format(AppConstants.DEBUG_OAUTH2_PROVIDER_ENABLED,
                                                        oauth2Config.getName()))
                                        .details(new java.util.HashMap<String, Object>() {{
                                                put("provider", "OAUTH2");
                                                put("providerName", oauth2Config.getName() != null ? oauth2Config.getName() : "");
                                        }})
                                        .log();
                        java.util.Map<String, Object> oauth2Provider = new java.util.HashMap<>();
                        oauth2Provider.put("name", oauth2Config.getName());
                        oauth2Provider.put("id", "oauth2");
                        oauth2Provider.put("type", "OAUTH2");
                        oauth2Provider.put("authorizationUri", oauth2Config.getAuthorizationUri());
                        oauth2Provider.put("clientId", oauth2Config.getClientId());
                        oauth2Provider.put("redirectUri", oauth2Config.getRedirectUri());
                        providers.add(oauth2Provider);
                }
                logger.info().category(AppConstants.LOG_CATEGORY_CONFIGURATION)
                                .message(String.format(AppConstants.INFO_PROVIDERS_LIST_BUILT, providers.size()))
                                .detail("providerCount", providers.size())
                                .detail("ldapEnabled", ldapConfig != null && ldapConfig.isEnabled())
                                .detail("samlEnabled", samlConfig != null && samlConfig.isEnabled())
                                .detail("oauth2Enabled", oauth2Config != null && oauth2Config.isEnabled()).log();
                java.util.Map<String, Object> result = new java.util.HashMap<>();
                result.put("providers", providers);
                result.put("ldapEnabled", ldapConfig != null && ldapConfig.isEnabled());
                result.put("samlEnabled", samlConfig != null && samlConfig.isEnabled());
                result.put("oauth2Enabled", oauth2Config != null && oauth2Config.isEnabled());
                return result;
        }

        private HttpServletRequest getCurrentHttpRequest() {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                                .getRequestAttributes();
                return attributes != null ? attributes.getRequest() : null;
        }

        // --- Delegated Methods ---

        @Override
        public LoginSuccessResponse<TokenResponse> refreshAccessToken(RefreshTokenRequest request) {
                return tokenService.refreshAccessToken(request);
        }

        @Override
        public LoginSuccessResponse<TokenResponse> ldapLogin(LoginRequest request) {
                return ldapAuthService.ldapLogin(request);
        }

        @Override
        public Map<String, Object> initiateSamlLogin() {
                return samlAuthService.initiateSamlLogin();
        }

        @Override
        public Map<String, Object> initiateSamlSso() {
                return samlAuthService.initiateSamlSso();
        }

        @Override
        public LoginSuccessResponse<TokenResponse> handleSamlResponse(Authentication authentication) {
                return samlAuthService.handleSamlResponse(authentication);
        }

        @Override
        public LoginSuccessResponse<TokenResponse> handleSamlCallback(Boolean success, Boolean error, String message,
                        Authentication authentication) {
                return samlAuthService.handleSamlCallback(success, error, message, authentication);
        }

        @Override
        public Map<String, Object> initiateOAuth2Login() {
                return oAuth2AuthService.initiateOAuth2Login();
        }

        @Override
        public Object handleOAuth2CallbackGet(String code, String state, String error, String errorDescription,
                        Boolean success, String message, Authentication authentication) {
                return oAuth2AuthService.handleOAuth2CallbackGet(code, state, error, errorDescription, success, message,
                                authentication);
        }

        @Override
        public Map<String, Object> handleOAuth2Callback(Authentication authentication) {
                return oAuth2AuthService.handleOAuth2Callback(authentication);
        }

        @Override
        public StatusMessageResponse updatePassword(UpdatePasswordRequest request, UUID userId) {
                return passwordService.updatePassword(request, userId);
        }

        @Override
        public StatusMessageResponse requestPasswordReset(PasswordResetRequest request) {
                return passwordService.requestPasswordReset(request);
        }

        @Override
        public StatusMessageResponse resetPassword(String token, ResetPasswordRequest request) {
                return passwordService.resetPassword(token, request);
        }

        @Override
        public Map<String, String> getCurrentUserProfile() {
                org.springframework.security.core.Authentication authentication = 
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                
                if (authentication == null || !authentication.isAuthenticated()) {
                        throw new RuntimeException("User not authenticated");
                }
                
                Object principal = authentication.getPrincipal();
                if (!(principal instanceof org.springframework.security.core.userdetails.UserDetails)) {
                        throw new RuntimeException("Invalid authentication principal");
                }
                
                String userId = ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
                User user = userRepository.findById(UUID.fromString(userId))
                        .orElseThrow(() -> new UserNotFoundException("User not found"));
                
                return Map.of(
                        "username", user.getUsername() != null ? user.getUsername() : "",
                        "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()
                );
        }

        @Override
        @Transactional
        public void terminateSession(UUID sessionId, UUID userId, String tenantId) {
                logger.trace()
                        .category(com.aidentx.authservice.config.AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                        .message(com.aidentx.authservice.config.AppConstants.TRACE_TERMINATE_SESSION_START)
                        .detail(com.aidentx.authservice.config.AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                        .detail(com.aidentx.authservice.config.AppConstants.DETAIL_REQUESTING_USER_ID, userId.toString())
                        .detail(com.aidentx.authservice.config.AppConstants.DETAIL_TENANT_ID, tenantId)
                        .log();

                logger.trace()
                        .category(com.aidentx.authservice.config.AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                        .message(com.aidentx.authservice.config.AppConstants.TRACE_TERMINATE_SESSION_VALIDATING_AUTH)
                        .detail(com.aidentx.authservice.config.AppConstants.DETAIL_REQUESTING_USER_ID, userId.toString())
                        .log();

                // Load requesting user
                User requestingUser = userRepository.findById(userId)
                        .orElseThrow(() -> {
                                logger.error()
                                        .category(com.aidentx.authservice.config.AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                                        .message(com.aidentx.authservice.config.AppConstants.ERROR_TERMINATE_SESSION_UNAUTHORIZED)
                                        .detail(com.aidentx.authservice.config.AppConstants.DETAIL_REQUESTING_USER_ID, userId.toString())
                                        .log();
                                return new UserNotFoundException(AppConstants.USER_NOT_FOUND);
                        });

                logger.debug()
                        .category(com.aidentx.authservice.config.AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                        .message(com.aidentx.authservice.config.AppConstants.DEBUG_TERMINATE_SESSION_USER_EXTRACTED)
                        .detail(com.aidentx.authservice.config.AppConstants.DETAIL_REQUESTING_USER_ID, userId.toString())
                        .log();

                // Delegate to SessionTerminationService
                sessionTerminationService.terminateSession(sessionId, requestingUser, tenantId);

                logger.info()
                        .category(com.aidentx.authservice.config.AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                        .message(com.aidentx.authservice.config.AppConstants.INFO_TERMINATE_SESSION_BY_ADMIN)
                        .detail(com.aidentx.authservice.config.AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                        .detail(com.aidentx.authservice.config.AppConstants.DETAIL_REQUESTING_USER_ID, userId.toString())
                        .log();
        }
}