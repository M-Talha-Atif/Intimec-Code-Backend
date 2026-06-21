package com.aidentx.core.log_management.config.constants.analytics;

/**
 * Constants for the error volume by source analytics computation.
 *
 * <p>The error volume endpoint returns one entry per hour covering the
 * requested window. Each entry contains the hour label and dynamic source
 * keys -- one key per top-N source, where N is caller-controlled via the
 * {@code limit} parameter.
 *
 * <p>Sources are ranked by total ERROR+WARN count within the window.
 * Only the top N sources appear as keys. All other sources are excluded.
 * The frontend renders each source as a stacked Area with a dynamically
 * assigned chart colour.
 *
 * <p>Example response for limit=3, hours=3 (3 entries shown):
 * <pre>
 * {
 *   "hours": 3,
 *   "limit": 3,
 *   "sources": ["JwtAuthenticationFilter", "GatewayPermissionFilter", "UserNotificationService"],
 *   "timeline": [
 *     { "hour": "10:00", "JwtAuthenticationFilter": 5, "GatewayPermissionFilter": 12, "UserNotificationService": 0 },
 *     { "hour": "11:00", "JwtAuthenticationFilter": 2, "GatewayPermissionFilter": 3,  "UserNotificationService": 1 },
 *     { "hour": "12:00", "JwtAuthenticationFilter": 8, "GatewayPermissionFilter": 1,  "UserNotificationService": 4 }
 *   ]
 * }
 * </pre>
 *
 * <p>The {@code sources} array in the response tells the frontend exactly
 * which keys to render as Area components, in rank order. The frontend does
 * not need to guess or derive the source list from the timeline entries.
 *
 * <p>OpenSearch aggregation strategy:
 * <pre>
 *   1. Filter: level IN ['ERROR','WARN'] AND @timestamp >= now - (hours * 3600000)
 *   2. Terms agg on source (size=limit) -- identifies top N sources
 *   3. Date histogram on @timestamp (interval=1h) -- one bucket per hour
 *   4. Inside each hour bucket: terms agg on source (size=limit)
 * </pre>
 *
 * <p>Gap filling: the OpenSearch date_histogram returns only hours that have
 * at least one matching document. Hours with zero errors for a source produce
 * no bucket. The service fills all gaps with 0 so every hour in the window
 * is present in the response.
 */
public final class LogAnalyticsErrorVolumeConstants {

    private LogAnalyticsErrorVolumeConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path constant
    // -------------------------------------------------------------------------

    /**
     * Sub-endpoint for the error volume timeline.
     * Full path: {@code /api/v1/logs/analytics/error-volume}
     */
    public static final String API_ERROR_VOLUME = "/error-volume";

    // -------------------------------------------------------------------------
    // OpenSearch field names (verified against main-logs index mapping)
    // -------------------------------------------------------------------------

    /**
     * Level field with keyword sub-field for exact match.
     */
    public static final String FIELD_LEVEL_KEYWORD = "level.keyword";

    /**
     * UTC epoch millisecond timestamp field.
     */
    public static final String FIELD_TIMESTAMP = "@timestamp";

    /**
     * Source field
     * Example values: "JwtAuthenticationFilter", "GatewayPermissionFilter".
     */
    public static final String FIELD_SOURCE_KEYWORD = "source";


    /**
     * Total number of hour slots in a full day.
     * Used when rangeAll=true to produce exactly 24 entries (hours 0-23)
     * regardless of the requested hours parameter.
     */
    public static final int HOURS_IN_DAY = 24;

    // -------------------------------------------------------------------------
    // Range parameter
    // -------------------------------------------------------------------------

    /**
     * Value that disables the time window filter.
     * When supplied as {@code range=all}, covers full log history bucketed
     * by hour-of-day rather than the last N hours.
     */
    public static final String RANGE_ALL = "all";

    /**
     * Query parameter name for the range flag.
     */
    public static final String RANGE_PARAM = "range";

    // -------------------------------------------------------------------------
    // Log level literals
    // -------------------------------------------------------------------------

    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_WARN = "WARN";

    // -------------------------------------------------------------------------
    // Default parameter values
    // -------------------------------------------------------------------------

    /**
     * Default number of hours in the look-back window when the caller omits
     * the {@code hours} parameter.
     * Example: value of 24 covers events from now-24h to now.
     */
    public static final int DEFAULT_HOURS = 24;

    /**
     * Default number of top sources returned when the caller omits the
     * {@code limit} parameter.
     */
    public static final int DEFAULT_SOURCE_LIMIT = 5;

    /**
     * Minimum allowed value for the {@code hours} parameter.
     */
    public static final int MINIMUM_HOURS = 1;

    /**
     * Maximum allowed value for the {@code hours} parameter.
     * 168 = 7 days. Beyond this the hourly granularity produces 168+ bars
     * which is not useful for the area chart and risks heavy OpenSearch load.
     */
    public static final int MAXIMUM_HOURS = 168;

    /**
     * Minimum allowed value for the {@code limit} parameter.
     */
    public static final int MINIMUM_SOURCE_LIMIT = 1;

