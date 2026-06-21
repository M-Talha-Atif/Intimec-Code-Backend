package com.aidentx.core.log_management.config.constants.analytics;

/**
 * Constants for the log analytics KPI computation layer.
 *
 * <p>All field names are verified against the actual main-logs index mapping.
 * The index has no tenantId field -- logs are not tenant-scoped at the
 * document level. All KPI aggregations run across the full index.
 *
 * <p>Top-level fields confirmed in the index:
 * <ul>
 *   <li>{@code level}        -- text + keyword sub-field, values: ERROR, WARN, INFO, DEBUG</li>
 *   <li>{@code source}       -- text + keyword sub-field</li>
 *   <li>{@code microservice} -- text + keyword sub-field</li>
 *   <li>{@code message}      -- text + keyword sub-field</li>
 *   <li>{@code category}     -- text + keyword sub-field</li>
 *   <li>{@code @timestamp}   -- long (epoch milliseconds)</li>
 *   <li>{@code correlationId}-- text + keyword sub-field</li>
 *   <li>{@code details}      -- nested object, contents vary per log entry</li>
 * </ul>
 *
 * <p>Fields NOT present in the index (skipped from KPI computation):
 * <ul>
 *   <li>{@code tenantId}         -- does not exist at top level or in details</li>
 *   <li>{@code isResolved}       -- does not exist</li>
 *   <li>{@code resolvedAt}       -- does not exist</li>
 *   <li>{@code details.executionTime} -- not consistently present</li>
 * </ul>
 *
 * <p>KPI formula reference given actual available fields:
 * <pre>
 *   totalErrors        = COUNT where level IN ['ERROR','WARN']
 *                        AND @timestamp >= now - range window
 *
 *   criticalFailures   = COUNT where level = 'ERROR'
 *                        within the effective window
 *
 *   avgResponseTimeMs  = AVG of details.executionTime
 *                        where field exists -- returns 0.0 when absent
 *                        (reserved for future use when field is populated)
 *
 *   mttrMinutes        = 0.0 -- isResolved and resolvedAt not present in index
 *                        (reserved for future use when resolution tracking added)
 *
 *   recurringPatterns  = COUNT of message.keyword buckets
 *                        where doc_count >= RECURRING_MIN_OCCURRENCES
 *                        filtered to level = 'ERROR'
 * </pre>
 */
public final class LogAnalyticsKpisConstants {

    private LogAnalyticsKpisConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path constants
    // -------------------------------------------------------------------------

    /**
     * Base path for all log analytics endpoints.
     */
    public static final String API_LOG_ANALYTICS_BASE = "/api/v1/logs/analytics";

    /**
     * Sub-endpoint for fetching log analytics KPI data.
     * Full path: {@code /api/v1/logs/analytics/kpis}
     */
    public static final String API_LOG_ANALYTICS_KPIS = "/kpis";

    // -------------------------------------------------------------------------
    // Range parameter constants
    // -------------------------------------------------------------------------

    /**
     * Query parameter name for the range filter.
     */
    public static final String RANGE_PARAM = "range";

    /**
     * Range value for last 24 hours.
     * Applied to totalErrors KPI when specified.
     */
    public static final String RANGE_24H = "24h";

    /**
     * Range value for last 30 days.
     * Default for criticalFailures, avgResponseTimeMs, mttrMinutes, recurringPatterns.
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
     * Value that disables all date filtering.
     * When supplied, all KPIs cover the full log history.
     */
    public static final String RANGE_ALL = "all";

    /**
     * Default range value when not specified.
     * totalErrors uses 24h window, other KPIs use 30d window.
     */
    public static final String RANGE_DEFAULT = "24h";

    // -------------------------------------------------------------------------
    // Time constants (milliseconds)
    // -------------------------------------------------------------------------

    /**
     * Duration in milliseconds representing 24 hours.
     */
    public static final long TWENTY_FOUR_HOURS_MS = 86_400_000L;

    /**
     * Duration in milliseconds representing 30 days.
     */
    public static final long THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L;

    /**
     * Duration in milliseconds representing 60 days.
     */
    public static final long SIXTY_DAYS_MS = 60L * 24L * 60L * 60L * 1000L;

    /**
     * Duration in milliseconds representing 90 days.
     */
    public static final long NINETY_DAYS_MS = 90L * 24L * 60L * 60L * 1000L;

    // -------------------------------------------------------------------------
    // OpenSearch index
    // -------------------------------------------------------------------------

    /**
     * Name of the OpenSearch index storing all application log documents.
     */
    public static final String LOGS_INDEX = "main-logs";

    // -------------------------------------------------------------------------
    // OpenSearch field names (verified against main-logs index mapping)
    // -------------------------------------------------------------------------

    /**
     * Top-level field storing the log severity level.
     * Type: text with keyword sub-field.
     * Confirmed values in index: "ERROR", "WARN", "INFO", "DEBUG".
     * Use {@link #FIELD_LEVEL_KEYWORD} for exact match aggregations.
     */
    public static final String FIELD_LEVEL_KEYWORD = "level.keyword";

    /**
     * Top-level field storing the UTC event timestamp.
     * Type: long (epoch milliseconds).
     * Example value: 1770978281658
     * Used for date range filtering in all KPIs.
     */
    public static final String FIELD_TIMESTAMP = "@timestamp";

    /**
     * Top-level field storing the log message text.
     * Type: text with keyword sub-field.
     * Use the keyword sub-field for terms aggregation in recurring patterns.
     *
     * <p>Example values:
     * <pre>
     *   "LDAP bind failed: invalid credentials for service account"
     *   "500 Server Error for HTTP GET /api/v1/artifacts/..."
     * </pre>
     */
    public static final String FIELD_MESSAGE_KEYWORD = "message.keyword";

