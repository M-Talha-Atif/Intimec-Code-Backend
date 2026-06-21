package com.aidentx.workflowsservice.service.impl;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.config.analytics.WorkflowAnalyticsConstants;
import com.aidentx.workflowsservice.config.analytics.WorkflowExecutionDistributionConstants;
import com.aidentx.workflowsservice.dto.request.ExecutionDistributionRequestDTO;
import com.aidentx.workflowsservice.dto.response.ExecutionCategoryDTO;
import com.aidentx.workflowsservice.dto.response.ExecutionDistributionResponseDTO;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.service.WorkflowExecutionDistributionService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of WorkflowExecutionDistributionService
 *
 * Calculates execution distribution by querying database
 * for different execution statuses and categorizing them.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Service
public class WorkflowExecutionDistributionServiceImpl implements WorkflowExecutionDistributionService {

    private final IUnifiedLogger logger;
    private final WorkflowExecutionRepository executionRepository;

    public WorkflowExecutionDistributionServiceImpl(
            WorkflowExecutionRepository executionRepository,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.executionRepository = executionRepository;
        this.logger = loggerFactory.getLogger(
                WorkflowExecutionDistributionServiceImpl.class,
                WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutionDistributionResponseDTO getExecutionDistribution(ExecutionDistributionRequestDTO requestDTO) {
        logger.info()
                .message(WorkflowExecutionDistributionConstants.LOG_EXECUTION_DISTRIBUTION_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .detail(WorkflowExecutionDistributionConstants.LOG_FIELD_TIME_PERIOD, requestDTO.getTimePeriod())
                .log();

        try {
            logger.debug()
                    .message(WorkflowExecutionDistributionConstants.LOG_EXECUTION_DISTRIBUTION_CALCULATION_START)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowExecutionDistributionConstants.LOG_FIELD_TIME_PERIOD, requestDTO.getTimePeriod())
                    .log();

            // Calculate time boundary
            OffsetDateTime startDate = calculateStartDate(requestDTO.getTimePeriod());

            // Calculate counts for each category
            Long successCount = calculateSuccessCount(startDate);
            Long failedCount = calculateFailedCount(startDate);
            Long pendingCount = calculatePendingCount(startDate);

            // Calculate total
            Long totalExecutions = successCount + failedCount + pendingCount;

            // Build distribution list
            List<ExecutionCategoryDTO> distribution = buildDistribution(
                    successCount,
                    failedCount,
                    pendingCount,
                    totalExecutions
            );

            logger.info()
                    .message(WorkflowExecutionDistributionConstants.LOG_EXECUTION_DISTRIBUTION_CALCULATION_COMPLETE)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowExecutionDistributionConstants.LOG_FIELD_TIME_PERIOD, requestDTO.getTimePeriod())
                    .detail(WorkflowExecutionDistributionConstants.LOG_FIELD_TOTAL_EXECUTIONS, totalExecutions.toString())
                    .log();

            return ExecutionDistributionResponseDTO.builder()
                    .timePeriod(requestDTO.getTimePeriod())
                    .totalExecutions(totalExecutions)
                    .distribution(distribution)
                    .build();

        } catch (Exception e) {
            logger.error()
                    .message(WorkflowExecutionDistributionConstants.LOG_EXECUTION_DISTRIBUTION_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowExecutionDistributionConstants.LOG_FIELD_TIME_PERIOD, requestDTO.getTimePeriod())
                    .detail(WorkflowExecutionDistributionConstants.LOG_FIELD_ERROR, e.getMessage())
                    .log();

            throw new RuntimeException(
                    WorkflowExecutionDistributionConstants.ERR_EXECUTION_DISTRIBUTION_CALCULATION_FAILED,
                    e
            );
        }
    }

    /**
     * Calculate start date based on time period
     *
     * @param timePeriod Time period string
     * @return Start date for filtering, or null for ALL_TIME
     */
    private OffsetDateTime calculateStartDate(String timePeriod) {
        OffsetDateTime now = OffsetDateTime.now();

        return switch (timePeriod) {
            case WorkflowExecutionDistributionConstants.PERIOD_LAST_24_HOURS ->
                    now.minusHours(WorkflowExecutionDistributionConstants.HOURS_24);
            case WorkflowExecutionDistributionConstants.PERIOD_LAST_30_DAYS ->
                    now.minusDays(WorkflowExecutionDistributionConstants.DAYS_30);
            case WorkflowExecutionDistributionConstants.PERIOD_LAST_60_DAYS ->
                    now.minusDays(WorkflowExecutionDistributionConstants.DAYS_60);
            case WorkflowExecutionDistributionConstants.PERIOD_LAST_90_DAYS ->
                    now.minusDays(WorkflowExecutionDistributionConstants.DAYS_90);
            case WorkflowExecutionDistributionConstants.PERIOD_ALL_TIME -> null;
            default -> {
                logger.warn()
                        .message(WorkflowExecutionDistributionConstants.LOG_INVALID_TIME_PERIOD)
                        .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                        .detail(WorkflowExecutionDistributionConstants.LOG_FIELD_TIME_PERIOD, timePeriod)
                        .log();
                yield now.minusDays(WorkflowExecutionDistributionConstants.DAYS_30);
            }
        };
    }

    /**
     * Calculate count of successful executions
     *
     * @param startDate Start date filter (null for all time)
     * @return Count of successful executions
     */
    private Long calculateSuccessCount(OffsetDateTime startDate) {
        if (startDate == null) {
            return executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.COMPLETED);
        } else {
            return executionRepository.countByStatusAndStartedAtAfter(
                    WorkflowExecution.ExecutionStatus.COMPLETED,
                    startDate
            );
        }
    }

