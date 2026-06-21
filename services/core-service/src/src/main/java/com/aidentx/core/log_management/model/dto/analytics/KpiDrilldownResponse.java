package com.aidentx.core.log_management.model.dto.analytics;

import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for {@code GET /api/v1/logs/analytics/kpi-drilldown}.
 *
 * <p>Returns the paginated log entries that were counted to produce a
 * specific KPI card value. The response shape is intentionally identical
 * for all drilldown types so the frontend drawer can render them uniformly.
 *
 * <p>Example response for type=total_errors, page=0, size=2:
 * <pre>
 * {
 *   "type": "total_errors",
 *   "logs": [
 *     { "id": "abc", "level": "ERROR", "source": "JwtUtil", "message": "...", ... },
 *     { "id": "def", "level": "WARN",  "source": "GatewayFilter", "message": "...", ... }
 *   ],
 *   "pagination": {
 *     "page": 0,
 *     "size": 2,
 *     "totalElements": 176,
 *     "totalPages": 88,
 *     "hasNext": true,
 *     "hasPrevious": false
 *   }
 * }
 * </pre>
 *
 * <p>Frontend mapping:
 * <ul>
 *   <li>{@code type}       -- displayed in the drawer header for context</li>
 *   <li>{@code logs}       -- rendered as rows in the drilldown table</li>
 *   <li>{@code pagination} -- drives prev/next buttons and total count label</li>
 * </ul>
 */
public class KpiDrilldownResponse {

    /**
     * The drilldown type that was requested.
     * One of: total_errors, critical, slow_responses, recurring.
     * Included in the response so the frontend can label the drawer
     * without needing to track request state separately.
     */
    @JsonProperty("type")
    private String type;

    /**
     * Paginated log entries matching the requested drilldown type.
     * Reuses the existing {@link LogEntry} POJO already used by the
     * main log search endpoint -- no new mapping needed on the frontend.
     */
    @JsonProperty("logs")
    private List<LogEntry> logs;

    /**
     * Pagination metadata matching the {@link PaginationMetadata} shape
     * already consumed by the frontend from the main search endpoint.
     */
    @JsonProperty("pagination")
    private PaginationMetadata pagination;

    public KpiDrilldownResponse() {}

    public KpiDrilldownResponse(
            String type,
            List<LogEntry> logs,
            PaginationMetadata pagination) {
        this.type = type;
        this.logs = logs;
        this.pagination = pagination;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public List<LogEntry> getLogs() { return logs; }
    public void setLogs(List<LogEntry> logs) { this.logs = logs; }

    public PaginationMetadata getPagination() { return pagination; }
    public void setPagination(PaginationMetadata pagination) { this.pagination = pagination; }
}