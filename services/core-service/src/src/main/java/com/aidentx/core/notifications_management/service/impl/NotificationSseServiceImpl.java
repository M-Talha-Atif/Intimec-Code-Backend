package com.aidentx.core.notifications_management.service.impl;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.model.dto.NotificationSseDto;
import com.aidentx.core.notifications_management.service.NotificationSseService;
import com.aidentx.core.shared.security.JwtUtil;
import com.aidentx.core.shared.security.SecurityConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of NotificationSseService for handling SSE connections.
 * <p>
 * This service manages Server-Sent Event connections for real-time notifications:
 * <ul>
 *   <li>Maintains active SSE connections per user</li>
 *   <li>Sends real-time notifications to connected users</li>
 *   <li>Handles connection lifecycle and cleanup</li>
 *   <li>Extracts user ID from JWT token</li>
 * </ul>
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-25
 */
@Service
public class NotificationSseServiceImpl implements NotificationSseService {
    
    private final IUnifiedLogger logger;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    
    // Map to store active SSE connections per user ID
    private final ConcurrentHashMap<String, SseEmitter> activeConnections = new ConcurrentHashMap<>();
    
    // SSE connection timeout (30 minutes)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;
    
    /**
     * Constructor for NotificationSseServiceImpl.
     * 
     * @param loggerFactory factory for creating unified loggers
     * @param objectMapper Jackson ObjectMapper for JSON serialization
     * @param jwtUtil utility for JWT token operations
     */
    public NotificationSseServiceImpl(UnifiedLoggerFactory loggerFactory, ObjectMapper objectMapper, JwtUtil jwtUtil) {
        this.logger = loggerFactory.getLogger("NotificationSseService", AppConstants.LOG_CATEGORY_NOTIFICATIONS_SSE_SERVICE);
        this.objectMapper = objectMapper;
        this.jwtUtil = jwtUtil;
        
        // Start heartbeat scheduler to keep connections alive
        startHeartbeatScheduler();
    }
    
    /**
     * Creates an SSE connection for the current user.
     * User ID is extracted from JWT token.
     * 
     * @return SseEmitter for streaming notifications
     */
    @Override
    public SseEmitter createSseConnection() {
        String userId = getCurrentUserId();
        
        logger.info().message(AppConstants.LOG_SSE_CONNECTION_CREATED)
                .detail("userId", userId)
                .objectType(AppConstants.OBJECT_TYPE_SSE)
                .log();
        
        // Remove existing connection if any
        if (activeConnections.containsKey(userId)) {
            removeSseConnection(userId);
        }
        
        // Create new SSE emitter
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // Set up connection event handlers
        emitter.onCompletion(() -> {
            logger.info().message(AppConstants.LOG_SSE_CONNECTION_COMPLETED)
                    .detail("userId", userId)
                    .objectType(AppConstants.OBJECT_TYPE_SSE)
                    .log();
            activeConnections.remove(userId);
        });
        
        emitter.onTimeout(() -> {
            logger.info().message(AppConstants.LOG_SSE_CONNECTION_TIMED_OUT)
                    .detail("userId", userId)
                    .objectType(AppConstants.OBJECT_TYPE_SSE)
                    .log();
            activeConnections.remove(userId);
        });
        
        emitter.onError((throwable) -> {
            logger.error(throwable).message(AppConstants.LOG_SSE_CONNECTION_ERROR)
                    .detail("userId", userId)
                    .objectType(AppConstants.OBJECT_TYPE_SSE)
                    .log();
            activeConnections.remove(userId);
        });
        
        // Store the connection
        activeConnections.put(userId, emitter);
        
        // Send initial connection confirmation
        try {
            emitter.send(SseEmitter.event()
                    .name("connection")
                    .data("{\"status\":\"connected\",\"timestamp\":" + System.currentTimeMillis() + "}"));
        } catch (IOException e) {
            logger.error(e).message(AppConstants.LOG_SSE_INITIAL_MESSAGE_FAILED)
                    .detail("userId", userId)
                    .objectType(AppConstants.OBJECT_TYPE_SSE)
                    .log();
        }
        
        return emitter;
    }
    
