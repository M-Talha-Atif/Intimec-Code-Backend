package com.aidentx.workflowsservice.dto.request;

import com.aidentx.workflowsservice.config.analytics.WorkflowKpiDrilldownConstants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for Workflow KPI Drilldown API
 *
 * Contains parameters for filtering and paginating
 * workflow data based on KPI type.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiDrilldownRequestDTO {

    /**
     * KPI type to filter workflows
     *
     * Allowed values:
     * - TOTAL_WORKFLOWS: All workflows
     * - ACTIVE_DRAFT_WORKFLOWS: Active and Draft workflows (combined)
     * - AUTOMATED_MANUAL_WORKFLOWS: Automated and Manual workflows (combined)
     * - SUCCESS_RATE_WORKFLOWS: Workflows that contributed to success rate
     * - AVERAGE_EXECUTION_TIME_WORKFLOWS: Workflows sorted by average completion time
     * - FAILED_EXECUTION_WORKFLOWS: Workflows with failed executions
     * - HIGH_RISK_WORKFLOWS: Workflows with risk score >= 7
     * - AI_DRIVEN_WORKFLOWS: AI-Risk category workflows
     */
    private String kpiType;

    /**
     * Page number for pagination (0-indexed)
     */
    @Builder.Default
    private Integer page = WorkflowKpiDrilldownConstants.DEFAULT_PAGE;

    /**
     * Page size for pagination
     */
    @Builder.Default
    private Integer pageSize = WorkflowKpiDrilldownConstants.DEFAULT_PAGE_SIZE;

    /**
     * Field to sort by
     *
     * Allowed values:
     * - name: Sort by workflow name
     * - riskScore: Sort by risk score
     * - executionCount: Sort by execution count
     * - createdAt: Sort by creation date
     * - avgExecutionTime: Sort by average execution time (for AVERAGE_EXECUTION_TIME_WORKFLOWS)
     */
    @Builder.Default
    private String sortBy = WorkflowKpiDrilldownConstants.DEFAULT_SORT_BY;

    /**
     * Sort direction
     *
     * Allowed values: ASC, DESC
     */
    @Builder.Default
    private String sortDirection = WorkflowKpiDrilldownConstants.DEFAULT_SORT_DIRECTION;
}