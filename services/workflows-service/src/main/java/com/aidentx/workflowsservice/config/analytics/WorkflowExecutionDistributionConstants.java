package com.aidentx.workflowsservice.config.analytics;

/**
 * Constants for Workflow Execution Distribution API
 *
 * Contains all static strings, messages, and configuration values
 * used throughout the execution distribution analytics module.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public final class WorkflowExecutionDistributionConstants {

    // ==================== API Endpoints ====================

    /**
     * Endpoint for retrieving workflow execution distribution
     */
    public static final String ENDPOINT_EXECUTION_DISTRIBUTION = "/execution-distribution";

    // ==================== Success Messages ====================

    /**
     * Success message when execution distribution data is fetched successfully
     */
    public static final String MSG_EXECUTION_DISTRIBUTION_FETCHED_SUCCESS = "Workflow execution distribution fetched successfully";

    // ==================== Error Messages ====================

    /**
     * Error message when invalid time period is provided
     */
    public static final String ERR_INVALID_TIME_PERIOD = "Invalid time period. Allowed values: LAST_24_HOURS, LAST_30_DAYS, LAST_60_DAYS, LAST_90_DAYS, ALL_TIME";

    /**
     * Error message when execution distribution calculation fails
     */
    public static final String ERR_EXECUTION_DISTRIBUTION_CALCULATION_FAILED = "Failed to calculate execution distribution";

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
     * Default time period for execution distribution
     */
    public static final String DEFAULT_TIME_PERIOD = PERIOD_LAST_30_DAYS;

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

    // ==================== Distribution Category Names ====================

    /**
     * Category name for successful executions
     */
    public static final String CATEGORY_SUCCESS = "Success";

    /**
     * Category name for failed executions
     */
    public static final String CATEGORY_FAILED = "Failed";

    /**
     * Category name for pending executions
     */
    public static final String CATEGORY_PENDING = "Pending";

    // ==================== Calculation Constants ====================

    /**
     * Initial value for counting executions
     */
    public static final long INITIAL_COUNT = 0L;

    /**
     * Default percentage when total is zero
     */
    public static final double DEFAULT_PERCENTAGE = 0.0;

    /**
     * Multiplier for converting decimal to percentage
     */
    public static final double PERCENTAGE_MULTIPLIER = 100.0;

    /**
     * Scale for rounding percentage values (2 decimal places)
     */
    public static final double PERCENTAGE_SCALE = 100.0;

    // ==================== Logging Constants ====================

    /**
     * Log message when execution distribution request is received
     */
    public static final String LOG_EXECUTION_DISTRIBUTION_REQUEST_RECEIVED = "Execution distribution request received for time period: {}";

    /**
     * Log message when execution distribution calculation starts
     */
    public static final String LOG_EXECUTION_DISTRIBUTION_CALCULATION_START = "Starting execution distribution calculation for period: {}";

    /**
     * Log message when execution distribution calculation completes
     */
    public static final String LOG_EXECUTION_DISTRIBUTION_CALCULATION_COMPLETE = "Execution distribution calculation completed. Success: {}, Failed: {}, Pending: {}";

    /**
     * Log message when execution distribution calculation fails
     */
    public static final String LOG_EXECUTION_DISTRIBUTION_CALCULATION_ERROR = "Error calculating execution distribution: {}";

    /**
     * Log message when invalid time period is provided
     */
    public static final String LOG_INVALID_TIME_PERIOD = "Invalid time period provided: {}. Using default: {}";

    /**
     * Field name for time period in log details
     */
    public static final String LOG_FIELD_TIME_PERIOD = "timePeriod";

    /**
     * Field name for total executions in log details
     */
    public static final String LOG_FIELD_TOTAL_EXECUTIONS = "totalExecutions";

    /**
     * Field name for error in log details
     */
    public static final String LOG_FIELD_ERROR = "error";

    // ==================== Private Constructor ====================

    /**
     * Private constructor to prevent instantiation
     */
    private WorkflowExecutionDistributionConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}