package com.aidentx.authservice.utils;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Utility class for generating and validating JSON Web Tokens (JWT).
 */
@Component
public class JwtUtil {

    // ===================================================================================
    // Constants
    // ===================================================================================

    // --- Logger Configuration ---
    private static final String LOGGER_NAME = "JwtUtil";
    private static final String LOG_CATEGORY_UTIL = "jwt-util";
    private static final String LOG_CATEGORY_JWT = "jwt";

    // --- Log Detail Keys ---
    private static final String DETAIL_USER_ID = "userId";

    // ===================================================================================
    // Member Variables
    // ===================================================================================

    private final IUnifiedLogger logger;
    /**
     * Secret key used to sign and verify JWT tokens.
     * Injected from the application.properties.
     */
    @Value("${jwt.secret}")
    private String secret;
    /**
     * Access token expiration in milliseconds.
     * Injected from the application.properties.
     */
    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;
    /**
     * Refresh token expiration in milliseconds.
     * Injected from the application.properties.
     */
    @Value("${security.jwt.refresh-token.expiration-ms}")
    private long refreshExpirationInMs;

    public JwtUtil(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.logger = unifiedLoggerFactory.getLogger(LOGGER_NAME, LOG_CATEGORY_UTIL);
    }

    /**
     * Generates a JWT access token for a given user ID and username.
     *
     * @param userId   the UUID of the user
     * @param username the username of the user
     * @param tenantId the tenant ID that the user belongs to
     * @return a signed JWT access token as a String
     */
    public String generateToken(UUID userId, String username, String tenantId, UUID identityId) {
        logger.trace().category(LOG_CATEGORY_JWT).message(AppConstants.JWT_LOG_GENERATING_ACCESS_TOKEN).detail(DETAIL_USER_ID, userId.toString()).log();
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", username); // Add username to claims
            claims.put("tenantId", tenantId); // Add tenantId to claims
            // Add identityId only if it's not null (supports legacy users without identityId)
            if (identityId != null) {
                claims.put("identityId", identityId.toString());
            }

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId.toString())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
            logger.info().message(String.format(AppConstants.JWT_LOG_SUCCESS_GEN_TOKEN_FORMAT, AppConstants.JWT_TOKEN_TYPE_ACCESS, userId)).log();
            return token;
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.JWT_LOG_ERROR_GEN_TOKEN_FORMAT, AppConstants.JWT_TOKEN_TYPE_ACCESS, userId)).log();
            throw e;
        }
    }

    /**
     * Generates a JWT access token with custom expiration time.
     *
     * @param userId       the UUID of the user
     * @param username     the username of the user
     * @param tenantId     the tenant ID that the user belongs to
     * @param expirationMs the custom expiration time in milliseconds
     * @return a signed JWT access token as a String
     */
    public String generateTokenWithCustomExpiration(UUID userId, String username, String tenantId, UUID identityId, long expirationMs) {
        logger.trace().category(LOG_CATEGORY_JWT).message(AppConstants.JWT_LOG_GENERATING_ACCESS_TOKEN_CUSTOM)
                .detail(DETAIL_USER_ID, userId.toString())
                .detail("customExpirationMs", expirationMs)
                .log();
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("username", username); // Add username to claims
            claims.put("tenantId", tenantId); // Add tenantId to claims

            // Add identityId only if it's not null (supports legacy users without identityId)
            if (identityId != null) {
                claims.put("identityId", identityId.toString());
            }

            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId.toString())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
            logger.info().message(String.format(AppConstants.JWT_LOG_SUCCESS_GEN_TOKEN_FORMAT + " with custom expiration", AppConstants.JWT_TOKEN_TYPE_ACCESS, userId)).log();
            return token;
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.JWT_LOG_ERROR_GEN_TOKEN_FORMAT + " with custom expiration", AppConstants.JWT_TOKEN_TYPE_ACCESS, userId)).log();
            throw e;
        }
    }

    /**
     * Generates a JWT refresh token for a given user ID.
     * Refresh token usually has longer expiration time.
     *
     * @param userId the UUID of the user
     * @return a signed JWT refresh token as a String
     */
    public String generateRefreshToken(UUID userId) {
        logger.trace().category(LOG_CATEGORY_JWT).message(AppConstants.JWT_LOG_GENERATING_REFRESH_TOKEN)
                .detail(DETAIL_USER_ID, userId.toString()).log();
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put(AppConstants.JWT_CLAIM_KEY_TYPE, AppConstants.JWT_CLAIM_VALUE_REFRESH);
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId.toString())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + refreshExpirationInMs))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
            logger.info().message(String.format(AppConstants.JWT_LOG_SUCCESS_GEN_TOKEN_FORMAT, AppConstants.JWT_TOKEN_TYPE_REFRESH, userId)).log();
            return token;
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.JWT_LOG_ERROR_GEN_TOKEN_FORMAT, AppConstants.JWT_TOKEN_TYPE_REFRESH, userId)).log();
            throw e;
        }
    }

    /**
     * Generates a JWT refresh token with custom expiration time.
     *
     * @param userId       the UUID of the user
     * @param expirationMs the custom expiration time in milliseconds
     * @return a signed JWT refresh token as a String
     */
    public String generateRefreshTokenWithCustomExpiration(UUID userId, long expirationMs) {
        logger.trace().category(LOG_CATEGORY_JWT).message(AppConstants.JWT_LOG_GENERATING_REFRESH_TOKEN_CUSTOM)
                .detail(DETAIL_USER_ID, userId.toString())
                .detail("customExpirationMs", expirationMs)
                .log();
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put(AppConstants.JWT_CLAIM_KEY_TYPE, AppConstants.JWT_CLAIM_VALUE_REFRESH);
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId.toString())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
            logger.info().message(String.format(AppConstants.JWT_LOG_SUCCESS_GEN_TOKEN_FORMAT + " with custom expiration", AppConstants.JWT_TOKEN_TYPE_REFRESH, userId)).log();
            return token;
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.JWT_LOG_ERROR_GEN_TOKEN_FORMAT + " with custom expiration", AppConstants.JWT_TOKEN_TYPE_REFRESH, userId)).log();
            throw e;
        }
    }

    /**
     * Generates a JWT token for password reset containing the userId as subject.
     * The token expires in 24 hours.
     *
     * @param userId the UUID of the user
     * @return a signed JWT password reset token as a String
     */
    public String generatePasswordResetToken(UUID userId) {
        logger.trace().category(LOG_CATEGORY_JWT).message(AppConstants.JWT_LOG_GENERATING_PWD_RESET_TOKEN)
                .detail(DETAIL_USER_ID, userId.toString())
                .log();
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put(AppConstants.JWT_CLAIM_KEY_TYPE, AppConstants.JWT_CLAIM_VALUE_PASSWORD_RESET);
            long expiryMs = 24 * 60 * 60 * 1000L; // 24 hours
            String token = Jwts.builder()
                    .setClaims(claims)
                    .setSubject(userId.toString())
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
                    .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                    .compact();
            logger.info().message(String.format(AppConstants.JWT_LOG_SUCCESS_GEN_TOKEN_FORMAT, AppConstants.JWT_TOKEN_TYPE_PWD_RESET, userId)).log();
            return token;
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.JWT_LOG_ERROR_GEN_TOKEN_FORMAT, AppConstants.JWT_TOKEN_TYPE_PWD_RESET, userId)).log();
            throw e;
        }
    }

    /**
     * Extracts all claims (data) from a JWT token.
     *
     * @param token the JWT token to parse
     * @return a {@link Claims} object containing the token's data
     */
    private Claims extractAllClaims(String token) {
        logger.trace().category(LOG_CATEGORY_JWT).message(AppConstants.JWT_LOG_EXTRACTING_ALL_CLAIMS).log();
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            logger.trace().category(LOG_CATEGORY_JWT).message(AppConstants.JWT_LOG_SUCCESS_EXTRACTED_CLAIMS).log();
            return claims;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.JWT_LOG_ERROR_EXTRACTING_CLAIMS).log();
            throw e;
        }
    }

    /**
     * Validates a JWT token.
     * 
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (SignatureException e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_INVALID_SIGNATURE, e.getMessage())).log();
        } catch (MalformedJwtException e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_MALFORMED_TOKEN, e.getMessage())).log();
        } catch (ExpiredJwtException e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_TOKEN_EXPIRED, e.getMessage())).log();
        } catch (UnsupportedJwtException e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_UNSUPPORTED_TOKEN, e.getMessage())).log();
        } catch (IllegalArgumentException e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_EMPTY_CLAIMS, e.getMessage())).log();
        }
        return false;
    }

    /**
     * Validates a JWT refresh token by checking if it matches the provided user ID,
     * is not expired, and contains the 'refresh' type claim.
     *
     * @param token  the JWT refresh token to validate
     * @param userId the expected user ID to match with the token's subject
     * @return true if the refresh token is valid, false otherwise
     */
    public boolean validateRefreshToken(String token, String userId) {
        logger.trace().category(LOG_CATEGORY_JWT).message(AppConstants.JWT_LOG_VALIDATING_REFRESH_TOKEN).detail(DETAIL_USER_ID, userId)
                .log();
        try {
            final String extractedUserId = extractUserId(token);
            final Claims claims = extractAllClaims(token);
            String type = (String) claims.get(AppConstants.JWT_CLAIM_KEY_TYPE);
            boolean valid = (extractedUserId.equals(userId) && !isTokenExpired(token)
                    && AppConstants.JWT_CLAIM_VALUE_REFRESH.equals(type));
            if (valid) {
                logger.info().message(String.format(AppConstants.JWT_LOG_SUCCESS_VALIDATE_TOKEN_FORMAT, "Refresh", userId)).log();
            } else {
                logger.warn().message(String.format(AppConstants.JWT_LOG_WARN_VALIDATE_TOKEN_FAILED_FORMAT, "Refresh", userId)).log();
            }
            return valid;
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.JWT_LOG_ERROR_VALIDATE_TOKEN_FORMAT, AppConstants.JWT_TOKEN_TYPE_REFRESH, userId)).log();
            return false;
        }
    }

    /**
     * Validates a password reset JWT token by checking userId, expiry, and type
     * claim.
     *
     * @param token  the JWT token to validate
     * @param userId the expected user ID
     * @return true if valid, false otherwise
     */
    public boolean validatePasswordResetToken(String token, String userId) {
        logger.trace().category(LOG_CATEGORY_JWT).message(AppConstants.JWT_LOG_VALIDATING_PWD_RESET_TOKEN).detail(DETAIL_USER_ID, userId)
                .log();
        try {
            final String extractedUserId = extractUserId(token);
            final Claims claims = extractAllClaims(token);
            String type = (String) claims.get(AppConstants.JWT_CLAIM_KEY_TYPE);
            return (extractedUserId.equals(userId) && !isTokenExpired(token)
                    && AppConstants.JWT_CLAIM_VALUE_PASSWORD_RESET.equals(type));
        } catch (Exception e) {
            logger.error(e).message(String.format(AppConstants.JWT_LOG_ERROR_VALIDATE_TOKEN_FORMAT, AppConstants.JWT_TOKEN_TYPE_PWD_RESET, userId)).log();
            return false;
        }
    }

    /**
     * Extracts the userId (subject) from a password reset JWT token.
     *
     * @param token the JWT token
     * @return the userId as a String
     */
    public String extractUserIdFromPasswordResetToken(String token) {
        return extractUserId(token);
    }

    /**
     * Extracts the user ID from a JWT token's 'sub' (subject) claim.
     * 
     * @param token the JWT token
     * @return the user ID from the token's subject claim, or null if an error
     *         occurs.
     */
    public String extractUserId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();
        } catch (SignatureException e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_INVALID_SIGNATURE, e.getMessage())).log();
            return null;
        } catch (Exception e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_EXTRACTING_USER_ID, e.getMessage())).log();
            return null;
        }
    }


    /**
     * Extracts the identityId from a JWT token's 'identityId' claim.
     *
     * @param token the JWT token
     * @return the identity ID from the token's subject claim, or null if an error
     *         occurs.
     */
    public String extractIdentityId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("identityId", String.class);
        } catch (Exception e) {
            logger.error().message("Error extracting identityId").log();
            return null;
        }
    }


    /**
     * Extracts the username from a JWT token's 'username' claim.
     * 
     * @param token the JWT token
     * @return the username from the token's claim, or null if an error occurs.
     */
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.get("username", String.class);
        } catch (Exception e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_EXTRACTING_USERNAME, e.getMessage())).log();
            return null;
        }
    }

    /**
     * Extracts the tenantId from a JWT token's 'tenantId' claim.
     * 
     * @param token the JWT token
     * @return the tenantId from the token's claim, or "unknown" if an error occurs.
     */
    public String extractTenantId(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            String tenantId = claims.get("tenantId", String.class);
            return tenantId != null ? tenantId : com.aidentx.authservice.service.Auth.AppConstants.UNKNOWN_TENANT_ID;
        } catch (Exception e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_EXTRACTING_TENANT_ID, e.getMessage())).log();
            return com.aidentx.authservice.service.Auth.AppConstants.UNKNOWN_TENANT_ID;
        }
    }

    /**
     * Checks if a JWT token is expired.
     * 
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        Date expiration = extractExpiration(token);
        if (expiration == null) {
            // If expiration cannot be extracted, treat token as not expired (or handle as needed)
            return false;
        }
        return expiration.before(new Date());
    }

    /**
     * Extracts the expiration date from a JWT token.
     * 
     * @param token the JWT token
     * @return the expiration date
     */
    public Date extractExpiration(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration();
        } catch (Exception e) {
            logger.error().message(String.format(AppConstants.JWT_ERROR_EXTRACTING_EXPIRATION, e.getMessage()))
                    .log();
            return null;
        }
    }

    /**
     * Returns the JWT access token expiration duration in seconds.
     *
     * @return the access token expiration time in seconds
     */
    public long getExpiration() {
        return jwtExpirationInMs / 1000;
    }

    /**
     * Returns the JWT refresh token expiration duration in seconds.
     *
     * @return the refresh token expiration time in seconds
     */
    public long getRefreshExpiration() {
        return refreshExpirationInMs / 1000;
    }

    /**
     * Gets the signing key for JWT operations.
     * 
     * @return the secret key for signing/verifying JWTs
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}