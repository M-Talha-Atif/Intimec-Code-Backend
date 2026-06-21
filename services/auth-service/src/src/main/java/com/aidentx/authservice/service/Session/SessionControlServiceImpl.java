package com.aidentx.authservice.service.Session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.aidentx.authservice.audit.AuditLogEventBuilder;
import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.exception.PolicyAlreadyExistsException;
import com.aidentx.authservice.exception.PolicyNotFoundException;
import com.aidentx.authservice.model.dto.SessionControlRequest;
import com.aidentx.authservice.model.dto.SessionControlResponse;
import com.aidentx.authservice.model.dto.StatusMessageResponse;
import com.aidentx.authservice.model.entity.SessionControl;
import com.aidentx.authservice.repository.SessionControlRepository;
import com.aidentx.authservice.service.AuditLogEventService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.authservice.utils.RequestUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of {@link SessionControlService} for session control policy management.
 * <p>
 * This service handles CRUD operations for session control policies with complete
 * audit logging, tenant isolation, and proper error handling. Each tenant can have
 * only one session control policy.
 * </p>
 * 
 * <p>Features include:</p>
 * <ul>
 *   <li>Automatic conversion between time units (minutes/hours to milliseconds)</li>
 *   <li>Comprehensive audit logging for all operations</li>
 *   <li>Tenant-specific policy isolation</li>
 *   <li>Proper exception handling and error messages</li>
 *   <li>Transaction support for data consistency</li>
 * </ul>
 *
 * @author Aidentx
 * @since 1.0
 */
@Service
public class SessionControlServiceImpl implements SessionControlService {

    private final IUnifiedLogger logger;
    private final SessionControlRepository sessionControlRepository;
    private final AuditLogEventService auditLogEventService;
    private final JwtUtil jwtUtil;

