package com.aidentx.iam.events;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AggregationCompletedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("resourceId") UUID resourceId,
        @JsonProperty("aggregationStartedAt") Instant aggregationStartedAt,
        @JsonProperty("aggregationCompletedAt") Instant aggregationCompletedAt,
        @JsonProperty("eventSource") String eventSource,

        @JsonProperty("initiatingUserId") UUID initiatingUserId,
        @JsonProperty("initiatingUsername") String initiatingUsername) {
    public AggregationCompletedEvent(
            UUID resourceId,
            Instant aggregationStartedAt,
            Instant aggregationCompletedAt,
            UUID initiatingUserId,
            String initiatingUsername) {
        this(
                UUID.randomUUID(),
                resourceId,
                aggregationStartedAt,
                aggregationCompletedAt,
                "core-service",
                initiatingUserId,
                initiatingUsername);
    }
}