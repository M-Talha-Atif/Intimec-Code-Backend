package com.aidentx.logging.model;

/**
 * JSON formatting specific configuration
 */
public class JsonFormat {
    /**
     * Whether to use JSON format for log entries
     */
    private boolean enabled = true;

    /**
     * Whether to pretty-print JSON (useful for development)
     */
    private boolean prettyPrint = false;

    /**
     * Whether to include null values in JSON output
     */
    private boolean includeNulls = false;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public boolean isIncludeNulls() {
        return includeNulls;
    }

    public void setIncludeNulls(boolean includeNulls) {
        this.includeNulls = includeNulls;
    }
}
