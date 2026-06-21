package com.aidentx.core.audit_management.model.dto.analytics;

/**
 * Raw projection interface for the GROUP BY objectType, event query result.
 *
 * <p>Spring Data JPA maps each row returned by the JPQL query directly into
 * this interface by aligning getter names with the query column aliases.
 * The service maps these to {@link HeatmapEntry} instances with no further
 * computation required.
 *
 * <p>The JPQL query that populates this projection:
 * <pre>
 *   SELECT
 *     a.objectType AS objectType,
 *     a.event      AS eventName,
 *     COUNT(a)     AS eventCount
 *   FROM AuditLog a
 *   WHERE a.tenantId   = :tenantId
 *     AND a.timestamp  >= :dateFrom
 *     AND a.timestamp  <= :dateTo
 *     AND a.objectType IS NOT NULL
 *     AND a.event      IS NOT NULL
 *   GROUP BY a.objectType, a.event
 *   ORDER BY COUNT(a) DESC
 * </pre>
 *
 * <p>One row is returned per distinct (objectType, event) pair that has at
 * least one event. Zero cells are absent from the result set. The frontend
 * is responsible for rendering absent cells as zero.
 *
 * <p>Example result set rows mapped to this projection:
 * <pre>
 *   objectType="Role",       eventName="assign",    eventCount=142
 *   objectType="Role",       eventName="provision", eventCount=38
 *   objectType="User",       eventName="assign",    eventCount=97
 *   objectType="Permission", eventName="approve",   eventCount=21
 * </pre>
 */
public interface HeatmapProjection {

    /**
     * Object type as stored in audit_logs.object_type.
     * Examples: "Role", "User", "Permission".
     *
     * @return object type string, never null for rows returned by the query
     */
    String getObjectType();

    /**
     * Event name as stored in audit_logs.event.
     * Examples: "assign", "provision", "approve", "revoke".
     *
     * @return event name string, never null for rows returned by the query
     */
    String getEventName();

    /**
     * Count of events for this (objectType, eventName) pair in the window.
     *
     * @return event count, always >= 1 for rows returned by the GROUP BY query
     */
    long getEventCount();
}