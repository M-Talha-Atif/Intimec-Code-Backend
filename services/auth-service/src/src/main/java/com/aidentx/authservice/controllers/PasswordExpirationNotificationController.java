package com.aidentx.authservice.controllers;

import com.aidentx.authservice.model.entity.User;
import com.aidentx.authservice.service.Policy.PasswordExpirationNotificationService;
import com.aidentx.authservice.service.Auth.AppConstants;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing password expiration notification operations.
 * Provides endpoints for monitoring and manually triggering notification jobs.
 *
 * @author Aidentx
 * @since 1.0
 */
@RestController
@RequestMapping("/api/password-expiration-notifications")
public class PasswordExpirationNotificationController {

    private final IUnifiedLogger logger;
    private final PasswordExpirationNotificationService passwordExpirationNotificationService;

    public PasswordExpirationNotificationController(UnifiedLoggerFactory unifiedLoggerFactory,
                                                  PasswordExpirationNotificationService passwordExpirationNotificationService) {
        this.logger = unifiedLoggerFactory.getLogger("PasswordExpirationNotificationController", AppConstants.LOG_CATEGORY_POLICY_MGMT);
        this.passwordExpirationNotificationService = passwordExpirationNotificationService;
    }

    /**
     * Get the count of users currently requiring password expiration notifications.
     * 
     * @return ResponseEntity with users requiring notification count
     */
    @GetMapping("/users-requiring-notification-count")
    public ResponseEntity<Map<String, Object>> getUsersRequiringNotificationCount() {
        logger.debug().message(AppConstants.DEBUG_CONTROLLER_GET_NOTIFICATION_COUNT).log();
        
        try {
            long count = passwordExpirationNotificationService.getUsersRequiringNotificationCount();
            
            Map<String, Object> response = new HashMap<>();
            response.put("usersRequiringNotificationCount", count);
            response.put("timestamp", System.currentTimeMillis());
            response.put("status", "success");
            
            logger.info()
                .message(AppConstants.INFO_CONTROLLER_NOTIFICATION_COUNT_RETRIEVED)
                .detail("count", count)
                .log();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                    .message(AppConstants.ERROR_CONTROLLER_GET_NOTIFICATION_COUNT)
                    .detail("error", e.getMessage())
                    .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", AppConstants.ERROR_CONTROLLER_NOTIFICATION_COUNT_FAILED);
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get the list of users currently requiring password expiration notifications.
     * 
     * @param limit maximum number of users to return (default: 50)
     * @return ResponseEntity with list of users requiring notifications
     */
    @GetMapping("/users-requiring-notification")
    public ResponseEntity<Map<String, Object>> getUsersRequiringNotification(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        logger.debug()
            .message(AppConstants.DEBUG_CONTROLLER_GET_USERS_NOTIFICATION)
            .detail("limit", limit)
            .log();
        
        try {
            List<User> users = passwordExpirationNotificationService.getUsersRequiringNotification(limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("users", users.stream().map(user -> {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("id", user.getId().toString());
                userInfo.put("username", user.getUsername());
                userInfo.put("email", user.getEmail());
                userInfo.put("tenantId", user.getTenantId());
                userInfo.put("passwordChangedAt", user.getPasswordChangedAt());
                
                // Note: We cannot calculate exact expiration days here since we need the password policy
                // This endpoint should be used primarily for debugging/admin purposes
                userInfo.put("note", AppConstants.NOTE_CONTROLLER_EXPIRATION_CALC_REQUIRES_POLICY);
                
                return userInfo;
            }).toList());
            response.put("count", users.size());
            response.put("timestamp", System.currentTimeMillis());
            response.put("status", "success");
            
            logger.info()
                .message(AppConstants.INFO_CONTROLLER_USERS_NOTIFICATION_RETRIEVED)
                .detail("count", users.size())
                .detail("limit", limit)
                .log();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                    .message(AppConstants.ERROR_CONTROLLER_GET_USERS_NOTIFICATION)
                    .detail("limit", limit)
                    .detail("error", e.getMessage())
                    .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", AppConstants.ERROR_CONTROLLER_USERS_NOTIFICATION_FAILED);
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Manually trigger the password expiration notification job.
     * 
     * @return ResponseEntity with trigger result
     */
    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerPasswordExpirationNotifications() {
        logger.info().message(AppConstants.INFO_CONTROLLER_MANUAL_TRIGGER_REQUEST).log();
        
        try {
            int notificationsSent = passwordExpirationNotificationService.triggerPasswordExpirationNotifications();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", AppConstants.SUCCESS_CONTROLLER_NOTIFICATION_TRIGGER);
            response.put("notificationsSent", notificationsSent);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info()
                .message(AppConstants.INFO_CONTROLLER_MANUAL_TRIGGER_COMPLETED)
                .detail("notificationsSent", notificationsSent)
                .log();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                    .message(AppConstants.ERROR_CONTROLLER_TRIGGER_NOTIFICATION)
                    .detail("error", e.getMessage())
                    .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", AppConstants.ERROR_CONTROLLER_NOTIFICATION_TRIGGER_FAILED);
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Test endpoint to manually trigger the cron job (bypasses authentication for testing)
     * 
     * @return ResponseEntity with trigger result
     */
    @PostMapping("/test-trigger")
    public ResponseEntity<Map<String, Object>> testTriggerCronJob() {
        logger.info().message(AppConstants.INFO_CONTROLLER_TEST_TRIGGER_REQUEST).log();
        
        try {
            // Directly call the cron method
            passwordExpirationNotificationService.checkAndSendPasswordExpirationNotifications();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", AppConstants.SUCCESS_CONTROLLER_TEST_CRON_EXECUTED);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info().message(AppConstants.INFO_CONTROLLER_TEST_TRIGGER_COMPLETED).log();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                    .message(AppConstants.ERROR_CONTROLLER_TEST_CRON_FAILED)
                    .detail("error", e.getMessage())
                    .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", AppConstants.ERROR_CONTROLLER_CRON_EXECUTION_FAILED + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get the current configuration status of the password expiration notification service.
     * 
     * @return ResponseEntity with configuration status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getNotificationStatus() {
        logger.debug().message(AppConstants.DEBUG_CONTROLLER_GET_NOTIFICATION_STATUS).log();
        
        try {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", AppConstants.SUCCESS_CONTROLLER_NOTIFICATION_SERVICE_ACTIVE);
            response.put("timestamp", System.currentTimeMillis());
            
            // Add some basic statistics
            long usersRequiringNotification = passwordExpirationNotificationService.getUsersRequiringNotificationCount();
            response.put("usersRequiringNotification", usersRequiringNotification);
            
            logger.debug()
                .message(AppConstants.DEBUG_CONTROLLER_NOTIFICATION_STATUS_RETRIEVED)
                .detail("usersRequiringNotification", usersRequiringNotification)
                .log();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error(e)
                    .message(AppConstants.ERROR_CONTROLLER_GET_NOTIFICATION_STATUS)
                    .detail("error", e.getMessage())
                    .log();
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", AppConstants.ERROR_CONTROLLER_NOTIFICATION_STATUS_FAILED);
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
