package com.aidentx.access_portal_service.dto.response.access_request;


import com.aidentx.access_portal_service.entity.access_request.RequestStatus;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AccessRequestHistoryResponseDTO {

    private UUID requestId;
    private OffsetDateTime requestedAt;
    private RequestStatus status;
    private String overallJustification;
    private List<AccessRequestItemResponseDTO> items;
}

