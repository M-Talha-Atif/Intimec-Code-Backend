package com.aidentx.core.log_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for complex log search operations.
 * Supports filtering by multiple fields, global search, date ranges,
 * pagination, and sorting.
 */
public class LogSearchRequest {

    /**
     * Global search term that searches across multiple text fields
     * (message, source, microservice, category, details, etc.)
     */
    @JsonProperty("globalSearch")
    private String globalSearch;

    /**
     * Exact match filters for specific fields
     */
    @JsonProperty("level")
    private List<String> level;

    @JsonProperty("source")
    private List<String> source;

    @JsonProperty("microservice")
    private List<String> microservice;

    @JsonProperty("category")
    private String category;

    @JsonProperty("objectType")
    private List<String> objectType;

    @JsonProperty("isPinned")
    private Boolean isPinned;

    @JsonProperty("isResolved")
    private Boolean isResolved;

    @JsonProperty("pinnerId")
    private String pinnerId;

    @JsonProperty("resolverId")
    private String resolverId;

    /**
     * Correlation ID for tracing related operations across services
     */
    @JsonProperty("correlationId")
    private String correlationId;

    /**
     * Date range filtering (epoch milliseconds)
     */
    @JsonProperty("dateFrom")
    private Long dateFrom;

    @JsonProperty("dateTo")
    private Long dateTo;

    @JsonProperty("pinnedDateFrom")
    private Long pinnedDateFrom;

    @JsonProperty("pinnedDateTo")
    private Long pinnedDateTo;

    @JsonProperty("resolvedDateFrom")
    private Long resolvedDateFrom;

    @JsonProperty("resolvedDateTo")
    private Long resolvedDateTo;

    /**
     * Advanced filtering using custom field queries
     * Key-value pairs where key is the field path (e.g., "details.correlationId")
     * and value is the search term or exact match value
     */
    @JsonProperty("customFields")
    private Map<String, Object> customFields;

    /**
     * Array filtering for fields that contain arrays
     */
    @JsonProperty("tags")
    private List<String> tags;

    /**
     * Pagination settings
     */
    @JsonProperty("page")
    private Integer page = 0;

    @JsonProperty("size")
    private Integer size = 20;

    /**
     * Sorting configuration
     */
    @JsonProperty("sortBy")
    private String sortBy = "@timestamp";

    @JsonProperty("sortOrder")
    private SortOrder sortOrder = SortOrder.DESC;

    /**
     * Whether to include only aggregated summary data
     */
    @JsonProperty("aggregateOnly")
    private Boolean aggregateOnly = false;

    /**
     * Fields to include in response (for optimization)
     */
    @JsonProperty("includeFields")
    private List<String> includeFields;

    /**
     * Fields to exclude from response
     */
    @JsonProperty("excludeFields")
    private List<String> excludeFields;

    public enum SortOrder {
        ASC, DESC
    }

    // Default constructor
    public LogSearchRequest() {
    }

    // copy constructor
    public LogSearchRequest(LogSearchRequest other) {
        if (other == null) {
            return;
        }
        // Copy all fields from the other request
        this.globalSearch = other.globalSearch;
        this.level = other.level != null ? new ArrayList<>(other.level) : null;
        this.source = other.source != null ? new ArrayList<>(other.source) : null;
        this.microservice = other.microservice != null ? new ArrayList<>(other.microservice) : null;
        this.category = other.category;
        this.objectType = other.objectType != null ? new ArrayList<>(other.objectType) : null;
        this.isPinned = other.isPinned;
        this.isResolved = other.isResolved;
        this.pinnerId = other.pinnerId;
        this.resolverId = other.resolverId;
        this.correlationId = other.correlationId;
        this.dateFrom = other.dateFrom;
        this.dateTo = other.dateTo;
        this.pinnedDateFrom = other.pinnedDateFrom;
        this.pinnedDateTo = other.pinnedDateTo;
        this.resolvedDateFrom = other.resolvedDateFrom;
        this.resolvedDateTo = other.resolvedDateTo;
        this.customFields = other.customFields != null ? new HashMap<>(other.customFields) : null;
        this.tags = other.tags != null ? new ArrayList<>(other.tags) : null;
        this.sortBy = other.sortBy;
        this.sortOrder = other.sortOrder;
        this.page = other.page;
        this.size = other.size;
        this.aggregateOnly = other.aggregateOnly;
        this.includeFields = other.includeFields != null ? new ArrayList<>(other.includeFields) : null;
        this.excludeFields = other.excludeFields != null ? new ArrayList<>(other.excludeFields) : null;
    }

