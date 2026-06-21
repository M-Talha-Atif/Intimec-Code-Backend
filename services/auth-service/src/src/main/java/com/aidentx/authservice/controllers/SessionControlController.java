package com.aidentx.authservice.controllers;

import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.exception.PolicyAlreadyExistsException;
import com.aidentx.authservice.exception.PolicyNotFoundException;
import com.aidentx.authservice.model.dto.SessionControlRequest;
import com.aidentx.authservice.model.dto.SessionControlResponse;
import com.aidentx.authservice.model.dto.StatusMessageResponse;
import com.aidentx.authservice.service.Session.SessionControlService;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for session control policy management endpoints.
 * <p>
 * Handles CRUD operations for session control policies with automatic tenant extraction
 * from JWT tokens. Each tenant can have only one session control policy.
 * Time conversions are handled automatically (minutes to milliseconds for JWT expiration,
 * hours to milliseconds for refresh token expiration).
 * </p>
 *
 * @author Aidentx
 * @since 1.0
 */
@RestController
@RequestMapping("/api/v1/session-control")
public class SessionControlController {

    private final IUnifiedLogger logger;
    private final SessionControlService sessionControlService;
    private final JwtUtil jwtUtil;

    @Value("${jwt.expiration}")
    private Long defaultJwtExpirationMs;

    @Value("${security.jwt.refresh-token.expiration-ms}")
    private Long defaultRefreshTokenExpirationMs;

