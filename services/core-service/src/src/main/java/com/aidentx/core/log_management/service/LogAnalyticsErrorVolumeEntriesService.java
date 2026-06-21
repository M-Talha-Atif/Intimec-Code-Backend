package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.analytics.ErrorVolumeEntriesResponse;

import java.util.List;

/**
 * Service contract for fetching the log entries that make up a specific
 * hour slot in the error volume timeline.
 *
 * <p>The chart renders hour labels in the caller's timezone. When a user
 * clicks a bar the frontend sends the label ("15:00") and the timezone
 * ("Asia/Karachi"). This service resolves that combination to the correct
 * UTC epoch ms window and fetches matching documents from OpenSearch.
 *
 * <p>Why this service exists separately from the main search service:
 * The main search service accepts arbitrary LogSearchRequest filters.
 * This service accepts chart-native parameters (hour label + timezone +
 * window size) and handles the timezone-to-UTC conversion internally so
 * the frontend never has to do date math.
 *
 * @author aidentx
 * @since 1.0
 */
public interface LogAnalyticsErrorVolumeEntriesService {

    /**
     * Fetches paginated log entries for a specific hour slot.
     *
     * <p>Example: hour="15:00", timezone="Asia/Karachi", windowHours=24
     * resolves to UTC range [10:00:00Z, 10:59:59Z] on the date that falls
     * within the last 24 hours window.
     *
     * @param hour         hour label as displayed on the chart e.g. "15:00"
     * @param timezone     IANA timezone string used to render the chart
     *                     e.g. "Asia/Karachi", "Europe/Zurich", "UTC"
     * @param sources      list of source names to filter by; empty means all sources
     * @param windowHours  size of the look-back window in hours; default 24
     * @param page         zero-based page number
     * @param size         number of entries per page
     * @return populated response with resolved UTC bounds and paginated entries
     */
    ErrorVolumeEntriesResponse fetchEntriesForHour(
            String hour,
            String timezone,
            List<String> sources,
            int windowHours,
            int page,
            int size);
}