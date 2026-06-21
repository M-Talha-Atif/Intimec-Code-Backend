package com.aidentx.core.onboarding.model.entity;

import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Entity
@Table(name = "application_schema_attributes")
public class ApplicationSchemaAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    @JsonBackReference
    private Application application;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @NotBlank
    @Column(nullable = false)
    private String type;

    @NotBlank
    @Column(name = "object_type", nullable = false)
    private String objectType;

    @Column(name = "object_type_kind")
    private String objectTypeKind;

    @Pattern(regexp = "^(identity|entitlement|risk|metadata)$", message = "Category must be one of: identity, entitlement, risk, metadata")
    @Column(name = "category")
    private String category;

    @Column(name = "description")
    private String description;

    @Column(name = "is_display_attribute")
    private boolean isDisplayAttribute;

    @Column(name = "is_description_attribute", nullable = false)
    private boolean isDescriptionAttribute = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode flags;

    // <editor-fold desc="Getters and Setters">
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getObjectTypeKind() {
        return objectTypeKind;
    }

    public void setObjectTypeKind(String objectTypeKind) {
        this.objectTypeKind = objectTypeKind;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDisplayAttribute() {
        return isDisplayAttribute;
    }

    public void setDisplayAttribute(boolean displayAttribute) {
        isDisplayAttribute = displayAttribute;
    }

    public boolean isDescriptionAttribute() {
        return isDescriptionAttribute;
    }

    public void setDescriptionAttribute(boolean descriptionAttribute) {
        isDescriptionAttribute = descriptionAttribute;
    }

    public JsonNode getFlags() {
        return flags;
    }

    public void setFlags(JsonNode flags) {
        this.flags = flags;
    }
}