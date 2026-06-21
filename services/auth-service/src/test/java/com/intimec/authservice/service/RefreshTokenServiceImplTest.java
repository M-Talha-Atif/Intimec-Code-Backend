package com.aidentx.authservice.service;

import com.aidentx.authservice.model.entity.RefreshToken;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.repository.RefreshTokenRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RefreshTokenServiceImpl}.
 * <p>
 * This test class currently contains tests for the creation
 * of refresh tokens.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceImplTest {

    private final long refreshTokenDurationMs = 3600000L; // 1 hour

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UnifiedLoggerFactory loggerFactory; // Mock the logger factory

    @Mock
    private IUnifiedLogger logger;

    @Mock
    private LogEventBuilder logEventBuilder;

    @InjectMocks
    private RefreshTokenServiceImpl refreshTokenService;

    /**
     * Setup method runs before each test.
     * Manually injects the refresh token expiration duration because
     *
     * @Value injection does not work in unit tests.
     */
    @BeforeEach
    void setUp() {
        // Mock the factory to return our mocked logger - using the exact call the service makes
        when(loggerFactory.getLogger("RefreshTokenService", "token-refresh")).thenReturn(logger);
        
        // Mock logger methods that might be called - using lenient to avoid unnecessary stubbing errors
        lenient().when(logger.trace()).thenReturn(logEventBuilder);
        lenient().when(logger.info()).thenReturn(logEventBuilder);
        lenient().when(logger.debug()).thenReturn(logEventBuilder);
        lenient().when(logger.warn()).thenReturn(logEventBuilder);
        lenient().when(logger.error(any(Exception.class))).thenReturn(logEventBuilder);
        
        // Mock LogEventBuilder fluent methods
        lenient().when(logEventBuilder.message(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.category(anyString())).thenReturn(logEventBuilder);
        lenient().when(logEventBuilder.detail(anyString(), any())).thenReturn(logEventBuilder);
        
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, loggerFactory);
        try {
            var field = RefreshTokenServiceImpl.class.getDeclaredField("refreshTokenDurationMs");
            field.setAccessible(true);
            field.set(refreshTokenService, refreshTokenDurationMs);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set refreshTokenDurationMs for testing", e);
        }
    }

    /**
     * Test that a refresh token is created and saved correctly for a given user.
     * Verifies that the returned token is not null and contains expected values.
     */
    @Test
    @DisplayName("Should create and save a new refresh token for a user")
    void testCreateRefreshToken_success() {
        User user = new User();
        user.setId(java.util.UUID.randomUUID());

        String tokenStr = "sample-refresh-token";

        RefreshToken savedToken = new RefreshToken();
        savedToken.setToken(tokenStr);
        savedToken.setUser(user);
        savedToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));

        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(savedToken);

        RefreshToken result = refreshTokenService.createRefreshToken(user, tokenStr);

        assertNotNull(result);
        assertEquals(tokenStr, result.getToken());
        assertEquals(user, result.getUser());
        assertTrue(result.getExpiryDate().isAfter(Instant.now()));

        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    /**
     * Test that a valid refresh token is correctly validated and returned.
     * Verifies the token is found and not expired.
     */
    @Test
    @DisplayName("Should validate and return refresh token when valid")
    void testValidateRefreshToken_success() {
        String tokenStr = "valid-refresh-token";

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(tokenStr);
        refreshToken.setExpiryDate(Instant.now().plusMillis(10000)); // still valid

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(refreshToken));

        RefreshToken result = refreshTokenService.validateRefreshToken(tokenStr);

        assertNotNull(result);
        assertEquals(tokenStr, result.getToken());
        assertTrue(result.getExpiryDate().isAfter(Instant.now()));

        verify(refreshTokenRepository, times(1)).findByToken(tokenStr);
    }

    /**
     * Test that validating an expired refresh token throws an exception.
     * Verifies proper exception is thrown when token expiry date is in the past.
     */
    @Test
    @DisplayName("Should throw RuntimeException when refresh token is expired")
    void testValidateRefreshToken_expiredToken_throwsException() {
        String tokenStr = "expired-refresh-token";

        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setToken(tokenStr);
        expiredToken.setExpiryDate(Instant.now().minusSeconds(10)); // expired 10 seconds ago

        when(refreshTokenRepository.findByToken(tokenStr)).thenReturn(Optional.of(expiredToken));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.validateRefreshToken(tokenStr);
        });

        assertTrue(exception.getMessage().contains("expired"));

        verify(refreshTokenRepository, times(1)).findByToken(tokenStr);
    }

    /**
     * Test that validating a refresh token that does not exist
     * throws a RuntimeException indicating the token was not found or revoked.
     */
    @Test
    @DisplayName("Should throw exception when refresh token not found")
    void shouldThrowExceptionWhenRefreshTokenNotFound() {
        // Arrange
        String missingToken = "non-existent-token";

        when(refreshTokenRepository.findByToken(missingToken)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            refreshTokenService.validateRefreshToken(missingToken);
        });

        assertEquals("Refresh token not found or has been revoked", exception.getMessage());
        verify(refreshTokenRepository).findByToken(missingToken);
    }


    /**
     * Test case for revoking a refresh token.
     * <p>
     * Verifies that the token is deleted successfully when it exists,
     * and appropriate logs and repository methods are called.
     * Also tests the case where the token to revoke does not exist.
     * </p>
     */
    @Test
    @DisplayName("Should revoke refresh token successfully when token exists")
    void shouldRevokeRefreshTokenSuccessfully() {
        String token = "test-refresh-token";

        // Mock deleteByToken to simulate successful deletion (returns 1)
        when(refreshTokenRepository.deleteByToken(token)).thenReturn(1L);

        // Act
        assertDoesNotThrow(() -> refreshTokenService.revokeRefreshToken(token));

        // Verify that deleteByToken was called once with the correct token
        verify(refreshTokenRepository, times(1)).deleteByToken(token);
    }

    /**
     * Test case for revoking a refresh token that does not exist.
     * <p>
     * Verifies that the method handles the case gracefully when
     * no token is deleted (deleteByToken returns 0).
     * </p>
     */
    @Test
    @DisplayName("Should handle revocation when token does not exist")
    void shouldHandleRevocationWhenTokenNotFound() {
        String token = "non-existent-token";

        // Mock deleteByToken to simulate no deletion (returns 0)
        when(refreshTokenRepository.deleteByToken(token)).thenReturn(0L);

        // Act
        assertDoesNotThrow(() -> refreshTokenService.revokeRefreshToken(token));

        // Verify that deleteByToken was called once with the correct token
        verify(refreshTokenRepository, times(1)).deleteByToken(token);
    }
}