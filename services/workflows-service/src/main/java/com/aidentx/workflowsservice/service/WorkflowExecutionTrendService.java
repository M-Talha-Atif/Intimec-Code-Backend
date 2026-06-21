package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.dto.request.ExecutionTrendRequestDTO;
import com.aidentx.workflowsservice.dto.response.ExecutionTrendResponseDTO;

/**
 * Service interface for Workflow Execution Trend operations
 *
 * Provides methods for calculating and retrieving workflow
 * execution trends over time periods.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public interface WorkflowExecutionTrendService {

    /**
     * Get workflow execution trend for the specified number of months
     *
     * Calculates monthly execution statistics including:
     * - Total executions per month
     * - Failed executions per month
     *
     * Data is returned in chronological order (oldest to newest).
     *
     * @param requestDTO Request containing month count parameter
     * @return ExecutionTrendResponseDTO containing monthly trend data
     * @throws IllegalArgumentException if month count is invalid
     */
    ExecutionTrendResponseDTO getExecutionTrend(ExecutionTrendRequestDTO requestDTO);
}