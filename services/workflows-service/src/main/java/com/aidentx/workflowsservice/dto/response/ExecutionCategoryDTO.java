package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing execution count for a single category
 *
 * Contains the category name (Success/Failed/Pending),
 * count of executions, and percentage of total.
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
public class ExecutionCategoryDTO {

    /**
     * Category name
     *
     * Possible values:
     * - "Success": Successfully completed executions
     * - "Failed": Failed, timed out, or cancelled executions
     * - "Pending": Running, paused, or partially completed executions
     */
    private String name;

    /**
     * Number of executions in this category
     */
    private Long value;

    /**
     * Percentage of total executions (0-100)
     *
     * Calculated as: (value / totalExecutions) * 100
     * Rounded to 2 decimal places
     */
    private Double percentage;
}