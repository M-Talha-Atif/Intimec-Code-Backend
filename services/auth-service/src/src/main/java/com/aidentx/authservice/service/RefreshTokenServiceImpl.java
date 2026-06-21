package com.aidentx.authservice.service;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.model.entity.RefreshToken;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.RefreshTokenRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Implementation of the {@link RefreshTokenService} interface.
 * <p>
 * Provides operations for creating, validating, revoking, and deleting refresh
 * tokens.
 * Uses application-wide constants for logging messages.
 * </p>
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final IUnifiedLogger logger;
    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Expiration duration (in milliseconds) for refresh tokens.
     * Injected from application properties:
     * {@code security.jwt.refresh-token.expiration-ms}.
     */
    @Value("${security.jwt.refresh-token.expiration-ms}")
    private Long refreshTokenDurationMs;

    /**
     * Constructs the service with the required repository and logger factory.
     *
     * @param refreshTokenRepository the repository managing refresh token
     *                               persistence
     * @param loggerFactory          the factory to create a logger instance for
     *                               this class
     */
    public RefreshTokenServiceImpl(RefreshTokenRepository refreshTokenRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.logger = loggerFactory.getLogger("RefreshTokenService",
                "token-refresh");
    }

    /**
     * Creates and saves a new refresh token for a given user.
     * Logs the creation process and outcome.
     *
     * @param user  the user to associate with the refresh token
     * @param token the string value of the refresh token
     * @return the persisted {@link RefreshToken} entity
     */
    @Override
    public RefreshToken createRefreshToken(User user, String token) {
        logger.trace()
                .category(AppConstants.LOG_CATEGORY_REFRESH_TOKEN)
                .message(AppConstants.LOG_MSG_CREATING_REFRESH_TOKEN)
                .detail("userId", user.getId().toString()).log();

        try {
            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setToken(token);
            refreshToken.setUser(user);
            refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

            RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
            logger.info().message(AppConstants.LOG_MSG_REFRESH_TOKEN_CREATED_SUCCESS + user.getId());
            return savedToken;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_REFRESH_TOKEN_CREATE_ERROR + user.getId()).log();
            throw new RuntimeException(AppConstants.REFRESH_TOKEN_CREATE_EXCEPTION, e);
        }
    }

    /**
     * Validates a refresh token string by ensuring it exists, is not revoked,
     * and has not expired.
     * Logs each validation step and any issues encountered.
     *
     * @param token the refresh token string to validate
     * @return the valid {@link RefreshToken} entity
     * @throws RuntimeException if the token is missing, revoked, or expired
     */
    @Override
    public RefreshToken validateRefreshToken(String token) {
        logger.trace().category(AppConstants.LOG_CATEGORY_REFRESH_TOKEN)
                .message(AppConstants.LOG_MSG_VALIDATING_REFRESH_TOKEN).log();
        try {
            RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                    .orElseThrow(() -> {
                        logger.warn().message(AppConstants.LOG_MSG_REFRESH_TOKEN_NOT_FOUND).log();
                        return new RuntimeException(AppConstants.ERROR_REFRESH_TOKEN_NOT_FOUND_OR_REVOKED);
                    });

            if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                logger.warn().message(AppConstants.LOG_MSG_REFRESH_TOKEN_EXPIRED + refreshToken.getExpiryDate());
                throw new RuntimeException(AppConstants.ERROR_REFRESH_TOKEN_EXPIRED);
            }

            logger.debug()
                    .category(AppConstants.LOG_CATEGORY_REFRESH_TOKEN)
                    .message(AppConstants.LOG_MSG_REFRESH_TOKEN_VALID);
            return refreshToken;

        } catch (RuntimeException e) {
            // Re-throw known runtime exceptions
            throw e;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_REFRESH_TOKEN_VALIDATE_UNEXPECTED).log();
            throw new RuntimeException(AppConstants.ERROR_REFRESH_TOKEN_VALIDATE_UNEXPECTED, e);
        }
    }

    /**
     * Revokes a refresh token by deleting it from the database.
     * Logs the revocation attempt and its result.
     *
     * @param token the refresh token string to revoke
     */
    @Override
    public void revokeRefreshToken(String token) {
        logger.trace()
                .category(AppConstants.LOG_CATEGORY_REFRESH_TOKEN)
                .message(AppConstants.LOG_MSG_REVOKE_REFRESH_TOKEN).log();
        try {
            long deletedCount = refreshTokenRepository.deleteByToken(token);
            if (deletedCount > 0) {
                logger.info().message(AppConstants.LOG_MSG_REFRESH_TOKEN_REVOKED_SUCCESS).log();
            } else {
                logger.warn().message(AppConstants.LOG_MSG_REFRESH_TOKEN_REVOKE_NOT_FOUND).log();
            }
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_REFRESH_TOKEN_REVOKE_ERROR).log();
            throw new RuntimeException(AppConstants.ERROR_REFRESH_TOKEN_REVOKE_FAILED, e);
        }
    }

    /**
     * Deletes all refresh tokens associated with a user.
     * Logs the deletion process.
     *
     * @param user the user whose tokens should be deleted
     */
    @Override
    @Transactional
    public void deleteAllUserTokens(User user) {
        logger.trace().category(AppConstants.LOG_CATEGORY_REFRESH_TOKEN)
                .message(AppConstants.LOG_MSG_DELETE_ALL_REFRESH_TOKENS)
                .detail("userId", user.getId().toString());
        try {
            refreshTokenRepository.deleteAllByUser(user);
            logger.info().message(AppConstants.LOG_MSG_REFRESH_TOKEN_DELETE_ALL_SUCCESS + user.getId());
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_MSG_REFRESH_TOKEN_DELETE_ALL_ERROR + user.getId()).log();
            throw new RuntimeException(AppConstants.ERROR_REFRESH_TOKEN_DELETE_ALL_FAILED, e);
        }
    }

    /**
     * Deletes a specific refresh token from the database.
     * Logs the deletion process.
     *
     * @param token the refresh token string to delete
     */
    @Override
    @Transactional
    public void deleteByToken(String token) {
        logger.trace().category(AppConstants.LOG_CATEGORY_REFRESH_TOKEN)
                .message("Deleting specific refresh token")
                .detail("tokenPrefix", token.substring(0, Math.min(10, token.length())) + "...");
        try {
            refreshTokenRepository.deleteByToken(token);
            logger.info().message("Refresh token deleted successfully");
        } catch (Exception e) {
            logger.error(e).message("Error deleting refresh token").log();
            throw new RuntimeException("Failed to delete refresh token", e);
        }
    }
}