package com.aidentx.core.log_management.model.dto.analytics;

import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for GET /api/v1/logs/analytics/recurring-pattern-entries.
 *
 * <p>Returns the paginated log entries that share a specific repeated
 * message string. The caller passes the exact message text from a
 * {@link RecurringPatternEntry} and this endpoint returns all documents
 * that produced that message, newest first.
 *
 * <p>Example: user clicks the pattern card for
 * "Permission denied for user" which appeared 312 times.
 * <pre>
 * GET /api/v1/logs/analytics/recurring-pattern-entries
 *     ?message=Permission denied for user
 *     &page=0&size=20
 * </pre>
 *
 * <p>Example response:
 * <pre>
 * {
 *   "message": "Permission denied for user",
 *   "logs": [
 *     { "id": "abc", "level": "ERROR", "source": "GatewayPermissionFilter",
 *       "message": "Permission denied for user", "timestamp": 1742292000000, ... }
 *   ],
 *   "pagination": {
 *     "page": 0, "size": 20, "totalElements": 312,
 *     "totalPages": 16, "hasNext": true, "hasPrevious": false
 *   }
 * }
 * </pre>
 */
public class RecurringPatternEntriesResponse {

    /**
     * The exact message string that was queried.
     * Echoed back so the frontend drawer title can confirm which pattern
     * the entries belong to without needing to track it separately.
     */
    @JsonProperty("message")
    private String message;

    /**
     * Paginated log entries where message = the queried string exactly.
     * Sorted by @timestamp descending (newest first).
     */
    @JsonProperty("logs")
    private List<LogEntry> logs;

    /**
     * Pagination metadata matching the standard PaginationMetadata shape
     * used across all analytics endpoints.
     */
    @JsonProperty("pagination")
    private PaginationMetadata pagination;

    public RecurringPatternEntriesResponse() {}

    public RecurringPatternEntriesResponse(
            String message,
            List<LogEntry> logs,
            PaginationMetadata pagination) {
        this.message    = message;
        this.logs       = logs;
        this.pagination = pagination;
    }

    public String getMessage()             { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<LogEntry> getLogs()        { return logs; }
    public void setLogs(List<LogEntry> logs) { this.logs = logs; }

    public PaginationMetadata getPagination()                  { return pagination; }
    public void setPagination(PaginationMetadata pagination)   { this.pagination = pagination; }
}