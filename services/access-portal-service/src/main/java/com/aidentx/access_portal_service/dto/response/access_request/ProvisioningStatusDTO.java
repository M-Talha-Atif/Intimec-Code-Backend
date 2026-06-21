package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for provisioning status.
 * Each record represents provisioning for ONE (role + user) combination.
 */
@Getter
@Setter
public class ProvisioningStatusDTO {
    // Unique provisioning ID
    private UUID provisioningId;

    //  Which role is being provisioned
    private String roleId;

    // Which user is receiving this role
    private String targetUserId;

    private String status;

    private OffsetDateTime provisioningStartedAt; // was: startedAt
    private OffsetDateTime provisioningCompletedAt; // was: completedAt

    private String targetSystem;
    private String externalTicketId;
    private String errorMessage;
    private OffsetDateTime accessExpiresAt;

    // Retry tracking
    private Integer retryCount;
}