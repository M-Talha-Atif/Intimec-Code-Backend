package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing execution statistics for a single month
 *
 * Contains the month label, total execution count,
 * and failure count for visualization.
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
public class MonthlyExecutionDataDTO {

    /**
     * Month label (e.g., "Jan", "Feb", "Mar")
     */
    private String month;

    /**
     * Total number of workflow executions in this month
     */
    private Long executions;

    /**
     * Number of failed workflow executions in this month
     */
    private Long failures;
}