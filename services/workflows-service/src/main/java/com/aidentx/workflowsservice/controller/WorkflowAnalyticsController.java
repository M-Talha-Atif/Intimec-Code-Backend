package com.aidentx.workflowsservice.controller;

import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.aidentx.workflowsservice.config.analytics.WorkflowAnalyticsConstants;
import com.aidentx.workflowsservice.config.analytics.WorkflowCategoryDistributionConstants;
import com.aidentx.workflowsservice.config.analytics.WorkflowExecutionDistributionConstants;
import com.aidentx.workflowsservice.config.analytics.WorkflowExecutionTrendConstants;
import com.aidentx.workflowsservice.config.analytics.WorkflowKpiDrilldownConstants;
import com.aidentx.workflowsservice.dto.request.ExecutionDistributionRequestDTO;
import com.aidentx.workflowsservice.dto.request.ExecutionTrendRequestDTO;
import com.aidentx.workflowsservice.dto.request.KpiDrilldownRequestDTO;
import com.aidentx.workflowsservice.dto.request.WorkflowAnalyticsRequestDTO;
import com.aidentx.workflowsservice.dto.response.ApiResponse;
import com.aidentx.workflowsservice.dto.response.CategoryDistributionResponseDTO;
import com.aidentx.workflowsservice.dto.response.ExecutionDistributionResponseDTO;
import com.aidentx.workflowsservice.dto.response.ExecutionTrendResponseDTO;
import com.aidentx.workflowsservice.dto.response.KpiDrilldownResponseDTO;
import com.aidentx.workflowsservice.dto.response.WorkflowAnalyticsResponseDTO;
import com.aidentx.workflowsservice.service.WorkflowAnalyticsService;
import com.aidentx.workflowsservice.service.WorkflowCategoryDistributionService;
import com.aidentx.workflowsservice.service.WorkflowExecutionDistributionService;
import com.aidentx.workflowsservice.service.WorkflowExecutionTrendService;
import com.aidentx.workflowsservice.service.WorkflowKpiDrilldownService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Workflow Analytics API
 * <p>
 * Provides endpoints for retrieving workflow analytics,
 * KPIs, and performance metrics.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
@RestController
@RequestMapping(WorkflowAnalyticsConstants.API_BASE_PATH)
public class WorkflowAnalyticsController {

    private final IUnifiedLogger logger;
    private final WorkflowAnalyticsService analyticsService;
    private final WorkflowExecutionTrendService executionTrendService;
    private final WorkflowExecutionDistributionService executionDistributionService;
    private final WorkflowCategoryDistributionService categoryDistributionService;
    private final WorkflowKpiDrilldownService kpiDrilldownService;

    public WorkflowAnalyticsController(
            WorkflowAnalyticsService analyticsService,
            WorkflowExecutionTrendService executionTrendService,
            WorkflowExecutionDistributionService executionDistributionService,
            WorkflowCategoryDistributionService categoryDistributionService,
            WorkflowKpiDrilldownService kpiDrilldownService,
            UnifiedLoggerFactory loggerFactory
    ) {
        this.analyticsService = analyticsService;
        this.executionTrendService = executionTrendService;
        this.executionDistributionService = executionDistributionService;
        this.categoryDistributionService = categoryDistributionService;
        this.kpiDrilldownService = kpiDrilldownService;
        this.logger = loggerFactory.getLogger(WorkflowAnalyticsController.class, WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS);
    }

