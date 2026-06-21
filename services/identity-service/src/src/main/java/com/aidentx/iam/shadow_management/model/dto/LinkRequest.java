package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Link request DTO for linking shadows to identities
 */
public class LinkRequest {

    @JsonProperty("identityId")
    @NotNull
    private UUID identityId;

    // Constructors
    public LinkRequest() {
    }

    public LinkRequest(UUID identityId) {
        this.identityId = identityId;
    }

    // Getters and Setters
    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }
}
