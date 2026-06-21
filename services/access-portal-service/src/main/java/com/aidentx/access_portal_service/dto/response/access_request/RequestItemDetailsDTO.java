package com.aidentx.access_portal_service.dto.response.access_request;

import com.aidentx.access_portal_service.service.enrichment.NameResolvable;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

/**
 * Detailed DTO for an access request item.
 */
@Getter
@Setter
public class RequestItemDetailsDTO implements NameResolvable {
    private UUID itemId;
    private String itemType;
    private String itemName;
    private String itemDescription;
    private String requestTarget;
    private List<UUID> beneficiaryUserIds;
    private List<String> beneficiaryUserNames;
    private Integer validityDays;
    private String urgency;
    private String justification;
    private String tileId;
    private String tileName;

    private List<ProvisioningStatusDTO> provisioningStatuses;
    private List<ApprovalStepDTO> approvalSteps;
}