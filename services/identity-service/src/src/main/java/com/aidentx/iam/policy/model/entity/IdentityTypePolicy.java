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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing global governance policies for identity types.
 * Stores configuration that applies to the entire identity type, such as source
 * priorities.
 */
@Entity
@Table(name = "identity_type_policies", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "tenant_id", "identity_type" })
})
public class IdentityTypePolicy {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private UUID id;

  @NotBlank
  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @NotNull
  @Column(name = "identity_type", nullable = false)
  private IdentityType identityType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "source_priorities", columnDefinition = "jsonb")
  private JsonNode sourcePriorities;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Version
  private Long version;

  public IdentityTypePolicy() {
    this.createdAt = Instant.now();
    this.updatedAt = Instant.now();
  }

  public IdentityTypePolicy(String tenantId, IdentityType identityType) {
    this();
    this.tenantId = tenantId;
    this.identityType = identityType;
  }

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

  public JsonNode getSourcePriorities() {
    return sourcePriorities;
  }

  public void setSourcePriorities(JsonNode sourcePriorities) {
    this.sourcePriorities = sourcePriorities;
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
}
