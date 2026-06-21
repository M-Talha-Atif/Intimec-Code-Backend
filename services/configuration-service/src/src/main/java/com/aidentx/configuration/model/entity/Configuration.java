package com.aidentx.configuration.model.entity;

import com.aidentx.configuration.util.JsonNodeConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing an application configuration record.
 * <p>
 * This class maps to the <code>configurations</code> table and stores the
 * primary
 * configuration data as JSON in the {@code configData} field. It also serves as
 * a container
 * for supplementary {@link Artifact} entities, which hold categorized data
 * related to this main configuration.
 * </p>
 *
 * <p>
 * It includes metadata such as type, key, version, and active status, and
 * maintains relationships
 * with associated artifacts for extensibility.
 * </p>
 *
 * @author aidentx
 * @since 1.0
 */
@Entity
@Table(name = "configurations")
public class Configuration {

    /**
     * Unique identifier for the configuration record.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * The type/category of the configuration (e.g., SSO, LDAP).
     */
    @Column(name = "config_type", nullable = false, length = 100)
    private String configType;

    /**
     * The key or identifier for the configuration.
     */
    @Column(name = "config_key", length = 255)
    private String configKey;

    /**
     * The main configuration data stored as JSON.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "config_data", columnDefinition = "jsonb")
    private JsonNode configData;

    /**
     * Whether this configuration is currently active.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * The version number of the configuration (for versioning and rollback).
     */
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /**
     * List of supplementary artifacts associated with this configuration.
     */
    @OneToMany(mappedBy = "configuration", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JsonIgnore // Avoid infinite recursion when serializing a Configuration
    private List<Artifact> artifacts;

    /**
     * Timestamp when the configuration was created (Unix time).
     */
    @Column(name = "created_at")
    private Instant createdAt;

    /**
     * Timestamp when the configuration was last updated (Unix time).
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Optional tenant identifier to which this configuration belongs.
     * Keeping this nullable preserves backward compatibility with existing records.
     */
    @Column(name = "tenant_id", length = 128)
    private String tenantId;

    /**
     * Default constructor for frameworks that require it.
     */
    public Configuration() {
    }

    /**
     * Constructs a Configuration with the specified type and key.
     *
     * @param configType the type/category of the configuration
     * @param configKey  the key or identifier for the configuration
     */
    public Configuration(String configType, String configKey) {
        this.configType = configType;
        this.configKey = configKey;
        this.isActive = true;
        this.version = 1;
    }

    /**
     * JPA lifecycle callback to set creation and update timestamps before
     * persisting.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * JPA lifecycle callback to update the update timestamp before updating.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public JsonNode getConfigData() {
        return configData;
    }

    public void setConfigData(JsonNode configData) {
        this.configData = configData;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public List<Artifact> getArtifacts() {
        return artifacts;
    }

    public void setArtifacts(List<Artifact> artifacts) {
        this.artifacts = artifacts;
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

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    @Override
    public String toString() {
        // Don't touch the lazy collection; only include its size if initialized
        int count = -1;
        try {
            if (artifacts != null) {
                count = artifacts.size();
            }
        } catch (Exception ignored) {
            // Ignore lazy init exceptions in toString
        }
        return "Configuration{" +
                "id=" + id +
                ", configType='" + configType + '\'' +
                ", configKey='" + configKey + '\'' +
                ", isActive=" + isActive +
                ", version=" + version +
                ", tenantId=" + (tenantId != null ? ('\'' + tenantId + '\'') : null) +
                ", artifactsCount=" + (count >= 0 ? count : "LAZY") +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}