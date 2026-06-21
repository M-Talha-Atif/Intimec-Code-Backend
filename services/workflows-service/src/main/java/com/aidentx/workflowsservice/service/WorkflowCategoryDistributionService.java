package com.aidentx.workflowsservice.service;

import com.aidentx.workflowsservice.dto.response.CategoryDistributionResponseDTO;

/**
 * Service interface for Workflow Category Distribution operations
 *
 * Provides methods for calculating and retrieving workflow
 * distribution across lifecycle categories.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public interface WorkflowCategoryDistributionService {

    /**
     * Get workflow distribution by category
     *
     * Calculates the number and percentage of workflows in each
     * lifecycle category (JML, Access Request, Certification, etc.).
     *
     * Returns data suitable for bar chart visualization showing
     * how workflows are distributed across governance domains.
     *
     * @return CategoryDistributionResponseDTO containing distribution data
     */
    CategoryDistributionResponseDTO getCategoryDistribution();
}