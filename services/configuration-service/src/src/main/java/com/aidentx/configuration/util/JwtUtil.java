package com.aidentx.configuration.util;

import com.aidentx.configuration.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT utility class for parsing and validating JWT tokens in the API Gateway.
 * This class provides methods to extract claims and validate tokens.
 */
@Component
public class JwtUtil {

    private final IUnifiedLogger logger;

    @Value("${jwt.secret}")
    private String jwtSecret;

    public JwtUtil(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger("JwtUtil", AppConstants.LOG_CATEGORY_JWT_UTIL);
    }

    /**
     * Extracts the username from the JWT token.
     *
     * @param token the JWT token
     * @return the username extracted from the token
     */
    public String extractUsername(String token) {
        Claims claims = extractClaims(token);
        String username = claims.get("username", String.class);
        return username != null ? username : claims.getSubject();
    }

    /**
     * Extracts the user ID from the JWT token.
     *
     * @param token the JWT token
     * @return the user ID extracted from the token
     */
    public String extractUserId(String token) {
        Claims claims = extractClaims(token);
        String userId = claims.get("sub", String.class);
        return userId != null ? userId : claims.getSubject();
    }

    /**
     * Extracts the tenant ID from the JWT token.
     *
     * @param token the JWT token
     * @return the tenant ID extracted from the token, or null if not present
     */
    public String extractTenantId(String token) {
        try {
            Claims claims = extractClaims(token);
            String tenantId = claims.get("tenantId", String.class);
            if (tenantId != null) {
                logger.debug().message("Tenant ID extracted from JWT token").detail("tenantId", tenantId).log();
            } else {
                logger.debug().message("No tenant ID found in JWT token").log();
            }
            return tenantId;
        } catch (Exception e) {
            logger.warn().message("Failed to extract tenant ID from JWT token").detail("error", e.getMessage()).log();
            return null;
        }
    }

    /**
     * Extracts the expiration date from the JWT token.
     *
     * @param token the JWT token
     * @return the expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    /**
     * Extracts all claims from the JWT token.
     *
     * @param token the JWT token
     * @return the claims extracted from the token
     */
    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_CLAIMS_EXTRACTION_FAILURE).log();
            throw e;
        }
    }

    /**
     * Checks if the JWT token is expired.
     *
     * @param token the JWT token
     * @return true if the token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validates the JWT token.
     *
     * @param token the JWT token
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            boolean isExpired = isTokenExpired(token);
            if (!isExpired) {
                logger.trace().message(AppConstants.LOG_TOKEN_VALIDATION_SUCCESS).log();
            }
            return !isExpired;
        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_TOKEN_VALIDATION_FAILURE + "" + e.getMessage()).log();
            return false;
        }
    }

    /**
     * Gets the signing key for JWT token validation.
     *
     * @return the signing key
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}