package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

/**
 * Shadow Metadata DTO
 */
public class ShadowMetadata {

    @JsonProperty("createdAt")
    @NotNull
    private OffsetDateTime createdAt;

    @JsonProperty("updatedAt")
    @NotNull
    private OffsetDateTime updatedAt;

    @JsonProperty("lastSynchronizedAt")
    private OffsetDateTime lastSynchronizedAt;

    @JsonProperty("lastProvisionedAt")
    private OffsetDateTime lastProvisionedAt;

    // Constructors
    public ShadowMetadata() {
    }

    // Getters and Setters
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getLastSynchronizedAt() {
        return lastSynchronizedAt;
    }

    public void setLastSynchronizedAt(OffsetDateTime lastSynchronizedAt) {
        this.lastSynchronizedAt = lastSynchronizedAt;
    }

    public OffsetDateTime getLastProvisionedAt() {
        return lastProvisionedAt;
    }

    public void setLastProvisionedAt(OffsetDateTime lastProvisionedAt) {
        this.lastProvisionedAt = lastProvisionedAt;
    }
}
