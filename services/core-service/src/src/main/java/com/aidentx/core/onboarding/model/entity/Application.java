package com.aidentx.core.onboarding.model.entity;

import com.aidentx.core.onboarding.model.enums.LifecycleStatus;
import com.aidentx.core.onboarding.model.enums.OperationalStatus;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;

import groovyjarjarantlr4.v4.runtime.misc.NotNull;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifecycleStatus lifecycleStatus = LifecycleStatus.DRAFT;

    public LifecycleStatus getLifecycleStatus() {
        return lifecycleStatus;
    }

    public void setLifecycleStatus(LifecycleStatus lifecycleStatus) {
        this.lifecycleStatus = lifecycleStatus;
    }

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationalStatus operationalStatus = OperationalStatus.IDLE;

    public OperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(OperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    @Column(name = "last_status_details", columnDefinition = "TEXT")
    private String lastStatusDetails;

    public String getLastStatusDetails() {
        return lastStatusDetails;
    }

    public void setLastStatusDetails(String lastStatusDetails) {
        this.lastStatusDetails = lastStatusDetails;
    }

    @Column(name = "icon_path")
    private String iconPath;

    @NotBlank
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private boolean isAuthoritative = false;

    @Column(name = "associated_schema_id")
    private UUID associatedSchemaId;

    // --- Fields mapped from the new 'details' object ---
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode technicalOwners;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode businessOwners;

    @Column(length = 100)
    private String identityType;

    @Column(length = 50)
    private String riskLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode categories;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode complianceLabels;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode scopes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode department;

    // --- Core Policy and Configuration Fields ---
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode connectorConfiguration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode accessPolicy;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<ApplicationSchemaAttribute> schemaAttributes = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = this.updatedAt = Instant.now();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAuthoritative() {
        return isAuthoritative;
    }

    public void setAuthoritative(boolean authoritative) {
        isAuthoritative = authoritative;
    }

    public UUID getAssociatedSchemaId() {
        return associatedSchemaId;
    }

    public void setAssociatedSchemaId(UUID associatedSchemaId) {
        this.associatedSchemaId = associatedSchemaId;
    }

    public JsonNode getTechnicalOwners() {
        return technicalOwners;
    }

    public void setTechnicalOwners(JsonNode technicalOwners) {
        this.technicalOwners = technicalOwners;
    }

    public JsonNode getBusinessOwners() {
        return businessOwners;
    }

    public void setBusinessOwners(JsonNode businessOwners) {
        this.businessOwners = businessOwners;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public JsonNode getCategories() {
        return categories;
    }

    public void setCategories(JsonNode categories) {
        this.categories = categories;
    }

    public JsonNode getComplianceLabels() {
        return complianceLabels;
    }

    public void setComplianceLabels(JsonNode complianceLabels) {
        this.complianceLabels = complianceLabels;
    }

    public JsonNode getScopes() {
        return scopes;
    }

    public void setScopes(JsonNode scopes) {
        this.scopes = scopes;
    }

    public JsonNode getDepartment() {
        return department;
    }

    public void setDepartment(JsonNode department) {
        this.department = department;
    }

    public JsonNode getConnectorConfiguration() {
        return connectorConfiguration;
    }

    public void setConnectorConfiguration(JsonNode connectorConfiguration) {
        this.connectorConfiguration = connectorConfiguration;
    }

    public JsonNode getAccessPolicy() {
        return accessPolicy;
    }

    public void setAccessPolicy(JsonNode accessPolicy) {
        this.accessPolicy = accessPolicy;
    }

    public List<ApplicationSchemaAttribute> getSchemaAttributes() {
        return schemaAttributes;
    }

    public void setSchemaAttributes(List<ApplicationSchemaAttribute> schemaAttributes) {
        this.schemaAttributes = schemaAttributes;
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

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }
}