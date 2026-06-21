package com.aidentx.access_portal_service.repository.access_request.projection;

import java.util.UUID;

/**
 * Projection for top-requested item aggregation queries.
 */
public interface TopRequestedItemProjection {
    UUID   getItemId();
    String getItemName();
    long   getRequestCount();
}