    /**
     * Get workflow analytics overview
     * <p>
     * Returns comprehensive analytics including workflow counts,
     * execution statistics, success rates, and performance metrics.
     *
     * @param timePeriod Time period for analytics (optional, defaults to LAST_30_DAYS)
     *                   Allowed values: LAST_24_HOURS, LAST_30_DAYS, LAST_60_DAYS, LAST_90_DAYS, ALL_TIME
     * @return ResponseEntity with ApiResponse containing WorkflowAnalyticsResponseDTO
     */
    @GetMapping(WorkflowAnalyticsConstants.ENDPOINT_OVERVIEW)
    public ResponseEntity<ApiResponse<WorkflowAnalyticsResponseDTO>> getAnalyticsOverview(
            @RequestParam(required = false, defaultValue = WorkflowAnalyticsConstants.DEFAULT_TIME_PERIOD)
            String timePeriod
    ) {
        logger.info()
                .message(WorkflowAnalyticsConstants.LOG_ANALYTICS_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .detail("timePeriod", timePeriod)
                .log();

        try {
            WorkflowAnalyticsRequestDTO requestDTO = WorkflowAnalyticsRequestDTO.builder()
                    .timePeriod(timePeriod)
                    .build();

            WorkflowAnalyticsResponseDTO analytics = analyticsService.getAnalyticsOverview(requestDTO);

            logger.info()
                    .message(WorkflowAnalyticsConstants.MSG_ANALYTICS_OVERVIEW_FETCHED_SUCCESS)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail("timePeriod", timePeriod)
                    .detail("totalWorkflows", analytics.getTotalWorkflows().toString())
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            WorkflowAnalyticsConstants.MSG_ANALYTICS_FETCHED_SUCCESS,
                            analytics
                    )
            );

        } catch (Exception e) {
            logger.error()
                    .message(WorkflowAnalyticsConstants.LOG_ANALYTICS_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail("timePeriod", timePeriod)
                    .detail("error", e.getMessage())
                    .log();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            WorkflowAnalyticsConstants.ERR_ANALYTICS_CALCULATION_FAILED
                    ));
        }
    }

    /**
     * Get workflow execution trend
     * <p>
     * Returns monthly execution statistics including total executions
     * and failure counts for the specified number of months.
     *
     * @param monthCount Number of months to include in trend (optional, defaults to 6)
     *                   Allowed values: 5, 6, 7, 8, 10, 12
     * @return ResponseEntity with ApiResponse containing ExecutionTrendResponseDTO
     */
    @GetMapping(WorkflowExecutionTrendConstants.ENDPOINT_EXECUTION_TREND)
    public ResponseEntity<ApiResponse<ExecutionTrendResponseDTO>> getExecutionTrend(
            @RequestParam(required = false, defaultValue = "6") Integer monthCount
    ) {
        logger.info()
                .message(WorkflowExecutionTrendConstants.LOG_EXECUTION_TREND_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .detail("monthCount", monthCount.toString())
                .log();

        try {
            ExecutionTrendRequestDTO requestDTO = ExecutionTrendRequestDTO.builder()
                    .monthCount(monthCount)
                    .build();

            ExecutionTrendResponseDTO trend = executionTrendService.getExecutionTrend(requestDTO);

            logger.info()
                    .message(WorkflowExecutionTrendConstants.MSG_EXECUTION_TREND_FETCHED_SUCCESS)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail("monthCount", monthCount.toString())
                    .detail("dataPoints", String.valueOf(trend.getTrend().size()))
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            WorkflowExecutionTrendConstants.MSG_EXECUTION_TREND_FETCHED_SUCCESS,
                            trend
                    )
            );

        } catch (IllegalArgumentException e) {
            logger.warn()
                    .message(WorkflowExecutionTrendConstants.LOG_EXECUTION_TREND_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail("monthCount", monthCount.toString())
                    .detail("error", e.getMessage())
                    .log();

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));

        } catch (Exception e) {
            logger.error()
                    .message(WorkflowExecutionTrendConstants.LOG_EXECUTION_TREND_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail("monthCount", monthCount.toString())
                    .detail("error", e.getMessage())
                    .log();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            WorkflowExecutionTrendConstants.ERR_EXECUTION_TREND_CALCULATION_FAILED
                    ));
        }
    }

    /**
     * Get workflow execution distribution
     * <p>
     * Returns distribution of workflow executions by status
     * (Success, Failed, Pending) for the specified time period.
     *
     * @param timePeriod Time period for distribution (optional, defaults to LAST_30_DAYS)
     *                   Allowed values: LAST_24_HOURS, LAST_30_DAYS, LAST_60_DAYS, LAST_90_DAYS, ALL_TIME
     * @return ResponseEntity with ApiResponse containing ExecutionDistributionResponseDTO
     */
    @GetMapping(WorkflowExecutionDistributionConstants.ENDPOINT_EXECUTION_DISTRIBUTION)
    public ResponseEntity<ApiResponse<ExecutionDistributionResponseDTO>> getExecutionDistribution(
            @RequestParam(required = false, defaultValue = WorkflowExecutionDistributionConstants.DEFAULT_TIME_PERIOD)
            String timePeriod
    ) {
        logger.info()
                .message(WorkflowExecutionDistributionConstants.LOG_EXECUTION_DISTRIBUTION_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .detail("timePeriod", timePeriod)
                .log();

        try {
            ExecutionDistributionRequestDTO requestDTO = ExecutionDistributionRequestDTO.builder()
                    .timePeriod(timePeriod)
                    .build();

            ExecutionDistributionResponseDTO distribution = executionDistributionService.getExecutionDistribution(requestDTO);

            logger.info()
                    .message(WorkflowExecutionDistributionConstants.MSG_EXECUTION_DISTRIBUTION_FETCHED_SUCCESS)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail("timePeriod", timePeriod)
                    .detail("totalExecutions", distribution.getTotalExecutions().toString())
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            WorkflowExecutionDistributionConstants.MSG_EXECUTION_DISTRIBUTION_FETCHED_SUCCESS,
                            distribution
                    )
            );

        } catch (Exception e) {
            logger.error()
                    .message(WorkflowExecutionDistributionConstants.LOG_EXECUTION_DISTRIBUTION_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail("timePeriod", timePeriod)
                    .detail("error", e.getMessage())
                    .log();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            WorkflowExecutionDistributionConstants.ERR_EXECUTION_DISTRIBUTION_CALCULATION_FAILED
                    ));
        }
    }

    /**
     * Get workflow category distribution
     * <p>
     * Returns distribution of workflows across lifecycle categories
     * (JML, Access Request, Certification, Revocation, Exception, AI Risk).
     * <p>
     * Data is sorted by count in descending order.
     *
     * @return ResponseEntity with ApiResponse containing CategoryDistributionResponseDTO
     */
    @GetMapping(WorkflowCategoryDistributionConstants.ENDPOINT_CATEGORY_DISTRIBUTION)
    public ResponseEntity<ApiResponse<CategoryDistributionResponseDTO>> getCategoryDistribution() {
        logger.info()
                .message(WorkflowCategoryDistributionConstants.LOG_CATEGORY_DISTRIBUTION_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .log();

        try {
            CategoryDistributionResponseDTO distribution = categoryDistributionService.getCategoryDistribution();

            logger.info()
                    .message(WorkflowCategoryDistributionConstants.MSG_CATEGORY_DISTRIBUTION_FETCHED_SUCCESS)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail("totalWorkflows", distribution.getTotalWorkflows().toString())
                    .detail("categoryCount", distribution.getCategoryCount().toString())
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            WorkflowCategoryDistributionConstants.MSG_CATEGORY_DISTRIBUTION_FETCHED_SUCCESS,
                            distribution
                    )
            );

        } catch (Exception e) {
            logger.error()
                    .message(WorkflowCategoryDistributionConstants.LOG_CATEGORY_DISTRIBUTION_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail("error", e.getMessage())
                    .log();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            WorkflowCategoryDistributionConstants.ERR_CATEGORY_DISTRIBUTION_CALCULATION_FAILED
                    ));
        }
    }

    /**
     * Get workflow KPI drilldown data
     * <p>
     * Returns paginated list of workflows filtered by KPI type.
     * Used to populate the drilldown modal when users click on KPI cards.
     *
     * @param kpiType       KPI type to filter workflows (required)
     *                      Allowed values:
     *                      - TOTAL_WORKFLOWS: All workflows
     *                      - ACTIVE_DRAFT_WORKFLOWS: Active and Draft workflows (combined)
     *                      - AUTOMATED_MANUAL_WORKFLOWS: Automated and Manual workflows (combined)
     *                      - SUCCESS_RATE_WORKFLOWS: Workflows that contributed to success rate
     *                      - AVERAGE_EXECUTION_TIME_WORKFLOWS: Workflows sorted by average completion time
     *                      - FAILED_EXECUTION_WORKFLOWS: Workflows with failed executions
     *                      - HIGH_RISK_WORKFLOWS: Workflows with risk score >= 7
     *                      - AI_DRIVEN_WORKFLOWS: AI-Risk category workflows
     * @param page          Page number (0-indexed, default: 0)
     * @param pageSize      Page size (default: 50, max: 100)
     * @param sortBy        Field to sort by (default: name)
     *                      Allowed values: name, riskScore, executionCount, createdAt, avgExecutionTime
     * @param sortDirection Sort direction (default: ASC)
     *                      Allowed values: ASC, DESC
     * @return ResponseEntity with ApiResponse containing KpiDrilldownResponseDTO
     */
    @GetMapping(WorkflowKpiDrilldownConstants.ENDPOINT_KPI_DRILLDOWN)
    public ResponseEntity<ApiResponse<KpiDrilldownResponseDTO>> getKpiDrilldown(
            @RequestParam(required = true) String kpiType,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "50") Integer pageSize,
            @RequestParam(required = false, defaultValue = "name") String sortBy,
            @RequestParam(required = false, defaultValue = "ASC") String sortDirection
    ) {
        logger.info()
                .message(WorkflowKpiDrilldownConstants.LOG_KPI_DRILLDOWN_REQUEST_RECEIVED)
                .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_KPI_TYPE, kpiType)
                .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_PAGE, page.toString())
                .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_PAGE_SIZE, pageSize.toString())
                .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_SORT_BY, sortBy)
                .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_SORT_DIRECTION, sortDirection)
                .log();

        try {
            KpiDrilldownRequestDTO requestDTO = KpiDrilldownRequestDTO.builder()
                    .kpiType(kpiType)
                    .page(page)
                    .pageSize(pageSize)
                    .sortBy(sortBy)
                    .sortDirection(sortDirection)
                    .build();

            KpiDrilldownResponseDTO drilldown = kpiDrilldownService.getKpiDrilldown(requestDTO);

            logger.info()
                    .message(WorkflowKpiDrilldownConstants.MSG_KPI_DRILLDOWN_FETCHED_SUCCESS)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_KPI_TYPE, kpiType)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_TOTAL_WORKFLOWS, drilldown.getTotalWorkflows().toString())
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_CURRENT_PAGE, drilldown.getCurrentPage().toString())
                    .log();

            return ResponseEntity.ok(
                    ApiResponse.success(
                            WorkflowKpiDrilldownConstants.MSG_KPI_DRILLDOWN_FETCHED_SUCCESS,
                            drilldown
                    )
            );
        } catch (IllegalArgumentException e) {
            logger.warn()
                    .message(WorkflowKpiDrilldownConstants.LOG_KPI_DRILLDOWN_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_KPI_TYPE, kpiType)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_ERROR, e.getMessage())
                    .log();

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error()
                    .message(WorkflowKpiDrilldownConstants.LOG_KPI_DRILLDOWN_CALCULATION_ERROR)
                    .category(WorkflowAnalyticsConstants.LOG_CATEGORY_WORKFLOW_ANALYTICS)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_KPI_TYPE, kpiType)
                    .detail(WorkflowKpiDrilldownConstants.LOG_FIELD_ERROR, e.getMessage())
                    .log();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(
                            WorkflowKpiDrilldownConstants.ERR_KPI_DRILLDOWN_CALCULATION_FAILED
                    ));
        }
    }
}