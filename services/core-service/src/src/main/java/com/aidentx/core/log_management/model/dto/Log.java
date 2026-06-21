package com.aidentx.core.log_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * DTO representing a pinned log entry from OpenSearch.
 * Maps OpenSearch document fields to a structured response.
 */
public class Log {
    
    private String id;
    private String message;
    private String level;
    private String source;
    private String microservice;
    private String category;
    private Long timestamp;
    private String pinnerId;
    private Long pinnedAt;
    private String pinnedName;
    private String pinnedDescription;
    private boolean isPinned;
    private boolean isResolved;
    private String resolverId;
    private Long resolvedAt;

    // Constructors
    public Log() {}

    public Log(String id, String message, String level, String source, 
                       String microservice, String category, Long timestamp) {
        this.id = id;
        this.message = message;
        this.level = level;
        this.source = source;
        this.microservice = microservice;
        this.category = category;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @JsonProperty("@timestamp")
    public Long getTimestampProperty() {
        return timestamp;
    }

    @JsonProperty("@timestamp")
    public void setTimestampProperty(Long timestamp) {
        this.timestamp = timestamp;
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

    @JsonProperty("pinned.name")
    public String getPinnedName() {
        return pinnedName;
    }

    @JsonProperty("pinned.name")
    public void setPinnedName(String pinnedName) {
        this.pinnedName = pinnedName;
    }

    @JsonProperty("pinned.description")
    public String getPinnedDescription() {
        return pinnedDescription;
    }

    @JsonProperty("pinned.description")
    public void setPinnedDescription(String pinnedDescription) {
        this.pinnedDescription = pinnedDescription;
    }

    public boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(boolean isPinned) {
        this.isPinned = isPinned;
    }

    public boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(boolean isResolved) {
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

    // Utility methods
    public LocalDateTime getPinnedAtAsDateTime() {
        return pinnedAt != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(pinnedAt), ZoneOffset.UTC) : null;
    }

    public LocalDateTime getResolvedAtAsDateTime() {
        return resolvedAt != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(resolvedAt), ZoneOffset.UTC) : null;
    }

    public LocalDateTime getTimestampAsDateTime() {
        return timestamp != null ? LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC) : null;
    }

    @Override
    public String toString() {
        return "PinnedLogDto{" +
                "id='" + id + '\'' +
                ", message='" + message + '\'' +
                ", level='" + level + '\'' +
                ", source='" + source + '\'' +
                ", microservice='" + microservice + '\'' +
                ", category='" + category + '\'' +
                ", timestamp=" + timestamp +
                ", pinnerId='" + pinnerId + '\'' +
                ", pinnedAt=" + pinnedAt +
                ", pinnedName='" + pinnedName + '\'' +
                ", pinnedDescription='" + pinnedDescription + '\'' +
                ", isPinned=" + isPinned +
                ", isResolved=" + isResolved +
                ", resolverId='" + resolverId + '\'' +
                ", resolvedAt=" + resolvedAt +
                '}';
    }
}
