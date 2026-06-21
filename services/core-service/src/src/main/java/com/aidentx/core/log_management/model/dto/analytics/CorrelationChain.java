package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * A single correlation chain representing a cascading sequence of log events
 * that share the same {@code correlationId}.
 *
 * <p>Example serialised chain:
 * <pre>
 * {
 *   "id": "req-17136fed-b7ed-4441-87d4-d59bc7b4730f",
 *   "rootCause": "LDAP bind failed: invalid credentials for service account",
 *   "severity": "high",
 *   "timestamp": "2026-03-18T10:15:00.000Z",
 *   "affectedCount": 4,
 *   "chain": [
 *     { "step": 1, "source": "JwtAuthenticationFilter", "message": "LDAP bind failed...", "level": "ERROR" },
 *     { "step": 2, "source": "GatewayPermissionFilter",  "message": "Permission check failed...", "level": "ERROR" },
 *     { "step": 3, "source": "UserNotificationService",  "message": "Notification dispatch failed...", "level": "WARN" },
 *     { "step": 4, "source": "TokenService",             "message": "Token refresh skipped", "level": "INFO" }
 *   ]
 * }
 * </pre>
 *
 * <p>Frontend mapping:
 * <ul>
 *   <li>{@code id}           -- React key for the chain card</li>
 *   <li>{@code rootCause}    -- bold header text of the chain card</li>
 *   <li>{@code severity}     -- badge: "high"=red, "medium"=yellow, "low"=green</li>
 *   <li>{@code timestamp}    -- not displayed but available for sorting</li>
 *   <li>{@code affectedCount}-- "X affected" label in the card header</li>
 *   <li>{@code chain}        -- step chips rendered left to right with arrows</li>
 * </ul>
 */
public class CorrelationChain {

    /**
     * The correlationId value shared by all documents in this chain.
     * Used as the React key and as the drilldown identifier.
     * Example: "req-17136fed-b7ed-4441-87d4-d59bc7b4730f"
     */
    @JsonProperty("id")
    private String id;

    /**
     * The message from the first ERROR or WARN document in the chain by
     * timestamp. Represents the originating failure that triggered the cascade.
     * Falls back to the first document message when no ERROR or WARN exists.
     *
     * <p>Example: "LDAP bind failed: invalid credentials for service account"
     */
    @JsonProperty("rootCause")
    private String rootCause;

    /**
     * Highest severity present in the chain.
     * <ul>
     *   <li>"high"   -- chain contains at least one ERROR document</li>
     *   <li>"medium" -- chain contains WARN but no ERROR</li>
     *   <li>"low"    -- chain contains only INFO or DEBUG</li>
     * </ul>
     */
    @JsonProperty("severity")
    private String severity;

    /**
     * ISO-8601 timestamp of the first document in the chain (earliest event).
     * Example: "2026-03-18T10:15:00.000Z"
     */
    @JsonProperty("timestamp")
    private String timestamp;

    /**
     * Total number of documents in this chain.
     * Displayed as "X affected" in the chain card header.
     */
    @JsonProperty("affectedCount")
    private int affectedCount;

    /**
     * Ordered list of steps in the chain, step 1 = earliest event.
     * Capped at {@link LogAnalyticsCorrelationConstants#MAX_STEPS_PER_CHAIN}.
     */
    @JsonProperty("chain")
    private List<CorrelationStep> chain;

    public CorrelationChain() {}

    public CorrelationChain(
            String id,
            String rootCause,
            String severity,
            String timestamp,
            int affectedCount,
            List<CorrelationStep> chain) {
        this.id = id;
        this.rootCause = rootCause;
        this.severity = severity;
        this.timestamp = timestamp;
        this.affectedCount = affectedCount;
        this.chain = chain;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRootCause() { return rootCause; }
    public void setRootCause(String rootCause) { this.rootCause = rootCause; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public int getAffectedCount() { return affectedCount; }
    public void setAffectedCount(int affectedCount) { this.affectedCount = affectedCount; }

    public List<CorrelationStep> getChain() { return chain; }
    public void setChain(List<CorrelationStep> chain) { this.chain = chain; }
}