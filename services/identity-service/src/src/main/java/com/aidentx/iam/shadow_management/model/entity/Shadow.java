package com.aidentx.iam.shadow_management.model.entity;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shadow Entity - Persistent representation of resource objects.
 * 
 * Shadow objects are the intermediary layer between external resources
 * and canonical Identity objects in the IGA platform.
 * 
 * Key Concepts:
 * - resourceUid: Unique identifier from the source resource (e.g., LDAP DN)
 * - Linking: Association between a Shadow and a canonical Identity object
 * - Attributes: Temporary key-value storage during operations (cleared after
 * processing)
 */
@Entity
@Table(name = "shadows", indexes = {
        @Index(name = "idx_shadow_resource_id", columnList = "resource_id"),
        @Index(name = "idx_shadow_resource_uid", columnList = "resource_uid"),
        @Index(name = "idx_shadow_linked_identity", columnList = "linked_identity_id"),
        @Index(name = "idx_shadow_sync_situation", columnList = "synchronization_situation"),
        @Index(name = "idx_shadow_tenant", columnList = "tenant_id"),
        @Index(name = "idx_shadow_last_sync_run", columnList = "resource_id, tenant_id, last_sync_run_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_shadow_resource_uid", columnNames = { "resource_id", "resource_uid", "tenant_id" })
})
public class Shadow {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Identifier of the external resource/system this shadow comes from.
     * Examples: "AD", "LDAP_SERVER_1", "HR_SYSTEM", "OKTA"
     */
    @NotBlank
    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    /**
     * The unique identifier of this object within the resource.
     * Examples: "CN=user,OU=users,DC=example,DC=com", "uid=jdoe", "employeeId:12345"
     */
    @NotBlank
    @Column(name = "resource_uid", nullable = false, length = 1024)
    private String resourceUid;

    @NotBlank
    @Column(name = "object_class", nullable = false)
    private String objectClass;

    /**
     * The kind of resource object (account, entitlement, or generic)
     */
    @Column(name = "kind", length = 50)
    private String kind;

    /**
     * Reference to the linked Identity object (if linked)
     */
    @Column(name = "linked_identity_id")
    private UUID linkedIdentityId;

    /**
     * Current synchronization situation
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "synchronization_situation", nullable = false, length = 50)
    private SynchronizationSituation synchronizationSituation;

    /**
     * Attributes from the external resource object.
     * Stored as JSONB in PostgreSQL for flexibility and efficient querying.
     * Uses Hypersistence JsonBinaryType for proper JDBC type binding.
     */
    @Type(JsonBinaryType.class)
    @Column(name = "attributes", columnDefinition = "jsonb")
    private Map<String, Object> attributes;

    /**
     * Dgraph UID for graph database synchronization
     */
    @Column(name = "dgraph_uid")
    private String dgraphUid;

    // Metadata fields
    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "last_synchronized_at")
    private Long lastSynchronizedAt;

    @Column(name = "last_provisioned_at")
    private Long lastProvisionedAt;

    /**
     * Tracks which sync run last touched this shadow.
     * Used for server-side deletion detection: shadows whose lastSyncRunId
     * differs from the current run were not seen and may be deleted.
     * This replaces the previous approach of sending all seen UIDs over gRPC.
     */
    @Column(name = "last_sync_run_id", length = 36)
    private String lastSyncRunId;

    /**
     * Synchronization Situation Enum
     */
    public enum SynchronizationSituation {
        LINKED, // Shadow is linked to an identity
        UNLINKED, // Shadow exists but not linked
        UNMATCHED, // Shadow found on resource but no correlation match
        DISPUTED, // Multiple potential identity matches
        DELETED // Shadow marked for deletion
    }

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().toEpochMilli();
        this.createdAt = now;
        this.updatedAt = now;

        // Set default synchronization situation if not set
        if (this.synchronizationSituation == null) {
            this.synchronizationSituation = SynchronizationSituation.UNLINKED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    // Getters and Setters

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

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getResourceUid() {
        return resourceUid;
    }

    public void setResourceUid(String resourceUid) {
        this.resourceUid = resourceUid;
    }

    public String getObjectClass() {
        return objectClass;
    }

    public void setObjectClass(String objectClass) {
        this.objectClass = objectClass;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public UUID getLinkedIdentityId() {
        return linkedIdentityId;
    }

    public void setLinkedIdentityId(UUID linkedIdentityId) {
        this.linkedIdentityId = linkedIdentityId;
    }

    public SynchronizationSituation getSynchronizationSituation() {
        return synchronizationSituation;
    }

    public void setSynchronizationSituation(SynchronizationSituation synchronizationSituation) {
        this.synchronizationSituation = synchronizationSituation;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public String getDgraphUid() {
        return dgraphUid;
    }

    public void setDgraphUid(String dgraphUid) {
        this.dgraphUid = dgraphUid;
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

    public Long getLastSynchronizedAt() {
        return lastSynchronizedAt;
    }

    public void setLastSynchronizedAt(Long lastSynchronizedAt) {
        this.lastSynchronizedAt = lastSynchronizedAt;
    }

    public Long getLastProvisionedAt() {
        return lastProvisionedAt;
    }

    public void setLastProvisionedAt(Long lastProvisionedAt) {
        this.lastProvisionedAt = lastProvisionedAt;
    }

    public String getLastSyncRunId() {
        return lastSyncRunId;
    }

    public void setLastSyncRunId(String lastSyncRunId) {
        this.lastSyncRunId = lastSyncRunId;
    }

    /**
     * Helper method to get attributes with null-safety.
     * Returns empty map instead of null.
     */
    @Transient
    public Map<String, Object> getAttributesSafe() {
        return attributes != null ? attributes : new HashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Shadow))
            return false;
        Shadow shadow = (Shadow) o;
        return id != null && id.equals(shadow.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Shadow{" +
                "id=" + id +
                ", tenantId='" + tenantId + '\'' +
                ", resourceId='" + resourceId + '\'' +
                ", resourceUid='" + resourceUid + '\'' +
                ", objectClass='" + objectClass + '\'' +
                ", kind='" + kind + '\'' +
                ", synchronizationSituation=" + synchronizationSituation +
                ", linkedIdentityId=" + linkedIdentityId +
                '}';
    }
}
