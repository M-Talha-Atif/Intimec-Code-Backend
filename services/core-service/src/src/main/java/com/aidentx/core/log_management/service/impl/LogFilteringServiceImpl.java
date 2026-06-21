package com.aidentx.core.log_management.service.impl;

import com.aidentx.core.log_management.config.AppConstants;
import com.aidentx.core.log_management.model.dto.LogSearchRequest;
import com.aidentx.core.log_management.model.dto.LogSearchResponse;
import com.aidentx.core.log_management.model.pojo.AggregationBucket;
import com.aidentx.core.log_management.service.Helpers.LogFilteringHelper;
import com.aidentx.core.log_management.service.LogFilteringService;
import com.aidentx.core.opensearch.service.OpenSearchService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of LogFilteringService for advanced OpenSearch-based log
 * querying.
 * Handles complex query building, result parsing, and aggregation processing.
 */
@Service
public class LogFilteringServiceImpl implements LogFilteringService {

    private final OpenSearchService openSearchService;
    private final IUnifiedLogger logger;

    // Maximum allowed page size to prevent memory issues
    private static final int MAX_PAGE_SIZE = 1000;

    public LogFilteringServiceImpl(OpenSearchService openSearchService,
            UnifiedLoggerFactory loggerFactory) {
        this.openSearchService = openSearchService;
        this.logger = loggerFactory.getLogger("LogFilteringService", "log-filtering");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogSearchResponse searchLogs(LogSearchRequest searchRequest) {
        logger.trace().message(AppConstants.LOG_STARTING_LOG_SEARCH)
                .detail("request", searchRequest.toString())
                .log();

        try {
            // Validate the search request
            validateSearchRequest(searchRequest);

            // Build OpenSearch query
            Map<String, Object> openSearchQuery = LogFilteringHelper.buildOpenSearchQuery(searchRequest);

            // Execute search
            JsonNode searchResults = openSearchService.searchDocuments(LogFilteringHelper.LOGS_INDEX, openSearchQuery);

            if (searchResults == null) {
                logger.warn().message(AppConstants.LOG_OPENSEARCH_RETURNED_NULL_RESULTS).log();
                return LogFilteringHelper.createEmptyResponse(searchRequest);
            }

            // Parse results
            LogSearchResponse response = LogFilteringHelper.parseSearchResults(searchResults, searchRequest);

            logger.info().message(AppConstants.LOG_LOG_SEARCH_COMPLETED_SUCCESSFULLY)
                    .detail("totalResults", response.getPagination().getTotalElements())
                    .detail("page", searchRequest.getPage())
                    .detail("size", searchRequest.getSize())
                    .log();

            return response;

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_EXECUTE_LOG_SEARCH_IMPL)
                    .detail("request", searchRequest.toString())
                    .log();
            throw new RuntimeException("Failed to search logs", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, List<AggregationBucket>> getFilterAggregations(List<String> fields) {
        logger.trace().message(AppConstants.LOG_GETTING_FILTER_AGGREGATIONS)
                .detail("fields", fields)
                .log();

        try {
            Map<String, Object> query = LogFilteringHelper.buildAggregationQuery(fields);
            JsonNode results = openSearchService.searchDocuments(LogFilteringHelper.LOGS_INDEX, query);

            if (results == null) {
                logger.warn().message(AppConstants.LOG_OPENSEARCH_RETURNED_NULL_AGGREGATION_RESULTS).log();
                return Collections.emptyMap();
            }

            return LogFilteringHelper.parseAggregationResults(results, fields);

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_GET_FILTER_AGGREGATIONS_IMPL)
                    .detail("fields", fields)
                    .log();
            throw new RuntimeException("Failed to get aggregations", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LogSearchResponse getLogStatistics(LogSearchRequest searchRequest) {
        logger.trace().message(AppConstants.LOG_GETTING_LOG_STATISTICS)
                .detail("request", searchRequest.toString())
                .log();

        try {
            // Set aggregateOnly flag and size to 0 for stats-only query
            LogSearchRequest statsRequest = new LogSearchRequest(searchRequest);
            statsRequest.setAggregateOnly(true);
            statsRequest.setSize(0);

            return searchLogs(statsRequest);

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_GET_LOG_STATISTICS_IMPL)
                    .detail("request", searchRequest.toString())
                    .log();
            throw new RuntimeException("Failed to get log statistics", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getSearchSuggestions(String fieldName, String partialValue, int maxSuggestions) {
        logger.trace().message(AppConstants.LOG_GETTING_SEARCH_SUGGESTIONS)
                .detail("fieldName", fieldName)
                .detail("partialValue", partialValue)
                .detail("maxSuggestions", maxSuggestions)
                .log();

        try {
            if (partialValue == null || partialValue.trim().isEmpty()) {
                return Collections.emptyList();
            }

            Map<String, Object> query = LogFilteringHelper.buildSuggestionQuery(fieldName, partialValue,
                    maxSuggestions);
            JsonNode results = openSearchService.searchDocuments(LogFilteringHelper.LOGS_INDEX, query);

            if (results == null) {
                return Collections.emptyList();
            }

            return LogFilteringHelper.parseSuggestionResults(results, fieldName);

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_GET_SEARCH_SUGGESTIONS_IMPL)
                    .detail("fieldName", fieldName)
                    .detail("partialValue", partialValue)
                    .log();
            throw new RuntimeException("Failed to get suggestions", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateSearchRequest(LogSearchRequest searchRequest) {
        if (searchRequest == null) {
            throw new IllegalArgumentException("Search request cannot be null");
        }

        // Validate page and size
        if (searchRequest.getPage() != null && searchRequest.getPage() < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }

        if (searchRequest.getSize() != null && searchRequest.getSize() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Page size cannot exceed " + MAX_PAGE_SIZE);
        }

        // Allow size 0 for aggregate-only queries, otherwise require at least 1
        if (searchRequest.getSize() != null && searchRequest.getSize() < 0) {
            throw new IllegalArgumentException("Page size cannot be negative");
        }

        if (searchRequest.getSize() != null && !searchRequest.getAggregateOnly() && searchRequest.getSize() < 1) {
            throw new IllegalArgumentException("Page size must be at least 1 for document queries");
        }

        // Validate date ranges
        LogFilteringHelper.validateDateRange(searchRequest.getDateFrom(), searchRequest.getDateTo(), "date");
        LogFilteringHelper.validateDateRange(searchRequest.getPinnedDateFrom(), searchRequest.getPinnedDateTo(),
                "pinnedDate");
        LogFilteringHelper.validateDateRange(searchRequest.getResolvedDateFrom(), searchRequest.getResolvedDateTo(),
                "resolvedDate");

        // Validate sort field
        if (searchRequest.getSortBy() != null && !LogFilteringHelper.isValidSortField(searchRequest.getSortBy())) {
            throw new IllegalArgumentException("Invalid sort field: " + searchRequest.getSortBy());
        }
    }
}
