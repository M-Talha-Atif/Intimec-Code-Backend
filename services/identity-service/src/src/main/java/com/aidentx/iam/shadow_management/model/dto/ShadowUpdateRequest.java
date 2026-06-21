package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

/**
 * Request DTO for updating an existing shadow.
 */
public class ShadowUpdateRequest {

    @JsonProperty("kind")
    private String kind;

    @JsonProperty("synchronizationSituation")
    private String synchronizationSituation;

    @JsonProperty("attributes")
    @Valid
    private Map<String, Object> attributes = new HashMap<>();

    // Constructors
    public ShadowUpdateRequest() {
    }

    // Getters and Setters
    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getSynchronizationSituation() {
        return synchronizationSituation;
    }

    public void setSynchronizationSituation(String synchronizationSituation) {
        this.synchronizationSituation = synchronizationSituation;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }
}