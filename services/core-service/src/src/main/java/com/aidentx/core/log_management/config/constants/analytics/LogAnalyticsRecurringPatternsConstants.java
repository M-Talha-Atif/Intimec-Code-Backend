package com.aidentx.core.log_management.config.constants.analytics;

/**
 * Constants for the recurring patterns analytics computation.
 *
 * <p>A recurring pattern is any log message string that appears at least
 * {@link #MIN_OCCURRENCES} times in the index. This endpoint returns the
 * top-N patterns with full detail: occurrence count, first/last seen
 * timestamps, dominant source, and severity derived from log level.
 *
 * <p>OpenSearch aggregation strategy:
 * <pre>
 *   terms agg on message.keyword (size=limit, min_doc_count=MIN_OCCURRENCES)
 *     -> min(@timestamp) sub-agg    = firstSeen
 *     -> max(@timestamp) sub-agg    = lastSeen
 *     -> terms on source.keyword (size=1) sub-agg = dominant source
 *     -> terms on level.keyword (size=4) sub-agg  = dominant level for severity
 * </pre>
 *
 * <p>Example response entry:
 * <pre>
 * {
 *   "id": "pat-0",
 *   "message": "Permission denied for user",
 *   "occurrences": 312,
 *   "firstSeen": "2026-03-17T10:15:00.000Z",
 *   "lastSeen":  "2026-03-18T15:42:00.000Z",
 *   "source":    "GatewayPermissionFilter",
 *   "severity":  "high"
 * }
 * </pre>
 */
public final class LogAnalyticsRecurringPatternsConstants {

    private LogAnalyticsRecurringPatternsConstants() {}

    // -------------------------------------------------------------------------
    // API path
    // -------------------------------------------------------------------------

    /**
     * Sub-endpoint for the recurring patterns list.
     * Full path: {@code /api/v1/logs/analytics/recurring-patterns}
     */
    public static final String API_RECURRING_PATTERNS = "/recurring-patterns";

    // -------------------------------------------------------------------------
    // OpenSearch index and field names
    // -------------------------------------------------------------------------

    public static final String LOGS_INDEX              = "main-logs";
    public static final String FIELD_MESSAGE_KEYWORD   = "message.keyword";
    public static final String FIELD_TIMESTAMP         = "@timestamp";
    public static final String FIELD_SOURCE_KEYWORD    = "source";
    public static final String FIELD_LEVEL_KEYWORD     = "level.keyword";

    // -------------------------------------------------------------------------
    // Log level literals
    // -------------------------------------------------------------------------

    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_WARN  = "WARN";

    // -------------------------------------------------------------------------
    // Severity labels (match frontend getSeverityBadge keys)
    // -------------------------------------------------------------------------

    public static final String SEVERITY_HIGH   = "high";
    public static final String SEVERITY_MEDIUM = "medium";
    public static final String SEVERITY_LOW    = "low";

    // -------------------------------------------------------------------------
    // Default parameter values
    // -------------------------------------------------------------------------

    /**
     * Minimum number of occurrences for a message to be classified as a
     * recurring pattern. Must stay in sync with the KPI computation in
     * {@link LogAnalyticsKpisConstants#RECURRING_MIN_OCCURRENCES}.
     * A message appearing fewer than this many times is a one-off, not a pattern.
     */
    public static final int MIN_OCCURRENCES = 3;

    /**
     * Default number of top patterns returned when limit is omitted.
     * Matches the number of mock patterns shown in the frontend.
     */
    public static final int DEFAULT_LIMIT = 10;

    public static final int MINIMUM_LIMIT = 1;
    public static final int MAXIMUM_LIMIT = 100;

    /**
     * Default look-back window in hours. 0 means no time filter (all history).
     * The recurring patterns section defaults to all history so even
     * infrequent but persistently recurring messages are visible.
     */
    public static final int DEFAULT_HOURS = 0;

    public static final int MINIMUM_HOURS = 0;
    public static final int MAXIMUM_HOURS = 168;

    public static final long MS_PER_HOUR = 3_600_000L;

    // -------------------------------------------------------------------------
    // OpenSearch aggregation names
    // -------------------------------------------------------------------------

    /** Top-level terms aggregation grouping by message.keyword. */
    public static final String AGG_MESSAGES       = "recurring_messages";

    /** Min timestamp sub-aggregation inside each message bucket. */
    public static final String AGG_FIRST_SEEN     = "first_seen";

    /** Max timestamp sub-aggregation inside each message bucket. */
    public static final String AGG_LAST_SEEN      = "last_seen";

    /**
     * Terms sub-aggregation (size=1) returning the dominant source.
     * size=1 returns only the top source by doc_count which is the component
     * that produced the most occurrences of this message.
     */
    public static final String AGG_TOP_SOURCE     = "top_source";

    /**
     * Terms sub-aggregation (size=4) returning level distribution.
     * size=4 covers all four possible levels: ERROR, WARN, INFO, DEBUG.
     * The level with the highest doc_count determines severity.
     */
    public static final String AGG_LEVEL_DIST     = "level_distribution";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    public static final String VALIDATION_LIMIT_OUT_OF_RANGE =
            "limit must be between 1 and 100";

    public static final String VALIDATION_HOURS_OUT_OF_RANGE =
            "hours must be between 0 and 168 (0 means no time filter)";

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_LIMIT       = "limit";
    public static final String LOG_KEY_HOURS       = "hours";
    public static final String LOG_KEY_RESULT_COUNT = "resultCount";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    public static final String LOG_REQUEST_RECEIVED =
            "Recurring patterns request received [{}={}]";

    public static final String LOG_COMPUTATION_STARTED =
            "Recurring patterns computation started [{}={}, {}={}]";

    public static final String LOG_COMPUTATION_COMPLETE =
            "Recurring patterns computation complete [{}={}]";

    public static final String LOG_OPENSEARCH_NULL =
            "OpenSearch returned null for recurring patterns [{}={}]";
}