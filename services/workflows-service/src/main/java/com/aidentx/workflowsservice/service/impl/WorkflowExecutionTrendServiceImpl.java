package com.aidentx.workflowsservice.service.impl;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.config.analytics.WorkflowAnalyticsConstants;
import com.aidentx.workflowsservice.config.analytics.WorkflowExecutionTrendConstants;
import com.aidentx.workflowsservice.dto.request.ExecutionTrendRequestDTO;
import com.aidentx.workflowsservice.dto.response.ExecutionTrendResponseDTO;
import com.aidentx.workflowsservice.dto.response.MonthlyExecutionDataDTO;
import com.aidentx.workflowsservice.entity.WorkflowExecution;
import com.aidentx.workflowsservice.repository.WorkflowExecutionRepository;
import com.aidentx.workflowsservice.service.WorkflowExecutionTrendService;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of WorkflowExecutionTrendService
 *
 * Calculates monthly execution trends by aggregating
 * workflow execution data from the database.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@Service
public class WorkflowExecutionTrendServiceImpl implements WorkflowExecutionTrendService {

    private final IUnifiedLogger logger;
    private final WorkflowExecutionRepository executionRepository;

    public WorkflowExecutionTrendServiceImpl(
            WorkflowExecutionRepository executionRepository,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.executionRepository = executionRepository;
        this.logger = loggerFactory.getLogger(
                WorkflowExecutionTrendServiceImpl.class,
                WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExecutionTrendResponseDTO getExecutionTrend(ExecutionTrendRequestDTO requestDTO) {
        logger.info()
                .message(WorkflowExecutionTrendConstants.LOG_EXECUTION_TREND_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .detail(WorkflowExecutionTrendConstants.LOG_FIELD_MONTH_COUNT, requestDTO.getMonthCount().toString())
                .log();

        try {
            // Validate month count
            int monthCount = validateMonthCount(requestDTO.getMonthCount());

            logger.debug()
                    .message(WorkflowExecutionTrendConstants.LOG_EXECUTION_TREND_CALCULATION_START)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowExecutionTrendConstants.LOG_FIELD_MONTH_COUNT, String.valueOf(monthCount))
                    .log();

            // Calculate trend data
            List<MonthlyExecutionDataDTO> trendData = calculateMonthlyTrend(monthCount);

            logger.info()
                    .message(WorkflowExecutionTrendConstants.LOG_EXECUTION_TREND_CALCULATION_COMPLETE)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowExecutionTrendConstants.LOG_FIELD_MONTH_COUNT, String.valueOf(monthCount))
                    .detail(WorkflowExecutionTrendConstants.LOG_FIELD_DATA_POINTS, String.valueOf(trendData.size()))
                    .log();

            return ExecutionTrendResponseDTO.builder()
                    .monthCount(monthCount)
                    .trend(trendData)
                    .build();

        } catch (IllegalArgumentException e) {
            logger.error()
                    .message(WorkflowExecutionTrendConstants.LOG_EXECUTION_TREND_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowExecutionTrendConstants.LOG_FIELD_ERROR, e.getMessage())
                    .log();
            throw e;
        } catch (Exception e) {
            logger.error()
                    .message(WorkflowExecutionTrendConstants.LOG_EXECUTION_TREND_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowExecutionTrendConstants.LOG_FIELD_ERROR, e.getMessage())
                    .log();

            throw new RuntimeException(WorkflowExecutionTrendConstants.ERR_EXECUTION_TREND_CALCULATION_FAILED, e);
        }
    }

    /**
     * Validate month count against allowed values
     *
     * @param monthCount Requested month count
     * @return Validated month count
     * @throws IllegalArgumentException if month count is invalid
     */
    private int validateMonthCount(Integer monthCount) {
        if (monthCount == null) {
            logger.warn()
                    .message(WorkflowExecutionTrendConstants.LOG_INVALID_MONTH_COUNT)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowExecutionTrendConstants.LOG_FIELD_MONTH_COUNT, WorkflowExecutionTrendConstants.NULL_STRING)
                    .log();
            return WorkflowExecutionTrendConstants.DEFAULT_MONTH_COUNT;
        }

        boolean isValid = Arrays.stream(WorkflowExecutionTrendConstants.ALLOWED_MONTH_COUNTS)
                .anyMatch(allowed -> allowed == monthCount);

        if (!isValid) {
            logger.error()
                    .message(WorkflowExecutionTrendConstants.LOG_INVALID_MONTH_COUNT)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowExecutionTrendConstants.LOG_FIELD_MONTH_COUNT, monthCount.toString())
                    .log();
            throw new IllegalArgumentException(WorkflowExecutionTrendConstants.ERR_INVALID_MONTH_COUNT);
        }

        return monthCount;
    }

    /**
     * Calculate monthly execution trend data
     *
     * @param monthCount Number of months to include
     * @return List of monthly execution data in chronological order
     */
    private List<MonthlyExecutionDataDTO> calculateMonthlyTrend(int monthCount) {
        List<MonthlyExecutionDataDTO> trendData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(WorkflowExecutionTrendConstants.MONTH_FORMAT_SHORT);

        // Get current month
        YearMonth currentMonth = YearMonth.now();

        // Calculate trend for each month (from oldest to newest)
        for (int i = monthCount - WorkflowExecutionTrendConstants.LOOP_DECREMENT;
             i >= WorkflowExecutionTrendConstants.LOOP_START;
             i--) {
            YearMonth targetMonth = currentMonth.minusMonths(i);

            // Calculate start and end of month
            OffsetDateTime monthStart = targetMonth
                    .atDay(WorkflowExecutionTrendConstants.FIRST_DAY_OF_MONTH)
                    .atTime(
                            WorkflowExecutionTrendConstants.START_HOUR,
                            WorkflowExecutionTrendConstants.START_MINUTE,
                            WorkflowExecutionTrendConstants.START_SECOND
                    )
                    .atOffset(OffsetDateTime.now().getOffset());

            OffsetDateTime monthEnd = targetMonth
                    .atEndOfMonth()
                    .atTime(
                            WorkflowExecutionTrendConstants.END_HOUR,
                            WorkflowExecutionTrendConstants.END_MINUTE,
                            WorkflowExecutionTrendConstants.END_SECOND
                    )
                    .atOffset(OffsetDateTime.now().getOffset());

            // Query executions for this month
            Long totalExecutions = executionRepository.countByStartedAtBetween(monthStart, monthEnd);

            Long failedExecutions = executionRepository.countByStatusAndStartedAtBetween(
                    WorkflowExecution.ExecutionStatus.FAILED,
                    monthStart,
                    monthEnd
            );

            // Format month label
            String monthLabel = targetMonth.atDay(WorkflowExecutionTrendConstants.FIRST_DAY_OF_MONTH).format(formatter);

            trendData.add(MonthlyExecutionDataDTO.builder()
                    .month(monthLabel)
                    .executions(totalExecutions != null ? totalExecutions : WorkflowExecutionTrendConstants.DEFAULT_EXECUTION_COUNT)
                    .failures(failedExecutions != null ? failedExecutions : WorkflowExecutionTrendConstants.DEFAULT_FAILURE_COUNT)
                    .build()
            );
        }

        return trendData;
    }
}