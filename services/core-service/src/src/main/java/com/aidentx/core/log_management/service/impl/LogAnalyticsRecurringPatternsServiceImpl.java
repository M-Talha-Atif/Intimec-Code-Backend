package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsRecurringPatternsConstants;
import com.aidentx.core.log_management.model.dto.analytics.RecurringPatternEntry;
import com.aidentx.core.log_management.model.dto.analytics.RecurringPatternsResponse;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.aidentx.core.log_management.service.LogAnalyticsRecurringPatternsService;
import com.aidentx.core.opensearch.service.OpenSearchService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link LogAnalyticsRecurringPatternsService}.
 *
 * <p>Issues one OpenSearch aggregation query per call. The query uses a
 * terms aggregation on message.keyword with min_doc_count to filter out
 * one-off messages. Inside each message bucket four sub-aggregations
 * compute the detail fields:
 * <ul>
 *   <li>min(@timestamp) -> firstSeen</li>
 *   <li>max(@timestamp) -> lastSeen</li>
 *   <li>terms on source (size=1) -> dominant source</li>
 *   <li>terms on level.keyword (size=4) -> dominant level for severity</li>
 * </ul>
 *
 * <p>Full OpenSearch query (limit=10, no time filter):
 * <pre>
 * POST main-logs/_search
 * {
 *   "size": 0,
 *   "query": { "match_all": {} },
 *   "aggs": {
 *     "recurring_messages": {
 *       "terms": {
 *         "field": "message.keyword",
 *         "size": 10,
 *         "min_doc_count": 3,
 *         "order": { "_count": "desc" }
 *       },
 *       "aggs": {
 *         "first_seen":         { "min":   { "field": "@timestamp" } },
 *         "last_seen":          { "max":   { "field": "@timestamp" } },
 *         "top_source":         { "terms": { "field": "source", "size": 1 } },
 *         "level_distribution": { "terms": { "field": "level.keyword",  "size": 4 } }
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>With hours=24 the root query adds a range filter:
 * <pre>
 * "query": {
 *   "range": { "@timestamp": { "gte": now - 86400000 } }
 * }
 * </pre>
 *
 * <p>Severity derivation:
 * The level_distribution buckets are iterated to find the level with the
 * highest doc_count. That level maps to a severity label:
 * ERROR -> high, WARN -> medium, INFO/DEBUG -> low.
 * If all occurrences of a message are INFO level, severity is low --
 * INFO messages can still be recurring patterns worth investigating.
 */
@Service
public class LogAnalyticsRecurringPatternsServiceImpl
        implements LogAnalyticsRecurringPatternsService {

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsRecurringPatternsServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsRecurringPatternsService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RecurringPatternsResponse computeRecurringPatterns(int page, int size, int hours) {
        logger.info().message(LogAnalyticsRecurringPatternsConstants.LOG_COMPUTATION_STARTED)
                .detail(LogAnalyticsRecurringPatternsConstants.LOG_KEY_LIMIT, size)
                .detail(LogAnalyticsRecurringPatternsConstants.LOG_KEY_HOURS, hours)
                .log();

        // Fetch enough buckets from OpenSearch to cover all pages up to and
        // including the requested page. Terms aggregation always returns from
        // the top so we cannot skip -- we fetch page*size+size and slice.
        // Cap at MAXIMUM_LIMIT to avoid runaway aggregation on deep pages.
        int fetchSize = Math.min(
                (page + 1) * size,
                LogAnalyticsRecurringPatternsConstants.MAXIMUM_LIMIT);

        Map<String, Object> query = buildQuery(fetchSize, hours);
        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsRecurringPatternsConstants.LOGS_INDEX, query);

        if (result == null) {
            logger.warn().message(LogAnalyticsRecurringPatternsConstants.LOG_OPENSEARCH_NULL)
                    .detail(LogAnalyticsRecurringPatternsConstants.LOG_KEY_LIMIT, size)
                    .log();
            return buildEmptyResponse(page, size);
        }

        List<RecurringPatternEntry> allPatterns = parsePatterns(result);

        // Slice the fetched list to the requested page
        int fromIndex = page * size;
        if (fromIndex >= allPatterns.size()) {
            return buildEmptyResponse(page, size);
        }
        int toIndex = Math.min(fromIndex + size, allPatterns.size());
        List<RecurringPatternEntry> pagePatterns = allPatterns.subList(fromIndex, toIndex);

        // totalElements is approximate -- we only fetched up to MAXIMUM_LIMIT.
        // When the fetched count equals fetchSize there may be more beyond the cap.
        long totalElements = allPatterns.size();
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        PaginationMetadata pagination = new PaginationMetadata();
        pagination.setPage(page);
        pagination.setSize(size);
        pagination.setTotalElements(totalElements);
        pagination.setTotalPages(totalPages);
        pagination.setHasNext(page < totalPages - 1);
        pagination.setHasPrevious(page > 0);

        logger.info().message(LogAnalyticsRecurringPatternsConstants.LOG_COMPUTATION_COMPLETE)
                .detail(LogAnalyticsRecurringPatternsConstants.LOG_KEY_RESULT_COUNT, pagePatterns.size())
                .log();

        return new RecurringPatternsResponse(pagePatterns, pagination);
    }
    /**
     * Builds the full OpenSearch aggregation query.
     *
     * <p>Root query is match_all when hours=0 (no time filter).
     * When hours>0 a range filter scopes to the last N hours.
     *
     * <p>The terms aggregation uses {@code "order": {"_count": "desc"}} to
     * return patterns ranked by occurrence count descending -- most frequent
     * pattern first. This is the default for terms agg but stated explicitly
     * for clarity.
     *
     * @param limit number of patterns to return (terms agg size)
     * @param hours look-back window; 0 = no filter
     * @return OpenSearch request body
     */
   private Map<String, Object> buildQuery(int limit, int hours) {
        Map<String, Object> rootQuery = buildRootQuery(hours);

        Map<String, Object> subAggs = new HashMap<>();

        subAggs.put(LogAnalyticsRecurringPatternsConstants.AGG_FIRST_SEEN,
                Map.of("min", Map.of(
                        "field", LogAnalyticsRecurringPatternsConstants.FIELD_TIMESTAMP)));

        subAggs.put(LogAnalyticsRecurringPatternsConstants.AGG_LAST_SEEN,
                Map.of("max", Map.of(
                        "field", LogAnalyticsRecurringPatternsConstants.FIELD_TIMESTAMP)));

        subAggs.put(LogAnalyticsRecurringPatternsConstants.AGG_TOP_SOURCE,
                Map.of("terms", Map.of(
                        "field", LogAnalyticsRecurringPatternsConstants.FIELD_SOURCE_KEYWORD,
                        "size", 1)));

        subAggs.put(LogAnalyticsRecurringPatternsConstants.AGG_LEVEL_DIST,
                Map.of("terms", Map.of(
                        "field", LogAnalyticsRecurringPatternsConstants.FIELD_LEVEL_KEYWORD,
                        "size", 4)));

        Map<String, Object> messagesAgg = new HashMap<>();
        messagesAgg.put("terms", Map.of(
                "field",         LogAnalyticsRecurringPatternsConstants.FIELD_MESSAGE_KEYWORD,
                "size",          limit,
                "min_doc_count", LogAnalyticsRecurringPatternsConstants.MIN_OCCURRENCES,
                "order",         Map.of("_count", "desc")));
        messagesAgg.put("aggs", subAggs);

        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);
        query.put("query", rootQuery);
        query.put("aggs", Map.of(
                LogAnalyticsRecurringPatternsConstants.AGG_MESSAGES, messagesAgg));

        return query;
    }

    /**
     * Builds the root query clause.
     * Returns match_all when hours is 0 (covers all stored history).
     * Returns a range filter when hours > 0 (covers last N hours).
     *
     * @param hours look-back window; 0 = no filter
     * @return OpenSearch query clause
     */
    private Map<String, Object> buildRootQuery(int hours) {
        List<Object> mustClauses = new ArrayList<>();

        // Level filter -- recurring patterns are only meaningful for ERROR
        // and WARN. INFO messages repeating is expected (health checks,
        // permission granted confirmations). Including them would return
        // thousands of normal operational patterns that are not actionable.
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsRecurringPatternsConstants.FIELD_LEVEL_KEYWORD,
                List.of(LogAnalyticsRecurringPatternsConstants.LEVEL_ERROR,
                        LogAnalyticsRecurringPatternsConstants.LEVEL_WARN))));

        if (hours > 0) {
            long windowStartMs = System.currentTimeMillis()
                    - (long) hours * LogAnalyticsRecurringPatternsConstants.MS_PER_HOUR;
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsRecurringPatternsConstants.FIELD_TIMESTAMP,
                    Map.of("gte", windowStartMs))));
        }

        return Map.of("bool", Map.of("must", mustClauses));
    }

    /**
     * Parses the OpenSearch aggregation response into a list of patterns.
     *
     * <p>Each bucket in the terms aggregation represents one recurring message.
     * The bucket structure is:
     * <pre>
     * {
     *   "key": "Permission denied for user",
     *   "doc_count": 312,
     *   "first_seen":         { "value": 1742205600000 },
     *   "last_seen":          { "value": 1742292000000 },
     *   "top_source":         { "buckets": [{ "key": "GatewayPermissionFilter", "doc_count": 280 }] },
     *   "level_distribution": { "buckets": [
     *     { "key": "ERROR", "doc_count": 290 },
     *     { "key": "INFO",  "doc_count":  22 }
     *   ]}
     * }
     * </pre>
     *
     * @param result full OpenSearch response
     * @return list of RecurringPatternEntry ordered by occurrence count descending
     */
    private List<RecurringPatternEntry> parsePatterns(JsonNode result) {
        List<RecurringPatternEntry> patterns = new ArrayList<>();

        JsonNode buckets = result
                .path("aggregations")
                .path(LogAnalyticsRecurringPatternsConstants.AGG_MESSAGES)
                .path("buckets");

        if (buckets.isMissingNode() || !buckets.isArray()) {
            return patterns;
        }

        int index = 0;
        for (JsonNode bucket : buckets) {
            String message = bucket.path("key").asText("");
            long occurrences = bucket.path("doc_count").asLong(0L);

            if (message.isBlank() || occurrences < LogAnalyticsRecurringPatternsConstants.MIN_OCCURRENCES) {
                continue;
            }

            String firstSeen = parseEpochToIso(
                    bucket.path(LogAnalyticsRecurringPatternsConstants.AGG_FIRST_SEEN)
                            .path("value").asLong(0L));

            String lastSeen = parseEpochToIso(
                    bucket.path(LogAnalyticsRecurringPatternsConstants.AGG_LAST_SEEN)
                            .path("value").asLong(0L));

            String topSource = parseTopSource(
                    bucket.path(LogAnalyticsRecurringPatternsConstants.AGG_TOP_SOURCE));

            String severity = parseSeverity(
                    bucket.path(LogAnalyticsRecurringPatternsConstants.AGG_LEVEL_DIST));

            patterns.add(new RecurringPatternEntry(
                    "pat-" + index,
                    message,
                    occurrences,
                    firstSeen,
                    lastSeen,
                    topSource,
                    severity));

            index++;
        }

        return patterns;
    }

    /**
     * Converts a UTC epoch ms long to an ISO-8601 string.
     * Returns empty string when epochMs is 0 (missing value from OpenSearch).
     *
     * <p>Example: 1742205600000 -> "2026-03-17T10:00:00Z"
     *
     * @param epochMs UTC epoch milliseconds
     * @return ISO-8601 string or empty string
     */
    private String parseEpochToIso(long epochMs) {
        if (epochMs == 0L) return "";
        return Instant.ofEpochMilli(epochMs).toString();
    }

    /**
     * Extracts the top source name from the top_source sub-aggregation.
     * Returns empty string when the buckets array is missing or empty.
     *
     * <p>The top_source agg was configured with size=1 so there is at most
     * one bucket. That bucket's key is the source component name.
     *
     * @param topSourceNode the top_source aggregation node from the bucket
     * @return source name or empty string
     */
    private String parseTopSource(JsonNode topSourceNode) {
        JsonNode buckets = topSourceNode.path("buckets");
        if (buckets.isMissingNode() || !buckets.isArray() || buckets.size() == 0) {
            return "";
        }
        return buckets.get(0).path("key").asText("");
    }

    /**
     * Derives the severity label from the level_distribution sub-aggregation.
     *
     * <p>Iterates all level buckets and picks the one with the highest
     * doc_count. Maps that level to a severity label:
     * <ul>
     *   <li>ERROR -> "high"</li>
     *   <li>WARN  -> "medium"</li>
     *   <li>INFO or DEBUG -> "low"</li>
     * </ul>
     *
     * <p>Example: a message appears 300 times as ERROR and 12 times as INFO.
     * Dominant level is ERROR -> severity = "high".
     *
     * @param levelDistNode the level_distribution aggregation node
     * @return severity label string
     */
    private String parseSeverity(JsonNode levelDistNode) {
        JsonNode buckets = levelDistNode.path("buckets");
        if (buckets.isMissingNode() || !buckets.isArray() || buckets.size() == 0) {
            return LogAnalyticsRecurringPatternsConstants.SEVERITY_LOW;
        }

        String dominantLevel = "";
        long maxCount = -1L;

        for (JsonNode bucket : buckets) {
            long count = bucket.path("doc_count").asLong(0L);
            if (count > maxCount) {
                maxCount = count;
                dominantLevel = bucket.path("key").asText("");
            }
        }

        if (LogAnalyticsRecurringPatternsConstants.LEVEL_ERROR
                .equalsIgnoreCase(dominantLevel)) {
            return LogAnalyticsRecurringPatternsConstants.SEVERITY_HIGH;
        }
        if (LogAnalyticsRecurringPatternsConstants.LEVEL_WARN
                .equalsIgnoreCase(dominantLevel)) {
            return LogAnalyticsRecurringPatternsConstants.SEVERITY_MEDIUM;
        }
        return LogAnalyticsRecurringPatternsConstants.SEVERITY_LOW;
    }

    /** Builds an empty response with zero patterns and pagination metadata indicating no results.
     * @param page current page number
     * @param size page size
     * @return RecurringPatternsResponse with empty patterns list and zero total elements/pages
     **/

    private RecurringPatternsResponse buildEmptyResponse(int page, int size) {
        PaginationMetadata pagination = new PaginationMetadata();
        pagination.setPage(page);
        pagination.setSize(size);
        pagination.setTotalElements(0L);
        pagination.setTotalPages(0);
        pagination.setHasNext(false);
        pagination.setHasPrevious(false);
        return new RecurringPatternsResponse(new ArrayList<>(), pagination);
    }
}