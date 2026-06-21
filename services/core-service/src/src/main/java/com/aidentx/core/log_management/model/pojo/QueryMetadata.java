package com.aidentx.core.log_management.model.pojo;

/**
 * Query execution metadata
 */
public class QueryMetadata {
    private long executionTimeMs;
    private String queryId;
    private int shardsTotal;
    private int shardsSuccessful;
    private int shardsFailed;
    private boolean timedOut;

    public QueryMetadata() {
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public int getShardsTotal() {
        return shardsTotal;
    }

    public void setShardsTotal(int shardsTotal) {
        this.shardsTotal = shardsTotal;
    }

    public int getShardsSuccessful() {
        return shardsSuccessful;
    }

    public void setShardsSuccessful(int shardsSuccessful) {
        this.shardsSuccessful = shardsSuccessful;
    }

    public int getShardsFailed() {
        return shardsFailed;
    }

    public void setShardsFailed(int shardsFailed) {
        this.shardsFailed = shardsFailed;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public void setTimedOut(boolean timedOut) {
        this.timedOut = timedOut;
    }
}