package com.aidentx.access_portal_service.service.impl;

import com.aidentx.access_portal_service.config.AccessPortalConstants;
import com.aidentx.access_portal_service.dto.response.access_request.AccessPortalAnalyticsDTO;
import com.aidentx.access_portal_service.entity.access_request.RequestStatus;
import com.aidentx.access_portal_service.repository.access_request.AccessRequestItemRepository;
import com.aidentx.access_portal_service.repository.access_request.AccessRequestRepository;
import com.aidentx.access_portal_service.repository.access_request.projection.StatusCountProjection;
import com.aidentx.access_portal_service.repository.access_request.projection.TopRequestedItemProjection;
import com.aidentx.access_portal_service.service.AccessPortalAnalyticsService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Analytics service implementation.
 * <p>
 * Uses exactly 2 DB queries per request:
 * 1. GROUP BY status  → derives total + all status counts + percentages
 * 2. TOP N items      → most requested items aggregation
 * <p>
 * All other metrics (approvalRate, avgProcessingTime, avgRiskScore)
 * return placeholder values until dedicated logic is implemented.
 */
@Service
public class AccessPortalAnalyticsServiceImpl implements AccessPortalAnalyticsService {

    private final AccessRequestRepository accessRequestRepository;
    private final AccessRequestItemRepository accessRequestItemRepository;
    private final IUnifiedLogger logger;

    public AccessPortalAnalyticsServiceImpl(
            AccessRequestRepository accessRequestRepository,
            AccessRequestItemRepository accessRequestItemRepository,
            UnifiedLoggerFactory loggerFactory) {
        this.accessRequestRepository = accessRequestRepository;
        this.accessRequestItemRepository = accessRequestItemRepository;
        this.logger = loggerFactory.getLogger(
                getClass(),
                "ANALYTICS"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AccessPortalAnalyticsDTO getAnalytics(UUID userId, String tenantId, int days) {

        // ── Build time window ────────────────────────────────────────
        OffsetDateTime since = OffsetDateTime.now().minusDays(days);

        logger.info()
                .message("Computing analytics")
                .detail("tenantId", tenantId)
                .detail("days", String.valueOf(days))
                .detail("since", since.toString())
                .log();

        // ── Query 1: Status breakdown (single GROUP BY) ──────────────
        List<StatusCountProjection> statusCounts =
                accessRequestRepository.countByStatusGrouped(tenantId, userId, since);

        // Derive all counts from the single query result
        int totalRequests = 0;
        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;

        for (StatusCountProjection row : statusCounts) {
            int count = (int) row.getCount();
            totalRequests += count;

            RequestStatus status = row.getStatus();

            // SUBMITTED maps to pending (awaiting first approval)
            if (status == RequestStatus.SUBMITTED ||
                    status == RequestStatus.PENDING_APPROVAL) {
                pendingCount += count;

            } else if (status == RequestStatus.APPROVED ||
                    status == RequestStatus.PROVISIONED ||
                    status == RequestStatus.PARTIALLY_PROVISIONED) {
                // PARTIALLY_APPROVED means at least one approval occurred
                approvedCount += count;

            } else if (status == RequestStatus.REJECTED) {
                rejectedCount += count;
            }
            // WITHDRAWN / DRAFT — counted in total but not in the three buckets
        }

        // Compute percentages (guard against zero total)
        double pendingPct = totalRequests > 0 ? round((double) pendingCount / totalRequests * 100) : 0.0;
        double approvedPct = totalRequests > 0 ? round((double) approvedCount / totalRequests * 100) : 0.0;
        double rejectedPct = totalRequests > 0 ? round((double) rejectedCount / totalRequests * 100) : 0.0;

        // ── Query 2: Top requested items ─────────────────────────────
        List<TopRequestedItemProjection> topItems =
                accessRequestItemRepository.findTopRequestedItemsForUser(
                        tenantId,
                        userId,
                        since,
                        AccessPortalConstants.ANALYTICS_TOP_ITEMS_LIMIT
                );

        List<AccessPortalAnalyticsDTO.TopRequestedItemDTO> mappedItems = topItems.stream()
                .map(p -> AccessPortalAnalyticsDTO.TopRequestedItemDTO.builder()
                        .itemId(p.getItemId())
                        .itemName(p.getItemName())
                        .requestCount(p.getRequestCount())
                        .build())
                .toList();

        // Compute approval rate based on completed requests only
        int completedRequests = approvedCount + rejectedCount;

        double approvalRate = completedRequests > 0
                ? round((double) approvedCount / completedRequests * 100)
                : 0.0;

        logger.info()
                .message("Analytics computed")
                .detail("tenantId", tenantId)
                .detail("userId", userId.toString())
                .detail("totalRequests", String.valueOf(totalRequests))
                .detail("pendingCount", String.valueOf(pendingCount))
                .detail("approvedCount", String.valueOf(approvedCount))
                .detail("rejectedCount", String.valueOf(rejectedCount))
                .detail("topItemsCount", String.valueOf(mappedItems.size()))
                .log();

        // ── Build and return ─────────────────────────────────────────
        return AccessPortalAnalyticsDTO.builder()
                // Section 1 — key metrics
                .totalRequests(totalRequests)
                .approvalRate(approvalRate)        // approval rate
                .avgProcessingTimeHours(AccessPortalConstants.ANALYTICS_DEFAULT_AVG_PROCESSING) // 0 placeholder
                .avgRiskScore(AccessPortalConstants.ANALYTICS_DEFAULT_AVG_RISK_SCORE)       // 1.0 placeholder
                // Section 2 — status breakdown
                .pendingCount(pendingCount)
                .pendingPercentage(pendingPct)
                .approvedCount(approvedCount)
                .approvedPercentage(approvedPct)
                .rejectedCount(rejectedCount)
                .rejectedPercentage(rejectedPct)
                // Section 3 — top items
                .mostRequestedItems(mappedItems)
                // Meta
                .periodDays(days)
                .build();
    }

    /**
     * Rounds a double to 2 decimal places
     */
    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}