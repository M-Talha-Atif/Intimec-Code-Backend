package com.aidentx.core.log_management.model.dto.analytics;

import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for GET /api/v1/logs/analytics/error-category-entries.
 *
 * <p>Returns the paginated log entries that belong to a specific category
 * value. The category value comes directly from what the user clicked on
 * the Error Category Distribution bar chart.
 *
 * <p>The category field in OpenSearch is a free-form string set by each
 * microservice when it writes a log entry. Examples: "Security-Auth",
 * "SECURITY", "NOTIFICATIONS_MANAGEMENT", "PERMISSION_CHECK". This endpoint
 * does an exact match on category.keyword against that value.
 *
 * <p>Example: user clicks "Security-Auth" on the bar chart.
 * <pre>
 * GET /api/v1/logs/analytics/error-category-entries?category=Security-Auth&page=0&size=20
 * </pre>
 *
 * <p>Example response:
 * <pre>
 * {
 *   "category": "Security-Auth",
 *   "logs": [
 *     { "id": "abc", "level": "ERROR", "source": "JwtUtil", "message": "...", ... }
 *   ],
 *   "pagination": {
 *     "page": 0, "size": 20, "totalElements": 312,
 *     "totalPages": 16, "hasNext": true, "hasPrevious": false
 *   }
 * }
 * </pre>
 */
public class ErrorCategoryEntriesResponse {

    /**
     * The category value that was filtered on.
     * Echoed back so the frontend can display it in the drawer title
     * without needing to track it separately.
     * Example: "Security-Auth", "SECURITY", "PERMISSION_CHECK"
     */
    @JsonProperty("category")
    private String category;

    /**
     * Paginated log entries belonging to this category.
     * Sorted by @timestamp descending (newest first).
     */
    @JsonProperty("logs")
    private List<LogEntry> logs;

    /**
     * Pagination metadata. Shape matches PaginationMetadata used across
     * all other analytics endpoints so the frontend drawer is consistent.
     */
    @JsonProperty("pagination")
    private PaginationMetadata pagination;

    public ErrorCategoryEntriesResponse() {}

    public ErrorCategoryEntriesResponse(
            String category,
            List<LogEntry> logs,
            PaginationMetadata pagination) {
        this.category = category;
        this.logs = logs;
        this.pagination = pagination;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<LogEntry> getLogs() { return logs; }
    public void setLogs(List<LogEntry> logs) { this.logs = logs; }

    public PaginationMetadata getPagination() { return pagination; }
    public void setPagination(PaginationMetadata pagination) { this.pagination = pagination; }
}