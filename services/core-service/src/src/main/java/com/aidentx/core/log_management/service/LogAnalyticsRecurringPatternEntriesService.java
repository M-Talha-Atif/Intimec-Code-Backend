package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.RecurringPatternEntriesResponse;

/**
 * Service contract for fetching the log entries that share a specific
 * repeated message string.
 *
 * <p>The recurring patterns list shows aggregated statistics per message.
 * When a user clicks a pattern card this service fetches the actual
 * individual log entries that were counted in that pattern so the user
 * can inspect timestamps, sources, levels, and correlation IDs.
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsRecurringPatternEntriesService {

    /**
     * Fetches paginated log entries matching the exact message string.
     *
     * <p>Example: message="Permission denied for user", page=0, size=20
     * returns the 20 most recent entries where message exactly matches
     * that string.
     *
     * @param message exact message text from a RecurringPatternEntry
     * @param hours   look-back window in hours; 0 means all history
     * @param page    zero-based page number
     * @param size    entries per page
     * @return populated response with matched entries and pagination
     */
    RecurringPatternEntriesResponse fetchEntriesForMessage(
            String message,
            int hours,
            int page,
            int size);
}