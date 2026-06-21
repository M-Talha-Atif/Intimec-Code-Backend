package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for {@code GET /api/v1/logs/analytics/heatmap}.
 *
 * <p>Example (limit=2, hours=3, timezone=Asia/Karachi):
 * <pre>
 * {
 *   "hours": 3,
 *   "limit": 2,
 *   "sources": ["GatewayPermissionFilter", "JwtUtil"],
 *   "data": [
 *     { "source": "GatewayPermissionFilter", "hour": "15:00", "count": 49 },
 *     { "source": "GatewayPermissionFilter", "hour": "17:00", "count": 68 },
 *     { "source": "JwtUtil",                 "hour": "15:00", "count": 6  },
 *     { "source": "JwtUtil",                 "hour": "17:00", "count": 12 }
 *   ]
 * }
 * </pre>
 *
 * <p>Hours "16:00" for both sources are absent -- they had zero events.
 * Frontend fills them: {@code getHeatCount("GatewayPermissionFilter", "16:00") = 0}
 */
public class HeatmapResponse {

    /**
     * Effective look-back window in hours that was applied.
     */
    @JsonProperty("hours")
    private int hours;

    /**
     * Number of top sources included in the response.
     */
    @JsonProperty("limit")
    private int limit;

    /**
     * Ordered list of source names included in the data cells.
     * Sources are ordered by total event count descending (rank 1 = most events).
     */
    @JsonProperty("sources")
    private List<String> sources;

    /**
     * Sparse list of non-zero (source, hour, count) cells.
     * Zero-count cells are absent. Hour values are "HH:00" strings in the
     * caller-supplied timezone. Cells are ordered by source rank first,
     * then hour ascending.
     */
    @JsonProperty("data")
    private List<HeatmapCell> data;

    public HeatmapResponse() {}

    public HeatmapResponse(int hours, int limit, List<String> sources, List<HeatmapCell> data) {
        this.hours = hours;
        this.limit = limit;
        this.sources = sources;
        this.data = data;
    }

    public int getHours() { return hours; }
    public void setHours(int hours) { this.hours = hours; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }

    public List<HeatmapCell> getData() { return data; }
    public void setData(List<HeatmapCell> data) { this.data = data; }
}