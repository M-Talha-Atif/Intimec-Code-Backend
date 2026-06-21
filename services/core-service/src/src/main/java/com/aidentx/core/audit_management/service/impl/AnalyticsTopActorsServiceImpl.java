package com.aidentx.core.audit_management.service.impl;

import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventVolumeConstants;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsTopActorsConstants;
import com.aidentx.core.audit_management.model.dto.analytics.TopActorEntry;
import com.aidentx.core.audit_management.model.dto.analytics.TopActorProjection;
import com.aidentx.core.audit_management.repository.AuditLogRepository;
import com.aidentx.core.audit_management.service.AnalyticsTopActorsService;
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
 * Default implementation of {@link AnalyticsTopActorsService}.
 *
 * <p>When {@code rangeAll} is true, {@link Instant#EPOCH} and end-of-today
 * are used as query bounds covering the full tenant history.
 */
@Service
public class AnalyticsTopActorsServiceImpl implements AnalyticsTopActorsService {

    private static final Logger logger =
            LoggerFactory.getLogger(AnalyticsTopActorsServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AnalyticsTopActorsServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<TopActorEntry> computeTopActors(
            String tenantId, Instant dateFrom, Instant dateTo, Integer limit, boolean rangeAll) {

        Instant resolvedFrom = rangeAll ? Instant.EPOCH : resolveWindowStart(dateFrom, dateTo);
        Instant resolvedTo   = rangeAll ? resolveEndOfToday() : resolveWindowEnd(dateTo);
        int resolvedLimit    = resolveLimit(limit);
        boolean returnAll    = resolvedLimit == AnalyticsTopActorsConstants.LIMIT_ALL;

        logger.info(
                AnalyticsTopActorsConstants.LOG_COMPUTATION_STARTED,
                AnalyticsTopActorsConstants.LOG_KEY_TENANT_ID, tenantId,
                AnalyticsTopActorsConstants.LOG_KEY_DATE_FROM, resolvedFrom,
                AnalyticsTopActorsConstants.LOG_KEY_DATE_TO, resolvedTo,
                AnalyticsTopActorsConstants.LOG_KEY_RETURN_ALL, returnAll);

        List<TopActorProjection> rawRows =
                fetchActors(tenantId, resolvedFrom, resolvedTo, resolvedLimit, returnAll);

        List<TopActorEntry> result = mapToEntries(rawRows);

        logger.info(
                AnalyticsTopActorsConstants.LOG_COMPUTATION_COMPLETE,
                AnalyticsTopActorsConstants.LOG_KEY_RESULT_COUNT, result.size());

        return result;
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return AnalyticsTopActorsConstants.DEFAULT_LIMIT;
        }
        return limit;
    }

    private List<TopActorProjection> fetchActors(
            String tenantId,
            Instant resolvedFrom,
            Instant resolvedTo,
            int resolvedLimit,
            boolean returnAll) {

        if (returnAll) {
            return auditLogRepository.findTopActors(
                    tenantId, resolvedFrom, resolvedTo,
                    AnalyticsTopActorsConstants.STATUS_FAILURE);
        }

        Pageable pageable = PageRequest.of(0, resolvedLimit);
        return auditLogRepository.findTopActorsTopN(
                tenantId, resolvedFrom, resolvedTo,
                AnalyticsTopActorsConstants.STATUS_FAILURE, pageable);
    }

    private List<TopActorEntry> mapToEntries(List<TopActorProjection> rawRows) {
        return rawRows.stream()
                .map(row -> new TopActorEntry(
                        row.getActorName(),
                        row.getActorType(),
                        row.getTotalEvents(),
                        row.getFailureCount(),
                        computeRiskBadge(row.getWeightedRiskSum(), row.getTotalEvents())))
                .collect(Collectors.toList());
    }

    /**
     * Derives a risk badge from the weighted average of event risk levels.
     *
     * <pre>
     *   weightedAvg >= 2.5  -- "high"
     *   weightedAvg >= 1.5  -- "medium"
     *   otherwise           -- "low"
     * </pre>
     *
     * @param weightedRiskSum sum of per-event risk weights; >= 0
     * @param totalEvents     total events for this actor; >= 0
     * @return one of "low", "medium", "high"
     */
    private String computeRiskBadge(long weightedRiskSum, long totalEvents) {
        if (totalEvents == 0L) {
            return AnalyticsTopActorsConstants.RISK_BADGE_LOW;
        }

        double weightedAverage = weightedRiskSum / (double) totalEvents;

        if (weightedAverage >= AnalyticsTopActorsConstants.RISK_BADGE_HIGH_THRESHOLD) {
            return AnalyticsTopActorsConstants.RISK_BADGE_HIGH;
        }

        if (weightedAverage >= AnalyticsTopActorsConstants.RISK_BADGE_MEDIUM_THRESHOLD) {
            return AnalyticsTopActorsConstants.RISK_BADGE_MEDIUM;
        }

        return AnalyticsTopActorsConstants.RISK_BADGE_LOW;
    }

    private Instant resolveWindowStart(Instant dateFrom, Instant dateTo) {
        if (dateFrom != null) {
            return dateFrom.truncatedTo(ChronoUnit.DAYS);
        }

        Instant anchor = (dateTo != null) ? dateTo : Instant.now();
        return anchor
                .truncatedTo(ChronoUnit.DAYS)
                .minus(AnalyticsEventVolumeConstants.DEFAULT_WINDOW_DAYS, ChronoUnit.DAYS);
    }

    private Instant resolveWindowEnd(Instant dateTo) {
        LocalDate endDate = (dateTo != null)
                ? dateTo.atZone(ZoneOffset.UTC).toLocalDate()
                : LocalDate.now(ZoneOffset.UTC);

        return endDate
                .atTime(23, 59, 59, 999_999_999)
                .toInstant(ZoneOffset.UTC);
    }

    private Instant resolveEndOfToday() {
        return LocalDate.now(ZoneOffset.UTC)
                .atTime(23, 59, 59, 999_999_999)
                .toInstant(ZoneOffset.UTC);
    }
}