    /**
     * Calculate count of failed executions
     *
     * Includes: FAILED, TIMEOUT, CANCELLED statuses
     *
     * @param startDate Start date filter (null for all time)
     * @return Count of failed executions
     */
    private Long calculateFailedCount(OffsetDateTime startDate) {
        Long failedCount = WorkflowExecutionDistributionConstants.INITIAL_COUNT;

        // Count FAILED executions
        if (startDate == null) {
            failedCount += executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.FAILED);
            failedCount += executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.TIMEOUT);
            failedCount += executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.CANCELLED);
        } else {
            failedCount += executionRepository.countByStatusAndStartedAtAfter(
                    WorkflowExecution.ExecutionStatus.FAILED,
                    startDate
            );
            failedCount += executionRepository.countByStatusAndStartedAtAfter(
                    WorkflowExecution.ExecutionStatus.TIMEOUT,
                    startDate
            );
            failedCount += executionRepository.countByStatusAndStartedAtAfter(
                    WorkflowExecution.ExecutionStatus.CANCELLED,
                    startDate
            );
        }

        return failedCount;
    }

    /**
     * Calculate count of pending executions
     *
     * Includes: RUNNING, PAUSED, PARTIAL statuses
     *
     * @param startDate Start date filter (null for all time)
     * @return Count of pending executions
     */
    private Long calculatePendingCount(OffsetDateTime startDate) {
        Long pendingCount = WorkflowExecutionDistributionConstants.INITIAL_COUNT;

        // Count RUNNING, PAUSED, PARTIAL executions
        if (startDate == null) {
            pendingCount += executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.RUNNING);
            pendingCount += executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.PAUSED);
            pendingCount += executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.PARTIAL);
        } else {
            pendingCount += executionRepository.countByStatusAndStartedAtAfter(
                    WorkflowExecution.ExecutionStatus.RUNNING,
                    startDate
            );
            pendingCount += executionRepository.countByStatusAndStartedAtAfter(
                    WorkflowExecution.ExecutionStatus.PAUSED,
                    startDate
            );
            pendingCount += executionRepository.countByStatusAndStartedAtAfter(
                    WorkflowExecution.ExecutionStatus.PARTIAL,
                    startDate
            );
        }

        return pendingCount;
    }

    /**
     * Build distribution list with percentages
     *
     * @param successCount Number of successful executions
     * @param failedCount Number of failed executions
     * @param pendingCount Number of pending executions
     * @param totalExecutions Total number of executions
     * @return List of execution categories with percentages
     */
    private List<ExecutionCategoryDTO> buildDistribution(
            Long successCount,
            Long failedCount,
            Long pendingCount,
            Long totalExecutions
    ) {
        List<ExecutionCategoryDTO> distribution = new ArrayList<>();

        // Add Success category
        distribution.add(ExecutionCategoryDTO.builder()
                .name(WorkflowExecutionDistributionConstants.CATEGORY_SUCCESS)
                .value(successCount)
                .percentage(calculatePercentage(successCount, totalExecutions))
                .build()
        );

        // Add Failed category
        distribution.add(ExecutionCategoryDTO.builder()
                .name(WorkflowExecutionDistributionConstants.CATEGORY_FAILED)
                .value(failedCount)
                .percentage(calculatePercentage(failedCount, totalExecutions))
                .build()
        );

        // Add Pending category
        distribution.add(ExecutionCategoryDTO.builder()
                .name(WorkflowExecutionDistributionConstants.CATEGORY_PENDING)
                .value(pendingCount)
                .percentage(calculatePercentage(pendingCount, totalExecutions))
                .build()
        );

        return distribution;
    }

    /**
     * Calculate percentage with 2 decimal places
     *
     * @param value Category value
     * @param total Total value
     * @return Percentage (0-100) rounded to 2 decimal places
     */
    private Double calculatePercentage(Long value, Long total) {
        if (total == null || total == WorkflowExecutionDistributionConstants.INITIAL_COUNT) {
            return WorkflowExecutionDistributionConstants.DEFAULT_PERCENTAGE;
        }

        double percentage = (value.doubleValue() / total.doubleValue()) *
                WorkflowExecutionDistributionConstants.PERCENTAGE_MULTIPLIER;
        return Math.round(percentage * WorkflowExecutionDistributionConstants.PERCENTAGE_SCALE) /
                WorkflowExecutionDistributionConstants.PERCENTAGE_SCALE;
    }
}