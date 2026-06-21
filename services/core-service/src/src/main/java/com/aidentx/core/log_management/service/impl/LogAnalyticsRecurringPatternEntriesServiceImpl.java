package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsRecurringPatternsConstants;
import com.aidentx.core.log_management.model.dto.analytics.RecurringPatternEntriesResponse;
import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.aidentx.core.log_management.service.LogAnalyticsRecurringPatternEntriesService;
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
 * Default implementation of {@link LogAnalyticsRecurringPatternEntriesService}.
 *
 * <p>Issues one OpenSearch query per call. The query does an exact match
 * on {@code message.keyword} for the requested message string. This is the
 * same field used by the recurring patterns aggregation so the entries
 * returned are exactly the documents counted in that pattern's occurrence
 * count.
 *
 * <p>Why message.keyword and not message (text field):
 * The {@code message} field is analyzed (tokenized) for full-text search.
 * Searching it with a term query would not match the full string reliably.
 * {@code message.keyword} is the unanalyzed sub-field -- a term query on
 * it does a byte-for-byte exact match which is what we need to return
 * only entries with this exact message, no partial matches.
 *
 * <p>Query shape (hours=0, no time filter):
 * <pre>
 * {
 *   "size": 20,
 *   "from": 0,
 *   "track_total_hits": true,
 *   "sort": [{ "@timestamp": { "order": "desc" } }],
 *   "query": {
 *     "term": { "message.keyword": "Permission denied for user" }
 *   }
 * }
 * </pre>
 *
 * <p>Query shape (hours=24, with time filter):
 * <pre>
 * {
 *   "size": 20,
 *   "from": 0,
 *   "track_total_hits": true,
 *   "sort": [{ "@timestamp": { "order": "desc" } }],
 *   "query": {
 *     "bool": {
 *       "must": [
 *         { "term":  { "message.keyword": "Permission denied for user" } },
 *         { "range": { "@timestamp": { "gte": 1742211620000 } } }
 *       ]
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>The totalElements in the response should match the occurrences count
 * shown on the pattern card exactly when hours=0 (both use all history).
 * A small discrepancy is possible if new entries arrived between when the
 * pattern list was fetched and when the entries are fetched.
 */
@Service
public class LogAnalyticsRecurringPatternEntriesServiceImpl
        implements LogAnalyticsRecurringPatternEntriesService {

    private static final String LOGS_INDEX = "main-logs";
    private static final String FIELD_TIMESTAMP = "@timestamp";

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsRecurringPatternEntriesServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsRecurringPatternEntriesService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecurringPatternEntriesResponse fetchEntriesForMessage(
            String message,
            int hours,
            int page,
            int size) {

        logger.info()
                .message("Recurring pattern entries request received")
                .detail("messageLength", message.length())
                .detail("hours", hours)
                .detail("page", page)
                .detail("size", size)
                .log();

        Map<String, Object> query = buildQuery(message, hours, page, size);
        JsonNode result = openSearchService.searchDocuments(LOGS_INDEX, query);

        if (result == null) {
            logger.warn()
                    .message("OpenSearch returned null for recurring pattern entries")
                    .detail("messageLength", message.length())
                    .log();
            return buildEmptyResponse(message, page, size);
        }

        long totalElements = result
                .path("hits").path("total").path("value").asLong(0L);

        List<LogEntry> entries = extractLogEntries(result);

        logger.info()
                .message("Recurring pattern entries query complete")
                .detail("totalElements", totalElements)
                .detail("returnedEntries", entries.size())
                .log();

        return new RecurringPatternEntriesResponse(
                message,
                entries,
                buildPagination(page, size, totalElements));
    }

    /**
     * Builds the OpenSearch query for entries matching the exact message string.
     *
     * <p>The message is not logged at full length to avoid polluting logs with
     * potentially long message strings. Only the length is logged.
     *
     * <p>When hours is 0 the root query is a simple term filter.
     * When hours is greater than 0 a bool/must wraps both the term filter
     * and a timestamp range filter.
     *
     * @param message exact message string to match on message.keyword
     * @param hours   look-back window; 0 = all history
     * @param page    zero-based page
     * @param size    page size
     * @return full OpenSearch request body
     */
    private Map<String, Object> buildQuery(
            String message, int hours, int page, int size) {

        List<Object> mustClauses = new ArrayList<>();

        // Level filter -- consistent with the recurring patterns list which
        // only includes ERROR+WARN messages. Entries returned here should be
        // the same documents counted in the pattern's occurrences field.
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsRecurringPatternsConstants.FIELD_LEVEL_KEYWORD,
                List.of(LogAnalyticsRecurringPatternsConstants.LEVEL_ERROR,
                        LogAnalyticsRecurringPatternsConstants.LEVEL_WARN))));

        // Exact message match -- finds only entries with this specific message text
        mustClauses.add(Map.of("term", Map.of(
                LogAnalyticsRecurringPatternsConstants.FIELD_MESSAGE_KEYWORD,
                message)));

        if (hours > 0) {
            long windowStartMs = System.currentTimeMillis()
                    - (long) hours * LogAnalyticsRecurringPatternsConstants.MS_PER_HOUR;
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
     * Extracts LogEntry objects from hits.hits array.
     * Document ID from hit._id, all content from hit._source.
     *
     * @param result full OpenSearch response
     * @return list of log entries
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
     * Builds PaginationMetadata from total hits and page parameters.
     *
     * @param page          zero-based current page
     * @param size          page size
     * @param totalElements total matching documents
     * @return populated metadata
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
     * Returns an empty response when OpenSearch returns null.
     *
     * @param message the queried message string
     * @param page    current page
     * @param size    page size
     * @return empty response with zero pagination
     */
    private RecurringPatternEntriesResponse buildEmptyResponse(
            String message, int page, int size) {
        return new RecurringPatternEntriesResponse(
                message,
                new ArrayList<>(),
                buildPagination(page, size, 0L));
    }
}