package com.aidentx.authservice.config;

import com.aidentx.authservice.utils.JwtUtil;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import static com.aidentx.authservice.config.AppConstants.*;

/**
 * WebSocket configuration for real-time session termination notifications.
 * 
 * <p>Configures STOMP over WebSocket with JWT authentication.
 * Clients connect to /ws and subscribe to /user/queue/session-termination
 * to receive notifications when their session is terminated.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final IUnifiedLogger logger;

    @Autowired
    private JwtUtil jwtUtil;

    public WebSocketConfig(UnifiedLoggerFactory unifiedLoggerFactory) {
        this.logger = unifiedLoggerFactory.getLogger(WebSocketConfig.class);
    }

    /**
     * Configure message broker for WebSocket communication.
     * 
     * @param registry the message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        logger.trace()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_CONFIGURING_BROKER)
            .log();
        
        // Enable a simple memory-based message broker for /topic and /queue
        // Note: Do NOT include /user in broker destinations - it's handled by UserDestinationMessageHandler
        registry.enableSimpleBroker(WEBSOCKET_BROKER_TOPIC, WEBSOCKET_BROKER_QUEUE);
        
        logger.debug()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_BROKER_ENABLED)
            .detail(LOG_DETAIL_WEBSOCKET_BROKERS, String.format("%s, %s", 
                WEBSOCKET_BROKER_TOPIC, WEBSOCKET_BROKER_QUEUE))
            .log();
        
        // Messages with destination prefix /app will be routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes(WEBSOCKET_APP_PREFIX);
        
        logger.debug()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_APP_PREFIX_SET)
            .detail(LOG_DETAIL_WEBSOCKET_APP_PREFIX, WEBSOCKET_APP_PREFIX)
            .log();
        
        // Set user destination prefix for targeted user messages
        registry.setUserDestinationPrefix(WEBSOCKET_USER_PREFIX);
        
        logger.debug()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_USER_PREFIX_SET)
            .detail(LOG_DETAIL_USER_PREFIX, WEBSOCKET_USER_PREFIX)
            .log();
        
        logger.info()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_BROKER_CONFIGURED)
            .detail(LOG_DETAIL_WEBSOCKET_BROKERS, String.format("%s, %s", 
                WEBSOCKET_BROKER_TOPIC, WEBSOCKET_BROKER_QUEUE))
            .detail(LOG_DETAIL_WEBSOCKET_APP_PREFIX, WEBSOCKET_APP_PREFIX)
            .detail(LOG_DETAIL_USER_PREFIX, WEBSOCKET_USER_PREFIX)
            .log();
    }

    /**
     * Register STOMP endpoints for WebSocket connections.
     * 
     * @param registry the STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        logger.trace()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_REGISTERING_ENDPOINTS)
            .log();
        
        // Register /ws endpoint with SockJS fallback
        // Note: CORS is handled by API Gateway, so we don't set allowedOrigins here
        registry.addEndpoint(WEBSOCKET_ENDPOINT)
            .setAllowedOriginPatterns("*")  // Allow all since Gateway handles CORS
            .withSockJS();
        
        logger.debug()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_ENDPOINT_ADDED)
            .detail(LOG_DETAIL_WEBSOCKET_ENDPOINT, WEBSOCKET_ENDPOINT)
            .detail(LOG_DETAIL_ALLOWED_ORIGINS, WEBSOCKET_ALLOWED_ORIGINS_PATTERN)
            .log();
        
        logger.info()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_ENDPOINT_REGISTERED)
            .detail(LOG_DETAIL_WEBSOCKET_ENDPOINT, WEBSOCKET_ENDPOINT)
            .detail(LOG_DETAIL_WEBSOCKET_SOCKJS, "enabled")
            .log();
    }

    /**
     * Configure client inbound channel with JWT authentication interceptor.
     * 
     * @param registration the channel registration
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        logger.trace()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_CONFIGURING_INBOUND_CHANNEL)
            .log();
        
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    logger.trace()
                        .category(LOG_CATEGORY_WEBSOCKET)
                        .message(LOG_MSG_WEBSOCKET_PROCESSING_CONNECT)
                        .log();
                    
                    // Extract JWT token from headers
                    String authToken = accessor.getFirstNativeHeader(WEBSOCKET_AUTH_HEADER);
                    
                    if (authToken != null && authToken.startsWith(WEBSOCKET_AUTH_BEARER_PREFIX)) {
                        String token = authToken.substring(WEBSOCKET_AUTH_BEARER_PREFIX.length());
                        
                        logger.debug()
                            .category(LOG_CATEGORY_WEBSOCKET)
                            .message(LOG_MSG_WEBSOCKET_VALIDATING_TOKEN)
                            .detail(LOG_DETAIL_TOKEN_LENGTH, String.valueOf(token.length()))
                            .log();
                        
                        try {
                            // Extract sessionId from header (required for session-specific notifications)
                            String sessionId = accessor.getFirstNativeHeader("X-Session-Id");
                            
                            logger.trace()
                                .category(LOG_CATEGORY_WEBSOCKET)
                                .message(LOG_MSG_WEBSOCKET_EXTRACTING_USERNAME)
                                .detail("sessionId", sessionId)
                                .log();
                            
                            if (sessionId == null || sessionId.isEmpty()) {
                                logger.warn()
                                    .category(LOG_CATEGORY_WEBSOCKET)
                                    .message("WebSocket connection missing X-Session-Id header")
                                    .log();
                                return message;
                            }
                            
                            // Validate JWT token
                            if (jwtUtil.validateToken(token)) {
                                logger.trace()
                                    .category(LOG_CATEGORY_WEBSOCKET)
                                    .message(LOG_MSG_WEBSOCKET_SETTING_USER_CONTEXT)
                                    .detail("sessionId", sessionId)
                                    .log();
                                
                                // Set sessionId as principal name for user-specific routing
                                UsernamePasswordAuthenticationToken authentication = 
                                    new UsernamePasswordAuthenticationToken(sessionId, null, null);
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                accessor.setUser(authentication);
                                
                                logger.info()
                                    .category(LOG_CATEGORY_WEBSOCKET)
                                    .message(LOG_MSG_WEBSOCKET_CONNECTION_AUTHENTICATED)
                                    .detail("sessionId", sessionId)
                                    .detail(LOG_DETAIL_WEBSOCKET_TOKEN_VALID, "true")
                                    .log();
                            } else {
                                logger.warn()
                                    .category(LOG_CATEGORY_WEBSOCKET)
                                    .message(LOG_MSG_WEBSOCKET_INVALID_JWT)
                                    .detail("sessionId", sessionId)
                                    .detail(LOG_DETAIL_WEBSOCKET_TOKEN_VALID, "false")
                                    .log();
                            }
                        } catch (Exception e) {
                            logger.error(e)
                                .category(LOG_CATEGORY_WEBSOCKET)
                                .message(LOG_MSG_WEBSOCKET_JWT_VALIDATION_ERROR)
                                .detail(LOG_DETAIL_ERROR, e.getMessage())
                                .log();
                        }
                    } else {
                        logger.warn()
                            .category(LOG_CATEGORY_WEBSOCKET)
                            .message(LOG_MSG_WEBSOCKET_NO_AUTH_HEADER)
                            .detail(LOG_DETAIL_AUTH_TOKEN_PRESENT, String.valueOf(authToken != null))
                            .log();
                    }
                }
                
                return message;
            }
        });
        
        logger.debug()
            .category(LOG_CATEGORY_WEBSOCKET)
            .message(LOG_MSG_WEBSOCKET_INBOUND_CHANNEL_CONFIGURED)
            .log();
    }
}
