package com.aidentx.core.onboarding.model.entity;

import com.aidentx.core.onboarding.model.enums.ManagedAccountStatus;
import com.aidentx.core.utils.JsonNodeConverter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "managed_accounts", indexes = {
        @Index(name = "idx_managedaccount_identity_id", columnList = "identity_id"),
        @Index(name = "uk_managedaccount_tenant_resource_native", columnList = "tenant_id, resource_id, native_identifier", unique = true),
        @Index(name = "idx_managedaccount_tenant_resource", columnList = "tenant_id, resource_id")
})
public class ManagedAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @NotNull
    @Column(name = "native_identifier", nullable = false, columnDefinition = "TEXT")
    private String nativeIdentifier;

    @Column(name = "identity_id")
    private UUID identityId; // Nullable until correlated

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ManagedAccountStatus status;

    @NotNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    private JsonNode attributes;

    @NotNull
    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public void setResourceId(UUID resourceId) {
        this.resourceId = resourceId;
    }

    public String getNativeIdentifier() {
        return nativeIdentifier;
    }

    public void setNativeIdentifier(String nativeIdentifier) {
        this.nativeIdentifier = nativeIdentifier;
    }

    public UUID getIdentityId() {
        return identityId;
    }

    public void setIdentityId(UUID identityId) {
        this.identityId = identityId;
    }

    public ManagedAccountStatus getStatus() {
        return status;
    }

    public void setStatus(ManagedAccountStatus status) {
        this.status = status;
    }

    public JsonNode getAttributes() {
        return attributes;
    }

    public void setAttributes(JsonNode attributes) {
        this.attributes = attributes;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}