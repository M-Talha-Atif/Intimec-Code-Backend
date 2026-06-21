package com.aidentx.core.audit_management.config.constants.analytics;

/**
 * Constants for the change impact heatmap analytics computation.
 *
 * <p>The heatmap endpoint returns a sparse list of (objectType, eventName, count)
 * triplets representing how many times each event was performed against each
 * object type within the requested window. The frontend renders this as a
 * matrix where object types are rows and event names are columns.
 *
 * <p>Example data returned by this endpoint:
 * <pre>
 * [
 *   { "objectType": "Role",       "eventName": "assign",    "count": 142 },
 *   { "objectType": "Role",       "eventName": "provision", "count": 38  },
 *   { "objectType": "User",       "eventName": "assign",    "count": 97  },
 *   { "objectType": "Permission", "eventName": "approve",   "count": 21  }
 * ]
 * </pre>
 *
 * <p>Frontend matrix rendering from the above:
 * <pre>
 *              assign   provision   approve
 *   Role         142       38          0
 *   User          97        0          0
 *   Permission     0        0         21
 * </pre>
 *
 * <p>Zero cells are not included in the response. The frontend fills them.
 * Only (objectType, eventName) pairs that have at least one event appear in
 * the list. This keeps the payload small when the matrix is sparse.
 *
 * <p>This class covers four concern groups:
 * <ul>
 *   <li>API path constant -- segment appended to the shared analytics base
 *       path in {@link AnalyticsKpisConstants#API_AUDIT_LOGS_ANALYTICS}.</li>
 *   <li>Limit constants -- same zero-means-all contract used by event
 *       distribution and top actors. Here the limit caps the number of
 *       distinct (objectType, eventName) pairs returned, not the number
 *       of rows in the matrix.</li>
 *   <li>Structured log keys and message templates.</li>
 *   <li>Validation error messages.</li>
 * </ul>
 *
 * <p>Example queries this drives (conceptually):
 *
 * <p>Bounded (top N pairs by count):
 * <pre>
 *   SELECT objectType, event AS eventName, COUNT(*) AS eventCount
 *   FROM audit_logs
 *   WHERE tenant_id = :tenantId
 *     AND timestamp BETWEEN :dateFrom AND :dateTo
 *     AND objectType IS NOT NULL
 *     AND event IS NOT NULL
 *   GROUP BY objectType, event
 *   ORDER BY COUNT(*) DESC
 *   LIMIT :limit
 * </pre>
 *
 * <p>Unbounded (all pairs):
 * <pre>
 *   SELECT objectType, event AS eventName, COUNT(*) AS eventCount
 *   FROM audit_logs
 *   WHERE tenant_id = :tenantId
 *     AND timestamp BETWEEN :dateFrom AND :dateTo
 *     AND objectType IS NOT NULL
 *     AND event IS NOT NULL
 *   GROUP BY objectType, event
 *   ORDER BY COUNT(*) DESC
 * </pre>
 */
public final class AnalyticsHeatmapConstants {

    private AnalyticsHeatmapConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path constant
    // -------------------------------------------------------------------------

    /**
     * Path segment appended to
     * {@link AnalyticsKpisConstants#API_AUDIT_LOGS_ANALYTICS} for this endpoint.
     * Produces: {@code /api/v1/audit-logs/analytics/heatmap}
     */
    public static final String HEATMAP_PATH = "/heatmap";

    // -------------------------------------------------------------------------
    // Limit constants
    // -------------------------------------------------------------------------

    /**
     * Default number of (objectType, eventName) pairs returned when the caller
     * omits the {@code limit} parameter.
     *
     * <p>A pair here means one cell in the matrix. With 10 object types and
     * 8 event names there are up to 80 possible pairs. The default of 50
     * returns the 50 most active cells, which is sufficient to render a
     * meaningful heatmap without over-fetching.
     */
    public static final int DEFAULT_LIMIT = 50;

    /**
     * Sentinel value the caller passes as {@code limit} to request all
     * (objectType, eventName) pairs with no truncation.
     * Same zero-means-all contract as event distribution and top actors.
     */
    public static final int LIMIT_ALL = 0;

    /**
     * Minimum allowed value for a bounded limit request.
     */
    public static final int MINIMUM_LIMIT = 1;

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

    /**
     * Two placeholders: key, value.
     */
    public static final String LOG_REQUEST_RECEIVED =
            "Heatmap request received [{}={}]";

    /**
     * Eight placeholders: tenantId, dateFrom, dateTo, returnAll -- each key+val.
     */
    public static final String LOG_COMPUTATION_STARTED =
            "Heatmap computation started [{}={}, {}={}, {}={}, {}={}]";

    /**
     * Two placeholders: resultCount key+val.
     */
    public static final String LOG_COMPUTATION_COMPLETE =
            "Heatmap computation complete [{}={}]";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    /**
     * Returned when the caller supplies a limit that is neither {@link #LIMIT_ALL}
     * nor a positive integer.
     */
    public static final String VALIDATION_LIMIT_INVALID =
            "limit must be 0 (return all) or a positive integer";
}