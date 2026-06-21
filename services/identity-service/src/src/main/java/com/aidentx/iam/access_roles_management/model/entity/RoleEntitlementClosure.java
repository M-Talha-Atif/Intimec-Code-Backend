package com.aidentx.iam.access_roles_management.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Materialized role-to-entitlement closure row.
 *
 * <p>Each row says that {@code roleId} effectively contains {@code entitlementId}
 * because that entitlement is directly attached to {@code sourceRoleId}. When
 * {@code roleId == sourceRoleId}, the row is direct; otherwise it is inherited
 * through one or more child roles.</p>
 */
@Entity
@Table(name = "access_role_entitlement_closure", indexes = {
        @Index(name = "idx_rec_tenant_role", columnList = "tenant_id, role_id"),
        @Index(name = "idx_rec_tenant_entitlement", columnList = "tenant_id, entitlement_id"),
        @Index(name = "idx_rec_tenant_source_role", columnList = "tenant_id, source_role_id"),
        @Index(name = "idx_rec_tenant_role_direct", columnList = "tenant_id, role_id, direct")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_role_entitlement_closure_source",
                columnNames = {"tenant_id", "role_id", "entitlement_id", "source_role_id"})
})
public class RoleEntitlementClosure {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @NotNull
    @Column(name = "entitlement_id", nullable = false)
    private UUID entitlementId;

    @NotNull
    @Column(name = "source_role_id", nullable = false)
    private UUID sourceRoleId;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "direct", nullable = false)
    private boolean direct;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "path_role_ids", columnDefinition = "jsonb")
    private List<UUID> pathRoleIds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().toEpochMilli();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now().toEpochMilli();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public UUID getRoleId() { return roleId; }
    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public UUID getEntitlementId() { return entitlementId; }
    public void setEntitlementId(UUID entitlementId) { this.entitlementId = entitlementId; }

    public UUID getSourceRoleId() { return sourceRoleId; }
    public void setSourceRoleId(UUID sourceRoleId) { this.sourceRoleId = sourceRoleId; }

    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }

    public boolean isDirect() { return direct; }
    public void setDirect(boolean direct) { this.direct = direct; }

    public List<UUID> getPathRoleIds() { return pathRoleIds; }
    public void setPathRoleIds(List<UUID> pathRoleIds) { this.pathRoleIds = pathRoleIds; }

    public Long getCreatedAt() { return createdAt; }
    public void setCreatedAt(Long createdAt) { this.createdAt = createdAt; }

    public Long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Long updatedAt) { this.updatedAt = updatedAt; }
}

