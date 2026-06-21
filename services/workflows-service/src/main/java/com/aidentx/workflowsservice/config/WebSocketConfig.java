package com.aidentx.workflowsservice.config;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

/**
 * WebSocket configuration for real-time workflow simulation updates.
 * 
 * <p>Configures STOMP over WebSocket for broadcasting node execution progress
 * during workflow simulations. Clients can subscribe to:
 * - /topic/simulation/{executionId} - for public simulation updates
 * - /user/queue/simulation/{executionId} - for user-specific updates
 * </p>
 * 
 * @author AidentX Development Team
 * @since Phase 7
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final IUnifiedLogger logger;

    public WebSocketConfig(UnifiedLoggerFactory loggerFactory) {
        this.logger = loggerFactory.getLogger(WebSocketConfig.class, LOG_CATEGORY_WORKFLOW_CONFIG);
    }

    /**
     * Configure message broker for WebSocket communication
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        logger.trace()
            .message("Configuring WebSocket message broker")
            .category(LOG_CATEGORY_WORKFLOW_CONFIG)
            .log();
        
        // Enable simple in-memory broker for /topic and /queue
        registry.enableSimpleBroker("/topic", "/queue");
        
        // Application destination prefix for @MessageMapping
        registry.setApplicationDestinationPrefixes("/app");
        
        // User destination prefix for targeted messages
        registry.setUserDestinationPrefix("/user");
        
        logger.info()
            .message("WebSocket message broker configured")
            .category(LOG_CATEGORY_WORKFLOW_CONFIG)
            .detail("brokers", "/topic, /queue")
            .detail("appPrefix", "/app")
            .detail("userPrefix", "/user")
            .log();
    }

    /**
     * Register STOMP endpoints for WebSocket connections
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        logger.trace()
            .message("Registering STOMP endpoints")
            .category(LOG_CATEGORY_WORKFLOW_CONFIG)
            .log();
        
        // Register /ws endpoint with SockJS fallback
        // CORS is handled by API Gateway
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .withSockJS();
        
        logger.info()
            .message("STOMP endpoint registered: /ws")
            .category(LOG_CATEGORY_WORKFLOW_CONFIG)
            .detail("endpoint", "/ws")
            .detail("sockJS", "enabled")
            .log();
    }
}
