package com.aidentx.iam.shadow_management.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Error Response DTO
 */
public class ErrorResponse {

    @JsonProperty("error")
    @NotBlank
    private String error;

    @JsonProperty("message")
    @NotBlank
    private String message;

    @JsonProperty("details")
    @Valid
    private Map<String, Object> details = new HashMap<>();

    @JsonProperty("timestamp")
    @NotNull
    private OffsetDateTime timestamp;

    @JsonProperty("path")
    private String path;

    // Constructors
    public ErrorResponse() {
    }

    public ErrorResponse(String error, String message, OffsetDateTime timestamp) {
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
