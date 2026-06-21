package com.aidentx.core.log_management.config.constants.analytics;

/**
 * Constants for the MTTR trend computation.
 *
 * <p>The MTTR trend endpoint returns one entry per calendar day for the
 * last 7 days. Each entry shows the average minutes between log creation
 * and resolution for entries resolved on that day.
 *
 * <p>Data requirements:
 * Documents must have both {@code @timestamp} (creation time, always present)
 * and {@code resolvedAt} (resolution time, written by LogPinningHelper).
 * Documents without {@code resolvedAt} are excluded by an exists filter
 * so they do not skew the average.
 *
 * <p>OpenSearch aggregation strategy:
 * <pre>
 *   Filter: resolvedAt exists AND resolvedAt >= 7 days ago
 *   Date histogram on resolvedAt (calendar_interval=1d, timezone=caller tz)
 *   Inside each day bucket:
 *     Scripted metric: avg(resolvedAt - @timestamp) / 60000 = minutes
 * </pre>
 *
 * <p>The date histogram uses the caller-supplied timezone so bucket
 * boundaries align to midnight in the caller's local time. This means
 * "Mon" contains all entries resolved between Mon 00:00 and Mon 23:59
 * in the caller's timezone, not in UTC.
 *
 * <p>Example response (timezone=Asia/Karachi, today=Wednesday):
 * <pre>
 * [
 *   { "day": "Thu", "mttr": 45.0 },
 *   { "day": "Fri", "mttr": 38.0 },
 *   { "day": "Sat", "mttr": 52.0 },
 *   { "day": "Sun", "mttr": 33.0 },
 *   { "day": "Mon", "mttr": 28.0 },
 *   { "day": "Tue", "mttr": 22.0 },
 *   { "day": "Wed", "mttr": 25.0 }
 * ]
 * </pre>
 */
public final class LogAnalyticsMttrTrendConstants {

    private LogAnalyticsMttrTrendConstants() {}

    // -------------------------------------------------------------------------
    // API path
    // -------------------------------------------------------------------------

    /**
     * Sub-endpoint for the MTTR trend.
     * Full path: {@code /api/v1/logs/analytics/mttr-trend}
     */
    public static final String API_MTTR_TREND = "/mttr-trend";

    // -------------------------------------------------------------------------
    // OpenSearch index and field names
    // -------------------------------------------------------------------------

    public static final String LOGS_INDEX          = "main-logs";
    public static final String FIELD_TIMESTAMP     = "@timestamp";
    public static final String FIELD_RESOLVED_AT   = "resolvedAt";

    // -------------------------------------------------------------------------
    // Window and label constants
    // -------------------------------------------------------------------------

    /**
     * Number of days in the trend window.
     * Always 7 -- one data point per day of the week.
     */
    public static final int TREND_DAYS = 7;

    /**
     * Milliseconds in one day.
     * Used to compute the window lower bound: now - (7 * MS_PER_DAY).
     */
    public static final long MS_PER_DAY = 86_400_000L;

    /**
     * Milliseconds in one minute.
     * Used in the scripted metric to convert ms delta to minutes.
     */
    public static final long MS_PER_MINUTE = 60_000L;

    /**
     * Default IANA timezone applied when the caller omits the timezone parameter.
     */
    public static final String DEFAULT_TIMEZONE = "UTC";

    /** Query parameter name for the IANA timezone string. */
    public static final String TIMEZONE_PARAM = "timezone";

    // -------------------------------------------------------------------------
    // OpenSearch aggregation names
    // -------------------------------------------------------------------------

    /**
     * Filter aggregation name scoping to documents that have resolvedAt.
     * Documents without resolvedAt are not resolved and must not contribute
     * to MTTR -- they would produce a delta of (now - @timestamp) which
     * would inflate the average significantly.
     */
    public static final String AGG_RESOLVED_FILTER = "resolved_filter";

    /**
     * Date histogram aggregation name producing one bucket per calendar day.
     * Uses calendar_interval=1d with the caller-supplied timezone so bucket
     * boundaries align to local midnight rather than UTC midnight.
     */
    public static final String AGG_DAILY_BUCKETS = "daily_buckets";

    /**
     * Scripted metric aggregation name inside each day bucket.
     * Computes avg(resolvedAt - @timestamp) / MS_PER_MINUTE for that day.
     */
    public static final String AGG_MTTR_SCRIPT = "mttr_minutes";

    // -------------------------------------------------------------------------
    // Day label format
    // -------------------------------------------------------------------------

    /**
     * Java DateTimeFormatter pattern producing short day name.
     * Example: "Mon", "Tue", "Wed".
     * Matches the frontend XAxis dataKey values in the mock data.
     */
    public static final String DAY_LABEL_FORMAT = "EEE";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    public static final String VALIDATION_TIMEZONE_INVALID =
            "timezone must be a valid IANA timezone string e.g. Asia/Karachi, Europe/Zurich, UTC";

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_TIMEZONE     = "timezone";
    public static final String LOG_KEY_ENTRY_COUNT  = "entryCount";
    public static final String LOG_KEY_WINDOW_START = "windowStart";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    public static final String LOG_REQUEST_RECEIVED =
            "MTTR trend request received [{}={}]";

    public static final String LOG_COMPUTATION_STARTED =
            "MTTR trend computation started [{}={}, {}={}]";

    public static final String LOG_COMPUTATION_COMPLETE =
            "MTTR trend computation complete [{}={}]";

    public static final String LOG_OPENSEARCH_NULL =
            "OpenSearch returned null for MTTR trend [{}={}]";
}