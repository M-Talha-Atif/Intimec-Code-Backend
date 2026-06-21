package com.aidentx.core.log_management.config.constants.analytics;

import java.util.List;

/**
 * Constants for the KPI drilldown endpoint.
 *
 * <p>The KPI drilldown endpoint returns the actual log entries that were
 * counted to produce each KPI card value. This lets users click a KPI
 * card and see exactly which logs contributed to the number shown.
 *
 * <p>Supported drilldown types (passed as the {@code type} query parameter):
 * <ul>
 *   <li>{@link #TYPE_TOTAL_ERRORS}    -- ERROR and WARN entries from the last 24 hours</li>
 *   <li>{@link #TYPE_CRITICAL}        -- ERROR level entries only</li>
 *   <li>{@link #TYPE_SLOW_RESPONSES}  -- entries where executionTime exceeds the
 *       computed average; requires the average to be fetched first via a
 *       pre-aggregation query</li>
 *   <li>{@link #TYPE_RECURRING}       -- entries whose exact message text appears
 *       {@link #RECURRING_MIN_OCCURRENCES} or more times in the index</li>
 * </ul>
 *
 * <p>All types return the same response shape: paginated {@code LogEntry} list
 * with {@code PaginationMetadata}. The frontend renders them identically in
 * the drilldown drawer.
 *
 * <p>Example: clicking the Total Errors KPI card:
 * <pre>
 *   GET /api/v1/logs/analytics/kpi-drilldown?type=total_errors&page=0&size=20
 * </pre>
 *
 * <p>Example: clicking the Recurring Patterns KPI card:
 * <pre>
 *   GET /api/v1/logs/analytics/kpi-drilldown?type=recurring&page=0&size=20
 * </pre>
 */
public final class LogAnalyticsKpiDrilldownConstants {

    private LogAnalyticsKpiDrilldownConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path
    // -------------------------------------------------------------------------

    /**
     * Sub-endpoint for the KPI drilldown.
     * Full path: {@code /api/v1/logs/analytics/kpi-drilldown}
     */
    public static final String API_KPI_DRILLDOWN = "/kpi-drilldown";

    // -------------------------------------------------------------------------
    // Drilldown type values (query parameter: type)
    // -------------------------------------------------------------------------

    /**
     * Returns all ERROR and WARN entries from the last 24 hours.
     * Matches exactly the documents counted by the totalErrors24h KPI.
     */
    public static final String TYPE_TOTAL_ERRORS = "total_errors";

    /**
     * Returns all ERROR level entries.
     * Matches exactly the documents counted by the criticalFailures KPI.
     */
    public static final String TYPE_CRITICAL = "critical";

    /**
     * Returns entries where {@code details.executionTime} exceeds the
     * computed average execution time across all documents that have that field.
     * Matches the population used to compute avgResponseTimeMs KPI.
     */
    public static final String TYPE_SLOW_RESPONSES = "slow_responses";

    /**
     * Returns entries whose message text appears
     * {@link #RECURRING_MIN_OCCURRENCES} or more times in the index.
     * Matches the messages counted by the recurringPatterns KPI.
     */
    public static final String TYPE_RECURRING = "recurring";

    // -------------------------------------------------------------------------
    // OpenSearch index and field names
    // -------------------------------------------------------------------------

    /**
     * Main logs index name.
     */
    public static final String LOGS_INDEX = "main-logs";

    /**
     * Level field with keyword sub-field for exact match.
     */
    public static final String FIELD_LEVEL_KEYWORD = "level.keyword";

    /**
     * UTC epoch millisecond timestamp field.
     */
    public static final String FIELD_TIMESTAMP = "@timestamp";

    /**
     * Execution time field inside the details object.
     * Type: long (milliseconds). Present on some documents only.
     * Used for avg response time computation and slow response filtering.
     */
    public static final String FIELD_EXECUTION_TIME = "details.executionTime";

    /**
     * Message field -- text type.
     * Used for recurring pattern detection via terms aggregation.
     */
    public static final String FIELD_MESSAGE = "message";

    /**
     * Message field with keyword sub-field for exact match aggregations.
     * Used in the terms aggregation to find repeated message strings.
     */
    public static final String FIELD_MESSAGE_KEYWORD = "message.keyword";

    // -------------------------------------------------------------------------
    // Log level literals
    // -------------------------------------------------------------------------

    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_WARN = "WARN";

    // -------------------------------------------------------------------------
    // Time constants
    // -------------------------------------------------------------------------

    /**
     * Milliseconds in 24 hours.
     * Used to compute the lower bound for the total_errors drilldown window.
     * Matches the same window used by the totalErrors24h KPI computation.
     */
    public static final long TWENTY_FOUR_HOURS_MS = 86_400_000L;

    /**
     * Milliseconds in 30 days.
     */
    public static final long THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L;

    /**
     * Milliseconds in 60 days.
     */
    public static final long SIXTY_DAYS_MS = 60L * 24L * 60L * 60L * 1000L;

    /**
     * Milliseconds in 90 days.
     */
    public static final long NINETY_DAYS_MS = 90L * 24L * 60L * 60L * 1000L;

    // -------------------------------------------------------------------------
    // Range parameter values
    // -------------------------------------------------------------------------

    /**
     * Query parameter name for the range filter.
     */
    public static final String RANGE_PARAM = "range";

    /**
     * Range value for last 24 hours.
     */
    public static final String RANGE_24H = "24h";

    /**
     * Range value for last 30 days.
     */
    public static final String RANGE_30D = "30d";

