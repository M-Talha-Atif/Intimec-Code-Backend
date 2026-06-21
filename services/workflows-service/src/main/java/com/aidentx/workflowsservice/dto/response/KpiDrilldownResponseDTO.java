package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Workflow KPI Drilldown
 *
 * Contains paginated list of workflows matching
 * the specified KPI criteria.
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
public class KpiDrilldownResponseDTO {

    /**
     * KPI type requested
     */
    private String kpiType;

    /**
     * Total number of workflows matching the KPI criteria
     */
    private Long totalWorkflows;

    /**
     * Current page number (0-indexed)
     */
    private Integer currentPage;

    /**
     * Page size
     */
    private Integer pageSize;

    /**
     * Total number of pages
     */
    private Integer totalPages;

    /**
     * Whether there is a next page
     */
    private Boolean hasNext;

    /**
     * Whether there is a previous page
     */
    private Boolean hasPrevious;

    /**
     * List of workflows in the current page
     */
    private List<WorkflowKpiEntryDTO> workflows;
}