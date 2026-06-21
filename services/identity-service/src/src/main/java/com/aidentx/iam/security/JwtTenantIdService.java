package com.aidentx.iam.security;

import com.aidentx.iam.config.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service for extracting tenant ID from JWT tokens and request headers.
 *
 * <p>This service provides a centralized way to extract tenant IDs from various sources
 * in the following priority order:</p>
 * <ol>
 *   <li>JWT token claims (primary source)</li>
 *   <li>Request headers (fallback)</li>
 * </ol>
 *
 * <p>The service ensures consistent tenant ID extraction across the application
 * and provides proper logging and error handling for audit and debugging purposes.</p>
 *
 * @author Aidentx Development Team
 * @see JwtUtil
 * @see SecurityConstants
 * @since 1.0
 */
@Service
public class JwtTenantIdService {

    private final JwtUtil jwtUtil;
    private final IUnifiedLogger logger;

    /**
     * Constructs a new JWT tenant ID service.
     *
     * @param jwtUtil       the JWT utility for token operations
     * @param loggerFactory the logger factory for creating loggers
     */
    public JwtTenantIdService(JwtUtil jwtUtil, UnifiedLoggerFactory loggerFactory) {
        this.jwtUtil = jwtUtil;
        this.logger = loggerFactory.getLogger(AppConstants.JWT_TENANTID_SERVICE_LOGGER_NAME, AppConstants.LOG_CATEGORY_JWT_TENANTID);
    }

    /**
     * Extracts tenant ID from the current request context.
     *
     * <p>This method attempts to extract the tenant ID using the following strategy:</p>
     * <ol>
     *   <li>Extract from JWT token in Authorization header</li>
     *   <li>Fallback to X-Tenant-ID header if JWT extraction fails</li>
     *   <li>Return null if no tenant ID is found</li>
     * </ol>
     *
     * @return the tenant ID if found, null otherwise
     * @throws RuntimeException if tenant ID extraction fails unexpectedly
     */
    public String extractTenantIdFromRequest() {
        logger.debug().message(AppConstants.LOG_JWT_TENANTID_EXTRACTION_STARTED).log();

        try {
            // Get the current HTTP request
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                logger.warn().message(AppConstants.LOG_JWT_TENANTID_NO_FALLBACK).detail(AppConstants.DETAIL_JWT_TOKEN_AVAILABLE, false).log();
                return null;
            }

            HttpServletRequest request = attributes.getRequest();

            // Try to extract from JWT first
            String tenantIdFromJwt = extractTenantIdFromJwt(request);
            if (tenantIdFromJwt != null) {
                logger.info().message(AppConstants.LOG_JWT_TENANTID_EXTRACTION_SUCCESS).detail(AppConstants.DETAIL_JWT_EXTRACTED_TENANTID, tenantIdFromJwt).detail(AppConstants.DETAIL_AUDIT_TENANTID_SOURCE, AppConstants.TENANTID_SOURCE_JWT).log();
                return tenantIdFromJwt;
            }

            // Fallback to header
            String tenantIdFromHeader = extractTenantIdFromHeader(request);
            if (tenantIdFromHeader != null) {
                logger.info().message(AppConstants.LOG_JWT_TENANTID_FALLBACK_HEADER).detail(AppConstants.DETAIL_JWT_HEADER_TENANTID, tenantIdFromHeader).detail(AppConstants.DETAIL_AUDIT_TENANTID_SOURCE, AppConstants.TENANTID_SOURCE_HEADER).log();
                return tenantIdFromHeader;
            }

            logger.warn().message(AppConstants.LOG_JWT_TENANTID_NO_FALLBACK).log();
            return null;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_JWT_TENANTID_EXTRACTION_FAILED).log();
            throw new RuntimeException(AppConstants.EXCEPTION_JWT_TENANTID_EXTRACTION_FAILED, e);
        }
    }

    /**
     * Extracts tenant ID from JWT token in the Authorization header.
     *
     * @param request the HTTP request containing the Authorization header
     * @return the tenant ID from JWT token, or null if not found
     */
    private String extractTenantIdFromJwt(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
            if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
                logger.debug().message(AppConstants.LOG_JWT_TENANTID_NOT_FOUND).detail(AppConstants.DETAIL_JWT_TOKEN_AVAILABLE, false).log();
                return null;
            }

            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
            if (jwtUtil.validateToken(token)) {
                String tenantId = jwtUtil.extractTenantId(token);
                if (tenantId != null) {
                    logger.debug().message(AppConstants.LOG_JWT_TENANTID_EXTRACTION_SUCCESS).detail(AppConstants.DETAIL_JWT_EXTRACTED_TENANTID, tenantId).log();
                    return tenantId;
                }
            }

            logger.debug().message(AppConstants.LOG_JWT_TENANTID_NOT_FOUND).detail(AppConstants.DETAIL_JWT_TOKEN_AVAILABLE, true).log();
            return null;

        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_JWT_TENANTID_EXTRACTION_FAILED).detail(AppConstants.LOG_DETAIL_KEY_ERROR, e.getMessage()).log();
            return null;
        }
    }

    /**
     * Extracts tenant ID from request header.
     *
     * @param request the HTTP request containing headers
     * @return the tenant ID from header, or null if not found
     */
    private String extractTenantIdFromHeader(HttpServletRequest request) {
        String tenantId = request.getHeader("X-Tenant-ID");
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            logger.debug().message(AppConstants.LOG_JWT_TENANTID_FALLBACK_HEADER).detail(AppConstants.DETAIL_JWT_HEADER_TENANTID, tenantId.trim()).log();
            return tenantId.trim();
        }
        return null;
    }

    /**
     * Extracts tenant ID from JWT token directly (without request context).
     *
     * <p>This method is useful when you already have a JWT token and need to extract
     * the tenant ID without going through the request context.</p>
     *
     * @param jwtToken the JWT token to extract tenant ID from
     * @return the tenant ID if found, null otherwise
     */
    public String extractTenantIdFromToken(String jwtToken) {
        logger.debug().message(AppConstants.LOG_JWT_TENANTID_EXTRACTION_STARTED).log();

        try {
            if (jwtToken == null || jwtToken.trim().isEmpty()) {
                logger.debug().message(AppConstants.LOG_JWT_TENANTID_NOT_FOUND).detail(AppConstants.DETAIL_JWT_TOKEN_AVAILABLE, false).log();
                return null;
            }

            if (jwtUtil.validateToken(jwtToken)) {
                String tenantId = jwtUtil.extractTenantId(jwtToken);
                if (tenantId != null) {
                    logger.debug().message(AppConstants.LOG_JWT_TENANTID_EXTRACTION_SUCCESS).detail(AppConstants.DETAIL_JWT_EXTRACTED_TENANTID, tenantId).log();
                    return tenantId;
                }
            }

            logger.debug().message(AppConstants.LOG_JWT_TENANTID_NOT_FOUND).log();
            return null;

        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_JWT_TENANTID_EXTRACTION_FAILED).detail(AppConstants.LOG_DETAIL_KEY_ERROR, e.getMessage()).log();
            return null;
        }
    }
}