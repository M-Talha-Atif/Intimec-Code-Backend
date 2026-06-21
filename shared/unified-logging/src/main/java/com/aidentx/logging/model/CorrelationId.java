package com.aidentx.logging.model;

/**
 * Correlation ID specific configuration
 */
public class CorrelationId {

    /**
     * Header name to look for correlation ID in HTTP requests
     */
    private String headerName = "X-Correlation-ID";

    /**
     * Whether correlation ID tracking is enabled
     */
    private boolean enabled = true;

    /**
     * Whether to auto-generate correlation IDs when not present
     */
    private boolean autoGenerate = true;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoGenerate() {
        return autoGenerate;
    }

    public void setAutoGenerate(boolean autoGenerate) {
        this.autoGenerate = autoGenerate;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }
}