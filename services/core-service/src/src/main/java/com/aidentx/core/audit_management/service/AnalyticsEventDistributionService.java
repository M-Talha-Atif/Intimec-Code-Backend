package com.aidentx.core.audit_management.service;

import com.aidentx.core.audit_management.model.dto.analytics.EventDistributionEntry;

import java.time.Instant;
import java.util.List;

/**
 * Service contract for computing the event type distribution.
 *
 * <p>Returns a ranked list of event names and their occurrence counts within
 * the requested window. The list is always ordered by count descending.
 *
 * <p>Limit behaviour:
 * <ul>
 *   <li>null -- defaults to
 *       {@link com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventDistributionConstants#DEFAULT_LIMIT}
 *       (top 10).</li>
 *   <li>Positive integer (e.g. 5, 10, 25) -- returns that many event types.</li>
 *   <li>{@link com.aidentx.core.audit_management.config.constants.analytics.AnalyticsEventDistributionConstants#LIMIT_ALL}
 *       (0) -- returns every distinct event type with no truncation.</li>
 * </ul>
 *
 * <p>Date window resolution follows the same rules established in
 * {@link AnalyticsEventVolumeService}: both null defaults to the last 30 days,
 * one-sided windows are open-ended on the absent side.
 *
 * @author aidentx
 * @since 1.0
 */
public interface AnalyticsEventDistributionService {

    /**
     * Returns the event type distribution ranked by occurrence count.
     *
     * <p>Example: tenant "acme" with default limit=10 and data across 15
     * distinct event types returns only the 10 most frequent:
     * <pre>
     * [
     *   { "eventName": "provision", "count": 843 },
     *   { "eventName": "assign",    "count": 612 },
     *   { "eventName": "approve",   "count": 390 },
     *   ...
     * ]
     * </pre>
     *
     * @param tenantId non-blank tenant identifier from the caller's JWT
     * @param dateFrom optional window start (inclusive); null triggers default resolution
     * @param dateTo   optional window end (inclusive); null triggers default resolution
     * @param limit    null defaults to top 10; 0 returns all; any positive integer
     * @param rangeAll if true, ignore dateFrom and dateTo              (ignored when rangeAll=true)
     *                 returns that many results
     * @return ordered list of event distribution entries
     */
    List<EventDistributionEntry> computeEventDistribution(
            String tenantId, Instant dateFrom, Instant dateTo, Integer limit, boolean rangeAll);
}