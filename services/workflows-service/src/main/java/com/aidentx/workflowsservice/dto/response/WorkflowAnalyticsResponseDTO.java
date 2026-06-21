package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Workflow Analytics Overview
 *
 * Contains comprehensive analytics data including KPIs,
 * workflow statistics, and performance metrics.
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
public class WorkflowAnalyticsResponseDTO {

    /**
     * Time period for which analytics are calculated
     */
    private String timePeriod;

    /**
     * Total number of workflows in the system
     */
    private Long totalWorkflows;

    /**
     * Number of active workflows
     */
    private Long activeWorkflows;

    /**
     * Number of draft workflows
     */
    private Long draftWorkflows;

    /**
     * Number of deprecated workflows
     */
    private Long deprecatedWorkflows;

    /**
     * Number of automated workflows (event-based or time-based triggers)
     */
    private Long automatedWorkflows;

    /**
     * Number of manual workflows
     */
    private Long manualWorkflows;

    /**
     * Workflow success rate as a percentage (0-100)
     */
    private Double successRate;

    /**
     * Average execution time in hours
     */
    private Double averageExecutionTimeHours;

    /**
     * Number of failed workflow executions in the specified time period
     */
    private Long failedExecutions;

    /**
     * Total number of executions in the specified time period
     */
    private Long totalExecutions;

    /**
     * Number of successful executions in the specified time period
     */
    private Long successfulExecutions;

    /**
     * Number of high-risk workflows (risk score >= 7)
     */
    private Long highRiskWorkflows;

    /**
     * Number of AI-driven workflows (category = AI_RISK)
     */
    private Long aiDrivenWorkflows;
}