    public SessionControlServiceImpl(UnifiedLoggerFactory unifiedLoggerFactory,
                                   SessionControlRepository sessionControlRepository,
                                   AuditLogEventService auditLogEventService,
                                   JwtUtil jwtUtil) {
        this.logger = unifiedLoggerFactory.getLogger("SessionControlService", AppConstants.LOG_CATEGORY_SESSION_CONTROL);
        this.sessionControlRepository = sessionControlRepository;
        this.auditLogEventService = auditLogEventService;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional
    public StatusMessageResponse createSessionControl(String tenantId, SessionControlRequest request) {
        logger.trace().message(String.format(AppConstants.LOG_SESSION_CONTROL_CREATE_START, tenantId))
                .detail("tenantId", tenantId)
                .detail("jwtExpirationMinutes", request.getJwtExpirationMinutes())
                .detail("refreshTokenExpirationHours", request.getRefreshTokenExpirationHours())
                .detail("forceLogoutOnPasswordChange", request.getForceLogoutOnPasswordChange())
                .log();

        try {
            // Check if session control policy already exists for this tenant
            if (sessionControlRepository.existsByTenantId(tenantId)) {
                logger.warn().message(AppConstants.SESSION_CONTROL_ALREADY_EXISTS)
                        .detail("tenantId", tenantId).log();
                throw new PolicyAlreadyExistsException(AppConstants.SESSION_CONTROL_ALREADY_EXISTS);
            }

            // Convert time units to milliseconds
            long jwtExpirationMs = convertMinutesToMilliseconds(request.getJwtExpirationMinutes());
            long refreshTokenExpirationMs = convertHoursToMilliseconds(request.getRefreshTokenExpirationHours());

            logger.debug().message(String.format(AppConstants.LOG_CONVERTING_JWT_MINUTES_TO_MS, 
                    request.getJwtExpirationMinutes(), jwtExpirationMs))
                    .detail("originalMinutes", request.getJwtExpirationMinutes())
                    .detail("convertedMs", jwtExpirationMs)
                    .log();

            logger.debug().message(String.format(AppConstants.LOG_CONVERTING_REFRESH_HOURS_TO_MS, 
                    request.getRefreshTokenExpirationHours(), refreshTokenExpirationMs))
                    .detail("originalHours", request.getRefreshTokenExpirationHours())
                    .detail("convertedMs", refreshTokenExpirationMs)
                    .log();

            // Create and save session control policy
            SessionControl sessionControl = new SessionControl(
                    tenantId,
                    jwtExpirationMs,
                    refreshTokenExpirationMs,
                    request.getForceLogoutOnPasswordChange()
            );

            SessionControl savedPolicy = sessionControlRepository.save(sessionControl);

            logger.info().message(String.format(AppConstants.LOG_SESSION_CONTROL_CREATE_SUCCESS, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("sessionControlId", savedPolicy.getId())
                    .log();

            // Send audit log
            sendSessionControlAuditEvent(tenantId, AppConstants.AUDIT_SESSION_CONTROL_CREATED, 
                    null, savedPolicy);

            return new StatusMessageResponse(201, AppConstants.SESSION_CONTROL_CREATED_SUCCESS);

        } catch (PolicyAlreadyExistsException ex) {
            logger.error().message(String.format(AppConstants.LOG_SESSION_CONTROL_CREATE_ERROR, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("error", ex.getMessage())
                    .log();
            throw ex;
        } catch (Exception ex) {
            logger.error().message(String.format(AppConstants.LOG_SESSION_CONTROL_CREATE_ERROR, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("error", ex.getMessage())
                    .log();
            throw new RuntimeException(AppConstants.FAILED_TO_CREATE_SESSION_CONTROL_POLICY, ex);
        }
    }

    @Override
    @Transactional
    public StatusMessageResponse updateSessionControl(String tenantId, SessionControlRequest request) {
        logger.trace().message(String.format(AppConstants.LOG_SESSION_CONTROL_UPDATE_START, tenantId))
                .detail("tenantId", tenantId)
                .detail("jwtExpirationMinutes", request.getJwtExpirationMinutes())
                .detail("refreshTokenExpirationHours", request.getRefreshTokenExpirationHours())
                .detail("forceLogoutOnPasswordChange", request.getForceLogoutOnPasswordChange())
                .log();

        try {
            // Find existing session control policy
            Optional<SessionControl> existingPolicyOpt = sessionControlRepository.findByTenantId(tenantId);
            if (existingPolicyOpt.isEmpty()) {
                logger.warn().message(AppConstants.SESSION_CONTROL_NOT_FOUND)
                        .detail("tenantId", tenantId).log();
                throw new PolicyNotFoundException(AppConstants.SESSION_CONTROL_NOT_FOUND);
            }

            SessionControl existingPolicy = existingPolicyOpt.get();
            SessionControl stateBefore = createPolicyCopy(existingPolicy);

            // Convert time units to milliseconds
            long jwtExpirationMs = convertMinutesToMilliseconds(request.getJwtExpirationMinutes());
            long refreshTokenExpirationMs = convertHoursToMilliseconds(request.getRefreshTokenExpirationHours());

            logger.debug().message(String.format(AppConstants.LOG_CONVERTING_JWT_MINUTES_TO_MS, 
                    request.getJwtExpirationMinutes(), jwtExpirationMs))
                    .detail("originalMinutes", request.getJwtExpirationMinutes())
                    .detail("convertedMs", jwtExpirationMs)
                    .log();

            logger.debug().message(String.format(AppConstants.LOG_CONVERTING_REFRESH_HOURS_TO_MS, 
                    request.getRefreshTokenExpirationHours(), refreshTokenExpirationMs))
                    .detail("originalHours", request.getRefreshTokenExpirationHours())
                    .detail("convertedMs", refreshTokenExpirationMs)
                    .log();

            // Update session control policy
            existingPolicy.setJwtExpirationMs(jwtExpirationMs);
            existingPolicy.setRefreshTokenExpirationMs(refreshTokenExpirationMs);
            existingPolicy.setForceLogoutOnPasswordChange(request.getForceLogoutOnPasswordChange());

            SessionControl updatedPolicy = sessionControlRepository.save(existingPolicy);

            logger.info().message(String.format(AppConstants.LOG_SESSION_CONTROL_UPDATE_SUCCESS, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("sessionControlId", updatedPolicy.getId())
                    .log();

            // Send audit log
            sendSessionControlAuditEvent(tenantId, AppConstants.AUDIT_SESSION_CONTROL_UPDATED, 
                    stateBefore, updatedPolicy);

            return new StatusMessageResponse(200, AppConstants.SESSION_CONTROL_UPDATED_SUCCESS);

        } catch (PolicyNotFoundException ex) {
            logger.error().message(String.format(AppConstants.LOG_SESSION_CONTROL_UPDATE_ERROR, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("error", ex.getMessage())
                    .log();
            throw ex;
        } catch (Exception ex) {
            logger.error().message(String.format(AppConstants.LOG_SESSION_CONTROL_UPDATE_ERROR, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("error", ex.getMessage())
                    .log();
            throw new RuntimeException(AppConstants.FAILED_TO_UPDATE_SESSION_CONTROL_POLICY, ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SessionControlResponse getSessionControl(String tenantId) {
        logger.trace().message(String.format(AppConstants.LOG_SESSION_CONTROL_GET_START, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            Optional<SessionControl> policyOpt = sessionControlRepository.findByTenantId(tenantId);
            if (policyOpt.isEmpty()) {
                logger.warn().message(AppConstants.SESSION_CONTROL_NOT_FOUND)
                        .detail("tenantId", tenantId).log();
                throw new PolicyNotFoundException(AppConstants.SESSION_CONTROL_NOT_FOUND);
            }

            SessionControl policy = policyOpt.get();

            logger.info().message(String.format(AppConstants.LOG_SESSION_CONTROL_GET_SUCCESS, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("sessionControlId", policy.getId())
                    .log();

            // Convert to response DTO
            return new SessionControlResponse(
                    policy.getId(),
                    policy.getTenantId(),
                    policy.getJwtExpirationMs(),
                    policy.getRefreshTokenExpirationMs(),
                    policy.getForceLogoutOnPasswordChange()
            );

        } catch (PolicyNotFoundException ex) {
            logger.error().message(String.format(AppConstants.LOG_SESSION_CONTROL_GET_ERROR, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("error", ex.getMessage())
                    .log();
            throw ex;
        } catch (Exception ex) {
            logger.error().message(String.format(AppConstants.LOG_SESSION_CONTROL_GET_ERROR, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("error", ex.getMessage())
                    .log();
            throw new RuntimeException(AppConstants.FAILED_TO_RETRIEVE_SESSION_CONTROL_POLICY, ex);
        }
    }

    @Override
    @Transactional
    public StatusMessageResponse deleteSessionControl(String tenantId) {
        logger.trace().message(String.format(AppConstants.LOG_SESSION_CONTROL_DELETE_START, tenantId))
                .detail("tenantId", tenantId).log();

        try {
            Optional<SessionControl> policyOpt = sessionControlRepository.findByTenantId(tenantId);
            if (policyOpt.isEmpty()) {
                logger.info().message(AppConstants.SESSION_CONTROL_NOT_FOUND)
                        .detail("tenantId", tenantId).log();
                // Return success for idempotent operation
                return new StatusMessageResponse(200, AppConstants.SESSION_CONTROL_DELETED_SUCCESS);
            }

            SessionControl existingPolicy = policyOpt.get();

            // Delete the session control policy
            int deletedCount = sessionControlRepository.deleteByTenantId(tenantId);

            if (deletedCount > 0) {
                logger.info().message(String.format(AppConstants.LOG_SESSION_CONTROL_DELETE_SUCCESS, tenantId))
                        .detail("tenantId", tenantId)
                        .detail("deletedCount", deletedCount)
                        .log();

                // Send audit log
                sendSessionControlAuditEvent(tenantId, AppConstants.AUDIT_SESSION_CONTROL_DELETED, 
                        existingPolicy, null);

                return new StatusMessageResponse(200, AppConstants.SESSION_CONTROL_DELETED_SUCCESS);
            } else {
                logger.warn().message(AppConstants.SESSION_CONTROL_DELETE_FAILED)
                        .detail("tenantId", tenantId).log();
                throw new RuntimeException(AppConstants.SESSION_CONTROL_DELETE_FAILED);
            }

        } catch (Exception ex) {
            logger.error().message(String.format(AppConstants.LOG_SESSION_CONTROL_DELETE_ERROR, tenantId))
                    .detail("tenantId", tenantId)
                    .detail("error", ex.getMessage())
                    .log();
            throw new RuntimeException(AppConstants.FAILED_TO_DELETE_SESSION_CONTROL_POLICY, ex);
        }
    }

    /**
     * Converts minutes to milliseconds.
     *
     * @param minutes the minutes to convert
     * @return the equivalent milliseconds
     */
    private long convertMinutesToMilliseconds(int minutes) {
        return minutes * 60L * 1000L;
    }

    /**
     * Converts hours to milliseconds.
     *
     * @param hours the hours to convert
     * @return the equivalent milliseconds
     */
    private long convertHoursToMilliseconds(int hours) {
        return hours * 60L * 60L * 1000L;
    }

    /**
     * Creates a deep copy of a SessionControl entity for audit logging.
     *
     * @param original the original SessionControl entity
     * @return a copy of the original entity
     */
    private SessionControl createPolicyCopy(SessionControl original) {
        SessionControl copy = new SessionControl();
        copy.setId(original.getId());
        copy.setTenantId(original.getTenantId());
        copy.setJwtExpirationMs(original.getJwtExpirationMs());
        copy.setRefreshTokenExpirationMs(original.getRefreshTokenExpirationMs());
        copy.setForceLogoutOnPasswordChange(original.getForceLogoutOnPasswordChange());
        return copy;
    }

    /**
     * Sends an audit log event for session control policy operations.
     * <p>
     * Creates comprehensive audit logs with before/after states for tracking
     * all session control policy changes.
     * </p>
     *
     * @param action      the action performed
     * @param tenantId    the tenant identifier
     * @param stateBefore the entity state before the operation (can be null)
     * @param stateAfter  the entity state after the operation (can be null)
     */
    private void sendSessionControlAuditEvent(String tenantId, String action, 
                                             SessionControl stateBefore, SessionControl stateAfter) {
        try {
            logger.debug().message(AppConstants.PREPARING_SESSION_CONTROL_AUDIT_EVENT)
                    .detail("action", action)
                    .detail("tenantId", tenantId)
                    .log();

            ObjectMapper objectMapper = new ObjectMapper();
            
            // Create state before JSON
            ObjectNode stateBeforeJson = null;
            if (stateBefore != null) {
                stateBeforeJson = objectMapper.createObjectNode();
                stateBeforeJson.put("id", stateBefore.getId() != null ? stateBefore.getId().toString() : null);
                stateBeforeJson.put("tenantId", stateBefore.getTenantId());
                stateBeforeJson.put("jwtExpirationMs", stateBefore.getJwtExpirationMs());
                stateBeforeJson.put("refreshTokenExpirationMs", stateBefore.getRefreshTokenExpirationMs());
                stateBeforeJson.put("forceLogoutOnPasswordChange", stateBefore.getForceLogoutOnPasswordChange());
            }

            // Create state after JSON
            ObjectNode stateAfterJson = null;
            if (stateAfter != null) {
                stateAfterJson = objectMapper.createObjectNode();
                stateAfterJson.put("id", stateAfter.getId() != null ? stateAfter.getId().toString() : null);
                stateAfterJson.put("tenantId", stateAfter.getTenantId());
                stateAfterJson.put("jwtExpirationMs", stateAfter.getJwtExpirationMs());
                stateAfterJson.put("refreshTokenExpirationMs", stateAfter.getRefreshTokenExpirationMs());
                stateAfterJson.put("forceLogoutOnPasswordChange", stateAfter.getForceLogoutOnPasswordChange());
            }

            // Create custom metadata
            ObjectNode customMetadata = objectMapper.createObjectNode();
            customMetadata.put("type", "session_control");
            customMetadata.put("tenantId", tenantId);

            // Get JWT token from request
            String token = null;
            HttpServletRequest request = null;
            try {
                ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
                request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    token = authHeader.substring(7);
                }
            } catch (Exception e) {
                // No token available
            }

            // Extract user info from JWT
            UUID actorId = null;
            String actorName = "system";
            if (token != null) {
                String userIdStr = jwtUtil.extractUserId(token);
                String username = jwtUtil.extractUsername(token);
                if (userIdStr != null) {
                    try {
                        actorId = UUID.fromString(userIdStr);
                    } catch (Exception e) {
                        // Invalid UUID format
                    }
                }
                if (username != null) {
                    actorName = username;
                }
            }

            // Get client IP and User-Agent from request
            String clientIp = request != null ? RequestUtils.getClientIP(request) : AppConstants.UNKNOWN_IP_ADDRESS;
            String userAgent = request != null ? RequestUtils.getUserAgent(request) : "system";

            // Send audit event
            auditLogEventService.sendAuditEvent(AuditLogEventBuilder.createEventWithTenant(
                    actorId, // actorId from JWT sub
                    "user", // actorType
                    actorName, // actorName from JWT username
                    tenantId,
                    stateAfter != null ? stateAfter.getId() : null, // objectId
                    "session_control", // objectType
                    "Session Control Policy", // objectName
                    action, // event
                    AppConstants.SUCCESS_EVENT_TYPE, // status
                    AppConstants.EVENT_RISK_MEDIUM, // riskLevel
                    clientIp, // clientIp - actual IP from request
                    userAgent, // userAgent - actual User-Agent from request
                    action + " session control policy", // message
                    stateBeforeJson,
                    stateAfterJson,
                    null, // additionalData
                    customMetadata
            ));

            logger.info().message(AppConstants.SESSION_CONTROL_AUDIT_EVENT_SENT_SUCCESSFULLY)
                    .detail("action", action)
                    .detail("tenantId", tenantId)
                    .log();

        } catch (Exception ex) {
            logger.error().message(AppConstants.FAILED_TO_SEND_SESSION_CONTROL_AUDIT_EVENT)
                    .detail("tenantId", tenantId)
                    .detail("action", action)
                    .detail("error", ex.getMessage())
                    .log();
        }
    }
}
