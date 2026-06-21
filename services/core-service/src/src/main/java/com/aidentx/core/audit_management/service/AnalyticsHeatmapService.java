package com.aidentx.core.audit_management.service;

import com.aidentx.core.audit_management.model.dto.analytics.HeatmapEntry;

import java.time.Instant;
import java.util.List;

/**
 * Service contract for computing the change impact heatmap.
 *
 * <p>Returns a list of (objectType, eventName, count) entries representing
 * non-zero cells in the heatmap matrix. Zero cells are omitted. The list is
 * ordered by count descending so the hottest cells appear first.
 *
 * <p>Limit behaviour:
 * <ul>
 *   <li>null -- defaults to top
 *       {@link com.aidentx.core.audit_management.config.constants.analytics.AnalyticsHeatmapConstants#DEFAULT_LIMIT}
 *       pairs (50).</li>
 *   <li>Positive integer -- returns that many (objectType, eventName) pairs.</li>
 *   <li>{@link com.aidentx.core.audit_management.config.constants.analytics.AnalyticsHeatmapConstants#LIMIT_ALL}
 *       (0) -- returns all distinct pairs with no truncation.</li>
 * </ul>
 *
 * <p>Range behaviour:
 * <ul>
 *   <li>{@code rangeAll=false} -- applies date window (default 30 days).</li>
 *   <li>{@code rangeAll=true}  -- disables date filtering; covers full history.</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
public interface AnalyticsHeatmapService {

    /**
     * Returns the change impact heatmap as a list of non-zero matrix cells.
     *
     * <p>Example: tenant "acme", last 30 days, default limit=50:
     * <pre>
     * [
     *   { "objectType": "Role",       "eventName": "assign",    "count": 142 },
     *   { "objectType": "User",       "eventName": "assign",    "count": 97  },
     *   { "objectType": "Role",       "eventName": "provision", "count": 38  },
     *   { "objectType": "Permission", "eventName": "approve",   "count": 21  }
     * ]
     * </pre>
     *
     * <p>Frontend matrix from the above (zeros filled by the frontend):
     * <pre>
     *              assign   provision   approve
     *   Role         142       38          0
     *   User          97        0          0
     *   Permission     0        0         21
     * </pre>
     *
     * @param tenantId non-blank tenant identifier from the caller's JWT
     * @param dateFrom optional window start (inclusive); ignored when rangeAll is true
     * @param dateTo   optional window end (inclusive); ignored when rangeAll is true
     * @param limit    null = top 50, 0 = all pairs, positive integer = top N pairs
     * @param rangeAll when true, disables date filtering and covers full history
     * @return list of non-zero heatmap cells ordered by count descending
     */
    List<HeatmapEntry> computeHeatmap(
            String tenantId, Instant dateFrom, Instant dateTo, Integer limit, boolean rangeAll);
}