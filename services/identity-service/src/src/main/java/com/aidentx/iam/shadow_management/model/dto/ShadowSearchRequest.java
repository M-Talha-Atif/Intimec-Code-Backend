package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

/**
 * Request DTO for advanced shadow search.
 */
public class ShadowSearchRequest {

    @JsonProperty("filter")
    @Valid
    private SearchFilter filter;

    @JsonProperty("page")
    @Min(0)
    private Integer page = 0;

    @JsonProperty("size")
    @Min(1)
    @Max(1000)
    private Integer size = 50;

    @JsonProperty("sort")
    @Valid
    private List<SortCriteria> sort;

    // Constructors
    public ShadowSearchRequest() {
    }

    // Getters and Setters
    public SearchFilter getFilter() {
        return filter;
    }

    public void setFilter(SearchFilter filter) {
        this.filter = filter;
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

    public List<SortCriteria> getSort() {
        return sort;
    }

    public void setSort(List<SortCriteria> sort) {
        this.sort = sort;
    }
}