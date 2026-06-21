package com.aidentx.core.audit_management.service.impl;

import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventVolumeConstants;
import com.aidentx.core.audit_management.model.dto.analytics.EventVolumeEntry;
import com.aidentx.core.audit_management.model.dto.analytics.EventVolumeProjection;
import com.aidentx.core.audit_management.repository.AuditLogRepository;
import com.aidentx.core.audit_management.service.AnalyticsEventVolumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link AnalyticsEventVolumeService}.
 *
 * <p>When {@code rangeAll} is true, window resolution is skipped entirely.
 * The repository query is issued with the earliest possible Instant
 * ({@link Instant#EPOCH}) as the lower bound and {@link Instant#now()} as the
 * upper bound, effectively covering all stored data without requiring a
 * separate unbounded query method.
 *
 * <p>Gap filling still applies on the rangeAll path: every calendar day
 * between the first and last event date is included in the response with
 * zero counts for days that have no events.
 */
@Service
public class AnalyticsEventVolumeServiceImpl implements AnalyticsEventVolumeService {

    private static final Logger logger =
            LoggerFactory.getLogger(AnalyticsEventVolumeServiceImpl.class);

    private static final DateTimeFormatter DAY_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd");

    private final AuditLogRepository auditLogRepository;

    public AnalyticsEventVolumeServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When {@code rangeAll} is true, {@link Instant#EPOCH} and
     * end-of-today UTC are used as the query bounds so that all stored
     * rows are included. The gap-fill walk still runs between those bounds,
     * so very large history ranges will produce large response arrays.
     */
    @Override
    public List<EventVolumeEntry> computeEventVolume(
            String tenantId, Instant dateFrom, Instant dateTo, boolean rangeAll) {

        Instant resolvedFrom = rangeAll ? Instant.EPOCH : resolveWindowStart(dateFrom, dateTo);
        Instant resolvedTo   = rangeAll ? resolveEndOfToday() : resolveWindowEnd(dateTo);

        logger.info(
                AnalyticsEventVolumeConstants.LOG_COMPUTATION_STARTED,
                AnalyticsEventVolumeConstants.LOG_KEY_TENANT_ID, tenantId,
                AnalyticsEventVolumeConstants.LOG_KEY_DATE_FROM, resolvedFrom,
                AnalyticsEventVolumeConstants.LOG_KEY_DATE_TO, resolvedTo);

        List<EventVolumeProjection> rawRows = auditLogRepository
                .findDailyEventVolumeByRisk(tenantId, resolvedFrom, resolvedTo);

        Map<LocalDate, Map<String, Long>> indexedByDate = indexProjectionsByDate(rawRows);

        List<EventVolumeEntry> timeline = buildTimeline(resolvedFrom, resolvedTo, indexedByDate);

        logger.info(
                AnalyticsEventVolumeConstants.LOG_COMPUTATION_COMPLETE,
                AnalyticsEventVolumeConstants.LOG_KEY_ROW_COUNT, timeline.size());

        return timeline;
    }

    /**
     * Resolves the effective window start to a UTC midnight Instant.
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
     * Returns 23:59:59.999999999 UTC of today. Used as the upper query bound
     * on the rangeAll path to ensure events logged today are included.
     *
     * @return end-of-today Instant in UTC
     */
    private Instant resolveEndOfToday() {
        return LocalDate.now(ZoneOffset.UTC)
                .atTime(23, 59, 59, 999_999_999)
                .toInstant(ZoneOffset.UTC);
    }

    /**
     * Indexes raw projection rows into a nested map for O(1) lookup.
     * Structure: {@code Map<LocalDate, Map<riskLevel, count>>}
     *
     * @param rawRows projection list from repository; may be empty
     * @return nested map, never null
     */
    private Map<LocalDate, Map<String, Long>> indexProjectionsByDate(
            List<EventVolumeProjection> rawRows) {

        Map<LocalDate, Map<String, Long>> index = new HashMap<>();

        for (EventVolumeProjection row : rawRows) {
            LocalDate day = row.getEventDate();
            index.computeIfAbsent(day, d -> new HashMap<>())
                 .put(row.getRiskLevel(), row.getEventCount());
        }

        return index;
    }

    /**
     * Walks every calendar day in [resolvedFrom, resolvedTo] and builds one
     * {@link EventVolumeEntry} per day, defaulting missing risk levels to zero.
     *
     * @param resolvedFrom window start
     * @param resolvedTo   window end
     * @param index        date-indexed projection map
     * @return ordered list of entries, one per day
     */
    private List<EventVolumeEntry> buildTimeline(
            Instant resolvedFrom,
            Instant resolvedTo,
            Map<LocalDate, Map<String, Long>> index) {

        LocalDate startDate = resolvedFrom.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate   = resolvedTo.atZone(ZoneOffset.UTC).toLocalDate();

        List<EventVolumeEntry> timeline = new ArrayList<>();
        LocalDate cursor = startDate;

        while (!cursor.isAfter(endDate)) {
            Map<String, Long> riskCounts = index.getOrDefault(cursor, Map.of());

            EventVolumeEntry entry = new EventVolumeEntry(
                    cursor.format(DAY_LABEL_FORMATTER),
                    riskCounts.getOrDefault(AnalyticsEventVolumeConstants.RISK_LOW,    0L),
                    riskCounts.getOrDefault(AnalyticsEventVolumeConstants.RISK_MEDIUM, 0L),
                    riskCounts.getOrDefault(AnalyticsEventVolumeConstants.RISK_HIGH,   0L));

            timeline.add(entry);
            cursor = cursor.plusDays(1);
        }

        return timeline;
    }
}