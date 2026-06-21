package com.aidentx.iam.policy.model.entity;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing governance policies for identity schemas.
 * Stores correlation rules, attribute mappings, and source priorities at the
 * schema level.
 * Moved from IAM service to Core service as part of architectural refactoring.
 */
@Entity
@Table(name = "identity_governance_policies", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tenant_id", "resource_id", "identity_type" })
})
public class IdentityGovernancePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "resource_id", nullable = false)
    private UUID resourceId;

    @NotNull
    @Column(name = "identity_type", nullable = false)
    private IdentityType identityType;

    @Column(name = "schema_id")
    private UUID schemaId;

    @Column(name = "schema_name")
    private String schemaName;

    // Governance data stored as JSON
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "correlation_rules", columnDefinition = "jsonb")
    private JsonNode correlationRules;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attribute_mappings", columnDefinition = "jsonb")
    private JsonNode attributeMappings;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "synchronization_reactions", columnDefinition = "jsonb")
    private JsonNode synchronizationReactions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    // Constructors
    public IdentityGovernancePolicy() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public IdentityGovernancePolicy(String tenantId, IdentityType identityType, UUID resourceId, UUID schemaId) {
        this();
        this.tenantId = tenantId;
        this.identityType = identityType;
        this.resourceId = resourceId;
        this.schemaId = schemaId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public UUID getResourceId() {
        return resourceId;
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

    public UUID getSchemaId() {
        return schemaId;
    }

    public void setSchemaId(UUID schemaId) {
        this.schemaId = schemaId;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public JsonNode getCorrelationRules() {
        return correlationRules;
    }

    public void setCorrelationRules(JsonNode correlationRules) {
        this.correlationRules = correlationRules;
        this.updatedAt = Instant.now();
    }

    public JsonNode getAttributeMappings() {
        return attributeMappings;
    }

    public void setAttributeMappings(JsonNode attributeMappings) {
        this.attributeMappings = attributeMappings;
        this.updatedAt = Instant.now();
    }

    public JsonNode getSynchronizationReactions() {
        return synchronizationReactions;
    }

    public void setSynchronizationReactions(JsonNode synchronizationReactions) {
        this.synchronizationReactions = synchronizationReactions;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