    /**
     * Maximum allowed value for the {@code limit} parameter.
     * Caps the number of Area components the frontend renders.
     */
    public static final int MAXIMUM_SOURCE_LIMIT = 20;

    // -------------------------------------------------------------------------
    // OpenSearch aggregation names
    // -------------------------------------------------------------------------

    /**
     * Top-level filter aggregation name scoping all sub-aggregations to
     * ERROR and WARN documents within the requested time window.
     */
    public static final String AGG_ERROR_FILTER = "error_filter";

    /**
     * Terms aggregation name used to identify the top-N sources by total
     * error count across the full window. This is computed first and the
     * resulting source list drives gap-filling in the timeline.
     *
     * <p>Example result: ["JwtAuthenticationFilter", "GatewayPermissionFilter", ...]
     */
    public static final String AGG_TOP_SOURCES = "top_sources";

    /**
     * Date histogram aggregation name producing one bucket per hour.
     * Nested inside {@link #AGG_ERROR_FILTER}.
     *
     * <p>Uses calendar_interval=1h so bucket boundaries align to clock hours
     * (00:00, 01:00, 02:00...) regardless of when the query is executed.
     */
    public static final String AGG_HOURLY_BUCKETS = "hourly_buckets";

    /**
     * Terms aggregation name nested inside each hourly bucket.
     * Returns the per-source count for that specific hour.
     */
    public static final String AGG_SOURCE_PER_HOUR = "source_per_hour";

    /**
     * Key field name inside each date histogram bucket for the epoch timestamp.
     */
    public static final String BUCKET_KEY_AS_STRING = "key_as_string";

    /**
     * Key field name inside each date histogram bucket for the epoch ms value.
     * Used to derive the hour-of-day label (HH:00).
     */
    public static final String BUCKET_KEY = "key";

    // -------------------------------------------------------------------------
    // Hour label format
    // -------------------------------------------------------------------------

    /**
     * Format string used to produce the hour label in the response.
     * Example: hour 2 -> "02:00", hour 14 -> "14:00".
     * Matches the frontend XAxis dataKey format.
     */
    public static final String HOUR_LABEL_FORMAT = "%02d:00";


     // -------------------------------------------------------------------------
     // Timezone constants
    // -------------------------------------------------------------------------

    /**
     * Default IANA timezone applied when the caller omits the {@code timezone}
     * parameter. UTC is the safest default -- all stored timestamps are UTC so
     * no conversion errors occur, and dashboard administrators typically view
     * system logs in UTC.
     *
     * <p>Callers in local timezones should pass their IANA timezone string:
     * <ul>
     *   <li>Pakistan   -- "Asia/Karachi"   (UTC+5, no DST)</li>
     *   <li>Switzerland -- "Europe/Zurich" (UTC+1/+2 with DST)</li>
     *   <li>UAE         -- "Asia/Dubai"    (UTC+4, no DST)</li>
     * </ul>
     */
    public static final String DEFAULT_TIMEZONE = "UTC";

    /**
     * Query parameter name for the IANA timezone string.
     * Example: {@code ?timezone=Asia/Karachi}
     */
    public static final String TIMEZONE_PARAM = "timezone";




    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_TENANT_ID = "tenantId";
    public static final String LOG_KEY_HOURS = "hours";
    public static final String LOG_KEY_LIMIT = "limit";
    public static final String LOG_KEY_RANGE_ALL = "rangeAll";
    public static final String LOG_KEY_SOURCE_COUNT = "sourceCount";
    public static final String LOG_KEY_ENTRY_COUNT = "entryCount";
    public static final String LOG_KEY_TIMEZONE = "timezone";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    /**
     * Two placeholders: key, value.
     */
    public static final String LOG_REQUEST_RECEIVED =
            "Error volume request received [{}={}]";

    /**
     * Six placeholders: hours key+val, limit key+val, rangeAll key+val.
     */
    public static final String LOG_COMPUTATION_STARTED =
            "Error volume computation started [{}={}, {}={}, {}={}]";

    /**
     * Four placeholders: sourceCount key+val, entryCount key+val.
     */
    public static final String LOG_COMPUTATION_COMPLETE =
            "Error volume computation complete [{}={}, {}={}]";

    /**
     * Two placeholders: key, value.
     */
    public static final String LOG_OPENSEARCH_NULL =
            "OpenSearch returned null for error volume [{}={}]";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    public static final String VALIDATION_HOURS_OUT_OF_RANGE =
            "hours must be between 1 and 168 (7 days)";

    public static final String VALIDATION_LIMIT_OUT_OF_RANGE =
            "limit must be between 1 and 20";

    /**
     * Validation error returned when an unrecognised timezone string is supplied.
     */
    public static final String VALIDATION_TIMEZONE_INVALID =
            "timezone must be a valid IANA timezone string e.g. Asia/Karachi, Europe/Zurich, UTC";


    /**
     * OpenSearch index name. Same as KPIs service.
     */
    public static final String LOGS_INDEX = "main-logs";

    /**
     * Milliseconds in one hour.
     */
    public static final long MS_PER_HOUR = 3_600_000L;



}