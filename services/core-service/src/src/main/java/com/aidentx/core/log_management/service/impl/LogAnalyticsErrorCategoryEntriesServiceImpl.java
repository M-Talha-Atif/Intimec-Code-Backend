package com.aidentx.core.log_management.service.impl;

import static com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorCategoryConstants.FIELD_LEVEL_KEYWORD;
import static com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorCategoryConstants.LEVEL_ERROR;
import static com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorCategoryConstants.LEVEL_WARN;

import com.aidentx.core.log_management.model.dto.analytics.ErrorCategoryEntriesResponse;
import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.aidentx.core.log_management.service.LogAnalyticsErrorCategoryEntriesService;
import com.aidentx.core.opensearch.service.OpenSearchService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link LogAnalyticsErrorCategoryEntriesService}.
 *
 * <p>Issues one OpenSearch query per call. The query filters on
 * {@code category} for exact match because category is a
 * free-form string field and must not be tokenized.
 *
 * <p>Time filter behaviour:
 * <ul>
 *   <li>{@code hours=0}  -- no time filter, covers all stored log history.
 *       This matches the default chart behaviour (range=all).</li>
 *   <li>{@code hours>0}  -- applies {@code @timestamp >= now - (hours * 3600000)}.
 *       Use this when the chart was rendered with a time window.</li>
 * </ul>
 *
 * <p>Query shape (hours=0, no time filter):
 * <pre>
 * {
 *   "size": size,
 *   "from": page * size,
 *   "track_total_hits": true,
 *   "sort": [{ "@timestamp": { "order": "desc" } }],
 *   "query": {
 *     "term": { "category": "Security-Auth" }
 *   }
 * }
 * </pre>
 *
 * <p>Query shape (hours=24, with time filter):
 * <pre>
 * {
 *   "size": size,
 *   "from": page * size,
 *   "track_total_hits": true,
 *   "sort": [{ "@timestamp": { "order": "desc" } }],
 *   "query": {
 *     "bool": {
 *       "must": [
 *         { "term": { "category": "Security-Auth" } },
 *         { "range": { "@timestamp": { "gte": 1742211620000 } } }
 *       ]
 *     }
 *   }
 * }
 * </pre>
 */
