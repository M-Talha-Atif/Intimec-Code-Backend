package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.HeatmapResponse;

/**
 * Service contract for computing the log error heatmap.
 *
 * <p>Returns a sparse list of (source, hour, count) cells for the top-N
 * sources within the requested window. Hour integers are in the caller-supplied
 * timezone so they match the user's local clock.
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsHeatmapService {

    /**
     * Computes the hour x source heatmap for log events.
     *
     * <p>Example: hours=24, limit=2, timezone=Asia/Karachi:
     * <pre>
     * {
     *   "hours": 24,
     *   "limit": 2,
     *   "sources": ["GatewayPermissionFilter", "JwtUtil"],
     *   "data": [
     *     { "source": "GatewayPermissionFilter", "hour": 15, "count": 49 },
     *     { "source": "GatewayPermissionFilter", "hour": 17, "count": 68 },
     *     { "source": "JwtUtil",                 "hour": 17, "count": 12 }
     *   ]
     * }
     * </pre>
     *
     * @param hours    look-back window in hours
     * @param limit    number of top sources to include as heatmap rows
     * @param rangeAll when true, covers full log history with no time filter
     * @param timezone IANA timezone string for hour conversion;
     *                 null or blank defaults to UTC
     * @return populated {@link HeatmapResponse}
     */
    HeatmapResponse computeHeatmap(int hours, int limit, boolean rangeAll, String timezone);
}