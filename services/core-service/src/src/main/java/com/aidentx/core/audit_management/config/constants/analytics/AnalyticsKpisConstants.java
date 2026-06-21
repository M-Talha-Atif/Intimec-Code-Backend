package com.aidentx.core.audit_management.config.constants.analytics;

/**
 * Centralised constants for the analytics KPI computation layer.
 *
 * <p>Covers four concern groups:
 * <ul>
 *   <li>Column value literals must exactly match values stored in audit_logs.risk
 *       and audit_logs.status, as they flow directly into JPA predicates.</li>
 *   <li>Numeric constants eliminate magic numbers in percentage math.</li>
 *   <li>Structured log keys every log statement uses these keys so that log
 *       aggregators (e.g. ELK, Datadog) can index fields by name rather than
 *       parsing positional text.</li>
 *   <li>Structured log message templates SLF4J {@code {}} placeholder style;
 *       each template declares exactly as many placeholders as keys+values the
 *       caller must supply.</li>
 * </ul>
 *
 * <p>Usage in service:
 * <pre>
 *   logger.info(
 *       AnalyticsKpisConstants.LOG_KPI_REQUEST_RECEIVED,
 *       AnalyticsKpisConstants.LOG_KEY_TENANT_ID, tenantId);
 * </pre>
 */
public final class AnalyticsKpisConstants {

    private AnalyticsKpisConstants() {
        // Utility class no instantiation
    }

    /**
     * Base path for all analytics endpoints.
     */
    public static final String API_AUDIT_LOGS_ANALYTICS = "/api/v1/audit-logs/analytics";

    /**
     * Sub-endpoint for fetching KPI data under analytics.
     */
    public static final String API_ANALYTICS_KPIS = "/kpis";

    // -------------------------------------------------------------------------
    // Risk level literals (must match audit_logs.risk column values)
    // -------------------------------------------------------------------------

    /**
     * Stored value for high-severity events in audit_logs.risk.
     * High is the maximum severity level stored -- no critical level exists.
     */
    public static final String RISK_LEVEL_HIGH = "high";

    /**
     * Single value passed to {@link com.aidentx.core.audit_management.specification.AuditLogSpecification#hasRisk(String)}.
     * Produces an SQL predicate: risk = 'high'.
     * High is the maximum severity stored in audit_logs.risk.
     */
    public static final String RISK_LEVEL_HIGH_ONLY = RISK_LEVEL_HIGH;

    // -------------------------------------------------------------------------
    // Status literals (must match audit_logs.status column values)
    // -------------------------------------------------------------------------

    /**
     * Stored value for failed operations in audit_logs.status.
     */
    public static final String STATUS_FAILURE = "failure";

    // -------------------------------------------------------------------------
    // Numeric constants for percentage calculation
    // -------------------------------------------------------------------------

    /**
     * Multiplier used to convert a ratio to a percentage: ratio * 100.0.
     */
    public static final double PERCENTAGE_MULTIPLIER = 100.0;

    /**
     * Number of decimal places applied during BigDecimal rounding of the
     * failure percentage. Value of 1 yields results such as 2.1 or 30.0.
     */
    public static final int PERCENTAGE_DECIMAL_SCALE = 1;

    /**
     * Sentinel value used in zero-denominator guard.
     * Compared against totalEvents before attempting division.
     */
    public static final long ZERO_COUNT = 0L;

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_TENANT_ID         = "tenantId";
    public static final String LOG_KEY_DATE_FROM         = "dateFrom";
    public static final String LOG_KEY_DATE_TO           = "dateTo";
    public static final String LOG_KEY_TOTAL_EVENTS      = "totalEvents";
    public static final String LOG_KEY_HIGH_RISK_COUNT   = "highRiskCount";
    public static final String LOG_KEY_FAILED_COUNT      = "failedOperationsCount";
    public static final String LOG_KEY_FAILED_PERCENTAGE = "failedOperationsPercentage";

    // -------------------------------------------------------------------------
    // Structured log message templates
    // -------------------------------------------------------------------------

    /**
     * Two placeholders: key, value.
     */
    public static final String LOG_KPI_REQUEST_RECEIVED =
            "Analytics KPI request received [{}={}]";

    /**
     * Six placeholders: tenantId key+val, dateFrom key+val, dateTo key+val.
     */
    public static final String LOG_KPI_COMPUTATION_STARTED =
            "KPI computation started [{}={}, {}={}, {}={}]";

    /**
     * Eight placeholders: totalEvents, highRisk, failedCount, failedPct -- each key+val.
     */
    public static final String LOG_KPI_COMPUTATION_COMPLETE =
            "KPI computation complete [{}={}, {}={}, {}={}, {}={}]";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    public static final String VALIDATION_DATE_FROM_AFTER_DATE_TO =
            "dateFrom must not be after dateTo";

    public static final String VALIDATION_TENANT_ID_BLANK =
            "tenantId must not be blank";
}