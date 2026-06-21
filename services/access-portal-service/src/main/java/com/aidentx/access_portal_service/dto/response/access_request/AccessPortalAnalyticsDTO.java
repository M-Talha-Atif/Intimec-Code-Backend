package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Analytics response DTO for the Access Portal dashboard.
 * Contains aggregated request metrics and top requested items.
 */
@Data
@Builder
public class AccessPortalAnalyticsDTO {

    // ─── Section 1: Key Metrics ───────────────────────────────────

    /** Total number of access requests in the time window */
    private int totalRequests;

    /** Approval rate as a percentage (0.0 for now — no logic) */
    private double approvalRate;

    /** Average processing time in hours (0 for now — no logic) */
    private int avgProcessingTimeHours;

    /** Average risk score (1.0 for now — no logic) */
    private double avgRiskScore;

    // ─── Section 2: Status Breakdown ─────────────────────────────

    private int pendingCount;
    private double pendingPercentage;

    private int approvedCount;
    private double approvedPercentage;

    private int rejectedCount;
    private double rejectedPercentage;

    // ─── Section 3: Top Requested Items ──────────────────────────

    private List<TopRequestedItemDTO> mostRequestedItems;

    // ─── Filter metadata (echo back what was applied) ────────────

    /** Number of days the analytics window covers */
    private int periodDays;

    // ─── Nested DTO ───────────────────────────────────────────────

    @Data
    @Builder
    public static class TopRequestedItemDTO {
        private UUID   itemId;
        private String itemName;
        private long   requestCount;
    }
}