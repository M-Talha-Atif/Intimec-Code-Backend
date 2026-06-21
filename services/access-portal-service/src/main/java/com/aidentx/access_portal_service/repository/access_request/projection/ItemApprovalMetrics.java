package com.aidentx.access_portal_service.repository.access_request.projection;

/**
 * Projection for approval metrics
 */
public interface ItemApprovalMetrics {
    Long getTotalRequests();
    Long getApprovedCount();
    Long getRejectedCount();
}
