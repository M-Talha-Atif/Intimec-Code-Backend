package com.aidentx.core.log_management.model.dto;

/**
 * DTO for creating a new pinned log entry.
 * Contains the log ID, name, description, and JSON data fields.
 */
public class CreatePinnedLogRequest {

    private String documentId;
    private String name;
    private String description;

    /**
     * Default constructor.
     */
    public CreatePinnedLogRequest() {
    }

    /**
     * Constructor with all fields.
     * 
     * @param documentId  the OpenSearch document ID
     * @param name        the name/title for the pinned log
     * @param description the description for the pinned log
     */
    public CreatePinnedLogRequest(String documentId, String name, String description) {
        this.documentId = documentId;
        this.name = name;
        this.description = description;
    }

    /**
     * Gets the document ID.
     * 
     * @return the document ID
     */
    public String getDocumentId() {
        return documentId;
    }

    /**
     * Sets the document ID.
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

    @Override
    public String toString() {
        return "CreatePinnedLogRequest{" +
                "documentId='" + documentId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
