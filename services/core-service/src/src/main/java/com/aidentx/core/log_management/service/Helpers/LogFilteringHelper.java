package com.aidentx.core.log_management.service.Helpers;

import com.aidentx.core.log_management.model.dto.LogSearchRequest;
import com.aidentx.core.log_management.model.dto.LogSearchResponse;
import com.aidentx.core.log_management.model.pojo.AggregationBucket;
import com.aidentx.core.log_management.model.pojo.LogEntry;
import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.aidentx.core.log_management.model.pojo.QueryMetadata;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

public class LogFilteringHelper {

    // Default index for log searches
    public static final String LOGS_INDEX = "main-logs";

    // Fields that support global search
    public static final List<String> GLOBAL_SEARCH_FIELDS = Arrays.asList(
            "message", "source", "microservice", "category", "correlationId", "details.correlationId",
            "details.error", "details.stackTrace", "details.request", "pinned.name", "pinned.description");

    // Fields that can be used for exact matching
    public static final Set<String> EXACT_MATCH_FIELDS = Set.of(
            "level", "source", "microservice", "category", "objectType", "isPinned", "isResolved",
            "pinnerId", "resolverId", "correlationId");

    // Map API fields to their OpenSearch field names based on actual index mapping
    public static final Map<String, String> FIELD_MAPPINGS = Map.ofEntries(
            Map.entry("level", "level.keyword"), // text field with keyword subfield
            Map.entry("source", "source"), // keyword field
            Map.entry("microservice", "microservice"), // keyword field
            Map.entry("category", "category"), // keyword field
            Map.entry("objectType", "objectType.keyword"), // text field with keyword subfield
            Map.entry("isPinned", "isPinned"), // boolean field
            Map.entry("isResolved", "isResolved"), // boolean field
            Map.entry("pinnerId", "pinnerId"), // keyword field
            Map.entry("resolverId", "resolverId"), // keyword field
            Map.entry("correlationId", "correlationId.keyword") // text field with keyword subfield
    );

    // Date fields for range filtering
    public static final Set<String> DATE_FIELDS = Set.of(
            "@timestamp", "pinnedAt", "resolvedAt", "updatedAt");

    /**
     * Builds the OpenSearch query from the search request
     */
    public static Map<String, Object> buildOpenSearchQuery(LogSearchRequest searchRequest) {
        Map<String, Object> query = new HashMap<>();

        // Build the main query
        Map<String, Object> boolQuery = new HashMap<>();
        List<Map<String, Object>> mustClauses = new ArrayList<>();
        List<Map<String, Object>> filterClauses = new ArrayList<>();

        // Global search across multiple fields
        if (searchRequest.getGlobalSearch() != null && !searchRequest.getGlobalSearch().trim().isEmpty()) {
            Map<String, Object> multiMatch = new HashMap<>();
            multiMatch.put("query", searchRequest.getGlobalSearch());
            multiMatch.put("fields", GLOBAL_SEARCH_FIELDS);
            multiMatch.put("type", "best_fields");
            multiMatch.put("fuzziness", "AUTO");

            mustClauses.add(Map.of("multi_match", multiMatch));
        }

        // Exact match filters
        addListFilter(filterClauses, FIELD_MAPPINGS.get("level"), searchRequest.getLevel());
        addListFilter(filterClauses, FIELD_MAPPINGS.get("source"), searchRequest.getSource());
        addListFilter(filterClauses, FIELD_MAPPINGS.get("microservice"), searchRequest.getMicroservice());
        addListFilter(filterClauses, FIELD_MAPPINGS.get("objectType"), searchRequest.getObjectType());
        addExactMatchFilter(filterClauses, FIELD_MAPPINGS.get("category"), searchRequest.getCategory());
        addExactMatchFilter(filterClauses, FIELD_MAPPINGS.get("isPinned"), searchRequest.getIsPinned());
        addExactMatchFilter(filterClauses, FIELD_MAPPINGS.get("isResolved"), searchRequest.getIsResolved());
        addExactMatchFilter(filterClauses, FIELD_MAPPINGS.get("pinnerId"), searchRequest.getPinnerId());
        addExactMatchFilter(filterClauses, FIELD_MAPPINGS.get("resolverId"), searchRequest.getResolverId());
        addExactMatchFilter(filterClauses, FIELD_MAPPINGS.get("correlationId"), searchRequest.getCorrelationId());

        // Date range filters
        addDateRangeFilter(filterClauses, "@timestamp", searchRequest.getDateFrom(), searchRequest.getDateTo());
        addDateRangeFilter(filterClauses, "pinnedAt", searchRequest.getPinnedDateFrom(),
                searchRequest.getPinnedDateTo());
        addDateRangeFilter(filterClauses, "resolvedAt", searchRequest.getResolvedDateFrom(),
                searchRequest.getResolvedDateTo());

        // Custom field filters
        if (searchRequest.getCustomFields() != null) {
            for (Map.Entry<String, Object> entry : searchRequest.getCustomFields().entrySet()) {
                String fieldPath = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String && ((String) value).contains("*")) {
                    // Wildcard search - use wildcard query
                    Map<String, Object> wildcardQuery = new HashMap<>();

                    // For text fields, try to use .keyword subfield for exact wildcard matching
                    String wildcardField = fieldPath;
                    if (!fieldPath.endsWith(".keyword") && !isNonTextFieldType(fieldPath)) {
                        wildcardField = fieldPath + ".keyword";
                    }

                    wildcardQuery.put(wildcardField, Map.of("value", value));
                    filterClauses.add(Map.of("wildcard", wildcardQuery));
                } else {
                    // Exact match - determine correct field name based on field type
                    String exactMatchField = getExactMatchFieldName(fieldPath);
                    addExactMatchFilter(filterClauses, exactMatchField, value);
                }
            }
        }

