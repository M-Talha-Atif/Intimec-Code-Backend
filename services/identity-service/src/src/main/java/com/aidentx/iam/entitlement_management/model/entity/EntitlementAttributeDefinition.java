package com.aidentx.iam.entitlement_management.model.entity;

import com.aidentx.iam.entitlement_management.model.enums.SchemaLifecycleState;
import com.aidentx.iam.identity_management.model.enums.DataType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "entitlement_attribute_definitions", indexes = {
        @Index(name = "idx_entitlement_attr_def_tenant", columnList = "tenant_id"),
        @Index(name = "idx_entitlement_attr_def_tenant_system_key", columnList = "tenant_id, system_key", unique = true)
})
public class EntitlementAttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotBlank
    @Column(name = "system_key", nullable = false, length = 120)
    private String systemKey;

    @NotBlank
    @Column(name = "display_name", nullable = false, length = 180)
    private String displayName;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private DataType dataType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @Column(name = "is_unique", nullable = false)
    private Boolean isUnique = false;

    @Column(name = "is_sensitive", nullable = false)
    private Boolean isSensitive = false;

    @Column(name = "is_writable", nullable = false)
    private Boolean isWritable = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 20)
    private SchemaLifecycleState lifecycleState = SchemaLifecycleState.ACTIVE;

    @Type(JsonBinaryType.class)
    @Column(name = "default_value", columnDefinition = "jsonb")
    private Object defaultValue;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = Instant.now().toEpochMilli();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lifecycleState == null) {
            this.lifecycleState = SchemaLifecycleState.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now().toEpochMilli();
    }

    public UUID getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSystemKey() {
        return systemKey;
    }

    public void setSystemKey(String systemKey) {
        this.systemKey = systemKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }

    public void setIsRequired(Boolean required) {
        isRequired = required;
    }

    public Boolean getIsUnique() {
        return isUnique;
    }

    public void setIsUnique(Boolean unique) {
        isUnique = unique;
    }

    public Boolean getIsSensitive() {
        return isSensitive;
    }

    public void setIsSensitive(Boolean sensitive) {
        isSensitive = sensitive;
    }

    public Boolean getIsWritable() {
        return isWritable;
    }

    public void setIsWritable(Boolean writable) {
        isWritable = writable;
    }

    public SchemaLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(SchemaLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }
}
