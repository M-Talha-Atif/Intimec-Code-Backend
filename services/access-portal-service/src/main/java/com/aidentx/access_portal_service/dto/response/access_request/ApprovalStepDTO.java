package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO representing an approval step in the workflow.
 * Each step represents ONE (role + user + approver) combination.
 */
@Getter
@Setter
public class ApprovalStepDTO {
    private UUID stepId;

    // NEW: Which item is being approved
    private String itemId;

    // Which user is receiving this item
    private String targetUserId;
    private String targetUserName;

    private Integer stepOrder;
    private String approverRole;
    private String assignedApproverId;
    private String assignedApproverName;
    private String assignedApproverEmail;
    private String status;

    private OffsetDateTime createdAt;

    private OffsetDateTime respondedAt;
    private String decision;
    private String comments;
    private UUID delegatedToUserId;
    private String delegatedToUserName;
    private OffsetDateTime delegatedAt;
}