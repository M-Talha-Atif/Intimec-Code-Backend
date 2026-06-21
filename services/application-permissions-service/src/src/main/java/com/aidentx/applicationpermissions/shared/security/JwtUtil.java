package com.aidentx.applicationpermissions.shared.security;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Shared JWT Utility class for handling JWT token operations.
 * This component provides token validation, parsing, and extraction methods
 * used across application-permissions-service.
 *
 * Production-ready with proper error handling, logging, and security best
 * practices.
 */
@Component
public class JwtUtil {

    private final IUnifiedLogger logger;
    private final JwtProperties jwtProperties;

    public JwtUtil(JwtProperties jwtProperties, UnifiedLoggerFactory loggerFactory) {
        this.jwtProperties = jwtProperties;
        this.logger = loggerFactory.getLogger("JwtUtil", SecurityConstants.LOG_CATEGORY_SECURITY_JWT);
    }

    /**
     * Extracts username from JWT token.
     * Gets the username from the "username" claim, not from the subject.
     *
     * @param token the JWT token
     * @return the username extracted from the token
     * @throws RuntimeException if token parsing fails
     */
    public String extractUsername(String token) {
        try {
            // Extract username from the "username" claim, not from subject
            return extractClaim(token, claims -> claims.get("username", String.class));
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_EXTRACT_USERNAME).log();
            throw new RuntimeException(AppConstants.MSG_INVALID_TOKEN_USERNAME, e);
        }
    }

    /**
     * Extracts user ID from JWT token.
     * First tries to get from "userId" claim, then falls back to "sub" claim.
     * 
     * @param token the JWT token
     * @return the user ID extracted from the token
     * @throws RuntimeException if token parsing fails
     */
    public String extractUserId(String token) {
        try {
            // First try to get from "userId" claim
            String userId = extractClaim(token, claims -> claims.get("userId", String.class));
            if (userId != null && !userId.trim().isEmpty()) {
                return userId;
            }

            // Fall back to "sub" (subject) claim
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            logger.error(e).message(AppConstants.FAILED_TO_EXTRACT_USER_ID).log();
            throw new RuntimeException(AppConstants.INVALID_TOKEN_CANNOT_EXTRACT_USER_ID, e);
        }
    }

    /**
     * Extracts tenant ID from JWT token.
     * Gets the tenant ID from the "tenantId" claim.
     * 
     * @param token the JWT token
     * @return the tenant ID extracted from the token
     * @throws RuntimeException if token parsing fails or tenantId is not found
     */
    public String extractTenantId(String token) {
        try {
            String tenantId = extractClaim(token, claims -> claims.get("tenantId", String.class));
            if (tenantId == null || tenantId.trim().isEmpty()) {
                logger.error().message(AppConstants.TENANT_ID_MISSING).log();
                throw new RuntimeException(AppConstants.TENANT_ID_MISSING);
            }
            return tenantId;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.TENANT_EXTRACTION_FAILED).log();
            throw new RuntimeException(AppConstants.TENANT_EXTRACTION_FAILED, e);
        }
    }

    /**
     * Extracts expiration date from JWT token.
     * 
     * @param token the JWT token
     * @return the expiration date
     * @throws RuntimeException if token parsing fails
     */
    public Date extractExpiration(String token) {
        try {
            return extractClaim(token, Claims::getExpiration);
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_EXTRACT_EXPIRATION).log();
            throw new RuntimeException(AppConstants.MSG_INVALID_TOKEN_EXPIRATION, e);
        }
    }

    /**
     * Extracts a specific claim from JWT token.
     *
     * @param token          the JWT token
     * @param claimsResolver function to extract the specific claim
     * @param <T>            the type of the claim
     * @return the extracted claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all claims from JWT token.
     *
     * @param token the JWT token
     * @return all claims from the token
     * @throws RuntimeException if token parsing fails
     */
    private Claims extractAllClaims(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
            logger.debug().message(AppConstants.LOG_JWT_SECRET_USED).detail("secretLength", jwtProperties.getSecret().length()).log();
            return Jwts.parserBuilder().setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_PARSE_TOKEN).detail("error", e.getMessage()).log();
            throw new RuntimeException(AppConstants.MSG_INVALID_TOKEN_PARSING, e);
        }
    }

    /**
     * Checks if the token is expired.
     *
     * @param token the JWT token
     * @return true if token is expired, false otherwise
     */
    public Boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_TOKEN_EXPIRATION_CHECK_FAILED + ' ' + e).log();
            return true;
        }
    }

    /**
     * Validates the JWT token.
     * Checks if token is properly formatted, not expired, and has valid signature.
     *
     * @param token the JWT token to validate
     * @return true if token is valid, false otherwise
     */
    public Boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                logger.debug().message(AppConstants.LOG_TOKEN_VALIDATION_NULL_EMPTY).log();
                return false;
            }

            // This will throw an exception if token is invalid
            Claims claims = extractAllClaims(token);
            logger.debug().message(AppConstants.LOG_JWT_CLAIMS_EXTRACTED).detail("subject", claims.getSubject()).detail("expiration", claims.getExpiration())
                    .log();

            // Check if token is expired
            if (isTokenExpired(token)) {
                logger.debug().message(AppConstants.LOG_TOKEN_VALIDATION_EXPIRED).log();
                return false;
            }

            logger.trace().message(AppConstants.LOG_TOKEN_VALIDATION_SUCCESS).log();
            return true;

        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_TOKEN_VALIDATION_FAILED).detail("reason", e.getMessage()).log();
            return false;
        }
    }

    /**
     * Validates the JWT token for a specific username.
     *
     * @param token    the JWT token
     * @param username the expected username
     * @return true if token is valid and belongs to the specified user
     */
    public Boolean validateToken(String token, String username) {
        try {
            if (!validateToken(token)) {
                return false;
            }

            String tokenUsername = extractUsername(token);
            boolean isValid = username.equals(tokenUsername);

            if (!isValid) {
                logger.debug().message(AppConstants.LOG_TOKEN_USERNAME_MISMATCH).detail("expected", username).detail("found", tokenUsername).log();
            }

            return isValid;

        } catch (Exception e) {
            logger.debug().message(AppConstants.LOG_TOKEN_VALIDATION_USER_FAILED).detail("username", username).detail("reason", e.getMessage()).log();
            return false;
        }
    }
}