    /**
     * Nested field for connector or API response time in milliseconds.
     * Not consistently present in all log documents.
     * When absent from a document, OpenSearch's avg aggregation excludes
     * that document automatically -- no explicit exists filter needed.
     *
     * <p>Reserved for use once this field is populated in log events.
     */
    public static final String FIELD_EXECUTION_TIME = "details.executionTime";

    /**
     * Boolean field indicating whether an error has been resolved.
     * Written by LogPinningHelper when a log is marked resolved.
     * Type: boolean. Present on documents that have been through the resolution flow.
     */
    public static final String FIELD_IS_RESOLVED = "isResolved";

    /**
     * Top-level field storing the epoch millisecond timestamp when the error
     * was resolved. Written by {@link com.aidentx.core.log_management.service.Helpers.LogPinningHelper}
     * when a log is marked resolved. Type: long.
     *
     * <p>Used together with {@link #FIELD_TIMESTAMP} to compute resolution delta:
     * <pre>
     *   deltaMs    = resolvedAt - @timestamp
     *   mttrMinutes = deltaMs / MS_PER_MINUTE
     * </pre>
     */
    public static final String FIELD_RESOLVED_AT = "resolvedAt";

    // -------------------------------------------------------------------------
    // Log level literals (confirmed from actual index documents)
    // -------------------------------------------------------------------------

    /**
     * Stored value for error-level events. Confirmed uppercase in index.
     */
    public static final String LEVEL_ERROR = "ERROR";

    /**
     * Stored value for warning-level events. Confirmed uppercase in index.
     */
    public static final String LEVEL_WARN = "WARN";

    // -------------------------------------------------------------------------
    // Recurring pattern threshold
    // -------------------------------------------------------------------------

    /**
     * Minimum number of times an error message must appear within the window
     * to be counted as a recurring pattern.
     *
     * <p>Example: "500 Server Error for HTTP GET" appearing 47 times counts
     * as 1 recurring pattern. A message appearing twice does not count.
     */
    public static final int RECURRING_MIN_OCCURRENCES = 3;

    /**
     * Maximum number of unique message buckets requested from OpenSearch
     * in the recurring patterns terms aggregation.
     */
    public static final int RECURRING_PATTERNS_AGG_SIZE = 1000;

    // -------------------------------------------------------------------------
    // Time conversion constants
    // -------------------------------------------------------------------------

    /**
     * Divisor to convert milliseconds to minutes.
     * Reserved for MTTR computation when resolvedAt field becomes available.
     */
    public static final double MS_PER_MINUTE = 60_000.0;

    // -------------------------------------------------------------------------
    // OpenSearch aggregation names
    // -------------------------------------------------------------------------

    /**
     * Filter aggregation name for total ERROR + WARN count.
     */
    public static final String AGG_TOTAL_ERRORS = "total_errors";

    /**
     * Filter aggregation name for ERROR-level count.
     */
    public static final String AGG_CRITICAL_FAILURES = "critical_failures";

    /**
     * Avg metric aggregation name for details.executionTime.
     */
    public static final String AGG_AVG_RESPONSE_TIME = "avg_response_time";

    /**
     * Filter + scripted_metric aggregation name for MTTR.
     * Returns 0.0 until isResolved and resolvedAt fields exist in the index.
     */
    public static final String AGG_MTTR = "mttr_minutes";

    /**
     * Filter + nested terms aggregation name for recurring error message patterns.
     * Inner aggregation name used when parsing buckets.
     */
    public static final String AGG_RECURRING_MESSAGES = "recurring_messages";

    /**
     * Inner terms aggregation name nested under AGG_RECURRING_MESSAGES.
     */
    public static final String AGG_RECURRING_MESSAGES_INNER = "messages";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    /**
     * Error when an unrecognised range value is supplied.
     */
    public static final String VALIDATION_RANGE_UNKNOWN =
            "unsupported range value; supported values: 24h, 30d, 60d, 90d, all";

    /**
     * Error when tenantId is null or blank.
     */
    public static final String VALIDATION_TENANT_ID_BLANK =
            "tenantId must not be blank";

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_TENANT_ID = "tenantId";
    public static final String LOG_KEY_RANGE = "range";
    public static final String LOG_KEY_TOTAL_ERRORS = "totalErrors";
    public static final String LOG_KEY_CRITICAL_FAILURES = "criticalFailures";
    public static final String LOG_KEY_AVG_RESPONSE_TIME_MS = "avgResponseTimeMs";
    public static final String LOG_KEY_MTTR_MINUTES = "mttrMinutes";
    public static final String LOG_KEY_RECURRING_PATTERNS = "recurringPatterns";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    /**
     * Two placeholders: tenantId, range.
     */
    public static final String LOG_KPI_REQUEST_RECEIVED =
            "Log analytics KPI request received [{}={}, {}={}]";

    /**
     * Two placeholders: tenantId, range.
     */
    public static final String LOG_KPI_COMPUTATION_STARTED =
            "Log analytics KPI computation started [{}={}, {}={}]";

    /**
     * Ten placeholders: all five KPI key+val pairs plus tenantId and range.
     */
    public static final String LOG_KPI_COMPUTATION_COMPLETE =
            "Log analytics KPI computation complete [{}={}, {}={}, {}={}, {}={}, {}={}, {}={}, {}={}]";

    /**
     * One placeholder: tenantId.
     */
    public static final String LOG_KPI_OPENSEARCH_NULL =
            "OpenSearch returned null for KPI aggregation [{}={}]";
}