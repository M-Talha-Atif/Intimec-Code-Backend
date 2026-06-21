package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Workflow Execution Trend
 *
 * Contains monthly execution statistics including total
 * executions and failure counts over a specified time period.
 *
 * This DTO aggregates execution data by month to provide
 * trend visualization for workflow performance analysis.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExecutionTrendResponseDTO {

    /**
     * Number of months included in the trend
     *
     * Indicates the time period covered by this trend data.
     * Common values: 5, 6, 7, 8, 10, 12 months.
     */
    private Integer monthCount;

    /**
     * Monthly execution trend data
     *
     * List of monthly statistics ordered chronologically
     * from oldest to newest month. Each entry contains
     * the month label, total executions, and failure count.
     */
    private List<MonthlyExecutionDataDTO> trend;
}