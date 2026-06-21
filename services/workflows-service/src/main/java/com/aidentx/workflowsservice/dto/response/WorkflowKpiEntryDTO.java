package com.aidentx.workflowsservice.dto.response;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single workflow entry in KPI drilldown
 *
 * Contains workflow details displayed in drilldown modals.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowKpiEntryDTO {

    /**
     * Workflow ID
     */
    private UUID id;

    /**
     * Workflow name
     */
    private String name;

    /**
     * Workflow category (display name with spaces)
     */
    private String category;

    /**
     * Workflow status (ACTIVE, DRAFT, DEPRECATED)
     */
    private String status;

    /**
     * Trigger type (Event Based, Time Based, Manual)
     */
    private String triggerType;

    /**
     * Risk score (0-10)
     */
    private Integer riskScore;

    /**
     * Total execution count
     */
    private Long executionCount;

    /**
     * Average execution time in hours
     * (Only populated for AVERAGE_EXECUTION_TIME_WORKFLOWS KPI)
     */
    private Double avgExecutionTimeHours;

    /**
     * Workflow description
     */
    private String description;

    /**
     * Workflow version
     */
    private String version;
}