package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Getter;
import lombok.Setter;

/**
 * Metadata about the request for analytics and display.
 */
@Getter
@Setter
public class RequestMetadataDTO {

    private Integer totalItems;
    private Integer approvedItems;
    private Integer rejectedItems;
    private Integer pendingItems;
    private Integer riskScore;
    private Boolean hasSODConflicts;
    private Boolean hasComplianceFlags;
    private Boolean isEmergencyRequest;
    private Integer totalApprovalSteps;
    private Integer completedApprovalSteps;
    private String currentApprovalStep;
}