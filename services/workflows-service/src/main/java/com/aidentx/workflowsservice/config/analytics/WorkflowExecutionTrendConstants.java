package com.aidentx.workflowsservice.config.analytics;

/**
 * Constants for Workflow Execution Trend API
 *
 * Contains all static strings, messages, and configuration values
 * used throughout the execution trend analytics module.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public final class WorkflowExecutionTrendConstants {

    // ==================== API Endpoints ====================

    /**
     * Endpoint for retrieving workflow execution trends
     */
    public static final String ENDPOINT_EXECUTION_TREND = "/execution-trend";

    // ==================== Success Messages ====================

    /**
     * Success message when execution trend data is fetched successfully
     */
    public static final String MSG_EXECUTION_TREND_FETCHED_SUCCESS = "Workflow execution trend fetched successfully";

    // ==================== Error Messages ====================

    /**
     * Error message when invalid month count is provided
     */
    public static final String ERR_INVALID_MONTH_COUNT = "Invalid month count. Allowed values: 5, 6, 7, 8, 10, 12";

    /**
     * Error message when execution trend calculation fails
     */
    public static final String ERR_EXECUTION_TREND_CALCULATION_FAILED = "Failed to calculate execution trend";

    // ==================== Month Count Configuration ====================

    /**
     * Default number of months for execution trend
     */
    public static final int DEFAULT_MONTH_COUNT = 6;

    /**
     * Minimum allowed month count
     */
    public static final int MIN_MONTH_COUNT = 5;

    /**
     * Maximum allowed month count
     */
    public static final int MAX_MONTH_COUNT = 12;

    /**
     * Allowed month counts for execution trend
     */
    public static final int[] ALLOWED_MONTH_COUNTS = {5, 6, 7, 8, 10, 12};

    // ==================== Date/Time Constants ====================

    /**
     * Month format for execution trend labels (e.g., "Jan", "Feb", "Mar")
     */
    public static final String MONTH_FORMAT_SHORT = "MMM";

    /**
     * Month-year format for detailed labels (e.g., "Jan 2024", "Feb 2024")
     */
    public static final String MONTH_FORMAT_FULL = "MMM yyyy";

    /**
     * First day of month
     */
    public static final int FIRST_DAY_OF_MONTH = 1;

    /**
     * Start hour of day
     */
    public static final int START_HOUR = 0;

    /**
     * Start minute of day
     */
    public static final int START_MINUTE = 0;

    /**
     * Start second of day
     */
    public static final int START_SECOND = 0;

    /**
     * End hour of day
     */
    public static final int END_HOUR = 23;

    /**
     * End minute of day
     */
    public static final int END_MINUTE = 59;

    /**
     * End second of day
     */
    public static final int END_SECOND = 59;

    /**
     * Decrement value for loop iteration
     */
    public static final int LOOP_DECREMENT = 1;

    /**
     * Initial loop index
     */
    public static final int LOOP_START = 0;

    // ==================== Default Values ====================

    /**
     * Default execution count when null
     */
    public static final long DEFAULT_EXECUTION_COUNT = 0L;

    /**
     * Default failure count when null
     */
    public static final long DEFAULT_FAILURE_COUNT = 0L;

    /**
     * Null string representation for logging
     */
    public static final String NULL_STRING = "null";

    // ==================== Logging Constants ====================

    /**
     * Log message when execution trend request is received
     */
    public static final String LOG_EXECUTION_TREND_REQUEST_RECEIVED = "Execution trend request received for {} months";

    /**
     * Log message when execution trend calculation starts
     */
    public static final String LOG_EXECUTION_TREND_CALCULATION_START = "Starting execution trend calculation for {} months";

    /**
     * Log message when execution trend calculation completes
     */
    public static final String LOG_EXECUTION_TREND_CALCULATION_COMPLETE = "Execution trend calculation completed. Data points: {}";

    /**
     * Log message when execution trend calculation fails
     */
    public static final String LOG_EXECUTION_TREND_CALCULATION_ERROR = "Error calculating execution trend: {}";

    /**
     * Log message when invalid month count is provided
     */
    public static final String LOG_INVALID_MONTH_COUNT = "Invalid month count provided: {}. Using default: {}";

    /**
     * Field name for month count in log details
     */
    public static final String LOG_FIELD_MONTH_COUNT = "monthCount";

    /**
     * Field name for data points in log details
     */
    public static final String LOG_FIELD_DATA_POINTS = "dataPoints";

    /**
     * Field name for error in log details
     */
    public static final String LOG_FIELD_ERROR = "error";

    // ==================== Private Constructor ====================

    /**
     * Private constructor to prevent instantiation
     */
    private WorkflowExecutionTrendConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}