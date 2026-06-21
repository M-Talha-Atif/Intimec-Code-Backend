package com.aidentx.core.log_management.config.constants.analytics;

/**
 * Constants for the log error heatmap analytics computation.
 *
 * <p>The heatmap endpoint returns a sparse list of (source, hour, count)
 * triplets. Each triplet represents how many log events occurred from
 * a specific source during a specific hour-of-day within the requested window.
 * Zero-count cells are omitted -- the frontend fills them when rendering.
 *
 * <p>The top-N sources by total event count are selected first. Only those
 * sources appear as rows in the heatmap. All 24 hour columns are always
 * present for each source (zero cells omitted, frontend fills them).
 *
 * <p>Hour values are integers 0-23 in the caller-supplied timezone.
 * A bucket with UTC key 2026-03-18T10:00:00Z becomes hour=15 for
 * Asia/Karachi (UTC+5) and hour=11 for Europe/Zurich (UTC+1).
 *
 * <p>Example response (limit=2, hours=24, timezone=Asia/Karachi):
 * <pre>
 * {
 *   "hours": 24,
 *   "limit": 2,
 *   "sources": ["GatewayPermissionFilter", "JwtUtil"],
 *   "data": [
 *     { "source": "GatewayPermissionFilter", "hour": 15, "count": 49 },
 *     { "source": "GatewayPermissionFilter", "hour": 16, "count": 5  },
 *     { "source": "GatewayPermissionFilter", "hour": 17, "count": 68 },
 *     { "source": "JwtUtil",                 "hour": 15, "count": 6  },
 *     { "source": "JwtUtil",                 "hour": 17, "count": 12 }
 *   ]
 * }
 * </pre>
 *
 * <p>Zero cells are absent. Frontend derives them:
 * {@code getHeatCount(source, hour) = data.find(d => d.source===source && d.hour===hour)?.count ?? 0}
 *
 * <p>OpenSearch aggregation strategy (conceptually):
 * <pre>
 *   Step 1 -- identify top-N sources:
 *   SELECT source, COUNT(*) FROM main-logs
 *   WHERE @timestamp >= windowStart
 *   GROUP BY source ORDER BY COUNT(*) DESC LIMIT N
 *
 *   Step 2 -- hourly breakdown per source:
 *   SELECT HOUR(@timestamp), source, COUNT(*)
 *   FROM main-logs
 *   WHERE @timestamp >= windowStart
 *   GROUP BY HOUR(@timestamp), source
 *   ORDER BY HOUR(@timestamp) ASC
 * </pre>
 */
public final class LogAnalyticsHeatmapConstants {

    private LogAnalyticsHeatmapConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path constant
    // -------------------------------------------------------------------------

    /**
     * Sub-endpoint for the log error heatmap.
     * Full path: {@code /api/v1/logs/analytics/heatmap}
     */
    public static final String API_HEATMAP = "/heatmap";

    // -------------------------------------------------------------------------
    // OpenSearch index and field names
    // -------------------------------------------------------------------------

    /**
     * Main logs index name.
     */
    public static final String LOGS_INDEX = "main-logs";

    /**
     * Source field
     * Example values: "GatewayPermissionFilter", "JwtUtil", "SsoConfigManager".
     */
    public static final String FIELD_SOURCE_KEYWORD = "source";

    /**
     * UTC epoch millisecond timestamp field.
     * Used for the time window filter and date_histogram bucketing.
     */
    public static final String FIELD_TIMESTAMP = "@timestamp";

    // -------------------------------------------------------------------------
    // Default parameter values
    // -------------------------------------------------------------------------

    /**
     * Default look-back window in hours when the caller omits {@code hours}.
     */
    public static final int DEFAULT_HOURS = 24;

    /**
     * Default number of top sources when the caller omits {@code limit}.
     */
    public static final int DEFAULT_SOURCE_LIMIT = 10;

    /**
     * Minimum allowed value for the {@code hours} parameter.
     */
    public static final int MINIMUM_HOURS = 1;

    /**
     * Maximum allowed value for the {@code hours} parameter.
     * 168 = 7 days -- beyond this the heatmap has too many columns to render.
     */
    public static final int MAXIMUM_HOURS = 168;

