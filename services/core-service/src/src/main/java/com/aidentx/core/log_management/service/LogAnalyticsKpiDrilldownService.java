package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.KpiDrilldownResponse;

/**
 * Service contract for KPI drilldown queries.
 *
 * <p>Each method returns the paginated log entries that were counted to
 * produce a specific KPI card value. The caller selects the drilldown
 * type via {@code type} and controls pagination via {@code page}/{@code size}.
 *
 * <p>Supported types and their data sources:
 * <ul>
 *   <li>{@code total_errors}   -- ERROR+WARN documents, time window based on range</li>
 *   <li>{@code critical}       -- ERROR documents only, time window based on range</li>
 *   <li>{@code slow_responses} -- documents where executionTime > computed average, time window based on range</li>
 *   <li>{@code recurring}      -- documents whose message appears 3+ times, time window based on range</li>
 * </ul>
 *
 * <p>The range parameter applies consistent time filtering across all drilldown types:
 * <ul>
 *   <li>{@code 24h} - Last 24 hours</li>
 *   <li>{@code 30d} - Last 30 days</li>
 *   <li>{@code 60d} - Last 60 days</li>
 *   <li>{@code 90d} - Last 90 days</li>
 *   <li>{@code all} - No time filter (full history)</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsKpiDrilldownService {

    /**
     * @param type  drilldown type string
     * @param page  zero-based page number
     * @param size  number of entries per page
     * @param range date range filter: "24h", "30d", "60d", "90d", or "all"
     * @return populated {@link KpiDrilldownResponse}
     */
    KpiDrilldownResponse fetchDrilldown(String type, int page, int size, String range);
}