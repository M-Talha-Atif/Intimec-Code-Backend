package com.aidentx.core.audit_management.model.dto.analytics;

import java.time.LocalDate;

/**
 * Raw projection interface for the GROUP BY query result from the repository.
 *
 * <p>Spring Data JPA maps each row returned by the JPQL query directly into
 * this interface using its getter names as the column aliases. The service
 * then converts these projections into {@link EventVolumeEntry} instances,
 * applying date formatting and filling zero counts for absent risk levels.
 *
 * <p>The JPQL query that populates this projection:
 * <pre>
 *   SELECT
 *     CAST(a.timestamp AS LocalDate) AS eventDate,
 *     a.risk                         AS riskLevel,
 *     COUNT(a)                       AS eventCount
 *   FROM AuditLog a
 *   WHERE a.tenantId  = :tenantId
 *     AND a.timestamp >= :dateFrom
 *     AND a.timestamp <= :dateTo
 *   GROUP BY CAST(a.timestamp AS LocalDate), a.risk
 *   ORDER BY CAST(a.timestamp AS LocalDate) ASC
 * </pre>
 *
 * <p>One row is returned per (date, risk) pair that has at least one event.
 * Days with zero events for a given risk level produce no row, so the service
 * must fill gaps with zero before returning the final response.
 * Risk values in the result will be one of: 'low', 'medium', 'high'.
 */
public interface EventVolumeProjection {

    /**
     * The calendar date on which the events occurred.
     * Derived by casting the {@code timestamp} column to a date.
     *
     * @return event date, never null for rows returned by the query
     */
    LocalDate getEventDate();

    /**
     * The risk level of the events counted in this row.
     * One of: 'low', 'medium', 'high', or null for events where risk was not recorded.
     * No critical level exists in audit_logs.risk.
     *
     * @return risk level string as stored in audit_logs.risk
     */
    String getRiskLevel();

    /**
     * Count of events for this (date, risk) combination.
     *
     * @return event count, always >= 1 for rows returned by the GROUP BY query
     */
    long getEventCount();
}