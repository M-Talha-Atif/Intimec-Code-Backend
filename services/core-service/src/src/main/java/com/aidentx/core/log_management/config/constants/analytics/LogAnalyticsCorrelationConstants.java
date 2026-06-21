package com.aidentx.core.log_management.config.constants.analytics;

/**
 * Constants for the correlation chain analysis computation.
 *
 * <p>A correlation chain is a group of log documents sharing the same
 * {@code correlationId} value, ordered by timestamp ascending. Each chain
 * represents a sequence of events that were triggered by a single root action
 * -- for example, an LDAP failure cascading into a workflow failure then a
 * provisioning failure.
 *
 * <p>Chain derivation rules applied in the service:
 * <ul>
 *   <li><b>rootCause</b> -- message from the first ERROR or WARN document
 *       in the chain by timestamp. Falls back to the first document message
 *       when no ERROR or WARN exists.</li>
 *   <li><b>severity</b> -- highest level present in the chain.
 *       ERROR = "high", WARN = "medium", INFO/DEBUG = "low".</li>
 *   <li><b>affectedCount</b> -- total number of documents in the chain.</li>
 *   <li><b>timestamp</b> -- ISO-8601 string of the first document in the chain.</li>
 *   <li><b>chain steps</b> -- all documents ordered by timestamp ascending,
 *       each carrying step number, source, message, and level.</li>
 * </ul>
 *
 * <p>Only chains with {@code doc_count >= MIN_CHAIN_SIZE} are returned.
 * Single-document correlationIds are excluded because they do not represent
 * a cascading failure pattern.
 *
 * <p>OpenSearch aggregation strategy:
 * <pre>
 *   Step 1 -- find top-N correlationIds with multiple documents:
 *   SELECT correlationId, COUNT(*) AS chainSize
 *   FROM main-logs
 *   WHERE correlationId IS NOT NULL
 *     AND @timestamp >= windowStart    -- when hours supplied
 *   GROUP BY correlationId
 *   HAVING chainSize >= MIN_CHAIN_SIZE
 *   ORDER BY chainSize DESC
 *   LIMIT N
 *
 *   Step 2 -- fetch all documents for those correlationIds:
 *   SELECT *
 *   FROM main-logs
 *   WHERE correlationId IN (:ids)
 *   ORDER BY @timestamp ASC
 * </pre>
 *
 * <p>Example response (limit=2):
 * <pre>
 * [
 *   {
 *     "id": "req-17136fed-b7ed-4441-87d4-d59bc7b4730f",
 *     "rootCause": "LDAP bind failed: invalid credentials for service account",
 *     "severity": "high",
 *     "timestamp": "2026-03-18T10:15:00.000Z",
 *     "affectedCount": 4,
 *     "chain": [
 *       { "step": 1, "source": "JwtAuthenticationFilter", "message": "LDAP bind failed...", "level": "ERROR" },
 *       { "step": 2, "source": "GatewayPermissionFilter",  "message": "Permission check failed...", "level": "ERROR" },
 *       { "step": 3, "source": "UserNotificationService",  "message": "Notification dispatch failed...", "level": "WARN" },
 *       { "step": 4, "source": "TokenService",             "message": "Token refresh skipped...", "level": "INFO" }
 *     ]
 *   }
 * ]
 * </pre>
 */
public final class LogAnalyticsCorrelationConstants {

    private LogAnalyticsCorrelationConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path constant
    // -------------------------------------------------------------------------

    /**
     * Sub-endpoint for the correlation chain analysis.
     * Full path: {@code /api/v1/logs/analytics/correlation-chains}
     */
    public static final String API_CORRELATION_CHAINS = "/correlation-chains";

    // -------------------------------------------------------------------------
    // OpenSearch index and field names
    // -------------------------------------------------------------------------

    /** Main logs index name. */
    public static final String LOGS_INDEX = "main-logs";

    /**
     * Correlation ID field with keyword sub-field.
     * Type: text with .keyword sub-field confirmed in index mapping.
     * Example values: "req-17136fed-b7ed-4441-87d4-d59bc7b4730f".
     * Documents without this field are excluded via an exists filter.
     */
    public static final String FIELD_CORRELATION_ID_KEYWORD = "correlationId.keyword";

