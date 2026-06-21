package com.aidentx.logging.config;

import com.aidentx.logging.model.CorrelationId;
import com.aidentx.logging.model.JsonFormat;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the unified logging system.
 * 
 * These properties can be configured in application.properties or
 * application.yml
 * to customize the behavior of the unified logging system.
 * 
 * Example configuration in application.properties:
 * unified.logging.enabled=true
 * unified.logging.correlation-id.enabled=true
 * unified.logging.correlation-id.auto-generate=true
 * unified.logging.json-format.enabled=true
 * unified.logging.json-format.pretty-print=false
 * unified.logging.ai-insights.enabled=false
 */
@ConfigurationProperties(prefix = "unified.logging")
public class UnifiedLoggingProperties {

    /**
     * Whether unified logging is enabled
     */
    private boolean enabled = true;

    /**
     * Default source name for log entries when not explicitly specified
     */
    private String defaultSource = "unknown";

    /**
     * Correlation ID configuration
     */
    private CorrelationId correlationId = new CorrelationId();

    /**
     * JSON formatting configuration
     */
    private JsonFormat jsonFormat = new JsonFormat();


    /**
     * Configuration for the OpenSearch log appender.
     */
    private OpenSearchProperties opensearch = new OpenSearchProperties();

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultSource() {
        return defaultSource;
    }

    public void setDefaultSource(String defaultSource) {
        this.defaultSource = defaultSource;
    }

    public CorrelationId getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(CorrelationId correlationId) {
        this.correlationId = correlationId;
    }

    public JsonFormat getJsonFormat() {
        return jsonFormat;
    }

    public void setJsonFormat(JsonFormat jsonFormat) {
        this.jsonFormat = jsonFormat;
    }

    // Add getter and setter for the new opensearch property
    public OpenSearchProperties getOpensearch() {
        return opensearch;
    }

    public void setOpensearch(OpenSearchProperties opensearch) {
        this.opensearch = opensearch;
    }

}