        // Tags filter (array field)
        if (searchRequest.getTags() != null && !searchRequest.getTags().isEmpty()) {
            Map<String, Object> termsQuery = new HashMap<>();
            termsQuery.put("tags", searchRequest.getTags());
            filterClauses.add(Map.of("terms", termsQuery));
        }

        // Build bool query
        if (!mustClauses.isEmpty()) {
            boolQuery.put("must", mustClauses);
        }
        if (!filterClauses.isEmpty()) {
            boolQuery.put("filter", filterClauses);
        }

        // Set main query
        if (boolQuery.isEmpty()) {
            query.put("query", Map.of("match_all", Map.of()));
        } else {
            query.put("query", Map.of("bool", boolQuery));
        }

        // Add pagination
        int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
        int size = searchRequest.getSize() != null ? searchRequest.getSize() : 20;

        if (!searchRequest.getAggregateOnly()) {
            query.put("from", page * size);
            query.put("size", size);
        } else {
            query.put("size", 0);
        }

        // Add sorting
        if (searchRequest.getSortBy() != null) {
            String sortField = searchRequest.getSortBy();
            String sortOrder = searchRequest.getSortOrder() == LogSearchRequest.SortOrder.ASC ? "asc" : "desc";

            Map<String, Object> sort = new HashMap<>();
            sort.put(sortField, Map.of("order", sortOrder));
            query.put("sort", List.of(sort));
        }

        // Add field selection
        if (searchRequest.getIncludeFields() != null || searchRequest.getExcludeFields() != null) {
            Map<String, Object> source = new HashMap<>();
            if (searchRequest.getIncludeFields() != null) {
                source.put("includes", searchRequest.getIncludeFields());
            }
            if (searchRequest.getExcludeFields() != null) {
                source.put("excludes", searchRequest.getExcludeFields());
            }
            query.put("_source", source);
        }

        // Add aggregations for statistics
        if (searchRequest.getAggregateOnly() || searchRequest.getSize() == 0) {
            query.put("aggs", buildDefaultAggregations());
        }

