package com.aidentx.core.notifications_management.service.impl;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.enums.NotificationType;
import com.aidentx.core.notifications_management.model.dto.MarkNotificationViewedResponseDto;
import com.aidentx.core.notifications_management.model.dto.UserNotificationResponseDto;
import com.aidentx.core.notifications_management.model.entity.Notification;
import com.aidentx.core.notifications_management.model.entity.UserNotification;
import com.aidentx.core.notifications_management.repository.NotificationRepository;
import com.aidentx.core.notifications_management.repository.UserNotificationRepository;
import com.aidentx.core.notifications_management.service.UserNotificationService;
import com.aidentx.core.shared.security.JwtUtil;
import com.aidentx.core.shared.security.SecurityConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of UserNotificationService for managing user notifications.
 * <p>
 * This service handles retrieving user notifications by type and marking
 * notifications as viewed. It extracts user ID from JWT tokens for authentication.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-18
 */
@Service
@Transactional
public class UserNotificationServiceImpl implements UserNotificationService {
    
    private final UserNotificationRepository userNotificationRepository;
    private final NotificationRepository notificationRepository;
    private final JwtUtil jwtUtil;
    private final IUnifiedLogger logger;
    
    // Constants for logging and error messages
    private static final String LOG_CATEGORY = "NOTIFICATIONS_MANAGEMENT";
    private static final String OBJECT_TYPE_USER_NOTIFICATION = "USER_NOTIFICATION";
    private static final String SECURITY_ANONYMOUS_USER = "anonymousUser";
    private static final String SECURITY_BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    
    // Log messages
    private static final String LOG_RETRIEVING_USER_NOTIFICATIONS = "Retrieving user notifications by type";
    private static final String LOG_USER_NOTIFICATIONS_RETRIEVED = "Successfully retrieved user notifications";
    private static final String LOG_MARKING_NOTIFICATION_VIEWED = "Marking notification as viewed";
    private static final String LOG_NOTIFICATION_MARKED_VIEWED = "Successfully marked notification as viewed";
    private static final String LOG_NOTIFICATION_NOT_FOUND = "Notification not found for user";
    private static final String LOG_FAILED_TO_EXTRACT_USER_ID = "Failed to extract user ID from JWT token";
    private static final String LOG_UNAUTHORIZED_ACCESS = "Unauthorized access attempt";
    
    // Response messages
    private static final String MSG_NOTIFICATION_MARKED_VIEWED = "Notification marked as viewed successfully";
    private static final String MSG_NOTIFICATION_NOT_FOUND = "Notification not found or already viewed";
    private static final String MSG_UNAUTHORIZED = "User authentication required";
    
    /**
     * Constructor for UserNotificationServiceImpl.
     * 
     * @param userNotificationRepository repository for user notification operations
     * @param notificationRepository repository for notification operations  
     * @param jwtUtil utility for JWT token operations
     * @param loggerFactory factory for creating unified loggers
     */
    public UserNotificationServiceImpl(UserNotificationRepository userNotificationRepository,
                                     NotificationRepository notificationRepository,
                                     JwtUtil jwtUtil,
                                     UnifiedLoggerFactory loggerFactory) {
        this.userNotificationRepository = userNotificationRepository;
        this.notificationRepository = notificationRepository;
        this.jwtUtil = jwtUtil;
        this.logger = loggerFactory.getLogger("UserNotificationService", LOG_CATEGORY);
    }
    
