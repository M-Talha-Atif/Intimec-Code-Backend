package com.aidentx.core.log_management.model.dto.analytics;

import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for GET /api/v1/logs/analytics/error-volume-entries.
 *
 * <p>Returns the paginated log entries that were counted in a specific hour
 * slot of the error volume timeline. The caller supplies the hour label and
 * timezone that were used to render the chart and this endpoint resolves the
 * correct UTC time window, fetches matching documents from OpenSearch, and
 * returns them with full pagination metadata.
 *
 * <p>Example: user clicks "15:00" on a chart rendered in Asia/Karachi (UTC+5).
 * The backend resolves "15:00 PKT" to 10:00 UTC, builds the range
 * [10:00:00.000Z, 10:59:59.999Z], queries OpenSearch for documents in that
 * window matching the requested sources, and returns them paginated.
 *
 * <p>Example response:
 * <pre>
 * {
 *   "hour": "15:00",
 *   "timezone": "Asia/Karachi",
 *   "resolvedDateFrom": 1742205600000,
 *   "resolvedDateTo":   1742209199999,
 *   "sources": ["GatewayPermissionFilter"],
 *   "logs": [ { "id": "...", "level": "INFO", ... } ],
 *   "pagination": {
 *     "page": 0, "size": 20, "totalElements": 9067,
 *     "totalPages": 454, "hasNext": true, "hasPrevious": false
 *   }
 * }
 * </pre>
 *
 * <p>The {@code resolvedDateFrom} and {@code resolvedDateTo} fields are
 * included in the response so the frontend can verify the exact UTC bounds
 * that were applied. This is useful for debugging timezone-related issues.
 */
public class ErrorVolumeEntriesResponse {

    /**
     * The hour label that was requested, e.g. "15:00".
     * Echoed back for the frontend to display in the drawer title.
     */
    @JsonProperty("hour")
    private String hour;

    /**
     * The IANA timezone that was used to resolve the hour label to UTC bounds.
     * Echoed back so the client can verify the timezone was applied correctly.
     */
    @JsonProperty("timezone")
    private String timezone;

    /**
     * The resolved UTC epoch ms lower bound (inclusive) for the hour slot.
     * Example: "15:00" in Asia/Karachi resolves to 1742205600000 (= 10:00 UTC).
     * Included for debugging and transparency.
     */
    @JsonProperty("resolvedDateFrom")
    private long resolvedDateFrom;

    /**
     * The resolved UTC epoch ms upper bound (inclusive) for the hour slot.
     * Always resolvedDateFrom + 3599999 (one full hour minus one ms).
     */
    @JsonProperty("resolvedDateTo")
    private long resolvedDateTo;

    /**
     * The source names that were applied as a filter.
     * Empty list means no source filter was applied (all sources included).
     */
    @JsonProperty("sources")
    private List<String> sources;

    /**
     * Paginated log entries from this hour slot matching the source filter.
     */
    @JsonProperty("logs")
    private List<LogEntry> logs;

    /**
     * Pagination metadata matching the standard PaginationMetadata shape.
     */
    @JsonProperty("pagination")
    private PaginationMetadata pagination;

    public ErrorVolumeEntriesResponse() {}

    public ErrorVolumeEntriesResponse(
            String hour,
            String timezone,
            long resolvedDateFrom,
            long resolvedDateTo,
            List<String> sources,
            List<LogEntry> logs,
            PaginationMetadata pagination) {
        this.hour = hour;
        this.timezone = timezone;
        this.resolvedDateFrom = resolvedDateFrom;
        this.resolvedDateTo = resolvedDateTo;
        this.sources = sources;
        this.logs = logs;
        this.pagination = pagination;
    }

    public String getHour() { return hour; }
    public void setHour(String hour) { this.hour = hour; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public long getResolvedDateFrom() { return resolvedDateFrom; }
    public void setResolvedDateFrom(long resolvedDateFrom) { this.resolvedDateFrom = resolvedDateFrom; }

    public long getResolvedDateTo() { return resolvedDateTo; }
    public void setResolvedDateTo(long resolvedDateTo) { this.resolvedDateTo = resolvedDateTo; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }

    public List<LogEntry> getLogs() { return logs; }
    public void setLogs(List<LogEntry> logs) { this.logs = logs; }

    public PaginationMetadata getPagination() { return pagination; }
    public void setPagination(PaginationMetadata pagination) { this.pagination = pagination; }
}