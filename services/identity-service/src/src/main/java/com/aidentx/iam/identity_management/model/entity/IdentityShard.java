// File: ./src/main/java/com/aidentx/iam/identity_management/model/entity/IdentityShard.java
// Status: Updated with correct package import

package com.aidentx.iam.identity_management.model.entity;

import com.aidentx.iam.common.converters.JsonNodeConverter; // CORRECTED IMPORT
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a "shard" of identity data as it exists on a specific source
 * system.
 * This table acts as a staging area for raw data before it is reconciled
 * into the canonical Identity record.
 */
@Entity
@Table(name = "identity_shards")
@IdClass(IdentityShardId.class)
public class IdentityShard {

    @Id
    @Column(name = "identity_id")
    private UUID identityId;

    @Id
    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "source_name")
    private String sourceName;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)

    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode attributes;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    public IdentityShard() {
    }

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.lastUpdatedAt = Instant.now();
    }

    // --- Getters and Setters remain the same ---
    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public JsonNode getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonNode attributes) {
        this.attributes = attributes;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}