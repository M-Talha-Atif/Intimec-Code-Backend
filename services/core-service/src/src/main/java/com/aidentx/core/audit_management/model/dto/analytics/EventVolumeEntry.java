package com.aidentx.core.audit_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single day's risk-bucketed event counts within the event volume timeline.
 *
 * <p>Each instance represents one calendar day in the response array. The
 * frontend area chart stacks the three risk counts to produce total daily
 * volume with colour-coded layers.
 *
 * <p>Example for a single day:
 * <pre>
 * {
 *   "date": "Mar 01",
 *   "low": 142,
 *   "medium": 67,
 *   "high": 23
 * }
 * </pre>
 *
 * <p>The {@code date} format ("MMM dd") is produced by the service layer so
 * the frontend can use the value directly as the {@code XAxis dataKey} without
 * any client-side transformation.
 *
 * <p>Risk counts default to zero. The repository projection only returns rows
 * that exist in the database, so days with no events of a given risk level
 * produce no row. The service fills missing risk levels with zero for each
 * day before returning the response, ensuring the frontend always receives all
 * three keys per day entry.
 */
public class EventVolumeEntry {

    /**
     * Calendar day label formatted as "MMM dd" (e.g. "Mar 01", "Dec 31").
     * Matches the format the frontend renders on the XAxis of the area chart.
     */
    @JsonProperty("date")
    private String date;

    /**
     * Count of events with risk level 'low' on this day.
     */
    @JsonProperty("low")
    private long low;

    /**
     * Count of events with risk level 'medium' on this day.
     */
    @JsonProperty("medium")
    private long medium;

    /**
     * Count of events with risk level 'high' on this day.
     * High is the maximum severity level stored in audit_logs.risk.
     */
    @JsonProperty("high")
    private long high;

    public EventVolumeEntry() {}

    public EventVolumeEntry(String date, long low, long medium, long high) {
        this.date = date;
        this.low = low;
        this.medium = medium;
        this.high = high;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public long getLow() { return low; }
    public void setLow(long low) { this.low = low; }

    public long getMedium() { return medium; }
    public void setMedium(long medium) { this.medium = medium; }

    public long getHigh() { return high; }
    public void setHigh(long high) { this.high = high; }
}