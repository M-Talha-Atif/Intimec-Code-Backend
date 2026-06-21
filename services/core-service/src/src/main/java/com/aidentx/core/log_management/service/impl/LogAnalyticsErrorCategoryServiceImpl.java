package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsErrorCategoryConstants;
import com.aidentx.core.log_management.model.dto.analytics.ErrorCategoryEntry;
import com.aidentx.core.log_management.service.LogAnalyticsErrorCategoryService;
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
 * Default implementation of {@link LogAnalyticsErrorCategoryService}.
 *
 * <p>Issues one OpenSearch aggregation query per call. The query uses a
 * bool filter scoping to ERROR+WARN documents that have a non-null category
 * field, then applies a terms aggregation on {@code category} with
 * size set to the resolved limit. OpenSearch returns the top-N buckets
 * ordered by doc_count descending natively -- no post-processing sort needed.
 *
 * <p>Query shape (rangeAll=false, hours=24, limit=10):
 * <pre>
 * POST main-logs/_search
 * {
 *   "size": 0,
 *   "query": {
 *     "bool": {
 *       "must": [
 *         { "exists": { "field": "category" } },
 *         { "range": { "@timestamp": { "gte": 1742211620000 } } }
 *       ]
 *     }
 *   },
 *   "aggs": {
 *     "error_categories": {
 *       "terms": {
 *         "field": "category",
 *         "size": 10
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Query shape (rangeAll=true, limit=10):
 * <pre>
 * POST main-logs/_search
 * {
 *   "size": 0,
 *   "query": {
 *     "bool": {
 *       "must": [
 *         { "terms": { "level.keyword": ["ERROR", "WARN"] } },
 *         { "exists": { "field": "category" } }
 *       ]
 *     }
 *   },
 *   "aggs": {
 *     "error_categories": {
 *       "terms": {
 *         "field": "category",
 *         "size": 10
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>OpenSearch terms aggregation returns buckets ordered by doc_count
 * descending by default, so rank 1 = most errors. The service maps each
 * bucket directly to an {@link ErrorCategoryEntry} with no transformation.
 */
@Service
public class LogAnalyticsErrorCategoryServiceImpl implements LogAnalyticsErrorCategoryService {

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsErrorCategoryServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsErrorCategoryService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Execution sequence:
     * <ol>
     *   <li>Compute the UTC epoch ms window start when rangeAll is false.</li>
     *   <li>Build the aggregation query with level filter, exists filter,
     *       optional time range filter, and terms agg on category.keyword.</li>
     *   <li>Execute against main-logs index.</li>
     *   <li>Parse buckets directly into {@link ErrorCategoryEntry} instances.</li>
     * </ol>
     */
    @Override
    public List<ErrorCategoryEntry> computeErrorCategoryDistribution(
            int limit, int hours, boolean rangeAll) {

        logger.info().message(LogAnalyticsErrorCategoryConstants.LOG_COMPUTATION_STARTED)
                .detail(LogAnalyticsErrorCategoryConstants.LOG_KEY_HOURS,
                        rangeAll ? "all" : hours)
                .detail(LogAnalyticsErrorCategoryConstants.LOG_KEY_LIMIT, limit)
                .detail(LogAnalyticsErrorCategoryConstants.LOG_KEY_RANGE_ALL, rangeAll)
                .log();

        long windowStartMs = resolveWindowStart(hours, rangeAll);
        Map<String, Object> query = buildQuery(limit, windowStartMs, rangeAll);

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsErrorCategoryConstants.LOGS_INDEX, query);

        if (result == null) {
            logger.warn().message(LogAnalyticsErrorCategoryConstants.LOG_OPENSEARCH_NULL)
                    .detail(LogAnalyticsErrorCategoryConstants.LOG_KEY_LIMIT, limit)
                    .log();
            return new ArrayList<>();
        }

        List<ErrorCategoryEntry> entries = parseResult(result);

        logger.info().message(LogAnalyticsErrorCategoryConstants.LOG_COMPUTATION_COMPLETE)
                .detail(LogAnalyticsErrorCategoryConstants.LOG_KEY_RESULT_COUNT, entries.size())
                .log();

        return entries;
    }

    /**
     * Computes the UTC epoch ms lower bound of the time window.
     *
     * <p>When rangeAll is true, returns 0 as a sentinel -- the query builder
     * will omit the range filter entirely in that case. The 0 value is never
     * used as an actual filter bound.
     *
     * <p>Example: hours=24, now=2026-03-18T10:27:00Z
     * -- windowStart = 1742211620000 (2026-03-17T10:27:00Z)
     *
     * @param hours    look-back window in hours
     * @param rangeAll when true, returns 0 (sentinel, not used as filter)
     * @return UTC epoch ms lower bound
     */
    private long resolveWindowStart(int hours, boolean rangeAll) {
        if (rangeAll) {
            return 0L;
        }
        return System.currentTimeMillis()
                - ((long) hours * LogAnalyticsErrorCategoryConstants.MS_PER_HOUR);
    }

    /**
     * Builds the OpenSearch aggregation query.
     *
     * <p>The {@code exists} filter on the category field ensures documents
     * without a category value are excluded. Without this filter, OpenSearch
     * would not include null-category documents in the terms aggregation
     * anyway (terms agg ignores null values), but the exists filter makes
     * the exclusion explicit and can improve query performance by reducing
     * the document set before aggregation.
     *
     * <p>The time range filter is only added when rangeAll is false.
     * Adding it unconditionally with a very low epoch value (e.g. 0) would
     * still work but is wasteful.
     *
     * @param limit         terms aggregation size -- top-N categories
     * @param windowStartMs UTC epoch ms lower bound; only used when rangeAll=false
     * @param rangeAll      when true, omits the time range filter
     * @return OpenSearch request body map
     */
    private Map<String, Object> buildQuery(int limit, long windowStartMs, boolean rangeAll) {
        List<Object> mustClauses = new ArrayList<>();

        // Level filter 1: -- only ERROR and WARN entries are relevant for the
        // error category distribution. INFO/DEBUG categories (GRPC_SERVICE,
        // PERMISSION_CHECK etc.) have high counts from routine operations and
        // would dominate the chart, hiding the actual error distribution.
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsErrorCategoryConstants.FIELD_LEVEL_KEYWORD,
                List.of(LogAnalyticsErrorCategoryConstants.LEVEL_ERROR,
                        LogAnalyticsErrorCategoryConstants.LEVEL_WARN))));


        // Filter 2: category field must exist -- excludes documents with no category
        mustClauses.add(Map.of("exists", Map.of(
                "field", "category")));

        // Filter 3: time window -- only applied when rangeAll is false
        if (!rangeAll) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsErrorCategoryConstants.FIELD_TIMESTAMP,
                    Map.of("gte", windowStartMs))));
        }

        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);
        query.put("query", Map.of("bool", Map.of("must", mustClauses)));

        // Terms aggregation on category.keyword -- OpenSearch returns top-N
        // by doc_count descending natively, no client-side sorting needed
        query.put("aggs", Map.of(
                LogAnalyticsErrorCategoryConstants.AGG_CATEGORIES,
                Map.of("terms", Map.of(
                        "field", LogAnalyticsErrorCategoryConstants.FIELD_CATEGORY_KEYWORD,
                        "size", limit))));

        return query;
    }

    /**
     * Parses the OpenSearch aggregation response into a list of entries.
     *
     * <p>Each bucket in the terms aggregation result maps to one entry:
     * <ul>
     *   <li>{@code bucket.key}       -- the category string</li>
     *   <li>{@code bucket.doc_count} -- the error count</li>
     * </ul>
     *
     * <p>Buckets are already ordered by doc_count descending as returned
     * by OpenSearch. The order is preserved in the output list so rank 1
     * (highest count) is always first.
     *
     * <p>Example raw bucket:
     * <pre>
     *   { "key": "Security-Auth", "doc_count": 312 }
     *   maps to: ErrorCategoryEntry("Security-Auth", 312)
     * </pre>
     *
     * @param result the full OpenSearch response JsonNode
     * @return list of entries preserving OpenSearch rank order
     */
    private List<ErrorCategoryEntry> parseResult(JsonNode result) {
        List<ErrorCategoryEntry> entries = new ArrayList<>();

        JsonNode buckets = result
                .path("aggregations")
                .path(LogAnalyticsErrorCategoryConstants.AGG_CATEGORIES)
                .path("buckets");

        if (buckets.isMissingNode() || !buckets.isArray()) {
            return entries;
        }

        for (JsonNode bucket : buckets) {
            String category = bucket.path("key").asText();
            long count = bucket.path("doc_count").asLong(0L);

            if (!category.isBlank()) {
                entries.add(new ErrorCategoryEntry(category, count));
            }
        }

        return entries;
    }
}