package com.aidentx.authservice.utils;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.LogEventBuilder;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "my-very-secret-key-for-jwt-signing";
    private final long accessTokenExpirationMs = 1000 * 60 * 10; // 10 minutes
    private final long refreshTokenExpirationMs = 1000 * 60 * 60 * 24; // 24 hours

    private UUID userId;

    @Mock
    private UnifiedLoggerFactory loggerFactory; // Mock the logger factory

    @Mock
    private IUnifiedLogger logger;

    @Mock
    private LogEventBuilder logEventBuilder;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Mock the factory to return our mocked logger - using the exact call the service makes
        when(loggerFactory.getLogger("JwtUtil", "jwt-util")).thenReturn(logger);
        
        // Mock logger methods that might be called
        when(logger.trace()).thenReturn(logEventBuilder);
        when(logger.info()).thenReturn(logEventBuilder);
        when(logger.debug()).thenReturn(logEventBuilder);
        when(logger.warn()).thenReturn(logEventBuilder);
        when(logger.error(any(Exception.class))).thenReturn(logEventBuilder);
        
        // Mock LogEventBuilder fluent methods
        when(logEventBuilder.message(any(String.class))).thenReturn(logEventBuilder);
        when(logEventBuilder.detail(any(String.class), any())).thenReturn(logEventBuilder);
        when(logEventBuilder.category(any(String.class))).thenReturn(logEventBuilder);
        
        // Use the new constructor that accepts the logger factory
        jwtUtil = new JwtUtil(loggerFactory);

        // Inject properties manually (no Spring context)
        var secretField = JwtUtil.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(jwtUtil, secret);

        var jwtExpField = JwtUtil.class.getDeclaredField("jwtExpirationInMs");
        jwtExpField.setAccessible(true);
        jwtExpField.setLong(jwtUtil, accessTokenExpirationMs);

        var refreshExpField = JwtUtil.class.getDeclaredField("refreshExpirationInMs");
        refreshExpField.setAccessible(true);
        refreshExpField.setLong(jwtUtil, refreshTokenExpirationMs);

        userId = UUID.randomUUID();
    }

    /**
     * Tests generating a JWT access token and verifies its claims.
     */
    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtUtil.generateToken(userId);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        assertEquals(userId.toString(), jwtUtil.extractUserId(token));
    }

    /**
     * Tests generating a JWT refresh token and verifies its claims.
     */
    @Test
    void generateRefreshToken_shouldCreateValidRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());

        assertEquals(userId.toString(), jwtUtil.extractUserId(refreshToken));
    }

    /**
     * Validates that a valid token passes validation.
     */
    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken(userId);
        assertTrue(jwtUtil.validateToken(token, userId.toString()));
    }

    /**
     * Validates that token validation fails for a mismatched user ID.
     */
    @Test
    void validateToken_shouldReturnFalseForWrongUserId() {
        String token = jwtUtil.generateToken(userId);
        assertFalse(jwtUtil.validateToken(token, UUID.randomUUID().toString()));
    }

    /**
     * Validates that an expired token fails validation.
     */
    @Test
    void validateToken_shouldReturnFalseForExpiredToken() throws Exception {
        String expiredToken = Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // expired
                .signWith(getSigningKey())
                .compact();

        assertFalse(jwtUtil.validateToken(expiredToken, userId.toString()));
    }

    /**
     * Validates that a valid refresh token passes validation.
     */
    @Test
    void validateRefreshToken_shouldReturnTrueForValidRefreshToken() {
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        assertTrue(jwtUtil.validateRefreshToken(refreshToken, userId.toString()));
    }

    /**
     * Validates that refresh token fails if 'type' claim is not 'refresh'.
     */
    @Test
    void validateRefreshToken_shouldReturnFalseIfTypeIsNotRefresh() throws Exception {
        String token = Jwts.builder()
                .setSubject(userId.toString())
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(getSigningKey())
                .compact();

        assertFalse(jwtUtil.validateRefreshToken(token, userId.toString()));
    }

    /**
     * Validates that an expired refresh token fails validation.
     */
    @Test
    void validateRefreshToken_shouldReturnFalseForExpiredRefreshToken() throws Exception {
        String expiredRefreshToken = Jwts.builder()
                .setSubject(userId.toString())
                .claim("type", "refresh")
                .setIssuedAt(new Date(System.currentTimeMillis() - 2000))
                .setExpiration(new Date(System.currentTimeMillis() - 1000)) // expired
                .signWith(getSigningKey())
                .compact();

        assertFalse(jwtUtil.validateRefreshToken(expiredRefreshToken, userId.toString()));
    }

    /**
     * Validates that refresh token validation fails for a mismatched user ID.
     */
    @Test
    void validateRefreshToken_shouldReturnFalseForWrongUserId() {
        String refreshToken = jwtUtil.generateRefreshToken(userId);
        assertFalse(jwtUtil.validateRefreshToken(refreshToken, UUID.randomUUID().toString()));
    }

    /**
     * Validates that malformed tokens fail access token validation gracefully.
     */
    @Test
    void validateToken_shouldReturnFalseForMalformedToken() {
        assertFalse(jwtUtil.validateToken("not.a.token", userId.toString()));
    }

    /**
     * Validates that malformed tokens fail refresh token validation gracefully.
     */
    @Test
    void validateRefreshToken_shouldReturnFalseForMalformedToken() {
        assertFalse(jwtUtil.validateRefreshToken("not.a.token", userId.toString()));
    }

    // Helper to get signing key matching JwtUtil logic
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, SignatureAlgorithm.HS256.getJcaName());
    }
}