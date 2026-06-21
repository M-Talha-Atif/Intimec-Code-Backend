package com.aidentx.core.log_management.service;

import com.aidentx.core.log_management.model.dto.LogSearchRequest;
import com.aidentx.core.log_management.model.dto.LogSearchResponse;
import com.aidentx.core.log_management.model.pojo.AggregationBucket;

import java.util.List;
import java.util.Map;

/**
 * Service interface for advanced log filtering and searching.
 * Provides complex query capabilities for OpenSearch-based log retrieval.
 */
public interface LogFilteringService {

    /**
     * Performs a complex search for logs based on the provided criteria.
     * Supports filtering, pagination, sorting, and aggregations.
     *
     * @param searchRequest the search criteria and configuration
     * @return paginated search results with metadata and aggregations
     * @throws RuntimeException if search execution fails
     */
    LogSearchResponse searchLogs(LogSearchRequest searchRequest);

    /**
     * Retrieves aggregation metadata for building filter dropdowns.
     * Returns counts and unique values for common filter fields.
     *
     * @param fields the fields to aggregate (e.g., "level", "source", "microservice")
     * @return map of field names to aggregation buckets
     * @throws RuntimeException if aggregation fails
     */
    Map<String, List<AggregationBucket>> getFilterAggregations(List<String> fields);

    /**
     * Performs a lightweight search to get only counts and aggregations
     * without returning actual log documents. Useful for dashboard analytics.
     *
     * @param searchRequest the search criteria (documents will not be returned)
     * @return response with only pagination metadata and aggregations
     * @throws RuntimeException if search execution fails
     */
    LogSearchResponse getLogStatistics(LogSearchRequest searchRequest);

    /**
     * Suggests search terms based on partial input for autocomplete functionality.
     * Searches across commonly searched fields like message, source, microservice.
     *
     * @param fieldName the field to get suggestions for
     * @param partialValue the partial search term
     * @param maxSuggestions maximum number of suggestions to return
     * @return list of suggested values
     * @throws RuntimeException if suggestion query fails
     */
    List<String> getSearchSuggestions(String fieldName, String partialValue, int maxSuggestions);

    /**
     * Validates a search request to ensure all parameters are within acceptable limits
     * and field names are valid for querying.
     *
     * @param searchRequest the search request to validate
     * @throws IllegalArgumentException if the request contains invalid parameters
     */
    void validateSearchRequest(LogSearchRequest searchRequest);
}
