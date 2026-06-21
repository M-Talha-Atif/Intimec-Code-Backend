package com.aidentx.core.log_management.model.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Data Transfer Object for PATCH operations on pinned logger entries.
 * Contains optional fields that can be updated in a pinned log.
 * All fields are optional to support partial updates.
 */
public class UpdatePinnedLogRequest {

    /**
     * Optional OpenSearch document ID for direct operations.
     * If provided, enables direct document updates without searching.
     */
    private String documentId;

    /**
     * Optional new name/title for the pinned logger entry.
     * If provided, must not be null or empty after trimming.
     */
    private String name;

    /**
     * Optional new description for the pinned logger entry.
     * Can be null to remove the description or a new value to update it.
     */
    private String description;

    /**
     * Optional new data content for the pinned logger entry.
     * If provided, will replace the existing data field completely.
     */
    private JsonNode data;

    /**
     * Default constructor for JSON deserialization.
     */
    public UpdatePinnedLogRequest() {
    }

    /**
     * Constructor with all fields.
     *
     * @param documentId  the OpenSearch document ID
     * @param name        the new name for the pinned log
     * @param description the new description for the pinned log
     * @param data        the new data content for the pinned log
     */
    public UpdatePinnedLogRequest(String documentId, String name, String description, JsonNode data) {
        this.documentId = documentId;
        this.name = name;
        this.description = description;
        this.data = data;
    }

    /**
     * Gets the document ID field.
     *
     * @return the document ID, or null if not provided
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Sets the document ID field.
     *
     * @param documentId the OpenSearch document ID
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    /**
     * Gets the name field.
     *
     * @return the name to update, or null if not being updated
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name field.
     *
     * @param name the new name for the pinned log
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the description field.
     *
     * @return the description to update, or null if not being updated
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description field.
     *
     * @param description the new description for the pinned log
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets the data field.
     *
     * @return the data to update, or null if not being updated
     */
    public JsonNode getData() {
        return data;
    }

    /**
     * Sets the data field.
     *
     * @param data the new data content for the pinned log
     */
    public void setData(JsonNode data) {
        this.data = data;
    }

    /**
     * Checks if any field is provided for update.
     *
     * @return true if at least one field is provided for update, false otherwise
     */
    public boolean hasUpdates() {
        return documentId != null || name != null || description != null || data != null;
    }

    /**
     * Checks if the document ID field is being updated.
     *
     * @return true if document ID is provided for update, false otherwise
     */
    public boolean isUpdatingDocumentId() {
        return documentId != null;
    }

    /**
     * Checks if the name field is being updated.
     *
     * @return true if name is provided for update, false otherwise
     */
    public boolean isUpdatingName() {
        return name != null;
    }

    /**
     * Checks if the description field is being updated.
     *
     * @return true if description is provided for update, false otherwise
     */
    public boolean isUpdatingDescription() {
        return description != null;
    }

    /**
     * Checks if the data field is being updated.
     *
     * @return true if data is provided for update, false otherwise
     */
    public boolean isUpdatingData() {
        return data != null;
    }

    @Override
    public String toString() {
        return "UpdatePinnedLogRequest{" +
                "documentId='" + documentId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", data=" + data +
                '}';
    }
}
