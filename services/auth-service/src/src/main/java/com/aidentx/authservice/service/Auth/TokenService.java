package com.aidentx.authservice.service.Auth;

import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.authservice.service.DynamicTokenService;
import com.aidentx.authservice.service.RefreshTokenService;
import com.aidentx.authservice.exception.InvalidRefreshTokenException;
import com.aidentx.authservice.model.dto.LoginSuccessResponse;
import com.aidentx.authservice.model.dto.RefreshTokenRequest;
import com.aidentx.authservice.model.dto.TokenResponse;
import com.aidentx.authservice.model.entity.RefreshToken;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.RefreshTokenRepository;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
public class TokenService {

    private final IUnifiedLogger logger;
    private final JwtUtil jwtUtil;
    private final DynamicTokenService dynamicTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public TokenService(UnifiedLoggerFactory unifiedLoggerFactory, JwtUtil jwtUtil,
            DynamicTokenService dynamicTokenService, RefreshTokenRepository refreshTokenRepository, 
            UserRepository userRepository, RefreshTokenService refreshTokenService) {
        this.logger = unifiedLoggerFactory.getLogger("TokenService", AppConstants.LOG_CATEGORY_TOKEN_MGMT);
        this.jwtUtil = jwtUtil;
        this.dynamicTokenService = dynamicTokenService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Refreshes the JWT access token using a valid refresh token.
     *
     * This method performs the following steps:
     * 1. Validates the refresh token's JWT structure and extracts the user ID.
     * 2. Checks that the refresh token exists in the database and is not revoked or
     * expired.
     * 3. Validates the token content against the user ID and token type.
     * 4. Fetches the associated user.
     * 5. Generates new access and refresh tokens.
     * 6. Revokes the old refresh token and saves the new refresh token to the
     * database.
     * 7. Returns the new tokens wrapped in a success response.
     *
     * @param request the refresh token request object containing the refresh token
     *                string provided by the client
     * @return a LoginSuccessResponse containing the new access and refresh tokens
     * @throws InvalidRefreshTokenException if the refresh token is malformed,
     *                                      revoked, expired, or invalid
     */
    @Transactional
    public LoginSuccessResponse<TokenResponse> refreshAccessToken(RefreshTokenRequest request) {
        logger.trace().message(AppConstants.TRACE_REFRESH_TOKEN_STARTED).log();
        try {
            String refreshTokenStr = request.getRefreshToken();
            logger.info().objectType("token")
                    .objectId(refreshTokenStr.substring(0, Math.min(refreshTokenStr.length(), 20)) + "...")
                    .message(AppConstants.INFO_TOKEN_REFRESH_STARTED).detail("operation", AppConstants.OP_TOKEN_REFRESH)
                    .detail("tokenPrefix", refreshTokenStr.substring(0, Math.min(refreshTokenStr.length(), 10))).log();

            String userIdStr;
            try {
                userIdStr = jwtUtil.extractUserId(refreshTokenStr);
                logger.debug().message(AppConstants.DEBUG_USER_ID_EXTRACTED).detail("userId", userIdStr).log();
            } catch (Exception e) {
                logger.error(e).objectType("token")
                        .objectId(refreshTokenStr.substring(0, Math.min(refreshTokenStr.length(), 20)) + "...")
                        .message(AppConstants.ERROR_MALFORMED_TOKEN).detail("operation", AppConstants.OP_TOKEN_REFRESH)
                        .failureReason("MALFORMED_TOKEN").log();
                throw new InvalidRefreshTokenException(AppConstants.ERROR_MALFORMED_REFRESH_TOKEN);
            }

            UUID userId = UUID.fromString(userIdStr);
            RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr).orElseThrow(() -> {
                logger.warn().objectType("token")
                        .objectId(refreshTokenStr.substring(0, Math.min(refreshTokenStr.length(), 20)) + "...")
                        .message(AppConstants.WARN_REFRESH_TOKEN_NOT_FOUND)
                        .details(Map.of("operation", AppConstants.OP_TOKEN_REFRESH, "userId", userId.toString()))
                        .failureReason("TOKEN_NOT_FOUND").log();
                return new InvalidRefreshTokenException(AppConstants.ERROR_REFRESH_TOKEN_NOT_FOUND);
            });

            if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                logger.warn().objectType("token")
                        .objectId(refreshTokenStr.substring(0, Math.min(refreshTokenStr.length(), 20)) + "...")
                        .message(AppConstants.WARN_REFRESH_TOKEN_EXPIRED)
                        .details(Map.of("operation", AppConstants.OP_TOKEN_REFRESH, "userId", userId.toString(),
                                "expiryDate", refreshToken.getExpiryDate().toString(), "currentTime",
                                Instant.now().toString()))
                        .failureReason("TOKEN_EXPIRED").log();
                throw new InvalidRefreshTokenException(AppConstants.ERROR_REFRESH_TOKEN_EXPIRED);
            }

            if (!jwtUtil.validateRefreshToken(refreshTokenStr, userId.toString())) {
                logger.warn().objectType("token")
                        .objectId(refreshTokenStr.substring(0, Math.min(refreshTokenStr.length(), 20)) + "...")
                        .message(AppConstants.WARN_REFRESH_TOKEN_VALIDATION_FAILED)
                        .details(Map.of("operation", AppConstants.OP_TOKEN_REFRESH, "userId", userId.toString()))
                        .failureReason("TOKEN_VALIDATION_FAILED").log();
                throw new InvalidRefreshTokenException(AppConstants.ERROR_INVALID_REFRESH_TOKEN);
            }

            User user = userRepository.findById(userId).orElseThrow(() -> {
                logger.warn().objectType("user").objectId(userId.toString())
                        .message(AppConstants.WARN_USER_NOT_FOUND_FOR_TOKEN)
                        .detail("operation", AppConstants.OP_TOKEN_REFRESH).failureReason("USER_NOT_FOUND").log();
                return new InvalidRefreshTokenException(AppConstants.ERROR_USER_NOT_FOUND_FOR_REFRESH_TOKEN);
            });

            // Generate new access token only (keep existing refresh token)
            String newAccessToken = dynamicTokenService.generateJwtToken(
                userId, user.getUsername(), user.getTenantId(), user.getIdentityId());
            
            // Get dynamic expiration time for response
            long expiresInMs = dynamicTokenService.getJwtExpirationForTenant(user.getTenantId());
            long expiresIn = expiresInMs / 1000; // Convert to seconds for response

            logger.info().objectType("user").objectId(user.getUsername())
                    .message(AppConstants.INFO_NEW_ACCESS_TOKEN_GENERATED).detail("operation", AppConstants.OP_TOKEN_REFRESH)
                    .details(Map.of("userId", userId.toString(), "accessTokenGenerated", true, "refreshTokenKept", true))
                    .log();

            // Keep the existing refresh token (no deletion or recreation)
            logger.debug().message(AppConstants.DEBUG_REFRESH_TOKEN_REUSED).detail("userId", userId.toString()).log();

            // Return the new access token with the existing refresh token
            TokenResponse tokenResponse = new TokenResponse(newAccessToken, expiresIn, refreshTokenStr);
            logger.info().objectType("user").objectId(user.getUsername())
                    .message(AppConstants.INFO_TOKEN_REFRESH_COMPLETED)
                    .details(Map.of("operation", AppConstants.OP_TOKEN_REFRESH, "userId", userId.toString(), "success",
                            true))
                    .log();
            return new LoginSuccessResponse<>(200, tokenResponse);
        } catch (InvalidRefreshTokenException e) {
            logger.warn().objectType("token").objectId("unknown")
                    .message(String.format(AppConstants.WARN_INVALID_REFRESH_TOKEN_EXCEPTION, e.getMessage()))
                    .detail("operation", AppConstants.OP_TOKEN_REFRESH).failureReason("InvalidRefreshTokenException")
                    .log();
            throw e;
        } catch (Exception e) {
            logger.error(e).objectType("token").objectId("unknown")
                    .message(String.format(AppConstants.ERROR_UNEXPECTED_TOKEN_REFRESH, e.getMessage()))
                    .detail("operation", AppConstants.OP_TOKEN_REFRESH).log();
            throw new RuntimeException(AppConstants.GENERIC_ERROR_MESSAGE, e);
        }
    }
}