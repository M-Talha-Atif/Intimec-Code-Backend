package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsCorrelationConstants;
import com.aidentx.core.log_management.model.dto.analytics.CorrelationChain;
import com.aidentx.core.log_management.model.dto.analytics.CorrelationStep;
import com.aidentx.core.log_management.service.LogAnalyticsCorrelationService;
import com.aidentx.core.opensearch.service.OpenSearchService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of {@link LogAnalyticsCorrelationService}.
 *
 * <p>Execution sequence per request:
 * <ol>
 *   <li>Compute UTC epoch ms window start.</li>
 *   <li>Query 1: terms aggregation on correlationId.keyword to find the
 *       top-N correlationIds with the most documents. Filtered to documents
 *       that have a correlationId field (exists filter) and optionally a
 *       time range. Buckets with doc_count less than MIN_CHAIN_SIZE are
 *       excluded by the min_doc_count parameter.</li>
 *   <li>Collect the correlationId strings from the buckets.</li>
 *   <li>Query 2: terms query (ids filter) fetching all documents whose
 *       correlationId is in the collected set. Documents are sorted by
 *       @timestamp ascending so steps are in chronological order.</li>
 *   <li>Group fetched documents by correlationId in memory.</li>
 *   <li>For each correlationId build a {@link CorrelationChain} by
 *       deriving rootCause, severity, and assembling CorrelationStep list.</li>
 *   <li>Sort chains by affectedCount descending to match the ranking from
 *       the aggregation query.</li>
 * </ol>
 *
 * <p>Why two queries: the aggregation query finds which correlationIds have
 * multiple documents efficiently. The second query fetches the actual document
 * content (message, source, level, timestamp) for those IDs. OpenSearch
 * aggregations cannot return full document source, only field values used
 * in the aggregation.
 *
 * <p>Query 1 example (rangeAll=false, limit=10):
 * <pre>
 * {
 *   "size": 0,
 *   "query": {
 *     "bool": {
 *       "must": [
 *         { "exists": { "field": "correlationId" } },
 *         { "range": { "@timestamp": { "gte": 1742211620000 } } }
 *       ]
 *     }
 *   },
 *   "aggs": {
 *     "correlation_ids": {
 *       "terms": {
 *         "field": "correlationId.keyword",
 *         "size": 10,
 *         "min_doc_count": 2
 *       }
 *     }
 *   }
 * }
 * </pre>
 *
 * <p>Query 2 example (fetching documents for found correlationIds):
 * <pre>
 * {
 *   "size": 200,
 *   "query": {
 *     "terms": {
 *       "correlationId.keyword": [
 *         "req-17136fed-b7ed-4441-87d4-d59bc7b4730f",
 *         "req-b6a53e3b-2c33-4fb4-888a-f4ab943b6833"
 *       ]
 *     }
 *   },
 *   "sort": [ { "@timestamp": { "order": "asc" } } ]
 * }
 * </pre>
 */
@Service
public class LogAnalyticsCorrelationServiceImpl implements LogAnalyticsCorrelationService {

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsCorrelationServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsCorrelationService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CorrelationChain> computeCorrelationChains(
            int limit, int hours, boolean rangeAll) {

        logger.info().message(LogAnalyticsCorrelationConstants.LOG_COMPUTATION_STARTED)
                .detail(LogAnalyticsCorrelationConstants.LOG_KEY_HOURS,
                        rangeAll ? "all" : hours)
                .detail(LogAnalyticsCorrelationConstants.LOG_KEY_LIMIT, limit)
                .detail(LogAnalyticsCorrelationConstants.LOG_KEY_RANGE_ALL, rangeAll)
                .log();

        long windowStartMs = resolveWindowStart(hours, rangeAll);

        // Step 1: find top-N correlationIds with multiple documents
        List<String> topCorrelationIds = fetchTopCorrelationIds(
                limit, windowStartMs, rangeAll);

        if (topCorrelationIds.isEmpty()) {
            logger.info().message(LogAnalyticsCorrelationConstants.LOG_COMPUTATION_COMPLETE)
                    .detail(LogAnalyticsCorrelationConstants.LOG_KEY_CHAIN_COUNT, 0)
                    .log();
            return new ArrayList<>();
        }

        // Step 2: fetch all documents for those correlationIds
        List<JsonNode> documents = fetchChainDocuments(topCorrelationIds);

        // Step 3: group by correlationId and build chains
        List<CorrelationChain> chains = buildChains(topCorrelationIds, documents);

        logger.info().message(LogAnalyticsCorrelationConstants.LOG_COMPUTATION_COMPLETE)
                .detail(LogAnalyticsCorrelationConstants.LOG_KEY_CHAIN_COUNT, chains.size())
                .log();

        return chains;
    }

