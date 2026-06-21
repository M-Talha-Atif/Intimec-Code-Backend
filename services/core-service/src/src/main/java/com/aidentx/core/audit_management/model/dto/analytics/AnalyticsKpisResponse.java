package com.aidentx.core.audit_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for {@code GET /api/v1/audit-logs/analytics/kpis}.
 *
 * <p>All fields are derived from COUNT aggregations against audit_logs,
 * scoped to the caller's tenant and an optional date window.
 *
 * <p>Example serialised response:
 * <pre>
 * {
 *   "totalEvents": 4200,
 *   "highRiskCount": 312,
 *   "failedOperationsCount": 89,
 *   "failedOperationsPercentage": 2.1
 * }
 * </pre>
 *
 * <p>Frontend mapping:
 * <ul>
 *   <li>{@code totalEvents} -- "Total Events" KPI card</li>
 *   <li>{@code highRiskCount} -- "High Risk" KPI card</li>
 *   <li>{@code failedOperationsCount} + {@code failedOperationsPercentage}
 *       -- "Failed Operations" KPI card rendered as "89 (2.1%)"</li>
 * </ul>
 */
public class AnalyticsKpisResponse {

    /**
     * Total audit events in the requested window for the tenant.
     */
    @JsonProperty("totalEvents")
    private long totalEvents;

    /**
     * Count of events where {@code risk = 'high'}.
     * High is the maximum severity stored in audit_logs.risk.
     * Drives the "High Risk" KPI card.
     */
    @JsonProperty("highRiskCount")
    private long highRiskCount;

    /**
     * Count of events where {@code status = 'failure'}.
     * Drives the count portion of the "Failed Operations" KPI card.
     */
    @JsonProperty("failedOperationsCount")
    private long failedOperationsCount;

    /**
     * {@code (failedOperationsCount / totalEvents) * 100}, rounded to 1 decimal place.
     * Returns {@code 0.0} when {@code totalEvents} is zero.
     * Drives the percentage portion of the "Failed Operations" KPI card.
     */
    @JsonProperty("failedOperationsPercentage")
    private double failedOperationsPercentage;

    public AnalyticsKpisResponse() {}

    public AnalyticsKpisResponse(
            long totalEvents,
            long highRiskCount,
            long failedOperationsCount,
            double failedOperationsPercentage) {
        this.totalEvents = totalEvents;
        this.highRiskCount = highRiskCount;
        this.failedOperationsCount = failedOperationsCount;
        this.failedOperationsPercentage = failedOperationsPercentage;
    }

    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }

    public long getHighRiskCount() { return highRiskCount; }
    public void setHighRiskCount(long highRiskCount) { this.highRiskCount = highRiskCount; }

    public long getFailedOperationsCount() { return failedOperationsCount; }
    public void setFailedOperationsCount(long failedOperationsCount) { this.failedOperationsCount = failedOperationsCount; }

    public double getFailedOperationsPercentage() { return failedOperationsPercentage; }
    public void setFailedOperationsPercentage(double failedOperationsPercentage) { this.failedOperationsPercentage = failedOperationsPercentage; }
}