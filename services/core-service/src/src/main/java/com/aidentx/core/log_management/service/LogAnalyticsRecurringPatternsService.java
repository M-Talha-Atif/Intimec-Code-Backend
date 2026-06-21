package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.RecurringPatternEntry;
import com.aidentx.core.log_management.model.dto.analytics.RecurringPatternsResponse;

import java.util.List;

/**
 * Service contract for computing the recurring error patterns list.
 *
 * <p>Returns the top-N message strings that appear at least
 * {@link com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsRecurringPatternsConstants#MIN_OCCURRENCES}
 * times, with full detail per pattern: occurrence count, first/last seen,
 * dominant source, and severity.
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsRecurringPatternsService {

    /**
     * Computes the paginated recurring patterns list.
     *
     * <p>OpenSearch terms aggregation does not support cursor-based pagination.
     * The implementation fetches enough buckets to cover the requested page
     * (up to page * size + size) and slices the result in memory.
     *
     * <p>Example: page=1, size=5 -- fetches top 10 from OpenSearch, returns items 5-9.
     *
     * @param page  zero-based page number
     * @param size  items per page
     * @param hours look-back window in hours; 0 means no time filter
     * @return paginated response with patterns and pagination metadata
     */
    RecurringPatternsResponse computeRecurringPatterns(int page, int size, int hours);
}