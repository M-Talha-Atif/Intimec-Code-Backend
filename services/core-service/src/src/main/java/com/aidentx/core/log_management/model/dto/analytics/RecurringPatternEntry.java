package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single recurring message pattern detected in the log index.
 *
 * <p>A pattern is any message string that appears at least
 * {@link com.aidentx.core.log_management.config.constants.analytics.LogAnalyticsRecurringPatternsConstants#MIN_OCCURRENCES}
 * times. Patterns are ranked by occurrence count descending so the most
 * frequently repeated message appears first.
 *
 * <p>Example: the message "Permission denied for user" appears 312 times.
 * firstSeen is the timestamp of the earliest occurrence, lastSeen is the
 * most recent. source is the component that produced the most occurrences
 * of this message.
 *
 * <p>Example serialised entry:
 * <pre>
 * {
 *   "id": "pat-0",
 *   "message": "Permission denied for user",
 *   "occurrences": 312,
 *   "firstSeen": "2026-03-17T10:15:00.000Z",
 *   "lastSeen":  "2026-03-18T15:42:00.000Z",
 *   "source":    "GatewayPermissionFilter",
 *   "severity":  "high"
 * }
 * </pre>
 *
 * <p>Severity derivation:
 * <ul>
 *   <li>The most frequent log level among all occurrences of this message
 *       determines the severity label.</li>
 *   <li>ERROR -> "high", WARN -> "medium", INFO/DEBUG -> "low".</li>
 *   <li>This maps to the badge colours the frontend already uses for
 *       the mock recurring patterns section.</li>
 * </ul>
 */
public class RecurringPatternEntry {

    /**
     * Synthetic identifier for React key and drilldown reference.
     * Format: "pat-{index}" where index is the rank position (0-based).
     * Example: "pat-0" is the most frequently repeated message.
     */
    @JsonProperty("id")
    private String id;

    /**
     * The exact message text that recurs.
     * Value comes from the message.keyword bucket key in OpenSearch.
     * Example: "Permission denied for user"
     */
    @JsonProperty("message")
    private String message;

    /**
     * Total number of log entries with this exact message text.
     * Value is the doc_count of the terms aggregation bucket.
     */
    @JsonProperty("occurrences")
    private long occurrences;

    /**
     * ISO-8601 UTC timestamp of the earliest log entry with this message.
     * Derived from a min(@timestamp) sub-aggregation inside the message bucket.
     * Example: "2026-03-17T10:15:00.000Z"
     */
    @JsonProperty("firstSeen")
    private String firstSeen;

    /**
     * ISO-8601 UTC timestamp of the most recent log entry with this message.
     * Derived from a max(@timestamp) sub-aggregation inside the message bucket.
     * Example: "2026-03-18T15:42:00.000Z"
     */
    @JsonProperty("lastSeen")
    private String lastSeen;

    /**
     * The source component that produced the most occurrences of this message.
     * Derived from a top_source terms sub-aggregation (size=1) inside the
     * message bucket. Returns the single highest-count source.
     * Example: "GatewayPermissionFilter"
     */
    @JsonProperty("source")
    private String source;

    /**
     * Severity label derived from the dominant log level of this message.
     * The dominant level is the one with the highest doc_count among all
     * occurrences. Mapping:
     * ERROR -> "high", WARN -> "medium", INFO/DEBUG -> "low".
     */
    @JsonProperty("severity")
    private String severity;

    public RecurringPatternEntry() {}

    public RecurringPatternEntry(
            String id,
            String message,
            long occurrences,
            String firstSeen,
            String lastSeen,
            String source,
            String severity) {
        this.id          = id;
        this.message     = message;
        this.occurrences = occurrences;
        this.firstSeen   = firstSeen;
        this.lastSeen    = lastSeen;
        this.source      = source;
        this.severity    = severity;
    }

    public String getId()           { return id; }
    public void setId(String id)    { this.id = id; }

    public String getMessage()               { return message; }
    public void setMessage(String message)   { this.message = message; }

    public long getOccurrences()                  { return occurrences; }
    public void setOccurrences(long occurrences)  { this.occurrences = occurrences; }

    public String getFirstSeen()               { return firstSeen; }
    public void setFirstSeen(String firstSeen) { this.firstSeen = firstSeen; }

    public String getLastSeen()              { return lastSeen; }
    public void setLastSeen(String lastSeen) { this.lastSeen = lastSeen; }

    public String getSource()             { return source; }
    public void setSource(String source)  { this.source = source; }

    public String getSeverity()               { return severity; }
    public void setSeverity(String severity)  { this.severity = severity; }
}