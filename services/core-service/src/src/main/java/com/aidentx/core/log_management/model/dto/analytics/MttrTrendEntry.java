package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single day's MTTR value in the 7-day trend line.
 *
 * <p>MTTR (Mean Time To Resolution) for a day is the average number of
 * minutes between when a log entry was created (@timestamp) and when it
 * was resolved (resolvedAt) for all entries resolved on that calendar day.
 *
 * <p>Example: on Monday three errors were resolved. Their resolution times
 * were 30 min, 45 min, and 60 min. MTTR for Monday = (30+45+60)/3 = 45 min.
 *
 * <p>Example serialised entry:
 * <pre>
 * { "day": "Mon", "mttr": 45.0 }
 * </pre>
 *
 * <p>Frontend mapping:
 * <ul>
 *   <li>{@code day}  -- XAxis dataKey label on the line chart</li>
 *   <li>{@code mttr} -- Y-axis value in minutes</li>
 * </ul>
 *
 * <p>When no entries were resolved on a given day the mttr value is 0.0.
 * The frontend renders this as a zero-point on the line rather than a gap.
 */
public class MttrTrendEntry {

    /**
     * Short day label in the caller's timezone.
     * Values: "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun".
     * The label corresponds to the calendar day in the timezone passed
     * to the endpoint, not necessarily UTC.
     *
     * <p>Example: a log resolved at 01:00 UTC on Tuesday is 06:00 PKT
     * on Tuesday for an Asia/Karachi user. Both label it "Tue".
     * A New York user (UTC-5) would see the same event at 20:00 Monday
     * and label it "Mon". The timezone parameter controls this.
     */
    @JsonProperty("day")
    private String day;

    /**
     * Mean Time To Resolution in minutes for this calendar day.
     * Computed as: avg(resolvedAt - @timestamp) / 60000 across all
     * entries resolved on this day.
     * Value is 0.0 when no entries were resolved on this day.
     * Rounded to one decimal place.
     */
    @JsonProperty("mttr")
    private double mttr;

    public MttrTrendEntry() {}

    public MttrTrendEntry(String day, double mttr) {
        this.day  = day;
        this.mttr = mttr;
    }

    public String getDay()       { return day; }
    public void setDay(String d) { this.day = d; }

    public double getMttr()        { return mttr; }
    public void setMttr(double m)  { this.mttr = m; }
}