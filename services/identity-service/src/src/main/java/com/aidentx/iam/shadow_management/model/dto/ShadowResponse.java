package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shadow Response DTO
 */
public class ShadowResponse {

    @JsonProperty("id")
    @NotNull
    private UUID id;

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

    @JsonProperty("linkedIdentityId")
    private UUID linkedIdentityId;

    @JsonProperty("synchronizationSituation")
    @NotBlank
    private String synchronizationSituation;

    @JsonProperty("attributes")
    @Valid
    private Map<String, Object> attributes = new HashMap<>();

    @JsonProperty("metadata")
    private ShadowMetadata metadata;

    // Constructors
    public ShadowResponse() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public ShadowMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ShadowMetadata metadata) {
        this.metadata = metadata;
    }
}