    public SessionControlController(UnifiedLoggerFactory unifiedLoggerFactory,
                                  SessionControlService sessionControlService,
                                  JwtUtil jwtUtil) {
        this.logger = unifiedLoggerFactory.getLogger("SessionControlController", AppConstants.LOG_CATEGORY_SESSION_CONTROL);
        this.sessionControlService = sessionControlService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Creates a new session control policy for the authenticated tenant.
     * <p>
     * Extracts tenant ID from the JWT token automatically.
     * Each tenant can have only one session control policy.
     * JWT expiration is provided in minutes, refresh token expiration in hours.
     * </p>
     *
     * @param request the session control policy creation request
     * @param httpRequest the HTTP request containing JWT token
     * @return response entity with status and message
     */
    @PostMapping
    public ResponseEntity<StatusMessageResponse> createSessionControl(
            @Valid @RequestBody SessionControlRequest request,
            HttpServletRequest httpRequest) {
        
        String tenantId = extractTenantIdFromRequest(httpRequest);
        logger.trace().message(String.format(AppConstants.REST_CREATE_SESSION_CONTROL, tenantId))
                .detail("tenantId", tenantId)
                .detail("jwtExpirationMinutes", request.getJwtExpirationMinutes())
                .detail("refreshTokenExpirationHours", request.getRefreshTokenExpirationHours())
                .log();

        try {
            StatusMessageResponse response = sessionControlService.createSessionControl(tenantId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (PolicyAlreadyExistsException ex) {
            logger.warn().message(String.format(AppConstants.WARN_SESSION_CONTROL_ALREADY_EXISTS, tenantId))
                    .detail("tenantId", tenantId).log();
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new StatusMessageResponse(409, ex.getMessage()));
        }
    }

    /**
     * Updates the existing session control policy for the authenticated tenant.
     * <p>
     * Extracts tenant ID from the JWT token automatically.
     * The policy must exist for the tenant.
     * JWT expiration is provided in minutes, refresh token expiration in hours.
     * </p>
     *
     * @param request the session control policy update request
     * @param httpRequest the HTTP request containing JWT token
     * @return response entity with status and message
     */
    @PutMapping
    public ResponseEntity<StatusMessageResponse> updateSessionControl(
            @Valid @RequestBody SessionControlRequest request,
            HttpServletRequest httpRequest) {
        
        String tenantId = extractTenantIdFromRequest(httpRequest);
        logger.trace().message(String.format(AppConstants.REST_UPDATE_SESSION_CONTROL, tenantId))
                .detail("tenantId", tenantId)
                .detail("jwtExpirationMinutes", request.getJwtExpirationMinutes())
                .detail("refreshTokenExpirationHours", request.getRefreshTokenExpirationHours())
                .log();

        try {
            StatusMessageResponse response = sessionControlService.updateSessionControl(tenantId, request);
            return ResponseEntity.ok(response);
        } catch (PolicyNotFoundException ex) {
            logger.warn().message(String.format(AppConstants.WARN_SESSION_CONTROL_NOT_FOUND, tenantId))
                    .detail("tenantId", tenantId).log();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new StatusMessageResponse(404, ex.getMessage()));
        }
    }

    /**
     * Retrieves the session control policy for the authenticated tenant.
     * <p>
     * Extracts tenant ID from the JWT token automatically.
     * Returns both millisecond values and convenient time units (minutes/hours).
     * </p>
     *
     * @param httpRequest the HTTP request containing JWT token
     * @return response entity with session control policy data
     */
    @GetMapping
    public ResponseEntity<SessionControlResponse> getSessionControl(HttpServletRequest httpRequest) {
        
        String tenantId = extractTenantIdFromRequest(httpRequest);
        logger.trace().message(String.format(AppConstants.REST_GET_SESSION_CONTROL, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            SessionControlResponse response = sessionControlService.getSessionControl(tenantId);
            return ResponseEntity.ok(response);
        } catch (PolicyNotFoundException ex) {
            logger.warn().message(String.format(AppConstants.WARN_SESSION_CONTROL_NOT_FOUND, tenantId))
                    .detail("tenantId", tenantId).log();
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Retrieves session control expiration times for the authenticated user's tenant.
     * <p>
     * Extracts tenant ID from the JWT token automatically and returns only the
     * expiration times in milliseconds for frontend use.
     * </p>
     *
     * @param httpRequest the HTTP request containing JWT token
     * @return response entity with JWT and refresh token expiration times in milliseconds
     */
    @GetMapping("/expiration-times")
    public ResponseEntity<Map<String, Long>> getSessionExpirationTimes(HttpServletRequest httpRequest) {
        
        String tenantId = extractTenantIdFromRequest(httpRequest);
        logger.trace().message(String.format(AppConstants.REST_GET_SESSION_EXPIRATION_TIMES, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            SessionControlResponse response = sessionControlService.getSessionControl(tenantId);
            
            Map<String, Long> expirationTimes = new HashMap<>();
            expirationTimes.put("jwtExpirationMs", response.getJwtExpirationMs());
            expirationTimes.put("refreshTokenExpirationMs", response.getRefreshTokenExpirationMs());
            
            return ResponseEntity.ok(expirationTimes);
        } catch (PolicyNotFoundException ex) {
            logger.warn().message(String.format(AppConstants.WARN_SESSION_CONTROL_NOT_FOUND_USING_DEFAULTS, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("defaultJwtExpirationMs", defaultJwtExpirationMs)
                    .detail("defaultRefreshTokenExpirationMs", defaultRefreshTokenExpirationMs)
                    .log();
            
            // Return default values from application properties if no session control policy exists
            Map<String, Long> defaultTimes = new HashMap<>();
            defaultTimes.put("jwtExpirationMs", defaultJwtExpirationMs);
            defaultTimes.put("refreshTokenExpirationMs", defaultRefreshTokenExpirationMs);
            
            return ResponseEntity.ok(defaultTimes);
        }
    }

    /**
     * Deletes the session control policy for the authenticated tenant.
     * <p>
     * Extracts tenant ID from the JWT token automatically.
     * This operation is idempotent - returns success even if the policy doesn't exist.
     * </p>
     *
     * @param httpRequest the HTTP request containing JWT token
     * @return response entity with status and message
     */
    @DeleteMapping
    public ResponseEntity<StatusMessageResponse> deleteSessionControl(HttpServletRequest httpRequest) {
        
        String tenantId = extractTenantIdFromRequest(httpRequest);
        logger.trace().message(String.format(AppConstants.REST_DELETE_SESSION_CONTROL, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            StatusMessageResponse response = sessionControlService.deleteSessionControl(tenantId);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            logger.error().message(String.format(AppConstants.ERROR_DELETING_SESSION_CONTROL, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("error", ex.getMessage())
                    .log();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StatusMessageResponse(500, AppConstants.ERROR_FAILED_DELETE_SESSION_CONTROL));
        }
    }

    /**
     * Extracts tenant ID from the JWT token in the HTTP request.
     *
     * @param request the HTTP servlet request
     * @return the tenant ID extracted from the JWT token
     * @throws RuntimeException if tenant ID cannot be extracted
     */
    private String extractTenantIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.error().message(AppConstants.ERROR_MISSING_INVALID_AUTH_HEADER)
                    .detail("authHeader", authHeader).log();
            throw new RuntimeException(AppConstants.ERROR_INVALID_MISSING_JWT);
        }

        String token = authHeader.substring(7);
        String tenantId = jwtUtil.extractTenantId(token);
        
        if (tenantId == null || "unknown".equals(tenantId)) {
            logger.error().message(AppConstants.ERROR_FAILED_EXTRACT_TENANT_ID)
                    .detail("tokenPrefix", token.substring(0, Math.min(10, token.length()))).log();
            throw new RuntimeException(AppConstants.ERROR_INVALID_JWT_CANNOT_EXTRACT_TENANT);
        }

        return tenantId;
    }
}
