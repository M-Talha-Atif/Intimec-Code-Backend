package com.aidentx.core.log_management.model.pojo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * A POJO representing a single log entry from OpenSearch
 */
public class LogEntry {
    private String documentId;
    private String Id;
    private String level;
    private String source;
    private String microservice;
    private String category;
    private String message;
    private Long timestamp;
    private JsonNode details;

    // Pinning metadata
    private Boolean isPinned;
    private String pinnerId;
    private Long pinnedAt;
    private String pinnedName;
    private String pinnedDescription;

    // Resolution metadata
    private Boolean isResolved;
    private String resolverId;
    private Long resolvedAt;

    // Additional metadata
    private String correlationId;
    private List<String> tags;
    private Map<String, Object> customFields;

    // Constructors
    public LogEntry() {
    }

    // Getters and setters
    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getId() {
        return Id;
    }

    public void setId(String Id) {
        this.Id = Id;
    }
    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getMicroservice() {
        return microservice;
    }

    public void setMicroservice(String microservice) {
        this.microservice = microservice;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public JsonNode getDetails() {
        return details;
    }

    public void setDetails(JsonNode details) {
        this.details = details;
    }

    public Boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }

    public String getPinnerId() {
        return pinnerId;
    }

    public void setPinnerId(String pinnerId) {
        this.pinnerId = pinnerId;
    }

    public Long getPinnedAt() {
        return pinnedAt;
    }

    public void setPinnedAt(Long pinnedAt) {
        this.pinnedAt = pinnedAt;
    }

    public String getPinnedName() {
        return pinnedName;
    }

    public void setPinnedName(String pinnedName) {
        this.pinnedName = pinnedName;
    }

    public String getPinnedDescription() {
        return pinnedDescription;
    }

    public void setPinnedDescription(String pinnedDescription) {
        this.pinnedDescription = pinnedDescription;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
    }

    public String getResolverId() {
        return resolverId;
    }

    public void setResolverId(String resolverId) {
        this.resolverId = resolverId;
    }

    public Long getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Long resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields;
    }
}