package com.aidentx.core.audit_management.config.constants.analytics;

/**
 * Constants for the shared {@code range} query parameter accepted by all
 * analytics endpoints.
 *
 * <p>The {@code range} parameter gives the caller an explicit way to opt out
 * of date filtering entirely. When {@code range=all} is supplied, the service
 * layer skips window resolution and issues queries with no timestamp predicate,
 * returning aggregates across the full tenant history.
 *
 * <p>Precedence rule: if {@code range=all} is present alongside {@code dateFrom}
 * or {@code dateTo}, the {@code range=all} flag takes precedence and the date
 * parameters are ignored. The validator enforces this by rejecting combinations
 * that would otherwise produce ambiguous intent.
 *
 * <p>Supported values for the {@code range} parameter:
 * <ul>
 *   <li>{@link #RANGE_ALL} -- disables date filtering entirely</li>
 * </ul>
 *
 * <p>When {@code range} is omitted, default windowed behaviour applies in each
 * service (last 30 days unless {@code dateFrom} / {@code dateTo} are provided).
 */
public final class AnalyticsRangeConstants {

    private AnalyticsRangeConstants() {
        // Utility class -- no instantiation
    }

    // -------------------------------------------------------------------------
    // Range parameter name
    // -------------------------------------------------------------------------

    /**
     * Query parameter name used on all analytics endpoints.
     * Example: {@code GET /api/v1/audit-logs/analytics/kpis?range=all}
     */
    public static final String RANGE_PARAM = "range";

    // -------------------------------------------------------------------------
    // Range values
    // -------------------------------------------------------------------------

    /**
     * Value that disables date filtering. When the caller supplies
     * {@code range=all}, the service issues queries with no timestamp
     * predicate and returns data across the full tenant history.
     */
    public static final String RANGE_ALL = "all";

    // -------------------------------------------------------------------------
    // Validation error messages
    // -------------------------------------------------------------------------

    /**
     * Returned when {@code range=all} is supplied together with
     * {@code dateFrom} or {@code dateTo}. Combining them is ambiguous and
     * rejected explicitly rather than silently ignoring the date parameters.
     */
    public static final String VALIDATION_RANGE_ALL_WITH_DATES =
            "range=all cannot be combined with dateFrom or dateTo";

    /**
     * Returned when an unrecognised range value is supplied.
     * Currently the only accepted non-null value is 'all'.
     */
    public static final String VALIDATION_RANGE_UNKNOWN =
            "unsupported range value; supported values: all";

    // -------------------------------------------------------------------------
    // Structured log keys
    // -------------------------------------------------------------------------

    public static final String LOG_KEY_RANGE    = "range";
    public static final String LOG_KEY_RANGE_ALL = "rangeAll";
}