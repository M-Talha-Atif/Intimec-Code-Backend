package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.ErrorCategoryEntriesResponse;

/**
 * Service contract for fetching the log entries that belong to a specific
 * category value in the Error Category Distribution chart.
 *
 * <p>The chart groups log documents by their {@code category} field and
 * displays the top-N categories by count. When a user clicks a bar this
 * service fetches the actual documents that were counted in that bar,
 * paginated and sorted newest first.
 *
 * <p>No time filter is applied by default because the Error Category
 * Distribution chart itself covers full history (range=all default).
 * The caller may pass {@code hours} to narrow results to a recent window
 * matching a windowed chart view.
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsErrorCategoryEntriesService {

    /**
     * Fetches paginated log entries for a specific category value.
     *
     * <p>Example: category="Security-Auth", page=0, size=20
     * returns the first 20 log entries where category = "Security-Auth"
     * across all time, sorted newest first.
     *
     * @param category exact category value to filter on; must not be blank
     * @param hours    optional look-back window in hours; 0 means no time filter
     * @param page     zero-based page number
     * @param size     number of entries per page
     * @return populated response with matched entries and pagination metadata
     */
    ErrorCategoryEntriesResponse fetchEntriesForCategory(
            String category,
            int hours,
            int page,
            int size);
}