@Service
public class LogAnalyticsErrorCategoryEntriesServiceImpl
        implements LogAnalyticsErrorCategoryEntriesService {

    private static final String LOGS_INDEX = "main-logs";
    private static final String FIELD_CATEGORY_KEYWORD = "category";
    private static final String FIELD_TIMESTAMP = "@timestamp";
    private static final long MS_PER_HOUR = 3_600_000L;

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsErrorCategoryEntriesServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsErrorCategoryEntriesService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ErrorCategoryEntriesResponse fetchEntriesForCategory(
            String category,
            int hours,
            int page,
            int size) {

        logger.info()
                .message("Error category entries request received")
                .detail("category", category)
                .detail("hours", hours)
                .detail("page", page)
                .detail("size", size)
                .log();

        Map<String, Object> query = buildQuery(category, hours, page, size);
        JsonNode result = openSearchService.searchDocuments(LOGS_INDEX, query);

        if (result == null) {
            logger.warn()
                    .message("OpenSearch returned null for category entries")
                    .detail("category", category)
                    .log();
            return buildEmptyResponse(category, page, size);
        }

        long totalElements = result
                .path("hits").path("total").path("value").asLong(0L);

        List<LogEntry> entries = extractLogEntries(result);

        logger.info()
                .message("Error category entries query complete")
                .detail("category", category)
                .detail("totalElements", totalElements)
                .detail("returnedEntries", entries.size())
                .log();

        return new ErrorCategoryEntriesResponse(
                category,
                entries,
                buildPagination(page, size, totalElements));
    }

    /**
     * Builds the OpenSearch query for documents matching the category value.
     *
     * <p>When hours is 0 the query is a simple term filter on category.
     * When hours is greater than 0 a bool/must wraps both the term filter and
     * a timestamp range filter.
     *
     * @param category the exact category string to match
     * @param hours    look-back window in hours; 0 means no time filter
     * @param page     zero-based page number
     * @param size     page size
     * @return full OpenSearch request body
     */
    private Map<String, Object> buildQuery(
            String category, int hours, int page, int size) {

        List<Object> mustClauses = new ArrayList<>();

        // Level filter -- consistent with the error category distribution chart
        // which now only counts ERROR+WARN entries. Entries returned here use
        // the same filter so totalElements matches the bar count that was clicked.
        mustClauses.add(Map.of("terms", Map.of(
                FIELD_LEVEL_KEYWORD,
                List.of(LEVEL_ERROR, LEVEL_WARN))));

        // Exact category match
        mustClauses.add(Map.of("term", Map.of(FIELD_CATEGORY_KEYWORD, category)));

        if (hours > 0) {
            long windowStartMs = System.currentTimeMillis()
                    - (long) hours * MS_PER_HOUR;
            mustClauses.add(Map.of("range", Map.of(
                    FIELD_TIMESTAMP, Map.of("gte", windowStartMs))));
        }

        Map<String, Object> query = new HashMap<>();
        query.put("size", size);
        query.put("from", (long) page * size);
        query.put("track_total_hits", true);
        query.put("sort", List.of(
                Map.of(FIELD_TIMESTAMP, Map.of("order", "desc"))));
        query.put("query", Map.of("bool", Map.of("must", mustClauses)));

        return query;
    }

    /**
     * Extracts LogEntry objects from the hits.hits array of the response.
     * Document ID comes from hit._id, all content fields from hit._source.
     *
     * @param result full OpenSearch response
     * @return list of log entries; empty when hits array is missing
     */
    private List<LogEntry> extractLogEntries(JsonNode result) {
        List<LogEntry> entries = new ArrayList<>();
        JsonNode hits = result.path("hits").path("hits");

        if (hits.isMissingNode() || !hits.isArray()) {
            return entries;
        }

        for (JsonNode hit : hits) {
            JsonNode source = hit.path("_source");
            LogEntry entry = new LogEntry();
            entry.setId(hit.path("_id").asText(""));
            entry.setTimestamp(source.path(FIELD_TIMESTAMP).asLong(0L));
            entry.setLevel(source.path("level").asText(""));
            entry.setSource(source.path("source").asText(""));
            entry.setMessage(source.path("message").asText(""));
            entry.setCategory(source.path("category").asText(null));
            entry.setCorrelationId(source.path("correlationId").asText(null));
            entry.setMicroservice(source.path("microservice").asText(null));
            entries.add(entry);
        }

        return entries;
    }

    /**
     * Builds PaginationMetadata from total hit count and page parameters.
     *
     * @param page          zero-based current page
     * @param size          page size
     * @param totalElements total matching documents from OpenSearch
     * @return populated pagination metadata
     */
    private PaginationMetadata buildPagination(int page, int size, long totalElements) {
        int totalPages = size > 0
                ? (int) Math.ceil((double) totalElements / size)
                : 0;

        PaginationMetadata meta = new PaginationMetadata();
        meta.setPage(page);
        meta.setSize(size);
        meta.setTotalElements(totalElements);
        meta.setTotalPages(totalPages);
        meta.setHasNext(page < totalPages - 1);
        meta.setHasPrevious(page > 0);
        return meta;
    }

    /**
     * Returns an empty response when OpenSearch returns null or errors out.
     *
     * @param category the requested category
     * @param page     current page
     * @param size     page size
     * @return empty response with zero pagination
     */
    private ErrorCategoryEntriesResponse buildEmptyResponse(
            String category, int page, int size) {
        return new ErrorCategoryEntriesResponse(
                category,
                new ArrayList<>(),
                buildPagination(page, size, 0L));
    }
}