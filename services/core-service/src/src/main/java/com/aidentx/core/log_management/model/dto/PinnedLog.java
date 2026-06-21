package com.aidentx.core.log_management.model.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Model class representing a pinned logger entry.
 * Used as a DTO for transferring pinned log data.
 */
public class PinnedLog {

    /**
     * The OpenSearch document _id for direct operations.
     * This eliminates the need to search for documents by log ID.
     */
    private String documentId;

    /**
     * Name/title for the pinned logger entry.
     */
    private String name;

    /**
     * Description for the pinned logger entry.
     */
    private String description;

    /**
     * JSON data field for flexible storage.
     */
    private JsonNode data;

    /**
     * Timestamp when the pinned logger entry was last updated (epoch milliseconds).
     */
    private long updatedAt;

    /**
     * ID of the user who pinned this log entry.
     */
    private String pinnerId;

    /**
     * Timestamp when the log was pinned (epoch milliseconds).
     */
    private long pinnedAt;

    /**
     * ID of the user who resolved this log entry.
     */
    private String resolverId;

    /**
     * Timestamp when the log was resolved (epoch milliseconds).
     */
    private long resolvedAt;

    /**
     * ID of the user who last updated this log entry.
     */
    private String updatedBy;

    /**
     * Indicates whether this log entry is currently pinned.
     */
    private boolean isPinned;

    /**
     * Indicates whether this log entry has been resolved.
     */
    private boolean isResolved;

    /**
     * Default constructor.
     */
    public PinnedLog() {
    }

    /**
     * Constructor with name, description and data.
     * 
     * @param id          the log ID
     * @param name        the name/title
     * @param description the description
     * @param data        the JSON data
     */
    public PinnedLog(String documentId, String name, String description, JsonNode data) {
        this.documentId = documentId;
        this.name = name;
        this.description = description;
        this.data = data;
    }

    /**
     * Constructor with name, description and data.
     * 
     * @param id          the log ID
     * @param name        the name/title
     * @param description the description
     */
    public PinnedLog(String documentId, String name, String description) {
        this.documentId = documentId;
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the OpenSearch document ID.
     * 
     * @return the document ID
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Sets the OpenSearch document ID.
     * 
     * @param documentId the document ID to set
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * Gets the name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     * 
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the JSON data.
     * 
     * @return the data
     */
    public JsonNode getData() {
        return data;
    }

    /**
     * Sets the JSON data.
     * 
     * @param data the data to set
     */
    public void setData(JsonNode data) {
        this.data = data;
    }


    /**
     * Gets the last update timestamp.
     * 
     * @return the last update timestamp in epoch milliseconds
     */
    public long getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets the last update timestamp.
     * 
     * @param updatedAt the last update timestamp in epoch milliseconds to set
     */
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getPinnerId() {
        return pinnerId;
    }

    public void setPinnerId(String pinnerId) {
        this.pinnerId = pinnerId;
    }

    public long getPinnedAt() {
        return pinnedAt;
    }

    public void setPinnedAt(long pinnedAt) {
        this.pinnedAt = pinnedAt;
    }

    public String getResolverId() {
        return resolverId;
    }

    public void setResolverId(String resolverId) {
        this.resolverId = resolverId;
    }

    public long getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(long resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    @Override
    public String toString() {
        return "PinnedLog{" +
                "documentId='" + documentId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", data=" + data +
                ", updatedAt=" + updatedAt +
                ", pinnerId='" + pinnerId + '\'' +
                ", pinnedAt=" + pinnedAt +
                ", resolverId='" + resolverId + '\'' +
                ", resolvedAt=" + resolvedAt +
                ", updatedBy='" + updatedBy + '\'' +
                ", isPinned=" + isPinned +
                ", isResolved=" + isResolved +
                '}';
    }
}
