package com.aidentx.iam.security;

import com.aidentx.iam.config.AppConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * JWT Utility class for handling JWT token operations in the identities
 * service.
 * 
 * <p>
 * This component provides comprehensive JWT token management capabilities
 * including:
 * </p>
 * <ul>
 * <li>Token validation and parsing with robust error handling</li>
 * <li>Claims extraction for username, user ID, tenant ID, and roles</li>
 * <li>Token expiration verification and security validation</li>
 * <li>Support for multiple claim formats and fallback mechanisms</li>
 * </ul>
 * 
 * <p>
 * The utility supports both standard JWT claims (sub, exp) and custom claims
 * (username, userId, tenantId, roles) to accommodate various authentication
 * scenarios.
 * All operations include comprehensive logging for security auditing and
 * debugging.
 * </p>
 *
 * @author aidentx Development Team
 * @since 1.0
 * @see JwtProperties
 * @see SecurityConstants
 */
@Component
public class JwtUtil {

    private final IUnifiedLogger logger;
    private final JwtProperties jwtProperties;

    /**
     * Constructs a new JwtUtil with the specified JWT properties.
     * 
     * @param jwtProperties        the JWT configuration properties containing
     *                             secret key and other settings
     * @param unifiedLoggerFactory the unified logger factory
     */
    public JwtUtil(JwtProperties jwtProperties, UnifiedLoggerFactory unifiedLoggerFactory) {
        this.jwtProperties = jwtProperties;
        this.logger = unifiedLoggerFactory.getLogger(JwtUtil.class);
        logger.debug().message(AppConstants.LOG_DEBUG_CONFIGURATION_VALUE + ": JWT properties configured").log();
    }

