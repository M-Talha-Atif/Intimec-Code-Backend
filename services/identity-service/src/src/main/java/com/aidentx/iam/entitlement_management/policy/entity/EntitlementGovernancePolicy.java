package com.aidentx.iam.entitlement_management.policy.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Governance policy for entitlement attribute mappings.
 *
 * Stores inbound attribute mapping rules per resource (application) that
 * control how connector attributes are mapped to canonical Entitlement fields
 * during synchronization. This is the entitlement equivalent of
 * {@code IdentityGovernancePolicy} and follows the same architectural pattern.
 *
 * <p>Key: {@code (tenant_id, resource_id)} – one policy per application per tenant.</p>
 *
 * <p>The {@code attribute_mappings} JSONB column holds a serialized
 * {@code List<AttributeMappingDTO>} that the {@code EntitlementMappingEngine}
 * reads at sync time to drive field population.</p>
 */
@Entity
@Table(
    name = "entitlement_governance_policies",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_entitlement_governance_policy_tenant_resource",
            columnNames = { "tenant_id", "resource_id" }
        )
    },
    indexes = {
        @Index(name = "idx_entitlement_governance_policy_tenant", columnList = "tenant_id"),
        @Index(name = "idx_entitlement_governance_policy_resource", columnList = "resource_id")
    }
)
public class EntitlementGovernancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false, length = 255)
    private String tenantId;

    @NotNull
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    /**
     * Serialized {@code List<AttributeMappingDTO>}.
     * Controls how connector-provided attributes are mapped to Entitlement
     * governance fields during inbound synchronization.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attribute_mappings", columnDefinition = "jsonb")
    private JsonNode attributeMappings;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    public EntitlementGovernancePolicy() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public EntitlementGovernancePolicy(String tenantId, UUID resourceId) {
        this();
        this.tenantId = tenantId;
        this.resourceId = resourceId;
    }

    // -----------------------------------------------------------------------
    // Lifecycle
    // -----------------------------------------------------------------------

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // -----------------------------------------------------------------------
    // Getters & Setters
    // -----------------------------------------------------------------------

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }

    public JsonNode getAttributeMappings() { return attributeMappings; }
    public void setAttributeMappings(JsonNode attributeMappings) {
        this.attributeMappings = attributeMappings;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
