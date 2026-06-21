package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsKpisConstants;
import com.aidentx.core.log_management.model.dto.analytics.LogAnalyticsKpisResponse;
import com.aidentx.core.log_management.service.LogAnalyticsKpisService;
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
 * Default implementation of {@link LogAnalyticsKpisService}.
 *
 * <p>Issues one OpenSearch request per call carrying all five named aggregations.
 * Only one HTTP round-trip to OpenSearch is made regardless of the number of KPIs.
 *
 * <p>Index reality (verified from actual documents and mapping):
 * <ul>
 *   <li>No tenantId field exists -- queries run across the full index.</li>
 *   <li>level is uppercase: ERROR, WARN, INFO, DEBUG.</li>
 *   <li>@timestamp is epoch milliseconds (long).</li>
 *   <li>isResolved and resolvedAt are written by LogPinningHelper at resolution time.
 *       Both are stored as epoch milliseconds longs. MTTR returns 0.0 until
 *       at least one log has been resolved.</li>
 *   <li>details.executionTime is not consistently present -- avgResponseTimeMs
 *       returns 0.0 until populated.</li>
 * </ul>
 *
 * <p>Aggregation strategy:
 * <ol>
 *   <li><b>totalErrors</b> -- filter agg on level IN [ERROR, WARN] with
 *       timestamp lower bound based on range parameter.</li>
 *   <li><b>criticalFailures</b> -- filter agg on level = ERROR with same
 *       timestamp bound as totalErrors.</li>
 *   <li><b>avgResponseTimeMs</b> -- avg metric on details.executionTime with
 *       same timestamp bound.</li>
 *   <li><b>mttrMinutes</b> -- scripted_metric on resolved documents with
 *       same timestamp bound. Returns 0.0 because isResolved does not exist
 *       in the index yet.</li>
 *   <li><b>recurringPatterns</b> -- filter on ERROR + nested terms on
 *       message.keyword with same timestamp bound. Service post-processes
 *       buckets to count those with doc_count >= RECURRING_MIN_OCCURRENCES.</li>
 * </ol>
 */
