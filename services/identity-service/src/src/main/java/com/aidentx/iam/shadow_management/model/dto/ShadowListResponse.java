package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Paginated shadow list response DTO
 */
public class ShadowListResponse {

    @JsonProperty("shadows")
    @Valid
    private List<ShadowResponse> shadows;

    @JsonProperty("pagination")
    @Valid
    private PaginationMetadata pagination;

    // Constructors
    public ShadowListResponse() {
    }

    public ShadowListResponse(List<ShadowResponse> shadows, PaginationMetadata pagination) {
        this.shadows = shadows;
        this.pagination = pagination;
    }

    // Getters and Setters
    public List<ShadowResponse> getShadows() {
        return shadows;
    }

    public void setShadows(List<ShadowResponse> shadows) {
        this.shadows = shadows;
    }

    public PaginationMetadata getPagination() {
        return pagination;
    }

    public void setPagination(PaginationMetadata pagination) {
        this.pagination = pagination;
    }
}
