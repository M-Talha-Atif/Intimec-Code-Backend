package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single step within a correlation chain.
 *
 * <p>Each step corresponds to one log document that shares the chain's
 * correlationId. Steps are ordered by timestamp ascending so step 1 is
 * the earliest event in the chain.
 *
 * <p>Example serialised step:
 * <pre>
 * {
 *   "step": 1,
 *   "source": "JwtAuthenticationFilter",
 *   "message": "LDAP bind failed: invalid credentials for service account",
 *   "level": "ERROR"
 * }
 * </pre>
 *
 * <p>Frontend rendering:
 * <ul>
 *   <li>{@code step}    -- not rendered directly; used for ordering</li>
 *   <li>{@code source}  -- bold label in the step chip</li>
 *   <li>{@code message} -- truncated to 40 chars in the chip</li>
 *   <li>{@code level}   -- drives chip background colour:
 *       ERROR=destructive/10, WARN=warning/10, INFO/DEBUG=primary/10</li>
 * </ul>
 */
public class CorrelationStep {

    /**
     * Sequential step number starting at 1.
     * Derived from the document's position after sorting by @timestamp ascending.
     */
    @JsonProperty("step")
    private int step;

    /**
     * Source component name as stored in the log document.
     * Example: "JwtAuthenticationFilter", "GatewayPermissionFilter".
     */
    @JsonProperty("source")
    private String source;

    /**
     * Full log message from the document.
     * The frontend truncates to 40 characters when rendering the step chip.
     */
    @JsonProperty("message")
    private String message;

    /**
     * Log level of this step: "ERROR", "WARN", "INFO", or "DEBUG".
     * Drives the chip colour in the frontend.
     */
    @JsonProperty("level")
    private String level;

    public CorrelationStep() {}

    public CorrelationStep(int step, String source, String message, String level) {
        this.step = step;
        this.source = source;
        this.message = message;
        this.level = level;
    }

    public int getStep() { return step; }
    public void setStep(int step) { this.step = step; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
}