package com.aidentx.core.log_management.model.dto.analytics;

import com.aidentx.core.log_management.model.pojo.PaginationMetadata;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class RecurringPatternsResponse {

    @JsonProperty("patterns")
    private List<RecurringPatternEntry> patterns;

    @JsonProperty("pagination")
    private PaginationMetadata pagination;

    public RecurringPatternsResponse() {}

    public RecurringPatternsResponse(
            List<RecurringPatternEntry> patterns,
            PaginationMetadata pagination) {
        this.patterns   = patterns;
        this.pagination = pagination;
    }

    public List<RecurringPatternEntry> getPatterns()                { return patterns; }
    public void setPatterns(List<RecurringPatternEntry> patterns)   { this.patterns = patterns; }

    public PaginationMetadata getPagination()                       { return pagination; }
    public void setPagination(PaginationMetadata pagination)        { this.pagination = pagination; }
}