    // Getters and setters
    public String getGlobalSearch() {
        return globalSearch;
    }

    public void setGlobalSearch(String globalSearch) {
        this.globalSearch = globalSearch;
    }

    public List<String> getLevel() {
        return level;
    }

    public void setLevel(List<String> level) {
        this.level = level;
    }

    public List<String> getSource() {
        return source;
    }

    public void setSource(List<String> source) {
        this.source = source;
    }

    public List<String> getMicroservice() {
        return microservice;
    }

    public void setMicroservice(List<String> microservice) {
        this.microservice = microservice;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getObjectType() {
        return objectType;
    }

    public void setObjectType(List<String> objectType) {
        this.objectType = objectType;
    }

    public Boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
    }

    public String getPinnerId() {
        return pinnerId;
    }

    public void setPinnerId(String pinnerId) {
        this.pinnerId = pinnerId;
    }

    public String getResolverId() {
        return resolverId;
    }

    public void setResolverId(String resolverId) {
        this.resolverId = resolverId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Long getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Long dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Long getDateTo() {
        return dateTo;
    }

    public void setDateTo(Long dateTo) {
        this.dateTo = dateTo;
    }

    public Long getPinnedDateFrom() {
        return pinnedDateFrom;
    }

    public void setPinnedDateFrom(Long pinnedDateFrom) {
        this.pinnedDateFrom = pinnedDateFrom;
    }

    public Long getPinnedDateTo() {
        return pinnedDateTo;
    }

    public void setPinnedDateTo(Long pinnedDateTo) {
        this.pinnedDateTo = pinnedDateTo;
    }

    public Long getResolvedDateFrom() {
        return resolvedDateFrom;
    }

    public void setResolvedDateFrom(Long resolvedDateFrom) {
        this.resolvedDateFrom = resolvedDateFrom;
    }

    public Long getResolvedDateTo() {
        return resolvedDateTo;
    }

    public void setResolvedDateTo(Long resolvedDateTo) {
        this.resolvedDateTo = resolvedDateTo;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getAggregateOnly() {
        return aggregateOnly;
    }

    public void setAggregateOnly(Boolean aggregateOnly) {
        this.aggregateOnly = aggregateOnly;
    }

    public List<String> getIncludeFields() {
        return includeFields;
    }

    public void setIncludeFields(List<String> includeFields) {
        this.includeFields = includeFields;
    }

    public List<String> getExcludeFields() {
        return excludeFields;
    }

    public void setExcludeFields(List<String> excludeFields) {
        this.excludeFields = excludeFields;
    }

    @Override
    public String toString() {
        return "LogSearchRequest{" +
                "globalSearch='" + globalSearch + '\'' +
                ", level=" + level +
                ", source=" + source +
                ", microservice=" + microservice +
                ", category='" + category + '\'' +
                ", isPinned=" + isPinned +
                ", isResolved=" + isResolved +
                ", pinnerId='" + pinnerId + '\'' +
                ", resolverId='" + resolverId + '\'' +
                ", correlationId='" + correlationId + '\'' +
                ", dateFrom=" + dateFrom +
                ", dateTo=" + dateTo +
                ", pinnedDateFrom=" + pinnedDateFrom +
                ", pinnedDateTo=" + pinnedDateTo +
                ", resolvedDateFrom=" + resolvedDateFrom +
                ", resolvedDateTo=" + resolvedDateTo +
                ", customFields=" + customFields +
                ", tags=" + tags +
                ", page=" + page +
                ", size=" + size +
                ", sortBy='" + sortBy + '\'' +
                ", sortOrder=" + sortOrder +
                ", aggregateOnly=" + aggregateOnly +
                ", includeFields=" + includeFields +
                ", excludeFields=" + excludeFields +
                '}';
    }
}
