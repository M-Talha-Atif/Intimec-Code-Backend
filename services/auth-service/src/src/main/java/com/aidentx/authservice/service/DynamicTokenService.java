package com.aidentx.authservice.service;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.model.entity.SessionControl;
import com.aidentx.authservice.repository.SessionControlRepository;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for generating JWT tokens with dynamic expiration based on tenant session control policies.
 * <p>
 * This service looks up the tenant's session control policy and uses those settings
 * to determine JWT and refresh token expiration times. If no policy is found,
 * it falls back to default values configured in application properties.
 * </p>
 *
 * @author Aidentx Development Team
 * @version 1.0
 * @since 2025-09-10
 */
@Service
public class DynamicTokenService {

    private final SessionControlRepository sessionControlRepository;
    private final JwtUtil jwtUtil;
    private final IUnifiedLogger logger;

    public DynamicTokenService(SessionControlRepository sessionControlRepository,
                              JwtUtil jwtUtil,
                              UnifiedLoggerFactory loggerFactory) {
        this.sessionControlRepository = sessionControlRepository;
        this.jwtUtil = jwtUtil;
        this.logger = loggerFactory.getLogger("DynamicTokenService", AppConstants.LOG_CATEGORY_SESSION_CONTROL);
    }

    /**
     * Generates a JWT token with dynamic expiration based on tenant's session control policy.
     *
     * @param userId   the UUID of the user
     * @param username the username
     * @param tenantId the tenant ID
     * @return the generated JWT token with tenant-specific expiration
     */
    public String generateJwtToken(UUID userId, String username, String tenantId,  UUID identityId) {
        logger.debug()
            .message(AppConstants.LOG_GENERATING_JWT_DYNAMIC)
            .detail(AppConstants.AUDIT_DETAIL_USER_ID, userId.toString())
            .detail(AppConstants.AUDIT_DETAIL_USERNAME, username)
            .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
            .detail("identityId", identityId != null ? identityId.toString() : "null")
            .log();

        Optional<SessionControl> sessionControl = sessionControlRepository.findByTenantId(tenantId);
        
        if (sessionControl.isPresent()) {
            long jwtExpirationMs = sessionControl.get().getJwtExpirationMs();
            
            logger.info()
                .message(AppConstants.LOG_USING_TENANT_JWT_EXPIRATION)
                .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.AUDIT_DETAIL_JWT_EXPIRATION_MS, jwtExpirationMs)
                .detail(AppConstants.AUDIT_DETAIL_JWT_EXPIRATION_MINUTES, jwtExpirationMs / 60000)
                .log();

            return jwtUtil.generateTokenWithCustomExpiration(userId, username, tenantId, identityId, jwtExpirationMs);
        } else {
            logger.warn()
                .message(AppConstants.LOG_NO_POLICY_DEFAULT_JWT)
                .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                .log();
            
            return jwtUtil.generateToken(userId, username, tenantId, identityId);
        }
    }

    /**
     * Generates a refresh token with dynamic expiration based on tenant's session control policy.
     *
     * @param userId   the UUID of the user
     * @param tenantId the tenant ID
     * @return the generated refresh token with tenant-specific expiration
     */
    public String generateRefreshToken(UUID userId, String tenantId) {
        logger.debug()
            .message(AppConstants.LOG_GENERATING_REFRESH_DYNAMIC)
            .detail(AppConstants.AUDIT_DETAIL_USER_ID, userId.toString())
            .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
            .log();

        Optional<SessionControl> sessionControl = sessionControlRepository.findByTenantId(tenantId);
        
        if (sessionControl.isPresent()) {
            long refreshTokenExpirationMs = sessionControl.get().getRefreshTokenExpirationMs();
            
            logger.info()
                .message(AppConstants.LOG_USING_TENANT_REFRESH_EXPIRATION)
                .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.AUDIT_DETAIL_REFRESH_TOKEN_EXPIRATION_MS, refreshTokenExpirationMs)
                .detail(AppConstants.AUDIT_DETAIL_REFRESH_TOKEN_EXPIRATION_HOURS, refreshTokenExpirationMs / 3600000)
                .log();

            return jwtUtil.generateRefreshTokenWithCustomExpiration(userId, refreshTokenExpirationMs);
        } else {
            logger.warn()
                .message(AppConstants.LOG_NO_POLICY_DEFAULT_REFRESH)
                .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                .log();
            
            return jwtUtil.generateRefreshToken(userId);
        }
    }

    /**
     * Gets the JWT expiration time for a specific tenant.
     * Falls back to default if no session control policy is found.
     *
     * @param tenantId the tenant ID
     * @return JWT expiration time in milliseconds
     */
    public long getJwtExpirationForTenant(String tenantId) {
        Optional<SessionControl> sessionControl = sessionControlRepository.findByTenantId(tenantId);
        
        if (sessionControl.isPresent()) {
            long expirationMs = sessionControl.get().getJwtExpirationMs();
            logger.debug()
                .message(AppConstants.LOG_RETRIEVED_JWT_EXPIRATION_POLICY)
                .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.AUDIT_DETAIL_JWT_EXPIRATION_MS, expirationMs)
                .log();
            return expirationMs;
        } else {
            long defaultExpiration = jwtUtil.getExpiration() * 1000; // Convert seconds to milliseconds
            logger.debug()
                .message(AppConstants.LOG_USING_DEFAULT_JWT_EXPIRATION)
                .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.AUDIT_DETAIL_DEFAULT_JWT_EXPIRATION_MS, defaultExpiration)
                .log();
            return defaultExpiration;
        }
    }

    /**
     * Gets the refresh token expiration time for a specific tenant.
     * Falls back to default if no session control policy is found.
     *
     * @param tenantId the tenant ID
     * @return refresh token expiration time in milliseconds
     */
    public long getRefreshTokenExpirationForTenant(String tenantId) {
        Optional<SessionControl> sessionControl = sessionControlRepository.findByTenantId(tenantId);
        
        if (sessionControl.isPresent()) {
            long expirationMs = sessionControl.get().getRefreshTokenExpirationMs();
            logger.debug()
                .message(AppConstants.LOG_RETRIEVED_REFRESH_EXPIRATION_POLICY)
                .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.AUDIT_DETAIL_REFRESH_TOKEN_EXPIRATION_MS, expirationMs)
                .log();
            return expirationMs;
        } else {
            long defaultExpiration = jwtUtil.getRefreshExpiration() * 1000; // Convert seconds to milliseconds
            logger.debug()
                .message(AppConstants.LOG_USING_DEFAULT_REFRESH_EXPIRATION)
                .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
                .detail(AppConstants.AUDIT_DETAIL_DEFAULT_JWT_EXPIRATION_MS, defaultExpiration)
                .log();
            return defaultExpiration;
        }
    }

    /**
     * Generates both JWT and refresh tokens with tenant-specific expiration settings.
     *
     * @param userId   the UUID of the user
     * @param username the username
     * @param tenantId the tenant ID
     * @return TokenPair containing both tokens
     */
    public TokenPair generateTokenPair(UUID userId, String username, String tenantId, UUID identityId) {
        logger.info()
            .message(AppConstants.LOG_GENERATING_TOKEN_PAIR)
            .detail(AppConstants.AUDIT_DETAIL_USER_ID, userId.toString())
            .detail(AppConstants.AUDIT_DETAIL_USERNAME, username)
            .detail(AppConstants.AUDIT_DETAIL_TENANT_ID, tenantId)
            .log();

        String jwtToken = generateJwtToken(userId, username, tenantId, identityId);
        String refreshToken = generateRefreshToken(userId, tenantId);

        return new TokenPair(jwtToken, refreshToken);
    }

    /**
     * Data class to hold both JWT and refresh tokens.
     */
    public static class TokenPair {
        private final String jwtToken;
        private final String refreshToken;

        public TokenPair(String jwtToken, String refreshToken) {
            this.jwtToken = jwtToken;
            this.refreshToken = refreshToken;
        }

        public String getJwtToken() {
            return jwtToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}
