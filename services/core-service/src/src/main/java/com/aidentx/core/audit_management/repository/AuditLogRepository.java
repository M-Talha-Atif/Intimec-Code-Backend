package com.aidentx.core.audit_management.repository;

import com.aidentx.core.audit_management.model.dto.analytics.EventDistributionProjection;
import com.aidentx.core.audit_management.model.dto.analytics.EventVolumeProjection;
import com.aidentx.core.audit_management.model.dto.analytics.HeatmapProjection;
import com.aidentx.core.audit_management.model.dto.analytics.TopActorProjection;
import com.aidentx.core.audit_management.model.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for persisting AuditLog entities.
 * <p>
 * Provides CRUD operations for the AuditLog table.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID>, JpaSpecificationExecutor<AuditLog> {

    /**
     * Finds audit logs by status field containing the specified value (case-insensitive).
     * Results are ordered by timestamp in descending order (newest first).
     *
     * @param status   the status value to search for (partial match, case-insensitive)
     * @param pageable pagination and sorting information
     * @return paginated list of audit logs matching the status filter
     */
    Page<AuditLog> findByStatusContainingIgnoreCaseOrderByTimestampDesc(String status, Pageable pageable);

    /**
     * Finds audit logs by event field containing the specified value (case-insensitive).
     * Results are ordered by timestamp in descending order (newest first).
     *
     * @param event    the event value to search for (partial match, case-insensitive)
     * @param pageable pagination and sorting information
     * @return paginated list of audit logs matching the event filter
     */
    Page<AuditLog> findByEventContainingIgnoreCaseOrderByTimestampDesc(String event, Pageable pageable);

    /**
     * Finds audit logs by object type field containing the specified value (case-insensitive).
     * Results are ordered by timestamp in descending order (newest first).
     *
     * @param objectType the object type value to search for (partial match, case-insensitive)
     * @param pageable   pagination and sorting information
     * @return paginated list of audit logs matching the object type filter
     */
    Page<AuditLog> findByObjectTypeContainingIgnoreCaseOrderByTimestampDesc(String objectType, Pageable pageable);

    /**
     * Finds audit logs by risk field containing the specified value (case-insensitive).
     * Results are ordered by timestamp in descending order (newest first).
     *
     * @param risk     the risk value to search for (partial match, case-insensitive)
     * @param pageable pagination and sorting information
     * @return paginated list of audit logs matching the risk filter
     */
    Page<AuditLog> findByRiskContainingIgnoreCaseOrderByTimestampDesc(String risk, Pageable pageable);

    /**
     * Finds audit logs with timestamps between the specified date range (inclusive).
     * Results are ordered by timestamp in descending order (newest first).
     *
     * @param dateFrom the start date for the filter (inclusive)
     * @param dateTo   the end date for the filter (inclusive)
     * @param pageable pagination and sorting information
     * @return paginated list of audit logs within the specified date range
     */
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(Instant dateFrom, Instant dateTo, Pageable pageable);

    /**
     * Finds audit logs with timestamps greater than or equal to the specified date.
     * Results are ordered by timestamp in descending order (newest first).
     *
     * @param dateFrom the start date for the filter (inclusive)
     * @param pageable pagination and sorting information
     * @return paginated list of audit logs from the specified date onwards
     */
    Page<AuditLog> findByTimestampGreaterThanEqualOrderByTimestampDesc(Instant dateFrom, Pageable pageable);

    /**
     * Finds audit logs with timestamps less than or equal to the specified date.
     * Results are ordered by timestamp in descending order (newest first).
     *
     * @param dateTo   the end date for the filter (inclusive)
     * @param pageable pagination and sorting information
     * @return paginated list of audit logs up to the specified date
     */
    Page<AuditLog> findByTimestampLessThanEqualOrderByTimestampDesc(Instant dateTo, Pageable pageable);

    /**
     * Gets all distinct event values from the audit logs.
     *
     * @return list of unique events
     */
    @Query("SELECT DISTINCT a.event FROM AuditLog a WHERE a.event IS NOT NULL ORDER BY a.event ASC")
    List<String> findDistinctEvents();

    /**
     * Gets all distinct object type values from the audit logs.
     *
     * @return list of unique object types
     */
    @Query("SELECT DISTINCT a.objectType FROM AuditLog a WHERE a.objectType IS NOT NULL ORDER BY a.objectType ASC")
    List<String> findDistinctObjectTypes();

    /**
     * Gets all distinct risk values from the audit logs.
     *
     * @return list of unique risk levels
     */
    @Query("SELECT DISTINCT a.risk FROM AuditLog a WHERE a.risk IS NOT NULL ORDER BY a.risk ASC")
    List<String> findDistinctRisks();

    /**
     * Gets all distinct status values from the audit logs.
     *
     * @return list of unique statuses
     */
    @Query("SELECT DISTINCT a.status FROM AuditLog a WHERE a.status IS NOT NULL ORDER BY a.status ASC")
    List<String> findDistinctStatuses();

    /**
     * Gets all distinct event values from the audit logs for a specific tenant.
     *
     * @param tenantId the tenant ID to filter by
     * @return list of unique events for the tenant
     */
    @Query("SELECT DISTINCT a.event FROM AuditLog a WHERE a.event IS NOT NULL AND a.tenantId = :tenantId ORDER BY a.event ASC")
    List<String> findDistinctEventsByTenantId(String tenantId);

    /**
     * Gets all distinct object type values from the audit logs for a specific tenant.
     *
     * @param tenantId the tenant ID to filter by
     * @return list of unique object types for the tenant
     */
    @Query("SELECT DISTINCT a.objectType FROM AuditLog a WHERE a.objectType IS NOT NULL AND a.tenantId = :tenantId ORDER BY a.objectType ASC")
    List<String> findDistinctObjectTypesByTenantId(String tenantId);

    /**
     * Gets all distinct risk values from the audit logs for a specific tenant.
     *
     * @param tenantId the tenant ID to filter by
     * @return list of unique risk levels for the tenant
     */
    @Query("SELECT DISTINCT a.risk FROM AuditLog a WHERE a.risk IS NOT NULL AND a.tenantId = :tenantId ORDER BY a.risk ASC")
    List<String> findDistinctRisksByTenantId(String tenantId);

    /**
     * Gets all distinct status values from the audit logs for a specific tenant.
     *
     * @param tenantId the tenant ID to filter by
     * @return list of unique statuses for the tenant
     */
    @Query("SELECT DISTINCT a.status FROM AuditLog a WHERE a.status IS NOT NULL AND a.tenantId = :tenantId ORDER BY a.status ASC")
    List<String> findDistinctStatusesByTenantId(String tenantId);

    /**
     * Returns daily event counts grouped by risk level for a tenant within a date window.
     *
     * <p>Each row in the result corresponds to one (date, risk) pair that has at
     * least one event. Days with zero events for a given risk level produce no row.
     * The service layer is responsible for filling those gaps with zero.
     *
     * <p>The CAST to LocalDate truncates the stored UTC Instant to a calendar day.
     * All timestamps are stored in UTC so the truncation is consistent regardless
     * of where the application server runs.
     *
     * @param tenantId non-blank tenant identifier
     * @param dateFrom window start, inclusive
     * @param dateTo   window end, inclusive
     * @return list of (date, riskLevel, count) projections ordered by date ascending
     */
    @Query("""
            SELECT
                CAST(a.timestamp AS LocalDate) AS eventDate,
                a.risk                         AS riskLevel,
                COUNT(a)                       AS eventCount
            FROM AuditLog a
            WHERE a.tenantId  = :tenantId
              AND a.timestamp >= :dateFrom
              AND a.timestamp <= :dateTo
            GROUP BY CAST(a.timestamp AS LocalDate), a.risk
            ORDER BY CAST(a.timestamp AS LocalDate) ASC
            """)
    List<EventVolumeProjection> findDailyEventVolumeByRisk(
            @Param("tenantId") String tenantId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo);

    /**
     * Returns all distinct event names and their counts for a tenant within a
     * date window, ordered by count descending with no row limit applied.
     *
     * <p>Used when the caller passes {@code limit=0} to request the full
     * distribution. No LIMIT clause is appended to the query.
     *
     * @param tenantId non-blank tenant identifier
     * @param dateFrom window start, inclusive
     * @param dateTo   window end, inclusive
     * @return full list of (eventName, count) projections ordered by count DESC
     */
    @Query("""
            SELECT a.event AS eventName, COUNT(a) AS eventCount
            FROM AuditLog a
            WHERE a.tenantId  = :tenantId
              AND a.timestamp >= :dateFrom
              AND a.timestamp <= :dateTo
              AND a.event IS NOT NULL
            GROUP BY a.event
            ORDER BY COUNT(a) DESC
            """)
    List<EventDistributionProjection> findEventDistribution(
            @Param("tenantId") String tenantId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo);

    /**
     * Returns the top N distinct event names and their counts for a tenant
     * within a date window, ordered by count descending.
     *
     * <p>Used when the caller supplies a positive {@code limit} value (e.g. 5, 10).
     * The LIMIT is applied via {@link Pageable} so Spring Data JPA handles the
     * database-specific syntax (LIMIT for PostgreSQL) without manual JPQL changes.
     *
     * <p>Example: pageable = PageRequest.of(0, 10) returns the top 10 event types.
     *
     * @param tenantId non-blank tenant identifier
     * @param dateFrom window start, inclusive
     * @param dateTo   window end, inclusive
     * @param pageable pagination config; only page size is used, always page 0
     * @return top-N list of (eventName, count) projections ordered by count DESC
     */
    @Query("""
            SELECT a.event AS eventName, COUNT(a) AS eventCount
            FROM AuditLog a
            WHERE a.tenantId  = :tenantId
              AND a.timestamp >= :dateFrom
              AND a.timestamp <= :dateTo
              AND a.event IS NOT NULL
            GROUP BY a.event
            ORDER BY COUNT(a) DESC
            """)
    List<EventDistributionProjection> findEventDistributionTopN(
            @Param("tenantId") String tenantId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable);

    /**
     * Returns all distinct actors with their total event count, failure count,
     * and weighted risk sum for a tenant within a date window, ordered by total
     * event count descending.
     *
     * <p>The weighted risk sum is computed inline via a CASE expression assigning
     * integer weights to each risk level: low=1, medium=2, high=3, unknown=0.
     * Events with a null or unrecognised risk value contribute 0 to the sum.
     *
     * <p>Used when the caller passes {@code limit=0} to request all actors.
     *
     * @param tenantId non-blank tenant identifier
     * @param dateFrom window start, inclusive
     * @param dateTo   window end, inclusive
     * @param failure  failure status literal from
     *                 {@link com.aidentx.core.audit_management.config.constants.analytics.AnalyticsTopActorsConstants#STATUS_FAILURE}
     * @return full list of actor projections ordered by totalEvents DESC
     */
    @Query("""
            SELECT
                a.actorName AS actorName,
                a.actorType AS actorType,
                COUNT(a)    AS totalEvents,
                SUM(CASE WHEN a.status = :failure THEN 1L ELSE 0L END) AS failureCount,
                SUM(CASE a.risk
                      WHEN 'low'    THEN 1L
                      WHEN 'medium' THEN 2L
                      WHEN 'high'   THEN 3L
                      ELSE 0L END) AS weightedRiskSum
            FROM AuditLog a
            WHERE a.tenantId  = :tenantId
              AND a.timestamp >= :dateFrom
              AND a.timestamp <= :dateTo
              AND a.actorName IS NOT NULL
            GROUP BY a.actorName, a.actorType
            ORDER BY COUNT(a) DESC
            """)
    List<TopActorProjection> findTopActors(
            @Param("tenantId") String tenantId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("failure") String failure);

    /**
     * Returns the top N distinct actors with their total event count, failure count,
     * and weighted risk sum for a tenant within a date window, ordered by total
     * event count descending.
     *
     * <p>The LIMIT is applied via {@link Pageable}. Always pass
     * {@code PageRequest.of(0, N)} where N is the desired result count.
     *
     * @param tenantId non-blank tenant identifier
     * @param dateFrom window start, inclusive
     * @param dateTo   window end, inclusive
     * @param failure  failure status literal
     * @param pageable pagination config; only page size is used, always page 0
     * @return top-N list of actor projections ordered by totalEvents DESC
     */
    @Query("""
            SELECT
                a.actorName AS actorName,
                a.actorType AS actorType,
                COUNT(a)    AS totalEvents,
                SUM(CASE WHEN a.status = :failure THEN 1L ELSE 0L END) AS failureCount,
                SUM(CASE a.risk
                      WHEN 'low'    THEN 2L
                      WHEN 'medium' THEN 2L
                      WHEN 'high'   THEN 3L
                      ELSE 0L END) AS weightedRiskSum
            FROM AuditLog a
            WHERE a.tenantId  = :tenantId
              AND a.timestamp >= :dateFrom
              AND a.timestamp <= :dateTo
              AND a.actorName IS NOT NULL
            GROUP BY a.actorName, a.actorType
            ORDER BY COUNT(a) DESC
            """)
    List<TopActorProjection> findTopActorsTopN(
            @Param("tenantId") String tenantId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("failure") String failure,
            Pageable pageable);

    /**
     * Returns all distinct (objectType, event) pairs and their counts for a
     * tenant within a date window, ordered by count descending with no row limit.
     *
     * <p>Rows where objectType or event is null are excluded. Only pairs with
     * at least one event appear in the result set.
     *
     * <p>Used when the caller passes {@code limit=0} to request all heatmap cells.
     *
     * <p>Example: tenant "acme" in a 30-day window with 4 object types and
     * 6 event names produces at most 24 rows (4 x 6), but typically fewer
     * because not every combination has events.
     *
     * @param tenantId non-blank tenant identifier
     * @param dateFrom window start, inclusive
     * @param dateTo   window end, inclusive
     * @return full list of (objectType, eventName, count) projections ordered
     * by count DESC
     */
    @Query("""
            SELECT
                a.objectType AS objectType,
                a.event      AS eventName,
                COUNT(a)     AS eventCount
            FROM AuditLog a
            WHERE a.tenantId   = :tenantId
              AND a.timestamp  >= :dateFrom
              AND a.timestamp  <= :dateTo
              AND a.objectType IS NOT NULL
              AND a.event      IS NOT NULL
            GROUP BY a.objectType, a.event
            ORDER BY COUNT(a) DESC
            """)
    List<HeatmapProjection> findHeatmapData(
            @Param("tenantId") String tenantId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo);

    /**
     * Returns the top N distinct (objectType, event) pairs and their counts for
     * a tenant within a date window, ordered by count descending.
     *
     * <p>The LIMIT is applied via {@link Pageable} so Spring Data JPA emits the
     * correct database-specific syntax. Always pass {@code PageRequest.of(0, N)}.
     *
     * <p>Example: {@code PageRequest.of(0, 50)} returns the 50 most active
     * (objectType, eventName) pairs. For a tenant with 4 object types and
     * 6 event names, this covers all 24 possible pairs and returns all of them.
     * For a tenant with 10 object types and 15 event names (150 possible pairs),
     * only the top 50 by count are returned.
     *
     * @param tenantId non-blank tenant identifier
     * @param dateFrom window start, inclusive
     * @param dateTo   window end, inclusive
     * @param pageable pagination config; only page size is used, always page 0
     * @return top-N list of (objectType, eventName, count) projections ordered
     * by count DESC
     */
    @Query("""
            SELECT
                a.objectType AS objectType,
                a.event      AS eventName,
                COUNT(a)     AS eventCount
            FROM AuditLog a
            WHERE a.tenantId   = :tenantId
              AND a.timestamp  >= :dateFrom
              AND a.timestamp  <= :dateTo
              AND a.objectType IS NOT NULL
              AND a.event      IS NOT NULL
            GROUP BY a.objectType, a.event
            ORDER BY COUNT(a) DESC
            """)
    List<HeatmapProjection> findHeatmapDataTopN(
            @Param("tenantId") String tenantId,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            Pageable pageable);
}
