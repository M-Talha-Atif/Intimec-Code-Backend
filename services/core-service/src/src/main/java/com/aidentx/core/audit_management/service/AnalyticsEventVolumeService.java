package com.aidentx.core.audit_management.service;

import com.aidentx.core.audit_management.model.dto.analytics.EventVolumeEntry;

import java.time.Instant;
import java.util.List;

/**
 * Service contract for computing the event volume timeline.
 *
 * <p>The timeline is an ordered list of daily entries covering the requested
 * date window. Every day in the window is always present in the response,
 * even if it had zero events, so the frontend area chart renders a continuous
 * x-axis without gaps.
 *
 * <p>Date window resolution:
 * <ul>
 *   <li>Both null -- defaults to the last
 *       {@link com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventVolumeConstants#DEFAULT_WINDOW_DAYS}
 *       days up to now.</li>
 *   <li>Only dateFrom -- window runs from dateFrom to now.</li>
 *   <li>Only dateTo -- window runs from (dateTo minus DEFAULT_WINDOW_DAYS) to dateTo.</li>
 *   <li>Both present -- exact bounded window.</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
public interface AnalyticsEventVolumeService {

    /**
     * Returns the daily event volume timeline broken down by risk level.
     *
     * <p>Example: calling with tenantId="acme", dateFrom=Mar 01, dateTo=Mar 03
     * and data only existing on Mar 01 and Mar 03 produces:
     * <pre>
     * [
     *   { "date": "Mar 01", "low": 80, "medium": 30, "high": 10 },
     *   { "date": "Mar 02", "low": 0,  "medium": 0,  "high": 0  },
     *   { "date": "Mar 03", "low": 55, "medium": 18, "high": 7  }
     * ]
     * </pre>
     *
     * @param tenantId non-blank tenant identifier from the caller's JWT
     * @param dateFrom optional window start (inclusive); null triggers default resolution
     * @param dateTo   optional window end (inclusive); null triggers default resolution
     * @param rangeAll if true, ignore dateFrom and dateTo              (ignored when rangeAll=true)
     * @return ordered list of daily entries, one per calendar day in the window
     */
    List<EventVolumeEntry> computeEventVolume(
            String tenantId, Instant dateFrom, Instant dateTo, boolean rangeAll);
}