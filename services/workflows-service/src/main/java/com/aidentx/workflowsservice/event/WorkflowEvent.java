package com.aidentx.workflowsservice.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Event payload received from Kafka topics
 * Represents an event that can trigger workflow execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEvent {
    
    /**
     * Event type identifier (e.g., "user.created", "role.assigned")
     */
    private String eventType;
    
    /**
     * Source service that published the event
     */
    private String sourceService;
    
    /**
     * Tenant ID for multi-tenancy
     */
    private String tenantId;
    
    /**
     * Event timestamp
     */
    private OffsetDateTime timestamp;
    
    /**
     * Unique event ID for idempotency
     */
    private UUID eventId;
    
    /**
     * Event payload data (flexible structure)
     */
    private Map<String, Object> payload;
    
    /**
     * Correlation ID for tracing
     */
    private String correlationId;
    
    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;
}