    /**
     * Computes the UTC epoch ms window lower bound.
     *
     * @param hours    look-back window in hours
     * @param rangeAll when true, returns 0 (no lower bound)
     * @return UTC epoch ms lower bound
     */
    private long resolveWindowStart(int hours, boolean rangeAll) {
        if (rangeAll) {
            return 0L;
        }
        return System.currentTimeMillis()
                - ((long) hours * LogAnalyticsCorrelationConstants.MS_PER_HOUR);
    }

    /**
     * Issues the terms aggregation query to find top-N correlationIds.
     *
     * <p>Uses {@code min_doc_count: MIN_CHAIN_SIZE} so single-document
     * correlationIds are excluded by OpenSearch before the bucket is returned.
     * This is more efficient than filtering in the service after receiving
     * all buckets.
     *
     * <p>When rangeAll=false, a range filter limits documents to the window.
     * Note: when rangeAll=true the exists filter alone applies, ensuring only
     * documents with a non-null correlationId are counted.
     *
     * @param limit         terms agg size -- top-N correlationIds
     * @param windowStartMs UTC epoch ms lower bound; ignored when rangeAll=true
     * @param rangeAll      when true, omits range filter
     * @return list of correlationId strings ordered by doc_count descending
     */
    private List<String> fetchTopCorrelationIds(
            int limit, long windowStartMs, boolean rangeAll) {

        List<Object> mustClauses = new ArrayList<>();

        // Level filter -- correlation chains are only meaningful when they
        // contain at least one error or warning. A chain of INFO messages
        // is normal request flow, not a cascading failure.
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsCorrelationConstants.FIELD_LEVEL_KEYWORD,
                List.of(LogAnalyticsCorrelationConstants.LEVEL_ERROR,
                        LogAnalyticsCorrelationConstants.LEVEL_WARN))));

        mustClauses.add(Map.of("exists", Map.of("field", "correlationId")));

        if (!rangeAll) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsCorrelationConstants.FIELD_TIMESTAMP,
                    Map.of("gte", windowStartMs))));
        }

        Map<String, Object> termsAgg = new HashMap<>();
        termsAgg.put("field", LogAnalyticsCorrelationConstants.FIELD_CORRELATION_ID_KEYWORD);
        termsAgg.put("size", limit);
        termsAgg.put("min_doc_count", LogAnalyticsCorrelationConstants.MIN_CHAIN_SIZE);

        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);
        query.put("query", Map.of("bool", Map.of("must", mustClauses)));
        query.put("aggs", Map.of(
                LogAnalyticsCorrelationConstants.AGG_CORRELATION_IDS,
                Map.of("terms", termsAgg)));

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsCorrelationConstants.LOGS_INDEX, query);

        if (result == null) {
            logger.warn().message(LogAnalyticsCorrelationConstants.LOG_OPENSEARCH_NULL)
                    .detail(LogAnalyticsCorrelationConstants.LOG_KEY_LIMIT, limit)
                    .log();
            return new ArrayList<>();
        }

        List<String> ids = new ArrayList<>();
        JsonNode buckets = result
                .path("aggregations")
                .path(LogAnalyticsCorrelationConstants.AGG_CORRELATION_IDS)
                .path("buckets");

        if (!buckets.isMissingNode() && buckets.isArray()) {
            for (JsonNode bucket : buckets) {
                String correlationId = bucket.path("key").asText();
                if (!correlationId.isBlank()) {
                    ids.add(correlationId);
                }
            }
        }

        return ids;
    }

    /**
     * Fetches all log documents whose correlationId is in the provided list.
     * Documents are sorted by @timestamp ascending so steps are chronological.
     *
     * <p>The query size is set to
     * {@code topCorrelationIds.size() * MAX_STEPS_PER_CHAIN} to provide
     * an upper bound. In practice most chains have 2-5 steps.
     *
     * @param correlationIds list of correlationId strings to fetch
     * @return list of raw document _source JsonNodes ordered by timestamp ASC
     */
    private List<JsonNode> fetchChainDocuments(List<String> correlationIds) {
        int maxDocs = correlationIds.size()
                * LogAnalyticsCorrelationConstants.MAX_STEPS_PER_CHAIN;

        Map<String, Object> query = new HashMap<>();
        query.put("size", maxDocs);
        query.put("query", Map.of("terms", Map.of(
                LogAnalyticsCorrelationConstants.FIELD_CORRELATION_ID_KEYWORD,
                correlationIds)));
        query.put("sort", List.of(Map.of(
                LogAnalyticsCorrelationConstants.FIELD_TIMESTAMP,
                Map.of("order", "asc"))));

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsCorrelationConstants.LOGS_INDEX, query);

        List<JsonNode> documents = new ArrayList<>();

        if (result == null) {
            return documents;
        }

        JsonNode hits = result.path("hits").path("hits");
        if (!hits.isMissingNode() && hits.isArray()) {
            for (JsonNode hit : hits) {
                documents.add(hit.path("_source"));
            }
        }

        return documents;
    }

    /**
     * Groups the fetched documents by correlationId and builds a
     * {@link CorrelationChain} for each.
     *
     * <p>The {@code topCorrelationIds} list order is preserved in the output
     * so the chains are ranked by document count descending matching the
     * aggregation query order.
     *
     * <p>For each chain:
     * <ul>
     *   <li>Steps are already in chronological order from the sort in the
     *       fetch query.</li>
     *   <li>Steps are capped at MAX_STEPS_PER_CHAIN.</li>
     *   <li>rootCause = message of the first ERROR or WARN step; falls back
     *       to step 1 message when no error/warn exists.</li>
     *   <li>severity = "high" if any ERROR present, "medium" if any WARN,
     *       "low" otherwise.</li>
     *   <li>timestamp = ISO-8601 string of the first document's @timestamp.</li>
     * </ul>
     *
     * @param topCorrelationIds ordered list of correlationId strings
     * @param documents         all documents for those correlationIds, sorted by timestamp ASC
     * @return list of built chains in topCorrelationIds order
     */
    private List<CorrelationChain> buildChains(
            List<String> topCorrelationIds, List<JsonNode> documents) {

        // Group documents by correlationId preserving timestamp order
        Map<String, List<JsonNode>> grouped = new HashMap<>();
        for (JsonNode doc : documents) {
            String corrId = doc.path("correlationId").asText("");
            if (!corrId.isBlank()) {
                grouped.computeIfAbsent(corrId, k -> new ArrayList<>()).add(doc);
            }
        }

        List<CorrelationChain> chains = new ArrayList<>();

        // Build chains in topCorrelationIds order to preserve rank
        for (String correlationId : topCorrelationIds) {
            List<JsonNode> chainDocs = grouped.getOrDefault(correlationId, new ArrayList<>());

            if (chainDocs.isEmpty()) {
                continue;
            }

            // Cap steps to MAX_STEPS_PER_CHAIN
            List<JsonNode> cappedDocs = chainDocs.size() > LogAnalyticsCorrelationConstants.MAX_STEPS_PER_CHAIN
                    ? chainDocs.subList(0, LogAnalyticsCorrelationConstants.MAX_STEPS_PER_CHAIN)
                    : chainDocs;

            List<CorrelationStep> steps = buildSteps(cappedDocs);
            String severity = deriveSeverity(cappedDocs);
            String rootCause = deriveRootCause(cappedDocs);
            String timestamp = deriveTimestamp(chainDocs.get(0));

            chains.add(new CorrelationChain(
                    correlationId,
                    rootCause,
                    severity,
                    timestamp,
                    chainDocs.size(),
                    steps));
        }

        return chains;
    }

    /**
     * Builds the ordered step list from documents.
     * Step numbers start at 1 and increment by 1.
     *
     * <p>Example: 3 documents produce steps 1, 2, 3.
     *
     * @param docs document list sorted by timestamp ASC
     * @return step list
     */
    private List<CorrelationStep> buildSteps(List<JsonNode> docs) {
        List<CorrelationStep> steps = new ArrayList<>();
        int stepNumber = 1;

        for (JsonNode doc : docs) {
            String source = doc.path(LogAnalyticsCorrelationConstants.FIELD_SOURCE).asText("");
            String message = doc.path(LogAnalyticsCorrelationConstants.FIELD_MESSAGE).asText("");
            String level = doc.path("level").asText(LogAnalyticsCorrelationConstants.LEVEL_INFO);

            steps.add(new CorrelationStep(stepNumber, source, message, level));
            stepNumber++;
        }

        return steps;
    }

    /**
     * Derives the chain severity from the highest level present in the documents.
     *
     * <p>Priority: ERROR > WARN > INFO/DEBUG
     * <ul>
     *   <li>Any ERROR document -- severity = "high"</li>
     *   <li>Any WARN, no ERROR -- severity = "medium"</li>
     *   <li>Only INFO/DEBUG    -- severity = "low"</li>
     * </ul>
     *
     * @param docs all documents in the chain
     * @return severity string matching frontend getSeverityBadge keys
     */
    private String deriveSeverity(List<JsonNode> docs) {
        boolean hasError = false;
        boolean hasWarn = false;

        for (JsonNode doc : docs) {
            String level = doc.path("level").asText("");
            if (LogAnalyticsCorrelationConstants.LEVEL_ERROR.equalsIgnoreCase(level)) {
                hasError = true;
                break;
            }
            if (LogAnalyticsCorrelationConstants.LEVEL_WARN.equalsIgnoreCase(level)) {
                hasWarn = true;
            }
        }

        if (hasError) {
            return LogAnalyticsCorrelationConstants.SEVERITY_HIGH;
        }
        if (hasWarn) {
            return LogAnalyticsCorrelationConstants.SEVERITY_MEDIUM;
        }
        return LogAnalyticsCorrelationConstants.SEVERITY_LOW;
    }

    /**
     * Derives the root cause from the first ERROR or WARN document message.
     * Falls back to the first document message when no error/warn exists.
     *
     * <p>The root cause represents the originating failure that triggered
     * the cascade. The first ERROR or WARN in chronological order is the
     * most likely trigger.
     *
     * <p>Example: chain has INFO -> ERROR -> WARN -> INFO
     * -- rootCause = message of the ERROR document (first error/warn)
     *
     * @param docs all documents sorted by timestamp ASC
     * @return root cause message string
     */
    private String deriveRootCause(List<JsonNode> docs) {
        for (JsonNode doc : docs) {
            String level = doc.path("level").asText("");
            if (LogAnalyticsCorrelationConstants.LEVEL_ERROR.equalsIgnoreCase(level)
                    || LogAnalyticsCorrelationConstants.LEVEL_WARN.equalsIgnoreCase(level)) {
                String message = doc.path(LogAnalyticsCorrelationConstants.FIELD_MESSAGE).asText("");
                if (!message.isBlank()) {
                    return message;
                }
            }
        }
        // Fallback: first document message regardless of level
        return docs.get(0).path(LogAnalyticsCorrelationConstants.FIELD_MESSAGE).asText("Unknown");
    }

    /**
     * Derives the ISO-8601 timestamp string from the first document's
     *
     * @param firstDoc the chronologically earliest document in the chain
     * @return ISO-8601 timestamp string
     * @timestamp field (epoch ms).
     *
     * <p>Example: @timestamp = 1742298900000
     * -- returns "2026-03-18T10:15:00Z"
     */
    private String deriveTimestamp(JsonNode firstDoc) {
        long epochMs = firstDoc.path(LogAnalyticsCorrelationConstants.FIELD_TIMESTAMP).asLong(0L);
        if (epochMs == 0L) {
            return "";
        }
        return Instant.ofEpochMilli(epochMs).toString();
    }
}