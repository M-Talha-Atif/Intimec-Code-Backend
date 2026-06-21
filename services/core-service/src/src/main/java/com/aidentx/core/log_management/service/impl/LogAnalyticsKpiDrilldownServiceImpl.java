package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsKpiDrilldownConstants;
import com.aidentx.core.log_management.model.dto.analytics.KpiDrilldownResponse;
import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.aidentx.core.log_management.service.LogAnalyticsKpiDrilldownService;
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
 * Default implementation of {@link LogAnalyticsKpiDrilldownService}.
 *
 * <p>Level and time filter rules per drilldown type:
 * <ul>
 *   <li><b>total_errors</b>   -- ERROR+WARN only, time window based on range</li>
 *   <li><b>critical</b>       -- ERROR only, time window based on range</li>
 *   <li><b>slow_responses</b> -- ERROR+WARN only, time window based on range</li>
 *   <li><b>recurring</b>      -- ERROR+WARN only, time window based on range</li>
 * </ul>
 *
 * <p>When range = "all", the timestamp range filter is omitted entirely
 * so all stored log documents are considered, matching the behaviour of the
 * KPI computation which also uses range=all for the global counts.
 *
 * <p>When range = "24h", "30d", "60d", or "90d", the appropriate timestamp
 * lower bound is applied consistently across all drilldown types.
 */
