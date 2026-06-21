package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Workflow Execution Distribution
 *
 * Contains distribution of workflow executions by status
 * (Success, Failed, Pending) for the specified time period.
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
public class ExecutionDistributionResponseDTO {

    /**
     * Time period for which distribution is calculated
     */
    private String timePeriod;

    /**
     * Total number of executions included in the distribution
     */
    private Long totalExecutions;

    /**
     * Distribution data by category (Success, Failed, Pending)
     */
    private List<ExecutionCategoryDTO> distribution;
}