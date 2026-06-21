package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Detailed response DTO for a single access request.
 * Includes full request details, items, approval workflow, and comments.
 */
@Getter
@Setter
public class RequestDetailsResponseDTO {

    private UUID requestId;
    private UUID requestedByUserId;
    private String requestedByUserName;
    private String overallJustification;
    private String status;
    private OffsetDateTime requestedAt;
    private OffsetDateTime lastUpdatedAt;

    private List<RequestItemDetailsDTO> items;
//    private List<ApprovalStepDTO> approvalSteps;
    private List<RequestCommentDTO> comments;
    private List<AuditLogEntryDTO> auditLog;

    private RequestMetadataDTO metadata;
}