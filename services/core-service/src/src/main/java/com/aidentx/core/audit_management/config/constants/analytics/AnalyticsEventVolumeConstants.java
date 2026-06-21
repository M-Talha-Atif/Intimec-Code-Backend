package com.aidentx.core.audit_management.config.constants.analytics;

/**
 * Constants for the event volume timeline analytics computation.
 *
 * <p>The event volume endpoint returns a 30-day (default) daily breakdown of
 * audit event counts, bucketed by risk level. Each row in the response
 * corresponds to one calendar day and carries three counts: low, medium, high.
 * No critical severity level exists in audit_logs.risk.
 *
 * <p>This class covers four concern groups:
 * <ul>
 *   <li>API path constants -- base path and the specific segment for this
 *       endpoint, composed in the controller so no magic strings appear
 *       in annotations.</li>
 *   <li>Default window constant -- the number of days used when the caller
 *       omits both dateFrom and dateTo. Declared here so the business
 *       rule is visible and changeable without touching service logic.</li>
 *   <li>Risk level literals -- must exactly match the values stored in
 *       audit_logs.risk, used in the JPQL GROUP BY projection.</li>
 *   <li>Structured log keys and message templates -- same structured
 *       logging contract established in {@link AnalyticsKpisConstants}.</li>
 * </ul>
 *
 * <p>Example query this drives (conceptually):
 * <pre>
 *   SELECT DATE(timestamp), risk, COUNT(*)
 *   FROM audit_logs
 *   WHERE tenant_id = :tenantId
 *     AND timestamp BETWEEN :dateFrom AND :dateTo
 *   GROUP BY DATE(timestamp), risk
 *   ORDER BY DATE(timestamp) ASC
 * </pre>
 */
public final class AnalyticsEventVolumeConstants {

    private AnalyticsEventVolumeConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // API path constants
    // -------------------------------------------------------------------------

    /**
     * Base path shared by all analytics endpoints.
     */
    public static final String ANALYTICS_BASE_PATH = "/api/v1/audit-logs/analytics";

    /**
     * Path segment appended to base path for the event volume endpoint.
     */
    public static final String EVENT_VOLUME_PATH = "/event-volume";

    // -------------------------------------------------------------------------
    // Default window
    // -------------------------------------------------------------------------

    /**
     * Default look-back period in days applied when the caller supplies no
     * date window. The frontend chart is labelled "Event Volume (30 Days)"
     * so this value must stay in sync with that label.
     *
     * <p>Used in the service as:
     * <pre>
     *   Instant resolvedFrom = (dateFrom != null)
     *       ? dateFrom
     *       : Instant.now().minus(
     *             AnalyticsEventVolumeConstants.DEFAULT_WINDOW_DAYS,
     *             ChronoUnit.DAYS);
     * </pre>
     */
    public static final long DEFAULT_WINDOW_DAYS = 30L;

    // -------------------------------------------------------------------------
    // Risk level literals (must match audit_logs.risk column values)
    // -------------------------------------------------------------------------

    /**
     * Stored value for low-severity events. Maps to {@code low} in the response.
     */
    public static final String RISK_LOW = "low";

    /**
     * Stored value for medium-severity events. Maps to {@code medium} in the response.
     */
    public static final String RISK_MEDIUM = "medium";

    /**
     * Stored value for high-severity events. Maps to {@code high} in the response.
     * High is the maximum severity level stored -- no critical level exists.
     */
    public static final String RISK_HIGH = "high";

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_TENANT_ID = "tenantId";
    public static final String LOG_KEY_DATE_FROM = "dateFrom";
    public static final String LOG_KEY_DATE_TO   = "dateTo";
    public static final String LOG_KEY_ROW_COUNT = "rowCount";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    /**
     * Two placeholders: key, value.
     */
    public static final String LOG_REQUEST_RECEIVED =
            "Event volume request received [{}={}]";

    /**
     * Six placeholders: tenantId key+val, dateFrom key+val, dateTo key+val.
     */
    public static final String LOG_COMPUTATION_STARTED =
            "Event volume computation started [{}={}, {}={}, {}={}]";

    /**
     * Two placeholders: rowCount key+val.
     */
    public static final String LOG_COMPUTATION_COMPLETE =
            "Event volume computation complete [{}={}]";
}