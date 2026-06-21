package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.config.analytics.WorkflowExecutionTrendConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Workflow Execution Trend API
 *
 * Contains parameters for retrieving execution trend data
 * over a specified number of months.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTrendRequestDTO {

    /**
     * Number of months to include in the trend
     *
     * Allowed values: 5, 6, 7, 8, 10, 12
     * Default: 6 months
     */
    @Builder.Default
    private Integer monthCount = WorkflowExecutionTrendConstants.DEFAULT_MONTH_COUNT;
}