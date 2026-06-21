package com.aidentx.workflowsservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for Workflow Category Distribution
 *
 * Contains distribution of workflows across different
 * lifecycle categories (JML, Access Request, Certification, etc.).
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
public class CategoryDistributionResponseDTO {

    /**
     * Total number of workflows included in the distribution
     */
    private Long totalWorkflows;

    /**
     * Number of distinct categories
     */
    private Integer categoryCount;

    /**
     * Distribution data by category
     */
    private List<CategoryDataDTO> distribution;
}