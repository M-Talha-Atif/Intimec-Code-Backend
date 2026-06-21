package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * DTO for a single event in the request lifecycle.
 */
@Getter
@Setter
public class LifecycleEventDTO {

    private String eventType;
    private String eventDescription;
    private String performedBy;
    private OffsetDateTime occurredAt;
    private String icon;
    private String color;
}