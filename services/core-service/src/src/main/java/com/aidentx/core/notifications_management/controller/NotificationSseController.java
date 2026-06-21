package com.aidentx.core.notifications_management.controller;

import com.aidentx.core.notifications_management.config.AppConstants;
import com.aidentx.core.notifications_management.service.NotificationSseService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Controller for Server-Sent Events (SSE) real-time notification streaming.
 * <p>
 * This controller provides endpoints for establishing SSE connections to receive
 * real-time notifications for users. The user ID is extracted from JWT token.
 * </p>
 *
 * @author aidentx Development Team
 * @version 1.0
 * @since 2025-08-25
 */
@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class NotificationSseController {
    
    private final NotificationSseService notificationSseService;
    private final IUnifiedLogger logger;
    
    /**
     * Constructor for NotificationSseController.
     * 
     * @param notificationSseService service for SSE notification operations
     * @param loggerFactory factory for creating unified loggers
     */
    public NotificationSseController(NotificationSseService notificationSseService,
                                   UnifiedLoggerFactory loggerFactory) {
        this.notificationSseService = notificationSseService;
        this.logger = loggerFactory.getLogger("NotificationSseController", AppConstants.LOG_CATEGORY_NOTIFICATIONS_SSE);
    }
    
    /**
     * Establishes SSE connection for real-time notifications.
     * User ID is extracted from JWT token in the service layer.
     * 
     * @return SseEmitter for streaming real-time notifications
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamNotifications() {
        logger.info().message(AppConstants.LOG_SSE_CONNECTION_REQUEST)
                .objectType(AppConstants.OBJECT_TYPE_SSE_CONNECTION)
                .log();
        
        try {
            SseEmitter emitter = notificationSseService.createSseConnection();
            
            logger.info().message(AppConstants.LOG_SSE_CONNECTION_ESTABLISHED)
                    .objectType(AppConstants.OBJECT_TYPE_SSE_CONNECTION)
                    .log();
            
            return ResponseEntity.ok()
                    .header("Cache-Control", "no-cache")
                    .header("Connection", "keep-alive")
                    .body(emitter);
            
        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_SSE_CONNECTION_ERROR)
                    .objectType(AppConstants.OBJECT_TYPE_SSE_CONNECTION)
                    .log();
            
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * Health check endpoint for SSE connection status.
     * 
     * @return connection status
     */
    @GetMapping("/stream/health")
    public ResponseEntity<String> sseHealthCheck() {
        return ResponseEntity.ok("SSE endpoint is available");
    }
}
