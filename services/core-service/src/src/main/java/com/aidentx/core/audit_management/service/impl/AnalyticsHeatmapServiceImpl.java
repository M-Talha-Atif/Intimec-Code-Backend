package com.aidentx.core.audit_management.service.impl;

import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventVolumeConstants;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsHeatmapConstants;
import com.aidentx.core.audit_management.model.dto.analytics.HeatmapEntry;
import com.aidentx.core.audit_management.model.dto.analytics.HeatmapProjection;
import com.aidentx.core.audit_management.repository.AuditLogRepository;
import com.aidentx.core.audit_management.service.AnalyticsHeatmapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link AnalyticsHeatmapService}.
 *
 * <p>Execution sequence per request:
 * <ol>
 *   <li>Resolve the effective date window (or apply rangeAll bounds).</li>
 *   <li>Resolve the effective limit (null to DEFAULT_LIMIT, 0 to all, N to top-N).</li>
 *   <li>Route to the appropriate repository method.</li>
 *   <li>Map projections to {@link HeatmapEntry} instances and return.</li>
 * </ol>
 *
 * <p>No gap filling is needed here unlike the event volume timeline. The
 * heatmap only returns cells that have at least one event. Zero cells are
 * intentionally absent and the frontend handles them during matrix rendering.
 *
 * <p>Example illustrating the rangeAll path:
 * <pre>
 *   Request:  GET /api/v1/audit-logs/analytics/heatmap?range=all&limit=0
 *   Resolved: dateFrom=Instant.EPOCH, dateTo=end-of-today
 *   Result:   all (objectType, eventName) pairs ever recorded for the tenant
 * </pre>
 *
 * <p>Example illustrating the default path:
 * <pre>
 *   Request:  GET /api/v1/audit-logs/analytics/heatmap
 *   Resolved: dateFrom=30 days ago midnight UTC, dateTo=end-of-today UTC
 *   Result:   top 50 (objectType, eventName) pairs in the last 30 days
 * </pre>
 *
 * <p>Example illustrating a bounded custom window:
 * <pre>
 *   Request:  GET /api/v1/audit-logs/analytics/heatmap
 *                 ?dateFrom=2025-01-01T00:00:00Z
 *                 &dateTo=2025-01-31T23:59:59Z
 *                 &limit=10
 *   Resolved: dateFrom=2025-01-01 midnight, dateTo=2025-01-31 end-of-day
 *   Result:   top 10 (objectType, eventName) pairs in January 2025
 * </pre>
 */
@Service
public class AnalyticsHeatmapServiceImpl implements AnalyticsHeatmapService {

    private static final Logger logger =
            LoggerFactory.getLogger(AnalyticsHeatmapServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AnalyticsHeatmapServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When {@code rangeAll} is true, {@link Instant#EPOCH} is used as the
     * lower bound and end-of-today UTC as the upper bound, so all stored
     * rows for the tenant are included in the GROUP BY aggregation.
     *
     * <p>When {@code rangeAll} is false, the window is resolved exactly as in
     * the event distribution and top actors services: dateFrom truncated to
     * midnight UTC, dateTo extended to 23:59:59.999999999 UTC, with a 30-day
     * default when both are null.
     */
    @Override
    public List<HeatmapEntry> computeHeatmap(
            String tenantId, Instant dateFrom, Instant dateTo, Integer limit, boolean rangeAll) {

        Instant resolvedFrom = rangeAll ? Instant.EPOCH : resolveWindowStart(dateFrom, dateTo);
        Instant resolvedTo   = rangeAll ? resolveEndOfToday() : resolveWindowEnd(dateTo);
        int resolvedLimit    = resolveLimit(limit);
        boolean returnAll    = resolvedLimit == AnalyticsHeatmapConstants.LIMIT_ALL;

        logger.info(
                AnalyticsHeatmapConstants.LOG_COMPUTATION_STARTED,
                AnalyticsHeatmapConstants.LOG_KEY_TENANT_ID, tenantId,
                AnalyticsHeatmapConstants.LOG_KEY_DATE_FROM, resolvedFrom,
                AnalyticsHeatmapConstants.LOG_KEY_DATE_TO, resolvedTo,
                AnalyticsHeatmapConstants.LOG_KEY_RETURN_ALL, returnAll);

        List<HeatmapProjection> rawRows =
                fetchHeatmapData(tenantId, resolvedFrom, resolvedTo, resolvedLimit, returnAll);

        List<HeatmapEntry> result = mapToEntries(rawRows);

        logger.info(
                AnalyticsHeatmapConstants.LOG_COMPUTATION_COMPLETE,
                AnalyticsHeatmapConstants.LOG_KEY_RESULT_COUNT, result.size());

        return result;
    }

    /**
     * Resolves a null or supplied limit to its effective integer value.
     *
     * <p>Null is substituted with {@link AnalyticsHeatmapConstants#DEFAULT_LIMIT}
     * (50) because a heatmap with potentially hundreds of cells should not
     * return everything by default. 50 covers a matrix of roughly 10 object
     * types by 5 event names which is a typical tenant dataset size.
     *
     * <p>Example:
     * <pre>
     *   limit=null  -- resolves to 50 (top 50 cells)
     *   limit=0     -- resolves to 0  (all cells, no truncation)
     *   limit=10    -- resolves to 10 (top 10 cells)
     * </pre>
     *
     * @param limit caller-supplied limit; may be null
     * @return effective limit integer
     */
    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return AnalyticsHeatmapConstants.DEFAULT_LIMIT;
        }
        return limit;
    }

