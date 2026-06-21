package com.aidentx.authservice.service.Auth;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.exception.InvalidRefreshTokenException;
import com.aidentx.authservice.model.dto.LoginSuccessResponse;
import com.aidentx.authservice.model.dto.RefreshTokenRequest;
import com.aidentx.authservice.model.dto.TokenResponse;
import com.aidentx.authservice.model.entity.RefreshToken;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.RefreshTokenRepository;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.service.RefreshTokenService;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Rigorous unit tests for {@link TokenService}.
 * <p>
 * This class tests the JWT refresh token functionality, ensuring all validation
 * paths (success, expiration, not found, malformed) are covered.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    // --- Mocks for Dependencies ---
    @Mock private JwtUtil jwtUtil;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UnifiedLoggerFactory loggerFactory;

    // --- Mocks for Logging ---
    @Mock private IUnifiedLogger logger;
    @Mock private LogEventBuilder logEventBuilder;

    // --- SUT (System Under Test) ---
    private TokenService tokenService;

    // --- Test Data ---
    private UUID userId;
    private User user;
    private String validRefreshToken;
    private String newAccessToken;
    private String newRefreshTokenStr;

    @BeforeEach
    void setUp() {
        // Mock the logger factory and fluent API
        when(loggerFactory.getLogger(anyString(), anyString())).thenReturn(logger);
        lenient().when(logger.trace()).thenReturn(logEventBuilder);
        lenient().when(logger.info()).thenReturn(logEventBuilder);
        lenient().when(logger.debug()).thenReturn(logEventBuilder);
        lenient().when(logger.warn()).thenReturn(logEventBuilder);
        lenient().when(logger.error(any(Throwable.class))).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.message(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.detail(anyString(), any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.details(any())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectId(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.objectType(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.failureReason(anyString())).thenReturn(logEventBuilder);
        doNothing().when(logEventBuilder).log();

        // Instantiate the TokenService
        tokenService = new TokenService(
            loggerFactory,
            jwtUtil,
            refreshTokenRepository,
            userRepository,
            refreshTokenService
        );

        // Setup common test data
        userId = UUID.randomUUID();
        user = new User();
        user.setId(userId);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        validRefreshToken = "valid-refresh-token";
        newAccessToken = "new-access-token";
        newRefreshTokenStr = "new-refresh-token";
    }

    private RefreshTokenRequest buildRefreshTokenRequest(String token) {
        var req = new RefreshTokenRequest();
        req.setRefreshToken(token);
        return req;
    }
    
    @Test
    @DisplayName("Refresh Token: Should succeed with a valid token")
    void refreshAccessToken_shouldSucceed_whenTokenIsValid() {
        // Arrange
        RefreshTokenRequest request = buildRefreshTokenRequest(validRefreshToken);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(validRefreshToken);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(jwtUtil.extractUserId(validRefreshToken)).thenReturn(userId.toString());
        when(refreshTokenRepository.findByToken(validRefreshToken)).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.validateRefreshToken(validRefreshToken, userId.toString())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(userId)).thenReturn(newAccessToken);
        when(jwtUtil.generateRefreshToken(userId)).thenReturn(newRefreshTokenStr);

        // Act
        LoginSuccessResponse<TokenResponse> response = tokenService.refreshAccessToken(request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatus());
        TokenResponse tokenResponse = response.getData();
        assertEquals(newAccessToken, tokenResponse.getToken());
        assertEquals(newRefreshTokenStr, tokenResponse.getRefreshToken());

        // Verify interactions
        verify(refreshTokenRepository).delete(refreshToken);
        verify(refreshTokenService).createRefreshToken(user, newRefreshTokenStr);
        verify(jwtUtil).generateToken(userId);
        verify(jwtUtil).generateRefreshToken(userId);
    }

    @Test
    @DisplayName("Refresh Token: Should throw InvalidRefreshTokenException for malformed token")
    void refreshAccessToken_shouldThrowException_whenTokenIsMalformed() {
        // Arrange
        String malformedToken = "not-a-jwt";
        RefreshTokenRequest request = buildRefreshTokenRequest(malformedToken);
        when(jwtUtil.extractUserId(malformedToken)).thenThrow(new RuntimeException("JWT parsing error"));

        // Act & Assert
        InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshAccessToken(request));

        assertEquals(AppConstants.ERROR_MALFORMED_REFRESH_TOKEN, exception.getMessage());
        verify(refreshTokenRepository, never()).findByToken(any());
    }

    @Test
    @DisplayName("Refresh Token: Should throw InvalidRefreshTokenException when token not found in DB")
    void refreshAccessToken_shouldThrowException_whenTokenNotFoundInDb() {
        // Arrange
        RefreshTokenRequest request = buildRefreshTokenRequest(validRefreshToken);
        when(jwtUtil.extractUserId(validRefreshToken)).thenReturn(userId.toString());
        when(refreshTokenRepository.findByToken(validRefreshToken)).thenReturn(Optional.empty());

        // Act & Assert
        InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshAccessToken(request));
        
        assertEquals(AppConstants.ERROR_REFRESH_TOKEN_NOT_FOUND, exception.getMessage());
        verify(jwtUtil, never()).validateRefreshToken(anyString(), anyString());
    }
    
    @Test
    @DisplayName("Refresh Token: Should throw InvalidRefreshTokenException for expired token")
    void refreshAccessToken_shouldThrowException_whenTokenIsExpired() {
        // Arrange
        RefreshTokenRequest request = buildRefreshTokenRequest(validRefreshToken);
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken(validRefreshToken);
        expiredToken.setUser(user);
        expiredToken.setExpiryDate(Instant.now().minusSeconds(60)); // Expired

        when(jwtUtil.extractUserId(validRefreshToken)).thenReturn(userId.toString());
        when(refreshTokenRepository.findByToken(validRefreshToken)).thenReturn(Optional.of(expiredToken));

        // Act & Assert
        InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshAccessToken(request));
        
        assertEquals(AppConstants.ERROR_REFRESH_TOKEN_EXPIRED, exception.getMessage());
        verify(jwtUtil, never()).validateRefreshToken(anyString(), anyString());
    }

    @Test
    @DisplayName("Refresh Token: Should throw InvalidRefreshTokenException on validation failure")
    void refreshAccessToken_shouldThrowException_whenTokenValidationFails() {
        // Arrange
        RefreshTokenRequest request = buildRefreshTokenRequest(validRefreshToken);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(validRefreshToken);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(jwtUtil.extractUserId(validRefreshToken)).thenReturn(userId.toString());
        when(refreshTokenRepository.findByToken(validRefreshToken)).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.validateRefreshToken(validRefreshToken, userId.toString())).thenReturn(false);

        // Act & Assert
        InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshAccessToken(request));
        
        assertEquals(AppConstants.ERROR_INVALID_REFRESH_TOKEN, exception.getMessage());
        verify(userRepository, never()).findById(any());
    }
    
    @Test
    @DisplayName("Refresh Token: Should throw InvalidRefreshTokenException when user not found")
    void refreshAccessToken_shouldThrowException_whenUserIsNotFound() {
        // Arrange
        RefreshTokenRequest request = buildRefreshTokenRequest(validRefreshToken);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(validRefreshToken);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(jwtUtil.extractUserId(validRefreshToken)).thenReturn(userId.toString());
        when(refreshTokenRepository.findByToken(validRefreshToken)).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.validateRefreshToken(validRefreshToken, userId.toString())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.empty()); // User not found

        // Act & Assert
        InvalidRefreshTokenException exception = assertThrows(InvalidRefreshTokenException.class,
                () -> tokenService.refreshAccessToken(request));
        
        assertEquals(AppConstants.ERROR_USER_NOT_FOUND_FOR_REFRESH_TOKEN, exception.getMessage());
        verify(jwtUtil, never()).generateToken(any());
    }
    
    @Test
    @DisplayName("Refresh Token: Should throw RuntimeException on unexpected error")
    void refreshAccessToken_shouldThrowRuntimeException_onUnexpectedError() {
        // Arrange
        RefreshTokenRequest request = buildRefreshTokenRequest(validRefreshToken);
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(validRefreshToken);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusSeconds(3600));

        when(jwtUtil.extractUserId(validRefreshToken)).thenReturn(userId.toString());
        when(refreshTokenRepository.findByToken(validRefreshToken)).thenReturn(Optional.of(refreshToken));
        when(jwtUtil.validateRefreshToken(validRefreshToken, userId.toString())).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        // Simulate an unexpected database error
        doThrow(new RuntimeException("Database connection error")).when(refreshTokenRepository).delete(any(RefreshToken.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> tokenService.refreshAccessToken(request));
        
        assertEquals(AppConstants.GENERIC_ERROR_MESSAGE, exception.getMessage());
        verify(logger).error(any(RuntimeException.class));
    }
}