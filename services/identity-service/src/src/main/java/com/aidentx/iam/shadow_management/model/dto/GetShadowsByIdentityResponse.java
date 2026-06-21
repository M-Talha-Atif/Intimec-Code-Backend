package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for getting shadows by identity
 */
public class GetShadowsByIdentityResponse {

    @JsonProperty("identityId")
    @NotNull
    private UUID identityId;

    @JsonProperty("shadows")
    @Valid
    private List<ShadowResponse> shadows;

    @JsonProperty("totalCount")
    @NotNull
    private Integer totalCount;

    // Constructors
    public GetShadowsByIdentityResponse() {
    }

    public GetShadowsByIdentityResponse(UUID identityId, List<ShadowResponse> shadows) {
        this.identityId = identityId;
        this.shadows = shadows;
        this.totalCount = shadows != null ? shadows.size() : 0;
    }

    // Getters and Setters
    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    public List<ShadowResponse> getShadows() {
        return shadows;
    }

    public void setShadows(List<ShadowResponse> shadows) {
        this.shadows = shadows;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
}
