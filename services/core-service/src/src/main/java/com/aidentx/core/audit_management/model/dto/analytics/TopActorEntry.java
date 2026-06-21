package com.aidentx.core.audit_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single actor's activity summary within the top actors result.
 *
 * <p>Example serialised entry:
 * <pre>
 * {
 *   "name": "john.doe",
 *   "type": "user",
 *   "events": 412,
 *   "failures": 18,
 *   "riskLevel": "medium"
 * }
 * </pre>
 *
 * <p>Frontend mapping:
 * <ul>
 *   <li>{@code name}      -- actor column</li>
 *   <li>{@code type}      -- type badge</li>
 *   <li>{@code events}    -- events column</li>
 *   <li>{@code failures}  -- failures column; values above 10 render in red</li>
 *   <li>{@code riskLevel} -- one of "low", "medium", "high"; drives badge colour</li>
 * </ul>
 */
public class TopActorEntry {

    /**
     * Actor name as stored in audit_logs.actor_name.
     */
    @JsonProperty("name")
    private String name;

    /**
     * Actor type as stored in audit_logs.actor_type (e.g. "user", "system", "service").
     */
    @JsonProperty("type")
    private String type;

    /**
     * Total number of audit events attributed to this actor in the requested window.
     */
    @JsonProperty("events")
    private long events;

    /**
     * Number of events with status 'failure' attributed to this actor.
     */
    @JsonProperty("failures")
    private long failures;

    /**
     * Risk badge derived from the weighted average of the actor's event risk levels.
     * One of: "low", "medium", "high".
     *
     * <p>Computed by the service layer using ceiling rounding:
     * weightedAvg in [0, 1.5)  -- "low",
     * weightedAvg in [1.5, 2.5) -- "medium",
     * weightedAvg in [2.5, 3.0] -- "high".
     */
    @JsonProperty("riskLevel")
    private String riskLevel;

    public TopActorEntry() {}

    public TopActorEntry(String name, String type, long events, long failures, String riskLevel) {
        this.name = name;
        this.type = type;
        this.events = events;
        this.failures = failures;
        this.riskLevel = riskLevel;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public long getEvents() { return events; }
    public void setEvents(long events) { this.events = events; }

    public long getFailures() { return failures; }
    public void setFailures(long failures) { this.failures = failures; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String riskLevel) { this.riskLevel = riskLevel; }
}