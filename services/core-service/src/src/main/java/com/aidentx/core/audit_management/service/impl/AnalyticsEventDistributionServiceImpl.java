package com.aidentx.core.audit_management.service.impl;

import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventDistributionConstants;
import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventVolumeConstants;
import com.aidentx.core.audit_management.model.dto.analytics.EventDistributionEntry;
import com.aidentx.core.audit_management.model.dto.analytics.EventDistributionProjection;
import com.aidentx.core.audit_management.repository.AuditLogRepository;
import com.aidentx.core.audit_management.service.AnalyticsEventDistributionService;
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
 * Default implementation of {@link AnalyticsEventDistributionService}.
 *
 * <p>When {@code rangeAll} is true, {@link Instant#EPOCH} and end-of-today
 * are used as query bounds, covering the full tenant history without requiring
 * a separate unbounded repository method.
 */
@Service
public class AnalyticsEventDistributionServiceImpl implements AnalyticsEventDistributionService {

    private static final Logger logger =
            LoggerFactory.getLogger(AnalyticsEventDistributionServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AnalyticsEventDistributionServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EventDistributionEntry> computeEventDistribution(
            String tenantId, Instant dateFrom, Instant dateTo, Integer limit, boolean rangeAll) {

        Instant resolvedFrom = rangeAll ? Instant.EPOCH : resolveWindowStart(dateFrom, dateTo);
        Instant resolvedTo   = rangeAll ? resolveEndOfToday() : resolveWindowEnd(dateTo);
        int resolvedLimit    = resolveLimit(limit);
        boolean returnAll    = resolvedLimit == AnalyticsEventDistributionConstants.LIMIT_ALL;

        logger.info(
                AnalyticsEventDistributionConstants.LOG_COMPUTATION_STARTED,
                AnalyticsEventDistributionConstants.LOG_KEY_TENANT_ID, tenantId,
                AnalyticsEventDistributionConstants.LOG_KEY_DATE_FROM, resolvedFrom,
                AnalyticsEventDistributionConstants.LOG_KEY_DATE_TO, resolvedTo,
                AnalyticsEventDistributionConstants.LOG_KEY_RETURN_ALL, returnAll);

        List<EventDistributionProjection> rawRows =
                fetchDistribution(tenantId, resolvedFrom, resolvedTo, resolvedLimit, returnAll);

        List<EventDistributionEntry> result = mapToEntries(rawRows);

        logger.info(
                AnalyticsEventDistributionConstants.LOG_COMPUTATION_COMPLETE,
                AnalyticsEventDistributionConstants.LOG_KEY_RESULT_COUNT, result.size());

        return result;
    }

    private int resolveLimit(Integer limit) {
        if (limit == null) {
            return AnalyticsEventDistributionConstants.DEFAULT_LIMIT;
        }
        return limit;
    }

    private List<EventDistributionProjection> fetchDistribution(
            String tenantId,
            Instant resolvedFrom,
            Instant resolvedTo,
            int resolvedLimit,
            boolean returnAll) {

        if (returnAll) {
            return auditLogRepository.findEventDistribution(tenantId, resolvedFrom, resolvedTo);
        }

        Pageable pageable = PageRequest.of(0, resolvedLimit);
        return auditLogRepository.findEventDistributionTopN(
                tenantId, resolvedFrom, resolvedTo, pageable);
    }

    private List<EventDistributionEntry> mapToEntries(List<EventDistributionProjection> rawRows) {
        return rawRows.stream()
                .map(row -> new EventDistributionEntry(row.getEventName(), row.getEventCount()))
                .collect(Collectors.toList());
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