package com.aidentx.access_portal_service.dto.response.access_request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO for a comment on an access request.
 */
@Getter
@Setter
public class RequestCommentDTO {

    private UUID commentId;
    private UUID commentedByUserId;
    private String commentedByUserName;
    private String commentText;
    private String commentType;
    private UUID parentCommentId;
    private OffsetDateTime commentedAt;
    private Boolean isInternal;
    private UUID mentionedUserId;
}