    /**
     * Routes to the correct repository method based on whether the result
     * set is bounded or unbounded.
     *
     * <p>For bounded queries {@link PageRequest#of(int, int)} is constructed
     * with page=0 and size=resolvedLimit, equivalent to OFFSET 0 LIMIT N.
     *
     * <p>Example:
     * <pre>
     *   returnAll=true  -- calls findHeatmapData, no LIMIT clause
     *   returnAll=false -- calls findHeatmapDataTopN with PageRequest.of(0, 50)
     * </pre>
     *
     * @param tenantId      non-blank tenant ID
     * @param resolvedFrom  window start
     * @param resolvedTo    window end
     * @param resolvedLimit effective limit after null resolution
     * @param returnAll     true when the caller requested all cells
     * @return projection list from repository
     */
    private List<HeatmapProjection> fetchHeatmapData(
            String tenantId,
            Instant resolvedFrom,
            Instant resolvedTo,
            int resolvedLimit,
            boolean returnAll) {

        if (returnAll) {
            return auditLogRepository.findHeatmapData(tenantId, resolvedFrom, resolvedTo);
        }

        Pageable pageable = PageRequest.of(0, resolvedLimit);
        return auditLogRepository.findHeatmapDataTopN(tenantId, resolvedFrom, resolvedTo, pageable);
    }

    /**
     * Maps a list of {@link HeatmapProjection} rows to {@link HeatmapEntry}
     * instances. Order from the repository is preserved -- results remain
     * sorted by count descending as specified in the query.
     *
     * <p>Example mapping:
     * <pre>
     *   projection: objectType="Role", eventName="assign", eventCount=142
     *   entry:    { "objectType": "Role", "eventName": "assign", "count": 142 }
     * </pre>
     *
     * @param rawRows projection rows from the repository; may be empty
     * @return mapped entry list, never null
     */
    private List<HeatmapEntry> mapToEntries(List<HeatmapProjection> rawRows) {
        return rawRows.stream()
                .map(row -> new HeatmapEntry(
                        row.getObjectType(),
                        row.getEventName(),
                        row.getEventCount()))
                .collect(Collectors.toList());
    }

    /**
     * Resolves the effective window start to a UTC midnight Instant.
     *
     * <p>Resolution rules:
     * <ul>
     *   <li>dateFrom present -- use dateFrom truncated to UTC midnight.</li>
     *   <li>dateFrom null, dateTo present -- go back 30 days from dateTo midnight.</li>
     *   <li>Both null -- go back 30 days from now.</li>
     * </ul>
     *
     * <p>Example: dateTo=2025-03-18T14:30:00Z, dateFrom=null
     * -- resolvedFrom = 2025-02-16T00:00:00Z
     *
     * @param dateFrom caller-supplied start, may be null
     * @param dateTo   caller-supplied end, may be null
     * @return resolved window start at UTC midnight
     */
    private Instant resolveWindowStart(Instant dateFrom, Instant dateTo) {
        if (dateFrom != null) {
            return dateFrom.truncatedTo(ChronoUnit.DAYS);
        }

        Instant anchor = (dateTo != null) ? dateTo : Instant.now();
        return anchor
                .truncatedTo(ChronoUnit.DAYS)
                .minus(AnalyticsEventVolumeConstants.DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS);
    }

    /**
     * Resolves the effective window end to 23:59:59.999999999 UTC of the
     * caller-supplied or today's date.
     *
     * <p>Example: dateTo=2025-03-18T09:00:00Z
     * -- resolvedTo = 2025-03-18T23:59:59.999999999Z
     *
     * @param dateTo caller-supplied end, may be null
     * @return resolved window end at end-of-day UTC
     */
    private Instant resolveWindowEnd(Instant dateTo) {
        LocalDate endDate = (dateTo != null)
                ? dateTo.atZone(ZoneOffset.UTC).toLocalDate()
                : LocalDate.now(ZoneOffset.UTC);

        return endDate
                .atTime(23, 59, 59, 999_999_999)
                .toInstant(ZoneOffset.UTC);
    }

    /**
     * Returns 23:59:59.999999999 UTC of today.
     * Used as the upper query bound on the rangeAll path.
     *
     * @return end-of-today Instant in UTC
     */
    private Instant resolveEndOfToday() {
        return LocalDate.now(ZoneOffset.UTC)
                .atTime(23, 59, 59, 999_999_999)
                .toInstant(ZoneOffset.UTC);
    }
}