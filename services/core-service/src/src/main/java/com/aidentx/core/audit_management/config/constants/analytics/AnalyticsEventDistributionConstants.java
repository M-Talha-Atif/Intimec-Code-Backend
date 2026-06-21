package com.aidentx.core.audit_management.config.constants.analytics;

/**
 * Constants for the event type distribution analytics computation.
 *
 * <p>The event distribution endpoint returns a ranked list of event names
 * and their occurrence counts within the requested date window, scoped to
 * the caller's tenant. Results are ordered by count descending so the most
 * active event types appear first.
 *
 * <p>This class covers three concern groups:
 * <ul>
 *   <li>API path constant -- the segment appended to the shared analytics
 *       base path defined in {@link AnalyticsKpisConstants#API_AUDIT_LOGS_ANALYTICS}.</li>
 *   <li>Limit constants -- the default and sentinel values governing how many
 *       event types are returned. The sentinel value signals the caller wants
 *       all event types with no truncation applied.</li>
 *   <li>Structured log keys and message templates -- consistent with the
 *       logging contract in {@link AnalyticsKpisConstants} and
 *       {@link AnalyticsEventVolumeConstants}.</li>
 * </ul>
 *
 * <p>Example query this drives (conceptually):
 * <pre>
 *   SELECT event, COUNT(*) AS eventCount
 *   FROM audit_logs
 *   WHERE tenant_id = :tenantId
 *     AND timestamp BETWEEN :dateFrom AND :dateTo
 *   GROUP BY event
 *   ORDER BY eventCount DESC
 *   LIMIT :limit          -- omitted entirely when caller requests all results
 * </pre>
 */
public final class AnalyticsEventDistributionConstants {

    private AnalyticsEventDistributionConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path constant
    // -------------------------------------------------------------------------

    /**
     * Path segment appended to
     * {@link AnalyticsKpisConstants#API_AUDIT_LOGS_ANALYTICS} for this endpoint.
     * Composed in the controller via {@code @GetMapping} so no magic string
     * appears in the annotation directly.
     */
    public static final String EVENT_DISTRIBUTION_PATH = "/event-distribution";

    // -------------------------------------------------------------------------
    // Limit constants
    // -------------------------------------------------------------------------

    /**
     * Default number of event types returned when the caller omits the
     * {@code limit} query parameter. Matches the frontend bar chart which
     * renders ten bars by default.
     *
     * <p>Applied in the service when {@code limit} is null:
     * <pre>
     *   int resolvedLimit = (limit != null) ? limit : DEFAULT_LIMIT;
     * </pre>
     */
    public static final int DEFAULT_LIMIT = 10;

    /**
     * Sentinel value the caller passes as {@code limit} to request all event
     * types with no truncation. The service detects this value and issues the
     * query without a LIMIT clause.
     *
     * <p>Chosen as 0 rather than -1 or Integer.MAX_VALUE because it has clear
     * semantic meaning (zero limit = no limit) and is easy to document in the
     * API contract. The controller validates that any other supplied value is
     * a positive integer.
     *
     * <p>Frontend usage:
     * <pre>
     *   GET /api/v1/audit-logs/analytics/event-distribution?limit=0
     * </pre>
     */
    public static final int LIMIT_ALL = 0;

    /**
     * Minimum allowed value for a bounded limit request.
     * The controller rejects any limit below this value unless it equals
     * {@link #LIMIT_ALL}. Prevents nonsensical requests like limit=−5.
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
            "Event distribution request received [{}={}]";

    /**
     * Eight placeholders: tenantId, dateFrom, dateTo, limit/returnAll -- each key+val.
     */
    public static final String LOG_COMPUTATION_STARTED =
            "Event distribution computation started [{}={}, {}={}, {}={}, {}={}]";

    /**
     * Two placeholders: resultCount key+val.
     */
    public static final String LOG_COMPUTATION_COMPLETE =
            "Event distribution computation complete [{}={}]";

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