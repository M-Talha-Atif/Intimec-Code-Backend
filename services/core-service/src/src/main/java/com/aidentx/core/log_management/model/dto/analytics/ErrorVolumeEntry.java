package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A single hour slot in the error volume timeline.
 *
 * <p>The {@code sourceCounts} map holds one entry per top-N source. Jackson
 * serialises map entries as top-level JSON keys via {@link JsonAnyGetter},
 * so the frontend receives a flat object matching the shape it expects:
 * <pre>
 * {
 *   "hour": "02:00",
 *   "JwtAuthenticationFilter": 12,
 *   "GatewayPermissionFilter": 5,
 *   "UserNotificationService": 0
 * }
 * </pre>
 *
 * <p>Sources that had zero errors in this hour are included with value 0.
 * This guarantees every hour entry has the same set of keys, which Recharts
 * requires to render a stacked area chart without gaps.
 *
 * <p>Example: top 3 sources are ["A","B","C"], hour "03:00" had 2 errors
 * from A and none from B or C:
 * <pre>
 *   { "hour": "03:00", "A": 2, "B": 0, "C": 0 }
 * </pre>
 */
public class ErrorVolumeEntry {

    /**
     * Hour label formatted as "HH:00" (e.g. "00:00", "14:00", "23:00").
     * Maps directly to the frontend XAxis dataKey.
     */
    @JsonProperty("hour")
    private String hour;

    /**
     * Dynamic map of source name to error count for this hour.
     * Serialised as flat top-level JSON keys by {@link JsonAnyGetter}.
     * Always contains one entry per top-N source, defaulting to 0 for
     * sources with no errors in this hour.
     */
    private final Map<String, Long> sourceCounts = new LinkedHashMap<>();

    public ErrorVolumeEntry() {}

    public ErrorVolumeEntry(String hour) {
        this.hour = hour;
    }

    public String getHour() { return hour; }
    public void setHour(String hour) { this.hour = hour; }

    /**
     * Adds or updates the count for a specific source in this hour slot.
     *
     * @param source source name as stored in audit_logs.source
     * @param count  error count for this source in this hour
     */
    public void setSourceCount(String source, long count) {
        sourceCounts.put(source, count);
    }

    /**
     * Returns 0 if the source had no errors in this hour.
     *
     * @param source source name
     * @return error count, >= 0
     */
    public long getSourceCount(String source) {
        return sourceCounts.getOrDefault(source, 0L);
    }

    /**
     * Returns the source counts map serialised as flat top-level JSON keys.
     * Jackson calls this during serialisation and merges the map entries
     * into the parent JSON object rather than nesting them under a key.
     *
     * @return source-to-count map
     */
    @JsonAnyGetter
    public Map<String, Long> getSourceCounts() {
        return sourceCounts;
    }
}