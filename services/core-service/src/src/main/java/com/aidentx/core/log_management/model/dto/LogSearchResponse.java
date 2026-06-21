package com.aidentx.core.log_management.model.dto;

import com.aidentx.core.log_management.model.pojo.AggregationBucket;
import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.aidentx.core.log_management.model.pojo.QueryMetadata;

import java.util.List;
import java.util.Map;

/**
 * Response DTO for log search operations.
 * Contains paginated results, aggregation data, and metadata.
 */
public class LogSearchResponse {

    /**
     * Paginated log entries
     */
    private List<LogEntry> logs;

    /**
     * Pagination metadata
     */
    private PaginationMetadata pagination;

    /**
     * Aggregation data for filtering dropdowns and analytics
     */
    private Map<String, List<AggregationBucket>> aggregations;

    /**
     * Query execution metadata
     */
    private QueryMetadata queryMetadata;

    // Main class constructors
    public LogSearchResponse() {
    }

    // Main class getters and setters
    public List<LogEntry> getLogs() {
        return logs;
    }

    public void setLogs(List<LogEntry> logs) {
        this.logs = logs;
    }

    public PaginationMetadata getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMetadata pagination) {
        this.pagination = pagination;
    }

    public Map<String, List<AggregationBucket>> getAggregations() {
        return aggregations;
    }

    public void setAggregations(Map<String, List<AggregationBucket>> aggregations) {
        this.aggregations = aggregations;
    }

    public QueryMetadata getQueryMetadata() {
        return queryMetadata;
    }

    public void setQueryMetadata(QueryMetadata queryMetadata) {
        this.queryMetadata = queryMetadata;
    }
}
