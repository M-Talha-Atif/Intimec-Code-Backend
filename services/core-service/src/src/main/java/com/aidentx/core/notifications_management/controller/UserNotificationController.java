package com.aidentx.core.notifications_management.controller;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.enums.NotificationType;
import com.aidentx.core.notifications_management.model.dto.MarkNotificationViewedResponseDto;
import com.aidentx.core.notifications_management.model.dto.UserNotificationResponseDto;
import com.aidentx.core.notifications_management.service.UserNotificationService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for user notification operations.
 * <p>
 * This controller provides endpoints for users to retrieve their notifications
 * by type and mark notifications as viewed. User authentication is handled
 * through JWT tokens.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@RestController
@RequestMapping("/api/v1/user-notifications")
public class UserNotificationController {
    
    private final UserNotificationService userNotificationService;
    private final IUnifiedLogger logger;
    
    // Constants for logging
    private static final String LOG_CATEGORY = "NOTIFICATIONS_MANAGEMENT";
    private static final String OBJECT_TYPE_USER_NOTIFICATION = "USER_NOTIFICATION";
    
    // Log messages
    private static final String LOG_GET_ANNOUNCEMENTS_REQUEST = "GET request for user announcements";
    private static final String LOG_GET_ALERTS_REQUEST = "GET request for user alerts";
    private static final String LOG_GET_NOTIFICATIONS_REQUEST = "GET request for user notifications";
    private static final String LOG_MARK_VIEWED_REQUEST = "PUT request to mark notification as viewed";
    private static final String LOG_RESPONSE_SUCCESS = "Successful response sent";
    private static final String LOG_RESPONSE_ERROR = "Error response sent";
    
    /**
     * Constructor for UserNotificationController.
     * 
     * @param userNotificationService service for user notification operations
     * @param loggerFactory factory for creating unified loggers
     */
    public UserNotificationController(UserNotificationService userNotificationService,
                                    UnifiedLoggerFactory loggerFactory) {
        this.userNotificationService = userNotificationService;
        this.logger = loggerFactory.getLogger("UserNotificationController", LOG_CATEGORY);
    }
    
