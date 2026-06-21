package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for {@code GET /api/v1/logs/analytics/error-volume}.
 *
 * <p>The response carries three fields so the frontend does not need to
 * inspect timeline entries to discover which sources are present:
 * <ul>
 *   <li>{@code hours}    -- the effective window that was used</li>
 *   <li>{@code limit}    -- the number of top sources included</li>
 *   <li>{@code sources}  -- ordered list of source names (rank 1 first);
 *       the frontend uses this list to render one Area per source</li>
 *   <li>{@code timeline} -- one entry per hour, 0 to (hours-1)</li>
 * </ul>
 *
 * <p>Example (limit=2, hours=2, window covers 10:00-11:59):
 * <pre>
 * {
 *   "hours": 2,
 *   "limit": 2,
 *   "sources": ["JwtAuthenticationFilter", "GatewayPermissionFilter"],
 *   "timeline": [
 *     { "hour": "10:00", "JwtAuthenticationFilter": 5, "GatewayPermissionFilter": 12 },
 *     { "hour": "11:00", "JwtAuthenticationFilter": 2, "GatewayPermissionFilter": 3  }
 *   ]
 * }
 * </pre>
 */
public class ErrorVolumeResponse {

    /**
     * The effective look-back window in hours that was applied.
     * Matches the {@code hours} request parameter, or the default when omitted.
     */
    @JsonProperty("hours")
    private int hours;

    /**
     * The number of top sources included in the response.
     * Matches the {@code limit} request parameter, or the default when omitted.
     */
    @JsonProperty("limit")
    private int limit;

    /**
     * Ordered list of source names included in the timeline entries.
     * Sources are ordered by total error count descending (rank 1 = most errors).
     * The frontend uses this list to determine which Area components to render
     * and in what order.
     */
    @JsonProperty("sources")
    private List<String> sources;

    /**
     * Ordered list of hourly timeline entries covering the requested window.
     * The list always contains exactly {@code hours} entries, one per clock
     * hour, with gaps filled to 0. Hours are in ascending order (oldest first).
     */
    @JsonProperty("timeline")
    private List<ErrorVolumeEntry> timeline;

    public ErrorVolumeResponse() {}

    public ErrorVolumeResponse(
            int hours,
            int limit,
            List<String> sources,
            List<ErrorVolumeEntry> timeline) {
        this.hours = hours;
        this.limit = limit;
        this.sources = sources;
        this.timeline = timeline;
    }

    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }

    public List<ErrorVolumeEntry> getTimeline() { return timeline; }
    public void setTimeline(List<ErrorVolumeEntry> timeline) { this.timeline = timeline; }
}