@Service
public class LogAnalyticsKpisServiceImpl implements LogAnalyticsKpisService {

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsKpisServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsKpisService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     *
     * <p>The tenantId parameter is accepted for API consistency with the audit
     * analytics layer but is not applied as a query filter because the main-logs
     * index has no tenantId field at the document level. All aggregations run
     * across the full index.
     */
    @Override
    public LogAnalyticsKpisResponse computeKpis(String tenantId, String range) {
        logger.info().message(LogAnalyticsKpisConstants.LOG_KPI_COMPUTATION_STARTED)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_TENANT_ID, tenantId)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_RANGE, range)
                .log();

        Map<String, Object> query = buildKpiQuery(range);

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsKpisConstants.LOGS_INDEX, query);

        if (result == null) {
            logger.warn().message(LogAnalyticsKpisConstants.LOG_KPI_OPENSEARCH_NULL)
                    .detail(LogAnalyticsKpisConstants.LOG_KEY_TENANT_ID, tenantId)
                    .log();
            return new LogAnalyticsKpisResponse(0L, 0L, 0.0, 0.0, 0L);
        }

        JsonNode aggs = result.path("aggregations");

        long totalErrors = parseTotalErrors(aggs);
        long criticalFailures = parseCriticalFailures(aggs);
        double avgResponseTime = parseAvgResponseTime(aggs);
        double mttrMinutes = parseMttrMinutes(aggs);
        long recurringPatterns = parseRecurringPatterns(aggs);

        logger.info().message(LogAnalyticsKpisConstants.LOG_KPI_COMPUTATION_COMPLETE)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_TENANT_ID, tenantId)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_RANGE, range)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_TOTAL_ERRORS, totalErrors)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_CRITICAL_FAILURES, criticalFailures)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_AVG_RESPONSE_TIME_MS, avgResponseTime)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_MTTR_MINUTES, mttrMinutes)
                .detail(LogAnalyticsKpisConstants.LOG_KEY_RECURRING_PATTERNS, recurringPatterns)
                .log();

        return new LogAnalyticsKpisResponse(
                totalErrors,
                criticalFailures,
                avgResponseTime,
                mttrMinutes,
                recurringPatterns);
    }

    /**
     * Builds the combined OpenSearch query with all five aggregations.
     *
     * <p>The root query is {@code match_all} because there is no tenantId
     * field in the index to scope by. {@code size=0} prevents documents from
     * being returned since only aggregation results are needed.
     *
     * <p>A timestamp filter is applied to all aggregations based on the
     * range parameter. This ensures consistent time windows across all KPIs.
     *
     * @param range date range filter: "24h", "30d", "60d", "90d", or "all"
     * @return OpenSearch request body as a nested Map
     */
    private Map<String, Object> buildKpiQuery(String range) {
        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);
        query.put("query", Map.of("match_all", Map.of()));

        Long timestampLowerBound = getTimestampLowerBound(range);

        Map<String, Object> aggs = new HashMap<>();
        aggs.put(LogAnalyticsKpisConstants.AGG_TOTAL_ERRORS,
                buildTotalErrorsAgg(timestampLowerBound));
        aggs.put(LogAnalyticsKpisConstants.AGG_CRITICAL_FAILURES,
                buildCriticalFailuresAgg(timestampLowerBound));
        aggs.put(LogAnalyticsKpisConstants.AGG_AVG_RESPONSE_TIME,
                buildAvgResponseTimeAgg(timestampLowerBound));
        aggs.put(LogAnalyticsKpisConstants.AGG_MTTR,
                buildMttrAgg(timestampLowerBound));
        aggs.put(LogAnalyticsKpisConstants.AGG_RECURRING_MESSAGES,
                buildRecurringPatternsAgg(timestampLowerBound));

        query.put("aggs", aggs);
        return query;
    }

    /**
     * Returns the timestamp lower bound in milliseconds based on range parameter.
     *
     * <p>When range is "all", returns null (no timestamp filter).
     * Otherwise computes now - range duration.
     *
     * @param range date range filter
     * @return timestamp lower bound in epoch ms, or null for no filter
     */
    private Long getTimestampLowerBound(String range) {
        if (LogAnalyticsKpisConstants.RANGE_ALL.equalsIgnoreCase(range)) {
            return null;
        }

        long now = System.currentTimeMillis();
        long durationMs;

        switch (range) {
            case LogAnalyticsKpisConstants.RANGE_24H:
                durationMs = LogAnalyticsKpisConstants.TWENTY_FOUR_HOURS_MS;
                break;
            case LogAnalyticsKpisConstants.RANGE_30D:
                durationMs = LogAnalyticsKpisConstants.THIRTY_DAYS_MS;
                break;
            case LogAnalyticsKpisConstants.RANGE_60D:
                durationMs = LogAnalyticsKpisConstants.SIXTY_DAYS_MS;
                break;
            case LogAnalyticsKpisConstants.RANGE_90D:
                durationMs = LogAnalyticsKpisConstants.NINETY_DAYS_MS;
                break;
            default:
                // Default to 24h if range is unrecognized
                durationMs = LogAnalyticsKpisConstants.TWENTY_FOUR_HOURS_MS;
        }

        return now - durationMs;
    }

    /**
     * Builds the filter aggregation for total ERROR and WARN events.
     *
     * <p>When timestampLowerBound is null, only level filtering is applied.
     * Otherwise a range filter is added to count only events from the
     * specified time window.
     *
     * <p>Example (timestampLowerBound = 1770891886000):
     * <pre>
     *   "total_errors": {
     *     "filter": {
     *       "bool": {
     *         "must": [
     *           { "terms": { "level.keyword": ["ERROR","WARN"] } },
     *           { "range": { "@timestamp": { "gte": 1770891886000 } } }
     *         ]
     *       }
     *     }
     *   }
     * </pre>
     *
     * @param timestampLowerBound lower bound in epoch ms, null for no filter
     * @return aggregation definition map
     */
    private Map<String, Object> buildTotalErrorsAgg(Long timestampLowerBound) {
        List<Object> mustClauses = new ArrayList<>();

        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsKpisConstants.FIELD_LEVEL_KEYWORD,
                List.of(LogAnalyticsKpisConstants.LEVEL_ERROR,
                        LogAnalyticsKpisConstants.LEVEL_WARN))));

        if (timestampLowerBound != null) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsKpisConstants.FIELD_TIMESTAMP,
                    Map.of("gte", timestampLowerBound))));
        }

        return Map.of("filter", Map.of("bool", Map.of("must", mustClauses)));
    }

    /**
     * Builds the filter aggregation for critical ERROR-level failures.
     *
     * <p>Applies the same timestamp filter as totalErrors for consistent
     * time windows across all KPIs.
     *
     * <p>Example:
     * <pre>
     *   "critical_failures": {
     *     "filter": {
     *       "bool": {
     *         "must": [
     *           { "term": { "level.keyword": "ERROR" } },
     *           { "range": { "@timestamp": { "gte": 1770891886000 } } }
     *         ]
     *       }
     *     }
     *   }
     * </pre>
     *
     * @param timestampLowerBound lower bound in epoch ms, null for no filter
     * @return aggregation definition map
     */
    private Map<String, Object> buildCriticalFailuresAgg(Long timestampLowerBound) {
        List<Object> mustClauses = new ArrayList<>();

        mustClauses.add(Map.of("term", Map.of(
                LogAnalyticsKpisConstants.FIELD_LEVEL_KEYWORD,
                LogAnalyticsKpisConstants.LEVEL_ERROR)));

        if (timestampLowerBound != null) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsKpisConstants.FIELD_TIMESTAMP,
                    Map.of("gte", timestampLowerBound))));
        }

        return Map.of("filter", Map.of("bool", Map.of("must", mustClauses)));
    }

    /**
     * Builds the avg metric aggregation for connector response time.
     *
     * <p>Documents without details.executionTime are excluded automatically
     * by OpenSearch. Returns 0.0 when no documents carry the field.
     *
     * <p>Applies the same timestamp filter for consistent time windows.
     *
     * <p>Example:
     * <pre>
     *   "avg_response_time": {
     *     "filter": {
     *       "range": { "@timestamp": { "gte": 1770891886000 } }
     *     },
     *     "aggs": {
     *       "avg_response": {
     *         "avg": { "field": "details.executionTime" }
     *       }
     *     }
     *   }
     * </pre>
     *
     * @param timestampLowerBound lower bound in epoch ms, null for no filter
     * @return aggregation definition map
     */
    private Map<String, Object> buildAvgResponseTimeAgg(Long timestampLowerBound) {
        Map<String, Object> avgAgg = Map.of("avg", Map.of(
                "field", LogAnalyticsKpisConstants.FIELD_EXECUTION_TIME));

        if (timestampLowerBound != null) {
            return Map.of(
                    "filter", Map.of("range", Map.of(
                            LogAnalyticsKpisConstants.FIELD_TIMESTAMP,
                            Map.of("gte", timestampLowerBound))),
                    "aggs", Map.of("avg_response", avgAgg));
        }

        return avgAgg;
    }

    /**
     * Builds the MTTR aggregation using a filter + scripted_metric combination.
     *
     * <p>The outer filter restricts computation to resolved documents only
     * (isResolved = true), written by LogPinningHelper when a log is resolved.
     *
     * <p>Applies the same timestamp filter for consistent time windows.
     *
     * <p>Both {@code resolvedAt} and {@code @timestamp} are stored as epoch
     * milliseconds longs. The script reads them via {@code doc['field'].value}
     * which returns the raw long for numeric fields -- no .toInstant() call
     * is needed or correct for long-typed fields in OpenSearch/Painless.
     *
     * <p>Script logic:
     * <ul>
     *   <li>map: for each resolved document compute
     *       {@code resolvedAt - @timestamp} in ms and add to state list.</li>
     *   <li>combine: sum the shard-local deltas into a [sum, count] pair.</li>
     *   <li>reduce: sum all shard pairs, divide total sum by total count,
     *       then divide by MS_PER_MINUTE to produce minutes.</li>
     * </ul>
     *
     * @param timestampLowerBound lower bound in epoch ms, null for no filter
     * @return aggregation definition map
     */
    private Map<String, Object> buildMttrAgg(Long timestampLowerBound) {
        Map<String, Object> scriptedMetric = new HashMap<>();

        scriptedMetric.put("init_script", "state.deltas = []");

        scriptedMetric.put("map_script",
                "if (doc['" + LogAnalyticsKpisConstants.FIELD_RESOLVED_AT + "'].size() > 0 " +
                        "&& doc['" + LogAnalyticsKpisConstants.FIELD_TIMESTAMP + "'].size() > 0) { " +
                        "long resolvedMs = doc['" + LogAnalyticsKpisConstants.FIELD_RESOLVED_AT + "'].value; " +
                        "long createdMs  = doc['" + LogAnalyticsKpisConstants.FIELD_TIMESTAMP + "'].value; " +
                        "if (resolvedMs > createdMs) { state.deltas.add(resolvedMs - createdMs); } }");

        scriptedMetric.put("combine_script",
                "double sum = 0; " +
                        "for (d in state.deltas) { sum += d; } " +
                        "return [sum, state.deltas.size()]");

        scriptedMetric.put("reduce_script",
                "double totalSum = 0; long totalCount = 0; " +
                        "for (s in states) { " +
                        "  if (s != null) { totalSum += s[0]; totalCount += (long) s[1]; } " +
                        "} " +
                        "return totalCount > 0 " +
                        "  ? totalSum / totalCount / " + LogAnalyticsKpisConstants.MS_PER_MINUTE +
                        "  : 0");

        Map<String, Object> innerAggs = new HashMap<>();
        innerAggs.put("mttr_script", Map.of("scripted_metric", scriptedMetric));

        List<Object> mustClauses = new ArrayList<>();
        mustClauses.add(Map.of("term", Map.of(
                LogAnalyticsKpisConstants.FIELD_IS_RESOLVED, true)));

        if (timestampLowerBound != null) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsKpisConstants.FIELD_TIMESTAMP,
                    Map.of("gte", timestampLowerBound))));
        }

        return Map.of(
                "filter", Map.of("bool", Map.of("must", mustClauses)),
                "aggs", innerAggs);
    }

    /**
     * Builds the recurring patterns aggregation across error and warn levels.
     *
     * <p>Groups by message.keyword across all documents with level = ERROR or WARN.
     * Service post-processing counts only buckets where
     * doc_count >= {@link LogAnalyticsKpisConstants#RECURRING_MIN_OCCURRENCES}.
     *
     * <p>Applies the same timestamp filter for consistent time windows.
     *
     * @param timestampLowerBound lower bound in epoch ms, null for no filter
     * @return aggregation definition map
     */
    private Map<String, Object> buildRecurringPatternsAgg(Long timestampLowerBound) {
        Map<String, Object> innerAggs = new HashMap<>();
        innerAggs.put(LogAnalyticsKpisConstants.AGG_RECURRING_MESSAGES_INNER,
                Map.of("terms", Map.of(
                        "field", LogAnalyticsKpisConstants.FIELD_MESSAGE_KEYWORD,
                        "size", LogAnalyticsKpisConstants.RECURRING_PATTERNS_AGG_SIZE)));

        List<Object> mustClauses = new ArrayList<>();
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsKpisConstants.FIELD_LEVEL_KEYWORD,
                List.of(LogAnalyticsKpisConstants.LEVEL_ERROR,
                        LogAnalyticsKpisConstants.LEVEL_WARN))));

        if (timestampLowerBound != null) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsKpisConstants.FIELD_TIMESTAMP,
                    Map.of("gte", timestampLowerBound))));
        }

        return Map.of(
                "filter", Map.of("bool", Map.of("must", mustClauses)),
                "aggs", innerAggs);
    }

    /**
     * Parses totalErrors from the filter aggregation doc_count.
     *
     * @param aggs aggregations node from OpenSearch response
     * @return ERROR + WARN count in the effective window
     */
    private long parseTotalErrors(JsonNode aggs) {
        return aggs
                .path(LogAnalyticsKpisConstants.AGG_TOTAL_ERRORS)
                .path("doc_count")
                .asLong(0L);
    }

    /**
     * Parses criticalFailures from the filter aggregation doc_count.
     *
     * @param aggs aggregations node from OpenSearch response
     * @return ERROR-level event count
     */
    private long parseCriticalFailures(JsonNode aggs) {
        return aggs
                .path(LogAnalyticsKpisConstants.AGG_CRITICAL_FAILURES)
                .path("doc_count")
                .asLong(0L);
    }

    /**
     * Parses avgResponseTimeMs from the avg aggregation value node.
     *
     * <p>Returns 0.0 when the value node is null or missing, which happens
     * when no documents in the window carry a details.executionTime field.
     *
     * @param aggs aggregations node from OpenSearch response
     * @return average execution time in milliseconds, or 0.0 if none recorded
     */
    private double parseAvgResponseTime(JsonNode aggs) {
        JsonNode avgAgg = aggs.path(LogAnalyticsKpisConstants.AGG_AVG_RESPONSE_TIME);
        JsonNode valueNode;

        // Handle both filtered and unfiltered aggregation structures
        if (avgAgg.has("avg_response")) {
            valueNode = avgAgg.path("avg_response").path("value");
        } else {
            valueNode = avgAgg.path("value");
        }

        if (valueNode.isNull() || valueNode.isMissingNode()) {
            return 0.0;
        }
        return valueNode.asDouble(0.0);
    }

    /**
     * Parses mttrMinutes from the nested scripted_metric value.
     *
     * <p>Returns 0.0 when isResolved field does not exist in the index,
     * which causes the filter to match zero documents and the reduce script
     * to return 0.
     *
     * @param aggs aggregations node from OpenSearch response
     * @return mean time to resolution in minutes, or 0.0 if no resolved errors
     */
    private double parseMttrMinutes(JsonNode aggs) {
        JsonNode valueNode = aggs
                .path(LogAnalyticsKpisConstants.AGG_MTTR)
                .path("mttr_script")
                .path("value");

        if (valueNode.isNull() || valueNode.isMissingNode()) {
            return 0.0;
        }
        return valueNode.asDouble(0.0);
    }

    /**
     * Counts message buckets with doc_count at or above the recurring threshold.
     *
     * <p>Example with RECURRING_MIN_OCCURRENCES=3:
     * <pre>
     *   bucket "500 Server Error..." -- doc_count=47 -- counted
     *   bucket "LDAP bind failed"    -- doc_count=23 -- counted
     *   bucket "Schema validation"   -- doc_count=15 -- counted
     *   bucket "One-off error"       -- doc_count=2  -- not counted
     *   result = 3
     * </pre>
     *
     * @param aggs aggregations node from OpenSearch response
     * @return count of error messages recurring 3 or more times
     */
    private long parseRecurringPatterns(JsonNode aggs) {
        JsonNode buckets = aggs
                .path(LogAnalyticsKpisConstants.AGG_RECURRING_MESSAGES)
                .path(LogAnalyticsKpisConstants.AGG_RECURRING_MESSAGES_INNER)
                .path("buckets");

        if (buckets.isMissingNode() || !buckets.isArray()) {
            return 0L;
        }

        long count = 0L;
        for (JsonNode bucket : buckets) {
            long docCount = bucket.path("doc_count").asLong(0L);
            if (docCount >= LogAnalyticsKpisConstants.RECURRING_MIN_OCCURRENCES) {
                count++;
            }
        }
        return count;
    }
}