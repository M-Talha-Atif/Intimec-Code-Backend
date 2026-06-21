package com.aidentx.workflowsservice.config.analytics;

/**
 * Constants for Workflow Analytics API
 * <p>
 * Contains all static strings, messages, and configuration values
 * used throughout the analytics module.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public final class WorkflowAnalyticsConstants {

    // ==================== API Endpoints ====================

    /**
     * Base path for analytics API endpoints
     */
    public static final String API_BASE_PATH = "/api/v1/workflows/analytics";

    /**
     * Endpoint for retrieving workflow analytics overview
     */
    public static final String ENDPOINT_OVERVIEW = "/overview";

    // ==================== Success Messages ====================

    /**
     * Success message when analytics data is fetched successfully
     */
    public static final String MSG_ANALYTICS_FETCHED_SUCCESS = "Workflow analytics fetched successfully";

    /**
     * Success message when analytics overview is fetched successfully
     */
    public static final String MSG_ANALYTICS_OVERVIEW_FETCHED_SUCCESS = "Analytics overview fetched successfully";

    // ==================== Error Messages ====================

    /**
     * Error message when invalid time period is provided
     */
    public static final String ERR_INVALID_TIME_PERIOD = "Invalid time period. Allowed values: LAST_24_HOURS, LAST_30_DAYS, LAST_60_DAYS, LAST_90_DAYS, ALL_TIME";

    /**
     * Error message when analytics calculation fails
     */
    public static final String ERR_ANALYTICS_CALCULATION_FAILED = "Failed to calculate workflow analytics";

    // ==================== Time Period Values ====================

    /**
     * Time period: Last 24 hours
     */
    public static final String PERIOD_LAST_24_HOURS = "LAST_24_HOURS";

    /**
     * Time period: Last 30 days (default)
     */
    public static final String PERIOD_LAST_30_DAYS = "LAST_30_DAYS";

    /**
     * Time period: Last 60 days
     */
    public static final String PERIOD_LAST_60_DAYS = "LAST_60_DAYS";

    /**
     * Time period: Last 90 days
     */
    public static final String PERIOD_LAST_90_DAYS = "LAST_90_DAYS";

    /**
     * Time period: All time
     */
    public static final String PERIOD_ALL_TIME = "ALL_TIME";

    // ==================== Default Values ====================

    /**
     * Loop start value - used for checking empty or zero executions
     */
    public static final long LOOP_START = 0L;

    /**
     * Default time period for analytics
     */
    public static final String DEFAULT_TIME_PERIOD = PERIOD_LAST_30_DAYS;

    /**
     * Default success rate when no executions exist (percentage)
     */
    public static final double DEFAULT_SUCCESS_RATE = 0.0;

    /**
     * Default average execution time when no executions exist (hours)
     */
    public static final double DEFAULT_AVG_EXECUTION_TIME = 0.0;

    /**
     * Default number of days to subtract for 30-day period
     */
    public static final int DAYS_30 = 30;

    /**
     * Default number of days to subtract for 60-day period
     */
    public static final int DAYS_60 = 60;

    /**
     * Default number of days to subtract for 90-day period
     */
    public static final int DAYS_90 = 90;

    /**
     * Default number of hours to subtract for 24-hour period
     */
    public static final int HOURS_24 = 24;

    // ==================== KPI Calculation Constants ====================

    /**
     * Minimum risk score threshold for high-risk workflows
     */
    public static final int HIGH_RISK_THRESHOLD = 7;

    /**
     * Multiplier for converting decimal to percentage
     */
    public static final double PERCENTAGE_MULTIPLIER = 100.0;

    /**
     * Scale for rounding percentage values (2 decimal places)
     */
    public static final double PERCENTAGE_SCALE = 100.0;

    /**
     * Scale for rounding hour values (1 decimal place)
     */
    public static final double HOUR_SCALE = 10.0;

    /**
     * Milliseconds in one hour
     */
    public static final double MS_PER_HOUR = 1000.0 * 60.0 * 60.0;

    // ==================== Logging Constants ====================

    /**
     * Log category for workflow analytics operations
     */
    public static final String LOG_CATEGORY_WORKFLOW_ANALYTICS = "WorkflowAnalytics";

    /**
     * Log message when analytics request is received
     */
    public static final String LOG_ANALYTICS_REQUEST_RECEIVED = "Analytics request received for time period: {}";

    /**
     * Log message when analytics calculation starts
     */
    public static final String LOG_ANALYTICS_CALCULATION_START = "Starting analytics calculation for period: {}";

    /**
     * Log message when analytics calculation completes
     */
    public static final String LOG_ANALYTICS_CALCULATION_COMPLETE = "Analytics calculation completed. Total workflows: {}, Success rate: {}%";

    /**
     * Log message when analytics calculation fails
     */
    public static final String LOG_ANALYTICS_CALCULATION_ERROR = "Error calculating analytics: {}";

    /**
     * Log message when invalid time period is provided
     */
    public static final String LOG_INVALID_TIME_PERIOD = "Invalid time period provided: {}. Using default: {}";

    /**
     * Field name for time period in log details
     */
    public static final String LOG_FIELD_TIME_PERIOD = "timePeriod";

    /**
     * Field name for total workflows in log details
     */
    public static final String LOG_FIELD_TOTAL_WORKFLOWS = "totalWorkflows";

    /**
     * Field name for error in log details
     */
    public static final String LOG_FIELD_ERROR = "error";

    /**
     * Format string for success rate (2 decimal places)
     */
    public static final String FORMAT_SUCCESS_RATE = "%.2f";

    // ==================== Private Constructor ====================

    /**
     * Private constructor to prevent instantiation
     */
    private WorkflowAnalyticsConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}