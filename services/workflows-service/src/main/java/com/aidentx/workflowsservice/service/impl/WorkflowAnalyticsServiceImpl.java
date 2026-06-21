package com.aidentx.workflowsservice.service.impl;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.config.analytics.WorkflowAnalyticsConstants;
import com.aidentx.workflowsservice.dto.request.WorkflowAnalyticsRequestDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowAnalyticsResponseDTO;
import com.aidentx.workflowsservice.entity.Workflow;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.repository.WorkflowRepository;
import com.aidentx.workflowsservice.service.WorkflowAnalyticsService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

/**
 * Implementation of WorkflowAnalyticsService
 *
 * Calculates workflow analytics based on database queries
 * and applies business logic for KPI computation.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Service
public class WorkflowAnalyticsServiceImpl implements WorkflowAnalyticsService {

    private final IUnifiedLogger logger;
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecutionRepository executionRepository;

    public WorkflowAnalyticsServiceImpl(
            WorkflowRepository workflowRepository,
            WorkflowExecutionRepository executionRepository,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.workflowRepository = workflowRepository;
        this.executionRepository = executionRepository;
        this.logger = loggerFactory.getLogger(
                WorkflowAnalyticsServiceImpl.class,
                WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowAnalyticsResponseDTO getAnalyticsOverview(WorkflowAnalyticsRequestDTO requestDTO) {
        logger.info()
                .message(WorkflowAnalyticsConstants.LOG_ANALYTICS_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .detail(WorkflowAnalyticsConstants.LOG_FIELD_TIME_PERIOD, requestDTO.getTimePeriod())
                .log();

        try {
            logger.debug()
                    .message(WorkflowAnalyticsConstants.LOG_ANALYTICS_CALCULATION_START)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowAnalyticsConstants.LOG_FIELD_TIME_PERIOD, requestDTO.getTimePeriod())
                    .log();

            // Calculate time boundary
            OffsetDateTime startDate = calculateStartDate(requestDTO.getTimePeriod());

            // Fetch workflow counts
            Long totalWorkflows = workflowRepository.count();
            Long activeWorkflows = workflowRepository.countByStatus(Workflow.WorkflowStatus.ACTIVE);
            Long draftWorkflows = workflowRepository.countByStatus(Workflow.WorkflowStatus.DRAFT);
            Long deprecatedWorkflows = workflowRepository.countByStatus(Workflow.WorkflowStatus.DEPRECATED);

            // Calculate automated vs manual workflows
            Long automatedWorkflows = workflowRepository.countByTriggerTypeIn(
                    Workflow.TriggerType.EVENT_BASED,
                    Workflow.TriggerType.TIME_BASED
            );
            Long manualWorkflows = workflowRepository.countByTriggerType(Workflow.TriggerType.MANUAL);

            // Calculate high-risk workflows
            Long highRiskWorkflows = workflowRepository.countByRiskScoreGreaterThanEqual(
                    WorkflowAnalyticsConstants.HIGH_RISK_THRESHOLD
            );

            // Calculate AI-driven workflows
            Long aiDrivenWorkflows = workflowRepository.countByCategory(Workflow.WorkflowCategory.AI_RISK);

            // Fetch execution statistics for the time period
            Long totalExecutions;
            Long successfulExecutions;
            Long failedExecutions;
            Double avgExecutionTimeMs;

            if (startDate == null) {
                // ALL_TIME
                totalExecutions = executionRepository.count();
                successfulExecutions = executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.COMPLETED);
                failedExecutions = executionRepository.countByStatus(WorkflowExecution.ExecutionStatus.FAILED);
                avgExecutionTimeMs = executionRepository.getAverageDuration();
            } else {
                // Time-filtered queries
                totalExecutions = executionRepository.countByStartedAtAfter(startDate);
                successfulExecutions = executionRepository.countByStatusAndStartedAtAfter(
                        WorkflowExecution.ExecutionStatus.COMPLETED,
                        startDate
                );
                failedExecutions = executionRepository.countByStatusAndStartedAtAfter(
                        WorkflowExecution.ExecutionStatus.FAILED,
                        startDate
                );
                avgExecutionTimeMs = executionRepository.getAverageDurationAfter(startDate);
            }

            // Calculate success rate
            Double successRate = calculateSuccessRate(successfulExecutions, totalExecutions);

            // Convert average execution time from milliseconds to hours
            Double avgExecutionTimeHours = convertMsToHours(avgExecutionTimeMs);

            logger.info()
                    .message(WorkflowAnalyticsConstants.LOG_ANALYTICS_CALCULATION_COMPLETE)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowAnalyticsConstants.LOG_FIELD_TOTAL_WORKFLOWS, totalWorkflows.toString())
                    .detail(WorkflowAnalyticsConstants.LOG_FIELD_TIME_PERIOD, requestDTO.getTimePeriod())
                    .log();

            return WorkflowAnalyticsResponseDTO.builder()
                    .timePeriod(requestDTO.getTimePeriod())
                    .totalWorkflows(totalWorkflows)
                    .activeWorkflows(activeWorkflows)
                    .draftWorkflows(draftWorkflows)
                    .deprecatedWorkflows(deprecatedWorkflows)
                    .automatedWorkflows(automatedWorkflows)
                    .manualWorkflows(manualWorkflows)
                    .successRate(successRate)
                    .averageExecutionTimeHours(avgExecutionTimeHours)
                    .failedExecutions(failedExecutions)
                    .totalExecutions(totalExecutions)
                    .successfulExecutions(successfulExecutions)
                    .highRiskWorkflows(highRiskWorkflows)
                    .aiDrivenWorkflows(aiDrivenWorkflows)
                    .build();

        } catch (Exception e) {
            logger.error()
                    .message(WorkflowAnalyticsConstants.LOG_ANALYTICS_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowAnalyticsConstants.LOG_FIELD_TIME_PERIOD, requestDTO.getTimePeriod())
                    .detail(WorkflowAnalyticsConstants.LOG_FIELD_ERROR, e.getMessage())
                    .log();

            throw new RuntimeException(WorkflowAnalyticsConstants.ERR_ANALYTICS_CALCULATION_FAILED, e);
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
            case WorkflowAnalyticsConstants.PERIOD_LAST_24_HOURS ->
                    now.minusHours(WorkflowAnalyticsConstants.HOURS_24);
            case WorkflowAnalyticsConstants.PERIOD_LAST_30_DAYS ->
                    now.minusDays(WorkflowAnalyticsConstants.DAYS_30);
            case WorkflowAnalyticsConstants.PERIOD_LAST_60_DAYS ->
                    now.minusDays(WorkflowAnalyticsConstants.DAYS_60);
            case WorkflowAnalyticsConstants.PERIOD_LAST_90_DAYS ->
                    now.minusDays(WorkflowAnalyticsConstants.DAYS_90);
            case WorkflowAnalyticsConstants.PERIOD_ALL_TIME -> null;
            default -> {
                logger.warn()
                        .message(WorkflowAnalyticsConstants.LOG_INVALID_TIME_PERIOD)
                        .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                        .detail(WorkflowAnalyticsConstants.LOG_FIELD_TIME_PERIOD, timePeriod)
                        .log();
                yield now.minusDays(WorkflowAnalyticsConstants.DAYS_30);
            }
        };
    }

    /**
     * Calculate success rate percentage
     *
     * @param successfulExecutions Number of successful executions
     * @param totalExecutions Total number of executions
     * @return Success rate as percentage (0-100), or default if no executions
     */
    private Double calculateSuccessRate(Long successfulExecutions, Long totalExecutions) {
        if (totalExecutions == null || totalExecutions == WorkflowAnalyticsConstants.LOOP_START) {
            return WorkflowAnalyticsConstants.DEFAULT_SUCCESS_RATE;
        }

        double rate = (successfulExecutions.doubleValue() / totalExecutions.doubleValue()) *
                      WorkflowAnalyticsConstants.PERCENTAGE_MULTIPLIER;
        return Math.round(rate * WorkflowAnalyticsConstants.PERCENTAGE_SCALE) /
               WorkflowAnalyticsConstants.PERCENTAGE_SCALE;
    }

    /**
     * Convert milliseconds to hours
     *
     * @param milliseconds Execution time in milliseconds
     * @return Execution time in hours, or default if null
     */
    private Double convertMsToHours(Double milliseconds) {
        if (milliseconds == null) {
            return WorkflowAnalyticsConstants.DEFAULT_AVG_EXECUTION_TIME;
        }

        double hours = milliseconds / WorkflowAnalyticsConstants.MS_PER_HOUR;
        return Math.round(hours * WorkflowAnalyticsConstants.HOUR_SCALE) /
               WorkflowAnalyticsConstants.HOUR_SCALE;
    }
}