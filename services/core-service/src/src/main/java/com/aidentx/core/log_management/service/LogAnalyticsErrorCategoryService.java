package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.ErrorCategoryEntry;

import java.util.List;

/**
 * Service contract for computing the error category distribution.
 *
 * <p>Returns a ranked list of categories ordered by ERROR+WARN count
 * descending. Categories are the actual values stored in the
 * {@code category} field of log documents -- no hardcoded mapping applied.
 *
 * <p>Time window behaviour:
 * <ul>
 *   <li>{@code rangeAll=true} (default) -- no time filter, covers full history.</li>
 *   <li>{@code rangeAll=false} -- applies {@code @timestamp >= now - (hours * MS_PER_HOUR)}.</li>
 * </ul>
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsErrorCategoryService {

    /**
     * Returns the top-N error categories ranked by event count.
     *
     * <p>Example: limit=3, rangeAll=true:
     * <pre>
     * [
     *   { "category": "Security-Auth",            "count": 312 },
     *   { "category": "SECURITY",                 "count": 198 },
     *   { "category": "NOTIFICATIONS_MANAGEMENT", "count": 87  }
     * ]
     * </pre>
     *
     * <p>Example: limit=3, rangeAll=false, hours=24:
     * <pre>
     * [
     *   { "category": "Security-Auth", "count": 42 },
     *   { "category": "SECURITY",      "count": 18 },
     *   { "category": "AUDIT",         "count": 5  }
     * ]
     * </pre>
     *
     * @param limit    number of top categories to return
     * @param hours    look-back window in hours; only used when rangeAll is false
     * @param rangeAll when true, covers full log history with no time filter
     * @return ordered list of category entries, rank 1 (highest count) first
     */
    List<ErrorCategoryEntry> computeErrorCategoryDistribution(
            int limit, int hours, boolean rangeAll);
}