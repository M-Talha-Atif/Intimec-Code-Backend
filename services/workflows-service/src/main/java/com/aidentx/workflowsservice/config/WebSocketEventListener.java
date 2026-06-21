package com.aidentx.workflowsservice.config;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.service.SimulationProgressBroadcaster;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aidentx.workflowsservice.config.WorkflowConstants.LOG_CATEGORY_WORKFLOW_SERVICE;

/**
 * Listens to WebSocket subscription events and triggers state replay for simulations.
 * 
 * <p>When a client subscribes to a simulation topic, this listener checks if the
 * execution has already started and replays its current state so the UI can catch up.</p>
 */
@Component
public class WebSocketEventListener {

    private static final Pattern SIMULATION_TOPIC_PATTERN = Pattern.compile("/topic/simulation/([0-9a-f-]{36})");
    
    private final IUnifiedLogger logger;
    private final SimulationProgressBroadcaster broadcaster;

    public WebSocketEventListener(
            SimulationProgressBroadcaster broadcaster,
            UnifiedLoggerFactory loggerFactory) {
        this.broadcaster = broadcaster;
        this.logger = loggerFactory.getLogger(WebSocketEventListener.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Handle WebSocket subscription events.
     * When a client subscribes to a simulation topic, replay the execution state.
     */
    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = headerAccessor.getDestination();
        
        if (destination == null) {
            return;
        }

        // Check if subscribing to a simulation topic
        Matcher matcher = SIMULATION_TOPIC_PATTERN.matcher(destination);
        if (matcher.matches()) {
            String executionIdStr = matcher.group(1);
            try {
                UUID executionId = UUID.fromString(executionIdStr);
                
                logger.info()
                    .message("Client subscribed to simulation topic - triggering state replay")
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("executionId", executionId.toString())
                    .detail("destination", destination)
                    .detail("sessionId", headerAccessor.getSessionId())
                    .log();
                
                // Replay the current state asynchronously (don't block the subscription)
                new Thread(() -> {
                    try {
                        // Small delay to ensure subscription is fully established
                        Thread.sleep(200);
                        broadcaster.replayExecutionState(executionId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn()
                            .message("State replay interrupted")
                            .detail("executionId", executionId.toString())
                            .log();
                    }
                }).start();
                
            } catch (IllegalArgumentException e) {
                logger.warn()
                    .message("Invalid execution ID in subscription")
                    .detail("destination", destination)
                    .log();
            }
        }
    }
}