    /**
     * Retrieves all notifications of a specific type for the current user.
     * 
     * @param type the notification type to filter by
     * @return a list of user notifications of the specified type
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserNotificationResponseDto> getUserNotificationsByType(NotificationType type) {
        logger.info().message(LOG_RETRIEVING_USER_NOTIFICATIONS)
                .detail("notificationType", type.name())
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            // Extract user ID from JWT token
            String userIdStr = extractUserIdFromCurrentRequest();
            if (userIdStr == null) {
                logger.warn().message(LOG_UNAUTHORIZED_ACCESS).log();
                throw new RuntimeException(MSG_UNAUTHORIZED);
            }
            
            UUID userId = UUID.fromString(userIdStr);
            
            // Get user notifications by type
            List<UserNotification> userNotifications = userNotificationRepository
                    .findByUserIdAndNotificationType(userId, type);
            
            // Get notification details
            List<UUID> notificationIds = userNotifications.stream()
                    .map(UserNotification::getNotificationId)
                    .collect(Collectors.toList());
            
            Map<UUID, Notification> notificationMap = notificationRepository
                    .findAllById(notificationIds)
                    .stream()
                    .collect(Collectors.toMap(Notification::getId, Function.identity()));
            
            // Convert to DTOs
            List<UserNotificationResponseDto> responseDtos = userNotifications.stream()
                    .map(userNotification -> {
                        Notification notification = notificationMap.get(userNotification.getNotificationId());
                        if (notification != null) {
                            return new UserNotificationResponseDto(
                                    userNotification.getId(),
                                    notification.getId(),
                                    notification.getDescription(),
                                    notification.getDetails(),
                                    notification.getType(),
                                    notification.getOperation(),
                                    userNotification.getIsViewed(),
                                    userNotification.getIsPinned()
                            );
                        }
                        return null;
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
            
            logger.info().message(LOG_USER_NOTIFICATIONS_RETRIEVED)
                    .detail("notificationType", type.name())
                    .detail("count", responseDtos.size())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return responseDtos;
            
        } catch (Exception e) {
            logger.error(e).message("Failed to retrieve user notifications")
                    .detail("notificationType", type.name())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            throw e;
        }
    }
    
    /**
     * Marks a specific notification as viewed for the current user.
     * 
     * @param notificationId the UUID of the notification to mark as viewed
     * @return response indicating success or failure
     */
    @Override
    @Transactional
    public MarkNotificationViewedResponseDto markNotificationAsViewed(UUID notificationId) {
        logger.info().message(LOG_MARKING_NOTIFICATION_VIEWED)
                .detail("notificationId", notificationId.toString())
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            // Extract user ID from JWT token
            String userIdStr = extractUserIdFromCurrentRequest();
            if (userIdStr == null) {
                logger.warn().message(LOG_UNAUTHORIZED_ACCESS).log();
                return new MarkNotificationViewedResponseDto(false, MSG_UNAUTHORIZED);
            }
            
            UUID userId = UUID.fromString(userIdStr);
            
            // Mark notification as viewed
            int updatedCount = userNotificationRepository
                    .markAsViewedByUserIdAndNotificationId(userId, notificationId);
            
            if (updatedCount > 0) {
                logger.info().message(LOG_NOTIFICATION_MARKED_VIEWED)
                        .detail("notificationId", notificationId.toString())
                        .detail("userId", userId.toString())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                return new MarkNotificationViewedResponseDto(true, MSG_NOTIFICATION_MARKED_VIEWED);
            } else {
                logger.warn().message(LOG_NOTIFICATION_NOT_FOUND)
                        .detail("notificationId", notificationId.toString())
                        .detail("userId", userId.toString())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                return new MarkNotificationViewedResponseDto(false, MSG_NOTIFICATION_NOT_FOUND);
            }
            
        } catch (Exception e) {
            logger.error(e).message("Failed to mark notification as viewed")
                    .detail("notificationId", notificationId.toString())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return new MarkNotificationViewedResponseDto(false, "Failed to mark notification as viewed: " + e.getMessage());
        }
    }

    /**
     * Pins a specific notification for the current user.
     * 
     * @param notificationId the UUID of the notification to pin
     * @return response indicating success or failure
     */
    @Override
    @Transactional
    public MarkNotificationViewedResponseDto pinNotification(UUID notificationId) {
        logger.info().message(AppConstants.LOG_PINNING_NOTIFICATION)
                .detail("notificationId", notificationId.toString())
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            // Extract user ID from JWT token
            String userIdStr = extractUserIdFromCurrentRequest();
            if (userIdStr == null) {
                logger.warn().message(LOG_UNAUTHORIZED_ACCESS).log();
                return new MarkNotificationViewedResponseDto(false, MSG_UNAUTHORIZED);
            }
            
            UUID userId = UUID.fromString(userIdStr);
            
            // Pin notification
            int updatedCount = userNotificationRepository
                    .updatePinnedStatusByUserIdAndNotificationId(userId, notificationId, true);
            
            if (updatedCount > 0) {
                logger.info().message(AppConstants.LOG_NOTIFICATION_PINNED_SUCCESS)
                        .detail("notificationId", notificationId.toString())
                        .detail("userId", userId.toString())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                return new MarkNotificationViewedResponseDto(true, AppConstants.MSG_NOTIFICATION_PINNED_SUCCESS);
            } else {
                logger.warn().message(AppConstants.LOG_NOTIFICATION_PIN_NOT_FOUND)
                        .detail("notificationId", notificationId.toString())
                        .detail("userId", userId.toString())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                return new MarkNotificationViewedResponseDto(false, AppConstants.MSG_NOTIFICATION_NOT_FOUND_FOR_USER);
            }
            
        } catch (Exception e) {
            logger.error(e).message("Failed to pin notification")
                    .detail("notificationId", notificationId.toString())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return new MarkNotificationViewedResponseDto(false, "Failed to pin notification: " + e.getMessage());
        }
    }
    
    /**
     * Unpins a specific notification for the current user.
     * 
     * @param notificationId the UUID of the notification to unpin
     * @return response indicating success or failure
     */
    @Override
    @Transactional
    public MarkNotificationViewedResponseDto unpinNotification(UUID notificationId) {
        logger.info().message(AppConstants.LOG_UNPINNING_NOTIFICATION)
                .detail("notificationId", notificationId.toString())
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            // Extract user ID from JWT token
            String userIdStr = extractUserIdFromCurrentRequest();
            if (userIdStr == null) {
                logger.warn().message(LOG_UNAUTHORIZED_ACCESS).log();
                return new MarkNotificationViewedResponseDto(false, MSG_UNAUTHORIZED);
            }
            
            UUID userId = UUID.fromString(userIdStr);
            
            // Unpin notification
            int updatedCount = userNotificationRepository
                    .updatePinnedStatusByUserIdAndNotificationId(userId, notificationId, false);
            
            if (updatedCount > 0) {
                logger.info().message(AppConstants.LOG_NOTIFICATION_UNPINNED_SUCCESS)
                        .detail("notificationId", notificationId.toString())
                        .detail("userId", userId.toString())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                return new MarkNotificationViewedResponseDto(true, AppConstants.MSG_NOTIFICATION_UNPINNED_SUCCESS);
            } else {
                logger.warn().message(AppConstants.LOG_NOTIFICATION_UNPIN_NOT_FOUND)
                        .detail("notificationId", notificationId.toString())
                        .detail("userId", userId.toString())
                        .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                        .log();
                
                return new MarkNotificationViewedResponseDto(false, AppConstants.MSG_NOTIFICATION_NOT_FOUND_FOR_USER);
            }
            
        } catch (Exception e) {
            logger.error(e).message("Failed to unpin notification")
                    .detail("notificationId", notificationId.toString())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return new MarkNotificationViewedResponseDto(false, "Failed to unpin notification: " + e.getMessage());
        }
    }
    
    /**
     * Retrieves all pinned notifications for the current user.
     * 
     * @return a list of pinned user notifications
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserNotificationResponseDto> getPinnedNotifications() {
        logger.info().message(AppConstants.LOG_RETRIEVING_PINNED_NOTIFICATIONS)
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            // Extract user ID from JWT token
            String userIdStr = extractUserIdFromCurrentRequest();
            if (userIdStr == null) {
                logger.warn().message(LOG_UNAUTHORIZED_ACCESS).log();
                throw new RuntimeException(MSG_UNAUTHORIZED);
            }
            
            UUID userId = UUID.fromString(userIdStr);
            
            // Get pinned user notifications
            List<UserNotification> userNotifications = userNotificationRepository
                    .findByUserIdAndIsPinned(userId, true);
            
            // Get notification details
            List<UUID> notificationIds = userNotifications.stream()
                    .map(UserNotification::getNotificationId)
                    .collect(Collectors.toList());
            
            Map<UUID, Notification> notificationMap = notificationRepository
                    .findAllById(notificationIds)
                    .stream()
                    .collect(Collectors.toMap(Notification::getId, Function.identity()));
            
            // Convert to DTOs
            List<UserNotificationResponseDto> responseDtos = userNotifications.stream()
                    .map(userNotification -> {
                        Notification notification = notificationMap.get(userNotification.getNotificationId());
                        if (notification != null) {
                            return new UserNotificationResponseDto(
                                    userNotification.getId(),
                                    notification.getId(),
                                    notification.getDescription(),
                                    notification.getDetails(),
                                    notification.getType(),
                                    notification.getOperation(),
                                    userNotification.getIsViewed(),
                                    userNotification.getIsPinned()
                            );
                        }
                        return null;
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
            
            logger.info().message(AppConstants.LOG_PINNED_NOTIFICATIONS_RETRIEVED)
                    .detail("count", responseDtos.size())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return responseDtos;
            
        } catch (Exception e) {
            logger.error(e).message("Failed to retrieve pinned notifications")
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            throw e;
        }
    }
    
    /**
     * Retrieves all pinned notifications of a specific type for the current user.
     * 
     * @param type the notification type to filter by
     * @return a list of pinned user notifications of the specified type
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserNotificationResponseDto> getPinnedNotificationsByType(NotificationType type) {
        logger.info().message(AppConstants.LOG_RETRIEVING_PINNED_NOTIFICATIONS)
                .detail("notificationType", type.name())
                .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                .log();
        
        try {
            // Extract user ID from JWT token
            String userIdStr = extractUserIdFromCurrentRequest();
            if (userIdStr == null) {
                logger.warn().message(LOG_UNAUTHORIZED_ACCESS).log();
                throw new RuntimeException(MSG_UNAUTHORIZED);
            }
            
            UUID userId = UUID.fromString(userIdStr);
            
            // Get pinned user notifications by type
            List<UserNotification> userNotifications = userNotificationRepository
                    .findByUserIdAndNotificationTypeAndIsPinned(userId, type, true);
            
            // Get notification details
            List<UUID> notificationIds = userNotifications.stream()
                    .map(UserNotification::getNotificationId)
                    .collect(Collectors.toList());
            
            Map<UUID, Notification> notificationMap = notificationRepository
                    .findAllById(notificationIds)
                    .stream()
                    .collect(Collectors.toMap(Notification::getId, Function.identity()));
            
            // Convert to DTOs
            List<UserNotificationResponseDto> responseDtos = userNotifications.stream()
                    .map(userNotification -> {
                        Notification notification = notificationMap.get(userNotification.getNotificationId());
                        if (notification != null) {
                            return new UserNotificationResponseDto(
                                    userNotification.getId(),
                                    notification.getId(),
                                    notification.getDescription(),
                                    notification.getDetails(),
                                    notification.getType(),
                                    notification.getOperation(),
                                    userNotification.getIsViewed(),
                                    userNotification.getIsPinned()
                            );
                        }
                        return null;
                    })
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());
            
            logger.info().message(AppConstants.LOG_PINNED_NOTIFICATIONS_RETRIEVED)
                    .detail("notificationType", type.name())
                    .detail("count", responseDtos.size())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            
            return responseDtos;
            
        } catch (Exception e) {
            logger.error(e).message("Failed to retrieve pinned notifications by type")
                    .detail("notificationType", type.name())
                    .objectType(OBJECT_TYPE_USER_NOTIFICATION)
                    .log();
            throw e;
        }
    }
    
    /**
     * Extracts user ID from the current JWT token in the request.
     * 
     * @return the user ID string, or null if extraction fails
     */
    private String extractUserIdFromCurrentRequest() {
        try {
            // Get authentication from SecurityContext
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() &&
                    !SECURITY_ANONYMOUS_USER.equals(authentication.getPrincipal())) {

                // Try to get JWT token from current request
                ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder
                        .getRequestAttributes();
                if (requestAttributes != null) {
                    String authHeader = requestAttributes.getRequest()
                            .getHeader(SecurityConstants.AUTHORIZATION_HEADER);
                    if (authHeader != null && authHeader.startsWith(SECURITY_BEARER_PREFIX)) {
                        String token = authHeader.substring(BEARER_PREFIX_LENGTH); // Remove "Bearer " prefix

                        // Extract user ID from JWT token using JwtUtil
                        return jwtUtil.extractUserId(token);
                    }
                }

                // Fallback to authentication name if JWT extraction fails
                return authentication.getName();
            }
        } catch (Exception e) {
            logger.warn().message(LOG_FAILED_TO_EXTRACT_USER_ID).log();
        }
        return null;
    }
}
