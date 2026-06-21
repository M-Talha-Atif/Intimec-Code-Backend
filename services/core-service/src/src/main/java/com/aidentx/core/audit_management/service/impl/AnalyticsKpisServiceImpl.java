package com.aidentx.core.audit_management.service.impl;

import com.aidentx.core.audit_management.config.constants.analytics.AnalyticsKpisConstants;
import com.aidentx.core.audit_management.model.dto.analytics.AnalyticsKpisResponse;
import com.aidentx.core.audit_management.model.entity.AuditLog;
import com.aidentx.core.audit_management.repository.AuditLogRepository;
import com.aidentx.core.audit_management.service.AnalyticsKpisService;
import com.aidentx.core.audit_management.specification.AuditLogSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Default implementation of {@link AnalyticsKpisService}.
 *
 * <p>Three COUNT queries are issued per call in sequence:
 * <ol>
 *   <li>Base spec: tenant + optional date window -- total event count</li>
 *   <li>Base spec + risk = 'high' -- high risk count</li>
 *   <li>Base spec + status = 'failure' -- failed operations count</li>
 * </ol>
 *
 * <p>When {@code rangeAll} is true the base spec contains only the tenant
 * predicate and no timestamp bounds, returning counts across the full
 * tenant history.
 * @author aidentx
 * @since 1.0
 */
@Service
public class AnalyticsKpisServiceImpl implements AnalyticsKpisService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsKpisServiceImpl.class);

    private final AuditLogRepository auditLogRepository;

    public AnalyticsKpisServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>When {@code rangeAll} is true, {@code dateFrom} and {@code dateTo}
     * are both treated as null regardless of any values passed, so the base
     * specification carries only the tenant predicate.
     */
    @Override
    public AnalyticsKpisResponse computeKpis(
            String tenantId, Instant dateFrom, Instant dateTo, boolean rangeAll) {

        Instant effectiveFrom = rangeAll ? null : dateFrom;
        Instant effectiveTo   = rangeAll ? null : dateTo;

        logger.info(
                AnalyticsKpisConstants.LOG_KPI_COMPUTATION_STARTED,
                AnalyticsKpisConstants.LOG_KEY_TENANT_ID, tenantId,
                AnalyticsKpisConstants.LOG_KEY_DATE_FROM, effectiveFrom,
                AnalyticsKpisConstants.LOG_KEY_DATE_TO, effectiveTo);

        Specification<AuditLog> baseSpec =
                buildBaseSpecification(tenantId, effectiveFrom, effectiveTo);

        long totalEvents = auditLogRepository.count(baseSpec);

        Specification<AuditLog> highRiskSpec = baseSpec
                .and(AuditLogSpecification.hasRisk(
                        AnalyticsKpisConstants.RISK_LEVEL_HIGH_ONLY));

        long highRiskCount = auditLogRepository.count(highRiskSpec);

        Specification<AuditLog> failedSpec = baseSpec
                .and(AuditLogSpecification.hasStatus(
                        AnalyticsKpisConstants.STATUS_FAILURE));

        long failedOperationsCount = auditLogRepository.count(failedSpec);

        double failedOperationsPercentage = computeFailurePercentage(
                totalEvents, failedOperationsCount);

        logger.info(
                AnalyticsKpisConstants.LOG_KPI_COMPUTATION_COMPLETE,
                AnalyticsKpisConstants.LOG_KEY_TOTAL_EVENTS, totalEvents,
                AnalyticsKpisConstants.LOG_KEY_HIGH_RISK_COUNT, highRiskCount,
                AnalyticsKpisConstants.LOG_KEY_FAILED_COUNT, failedOperationsCount,
                AnalyticsKpisConstants.LOG_KEY_FAILED_PERCENTAGE, failedOperationsPercentage);

        return new AnalyticsKpisResponse(
                totalEvents,
                highRiskCount,
                failedOperationsCount,
                failedOperationsPercentage);
    }

    /**
     * Assembles the base specification shared by all three COUNT queries.
     * Both date predicates are no-ops when their argument is null, so passing
     * null for either produces an open-ended window on that side.
     * Passing null for both (rangeAll path) leaves only the tenant predicate.
     *
     * @param tenantId non-blank tenant ID
     * @param dateFrom optional range start; null means no lower bound
     * @param dateTo   optional range end; null means no upper bound
     * @return combined specification
     */
    private Specification<AuditLog> buildBaseSpecification(
            String tenantId, Instant dateFrom, Instant dateTo) {
        return AuditLogSpecification.hasTenantId(tenantId)
                .and(AuditLogSpecification.hasTimestampFrom(dateFrom))
                .and(AuditLogSpecification.hasTimestampTo(dateTo));
    }

    /**
     * Computes the failure percentage rounded to one decimal place using
     * {@link BigDecimal} to guarantee consistent rounding across JVM platforms.
     *
     * @param totalEvents           total event count; >= 0
     * @param failedOperationsCount events with status failure; >= 0
     * @return failure percentage rounded to 1 d.p., or 0.0 when totalEvents is zero
     */
    private double computeFailurePercentage(long totalEvents, long failedOperationsCount) {
        if (totalEvents == AnalyticsKpisConstants.ZERO_COUNT) {
            return 0.0;
        }

        double rawPercentage =
                ((double) failedOperationsCount / totalEvents)
                        * AnalyticsKpisConstants.PERCENTAGE_MULTIPLIER;

        return BigDecimal.valueOf(rawPercentage)
                .setScale(
                        AnalyticsKpisConstants.PERCENTAGE_DECIMAL_SCALE,
                        RoundingMode.HALF_UP)
                .doubleValue();
    }
}