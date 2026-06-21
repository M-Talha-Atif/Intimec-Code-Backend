package com.aidentx.core.log_management.model.pojo;

import java.util.Map;

/**
 * Aggregation bucket for faceted search
 */
public class AggregationBucket {
    private String key;
    private long count;
    private Map<String, Object> metadata;

    public AggregationBucket() {
    }

    public AggregationBucket(String key, long count) {
        this.key = key;
        this.count = count;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}