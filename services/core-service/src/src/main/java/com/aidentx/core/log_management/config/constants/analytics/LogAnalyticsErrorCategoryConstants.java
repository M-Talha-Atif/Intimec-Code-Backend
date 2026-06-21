package com.aidentx.core.log_management.config.constants.analytics;

/**
 * Constants for the error category distribution analytics computation.
 *
 * <p>Groups ERROR and WARN log events by their {@code category} field and
 * returns a ranked list ordered by count descending. Categories are the
 * actual values stored in the index -- no hardcoded mapping or keyword
 * classification is applied.
 *
 * <p>Default behaviour: {@code range=all} with no time window, top 10
 * categories. The caller can narrow by time using {@code hours} or widen
 * the result set using {@code limit}.
 *
 * <p>Example OpenSearch aggregation (conceptually):
 * <pre>
 *   SELECT category, COUNT(*) AS count
 *   FROM main-logs
 *   WHERE level IN ('ERROR', 'WARN')
 *     AND category IS NOT NULL
 *   GROUP BY category
 *   ORDER BY count DESC
 *   LIMIT 10
 * </pre>
 *
 * <p>Example response for limit=3, range=all:
 * <pre>
 * [
 *   { "category": "Security-Auth",            "count": 312 },
 *   { "category": "SECURITY",                 "count": 198 },
 *   { "category": "NOTIFICATIONS_MANAGEMENT", "count": 87  }
 * ]
 * </pre>
 *
 * <p>Color assignment is handled entirely on the frontend by cycling through
 * chart CSS variables. The backend never returns color values.
 */
public final class LogAnalyticsErrorCategoryConstants {

    private LogAnalyticsErrorCategoryConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path constant
    // -------------------------------------------------------------------------

    /**
     * Sub-endpoint for the error category distribution.
     * Full path: {@code /api/v1/logs/analytics/error-category-distribution}
     */
    public static final String API_ERROR_CATEGORY = "/error-category-distribution";

    // -------------------------------------------------------------------------
    // OpenSearch index and field names
    // -------------------------------------------------------------------------

    /** Main logs index name. */
    public static final String LOGS_INDEX = "main-logs";

    /**
     * Level field with keyword sub-field for exact match.
     * Confirmed values in index: "ERROR", "WARN", "INFO", "DEBUG".
     */
    public static final String FIELD_LEVEL_KEYWORD = "level.keyword";

    /**
     * Category field
     * Type: text with .keyword sub-field confirmed in index mapping.
     *
     * <p>Example values from real index documents:
     * "Security-Auth", "SECURITY", "NOTIFICATIONS_MANAGEMENT",
     * "PROVISIONING", "AUDIT", "WORKFLOW".
     *
     * <p>Documents without a category field are excluded via an
     * {@code exists} filter in the query so null categories never
     * appear as a bucket.
     */
    public static final String FIELD_CATEGORY_KEYWORD = "category";

    /**
     * UTC epoch millisecond timestamp field.
     * Used for the optional time window filter when {@code hours} is supplied.
     */
    public static final String FIELD_TIMESTAMP = "@timestamp";

    // -------------------------------------------------------------------------
    // Log level literals (confirmed uppercase from real index documents)
    // -------------------------------------------------------------------------

    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_WARN  = "WARN";

    // -------------------------------------------------------------------------
    // Default parameter values
    // -------------------------------------------------------------------------

    /**
     * Default number of top categories returned when the caller omits
     * the {@code limit} parameter.
     */
    public static final int DEFAULT_LIMIT = 10;

    /**
     * Minimum allowed value for the {@code limit} parameter.
     */
    public static final int MINIMUM_LIMIT = 1;

    /**
     * Maximum allowed value for the {@code limit} parameter.
     */
    public static final int MAXIMUM_LIMIT = 50;

    /**
     * Minimum allowed value for the {@code hours} parameter when supplied.
     */
    public static final int MINIMUM_HOURS = 1;

    /**
     * Maximum allowed value for the {@code hours} parameter.
     * 168 = 7 days, same ceiling as the error volume endpoint.
     */
    public static final int MAXIMUM_HOURS = 168;

    /**
     * Milliseconds in one hour. Used to compute the window lower bound
     * when the caller supplies the {@code hours} parameter.
     *
     * <p>Example: hours=24
     * -- windowStart = System.currentTimeMillis() - (24 * 3_600_000)
     */
    public static final long MS_PER_HOUR = 3_600_000L;

    // -------------------------------------------------------------------------
    // Range parameter
    // -------------------------------------------------------------------------

    /**
     * Value that disables time filtering entirely.
     * This is the DEFAULT for this endpoint -- category distribution is most
     * meaningful across full history because rare but critical categories
     * (e.g. "PROVISIONING_FAILURE") would be invisible in a narrow window.
     *
     * <p>When the caller omits both {@code range} and {@code hours}, the
     * service treats it as {@code range=all} automatically.
     */
    public static final String RANGE_ALL = "all";

    /** Query parameter name for the range flag. */
    public static final String RANGE_PARAM = "range";

    // -------------------------------------------------------------------------
    // OpenSearch aggregation name
    // -------------------------------------------------------------------------

    /**
     * Terms aggregation name for grouping by category.keyword.
     * The size of this aggregation is set to the resolved {@code limit}
     * so only top-N categories are returned by OpenSearch directly --
     * no post-processing truncation needed.
     */
    public static final String AGG_CATEGORIES = "error_categories";

    /**
     * Filter aggregation name scoping the terms agg to documents that
     * actually have a category field. Documents without category are
     * excluded so null never appears as a category bucket.
     */
    public static final String AGG_CATEGORY_FILTER = "category_filter";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    public static final String VALIDATION_LIMIT_OUT_OF_RANGE =
            "limit must be between 1 and 50";

    public static final String VALIDATION_HOURS_OUT_OF_RANGE =
            "hours must be between 1 and 168 (7 days)";

    public static final String VALIDATION_RANGE_UNKNOWN =
            "unsupported range value; supported values: all";

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_HOURS        = "hours";
    public static final String LOG_KEY_LIMIT        = "limit";
    public static final String LOG_KEY_RANGE_ALL    = "rangeAll";
    public static final String LOG_KEY_RESULT_COUNT = "resultCount";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    /** Two placeholders: key, value. */
    public static final String LOG_REQUEST_RECEIVED =
            "Error category distribution request received [{}={}]";

    /** Six placeholders: hours, limit, rangeAll -- each key+val. */
    public static final String LOG_COMPUTATION_STARTED =
            "Error category distribution computation started [{}={}, {}={}, {}={}]";

    /** Two placeholders: resultCount key+val. */
    public static final String LOG_COMPUTATION_COMPLETE =
            "Error category distribution computation complete [{}={}]";

    /** Two placeholders: key, value. */
    public static final String LOG_OPENSEARCH_NULL =
            "OpenSearch returned null for error category distribution [{}={}]";
}