package com.aidentx.access_portal_service.service;

import java.util.UUID;

import com.aidentx.access_portal_service.dto.response.access_request.AccessPortalAnalyticsDTO;

/**
 * Service contract for access portal analytics.
 */
public interface AccessPortalAnalyticsService {

    /**
     * Returns aggregated analytics for the given tenant over a rolling time window.
     *
     * @param tenantId  tenant identifier
     * @param days      number of days to look back (e.g. 30, 45, 60)
     * @return analytics DTO
     */
    AccessPortalAnalyticsDTO getAnalytics(UUID userId, String tenantId, int days);
}