    /**
     * UTC epoch millisecond timestamp field.
     * Used for time window filter and ordering chain steps chronologically.
     */
    public static final String FIELD_TIMESTAMP = "@timestamp";

    /**
     * Level field with keyword sub-field.
     * Used to derive chain severity and identify the root cause message.
     */
    public static final String FIELD_LEVEL_KEYWORD = "level.keyword";

    /**
     * Source field with keyword sub-field.
     * Mapped to {@code source} in each chain step.
     */
    public static final String FIELD_SOURCE = "source";

    /**
     * Message field.
     * Mapped to {@code message} in each chain step and used for rootCause.
     */
    public static final String FIELD_MESSAGE = "message";

    // -------------------------------------------------------------------------
    // Log level literals
    // -------------------------------------------------------------------------

    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_WARN  = "WARN";
    public static final String LEVEL_INFO  = "INFO";

    // -------------------------------------------------------------------------
    // Severity labels (returned in response -- match frontend getSeverityBadge keys)
    // -------------------------------------------------------------------------

    /**
     * Severity label when the chain contains at least one ERROR document.
     * Maps to the red badge in the frontend.
     */
    public static final String SEVERITY_HIGH = "high";

    /**
     * Severity label when the chain contains WARN but no ERROR documents.
     * Maps to the yellow badge in the frontend.
     */
    public static final String SEVERITY_MEDIUM = "medium";

    /**
     * Severity label when the chain contains only INFO/DEBUG documents.
     */
    public static final String SEVERITY_LOW = "low";

    // -------------------------------------------------------------------------
    // Default parameter values
    // -------------------------------------------------------------------------

    /**
     * Default number of correlation chains returned when {@code limit} is omitted.
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
     */
    public static final int MAXIMUM_HOURS = 168;

    /**
     * Milliseconds in one hour. Used to compute the window lower bound.
     */
    public static final long MS_PER_HOUR = 3_600_000L;

    /**
     * Minimum number of documents a correlationId must have to be included
     * as a chain. A single-document correlationId is not a cascading failure.
     *
     * <p>Example: correlationId "req-abc" has 4 documents -- included.
     * correlationId "req-xyz" has 1 document -- excluded.
     */
    public static final int MIN_CHAIN_SIZE = 2;

    /**
     * Maximum number of documents fetched per chain in the detail query.
     * Prevents returning thousands of steps for very busy correlationIds.
     * The frontend renders steps linearly so a cap of 20 is sufficient.
     */
    public static final int MAX_STEPS_PER_CHAIN = 20;

    // -------------------------------------------------------------------------
    // Range parameter
    // -------------------------------------------------------------------------

    /**
     * Value that disables time filtering.
     * Default for this endpoint -- correlation chains may span hours or days
     * so restricting by time risks cutting chains in half.
     */
    public static final String RANGE_ALL = "all";

    /** Query parameter name for the range flag. */
    public static final String RANGE_PARAM = "range";

    // -------------------------------------------------------------------------
    // OpenSearch aggregation and query names
    // -------------------------------------------------------------------------

    /**
     * Terms aggregation name for grouping by correlationId.keyword.
     * Returns top-N correlationIds by document count.
     */
    public static final String AGG_CORRELATION_IDS = "correlation_ids";

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

    public static final String LOG_KEY_HOURS         = "hours";
    public static final String LOG_KEY_LIMIT         = "limit";
    public static final String LOG_KEY_RANGE_ALL     = "rangeAll";
    public static final String LOG_KEY_CHAIN_COUNT   = "chainCount";
    public static final String LOG_KEY_CORRELATION_ID = "correlationId";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    /** Two placeholders: key, value. */
    public static final String LOG_REQUEST_RECEIVED =
            "Correlation chains request received [{}={}]";

    /** Six placeholders: hours, limit, rangeAll -- each key+val. */
    public static final String LOG_COMPUTATION_STARTED =
            "Correlation chains computation started [{}={}, {}={}, {}={}]";

    /** Two placeholders: chainCount key+val. */
    public static final String LOG_COMPUTATION_COMPLETE =
            "Correlation chains computation complete [{}={}]";

    /** Two placeholders: key, value. */
    public static final String LOG_OPENSEARCH_NULL =
            "OpenSearch returned null for correlation chains [{}={}]";
}