package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.repository.ProcessedEventRepository;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Cleanup service for processed events.
 * 
 * The processed_events table grows indefinitely as events are processed.
 * This service periodically deletes old records to prevent unbounded growth
 * while maintaining a reasonable history for debugging and auditing.
 * 
 * Default retention: 30 days (configurable)
 */
import static com.aidentx.workflowsservice.config.WorkflowConstants.*;

@Service
public class ProcessedEventCleanupService {

    private final IUnifiedLogger logger;
    private final ProcessedEventRepository processedEventRepository;

    public ProcessedEventCleanupService(
            ProcessedEventRepository processedEventRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.processedEventRepository = processedEventRepository;
        this.logger = loggerFactory.getLogger(ProcessedEventCleanupService.class, LOG_CATEGORY_WORKFLOW_SERVICE);
    }

    /**
     * Number of days to retain processed event records
     * Default: 30 days (enough to catch any Kafka replay issues)
     * Can be overridden in application.properties
     */
    @Value("${workflow.processed-events.retention-days:30}")
    private int retentionDays;

    /**
     * Clean up old processed events.
     * Runs daily at 2 AM (configurable via cron expression)
     * 
     * Schedule can be overridden in application.properties:
     * workflow.processed-events.cleanup-cron=0 0 2 * * ?
     */
    @Scheduled(cron = "${workflow.processed-events.cleanup-cron:0 0 2 * * ?}")
    @Transactional
    public void cleanupOldProcessedEvents() {
        try {
            OffsetDateTime cutoffDate = OffsetDateTime.now().minusDays(retentionDays);
            
            logger.info()
                .message(LOG_STARTING_CLEANUP_PROCESSED_EVENTS)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("cutoffDate", cutoffDate.toString())
                .detail("retentionDays", retentionDays)
                .log();
            
            int deletedCount = processedEventRepository.deleteByProcessedAtBefore(cutoffDate);
            
            if (deletedCount > 0) {
                logger.info()
                    .message(LOG_CLEANED_UP_PROCESSED_EVENTS)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("deletedCount", deletedCount)
                    .detail("retentionDays", retentionDays)
                    .log();
            } else {
                logger.trace()
                    .message(LOG_NO_PROCESSED_EVENTS_TO_CLEANUP)
                    .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                    .detail("cutoffDate", cutoffDate.toString())
                    .log();
            }
            
        } catch (Exception e) {
            logger.error()
                .message(LOG_FAILED_CLEANUP_PROCESSED_EVENTS)
                .category(LOG_CATEGORY_WORKFLOW_SERVICE)
                .detail("error", e.getMessage())
                .detail("errorType", e.getClass().getSimpleName())
                .log();
            // Don't throw - this is a background maintenance task
        }
    }

    /**
     * Get current retention period in days
     */
    public int getRetentionDays() {
        return retentionDays;
    }
}
