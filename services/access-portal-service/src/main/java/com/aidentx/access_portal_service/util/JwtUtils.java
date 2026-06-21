package com.aidentx.access_portal_service.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static com.aidentx.access_portal_service.config.AccessPortalConstants.*;

/**
 * Utility class for extracting information from JWT tokens.
 * Used to extract tenantId and userId from request headers.
 * 
 * @author aidentx Development Team
 * @version 1.0
 */
@Component
@Slf4j
public class JwtUtils {

    @Value("${jwt.secret:mySuperSecretKey12345678901234AZERTY123456789}")
    private String jwtSecret;

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
                return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
            }
        } catch (Exception e) {
            log.warn(LOG_TENANT_ID_EXTRACTION_FAILED, e.getMessage());
        }
        return DEFAULT_TENANT_ID;
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
            log.warn("Failed to extract userId from JWT token: {}", e.getMessage());
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
            log.info("JwtUtils.extractUsername called");
            
            // First check if API Gateway already extracted and sent the username
            String username = request.getHeader("X-Username");
            log.info("X-Username header value: {}", username != null ? username : "NULL");
            
            if (username != null && !username.isEmpty()) {
                log.info("Returning username from X-Username header: {}", username);
                return username;
            }
            
            // Fallback to extracting from JWT token
            String token = extractTokenFromRequest(request);
            log.info("JWT token extracted: {}", token != null ? "YES" : "NULL");
            
            if (token != null) {
                Claims claims = parseToken(token);
                String usernameFromToken = claims.get("username", String.class);
                log.info("Username from JWT token: {}", usernameFromToken != null ? usernameFromToken : "NULL");
                return usernameFromToken;
            }
        } catch (Exception e) {
            log.warn("Failed to extract username from JWT token: {}", e.getMessage(), e);
        }
        log.warn("Returning NULL username - no source available");
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
}