@Service
public class LogAnalyticsKpiDrilldownServiceImpl implements LogAnalyticsKpiDrilldownService {

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    public LogAnalyticsKpiDrilldownServiceImpl(
            OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger(
                "LogAnalyticsKpiDrilldownService", "log-analytics");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KpiDrilldownResponse fetchDrilldown(
            String type, int page, int size, String range) {

        logger.info().message(LogAnalyticsKpiDrilldownConstants.LOG_QUERY_STARTED)
                .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_TYPE, type)
                .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_PAGE, page)
                .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_SIZE, size)
                .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_RANGE, range != null ? range : "default")
                .log();

        return switch (type) {
            case LogAnalyticsKpiDrilldownConstants.TYPE_TOTAL_ERRORS -> fetchTotalErrors(page, size, range);
            case LogAnalyticsKpiDrilldownConstants.TYPE_CRITICAL -> fetchCriticalFailures(page, size, range);
            case LogAnalyticsKpiDrilldownConstants.TYPE_SLOW_RESPONSES -> fetchSlowResponses(page, size, range);
            case LogAnalyticsKpiDrilldownConstants.TYPE_RECURRING -> fetchRecurringPatterns(page, size, range);
            default -> throw new IllegalArgumentException(
                    LogAnalyticsKpiDrilldownConstants.VALIDATION_TYPE_REQUIRED);
        };
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
        if (range == null) {
            // Default to 24h when range not specified
            return System.currentTimeMillis() - LogAnalyticsKpiDrilldownConstants.TWENTY_FOUR_HOURS_MS;
        }

        if (LogAnalyticsKpiDrilldownConstants.RANGE_ALL.equalsIgnoreCase(range)) {
            return null;
        }

        long now = System.currentTimeMillis();
        long durationMs;

        switch (range) {
            case LogAnalyticsKpiDrilldownConstants.RANGE_24H:
                durationMs = LogAnalyticsKpiDrilldownConstants.TWENTY_FOUR_HOURS_MS;
                break;
            case LogAnalyticsKpiDrilldownConstants.RANGE_30D:
                durationMs = LogAnalyticsKpiDrilldownConstants.THIRTY_DAYS_MS;
                break;
            case LogAnalyticsKpiDrilldownConstants.RANGE_60D:
                durationMs = LogAnalyticsKpiDrilldownConstants.SIXTY_DAYS_MS;
                break;
            case LogAnalyticsKpiDrilldownConstants.RANGE_90D:
                durationMs = LogAnalyticsKpiDrilldownConstants.NINETY_DAYS_MS;
                break;
            default:
                // Default to 24h if range is unrecognized
                durationMs = LogAnalyticsKpiDrilldownConstants.TWENTY_FOUR_HOURS_MS;
        }

        return now - durationMs;
    }

    // -------------------------------------------------------------------------
    // total_errors drilldown
    // -------------------------------------------------------------------------

    /**
     * Fetches ERROR and WARN level documents.
     *
     * <p>Time filter behaviour based on range parameter:
     * <ul>
     *   <li>{@code range=24h} -- restricts to last 24 hours</li>
     *   <li>{@code range=30d} -- restricts to last 30 days</li>
     *   <li>{@code range=60d} -- restricts to last 60 days</li>
     *   <li>{@code range=90d} -- restricts to last 90 days</li>
     *   <li>{@code range=all} -- no date filter, returns all stored documents</li>
     * </ul>
     *
     * <p>Level filter: ERROR and WARN only. These are the levels that indicate
     * an operation failed or is at risk of failing.
     *
     * @param page  zero-based page number
     * @param size  page size
     * @param range date range filter
     * @return paginated drilldown response
     */
    private KpiDrilldownResponse fetchTotalErrors(int page, int size, String range) {
        List<Object> mustClauses = new ArrayList<>();

        // Level filter -- total_errors KPI counts ERROR and WARN
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsKpiDrilldownConstants.FIELD_LEVEL_KEYWORD,
                LogAnalyticsKpiDrilldownConstants.TOTAL_ERRORS_LEVELS)));

        Long timestampLowerBound = getTimestampLowerBound(range);
        if (timestampLowerBound != null) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsKpiDrilldownConstants.FIELD_TIMESTAMP,
                    Map.of("gte", timestampLowerBound))));
        }

        return executeAndBuild(
                LogAnalyticsKpiDrilldownConstants.TYPE_TOTAL_ERRORS,
                buildPagedQuery(Map.of("bool", Map.of("must", mustClauses)), page, size),
                page, size);
    }

    // -------------------------------------------------------------------------
    // critical drilldown
    // -------------------------------------------------------------------------

    /**
     * Fetches ERROR level documents.
     *
     * <p>Time filter behaviour based on range parameter:
     * <ul>
     *   <li>{@code range=24h} -- restricts to last 24 hours</li>
     *   <li>{@code range=30d} -- restricts to last 30 days</li>
     *   <li>{@code range=60d} -- restricts to last 60 days</li>
     *   <li>{@code range=90d} -- restricts to last 90 days</li>
     *   <li>{@code range=all} -- no date filter, returns all stored documents</li>
     * </ul>
     *
     * <p>Level filter: ERROR only. These are critical failures requiring attention.
     *
     * @param page  zero-based page number
     * @param size  page size
     * @param range date range filter
     * @return paginated drilldown response
     */
    private KpiDrilldownResponse fetchCriticalFailures(int page, int size, String range) {
        List<Object> mustClauses = new ArrayList<>();

        // Level filter -- critical failures KPI counts ERROR only
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsKpiDrilldownConstants.FIELD_LEVEL_KEYWORD,
                LogAnalyticsKpiDrilldownConstants.CRITICAL_LEVELS)));

        Long timestampLowerBound = getTimestampLowerBound(range);
        if (timestampLowerBound != null) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsKpiDrilldownConstants.FIELD_TIMESTAMP,
                    Map.of("gte", timestampLowerBound))));
        }

        return executeAndBuild(
                LogAnalyticsKpiDrilldownConstants.TYPE_CRITICAL,
                buildPagedQuery(Map.of("bool", Map.of("must", mustClauses)), page, size),
                page, size);
    }

    // -------------------------------------------------------------------------
    // slow_responses drilldown
    // -------------------------------------------------------------------------

    /**
     * Fetches documents where {@code details.executionTime} exceeds the average.
     *
     * <p>Level filter: ERROR and WARN only.
     *
     * <p>Time filter behaviour based on range parameter:
     * <ul>
     *   <li>{@code range=24h} -- restricts to last 24 hours for both avg computation and doc fetch</li>
     *   <li>{@code range=30d} -- restricts to last 30 days</li>
     *   <li>{@code range=60d} -- restricts to last 60 days</li>
     *   <li>{@code range=90d} -- restricts to last 90 days</li>
     *   <li>{@code range=all} -- no date filter, uses all documents</li>
     * </ul>
     *
     * <p>Two-step process:
     * <ol>
     *   <li>Pre-aggregation: compute avg(details.executionTime) across all
     *       documents that have that field, applying time filter based on range.</li>
     *   <li>Document fetch: return documents where executionTime > computed avg.</li>
     * </ol>
     *
     * @param page  zero-based page number
     * @param size  page size
     * @param range date range filter
     * @return paginated drilldown response
     */
    private KpiDrilldownResponse fetchSlowResponses(int page, int size, String range) {
        double avgExecutionTime = computeAverageExecutionTime(range);

        logger.info().message("Slow responses threshold computed")
                .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_AVG_EXEC_TIME, avgExecutionTime)
                .log();

        if (avgExecutionTime == 0.0) {
            return buildEmptyResponse(
                    LogAnalyticsKpiDrilldownConstants.TYPE_SLOW_RESPONSES, page, size);
        }

        logger.info().message("Slow responses threshold computed")
        .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_AVG_EXEC_TIME, avgExecutionTime)
        .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_RANGE, range)
        .log();

        // Level filter + executionTime threshold combined
        List<Object> mustClauses = new ArrayList<>();
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsKpiDrilldownConstants.FIELD_LEVEL_KEYWORD,
                LogAnalyticsKpiDrilldownConstants.SLOW_RESPONSES_LEVELS)));
        mustClauses.add(Map.of("range", Map.of(
                LogAnalyticsKpiDrilldownConstants.FIELD_EXECUTION_TIME,
                Map.of("gt", avgExecutionTime))));

        Long timestampLowerBound = getTimestampLowerBound(range);
        if (timestampLowerBound != null) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsKpiDrilldownConstants.FIELD_TIMESTAMP,
                    Map.of("gte", timestampLowerBound))));
        }

        return executeAndBuild(
                LogAnalyticsKpiDrilldownConstants.TYPE_SLOW_RESPONSES,
                buildPagedQueryWithSort(
                        Map.of("bool", Map.of("must", mustClauses)),
                        page, size,
                        LogAnalyticsKpiDrilldownConstants.FIELD_EXECUTION_TIME,
                        LogAnalyticsKpiDrilldownConstants.SORT_DESC),
                page, size);
    }

    /**
     * Computes the average execution time via a size=0 avg aggregation.
     *
     * <p>Scopes to documents that have the {@code details.executionTime} field.
     * Applies time filter based on range parameter.
     *
     * @param range date range filter
     * @return average execution time in milliseconds; 0.0 when no data
     */
    private double computeAverageExecutionTime(String range) {
        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);

        // Build query with optional timestamp filter
        Long timestampLowerBound = getTimestampLowerBound(range);
        if (timestampLowerBound != null) {
            query.put("query", Map.of(
                    "bool", Map.of(
                            "must", List.of(
                                    Map.of("exists", Map.of("field", LogAnalyticsKpiDrilldownConstants.FIELD_EXECUTION_TIME)),
                                    Map.of("range", Map.of(
                                            LogAnalyticsKpiDrilldownConstants.FIELD_TIMESTAMP,
                                            Map.of("gte", timestampLowerBound)))))));
        } else {
            query.put("query", Map.of(
                    "exists", Map.of("field", LogAnalyticsKpiDrilldownConstants.FIELD_EXECUTION_TIME)));
        }

        query.put("aggs", Map.of(
                LogAnalyticsKpiDrilldownConstants.AGG_AVG_EXECUTION_TIME,
                Map.of("avg", Map.of(
                        "field", LogAnalyticsKpiDrilldownConstants.FIELD_EXECUTION_TIME))));

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsKpiDrilldownConstants.LOGS_INDEX, query);

        if (result == null) {
            return 0.0;
        }

        return result
                .path("aggregations")
                .path(LogAnalyticsKpiDrilldownConstants.AGG_AVG_EXECUTION_TIME)
                .path("value")
                .asDouble(0.0);
    }

    // -------------------------------------------------------------------------
    // recurring drilldown
    // -------------------------------------------------------------------------

    /**
     * Fetches documents whose message text appears 3+ times in the index.
     *
     * <p>Level filter: ERROR and WARN only.
     *
     * <p>Time filter behaviour based on range parameter:
     * <ul>
     *   <li>{@code range=24h} -- restricts to last 24 hours for both aggregation and doc fetch</li>
     *   <li>{@code range=30d} -- restricts to last 30 days</li>
     *   <li>{@code range=60d} -- restricts to last 60 days</li>
     *   <li>{@code range=90d} -- restricts to last 90 days</li>
     *   <li>{@code range=all} -- no date filter, uses all documents</li>
     * </ul>
     *
     * <p>Two-step process:
     * <ol>
     *   <li>Pre-aggregation: terms agg with min_doc_count to find recurring messages.</li>
     *   <li>Document fetch: terms query filtering to those message strings.</li>
     * </ol>
     *
     * @param page  zero-based page number
     * @param size  page size
     * @param range date range filter
     * @return paginated drilldown response
     */
    private KpiDrilldownResponse fetchRecurringPatterns(int page, int size, String range) {
        List<String> recurringMessages = fetchRecurringMessageStrings(range);

        logger.info().message("Recurring patterns drilldown messages found")
                .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_RECURRING_COUNT,
                        recurringMessages.size())
                .log();

        if (recurringMessages.isEmpty()) {
            return buildEmptyResponse(
                    LogAnalyticsKpiDrilldownConstants.TYPE_RECURRING, page, size);
        }

        List<Object> mustClauses = new ArrayList<>();
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsKpiDrilldownConstants.FIELD_MESSAGE_KEYWORD,
                recurringMessages)));
        mustClauses.add(Map.of("terms", Map.of(
                LogAnalyticsKpiDrilldownConstants.FIELD_LEVEL_KEYWORD,
                LogAnalyticsKpiDrilldownConstants.RECURRING_LEVELS)));

        Long timestampLowerBound = getTimestampLowerBound(range);
        if (timestampLowerBound != null) {
            mustClauses.add(Map.of("range", Map.of(
                    LogAnalyticsKpiDrilldownConstants.FIELD_TIMESTAMP,
                    Map.of("gte", timestampLowerBound))));
        }

        return executeAndBuild(
                LogAnalyticsKpiDrilldownConstants.TYPE_RECURRING,
                buildPagedQuery(Map.of("bool", Map.of("must", mustClauses)), page, size),
                page, size);
    }

    /**
     * Issues a terms aggregation to find message strings appearing
     * {@link LogAnalyticsKpiDrilldownConstants#RECURRING_MIN_OCCURRENCES}+
     * times within the specified time window.
     *
     * @param range date range filter
     * @return list of recurring message strings; empty when none found
     */
    private List<String> fetchRecurringMessageStrings(String range) {
        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);

        // Build query with optional timestamp filter
        Long timestampLowerBound = getTimestampLowerBound(range);
        if (timestampLowerBound != null) {
            query.put("query", Map.of(
                    "bool", Map.of(
                            "must", List.of(
                                    Map.of("terms", Map.of(
                                            LogAnalyticsKpiDrilldownConstants.FIELD_LEVEL_KEYWORD,
                                            LogAnalyticsKpiDrilldownConstants.RECURRING_LEVELS)),
                                    Map.of("range", Map.of(
                                            LogAnalyticsKpiDrilldownConstants.FIELD_TIMESTAMP,
                                            Map.of("gte", timestampLowerBound)))))));
        } else {
            query.put("query", Map.of(
                    "terms", Map.of(
                            LogAnalyticsKpiDrilldownConstants.FIELD_LEVEL_KEYWORD,
                            LogAnalyticsKpiDrilldownConstants.RECURRING_LEVELS)));
        }

        query.put("aggs", Map.of(
                LogAnalyticsKpiDrilldownConstants.AGG_RECURRING_MESSAGES,
                Map.of("terms", Map.of(
                        "field", LogAnalyticsKpiDrilldownConstants.FIELD_MESSAGE_KEYWORD,
                        "size", LogAnalyticsKpiDrilldownConstants.RECURRING_MESSAGES_FETCH_SIZE,
                        "min_doc_count", LogAnalyticsKpiDrilldownConstants.RECURRING_MIN_OCCURRENCES))));

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsKpiDrilldownConstants.LOGS_INDEX, query);

        List<String> messages = new ArrayList<>();
        if (result == null) return messages;

        JsonNode buckets = result
                .path("aggregations")
                .path(LogAnalyticsKpiDrilldownConstants.AGG_RECURRING_MESSAGES)
                .path("buckets");

        if (!buckets.isMissingNode() && buckets.isArray()) {
            for (JsonNode bucket : buckets) {
                String message = bucket.path("key").asText();
                if (!message.isBlank()) {
                    messages.add(message);
                }
            }
        }

        return messages;
    }

    // -------------------------------------------------------------------------
    // Shared query builders
    // -------------------------------------------------------------------------

    /**
     * Wraps a filter into a paged request sorted by timestamp descending.
     *
     * @param filter the query clause
     * @param page   zero-based page
     * @param size   page size
     * @return full OpenSearch request body
     */
    private Map<String, Object> buildPagedQuery(
            Map<String, Object> filter, int page, int size) {
        return buildPagedQueryWithSort(
                filter, page, size,
                LogAnalyticsKpiDrilldownConstants.FIELD_TIMESTAMP,
                LogAnalyticsKpiDrilldownConstants.SORT_DESC);
    }

    /**
     * Wraps a filter into a paged request with a caller-specified sort.
     * Uses from+size pagination. {@code from = page * size}.
     *
     * @param filter    the query clause
     * @param page      zero-based page
     * @param size      page size
     * @param sortField field to sort by
     * @param sortOrder "desc" or "asc"
     * @return full OpenSearch request body
     */
    private Map<String, Object> buildPagedQueryWithSort(
            Map<String, Object> filter,
            int page, int size,
            String sortField, String sortOrder) {
        Map<String, Object> query = new HashMap<>();
        query.put("size", size);
        query.put("from", page * size);
        query.put("sort", List.of(Map.of(sortField, Map.of("order", sortOrder))));
        query.put("query", filter);
        query.put("track_total_hits", true);
        return query;
    }

    // -------------------------------------------------------------------------
    // Response builders
    // -------------------------------------------------------------------------

    /**
     * Executes the query and maps hits to a {@link KpiDrilldownResponse}.
     *
     * @param type  drilldown type for response metadata
     * @param query full OpenSearch request body
     * @param page  current page for pagination metadata
     * @param size  page size for pagination metadata
     * @return populated response
     */
    private KpiDrilldownResponse executeAndBuild(
            String type, Map<String, Object> query, int page, int size) {

        JsonNode result = openSearchService.searchDocuments(
                LogAnalyticsKpiDrilldownConstants.LOGS_INDEX, query);

        if (result == null) {
            logger.warn().message(LogAnalyticsKpiDrilldownConstants.LOG_OPENSEARCH_NULL)
                    .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_TYPE, type)
                    .log();
            return buildEmptyResponse(type, page, size);
        }

        long totalElements = result
                .path("hits").path("total").path("value").asLong(0L);

        JsonNode hits = result.path("hits").path("hits");
        List<LogEntry> entries = new ArrayList<>();

        if (!hits.isMissingNode() && hits.isArray()) {
            for (JsonNode hit : hits) {
                entries.add(mapHitToLogEntry(hit));
            }
        }

        logger.info().message(LogAnalyticsKpiDrilldownConstants.LOG_QUERY_COMPLETE)
                .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_TOTAL_ELEMENTS,
                        totalElements)
                .detail(LogAnalyticsKpiDrilldownConstants.LOG_KEY_TYPE, type)
                .log();

        return new KpiDrilldownResponse(
                type,
                entries,
                buildPaginationMetadata(page, size, totalElements));
    }

    /**
     * Maps a single OpenSearch hit to a {@link LogEntry}.
     * Document ID is read from hit._id, all other fields from hit._source.
     *
     * @param hit a single element from hits.hits
     * @return populated LogEntry
     */
    private LogEntry mapHitToLogEntry(JsonNode hit) {
        JsonNode source = hit.path("_source");
        LogEntry entry = new LogEntry();
        entry.setId(hit.path("_id").asText(""));
        entry.setTimestamp(source.path(
                LogAnalyticsKpiDrilldownConstants.FIELD_TIMESTAMP).asLong(0L));
        entry.setLevel(source.path("level").asText(""));
        entry.setSource(source.path("source").asText(""));
        entry.setMessage(source.path("message").asText(""));
        entry.setCategory(source.path("category").asText(null));
        entry.setCorrelationId(source.path("correlationId").asText(null));
        entry.setMicroservice(source.path("microservice").asText(null));
        return entry;
    }

    /**
     * Builds {@link PaginationMetadata} from total hits and page parameters.
     *
     * @param page          zero-based current page
     * @param size          page size
     * @param totalElements total document count from OpenSearch
     * @return populated pagination metadata
     */
    private PaginationMetadata buildPaginationMetadata(
            int page, int size, long totalElements) {
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
     * Returns an empty response with zero-filled pagination.
     *
     * @param type drilldown type for response metadata
     * @param page current page
     * @param size page size
     * @return empty response
     */
    private KpiDrilldownResponse buildEmptyResponse(
            String type, int page, int size) {
        return new KpiDrilldownResponse(
                type, new ArrayList<>(),
                buildPaginationMetadata(page, size, 0L));
    }
}