    /**
     * Range value for last 60 days.
     */
    public static final String RANGE_60D = "60d";

    /**
     * Range value for last 90 days.
     */
    public static final String RANGE_90D = "90d";

    /**
     * When supplied as the {@code range} parameter, disables all time filtering.
     * Matches the range=all behaviour used across all other analytics endpoints.
     */
    public static final String RANGE_ALL = "all";

    // -------------------------------------------------------------------------
    // Recurring pattern threshold
    // -------------------------------------------------------------------------

    /**
     * Minimum number of times a message must appear to be classified as a
     * recurring pattern. Must match the value used by the KPI computation
     * in {@link LogAnalyticsKpisConstants#RECURRING_MIN_OCCURRENCES}.
     *
     * <p>A document is included in the recurring drilldown only if its exact
     * message text appears at least this many times across the full index.
     */
    public static final int RECURRING_MIN_OCCURRENCES = 3;

    /**
     * Maximum number of distinct recurring message strings fetched in the
     * pre-aggregation step before fetching matching documents.
     * Capped to keep the subsequent terms query manageable.
     */
    public static final int RECURRING_MESSAGES_FETCH_SIZE = 500;

    // -------------------------------------------------------------------------
    // Pagination defaults
    // -------------------------------------------------------------------------

    /**
     * Default page number (0-based) when the caller omits {@code page}.
     */
    public static final int DEFAULT_PAGE = 0;

    /**
     * Default page size when the caller omits {@code size}.
     */
    public static final int DEFAULT_SIZE = 20;

    /**
     * Minimum allowed page size.
     */
    public static final int MINIMUM_SIZE = 1;

    /**
     * Maximum allowed page size. Prevents returning oversized payloads.
     */
    public static final int MAXIMUM_SIZE = 100;

    // -------------------------------------------------------------------------
    // OpenSearch aggregation names
    // -------------------------------------------------------------------------

    /**
     * Avg aggregation name for computing the average execution time.
     * Used in the slow_responses pre-aggregation query.
     */
    public static final String AGG_AVG_EXECUTION_TIME = "avg_execution_time";

    /**
     * Terms aggregation name for finding repeated message strings.
     * Used in the recurring pre-aggregation query.
     */
    public static final String AGG_RECURRING_MESSAGES = "recurring_messages";

    // -------------------------------------------------------------------------
    // Sort direction
    // -------------------------------------------------------------------------

    /**
     * Sort order constant for descending (newest first).
     */
    public static final String SORT_DESC = "desc";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    public static final String VALIDATION_TYPE_REQUIRED =
            "type parameter is required; supported values: "
                    + TYPE_TOTAL_ERRORS + ", "
                    + TYPE_CRITICAL + ", "
                    + TYPE_SLOW_RESPONSES + ", "
                    + TYPE_RECURRING;

    public static final String VALIDATION_SIZE_OUT_OF_RANGE =
            "size must be between 1 and 100";

    public static final String VALIDATION_PAGE_NEGATIVE =
            "page must be >= 0";

    public static final String VALIDATION_RANGE_UNKNOWN =
            "unsupported range value; supported values: 24h, 30d, 60d, 90d, all";

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_TYPE = "type";
    public static final String LOG_KEY_PAGE = "page";
    public static final String LOG_KEY_SIZE = "size";
    public static final String LOG_KEY_RANGE = "range";
    public static final String LOG_KEY_TOTAL_ELEMENTS = "totalElements";
    public static final String LOG_KEY_AVG_EXEC_TIME = "avgExecutionTimeMs";
    public static final String LOG_KEY_RECURRING_COUNT = "recurringMessageCount";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    /**
     * Four placeholders: type, page, size, range.
     */
    public static final String LOG_REQUEST_RECEIVED =
            "KPI drilldown request received [{}={}, {}={}, {}={}, {}={}]";

    /**
     * Six placeholders: type, page, size, range -- each key+val.
     */
    public static final String LOG_QUERY_STARTED =
            "KPI drilldown query started [{}={}, {}={}, {}={}, {}={}]";

    /**
     * Four placeholders: totalElements key+val, type key+val.
     */
    public static final String LOG_QUERY_COMPLETE =
            "KPI drilldown query complete [{}={}, {}={}]";

    /**
     * Two placeholders: key, value.
     */
    public static final String LOG_OPENSEARCH_NULL =
            "OpenSearch returned null for KPI drilldown [{}={}]";

    // -------------------------------------------------------------------------
    // Level filter scope per drilldown type
    // -------------------------------------------------------------------------

    /**
     * Total errors drilldown includes ERROR and WARN levels.
     * Matches the KPI computation which counts ERROR+WARN.
     */
    public static final List<String> TOTAL_ERRORS_LEVELS = List.of("ERROR", "WARN");

    /**
     * Critical failures drilldown filters to ERROR only.
     * Matches the KPI computation which counts ERROR level events.
     */
    public static final List<String> CRITICAL_LEVELS = List.of("ERROR");

    /**
     * Slow responses drilldown includes ERROR and WARN levels.
     * Matches the KPI computation which focuses on error-level slow responses.
     */
    public static final List<String> SLOW_RESPONSES_LEVELS = List.of("ERROR", "WARN","INFO");

    /**
     * Recurring patterns drilldown includes ERROR and WARN levels.
     * Matches the KPI computation which counts recurring patterns at error/warn levels.
     */
    public static final List<String> RECURRING_LEVELS = List.of("ERROR", "WARN");
}