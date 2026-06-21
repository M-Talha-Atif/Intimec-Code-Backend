package com.aidentx.access_portal_service.dto.response.access_request;


import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class AccessRequestItemResponseDTO {

    private UUID requestItemId;

    private UUID tileId;
    private String tileTitle;

    private String itemType;
    private UUID itemId;
    private String itemName;

    private String requestTarget; // SELF / OTHERS

    private UUID beneficiaryUserId;
    private String beneficiaryName;

    private Integer validityDays;
    private String urgency; // LOW, MEDIUM, HIGH

    private String justification;
}

