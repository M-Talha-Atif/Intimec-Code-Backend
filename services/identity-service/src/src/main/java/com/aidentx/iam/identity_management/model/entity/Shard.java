package com.aidentx.iam.identity_management.model.entity;

import java.time.Instant;
import java.util.UUID;

import com.aidentx.iam.common.converters.JsonNodeConverter;
import com.aidentx.iam.identity_management.model.enums.ShardStatus;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "shards", indexes = {
    @Index(name = "idx_shard_resource_uid", columnList = "resource_id, object_uid", unique = true),
    @Index(name = "idx_shard_correlated_identity_id", columnList = "correlated_identity_id")
})
public class Shard {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "tenant_id", nullable = false)
  private String tenantId;

  @Column(name = "resource_id", nullable = false)
  private String resourceId;

  @Column(name = "resource_name")
  private String resourceName;

  // --- Object Classification (MidPoint Compatible) ---
  @Column(name = "kind", nullable = false)
  private String kind; // account, entitlement, generic

  @Column(name = "intent", nullable = false)
  private String intent; // default, admin, service

  @Column(name = "object_class")
  private String objectClass; // e.g., __ACCOUNT__, __GROUP__

  @Column(name = "object_uid", nullable = false)
  private String objectUid; // Immutable UID from target system

  @Convert(converter = JsonNodeConverter.class)
  @Column(name = "shard_attributes", columnDefinition = "jsonb")
  private JsonNode shardAttributes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "correlated_identity_id") // This is the foreign key to the 'identities' table
  private Identity correlatedIdentity;

  // --- Lifecycle State ---
  @Enumerated(EnumType.STRING)
  @Column(name = "shard_status", nullable = false)
  private ShardStatus shardStatus;

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

  public void setStatus(ShardStatus shardStatus) {
    this.shardStatus = shardStatus;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }

  public void setResourceId(String resourceId) {
    this.resourceId = resourceId;
  }

  public void setResourceName(String resourceName) {
    this.resourceName = resourceName;
  }

  public void setKind(String kind) {
    this.kind = kind;
  }

  public void setIntent(String intent) {
    this.intent = intent;
  }

  public void setObjectClass(String objectClass) {
    this.objectClass = objectClass;
  }

  public void setObjectUid(String objectUid) {
    this.objectUid = objectUid;
  }

  public void setShardAttributes(JsonNode shardAttributes) {
    this.shardAttributes = shardAttributes;
  }

  public void setCorrelatedIdentity(Identity correlatedIdentity) {
    this.correlatedIdentity = correlatedIdentity;
  }

  public UUID getId() {
    return id;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getResourceId() {
    return resourceId;
  }

  public String getResourceName() {
    return resourceName;
  }

  public String getKind() {
    return kind;
  }

  public String getIntent() {
    return intent;
  }

  public String getObjectClass() {
    return objectClass;
  }

  public String getObjectUid() {
    return objectUid;
  }

  public JsonNode getShardAttributes() {
    return shardAttributes;
  }

  public Identity getCorrelatedIdentity() {
    return correlatedIdentity;
  }

  public ShardStatus getShardStatus() {
    return shardStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
