package com.aidentx.iam.identity_management.model.entity;

import com.aidentx.iam.identity_management.model.enums.IdentityStatus;
import com.aidentx.iam.identity_management.model.enums.IdentityType;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * The abstract base entity for all identity types in the system.
 * This table contains the universal attributes shared by every identity,
 * both human and non-human.
 */
@Entity
@Table(name = "identities", indexes = {
        @Index(name = "idx_identity_tenant_id", columnList = "tenant_id"),
        @Index(name = "idx_identity_status", columnList = "status")
})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "identity_type", discriminatorType = DiscriminatorType.STRING)
public abstract class Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "identity_id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "dgraph_node_id", length = 100, unique = true)
    private String dgraphNodeId;

    // This field is managed by JPA's discriminator mechanism.
    // We include it so we can read the value, but it's not directly writable.
    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, updatable = false, insertable = false)
    private IdentityType identityType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private IdentityStatus status = IdentityStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        Long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = System.currentTimeMillis();
    }

    // --- Getters and Setters ---

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public IdentityType getIdentityType() {
        return identityType;
    }

    public void setIdentityType(IdentityType identityType) {
        this.identityType = identityType;
    }

    public IdentityStatus getStatus() {
        return status;
    }

    public void setStatus(IdentityStatus status) {
        this.status = status;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDgraphNodeId() {
        return dgraphNodeId;
    }

    public void setDgraphNodeId(String dgraphNodeId) {
        this.dgraphNodeId = dgraphNodeId;
    }
}