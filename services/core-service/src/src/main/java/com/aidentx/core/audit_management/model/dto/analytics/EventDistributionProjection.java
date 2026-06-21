package com.aidentx.core.audit_management.model.dto.analytics;

/**
 * Raw projection interface for the GROUP BY event query result from the repository.
 *
 * <p>Spring Data JPA maps each row returned by the JPQL query directly into
 * this interface by aligning getter names with the query column aliases.
 * The service converts these into {@link EventDistributionEntry} instances.
 *
 * <p>The JPQL query that populates this projection:
 * <pre>
 *   SELECT a.event AS eventName, COUNT(a) AS eventCount
 *   FROM AuditLog a
 *   WHERE a.tenantId  = :tenantId
 *     AND a.timestamp >= :dateFrom
 *     AND a.timestamp <= :dateTo
 *   GROUP BY a.event
 *   ORDER BY COUNT(a) DESC
 * </pre>
 *
 * <p>One row is returned per distinct non-null event name. The LIMIT clause
 * is applied at the repository level via two dedicated query methods:
 * one using a {@code Pageable} for bounded results and one without for
 * unbounded results. This avoids building dynamic JPQL strings in the
 * service layer.
 */
public interface EventDistributionProjection {

    /**
     * The event name as stored in audit_logs.event.
     *
     * @return event name string, never null for rows returned by the query
     */
    String getEventName();

    /**
     * Total occurrences of this event name in the queried window.
     *
     * @return count, always >= 1 for rows returned by the GROUP BY query
     */
    long getEventCount();
}