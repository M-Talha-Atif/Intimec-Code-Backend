package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO showing the complete lifecycle timeline of a request.
 */
@Getter
@Setter
public class RequestLifecycleDTO {

    private OffsetDateTime submittedAt;
    private OffsetDateTime approvedAt;
    private OffsetDateTime rejectedAt;
    private OffsetDateTime provisionedAt;
    private OffsetDateTime withdrawnAt;
    private Long totalDurationHours;
    private Long approvalDurationHours;
    private Long provisioningDurationHours;
    private List<LifecycleEventDTO> events;
}