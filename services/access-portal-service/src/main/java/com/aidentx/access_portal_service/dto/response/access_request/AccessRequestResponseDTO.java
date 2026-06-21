package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AccessRequestResponseDTO {

    private UUID requestId;
    private String status;
    private String overallJustification;
    private String tenantId;
    private List<AccessRequestItemResponseDTO> items;
}
