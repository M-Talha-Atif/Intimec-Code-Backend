package com.aidentx.core.audit_management.model.dto.analytics;

/**
 * Raw projection interface for the GROUP BY actorName, actorType query result.
 *
 * <p>Spring Data JPA maps each row returned by the JPQL query directly into
 * this interface by aligning getter names with the query column aliases. The
 * service converts these into {@link TopActorEntry} instances, computing a
 * deterministic risk score from the weighted risk sum and total event count.
 *
 * <p>The JPQL query that populates this projection:
 * <pre>
 *   SELECT
 *     a.actorName AS actorName,
 *     a.actorType AS actorType,
 *     COUNT(a)    AS totalEvents,
 *     SUM(CASE WHEN a.status = :failure THEN 1L ELSE 0L END) AS failureCount,
 *     SUM(CASE a.risk
 *           WHEN 'low'    THEN 1L
 *           WHEN 'medium' THEN 2L
 *           WHEN 'high'   THEN 3L
 *           ELSE 0L END) AS weightedRiskSum
 *   FROM AuditLog a
 *   WHERE a.tenantId  = :tenantId
 *     AND a.timestamp >= :dateFrom
 *     AND a.timestamp <= :dateTo
 *     AND a.actorName IS NOT NULL
 *   GROUP BY a.actorName, a.actorType
 *   ORDER BY COUNT(a) DESC
 * </pre>
 *
 * <p>One row per distinct (actorName, actorType) pair. Both the failure count
 * and weighted risk sum are derived inline via CASE expressions so the full
 * actor summary is produced in a single query with no post-query joins.
 */
public interface TopActorProjection {

    /**
     * Actor name as stored in audit_logs.actor_name.
     *
     * @return actor name, never null for rows returned by the query
     */
    String getActorName();

    /**
     * Actor type as stored in audit_logs.actor_type.
     * Typical values: "user", "system", "service".
     *
     * @return actor type string or null if not recorded on the originating event
     */
    String getActorType();

    /**
     * Total audit events attributed to this (actorName, actorType) pair
     * within the queried window.
     *
     * @return total event count, always >= 1 for rows returned by the GROUP BY query
     */
    long getTotalEvents();

    /**
     * Count of events with status 'failure' for this actor within the window.
     * Derived via a CASE expression inside the GROUP BY query.
     *
     * @return failure count, >= 0
     */
    long getFailureCount();

    /**
     * Sum of per-event risk weights for this actor within the window.
     * Each event contributes its risk weight to this sum:
     * low=1, medium=2, high=3, null/unknown=0.
     *
     * <p>Used together with {@link #getTotalEvents()} to compute the
     * normalised risk score in the service layer:
     * <pre>
     *   riskScore = ROUND( (weightedRiskSum / totalEvents) / 3.0 * 100 )
     * </pre>
     *
     * <p>Example: 7 low + 3 high events on the same actor:
     * <pre>
     *   weightedRiskSum = (7*1) + (3*3) = 16
     *   riskScore       = ROUND( (16/10) / 3.0 * 100 ) = 53
     * </pre>
     *
     * @return sum of risk weights, >= 0
     */
    long getWeightedRiskSum();
}