        return query;
    }

    /**
     * Adds an exact match filter to the filter clauses
     */
    public static void addExactMatchFilter(List<Map<String, Object>> filterClauses, String field, Object value) {
        if (value != null) {
            Map<String, Object> termQuery = new HashMap<>();
            termQuery.put(field, value);
            filterClauses.add(Map.of("term", termQuery));
        }
    }

    /**
     * Adds a list filter (terms query) to the filter clauses
     */
    public static void addListFilter(List<Map<String, Object>> filterClauses, String field, List<String> values) {
        if (values != null && !values.isEmpty()) {
            Map<String, Object> termsQuery = new HashMap<>();
            termsQuery.put(field, values);
            filterClauses.add(Map.of("terms", termsQuery));
        }
    }

    /**
     * Adds a date range filter to the filter clauses
     */
    public static void addDateRangeFilter(List<Map<String, Object>> filterClauses, String field, Long from, Long to) {
        if (from != null || to != null) {
            Map<String, Object> rangeQuery = new HashMap<>();
            Map<String, Object> rangeConditions = new HashMap<>();

            if (from != null) {
                rangeConditions.put("gte", from);
            }
            if (to != null) {
                rangeConditions.put("lte", to);
            }

            rangeQuery.put(field, rangeConditions);
            filterClauses.add(Map.of("range", rangeQuery));
        }
    }

    /**
     * Builds default aggregations for common filter fields
     */
    public static Map<String, Object> buildDefaultAggregations() {
        Map<String, Object> aggs = new HashMap<>();

        // Common field aggregations - using correct field mappings
        aggs.put("levels", Map.of("terms", Map.of("field", "level.keyword", "size", 50)));
        aggs.put("sources", Map.of("terms", Map.of("field", "source", "size", 100)));
        aggs.put("microservices", Map.of("terms", Map.of("field", "microservice", "size", 100)));
        aggs.put("categories", Map.of("terms", Map.of("field", "category", "size", 100)));

        // Status aggregations
        aggs.put("pinned_status", Map.of("terms", Map.of("field", "isPinned")));
        aggs.put("resolved_status", Map.of("terms", Map.of("field", "isResolved")));

        // Date histogram for timeline view
        aggs.put("timeline", Map.of("date_histogram", Map.of(
                "field", "@timestamp",
                "interval", "1h",
                "format", "yyyy-MM-dd HH:mm")));

        return aggs;
    }

    /**
     * Builds aggregation-only query for filter dropdowns
     */
    public static Map<String, Object> buildAggregationQuery(List<String> fields) {
        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);
        query.put("query", Map.of("match_all", Map.of()));

        Map<String, Object> aggs = new HashMap<>();
        for (String field : fields) {
            if (EXACT_MATCH_FIELDS.contains(field)) {
                String mappedField = FIELD_MAPPINGS.getOrDefault(field, field);
                aggs.put(field + "_agg", Map.of("terms", Map.of("field", mappedField, "size", 1000)));
            }
        }

        query.put("aggs", aggs);
        return query;
    }

    /**
     * Builds suggestion query for autocomplete
     */
    public static Map<String, Object> buildSuggestionQuery(String fieldName, String partialValue, int maxSuggestions) {
        Map<String, Object> query = new HashMap<>();
        query.put("size", 0);

        // Use prefix aggregation for suggestions
        Map<String, Object> aggs = new HashMap<>();
        Map<String, Object> termsAgg = new HashMap<>();
        String mappedField = FIELD_MAPPINGS.getOrDefault(fieldName, fieldName);
        termsAgg.put("field", mappedField);
        termsAgg.put("size", maxSuggestions);
        termsAgg.put("include", ".*" + partialValue + ".*");

        aggs.put("suggestions", Map.of("terms", termsAgg));
        query.put("aggs", aggs);

        return query;
    }

    /**
     * Parses OpenSearch results into LogSearchResponse
     */
    public static LogSearchResponse parseSearchResults(JsonNode searchResults, LogSearchRequest searchRequest) {
        LogSearchResponse response = new LogSearchResponse();

        // Parse hits
        JsonNode hits = searchResults.path("hits");
        long totalHits = hits.path("total").path("value").asLong(0);

        List<LogEntry> logEntries = new ArrayList<>();

        if (!searchRequest.getAggregateOnly()) {
            JsonNode documents = hits.path("hits");
            for (JsonNode doc : documents) {
                LogEntry logEntry = parseLogEntry(doc);
                if (logEntry != null) {
                    // Transform @timestamp within details if present
                    JsonNode details = logEntry.getDetails();
                    if (details != null && details.has("@timestamp")) {
                        long detailsTimestamp = details.path("@timestamp").asLong(0);
                        if (detailsTimestamp != 0) {
                            ((ObjectNode) details).put("timestamp", detailsTimestamp);
                            ((ObjectNode) details).remove("@timestamp");
                        }
                    }
                    logEntries.add(logEntry);
                }
            }
        }

        response.setLogs(logEntries);

        // Parse pagination
        PaginationMetadata pagination = new PaginationMetadata();
        int page = searchRequest.getPage() != null ? searchRequest.getPage() : 0;
        int size = searchRequest.getSize() != null ? searchRequest.getSize() : 20;

        pagination.setPage(page);
        pagination.setSize(size);
        pagination.setTotalElements(totalHits);
        pagination.setTotalPages((int) Math.ceil((double) totalHits / size));
        pagination.setHasNext(page < pagination.getTotalPages() - 1);
        pagination.setHasPrevious(page > 0);

        response.setPagination(pagination);

        // Parse aggregations
        if (searchResults.has("aggregations")) {
            response.setAggregations(parseAggregations(searchResults.path("aggregations")));
        }

        // Parse query metadata
        QueryMetadata queryMetadata = new QueryMetadata();
        queryMetadata.setExecutionTimeMs(searchResults.path("took").asLong(0));
        queryMetadata.setShardsTotal(searchResults.path("_shards").path("total").asInt(0));
        queryMetadata.setShardsSuccessful(searchResults.path("_shards").path("successful").asInt(0));
        queryMetadata.setShardsFailed(searchResults.path("_shards").path("failed").asInt(0));
        queryMetadata.setTimedOut(searchResults.path("timed_out").asBoolean(false));

        response.setQueryMetadata(queryMetadata);

        return response;
    }

    /**
     * Parses a single log entry from OpenSearch document
     */
    public static LogEntry parseLogEntry(JsonNode doc) {
        try {
            LogEntry entry = new LogEntry();
            JsonNode source = doc.path("_source");

            entry.setDocumentId(doc.path("_id").asText());
            entry.setId(source.path("id").asText());
            entry.setLevel(source.path("level").asText());
            entry.setSource(source.path("source").asText());
            entry.setMicroservice(source.path("microservice").asText());
            entry.setCategory(source.path("category").asText());
            entry.setMessage(source.path("message").asText());
            entry.setTimestamp(source.path("@timestamp").asLong());

            // Pinning metadata
            entry.setIsPinned(source.path("isPinned").asBoolean(false));
            entry.setPinnerId(source.path("pinnerId").asText(""));
            entry.setPinnedAt(source.path("pinnedAt").asLong(0));
            entry.setPinnedName(source.path("pinned.name").asText(""));
            entry.setPinnedDescription(source.path("pinned.description").asText(""));

            // Resolution metadata
            entry.setIsResolved(source.path("isResolved").asBoolean(false));
            entry.setResolverId(source.path("resolverId").asText(""));
            entry.setResolvedAt(source.path("resolvedAt").asLong(0));

            if (source.has("details")) {
                entry.setDetails(source.path("details"));
            }

            // Extract correlation ID from root level first, then details as fallback
            String correlationId = source.path("correlationId").asText("");
            if (correlationId.isEmpty() && source.has("details")) {
                correlationId = source.path("details").path("correlationId").asText("");
            }
            entry.setCorrelationId(correlationId);

            return entry;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parses aggregation results
     */
    public static Map<String, List<AggregationBucket>> parseAggregations(JsonNode aggregations) {
        Map<String, List<AggregationBucket>> result = new HashMap<>();

        aggregations.fieldNames().forEachRemaining(aggName -> {
            JsonNode aggData = aggregations.get(aggName);

            List<AggregationBucket> buckets = new ArrayList<>();

            if (aggData.has("buckets")) {
                for (JsonNode bucket : aggData.path("buckets")) {
                    String key = bucket.path("key").asText();
                    long count = bucket.path("doc_count").asLong();
                    buckets.add(new AggregationBucket(key, count));
                }
            }

            result.put(aggName, buckets);
        });

        return result;
    }

    /**
     * Parses aggregation results for filter dropdowns
     */
    public static Map<String, List<AggregationBucket>> parseAggregationResults(JsonNode results, List<String> fields) {
        if (!results.has("aggregations")) {
            return Collections.emptyMap();
        }

        return parseAggregations(results.path("aggregations"));
    }

    /**
     * Parses suggestion results
     */
    public static List<String> parseSuggestionResults(JsonNode results, String fieldName) {
        List<String> suggestions = new ArrayList<>();

        JsonNode aggregations = results.path("aggregations");
        if (aggregations.has("suggestions")) {
            JsonNode buckets = aggregations.path("suggestions").path("buckets");
            for (JsonNode bucket : buckets) {
                suggestions.add(bucket.path("key").asText());
            }
        }

        return suggestions;
    }

    /**
     * Creates an empty response for failed searches
     */
    public static LogSearchResponse createEmptyResponse(LogSearchRequest searchRequest) {
        LogSearchResponse response = new LogSearchResponse();
        response.setLogs(Collections.emptyList());

        PaginationMetadata pagination = new PaginationMetadata();
        pagination.setPage(searchRequest.getPage() != null ? searchRequest.getPage() : 0);
        pagination.setSize(searchRequest.getSize() != null ? searchRequest.getSize() : 20);
        pagination.setTotalElements(0);
        pagination.setTotalPages(0);
        pagination.setHasNext(false);
        pagination.setHasPrevious(false);

        response.setPagination(pagination);
        response.setAggregations(Collections.emptyMap());

        return response;
    }

    /**
     * Validates date range parameters
     */
    public static void validateDateRange(Long from, Long to, String fieldName) {
        if (from != null && to != null && from > to) {
            throw new IllegalArgumentException(fieldName + " range is invalid: 'from' date must be before 'to' date");
        }
    }

    /**
     * Checks if a field is valid for sorting
     */
    public static boolean isValidSortField(String field) {
        return DATE_FIELDS.contains(field) || EXACT_MATCH_FIELDS.contains(field) ||
                field.equals("message") || field.equals("_score");
    }

    /**
     * Determines if a field is a non-text type that doesn't need .keyword suffix
     */
    public static boolean isNonTextFieldType(String fieldPath) {
        // Fields that are known to be non-text types (keyword, boolean, date, long,
        // etc.)
        Set<String> nonTextFields = Set.of(
                "microservice", "source", "category", "pinnerId", "resolverId", "id",
                "isPinned", "isResolved", "retryable", "thread",
                "@timestamp", "pinnedAt", "resolvedAt", "updatedAt",
                "details.executionTime", "details.page", "details.size", "details.totalResults",
                "details.suggestionCount", "details.totalCount", "details.oauth2Enabled",
                "details.samlEnabled", "details.resolvedOnly", "details.unresolvedOnly",
                "object.id", "object.type", "trace.id", "log.level", "log.logger");

        return nonTextFields.contains(fieldPath) ||
                fieldPath.startsWith("details.") && (fieldPath.endsWith("Count") ||
                        fieldPath.endsWith("Time") ||
                        fieldPath.endsWith("Enabled") ||
                        fieldPath.endsWith("Only"));
    }

    /**
     * Gets the correct field name for exact matching based on field type
     */
    public static String getExactMatchFieldName(String fieldPath) {
        // Check if it's already a known mapped field
        if (FIELD_MAPPINGS.containsKey(fieldPath)) {
            return FIELD_MAPPINGS.get(fieldPath);
        }

        // If it's already a .keyword field, use as-is
        if (fieldPath.endsWith(".keyword")) {
            return fieldPath;
        }

        // If it's a non-text field type, use as-is
        if (isNonTextFieldType(fieldPath)) {
            return fieldPath;
        }

        // For text fields, append .keyword for exact matching
        return fieldPath + ".keyword";
    }
}
