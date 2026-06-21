package com.aidentx.workflowsservice.repository;

import com.aidentx.workflowsservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Repository for tracking processed events to ensure idempotent event processing
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID> {

    /**
     * Check if an event has already been processed
     * This is the critical method for event-level deduplication
     * 
     * @param eventId the event ID to check
     * @return true if the event has been processed, false otherwise
     */
    boolean existsById(UUID eventId);

    /**
     * Delete processed events older than the specified date
     * This should be called periodically to prevent unbounded table growth
     * Recommendation: Keep last 30-90 days of events
     * 
     * @param cutoffDate delete events processed before this date
     * @return number of records deleted
     */
    @Modifying
    @Query("DELETE FROM ProcessedEvent p WHERE p.processedAt < :cutoffDate")
    int deleteByProcessedAtBefore(@Param("cutoffDate") OffsetDateTime cutoffDate);

    /**
     * Count processed events by event type
     * Useful for monitoring and analytics
     * 
     * @param eventType the event type to count
     * @return number of events of this type
     */
    long countByEventType(String eventType);
}
