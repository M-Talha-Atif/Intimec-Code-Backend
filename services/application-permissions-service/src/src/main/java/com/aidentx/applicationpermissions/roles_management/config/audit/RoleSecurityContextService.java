package com.aidentx.applicationpermissions.roles_management.config.audit;

import com.aidentx.applicationpermissions.roles_management.config.AppConstants;
import com.aidentx.applicationpermissions.shared.security.TenantContextService;
import com.aidentx.applicationpermissions.shared.security.UserContext;
import com.aidentx.applicationpermissions.shared.security.UserContextHolder;
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
 * Service for extracting user information from security context for role management audit purposes.
 * <p>
 * This service provides methods to extract user details from the Spring Security
 * context, including user ID, username, and IP address for role management audit logging.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-01-21
 */
@Service
public class RoleSecurityContextService {
    
    private final IUnifiedLogger logger;
    private final TenantContextService tenantContextService;
    
    @Autowired
    public RoleSecurityContextService(UnifiedLoggerFactory loggerFactory, TenantContextService tenantContextService) {
        this.logger = loggerFactory.getLogger(
            "RoleSecurityContextService", 
            AppConstants.LOG_CATEGORY_ROLE_AUDIT
        );
        this.tenantContextService = tenantContextService;
    }
    
    /**
     * Extracts audit user information from the current security context.
     * 
     * @return AuditUser containing user information
     */
    public AuditUser getAuditUser() {
        try {
            String ipAddress = getClientIpAddress();
            
            // Primary: Use TenantContextService to get user ID and username directly from JWT
            try {
                String userId = tenantContextService.getCurrentUserId(); // This gets from JWT sub claim
                String username = tenantContextService.getCurrentUsername(); // This gets from JWT username claim
                
                logger.info()
                    .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                    .message(AppConstants.EXTRACTED_USER_INFO_FROM_TENANT_CONTEXT)
                    .details(Map.of(
                        "userId", userId,
                        "username", username,
                        "ipAddress", ipAddress
                    ))
                    .log();
                
                return new AuditUser(userId, username, ipAddress);
            } catch (Exception e) {
                logger.warn()
                    .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                    .message(AppConstants.FAILED_TO_EXTRACT_USER_INFO_FROM_TENANT_CONTEXT)
                    .details(Map.of("error", e.getMessage(), "stackTrace", getStackTraceString(e)))
                    .log();
            }
            
            // Fallback: try UserContextHolder (set by JWT filter)
            UserContext userContext = UserContextHolder.getContext();
            if (userContext != null) {
                logger.info()
                    .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                    .message(AppConstants.EXTRACTED_USER_INFO_FROM_USER_CONTEXT_HOLDER)
                    .details(Map.of(
                        "userId", userContext.userId(),
                        "username", userContext.username(),
                        "ipAddress", ipAddress
                    ))
                    .log();
                
                return new AuditUser(userContext.userId(), userContext.username(), ipAddress);
            } else {
                logger.warn()
                    .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                    .message("UserContextHolder returned null - no user context available")
                    .log();
            }
            
                        // Fallback: try request attributes (will not exist in scheduled tasks)
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                HttpServletRequest request = attributes.getRequest();
                
                String userId = (String) request.getAttribute("userId");
                String username = (String) request.getAttribute("username");
                
                if (userId != null && username != null) {
                    logger.info()
                        .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                        .message(AppConstants.AUDIT_LOG_EXTRACTING_USER_INFO_FROM_REQUEST_ATTRIBUTES)
                        .details(Map.of(
                            "userId", userId,
                            "username", username,
                            "ipAddress", ipAddress
                        ))
                        .log();
                    
                    return new AuditUser(userId, username, ipAddress);
                }
                
                // Try authentication from Spring Security context
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.getName() != null) {
                    String authName = authentication.getName();
                    logger.info()
                        .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                        .message(AppConstants.AUDIT_LOG_FALLBACK_TO_AUTHENTICATION_NAME)
                        .details(Map.of("authenticationName", authName))
                        .log();
                    
                    return new AuditUser(authName, authName, ipAddress);
                }
            } catch (IllegalStateException e) {
                // No request context available - this is expected for scheduled tasks
                logger.warn()
                    .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                    .message("No request context available (expected for background tasks)")
                    .log();
            }
            
            // Ultimate fallback for scheduled operations - use a system UUID
            logger.warn()
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message("Falling back to system user - all auth extraction methods failed")
                .details(Map.of("ipAddress", ipAddress))
                .log();
            return new AuditUser("00000000-0000-0000-0000-000000000000", "system", ipAddress);
            
        } catch (Exception e) {
            logger.error(e)
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message(AppConstants.AUDIT_LOG_ERROR_EXTRACTING_USER_INFO)
                .details(Map.of("error", e.getMessage()))
                .log();
            
            // Use system UUID for error fallback as well
            return new AuditUser("00000000-0000-0000-0000-000000000000", "system", getClientIpAddress());
        }
    }
    
    /**
     * Extracts the client IP address from the current HTTP request.
     * 
     * @return the client IP address or "unknown" if not available
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            
            // Check for X-Forwarded-For header (proxy scenarios)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return xForwardedFor.split(",")[0].trim();
            }
            
            // Check for X-Real-IP header
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            // Fallback to remote address
            return request.getRemoteAddr();
            
        } catch (Exception e) {
            logger.debug()
                .category(AppConstants.LOG_CATEGORY_ROLE_AUDIT)
                .message(AppConstants.AUDIT_LOG_FAILED_TO_GET_CLIENT_IP)
                .details(Map.of("error", e.getMessage()))
                .log();
            
            return "unknown";
        }
    }
    
    /**
     * Converts exception stack trace to string for logging.
     * 
     * @param e the exception
     * @return stack trace as string
     */
    private String getStackTraceString(Exception e) {
        if (e == null) return "";
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("; ");
            if (sb.length() > 500) break; // Limit length
        }
        return sb.toString();
    }
}