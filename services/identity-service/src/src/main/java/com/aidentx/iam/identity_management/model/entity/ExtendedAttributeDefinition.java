package com.aidentx.iam.identity_management.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

import com.aidentx.iam.identity_management.model.enums.DataType;
import com.aidentx.iam.identity_management.model.enums.IdentityType;
import com.aidentx.iam.identity_management.model.enums.SchemaLifecycleState;

/**
 * Defines a custom, tenant-specific extended attribute.
 * This acts as a catalog for all custom fields that can be associated with
 * identities.
 */
@Entity
@Table(name = "extended_attribute_definitions", indexes = {
        @Index(name = "idx_ext_attr_def_tenant_type_key", columnList = "tenant_id, identity_type, system_key", unique = true)
})
public class ExtendedAttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false)
    private IdentityType identityType; // E.g., EMPLOYEE, NON_HUMAN

    @NotBlank
    @Column(name = "system_key", nullable = false)
    private String systemKey; // The machine-readable key, e.g., "tShirtSize"

    @NotBlank
    @Column(name = "display_name", nullable = false)
    private String displayName; // The human-readable label, e.g., "T-Shirt Size"

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false)
    private DataType dataType; // E.g., STRING, NUMBER, DATE, BOOLEAN

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    // --- Validation and Behavior Flags ---
    @NotNull
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = false;

    @NotNull
    @Column(name = "is_unique", nullable = false)
    private Boolean isUnique = false;

    @NotNull
    @Column(name = "is_sensitive", nullable = false)
    private Boolean isSensitive = false;

    @NotNull
    @Column(name = "is_writable", nullable = false)
    private Boolean isWritable = true;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_state", nullable = false, length = 20)
    private SchemaLifecycleState lifecycleState = SchemaLifecycleState.ACTIVE;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue; // JSON string representation of the default value

    // --- Timestamps ---
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.lifecycleState == null) {
            this.lifecycleState = SchemaLifecycleState.ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
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

    public void setIsRequired(Boolean isRequired) {
        this.isRequired = isRequired;
    }

    public Boolean getIsUnique() {
        return isUnique;
    }

    public void setIsUnique(Boolean isUnique) {
        this.isUnique = isUnique;
    }

    public Boolean getIsSensitive() {
        return isSensitive;
    }

    public void setIsSensitive(Boolean isSensitive) {
        this.isSensitive = isSensitive;
    }

    public Boolean getIsWritable() {
        return isWritable;
    }

    public void setIsWritable(Boolean isWritable) {
        this.isWritable = isWritable;
    }

    public SchemaLifecycleState getLifecycleState() {
        return lifecycleState;
    }

    public void setLifecycleState(SchemaLifecycleState lifecycleState) {
        this.lifecycleState = lifecycleState;
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    // --- Getters and Setters ---
    // (Omitted for brevity, but would be included here)
}