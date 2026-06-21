package com.aidentx.workflowsservice.config.analytics;

/**
 * Constants for Workflow KPI Drilldown API
 *
 * Contains all static strings, messages, and configuration values
 * used throughout the KPI drilldown analytics module.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public final class WorkflowKpiDrilldownConstants {

    // ==================== API Endpoints ====================

    /**
     * Endpoint for retrieving workflow KPI drilldown data
     */
    public static final String ENDPOINT_KPI_DRILLDOWN = "/kpi-drilldown";

    // ==================== Success Messages ====================

    /**
     * Success message when KPI drilldown data is fetched successfully
     */
    public static final String MSG_KPI_DRILLDOWN_FETCHED_SUCCESS = "Workflow KPI drilldown data fetched successfully";

    // ==================== Error Messages ====================

    /**
     * Error message when invalid KPI type is provided
     */
    public static final String ERR_INVALID_KPI_TYPE = "Invalid KPI type. Allowed values: TOTAL_WORKFLOWS, ACTIVE_DRAFT_WORKFLOWS, AUTOMATED_MANUAL_WORKFLOWS, SUCCESS_RATE_WORKFLOWS, AVERAGE_EXECUTION_TIME_WORKFLOWS, FAILED_EXECUTION_WORKFLOWS, HIGH_RISK_WORKFLOWS, AI_DRIVEN_WORKFLOWS";

    /**
     * Error message when KPI drilldown calculation fails
     */
    public static final String ERR_KPI_DRILLDOWN_CALCULATION_FAILED = "Failed to fetch KPI drilldown data";

    // ==================== KPI Type Values ====================

    /**
     * KPI Type: Total Workflows
     */
    public static final String KPI_TYPE_TOTAL_WORKFLOWS = "TOTAL_WORKFLOWS";

    /**
     * KPI Type: Active and Draft Workflows (combined)
     */
    public static final String KPI_TYPE_ACTIVE_DRAFT_WORKFLOWS = "ACTIVE_DRAFT_WORKFLOWS";

    /**
     * KPI Type: Automated and Manual Workflows (combined)
     */
    public static final String KPI_TYPE_AUTOMATED_MANUAL_WORKFLOWS = "AUTOMATED_MANUAL_WORKFLOWS";

    /**
     * KPI Type: Success Rate Workflows (workflows that contributed to success rate)
     */
    public static final String KPI_TYPE_SUCCESS_RATE_WORKFLOWS = "SUCCESS_RATE_WORKFLOWS";

    /**
     * KPI Type: Average Execution Time Workflows (sorted by average completion time)
     */
    public static final String KPI_TYPE_AVERAGE_EXECUTION_TIME_WORKFLOWS = "AVERAGE_EXECUTION_TIME_WORKFLOWS";

    /**
     * KPI Type: Failed Execution Workflows (workflows with failed executions)
     */
    public static final String KPI_TYPE_FAILED_EXECUTION_WORKFLOWS = "FAILED_EXECUTION_WORKFLOWS";

    /**
     * KPI Type: High-Risk Workflows
     */
    public static final String KPI_TYPE_HIGH_RISK_WORKFLOWS = "HIGH_RISK_WORKFLOWS";

    /**
     * KPI Type: AI-Driven Workflows
     */
    public static final String KPI_TYPE_AI_DRIVEN_WORKFLOWS = "AI_DRIVEN_WORKFLOWS";

    // ==================== Default Values ====================

    /**
     * Default page number for pagination
     */
    public static final int DEFAULT_PAGE = 0;

    /**
     * Default page size for pagination
     */
    public static final int DEFAULT_PAGE_SIZE = 50;

    /**
     * Maximum page size for pagination
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * High risk score threshold
     */
    public static final int HIGH_RISK_THRESHOLD = 7;

    /**
     * Default execution count when null
     */
    public static final long DEFAULT_EXECUTION_COUNT = 0L;

    /**
     * Default average execution time when null (in milliseconds)
     */
    public static final double DEFAULT_AVG_EXECUTION_TIME = 0.0;

    /**
     * Empty string constant
     */
    public static final String EMPTY_STRING = "";

    // ==================== Sort Values ====================

    /**
     * Sort by workflow name
     */
    public static final String SORT_BY_NAME = "name";

    /**
     * Sort by risk score
     */
    public static final String SORT_BY_RISK_SCORE = "riskScore";

    /**
     * Sort by execution count
     */
    public static final String SORT_BY_EXECUTION_COUNT = "executionCount";

    /**
     * Sort by created date
     */
    public static final String SORT_BY_CREATED_AT = "createdAt";

    /**
     * Sort by average execution time
     */
    public static final String SORT_BY_AVG_EXECUTION_TIME = "avgExecutionTime";

    /**
     * Default sort field
     */
    public static final String DEFAULT_SORT_BY = SORT_BY_NAME;

    /**
     * Default sort direction
     */
    public static final String DEFAULT_SORT_DIRECTION = "ASC";

    /**
     * Sort direction: Descending
     */
    public static final String SORT_DIRECTION_DESC = "DESC";

    /**
     * Sort direction: Ascending
     */
    public static final String SORT_DIRECTION_ASC = "ASC";

    // ==================== Display Name Constants ====================

    /**
     * Display name for Joiner Mover Leaver category
     */
    public static final String DISPLAY_NAME_JML = "Joiner Mover Leaver";

    /**
     * Display name for Access Request category
     */
    public static final String DISPLAY_NAME_ACCESS_REQUEST = "Access Request";

    /**
     * Display name for Certification category
     */
    public static final String DISPLAY_NAME_CERTIFICATION = "Certification";

    /**
     * Display name for Revocation category
     */
    public static final String DISPLAY_NAME_REVOCATION = "Revocation";

    /**
     * Display name for Exception category
     */
    public static final String DISPLAY_NAME_EXCEPTION = "Exception";

    /**
     * Display name for AI Risk category
     */
    public static final String DISPLAY_NAME_AI_RISK = "AI Risk";

    /**
     * Display name for Event Based trigger
     */
    public static final String DISPLAY_NAME_EVENT_BASED = "Event Based";

    /**
     * Display name for Time Based trigger
     */
    public static final String DISPLAY_NAME_TIME_BASED = "Time Based";

    /**
     * Display name for Manual trigger
     */
    public static final String DISPLAY_NAME_MANUAL = "Manual";

    // ==================== Logging Constants ====================

    /**
     * Log message when KPI drilldown request is received
     */
    public static final String LOG_KPI_DRILLDOWN_REQUEST_RECEIVED = "KPI drilldown request received for KPI type: {}";

    /**
     * Log message when KPI drilldown calculation starts
     */
    public static final String LOG_KPI_DRILLDOWN_CALCULATION_START = "Starting KPI drilldown calculation for type: {}";

    /**
     * Log message when KPI drilldown calculation completes
     */
    public static final String LOG_KPI_DRILLDOWN_CALCULATION_COMPLETE = "KPI drilldown calculation completed. Total workflows: {}, Page: {}";

    /**
     * Log message when KPI drilldown calculation fails
     */
    public static final String LOG_KPI_DRILLDOWN_CALCULATION_ERROR = "Error calculating KPI drilldown: {}";

    /**
     * Log message when invalid KPI type is provided
     */
    public static final String LOG_INVALID_KPI_TYPE = "Invalid KPI type provided: {}";

    /**
     * Field name for KPI type in log details
     */
    public static final String LOG_FIELD_KPI_TYPE = "kpiType";

    /**
     * Field name for page in log details
     */
    public static final String LOG_FIELD_PAGE = "page";

    /**
     * Field name for page size in log details
     */
    public static final String LOG_FIELD_PAGE_SIZE = "pageSize";

    /**
     * Field name for sort by in log details
     */
    public static final String LOG_FIELD_SORT_BY = "sortBy";

    /**
     * Field name for sort direction in log details
     */
    public static final String LOG_FIELD_SORT_DIRECTION = "sortDirection";

    /**
     * Field name for total workflows in log details
     */
    public static final String LOG_FIELD_TOTAL_WORKFLOWS = "totalWorkflows";

    /**
     * Field name for current page in log details
     */
    public static final String LOG_FIELD_CURRENT_PAGE = "currentPage";

    /**
     * Field name for error in log details
     */
    public static final String LOG_FIELD_ERROR = "error";

    /**
     * Field name for average execution time in log details
     */
    public static final String LOG_FIELD_AVG_EXECUTION_TIME = "avgExecutionTime";

    // ==================== Private Constructor ====================

    /**
     * Private constructor to prevent instantiation
     */
    private WorkflowKpiDrilldownConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}