package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.ErrorVolumeResponse;

/**
 * Service contract for computing the error volume by source timeline.
 *
 * <p>Returns one entry per hour in the requested window, with dynamic source
 * columns representing the top-N sources ranked by total error count.
 *
 * <p>Window behaviour:
 * <ul>
 *   <li>{@code rangeAll=false} -- covers the last {@code hours} hours from now.</li>
 *   <li>{@code rangeAll=true}  -- covers all historical data, bucketed by
 *       hour-of-day (0-23). Each hour label aggregates that hour across
 *       all days in history.</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsErrorVolumeService {

    /**
     * Computes the error volume timeline broken down by top-N sources.
     *
     * <p>Example: hours=24, limit=3, rangeAll=false:
     * <pre>
     * {
     *   "hours": 24,
     *   "limit": 3,
     *   "sources": ["JwtAuthenticationFilter", "GatewayPermissionFilter", "UserNotificationService"],
     *   "timeline": [
     *     { "hour": "00:00", "JwtAuthenticationFilter": 0, "GatewayPermissionFilter": 2, "UserNotificationService": 1 },
     *     { "hour": "01:00", "JwtAuthenticationFilter": 3, "GatewayPermissionFilter": 0, "UserNotificationService": 0 },
     *     ...
     *   ]
     * }
     * </pre>
     *
     * @param hours    number of hours to look back; defaults to
     *                 {@link com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorVolumeConstants#DEFAULT_HOURS}
     * @param limit    number of top sources to include; defaults to
     *                 {@link com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorVolumeConstants#DEFAULT_SOURCE_LIMIT}
     * @param rangeAll when true, covers full history bucketed by hour-of-day
     * @param timezone timezone to use for bucketing hours (e.g. "UTC", "America/New_York"); defaults to UTC when omitted or invalid
     * @return populated {@link ErrorVolumeResponse}
     */
    ErrorVolumeResponse computeErrorVolume(int hours, int limit, boolean rangeAll, String timezone);
}