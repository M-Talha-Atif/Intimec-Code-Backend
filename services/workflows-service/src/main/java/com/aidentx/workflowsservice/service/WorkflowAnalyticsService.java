package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.dto.request.WorkflowAnalyticsRequestDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowAnalyticsResponseDTO;

/**
 * Service interface for Workflow Analytics operations
 *
 * Provides methods for calculating and retrieving workflow analytics,
 * including KPIs, execution statistics, and performance metrics.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public interface WorkflowAnalyticsService {

    /**
     * Get workflow analytics overview for the specified time period
     *
     * Calculates comprehensive analytics including:
     * - Total, active, and draft workflow counts
     * - Success rate based on execution outcomes
     * - Average execution time
     * - Failed execution count
     * - Mock KPIs for future implementation
     *
     * @param requestDTO Request containing time period filter
     * @return WorkflowAnalyticsResponseDTO containing calculated analytics
     */
    WorkflowAnalyticsResponseDTO getAnalyticsOverview(WorkflowAnalyticsRequestDTO requestDTO);
}