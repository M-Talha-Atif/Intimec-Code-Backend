package com.aidentx.core.audit_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single event type and its occurrence count within the distribution result.
 *
 * <p>Each instance represents one distinct event name found in audit_logs.event
 * for the requested tenant and date window. The list is ordered by
 * {@code count} descending so the most active event type appears first.
 *
 * <p>Example serialised entry:
 * <pre>
 * {
 *   "eventName": "provision",
 *   "count": 843
 * }
 * </pre>
 *
 * <p>Frontend mapping: the {@code eventName} value maps directly to the
 * {@code type} field rendered on the YAxis of the horizontal bar chart.
 * The {@code count} value drives the bar length on the XAxis.
 * Color assignment is handled entirely on the frontend and is not part
 * of this response.
 */
public class EventDistributionEntry {

    /**
     * The event name as stored in audit_logs.event (e.g. "provision", "assign", "approve").
     * Maps to the {@code type} field the frontend bar chart renders on the YAxis.
     */
    @JsonProperty("eventName")
    private String eventName;

    /**
     * Total occurrences of this event name within the requested window and tenant.
     * Drives the bar length on the XAxis of the frontend chart.
     */
    @JsonProperty("count")
    private long count;

    public EventDistributionEntry() {}

    public EventDistributionEntry(String eventName, long count) {
        this.eventName = eventName;
        this.count = count;
    }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}