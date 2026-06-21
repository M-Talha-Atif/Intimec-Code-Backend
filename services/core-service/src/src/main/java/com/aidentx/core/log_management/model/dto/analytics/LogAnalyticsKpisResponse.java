package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for {@code GET /api/v1/logs/analytics/kpis}.
 *
 * <p>All fields are derived from OpenSearch aggregations against the
 * main-logs index, scoped to the caller's tenant and an optional date window.
 *
 * <p>Example serialised response:
 * <pre>
 * {
 *   "totalErrors24h": 247,
 *   "criticalFailures": 18,
 *   "avgResponseTimeMs": 2800.0,
 *   "mttrMinutes": 32.0,
 *   "recurringPatterns": 5
 * }
 * </pre>
 *
 * <p>Frontend KPI card mapping:
 * <ul>
 *   <li>{@code totalErrors24h}    -- "Total Errors (24h)" card</li>
 *   <li>{@code criticalFailures}  -- "Critical Failures" card</li>
 *   <li>{@code avgResponseTimeMs} -- "Avg Response Time" card; frontend
 *       formats as "2.8s" by dividing by 1000</li>
 *   <li>{@code mttrMinutes}       -- "MTTR (minutes)" card</li>
 *   <li>{@code recurringPatterns} -- "Recurring Patterns" card; counts
 *       error messages that appeared 3+ times</li>
 * </ul>
 */
public class LogAnalyticsKpisResponse {

    /**
     * Total count of ERROR and WARN level events in the last 24 hours
     * (or full history when range=all).
     * Drives the "Total Errors (24h)" KPI card.
     */
    @JsonProperty("totalErrors24h")
    private long totalErrors24h;

    /**
     * Count of events where {@code level = 'ERROR'} within the window.
     * Drives the "Critical Failures" KPI card.
     */
    @JsonProperty("criticalFailures")
    private long criticalFailures;

    /**
     * Average value of {@code details.executionTime} across all events that
     * carry that field, in milliseconds. Events without the field are excluded
     * from the average.
     *
     * <p>Returns 0.0 when no events have an executionTime field in the window.
     * Frontend formats this as seconds: {@code (2800.0 / 1000).toFixed(1) + 's'}
     * to display "2.8s".
     */
    @JsonProperty("avgResponseTimeMs")
    private double avgResponseTimeMs;

    /**
     * Mean time to resolution in minutes across all resolved errors in the window.
     * Computed as the average of {@code (resolvedAt - @timestamp) / 60000}
     * for all documents where {@code isResolved = true}.
     *
     * <p>Returns 0.0 when no resolved errors exist in the window.
     *
     * <p>Example: an error logged at 10:00 and resolved at 10:32 contributes
     * 32.0 minutes to the average.
     */
    @JsonProperty("mttrMinutes")
    private double mttrMinutes;

    /**
     * Count of distinct error messages that appeared
     * {@link com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsKpisConstants#RECURRING_MIN_OCCURRENCES}
     * or more times within the window.
     *
     * <p>Example: if "LDAP bind failed" appeared 47 times and
     * "Timeout waiting for ServiceNow" appeared 23 times and no other message
     * appeared 3+ times, recurringPatterns = 2.
     *
     * <p>Drives the "Recurring Patterns" KPI card.
     */
    @JsonProperty("recurringPatterns")
    private long recurringPatterns;

    public LogAnalyticsKpisResponse() {}

    public LogAnalyticsKpisResponse(
            long totalErrors24h,
            long criticalFailures,
            double avgResponseTimeMs,
            double mttrMinutes,
            long recurringPatterns) {
        this.totalErrors24h = totalErrors24h;
        this.criticalFailures = criticalFailures;
        this.avgResponseTimeMs = avgResponseTimeMs;
        this.mttrMinutes = mttrMinutes;
        this.recurringPatterns = recurringPatterns;
    }

    public long getTotalErrors24h() { return totalErrors24h; }
    public void setTotalErrors24h(long totalErrors24h) { this.totalErrors24h = totalErrors24h; }

    public long getCriticalFailures() { return criticalFailures; }
    public void setCriticalFailures(long criticalFailures) { this.criticalFailures = criticalFailures; }

    public double getAvgResponseTimeMs() { return avgResponseTimeMs; }
    public void setAvgResponseTimeMs(double avgResponseTimeMs) { this.avgResponseTimeMs = avgResponseTimeMs; }

    public double getMttrMinutes() { return mttrMinutes; }
    public void setMttrMinutes(double mttrMinutes) { this.mttrMinutes = mttrMinutes; }

    public long getRecurringPatterns() { return recurringPatterns; }
    public void setRecurringPatterns(long recurringPatterns) { this.recurringPatterns = recurringPatterns; }
}