    /**
     * Extracts username from JWT token.
     * 
     * <p>
     * Retrieves the username from the "username" claim rather than the standard
     * "sub" claim
     * to support custom authentication schemes. This method provides robust error
     * handling
     * and comprehensive logging for security auditing.
     * </p>
     * 
     * @param token the JWT token to extract username from
     * @return the username extracted from the token
     * @throws RuntimeException if token parsing fails or username cannot be
     *                          extracted
     */
    public String extractUsername(String token) {
        logger.debug().message(AppConstants.LOG_JWT_USERNAME_EXTRACTION_STARTED).log();
        ;
        try {
            // Extract username from the "username" claim, not from subject
            String username = extractClaim(token, claims -> claims.get("username", String.class));
            logger.debug()
                    .message(String.format(AppConstants.LOG_JWT_USERNAME_EXTRACTION_SUCCESSFUL + ": {}", username))
                    .log();
            return username;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_USERNAME_EXTRACTION_FAILED).log();
            throw new RuntimeException(AppConstants.EXCEPTION_JWT_USERNAME_EXTRACTION_FAILED, e);
        }
    }

    /**
     * Extracts user ID from JWT token.
     * <p>
     * Implements a fallback mechanism for user ID extraction:
     * </p>
     * <ol>
     * <li>First attempts to extract from custom "userId" claim</li>
     * <li>Falls back to standard "sub" (subject) claim if userId is not
     * available</li>
     * </ol>
     * <p>
     * This dual approach ensures compatibility with various JWT token formats
     * and authentication providers while maintaining security standards.
     * </p>
     * 
     * @param token the JWT token to extract user ID from
     * @return the user ID extracted from the token
     * @throws RuntimeException if token parsing fails or user ID cannot be
     *                          extracted
     */
    public String extractUserId(String token) {
        logger.debug().message(AppConstants.LOG_JWT_USERID_EXTRACTION_STARTED).log();
        try {
            // First try to get from "userId" claim
            String userId = extractClaim(token, claims -> claims.get("userId", String.class));
            if (userId != null && !userId.trim().isEmpty()) {
                logger.debug()
                        .message(String.format(AppConstants.LOG_JWT_USERID_EXTRACTION_SUCCESSFUL + ": {}", userId))
                        .log();
                return userId;
            }

            // Fall back to "sub" (subject) claim
            logger.debug().message(AppConstants.LOG_JWT_USERID_FALLBACK_TO_SUBJECT).log();
            userId = extractClaim(token, Claims::getSubject);
            logger.debug().message(String.format(AppConstants.LOG_JWT_USERID_EXTRACTION_SUCCESSFUL + ": {}", userId))
                    .log();
            return userId;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_USERID_EXTRACTION_FAILED).log();
            throw new RuntimeException(AppConstants.EXCEPTION_JWT_USERID_EXTRACTION_FAILED, e);
        }
    }

    /**
     * Extracts expiration date from JWT token.
     * 
     * <p>
     * Retrieves the standard "exp" claim from the JWT token to determine
     * when the token expires. This is critical for security validation
     * and session management.
     * </p>
     * 
     * @param token the JWT token to extract expiration from
     * @return the expiration date of the token
     * @throws RuntimeException if token parsing fails or expiration cannot be
     *                          extracted
     */
    public Date extractExpiration(String token) {
        logger.debug().message(AppConstants.LOG_JWT_EXPIRATION_EXTRACTION_STARTED).log();
        try {
            Date expiration = extractClaim(token, Claims::getExpiration);
            logger.debug()
                    .message(String.format(AppConstants.LOG_JWT_EXPIRATION_EXTRACTION_SUCCESSFUL + ": {}", expiration))
                    .log();
            return expiration;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_EXPIRATION_EXTRACTION_FAILED).log();
            throw new RuntimeException(AppConstants.EXCEPTION_JWT_EXPIRATION_EXTRACTION_FAILED, e);
        }
    }

    /**
     * Extracts a specific claim from JWT token using a claims resolver function.
     * 
     * <p>
     * This is a generic method that allows extraction of any claim from the JWT
     * token
     * using a functional approach. It provides a flexible foundation for all other
     * claim extraction methods in this utility class.
     * </p>
     * 
     * @param token          the JWT token to extract claims from
     * @param claimsResolver function to extract the specific claim from the claims
     *                       object
     * @param <T>            the type of the claim to be extracted
     * @return the extracted claim of the specified type
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_ENTRY + ": extractClaim").log();
        final Claims claims = extractAllClaims(token);
        T result = claimsResolver.apply(claims);
        logger.debug().message(AppConstants.LOG_DEBUG_METHOD_EXIT + ": extractClaim").log();
        return result;
    }

    /**
     * Extracts all claims from JWT token.
     * 
     * <p>
     * Parses and validates the JWT token using the configured secret key,
     * then extracts all claims from the token body. This method forms the
     * foundation for all claim extraction operations and includes comprehensive
     * security validation.
     * </p>
     * 
     * <p>
     * The method uses HMAC-SHA algorithm for signature verification and
     * ensures the token structure and signature are valid before extracting claims.
     * </p>
     * 
     * @param token the JWT token to parse and extract claims from
     * @return all claims contained in the token
     * @throws RuntimeException if token parsing fails, signature verification
     *                          fails, or token is malformed
     */
    private Claims extractAllClaims(String token) {
        logger.debug().message(AppConstants.LOG_JWT_CLAIMS_PARSING_STARTED).log();
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            logger.debug().message(AppConstants.LOG_JWT_CLAIMS_PARSING_SUCCESSFUL).log();
            return claims;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_CLAIMS_PARSING_FAILED).log();
            throw new RuntimeException(AppConstants.EXCEPTION_JWT_TOKEN_PARSING_FAILED, e);
        }
    }

    /**
     * Checks if JWT token is expired.
     * 
     * <p>
     * Compares the token's expiration date with the current system time
     * to determine if the token is still valid. Uses defensive programming
     * by considering any token with extraction errors as expired for security.
     * </p>
     * 
     * @param token the JWT token to check for expiration
     * @return true if token is expired or cannot be verified, false if token is
     *         still valid
     */
    public Boolean isTokenExpired(String token) {
        logger.debug().message(AppConstants.LOG_JWT_EXPIRATION_CHECK_STARTED).log();
        try {
            boolean expired = extractExpiration(token).before(new Date());
            logger.debug().message(String.format(AppConstants.LOG_JWT_EXPIRATION_CHECK_SUCCESSFUL + ": {}", expired)).log();
            return expired;
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_EXPIRATION_CHECK_FAILED).log();
            return true; // Consider expired if we can't determine
        }
    }

    /**
     * Validates JWT token comprehensively.
     * 
     * <p>
     * Performs complete JWT token validation including:
     * </p>
     * <ul>
     * <li>Null and empty string validation</li>
     * <li>Token structure and signature verification</li>
     * <li>Claims parsing and extraction</li>
     * <li>Expiration date verification</li>
     * </ul>
     * 
     * <p>
     * This method provides the primary security gate for JWT authentication
     * and includes comprehensive logging for security auditing purposes.
     * </p>
     * 
     * @param token the JWT token to validate
     * @return true if token is valid and not expired, false otherwise
     */
    public Boolean validateToken(String token) {
        logger.debug().message(AppConstants.LOG_JWT_TOKEN_VALIDATION_STARTED).log();
        try {
            if (token == null || token.trim().isEmpty()) {
                logger.warn().message(AppConstants.LOG_JWT_TOKEN_NULL_OR_EMPTY).log();
                return false;
            }

            // Parse and validate the token
            extractAllClaims(token);

            // Check if token is expired
            if (isTokenExpired(token)) {
                logger.warn().message(AppConstants.LOG_JWT_TOKEN_EXPIRED).log();
                return false;
            }

            logger.debug().message(AppConstants.LOG_JWT_TOKEN_VALIDATION_SUCCESSFUL).log();
            return true;

        } catch (Exception e) {
            logger.error().message(AppConstants.LOG_JWT_TOKEN_VALIDATION_FAILED).log();
            return false;
        }
    }

    /**
     * Extracts tenant ID from JWT token.
     * 
     * <p>
     * Retrieves the tenant ID from the custom "tenantId" claim to support
     * multi-tenant applications. This method is designed to be resilient,
     * returning null if the tenant ID is not present rather than failing.
     * </p>
     * 
     * @param token the JWT token to extract tenant ID from
     * @return the tenant ID extracted from the token, or null if not present or
     *         extraction fails
     */
    public String extractTenantId(String token) {
        logger.debug().message(AppConstants.LOG_JWT_TENANT_EXTRACTION_STARTED).log();
        try {
            String tenantId = extractClaim(token, claims -> claims.get("tenantId", String.class));
            logger.debug().message(String.format(AppConstants.LOG_JWT_TENANT_EXTRACTION_SUCCESSFUL + ": {}", tenantId))
                    .log();
            return tenantId;
        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_JWT_TENANT_EXTRACTION_FAILED).log();
            return null;
        }
    }

    /**
     * Extracts roles from JWT token.
     * 
     * <p>
     * Retrieves the roles from the custom "roles" claim to support
     * role-based access control (RBAC). This method is designed to be resilient,
     * returning null if roles are not present rather than failing.
     * </p>
     * 
     * @param token the JWT token to extract roles from
     * @return the roles extracted from the token, or null if not present or
     *         extraction fails
     */
    public String extractRoles(String token) {
        logger.debug().message(AppConstants.LOG_JWT_ROLES_EXTRACTION_STARTED).log();
        try {
            String roles = extractClaim(token, claims -> claims.get("roles", String.class));
            logger.debug().message(String.format(AppConstants.LOG_JWT_ROLES_EXTRACTION_SUCCESSFUL + ": {}", roles)).log();
            return roles;
        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_JWT_ROLES_EXTRACTION_FAILED).log();
            return null;
        }
    }
}
