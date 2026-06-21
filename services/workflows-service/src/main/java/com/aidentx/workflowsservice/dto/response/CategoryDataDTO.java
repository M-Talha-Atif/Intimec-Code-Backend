package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing workflow count for a single category
 *
 * Contains the category name, workflow count, and
 * percentage of total workflows.
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
public class CategoryDataDTO {

    /**
     * Category display name
     *
     * Human-readable category name with spaces
     * (e.g., "Joiner Mover Leaver", "Access Request")
     */
    private String name;

    /**
     * Number of workflows in this category
     */
    private Long count;

    /**
     * Percentage of total workflows (0-100)
     *
     * Calculated as: (count / totalWorkflows) * 100
     * Rounded to 2 decimal places
     */
    private Double percentage;
}