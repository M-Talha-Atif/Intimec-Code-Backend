package com.aidentx.workflowsservice.config.analytics;

/**
 * Constants for Workflow Category Distribution API
 *
 * Contains all static strings, messages, and configuration values
 * used throughout the category distribution analytics module.
 *
 * @author AidentX Development Team
 * @version 1.0
 * @since 2024
 */
public final class WorkflowCategoryDistributionConstants {

    // ==================== API Endpoints ====================

    /**
     * Endpoint for retrieving workflow category distribution
     */
    public static final String ENDPOINT_CATEGORY_DISTRIBUTION = "/category-distribution";

    // ==================== Success Messages ====================

    /**
     * Success message when category distribution data is fetched successfully
     */
    public static final String MSG_CATEGORY_DISTRIBUTION_FETCHED_SUCCESS = "Workflow category distribution fetched successfully";

    // ==================== Error Messages ====================

    /**
     * Error message when category distribution calculation fails
     */
    public static final String ERR_CATEGORY_DISTRIBUTION_CALCULATION_FAILED = "Failed to calculate category distribution";

    // ==================== Category Display Names ====================

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

    // ==================== Calculation Constants ====================

    /**
     * Minimum count threshold for including a category in results
     */
    public static final int MIN_COUNT_THRESHOLD = 0;

    /**
     * Default value when total workflows is zero
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

    /**
     * Initial value for counting workflows
     */
    public static final long INITIAL_COUNT = 0L;

    /**
     * Increment value for counting workflows
     */
    public static final long COUNT_INCREMENT = 1L;

    // ==================== Logging Constants ====================

    /**
     * Log message when category distribution request is received
     */
    public static final String LOG_CATEGORY_DISTRIBUTION_REQUEST_RECEIVED = "Category distribution request received";

    /**
     * Log message when category distribution calculation starts
     */
    public static final String LOG_CATEGORY_DISTRIBUTION_CALCULATION_START = "Starting category distribution calculation";

    /**
     * Log message when category distribution calculation completes
     */
    public static final String LOG_CATEGORY_DISTRIBUTION_CALCULATION_COMPLETE = "Category distribution calculation completed. Total categories: {}";

    /**
     * Log message when category distribution calculation fails
     */
    public static final String LOG_CATEGORY_DISTRIBUTION_CALCULATION_ERROR = "Error calculating category distribution: {}";

    /**
     * Field name for total workflows in log details
     */
    public static final String LOG_FIELD_TOTAL_WORKFLOWS = "totalWorkflows";

    /**
     * Field name for category count in log details
     */
    public static final String LOG_FIELD_CATEGORY_COUNT = "categoryCount";

    /**
     * Field name for error in log details
     */
    public static final String LOG_FIELD_ERROR = "error";

    // ==================== Private Constructor ====================

    /**
     * Private constructor to prevent instantiation
     */
    private WorkflowCategoryDistributionConstants() {
        throw new UnsupportedOperationException("This is a constants class and cannot be instantiated");
    }
}