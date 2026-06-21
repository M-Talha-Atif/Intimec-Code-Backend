package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.dto.request.ExecutionDistributionRequestDTO;
import com.aidentx.workflowsservice.dto.response.ExecutionDistributionResponseDTO;

/**
 * Service interface for Workflow Execution Distribution operations
 *
 * Provides methods for calculating and retrieving workflow
 * execution distribution by status (Success, Failed, Pending).
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public interface WorkflowExecutionDistributionService {

    /**
     * Get workflow execution distribution for the specified time period
     *
     * Calculates distribution of executions across three categories:
     * - Success: COMPLETED status
     * - Failed: FAILED, TIMEOUT, CANCELLED statuses
     * - Pending: RUNNING, PAUSED, PARTIAL statuses
     *
     * @param requestDTO Request containing time period filter
     * @return ExecutionDistributionResponseDTO containing distribution data
     */
    ExecutionDistributionResponseDTO getExecutionDistribution(ExecutionDistributionRequestDTO requestDTO);
}