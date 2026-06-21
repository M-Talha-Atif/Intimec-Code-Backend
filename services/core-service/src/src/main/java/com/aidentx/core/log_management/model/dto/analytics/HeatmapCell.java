package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single non-zero (source, hour, count) cell in the log error heatmap.
 *
 * <p>Zero-count cells are omitted from the response. The frontend fills
 * them with 0 when rendering the matrix.
 *
 * <p>Example serialised cell:
 * <pre>
 * { "source": "GatewayPermissionFilter", "hour": "15:00", "count": 49 }
 * </pre>
 *
 * <p>Frontend usage:
 * <pre>
 *   const getHeatCount = (source, hour) =>
 *     data.find(d => d.source === source && d.hour === hour)?.count ?? 0;
 *
 *   // hour param passed as "15:00" string matching XAxis dataKey format
 *   getHeatCount("GatewayPermissionFilter", "15:00")
 * </pre>
 *
 * <p>The {@code hour} value is formatted as "HH:00" in the caller-supplied
 * timezone. For Asia/Karachi (UTC+5), a log event at 10:00 UTC appears
 * as "15:00". For Europe/Zurich (UTC+1), the same event appears as "11:00".
 */
public class HeatmapCell {

    /**
     * Source name as stored in the {@code source} field of the log document.
     * Examples: "GatewayPermissionFilter", "JwtUtil", "SsoConfigManager".
     * Maps to a row in the frontend heatmap table.
     */
    @JsonProperty("source")
    private String source;

    /**
     * Hour of day formatted as "HH:00" in the caller-supplied timezone.
     * Maps to a column in the frontend heatmap table.
     *
     * <p>Example: UTC bucket 2026-03-18T10:00:00Z
     * <ul>
     *   <li>Asia/Karachi  (UTC+5) -- hour = "15:00"</li>
     *   <li>Europe/Zurich (UTC+1) -- hour = "11:00"</li>
     *   <li>UTC                   -- hour = "10:00"</li>
     * </ul>
     */
    @JsonProperty("hour")
    private String hour;

    /**
     * Total event count from this source during this hour within the window.
     * Always >= 1 since zero-count cells are omitted from the response.
     */
    @JsonProperty("count")
    private long count;

    public HeatmapCell() {}

    public HeatmapCell(String source, String hour, long count) {
        this.source = source;
        this.hour = hour;
        this.count = count;
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getHour() { return hour; }
    public void setHour(String hour) { this.hour = hour; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}