    /**
     * Sends a real-time notification to a specific user.
     * 
     * @param userId the user ID to send notification to
     * @param notification the notification DTO to send
     */
    @Override
    public void sendNotificationToUser(String userId, NotificationSseDto notification) {
        SseEmitter emitter = activeConnections.get(userId);
        
        if (emitter != null) {
            try {
                String jsonData = objectMapper.writeValueAsString(notification);
                
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(jsonData));
                
                logger.info().message(AppConstants.LOG_SSE_NOTIFICATION_SENT)
                        .detail("userId", userId)
                        .detail("notificationData", jsonData)
                        .objectType(AppConstants.OBJECT_TYPE_SSE)
                        .log();
                
            } catch (IOException e) {
                logger.error(e).message(AppConstants.LOG_SSE_NOTIFICATION_FAILED)
                        .detail("userId", userId)
                        .objectType(AppConstants.OBJECT_TYPE_SSE)
                        .log();
                
                // Remove failed connection
                removeSseConnection(userId);
            } catch (Exception e) {
                logger.error(e).message(AppConstants.LOG_SSE_SERIALIZATION_FAILED)
                        .detail("userId", userId)
                        .objectType(AppConstants.OBJECT_TYPE_SSE)
                        .log();
            }
        } else {
            logger.debug().message(AppConstants.LOG_SSE_NO_ACTIVE_CONNECTION)
                    .detail("userId", userId)
                    .objectType(AppConstants.OBJECT_TYPE_SSE)
                    .log();
        }
    }
    
    /**
     * Removes SSE connection for a user.
     * 
     * @param userId the user ID to remove connection for
     */
    @Override
    public void removeSseConnection(String userId) {
        SseEmitter emitter = activeConnections.remove(userId);
        
        if (emitter != null) {
            try {
                emitter.complete();
                
                logger.info().message(AppConstants.LOG_SSE_CONNECTION_REMOVED)
                        .detail("userId", userId)
                        .objectType(AppConstants.OBJECT_TYPE_SSE)
                        .log();
                
            } catch (Exception e) {
                logger.error(e).message(AppConstants.LOG_SSE_CONNECTION_COMPLETION_ERROR)
                        .detail("userId", userId)
                        .objectType(AppConstants.OBJECT_TYPE_SSE)
                        .log();
            }
        }
    }
    
    /**
     * Extracts the current user ID from JWT token.
     * First checks Authorization header, then falls back to cookies for SSE compatibility.
     * 
     * @return the user ID from JWT 'sub' claim
     */
    private String getCurrentUserId() {
        try {
            // Get authentication from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !AppConstants.SECURITY_ANONYMOUS_USER.equals(authentication.getPrincipal())) {

                // Try to get JWT token from current request
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();
                if (requestAttributes != null) {
                    HttpServletRequest request = requestAttributes.getRequest();
                    
                    // First try Authorization header
                    String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
                    String token = null;
                    
                    if (authHeader != null && authHeader.startsWith(AppConstants.SECURITY_BEARER_PREFIX)) {
                        token = authHeader.substring(AppConstants.BEARER_PREFIX_LENGTH); // Remove "Bearer " prefix
                    } else {
                        // Fallback to cookies for SSE connections
                        Cookie[] cookies = request.getCookies();
                        if (cookies != null) {
                            for (Cookie cookie : cookies) {
                                if (AppConstants.AUTH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                                    token = cookie.getValue();
                                    break;
                                }
                            }
                        }
                    }
                    
                    if (token != null) {
                        // Extract user ID from JWT token using JwtUtil
                        String userId = jwtUtil.extractUserId(token);
                        
                        logger.debug().message(AppConstants.LOG_USER_ID_EXTRACTED)
                                .detail("userId", userId)
                                .objectType(AppConstants.OBJECT_TYPE_SSE)
                                .log();
                        
                        return userId;
                    }
                }

                // Fallback to authentication name if JWT extraction fails
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.warn().message(AppConstants.LOG_SSE_USER_ID_EXTRACTION_FAILED)
                    .objectType(AppConstants.OBJECT_TYPE_SSE)
                    .log();
        }
        
        throw new RuntimeException(AppConstants.LOG_SSE_USER_ID_EXTRACTION_ERROR);
    }
    
    /**
     * Starts a scheduled task to send heartbeat messages to keep SSE connections alive.
     */
    private void startHeartbeatScheduler() {
        scheduler.scheduleAtFixedRate(() -> {
            for (String userId : activeConnections.keySet()) {
                SseEmitter emitter = activeConnections.get(userId);
                if (emitter != null) {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("heartbeat")
                                .data("{\"timestamp\":" + System.currentTimeMillis() + "}"));
                        
                        logger.debug().message(AppConstants.LOG_SSE_HEARTBEAT_SENT)
                                .detail("userId", userId)
                                .objectType(AppConstants.OBJECT_TYPE_SSE)
                                .log();
                        
                    } catch (IOException e) {
                        logger.warn().message(AppConstants.LOG_SSE_HEARTBEAT_FAILED)
                                .detail("userId", userId)
                                .objectType(AppConstants.OBJECT_TYPE_SSE)
                                .log();
                        
                        removeSseConnection(userId);
                    }
                }
            }
        }, 30, 30, TimeUnit.SECONDS); // Send heartbeat every 30 seconds
    }
}
