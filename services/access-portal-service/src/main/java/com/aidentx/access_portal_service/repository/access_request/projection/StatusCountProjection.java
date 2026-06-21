package com.aidentx.access_portal_service.repository.access_request.projection;

import com.aidentx.access_portal_service.entity.access_request.RequestStatus;

/**
 * Projection for status-grouped count queries.
 */
public interface StatusCountProjection {
    RequestStatus getStatus();
    long getCount();
}