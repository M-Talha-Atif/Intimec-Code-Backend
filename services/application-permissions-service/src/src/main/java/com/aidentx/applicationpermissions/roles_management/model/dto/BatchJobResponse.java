package com.aidentx.applicationpermissions.roles_management.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for batch job status.
 * Returned when submitting async batch operations.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 1.0
 */
public record BatchJobResponse(
        UUID batchId,
        String status,
        Integer totalAssignments,
        Integer completedCount,
        Integer failedCount,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        String estimatedCompletionTime,
        String trackingUrl
) {
    /**
     * Calculates progress percentage.
     *
     * @return percentage (0-100)
     */
    public int getProgressPercentage() {
        if (totalAssignments == 0) return 0;
        return (int) (((double) (completedCount + failedCount) / totalAssignments) * 100);
    }
}