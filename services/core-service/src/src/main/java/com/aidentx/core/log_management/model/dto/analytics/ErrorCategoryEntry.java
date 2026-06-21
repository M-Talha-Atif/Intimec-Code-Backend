package com.aidentx.core.log_management.model.dto.analytics;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single category and its ERROR+WARN event count.
 *
 * <p>The {@code category} value is taken directly from the
 * {@code category.keyword} field in OpenSearch -- no transformation,
 * no mapping, no hardcoded labels. Whatever is stored in the index
 * is returned as-is.
 *
 * <p>Example serialised entries:
 * <pre>
 * { "category": "Security-Auth",            "count": 312 }
 * { "category": "SECURITY",                 "count": 198 }
 * { "category": "NOTIFICATIONS_MANAGEMENT", "count": 87  }
 * </pre>
 *
 * <p>Frontend mapping:
 * <ul>
 *   <li>{@code category} -- YAxis dataKey label on the horizontal bar chart</li>
 *   <li>{@code count}    -- bar length on the XAxis</li>
 *   <li>color            -- assigned by the frontend by cycling through
 *                           chart CSS variables; not part of this response</li>
 * </ul>
 */
public class ErrorCategoryEntry {

    /**
     * Category name as stored in the {@code category} field of the log document.
     * Examples: "Security-Auth", "SECURITY", "NOTIFICATIONS_MANAGEMENT".
     */
    @JsonProperty("category")
    private String category;

    /**
     * Total count of ERROR and WARN events in this category within the
     * requested window.
     */
    @JsonProperty("count")
    private long count;

    public ErrorCategoryEntry() {}

    public ErrorCategoryEntry(String category, long count) {
        this.category = category;
        this.count = count;
    }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
}