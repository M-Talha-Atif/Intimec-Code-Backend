package com.aidentx.workflowsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Tracks processed events to ensure idempotent event processing.
 * Prevents duplicate workflow executions when Kafka replays messages due to:
 * - Consumer rebalancing
 * - Service restarts
 * - Network issues
 * - Kafka broker failures
 * 
 * This provides event-level deduplication as the first line of defense,
 * with workflow-level idempotency (workflowId-eventId) as the second layer.
 */
@Entity
@Table(
    name = "processed_events",
    indexes = {
        @Index(name = "idx_processed_events_processed_at", columnList = "processed_at"),
        @Index(name = "idx_processed_events_event_type", columnList = "event_type")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    /**
     * Event ID from the Kafka message (UUID)
     * This is the primary key and ensures uniqueness
     */
    @Id
    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    /**
     * Type of event (e.g., role.created, user.updated)
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * Source service that published the event
     */
    @Column(name = "source_service", length = 100)
    private String sourceService;

    /**
     * Tenant ID from the event
     */
    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    /**
     * Timestamp when this event was processed
     */
    @Column(name = "processed_at", nullable = false)
    private OffsetDateTime processedAt;

    /**
     * Number of workflows that were triggered by this event
     */
    @Column(name = "workflow_count")
    private Integer workflowCount;

    /**
     * Correlation ID for tracing (if available)
     */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = OffsetDateTime.now();
        }
    }
}
