package com.aidentx.core.log_management.controller;

import com.aidentx.core.log_management.config.AppConstants;
import com.aidentx.core.log_management.model.dto.LogSearchRequest;
import com.aidentx.core.log_management.model.dto.LogSearchResponse;
import com.aidentx.core.log_management.model.pojo.AggregationBucket;
import com.aidentx.core.log_management.service.LogFilteringService;
import com.aidentx.logging.service.IUnifiedLogger;
import com.aidentx.logging.service.UnifiedLoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing pinned logger entries.
 * Provides endpoints for retrieving pinned logs.
 */
@RestController
@RequestMapping("/api/v1/logs")
public class LogsFilteringController {

    private final IUnifiedLogger logger;
    private final LogFilteringService logFilteringService;

    /**
     * Constructor with dependency injection.
     *
     * @param logFilteringService the log filtering service
     * @param loggerFactory       the factory to create a logger instance
     */
    public LogsFilteringController(
            LogFilteringService logFilteringService,
            UnifiedLoggerFactory loggerFactory) {
        this.logFilteringService = logFilteringService;
        this.logger = loggerFactory.getLogger("LogsController", AppConstants.LOG_CATEGORY_PINNED_LOGS);
    }

    /**
     * Performs a complex search for logs based on multiple criteria.
     * Supports filtering by fields, global search, date ranges, pagination, and
     * sorting.
     *
     * @param searchRequest the search criteria and configuration
     * @return paginated search results with metadata and aggregations
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchLogs(@RequestBody LogSearchRequest searchRequest) {
        logger.trace().message(AppConstants.LOG_ADVANCED_LOG_SEARCH_REQUEST_RECEIVED)
                .detail("request", searchRequest.toString())
                .log();

        try {
            LogSearchResponse response = logFilteringService.searchLogs(searchRequest);

            logger.info().message(AppConstants.LOG_ADVANCED_LOG_SEARCH_COMPLETED)
                    .detail("totalResults", response.getPagination().getTotalElements())
                    .detail("executionTime", response.getQueryMetadata().getExecutionTimeMs())
                    .log();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn().message(AppConstants.LOG_INVALID_SEARCH_REQUEST_PARAMETERS)
                    .detail("error", e.getMessage())
                    .log();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_EXECUTE_LOG_SEARCH)
                    .detail("request", searchRequest.toString())
                    .log();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to search logs: " + e.getMessage()));
        }
    }

    /**
     * Retrieves aggregation data for building filter dropdowns.
     * Returns counts and unique values for common filter fields.
     *
     * @param fields comma-separated list of field names to aggregate
     * @return map of field names to aggregation buckets
     */
    @GetMapping("/search/aggregations")
    public ResponseEntity<?> getFilterAggregations(@RequestParam("fields") String fields) {
        logger.trace().message(AppConstants.LOG_FILTER_AGGREGATIONS_REQUEST_RECEIVED)
                .detail("fields", fields)
                .log();

        try {
            List<String> fieldList = Arrays.asList(fields.split(","));
            Map<String, List<AggregationBucket>> aggregations = logFilteringService
                    .getFilterAggregations(fieldList);

            logger.info().message(AppConstants.LOG_FILTER_AGGREGATIONS_COMPLETED)
                    .detail("fieldCount", fieldList.size())
                    .log();

            return ResponseEntity.ok(aggregations);

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_GET_FILTER_AGGREGATIONS)
                    .detail("fields", fields)
                    .log();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get aggregations: " + e.getMessage()));
        }
    }

    /**
     * Retrieves log statistics without returning actual documents.
     * Useful for dashboard analytics and overview data.
     *
     * @param searchRequest the search criteria (documents will not be returned)
     * @return response with only pagination metadata and aggregations
     */
    @PostMapping("/search/statistics")
    public ResponseEntity<?> getLogStatistics(@RequestBody LogSearchRequest searchRequest) {
        logger.trace().message(AppConstants.LOG_LOG_STATISTICS_REQUEST_RECEIVED)
                .detail("request", searchRequest.toString())
                .log();

        try {
            LogSearchResponse response = logFilteringService.getLogStatistics(searchRequest);

            logger.info().message(AppConstants.LOG_LOG_STATISTICS_COMPLETED)
                    .detail("totalResults", response.getPagination().getTotalElements())
                    .detail("executionTime", response.getQueryMetadata().getExecutionTimeMs())
                    .log();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn().message(AppConstants.LOG_INVALID_STATISTICS_REQUEST_PARAMETERS)
                    .detail("error", e.getMessage())
                    .log();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_GET_LOG_STATISTICS)
                    .detail("request", searchRequest.toString())
                    .log();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get statistics: " + e.getMessage()));
        }
    }

    /**
     * Provides search suggestions for autocomplete functionality.
     * Suggests values based on partial input for specified fields.
     *
     * @param field the field to get suggestions for
     * @param query the partial search term
     * @param limit maximum number of suggestions to return (default: 10)
     * @return list of suggested values
     */
    @GetMapping("/search/suggestions")
    public ResponseEntity<?> getSearchSuggestions(
            @RequestParam("field") String field,
            @RequestParam("query") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        logger.trace().message(AppConstants.LOG_SEARCH_SUGGESTIONS_REQUEST_RECEIVED)
                .detail("field", field)
                .detail("query", query)
                .detail("limit", limit)
                .log();

        try {
            if (limit > 100) {
                limit = 100; // Cap to prevent abuse
            }

            List<String> suggestions = logFilteringService.getSearchSuggestions(field, query, limit);

            logger.info().message(AppConstants.LOG_SEARCH_SUGGESTIONS_COMPLETED)
                    .detail("suggestionCount", suggestions.size())
                    .log();

            return ResponseEntity.ok(Map.of(
                    "field", field,
                    "query", query,
                    "suggestions", suggestions));

        } catch (Exception e) {
            logger.error(e).message(AppConstants.LOG_FAILED_TO_GET_SEARCH_SUGGESTIONS)
                    .detail("field", field)
                    .detail("query", query)
                    .log();
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get suggestions: " + e.getMessage()));
        }
    }
}