package com.aidentx.workflowsservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * Utility class for extracting information from JWT tokens.
 * Used to extract tenantId and userId from request headers.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Component
public class JwtUtils {

    private final IUnifiedLogger logger;
    
    @Value("${jwt.secret:mySuperSecretKey12345678901234AZERTY123456789}")
    private String jwtSecret;

    public JwtUtils(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(JwtUtils.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Extracts tenant ID from JWT token in request header.
     *
     * @param request HTTP request containing Authorization header
     * @return tenant ID from JWT claims, or DEFAULT_TENANT_ID if not found
     */
    public String extractTenantId(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                Claims claims = parseToken(token);
                String tenantId = claims.get(CLAIM_TENANT_ID, String.class);
                if (tenantId != null && !tenantId.trim().isEmpty()) {
                    return tenantId;
                }
                logger.error()
                    .message(LOG_JWT_TENANT_ID_NULL)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .log();
                throw new IllegalStateException("Tenant ID missing in JWT token");
            }
            logger.error()
                .message(LOG_JWT_TOKEN_NULL)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .log();
            throw new IllegalStateException("JWT token missing in request");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error()
                .message(LOG_TENANT_ID_EXTRACTION_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            throw new IllegalStateException("Failed to extract tenant ID from JWT: " + e.getMessage(), e);
        }
    }

    /**
     * Extracts user ID from JWT token in request header.
     *
     * @param request HTTP request containing Authorization header
     * @return user ID from JWT subject claim, or null if not found
     */
    public String extractUserId(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            if (token != null) {
                Claims claims = parseToken(token);
                return claims.getSubject();
            }
        } catch (Exception e) {
            logger.warn()
                .message(LOG_JWT_EXTRACT_USERID_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
        return null;
    }

    /**
     * Extracts username from X-Username header (set by API Gateway) or JWT token.
     *
     * @param request HTTP request containing X-Username header or Authorization header
     * @return username from header or JWT claims, or null if not found
     */
    public String extractUsername(HttpServletRequest request) {
        try {
            logger.info()
                .message(LOG_JWT_EXTRACT_USERNAME_CALLED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .log();
            
            // First check if API Gateway already extracted and sent the username
            String username = request.getHeader("X-Username");
            logger.info()
                .message(LOG_JWT_USERNAME_HEADER_VALUE)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("username", username != null ? username : "NULL")
                .log();
            
            if (username != null && !username.isEmpty()) {
                logger.info()
                    .message(LOG_JWT_USERNAME_FROM_HEADER)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("username", username)
                    .log();
                return username;
            }
            
            // Fallback to extracting from JWT token
            String token = extractTokenFromRequest(request);
            logger.info()
                .message(LOG_JWT_TOKEN_EXTRACTED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("hasToken", token != null ? "YES" : "NULL")
                .log();
            
            if (token != null) {
                Claims claims = parseToken(token);
                String usernameFromToken = claims.get("username", String.class);
                logger.info()
                    .message(LOG_JWT_USERNAME_FROM_TOKEN)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("username", usernameFromToken != null ? usernameFromToken : "NULL")
                    .log();
                return usernameFromToken;
            }
        } catch (Exception e) {
            logger.warn()
                .message(LOG_JWT_USERNAME_EXTRACTION_FAILED)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
        }
        logger.warn()
            .message(LOG_JWT_USERNAME_NULL)
            .category(LOG_CATEGORY_WORKFLOW_SERVICE)
            .log();
        return null;
    }

    /**
     * Extracts JWT token from Authorization header.
     *
     * @param request HTTP request
     * @return JWT token string without "Bearer " prefix, or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    /**
     * Parses JWT token and extracts claims.
     *
     * @param token JWT token string
     * @return parsed claims
     */
    private Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Parses a JWT token string directly (not from HTTP request).
     * Used to validate and extract claims from tokens stored in workflow context.
     *
     * @param token JWT token string
     * @return parsed claims, or null if token is invalid
     */
    public Claims parseTokenString(String token) {
        try {
            return parseToken(token);
        } catch (Exception e) {
            logger.debug()
                .message("Failed to parse token string")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            return null;
        }
    }

    /**
     * Checks if a JWT token is expired.
     *
     * @param token JWT token string
     * @return true if token is expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            // If we can't parse the token, consider it expired/invalid
            logger.debug()
                .message("Cannot determine token expiration, treating as expired")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .log();
            return true;
        }
    }

    /**
     * Generates a fresh service-to-service JWT token for HTTP action nodes.
     * Used to replace expired tokens in long-running workflows.
     * Token structure matches the original tokens exactly:
     * - tenantId claim
     * - username claim
     * - sub (userId) claim
     * - iat (issued at) timestamp
     * - exp (expiration) timestamp (1 hour from now)
     *
     * @param tenantId Tenant identifier
     * @param userId User UUID (goes into 'sub' claim)
     * @param username Username
     * @return Fresh JWT token string
     */
    public String generateServiceToken(String tenantId, String userId, String username) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            long nowMillis = System.currentTimeMillis();
            Date now = new Date(nowMillis);
            Date expiration = new Date(nowMillis + (60 * 60 * 1000)); // 1 hour from now

            String token = Jwts.builder()
                .setSubject(userId)                      // 'sub' claim = userId
                .claim(CLAIM_TENANT_ID, tenantId)        // 'tenantId' claim
                .claim("username", username)             // 'username' claim
                .setIssuedAt(now)                        // 'iat' timestamp
                .setExpiration(expiration)               // 'exp' timestamp
                .signWith(key)                           // Sign with same secret
                .compact();

            logger.info()
                .message("Generated fresh service token for workflow HTTP action")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("tenantId", tenantId)
                .detail("userId", userId)
                .detail("expiresIn", "1 hour")
                .log();

            return token;
        } catch (Exception e) {
            logger.error(e)
                .message("Failed to generate service token")
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("tenantId", tenantId)
                .detail("userId", userId)
                .log();
            throw new RuntimeException("Failed to generate service token: " + e.getMessage(), e);
        }
    }
}