    /**
     * Retrieves all announcements for the current user.
     * User ID is extracted from JWT token.
     * 
     * @return list of user announcements
     */
    /**
     * Retrieves all announcements for the current user.
     * User ID is extracted from JWT token.
     * 
     * @return list of user announcements
     */
    @GetMapping("/announcements")
    public ResponseEntity<List<UserNotificationResponseDto>> getUserAnnouncements() {
        logger.info().message(LOG_GET_ANNOUNCEMENTS_REQUEST)
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            List<UserNotificationResponseDto> announcements = userNotificationService
                    .getUserNotificationsByType(NotificationType.ANNOUNCEMENTS);
            
            logger.info().message(LOG_RESPONSE_SUCCESS)
                    .detail("notificationType", "ANNOUNCEMENTS")
                    .detail("count", announcements.size())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return ResponseEntity.ok(announcements);
            
        } catch (Exception e) {
            logger.error(e).message(LOG_RESPONSE_ERROR)
                    .detail("notificationType", "ANNOUNCEMENTS")
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Retrieves all alerts for the current user.
     * User ID is extracted from JWT token.
     * 
     * @return list of user alerts
     */
    /**
     * Retrieves all alerts for the current user.
     * User ID is extracted from JWT token.
     * 
     * @return list of user alerts
     */
    @GetMapping("/alerts")
    public ResponseEntity<List<UserNotificationResponseDto>> getUserAlerts() {
        logger.info().message(LOG_GET_ALERTS_REQUEST)
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            List<UserNotificationResponseDto> alerts = userNotificationService
                    .getUserNotificationsByType(NotificationType.ALERTS);
            
            logger.info().message(LOG_RESPONSE_SUCCESS)
                    .detail("notificationType", "ALERTS")
                    .detail("count", alerts.size())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return ResponseEntity.ok(alerts);
            
        } catch (Exception e) {
            logger.error(e).message(LOG_RESPONSE_ERROR)
                    .detail("notificationType", "ALERTS")
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Retrieves all general notifications for the current user.
     * User ID is extracted from JWT token.
     * 
     * @return list of user notifications
     */
    /**
     * Retrieves all general notifications for the current user.
     * User ID is extracted from JWT token.
     * 
     * @return list of user notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<UserNotificationResponseDto>> getUserNotifications() {
        logger.info().message(LOG_GET_NOTIFICATIONS_REQUEST)
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();      
        try {
            List<UserNotificationResponseDto> notifications = userNotificationService
                    .getUserNotificationsByType(NotificationType.NOTIFICATIONS);
            
            logger.info().message(LOG_RESPONSE_SUCCESS)
                    .detail("notificationType", "NOTIFICATIONS")
                    .detail("count", notifications.size())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return ResponseEntity.ok(notifications);
            
        } catch (Exception e) {
            logger.error(e).message(LOG_RESPONSE_ERROR)
                    .detail("notificationType", "NOTIFICATIONS")
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Marks a specific notification as viewed for the current user.
     * User ID is extracted from JWT token.
     * 
     * @param notificationId the UUID of the notification to mark as viewed
     * @return response indicating success or failure
     */
    /**
     * Marks a specific notification as viewed for the current user.
     * User ID is extracted from JWT token.
     * 
     * @param notificationId the UUID of the notification to mark as viewed
     * @return response indicating success or failure
     */
    @PutMapping("/mark-viewed/{notificationId}")
    public ResponseEntity<MarkNotificationViewedResponseDto> markNotificationAsViewed(
            @PathVariable UUID notificationId) {
        
        logger.info().message(LOG_MARK_VIEWED_REQUEST)
                .detail("notificationId", notificationId.toString())
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            MarkNotificationViewedResponseDto response = userNotificationService
                    .markNotificationAsViewed(notificationId);
            
            if (response.isSuccess()) {
                logger.info().message(LOG_RESPONSE_SUCCESS)
                        .detail("notificationId", notificationId.toString())
                        .detail("message", response.getMessage())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                return ResponseEntity.ok(response);
            } else {
                logger.warn().message("Notification marking failed")
                        .detail("notificationId", notificationId.toString())
                        .detail("message", response.getMessage())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                if (response.getMessage().contains("Unauthorized") || response.getMessage().contains("authentication")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }
            }
            
        } catch (Exception e) {
            logger.error(e).message(LOG_RESPONSE_ERROR)
                    .detail("notificationId", notificationId.toString())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            MarkNotificationViewedResponseDto errorResponse = new MarkNotificationViewedResponseDto(
                    false, "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Pins a specific notification for the current user.
     * 
     * @param notificationId the UUID of the notification to pin
     * @return response indicating success or failure
     */
    @PutMapping("/{notificationId}/pin")
    public ResponseEntity<MarkNotificationViewedResponseDto> pinNotification(
            @PathVariable UUID notificationId) {
        
        logger.info().message(AppConstants.LOG_PIN_NOTIFICATION_REQUEST)
                .detail("notificationId", notificationId.toString())
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            MarkNotificationViewedResponseDto response = userNotificationService
                    .pinNotification(notificationId);
            
            if (response.isSuccess()) {
                logger.info().message(LOG_RESPONSE_SUCCESS)
                        .detail("notificationId", notificationId.toString())
                        .detail("message", response.getMessage())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                return ResponseEntity.ok(response);
            } else {
                logger.warn().message(AppConstants.LOG_NOTIFICATION_PINNING_FAILED)
                        .detail("notificationId", notificationId.toString())
                        .detail("message", response.getMessage())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                if (response.getMessage().contains("Unauthorized") || response.getMessage().contains("authentication")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }
            }
            
        } catch (Exception e) {
            logger.error(e).message(LOG_RESPONSE_ERROR)
                    .detail("notificationId", notificationId.toString())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            MarkNotificationViewedResponseDto errorResponse = new MarkNotificationViewedResponseDto(
                    false, "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Unpins a specific notification for the current user.
     * 
     * @param notificationId the UUID of the notification to unpin
     * @return response indicating success or failure
     */
    @PutMapping("/{notificationId}/unpin")
    public ResponseEntity<MarkNotificationViewedResponseDto> unpinNotification(
            @PathVariable UUID notificationId) {
        
        logger.info().message(AppConstants.LOG_UNPIN_NOTIFICATION_REQUEST)
                .detail("notificationId", notificationId.toString())
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            MarkNotificationViewedResponseDto response = userNotificationService
                    .unpinNotification(notificationId);
            
            if (response.isSuccess()) {
                logger.info().message(LOG_RESPONSE_SUCCESS)
                        .detail("notificationId", notificationId.toString())
                        .detail("message", response.getMessage())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                return ResponseEntity.ok(response);
            } else {
                logger.warn().message(AppConstants.LOG_NOTIFICATION_UNPINNING_FAILED)
                        .detail("notificationId", notificationId.toString())
                        .detail("message", response.getMessage())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                if (response.getMessage().contains("Unauthorized") || response.getMessage().contains("authentication")) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
                }
            }
            
        } catch (Exception e) {
            logger.error(e).message(LOG_RESPONSE_ERROR)
                    .detail("notificationId", notificationId.toString())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            MarkNotificationViewedResponseDto errorResponse = new MarkNotificationViewedResponseDto(
                    false, "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    /**
     * Retrieves all pinned announcements for the current user.
     * 
     * @return list of pinned user announcements
     */
    @GetMapping("/announcements/pinned")
    public ResponseEntity<List<UserNotificationResponseDto>> getPinnedAnnouncements() {
        logger.info().message(AppConstants.LOG_GET_PINNED_ANNOUNCEMENTS_REQUEST)
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            List<UserNotificationResponseDto> pinnedAnnouncements = userNotificationService
                    .getPinnedNotificationsByType(NotificationType.ANNOUNCEMENTS);
            
            logger.info().message(LOG_RESPONSE_SUCCESS)
                    .detail("notificationType", "ANNOUNCEMENTS")
                    .detail("count", pinnedAnnouncements.size())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return ResponseEntity.ok(pinnedAnnouncements);
            
        } catch (Exception e) {
            logger.error(e).message(LOG_RESPONSE_ERROR)
                    .detail("notificationType", "ANNOUNCEMENTS")
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            if (e.getMessage().contains("Unauthorized") || e.getMessage().contains("authentication")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
    
    /**
     * Retrieves all pinned alerts for the current user.
     * 
     * @return list of pinned user alerts
     */
    @GetMapping("/alerts/pinned")
    public ResponseEntity<List<UserNotificationResponseDto>> getPinnedAlerts() {
        logger.info().message(AppConstants.LOG_GET_PINNED_ALERTS_REQUEST)
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            List<UserNotificationResponseDto> pinnedAlerts = userNotificationService
                    .getPinnedNotificationsByType(NotificationType.ALERTS);
            
            logger.info().message(LOG_RESPONSE_SUCCESS)
                    .detail("notificationType", "ALERTS")
                    .detail("count", pinnedAlerts.size())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return ResponseEntity.ok(pinnedAlerts);
            
        } catch (Exception e) {
            logger.error(e).message(LOG_RESPONSE_ERROR)
                    .detail("notificationType", "ALERTS")
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            if (e.getMessage().contains("Unauthorized") || e.getMessage().contains("authentication")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }
    
    /**
     * Retrieves all pinned notifications for the current user.
     * 
     * @return list of pinned user notifications
     */
    @GetMapping("/notifications/pinned")
    public ResponseEntity<List<UserNotificationResponseDto>> getPinnedNotifications() {
        logger.info().message(AppConstants.LOG_GET_PINNED_NOTIFICATIONS_REQUEST)
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            List<UserNotificationResponseDto> pinnedNotifications = userNotificationService
                    .getPinnedNotificationsByType(NotificationType.NOTIFICATIONS);
            
            logger.info().message(LOG_RESPONSE_SUCCESS)
                    .detail("notificationType", "NOTIFICATIONS")
                    .detail("count", pinnedNotifications.size())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return ResponseEntity.ok(pinnedNotifications);
            
        } catch (Exception e) {
            logger.error(e).message(LOG_RESPONSE_ERROR)
                    .detail("notificationType", "NOTIFICATIONS")
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            if (e.getMessage().contains("Unauthorized") || e.getMessage().contains("authentication")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    /**
     * Marks all old WORKFLOW_APPROVAL_REQUESTED notifications as viewed for the current user.
     * This is a manual cleanup endpoint to handle historical notifications.
     * Only marks notifications older than 1 hour to preserve recent active approvals.
     * User ID is extracted from JWT token.
     * 
     * @return response indicating success or failure
     */
}
