package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for creating a new shadow.
 */
public class ShadowCreateRequest {

    @JsonProperty("resourceId")
    @NotBlank
    private String resourceId;

    @JsonProperty("resourceUid")
    @NotBlank
    private String resourceUid;

    @JsonProperty("objectClass")
    @NotBlank
    private String objectClass;

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("attributes")
    @Valid
    private Map<String, Object> attributes = new HashMap<>();

    // Constructors
    public ShadowCreateRequest() {
    }

    // Getters and Setters
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

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}