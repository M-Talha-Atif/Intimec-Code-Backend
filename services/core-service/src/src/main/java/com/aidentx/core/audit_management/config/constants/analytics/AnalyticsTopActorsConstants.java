package com.aidentx.core.audit_management.config.constants.analytics;

/**
 * Constants for the top actors analytics computation.
 *
 * <p>Risk badge formula:
 * <pre>
 *   weightedAvg = weightedRiskSum / totalEvents
 * </pre>
 *
 * <p>Badge assignment uses ceiling rounding -- when the weighted average falls
 * exactly between two weights, the higher badge is assigned:
 * <ul>
 *   <li>weightedAvg in (0,  1.5) -- "low"    (closer to weight 1)</li>
 *   <li>weightedAvg in [1.5, 2.5) -- "medium" (1.5 rounds up to 2)</li>
 *   <li>weightedAvg in [2.5, 3.0] -- "high"   (2.5 rounds up to 3)</li>
 * </ul>
 *
 * <p>Example: 7 low + 3 high events (10 total):
 * <pre>
 *   weightedRiskSum = (7*1) + (3*3) = 16
 *   weightedAvg     = 16 / 10       = 1.6
 *   badge           = "medium"  (1.6 is in [1.5, 2.5))
 * </pre>
 */
public final class AnalyticsTopActorsConstants {

    private AnalyticsTopActorsConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path constant
    // -------------------------------------------------------------------------

    public static final String TOP_ACTORS_PATH = "/top-actors";

    // -------------------------------------------------------------------------
    // Limit constants
    // -------------------------------------------------------------------------

    public static final int DEFAULT_LIMIT = 10;
    public static final int LIMIT_ALL     = 0;
    public static final int MINIMUM_LIMIT = 1;

    // -------------------------------------------------------------------------
    // Risk level weight constants
    // -------------------------------------------------------------------------

    /**
     * Numeric weight assigned to events with risk level 'low'.
     */
    public static final long RISK_WEIGHT_LOW = 1L;

    /**
     * Numeric weight assigned to events with risk level 'medium'.
     */
    public static final long RISK_WEIGHT_MEDIUM = 2L;

    /**
     * Numeric weight assigned to events with risk level 'high'.
     * High is the maximum severity stored -- no critical level exists.
     */
    public static final long RISK_WEIGHT_HIGH = 3L;

    // -------------------------------------------------------------------------
    // Badge threshold constants
    // -------------------------------------------------------------------------

    /**
     * Lower boundary (inclusive) at which the weighted average is considered
     * "medium". Values at or above this threshold and below
     * {@link #RISK_BADGE_HIGH_THRESHOLD} map to the medium badge.
     *
     * <p>At exactly 1.5 the average is equidistant between low (1) and
     * medium (2). The higher badge is assigned per the ceiling rounding rule,
     * so 1.5 becomes "medium".
     */
    public static final double RISK_BADGE_MEDIUM_THRESHOLD = 1.5;

    /**
     * Lower boundary (inclusive) at which the weighted average is considered
     * "high". Values at or above this threshold map to the high badge.
     *
     * <p>At exactly 2.5 the average is equidistant between medium (2) and
     * high (3). The higher badge is assigned, so 2.5 becomes "high".
     */
    public static final double RISK_BADGE_HIGH_THRESHOLD = 2.5;

    // -------------------------------------------------------------------------
    // Risk badge label constants
    // -------------------------------------------------------------------------

    /**
     * Badge label returned when the weighted average is below
     * {@link #RISK_BADGE_MEDIUM_THRESHOLD}.
     */
    public static final String RISK_BADGE_LOW = "low";

    /**
     * Badge label returned when the weighted average is in
     * [{@link #RISK_BADGE_MEDIUM_THRESHOLD}, {@link #RISK_BADGE_HIGH_THRESHOLD}).
     */
    public static final String RISK_BADGE_MEDIUM = "medium";

    /**
     * Badge label returned when the weighted average is at or above
     * {@link #RISK_BADGE_HIGH_THRESHOLD}.
     */
    public static final String RISK_BADGE_HIGH = "high";

    // -------------------------------------------------------------------------
    // Status literal
    // -------------------------------------------------------------------------

    public static final String STATUS_FAILURE = "failure";

    // -------------------------------------------------------------------------
    // Risk level literals (must match audit_logs.risk column values)
    // -------------------------------------------------------------------------

    public static final String RISK_LOW    = "low";
    public static final String RISK_MEDIUM = "medium";
    public static final String RISK_HIGH   = "high";

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_TENANT_ID    = "tenantId";
    public static final String LOG_KEY_DATE_FROM    = "dateFrom";
    public static final String LOG_KEY_DATE_TO      = "dateTo";
    public static final String LOG_KEY_LIMIT        = "limit";
    public static final String LOG_KEY_RETURN_ALL   = "returnAll";
    public static final String LOG_KEY_RESULT_COUNT = "resultCount";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    /** Two placeholders: key, value. */
    public static final String LOG_REQUEST_RECEIVED =
            "Top actors request received [{}={}]";

    /** Eight placeholders: tenantId, dateFrom, dateTo, returnAll -- each key+val. */
    public static final String LOG_COMPUTATION_STARTED =
            "Top actors computation started [{}={}, {}={}, {}={}, {}={}]";

    /** Two placeholders: resultCount key+val. */
    public static final String LOG_COMPUTATION_COMPLETE =
            "Top actors computation complete [{}={}]";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    public static final String VALIDATION_LIMIT_INVALID =
            "limit must be 0 (return all) or a positive integer";
}