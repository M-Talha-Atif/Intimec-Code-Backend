package com.aidentx.authservice.service;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.model.entity.SessionControl;
import com.aidentx.authservice.model.entity.UserSession;
import com.aidentx.authservice.repository.SessionControlRepository;
import com.aidentx.authservice.repository.UserSessionRepository;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.authservice.utils.RequestUtils;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user sessions.
 * 
 * <p>Handles creation, retrieval, and deletion of user sessions across all
 * authentication methods (normal login, LDAP, OAuth2, SAML).</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class UserSessionService {
    
    private final UserSessionRepository userSessionRepository;
    private final SessionControlRepository sessionControlRepository;
    private final JwtUtil jwtUtil;
    private final UnifiedLoggerFactory loggerFactory;
    private final IUnifiedLogger logger;
    private final UserAgentParser userAgentParser;
    
    public UserSessionService(
            UserSessionRepository userSessionRepository, 
            SessionControlRepository sessionControlRepository,
            JwtUtil jwtUtil,
            UnifiedLoggerFactory loggerFactory, 
            UserAgentParser userAgentParser) {
        this.userSessionRepository = userSessionRepository;
        this.sessionControlRepository = sessionControlRepository;
        this.jwtUtil = jwtUtil;
        this.loggerFactory = loggerFactory;
        this.logger = loggerFactory.getLogger("UserSessionService", AppConstants.LOG_CATEGORY_SESSION);
        this.userAgentParser = userAgentParser;
    }
    
    /**
     * Create a new session for a user after successful authentication.
     * 
     * @param userId the user ID
     * @param tenantId the tenant ID
     * @param refreshToken the refresh token
     * @param authMethod the authentication method (PASSWORD, LDAP, OAUTH2, SAML)
     * @param request the HTTP request to extract IP and user agent
     * @return the created session
     */
    @Transactional
    public UserSession createSession(UUID userId, String tenantId, String refreshToken, 
                                     String authMethod, HttpServletRequest request) {
        String ipAddress = RequestUtils.getClientIP(request);
        String userAgent = RequestUtils.getUserAgent(request);
        
        logger.info()
            .message(AppConstants.LOG_SESSION_CREATING)
            .detail("userId", userId.toString())
            .detail("tenantId", tenantId != null ? tenantId.toString() : "null")
            .detail("authMethod", authMethod)
            .detail("ipAddress", ipAddress)
            .detail("userAgent", userAgent)
            .objectType(AppConstants.OBJECT_TYPE_SESSION_CREATION)
            .log();
        
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setTenantId(tenantId);
        session.setRefreshToken(refreshToken);
        session.setIpAddress(ipAddress);
        session.setUserAgent(userAgent);
        session.setAuthMethod(authMethod);
        long currentTime = System.currentTimeMillis();
        session.setCreatedAt(currentTime);
        session.setLastActivityAt(currentTime); // Set initial activity time to login time
        
        // Parse and set device information
        String device = userAgentParser.parseDevice(userAgent);
        boolean isMobile = userAgentParser.isMobile(userAgent);
        boolean isDesktop = userAgentParser.isDesktop(userAgent);
        
        session.setDevice(device);
        session.setIsMobile(isMobile);
        session.setIsDesktop(isDesktop);
        
        UserSession savedSession = userSessionRepository.save(session);
        
        logger.info()
            .message(AppConstants.LOG_SESSION_CREATED_SUCCESS)
            .detail("sessionId", savedSession.getSessionId().toString())
            .detail("userId", userId.toString())
            .objectType(AppConstants.OBJECT_TYPE_SESSION_CREATION)
            .log();
        
        return savedSession;
    }
    
    /**
     * Find a session by refresh token.
     * 
     * @param refreshToken the refresh token
     * @return Optional containing the session if found
     */
    public Optional<UserSession> findByRefreshToken(String refreshToken) {
        return userSessionRepository.findByRefreshToken(refreshToken);
    }
    
    /**
     * Get all sessions for a user.
     * 
     * @param userId the user ID
     * @return list of user sessions
     */
    public List<UserSession> getUserSessions(UUID userId) {
        return userSessionRepository.findByUserId(userId);
    }
    
    /**
     * Delete a session by refresh token (used during logout).
     * 
     * @param refreshToken the refresh token
     * @return true if session was deleted, false otherwise
     */
    @Transactional
    public boolean deleteSessionByRefreshToken(String refreshToken) {
        Optional<UserSession> session = userSessionRepository.findByRefreshToken(refreshToken);
        
        if (session.isPresent()) {
            logger.info()
                .message(AppConstants.LOG_SESSION_DELETING)
                .detail("sessionId", session.get().getSessionId().toString())
                .detail("userId", session.get().getUserId().toString())
                .objectType(AppConstants.OBJECT_TYPE_SESSION_DELETION)
                .log();
            
            int deleted = userSessionRepository.deleteByRefreshToken(refreshToken);
            
            logger.info()
                .message(AppConstants.LOG_SESSION_DELETED_SUCCESS)
                .detail("deletedCount", String.valueOf(deleted))
                .objectType(AppConstants.OBJECT_TYPE_SESSION_DELETION)
                .log();
            
            return deleted > 0;
        }
        
        logger.warn()
            .message(AppConstants.LOG_SESSION_DELETE_NON_EXISTENT)
            .detail("refreshToken", refreshToken.substring(0, Math.min(20, refreshToken.length())) + "...")
            .objectType(AppConstants.OBJECT_TYPE_SESSION_DELETION)
            .log();
        
        return false;
    }
    
    /**
     * Delete all sessions for a user (used for logout from all devices).
     * 
     * @param userId the user ID
     * @return number of deleted sessions
     */
    @Transactional
    public int deleteAllUserSessions(UUID userId) {
        logger.info()
            .message(AppConstants.LOG_SESSION_DELETING_ALL_USER)
            .detail("userId", userId.toString())
            .objectType(AppConstants.OBJECT_TYPE_SESSION_DELETION)
            .log();
        
        int deleted = userSessionRepository.deleteByUserId(userId);
        
        logger.info()
            .message(AppConstants.LOG_SESSION_DELETED_ALL_USER)
            .detail("userId", userId.toString())
            .detail("deletedCount", String.valueOf(deleted))
            .objectType(AppConstants.OBJECT_TYPE_SESSION_DELETION)
            .log();
        
        return deleted;
    }
    
    /**
     * Check if a session exists by session ID.
     * 
     * @param sessionId the session ID
     * @return true if session exists, false otherwise
     */
    public boolean existsBySessionId(UUID sessionId) {
        return userSessionRepository.existsById(sessionId);
    }
    
    /**
     * Check if a session exists with the given refresh token.
     * 
     * @param refreshToken the refresh token
     * @return true if session exists, false otherwise
     */
    public boolean sessionExists(String refreshToken) {
        return userSessionRepository.existsByRefreshToken(refreshToken);
    }
    
    /**
     * Get all sessions for a specific tenant.
     * 
     * @param tenantId the tenant ID
     * @return list of sessions for the tenant
     */
    public List<UserSession> getSessionsByTenantId(String tenantId) {
        logger.debug()
            .message(AppConstants.LOG_SESSION_RETRIEVING_FOR_TENANT)
            .detail("tenantId", tenantId)
            .objectType(AppConstants.OBJECT_TYPE_SESSION_RETRIEVAL)
            .log();
        
        return userSessionRepository.findByTenantId(tenantId);
    }
    
    /**
     * Clean up expired sessions for a specific tenant (lazy deletion).
     * Called when retrieving sessions to automatically remove old sessions.
     * 
     * @param tenantId the tenant ID
     * @return number of deleted sessions
     */
    @Transactional
    public int cleanupExpiredSessionsForTenant(String tenantId) {
        // Get tenant's refresh token expiration time
        long refreshTokenExpirationMs = getRefreshTokenExpirationForTenant(tenantId);
        
        // Calculate expiration threshold: current time - refresh token expiration
        long expirationThreshold = System.currentTimeMillis() - refreshTokenExpirationMs;
        
        logger.debug()
            .message(AppConstants.LOG_SESSION_CLEANUP_STARTED)
            .detail("tenantId", tenantId)
            .detail("refreshTokenExpirationMs", String.valueOf(refreshTokenExpirationMs))
            .detail("expirationThreshold", String.valueOf(expirationThreshold))
            .objectType(AppConstants.OBJECT_TYPE_SESSION_CLEANUP)
            .log();
        
        // Get all sessions for this tenant
        List<UserSession> tenantSessions = userSessionRepository.findByTenantId(tenantId);
        int deletedCount = 0;
        
        // Delete sessions older than expiration threshold
        for (UserSession session : tenantSessions) {
            if (session.getCreatedAt() < expirationThreshold) {
                userSessionRepository.delete(session);
                deletedCount++;
                
                logger.info()
                    .message(AppConstants.LOG_SESSION_DELETED_DURING_CLEANUP)
                    .detail("sessionId", session.getSessionId().toString())
                    .detail("userId", session.getUserId().toString())
                    .detail("tenantId", tenantId)
                    .detail("createdAt", String.valueOf(session.getCreatedAt()))
                    .detail("expirationThreshold", String.valueOf(expirationThreshold))
                    .detail("ageMs", String.valueOf(System.currentTimeMillis() - session.getCreatedAt()))
                    .objectType(AppConstants.OBJECT_TYPE_SESSION_CLEANUP)
                    .log();
            }
        }
        
        if (deletedCount > 0) {
            logger.info()
                .message(AppConstants.LOG_SESSION_CLEANUP_COMPLETED)
                .detail("tenantId", tenantId)
                .detail("deletedCount", String.valueOf(deletedCount))
                .objectType(AppConstants.OBJECT_TYPE_SESSION_CLEANUP)
                .log();
        }
        
        return deletedCount;
    }
    
    /**
     * Get the refresh token expiration time for a tenant.
     * Falls back to default if no session control policy exists.
     * 
     * @param tenantId the tenant ID
     * @return refresh token expiration in milliseconds
     */
    private long getRefreshTokenExpirationForTenant(String tenantId) {
        Optional<SessionControl> sessionControl = sessionControlRepository.findByTenantId(tenantId);
        
        if (sessionControl.isPresent()) {
            long expirationMs = sessionControl.get().getRefreshTokenExpirationMs();
            logger.debug()
                .message(AppConstants.LOG_SESSION_USING_TENANT_EXPIRATION)
                .detail("tenantId", tenantId)
                .detail("refreshTokenExpirationMs", String.valueOf(expirationMs))
                .objectType(AppConstants.OBJECT_TYPE_SESSION_CLEANUP)
                .log();
            return expirationMs;
        } else {
            // Fallback to default refresh token expiration from application properties
            long defaultExpiration = jwtUtil.getRefreshExpiration() * 1000; // Convert seconds to milliseconds
            logger.debug()
                .message(AppConstants.LOG_SESSION_USING_DEFAULT_EXPIRATION)
                .detail("tenantId", tenantId)
                .detail("defaultRefreshTokenExpirationMs", String.valueOf(defaultExpiration))
                .objectType(AppConstants.OBJECT_TYPE_SESSION_CLEANUP)
                .log();
            return defaultExpiration;
        }
    }
}
