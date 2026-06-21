package com.aidentx.core.audit_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single (objectType, eventName, count) cell in the change impact heatmap.
 *
 * <p>The heatmap endpoint returns a list of these entries. Each entry
 * represents one non-zero cell in the matrix. Zero cells are omitted from
 * the response -- the frontend fills them when rendering the matrix.
 *
 * <p>Example serialised entry:
 * <pre>
 * { "objectType": "Role", "eventName": "assign", "count": 142 }
 * </pre>
 *
 * <p>Full example response for a small dataset:
 * <pre>
 * [
 *   { "objectType": "Role",       "eventName": "assign",    "count": 142 },
 *   { "objectType": "Role",       "eventName": "provision", "count": 38  },
 *   { "objectType": "User",       "eventName": "assign",    "count": 97  },
 *   { "objectType": "Permission", "eventName": "approve",   "count": 21  }
 * ]
 * </pre>
 *
 * <p>Frontend matrix rendering:
 * <pre>
 *              assign   provision   approve
 *   Role         142       38          0     (0 filled by frontend)
 *   User          97        0          0
 *   Permission     0        0         21
 * </pre>
 *
 * <p>The {@code count} value drives the heat intensity of the cell.
 * Higher counts produce darker or more saturated cell colours in the
 * heatmap visualisation. The frontend computes intensity relative to the
 * maximum count in the response:
 * <pre>
 *   intensity = count / max(all counts in response)
 *   intensity > 0.70 -- red   (destructive)
 *   intensity > 0.40 -- amber (warning)
 *   intensity > 0.15 -- blue  (primary/30)
 *   otherwise        -- light blue (primary/10)
 * </pre>
 */
public class HeatmapEntry {

    /**
     * The object type as stored in audit_logs.object_type.
     * Examples: "Role", "User", "Permission", "Account", "Policy".
     * Maps to a row in the frontend heatmap matrix.
     */
    @JsonProperty("objectType")
    private String objectType;

    /**
     * The event name as stored in audit_logs.event.
     * Examples: "assign", "provision", "approve", "revoke", "update".
     * Maps to a column in the frontend heatmap matrix.
     */
    @JsonProperty("eventName")
    private String eventName;

    /**
     * Count of audit events for this (objectType, eventName) combination
     * within the requested tenant and date window.
     * Drives the heat intensity of the cell in the frontend visualisation.
     */
    @JsonProperty("count")
    private long count;

    public HeatmapEntry() {}

    public HeatmapEntry(String objectType, String eventName, long count) {
        this.objectType = objectType;
        this.eventName = eventName;
        this.count = count;
    }

    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}