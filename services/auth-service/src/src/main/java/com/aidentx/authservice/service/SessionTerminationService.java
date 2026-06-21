package com.aidentx.authservice.service;

import com.aidentx.auditlogdto.AuditLogKafkaEvent;
import com.aidentx.authservice.audit.AuditLogEventBuilder;
import com.aidentx.authservice.config.AppConstants;
import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.model.entity.UserSession;
import com.aidentx.authservice.repository.UserRepository;
import com.aidentx.authservice.repository.UserSessionRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service responsible for terminating user sessions and notifying users via WebSocket.
 * 
 * <p>This service handles the complete session termination flow:
 * <ul>
 *   <li>Validates session existence and authorization</li>
 *   <li>Deletes the refresh token associated with the session</li>
 *   <li>Deletes the session from the database</li>
 *   <li>Sends real-time WebSocket notification to the user if connected</li>
 * </ul>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class SessionTerminationService {

    private final IUnifiedLogger logger;
    private final UserSessionRepository userSessionRepository;
    private final RefreshTokenService refreshTokenService;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuditLogEventService auditLogEventService;
    private final UserRepository userRepository;
    private final IpGeolocationService ipGeolocationService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new SessionTerminationService.
     *
     * @param unifiedLoggerFactory  factory to create unified loggers
     * @param userSessionRepository repository for user session operations
     * @param refreshTokenService   service to handle refresh token operations
     * @param messagingTemplate     WebSocket messaging template for sending notifications
     * @param auditLogEventService  service for sending audit log events
     * @param userRepository        repository for user operations
     * @param ipGeolocationService  service for IP geolocation
     */
    public SessionTerminationService(
            UnifiedLoggerFactory unifiedLoggerFactory,
            UserSessionRepository userSessionRepository,
            RefreshTokenService refreshTokenService,
            SimpMessagingTemplate messagingTemplate,
            AuditLogEventService auditLogEventService,
            UserRepository userRepository,
            IpGeolocationService ipGeolocationService) {
        this.logger = unifiedLoggerFactory.getLogger(SessionTerminationService.class);
        this.userSessionRepository = userSessionRepository;
        this.refreshTokenService = refreshTokenService;
        this.messagingTemplate = messagingTemplate;
        this.auditLogEventService = auditLogEventService;
        this.userRepository = userRepository;
        this.ipGeolocationService = ipGeolocationService;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Terminates a user session by session ID.
     * 
     * <p>This method:
     * <ol>
     *   <li>Validates that the session exists</li>
     *   <li>Checks that the requesting user has authorization (same tenant)</li>
     *   <li>Deletes the associated refresh token</li>
     *   <li>Deletes the session from the database</li>
     *   <li>Sends a WebSocket notification to the user</li>
     * </ol>
     *
     * @param sessionId      the UUID of the session to terminate
     * @param requestingUser the user requesting the termination (for authorization)
     * @param tenantId       the tenant ID of the requesting user
     * @throws IllegalArgumentException if session not found or unauthorized
     */
    @Transactional
    public void terminateSession(UUID sessionId, User requestingUser, String tenantId) {
        logger.trace()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.TRACE_TERMINATE_SESSION_START)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .detail(AppConstants.DETAIL_REQUESTING_USER_ID, requestingUser.getId().toString())
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        // Load session from database
        logger.trace()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.TRACE_TERMINATE_SESSION_LOADING_SESSION)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .log();

        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionId(sessionId);
        
        if (sessionOpt.isEmpty()) {
            logger.error()
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.ERROR_TERMINATE_SESSION_NOT_FOUND)
                    .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                    .log();
            throw new IllegalArgumentException(AppConstants.SESSION_NOT_FOUND);
        }

        UserSession session = sessionOpt.get();
        
        logger.debug()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.DEBUG_TERMINATE_SESSION_FOUND)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .detail(AppConstants.DETAIL_TARGET_USER_ID, session.getUserId().toString())
                .log();

        // Validate tenant authorization
        logger.trace()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.TRACE_TERMINATE_SESSION_CHECKING_TENANT)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                .log();

        if (!tenantId.equals(session.getTenantId())) {
            logger.error()
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.ERROR_TERMINATE_SESSION_TENANT_MISMATCH)
                    .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                    .detail(AppConstants.DETAIL_TENANT_ID, tenantId)
                    .detail("sessionTenantId", session.getTenantId())
                    .log();
            throw new IllegalArgumentException(AppConstants.SESSION_UNAUTHORIZED_TERMINATION);
        }

        logger.debug()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.DEBUG_TERMINATE_SESSION_TENANT_MATCH)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .log();

        // Delete the refresh token
        logger.trace()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.TRACE_TERMINATE_SESSION_DELETING_REFRESH_TOKEN)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .log();

        try {
            refreshTokenService.deleteByToken(session.getRefreshToken());
            
            logger.debug()
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.DEBUG_TERMINATE_SESSION_REFRESH_TOKEN_DELETED)
                    .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.ERROR_TERMINATE_SESSION_DELETE_TOKEN)
                    .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                    .log();
            throw new RuntimeException(AppConstants.SESSION_TERMINATION_FAILED, e);
        }

        // Get target user information before deleting session
        User targetUser = userRepository.findById(session.getUserId())
                .orElse(null);

        // Delete the session
        logger.trace()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.TRACE_TERMINATE_SESSION_DELETING_SESSION)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .log();

        try {
            userSessionRepository.delete(session);
            
            logger.debug()
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.DEBUG_TERMINATE_SESSION_SESSION_DELETED)
                    .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                    .log();
        } catch (Exception e) {
            logger.error(e)
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.ERROR_TERMINATE_SESSION_DELETE_SESSION)
                    .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                    .log();
            throw new RuntimeException(AppConstants.SESSION_TERMINATION_FAILED, e);
        }

        // Send audit log event
        sendAuditLogEvent(session, requestingUser, targetUser, tenantId);

        // Send WebSocket notification to the specific session
        sendTerminationNotification(sessionId, session.getUserId());

        logger.info()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.INFO_TERMINATE_SESSION_SUCCESS)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .detail(AppConstants.DETAIL_TARGET_USER_ID, session.getUserId().toString())
                .detail(AppConstants.DETAIL_REQUESTING_USER_ID, requestingUser.getId().toString())
                .log();

        logger.trace()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.TRACE_TERMINATE_SESSION_COMPLETED)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .log();
    }

    /**
     * Sends a WebSocket notification to the specific session that it has been terminated.
     * 
     * <p>The notification is sent to the session-specific queue destination:
     * /user/{sessionId}/queue/session-termination
     * 
     * <p>If the session is not connected via WebSocket, the notification will be silently
     * ignored (no error thrown), as the session deletion itself is the critical action.
     *
     * @param sessionId the UUID of the session that was terminated
     * @param userId the UUID of the user who owns the session (for logging)
     */
    private void sendTerminationNotification(UUID sessionId, UUID userId) {
        logger.trace()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.TRACE_TERMINATE_SESSION_SENDING_NOTIFICATION)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .detail(AppConstants.DETAIL_TARGET_USER_ID, userId.toString())
                .log();

        logger.info()
                .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                .message(AppConstants.INFO_WEBSOCKET_NOTIFICATION_ATTEMPT)
                .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                .detail(AppConstants.DETAIL_TARGET_USER_ID, userId.toString())
                .detail("destination", AppConstants.WEBSOCKET_SESSION_TERMINATION_DEST)
                .log();

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("type", AppConstants.WEBSOCKET_TERMINATION_TYPE);
            notification.put("message", AppConstants.WEBSOCKET_TERMINATION_MESSAGE);
            notification.put("reason", AppConstants.WEBSOCKET_TERMINATION_REASON_ADMIN);
            notification.put("timestamp", System.currentTimeMillis());

            // Send to session-specific destination: /user/{sessionId}/queue/session-termination
            String destination = AppConstants.WEBSOCKET_SESSION_TERMINATION_DEST;
            
            logger.info()
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.INFO_WEBSOCKET_CALLING_SEND)
                    .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                    .detail("destination", destination)
                    .detail("notificationType", notification.get("type"))
                    .log();
            
            messagingTemplate.convertAndSendToUser(
                    sessionId.toString(),
                    destination,
                    notification
            );

            logger.info()
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.INFO_WEBSOCKET_NOTIFICATION_SENT)
                    .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                    .detail(AppConstants.DETAIL_TARGET_USER_ID, userId.toString())
                    .detail(AppConstants.DETAIL_NOTIFICATION_SENT, true)
                    .log();

        } catch (Exception e) {
            // Log the error but don't fail the termination
            // The session is already deleted, WebSocket is just a courtesy notification
            logger.error(e)
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.ERROR_WEBSOCKET_NOTIFICATION_FAILED)
                    .detail(AppConstants.DETAIL_SESSION_ID, sessionId.toString())
                    .detail(AppConstants.DETAIL_TARGET_USER_ID, userId.toString())
                    .detail(AppConstants.DETAIL_WEBSOCKET_CONNECTED, false)
                    .detail("errorMessage", e.getMessage())
                    .log();
        }
    }

    /**
     * Sends an audit log event for session termination.
     * 
     * <p>Creates a comprehensive audit event capturing:
     * <ul>
     *   <li>Actor information: User who terminated the session</li>
     *   <li>Object information: Session ID that was terminated</li>
     *   <li>Target information: User whose session was terminated</li>
     *   <li>State information: Session details before termination (in stateAfter)</li>
     *   <li>Context: IP address and user agent from the terminated session</li>
     * </ul>
     * 
     * <p>The audit event uses {@code stateAfter} to record the terminated session details,
     * while {@code stateBefore} is null since there is no prior state to capture.
     * The {@code targetObject} is overridden via custom metadata to reference the user
     * whose session was terminated, not the session itself.
     *
     * @param session        the session that was terminated
     * @param requestingUser the user who requested the termination (actor)
     * @param targetUser     the user whose session was terminated (can be null if user not found)
     * @param tenantId       the tenant ID for multi-tenant isolation
     */
    private void sendAuditLogEvent(UserSession session, User requestingUser, User targetUser, String tenantId) {
        try {
            // Build stateAfter with session details (the terminated session data)
            ObjectNode stateAfter = objectMapper.createObjectNode();
            stateAfter.put("sessionId", session.getSessionId().toString());
            stateAfter.put("userId", session.getUserId().toString());
            if (targetUser != null) {
                stateAfter.put("username", targetUser.getUsername());
            }
            String sessionIpAddress = session.getIpAddress() != null ? session.getIpAddress() : "unknown";
            stateAfter.put("ipAddress", sessionIpAddress);
            stateAfter.put("device", session.getDevice() != null ? session.getDevice() : "unknown");
            stateAfter.put("createdAt", session.getCreatedAt());
            stateAfter.put("lastActivityAt", session.getLastActivityAt());
            
            // Get location from IP address and add to stateAfter
            if (!sessionIpAddress.equals("unknown")) {
                String location = ipGeolocationService.getLocation(sessionIpAddress);
                if (location != null) {
                    stateAfter.put("location", location);
                }
            }

            // Build the audit message
            String targetUsername = targetUser != null ? targetUser.getUsername() : "unknown";
            String message = String.format(AppConstants.AUDIT_MESSAGE_SESSION_TERMINATED, targetUsername);

            // Get user agent from the terminated session (IP address already retrieved above)
            String sessionUserAgent = session.getUserAgent() != null ? session.getUserAgent() : "unknown";

            // Create audit log event for session termination
            // objectId = userId (whose session was terminated) - this is used for targetObject
            // objectType = "user" - this makes targetObject.type = "user"
            // identifier = targetUsername - this makes targetObject.objectName = username
            // The actual sessionId is stored in stateAfter
            AuditLogKafkaEvent auditEvent = AuditLogEventBuilder.createEventWithTenant(
                    requestingUser.getId(),
                    AppConstants.AUDIT_ACTOR_TYPE_USER,
                    requestingUser.getUsername(),
                    tenantId,
                    session.getUserId(),
                    AppConstants.AUDIT_TARGET_OBJECT_TYPE_USER,
                    targetUsername,
                    AppConstants.AUDIT_EVENT_TERMINATE,
                    AppConstants.SUCCESS_EVENT_TYPE,
                    AppConstants.EVENT_RISK_HIGH,
                    sessionIpAddress,
                    sessionUserAgent,
                    message,
                    null,
                    stateAfter,
                    null,
                    null
            );

            // Send the audit event
            auditLogEventService.sendAuditEvent(auditEvent);

            logger.debug()
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.DEBUG_AUDIT_LOG_SENT_SESSION_TERMINATION)
                    .detail(AppConstants.DETAIL_SESSION_ID, session.getSessionId().toString())
                    .detail(AppConstants.DETAIL_REQUESTING_USER_ID, requestingUser.getId().toString())
                    .detail(AppConstants.DETAIL_TARGET_USER_ID, session.getUserId().toString())
                    .log();

        } catch (Exception e) {
            logger.error(e)
                    .category(AppConstants.LOG_CATEGORY_SESSION_TERMINATION)
                    .message(AppConstants.ERROR_AUDIT_LOG_FAILED_SESSION_TERMINATION)
                    .detail(AppConstants.DETAIL_SESSION_ID, session.getSessionId().toString())
                    .log();
        }
    }
}
