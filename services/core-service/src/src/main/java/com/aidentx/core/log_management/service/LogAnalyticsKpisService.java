package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.LogAnalyticsKpisResponse;

/**
 * Service contract for computing log analytics KPIs from OpenSearch.
 *
 * <p>All queries are issued against the main-logs index and scoped to the
 * supplied tenantId via a term filter on the tenantId field.
 *
 * <p>Date window behaviour:
 * <ul>
 *   <li>{@code rangeAll=false} -- applies the effective window. totalErrors24h
 *       always covers the last 24 hours regardless of dateFrom/dateTo.
 *       All other KPIs apply the supplied window (default: last 30 days).</li>
 *   <li>{@code rangeAll=true} -- disables all date filtering. totalErrors24h
 *       covers the full log history, same as all other KPIs.</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsKpisService {

    /**
     * Computes all five KPI values for the specified date range.
     *
     * <p>The range parameter controls the time window applied to all KPIs:
     * <ul>
     *   <li><b>24h</b> - Last 24 hours of logs</li>
     *   <li><b>30d</b> - Last 30 days of logs</li>
     *   <li><b>60d</b> - Last 60 days of logs</li>
     *   <li><b>90d</b> - Last 90 days of logs</li>
     *   <li><b>all</b> - Full log history, no date filtering</li>
     * </ul>
     *
     * <p>Example response:
     * <pre>
     * {
     *   "totalErrors": 247,
     *   "criticalFailures": 18,
     *   "avgResponseTimeMs": 2800.0,
     *   "mttrMinutes": 32.0,
     *   "recurringPatterns": 5
     * }
     * </pre>
     *
     * @param tenantId tenant identifier (reserved for future use, not applied as filter)
     * @param range    date range filter: "24h", "30d", "60d", "90d", or "all"
     * @return KPI response containing all five metrics
     */
    LogAnalyticsKpisResponse computeKpis(String tenantId, String range);
}