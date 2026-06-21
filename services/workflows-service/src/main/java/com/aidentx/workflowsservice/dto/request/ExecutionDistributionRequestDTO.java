package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.config.analytics.WorkflowExecutionDistributionConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Workflow Execution Distribution API
 *
 * Contains parameters for filtering execution distribution
 * data by time period.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionDistributionRequestDTO {

    /**
     * Time period for distribution calculation
     *
     * Allowed values:
     * - LAST_24_HOURS: Last 24 hours
     * - LAST_30_DAYS: Last 30 days (default)
     * - LAST_60_DAYS: Last 60 days
     * - LAST_90_DAYS: Last 90 days
     * - ALL_TIME: All historical data
     */
    @Builder.Default
    private String timePeriod = WorkflowExecutionDistributionConstants.DEFAULT_TIME_PERIOD;
}