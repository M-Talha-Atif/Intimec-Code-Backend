package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.dto.request.KpiDrilldownRequestDTO;
import com.aidentx.workflowsservice.dto.response.KpiDrilldownResponseDTO;

/**
 * Service interface for Workflow KPI Drilldown operations
 *
 * Provides methods for retrieving detailed workflow lists
 * filtered by KPI type with pagination and sorting.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public interface WorkflowKpiDrilldownService {

    /**
     * Get workflow drilldown data for a specific KPI
     *
     * Returns paginated list of workflows matching the specified
     * KPI criteria (e.g., all workflows, active workflows, high-risk workflows).
     *
     * Results can be sorted by various fields and paginated for
     * efficient data retrieval.
     *
     * @param requestDTO Request containing KPI type, pagination, and sorting parameters
     * @return KpiDrilldownResponseDTO containing paginated workflow list
     * @throws IllegalArgumentException if KPI type is invalid
     */
    KpiDrilldownResponseDTO getKpiDrilldown(KpiDrilldownRequestDTO requestDTO);
}