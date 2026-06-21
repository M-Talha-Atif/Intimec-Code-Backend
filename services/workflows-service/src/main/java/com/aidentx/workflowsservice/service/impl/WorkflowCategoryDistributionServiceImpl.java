package com.aidentx.workflowsservice.service.impl;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.config.analytics.WorkflowAnalyticsConstants;
import com.aidentx.workflowsservice.config.analytics.WorkflowCategoryDistributionConstants;
import com.aidentx.workflowsservice.dto.response.CategoryDataDTO;
import com.aidentx.workflowsservice.dto.response.CategoryDistributionResponseDTO;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.service.WorkflowCategoryDistributionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of WorkflowCategoryDistributionService
 *
 * Calculates category distribution by grouping workflows
 * by their category and computing counts and percentages.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Service
public class WorkflowCategoryDistributionServiceImpl implements WorkflowCategoryDistributionService {

    private final IUnifiedLogger logger;
    private final WorkflowRepository workflowRepository;

    public WorkflowCategoryDistributionServiceImpl(
            WorkflowRepository workflowRepository,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.workflowRepository = workflowRepository;
        this.logger = loggerFactory.getLogger(
                WorkflowCategoryDistributionServiceImpl.class,
                WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CategoryDistributionResponseDTO getCategoryDistribution() {
        logger.info()
                .message(WorkflowCategoryDistributionConstants.LOG_CATEGORY_DISTRIBUTION_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .log();

        try {
            logger.debug()
                    .message(WorkflowCategoryDistributionConstants.LOG_CATEGORY_DISTRIBUTION_CALCULATION_START)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .log();

            // Fetch all workflows
            List<Workflow> allWorkflows = workflowRepository.findAll();
            Long totalWorkflows = (long) allWorkflows.size();

            // Group workflows by category
            Map<Workflow.WorkflowCategory, Long> categoryCounts = new HashMap<>();

            for (Workflow workflow : allWorkflows) {
                Workflow.WorkflowCategory category = workflow.getCategory();
                if (category != null) {
                    categoryCounts.put(
                            category,
                            categoryCounts.getOrDefault(category, WorkflowCategoryDistributionConstants.INITIAL_COUNT) +
                                    WorkflowCategoryDistributionConstants.COUNT_INCREMENT
                    );
                }
            }

            // Build distribution list
            List<CategoryDataDTO> distribution = buildDistribution(categoryCounts, totalWorkflows);

            logger.info()
                    .message(WorkflowCategoryDistributionConstants.LOG_CATEGORY_DISTRIBUTION_CALCULATION_COMPLETE)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowCategoryDistributionConstants.LOG_FIELD_TOTAL_WORKFLOWS, totalWorkflows.toString())
                    .detail(WorkflowCategoryDistributionConstants.LOG_FIELD_CATEGORY_COUNT, String.valueOf(distribution.size()))
                    .log();

            return CategoryDistributionResponseDTO.builder()
                    .totalWorkflows(totalWorkflows)
                    .categoryCount(distribution.size())
                    .distribution(distribution)
                    .build();

        } catch (Exception e) {
            logger.error()
                    .message(WorkflowCategoryDistributionConstants.LOG_CATEGORY_DISTRIBUTION_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowCategoryDistributionConstants.LOG_FIELD_ERROR, e.getMessage())
                    .log();

            throw new RuntimeException(
                    WorkflowCategoryDistributionConstants.ERR_CATEGORY_DISTRIBUTION_CALCULATION_FAILED,
                    e
            );
        }
    }

    /**
     * Build distribution list from category counts
     *
     * @param categoryCounts Map of category to workflow count
     * @param totalWorkflows Total number of workflows
     * @return List of category data with display names and percentages
     */
    private List<CategoryDataDTO> buildDistribution(
            Map<Workflow.WorkflowCategory, Long> categoryCounts,
            Long totalWorkflows
    ) {
        List<CategoryDataDTO> distribution = new ArrayList<>();

        for (Map.Entry<Workflow.WorkflowCategory, Long> entry : categoryCounts.entrySet()) {
            Workflow.WorkflowCategory category = entry.getKey();
            Long count = entry.getValue();

            // Skip categories with count below threshold
            if (count < WorkflowCategoryDistributionConstants.MIN_COUNT_THRESHOLD) {
                continue;
            }

            distribution.add(CategoryDataDTO.builder()
                    .name(getCategoryDisplayName(category))
                    .count(count)
                    .percentage(calculatePercentage(count, totalWorkflows))
                    .build()
            );
        }

        // Sort by count descending
        distribution.sort((a, b) -> Long.compare(b.getCount(), a.getCount()));

        return distribution;
    }

    /**
     * Get human-readable display name for category
     *
     * Converts category enum to display name with spaces
     * (e.g., JOINER_MOVER_LEAVER -> "Joiner Mover Leaver")
     *
     * @param category Workflow category enum
     * @return Display name
     */
    private String getCategoryDisplayName(Workflow.WorkflowCategory category) {
        return switch (category) {
            case JOINER_MOVER_LEAVER -> WorkflowCategoryDistributionConstants.DISPLAY_NAME_JML;
            case ACCESS_REQUEST -> WorkflowCategoryDistributionConstants.DISPLAY_NAME_ACCESS_REQUEST;
            case CERTIFICATION -> WorkflowCategoryDistributionConstants.DISPLAY_NAME_CERTIFICATION;
            case REVOCATION -> WorkflowCategoryDistributionConstants.DISPLAY_NAME_REVOCATION;
            case EXCEPTION -> WorkflowCategoryDistributionConstants.DISPLAY_NAME_EXCEPTION;
            case AI_RISK -> WorkflowCategoryDistributionConstants.DISPLAY_NAME_AI_RISK;
        };
    }

    /**
     * Calculate percentage with 2 decimal places
     *
     * @param count Category count
     * @param total Total count
     * @return Percentage (0-100) rounded to 2 decimal places
     */
    private Double calculatePercentage(Long count, Long total) {
        if (total == null || total == WorkflowCategoryDistributionConstants.INITIAL_COUNT) {
            return WorkflowCategoryDistributionConstants.DEFAULT_PERCENTAGE;
        }

        double percentage = (count.doubleValue() / total.doubleValue()) *
                WorkflowCategoryDistributionConstants.PERCENTAGE_MULTIPLIER;
        return Math.round(percentage * WorkflowCategoryDistributionConstants.PERCENTAGE_SCALE) /
                WorkflowCategoryDistributionConstants.PERCENTAGE_SCALE;
    }
}