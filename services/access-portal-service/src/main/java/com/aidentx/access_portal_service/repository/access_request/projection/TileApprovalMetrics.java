package com.aidentx.access_portal_service.repository.access_request.projection;

/**
 * Projection for tile-level approval metrics
 */
public interface TileApprovalMetrics {
    Long getApprovedCount();
    Long getRejectedCount();
}
