package com.aidentx.core.notifications_management.config.audit;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.shared.security.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

/**
 * Service for extracting user information from security context for notification audit purposes.
 * <p>
 * This service provides methods to extract user details from the Spring Security
 * context, including user ID, username, and IP address for notification audit logging.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-22
 */
@Service
public class NotificationSecurityContextService {
    
    private final IUnifiedLogger logger;
    private final JwtUtil jwtUtil;
    
    @Autowired
    public NotificationSecurityContextService(UnifiedLoggerFactory loggerFactory, JwtUtil jwtUtil) {
        this.logger = loggerFactory.getLogger(
            "NotificationSecurityContextService", 
            AppConstants.LOG_CATEGORY_NOTIFICATION_AUDIT
        );
        this.jwtUtil = jwtUtil;
    }
    
    /**
     * Extracts audit user information from the current security context.
     * 
     * @return AuditUser containing user information
     */
    public AuditUser getAuditUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            // First try to get JWT token from request and extract username
            String jwtToken = getJwtTokenFromRequest();
            if (jwtToken != null) {
                try {
                    String jwtUsername = jwtUtil.extractUsername(jwtToken);
                    String jwtUserId = jwtUtil.extractUserId(jwtToken);
                    
                    logger.info()
                        .message(AppConstants.AUDIT_LOG_EXTRACTED_USER_INFO_FROM_JWT_TOKEN)
                        .detail("jwtUserId", jwtUserId)
                        .detail("jwtUsername", jwtUsername)
                        .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                        .log();
                    
                    return new AuditUser(
                        jwtUserId != null ? jwtUserId : "system",
                        jwtUsername != null ? jwtUsername : "system",
                        getClientIpAddress()
                    );
                } catch (Exception e) {
                    logger.warn()
                        .message(AppConstants.AUDIT_LOG_FAILED_TO_EXTRACT_USER_INFO_FROM_JWT_TOKEN)
                        .detail("error", e.getMessage())
                        .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                        .log();
                }
            }
            
            // Debug logging to understand what type of authentication we have
            logger.info()
                .message(AppConstants.AUDIT_LOG_DEBUG_AUTHENTICATION_OBJECT)
                .detail("authenticationClass", authentication != null ? authentication.getClass().getName() : "null")
                .detail("principalClass", authentication != null && authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null")
                .detail("authenticationName", authentication != null ? authentication.getName() : "null")
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
            
            if (authentication != null && authentication.getPrincipal() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> jwtClaims = (Map<String, Object>) authentication.getPrincipal();
                
                String userId = (String) jwtClaims.get("sub");
                String username = (String) jwtClaims.get("username");
                String ipAddress = getClientIpAddress();
                
                logger.info()
                    .message(AppConstants.AUDIT_LOG_EXTRACTING_USER_INFO_FROM_JWT_CLAIMS)
                    .detail("userId", userId)
                    .detail("username", username)
                    .detail("allClaims", jwtClaims.keySet().toString())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                    .log();
                
                return new AuditUser(
                    userId != null ? userId : "system",
                    username != null ? username : "system",
                    ipAddress != null ? ipAddress : "unknown"
                );
            }
            
            // Handle Spring Security User object
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.User) {
                org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) authentication.getPrincipal();
                
                logger.info()
                    .message(AppConstants.AUDIT_LOG_EXTRACTING_USER_INFO_FROM_SPRING_SECURITY_USER)
                    .detail("userUsername", user.getUsername())
                    .detail("authenticationName", authentication.getName())
                    .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                    .log();
                
                // For Spring Security User, the username should be the actual username
                // But we need to get the user ID from somewhere else (probably the authentication name)
                String userId = authentication.getName(); // This is the UUID
                String username = user.getUsername(); // This should be the actual username
                
                return new AuditUser(
                    userId != null ? userId : "system",
                    username != null ? username : "system",
                    getClientIpAddress()
                );
            }
            
            // Try to extract from authentication name if available
            if (authentication != null && authentication.getName() != null) {
                return new AuditUser(
                    authentication.getName(),
                    authentication.getName(),  // Use the same for both for now
                    getClientIpAddress()
                );
            }
            
            // Fallback for system operations
            return new AuditUser("system", "system", getClientIpAddress());
            
        } catch (Exception e) {
            logger.warn()
                .message(AppConstants.AUDIT_LOG_FAILED_TO_EXTRACT_USER_INFO_FROM_SECURITY_CONTEXT)
                .detail("error", e.getMessage())
                .objectType(AppConstants.LOG_OBJECT_TYPE_AUDIT_EVENT)
                .log();
            
            return new AuditUser("system", "system", "unknown");
        }
    }
    
    /**
     * Extracts JWT token from the current HTTP request.
     */
    private String getJwtTokenFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader.substring(7); // Remove "Bearer " prefix
                }
            }
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.AUDIT_LOG_FAILED_TO_EXTRACT_JWT_TOKEN_FROM_REQUEST)
                .detail("error", e.getMessage())
                .log();
        }
        
        return null;
    }

    /**
     * Extracts the client IP address from the HTTP request.
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                
                // Check for forwarded headers first
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            logger.debug()
                .message(AppConstants.AUDIT_LOG_FAILED_TO_EXTRACT_IP_ADDRESS_FROM_REQUEST)
                .detail("error", e.getMessage())
                .log();
        }
        
        return "unknown";
    }
}
