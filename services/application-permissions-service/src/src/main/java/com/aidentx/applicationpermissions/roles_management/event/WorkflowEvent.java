package com.aidentx.applicationpermissions.roles_management.event;

import java.util.Map;
import java.util.UUID;

/**
 * Event model for publishing workflow-related events to Kafka.
 * 
 * <p>This model represents events that trigger workflows in the workflow service,
 * following the standard event structure used across all services.</p>
 * 
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public class WorkflowEvent {
    
    /**
     * Type of the event (e.g., "role.created", "role.updated")
     */
    private String eventType;
    
    /**
     * Service that originated this event
     */
    private String sourceService;
    
    /**
     * Tenant identifier for multi-tenancy support
     */
    private String tenantId;
    
    /**
     * Unique identifier for this event (for idempotency)
     */
    private UUID eventId;
    
    /**
     * Event payload containing business data
     */
    private Map<String, Object> payload;
    
    /**
     * Correlation ID for tracing across services
     */
    private String correlationId;
    
    /**
     * Additional metadata about the event
     */
    private Map<String, Object> metadata;
    
    /**
     * Timestamp when the event occurred (epoch milliseconds)
     */
    private Long timestamp;
    
    // Constructors
    
    public WorkflowEvent() {
        this.eventId = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
    }
    
    public WorkflowEvent(String eventType, String sourceService, String tenantId) {
        this();
        this.eventType = eventType;
        this.sourceService = sourceService;
        this.tenantId = tenantId;
    }
    
    // Getters and Setters
    
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getSourceService() {
        return sourceService;
    }
    
    public void setSourceService(String sourceService) {
        this.sourceService = sourceService;
    }
    
    public String getTenantId() {
        return tenantId;
    }
    
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
    
    public UUID getEventId() {
        return eventId;
    }
    
    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
    
    public String getCorrelationId() {
        return correlationId;
    }
    
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    
    public Long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Override
    public String toString() {
        return "WorkflowEvent{" +
                "eventType='" + eventType + '\'' +
                ", sourceService='" + sourceService + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", eventId=" + eventId +
                ", timestamp=" + timestamp +
                '}';
    }
}
