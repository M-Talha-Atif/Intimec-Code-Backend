package com.aidentx.access_portal_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Standard workflow event structure for Kafka messages.
 * Matches the format expected by workflows-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEvent {

    private UUID eventId;
    private String eventType;
    private String sourceService;
    private String tenantId;
    private OffsetDateTime timestamp;
    private Map<String, Object> payload;
    private UUID correlationId;
    private Map<String, Object> metadata;

    /**
     * Constructor with basic fields - generates eventId and timestamp automatically
     */
    public WorkflowEvent(String eventType, String source, String tenantId) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.sourceService = source;
        this.tenantId = tenantId;
        this.timestamp = OffsetDateTime.now();
        this.correlationId = UUID.randomUUID();
    }
}