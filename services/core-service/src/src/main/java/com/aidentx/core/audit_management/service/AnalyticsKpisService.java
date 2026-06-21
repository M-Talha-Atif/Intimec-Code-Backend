package com.aidentx.core.audit_management.service;

import com.aidentx.core.audit_management.model.dto.analytics.AnalyticsKpisResponse;

import java.time.Instant;

/**
 * Service contract for computing audit analytics KPIs.
 *
 * <p>All queries are scoped to the supplied tenantId. The caller is responsible
 * for extracting a validated, non-blank tenantId before invoking this service.
 *
 * <p>The date window is optional in both directions:
 * <ul>
 *   <li>Both null -- full history for the tenant</li>
 *   <li>Only dateFrom -- all events from that point forward</li>
 *   <li>Only dateTo -- all events up to that point</li>
 *   <li>Both present -- bounded window</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
public interface AnalyticsKpisService {

    /**
     * Computes the three KPIs shown on the analytics dashboard.
     *
     * <p>Returned values:
     * <ol>
     *   <li>Total event count in the window</li>
     *   <li>Count of events with risk level 'high' (maximum severity stored)</li>
     *   <li>Count and percentage of events with status 'failure'</li>
     * </ol>
     *
     * <p>Example: tenant "acme" has 5000 events in the last 30 days,
     * 420 of which are high risk, and 95 have status failure.
     * Result -- totalEvents=5000, highRiskCount=420,
     * failedOperationsCount=95, failedOperationsPercentage=1.9.
     *
     * @param tenantId non-blank tenant identifier from the caller's JWT
     * @param dateFrom optional window start (inclusive)
     * @param dateTo   optional window end (inclusive)
     * @param rangeAll if true, ignore dateFrom and dateTo
     * @return populated {@link AnalyticsKpisResponse}
     */
     AnalyticsKpisResponse computeKpis(
            String tenantId, Instant dateFrom, Instant dateTo, boolean rangeAll);
}