    /**
     * Minimum allowed value for the {@code limit} parameter.
     */
    public static final int MINIMUM_SOURCE_LIMIT = 1;

    /**
     * Maximum allowed value for the {@code limit} parameter.
     * Caps the number of heatmap rows rendered on the frontend.
     */
    public static final int MAXIMUM_SOURCE_LIMIT = 20;

    /**
     * Milliseconds in one hour.
     * Used to compute the window lower bound:
     * {@code windowStart = now - (hours * MS_PER_HOUR)}
     */
    public static final long MS_PER_HOUR = 3_600_000L;

    /**
     * Total number of hours in a day.
     * Hour integers in the response are always in range [0, HOURS_IN_DAY).
     */
    public static final int HOURS_IN_DAY = 24;

    // -------------------------------------------------------------------------
    // Range and timezone parameters
    // -------------------------------------------------------------------------

    /**
     * Value that disables time filtering.
     * When supplied, all stored log data is included with no window lower bound.
     */
    public static final String RANGE_ALL = "all";

    /**
     * Query parameter name for the range flag.
     */
    public static final String RANGE_PARAM = "range";

    /**
     * Default IANA timezone applied when the caller omits {@code timezone}.
     * UTC guarantees consistent hour integers regardless of server location.
     */
    public static final String DEFAULT_TIMEZONE = "UTC";

    /**
     * Query parameter name for the IANA timezone string.
     */
    public static final String TIMEZONE_PARAM = "timezone";

    // -------------------------------------------------------------------------
    // OpenSearch aggregation names
    // -------------------------------------------------------------------------

    /**
     * Terms aggregation name for identifying top-N sources.
     * Issued in the first query to discover which sources have the most events.
     */
    public static final String AGG_TOP_SOURCES = "top_sources";

    /**
     * Date histogram aggregation name producing one bucket per hour.
     * Nested inside the main query filter.
     */
    public static final String AGG_HOURLY_BUCKETS = "hourly_buckets";

    /**
     * Terms aggregation name nested inside each hourly bucket.
     * Returns the per-source count for that specific hour.
     */
    public static final String AGG_SOURCE_PER_HOUR = "source_per_hour";

    /**
     * Epoch millisecond key field inside each date_histogram bucket.
     * Used to derive the timezone-local hour integer.
     */
    public static final String BUCKET_KEY = "key";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    public static final String VALIDATION_HOURS_OUT_OF_RANGE =
            "hours must be between 1 and 168 (7 days)";

    public static final String VALIDATION_LIMIT_OUT_OF_RANGE =
            "limit must be between 1 and 20";

    public static final String VALIDATION_RANGE_UNKNOWN =
            "unsupported range value; supported values: all";

    public static final String VALIDATION_TIMEZONE_INVALID =
            "timezone must be a valid IANA timezone string e.g. Asia/Karachi, Europe/Zurich, UTC";

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_HOURS = "hours";
    public static final String LOG_KEY_LIMIT = "limit";
    public static final String LOG_KEY_RANGE_ALL = "rangeAll";
    public static final String LOG_KEY_TIMEZONE = "timezone";
    public static final String LOG_KEY_SOURCE_COUNT = "sourceCount";
    public static final String LOG_KEY_CELL_COUNT = "cellCount";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    /**
     * Two placeholders: key, value.
     */
    public static final String LOG_REQUEST_RECEIVED =
            "Heatmap request received [{}={}]";

    /**
     * Eight placeholders: hours, limit, rangeAll, timezone -- each key+val.
     */
    public static final String LOG_COMPUTATION_STARTED =
            "Heatmap computation started [{}={}, {}={}, {}={}, {}={}]";

    /**
     * Four placeholders: sourceCount, cellCount -- each key+val.
     */
    public static final String LOG_COMPUTATION_COMPLETE =
            "Heatmap computation complete [{}={}, {}={}]";

    /**
     * Two placeholders: key, value.
     */
    public static final String LOG_OPENSEARCH_NULL =
            "OpenSearch returned null for heatmap [{}={}]";

    public static final String FIELD_LEVEL_KEYWORD = "level.keyword";
    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_WARN = "WARN";
}